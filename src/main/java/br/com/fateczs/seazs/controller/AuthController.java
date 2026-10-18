package br.com.fateczs.seazs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.fateczs.seazs.model.dto.LoginRequest;
import br.com.fateczs.seazs.model.dto.RefreshTokenRequest;
import br.com.fateczs.seazs.model.dto.TokenResponse;
import br.com.fateczs.seazs.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	@Autowired
	private AuthService authService;

	@PreAuthorize("permitAll()")
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest requisicao) {
		return authService.login(requisicao);
	}

	@PreAuthorize("permitAll()")
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest requisicao) {
		return authService.refresh(requisicao.refreshToken())
				.orElseThrow(RefreshTokenInvalidoException::new);
	}

	@PreAuthorize("permitAll()")
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody RefreshTokenRequest requisicao) {
		authService.logout(requisicao.refreshToken());
	}

	/** Refresh token desconhecido, expirado, revogado ou ja usado (todos respondem igual, 401). */
	public static class RefreshTokenInvalidoException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public RefreshTokenInvalidoException() {
			super("refresh token invalido", null, false, false);
		}
	}
}
