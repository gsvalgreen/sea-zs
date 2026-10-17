package br.com.fateczs.seazs.integration.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fateczs.seazs.controller.AgendamentoController;
import br.com.fateczs.seazs.controller.CertificadoController;
import br.com.fateczs.seazs.controller.EventoController;
import br.com.fateczs.seazs.controller.InscricaoController;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.CertificadoAtividade;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.AgendamentoService;
import br.com.fateczs.seazs.service.CertificadoService;
import br.com.fateczs.seazs.service.EventoService;
import br.com.fateczs.seazs.service.InscricaoService;

/**
 * Serializacao JSON dos grafos de entidades devolvidos por Evento, Agendamento, Inscricao e Certificado.
 * Os testes "..._naoExpoeASenha..." garantem o contrato do hotfix de seguranca
 * (@JsonProperty(access = WRITE_ONLY) em Usuario.senha): a senha nunca aparece nas respostas.
 */
@WebMvcTest({ EventoController.class, AgendamentoController.class, InscricaoController.class,
		CertificadoController.class })
public class EntidadesAninhadasWebMvcTest {

	@Autowired
	private MockMvc mvc;
	@MockitoBean
	private EventoService eventoService;
	@MockitoBean
	private AgendamentoService agendamentoService;
	@MockitoBean
	private InscricaoService inscricaoService;
	@MockitoBean
	private CertificadoService certificadoService;

	private Usuario ana;
	private Evento evento;
	private Agendamento agendamento;
	private Atividade atividade;

	@BeforeEach
	public void montarGrafo() {
		ana = DadosTeste.usuario("ana@teste.com");
		ana.setId(1);
		evento = DadosTeste.evento("Semana Tech", ana);
		evento.setId(10);
		agendamento = DadosTeste.agendamento(evento, ana, DadosTeste.PASSADO, DadosTeste.FUTURO);
		agendamento.setId(20);
		atividade = DadosTeste.atividade("Palestra", agendamento, DadosTeste.PASSADO, DadosTeste.FUTURO);
		atividade.setId(30);
		agendamento.setAtividades(Collections.singletonList(atividade));
	}

	private static final String CORPO_VAZIO = "{}";

	@Test
	public void listarEvento_serializaCamposDoEventoEOCriador() throws Exception {
		when(eventoService.listar()).thenReturn(Collections.singletonList(evento));

		mvc.perform(post("/evento/listar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(10))
				.andExpect(jsonPath("$[0].nome").value("Semana Tech"))
				.andExpect(jsonPath("$[0].usuarioCriador.email").value("ana@teste.com"));
	}

	@Test
	public void listarEvento_naoExpoeASenhaDoUsuarioCriador() throws Exception {
		when(eventoService.listar()).thenReturn(Collections.singletonList(evento));

		mvc.perform(post("/evento/listar"))
				.andExpect(jsonPath("$[0].usuarioCriador.senha").doesNotExist());
	}

	@Test
	public void listarAgendamento_naoSerializaCicloAtividadeParaAgendamento() throws Exception {
		when(agendamentoService.listar()).thenReturn(Collections.singletonList(agendamento));

		mvc.perform(post("/agendamento/listar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].atividades[0].nome").value("Palestra"))
				.andExpect(jsonPath("$[0].atividades[0].agendamento").doesNotExist());
	}

	@Test
	public void listarAgendamento_naoExpoeASenhaDoOrganizador() throws Exception {
		when(agendamentoService.listar()).thenReturn(Collections.singletonList(agendamento));

		mvc.perform(post("/agendamento/listar"))
				.andExpect(jsonPath("$[0].usuarioOrganizador.senha").doesNotExist());
	}

	@Test
	public void listarInscricao_serializaAtividadeSemAgendamentoCircular() throws Exception {
		Inscricao inscricao = DadosTeste.inscricao(atividade, ana, papel(), DadosTeste.PASSADO, true, false, 0);
		inscricao.setSequencialInscricao(40);
		when(inscricaoService.listar()).thenReturn(Collections.singletonList(inscricao));

		mvc.perform(post("/atividade/inscricao/listar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].sequencialInscricao").value(40))
				.andExpect(jsonPath("$[0].checkIn").value(true))
				.andExpect(jsonPath("$[0].atividade.nome").value("Palestra"))
				.andExpect(jsonPath("$[0].papel.nome").value("Participante"));
	}

	@Test
	public void listarInscricao_naoExpoeASenhaDoInscrito() throws Exception {
		Inscricao inscricao = DadosTeste.inscricao(atividade, ana, papel(), DadosTeste.PASSADO, true, false, 0);
		when(inscricaoService.listar()).thenReturn(Collections.singletonList(inscricao));

		mvc.perform(post("/atividade/inscricao/listar"))
				.andExpect(jsonPath("$[0].usuario.senha").doesNotExist());
	}

	@Test
	public void emitirCertificadoAtividade_serializaOsCamposDoCertificado() throws Exception {
		when(certificadoService.emiteCertificadoAtividade(any(Inscricao.class)))
				.thenReturn(new CertificadoAtividade("Ana", "Palestra", "Participante", 2));

		mvc.perform(post("/certificado/atividade/emitir").contentType(MediaType.APPLICATION_JSON)
				.content(corpoInscricao()))
				.andExpect(jsonPath("$.nomeUsuario").value("Ana"))
				.andExpect(jsonPath("$.atividade").value("Palestra"))
				.andExpect(jsonPath("$.papel").value("Participante"))
				.andExpect(jsonPath("$.cargaHorariaAtividade").value(2));
	}

	/** Caracterizacao: presenca nao validada vira 200 com corpo vazio (e nao 4xx). */
	@Test
	public void emitirCertificadoAtividade_semPresencaValidada_respondeOkComCorpoVazio() throws Exception {
		when(certificadoService.emiteCertificadoAtividade(any(Inscricao.class))).thenReturn(null);

		mvc.perform(post("/certificado/atividade/emitir").contentType(MediaType.APPLICATION_JSON)
				.content(corpoInscricao()))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	public void emitirCertificadoAtividade_semAtividadeUsuarioEPapel_respondeBadRequest() throws Exception {
		mvc.perform(post("/certificado/atividade/emitir").contentType(MediaType.APPLICATION_JSON)
				.content(CORPO_VAZIO))
				.andExpect(status().isBadRequest());
	}

	private static String corpoInscricao() {
		return "{\"atividade\":{\"id\":30},\"usuario\":{\"id\":1},\"papel\":{\"id\":2}}";
	}

	private static Papel papel() {
		Papel p = DadosTeste.papel("Participante");
		p.setId(2);
		return p;
	}
}
