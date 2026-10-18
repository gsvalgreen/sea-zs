package br.com.fateczs.seazs.integration.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fateczs.seazs.controller.UsuarioController;
import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.dto.LoginLegadoResponse;
import br.com.fateczs.seazs.service.AuthService;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.UsuarioService;

/**
 * Serializacao/desserializacao JSON de {@link UsuarioController} com o contexto web real
 * (Jackson do Boot, validacao, propriedades do application.properties) e o service mockado.
 *
 * O service e substituido por {@code @MockitoBean}; {@code @WebMvcTest} vem de {@code org.springframework.boot.webmvc.test.autoconfigure}.
 */
@WebMvcTest(UsuarioController.class)
@ComSeguranca
public class UsuarioControllerWebMvcTest {

	@Autowired
	private MockMvc mvc;
	@MockitoBean
	private UsuarioService usuarioService;
	@MockitoBean
	private AuthService authService;

	private static final String CORPO_USUARIO = "{\"id\":1,\"email\":\"ana@teste.com\",\"senha\":\"segredo123\"}";

	private static Usuario usuarioCompleto() {
		Usuario u = DadosTeste.usuario("ana@teste.com");
		u.setId(1);
		return u;
	}

	// ---- CONTRATO DE SEGURANCA (hotfix) -------------------------------------------------------
	// Usuario.senha e @JsonProperty(access = WRITE_ONLY): aceita na entrada, nunca serializada na saida.
	// --------------------------------------------------------------------------------------------

	@Test
	public void login_naoExpoeASenhaNaResposta() throws Exception {
		when(authService.loginLegado(any(), any())).thenReturn(Optional.of(new LoginLegadoResponse(usuarioCompleto(), "jwt-de-teste")));

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	public void login_devolveOTokenEmCampoNovoMantendoOsCamposAntigos() throws Exception {
		when(authService.loginLegado(any(), any())).thenReturn(Optional.of(new LoginLegadoResponse(usuarioCompleto(), "jwt-de-teste")));

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(jsonPath("$.token").value("jwt-de-teste"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("ana@teste.com"));
	}

	@Test
	public void buscar_naoExpoeASenhaNaResposta() throws Exception {
		when(usuarioService.buscar(any(Usuario.class))).thenReturn(usuarioCompleto());

		mvc.perform(post("/usuario/buscar").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	public void listar_naoExpoeASenhaDeNenhumUsuario() throws Exception {
		Usuario outro = DadosTeste.usuario("bia@teste.com");
		outro.setSenha("outraSenha");
		when(usuarioService.listar()).thenReturn(Arrays.asList(usuarioCompleto(), outro));

		mvc.perform(post("/usuario/listar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].senha").doesNotExist())
				.andExpect(jsonPath("$[1].senha").doesNotExist());
	}

	// ---- Contrato JSON atual --------------------------------------------------------------------

	@Test
	public void login_serializaEnumsPeloNomeEOsDemaisCamposPeloNomeDaPropriedade() throws Exception {
		when(authService.loginLegado(any(), any())).thenReturn(Optional.of(new LoginLegadoResponse(usuarioCompleto(), "jwt-de-teste")));

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("ana@teste.com"))
				.andExpect(jsonPath("$.turno").value("NOITE"))
				.andExpect(jsonPath("$.tipoUsuario").value("ALUNO"))
				.andExpect(jsonPath("$.adm").value(false))
				.andExpect(jsonPath("$.curso").doesNotExist());
	}

	/** Protege `spring.jackson.time-zone` do application.properties: datas saem em America/Sao_Paulo (-03:00 com dois-pontos). */
	@Test
	public void login_serializaDatasComoStringIsoNoFusoDeSaoPaulo() throws Exception {
		Usuario u = usuarioCompleto();
		Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		utc.clear();
		utc.set(2020, Calendar.JUNE, 15, 15, 0, 0);
		u.setDataNasc(utc.getTime());
		when(authService.loginLegado(any(), any())).thenReturn(Optional.of(new LoginLegadoResponse(u, "jwt-de-teste")));

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(jsonPath("$.dataNasc").value("2020-06-15T12:00:00.000-03:00"));
	}

	@Test
	public void login_comCredenciaisInvalidas_respondeOkComCorpoVazio() throws Exception {
		when(authService.loginLegado(any(), any())).thenReturn(Optional.empty());

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	public void login_aceitaSenhaNoCorpoDaRequisicao() throws Exception {
		when(authService.loginLegado(any(), any())).thenReturn(Optional.empty());

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO));

		verify(authService).loginLegado("ana@teste.com", "segredo123");
	}

	// ---- Validacao e mass assignment (caracterizacao) -------------------------------------------

	@Test
	public void inserir_semEmailNemSenha_respondeBadRequestSemChamarOService() throws Exception {
		mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\"}"))
				.andExpect(status().isBadRequest());

		verify(usuarioService, never()).inserir(any(Usuario.class));
	}

	/**
	 * Caracterizacao (mass assignment): o cadastro publico aceita `adm` e `tipoUsuario` enviados pelo cliente.
	 * A Fase 2 (seguranca) deve forcar esses campos no servidor; este teste entao deve ser invertido.
	 */
	@Test
	public void inserir_hoje_aceitaAdmETipoUsuarioEnviadosPeloCliente() throws Exception {
		String corpo = "{\"email\":\"x@teste.com\",\"senha\":\"s\",\"adm\":true,\"tipoUsuario\":\"PROFESSOR\"}";

		mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isOk());

		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioService).inserir(capturado.capture());
		assertTrue(capturado.getValue().getAdm());
	}

	@Test
	public void inserir_comTurnoInvalido_respondeBadRequest() throws Exception {
		String corpo = "{\"email\":\"x@teste.com\",\"senha\":\"s\",\"turno\":\"MADRUGADA\"}";

		mvc.perform(post("/usuario/inserir").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest());
	}

	// ---- Seguranca das rotas legadas (passo 2a) ---------------------------------------------------

	/**
	 * Documenta o estado do passo 2a: as rotas legadas seguem ABERTAS (o front atual nao envia Bearer).
	 * TODO(2b): ao fechar por RBAC, este teste deve ser invertido (401 sem token).
	 */
	@Test
	public void rotaLegada_hoje_continuaAbertaSemToken() throws Exception {
		when(usuarioService.listar()).thenReturn(Arrays.asList(usuarioCompleto()));

		mvc.perform(post("/usuario/listar"))
				.andExpect(status().isOk());
	}

	@Test
	public void rotaLegada_comBearerInvalido_respondeUnauthorizedEmProblemJson() throws Exception {
		mvc.perform(post("/usuario/listar").header("Authorization", "Bearer lixo"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
	}

	@Test
	public void preflightDoLoginLegado_deOrigemPermitida_ehAceito() throws Exception {
		mvc.perform(options("/usuario/login").header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
	}
}
