package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.security.Autorizacao;
import br.com.fateczs.seazs.service.AgendamentoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
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

	@Mock
	private Autorizacao autorizacao;

	@InjectMocks
	private AgendamentoController controller;

	@Test
	public void inserirAgendamentoDelegaParaOService() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.of(5));
		Agendamento arg = new Agendamento();
		controller.inserirAgendamento(arg);
		verify(agendamentoServ).inserir(arg);
	}

	@Test
	public void inserirSobrescreveOOrganizadorDoCorpoComOUsuarioAutenticado() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.of(5));
		Usuario falso = new Usuario();
		falso.setId(99);
		Agendamento arg = new Agendamento();
		arg.setUsuarioOrganizador(falso);

		controller.inserirAgendamento(arg);

		assertEquals(Integer.valueOf(5), arg.getUsuarioOrganizador().getId());
		verify(agendamentoServ).inserir(arg);
	}

	@Test
	public void inserirZeraOIdDoCorpoParaSempreCriar() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.of(5));
		Agendamento arg = new Agendamento();
		arg.setId(42);

		controller.inserirAgendamento(arg);

		assertNull(arg.getId());
		verify(agendamentoServ).inserir(arg);
	}

	@Test
	public void inserirSemUsuarioAutenticadoNegaSemChamarOService() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.empty());

		assertThrows(AccessDeniedException.class, () -> controller.inserirAgendamento(new Agendamento()));

		verifyNoInteractions(agendamentoServ);
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
