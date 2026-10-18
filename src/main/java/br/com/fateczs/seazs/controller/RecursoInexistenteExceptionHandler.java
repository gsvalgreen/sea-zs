package br.com.fateczs.seazs.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

/**
 * 404 quando evento ou agendamento nao existe (ex.: alterar com id inexistente). Mensagem fixa: nao expoe a mensagem
 * interna. Escopo restrito a esses controllers e a essa excecao (AccessDeniedException segue para o Spring Security).
 */
@RestControllerAdvice(assignableTypes = { EventoController.class, AgendamentoController.class })
public class RecursoInexistenteExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ProblemDetail> naoEncontrado(EntityNotFoundException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado");
		problema.setTitle("Recurso não encontrado");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problema);
	}
}
