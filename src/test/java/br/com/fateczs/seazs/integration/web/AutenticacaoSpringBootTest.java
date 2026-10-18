package br.com.fateczs.seazs.integration.web;

import br.com.fateczs.seazs.model.RefreshToken;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.utils.TipoUsuario;

/**
 * Wiring real (contexto completo, H2): DaoAuthenticationProvider + DelegatingPasswordEncoder + JPA + filtros.
 * Cobre a integracao dos componentes; as regras finas ficam nos testes unitarios. O collation case-insensitive do
 * MySQL so e exercitado no featureTest (o H2 e case-sensitive).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestPropertySource(properties = "app.jwt.secret=" + ComSeguranca.SEGREDO_TESTE)
public class AutenticacaoSpringBootTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private RefreshTokenRepository refreshTokens;
	@Autowired
	private br.com.fateczs.seazs.security.JwtTokenService tokens;

	private static String json(String email, String senha) {
		return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
	}

	private String postarComo(Usuario quem, String caminho, String corpo, int status) throws Exception {
		return mvc.perform(post(caminho).header("Authorization", "Bearer " + tokens.emitir(quem).valor())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
	}

	private Usuario salvarLegado(String senhaEmTextoPuro) {
		Usuario u = DadosTeste.usuario("legado-" + UUID.randomUUID() + "@teste.com");
		u.setSenha(senhaEmTextoPuro);
		return usuarios.save(u);
	}

	private String postar(String caminho, String corpo, int status) throws Exception {
		return mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
	}

	@Test
	public void loginDeUsuarioLegadoComSenhaEmTextoPuro_autenticaERehashParaBcrypt() throws Exception {
		Usuario u = salvarLegado("Segredo123");

		String corpo = postar("/api/v1/auth/login", json(u.getEmail(), "Segredo123"), 200);

		assertTrue(usuarios.findById(u.getId()).get().getSenha().startsWith("{bcrypt}$2a$"));
		// segue autenticando com o hash novo
		postar("/api/v1/auth/login", json(u.getEmail(), "Segredo123"), 200);
		assertEquals("Bearer", JsonPath.read(corpo, "$.tokenType"));
	}

	@Test
	public void senhaDeCaixaDiferente_naoAutentica_nemNoLegadoNemNoHash() throws Exception {
		Usuario u = salvarLegado("Segredo123");

		postar("/api/v1/auth/login", json(u.getEmail(), "SEGREDO123"), 401);
		postar("/api/v1/auth/login", json(u.getEmail(), "Segredo123"), 200); // agora hasheada
		postar("/api/v1/auth/login", json(u.getEmail(), "SEGREDO123"), 401);
	}

	@Test
	public void emailInexistenteESenhaErrada_respondemIgual() throws Exception {
		Usuario u = salvarLegado("Segredo123");

		String senhaErrada = postar("/api/v1/auth/login", json(u.getEmail(), "errada"), 401);
		String semUsuario = postar("/api/v1/auth/login", json("nao-existe@teste.com", "errada"), 401);

		assertEquals(senhaErrada, semUsuario);
		assertTrue(semUsuario.contains("Credenciais inválidas"));
	}

	@Test
	public void senhaErradaNaoAlteraOHashLegado() throws Exception {
		Usuario u = salvarLegado("Segredo123");

		postar("/api/v1/auth/login", json(u.getEmail(), "errada"), 401);

		assertEquals("Segredo123", usuarios.findById(u.getId()).get().getSenha());
	}

	@Test
	public void cadastroGuardaBcryptELoginFunciona() throws Exception {
		String email = "novo-" + UUID.randomUUID() + "@teste.com";
		postar("/usuario/inserir", "{\"email\":\"" + email + "\",\"senha\":\"Segredo123\",\"nome\":\"N\",\"endereco\":\"R\","
				+ "\"celular\":\"1\",\"dataNasc\":631152000000,\"adm\":false,\"rg\":\"1\",\"genero\":\"F\",\"tipoUsuario\":\"ALUNO\"}", 200);

		assertTrue(usuarios.findByEmail(email).get().getSenha().startsWith("{bcrypt}$2a$"));
		postar("/api/v1/auth/login", json(email, "Segredo123"), 200);
	}

	@Test
	public void cadastroComSenhaAcimaDe72Bytes_responde400() throws Exception {
		postar("/usuario/inserir", "{\"email\":\"longa@teste.com\",\"senha\":\"" + "a".repeat(73) + "\"}", 400);
	}

	@Test
	public void loginLegado_validaViaEncoder_devolveTokenSemSenha() throws Exception {
		Usuario u = salvarLegado("Segredo123");

		String corpo = postar("/usuario/login", json(u.getEmail(), "Segredo123"), 200);

		assertEquals(u.getId().intValue(), (int) JsonPath.read(corpo, "$.id"));
		assertNotEquals("", JsonPath.read(corpo, "$.token"));
		assertTrue(!corpo.contains("senha") && !corpo.contains("Segredo123"));
		assertEquals("", postar("/usuario/login", json(u.getEmail(), "SEGREDO123"), 200)); // contrato legado: 200 vazio
	}

	/** Regressao: e-mail inexistente no legado dava 500 (UnexpectedRollbackException por transacao externa rollback-only). */
	@Test
	public void loginLegadoComEmailInexistente_responde200ComCorpoVazio() throws Exception {
		assertEquals("", postar("/usuario/login", json("nao-existe@teste.com", "qualquer"), 200));
	}

	@Test
	public void loginLegado_emailInexistenteIndistinguivelDeSenhaErrada() throws Exception {
		Usuario u = salvarLegado("Segredo123");

		String senhaErrada = postar("/usuario/login", json(u.getEmail(), "errada"), 200);
		String semUsuario = postar("/usuario/login", json("nao-existe@teste.com", "errada"), 200);

		assertEquals(senhaErrada, semUsuario);
	}

	@Test
	public void senhaVaziaOuEmBranco_recusadaNoLoginV1EnoLegado() throws Exception {
		Usuario u = salvarLegado("Segredo123");
		for (String senha : new String[] { "", "   " }) {
			postar("/api/v1/auth/login", json(u.getEmail(), senha), 400);
			assertEquals("", postar("/usuario/login", json(u.getEmail(), senha), 200));
		}
	}

	@Test
	public void cadastroComSenhaVaziaOuEmBranco_responde400() throws Exception {
		for (String senha : new String[] { "", "   " }) {
			postar("/usuario/inserir", "{\"email\":\"branca@teste.com\",\"senha\":\"" + senha + "\"}", 400);
		}
	}

	@Test
	public void cadastroPublico_ignoraAdmETipoUsuarioDoCliente() throws Exception {
		String email = "escalada-" + UUID.randomUUID() + "@teste.com";
		postar("/usuario/inserir", "{\"email\":\"" + email + "\",\"senha\":\"Segredo123\",\"nome\":\"N\",\"endereco\":\"R\","
				+ "\"celular\":\"1\",\"dataNasc\":631152000000,\"adm\":true,\"rg\":\"1\",\"genero\":\"F\",\"tipoUsuario\":\"PROFESSOR\"}", 200);

		Usuario criado = usuarios.findByEmail(email).get();
		assertFalse(criado.getAdm());
		assertEquals(TipoUsuario.VISITANTE, criado.getTipoUsuario());
		String corpo = postar("/api/v1/auth/login", json(email, "Segredo123"), 200);
		assertEquals(java.util.Arrays.asList("ROLE_VISITANTE"), JsonPath.read(corpo, "$.usuario.authorities"));
	}

	@Test
	public void alterar_preservaAdmETipoUsuarioPersistidos() throws Exception {
		Usuario u = salvarLegado("Segredo123");
		u.setAdm(true);
		u.setTipoUsuario(TipoUsuario.FUNCIONARIO);
		usuarios.save(u);

		postarComo(u, "/usuario/alterar", "{\"id\":" + u.getId() + ",\"email\":\"" + u.getEmail() + "\",\"senha\":\"Nova12345\",\"nome\":\"N\","
				+ "\"endereco\":\"R\",\"celular\":\"1\",\"dataNasc\":631152000000,\"adm\":false,\"rg\":\"1\",\"genero\":\"F\","
				+ "\"tipoUsuario\":\"ALUNO\"}", 200);

		Usuario depois = usuarios.findById(u.getId()).get();
		assertTrue(depois.getAdm());
		assertEquals(TipoUsuario.FUNCIONARIO, depois.getTipoUsuario());
	}

	@Test
	public void fluxoCompleto_loginTokenChamadaProtegida() throws Exception {
		Usuario u = salvarLegado("Segredo123");
		String corpo = postar("/api/v1/auth/login", json(u.getEmail(), "Segredo123"), 200);
		String access = JsonPath.read(corpo, "$.accessToken");

		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/teste/quem-sou-eu")).andExpect(status().isUnauthorized());
	}

	@Test
	public void refreshRotaciona_reusoRevogaAFamilia_logoutRevoga() throws Exception {
		Usuario u = salvarLegado("Segredo123");
		String login = postar("/api/v1/auth/login", json(u.getEmail(), "Segredo123"), 200);
		String r1 = JsonPath.read(login, "$.refreshToken");

		String rotacionado = postar("/api/v1/auth/refresh", "{\"refreshToken\":\"" + r1 + "\"}", 200);
		String r2 = JsonPath.read(rotacionado, "$.refreshToken");
		assertNotEquals(r1, r2);

		// o token em texto puro nunca fica no banco
		assertTrue(refreshTokens.findAll().stream().noneMatch(t -> t.getTokenHash().equals(r1) || t.getTokenHash().equals(r2)));

		// reuso do r1 (ja rotacionado): 401 e derruba tambem o r2
		postar("/api/v1/auth/refresh", "{\"refreshToken\":\"" + r1 + "\"}", 401);
		postar("/api/v1/auth/refresh", "{\"refreshToken\":\"" + r2 + "\"}", 401);

		// logout invalida a sessao
		String outro = postar("/api/v1/auth/login", json(u.getEmail(), "Segredo123"), 200);
		String r3 = JsonPath.read(outro, "$.refreshToken");
		postar("/api/v1/auth/logout", "{\"refreshToken\":\"" + r3 + "\"}", 204);
		postar("/api/v1/auth/refresh", "{\"refreshToken\":\"" + r3 + "\"}", 401);
	}

	@Test
	public void refreshTokenDesconhecido_responde401() throws Exception {
		postar("/api/v1/auth/refresh", "{\"refreshToken\":\"desconhecido\"}", 401);
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros").isArray());
	}

	@Test
	public void limpezaRemoveSoOsExpiradosAlemDaCarencia() {
		Usuario u = salvarLegado("Segredo123");
		Instant agora = Instant.parse("2026-10-17T03:30:00Z");
		var antigo = refreshTokens.save(new RefreshToken("h-antigo-" + UUID.randomUUID(), "f1", u.getId(), agora.minusSeconds(900000), agora.minusSeconds(800000)));
		var recemExpirado = refreshTokens.save(new RefreshToken("h-recente-" + UUID.randomUUID(), "f2", u.getId(), agora.minusSeconds(90000), agora.minusSeconds(3600)));
		var valido = refreshTokens.save(new RefreshToken("h-valido-" + UUID.randomUUID(), "f3", u.getId(), agora, agora.plusSeconds(3600)));

		int removidos = refreshTokens.deleteByExpiraEmBefore(agora.minusSeconds(7 * 24 * 3600L));

		assertEquals(1, removidos);
		assertTrue(refreshTokens.findById(antigo.getId()).isEmpty());
		assertTrue(refreshTokens.findById(recemExpirado.getId()).isPresent());
		assertTrue(refreshTokens.findById(valido.getId()).isPresent());
	}

	@Test
	public void cadastroComEmailDuplicado_responde409Generico() throws Exception {
		String email = "dup-" + UUID.randomUUID() + "@teste.com";
		String corpo = "{\"email\":\"" + email + "\",\"senha\":\"Segredo123\",\"nome\":\"N\",\"endereco\":\"R\","
				+ "\"celular\":\"1\",\"dataNasc\":631152000000,\"adm\":false,\"rg\":\"1\",\"genero\":\"F\"}";
		postar("/usuario/inserir", corpo, 200);

		String resposta = postar("/usuario/inserir", corpo, 409);

		assertTrue(resposta.contains("Não foi possível concluir o cadastro"));
		assertTrue(!resposta.contains(email));
	}

	/** Regressao: campo NOT NULL ausente virava 409 "e-mail ja cadastrado" (catch largo demais); agora e 400 antes do banco. */
	@Test
	public void cadastroSemCamposObrigatorios_responde400EnaoConflito() throws Exception {
		String email = "incompleto-" + UUID.randomUUID() + "@teste.com";

		String resposta = postar("/usuario/inserir", "{\"email\":\"" + email + "\",\"senha\":\"Segredo123\"}", 400);

		assertFalse(resposta.contains("cadastro"));
		assertTrue(usuarios.findByEmail(email).isEmpty());
	}
}
