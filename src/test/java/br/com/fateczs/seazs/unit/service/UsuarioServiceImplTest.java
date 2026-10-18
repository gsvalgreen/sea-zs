package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fateczs.seazs.model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.serviceImpl.UsuarioServiceImpl;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplTest {

	@Mock
	private UsuarioRepository repository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UsuarioServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Usuario entidade = new Usuario();
		entidade.setId(7);
		Usuario esperado = new Usuario();
		when(repository.getReferenceById(7)).thenReturn(esperado);

		assertEquals(esperado, service.buscar(entidade));
	}

	@Test
	public void inserirGuardaSenhaCodificadaENuncaOTextoPuro() {
		Usuario entidade = new Usuario();
		entidade.setSenha("Segredo123");
		when(passwordEncoder.encode("Segredo123")).thenReturn("{bcrypt}HASH");

		service.inserir(entidade);

		ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
		verify(repository).save(salvo.capture());
		assertEquals("{bcrypt}HASH", salvo.getValue().getSenha());
	}

	@Test
	public void inserirComSenhaAcimaDoLimiteDoBcryptPropagaAExcecaoDoEncoderSemPersistir() {
		Usuario entidade = new Usuario();
		entidade.setSenha("x");
		when(passwordEncoder.encode("x")).thenThrow(new IllegalArgumentException("password cannot be more than 72 bytes"));

		assertThrows(IllegalArgumentException.class, () -> service.inserir(entidade));

		verify(repository, never()).save(any());
	}

	@Test
	public void alterarGuardaSenhaCodificada() {
		Usuario entidade = new Usuario();
		entidade.setSenha("nova");
		when(passwordEncoder.encode("nova")).thenReturn("{bcrypt}NOVO");

		service.alterar(entidade);

		verify(repository).save(entidade);
		assertEquals("{bcrypt}NOVO", entidade.getSenha());
	}

	@Test
	public void excluirRemoveOsRefreshTokensEOUsuario() {
		Usuario entidade = new Usuario();
		entidade.setId(9);
		service.excluir(entidade);
		verify(refreshTokenRepository).deleteByIdUsuario(9);
		verify(repository).delete(entidade);
	}

	@Test
	public void listarRetornaTodos() {
		List<Usuario> todos = Arrays.asList(new Usuario(), new Usuario());
		when(repository.findAll()).thenReturn(todos);

		assertEquals(todos, service.listar());
	}

	@Test
	public void emitirCrachaMontaCrachaDoUsuarioPersistido() {
		Usuario entrada = new Usuario();
		entrada.setId(3);
		Usuario persistido = new Usuario();
		persistido.setNome("Ana");
		persistido.setTipoUsuario(br.com.fateczs.seazs.utils.TipoUsuario.ALUNO);
		when(repository.getReferenceById(3)).thenReturn(persistido);

		br.com.fateczs.seazs.model.CrachaUsuario cracha = service.emitirCracha(entrada);

		assertEquals("Ana", cracha.getNomeDoUsuario());
		assertEquals("ALUNO", cracha.getTipoDoUsuario());
	}
}
