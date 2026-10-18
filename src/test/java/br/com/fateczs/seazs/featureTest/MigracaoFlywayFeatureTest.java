package br.com.fateczs.seazs.featureTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import br.com.fateczs.seazs.SeazsApplication;
import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Caminho de banco existente: um banco criado pelo Hibernate (ddl-auto=update, sem historico do Flyway) deve
 * receber o baseline (V1 marcada como aplicada, sem reexecutar) e passar no ddl-auto=validate.
 * O caminho de banco vazio (V1 executada + validate) e provado pelo contexto de todos os demais featureTest.
 */
public class MigracaoFlywayFeatureTest extends FeatureTestBase {

	private static final String BANCO = "seazs_legado";

	@Test
	public void bancoCriadoPeloHibernateRecebeBaselineEPassaNoValidate() throws Exception {
		try (Connection c = DriverManager.getConnection(mysqlContainer().getJdbcUrl(), "root", "system");
				Statement st = c.createStatement()) {
			st.execute("create database " + BANCO + " default character set utf8mb4 collate utf8mb4_unicode_ci");
		}
		String url = "jdbc:mysql://" + mysqlContainer().getHost() + ":" + mysqlContainer().getMappedPort(3306) + "/" + BANCO;

		// 1) Hibernate cria o schema (comportamento do dev local atual), Flyway desligado.
		iniciar(url, "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=update").close();

		// 2) Ambiente nao local sobre o mesmo banco: Flyway + validate.
		try (ConfigurableApplicationContext ctx = iniciar(url, "spring.flyway.enabled=true",
				"spring.flyway.baseline-on-migrate=true", "spring.jpa.hibernate.ddl-auto=validate")) {
			try (Connection c = DriverManager.getConnection(url, "root", "system");
					Statement st = c.createStatement();
					ResultSet rs = st.executeQuery(
							"select version, type, success from flyway_schema_history order by installed_rank")) {
				rs.next();
				assertEquals("1", rs.getString("version"));
				assertEquals("BASELINE", rs.getString("type"));
				assertEquals(true, rs.getBoolean("success"));
			}
		}
	}

	private static ConfigurableApplicationContext iniciar(String url, String... propriedades) {
		// Argumentos de linha de comando tem precedencia sobre o application.properties (.properties() seria o menor).
		String[] args = new String[propriedades.length + 4];
		args[0] = "--spring.datasource.url=" + url;
		args[1] = "--spring.datasource.username=root";
		args[2] = "--spring.datasource.password=system";
		args[3] = "--spring.jpa.show-sql=false";
		for (int i = 0; i < propriedades.length; i++) {
			args[4 + i] = "--" + propriedades[i];
		}
		return new SpringApplicationBuilder(SeazsApplication.class).web(WebApplicationType.NONE).run(args);
	}
}
