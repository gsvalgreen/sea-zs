package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.service.AgendamentoService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fateczs.seazs.controller.AgendamentoController;

@ExtendWith(MockitoExtension.class)
public class AgendamentoControllerTest {

	@Mock
	private AgendamentoService agendamentoServ;

	@InjectMocks
	private AgendamentoController controller;

	@Test
	public void inserirAgendamentoDelegaParaOService() {
		Agendamento arg = new Agendamento();
		controller.inserirAgendamento(arg);
		verify(agendamentoServ).inserir(arg);
	}

	@Test
	public void buscarAgendamentoDelegaParaOService() {
		Agendamento arg = new Agendamento();
		Agendamento esperado = new Agendamento();
		when(agendamentoServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarAgendamento(arg));
		verify(agendamentoServ).buscar(arg);
	}

	@Test
	public void alterarAgendamentoDelegaParaOService() {
		Agendamento arg = new Agendamento();
		controller.alterarAgendamento(arg);
		verify(agendamentoServ).alterar(arg);
	}

	@Test
	public void excluirAgendamentoDelegaParaOService() {
		Agendamento arg = new Agendamento();
		controller.excluirAgendamento(arg);
		verify(agendamentoServ).excluir(arg);
	}

	@Test
	public void listarAgendamentoDelegaParaOService() {
		List<Agendamento> esperado = new ArrayList<>();
		when(agendamentoServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarAgendamento());
		verify(agendamentoServ).listar();
	}

	@Test
	public void listarAgendamentoPorDtInicioDelegaParaOService() {
		List<Agendamento> esperado = new ArrayList<>();
		when(agendamentoServ.listarPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.lsitarAgendamentoPorDtinicio());
		verify(agendamentoServ).listarPorDataAsc();
	}

	@Test
	public void listarAgendamentoDoEventoDelegaParaOService() {
		Evento arg = new Evento();
		List<Agendamento> esperado = new ArrayList<>();
		when(agendamentoServ.listarPorEvento(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAgendamentoDoEvento(arg));
		verify(agendamentoServ).listarPorEvento(arg);
	}
}
