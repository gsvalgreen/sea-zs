package br.com.fateczs.seazs.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

	/**
	 * API stateless autenticada por Bearer JWT: sem sessao e sem CSRF. CSRF protege contra o navegador anexar
	 * credenciais ambientes (cookie de sessao/Basic); aqui o token e enviado explicitamente no header Authorization
	 * e nada e guardado em cookie, entao nao ha o que forjar.
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JsonMapper mapper) throws Exception {
		ProblemDetailSecurityHandlers handlers = new ProblemDetailSecurityHandlers(mapper);
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(a -> a
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/usuario/login", "/usuario/inserir").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						// Novas rotas exigem autenticacao.
						.requestMatchers("/api/v1/**").authenticated()
						// TODO(2b): fechar as rotas legadas por RBAC (@PreAuthorize/checagem do dono). Hoje o front
						// atual nao envia Bearer, entao elas seguem abertas; so /api/v1/** esta protegido.
						.anyRequest().permitAll())
				.oauth2ResourceServer(o -> o
						.jwt(j -> j.jwtAuthenticationConverter(conversor()))
						.authenticationEntryPoint(handlers)
						.accessDeniedHandler(handlers))
				.exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers));
		return http.build();
	}

	private JwtAuthenticationConverter conversor() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(JwtTokenService.CLAIM_ROLES);
		authorities.setAuthorityPrefix(""); // o claim ja traz ROLE_...
		JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
		conversor.setJwtGrantedAuthoritiesConverter(authorities);
		return conversor;
	}

	/** CORS global (substitui os @CrossOrigin por metodo). Origens vem de app.cors.allowed-origins. */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> origens) {
		if (origens.stream().anyMatch(o -> o.contains("*"))) {
			throw new IllegalStateException("app.cors.allowed-origins nao aceita curinga (*): liste as origens explicitamente");
		}
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origens);
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
		config.setAllowCredentials(false); // Bearer no header; nao ha cookies
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		fonte.registerCorsConfiguration("/**", config);
		return fonte;
	}
}
