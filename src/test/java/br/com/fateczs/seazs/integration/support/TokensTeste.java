package br.com.fateczs.seazs.integration.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Autenticacao simulada (JwtAuthenticationToken) para os {@code @WebMvcTest}: sub = id do usuario, papeis como no token real. */
public final class TokensTeste {

	private TokensTeste() {
	}

	/** Usuario com os papeis informados (ex.: "ROLE_ALUNO"). */
	public static RequestPostProcessor como(int idUsuario, String... papeis) {
		SimpleGrantedAuthority[] autoridades = new SimpleGrantedAuthority[papeis.length];
		for (int i = 0; i < papeis.length; i++) {
			autoridades[i] = new SimpleGrantedAuthority(papeis[i]);
		}
		return jwt().jwt(j -> j.subject(String.valueOf(idUsuario))).authorities(autoridades);
	}

	/** Administrador (id 1). */
	public static RequestPostProcessor admin() {
		return como(1, "ROLE_FUNCIONARIO", "ROLE_ADMIN");
	}
}
