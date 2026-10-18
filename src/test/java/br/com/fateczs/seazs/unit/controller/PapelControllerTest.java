package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.service.PapelService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fateczs.seazs.controller.PapelController;

@ExtendWith(MockitoExtension.class)
public class PapelControllerTest {

	@Mock
	private PapelService papelServ;

	@InjectMocks
	private PapelController controller;

	@Test
	public void inserirPapelDelegaParaOService() {
		Papel arg = new Papel();
		controller.inserirPapel(arg);
		verify(papelServ).inserir(arg);
	}

	@Test
	public void buscarPapelDelegaParaOService() {
		Papel arg = new Papel();
		Papel esperado = new Papel();
		when(papelServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarPapel(arg));
		verify(papelServ).buscar(arg);
	}

	@Test
	public void alterarPapelDelegaParaOService() {
		Papel arg = new Papel();
		controller.alterarPapel(arg);
		verify(papelServ).alterar(arg);
	}

	@Test
	public void excluirPapelDelegaParaOService() {
		Papel arg = new Papel();
		controller.excluirPapel(arg);
		verify(papelServ).excluir(arg);
	}

	@Test
	public void listarPapelDelegaParaOService() {
		List<Papel> esperado = new ArrayList<>();
		when(papelServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarPapel());
		verify(papelServ).listar();
	}
}
