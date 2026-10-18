package br.com.fateczs.seazs.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.fateczs.seazs.service.EmailJaCadastradoException;

/**
 * Erros do cadastro em ProblemDetail. Escopo restrito ao {@link UsuarioController}: os demais erros dos endpoints
 * legados mantem o comportamento atual.
 *
 * Trade-off de enumeracao: o 409 confirma que o e-mail ja existe. Num cadastro aberto isso nao da para evitar sem
 * confirmacao por e-mail (o usuario precisa saber que deve entrar em vez de cadastrar); a mensagem e generica e o
 * login continua sem diferenciar e-mail inexistente de senha errada. O rate limiting do passo 2b limita o abuso.
 */
@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class UsuarioExceptionHandler {

	@ExceptionHandler(EmailJaCadastradoException.class)
	public ResponseEntity<ProblemDetail> emailJaCadastrado(EmailJaCadastradoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Não foi possível concluir o cadastro com os dados informados");
		problema.setTitle("Cadastro não realizado");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
	}
}
