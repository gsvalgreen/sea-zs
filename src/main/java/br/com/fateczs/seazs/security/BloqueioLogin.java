package br.com.fateczs.seazs.security;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Bloqueio temporario progressivo por e-mail apos falhas de login, em memoria.
 *
 * <ul>
 * <li>A tentativa e <b>reservada antes</b> de verificar a senha ({@link #reservarTentativa}), de forma atomica por
 * e-mail: uma rajada concorrente nao ultrapassa {@code maxFalhas} tentativas. Um login bem-sucedido apaga o historico
 * (inclusive a reserva dessa propria tentativa); sem sucesso, a reserva conta como falha.</li>
 * <li>{@code maxFalhas} tentativas consecutivas (sem {@code janelaFalhas} de intervalo entre elas) bloqueiam o e-mail
 * por {@code duracao}; cada novo bloqueio dobra a duracao, ate {@code duracaoMaxima}.</li>
 * <li>O bloqueio vale para qualquer e-mail informado, exista ou nao: a resposta de um e-mail inexistente e igual a de um
 * existente, entao o bloqueio nao serve de oraculo de existencia.</li>
 * <li>Durante o bloqueio a senha nem e verificada (mesmo a correta e recusada) e novas tentativas nao estendem o prazo.</li>
 * <li>A chave acompanha a collation {@code utf8mb4_unicode_ci} do banco (ver {@link #normalizar}): variantes que o
 * MySQL considera o mesmo e-mail (caixa, acentos, caracteres ignoraveis) compartilham o mesmo bloqueio.</li>
 * </ul>
 *
 * Trade-off conhecido: quem conhece o e-mail de alguem pode bloquea-lo ao errar a senha de proposito (negacao de
 * servico contra uma conta); o limite por IP ({@link LimitadorPorIp}) reduz o alcance. O mapa tem teto
 * ({@code maxEntradas}): cheio, primeiro varre os historicos vencidos e, se ainda nao houver espaco, recusa e-mails
 * novos (falha fechada, 429), porque aceitar tentativas sem contar permitiria desligar o bloqueio enchendo o mapa.
 */
@Component
public class BloqueioLogin {

	private static final int MAX_ENTRADA = 1024;
	private static final int MAX_CHAVE = 255;
	private static final Pattern IGNORAVEIS = Pattern.compile("[\\p{M}\\p{Cf}\\p{Cc}]");

	private static final class Estado {
		int falhas;
		long ultimaFalha;
		int nivel;
		long bloqueadoAte;
	}

	private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();
	private final AtomicLong proximaVarredura = new AtomicLong();
	private final Clock clock;
	private final SegurancaProperties.Lockout cfg;
	private final int maxEntradas;

	public BloqueioLogin(Clock clock, SegurancaProperties props) {
		this.clock = clock;
		this.cfg = props.lockout();
		this.maxEntradas = props.maxEntradas();
		if (cfg.maxFalhas() < 1 || cfg.janelaFalhas().isNegative() || cfg.janelaFalhas().isZero()
				|| cfg.duracao().isNegative() || cfg.duracao().isZero()
				|| cfg.duracaoMaxima().compareTo(cfg.duracao()) < 0 || cfg.retencao().isNegative()) {
			throw new IllegalStateException("app.security.lockout invalido: max-falhas >= 1, duracoes positivas e "
					+ "duracao-maxima >= duracao");
		}
	}

	/**
	 * Chave do e-mail alinhada a collation {@code utf8mb4_unicode_ci} (insensivel a caixa e a acentos, ignora
	 * caracteres de controle/formatacao e espacos nas pontas): NFKD, sem marcas combinantes nem ignoraveis, em
	 * minusculas, com as expansoes/letras sem decomposicao mais comuns (ss, ae, oe, o, d, l) equiparadas.
	 */
	static String normalizar(String email) {
		String bruto = email == null ? "" : email;
		if (bruto.length() > MAX_ENTRADA) {
			bruto = bruto.substring(0, MAX_ENTRADA);
		}
		String n = Normalizer.normalize(bruto, Normalizer.Form.NFKD);
		n = IGNORAVEIS.matcher(n).replaceAll("").trim().toLowerCase(Locale.ROOT);
		n = n.replace("ß", "ss").replace("æ", "ae").replace("œ", "oe").replace("ø", "o")
				.replace("đ", "d").replace("ł", "l");
		return n.length() > MAX_CHAVE ? n.substring(0, MAX_CHAVE) : n;
	}

	/** Tempo restante de bloqueio do e-mail, se estiver bloqueado (apenas consulta, nao reserva tentativa). */
	public Optional<Duration> restante(String email) {
		long agora = clock.millis();
		Estado e = estados.get(normalizar(email));
		if (e == null) {
			return Optional.empty();
		}
		synchronized (e) {
			return e.bloqueadoAte > agora ? Optional.of(Duration.ofMillis(e.bloqueadoAte - agora)) : Optional.empty();
		}
	}

	/**
	 * Reserva atomicamente uma tentativa de login do e-mail. Lanca {@link LimiteExcedidoException} se ele estiver
	 * bloqueado (a tentativa nao conta nem estende o prazo) ou se o mapa estiver cheio mesmo apos a varredura. Quem
	 * autenticar com sucesso deve chamar {@link #registrarSucesso}; sem isso, a tentativa permanece contada como falha.
	 */
	public void reservarTentativa(String email) {
		long agora = clock.millis();
		varrerSeNecessario(agora);
		String chave = normalizar(email);
		long[] espera = { 0 };
		boolean[] cheio = { false };
		reservar(chave, agora, espera, cheio);
		if (cheio[0]) {
			varrer(agora);
			cheio[0] = false;
			reservar(chave, agora, espera, cheio);
			if (cheio[0]) {
				throw new LimiteExcedidoException(cfg.janelaFalhas());
			}
		}
		if (espera[0] > 0) {
			throw new LimiteExcedidoException(Duration.ofMillis(espera[0]));
		}
	}

	private void reservar(String chave, long agora, long[] espera, boolean[] cheio) {
		estados.compute(chave, (k, e) -> {
			if (e == null) {
				if (estados.size() >= maxEntradas) {
					cheio[0] = true;
					return null;
				}
				e = new Estado();
			}
			synchronized (e) {
				if (e.bloqueadoAte > agora) {
					espera[0] = e.bloqueadoAte - agora; // ja bloqueado: nao conta nem estende
					return e;
				}
				if (e.falhas > 0 && agora - e.ultimaFalha > cfg.janelaFalhas().toMillis()) {
					e.falhas = 0;
				}
				e.falhas++;
				e.ultimaFalha = agora;
				if (e.falhas >= cfg.maxFalhas()) {
					e.nivel++;
					e.falhas = 0;
					e.bloqueadoAte = agora + duracaoDoNivel(e.nivel);
				}
			}
			return e;
		});
	}

	/** Login bem-sucedido: esquece falhas, nivel e a reserva da propria tentativa. */
	public void registrarSucesso(String email) {
		estados.remove(normalizar(email));
	}

	private long duracaoDoNivel(int nivel) {
		long base = cfg.duracao().toMillis();
		long teto = cfg.duracaoMaxima().toMillis();
		long ms = base;
		for (int i = 1; i < nivel && ms < teto; i++) {
			ms = ms > teto / 2 ? teto : ms * 2;
		}
		return Math.min(ms, teto);
	}

	private void varrerSeNecessario(long agora) {
		long proxima = proximaVarredura.get();
		long intervalo = Math.max(1000, cfg.janelaFalhas().toMillis());
		if (agora >= proxima && proximaVarredura.compareAndSet(proxima, agora + intervalo)) {
			varrer(agora);
		}
	}

	/** Esquece historicos vencidos; a remocao e decidida dentro do compute da chave (nao perde reserva concorrente). */
	private void varrer(long agora) {
		long retencao = cfg.retencao().toMillis();
		for (String chave : estados.keySet()) {
			estados.computeIfPresent(chave, (k, e) -> {
				synchronized (e) {
					return e.bloqueadoAte <= agora && agora - e.ultimaFalha > retencao ? null : e;
				}
			});
		}
	}

	/** Quantidade de entradas em memoria (para testes e diagnostico). */
	public int tamanho() {
		return estados.size();
	}
}
