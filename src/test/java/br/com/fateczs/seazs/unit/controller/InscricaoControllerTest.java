package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.InscricaoService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.InscricaoController;

@RunWith(MockitoJUnitRunner.class)
public class InscricaoControllerTest {

	@Mock
	private InscricaoService inscricaoServ;

	@InjectMocks
	private InscricaoController controller;

	@Test
	public void inserirInscricaoDelegaParaOService() {
		Inscricao arg = new Inscricao();
		controller.inserirInscricao(arg);
		verify(inscricaoServ).inserir(arg);
	}

	@Test
	public void buscarInscricaoDelegaParaOService() {
		Inscricao arg = new Inscricao();
		Inscricao esperado = new Inscricao();
		when(inscricaoServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarInscricao(arg));
		verify(inscricaoServ).buscar(arg);
	}

	@Test
	public void alterarInscricaoDelegaParaOService() {
		Inscricao arg = new Inscricao();
		controller.alterarInscricao(arg);
		verify(inscricaoServ).alterar(arg);
	}

	@Test
	public void excluirInscricaoDelegaParaOService() {
		Inscricao arg = new Inscricao();
		controller.excluirInscricao(arg);
		verify(inscricaoServ).excluir(arg);
	}

	@Test
	public void listarInscricaoDelegaParaOService() {
		List<Inscricao> esperado = new ArrayList<>();
		when(inscricaoServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarInscricao());
		verify(inscricaoServ).listar();
	}

	@Test
	public void listarAtividadePorDtInicioDelegaParaOService() {
		List<Inscricao> esperado = new ArrayList<>();
		when(inscricaoServ.listarPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadePorDtInicio());
		verify(inscricaoServ).listarPorDataAsc();
	}

	@Test
	public void listarAtividadesInscritoDelegaParaOService() {
		Usuario arg = new Usuario();
		List<Atividade> esperado = new ArrayList<>();
		when(inscricaoServ.listarAtividadesInscrito(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadesInscrito(arg));
		verify(inscricaoServ).listarAtividadesInscrito(arg);
	}

	@Test
	public void listarAtividadesParticipouDelegaParaOService() {
		Usuario arg = new Usuario();
		List<Atividade> esperado = new ArrayList<>();
		when(inscricaoServ.listarAtividadesInscritoPassado(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarAtividadesParticipou(arg));
		verify(inscricaoServ).listarAtividadesInscritoPassado(arg);
	}

	@Test
	public void validaPresencaDelegaParaOService() {
		Inscricao arg = new Inscricao();
		Inscricao esperado = new Inscricao();
		when(inscricaoServ.validaPresenca(arg)).thenReturn(esperado);
		assertSame(esperado, controller.validaPresenca(arg));
		verify(inscricaoServ).validaPresenca(arg);
	}

	@Test
	public void listarPorAtividadeDelegaParaOService() {
		Atividade arg = new Atividade();
		List<Inscricao> esperado = new ArrayList<>();
		when(inscricaoServ.listarPorAtividade(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarPorAtividade(arg));
		verify(inscricaoServ).listarPorAtividade(arg);
	}

	@Test
	public void listarPorUsuarioDelegaParaOService() {
		Usuario arg = new Usuario();
		List<Inscricao> esperado = new ArrayList<>();
		when(inscricaoServ.listarPorUsuario(arg)).thenReturn(esperado);
		assertSame(esperado, controller.listarPorUsuario(arg));
		verify(inscricaoServ).listarPorUsuario(arg);
	}

	@Test
	public void somarTotalDePontosDelegaParaOService() {
		Usuario arg = new Usuario();
		Integer esperado = Integer.valueOf(3);
		when(inscricaoServ.somaTotalDePontos(arg)).thenReturn(esperado);
		assertSame(esperado, controller.somarTotalDePontos(arg));
		verify(inscricaoServ).somaTotalDePontos(arg);
	}

	@Test
	public void somarTotalDePontosSemestreDelegaParaOService() {
		Usuario arg = new Usuario();
		Integer esperado = Integer.valueOf(3);
		when(inscricaoServ.somaTotalDePontosNoSemestre(arg)).thenReturn(esperado);
		assertSame(esperado, controller.somarTotalDePontosSemestre(arg));
		verify(inscricaoServ).somaTotalDePontosNoSemestre(arg);
	}
}
