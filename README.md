# sea-zs

## Participantes

1. Artur Anderson 
2. Daniel Campos
3. Gustavo Valverde
4. Felipe Moura
5. Juliane Santos

### Turma de 2016 (1º Semestre) - ADS Noite			

## Ambiente local

Requisitos: Java 25 (`sdk env`) e Docker. Stack: Spring Boot 4.1.x, Gradle 9.8.

```bash
cp .env.example .env          # ajuste se necessário; .env não é versionado
docker compose up -d          # sobe o MySQL (8.4) com healthcheck e volume nomeado
docker compose ps             # STATUS deve ficar "healthy"
./gradlew bootRun             # a aplicação usa localhost:3306, db seazs, root/system
docker compose down           # para (use -v para apagar o volume de dados)
```

O MySQL fica exposto apenas em `127.0.0.1`. A versão da imagem é controlada por `MYSQL_VERSION` no `.env`.

## Schema e migrações (Flyway)

O schema versionado fica em `src/main/resources/db/migration/` (`V1__baseline.sql` reproduz o que o Hibernate gera para as entidades, no MySQL 8.4). Toda mudança de entidade que altere o schema exige uma nova migração `V<n>__descricao.sql`.

| Ambiente | Variáveis | Comportamento |
| --- | --- | --- |
| Local (padrão) | nenhuma | `ddl-auto=update`, Flyway desligado (o Hibernate cria/atualiza as tabelas). |
| Não local (homologação/produção) | `FLYWAY_ENABLED=true` `DDL_AUTO=validate` `SHOW_SQL=false` | Flyway aplica as migrações e o Hibernate apenas valida; o Hibernate só confere a existência e o tipo de tabelas e colunas (não confere NOT NULL, FKs, UNIQUE, defaults nem tamanho de varchar). |

Banco vazio: a `V1` é executada. Banco legado (criado antes pelo Hibernate, sem histórico do Flyway): só é adotado com `FLYWAY_BASELINE_ON_MIGRATE=true` (padrão `false`, use apenas no primeiro deploy e desligue depois). A `V1` é marcada como aplicada sem ser executada nem comparada ao banco; se ele divergir, o Flyway não avisa. **Antes de ligar o Flyway num banco legado, ele precisa estar idêntico ao baseline** (`turno` varchar(1), collation `utf8mb4_unicode_ci`, nomes das FKs, etc.) ou receber uma `V2` corretiva/`ALTER` manual. Não misture os modos no mesmo banco após adotar o Flyway: com `FLYWAY_ENABLED=true` use sempre `DDL_AUTO=validate`.

Credenciais: `root`/`system` servem só para o dev local. Em ambientes reais a aplicação deve usar um usuário com privilégios mínimos (DML: SELECT/INSERT/UPDATE/DELETE) e as migrações devem rodar com um usuário DDL separado (`spring.flyway.user` e `spring.flyway.password`, via variáveis de ambiente), nunca root. A `V1` não contém `DROP`, `create schema`, `GRANT` nem dados.

## Testes e cobertura

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

O relatório fica em `build/reports/jacoco/test/`. O build falha se a cobertura ficar abaixo do limiar definido em `build.gradle` (instruções 90%, branches 70%). Os `featureTest` (inclusive o `contextLoads`) sobem seu próprio MySQL 8.4 via Testcontainers com Flyway ligado e `ddl-auto=validate`, o que confere que tabelas e colunas das entidades existem na `V1__baseline.sql` com tipo compatível (não confere NOT NULL, FKs, UNIQUE, defaults nem tamanho de varchar); exigem Docker.

## CI (GitHub Actions)

`.github/workflows/ci.yml` roda build, testes (com MySQL 8.4 como service), verificação de cobertura JaCoCo e análise SonarCloud em push e pull request. Configure em *Settings > Secrets and variables > Actions*:

| Nome | Tipo | Uso |
| --- | --- | --- |
| `SONAR_TOKEN` | secret | token do SonarCloud |
| `SONAR_ORGANIZATION` | variable | chave da organização no SonarCloud |
| `SONAR_PROJECT_KEY` | variable | chave do projeto no SonarCloud |
| `SONAR_HOST_URL` | variable (opcional) | padrão `https://sonarcloud.io`; defina para SonarQube próprio |

Sem `SONAR_TOKEN` (ex.: PR de fork) o passo do Sonar é pulado sem falhar.
