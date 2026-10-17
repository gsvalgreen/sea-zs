package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.fateczs.seazs.model.Curso;

public class CursoModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(Curso.class) > 0);
	}
}
