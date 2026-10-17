package br.com.fateczs.seazs.featureTest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/** Fluxo 1: cadastro de usuario e login (MySQL real). */
public class LoginFeatureTest extends FeatureTestBase {

	private String login(String email, String senha) throws Exception {
		return postar("/usuario/login", "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}");
	}

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

	@Test
	public void login_comSenhaErrada_respondeOkComCorpoVazio() throws Exception {
		String email = unico("login") + "@teste.com";
		criarUsuario(email, "Segredo123");

		assertEquals("", login(email, "errada"));
	}

	@Test
	public void login_comEmailInexistente_respondeOkComCorpoVazio() throws Exception {
		assertEquals("", login(unico("fantasma") + "@teste.com", "qualquer"));
	}

	/**
	 * Caracterizacao (divergencia H2 x MySQL): com a collation utf8mb4_unicode_ci a comparacao de senha na
	 * query derivada e case-INsensitive, entao "SEGREDO123" autentica quem tem senha "Segredo123".
	 * Suspeito: BCrypt (Fase 2) elimina isso. Este teste so tem sentido no MySQL real, nao no H2.
	 * NAO EXECUTADO ate aqui (sem Docker): a expectativa vem da semantica da collation.
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
