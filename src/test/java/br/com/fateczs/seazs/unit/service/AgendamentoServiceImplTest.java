package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.repository.AgendamentoRepository;
import br.com.fateczs.seazs.service.serviceImpl.AgendamentoServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AgendamentoServiceImplTest {

	@Mock
	private AgendamentoRepository repository;

	@InjectMocks
	private AgendamentoServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Agendamento entrada = new Agendamento();
		entrada.setId(2);
		Agendamento esperado = new Agendamento();
		when(repository.getOne(2)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		Agendamento a = new Agendamento();
		List<Agendamento> todos = Arrays.asList(a);
		when(repository.findAll()).thenReturn(todos);

		service.inserir(a);
		service.alterar(a);
		service.excluir(a);

		verify(repository, times(2)).save(a);
		verify(repository).delete(a);
		assertEquals(todos, service.listar());
	}

	@Test
	public void listarPorDataAscOrdenaPorInicio() {
		List<Agendamento> todos = Arrays.asList(new Agendamento());
		when(repository.findAll(any(Sort.class))).thenReturn(todos);

		assertEquals(todos, service.listarPorDataAsc());

		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(repository).findAll(sort.capture());
		assertEquals(Sort.Direction.ASC, sort.getValue().getOrderFor("inicio").getDirection());
	}

	@Test
	public void listarPorEventoUsaIdDoEvento() {
		Evento evento = new Evento();
		evento.setId(9);
		List<Agendamento> doEvento = Arrays.asList(new Agendamento());
		when(repository.findByEventoId(9)).thenReturn(doEvento);
		assertEquals(doEvento, service.listarPorEvento(evento));
	}
}
