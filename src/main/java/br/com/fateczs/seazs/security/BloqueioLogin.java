package br.com.fateczs.seazs.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * Bloqueio temporario progressivo por e-mail apos falhas de login, em memoria.
 *
 * <ul>
 * <li>{@code maxFalhas} falhas consecutivas (sem {@code janelaFalhas} de intervalo entre elas) bloqueiam o e-mail por
 * {@code duracao}; cada novo bloqueio dobra a duracao, ate {@code duracaoMaxima}.</li>
 * <li>Um login bem-sucedido apaga todo o historico do e-mail.</li>
 * <li>O bloqueio vale para qualquer e-mail informado, exista ou nao: a resposta de um e-mail inexistente e igual a de um
 * existente, entao o bloqueio nao serve de oraculo de existencia.</li>
 * <li>Durante o bloqueio a senha nem e verificada (mesmo a correta e recusada) e novas tentativas nao estendem o prazo.</li>
 * </ul>
 *
 * Trade-off conhecido: quem conhece o e-mail de alguem pode bloquea-lo ao errar a senha de proposito (negacao de
 * servico contra uma conta); o limite por IP ({@link LimitadorPorIp}) reduz o alcance. O mapa tem teto
 * ({@code maxEntradas}): cheio, e-mails novos deixam de ser registrados (sem bloqueio), preferindo nao estourar a memoria.
 */
@Component
public class BloqueioLogin {

	private static final int MAX_CHAVE = 255;

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

	/** E-mail normalizado como chave: sem espacos nas pontas e em minusculas (o MySQL compara sem diferenciar caixa). */
	static String normalizar(String email) {
		String n = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		return n.length() > MAX_CHAVE ? n.substring(0, MAX_CHAVE) : n;
	}

	/** Tempo restante de bloqueio do e-mail, se estiver bloqueado. */
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

	/** Lanca {@link LimiteExcedidoException} se o e-mail estiver bloqueado. */
	public void exigirLiberado(String email) {
		restante(email).ifPresent(d -> {
			throw new LimiteExcedidoException(d);
		});
	}

	public void registrarFalha(String email) {
		long agora = clock.millis();
		varrerSeNecessario(agora);
		String chave = normalizar(email);
		if (!estados.containsKey(chave) && estados.size() >= maxEntradas) {
			return;
		}
		Estado e = estados.computeIfAbsent(chave, k -> new Estado());
		synchronized (e) {
			if (e.bloqueadoAte > agora) {
				return; // ja bloqueado: nao conta nem estende
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
	}

	/** Login bem-sucedido: esquece falhas e nivel do e-mail. */
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
			long retencao = cfg.retencao().toMillis();
			estados.entrySet().removeIf(en -> {
				Estado e = en.getValue();
				synchronized (e) {
					return e.bloqueadoAte <= agora && agora - e.ultimaFalha > retencao;
				}
			});
		}
	}

	/** Quantidade de entradas em memoria (para testes e diagnostico). */
	public int tamanho() {
		return estados.size();
	}
}
