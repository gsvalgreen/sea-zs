package br.com.fateczs.seazs.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Limite de requisicoes por IP (e escopo) em janela fixa, em memoria. Entradas expiradas sao varridas de forma
 * oportunista (a cada janela) e o mapa tem teto. Com o teto atingido, primeiro varre as expiradas; se ainda assim nao
 * houver espaco, uma chave nova e recusada (falha fechada: 429 ate a janela reabrir), porque deixar passar permitiria
 * a um atacante encher o mapa com chaves aleatorias e desligar o limite.
 *
 * Enderecos IPv6 sao agrupados pelo prefixo /64 (a menor alocacao tipica de um assinante), senao um unico cliente
 * contornaria o limite girando entre os 2^64 enderecos do seu prefixo. IPv4 e IPv4 mapeado em IPv6 usam o endereco.
 *
 * O IP vem de {@code request.getRemoteAddr()}. Atras de proxy, configure {@code server.forward-headers-strategy}
 * para o container resolver o IP real; este componente nunca le X-Forwarded-For por conta propria.
 */
@Component
public class LimitadorPorIp {

	private static final Pattern LITERAL_IP = Pattern.compile("[0-9a-fA-F:.]+(%[0-9A-Za-z._-]+)?");

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
		String chave = escopo + "|" + normalizarIp(ip);
		long[] restante = { 0 };
		boolean[] cheio = { false };
		contar(chave, agora, restante, cheio);
		if (cheio[0]) {
			varrer(agora); // libera as expiradas antes de desistir
			cheio[0] = false;
			contar(chave, agora, restante, cheio);
			if (cheio[0]) {
				return Optional.of(Duration.ofMillis(janelaMs));
			}
		}
		return restante[0] > 0 ? Optional.of(Duration.ofMillis(restante[0])) : Optional.empty();
	}

	private void contar(String chave, long agora, long[] restante, boolean[] cheio) {
		janelas.compute(chave, (k, atual) -> {
			if (atual == null && janelas.size() >= maxEntradas) {
				cheio[0] = true;
				return null;
			}
			Janela j = (atual == null || agora >= atual.inicio + janelaMs) ? new Janela(agora) : atual;
			j.contagem++;
			if (j.contagem > maximo) {
				restante[0] = j.inicio + janelaMs - agora;
			}
			return j;
		});
	}

	/** IPv6 vira o prefixo /64 em hexadecimal; qualquer outro valor (IPv4, texto desconhecido) segue como veio. */
	static String normalizarIp(String ip) {
		if (ip == null) {
			return "desconhecido";
		}
		if (ip.indexOf(':') < 0 || !LITERAL_IP.matcher(ip).matches()) {
			return ip;
		}
		try {
			// So literais chegam aqui (regex acima): getByName nao faz consulta de DNS.
			InetAddress endereco = InetAddress.getByName(ip);
			byte[] b = endereco.getAddress();
			if (b.length != 16) {
				return endereco.getHostAddress(); // IPv4 mapeado vira Inet4Address
			}
			StringBuilder prefixo = new StringBuilder("v6/");
			for (int i = 0; i < 8; i++) {
				prefixo.append(String.format("%02x", b[i]));
			}
			return prefixo.toString();
		} catch (UnknownHostException e) {
			return ip;
		}
	}

	private void varrerSeNecessario(long agora) {
		long proxima = proximaVarredura.get();
		if (agora >= proxima && proximaVarredura.compareAndSet(proxima, agora + janelaMs)) {
			varrer(agora);
		}
	}

	/** Remove as janelas expiradas sem perder contagem concorrente (a remocao e decidida dentro do compute da chave). */
	private void varrer(long agora) {
		for (String chave : janelas.keySet()) {
			janelas.computeIfPresent(chave, (k, j) -> agora >= j.inicio + janelaMs ? null : j);
		}
	}

	/** Quantidade de entradas em memoria (para testes e diagnostico). */
	public int tamanho() {
		return janelas.size();
	}
}
