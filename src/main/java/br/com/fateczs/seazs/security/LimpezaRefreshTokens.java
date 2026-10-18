package br.com.fateczs.seazs.security;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.repository.RefreshTokenRepository;

/**
 * Remove diariamente os refresh tokens expirados. Mantem uma carencia: um token rotacionado e reapresentado logo
 * apos expirar ainda e encontrado (e derruba a familia) em vez de sumir sem deixar rastro. Idempotente, entao
 * execucoes concorrentes em varias instancias so repetem o mesmo DELETE.
 */
@Component
public class LimpezaRefreshTokens {

	static final Duration CARENCIA = Duration.ofDays(7);

	@Autowired
	private RefreshTokenRepository refreshTokens;
	@Autowired
	private Clock clock;

	@Scheduled(cron = "${app.jwt.limpeza-cron:0 30 3 * * *}")
	@Transactional
	public int limparExpirados() {
		return refreshTokens.deleteByExpiraEmBefore(clock.instant().minus(CARENCIA));
	}
}
