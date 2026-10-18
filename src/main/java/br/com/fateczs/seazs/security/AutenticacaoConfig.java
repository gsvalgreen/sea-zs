package br.com.fateczs.seazs.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Codificacao de senhas e AuthenticationManager (e-mail + senha). */
@Configuration
public class AutenticacaoConfig {

	/**
	 * DelegatingPasswordEncoder com BCrypt como padrao de escrita. Somente {@code bcrypt} e {@code noop} sao
	 * registrados (nada de MD5/SHA legados): {@code noop} existe so para ler as senhas antigas em texto puro
	 * durante a migracao (veja {@link UsuarioAutenticado}); remova-o quando nao restarem senhas sem prefixo no banco.
	 */
	@Bean
	@SuppressWarnings("deprecation")
	public PasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("bcrypt", new BCryptPasswordEncoder());
		encoders.put("noop", NoOpPasswordEncoder.getInstance());
		return new DelegatingPasswordEncoder("bcrypt", encoders);
	}

	/**
	 * O DaoAuthenticationProvider uniformiza o tempo de resposta (compara contra um hash dummy quando o e-mail nao
	 * existe) e dispara o upgrade de hash via UserDetailsPasswordService.
	 */
	@Bean
	public AuthenticationManager authenticationManager(SeazsUserDetailsService detalhes, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(detalhes);
		provider.setPasswordEncoder(encoder);
		provider.setUserDetailsPasswordService(detalhes);
		return new ProviderManager(provider);
	}
}
