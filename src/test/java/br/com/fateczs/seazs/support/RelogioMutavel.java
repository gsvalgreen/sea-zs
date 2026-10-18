package br.com.fateczs.seazs.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relogio de teste que so anda quando o teste manda: elimina flakiness em limites de tempo. */
public class RelogioMutavel extends Clock {

	private volatile Instant agora;
	private final ZoneId zona;

	public RelogioMutavel(Instant inicio) {
		this(inicio, ZoneOffset.UTC);
	}

	private RelogioMutavel(Instant inicio, ZoneId zona) {
		this.agora = inicio;
		this.zona = zona;
	}

	public void avancar(Duration duracao) {
		agora = agora.plus(duracao);
	}

	@Override
	public ZoneId getZone() {
		return zona;
	}

	@Override
	public Clock withZone(ZoneId novaZona) {
		return new RelogioMutavel(agora, novaZona);
	}

	@Override
	public Instant instant() {
		return agora;
	}
}
