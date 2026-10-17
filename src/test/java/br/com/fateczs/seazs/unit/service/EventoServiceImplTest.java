package br.com.fateczs.seazs.unit.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.repository.EventoRepository;
import br.com.fateczs.seazs.service.serviceImpl.EventoServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class EventoServiceImplTest {

	@Mock
	private EventoRepository repository;

	@InjectMocks
	private EventoServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Evento entrada = new Evento();
		entrada.setId(4);
		Evento esperado = new Evento();
		when(repository.getOne(4)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		Evento e = new Evento();
		List<Evento> todos = Arrays.asList(e);
		when(repository.findAll()).thenReturn(todos);

		service.inserir(e);
		service.alterar(e);
		service.excluir(e);

		verify(repository, org.mockito.Mockito.times(2)).save(e);
		verify(repository).delete(e);
		assertEquals(todos, service.listar());
	}

	@Test
	public void listarPorDataAscOrdenaPorInicio() {
		List<Evento> todos = Arrays.asList(new Evento());
		when(repository.findAll(any(Sort.class))).thenReturn(todos);

		assertEquals(todos, service.listarPorDataAsc());

		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(repository).findAll(sort.capture());
		assertEquals(Sort.Direction.ASC, sort.getValue().getOrderFor("inicio").getDirection());
	}

	@Test
	public void listarDisponivelUsaConsultaDeFimAposHoje() {
		List<Evento> disponiveis = Arrays.asList(new Evento());
		when(repository.findByFimAfterToday(any(Sort.class))).thenReturn(disponiveis);

		assertEquals(disponiveis, service.listarDisponivelPorDataAsc());
	}
}
