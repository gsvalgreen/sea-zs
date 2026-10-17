package br.com.fateczs.seazs.featureTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Fluxo 1: cadastro de usuario e login (MySQL real). Senha errada e email inexistente ja sao cobertos na
 * camada integration, por isso nao se repetem aqui.
 */
public class LoginFeatureTest extends FeatureTestBase {

	@Test
	public void cadastrarEFazerLogin_comCredenciaisCorretas_devolveOUsuario() throws Exception {
		String email = unico("login") + "@teste.com";
		int id = criarUsuario(email, "Segredo123");

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"Segredo123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.email").value(email));
	}

	/**
	 * Caracterizacao (divergencia H2 x MySQL): EXPECTATIVA TEORICA, ainda nao confirmada por execucao. Pela
	 * semantica da collation utf8mb4_unicode_ci (case-insensitive), a query derivada de login deveria aceitar
	 * "SEGREDO123" para quem tem senha "Segredo123". A confirmar na primeira execucao com Docker; se o MySQL
	 * se comportar diferente, ajustar ou remover este teste. BCrypt (Fase 2) elimina o comportamento.
	 * So faz sentido no MySQL real, nao no H2.
	 */
	@Test
	public void login_hoje_ignoraDiferencaDeCaixaNaSenha_collationDoMySql() throws Exception {
		String email = unico("login") + "@teste.com";
		int id = criarUsuario(email, "Segredo123");

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"SEGREDO123\"}"))
				.andExpect(jsonPath("$.id").value(id));
	}
}
