package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.fateczs.seazs.model.RelatorioPontuacao;

public class RelatorioPontuacaoModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(RelatorioPontuacao.class) > 0);
	}
}
