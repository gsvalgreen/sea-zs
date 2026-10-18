package br.com.fateczs.seazs.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Geracao do refresh token opaco (256 bits aleatorios) e do hash SHA-256 que e o que vai para o banco. */
public final class RefreshTokens {

	private static final SecureRandom RANDOM = new SecureRandom();

	private RefreshTokens() {
	}

	public static String gerar() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** SHA-256 em hexadecimal (64 caracteres). Suficiente porque o token tem entropia alta (nao e uma senha). */
	public static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponivel", e);
		}
	}
}
