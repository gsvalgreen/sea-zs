package br.com.fateczs.seazs.featureTest;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;
import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.AgendamentoRepository;
import br.com.fateczs.seazs.repository.AtividadeRepository;
import br.com.fateczs.seazs.repository.EventoRepository;
import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.repository.PapelRepository;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;

/**
 * Executa as {@code @Query} (JPQL e SQL nativas) dos repositorios contra o MySQL 8 real. A camada integration
 * as cobre no H2, que nao reproduz o modo SQL do MySQL 8 ({@code ONLY_FULL_GROUP_BY}, derived table, BETWEEN com
 * datas). Os cenarios de regra ficam na integration; aqui basta provar que a SQL e aceita e devolve o esperado.
 * Cada teste usa usuarios proprios (e-mail unico), entao nao depende de outros dados do banco compartilhado.
 */
public class ConsultasMySqlFeatureTest extends FeatureTestBase {

	private static final Sort POR_INICIO = Sort.by(Sort.Direction.ASC, "atividade.inicioAtividade");

	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PapelRepository papeis;
	@Autowired
	private EventoRepository eventos;
	@Autowired
	private AgendamentoRepository agendamentos;
	@Autowired
	private AtividadeRepository atividades;
	@Autowired
	private InscricaoRepository inscricoes;
	@Autowired
	private StaffAgendamentoRepository staffs;

	private Usuario novoUsuario() {
		return usuarios.save(usuario(unico("u") + "@teste.com"));
	}

	private Agendamento novoAgendamento(Usuario criador) {
		return agendamentos.save(agendamento(eventos.save(evento(unico("Evento"), criador)), criador, PASSADO, FUTURO));
	}

	private Atividade novaAtividade(Agendamento ag, String nome, java.util.Date inicio, java.util.Date fim) {
		return atividades.save(atividade(unico(nome), ag, inicio, fim));
	}

	private static Calendar cal(int ano, int mes, int dia, int hora, int min, int seg) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(ano, mes, dia, hora, min, seg);
		return c;
	}

	@Test
	public void queriesJpqlDeInscricaoEStaff_filtramFuturoEPassadoPorUsuario() {
		Usuario ana = novoUsuario();
		Papel participante = papeis.save(papel(unico("Participante")));
		Agendamento ag = novoAgendamento(ana);
		Atividade futura = novaAtividade(ag, "futura", data(2999, Calendar.MARCH, 1, 10), FUTURO);
		Atividade passada = novaAtividade(ag, "passada", PASSADO, data(2000, Calendar.JANUARY, 11, 10));
		inscricoes.save(inscricao(futura, ana, participante, PASSADO, false, false, 0));
		inscricoes.save(inscricao(passada, ana, participante, PASSADO, true, true, 10));
		staffs.save(staff(ag, ana, PASSADO, true, true, 20));

		assertEquals(Collections.singletonList(futura.getId()),
				ids(inscricoes.findAtividadesInscrito(ana.getId(), POR_INICIO)));
		assertEquals(Collections.singletonList(passada.getId()),
				ids(inscricoes.findAtividadesInscritoPassado(ana.getId(), POR_INICIO)));
		assertEquals(1, staffs.findAgendamentosInscrito(ana.getId(), Sort.by("agendamento.inicio")).size());
		assertEquals(0, staffs.findAgendamentosInscritoPassado(ana.getId(), Sort.by("agendamento.inicio")).size());
		assertEquals(futura.getId(),
				inscricoes.findByUsuarioIdAndAtividadeId(ana.getId(), futura.getId()).getAtividade().getId());
	}

	@Test
	public void queriesJpqlDeEventoEAtividade_saoAceitasPeloMySql() {
		Usuario ana = novoUsuario();
		Agendamento ag = novoAgendamento(ana);
		Atividade futura = novaAtividade(ag, "futura", data(2999, Calendar.MARCH, 1, 10), FUTURO);

		assertFalse(eventos.findByFimAfterToday(Sort.by("inicio")).isEmpty());
		assertTrue(ids(atividades.findAtividadeDisponivelParaInscricao(ana.getId(), Sort.by("inicioAtividade")))
				.contains(futura.getId()));
		assertFalse(ids(atividades.findAtividadesPassadas(Sort.by("inicioAtividade")))
				.contains(futura.getId()));
	}

	@Test
	public void sqlNativaDeSomaDePontos_comGroupByFuncionaComOnlyFullGroupBy() {
		Usuario ana = novoUsuario();
		Usuario semRegistros = novoUsuario();
		Papel participante = papeis.save(papel(unico("Participante")));
		Agendamento ag = novoAgendamento(ana);
		inscricoes.save(inscricao(novaAtividade(ag, "a1", PASSADO, FUTURO), ana, participante, PASSADO, true, true, 10));
		inscricoes.save(inscricao(novaAtividade(ag, "a2", PASSADO, FUTURO), ana, participante, PASSADO, true, true, 5));
		staffs.save(staff(ag, ana, PASSADO, true, true, 20));

		assertEquals(Integer.valueOf(35), inscricoes.somaPontuacaoRecebida(ana.getId()));
		assertNull(inscricoes.somaPontuacaoRecebida(semRegistros.getId()));
	}

	@Test
	public void sqlNativaDeSomaNoSemestre_respeitaIntervaloComLimitesInclusivos() {
		Usuario ana = novoUsuario();
		Papel participante = papeis.save(papel(unico("Participante")));
		Agendamento ag = novoAgendamento(ana);
		Calendar ini = cal(2024, Calendar.JANUARY, 1, 0, 0, 1);
		Calendar fim = cal(2024, Calendar.JULY, 31, 23, 59, 59);
		inscricoes.save(inscricao(novaAtividade(ag, "no-inicio", PASSADO, FUTURO), ana, participante, ini.getTime(), true, true, 3));
		inscricoes.save(inscricao(novaAtividade(ag, "no-fim", PASSADO, FUTURO), ana, participante, fim.getTime(), true, true, 4));
		inscricoes.save(inscricao(novaAtividade(ag, "fora", PASSADO, FUTURO), ana, participante,
				data(2024, Calendar.AUGUST, 10, 9), true, true, 99));
		staffs.save(staff(ag, ana, data(2024, Calendar.JUNE, 1, 9), true, true, 20));

		assertEquals(Integer.valueOf(27), inscricoes.somaPontuacaoRecebidaNoSemestre(ana.getId(), ini, fim));
		assertNull(inscricoes.somaPontuacaoRecebidaNoSemestre(novoUsuario().getId(), ini, fim));
	}

	private static List<Integer> ids(List<Atividade> lista) {
		return lista.stream().map(Atividade::getId).collect(Collectors.toList());
	}
}
