package br.com.fateczs.seazs.model.dto;

/**
 * Resposta de login/refresh. {@code expiresIn} e a validade do access token em segundos.
 */
public record TokenResponse(
		String tokenType,
		String accessToken,
		long expiresIn,
		String refreshToken,
		UsuarioResumoResponse usuario) {

	public static final String BEARER = "Bearer";

	@Override
	public String toString() {
		return "TokenResponse[usuario=" + usuario + "]"; // nunca os tokens
	}
}
