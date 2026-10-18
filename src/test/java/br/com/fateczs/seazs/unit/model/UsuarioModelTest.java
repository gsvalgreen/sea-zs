package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.Usuario;

public class UsuarioModelTest {

	@Test
	public void gettersESettersDevolvemOValorAtribuido() throws Exception {
		assertTrue(BeanTestSupport.verificaGettersESetters(Usuario.class) > 0);
	}

	@Test
	public void igualdadePorIdEntidadesTransitoriasSoSaoIguaisASiMesmas() {
		Usuario a = new Usuario();
		Usuario b = new Usuario();
		assertEquals(a, a);
		assertNotEquals(a, b);
		assertNotEquals(a, null);
		assertNotEquals(a, "texto");
	}

	@Test
	public void igualdadePorIdIgnoraOsDemaisCamposEHashCodeNaoMudaAoGanharId() {
		Usuario a = new Usuario();
		a.setEmail("a@x.com");
		int antes = a.hashCode();
		a.setId(1);
		Usuario b = new Usuario();
		b.setId(1);
		b.setEmail("outro@x.com");
		Usuario c = new Usuario();
		c.setId(2);

		assertEquals(antes, a.hashCode());
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(new Usuario(), a);
	}
}
