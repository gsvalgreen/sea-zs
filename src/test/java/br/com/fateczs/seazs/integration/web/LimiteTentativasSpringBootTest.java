package br.com.fateczs.seazs.integration.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.support.RelogioMutavel;

/**
 * Rate limit por IP e bloqueio por e-mail no contexto real (H2). O relogio e controlado pelo teste (sem sleep, sem
 * flakiness). Limite de IP baixo (4 por minuto); o bloqueio usa o padrao (5 falhas, 15 min).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Import(LimiteTentativasSpringBootTest.RelogioTeste.class)
@TestPropertySource(properties = { "app.jwt.secret=" + ComSeguranca.SEGREDO_TESTE,
		"app.security.rate-limit.max-requisicoes=4" })
public class LimiteTentativasSpringBootTest {

	@TestConfiguration
	static class RelogioTeste {
		@Bean
		@Primary
		public RelogioMutavel relogioMutavel() {
			return new RelogioMutavel(Instant.now());
		}
	}

	@Autowired
	private MockMvc mvc;
	@Autowired
	private RelogioMutavel relogio;
	@Autowired
	private UsuarioRepository usuarios;

	private static final AtomicInteger SEQUENCIA_IP = new AtomicInteger();

	/** IP IPv4 distinto a cada chamada (contador, sem colisao entre testes). */
	private static String ipUnico() {
		int n = SEQUENCIA_IP.incrementAndGet();
		return "10." + ((n >> 16) & 255) + "." + ((n >> 8) & 255) + "." + (n & 255);
	}

	private MockHttpServletResponse login(String caminho, String ip, String email, String senha) throws Exception {
		return mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				})).andReturn().getResponse();
	}

	private Usuario usuario(String senha) {
		Usuario u = DadosTeste.usuario("lim-" + UUID.randomUUID() + "@teste.com");
		u.setSenha(senha); // texto puro legado: migra no primeiro login
		return usuarios.save(u);
	}

	@Test
	public void ipExcedeOLimite_recebe429ComRetryAfter_eReabreDepoisDaJanela() throws Exception {
		String ip = ipUnico();
		for (int i = 0; i < 4; i++) {
			assertEquals(401, login("/api/v1/auth/login", ip, "x" + i + "@nao.com", "errada").getStatus());
		}

		MockHttpServletResponse bloqueada = login("/api/v1/auth/login", ip, "x9@nao.com", "errada");

		assertEquals(429, bloqueada.getStatus());
		assertEquals("60", bloqueada.getHeader("Retry-After"));
		assertTrue(bloqueada.getContentType().startsWith("application/problem+json"));
		// outro IP nao e afetado
		assertEquals(401, login("/api/v1/auth/login", ipUnico(), "x@nao.com", "errada").getStatus());

		relogio.avancar(Duration.ofMinutes(1));
		assertEquals(401, login("/api/v1/auth/login", ip, "x9@nao.com", "errada").getStatus());
	}

	@Test
	public void loginV1ELegadoCompartilhamOLimitePorIp_eRefreshECadastroSaoLimitados() throws Exception {
		String ip = ipUnico();
		for (int i = 0; i < 2; i++) {
			login("/api/v1/auth/login", ip, "a@nao.com", "x");
			login("/usuario/login", ip, "a@nao.com", "x");
		}
		assertEquals(429, login("/usuario/login", ip, "a@nao.com", "x").getStatus());

		String ipRefresh = ipUnico();
		for (int i = 0; i < 4; i++) {
			assertEquals(401, mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
					.content("{\"refreshToken\":\"x\"}").with(r -> {
						r.setRemoteAddr(ipRefresh);
						return r;
					})).andReturn().getResponse().getStatus());
		}
		assertEquals(429, mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"x\"}").with(r -> {
					r.setRemoteAddr(ipRefresh);
					return r;
				})).andReturn().getResponse().getStatus());

		String ipCadastro = ipUnico();
		for (int i = 0; i < 4; i++) {
			mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON).content("{}").with(r -> {
				r.setRemoteAddr(ipCadastro);
				return r;
			}));
		}
		assertEquals(429, mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON).content("{}")
				.with(r -> {
					r.setRemoteAddr(ipCadastro);
					return r;
				})).andReturn().getResponse().getStatus());
	}

	@Test
	public void xForwardedForNaoBurlaOLimite() throws Exception {
		String ip = ipUnico();
		for (int i = 0; i < 4; i++) {
			mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"a@nao.com\",\"senha\":\"x\"}").header("X-Forwarded-For", "9.9.9." + i)
					.with(r -> {
						r.setRemoteAddr(ip);
						return r;
					}));
		}
		assertEquals(429, mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"a@nao.com\",\"senha\":\"x\"}").header("X-Forwarded-For", "8.8.8.8")
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				})).andReturn().getResponse().getStatus());
	}

	@Test
	public void cincoFalhasBloqueiamOEmail_mesmoComSenhaCorreta_eExpiraComOTempo() throws Exception {
		Usuario u = usuario("Segredo123");
		for (int i = 0; i < 5; i++) {
			assertEquals(401, login("/api/v1/auth/login", ipUnico(), u.getEmail(), "errada").getStatus());
		}

		MockHttpServletResponse bloqueado = login("/api/v1/auth/login", ipUnico(), u.getEmail(), "Segredo123");

		assertEquals(429, bloqueado.getStatus());
		assertEquals("900", bloqueado.getHeader("Retry-After"));
		// legado tambem respeita o bloqueio (429 em vez do 200 vazio)
		assertEquals(429, login("/usuario/login", ipUnico(), u.getEmail(), "Segredo123").getStatus());
		// e e-mail com outra caixa/espacos e o mesmo bloqueio
		assertEquals(429, login("/api/v1/auth/login", ipUnico(), u.getEmail().toUpperCase(), "Segredo123").getStatus());

		relogio.avancar(Duration.ofMinutes(15));
		assertEquals(200, login("/api/v1/auth/login", ipUnico(), u.getEmail(), "Segredo123").getStatus());
	}

	@Test
	public void emailInexistenteTambemBloqueia_eRespostaIgualAExistente() throws Exception {
		Usuario existente = usuario("Segredo123");
		String fantasma = "fantasma-" + UUID.randomUUID() + "@nao.com";
		for (int i = 0; i < 5; i++) {
			login("/api/v1/auth/login", ipUnico(), existente.getEmail(), "errada");
			login("/api/v1/auth/login", ipUnico(), fantasma, "errada");
		}

		MockHttpServletResponse a = login("/api/v1/auth/login", ipUnico(), existente.getEmail(), "errada");
		MockHttpServletResponse b = login("/api/v1/auth/login", ipUnico(), fantasma, "errada");

		assertEquals(429, a.getStatus());
		assertEquals(429, b.getStatus());
		assertEquals(a.getContentAsString(), b.getContentAsString());
		assertEquals(a.getHeader("Retry-After"), b.getHeader("Retry-After"));
	}

	@Test
	public void sucessoZeraOContadorDeFalhas() throws Exception {
		Usuario u = usuario("Segredo123");
		for (int rodada = 0; rodada < 3; rodada++) {
			for (int i = 0; i < 4; i++) {
				assertEquals(401, login("/api/v1/auth/login", ipUnico(), u.getEmail(), "errada").getStatus());
			}
			assertEquals(200, login("/api/v1/auth/login", ipUnico(), u.getEmail(), "Segredo123").getStatus());
		}
		assertFalse(login("/api/v1/auth/login", ipUnico(), u.getEmail(), "errada").getStatus() == 429);
	}

	private MockHttpServletResponse loginUri(String uri, String ip) throws Exception {
		return mvc.perform(post(java.net.URI.create(uri)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"a@nao.com\",\"senha\":\"x\"}").with(r -> {
					r.setRemoteAddr(ip);
					return r;
				})).andReturn().getResponse();
	}

	@Test
	public void caminhoComByteCodificadoNaoBurlaOLimite() throws Exception {
		String ip = ipUnico();
		for (int i = 0; i < 2; i++) {
			loginUri("/api/v1/auth/%6cogin", ip);
			loginUri("/usuario/%6cogin", ip);
		}

		assertEquals(429, loginUri("/api/v1/auth/login", ip).getStatus());
		assertEquals(429, loginUri("/usuario/%6cogin", ip).getStatus());
		assertEquals(429, loginUri("/api/v1/auth/%6cogin", ip).getStatus());
	}

	@Test
	public void ipv6DoMesmoPrefixo64CompartilhaOLimite() throws Exception {
		String base = "2001:db8:" + Integer.toHexString(SEQUENCIA_IP.incrementAndGet()) + ":1:";
		for (int i = 0; i < 4; i++) {
			assertEquals(401, login("/api/v1/auth/login", base + ":" + (i + 1), "x" + i + "@nao.com", "x").getStatus());
		}

		assertEquals(429, login("/api/v1/auth/login", base + ":ffff", "x9@nao.com", "x").getStatus());
	}

	@Test
	public void variantesDoEmailPelaCollationDoBancoCompartilhamOBloqueio() throws Exception {
		String base = "acento-" + UUID.randomUUID().toString().substring(0, 8);
		for (int i = 0; i < 5; i++) {
			login("/api/v1/auth/login", ipUnico(), base + "@nao.com", "errada");
		}

		assertEquals(429, login("/api/v1/auth/login", ipUnico(), base.toUpperCase() + "\u200b@nao.com", "errada").getStatus());
	}
}
