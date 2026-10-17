package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.UsuarioDisciplina;
import br.com.fateczs.seazs.model.UsuarioDisciplinaID;
import br.com.fateczs.seazs.repository.UsuarioDisciplinaRepository;
import br.com.fateczs.seazs.service.serviceImpl.UsuarioDisciplinaServiceImpl;

@ExtendWith(MockitoExtension.class)
public class UsuarioDisciplinaServiceImplTest {

	@Mock
	private UsuarioDisciplinaRepository repository;

	@InjectMocks
	private UsuarioDisciplinaServiceImpl service;

	@Test
	public void buscarUsaChaveComposta() {
		UsuarioDisciplinaID id = new UsuarioDisciplinaID();
		UsuarioDisciplina entrada = new UsuarioDisciplina();
		entrada.setIdUsuarioDisciplina(id);
		UsuarioDisciplina esperado = new UsuarioDisciplina();
		when(repository.getOne(id)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		UsuarioDisciplina ud = new UsuarioDisciplina();
		List<UsuarioDisciplina> todas = Arrays.asList(ud);
		when(repository.findAll()).thenReturn(todas);

		service.inserir(ud);
		service.alterar(ud);
		service.excluir(ud);

		verify(repository, times(2)).save(ud);
		verify(repository).delete(ud);
		assertEquals(todas, service.listar());
	}

	@Test
	public void recuperarDisciplinasDoUsuarioUsaIdDoUsuario() {
		Usuario usuario = new Usuario();
		usuario.setId(6);
		List<UsuarioDisciplina> doUsuario = Arrays.asList(new UsuarioDisciplina());
		when(repository.findByIdUsuarioDisciplinaUsuarioId(6)).thenReturn(doUsuario);
		assertEquals(doUsuario, service.recuperarDisciplinasDoUsuario(usuario));
	}
}
