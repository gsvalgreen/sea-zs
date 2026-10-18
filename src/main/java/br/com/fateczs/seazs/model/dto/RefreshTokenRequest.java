package br.com.fateczs.seazs.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code /refresh} e {@code /logout}. */
public record RefreshTokenRequest(
		@NotBlank(message = "refreshToken é obrigatório") @Size(max = 512, message = "refreshToken inválido") String refreshToken) {

	@Override
	public String toString() {
		return "RefreshTokenRequest[]";
	}
}
