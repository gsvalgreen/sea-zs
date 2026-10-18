package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.StaffAgendamento;

public class PresencaTest {

	private Inscricao inscricao(boolean continua, boolean checkIn, boolean checkOut) {
		Atividade atividade = new Atividade();
		atividade.setFlagContinua(continua);
		Inscricao inscricao = new Inscricao();
		inscricao.setAtividade(atividade);
		inscricao.setCheckIn(checkIn);
		inscricao.setCheckOut(checkOut);
		return inscricao;
	}

	private StaffAgendamento staff(boolean checkIn, boolean checkOut) {
		StaffAgendamento staff = new StaffAgendamento();
		staff.setCheckIn(checkIn);
		staff.setCheckOut(checkOut);
		return staff;
	}

	@Test
	public void atividadeNaoContinuaExigeCheckInECheckOut() {
		assertTrue(inscricao(false, true, true).validouPresenca());
		assertFalse(inscricao(false, true, false).validouPresenca());
		assertFalse(inscricao(false, false, true).validouPresenca());
		assertFalse(inscricao(false, false, false).validouPresenca());
	}

	@Test
	public void atividadeContinuaExigeCheckInOuCheckOut() {
		assertTrue(inscricao(true, true, false).validouPresenca());
		assertTrue(inscricao(true, false, true).validouPresenca());
		assertTrue(inscricao(true, true, true).validouPresenca());
		assertFalse(inscricao(true, false, false).validouPresenca());
	}

	@Test
	public void staffExigeCheckInECheckOut() {
		assertTrue(staff(true, true).validouPresenca());
		assertFalse(staff(true, false).validouPresenca());
		assertFalse(staff(false, true).validouPresenca());
		assertFalse(staff(false, false).validouPresenca());
	}
}
