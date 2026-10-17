package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.StaffAgendamentoService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.StaffAgendamentoController;

@RunWith(MockitoJUnitRunner.class)
public class StaffAgendamentoControllerTest {

	@Mock
	private StaffAgendamentoService staffServ;

	@InjectMocks
	private StaffAgendamentoController controller;

	@Test
	public void inserirInscricaoDelegaParaOService() {
		StaffAgendamento arg = new StaffAgendamento();
		controller.inserirInscricao(arg);
		verify(staffServ).inserir(arg);
	}

	@Test
	public void buscarInscricaoDelegaParaOService() {
		StaffAgendamento arg = new StaffAgendamento();
		StaffAgendamento esperado = new StaffAgendamento();
		when(staffServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarInscricao(arg));
		verify(staffServ).buscar(arg);
	}

	@Test
	public void alterarInscricaoDelegaParaOService() {
		StaffAgendamento arg = new StaffAgendamento();
		controller.alterarInscricao(arg);
		verify(staffServ).alterar(arg);
	}

	@Test
	public void excluirInscricaoDelegaParaOService() {
		StaffAgendamento arg = new StaffAgendamento();
		controller.excluirInscricao(arg);
		verify(staffServ).excluir(arg);
	}

	@Test
	public void listarPapelDelegaParaOService() {
		List<StaffAgendamento> esperado = new ArrayList<>();
		when(staffServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarPapel());
		verify(staffServ).listar();
	}

	@Test
	public void listarAtividadePorDtInicioDelegaParaOService() {
		List<StaffAgendamento> esperado = new ArrayList<>();
		when(staffServ.listarPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadePorDtInicio());
		verify(staffServ).listarPorDataAsc();
	}

	@Test
	public void validarPresencaDelegaParaOService() {
		StaffAgendamento arg = new StaffAgendamento();
		arg.setAgendamento(new Agendamento());
		StaffAgendamento esperado = new StaffAgendamento();
		when(staffServ.validarPresencaStaff(arg)).thenReturn(esperado);
		assertSame(esperado, controller.validarPresenca(arg));
		verify(staffServ).validarPresencaStaff(arg);
	}

	@Test
	public void listarAgendamentosUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		List<Agendamento> esperado = new ArrayList<>();
		when(staffServ.listarAgendamentosUsuario(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAgendamentosUsuario(arg));
		verify(staffServ).listarAgendamentosUsuario(arg);
	}

	@Test
	public void listarAgendamentosUsuarioPassadoDelegaParaOService() {
		Usuario arg = new Usuario();
		List<Agendamento> esperado = new ArrayList<>();
		when(staffServ.listarAgendamentosUsuarioPassado(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAgendamentosUsuarioPassado(arg));
		verify(staffServ).listarAgendamentosUsuarioPassado(arg);
	}
}
