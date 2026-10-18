package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.UsuarioDisciplina;

public class UsuarioDisciplinaModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(UsuarioDisciplina.class) > 0);
	}
}
