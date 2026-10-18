package br.com.fateczs.seazs.service;

/** Cadastro recusado porque o e-mail ja existe (violacao da chave unica). Sem o e-mail na mensagem. */
public class EmailJaCadastradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailJaCadastradoException(Throwable causa) {
		super("e-mail ja cadastrado", causa);
	}
}
