package br.com.fateczs.seazs.integration.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.security.JwtTokenService;
import br.com.fateczs.seazs.utils.TipoUsuario;

/**
 * Servidor HTTP real (o MockMvc nao simula o despacho de erro do container). Com default-deny, o redirecionamento
 * interno para /error nao carrega o Bearer: sem a regra de DispatcherType.ERROR, um 400 de um usuario autenticado
 * viraria 401.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestPropertySource(properties = "app.jwt.secret=" + ComSeguranca.SEGREDO_TESTE)
public class ErroAutenticadoServidorRealTest {

	@LocalServerPort
	private int porta;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private JwtTokenService tokens;

	private int postar(String caminho, String corpo, String bearer) throws Exception {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(corpo));
		if (bearer != null) {
			b.header("Authorization", "Bearer " + bearer);
		}
		return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
	}

	@Test
	public void erroDeUsuarioAutenticadoNaoViraNaoAutenticado() throws Exception {
		br.com.fateczs.seazs.model.Usuario admin = DadosTeste.usuario("real-" + System.nanoTime() + "@teste.com");
		admin.setAdm(true);
		admin.setTipoUsuario(TipoUsuario.FUNCIONARIO);
		String token = tokens.emitir(usuarios.save(admin)).valor();

		assertEquals(400, postar("/papel/inserir", "{json quebrado", token));
		assertEquals(401, postar("/papel/inserir", "{json quebrado", null));
		assertEquals(200, postar("/evento/listar", "", token));
	}

	@Test
	public void erro500_naoVazaTraceNemMensagemInterna() throws Exception {
		br.com.fateczs.seazs.model.Usuario u = DadosTeste.usuario("erro500-" + System.nanoTime() + "@teste.com");
		String token = tokens.emitir(usuarios.save(u)).valor();

		HttpResponse<String> resposta = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/v1/teste/erro-interno"))
						.header("Accept", "application/json").header("Authorization", "Bearer " + token).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(500, resposta.statusCode());
		String corpo = resposta.body();
		for (String proibido : new String[] { "trace", "segredo-interno", "for update", "tb_refresh_token",
				"IllegalStateException", "br.com.fateczs" }) {
			assertFalse(corpo.contains(proibido), "a resposta 500 vazou '" + proibido + "': " + corpo);
		}
	}
}
