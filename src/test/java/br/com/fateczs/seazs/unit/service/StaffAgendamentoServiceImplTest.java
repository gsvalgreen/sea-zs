package br.com.fateczs.seazs.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;
import br.com.fateczs.seazs.service.serviceImpl.StaffAgendamentoServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class StaffAgendamentoServiceImplTest {

	private static final long MINUTO = 60 * 1000L;

	@Mock
	private StaffAgendamentoRepository repository;

	@InjectMocks
	private StaffAgendamentoServiceImpl service;

	private static final Instant AGORA = Instant.parse("2019-03-15T15:00:00Z");

	@Before
	public void fixaRelogio() {
		ReflectionTestUtils.setField(service, "clock", Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")));
	}

	private StaffAgendamento requisicao() {
		Agendamento agendamento = new Agendamento();
		agendamento.setId(1);
		Usuario usuario = new Usuario();
		usuario.setId(2);
		StaffAgendamento staff = new StaffAgendamento();
		staff.setAgendamento(agendamento);
		staff.setUsuario(usuario);
		return staff;
	}

	private StaffAgendamento persistido(long inicioOffsetMin, long fimOffsetMin) {
		long agora = AGORA.toEpochMilli();
		Agendamento agendamento = new Agendamento();
		agendamento.setInicio(new Date(agora + inicioOffsetMin * MINUTO));
		agendamento.setFim(new Date(agora + fimOffsetMin * MINUTO));
		agendamento.setPontuacaoStaff(5);
		StaffAgendamento staff = new StaffAgendamento();
		staff.setAgendamento(agendamento);
		staff.setCheckIn(false);
		staff.setCheckOut(false);
		staff.setPontuacaoRecebida(0);
		return staff;
	}

	@Test
	public void buscarUsaSequencialDoStaff() {
		StaffAgendamento entrada = new StaffAgendamento();
		entrada.setSequencialStaff(11);
		StaffAgendamento esperado = new StaffAgendamento();
		when(repository.getOne(11)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		StaffAgendamento s = new StaffAgendamento();
		List<StaffAgendamento> todos = Arrays.asList(s);
		when(repository.findAll()).thenReturn(todos);

		service.inserir(s);
		service.alterar(s);
		service.excluir(s);

		verify(repository, times(2)).save(s);
		verify(repository).delete(s);
		assertEquals(todos, service.listar());
	}

	@Test
	public void listarPorDataAscOrdenaPorDtInscricao() {
		List<StaffAgendamento> todos = Arrays.asList(new StaffAgendamento());
		when(repository.findAll(any(Sort.class))).thenReturn(todos);
		assertEquals(todos, service.listarPorDataAsc());
	}

	@Test
	public void validarPresencaDentroDaJanelaRegistraCheckInCheckOutEPontos() {
		StaffAgendamento staff = persistido(-5, 60);
		when(repository.findByAgendamentoIdAndUsuarioId(1, 2)).thenReturn(staff);
		when(repository.save(staff)).thenReturn(staff);

		StaffAgendamento resultado = service.validarPresencaStaff(requisicao());

		assertTrue(resultado.getCheckIn());
		assertTrue(resultado.getCheckOut());
		assertEquals(Integer.valueOf(5), resultado.getPontuacaoRecebida());
		verify(repository).save(staff);
	}

	@Test
	public void validarPresencaAntesDaJanelaNaoRegistra() {
		StaffAgendamento staff = persistido(30, 90);
		when(repository.findByAgendamentoIdAndUsuarioId(1, 2)).thenReturn(staff);

		StaffAgendamento resultado = service.validarPresencaStaff(requisicao());

		assertFalse(resultado.getCheckIn());
		assertEquals(Integer.valueOf(0), resultado.getPontuacaoRecebida());
		verify(repository, never()).save(any(StaffAgendamento.class));
	}

	@Test
	public void validarPresencaAposAJanelaNaoRegistra() {
		StaffAgendamento staff = persistido(-120, -60);
		when(repository.findByAgendamentoIdAndUsuarioId(1, 2)).thenReturn(staff);

		StaffAgendamento resultado = service.validarPresencaStaff(requisicao());

		assertFalse(resultado.getCheckOut());
		verify(repository, never()).save(any(StaffAgendamento.class));
	}

	@Test
	public void listarAgendamentosDoUsuario() {
		Usuario usuario = new Usuario();
		usuario.setId(2);
		List<Agendamento> futuros = Arrays.asList(new Agendamento());
		List<Agendamento> passados = Arrays.asList(new Agendamento(), new Agendamento());
		when(repository.findAgendamentosInscrito(eq(2), any(Sort.class))).thenReturn(futuros);
		when(repository.findAgendamentosInscritoPassado(eq(2), any(Sort.class))).thenReturn(passados);

		assertEquals(futuros, service.listarAgendamentosUsuario(usuario));
		assertEquals(passados, service.listarAgendamentosUsuarioPassado(usuario));
	}
}
