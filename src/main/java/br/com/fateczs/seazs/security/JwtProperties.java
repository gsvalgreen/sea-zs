package br.com.fateczs.seazs.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracao dos tokens (prefixo app.jwt). O segredo vem SEMPRE do ambiente (JWT_SECRET) e nao tem valor
 * padrao versionado; {@link JwtConfig} falha a partida se estiver ausente ou com menos de 32 bytes.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@DefaultValue("") String secret,
		@DefaultValue("seazs") String issuer,
		@DefaultValue("PT15M") Duration accessTtl,
		@DefaultValue("P7D") Duration refreshTtl) {

	@Override
	public String toString() {
		// Nunca imprimir o segredo em logs.
		return "JwtProperties[issuer=" + issuer + ", accessTtl=" + accessTtl + ", refreshTtl=" + refreshTtl + "]";
	}
}
