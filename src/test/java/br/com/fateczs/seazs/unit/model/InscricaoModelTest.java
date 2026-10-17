package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.fateczs.seazs.model.Inscricao;

public class InscricaoModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(Inscricao.class) > 0);
	}
}
