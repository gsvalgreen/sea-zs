package br.com.fateczs.seazs.service;

/** Usuario inexistente (404). */
public class UsuarioNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UsuarioNaoEncontradoException() {
		super("usuario nao encontrado", null, false, false);
	}
}
