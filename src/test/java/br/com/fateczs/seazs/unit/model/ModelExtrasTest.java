package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.CertificadoAtividade;
import br.com.fateczs.seazs.model.CertificadoStaff;
import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.Curso;
import br.com.fateczs.seazs.model.LinhaRelatorioPontuacao;
import br.com.fateczs.seazs.model.RelatorioPontuacao;
import br.com.fateczs.seazs.model.RodapeRelatorioPontuacao;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.utils.ModalidadeCurso;

/** Construtores, equals/hashCode e toString das classes de modelo. */
public class ModelExtrasTest {

	@Test
	public void cursoConstrutorComStringConverteId() {
		assertEquals(Integer.valueOf(5), new Curso("5").getId());
	}

	@Test
	public void cursoConstrutorComStringInvalidaFalha() {
		assertThrows(NumberFormatException.class, () -> new Curso("abc"));
	}

	@Test
	public void cursoEqualsEHashCode() {
		Curso a = new Curso("1");
		a.setNome("ADS");
		a.setModalidade(ModalidadeCurso.values()[0]);
		a.setQtdSemestres(6);
		Curso b = new Curso("1");
		b.setNome("ADS");
		b.setModalidade(ModalidadeCurso.values()[0]);
		b.setQtdSemestres(6);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertTrue(a.equals(a));
		assertFalse(a.equals(null));
		assertFalse(a.equals("x"));
		b.setNome("Outro");
		assertNotEquals(a, b);
		b.setNome("ADS");
		b.setModalidade(ModalidadeCurso.values()[1]);
		assertNotEquals(a, b);
		b.setModalidade(ModalidadeCurso.values()[0]);
		b.setQtdSemestres(5);
		assertNotEquals(a, b);
		assertNotEquals(a, new Curso("2"));
		assertNotEquals(new Curso(), a);
		assertEquals(new Curso(), new Curso());
	}

	@Test
	public void usuarioEqualsEHashCode() {
		Usuario a = new Usuario();
		Usuario b = new Usuario();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertTrue(a.equals(a));
		assertFalse(a.equals(null));
		assertFalse(a.equals("x"));
		a.setId(1);
		assertNotEquals(a, b);
		b.setId(1);
		assertEquals(a, b);
		a.setEmail("a@a");
		assertNotEquals(a, b);
		b.setEmail("a@a");
		a.setNome("n");
		assertNotEquals(a, b);
		b.setNome("n");
		a.setMatricula("m");
		assertNotEquals(a, b);
		b.setMatricula("m");
		a.setCurso(new Curso("1"));
		assertNotEquals(a, b);
		b.setCurso(new Curso("1"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void atividadeToStringIncluiCampos() {
		Atividade a = new Atividade();
		a.setNome("Palestra");
		String s = a.toString();
		assertTrue(s.startsWith("Atividade [id=null"));
		assertTrue(s.contains("nome=Palestra"));
	}

	@Test
	public void dtosImutaveisExpoemValoresDoConstrutor() {
		CertificadoAtividade ca = new CertificadoAtividade("Ana", "Palestra", "Ouvinte", 4);
		assertEquals("Ana", ca.getNomeUsuario());
		assertEquals("Palestra", ca.getAtividade());
		assertEquals("Ouvinte", ca.getPapel());
		assertEquals(4, ca.getCargaHorariaAtividade());

		CertificadoStaff cs = new CertificadoStaff("p", "Evento", "Staff", 8);
		assertEquals("Evento", cs.getNomeEvento());
		assertEquals(8, cs.getCargaHoraria());

		CrachaUsuario c = new CrachaUsuario("Ana", "Aluno");
		assertEquals("Ana", c.getNomeDoUsuario());
		assertEquals("Aluno", c.getTipoDoUsuario());

		LinhaRelatorioPontuacao l = new LinhaRelatorioPontuacao("Ana", "Aluno", "ADS", 3, 10);
		assertEquals("ADS", l.getCursoUsuario());
		assertEquals(10, l.getTotalDePontos());

		RodapeRelatorioPontuacao r = new RodapeRelatorioPontuacao(2, 3);
		assertEquals(2, r.getTotalDeUsuarios());
		assertEquals(3, r.getTotalDeAtividades());

		RelatorioPontuacao rel = new RelatorioPontuacao(Collections.singletonList(l), r);
		assertEquals(1, rel.getLinha().size());
		assertEquals(r, rel.getRodapeRelatorioPontuacao());
	}
}
