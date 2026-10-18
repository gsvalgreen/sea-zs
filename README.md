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

## Segurança (autenticação)

A API usa Spring Security como *resource server* JWT (HS256), stateless (sem sessão e sem CSRF: o token vai no header `Authorization: Bearer`, não em cookie).

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `JWT_SECRET` | **sem padrão (obrigatória)** | Segredo HS256, mínimo 32 bytes. A aplicação **não sobe** sem ele. Gere com `openssl rand -base64 48`; nunca versione. |
| `JWT_ISSUER` | `seazs` | Emissor (`iss`) validado nos tokens. |
| `JWT_ACCESS_TTL` | `PT15M` | Validade do access token (ISO-8601). |
| `JWT_REFRESH_TTL` | `P7D` | Validade do refresh token (ISO-8601). |
| `CORS_ALLOWED_ORIGINS` (`app.cors.allowed-origins`) | `http://localhost:4200` | Origens permitidas, separadas por vírgula; curingas (`*`) são recusados. |

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
./gradlew bootRun
```

Endpoints: `POST /api/v1/auth/login` (`{"email","senha"}` -> access + refresh token; 401 genérico "Credenciais inválidas" para e-mail inexistente ou senha errada; 400 para validação), `POST /api/v1/auth/refresh` (troca o refresh token por um novo par; o antigo é revogado a cada uso e reapresentá-lo revoga a sessão inteira) e `POST /api/v1/auth/logout` (204). O refresh token é opaco e só o hash SHA-256 fica no banco (`tb_refresh_token`). Rotas `/api/v1/**` (exceto `auth`) exigem Bearer; 401/403 saem em `application/problem+json`. As rotas legadas (`/usuario/**` etc.) seguem abertas até o passo 2b (RBAC). `POST /usuario/login` é **deprecated**: mantém o contrato antigo (200 com corpo vazio se inválido) e devolve o campo extra `token`; use `/api/v1/auth/login`.

> **AVISO DE SEGURANÇA (até o passo 2b):** as rotas legadas continuam **sem autenticação nem autorização**. Qualquer pessoa pode, por exemplo, chamar `/usuario/alterar` com o id de outro usuário e **trocar a senha dele (tomada de conta)**, `/usuario/excluir` para apagar contas, e `/usuario/buscar`, `/usuario/listar` e `/usuario/cracha` para **ler dados pessoais** (nome, e-mail, RG, celular, endereço etc.) de qualquer usuário; as demais rotas legadas (eventos, atividades, inscrições, certificados...) também aceitam leitura e escrita anônimas. **NÃO use esta versão fora de desenvolvimento** nem exponha a porta na internet/rede compartilhada. Só `/api/v1/**` (exceto `/api/v1/auth/**`) exige Bearer.

Cadastro com e-mail já existente responde `409` (`application/problem+json`, mensagem genérica sem repetir o e-mail). Isso confirma que o e-mail existe (enumeração), mas num cadastro aberto sem confirmação por e-mail o usuário precisa dessa resposta; o login segue sem diferenciar e-mail inexistente de senha errada. O limite de tentativas (rate limiting) ainda **não existe**: login, refresh e cadastro permitem força bruta/enumeração em massa até o passo 2b.

Higiene de tokens: um job diário (`LimpezaRefreshTokens`, 03:30; cron em `app.jwt.limpeza-cron`) apaga refresh tokens expirados há mais de 7 dias. Limitações conhecidas do access token (JWT HS256, 15 min): as roles ficam no token, então rebaixar/excluir um usuário só vale quando o token expira (no máximo `JWT_ACCESS_TTL`); o segredo simétrico sem `kid` não suporta rotação sem derrubar as sessões.

Senhas: `DelegatingPasswordEncoder` com BCrypt. O cadastro (`/usuario/inserir`) e a alteração gravam BCrypt (`{bcrypt}...`). Senhas antigas em texto puro (sem prefixo) continuam válidas e são regravadas como BCrypt no primeiro login bem-sucedido. O BCrypt só considera 72 bytes: senhas maiores são recusadas (400) no cadastro e no login (usuário legado com senha maior que 72 bytes não consegue logar e precisa de redefinição). A comparação de senha agora diferencia maiúsculas de minúsculas.
Senha vazia ou só com espaços é recusada (400) no cadastro e no login; no legado (`/usuario/login`) vira o 200 com corpo vazio de sempre.

### Cadastro público, perfis e primeiro administrador

O cadastro público (`/usuario/inserir`) **ignora** `adm`, `tipoUsuario` e `id` enviados pelo cliente: grava sempre `adm=false`, `tipoUsuario=VISITANTE` (perfil de menor privilégio; `PROFESSOR`/`FUNCIONARIO` podem receber privilégios no RBAC do passo 2b e `ALUNO` não é verificável no cadastro aberto) e um id novo. `/usuario/alterar` também não altera `adm`/`tipoUsuario`: mantém os valores gravados. Consequência: administradores e demais perfis passam a depender de promoção por um admin (passo 2b) ou de carga via SQL/migração. Enquanto o 2b não existe, crie o primeiro admin cadastrando o usuário normalmente e promovendo-o direto no banco:

```sql
UPDATE tb_usuario SET adm = 1, tipo_usuario = 3 WHERE email_usuario = 'admin@exemplo.com';  -- tipo: 1 VISITANTE, 2 PROFESSOR, 3 FUNCIONARIO, 4 ALUNO
```

O novo perfil entra no token no próximo login (o JWT já emitido continua com os papéis antigos até expirar).

### Migração das senhas legadas (`{noop}`)

O encoder ainda aceita senhas antigas em texto puro (`{noop}`) até serem regravadas em BCrypt no primeiro login. Dois pontos transitórios:

- **Oráculo de tempo:** a verificação de usuário `{noop}` é muito mais rápida que a de BCrypt, então a latência pode diferenciar usuário legado de usuário BCrypt (e, em menor grau, de e-mail inexistente). Some quando todas as senhas forem migradas.
- **Quando remover o `{noop}`:** rode `select count(*) from tb_usuario where senha not like '{bcrypt}%';`. Com resultado 0, remova o `{noop}` do `DelegatingPasswordEncoder`. Usuários que nunca logam continuam pendentes; avalie forçar redefinição de senha.

## Schema e migrações (Flyway)

O schema versionado fica em `src/main/resources/db/migration/` (`V1__baseline.sql` reproduz o que o Hibernate gera para as entidades, no MySQL 8.4). Toda mudança de entidade que altere o schema exige uma nova migração `V<n>__descricao.sql`.

| Ambiente | Variáveis | Comportamento |
| --- | --- | --- |
| Local (padrão) | nenhuma | `ddl-auto=update`, Flyway desligado (o Hibernate cria/atualiza as tabelas). |
| Não local (homologação/produção) | `FLYWAY_ENABLED=true` `DDL_AUTO=validate` `SHOW_SQL=false` | Flyway aplica as migrações e o Hibernate apenas valida; o Hibernate só confere a existência e o tipo de tabelas e colunas (não confere NOT NULL, FKs, UNIQUE, defaults nem tamanho de varchar). |

Banco vazio: a `V1` é executada. Banco legado (criado antes pelo Hibernate, sem histórico do Flyway): só é adotado com `FLYWAY_BASELINE_ON_MIGRATE=true` (padrão `false`, use apenas no primeiro deploy e desligue depois). A `V1` é marcada como aplicada sem ser executada nem comparada ao banco; se ele divergir, o Flyway não avisa. **Antes de ligar o Flyway num banco legado, ele precisa estar idêntico ao baseline** (`turno` varchar(1), collation `utf8mb4_unicode_ci`, nomes das FKs, etc.) ou receber uma migração corretiva (`V3` ou posterior)/`ALTER` manual. Depois do baseline (`V1`), o Flyway aplica normalmente as migrações seguintes (a `V2` cria `tb_refresh_token`, com `IF NOT EXISTS`). Não misture os modos no mesmo banco após adotar o Flyway: com `FLYWAY_ENABLED=true` use sempre `DDL_AUTO=validate`.

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
