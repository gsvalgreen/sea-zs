package br.com.fateczs.seazs.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Assinatura e verificacao dos access tokens: HS256 com segredo simetrico vindo de JWT_SECRET. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	/** HS256 exige chave de pelo menos 256 bits (RFC 7518, 3.2). */
	static final int SEGREDO_MIN_BYTES = 32;

	static SecretKey chave(JwtProperties props) {
		String segredo = props.secret();
		if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < SEGREDO_MIN_BYTES) {
			throw new IllegalStateException("JWT_SECRET (app.jwt.secret) ausente ou com menos de " + SEGREDO_MIN_BYTES
					+ " bytes. Gere um com: openssl rand -base64 48");
		}
		if (props.accessTtl().isZero() || props.accessTtl().isNegative()
				|| props.refreshTtl().isZero() || props.refreshTtl().isNegative()) {
			throw new IllegalStateException("app.jwt.access-ttl e app.jwt.refresh-ttl devem ser positivos");
		}
		return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties props) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(props)));
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties props) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(props)).macAlgorithm(MacAlgorithm.HS256).build();
		// Valida assinatura (HS256 fixo), exp/nbf e o emissor.
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
		return decoder;
	}
}
