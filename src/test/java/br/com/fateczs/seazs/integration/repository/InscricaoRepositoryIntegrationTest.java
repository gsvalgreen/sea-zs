package br.com.fateczs.seazs.integration.repository;

import static br.com.fateczs.seazs.integration.support.DadosTeste.FUTURO;
import static br.com.fateczs.seazs.integration.support.DadosTeste.PASSADO;
import static br.com.fateczs.seazs.integration.support.DadosTeste.agendamento;
import static br.com.fateczs.seazs.integration.support.DadosTeste.atividade;
import static br.com.fateczs.seazs.integration.support.DadosTeste.data;
import static br.com.fateczs.seazs.integration.support.DadosTeste.evento;
import static br.com.fateczs.seazs.integration.support.DadosTeste.inscricao;
import static br.com.fateczs.seazs.integration.support.DadosTeste.papel;
import static br.com.fateczs.seazs.integration.support.DadosTeste.staff;
import static br.com.fateczs.seazs.integration.support.DadosTeste.usuario;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.integration.support.RepositorioIntegracaoBase;
import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.InscricaoRepository;

/**
 * Cobre as 4 @Query de InscricaoRepository (2 JPQL + 2 SQL nativas) e as derivadas/unicidade.
 * As JPQL comparam com CURRENT_TIMESTAMP: os testes usam atividades em 2000 (passado) e 2999 (futuro).
 */
public class InscricaoRepositoryIntegrationTest extends RepositorioIntegracaoBase {

	private static final Sort POR_INICIO = new Sort(Sort.Direction.ASC, "atividade.inicioAtividade");

	@Autowired
	private TestEntityManager em;
	@Autowired
	private InscricaoRepository repository;

	private Usuario ana;
	private Usuario bia;
	private Papel participante;
	private Agendamento agendamento;

	@Before
	public void prepararCenarioBase() {
		ana = em.persist(usuario("ana@teste.com"));
		bia = em.persist(usuario("bia@teste.com"));
		participante = em.persist(papel("Participante"));
		agendamento = em.persist(agendamento(em.persist(evento("Semana Tech", ana)), ana, PASSADO, FUTURO));
	}

	private Atividade novaAtividade(String nome, Date inicio, Date fim) {
		return em.persist(atividade(nome, agendamento, inicio, fim));
	}

	private Inscricao inscrever(Atividade atividade, Usuario usuario, Date data, boolean in, boolean out, int pontos) {
		return em.persist(inscricao(atividade, usuario, participante, data, in, out, pontos));
	}

	private void sincronizar() {
		em.flush();
		em.clear();
	}

	private static List<String> nomes(List<Atividade> atividades) {
		java.util.ArrayList<String> nomes = new java.util.ArrayList<>();
		for (Atividade a : atividades) {
			nomes.add(a.getNome());
		}
		return nomes;
	}

	// ---- findAtividadesInscrito (JPQL: atividades ainda nao encerradas do usuario)

	@Test
	public void findAtividadesInscrito_retornaSomenteAtividadesFuturasDoUsuarioOrdenadasPorInicio() {
		Atividade futuraTarde = novaAtividade("futura-tarde", data(2999, Calendar.MARCH, 1, 10), FUTURO);
		Atividade futuraCedo = novaAtividade("futura-cedo", data(2999, Calendar.FEBRUARY, 1, 10), FUTURO);
		Atividade passada = novaAtividade("passada", PASSADO, data(2000, Calendar.JANUARY, 11, 10));
		Atividade deOutroUsuario = novaAtividade("de-outro", data(2999, Calendar.APRIL, 1, 10), FUTURO);
		inscrever(futuraTarde, ana, PASSADO, false, false, 0);
		inscrever(futuraCedo, ana, PASSADO, false, false, 0);
		inscrever(passada, ana, PASSADO, true, true, 10);
		inscrever(deOutroUsuario, bia, PASSADO, false, false, 0);
		sincronizar();

		List<Atividade> resultado = repository.findAtividadesInscrito(ana.getId(), POR_INICIO);

		assertEquals(Arrays.asList("futura-cedo", "futura-tarde"), nomes(resultado));
	}

	// ---- findAtividadesInscritoPassado (JPQL: atividades encerradas com checkIn e checkOut)

	@Test
	public void findAtividadesInscritoPassado_retornaSomenteEncerradasComCheckInECheckOut() {
		Atividade completa = novaAtividade("completa", PASSADO, data(2000, Calendar.JANUARY, 11, 10));
		Atividade soCheckIn = novaAtividade("so-checkin", PASSADO, data(2000, Calendar.JANUARY, 12, 10));
		Atividade futuraCompleta = novaAtividade("futura", data(2999, Calendar.MARCH, 1, 10), FUTURO);
		inscrever(completa, ana, PASSADO, true, true, 10);
		inscrever(soCheckIn, ana, PASSADO, true, false, 0);
		inscrever(futuraCompleta, ana, PASSADO, true, true, 10);
		sincronizar();

		List<Atividade> resultado = repository.findAtividadesInscritoPassado(ana.getId(), POR_INICIO);

		assertEquals(Collections.singletonList("completa"), nomes(resultado));
	}

	@Test
	public void findAtividadesInscritoPassado_naoRetornaAtividadesDeOutroUsuario() {
		Atividade completa = novaAtividade("completa", PASSADO, data(2000, Calendar.JANUARY, 11, 10));
		inscrever(completa, bia, PASSADO, true, true, 10);
		sincronizar();

		assertEquals(0, repository.findAtividadesInscritoPassado(ana.getId(), POR_INICIO).size());
	}

	// ---- somaPontuacaoRecebida (SQL nativa: inscricoes UNION ALL staff)

	@Test
	public void somaPontuacaoRecebida_somaInscricoesEStaffDoUsuario() {
		inscrever(novaAtividade("a1", PASSADO, FUTURO), ana, PASSADO, true, true, 10);
		inscrever(novaAtividade("a2", PASSADO, FUTURO), ana, PASSADO, true, true, 5);
		inscrever(novaAtividade("a3", PASSADO, FUTURO), bia, PASSADO, true, true, 99);
		em.persist(staff(agendamento, ana, PASSADO, true, true, 20));
		sincronizar();

		assertEquals(Integer.valueOf(35), repository.somaPontuacaoRecebida(ana.getId()));
	}

	@Test
	public void somaPontuacaoRecebida_somaSoStaffQuandoNaoHaInscricoes() {
		em.persist(staff(agendamento, ana, PASSADO, true, true, 20));
		sincronizar();

		assertEquals(Integer.valueOf(20), repository.somaPontuacaoRecebida(ana.getId()));
	}

	/** Caracterizacao: sem registros a query devolve null (nao 0). O Angular/servico precisam tratar. */
	@Test
	public void somaPontuacaoRecebida_devolveNullQuandoUsuarioNaoTemRegistros() {
		assertNull(repository.somaPontuacaoRecebida(ana.getId()));
	}

	// ---- somaPontuacaoRecebidaNoSemestre (SQL nativa com BETWEEN)

	private static Calendar cal(int ano, int mes, int dia, int hora, int min, int seg) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(ano, mes, dia, hora, min, seg);
		return c;
	}

	@Test
	public void somaPontuacaoRecebidaNoSemestre_consideraSomenteRegistrosDentroDoIntervalo() {
		Calendar ini = cal(2024, Calendar.JANUARY, 1, 0, 0, 1);
		Calendar fim = cal(2024, Calendar.JULY, 31, 23, 59, 59);
		inscrever(novaAtividade("dentro", PASSADO, FUTURO), ana, data(2024, Calendar.MARCH, 10, 9), true, true, 10);
		inscrever(novaAtividade("fora", PASSADO, FUTURO), ana, data(2024, Calendar.AUGUST, 10, 9), true, true, 7);
		em.persist(staff(agendamento, ana, data(2024, Calendar.JUNE, 1, 9), true, true, 20));
		sincronizar();

		assertEquals(Integer.valueOf(30), repository.somaPontuacaoRecebidaNoSemestre(ana.getId(), ini, fim));
	}

	@Test
	public void somaPontuacaoRecebidaNoSemestre_incluiRegistroExatamenteNosLimitesDoIntervalo() {
		Calendar ini = cal(2024, Calendar.JANUARY, 1, 0, 0, 1);
		Calendar fim = cal(2024, Calendar.JULY, 31, 23, 59, 59);
		inscrever(novaAtividade("no-inicio", PASSADO, FUTURO), ana, ini.getTime(), true, true, 3);
		inscrever(novaAtividade("no-fim", PASSADO, FUTURO), ana, fim.getTime(), true, true, 4);
		sincronizar();

		assertEquals(Integer.valueOf(7), repository.somaPontuacaoRecebidaNoSemestre(ana.getId(), ini, fim));
	}

	@Test
	public void somaPontuacaoRecebidaNoSemestre_devolveNullQuandoNadaNoIntervalo() {
		inscrever(novaAtividade("fora", PASSADO, FUTURO), ana, data(2023, Calendar.MARCH, 10, 9), true, true, 10);
		sincronizar();

		Integer soma = repository.somaPontuacaoRecebidaNoSemestre(ana.getId(),
				cal(2024, Calendar.JANUARY, 1, 0, 0, 1), cal(2024, Calendar.JULY, 31, 23, 59, 59));

		assertNull(soma);
	}

	// ---- findByUsuarioIdAndAtividadeId (JPQL) e derivadas

	@Test
	public void findByUsuarioIdAndAtividadeId_retornaAInscricaoDoParUsuarioAtividade() {
		Atividade atividade = novaAtividade("alvo", PASSADO, FUTURO);
		Inscricao esperada = inscrever(atividade, ana, PASSADO, true, false, 0);
		inscrever(atividade, bia, PASSADO, false, false, 0);
		sincronizar();

		Inscricao encontrada = repository.findByUsuarioIdAndAtividadeId(ana.getId(), atividade.getId());

		assertEquals(esperada.getSequencialInscricao(), encontrada.getSequencialInscricao());
	}

	@Test
	public void findByUsuarioIdAndAtividadeId_devolveNullQuandoNaoHaInscricao() {
		Atividade atividade = novaAtividade("sem-inscricao", PASSADO, FUTURO);
		sincronizar();

		assertNull(repository.findByUsuarioIdAndAtividadeId(ana.getId(), atividade.getId()));
	}

	@Test
	public void findByAtividadeId_retornaTodasAsInscricoesDaAtividade() {
		Atividade atividade = novaAtividade("lotada", PASSADO, FUTURO);
		inscrever(atividade, ana, PASSADO, false, false, 0);
		inscrever(atividade, bia, PASSADO, false, false, 0);
		inscrever(novaAtividade("outra", PASSADO, FUTURO), ana, PASSADO, false, false, 0);
		sincronizar();

		assertEquals(2, repository.findByAtividadeId(atividade.getId()).size());
	}

	@Test
	public void findByUsuarioId_retornaTodasAsInscricoesDoUsuario() {
		inscrever(novaAtividade("a1", PASSADO, FUTURO), ana, PASSADO, false, false, 0);
		inscrever(novaAtividade("a2", PASSADO, FUTURO), ana, PASSADO, false, false, 0);
		inscrever(novaAtividade("a3", PASSADO, FUTURO), bia, PASSADO, false, false, 0);
		sincronizar();

		assertEquals(2, repository.findByUsuarioId(ana.getId()).size());
	}

	@Test(expected = DataIntegrityViolationException.class)
	public void saveAndFlush_rejeitaSegundaInscricaoDoMesmoUsuarioNaMesmaAtividade() {
		Atividade atividade = novaAtividade("unica", PASSADO, FUTURO);
		repository.saveAndFlush(inscricao(atividade, ana, participante, PASSADO, false, false, 0));

		repository.saveAndFlush(inscricao(atividade, ana, participante, PASSADO, false, false, 0));
	}
}
