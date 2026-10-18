package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.Usuario;

public class UsuarioModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(Usuario.class) > 0);
	}
}
