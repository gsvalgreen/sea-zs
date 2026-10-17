package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.service.EventoService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.EventoController;

@RunWith(MockitoJUnitRunner.class)
public class EventoControllerTest {

	@Mock
	private EventoService eventoServ;

	@InjectMocks
	private EventoController controller;

	@Test
	public void inserirEventoDelegaParaOService() {
		Evento arg = new Evento();
		controller.inserirEvento(arg);
		verify(eventoServ).inserir(arg);
	}

	@Test
	public void buscarEventoDelegaParaOService() {
		Evento arg = new Evento();
		Evento esperado = new Evento();
		when(eventoServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarEvento(arg));
		verify(eventoServ).buscar(arg);
	}

	@Test
	public void alterarEventoDelegaParaOService() {
		Evento arg = new Evento();
		controller.alterarEvento(arg);
		verify(eventoServ).alterar(arg);
	}

	@Test
	public void excluirEventoDelegaParaOService() {
		Evento arg = new Evento();
		controller.excluirEvento(arg);
		verify(eventoServ).excluir(arg);
	}

	@Test
	public void listarEventoDelegaParaOService() {
		List<Evento> esperado = new ArrayList<>();
		when(eventoServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarEvento());
		verify(eventoServ).listar();
	}

	@Test
	public void listarEventoPorDtInicioDelegaParaOService() {
		List<Evento> esperado = new ArrayList<>();
		when(eventoServ.listarPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarEventoPorDtInicio());
		verify(eventoServ).listarPorDataAsc();
	}

	@Test
	public void listarEventoDisponivelPorDtInicioDelegaParaOService() {
		List<Evento> esperado = new ArrayList<>();
		when(eventoServ.listarDisponivelPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarEventoDisponivelPorDtInicio());
		verify(eventoServ).listarDisponivelPorDataAsc();
	}

	@Test
	public void inserirPreencheDataInclusaoAntesDeDelegar() {
		Evento evento = new Evento();
		controller.inserirEvento(evento);
		assertNotNull(evento.getDataInclusao());
		verify(eventoServ).inserir(evento);
	}
}
