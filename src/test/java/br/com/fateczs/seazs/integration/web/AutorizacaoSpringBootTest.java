package br.com.fateczs.seazs.integration.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.fateczs.seazs.integration.support.ComSeguranca;
import br.com.fateczs.seazs.integration.support.DadosTeste;
import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.AgendamentoRepository;
import br.com.fateczs.seazs.repository.AtividadeRepository;
import br.com.fateczs.seazs.repository.EventoRepository;
import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.repository.PapelRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.security.JwtTokenService;
import br.com.fateczs.seazs.utils.TipoUsuario;
import tools.jackson.databind.json.JsonMapper;

/**
 * Matriz de autorizacao ponta a ponta no contexto real (H2): JWT reais emitidos pelo {@link JwtTokenService}, filtros,
 * method security, {@code Autorizacao} com repositorios reais. Os casos finos (regras de posse) ficam em unit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestPropertySource(properties = "app.jwt.secret=" + ComSeguranca.SEGREDO_TESTE)
public class AutorizacaoSpringBootTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JsonMapper json;
	@Autowired
	private JwtTokenService tokens;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PapelRepository papeis;
	@Autowired
	private EventoRepository eventos;
	@Autowired
	private AgendamentoRepository agendamentos;
	@Autowired
	private AtividadeRepository atividades;
	@Autowired
	private InscricaoRepository inscricoes;

	private Usuario admin;
	private Usuario ana;
	private Usuario beto;
	private Papel papel;
	private Atividade atividade;
	private Inscricao inscricaoDaAna;

	private Usuario novo(String prefixo, TipoUsuario tipo, boolean adm) {
		Usuario u = DadosTeste.usuario(prefixo + "-" + UUID.randomUUID() + "@teste.com");
		u.setTipoUsuario(tipo);
		u.setAdm(adm);
		return usuarios.save(u);
	}

	@BeforeEach
	public void montar() {
		admin = novo("admin", TipoUsuario.FUNCIONARIO, true);
		ana = novo("ana", TipoUsuario.ALUNO, false);
		beto = novo("beto", TipoUsuario.ALUNO, false);
		papel = papeis.save(DadosTeste.papel("Ouvinte-" + UUID.randomUUID()));
		Evento evento = eventos.save(DadosTeste.evento("Semana", admin));
		Agendamento agendamento = agendamentos
				.save(DadosTeste.agendamento(evento, admin, DadosTeste.PASSADO, DadosTeste.FUTURO));
		atividade = atividades.save(DadosTeste.atividade("Palestra", agendamento, DadosTeste.PASSADO, DadosTeste.FUTURO));
		inscricaoDaAna = inscricoes.save(DadosTeste.inscricao(atividade, ana, papel, DadosTeste.PASSADO, false, false, 0));
	}

	private MockHttpServletRequestBuilder comoUsuario(Usuario quem, MockHttpServletRequestBuilder req) {
		return req.header("Authorization", "Bearer " + tokens.emitir(quem).valor()).contentType(MediaType.APPLICATION_JSON);
	}

	private String chamar(Usuario quem, String caminho, Object corpo, int statusEsperado) throws Exception {
		MockHttpServletRequestBuilder req = post(caminho).contentType(MediaType.APPLICATION_JSON)
				.content(corpo instanceof String ? (String) corpo : json.writeValueAsString(corpo));
		if (quem != null) {
			req = comoUsuario(quem, req);
		}
		org.springframework.mock.web.MockHttpServletResponse r = mvc.perform(req).andReturn().getResponse();
		String resposta = r.getContentAsString();
		assertEquals(statusEsperado, r.getStatus(), caminho + " -> " + resposta);
		assertFalse(resposta.contains("\"senha\""), "nenhuma resposta pode conter senha");
		return resposta;
	}

	private static String novoPapel() {
		return "{\"nome\":\"Papel-" + UUID.randomUUID() + "\",\"descricao\":\"d\"}";
	}

	private static String id(Usuario u) {
		return "{\"id\":" + u.getId() + "}";
	}

	// ---- 401 / 403 / 200 por perfil

	@Test
	public void semToken_toda_rota_protegida_responde401() throws Exception {
		for (String rota : new String[] { "/evento/listar", "/usuario/listar", "/papel/inserir", "/atividade/inscricao/listar",
				"/certificado/atividade/emitir", "/api/v1/usuarios/1/perfil" }) {
			chamar(null, rota, "{}", 401);
		}
	}

	@Test
	public void leituraDeEventosEAbertaAQualquerAutenticado_escritaSoAdmin() throws Exception {
		chamar(ana, "/evento/listar", "{}", 200);
		chamar(ana, "/papel/inserir", novoPapel(), 403);
		chamar(admin, "/papel/inserir", novoPapel(), 200);
	}

	@Test
	public void listagensSensiveisSaoSoDoAdmin() throws Exception {
		chamar(ana, "/usuario/listar", "{}", 403);
		chamar(ana, "/atividade/inscricao/listar", "{}", 403);
		chamar(ana, "/atividade/inscricao/listarAtividade", json.writeValueAsString(atividade), 403);
		chamar(ana, "/usuario/disciplina/listar", "{}", 403);
		String lista = chamar(admin, "/usuario/listar", "{}", 200);
		assertTrue(lista.contains(ana.getEmail()));
	}

	// ---- IDOR

	@Test
	public void usuarioNaoLeNemAlteraNemExcluiNemEmiteCrachaDeOutro() throws Exception {
		String corpoDaAna = "{\"id\":" + ana.getId() + ",\"email\":\"" + ana.getEmail() + "\",\"senha\":\"Segredo123\"}";
		chamar(beto, "/usuario/buscar", corpoDaAna, 403);
		chamar(beto, "/usuario/cracha", id(ana), 403);
		chamar(beto, "/usuario/excluir", corpoDaAna, 403);
		chamar(beto, "/usuario/alterar", completo(ana, "Hack"), 403);
		chamar(beto, "/usuario/disciplina/DisciplinasUsuario", id(ana), 403);
		chamar(ana, "/usuario/cracha", id(ana), 200);
		chamar(admin, "/usuario/cracha", id(ana), 200);
	}

	private String completo(Usuario u, String nome) {
		return "{\"id\":" + u.getId() + ",\"email\":\"" + u.getEmail() + "\",\"senha\":\"Nova12345\",\"nome\":\"" + nome
				+ "\",\"endereco\":\"R\",\"celular\":\"1\",\"dataNasc\":631152000000,\"rg\":\"1\",\"genero\":\"F\"}";
	}

	@Test
	public void usuarioNaoAcessaInscricoesPresencaPontosNemCertificadosDeOutro() throws Exception {
		chamar(beto, "/atividade/inscricao/listarUsuario", id(ana), 403);
		chamar(beto, "/atividade/inscricao/somarPontos", id(ana), 403);
		chamar(beto, "/atividade/inscricao/listarAtividadesInscrito", id(ana), 403);
		chamar(beto, "/atividade/listarDisponiveis", id(ana), 403);
		chamar(beto, "/agendamento/staff/AgendamentosUsuario", id(ana), 403);
		chamar(beto, "/atividade/inscricao/validaPresenca", inscricaoDaAna, 403);
		chamar(beto, "/atividade/inscricao/excluir", inscricaoDaAna, 403);
		chamar(beto, "/atividade/inscricao/buscar", inscricaoDaAna, 403);
		chamar(beto, "/certificado/atividade/emitir", inscricaoDaAna, 403);
		chamar(ana, "/atividade/inscricao/listarUsuario", id(ana), 200);
		chamar(admin, "/atividade/inscricao/listarUsuario", id(ana), 200);
	}

	// ---- mass assignment

	@Test
	public void inscreverOutroUsuarioEhNegado_eCamposDePresencaEIdDoClienteSaoIgnorados() throws Exception {
		Atividade outraAtividade = atividades.save(DadosTeste.atividade("Oficina", atividade.getAgendamento(),
				DadosTeste.PASSADO, DadosTeste.FUTURO));
		Inscricao paraOutro = DadosTeste.inscricao(outraAtividade, ana, papel, DadosTeste.PASSADO, true, true, 999);
		chamar(beto, "/atividade/inscricao/inserir", paraOutro, 403);

		// Beto se inscreve com checkIn/pontos forjados e tentando sobrescrever a inscricao da Ana pelo id.
		Inscricao propria = DadosTeste.inscricao(outraAtividade, beto, papel, DadosTeste.PASSADO, true, true, 999);
		propria.setSequencialInscricao(inscricaoDaAna.getSequencialInscricao());
		chamar(beto, "/atividade/inscricao/inserir", propria, 200);

		Inscricao salva = inscricoes.findByUsuarioIdAndAtividadeId(beto.getId(), outraAtividade.getId());
		assertFalse(salva.getCheckIn());
		assertFalse(salva.getCheckOut());
		assertEquals(0, salva.getPontuacaoRecebida());
		assertEquals(ana.getId(), inscricoes.findById(inscricaoDaAna.getSequencialInscricao()).get().getUsuario().getId());
	}

	@Test
	public void alterarInscricaoEStaffEOperacoesDeEscritaDeEstruturaSaoSoAdmin() throws Exception {
		chamar(ana, "/atividade/inscricao/alterar", inscricaoDaAna, 403);
		chamar(ana, "/agendamento/staff/inserir", "{}", 403);
		chamar(ana, "/atividade/alterar", atividade, 403);
		chamar(ana, "/papel/excluir", papel, 403);
	}

	// ---- promocao por admin

	@Test
	public void alterarCadastroNuncaMudaPerfil_soAPromocaoDoAdminMuda_eNovoTokenTrazOPerfilNovo() throws Exception {
		// alterar proprio cadastro tentando virar admin: ignorado
		String corpo = completo(ana, "Ana").replace("\"genero\":\"F\"", "\"genero\":\"F\",\"adm\":true,\"tipoUsuario\":\"FUNCIONARIO\"");
		chamar(ana, "/usuario/alterar", corpo, 200);
		Usuario depois = usuarios.findById(ana.getId()).get();
		assertFalse(depois.getAdm());
		assertEquals(TipoUsuario.ALUNO, depois.getTipoUsuario());

		String promocao = "{\"tipoUsuario\":\"FUNCIONARIO\",\"adm\":true}";
		put403(ana, "/api/v1/usuarios/" + ana.getId() + "/perfil", promocao);
		String resposta = mvc.perform(comoUsuario(admin, put("/api/v1/usuarios/" + ana.getId() + "/perfil").content(promocao)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(resposta.contains("ROLE_ADMIN"));
		assertFalse(resposta.contains(ana.getEmail()));

		// o token antigo da Ana segue com os papeis antigos ate expirar; o novo traz o perfil novo
		Usuario aposPromocao = usuarios.findById(ana.getId()).get();
		assertTrue(aposPromocao.getAdm());
		String tokenAntigo = tokens.emitir(depois).valor();
		mvc.perform(post("/usuario/listar").header("Authorization", "Bearer " + tokenAntigo)).andExpect(status().isForbidden());
		chamar(aposPromocao, "/usuario/listar", "{}", 200);
	}

	private void put403(Usuario quem, String caminho, String corpo) throws Exception {
		mvc.perform(comoUsuario(quem, put(caminho).content(corpo))).andExpect(status().isForbidden());
	}

	@Test
	public void promocao_validaCorpo_usuarioInexistenteEAutoRebaixamento() throws Exception {
		mvc.perform(comoUsuario(admin, put("/api/v1/usuarios/" + ana.getId() + "/perfil").content("{\"adm\":true}")))
				.andExpect(status().isBadRequest());
		mvc.perform(comoUsuario(admin, put("/api/v1/usuarios/" + ana.getId() + "/perfil").content("{\"tipoUsuario\":\"REI\",\"adm\":true}")))
				.andExpect(status().isBadRequest());
		mvc.perform(comoUsuario(admin, put("/api/v1/usuarios/999999/perfil").content("{\"tipoUsuario\":\"ALUNO\",\"adm\":false}")))
				.andExpect(status().isNotFound());
		mvc.perform(comoUsuario(admin, put("/api/v1/usuarios/" + admin.getId() + "/perfil").content("{\"tipoUsuario\":\"ALUNO\",\"adm\":false}")))
				.andExpect(status().isConflict());
		assertTrue(usuarios.findById(admin.getId()).get().getAdm());
	}

	// ---- token de usuario excluido

	@Test
	public void tokenDeUsuarioExcluidoValeAteExpirar_documentado() throws Exception {
		String token = tokens.emitir(beto).valor();
		chamar(admin, "/usuario/excluir", "{\"id\":" + beto.getId() + ",\"email\":\"" + beto.getEmail() + "\",\"senha\":\"Segredo123\"}", 200);
		assertTrue(usuarios.findById(beto.getId()).isEmpty());

		// Limitacao conhecida (README): o JWT e stateless, entao o access token ainda autentica ate expirar (15 min).
		mvc.perform(post("/evento/listar").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		// ...mas nao enxerga nada de ninguem: posse e por id e a conta nao existe mais.
		mvc.perform(post("/usuario/cracha").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content(id(ana))).andExpect(status().isForbidden());
	}

	@Test
	public void cadastroELoginPublicosNaoExigemToken() throws Exception {
		chamar(null, "/usuario/login", "{\"email\":\"x@y.com\",\"senha\":\"abc\"}", 200);
		chamar(null, "/api/v1/auth/login", "{\"email\":\"x@y.com\",\"senha\":\"abc\"}", 401);
	}

	@Test
	public void donoGravadoDaInscricaoEResolvidoPeloBanco() {
		assertEquals(ana.getId(), inscricoes.findIdUsuarioBySequencial(inscricaoDaAna.getSequencialInscricao()).get());
		assertTrue(inscricoes.findIdUsuarioBySequencial(-1).isEmpty());
	}
}
