package br.com.fateczs.seazs.model.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import br.com.fateczs.seazs.model.Usuario;

/**
 * Resposta do {@code POST /usuario/login} (legado): o mesmo JSON de {@link Usuario} de antes (a senha segue
 * fora da resposta) mais o campo novo {@code token} (access token). Sai quando o front migrar para
 * {@code /api/v1/auth/login}.
 */
public class LoginLegadoResponse {

	@JsonUnwrapped
	private final Usuario usuario;
	private final String token;

	public LoginLegadoResponse(Usuario usuario, String token) {
		this.usuario = usuario;
		this.token = token;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public String getToken() {
		return token;
	}
}
