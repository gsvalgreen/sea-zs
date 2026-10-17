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

import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.repository.PapelRepository;
import br.com.fateczs.seazs.service.serviceImpl.PapelServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class PapelServiceImplTest {

	@Mock
	private PapelRepository repository;

	@InjectMocks
	private PapelServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Papel entidade = new Papel();
		entidade.setId(7);
		Papel esperado = new Papel();
		when(repository.getOne(7)).thenReturn(esperado);

		assertEquals(esperado, service.buscar(entidade));
	}

	@Test
	public void inserirSalva() {
		Papel entidade = new Papel();
		service.inserir(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void alterarSalva() {
		Papel entidade = new Papel();
		service.alterar(entidade);
		verify(repository).save(entidade);
	}

	@Test
	public void excluirRemove() {
		Papel entidade = new Papel();
		service.excluir(entidade);
		verify(repository).delete(entidade);
	}

	@Test
	public void listarRetornaTodos() {
		List<Papel> todos = Arrays.asList(new Papel(), new Papel());
		when(repository.findAll()).thenReturn(todos);

		assertEquals(todos, service.listar());
	}
}
