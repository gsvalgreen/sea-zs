package br.com.fateczs.seazs.service;

import java.util.Optional;

import br.com.fateczs.seazs.model.dto.LoginLegadoResponse;
import br.com.fateczs.seazs.model.dto.LoginRequest;
import br.com.fateczs.seazs.model.dto.TokenResponse;

public interface AuthService {

	/** Autentica por e-mail e senha. Lanca {@code BadCredentialsException} (mensagem unica) se invalido. */
	TokenResponse login(LoginRequest requisicao);

	/** Troca um refresh token valido por um novo par (rotacao). Vazio se invalido, expirado ou ja usado. */
	Optional<TokenResponse> refresh(String refreshToken);

	/** Revoga a sessao (familia) do refresh token. Idempotente: token desconhecido e ignorado. */
	void logout(String refreshToken);

	/** Login do endpoint legado: vazio se as credenciais forem invalidas. */
	Optional<LoginLegadoResponse> loginLegado(String email, String senha);
}
