package br.com.fateczs.seazs.security;

import java.util.Map;
import java.util.Optional;
import java.time.Duration;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Aplica o limite por IP aos endpoints publicos sujeitos a forca bruta/enumeracao (somente POST): login (v1 e legado,
 * mesmo escopo), refresh e cadastro. Excedido, lanca {@link LimiteExcedidoException} (vira 429 em
 * {@code LimiteExcedidoExceptionHandler}). O IP e {@code request.getRemoteAddr()}: nenhum header e lido.
 *
 * O escopo e resolvido pelo caminho decodificado e normalizado (o mesmo que o Spring MVC usa para achar o handler):
 * {@code /usuario/%6cogin}, {@code /usuario/login;x=1} e {@code //usuario//login} caem no mesmo escopo de
 * {@code /usuario/login}. Usar {@code getRequestURI()} cru deixaria essas variantes contornarem o limite.
 */
public class LimiteTentativasInterceptor implements HandlerInterceptor {

	/** Caminho -> escopo do contador. Login v1 e legado dividem o mesmo contador (e o mesmo ataque). */
	static final Map<String, String> ESCOPOS = Map.of(
			"/api/v1/auth/login", "login",
			"/usuario/login", "login",
			"/api/v1/auth/refresh", "refresh",
			"/usuario/inserir", "cadastro");

	private static final UrlPathHelper CAMINHOS = new UrlPathHelper();

	private final LimitadorPorIp limitador;

	public LimiteTentativasInterceptor(LimitadorPorIp limitador) {
		this.limitador = limitador;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String escopo = "POST".equals(request.getMethod()) ? ESCOPOS.get(caminho(request)) : null;
		if (escopo != null) {
			Optional<Duration> espera = limitador.registrar(request.getRemoteAddr(), escopo);
			if (espera.isPresent()) {
				throw new LimiteExcedidoException(espera.get());
			}
		}
		return true;
	}

	/** Caminho decodificado, sem parametros de caminho ({@code ;x=y}), sem barras duplicadas e sem o context path. */
	static String caminho(HttpServletRequest request) {
		return CAMINHOS.getPathWithinApplication(request);
	}
}
