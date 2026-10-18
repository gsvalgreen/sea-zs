package br.com.fateczs.seazs.security;

import java.time.Duration;

/** Limite de tentativas atingido (IP ou e-mail em bloqueio temporario): vira 429 com {@code Retry-After}. */
public class LimiteExcedidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient Duration tentarNovamenteEm;

	public LimiteExcedidoException(Duration tentarNovamenteEm) {
		super("limite de tentativas excedido", null, false, false);
		this.tentarNovamenteEm = tentarNovamenteEm;
	}

	public Duration getTentarNovamenteEm() {
		return tentarNovamenteEm;
	}

	/** Segundos para o header Retry-After (arredonda para cima, minimo 1). */
	public long segundos() {
		long ms = tentarNovamenteEm.toMillis();
		return Math.max(1, (ms + 999) / 1000);
	}
}
