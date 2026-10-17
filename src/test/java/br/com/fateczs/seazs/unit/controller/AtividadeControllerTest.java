package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.AtividadeService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.AtividadeController;

@RunWith(MockitoJUnitRunner.class)
public class AtividadeControllerTest {

	@Mock
	private AtividadeService atividadeServ;

	@InjectMocks
	private AtividadeController controller;

	@Test
	public void inserirAtividadeDelegaParaOService() {
		Atividade arg = new Atividade();
		controller.inserirAtividade(arg);
		verify(atividadeServ).inserir(arg);
	}

	@Test
	public void buscarAtividadeDelegaParaOService() {
		Atividade arg = new Atividade();
		Atividade esperado = new Atividade();
		when(atividadeServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarAtividade(arg));
		verify(atividadeServ).buscar(arg);
	}

	@Test
	public void alterarAtividadeDelegaParaOService() {
		Atividade arg = new Atividade();
		controller.alterarAtividade(arg);
		verify(atividadeServ).alterar(arg);
	}

	@Test
	public void excluirAtividadeDelegaParaOService() {
		Atividade arg = new Atividade();
		controller.excluirAtividade(arg);
		verify(atividadeServ).excluir(arg);
	}

	@Test
	public void listarAtividadeDelegaParaOService() {
		List<Atividade> esperado = new ArrayList<>();
		when(atividadeServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividade());
		verify(atividadeServ).listar();
	}

	@Test
	public void listarAtividadePorDtInicioDelegaParaOService() {
		List<Atividade> esperado = new ArrayList<>();
		when(atividadeServ.listarPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadePorDtInicio());
		verify(atividadeServ).listarPorDataAsc();
	}

	@Test
	public void listarAtividadePorAgendamentoDelegaParaOService() {
		Agendamento arg = new Agendamento();
		List<Atividade> esperado = new ArrayList<>();
		when(atividadeServ.listarPorAgendamento(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadePorAgendamento(arg));
		verify(atividadeServ).listarPorAgendamento(arg);
	}

	@Test
	public void listarAtividadesDisponiveisDelegaParaOService() {
		Usuario arg = new Usuario();
		List<Atividade> esperado = new ArrayList<>();
		when(atividadeServ.listarDisponiveisParaInscricao(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadesDisponiveis(arg));
		verify(atividadeServ).listarDisponiveisParaInscricao(arg);
	}

	@Test
	public void listarAtividadesPassadasDelegaParaOService() {
		List<Atividade> esperado = new ArrayList<>();
		when(atividadeServ.listarPassadas()).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadesPassadas());
		verify(atividadeServ).listarPassadas();
	}
}
