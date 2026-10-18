package br.com.fateczs.seazs.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.PerfilInvalidoException;
import br.com.fateczs.seazs.service.UsuarioService;
import br.com.fateczs.seazs.utils.TipoUsuario;

/**
 * Dois administradores se rebaixando ao mesmo tempo: sem o lock pessimista dos administradores os dois commits
 * passariam e o sistema ficaria sem ADMIN. Banco H2 proprio (nao compartilha administradores com outros testes).
 * O H2 serializa o SELECT ... FOR UPDATE por linha; a fidelidade com o MySQL (InnoDB) fica para um featureTest.
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestPropertySource(properties = { "app.jwt.secret=" + ComSeguranca.SEGREDO_TESTE,
		"spring.datasource.url=jdbc:h2:mem:seazs_concorrencia;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" })
public class UltimoAdministradorConcorrenciaSpringBootTest {

	private static final int RODADAS = 15;

	@Autowired
	private UsuarioService service;
	@Autowired
	private UsuarioRepository usuarios;

	private Usuario admin(String prefixo) {
		Usuario u = DadosTeste.usuario(prefixo + "-" + UUID.randomUUID() + "@teste.com");
		u.setTipoUsuario(TipoUsuario.FUNCIONARIO);
		u.setAdm(true);
		return usuarios.save(u);
	}

	@Test
	public void rebaixamentosSimultaneosNuncaDeixamOSistemaSemAdministrador() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			for (int i = 0; i < RODADAS; i++) {
				usuarios.findAll().forEach(u -> {
					u.setAdm(false);
					usuarios.save(u);
				});
				Usuario a = admin("a");
				Usuario b = admin("b");
				CyclicBarrier largada = new CyclicBarrier(2);

				List<Future<Boolean>> resultados = new ArrayList<>();
				resultados.add(pool.submit(rebaixar(largada, b.getId(), a.getId())));
				resultados.add(pool.submit(rebaixar(largada, a.getId(), b.getId())));

				int sucessos = 0;
				for (Future<Boolean> r : resultados) {
					sucessos += r.get(30, TimeUnit.SECONDS) ? 1 : 0;
				}
				assertEquals(1, sucessos, "exatamente um rebaixamento passa (rodada " + i + ")");
				assertTrue(usuarios.findAll().stream().anyMatch(u -> Boolean.TRUE.equals(u.getAdm())), "ao menos um ADMIN permanece");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	/** Rebaixa {@code alvo} como {@code autor}; true se passou, false se recusado por ser o ultimo ADMIN. */
	private Callable<Boolean> rebaixar(CyclicBarrier largada, Integer alvo, Integer autor) {
		return () -> {
			largada.await(10, TimeUnit.SECONDS);
			try {
				service.alterarPerfil(alvo, TipoUsuario.ALUNO, false, autor);
				return true;
			} catch (PerfilInvalidoException e) {
				return false;
			}
		};
	}
}
