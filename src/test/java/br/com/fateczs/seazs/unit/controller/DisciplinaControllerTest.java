package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Disciplina;
import br.com.fateczs.seazs.service.DisciplinaService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.DisciplinaController;

@RunWith(MockitoJUnitRunner.class)
public class DisciplinaControllerTest {

	@Mock
	private DisciplinaService disciplinaServ;

	@InjectMocks
	private DisciplinaController controller;

	@Test
	public void inserirDisciplinaDelegaParaOService() {
		Disciplina arg = new Disciplina();
		controller.inserirDisciplina(arg);
		verify(disciplinaServ).inserir(arg);
	}

	@Test
	public void inserirLoteDisciplinaDelegaParaOService() {
		List<Disciplina> arg = new ArrayList<>();
		controller.inserirLoteDisciplina(arg);
		verify(disciplinaServ).inserirLote(arg);
	}

	@Test
	public void buscarDisciplinaDelegaParaOService() {
		Disciplina arg = new Disciplina();
		Disciplina esperado = new Disciplina();
		when(disciplinaServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarDisciplina(arg));
		verify(disciplinaServ).buscar(arg);
	}

	@Test
	public void alterarDisciplinaDelegaParaOService() {
		Disciplina arg = new Disciplina();
		controller.alterarDisciplina(arg);
		verify(disciplinaServ).alterar(arg);
	}

	@Test
	public void excluirDisciplinaDelegaParaOService() {
		Disciplina arg = new Disciplina();
		controller.excluirDisciplina(arg);
		verify(disciplinaServ).excluir(arg);
	}

	@Test
	public void listarDisciplinaDelegaParaOService() {
		List<Disciplina> esperado = new ArrayList<>();
		when(disciplinaServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarDisciplina());
		verify(disciplinaServ).listar();
	}
}
