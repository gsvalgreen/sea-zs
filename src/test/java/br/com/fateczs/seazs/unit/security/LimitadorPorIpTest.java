package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.security.LimitadorPorIp;
import br.com.fateczs.seazs.security.SegurancaProperties;
import br.com.fateczs.seazs.support.RelogioMutavel;

public class LimitadorPorIpTest {

	private RelogioMutavel relogio;
	private LimitadorPorIp limitador;

	private static SegurancaProperties props(int max, int maxEntradas) {
		return new SegurancaProperties(new SegurancaProperties.RateLimit(max, Duration.ofMinutes(1)),
				new SegurancaProperties.Lockout(5, Duration.ofMinutes(15), Duration.ofMinutes(15), Duration.ofHours(1),
						Duration.ofHours(24)),
				maxEntradas);
	}

	@BeforeEach
	public void montar() {
		relogio = new RelogioMutavel(Instant.parse("2026-10-17T12:00:00Z"));
		limitador = new LimitadorPorIp(relogio, props(3, 100));
	}

	@Test
	public void permiteAteOMaximoERecusaDepoisInformandoAEspera() {
		for (int i = 0; i < 3; i++) {
			assertFalse(limitador.registrar("10.0.0.1", "login").isPresent());
		}
		relogio.avancar(Duration.ofSeconds(20));

		assertEquals(Duration.ofSeconds(40), limitador.registrar("10.0.0.1", "login").get());
	}

	@Test
	public void janelaReabreDepoisDoPrazo() {
		for (int i = 0; i < 4; i++) {
			limitador.registrar("10.0.0.1", "login");
		}
		relogio.avancar(Duration.ofMinutes(1));

		assertFalse(limitador.registrar("10.0.0.1", "login").isPresent());
	}

	@Test
	public void ipsEEscoposSaoIndependentes() {
		for (int i = 0; i < 4; i++) {
			limitador.registrar("10.0.0.1", "login");
		}

		assertFalse(limitador.registrar("10.0.0.2", "login").isPresent());
		assertFalse(limitador.registrar("10.0.0.1", "cadastro").isPresent());
		assertTrue(limitador.registrar("10.0.0.1", "login").isPresent());
	}

	@Test
	public void ipNuloUsaChaveDesconhecidaSemQuebrar() {
		for (int i = 0; i < 3; i++) {
			limitador.registrar(null, "login");
		}
		assertTrue(limitador.registrar(null, "login").isPresent());
	}

	@Test
	public void entradasExpiradasSaoVarridas() {
		limitador.registrar("10.0.0.1", "login");
		limitador.registrar("10.0.0.2", "login");
		relogio.avancar(Duration.ofMinutes(2));
		limitador.registrar("10.0.0.3", "login"); // dispara a varredura

		assertEquals(1, limitador.tamanho());
	}

	@Test
	public void comMapaCheioIpNovoEhRecusadoFalhaFechadaEMemoriaNaoCresce() {
		LimitadorPorIp pequeno = new LimitadorPorIp(relogio, props(1, 2));
		pequeno.registrar("1", "login");
		pequeno.registrar("2", "login");

		assertEquals(Duration.ofMinutes(1), pequeno.registrar("3", "login").get());
		assertEquals(2, pequeno.tamanho());
		assertTrue(pequeno.registrar("1", "login").isPresent()); // existente continua limitado
	}

	@Test
	public void comMapaCheioExpiradasSaoVarridasAntesDeRecusar() {
		LimitadorPorIp pequeno = new LimitadorPorIp(relogio, props(1, 2));
		pequeno.registrar("1", "login");
		pequeno.registrar("2", "login");
		relogio.avancar(Duration.ofSeconds(61));

		assertFalse(pequeno.registrar("3", "login").isPresent());
		assertEquals(1, pequeno.tamanho());
	}

	@Test
	public void ipv6DoMesmoPrefixo64CompartilhaOContador() {
		for (int i = 0; i < 3; i++) {
			limitador.registrar("2001:db8:1:2:aaaa:bbbb:cccc:" + i, "login");
		}

		assertTrue(limitador.registrar("2001:db8:1:2::ffff", "login").isPresent());
		assertFalse(limitador.registrar("2001:db8:1:3::1", "login").isPresent()); // outro /64
		assertEquals(2, limitador.tamanho());
	}

	@Test
	public void ipv4MapeadoEmIpv6UsaOEnderecoIpv4() {
		for (int i = 0; i < 3; i++) {
			limitador.registrar("10.0.0.1", "login");
		}

		assertTrue(limitador.registrar("::ffff:10.0.0.1", "login").isPresent());
	}

	@Test
	public void valorNaoLiteralNaoFazConsultaDeDnsNemQuebra() {
		for (int i = 0; i < 3; i++) {
			limitador.registrar("host:invalido!", "login");
		}

		assertTrue(limitador.registrar("host:invalido!", "login").isPresent());
	}

	@Test
	public void configuracaoInvalidaFalhaNaPartida() {
		assertThrows(IllegalStateException.class, () -> new LimitadorPorIp(relogio, props(0, 10)));
	}
}
