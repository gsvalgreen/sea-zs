package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.UsuarioDisciplina;
import br.com.fateczs.seazs.service.UsuarioDisciplinaService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fateczs.seazs.controller.UsuarioDisciplinaController;

@ExtendWith(MockitoExtension.class)
public class UsuarioDisciplinaControllerTest {

	@Mock
	private UsuarioDisciplinaService usuarioDisciplinaServ;

	@InjectMocks
	private UsuarioDisciplinaController controller;

	@Test
	public void inserirUsuarioDisciplinaDelegaParaOService() {
		UsuarioDisciplina arg = new UsuarioDisciplina();
		controller.inserirUsuarioDisciplina(arg);
		verify(usuarioDisciplinaServ).inserir(arg);
	}

	@Test
	public void buscarUsuarioDisciplinaDelegaParaOService() {
		UsuarioDisciplina arg = new UsuarioDisciplina();
		UsuarioDisciplina esperado = new UsuarioDisciplina();
		when(usuarioDisciplinaServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarUsuarioDisciplina(arg));
		verify(usuarioDisciplinaServ).buscar(arg);
	}

	@Test
	public void alterarUsuarioDisciplinaDelegaParaOService() {
		UsuarioDisciplina arg = new UsuarioDisciplina();
		controller.alterarUsuarioDisciplina(arg);
		verify(usuarioDisciplinaServ).alterar(arg);
	}

	@Test
	public void excluirUsuarioDisciplinaDelegaParaOService() {
		UsuarioDisciplina arg = new UsuarioDisciplina();
		controller.excluirUsuarioDisciplina(arg);
		verify(usuarioDisciplinaServ).excluir(arg);
	}

	@Test
	public void listarUsuarioDisciplinaDelegaParaOService() {
		List<UsuarioDisciplina> esperado = new ArrayList<>();
		when(usuarioDisciplinaServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarUsuarioDisciplina());
		verify(usuarioDisciplinaServ).listar();
	}

	@Test
	public void listarDisciplinasUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		List<UsuarioDisciplina> esperado = new ArrayList<>();
		when(usuarioDisciplinaServ.recuperarDisciplinasDoUsuario(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarDisciplinasUsuario(arg));
		verify(usuarioDisciplinaServ).recuperarDisciplinasDoUsuario(arg);
	}
}
