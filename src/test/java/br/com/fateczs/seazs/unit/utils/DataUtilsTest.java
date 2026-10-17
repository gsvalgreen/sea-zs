package br.com.fateczs.seazs.unit.utils;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import br.com.fateczs.seazs.utils.DataUtils;

public class DataUtilsTest {

	private final DataUtils dataUtils = new DataUtils();

	private Date data(int ano, int mes, int dia, int hora, int minuto) {
		Calendar c = new GregorianCalendar(DataUtils.TIME_ZONE);
		c.clear();
		c.set(ano, mes, dia, hora, minuto, 0);
		return c.getTime();
	}

	@Test
	public void somaMinutosPositivo() {
		Date base = data(2019, Calendar.JANUARY, 10, 10, 0);
		assertEquals(base.getTime() + 30 * 60 * 1000L, dataUtils.somaMinutos(base, 30).getTime());
	}

	@Test
	public void somaMinutosNegativo() {
		Date base = data(2019, Calendar.JANUARY, 10, 10, 0);
		assertEquals(base.getTime() - 10 * 60 * 1000L, dataUtils.somaMinutos(base, -10).getTime());
	}

	@Test
	public void somaMinutosCruzandoDia() {
		Date base = data(2019, Calendar.JANUARY, 31, 23, 50);
		assertEquals(data(2019, Calendar.FEBRUARY, 1, 0, 10).getTime(), dataUtils.somaMinutos(base, 20).getTime());
	}

	@Test
	public void verificaSemestreMesesDoPrimeiroSemestre() {
		assertEquals(Integer.valueOf(1), dataUtils.verificaSemestre(new GregorianCalendar(2019, Calendar.JANUARY, 1)));
		assertEquals(Integer.valueOf(1), dataUtils.verificaSemestre(new GregorianCalendar(2019, Calendar.JULY, 1)));
	}

	@Test
	public void verificaSemestreMesesDoSegundoSemestre() {
		assertEquals(Integer.valueOf(2), dataUtils.verificaSemestre(new GregorianCalendar(2019, Calendar.AUGUST, 1)));
		assertEquals(Integer.valueOf(2), dataUtils.verificaSemestre(new GregorianCalendar(2019, Calendar.DECEMBER, 1)));
	}

	@Test
	public void getDataAtualUsaFusoSaoPaulo() {
		assertEquals(DataUtils.TIME_ZONE, dataUtils.getDataAtual().getTimeZone());
	}

	@Test
	public void getDataAtualUsaORelogioInjetado() {
		Instant instante = Instant.parse("2019-08-01T02:59:59Z");
		DataUtils fixo = new DataUtils(Clock.fixed(instante, ZoneId.of("UTC")));

		Calendar atual = fixo.getDataAtual();

		assertEquals(instante.toEpochMilli(), atual.getTimeInMillis());
		assertEquals(DataUtils.TIME_ZONE, atual.getTimeZone());
		// 23:59:59 de 31/07 em Sao Paulo, mesmo com o Clock em UTC
		assertEquals(Calendar.JULY, atual.get(Calendar.MONTH));
		assertEquals(Integer.valueOf(1), fixo.verificaSemestre(atual));
	}

	@Test
	public void formataDataDiaMesAnoSemZeroAEsquerda() {
		assertEquals("5/3/2019", dataUtils.formataDataDiaMesAno(data(2019, Calendar.MARCH, 5, 12, 0)));
		assertEquals("25/12/2020", dataUtils.formataDataDiaMesAno(data(2020, Calendar.DECEMBER, 25, 12, 0)));
	}

	@Test
	public void calculaCargaHorariaEmHoras() {
		assertEquals(4, dataUtils.calculaCargaHoraria(data(2019, Calendar.MAY, 1, 8, 0), data(2019, Calendar.MAY, 1, 12, 0)));
	}

	@Test
	public void calculaCargaHorariaTruncaHorasIncompletas() {
		assertEquals(1, dataUtils.calculaCargaHoraria(data(2019, Calendar.MAY, 1, 8, 0), data(2019, Calendar.MAY, 1, 9, 59)));
	}

	@Test
	public void calculaCargaHorariaIgnoraOrdemDasDatas() {
		assertEquals(3, dataUtils.calculaCargaHoraria(data(2019, Calendar.MAY, 1, 11, 0), data(2019, Calendar.MAY, 1, 8, 0)));
	}
}
