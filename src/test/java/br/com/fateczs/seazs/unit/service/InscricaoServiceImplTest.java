package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.service.serviceImpl.InscricaoServiceImpl;

@ExtendWith(MockitoExtension.class)
public class InscricaoServiceImplTest {

	private static final long MINUTO = 60 * 1000L;

	@Mock
	private InscricaoRepository repository;

	@InjectMocks
	private InscricaoServiceImpl service;

	private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

	/** Instante fixo (meio do ano, primeiro semestre) para tornar as janelas deterministicas. */
	private static final Instant AGORA = Instant.parse("2019-03-15T15:00:00Z");

	@BeforeEach
	public void fixaRelogio() {
		fixaRelogio(AGORA);
	}

	private void fixaRelogio(Instant instante) {
		ReflectionTestUtils.setField(service, "clock", Clock.fixed(instante, SP));
	}

	private Date emMinutos(long minutos) {
		return new Date(AGORA.toEpochMilli() + minutos * MINUTO);
	}

	private Inscricao requisicao() {
		Inscricao inscricao = new Inscricao();
		inscricao.setSequencialInscricao(1);
		return inscricao;
	}

	/** Atividade não contínua com início/fim relativos ao instante atual (em minutos). */
	private Inscricao persistida(long inicioMin, long fimMin, boolean checkIn, boolean checkOut) {
		Atividade atividade = new Atividade();
		atividade.setFlagContinua(false);
		atividade.setInicioAtividade(emMinutos(inicioMin));
		atividade.setFimAtividade(emMinutos(fimMin));
		atividade.setPontuacaoParticipante(10);
		Inscricao inscricao = new Inscricao();
		inscricao.setAtividade(atividade);
		inscricao.setCheckIn(checkIn);
		inscricao.setCheckOut(checkOut);
		inscricao.setPontuacaoRecebida(0);
		when(repository.getReferenceById(1)).thenReturn(inscricao);
		return inscricao;
	}

	/** Atividade contínua com janela de entrada aberta (início há 5 minutos). */
	private Inscricao continuaPersistida(long inicioMin, boolean cumulativa, int pontosJaRecebidos, int qtdCheckIn) {
		Inscricao inscricao = persistida(inicioMin, inicioMin + 600, false, false);
		inscricao.getAtividade().setFlagContinua(true);
		inscricao.getAtividade().setFlagCumulativa(cumulativa);
		inscricao.getAtividade().setQtdCheckIn(qtdCheckIn);
		inscricao.setPontuacaoRecebida(pontosJaRecebidos);
		// lenient: nem todo cenario chega a salvar (fora da janela/limite); MockitoExtension e estrito por teste.
		lenient().when(repository.save(inscricao)).thenReturn(inscricao);
		return inscricao;
	}

	@Test
	public void buscarUsaSequencialDaInscricao() {
		Inscricao esperado = new Inscricao();
		when(repository.getReferenceById(1)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(requisicao()));
	}

	@Test
	public void inserirIniciaInscricaoSemPresencaENemPontos() {
		Inscricao inscricao = new Inscricao();
		inscricao.setCheckIn(true);
		inscricao.setCheckOut(true);
		inscricao.setPontuacaoRecebida(99);

		service.inserir(inscricao);

		assertEquals(AGORA.toEpochMilli(), inscricao.getDtInscricao().getTime());
		assertFalse(inscricao.getCheckIn());
		assertFalse(inscricao.getCheckOut());
		assertEquals(Integer.valueOf(0), inscricao.getPontuacaoRecebida());
		verify(repository).save(inscricao);
	}

	@Test
	public void alterarExcluirListar() {
		Inscricao inscricao = new Inscricao();
		List<Inscricao> todas = Arrays.asList(inscricao);
		when(repository.findAll()).thenReturn(todas);

		service.alterar(inscricao);
		service.excluir(inscricao);

		verify(repository).save(inscricao);
		verify(repository).delete(inscricao);
		assertEquals(todas, service.listar());
	}

	@Test
	public void listarPorDataAscOrdenaPorDtInscricao() {
		List<Inscricao> todas = Arrays.asList(new Inscricao());
		when(repository.findAll(any(Sort.class))).thenReturn(todas);

		assertEquals(todas, service.listarPorDataAsc());

		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(repository).findAll(sort.capture());
		assertEquals(Sort.Direction.ASC, sort.getValue().getOrderFor("dtInscricao").getDirection());
	}

	@Test
	public void listagensPorUsuarioEAtividade() {
		Usuario usuario = new Usuario();
		usuario.setId(3);
		Atividade atividade = new Atividade();
		atividade.setId(4);
		List<Inscricao> porUsuario = Arrays.asList(new Inscricao());
		List<Inscricao> porAtividade = Arrays.asList(new Inscricao(), new Inscricao());
		List<Atividade> futuras = Arrays.asList(new Atividade());
		List<Atividade> passadas = Arrays.asList(new Atividade(), new Atividade());
		when(repository.findByUsuarioId(3)).thenReturn(porUsuario);
		when(repository.findByAtividadeId(4)).thenReturn(porAtividade);
		when(repository.findAtividadesInscrito(eq(3), any(Sort.class))).thenReturn(futuras);
		when(repository.findAtividadesInscritoPassado(eq(3), any(Sort.class))).thenReturn(passadas);

		assertEquals(porUsuario, service.listarPorUsuario(usuario));
		assertEquals(porAtividade, service.listarPorAtividade(atividade));
		assertEquals(futuras, service.listarAtividadesInscrito(usuario));
		assertEquals(passadas, service.listarAtividadesInscritoPassado(usuario));
	}

	@Test
	public void somaTotalDePontosDelegaAoRepositorio() {
		Usuario usuario = new Usuario();
		usuario.setId(3);
		when(repository.somaPontuacaoRecebida(3)).thenReturn(42);
		assertEquals(Integer.valueOf(42), service.somaTotalDePontos(usuario));
	}

	@Test
	public void somaTotalDePontosNoSemestreConsultaIntervaloDoSemestreAtual() {
		Usuario usuario = new Usuario();
		usuario.setId(3);
		when(repository.somaPontuacaoRecebidaNoSemestre(eq(3), any(Calendar.class), any(Calendar.class))).thenReturn(15);

		assertEquals(Integer.valueOf(15), service.somaTotalDePontosNoSemestre(usuario));

		ArgumentCaptor<Calendar> ini = ArgumentCaptor.forClass(Calendar.class);
		ArgumentCaptor<Calendar> fim = ArgumentCaptor.forClass(Calendar.class);
		verify(repository).somaPontuacaoRecebidaNoSemestre(eq(3), ini.capture(), fim.capture());
		assertEquals(Calendar.JANUARY, ini.getValue().get(Calendar.MONTH));
		assertEquals(Calendar.JULY, fim.getValue().get(Calendar.MONTH));
	}

	private void verificaIntervaloDoSemestre(String instante, int ano, int mesIni, int diaIni, int mesFim, int diaFim) {
		fixaRelogio(Instant.parse(instante));
		Usuario usuario = new Usuario();
		usuario.setId(3);
		service.somaTotalDePontosNoSemestre(usuario);

		ArgumentCaptor<Calendar> ini = ArgumentCaptor.forClass(Calendar.class);
		ArgumentCaptor<Calendar> fim = ArgumentCaptor.forClass(Calendar.class);
		verify(repository).somaPontuacaoRecebidaNoSemestre(eq(3), ini.capture(), fim.capture());
		assertEquals(ano, ini.getValue().get(Calendar.YEAR));
		assertEquals(mesIni, ini.getValue().get(Calendar.MONTH));
		assertEquals(diaIni, ini.getValue().get(Calendar.DAY_OF_MONTH));
		assertEquals(ano, fim.getValue().get(Calendar.YEAR));
		assertEquals(mesFim, fim.getValue().get(Calendar.MONTH));
		assertEquals(diaFim, fim.getValue().get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void semestre1ComecaEmJaneiroEmQualquerDiaDoSemestre() {
		// 01/01 00:00 em Sao Paulo
		verificaIntervaloDoSemestre("2019-01-01T02:00:00Z", 2019, Calendar.JANUARY, 1, Calendar.JULY, 31);
	}

	@Test
	public void semestre1UltimoInstanteEm31DeJulho() {
		// 31/07 23:59:59 em Sao Paulo (UTC-3)
		verificaIntervaloDoSemestre("2019-08-01T02:59:59Z", 2019, Calendar.JANUARY, 1, Calendar.JULY, 31);
	}

	@Test
	public void semestre2ComecaEm1DeAgostoMeiaNoiteDeSaoPaulo() {
		// 01/08 00:00 em Sao Paulo
		verificaIntervaloDoSemestre("2019-08-01T03:00:00Z", 2019, Calendar.AUGUST, 1, Calendar.DECEMBER, 31);
	}

	@Test
	public void semestre2UltimoInstanteEm31DeDezembro() {
		// 31/12 23:59:59 em Sao Paulo
		verificaIntervaloDoSemestre("2020-01-01T02:59:59Z", 2019, Calendar.AUGUST, 1, Calendar.DECEMBER, 31);
	}

	// ---- validaPresenca: atividade não contínua ----

	@Test
	public void checkInNoLimiteInferiorDaJanelaNaoRegistra() {
		// inicio = agora + 10min => inicioEntrada == agora (limite exclusivo)
		persistida(10, 120, false, false);

		assertFalse(service.validaPresenca(requisicao()).getCheckIn());
		verify(repository, never()).save(any(Inscricao.class));
	}

	@Test
	public void checkInUmMinutoDepoisDoLimiteInferiorRegistra() {
		persistida(9, 120, false, false);

		assertTrue(service.validaPresenca(requisicao()).getCheckIn());
	}

	@Test
	public void checkInNoLimiteSuperiorDaJanelaNaoRegistra() {
		// inicio = agora - 30min => fimEntrada == agora (limite exclusivo)
		persistida(-30, 120, false, false);

		assertFalse(service.validaPresenca(requisicao()).getCheckIn());
	}

	@Test
	public void checkInDentroDaJanelaDeEntrada() {
		Inscricao inscricao = persistida(-5, 120, false, false);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertTrue(resultado.getCheckIn());
		assertFalse(resultado.getCheckOut());
		assertEquals(Integer.valueOf(0), resultado.getPontuacaoRecebida());
		verify(repository).save(inscricao);
	}

	@Test
	public void checkInJaRealizadoNaoSalvaNovamente() {
		persistida(-5, 120, true, false);

		service.validaPresenca(requisicao());

		verify(repository, never()).save(any(Inscricao.class));
	}

	@Test
	public void antesDaJanelaDeEntradaNaoFazNada() {
		persistida(60, 180, false, false);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertFalse(resultado.getCheckIn());
		assertFalse(resultado.getCheckOut());
		verify(repository, never()).save(any(Inscricao.class));
	}

	@Test
	public void checkOutDentroDaJanelaDeSaidaComCheckInValidaPontos() {
		Inscricao inscricao = persistida(-120, 5, true, false);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertTrue(resultado.getCheckOut());
		assertEquals(Integer.valueOf(10), resultado.getPontuacaoRecebida());
		verify(repository, times(2)).save(inscricao);
	}

	@Test
	public void checkOutSemCheckInNaoGeraPontos() {
		persistida(-120, 5, false, false);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertTrue(resultado.getCheckOut());
		assertFalse(resultado.getCheckIn());
		assertEquals(Integer.valueOf(0), resultado.getPontuacaoRecebida());
	}

	@Test
	public void depoisDaJanelaDeSaidaNaoFazNada() {
		persistida(-240, -120, true, false);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertFalse(resultado.getCheckOut());
		verify(repository, never()).save(any(Inscricao.class));
	}

	// ---- validaPresenca: atividade contínua ----

	@Test
	public void continuaNaoCumulativaDentroDaJanelaPontuaUmaVez() {
		Inscricao inscricao = continuaPersistida(-5, false, 0, 1);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertTrue(resultado.getCheckIn());
		assertTrue(resultado.getCheckOut());
		assertEquals(Integer.valueOf(10), resultado.getPontuacaoRecebida());
		verify(repository).save(inscricao);
	}

	@Test
	public void continuaCumulativaSomaPontosAteOLimiteDeCheckIns() {
		continuaPersistida(-5, true, 10, 3);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertEquals(Integer.valueOf(20), resultado.getPontuacaoRecebida());
	}

	@Test
	public void continuaCumulativaNoLimiteNaoSomaMaisPontos() {
		Inscricao inscricao = continuaPersistida(-5, true, 30, 3);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertEquals(Integer.valueOf(30), resultado.getPontuacaoRecebida());
		verify(repository, never()).save(inscricao);
	}

	@Test
	public void continuaForaDaJanelaNaoPontua() {
		continuaPersistida(60, false, 0, 1);

		Inscricao resultado = service.validaPresenca(requisicao());

		assertFalse(resultado.getCheckIn());
		assertEquals(Integer.valueOf(0), resultado.getPontuacaoRecebida());
		verify(repository, never()).save(any(Inscricao.class));
	}
}
