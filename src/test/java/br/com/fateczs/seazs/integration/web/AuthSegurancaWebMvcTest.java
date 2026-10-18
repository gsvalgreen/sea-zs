package br.com.fateczs.seazs.integration.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fateczs.seazs.controller.AuthController;
import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.ProtegidoTesteController;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.dto.LoginRequest;
import br.com.fateczs.seazs.model.dto.TokenResponse;
import br.com.fateczs.seazs.model.dto.UsuarioResumoResponse;
import br.com.fateczs.seazs.security.JwtConfig;
import br.com.fateczs.seazs.security.JwtProperties;
import br.com.fateczs.seazs.security.JwtTokenService;
import br.com.fateczs.seazs.service.AuthService;
import br.com.fateczs.seazs.utils.TipoUsuario;

/**
 * Cadeia de seguranca real (filtros, JWT HS256, CORS, ProblemDetail) com o AuthService mockado: contrato HTTP de
 * /api/v1/auth e exigencia de token em /api/v1/**. As regras de negocio do login ficam nos testes unitarios.
 */
@WebMvcTest({ AuthController.class, ProtegidoTesteController.class })
@ComSeguranca
public class AuthSegurancaWebMvcTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JwtEncoder encoder;
	@Autowired
	private JwtProperties props;
	@MockitoBean
	private AuthService authService;

	private static final String LOGIN = "{\"email\":\"ana@teste.com\",\"senha\":\"Segredo123\"}";

	private String tokenEmitidoEm(Instant instante) {
		Usuario u = new Usuario();
		u.setId(7);
		u.setTipoUsuario(TipoUsuario.ALUNO);
		return new JwtTokenService(encoder, props, Clock.fixed(instante, ZoneOffset.UTC)).emitir(u).valor();
	}

	// ---- login

	@Test
	public void loginValido_devolveTokensEResumoSemSenhaNemEmail() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenReturn(new TokenResponse("Bearer", "acc", 900, "ref",
				new UsuarioResumoResponse(7, "Ana", "ALUNO", List.of("ROLE_ALUNO"))));

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").value("acc"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.refreshToken").value("ref"))
				.andExpect(jsonPath("$.usuario.id").value(7))
				.andExpect(jsonPath("$.usuario.authorities[0]").value("ROLE_ALUNO"))
				.andExpect(jsonPath("$..senha").doesNotExist())
				.andExpect(jsonPath("$..email").doesNotExist());
		verify(authService).login(new LoginRequest("ana@teste.com", "Segredo123"));
	}

	@Test
	public void loginInvalido_responde401GenericoEmProblemJson() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("usuario nao existe"));

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Credenciais inválidas"))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("usuario nao existe"))));
	}

	@Test
	public void loginSemCampos_responde400ComListaDeErros() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.erros.length()").value(2));
	}

	@Test
	public void loginComSenhaAcimaDe72Bytes_responde400ComMensagemClara() throws Exception {
		String corpo = "{\"email\":\"a@b.com\",\"senha\":\"" + "a".repeat(73) + "\"}";

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("senha"))
				.andExpect(jsonPath("$.erros[0].mensagem").value(org.hamcrest.Matchers.containsString("72 bytes")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("aaaaaaaa"))));
	}

	@Test
	public void loginComJsonMalformado_responde400() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest());
	}

	// ---- refresh / logout

	@Test
	public void refreshInvalido_responde401() throws Exception {
		when(authService.refresh("velho")).thenReturn(Optional.empty());

		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"velho\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Refresh token inválido ou expirado"));
	}

	@Test
	public void refreshValido_devolveNovoPar() throws Exception {
		when(authService.refresh("ok")).thenReturn(Optional.of(new TokenResponse("Bearer", "acc2", 900, "ref2",
				new UsuarioResumoResponse(7, "Ana", "ALUNO", List.of()))));

		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"ok\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.refreshToken").value("ref2"));
	}

	@Test
	public void refreshSemCorpoValido_responde400() throws Exception {
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void logout_responde204SemAutenticacao() throws Exception {
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"t\"}"))
				.andExpect(status().isNoContent());

		verify(authService).logout("t");
	}

	// ---- /api/v1/** protegido

	@Test
	public void rotaV1_semToken_responde401EmProblemJson() throws Exception {
		mvc.perform(get("/api/v1/teste/quem-sou-eu"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	public void rotaV1_comTokenValido_autenticaPeloSubject() throws Exception {
		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + tokenEmitidoEm(Instant.now())))
				.andExpect(status().isOk())
				.andExpect(content().string("7"));
	}

	@Test
	public void rotaV1_comTokenExpirado_responde401() throws Exception {
		String expirado = tokenEmitidoEm(Instant.now().minus(props.accessTtl()).minus(Duration.ofMinutes(10)));

		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + expirado))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
	}

	@Test
	public void rotaV1_comTokenAdulteradoOuLixo_responde401() throws Exception {
		String token = tokenEmitidoEm(Instant.now());
		String adulterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + adulterado))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer nao.e.jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void rotaV1_comTokenAssinadoComOutroSegredo_responde401() throws Exception {
		JwtProperties outro = new JwtProperties("outro-segredo-diferente-com-mais-de-32-bytes", "seazs",
				Duration.ofMinutes(15), Duration.ofDays(7));
		Usuario u = new Usuario();
		u.setId(7);
		String forjado = new JwtTokenService(new JwtConfig().jwtEncoder(outro), outro, Clock.systemUTC()).emitir(u).valor();

		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + forjado))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void rotaV1_comTokenAlgNone_responde401() throws Exception {
		String b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
				"{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		String corpo = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
				("{\"sub\":\"1\",\"iss\":\"seazs\",\"exp\":" + (Instant.now().getEpochSecond() + 600) + "}")
						.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + b64 + "." + corpo + "."))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void sessaoNuncaECriada() throws Exception {
		mvc.perform(get("/api/v1/teste/quem-sou-eu").header("Authorization", "Bearer " + tokenEmitidoEm(Instant.now())))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	// ---- CORS

	@Test
	public void preflight_deOrigemPermitida_devolveMetodosEHeadersExplicitos() throws Exception {
		mvc.perform(options("/api/v1/auth/login").header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
				.andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.equalToIgnoringCase("authorization, content-type")))
				.andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
	}

	@Test
	public void respostaComOrigemPermitida_expoeRetryAfterSemCuringa() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("x"));

		mvc.perform(post("/api/v1/auth/login").header("Origin", "http://localhost:4200")
				.contentType(MediaType.APPLICATION_JSON).content(LOGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"));
	}

	@Test
	public void preflight_emRotaProtegida_naoExigeToken() throws Exception {
		mvc.perform(options("/api/v1/teste/quem-sou-eu").header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isOk());
	}

	@Test
	public void preflight_deOrigemNaoPermitida_ehRecusado() throws Exception {
		mvc.perform(options("/api/v1/auth/login").header("Origin", "http://malicioso.example")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	public void preflight_comHeaderNaoListado_ehRecusado() throws Exception {
		mvc.perform(options("/api/v1/auth/login").header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "x-custom"))
				.andExpect(status().isForbidden());
	}

	@Test
	public void respostasDeAuthNaoSaoCacheadasNemFarejadas() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenReturn(new TokenResponse("Bearer", "acc", 900, "ref",
				new UsuarioResumoResponse(7, "Ana", "ALUNO", List.of("ROLE_ALUNO"))));

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
		mvc.perform(get("/api/v1/teste/quem-sou-eu"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
	}
}
