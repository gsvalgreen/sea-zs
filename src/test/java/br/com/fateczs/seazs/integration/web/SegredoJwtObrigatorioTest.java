package br.com.fateczs.seazs.integration.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import br.com.fateczs.seazs.SeazsApplication;

/** A aplicacao nao sobe sem JWT_SECRET valido (nao existe segredo padrao versionado). */
public class SegredoJwtObrigatorioTest {

	private static ConfigurableApplicationContext iniciar(String... extras) {
		String[] base = { "--spring.profiles.active=integration", "--spring.main.banner-mode=off" };
		String[] args = new String[base.length + extras.length];
		System.arraycopy(base, 0, args, 0, base.length);
		System.arraycopy(extras, 0, args, base.length, extras.length);
		return new SpringApplicationBuilder(SeazsApplication.class).web(WebApplicationType.SERVLET).run(args);
	}

	private static String causas(Throwable t) {
		StringBuilder sb = new StringBuilder();
		for (; t != null; t = t.getCause()) {
			sb.append(t.getMessage()).append('\n');
		}
		return sb.toString();
	}

	@Test
	public void semSegredo_aPartidaFalhaComMensagemQueCitaJwtSecret() {
		Exception falha = assertThrows(Exception.class, () -> iniciar("--app.jwt.secret=").close());

		assertTrue(causas(falha).contains("JWT_SECRET"), causas(falha));
	}

	@Test
	public void segredoCurto_aPartidaFalha() {
		Exception falha = assertThrows(Exception.class, () -> iniciar("--app.jwt.secret=curto").close());

		assertTrue(causas(falha).contains("32 bytes"), causas(falha));
	}

	@Test
	public void segredoValido_aPartidaFunciona() {
		iniciar("--app.jwt.secret=segredo-somente-para-testes-0123456789", "--server.port=0").close();
	}
}
