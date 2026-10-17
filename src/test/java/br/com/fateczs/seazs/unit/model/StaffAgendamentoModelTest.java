package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.fateczs.seazs.model.StaffAgendamento;

public class StaffAgendamentoModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(StaffAgendamento.class) > 0);
	}
}
