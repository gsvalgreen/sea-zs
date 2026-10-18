package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import br.com.fateczs.seazs.model.Evento;

/**
 * Caracteriza o formato de data devolvido pela API (Jackson 3 do Boot 4, que mantem o formato do Boot 2.7/3.5,
 * com spring.jackson.time-zone=America/Sao_Paulo), para detectar mudanca silenciosa.
 * Usa um JsonMapper montado a mao: protege contra
 * mudanca no Jackson ou na entidade, mas NAO valida a propriedade
 * spring.jackson.time-zone do application.properties.
 */
public class DataJsonFormatoTest {

	// 2024-03-01T13:00:00Z == 10:00 em America/Sao_Paulo (-03:00)
	private static final long INSTANTE = 1709298000000L;

	private JsonMapper mapperComoNoBoot() {
		return JsonMapper.builder()
				.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.defaultTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"))
				.build();
	}

	@Test
	public void dataDoEventoSerializaIso8601ComMilissegundosEOffsetDeSaoPaulo() throws Exception {
		Evento e = new Evento();
		e.setInicio(new Date(INSTANTE));

		String json = mapperComoNoBoot().writeValueAsString(e);

		assertTrue(json.contains("\"inicio\":\"2024-03-01T10:00:00.000-03:00\""), json);
	}
}
