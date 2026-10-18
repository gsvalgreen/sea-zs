package br.com.fateczs.seazs.featureTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Fluxo 2: organizador cria evento -> agendamento -> atividade e consulta pelas listagens. Bordas de
 * listarDisponivelData ficam nas camadas inferiores.
 */
public class EventoAgendamentoFeatureTest extends FeatureTestBase {

	@Test
	public void criarEventoAgendamentoEAtividade_apareceNasListagensDoEventoEDoAgendamento() throws Exception {
		long agora = System.currentTimeMillis();
		int organizador = criarUsuario(unico("org") + "@teste.com", "s");
		String nomeEvento = unico("Evento");
		int evento = criarEvento(nomeEvento, organizador, agora + DIA, agora + 2 * DIA);
		int agendamento = criarAgendamento(evento, organizador, agora + DIA, agora + 2 * DIA, 20);
		String nomeAtividade = unico("Atividade");
		int atividade = criarAtividade(nomeAtividade, agendamento, agora + DIA, agora + DIA + HORA, false);

		mvc.perform(post("/agendamento/listarEvento").with(admin()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + evento + "}"))
				.andExpect(jsonPath("$[0].id").value(agendamento))
				.andExpect(jsonPath("$[0].evento.nome").value(nomeEvento))
				// o corpo indicou "organizador", mas a autoria e do usuario autenticado (o admin do cenario)
				.andExpect(jsonPath("$[0].usuarioOrganizador.nome").value("Admin"));
		mvc.perform(post("/atividade/listarAgendamento").with(admin()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + agendamento + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(atividade))
				.andExpect(jsonPath("$[0].nome").value(nomeAtividade));
	}
}
