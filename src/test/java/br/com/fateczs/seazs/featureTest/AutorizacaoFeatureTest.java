package br.com.fateczs.seazs.featureTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Fluxo 2 (MySQL real): cadastro publico -> login -> operacao permitida -> operacao negada -> promocao por admin ->
 * nova operacao. As regras finas ficam em unit/integration; aqui so o caminho feliz de ponta a ponta, incluindo a
 * violacao UNIQUE do e-mail traduzida pelo Hibernate no MySQL.
 */
public class AutorizacaoFeatureTest extends FeatureTestBase {

	private MockHttpServletRequestBuilder com(String token, MockHttpServletRequestBuilder req) {
		return req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
	}

	private String login(String email, String senha) throws Exception {
		String corpo = postar("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}");
		return JsonPath.read(corpo, "$.accessToken");
	}

	@Test
	public void cadastroLoginOperacaoPermitidaNegadaPromocaoENovaOperacao() throws Exception {
		String email = unico("fluxo") + "@teste.com";
		int id = criarUsuario(email, "Segredo123");
		String outro = unico("outro") + "@teste.com";
		int idOutro = criarUsuario(outro, "Segredo123");
		String token = login(email, "Segredo123");

		// permitido: ler eventos e o proprio cracha
		mvc.perform(com(token, post("/evento/listar"))).andExpect(status().isOk());
		mvc.perform(com(token, post("/usuario/cracha").content("{\"id\":" + id + "}"))).andExpect(status().isOk());
		// negado: cracha alheio, criar papel, listar usuarios
		mvc.perform(com(token, post("/usuario/cracha").content("{\"id\":" + idOutro + "}"))).andExpect(status().isForbidden());
		mvc.perform(com(token, post("/papel/inserir").content("{\"nome\":\"x\",\"descricao\":\"d\"}")))
				.andExpect(status().isForbidden());
		mvc.perform(com(token, post("/usuario/listar").content("{}"))).andExpect(status().isForbidden());

		// promocao pelo admin (que nasceu por SQL); o token antigo segue sem permissao, o novo passa
		mvc.perform(com(tokenAdmin(), put("/api/v1/usuarios/" + id + "/perfil")
				.content("{\"tipoUsuario\":\"FUNCIONARIO\",\"adm\":true}")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorities[1]").value("ROLE_ADMIN"));
		mvc.perform(com(token, post("/usuario/listar").content("{}"))).andExpect(status().isForbidden());
		String novoToken = login(email, "Segredo123");
		mvc.perform(com(novoToken, post("/usuario/listar").content("{}"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].senha").doesNotExist());
		mvc.perform(com(novoToken, post("/papel/inserir").content("{\"nome\":\"" + unico("p") + "\",\"descricao\":\"d\"}")))
				.andExpect(status().isOk());
	}

	@Test
	public void semToken_401_eEmailDuplicado_409_noMySql() throws Exception {
		mvc.perform(post("/usuario/listar").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());

		String email = unico("dup") + "@teste.com";
		criarUsuario(email, "Segredo123");
		mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"Segredo123\",\"nome\":\"N\",\"endereco\":\"R\","
						+ "\"celular\":\"1\",\"dataNasc\":631152000000,\"rg\":\"1\",\"genero\":\"F\"}"))
				.andExpect(status().isConflict());
		// campo obrigatorio ausente: 400 (nao 409)
		mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + unico("semnome") + "@teste.com\",\"senha\":\"Segredo123\"}"))
				.andExpect(status().isBadRequest());
	}
}
