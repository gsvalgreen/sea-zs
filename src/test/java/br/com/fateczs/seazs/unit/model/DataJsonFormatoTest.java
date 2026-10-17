package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import br.com.fateczs.seazs.model.Evento;

/**
 * Caracteriza o formato de data devolvido pela API (Jackson do Boot 2.7 com
 * spring.jackson.time-zone=America/Sao_Paulo), para detectar mudanca silenciosa
 * na migracao para Boot 3/4.
 */
public class DataJsonFormatoTest {

	// 2024-03-01T13:00:00Z == 10:00 em America/Sao_Paulo (-03:00)
	private static final long INSTANTE = 1709298000000L;

	private ObjectMapper mapperComoNoBoot() {
		return new ObjectMapper()
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
	}

	@Test
	public void dataDoEventoSerializaIso8601ComMilissegundosEOffsetDeSaoPaulo() throws Exception {
		Evento e = new Evento();
		e.setInicio(new Date(INSTANTE));

		String json = mapperComoNoBoot().writeValueAsString(e);

		assertTrue(json.contains("\"inicio\":\"2024-03-01T10:00:00.000-03:00\""), json);
	}
}
