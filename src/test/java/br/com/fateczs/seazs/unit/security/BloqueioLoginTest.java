package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.security.BloqueioLogin;
import br.com.fateczs.seazs.security.LimiteExcedidoException;
import br.com.fateczs.seazs.security.SegurancaProperties;
import br.com.fateczs.seazs.support.RelogioMutavel;

public class BloqueioLoginTest {

	private RelogioMutavel relogio;
	private BloqueioLogin bloqueio;

	private static SegurancaProperties props(int maxFalhas, int maxEntradas) {
		return new SegurancaProperties(new SegurancaProperties.RateLimit(10, Duration.ofMinutes(1)),
				new SegurancaProperties.Lockout(maxFalhas, Duration.ofMinutes(15), Duration.ofMinutes(15),
						Duration.ofHours(1), Duration.ofHours(24)),
				maxEntradas);
	}

	@BeforeEach
	public void montar() {
		relogio = new RelogioMutavel(Instant.parse("2026-10-17T12:00:00Z"));
		bloqueio = new BloqueioLogin(relogio, props(5, 100));
	}

	private void falhar(String email, int vezes) {
		for (int i = 0; i < vezes; i++) {
			bloqueio.registrarFalha(email);
		}
	}

	@Test
	public void abaixoDoLimiteDeFalhasNaoBloqueia() {
		falhar("ana@teste.com", 4);

		assertFalse(bloqueio.restante("ana@teste.com").isPresent());
		bloqueio.exigirLiberado("ana@teste.com");
	}

	@Test
	public void naQuintaFalhaBloqueiaPelaDuracaoBase() {
		falhar("ana@teste.com", 5);

		assertEquals(Duration.ofMinutes(15), bloqueio.restante("ana@teste.com").get());
		LimiteExcedidoException ex = assertThrows(LimiteExcedidoException.class, () -> bloqueio.exigirLiberado("ana@teste.com"));
		assertEquals(900, ex.segundos());
	}

	@Test
	public void bloqueioExpiraComOTempo() {
		falhar("ana@teste.com", 5);

		relogio.avancar(Duration.ofMinutes(14).plusSeconds(59));
		assertTrue(bloqueio.restante("ana@teste.com").isPresent());
		relogio.avancar(Duration.ofSeconds(1));
		assertFalse(bloqueio.restante("ana@teste.com").isPresent());
	}

	@Test
	public void emailEhNormalizadoPorCaixaEEspacos() {
		falhar("Ana@Teste.com", 3);
		falhar("  ana@teste.COM ", 2);

		assertTrue(bloqueio.restante("ANA@TESTE.COM").isPresent());
	}

	@Test
	public void emailInexistenteBloqueiaIgualAExistente() {
		// O bloqueio so conhece o texto digitado: nao ha como diferenciar existente de inexistente.
		falhar("fantasma@nao-existe.com", 5);

		assertTrue(bloqueio.restante("fantasma@nao-existe.com").isPresent());
		assertFalse(bloqueio.restante("outro@nao-existe.com").isPresent());
	}

	@Test
	public void sucessoZeraOContadorDeFalhas() {
		falhar("ana@teste.com", 4);
		bloqueio.registrarSucesso("ana@teste.com");
		falhar("ana@teste.com", 4);

		assertFalse(bloqueio.restante("ana@teste.com").isPresent());
	}

	@Test
	public void falhasEspacadasAlemDaJanelaRecomecamAContagem() {
		falhar("ana@teste.com", 4);
		relogio.avancar(Duration.ofMinutes(16));
		falhar("ana@teste.com", 4);

		assertFalse(bloqueio.restante("ana@teste.com").isPresent());
	}

	@Test
	public void bloqueiosSeguidosDobramADuracaoAteOTeto() {
		falhar("ana@teste.com", 5);
		assertEquals(Duration.ofMinutes(15), bloqueio.restante("ana@teste.com").get());

		relogio.avancar(Duration.ofMinutes(15));
		falhar("ana@teste.com", 5);
		assertEquals(Duration.ofMinutes(30), bloqueio.restante("ana@teste.com").get());

		relogio.avancar(Duration.ofMinutes(30));
		falhar("ana@teste.com", 5);
		assertEquals(Duration.ofHours(1), bloqueio.restante("ana@teste.com").get());

		relogio.avancar(Duration.ofHours(1));
		falhar("ana@teste.com", 5);
		assertEquals(Duration.ofHours(1), bloqueio.restante("ana@teste.com").get()); // teto
	}

	@Test
	public void falhasDuranteOBloqueioNaoEstendemOPrazo() {
		falhar("ana@teste.com", 5);
		relogio.avancar(Duration.ofMinutes(5));
		falhar("ana@teste.com", 20);

		assertEquals(Duration.ofMinutes(10), bloqueio.restante("ana@teste.com").get());
	}

	@Test
	public void sucessoDepoisDoBloqueioReiniciaOsNiveis() {
		falhar("ana@teste.com", 5);
		relogio.avancar(Duration.ofMinutes(15));
		bloqueio.registrarSucesso("ana@teste.com");
		falhar("ana@teste.com", 5);

		assertEquals(Duration.ofMinutes(15), bloqueio.restante("ana@teste.com").get());
	}

	@Test
	public void historicoAntigoEEsquecidoNaVarredura() {
		falhar("ana@teste.com", 2);
		relogio.avancar(Duration.ofHours(25));
		falhar("outra@teste.com", 1); // dispara a varredura

		assertEquals(1, bloqueio.tamanho());
	}

	@Test
	public void comMapaCheioEmailsNovosNaoSaoRegistrados() {
		BloqueioLogin pequeno = new BloqueioLogin(relogio, props(1, 2));
		pequeno.registrarFalha("a@x.com");
		pequeno.registrarFalha("b@x.com");
		pequeno.registrarFalha("c@x.com"); // teto: nao registra

		assertEquals(2, pequeno.tamanho());
		assertFalse(pequeno.restante("c@x.com").isPresent());
		assertTrue(pequeno.restante("a@x.com").isPresent());
	}

	@Test
	public void emailNuloOuEnormeNaoQuebra() {
		bloqueio.registrarFalha(null);
		bloqueio.registrarFalha("a".repeat(5000));

		assertFalse(bloqueio.restante(null).isPresent());
		assertEquals(2, bloqueio.tamanho());
	}

	@Test
	public void configuracaoInvalidaFalhaNaPartida() {
		assertThrows(IllegalStateException.class, () -> new BloqueioLogin(relogio, props(0, 10)));
	}

	@Test
	public void segundosArredondaParaCimaComMinimoDeUm() {
		assertEquals(1, new LimiteExcedidoException(Duration.ofMillis(1)).segundos());
		assertEquals(2, new LimiteExcedidoException(Duration.ofMillis(1001)).segundos());
		assertEquals(1, new LimiteExcedidoException(Duration.ZERO).segundos());
	}
}
