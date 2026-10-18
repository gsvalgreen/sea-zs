package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.RelatorioPontuacao;
import br.com.fateczs.seazs.service.serviceImpl.GerenciamentoServiceImpl;

/**
 * Testes de caracterização: o repositório de gerenciamento ainda é um stub
 * (WIP, retorna null), então o relatório sai sem linhas nem rodapé. Quando as
 * consultas forem implementadas, estes testes devem ser substituídos.
 */
public class GerenciamentoServiceImplTest {

	private final GerenciamentoServiceImpl service = new GerenciamentoServiceImpl();

	@Test
	public void caracterizacaoStubConsultaPrimeiroSemestreRetornaRelatorioSemDados() {
		RelatorioPontuacao relatorio = service.consultaPontuacaoSemestre(2019, 1);
		assertNotNull(relatorio);
		assertNull(relatorio.getLinha());
		assertNull(relatorio.getRodapeRelatorioPontuacao());
	}

	@Test
	public void caracterizacaoStubConsultaSegundoSemestreRetornaRelatorioSemDados() {
		RelatorioPontuacao relatorio = service.consultaPontuacaoSemestre(2019, 2);
		assertNotNull(relatorio);
		assertNull(relatorio.getLinha());
		assertNull(relatorio.getRodapeRelatorioPontuacao());
	}
}
