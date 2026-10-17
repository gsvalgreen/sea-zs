package br.com.fateczs.seazs.integration.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fateczs.seazs.controller.UsuarioController;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.UsuarioService;

/**
 * Serializacao/desserializacao JSON de {@link UsuarioController} com o contexto web real
 * (Jackson do Boot, validacao, propriedades do application.properties) e o service mockado.
 *
 * Migracao: Boot 3 = {@code javax.validation} -> {@code jakarta.validation} (nada aqui usa); com JUnit 5
 * remover {@code @RunWith}; Boot 3.4+/4 troca {@code @MockBean} por {@code @MockitoBean};
 * Boot 4 move {@code @WebMvcTest} para {@code org.springframework.boot.webmvc.test.autoconfigure}.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(UsuarioController.class)
public class UsuarioControllerWebMvcTest {

	@Autowired
	private MockMvc mvc;
	@MockBean
	private UsuarioService usuarioService;

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
		when(usuarioService.validarLogin(any(Usuario.class))).thenReturn(usuarioCompleto());

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.senha").doesNotExist());
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
		when(usuarioService.validarLogin(any(Usuario.class))).thenReturn(usuarioCompleto());

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("ana@teste.com"))
				.andExpect(jsonPath("$.turno").value("NOITE"))
				.andExpect(jsonPath("$.tipoUsuario").value("ALUNO"))
				.andExpect(jsonPath("$.adm").value(false))
				.andExpect(jsonPath("$.curso").doesNotExist());
	}

	/** Protege `spring.jackson.time-zone` do application.properties: datas saem em America/Sao_Paulo (-03:00; Jackson 2.11+ serializa o offset com dois-pontos, antes era -0300). */
	@Test
	public void login_serializaDatasComoStringIsoNoFusoDeSaoPaulo() throws Exception {
		Usuario u = usuarioCompleto();
		Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		utc.clear();
		utc.set(2020, Calendar.JUNE, 15, 15, 0, 0);
		u.setDataNasc(utc.getTime());
		when(usuarioService.validarLogin(any(Usuario.class))).thenReturn(u);

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(jsonPath("$.dataNasc").value("2020-06-15T12:00:00.000-03:00"));
	}

	@Test
	public void login_comCredenciaisInvalidas_respondeOkComCorpoVazio() throws Exception {
		when(usuarioService.validarLogin(any(Usuario.class))).thenReturn(null);

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	public void login_aceitaSenhaNoCorpoDaRequisicao() throws Exception {
		when(usuarioService.validarLogin(any(Usuario.class))).thenReturn(null);

		mvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_USUARIO));

		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioService).validarLogin(capturado.capture());
		assertEquals("segredo123", capturado.getValue().getSenha());
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
}
