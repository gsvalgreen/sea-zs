package br.com.fateczs.seazs.integration.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import br.com.fateczs.seazs.security.JwtConfig;
import br.com.fateczs.seazs.security.SecurityConfig;

/**
 * Para {@code @WebMvcTest}: carrega a cadeia de seguranca real (SecurityConfig + JwtConfig) com um segredo de
 * TESTE. Sem isso, o slice aplicaria a configuracao padrao do Spring Security (tudo autenticado, com CSRF).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ SecurityConfig.class, JwtConfig.class })
@TestPropertySource(properties = "app.jwt.secret=" + ComSeguranca.SEGREDO_TESTE)
public @interface ComSeguranca {

	/** Segredo de teste (nao e um segredo real). */
	String SEGREDO_TESTE = "segredo-somente-para-testes-0123456789";
}
