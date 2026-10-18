package br.com.fateczs.seazs.service;

/** Mudanca de perfil recusada por regra de negocio (409): o administrador nao pode retirar o proprio acesso. */
public class PerfilInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PerfilInvalidoException(String mensagem) {
		super(mensagem, null, false, false);
	}
}
