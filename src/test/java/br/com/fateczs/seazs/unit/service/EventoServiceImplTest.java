package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
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

import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.EventoRepository;
import br.com.fateczs.seazs.service.serviceImpl.EventoServiceImpl;

@ExtendWith(MockitoExtension.class)
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
		when(repository.getReferenceById(4)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		Evento e = new Evento();
		e.setId(1);
		List<Evento> todos = Arrays.asList(e);
		when(repository.findAll()).thenReturn(todos);
		when(repository.findById(1)).thenReturn(Optional.of(new Evento()));

		service.inserir(e);
		service.alterar(e);
		service.excluir(e);

		verify(repository, org.mockito.Mockito.times(2)).save(e);
		verify(repository).delete(e);
		assertEquals(todos, service.listar());
	}

	@Test
	public void alterarPreservaOCriadorGravadoEIgnoraOdoCorpo() {
		Usuario gravado = new Usuario();
		gravado.setId(5);
		Evento noBanco = new Evento();
		noBanco.setId(1);
		noBanco.setUsuarioCriador(gravado);
		Date inclusao = new Date(1000L);
		noBanco.setDataInclusao(inclusao);
		when(repository.findById(1)).thenReturn(Optional.of(noBanco));
		Usuario falso = new Usuario();
		falso.setId(99);
		Evento corpo = new Evento();
		corpo.setId(1);
		corpo.setUsuarioCriador(falso);
		corpo.setDataInclusao(new Date(5000L));

		service.alterar(corpo);

		ArgumentCaptor<Evento> salvo = ArgumentCaptor.forClass(Evento.class);
		verify(repository).save(salvo.capture());
		assertSame(gravado, salvo.getValue().getUsuarioCriador());
		assertSame(inclusao, salvo.getValue().getDataInclusao());
	}

	@Test
	public void alterarEventoInexistenteOuSemIdLancaEntityNotFoundSemSalvar() {
		Evento semId = new Evento();
		Evento inexistente = new Evento();
		inexistente.setId(7);
		when(repository.findById(7)).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class, () -> service.alterar(semId));
		assertThrows(EntityNotFoundException.class, () -> service.alterar(inexistente));

		verify(repository, never()).save(any(Evento.class));
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
