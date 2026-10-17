package br.com.fateczs.seazs.unit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import br.com.fateczs.seazs.utils.ModalidadeCurso;
import br.com.fateczs.seazs.utils.TipoUsuario;
import br.com.fateczs.seazs.utils.Turno;

public class EnumsTest {

	@Test
	public void tipoUsuarioPorId() {
		assertEquals(TipoUsuario.VISITANTE, TipoUsuario.getTipoUsuario(1));
		assertEquals(TipoUsuario.PROFESSOR, TipoUsuario.getTipoUsuario(2));
		assertEquals(TipoUsuario.FUNCIONARIO, TipoUsuario.getTipoUsuario(3));
		assertEquals(TipoUsuario.ALUNO, TipoUsuario.getTipoUsuario(4));
		assertEquals(Integer.valueOf(4), TipoUsuario.ALUNO.getIdTipoUsuario());
	}

	@Test
	public void tipoUsuarioNuloRetornaNulo() {
		assertNull(TipoUsuario.getTipoUsuario(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void tipoUsuarioInexistenteLancaExcecao() {
		TipoUsuario.getTipoUsuario(99);
	}

	@Test
	public void modalidadeCursoPorId() {
		assertEquals(ModalidadeCurso.PRESENCIAL, ModalidadeCurso.getModalidadeCurso(1));
		assertEquals(ModalidadeCurso.EAD, ModalidadeCurso.getModalidadeCurso(2));
		assertEquals(ModalidadeCurso.SEMIPRESENCIAL, ModalidadeCurso.getModalidadeCurso(3));
		assertEquals(Integer.valueOf(2), ModalidadeCurso.EAD.getIdModalidadeCurso());
	}

	@Test
	public void modalidadeCursoNuloRetornaNulo() {
		assertNull(ModalidadeCurso.getModalidadeCurso(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void modalidadeCursoInexistenteLancaExcecao() {
		ModalidadeCurso.getModalidadeCurso(99);
	}

	@Test
	public void turnoPorId() {
		assertEquals(Turno.MANHA, Turno.getTurno('M'));
		assertEquals(Turno.TARDE, Turno.getTurno('T'));
		assertEquals(Turno.NOITE, Turno.getTurno('N'));
		assertEquals(Character.valueOf('N'), Turno.NOITE.getIdTurno());
	}

	@Test
	public void turnoNuloRetornaNulo() {
		assertNull(Turno.getTurno(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void turnoInexistenteLancaExcecao() {
		Turno.getTurno('X');
	}
}
