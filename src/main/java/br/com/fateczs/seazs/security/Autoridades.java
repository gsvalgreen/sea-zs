package br.com.fateczs.seazs.security;

import java.util.ArrayList;
import java.util.List;

import br.com.fateczs.seazs.model.Usuario;

/** Authorities de um usuario: ROLE_&lt;TipoUsuario&gt; e, se {@code adm}, ROLE_ADMIN. */
public final class Autoridades {

	public static final String PREFIXO = "ROLE_";
	public static final String ADMIN = PREFIXO + "ADMIN";

	private Autoridades() {
	}

	public static List<String> de(Usuario usuario) {
		List<String> autoridades = new ArrayList<>();
		if (usuario.getTipoUsuario() != null) {
			autoridades.add(PREFIXO + usuario.getTipoUsuario().name());
		}
		if (Boolean.TRUE.equals(usuario.getAdm())) {
			autoridades.add(ADMIN);
		}
		return autoridades;
	}
}
