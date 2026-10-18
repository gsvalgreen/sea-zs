package br.com.fateczs.seazs.security;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.fateczs.seazs.model.Usuario;

/**
 * Adapta {@link Usuario} para o Spring Security. O username e o e-mail.
 *
 * Migracao de senhas: senha armazenada sem o prefixo {@code {bcrypt}} e legada (texto puro) e e lida como
 * {@code {noop}}; o DelegatingPasswordEncoder pede upgrade e a senha vira BCrypt no proximo login.
 */
public class UsuarioAutenticado implements UserDetails {

	private static final long serialVersionUID = 1L;
	static final String PREFIXO_BCRYPT = "{bcrypt}";
	static final String PREFIXO_LEGADO = "{noop}";

	private final transient Usuario usuario;

	public UsuarioAutenticado(Usuario usuario) {
		this.usuario = usuario;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Autoridades.de(usuario).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		String armazenada = usuario.getSenha();
		return armazenada.startsWith(PREFIXO_BCRYPT) ? armazenada : PREFIXO_LEGADO + armazenada;
	}

	@Override
	public String getUsername() {
		return usuario.getEmail();
	}
}
