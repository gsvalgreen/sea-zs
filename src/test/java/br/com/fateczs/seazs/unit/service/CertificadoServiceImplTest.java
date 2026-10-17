package br.com.fateczs.seazs.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.CertificadoAtividade;
import br.com.fateczs.seazs.model.CertificadoStaff;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;
import br.com.fateczs.seazs.service.serviceImpl.CertificadoServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class CertificadoServiceImplTest {

	@Mock
	private InscricaoRepository inscricaoRepository;

	@Mock
	private StaffAgendamentoRepository staffAgendamentoRepository;

	@InjectMocks
	private CertificadoServiceImpl service;

	private Inscricao requisicaoInscricao() {
		Usuario usuario = new Usuario();
		usuario.setId(1);
		Atividade atividade = new Atividade();
		atividade.setId(2);
		Inscricao inscricao = new Inscricao();
		inscricao.setUsuario(usuario);
		inscricao.setAtividade(atividade);
		return inscricao;
	}

	private Inscricao inscricaoPersistida(boolean checkIn, boolean checkOut) {
		Usuario usuario = new Usuario();
		usuario.setNome("Maria");
		Atividade atividade = new Atividade();
		atividade.setNome("Oficina");
		atividade.setCargaHoraria(2);
		atividade.setFlagContinua(false);
		Papel papel = new Papel();
		papel.setNome("Ouvinte");
		Inscricao inscricao = new Inscricao();
		inscricao.setUsuario(usuario);
		inscricao.setAtividade(atividade);
		inscricao.setPapel(papel);
		inscricao.setCheckIn(checkIn);
		inscricao.setCheckOut(checkOut);
		return inscricao;
	}

	@Test
	public void emiteCertificadoDeAtividadeQuandoValidouPresenca() {
		when(inscricaoRepository.findByUsuarioIdAndAtividadeId(1, 2)).thenReturn(inscricaoPersistida(true, true));

		CertificadoAtividade certificado = service.emiteCertificadoAtividade(requisicaoInscricao());

		assertNotNull(certificado);
		assertEquals("Maria", certificado.getNomeUsuario());
		assertEquals("Oficina", certificado.getAtividade());
		assertEquals("Ouvinte", certificado.getPapel());
		assertEquals(2, certificado.getCargaHorariaAtividade());
	}

	@Test
	public void naoEmiteCertificadoDeAtividadeSemCheckOut() {
		when(inscricaoRepository.findByUsuarioIdAndAtividadeId(1, 2)).thenReturn(inscricaoPersistida(true, false));

		assertNull(service.emiteCertificadoAtividade(requisicaoInscricao()));
	}

	private Date data(int dia) {
		Calendar c = new GregorianCalendar();
		c.clear();
		c.set(2019, Calendar.MARCH, dia, 8, 0, 0);
		return c.getTime();
	}

	private StaffAgendamento requisicaoStaff() {
		Agendamento agendamento = new Agendamento();
		agendamento.setId(3);
		Usuario usuario = new Usuario();
		usuario.setId(4);
		StaffAgendamento staff = new StaffAgendamento();
		staff.setAgendamento(agendamento);
		staff.setUsuario(usuario);
		return staff;
	}

	private StaffAgendamento staffPersistido(boolean checkIn, boolean checkOut) {
		Evento evento = new Evento();
		evento.setNome("Semana Tech");
		Agendamento agendamento = new Agendamento();
		agendamento.setEvento(evento);
		agendamento.setInicio(data(4));
		agendamento.setFim(data(5));
		StaffAgendamento staff = new StaffAgendamento();
		staff.setAgendamento(agendamento);
		staff.setCheckIn(checkIn);
		staff.setCheckOut(checkOut);
		return staff;
	}

	@Test
	public void emiteCertificadoDeStaffQuandoValidouPresenca() {
		when(staffAgendamentoRepository.findByAgendamentoIdAndUsuarioId(3, 4)).thenReturn(staffPersistido(true, true));

		CertificadoStaff certificado = service.emiteCertificadoAgendamento(requisicaoStaff());

		assertNotNull(certificado);
		assertEquals("Semana Tech", certificado.getNomeEvento());
		assertEquals("Staff", certificado.getPapelUsuario());
		assertEquals(24, certificado.getCargaHoraria());
	}

	@Test
	public void naoEmiteCertificadoDeStaffSemPresenca() {
		when(staffAgendamentoRepository.findByAgendamentoIdAndUsuarioId(3, 4)).thenReturn(staffPersistido(false, false));

		assertNull(service.emiteCertificadoAgendamento(requisicaoStaff()));
	}
}
