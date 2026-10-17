package br.com.fateczs.seazs.featureTest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Fluxo 3: inscricao -> presenca -> certificado (atividade e staff), com somatorio de pontos.
 *
 * Sem Clock injetavel, as janelas sao relativas a "agora" (margem de minutos). Regras de borda da janela
 * de presenca ficam nos testes unitarios de InscricaoServiceImpl.
 */
public class PresencaCertificadoFeatureTest extends FeatureTestBase {

	private static final long MINUTO = 60_000L;
	private static final long HORA = 60 * MINUTO;

	private int idInscricao(int idUsuario) throws Exception {
		return primeiroId("/atividade/inscricao/listarUsuario", "{\"id\":" + idUsuario + "}",
				"$[*].sequencialInscricao");
	}

	private String validarPresenca(int idInscricao, int idAtividade, int idUsuario, int idPapel) throws Exception {
		return postar("/atividade/inscricao/validaPresenca", "{\"sequencialInscricao\":" + idInscricao
				+ ",\"atividade\":{\"id\":" + idAtividade + "},\"usuario\":{\"id\":" + idUsuario
				+ "},\"papel\":{\"id\":" + idPapel + "}}");
	}

	@Test
	public void atividadeContinua_inscreverValidarPresencaEEmitirCertificado() throws Exception {
		long agora = System.currentTimeMillis();
		int usuario = criarUsuario(unico("aluno") + "@teste.com", "s");
		int papel = criarPapel(unico("Participante"));
		int evento = criarEvento(unico("Evento"), usuario, agora - HORA, agora + 24 * HORA);
		int agendamento = criarAgendamento(evento, usuario, agora - HORA, agora + 24 * HORA, 20);
		String nomeAtividade = unico("Atividade");
		int atividade = criarAtividade(nomeAtividade, agendamento, agora - 2 * MINUTO, agora + 2 * HORA, true);
		inscrever(atividade, usuario, papel);

		String inscricaoValidada = validarPresenca(idInscricao(usuario), atividade, usuario, papel);

		assertEquals(Integer.valueOf(10), JsonPath.read(inscricaoValidada, "$.pontuacaoRecebida"));
		mvc.perform(post("/certificado/atividade/emitir").contentType(MediaType.APPLICATION_JSON)
				.content("{\"atividade\":{\"id\":" + atividade + "},\"usuario\":{\"id\":" + usuario + "},\"papel\":{\"id\":"
						+ papel + "}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.atividade").value(nomeAtividade))
				.andExpect(jsonPath("$.cargaHorariaAtividade").value(2));
	}

	@Test
	public void staff_validarPresencaEEmitirCertificadoDoAgendamento() throws Exception {
		long agora = System.currentTimeMillis();
		int usuario = criarUsuario(unico("staff") + "@teste.com", "s");
		String nomeEvento = unico("Evento");
		int evento = criarEvento(nomeEvento, usuario, agora - HORA, agora + 24 * HORA);
		int agendamento = criarAgendamento(evento, usuario, agora - 2 * MINUTO, agora + 3 * HORA, 20);
		postar("/agendamento/staff/inserir", "{\"agendamento\":{\"id\":" + agendamento + "},\"usuario\":{\"id\":" + usuario
				+ "},\"dtInscricao\":" + agora + ",\"checkIn\":false,\"checkOut\":false}");

		postar("/agendamento/staff/ValidarPresenca",
				"{\"agendamento\":{\"id\":" + agendamento + "},\"usuario\":{\"id\":" + usuario + "}}");

		mvc.perform(post("/certificado/agendamento/emitir").contentType(MediaType.APPLICATION_JSON)
				.content("{\"agendamento\":{\"id\":" + agendamento + "},\"usuario\":{\"id\":" + usuario + "}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nomeEvento").value(nomeEvento))
				.andExpect(jsonPath("$.papelUsuario").value("Staff"))
				.andExpect(jsonPath("$.cargaHoraria").value(3));
	}

	/**
	 * Especifico de MySQL: exercita a SQL nativa (SUM + UNION ALL inscricao/staff) de InscricaoRepository, que
	 * usa sintaxe/dialeto do MySQL e nao e garantida no H2 da camada integration. Por isso fica aqui, e e
	 * mantido como fluxo (inscricao + staff -> total) e nao como borda isolada.
	 */
	@Test
	public void somarPontos_somaPontosDeInscricaoEDeStaff() throws Exception {
		long agora = System.currentTimeMillis();
		int usuario = criarUsuario(unico("aluno") + "@teste.com", "s");
		int papel = criarPapel(unico("Participante"));
		int evento = criarEvento(unico("Evento"), usuario, agora - HORA, agora + 24 * HORA);
		int agendamento = criarAgendamento(evento, usuario, agora - 2 * MINUTO, agora + 3 * HORA, 20);
		int atividade = criarAtividade(unico("Atividade"), agendamento, agora - 2 * MINUTO, agora + 2 * HORA, true);
		inscrever(atividade, usuario, papel);
		validarPresenca(idInscricao(usuario), atividade, usuario, papel);
		postar("/agendamento/staff/inserir", "{\"agendamento\":{\"id\":" + agendamento + "},\"usuario\":{\"id\":" + usuario
				+ "},\"dtInscricao\":" + agora + ",\"checkIn\":false,\"checkOut\":false}");
		postar("/agendamento/staff/ValidarPresenca",
				"{\"agendamento\":{\"id\":" + agendamento + "},\"usuario\":{\"id\":" + usuario + "}}");

		String total = postar("/atividade/inscricao/somarPontos", "{\"id\":" + usuario + "}");

		assertEquals("30", total);
	}
}
