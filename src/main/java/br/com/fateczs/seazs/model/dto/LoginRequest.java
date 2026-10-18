package br.com.fateczs.seazs.model.dto;

import br.com.fateczs.seazs.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "e-mail é obrigatório") @Size(max = 255, message = "e-mail deve ter no máximo 255 caracteres") String email,
		@NotEmpty(message = "senha é obrigatória") @MaxBytes(MaxBytes.SENHA_BCRYPT) String senha) {

	@Override
	public String toString() {
		return "LoginRequest[email=" + email + "]"; // nunca a senha
	}
}
