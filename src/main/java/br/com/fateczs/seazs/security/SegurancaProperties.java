package br.com.fateczs.seazs.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limites contra forca bruta (prefixo app.security). Estado em memoria de uma instancia (veja o README sobre
 * multi-instancia). Duracoes em ISO-8601.
 *
 * @param rateLimit limite de requisicoes por IP nos endpoints publicos de autenticacao e cadastro
 * @param lockout   bloqueio temporario progressivo por e-mail apos falhas de login
 * @param maxEntradas teto de entradas em memoria de cada mapa (protege a memoria contra chaves aleatorias)
 */
@ConfigurationProperties(prefix = "app.security")
public record SegurancaProperties(
		@DefaultValue RateLimit rateLimit,
		@DefaultValue Lockout lockout,
		@DefaultValue("100000") int maxEntradas) {

	/**
	 * @param maxRequisicoes requisicoes permitidas por IP e escopo (login, refresh, cadastro) dentro da janela
	 * @param janela         tamanho da janela (janela fixa)
	 */
	public record RateLimit(
			@DefaultValue("10") int maxRequisicoes,
			@DefaultValue("PT1M") Duration janela) {
	}

	/**
	 * @param maxFalhas    falhas consecutivas (dentro de {@code janelaFalhas}) que disparam o bloqueio
	 * @param janelaFalhas tempo sem falhas apos o qual o contador de falhas recomeca
	 * @param duracao      duracao do primeiro bloqueio; dobra a cada novo bloqueio ate {@code duracaoMaxima}
	 * @param duracaoMaxima teto da duracao do bloqueio
	 * @param retencao     tempo sem falhas apos o qual o historico (inclusive o nivel de bloqueio) e esquecido
	 */
	public record Lockout(
			@DefaultValue("5") int maxFalhas,
			@DefaultValue("PT15M") Duration janelaFalhas,
			@DefaultValue("PT15M") Duration duracao,
			@DefaultValue("PT24H") Duration duracaoMaxima,
			@DefaultValue("PT24H") Duration retencao) {
	}
}
