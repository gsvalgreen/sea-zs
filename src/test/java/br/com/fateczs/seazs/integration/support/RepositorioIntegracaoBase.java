package br.com.fateczs.seazs.integration.support;

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Base dos testes de repositorio: H2 em memoria no modo MySQL (perfil "integration").
 *
 * Divergencias H2 x MySQL a ter em mente (nao cobertas aqui, ver featureTest):
 * - comparacao de strings: o MySQL e case-insensitive por padrao, o H2 nao (afeta getByEmailAndSenha);
 * - geracao de ID: "native" vira SEQUENCE no H2 e AUTO_INCREMENT no MySQL (nunca assumir valores de ID);
 * - agregacoes: SUM devolve BigDecimal no MySQL e BigInteger/Long no H2 (o Spring converte para Integer);
 * - nomes de tabela: o MySQL em Linux e case-sensitive para tabelas, o H2 nao.
 *
 * Migracao para JUnit 5: remover {@code @RunWith(SpringRunner.class)} (o @DataJpaTest ja inclui o
 * SpringExtension) e trocar {@code org.junit.Test} por {@code org.junit.jupiter.api.Test}.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
public abstract class RepositorioIntegracaoBase {
}
