package br.com.fateczs.seazs.unit.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.serviceImpl.UsuarioServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class UsuarioServiceImplTest {

	@Mock
	private UsuarioRepository repository;

	@InjectMocks
	private UsuarioServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Usuario entidade = new Usuario();
		entidade.setId(7);
		Usuario esperado = new Usuario();
		when(repository.getOne(7)).thenReturn(esperado);

		assertEquals(esperado, service.buscar(entidade));
	}

	@Test
	public void inserirSalva() {
		Usuario entidade = new Usuario();
		service.inserir(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void alterarSalva() {
		Usuario entidade = new Usuario();
		service.alterar(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void excluirRemove() {
		Usuario entidade = new Usuario();
		service.excluir(entidade);
		verify(repository).delete(entidade);
	}

	@Test
	public void listarRetornaTodos() {
		List<Usuario> todos = Arrays.asList(new Usuario(), new Usuario());
		when(repository.findAll()).thenReturn(todos);

		assertEquals(todos, service.listar());
	}

	@Test
	public void validarLoginBuscaPorEmailESenha() {
		Usuario entrada = new Usuario();
		entrada.setEmail("a@b.com");
		entrada.setSenha("123");
		Usuario encontrado = new Usuario();
		when(repository.getByEmailAndSenha("a@b.com", "123")).thenReturn(encontrado);

		assertEquals(encontrado, service.validarLogin(entrada));
	}

	@Test
	public void validarLoginRetornaNuloQuandoCredenciaisInvalidas() {
		Usuario entrada = new Usuario();
		entrada.setEmail("a@b.com");
		entrada.setSenha("errada");

		org.junit.Assert.assertNull(service.validarLogin(entrada));
	}

	@Test
	public void emitirCrachaMontaCrachaDoUsuarioPersistido() {
		Usuario entrada = new Usuario();
		entrada.setId(3);
		Usuario persistido = new Usuario();
		persistido.setNome("Ana");
		persistido.setTipoUsuario(br.com.fateczs.seazs.utils.TipoUsuario.ALUNO);
		when(repository.getOne(3)).thenReturn(persistido);

		br.com.fateczs.seazs.model.CrachaUsuario cracha = service.emitirCracha(entrada);

		assertEquals("Ana", cracha.getNomeDoUsuario());
		assertEquals("ALUNO", cracha.getTipoDoUsuario());
	}
}
