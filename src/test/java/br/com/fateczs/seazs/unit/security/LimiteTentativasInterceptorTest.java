package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import br.com.fateczs.seazs.security.LimiteExcedidoException;
import br.com.fateczs.seazs.security.LimiteTentativasInterceptor;
import br.com.fateczs.seazs.security.LimitadorPorIp;
import br.com.fateczs.seazs.security.SegurancaProperties;
import br.com.fateczs.seazs.support.RelogioMutavel;

public class LimiteTentativasInterceptorTest {

	private LimiteTentativasInterceptor interceptor;

	@BeforeEach
	public void montar() {
		SegurancaProperties props = new SegurancaProperties(new SegurancaProperties.RateLimit(2, Duration.ofMinutes(1)),
				new SegurancaProperties.Lockout(5, Duration.ofMinutes(15), Duration.ofMinutes(15), Duration.ofHours(1),
						Duration.ofHours(24)),
				100);
		interceptor = new LimiteTentativasInterceptor(
				new LimitadorPorIp(new RelogioMutavel(Instant.parse("2026-10-17T12:00:00Z")), props));
	}

	private static MockHttpServletRequest requisicao(String metodo, String caminho, String ip) {
		MockHttpServletRequest r = new MockHttpServletRequest(metodo, caminho);
		r.setRemoteAddr(ip);
		return r;
	}

	private boolean chamar(MockHttpServletRequest r) {
		return interceptor.preHandle(r, new MockHttpServletResponse(), new Object());
	}

	@Test
	public void terceiraRequisicaoDoMesmoIpNoLoginLanca429() {
		assertTrue(chamar(requisicao("POST", "/api/v1/auth/login", "10.0.0.1")));
		assertTrue(chamar(requisicao("POST", "/api/v1/auth/login", "10.0.0.1")));

		LimiteExcedidoException ex = assertThrows(LimiteExcedidoException.class,
				() -> chamar(requisicao("POST", "/api/v1/auth/login", "10.0.0.1")));
		assertEquals(60, ex.segundos());
	}

	@Test
	public void loginV1ELegadoCompartilhamOMesmoContador() {
		chamar(requisicao("POST", "/api/v1/auth/login", "10.0.0.1"));
		chamar(requisicao("POST", "/usuario/login", "10.0.0.1"));

		assertThrows(LimiteExcedidoException.class, () -> chamar(requisicao("POST", "/usuario/login", "10.0.0.1")));
	}

	@Test
	public void refreshECadastroTemContadoresProprios() {
		chamar(requisicao("POST", "/api/v1/auth/login", "10.0.0.1"));
		chamar(requisicao("POST", "/api/v1/auth/login", "10.0.0.1"));

		assertTrue(chamar(requisicao("POST", "/api/v1/auth/refresh", "10.0.0.1")));
		assertTrue(chamar(requisicao("POST", "/usuario/inserir", "10.0.0.1")));
	}

	@Test
	public void ipsDiferentesNaoSeAfetam() {
		chamar(requisicao("POST", "/usuario/inserir", "10.0.0.1"));
		chamar(requisicao("POST", "/usuario/inserir", "10.0.0.1"));

		assertTrue(chamar(requisicao("POST", "/usuario/inserir", "10.0.0.2")));
	}

	@Test
	public void headerXForwardedForEIgnorado() {
		for (int i = 0; i < 2; i++) {
			MockHttpServletRequest r = requisicao("POST", "/usuario/inserir", "10.0.0.1");
			r.addHeader("X-Forwarded-For", "9.9.9." + i);
			chamar(r);
		}
		MockHttpServletRequest terceira = requisicao("POST", "/usuario/inserir", "10.0.0.1");
		terceira.addHeader("X-Forwarded-For", "9.9.9.200");

		assertThrows(LimiteExcedidoException.class, () -> chamar(terceira));
	}

	@Test
	public void outrosCaminhosEMetodosNaoSaoLimitados() {
		for (int i = 0; i < 10; i++) {
			assertTrue(chamar(requisicao("POST", "/evento/listar", "10.0.0.1")));
			assertTrue(chamar(requisicao("GET", "/usuario/login", "10.0.0.1")));
		}
	}

	@Test
	public void caminhoComByteCodificadoNaoBurlaOLimite() {
		chamar(requisicao("POST", "/usuario/login", "10.0.0.1"));
		chamar(requisicao("POST", "/usuario/%6cogin", "10.0.0.1"));

		assertThrows(LimiteExcedidoException.class, () -> chamar(requisicao("POST", "/api/v1/auth/%6cogin", "10.0.0.1")));
	}

	@Test
	public void parametroDeCaminhoEBarrasDuplicadasNaoBurlamOLimite() {
		chamar(requisicao("POST", "/usuario/inserir;x=1", "10.0.0.1"));
		chamar(requisicao("POST", "//usuario//inserir", "10.0.0.1"));

		assertThrows(LimiteExcedidoException.class, () -> chamar(requisicao("POST", "/usuario/inserir", "10.0.0.1")));
	}
}
