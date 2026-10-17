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

import br.com.fateczs.seazs.model.Disciplina;
import br.com.fateczs.seazs.repository.DisciplinaRepository;
import br.com.fateczs.seazs.service.serviceImpl.DisciplinaServiceImpl;

@ExtendWith(MockitoExtension.class)
public class DisciplinaServiceImplTest {

	@Mock
	private DisciplinaRepository repository;

	@InjectMocks
	private DisciplinaServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Disciplina entidade = new Disciplina();
		entidade.setId(7);
		Disciplina esperado = new Disciplina();
		when(repository.getOne(7)).thenReturn(esperado);

		assertEquals(esperado, service.buscar(entidade));
	}

	@Test
	public void inserirSalva() {
		Disciplina entidade = new Disciplina();
		service.inserir(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void alterarSalva() {
		Disciplina entidade = new Disciplina();
		service.alterar(entidade);
		verify(repository).save(entidade);
	}

	// BUG CONHECIDO: excluir deveria chamar delete; hoje só chama save.
	@Test
	public void excluirApenasSalvaSemRemoverComportamentoAtual() {
		Disciplina entidade = new Disciplina();
		service.excluir(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void listarRetornaTodos() {
		List<Disciplina> todos = Arrays.asList(new Disciplina(), new Disciplina());
		when(repository.findAll()).thenReturn(todos);

		assertEquals(todos, service.listar());
	}

	@Test
	public void inserirLoteSalvaTodas() {
		List<Disciplina> lote = Arrays.asList(new Disciplina(), new Disciplina());
		service.inserirLote(lote);
		verify(repository).saveAll(lote);
	}
}
