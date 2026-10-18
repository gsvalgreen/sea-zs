package br.com.fateczs.seazs.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * Limite de requisicoes por IP (e escopo) em janela fixa, em memoria. Entradas expiradas sao varridas de forma
 * oportunista (a cada janela) e o mapa tem teto: com o teto atingido, IPs novos nao sao registrados (o limite deixa
 * de valer para eles, mas a memoria nao cresce sem controle).
 *
 * O IP vem de {@code request.getRemoteAddr()}. Atras de proxy, configure {@code server.forward-headers-strategy}
 * para o container resolver o IP real; este componente nunca le X-Forwarded-For por conta propria.
 */
@Component
public class LimitadorPorIp {

	private static final class Janela {
		final long inicio;
		int contagem;

		Janela(long inicio) {
			this.inicio = inicio;
		}
	}

	private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();
	private final AtomicLong proximaVarredura = new AtomicLong();
	private final Clock clock;
	private final int maximo;
	private final long janelaMs;
	private final int maxEntradas;

	public LimitadorPorIp(Clock clock, SegurancaProperties props) {
		this.clock = clock;
		this.maximo = props.rateLimit().maxRequisicoes();
		this.janelaMs = props.rateLimit().janela().toMillis();
		this.maxEntradas = props.maxEntradas();
		if (maximo < 1 || janelaMs < 1) {
			throw new IllegalStateException("app.security.rate-limit.max-requisicoes e janela devem ser positivos");
		}
	}

	/**
	 * Conta uma requisicao do IP no escopo.
	 *
	 * @return vazio se dentro do limite; senao o tempo ate a janela reabrir
	 */
	public Optional<Duration> registrar(String ip, String escopo) {
		long agora = clock.millis();
		varrerSeNecessario(agora);
		String chave = escopo + "|" + (ip == null ? "desconhecido" : ip);
		if (!janelas.containsKey(chave) && janelas.size() >= maxEntradas) {
			return Optional.empty();
		}
		long[] restante = { 0 };
		janelas.compute(chave, (k, atual) -> {
			Janela j = (atual == null || agora >= atual.inicio + janelaMs) ? new Janela(agora) : atual;
			j.contagem++;
			if (j.contagem > maximo) {
				restante[0] = j.inicio + janelaMs - agora;
			}
			return j;
		});
		return restante[0] > 0 ? Optional.of(Duration.ofMillis(restante[0])) : Optional.empty();
	}

	private void varrerSeNecessario(long agora) {
		long proxima = proximaVarredura.get();
		if (agora >= proxima && proximaVarredura.compareAndSet(proxima, agora + janelaMs)) {
			janelas.entrySet().removeIf(e -> agora >= e.getValue().inicio + janelaMs);
		}
	}

	/** Quantidade de entradas em memoria (para testes e diagnostico). */
	public int tamanho() {
		return janelas.size();
	}
}
