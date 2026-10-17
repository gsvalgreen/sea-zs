package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.fateczs.seazs.model.LinhaRelatorioPontuacao;

public class LinhaRelatorioPontuacaoModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(LinhaRelatorioPontuacao.class) > 0);
	}
}
