package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Usuario;
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
		when(repository.getReferenceById(2)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		Agendamento a = new Agendamento();
		a.setId(1);
		List<Agendamento> todos = Arrays.asList(a);
		when(repository.findAll()).thenReturn(todos);
		when(repository.findById(1)).thenReturn(Optional.of(new Agendamento()));

		service.inserir(a);
		service.alterar(a);
		service.excluir(a);

		verify(repository, times(2)).save(a);
		verify(repository).delete(a);
		assertEquals(todos, service.listar());
	}

	@Test
	public void alterarPreservaOOrganizadorGravadoEIgnoraODoCorpo() {
		Usuario gravado = new Usuario();
		gravado.setId(5);
		Agendamento noBanco = new Agendamento();
		noBanco.setId(1);
		noBanco.setUsuarioOrganizador(gravado);
		when(repository.findById(1)).thenReturn(Optional.of(noBanco));
		Usuario falso = new Usuario();
		falso.setId(99);
		Agendamento corpo = new Agendamento();
		corpo.setId(1);
		corpo.setUsuarioOrganizador(falso);

		service.alterar(corpo);

		ArgumentCaptor<Agendamento> salvo = ArgumentCaptor.forClass(Agendamento.class);
		verify(repository).save(salvo.capture());
		assertSame(gravado, salvo.getValue().getUsuarioOrganizador());
	}

	@Test
	public void alterarAgendamentoInexistenteOuSemIdLancaEntityNotFoundSemSalvar() {
		Agendamento semId = new Agendamento();
		Agendamento inexistente = new Agendamento();
		inexistente.setId(7);
		when(repository.findById(7)).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class, () -> service.alterar(semId));
		assertThrows(EntityNotFoundException.class, () -> service.alterar(inexistente));

		org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any(Agendamento.class));
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
