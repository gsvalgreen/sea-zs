package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fateczs.seazs.model.Curso;
import br.com.fateczs.seazs.repository.CursoRepository;
import br.com.fateczs.seazs.service.serviceImpl.CursoServiceImpl;

@ExtendWith(MockitoExtension.class)
public class CursoServiceImplTest {

	@Mock
	private CursoRepository repository;

	@InjectMocks
	private CursoServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Curso entidade = new Curso();
		entidade.setId(7);
		Curso esperado = new Curso();
		when(repository.getReferenceById(7)).thenReturn(esperado);

		assertEquals(esperado, service.buscar(entidade));
	}

	@Test
	public void inserirSalva() {
		Curso entidade = new Curso();
		service.inserir(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void alterarSalva() {
		Curso entidade = new Curso();
		service.alterar(entidade);
		verify(repository).save(entidade);
	}

	// BUG CONHECIDO: excluir deveria chamar delete; hoje só chama save.
	@Test
	public void excluirApenasSalvaSemRemoverComportamentoAtual() {
		Curso entidade = new Curso();
		service.excluir(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void listarRetornaTodos() {
		List<Curso> todos = Arrays.asList(new Curso(), new Curso());
		when(repository.findAll()).thenReturn(todos);

		assertEquals(todos, service.listar());
	}
}
