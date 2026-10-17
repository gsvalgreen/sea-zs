package br.com.fateczs.seazs.unit.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.CertificadoAtividade;
import br.com.fateczs.seazs.model.CertificadoStaff;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.service.CertificadoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import br.com.fateczs.seazs.controller.CertificadoController;

@RunWith(MockitoJUnitRunner.class)
public class CertificadoControllerTest {

	@Mock
	private CertificadoService certificadoService;

	@InjectMocks
	private CertificadoController controller;

	@Test
	public void emitirCertificadoAtividadeDelegaParaOService() {
		Inscricao arg = new Inscricao();
		CertificadoAtividade esperado = new CertificadoAtividade("Maria", "Oficina", "Ouvinte", 2);
		when(certificadoService.emiteCertificadoAtividade(arg)).thenReturn(esperado);
		assertSame(esperado, controller.emitirCertificadoAtividade(arg));
		verify(certificadoService).emiteCertificadoAtividade(arg);
	}

	@Test
	public void emitirCertificadoAgendamentoDelegaParaOService() {
		StaffAgendamento arg = new StaffAgendamento();
		CertificadoStaff esperado = new CertificadoStaff("1/3/2019 a 2/3/2019", "Semana Tech", "Staff", 24);
		when(certificadoService.emiteCertificadoAgendamento(arg)).thenReturn(esperado);
		assertSame(esperado, controller.emitirCertificadoAgendamento(arg));
		verify(certificadoService).emiteCertificadoAgendamento(arg);
	}
}
