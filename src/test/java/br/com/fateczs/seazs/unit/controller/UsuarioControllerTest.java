package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.dto.LoginLegadoResponse;
import br.com.fateczs.seazs.service.AuthService;
import br.com.fateczs.seazs.service.UsuarioService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fateczs.seazs.controller.UsuarioController;

@ExtendWith(MockitoExtension.class)
public class UsuarioControllerTest {

	@Mock
	private UsuarioService usuServ;

	@Mock
	private AuthService authService;

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
	public void loginUsuarioDelegaEmailESenhaParaOAuthService() {
		Usuario arg = new Usuario();
		arg.setEmail("a@b.com");
		arg.setSenha("123");
		LoginLegadoResponse esperado = new LoginLegadoResponse(new Usuario(), "tok");
		when(authService.loginLegado("a@b.com", "123")).thenReturn(Optional.of(esperado));
		assertSame(esperado, controller.loginUsuario(arg));
	}

	@Test
	public void loginUsuarioDevolveNuloQuandoCredenciaisInvalidas() {
		Usuario arg = new Usuario();
		arg.setEmail("a@b.com");
		arg.setSenha("errada");
		when(authService.loginLegado("a@b.com", "errada")).thenReturn(Optional.empty());
		assertNull(controller.loginUsuario(arg));
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
