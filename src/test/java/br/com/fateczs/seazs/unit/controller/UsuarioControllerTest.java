package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.UsuarioService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.UsuarioController;

@RunWith(MockitoJUnitRunner.class)
public class UsuarioControllerTest {

	@Mock
	private UsuarioService usuServ;

	@InjectMocks
	private UsuarioController controller;

	@Test
	public void inserirUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		controller.inserirUsuario(arg);
		verify(usuServ).inserir(arg);
	}

	@Test
	public void buscarUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		Usuario esperado = new Usuario();
		when(usuServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarUsuario(arg));
		verify(usuServ).buscar(arg);
	}

	@Test
	public void alterarUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		controller.alterarUsuario(arg);
		verify(usuServ).alterar(arg);
	}

	@Test
	public void excluirUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		controller.excluirUsuario(arg);
		verify(usuServ).excluir(arg);
	}

	@Test
	public void listarUsuarioDelegaParaOService() {
		List<Usuario> esperado = new ArrayList<>();
		when(usuServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarUsuario());
		verify(usuServ).listar();
	}

	@Test
	public void loginUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		Usuario esperado = new Usuario();
		when(usuServ.validarLogin(arg)).thenReturn(esperado);
		assertSame(esperado, controller.loginUsuario(arg));
		verify(usuServ).validarLogin(arg);
	}

	@Test
	public void emitirCrachaDelegaParaOService() {
		Usuario arg = new Usuario();
		CrachaUsuario esperado = new CrachaUsuario("Ana", "ALUNO");
		when(usuServ.emitirCracha(arg)).thenReturn(esperado);
		assertSame(esperado, controller.emitirCracha(arg));
		verify(usuServ).emitirCracha(arg);
	}
}
