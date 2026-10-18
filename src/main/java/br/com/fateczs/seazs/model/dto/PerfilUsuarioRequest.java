package br.com.fateczs.seazs.model.dto;

import br.com.fateczs.seazs.utils.TipoUsuario;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PUT /api/v1/usuarios/{id}/perfil}: unico caminho para mudar o perfil e o flag de administrador. */
public record PerfilUsuarioRequest(
		@NotNull(message = "tipoUsuario é obrigatório") TipoUsuario tipoUsuario,
		@NotNull(message = "adm é obrigatório") Boolean adm) {
}
