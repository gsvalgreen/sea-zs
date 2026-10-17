package br.com.fateczs.seazs.featureTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/** Fluxo 2: organizador cria evento -> agendamento -> atividade e consulta pelas listagens. */
public class EventoAgendamentoFeatureTest extends FeatureTestBase {

	private static final long UM_DIA = 24L * 60 * 60 * 1000;

	@Test
	public void criarEventoAgendamentoEAtividade_apareceNasListagensDoEventoEDoAgendamento() throws Exception {
		long agora = System.currentTimeMillis();
		int organizador = criarUsuario(unico("org") + "@teste.com", "s");
		String nomeEvento = unico("Evento");
		int evento = criarEvento(nomeEvento, organizador, agora + UM_DIA, agora + 2 * UM_DIA);
		int agendamento = criarAgendamento(evento, organizador, agora + UM_DIA, agora + 2 * UM_DIA, 20);
		String nomeAtividade = unico("Atividade");
		int atividade = criarAtividade(nomeAtividade, agendamento, agora + UM_DIA, agora + UM_DIA + 3_600_000L, false);

		mvc.perform(post("/atividade/listarAgendamento").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + agendamento + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(atividade))
				.andExpect(jsonPath("$[0].nome").value(nomeAtividade));
	}

	@Test
	public void criarEventoEAgendamento_agendamentoListadoPeloEventoTrazOEventoEOOrganizador() throws Exception {
		long agora = System.currentTimeMillis();
		int organizador = criarUsuario(unico("org") + "@teste.com", "s");
		String nomeEvento = unico("Evento");
		int evento = criarEvento(nomeEvento, organizador, agora + UM_DIA, agora + 2 * UM_DIA);
		int agendamento = criarAgendamento(evento, organizador, agora + UM_DIA, agora + 2 * UM_DIA, 20);

		mvc.perform(post("/agendamento/listarEvento").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + evento + "}"))
				.andExpect(jsonPath("$[0].id").value(agendamento))
				.andExpect(jsonPath("$[0].evento.nome").value(nomeEvento))
				.andExpect(jsonPath("$[0].usuarioOrganizador.id").value(organizador));
	}

	@Test
	public void eventoComFimNoFuturo_apareceEmListarDisponivelData() throws Exception {
		long agora = System.currentTimeMillis();
		int organizador = criarUsuario(unico("org") + "@teste.com", "s");
		String nomeEvento = unico("Evento");
		criarEvento(nomeEvento, organizador, agora + UM_DIA, agora + 2 * UM_DIA);

		mvc.perform(post("/evento/listarDisponivelData"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nome=='" + nomeEvento + "')].nome").isNotEmpty());
	}

	@Test
	public void eventoJaEncerrado_naoApareceEmListarDisponivelData() throws Exception {
		long agora = System.currentTimeMillis();
		int organizador = criarUsuario(unico("org") + "@teste.com", "s");
		String nomeEvento = unico("Evento");
		criarEvento(nomeEvento, organizador, agora - 2 * UM_DIA, agora - UM_DIA);

		mvc.perform(post("/evento/listarDisponivelData"))
				.andExpect(jsonPath("$[?(@.nome=='" + nomeEvento + "')].nome").isEmpty());
	}
}
