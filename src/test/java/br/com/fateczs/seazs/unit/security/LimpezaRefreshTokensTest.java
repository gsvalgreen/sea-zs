package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.security.LimpezaRefreshTokens;

@ExtendWith(MockitoExtension.class)
public class LimpezaRefreshTokensTest {

	private static final Instant AGORA = Instant.parse("2026-10-17T03:30:00Z");

	@Mock
	private RefreshTokenRepository refreshTokens;
	@Mock
	private Clock clock;
	@InjectMocks
	private LimpezaRefreshTokens limpeza;

	@Test
	public void removeApenasOsExpiradosHaMaisDaCarencia() {
		when(clock.instant()).thenReturn(AGORA);
		when(refreshTokens.deleteByExpiraEmBefore(AGORA.minusSeconds(7 * 24 * 3600L))).thenReturn(3);

		assertEquals(3, limpeza.limparExpirados());
	}
}
