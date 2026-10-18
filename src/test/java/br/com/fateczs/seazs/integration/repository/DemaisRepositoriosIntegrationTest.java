package br.com.fateczs.seazs.integration.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static br.com.fateczs.seazs.integration.support.DadosTeste.FUTURO;
import static br.com.fateczs.seazs.integration.support.DadosTeste.PASSADO;
import static br.com.fateczs.seazs.integration.support.DadosTeste.agendamento;
import static br.com.fateczs.seazs.integration.support.DadosTeste.data;
import static br.com.fateczs.seazs.integration.support.DadosTeste.evento;
import static br.com.fateczs.seazs.integration.support.DadosTeste.staff;
import static br.com.fateczs.seazs.integration.support.DadosTeste.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.integration.support.RepositorioIntegracaoBase;
import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.AgendamentoRepository;
import br.com.fateczs.seazs.repository.EventoRepository;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;

/** Repositorios menores: Usuario, Evento, Agendamento e StaffAgendamento. */
public class DemaisRepositoriosIntegrationTest extends RepositorioIntegracaoBase {

	private static final Sort POR_INICIO = Sort.by(Sort.Direction.ASC, "inicio");

	@Autowired
	private TestEntityManager em;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private EventoRepository eventoRepository;
	@Autowired
	private AgendamentoRepository agendamentoRepository;
	@Autowired
	private StaffAgendamentoRepository staffRepository;

	private Usuario ana;

	@BeforeEach
	public void prepararCenarioBase() {
		ana = em.persist(usuario("ana@teste.com"));
	}

	// ---- UsuarioRepository

	@Test
	public void findByEmail_retornaUsuarioPeloEmail() {
		em.flush();
		em.clear();

		assertEquals(ana.getId(), usuarioRepository.findByEmail("ana@teste.com").get().getId());
	}

	@Test
	public void findAdministradoresParaAtualizacao_devolveSoQuemTemAdm() {
		Usuario admin = usuario("admin@teste.com");
		admin.setAdm(true);
		em.persist(admin);
		em.flush();
		em.clear();

		List<Usuario> admins = usuarioRepository.findAdministradoresParaAtualizacao();

		assertEquals(Arrays.asList(admin.getId()), admins.stream().map(Usuario::getId).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	public void findByEmail_devolveVazioQuandoEmailNaoExiste() {
		assertFalse(usuarioRepository.findByEmail("nao-existe@teste.com").isPresent());
	}

	@Test
	public void findByNome_retornaUsuariosComOMesmoNome() {
		Usuario homonimo = usuario("outro@teste.com");
		homonimo.setNome(ana.getNome());
		em.persist(homonimo);

		assertEquals(2, usuarioRepository.findByNome(ana.getNome()).size());
	}

	@Test
	public void saveAndFlush_rejeitaEmailDuplicado() {
		assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(usuario("ana@teste.com")));
	}

	// ---- EventoRepository

	@Test
	public void findByFimAfterToday_retornaSomenteEventosNaoEncerradosOrdenadosPorInicio() {
		Evento encerrado = evento("encerrado", ana);
		encerrado.setFim(PASSADO);
		Evento futuroTarde = evento("futuro-tarde", ana);
		futuroTarde.setInicio(data(2998, Calendar.MARCH, 1, 10));
		Evento futuroCedo = evento("futuro-cedo", ana);
		futuroCedo.setInicio(data(2998, Calendar.FEBRUARY, 1, 10));
		em.persist(encerrado);
		em.persist(futuroTarde);
		em.persist(futuroCedo);
		em.flush();
		em.clear();

		List<Evento> disponiveis = eventoRepository.findByFimAfterToday(POR_INICIO);

		assertEquals(Arrays.asList("futuro-cedo", "futuro-tarde"),
				Arrays.asList(disponiveis.get(0).getNome(), disponiveis.get(1).getNome()));
	}

	// ---- AgendamentoRepository

	@Test
	public void findByEventoId_retornaSomenteAgendamentosDoEvento() {
		Evento e1 = em.persist(evento("e1", ana));
		Evento e2 = em.persist(evento("e2", ana));
		em.persist(agendamento(e1, ana, PASSADO, FUTURO));
		em.persist(agendamento(e1, ana, PASSADO, FUTURO));
		em.persist(agendamento(e2, ana, PASSADO, FUTURO));
		em.flush();
		em.clear();

		assertEquals(2, agendamentoRepository.findByEventoId(e1.getId()).size());
	}

	// ---- StaffAgendamentoRepository

	private Agendamento novoAgendamento(String evento) {
		return em.persist(agendamento(em.persist(evento(evento, ana)), ana, PASSADO, FUTURO));
	}

	@Test
	public void findByAgendamentoIdAndUsuarioId_retornaOStaffDoPar() {
		Agendamento ag = novoAgendamento("e1");
		StaffAgendamento esperado = em.persist(staff(ag, ana, PASSADO, false, false, 0));
		em.flush();
		em.clear();

		StaffAgendamento encontrado = staffRepository.findByAgendamentoIdAndUsuarioId(ag.getId(), ana.getId());

		assertEquals(esperado.getSequencialStaff(), encontrado.getSequencialStaff());
	}

	@Test
	public void findAgendamentosInscrito_retornaSomenteAgendamentosNaoEncerradosDoUsuario() {
		Agendamento futuro = em.persist(agendamento(em.persist(evento("e1", ana)), ana, PASSADO, FUTURO));
		Agendamento encerrado = em.persist(agendamento(em.persist(evento("e2", ana)), ana, PASSADO, data(2000, Calendar.JANUARY, 11, 10)));
		em.persist(staff(futuro, ana, PASSADO, false, false, 0));
		em.persist(staff(encerrado, ana, PASSADO, true, true, 20));
		em.flush();
		em.clear();

		List<Agendamento> resultado = staffRepository.findAgendamentosInscrito(ana.getId(),
				Sort.by(Sort.Direction.ASC, "agendamento.inicio"));

		assertEquals(Arrays.asList(futuro.getId()), Arrays.asList(resultado.get(0).getId()));
	}

	@Test
	public void findAgendamentosInscritoPassado_retornaSomenteEncerradosComCheckInECheckOut() {
		Agendamento encerradoCompleto = em.persist(agendamento(em.persist(evento("e1", ana)), ana, PASSADO, data(2000, Calendar.JANUARY, 11, 10)));
		Agendamento encerradoIncompleto = em.persist(agendamento(em.persist(evento("e2", ana)), ana, PASSADO, data(2000, Calendar.JANUARY, 12, 10)));
		em.persist(staff(encerradoCompleto, ana, PASSADO, true, true, 20));
		em.persist(staff(encerradoIncompleto, ana, PASSADO, true, false, 0));
		em.flush();
		em.clear();

		List<Agendamento> resultado = staffRepository.findAgendamentosInscritoPassado(ana.getId(),
				Sort.by(Sort.Direction.ASC, "agendamento.inicio"));

		assertEquals(Arrays.asList(encerradoCompleto.getId()), Arrays.asList(resultado.get(0).getId()));
	}
}
