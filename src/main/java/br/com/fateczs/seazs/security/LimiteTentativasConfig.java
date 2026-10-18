package br.com.fateczs.seazs.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra o limite por IP nos endpoints de autenticacao/cadastro e habilita {@link SegurancaProperties}. */
@Configuration
@EnableConfigurationProperties(SegurancaProperties.class)
public class LimiteTentativasConfig implements WebMvcConfigurer {

	private final LimitadorPorIp limitador;

	public LimiteTentativasConfig(LimitadorPorIp limitador) {
		this.limitador = limitador;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LimiteTentativasInterceptor(limitador))
				.addPathPatterns(LimiteTentativasInterceptor.ESCOPOS.keySet().toArray(new String[0]));
	}
}
