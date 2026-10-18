package br.com.fateczs.seazs.featureTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Fluxo 1: cadastro, login e uso do token (MySQL real, Flyway V1+V2 e ddl-auto=validate). Senha errada, e-mail
 * inexistente, refresh/reuso e expiracao ja sao cobertos nas camadas unit e integration.
 */
public class LoginFeatureTest extends FeatureTestBase {

	@Autowired
	private JdbcTemplate jdbc;

	private static String credenciais(String email, String senha) {
		return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
	}

	private String senhaNoBanco(String email) {
		return jdbc.queryForObject("select senha from tb_usuario where email_usuario = ?", String.class, email);
	}

	@Test
	public void cadastrarEFazerLoginLegado_comCredenciaisCorretas_devolveOUsuarioETokenSemSenha() throws Exception {
		String email = unico("login") + "@teste.com";
		int id = criarUsuario(email, "Segredo123");

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(credenciais(email, "Segredo123")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.senha").doesNotExist());
	}

	/**
	 * Inversao do teste de caracterizacao do collation: antes, getByEmailAndSenha comparava a senha no MySQL
	 * (utf8mb4_unicode_ci, case-insensitive) e "SEGREDO123" autenticava quem tinha "Segredo123". Agora a senha e
	 * verificada pelo PasswordEncoder (BCrypt, sensivel a caixa): nao autentica nem no legado nem no v1.
	 */
	@Test
	public void login_comSenhaDeCaixaDiferente_naoAutentica() throws Exception {
		String email = unico("login") + "@teste.com";
		criarUsuario(email, "Segredo123");

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(credenciais(email, "SEGREDO123")))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credenciais(email, "SEGREDO123")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void cadastro_guardaBcryptNaColuna_eFluxoV1LoginTokenChamadaProtegidaRefreshLogout() throws Exception {
		String email = unico("v1") + "@teste.com";
		criarUsuario(email, "Segredo123");
		assertTrue(senhaNoBanco(email).startsWith("{bcrypt}$2a$"), "senha deve ser guardada como BCrypt");

		String corpo = postar("/api/v1/auth/login", credenciais(email, "Segredo123"));
		String access = JsonPath.read(corpo, "$.accessToken");
		String refresh = JsonPath.read(corpo, "$.refreshToken");

		mvc.perform(get("/api/v1/teste/quem-sou-eu")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());

		// rotacao no MySQL real (lock pessimista, Instant em datetime(6)) e logout
		String rotacionado = postar("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}");
		String novo = JsonPath.read(rotacionado, "$.refreshToken");
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + novo + "\"}")).andExpect(status().isNoContent());
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + novo + "\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	public void usuarioLegadoComSenhaEmTextoPuro_fazLogin_eAColunaPassaAterHashBcrypt() throws Exception {
		String email = unico("legado") + "@teste.com";
		jdbc.update("insert into tb_usuario (adm, celular_usuario, data_nascimento, email_usuario, endereco_usuario,"
				+ " genero_usuario, nome_usuario, rg_usuario, senha, tipo_usuario) values (b'0', '1', '1990-01-01', ?, 'R', 'F', 'L', '1', 'Segredo123', 4)",
				email);
		assertEquals("Segredo123", senhaNoBanco(email));

		postar("/api/v1/auth/login", credenciais(email, "Segredo123"));

		assertTrue(senhaNoBanco(email).startsWith("{bcrypt}$2a$"), "rehash no primeiro login bem-sucedido");
		postar("/api/v1/auth/login", credenciais(email, "Segredo123")); // continua autenticando com o hash
	}
}
