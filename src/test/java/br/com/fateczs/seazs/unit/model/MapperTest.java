package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.CertificadoAtividade;
import br.com.fateczs.seazs.model.CertificadoStaff;
import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.mapper.CertificadoMapper;
import br.com.fateczs.seazs.model.mapper.CrachaUsuarioMapper;
import br.com.fateczs.seazs.utils.DataUtils;
import br.com.fateczs.seazs.utils.TipoUsuario;

public class MapperTest {

	private Date data(int dia, int hora) {
		Calendar c = new GregorianCalendar(DataUtils.TIME_ZONE);
		c.clear();
		c.set(2019, Calendar.MARCH, dia, hora, 0, 0);
		return c.getTime();
	}

	@Test
	public void certificadoAPartirDeInscricao() {
		Usuario usuario = new Usuario();
		usuario.setNome("Maria");
		Atividade atividade = new Atividade();
		atividade.setNome("Palestra");
		atividade.setCargaHoraria(4);
		Papel papel = new Papel();
		papel.setNome("Ouvinte");
		Inscricao inscricao = new Inscricao();
		inscricao.setUsuario(usuario);
		inscricao.setAtividade(atividade);
		inscricao.setPapel(papel);

		CertificadoAtividade certificado = new CertificadoMapper().createFromInscricao(inscricao);

		assertEquals("Maria", certificado.getNomeUsuario());
		assertEquals("Palestra", certificado.getAtividade());
		assertEquals("Ouvinte", certificado.getPapel());
		assertEquals(4, certificado.getCargaHorariaAtividade());
	}

	@Test
	public void certificadoAPartirDeStaffAgendamento() {
		Evento evento = new Evento();
		evento.setNome("Semana Tech");
		Agendamento agendamento = new Agendamento();
		agendamento.setEvento(evento);
		agendamento.setInicio(data(4, 8));
		agendamento.setFim(data(6, 8));
		StaffAgendamento staff = new StaffAgendamento();
		staff.setAgendamento(agendamento);

		CertificadoStaff certificado = new CertificadoMapper().createFromStaffAgendamento(staff);

		assertEquals("4/3/2019 a 6/3/2019", certificado.getPeriodoAgendamento());
		assertEquals("Semana Tech", certificado.getNomeEvento());
		assertEquals("Staff", certificado.getPapelUsuario());
		assertEquals(48, certificado.getCargaHoraria());
	}

	@Test
	public void crachaAPartirDeUsuario() {
		Usuario usuario = new Usuario();
		usuario.setNome("João");
		usuario.setTipoUsuario(TipoUsuario.PROFESSOR);

		CrachaUsuario cracha = new CrachaUsuarioMapper().createFrom(usuario);

		assertEquals("João", cracha.getNomeDoUsuario());
		assertEquals("PROFESSOR", cracha.getTipoDoUsuario());
	}
}
