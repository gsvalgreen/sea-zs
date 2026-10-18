package br.com.fateczs.seazs.security;

import java.util.Map;
import java.util.Optional;
import java.time.Duration;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Aplica o limite por IP aos endpoints publicos sujeitos a forca bruta/enumeracao (somente POST): login (v1 e legado,
 * mesmo escopo), refresh e cadastro. Excedido, lanca {@link LimiteExcedidoException} (vira 429 em
 * {@code LimiteExcedidoExceptionHandler}). O IP e {@code request.getRemoteAddr()}: nenhum header e lido.
 */
public class LimiteTentativasInterceptor implements HandlerInterceptor {

	/** Caminho -> escopo do contador. Login v1 e legado dividem o mesmo contador (e o mesmo ataque). */
	static final Map<String, String> ESCOPOS = Map.of(
			"/api/v1/auth/login", "login",
			"/usuario/login", "login",
			"/api/v1/auth/refresh", "refresh",
			"/usuario/inserir", "cadastro");

	private final LimitadorPorIp limitador;

	public LimiteTentativasInterceptor(LimitadorPorIp limitador) {
		this.limitador = limitador;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String escopo = "POST".equals(request.getMethod()) ? ESCOPOS.get(request.getRequestURI()) : null;
		if (escopo != null) {
			Optional<Duration> espera = limitador.registrar(request.getRemoteAddr(), escopo);
			if (espera.isPresent()) {
				throw new LimiteExcedidoException(espera.get());
			}
		}
		return true;
	}
}
