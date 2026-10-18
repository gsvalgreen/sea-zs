package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Curso;
import br.com.fateczs.seazs.service.CursoService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fateczs.seazs.controller.CursoController;

@ExtendWith(MockitoExtension.class)
public class CursoControllerTest {

	@Mock
	private CursoService cursoServ;

	@InjectMocks
	private CursoController controller;

	@Test
	public void inserirCursoDelegaParaOService() {
		Curso arg = new Curso();
		controller.inserirCurso(arg);
		verify(cursoServ).inserir(arg);
	}

	@Test
	public void buscarCursoDelegaParaOService() {
		Curso arg = new Curso();
		Curso esperado = new Curso();
		when(cursoServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarCurso(arg));
		verify(cursoServ).buscar(arg);
	}

	@Test
	public void alterarCursoDelegaParaOService() {
		Curso arg = new Curso();
		controller.alterarCurso(arg);
		verify(cursoServ).alterar(arg);
	}

	@Test
	public void excluirCursoDelegaParaOService() {
		Curso arg = new Curso();
		controller.excluirCurso(arg);
		verify(cursoServ).excluir(arg);
	}

	@Test
	public void listarCursoDelegaParaOService() {
		List<Curso> esperado = new ArrayList<>();
		when(cursoServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarCurso());
		verify(cursoServ).listar();
	}
}
