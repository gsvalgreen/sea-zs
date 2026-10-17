package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import br.com.fateczs.seazs.model.Disciplina;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.UsuarioDisciplinaID;

public class UsuarioDisciplinaIDTest {

	private UsuarioDisciplinaID id(Disciplina disciplina, Usuario usuario, Date dtInicio) {
		UsuarioDisciplinaID id = new UsuarioDisciplinaID();
		id.setDisciplina(disciplina);
		id.setUsuario(usuario);
		id.setDtInicio(dtInicio);
		return id;
	}

	@Test
	public void idsComMesmosComponentesSaoIguais() {
		Disciplina d = new Disciplina();
		Usuario u = new Usuario();
		Date data = new Date(0);
		UsuarioDisciplinaID a = id(d, u, data);
		UsuarioDisciplinaID b = id(d, u, data);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	public void idsComComponentesDiferentesNaoSaoIguais() {
		Disciplina d = new Disciplina();
		Usuario u = new Usuario();
		Date data = new Date(0);
		UsuarioDisciplinaID base = id(d, u, data);
		assertNotEquals(base, id(new Disciplina(), u, data));
		// Usuario implementa equals por valor; precisa diferir em algum campo
		Usuario outro = new Usuario();
		outro.setId(99);
		assertNotEquals(base, id(d, outro, data));
		assertNotEquals(base, id(d, u, new Date(1)));
	}

	@Test
	public void comparaComNuloETipoDiferente() {
		UsuarioDisciplinaID a = id(null, null, null);
		assertFalse(a.equals(null));
		assertFalse(a.equals("texto"));
	}

	@Test
	public void componentesNulosSaoTratados() {
		UsuarioDisciplinaID a = id(null, null, null);
		UsuarioDisciplinaID b = id(null, null, null);
		assertTrue(a.equals(b));
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(id(new Disciplina(), null, null)));
		assertFalse(a.equals(id(null, new Usuario(), null)));
		assertFalse(a.equals(id(null, null, new Date(0))));
		assertFalse(id(new Disciplina(), null, null).equals(a));
	}
}
