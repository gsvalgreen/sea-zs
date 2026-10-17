package br.com.fateczs.seazs.featureTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
	 * Caracterizacao (divergencia H2 x MySQL), CONFIRMADA por execucao em MySQL 5.7 real (Testcontainers) com
	 * --collation-server=utf8mb4_unicode_ci: a query derivada getByEmailAndSenha compara a senha sem diferenciar
	 * maiusculas de minusculas, entao "SEGREDO123" autentica quem tem senha "Segredo123" (UsuarioRepository.java:11).
	 * No H2 (camada integration) a comparacao e case-sensitive e esse login falharia. Comportamento inseguro que
	 * a adocao de BCrypt (Fase 2) elimina; nesse momento este teste deve ser invertido/removido.
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
