package br.com.fateczs.seazs.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros do {@link AuthController} em ProblemDetail (RFC 7807). Escopo restrito a esse controller: os endpoints
 * legados mantem o comportamento atual (a padronizacao global fica para a Fase 3).
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler extends ResponseEntityExceptionHandler {

	/** 401 generico: nao distingue e-mail inexistente de senha errada. */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ProblemDetail> credenciaisInvalidas(BadCredentialsException ex) {
		return naoAutorizado("Credenciais inválidas");
	}

	@ExceptionHandler(AuthController.RefreshTokenInvalidoException.class)
	public ResponseEntity<ProblemDetail> refreshInvalido(AuthController.RefreshTokenInvalidoException ex) {
		return naoAutorizado("Refresh token inválido ou expirado");
	}

	private ResponseEntity<ProblemDetail> naoAutorizado(String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detalhe);
		problema.setTitle("Não autenticado");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.body(problema);
	}

	/** 400 com a lista de campos invalidos (mensagens definidas pela propria aplicacao, sem valores enviados). */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requisição inválida");
		problema.setTitle("Dados inválidos");
		List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> Map.of("campo", e.getField(), "mensagem", String.valueOf(e.getDefaultMessage())))
				.collect(Collectors.toList());
		problema.setProperty("erros", erros);
		return ResponseEntity.badRequest().headers(headers).body(problema);
	}
}
