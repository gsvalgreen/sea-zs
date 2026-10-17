package br.com.fateczs.seazs.featureTest.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

import com.jayway.jsonpath.JsonPath;

/**
 * Base dos featureTest: sobe a aplicacao inteira (controllers -> services -> JPA) contra um MySQL 5.7 real
 * em container, com os mesmos parametros do Dockerfile do projeto. Sem Docker os testes sao PULADOS
 * (Assume) localmente, mas FALHAM quando a variavel de ambiente CI=true (GitHub Actions).
 *
 * Isolamento: o container e unico para toda a JVM de teste; cada teste cria seus proprios dados com sufixo
 * unico ({@link #unico(String)}) e nao depende de dados pre-existentes nem da ordem de execucao.
 *
 * Relogio: o sistema ainda usa o relogio real (ver Fase 1 do plano, injecao de Clock). Por isso as janelas
 * de presenca sao montadas relativas a "agora" com margem de minutos; trocar por Clock fixo apos o refactor.
 *
 * Migracao: JUnit 5 = {@code @ExtendWith(SpringExtension.class)} via {@code @SpringBootTest} (remover @RunWith)
 * e {@code @Testcontainers(disabledWithoutDocker = true)}; Boot 3.1+ = {@code @ServiceConnection} no lugar do
 * inicializador abaixo; Boot 3 = driver {@code com.mysql.cj.jdbc.Driver}/{@code mysql:8.4}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = FeatureTestBase.MySqlInitializer.class)
public abstract class FeatureTestBase {

	protected static final long MINUTO = 60_000L;
	protected static final long HORA = 60 * MINUTO;
	protected static final long DIA = 24 * HORA;

	private static MySQLContainer<?> mysql;

	@Autowired
	protected MockMvc mvc;

	@BeforeClass
	public static void exigirDocker() {
		boolean docker = DockerClientFactory.instance().isDockerAvailable();
		if ("true".equalsIgnoreCase(System.getenv("CI"))) {
			Assert.assertTrue("Docker indisponivel no CI: featureTest nao pode ser pulado", docker);
		} else {
			Assume.assumeTrue("Docker indisponivel: featureTest pulado", docker);
		}
		iniciarMySql();
	}

	private static synchronized void iniciarMySql() {
		if (mysql == null) {
			mysql = new MySQLContainer<>("mysql:5.7")
					.withDatabaseName("seazs")
					.withUsername("root")
					.withPassword("system")
					.withUrlParam("useSSL", "false")
					.withCommand("--ssl=0", "--character-set-server=utf8mb4",
							"--collation-server=utf8mb4_unicode_ci", "--default-time-zone=-03:00");
			mysql.start();
		}
	}

	public static class MySqlInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext context) {
			TestPropertyValues.of(
					"spring.datasource.url=" + mysql.getJdbcUrl(),
					"spring.datasource.username=" + mysql.getUsername(),
					"spring.datasource.password=" + mysql.getPassword()).applyTo(context);
		}
	}

	// ---- helpers de chamada ---------------------------------------------------------------------

	protected static String unico(String prefixo) {
		return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/** POST JSON esperando HTTP 200; devolve o corpo (pode ser vazio). */
	protected String postar(String caminho, String json) throws Exception {
		return mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	/** POST JSON esperando HTTP 200; devolve o primeiro id da lista filtrada por um JsonPath. */
	protected int primeiroId(String caminhoListagem, String corpo, String filtroJsonPath) throws Exception {
		List<Integer> ids = JsonPath.read(postar(caminhoListagem, corpo), filtroJsonPath);
		return ids.get(0);
	}

	// ---- helpers de cenario (sempre pelos controllers) ------------------------------------------

	protected int criarUsuario(String email, String senha) throws Exception {
		postar("/usuario/inserir", "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\","
				+ "\"nome\":\"Nome " + email + "\",\"endereco\":\"Rua A, 1\",\"celular\":\"11999999999\","
				+ "\"dataNasc\":631152000000,\"semestre\":3,\"turno\":\"NOITE\",\"adm\":false,"
				+ "\"rg\":\"123456789\",\"genero\":\"F\",\"matricula\":\"MAT-1\",\"tipoUsuario\":\"ALUNO\"}");
		return primeiroId("/usuario/listar", "{}", "$[?(@.email=='" + email + "')].id");
	}

	protected int criarPapel(String nome) throws Exception {
		postar("/papel/inserir", "{\"nome\":\"" + nome + "\",\"descricao\":\"d\"}");
		return primeiroId("/papel/listar", "{}", "$[?(@.nome=='" + nome + "')].id");
	}

	protected int criarEvento(String nome, int idCriador, long inicio, long fim) throws Exception {
		postar("/evento/inserir", "{\"nome\":\"" + nome + "\",\"descricao\":\"d\",\"inicio\":" + inicio
				+ ",\"fim\":" + fim + ",\"usuarioCriador\":{\"id\":" + idCriador + "}}");
		return primeiroId("/evento/listar", "{}", "$[?(@.nome=='" + nome + "')].id");
	}

	protected int criarAgendamento(int idEvento, int idOrganizador, long inicio, long fim, int pontosStaff)
			throws Exception {
		postar("/agendamento/inserir", "{\"evento\":{\"id\":" + idEvento + "},\"usuarioOrganizador\":{\"id\":"
				+ idOrganizador + "},\"inicio\":" + inicio + ",\"fim\":" + fim + ",\"pontuacaoStaff\":" + pontosStaff + "}");
		return primeiroId("/agendamento/listarEvento", "{\"id\":" + idEvento + "}", "$[*].id");
	}

	protected int criarAtividade(String nome, int idAgendamento, long inicio, long fim, boolean continua)
			throws Exception {
		postar("/atividade/inserir", "{\"agendamento\":{\"id\":" + idAgendamento + "},\"nome\":\"" + nome
				+ "\",\"categoria\":\"Palestra\",\"localAtividade\":\"Sala 1\",\"inicioAtividade\":" + inicio
				+ ",\"fimAtividade\":" + fim + ",\"cargaHoraria\":2,\"qtdVagas\":50,\"pontuacaoParticipante\":10,"
				+ "\"flagContinua\":" + continua + ",\"flagCumulativa\":false,\"qtdCheckIn\":1}");
		return primeiroId("/atividade/listarAgendamento", "{\"id\":" + idAgendamento + "}",
				"$[?(@.nome=='" + nome + "')].id");
	}

	protected void inscrever(int idAtividade, int idUsuario, int idPapel) throws Exception {
		postar("/atividade/inscricao/inserir", referenciaInscricao(idAtividade, idUsuario, idPapel));
	}

	protected static String referenciaInscricao(int idAtividade, int idUsuario, int idPapel) {
		return "{\"atividade\":{\"id\":" + idAtividade + "},\"usuario\":{\"id\":" + idUsuario
				+ "},\"papel\":{\"id\":" + idPapel + "}}";
	}

	/** Inscreve o usuario como staff do agendamento e valida a presenca (check-in) dele. */
	protected void inscreverStaffEValidarPresenca(int idAgendamento, int idUsuario, long dtInscricao) throws Exception {
		postar("/agendamento/staff/inserir", "{\"agendamento\":{\"id\":" + idAgendamento + "},\"usuario\":{\"id\":"
				+ idUsuario + "},\"dtInscricao\":" + dtInscricao + ",\"checkIn\":false,\"checkOut\":false}");
		postar("/agendamento/staff/ValidarPresenca",
				"{\"agendamento\":{\"id\":" + idAgendamento + "},\"usuario\":{\"id\":" + idUsuario + "}}");
	}
}
