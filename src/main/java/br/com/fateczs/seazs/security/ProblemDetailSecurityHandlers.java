package br.com.fateczs.seazs.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * 401 e 403 em JSON {@code application/problem+json} (RFC 7807). A mensagem e fixa: nunca expoe o motivo
 * interno (token expirado, assinatura invalida, etc.).
 */
public class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper mapper;

	public ProblemDetailSecurityHandlers(JsonMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		escrever(response, HttpStatus.UNAUTHORIZED, "Não autenticado", "Autenticação necessária ou token inválido/expirado");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		escrever(response, HttpStatus.FORBIDDEN, "Acesso negado", "Sem permissão para este recurso");
	}

	private void escrever(HttpServletResponse response, HttpStatus status, String titulo, String detalhe)
			throws IOException {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(mapper.writeValueAsString(problema));
	}
}
