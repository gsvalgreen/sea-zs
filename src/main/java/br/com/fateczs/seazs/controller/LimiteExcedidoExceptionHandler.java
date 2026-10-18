package br.com.fateczs.seazs.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.fateczs.seazs.security.LimiteExcedidoException;

/**
 * 429 com {@code Retry-After} para limite por IP e bloqueio por e-mail. Mensagem fixa e igual nos dois casos: nao diz se
 * o e-mail existe nem qual limite foi atingido. Trata so essa excecao, em todos os controllers (legados e /api/v1).
 */
@RestControllerAdvice
public class LimiteExcedidoExceptionHandler {

	@ExceptionHandler(LimiteExcedidoException.class)
	public ResponseEntity<ProblemDetail> limiteExcedido(LimiteExcedidoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
				"Muitas tentativas. Tente novamente mais tarde.");
		problema.setTitle("Limite de tentativas excedido");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.segundos())).body(problema);
	}
}
