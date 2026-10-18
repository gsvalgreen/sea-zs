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

Endpoints: `POST /api/v1/auth/login` (`{"email","senha"}` -> access + refresh token; 401 genérico "Credenciais inválidas" para e-mail inexistente ou senha errada; 400 para validação), `POST /api/v1/auth/refresh` (troca o refresh token por um novo par; o antigo é revogado a cada uso e reapresentá-lo revoga a sessão inteira) e `POST /api/v1/auth/logout` (204). O refresh token é opaco e só o hash SHA-256 fica no banco (`tb_refresh_token`). Todas as rotas, legadas e `/api/v1`, exigem Bearer (default-deny), exceto as públicas listadas em "Autorização"; 401/403 saem em `application/problem+json`. `POST /usuario/login` é **deprecated**: mantém o contrato antigo (200 com corpo vazio se inválido) e devolve o campo extra `token`; use `/api/v1/auth/login`.

Cadastro com e-mail já existente responde `409` (`application/problem+json`, mensagem genérica sem repetir o e-mail). Isso confirma que o e-mail existe (enumeração), mas num cadastro aberto sem confirmação por e-mail o usuário precisa dessa resposta; o login segue sem diferenciar e-mail inexistente de senha errada. Login, refresh e cadastro têm limite por IP e bloqueio por e-mail (veja "Rate limit e bloqueio").

Higiene de tokens: um job diário (`LimpezaRefreshTokens`, 03:30; cron em `app.jwt.limpeza-cron`) apaga refresh tokens expirados há mais de 7 dias. Limitações conhecidas do access token (JWT HS256, 15 min): as roles ficam no token, então rebaixar/excluir um usuário só vale quando o token expira (no máximo `JWT_ACCESS_TTL`); o segredo simétrico sem `kid` não suporta rotação sem derrubar as sessões.

Senhas: `DelegatingPasswordEncoder` com BCrypt. O cadastro (`/usuario/inserir`) e a alteração gravam BCrypt (`{bcrypt}...`). Senhas antigas em texto puro (sem prefixo) continuam válidas e são regravadas como BCrypt no primeiro login bem-sucedido. O BCrypt só considera 72 bytes: senhas maiores são recusadas (400) no cadastro e no login (usuário legado com senha maior que 72 bytes não consegue logar e precisa de redefinição). A comparação de senha agora diferencia maiúsculas de minúsculas.
Senha vazia ou só com espaços é recusada (400) no cadastro e no login; no legado (`/usuario/login`) vira o 200 com corpo vazio de sempre.

### Autorização (passo 2b)

Default-deny: `anyRequest().authenticated()`. Públicos: `/api/v1/auth/**`, `POST /usuario/login`, `POST /usuario/inserir`, `/actuator/health` e preflight CORS. O restante é decidido por `@PreAuthorize` em cada handler (method security); o teste `ControllersProtegidosTest` falha se um handler novo ficar sem a anotação. Perfis: `ROLE_<TipoUsuario>` (VISITANTE, PROFESSOR, FUNCIONARIO, ALUNO) e `ROLE_ADMIN` quando `adm=true`. "Dono" = o `sub` do JWT (id do usuário) é o usuário do recurso; nunca o que vem no corpo. Posse de inscrição/staff é conferida pelo registro gravado no banco.

Decisão (definida pelo dono do produto): a escrita de estrutura (evento, curso, disciplina, papel, agendamento, atividade, vínculo usuário-disciplina) é de **ADMIN e FUNCIONARIO** (tudo: inserir, alterar, excluir). **PROFESSOR só cria (`inserir`) evento, agendamento e atividade**: não edita nem exclui, e não toca em curso, disciplina, papel nem vínculo. O `inserir` do professor exige `id` nulo no corpo (com `id`, o `save` sobrescreveria um registro existente, o que seria edição disfarçada: 403). Leitura continua aberta a qualquer autenticado; os demais perfis (VISITANTE, ALUNO) não escrevem estrutura. Staff (`/agendamento/staff`), alterar inscrição, listagens sensíveis e a promoção de perfil seguem só ADMIN; `buscar`/`listar` de `/usuario/disciplina` também. O cadastro público grava sempre VISITANTE e `adm=false`. O front atual (sem Bearer) deixa de funcionar até o passo 2c.

| Recurso (prefixo) | Endpoint | Regra |
| --- | --- | --- |
| `/evento` | `listar`, `listarData`, `listarDisponivelData`, `buscar` | autenticado |
| | `inserir` | ADMIN, FUNCIONARIO ou PROFESSOR (só criar, `id` nulo) |
| | `alterar`, `excluir` | ADMIN ou FUNCIONARIO |
| `/agendamento` | `listar`, `listarData`, `listarEvento`, `buscar` | autenticado |
| | `inserir` | ADMIN, FUNCIONARIO ou PROFESSOR (só criar, `id` nulo) |
| | `alterar`, `excluir` | ADMIN ou FUNCIONARIO |
| `/atividade` | `listar`, `listarData`, `listarAgendamento`, `listarPassadas`, `buscar` | autenticado |
| | `listarDisponiveis` (corpo `usuario`) | dono ou ADMIN |
| | `inserir` | ADMIN, FUNCIONARIO ou PROFESSOR (só criar, `id` nulo) |
| | `alterar`, `excluir` | ADMIN ou FUNCIONARIO |
| `/curso`, `/disciplina`, `/papel` | `listar`, `buscar` | autenticado |
| | `inserir`, `inserirLote`, `alterar`, `excluir` | ADMIN ou FUNCIONARIO |
| `/usuario/disciplina` | `DisciplinasUsuario` (corpo `usuario`) | dono ou ADMIN |
| | `inserir`, `alterar`, `excluir` | ADMIN ou FUNCIONARIO |
| | `buscar`, `listar` | ADMIN |
| `/atividade/inscricao` | `inserir` (usuário do corpo = o do token) | dono ou ADMIN |
| | `buscar`, `excluir`, `validaPresenca` (inscrição gravada) | dono da inscrição ou ADMIN |
| | `listarUsuario`, `listarAtividadesInscrito`, `listarAtividadesParticipou`, `somarPontos`, `somarPontosSemestre` | dono ou ADMIN |
| | `alterar`, `listar`, `listarData`, `listarAtividade` | ADMIN |
| `/agendamento/staff` | `buscar` (registro gravado) | dono do registro ou ADMIN |
| | `ValidarPresenca`, `AgendamentosUsuario`, `AgendamentosUsuarioPassado` | dono ou ADMIN |
| | `inserir`, `alterar`, `excluir`, `listar`, `listarData` | ADMIN |
| `/certificado` | `atividade/emitir`, `agendamento/emitir` (usuário do corpo) | dono ou ADMIN |
| `/usuario` | `login`, `inserir` | público |
| | `buscar`, `alterar`, `excluir`, `cracha` | dono ou ADMIN |
| | `listar` | ADMIN |
| `/api/v1/usuarios/{id}/perfil` | `PUT` | ADMIN (nunca deixa o sistema sem ADMIN: 409) |
| `/api/v1/auth` | `login`, `refresh`, `logout` | público |
| Relatórios de pontuação | (`Gerenciamento*` não tem endpoint; é stub) | ADMIN quando existirem |

Comportamento:

- Sem token: 401. Perfil ou posse insuficiente: 403 (`application/problem+json`); recurso alheio e recurso inexistente dão o mesmo 403 para não-admin (sem oráculo de existência). A validação do corpo (`@Valid`) roda antes da autorização, então um corpo inválido responde 400 mesmo a quem não teria permissão.
- Última conta ADMIN: `PUT .../perfil` e `/usuario/excluir` recusam (409) a operação que deixaria o sistema sem administrador (a lista de admins é lida com lock de escrita, então dois rebaixamentos/exclusões concorrentes são serializados). O admin também não retira o próprio `adm`.
- Sessões: mudar o perfil (`PUT .../perfil`) ou a senha (`/usuario/alterar`) apaga os refresh tokens do usuário; o access token já emitido vale até expirar (15 min).
- `/usuario/alterar` não exige a senha atual (contrato legado: o corpo traz só a nova senha). Quem tem um access token válido pode trocá-la; mitigado pelo TTL curto e pela revogação dos refresh tokens. Exigir a senha atual é mudança de contrato, prevista para a API `/api/v1` (Fase 3).
- Mass assignment: `/usuario/alterar` e o cadastro nunca mudam `adm`/`tipoUsuario` (só o `PUT .../perfil`); `inscricao/inserir` ignora `checkIn`/`checkOut`/pontos/data/id enviados; alterar inscrição e todo o CRUD de staff são ADMIN. Inscrever ou emitir certificado para outro usuário é 403.
- Evento e agendamento são legíveis por qualquer autenticado, então `usuarioCriador`/`usuarioOrganizador` saem **sem** e-mail, endereço, celular, data de nascimento, RG, gênero, matrícula e `adm`. Nenhuma resposta contém `senha`.
- Limitações do JWT stateless: papéis ficam no token e valem até ele expirar (`JWT_ACCESS_TTL`, 15 min). Usuário excluído ou rebaixado ainda autentica até lá (mas, excluído, não é dono de nada); o refresh falha para usuário excluído. Dívida (Fase 3): DTOs de resposta em vez de entidades e `@Transactional` nos services.

### Rate limit e bloqueio

Em `POST /api/v1/auth/login`, `/api/v1/auth/refresh`, `/usuario/login` e `/usuario/inserir`:

- **Por IP** (janela fixa; `app.security.rate-limit.max-requisicoes`=10 e `janela`=PT1M, por escopo: login v1+legado, refresh, cadastro). O IP é `request.getRemoteAddr()`; `X-Forwarded-For` é **ignorado**. Atrás de proxy/LB confiável, configure `server.forward-headers-strategy=native` para o container resolver o IP real (nunca confie em header arbitrário).
- **Por e-mail** (só logins): `app.security.lockout.max-falhas`=5 falhas seguidas (intervalo máximo `janela-falhas`=PT15M) bloqueiam o e-mail por `duracao`=PT15M; cada novo bloqueio dobra até `duracao-maxima`=PT24H; sucesso zera tudo; `retencao`=PT24H esquece o histórico. Vale também para e-mail inexistente (resposta idêntica, sem oráculo) e o e-mail é normalizado (minúsculas, sem espaços). Bloqueado, nem a senha correta passa, e o legado responde 429 em vez do 200 vazio. Quem sabe o e-mail de alguém pode bloqueá-lo de propósito (negação de serviço pontual); o limite por IP reduz o alcance.
- Resposta: 429 `application/problem+json` com `Retry-After` (segundos). O login legado (`/usuario/login`) também pode responder 429 (antes só 200): é um status novo no contrato legado, mantido de propósito (um 200 vazio esconderia o bloqueio); o front trata isso no passo 2c.
- O escopo do limite por IP usa o caminho decodificado e normalizado (`/usuario/%6cogin`, `;x=1` e `//` caem no mesmo contador). IPv6 é agrupado por /64. Os mapas têm teto (`app.security.max-entradas`=100000): cheio, varre os vencidos e, se ainda faltar espaço, recusa chaves novas com 429 (falha fechada).
- A tentativa de login é reservada de forma atômica por e-mail antes de verificar a senha (uma rajada não passa de `max-falhas`); a chave do e-mail segue a collation `utf8mb4_unicode_ci` (caixa, acentos e caracteres ignoráveis são o mesmo e-mail).
- Armazenamento **em memória de uma instância** (mapas concorrentes com varredura de expirados e teto `app.security.max-entradas`=100000). Com várias instâncias cada uma conta separado: para multi-instância, migrar para Redis/JDBC. Reiniciar a aplicação zera os contadores. Sem dependência nova (não usa Bucket4j: o bloqueio progressivo não é um token bucket e o relógio injetável simplifica os testes).

### Cadastro público, perfis e primeiro administrador

O cadastro público (`/usuario/inserir`) **ignora** `adm`, `tipoUsuario` e `id` enviados pelo cliente: grava sempre `adm=false`, `tipoUsuario=VISITANTE` (perfil de menor privilégio; `PROFESSOR`/`FUNCIONARIO` podem receber privilégios no RBAC do passo 2b e `ALUNO` não é verificável no cadastro aberto) e um id novo. `/usuario/alterar` também não altera `adm`/`tipoUsuario`: mantém os valores gravados. Consequência: administradores e demais perfis dependem de promoção por um admin (`PUT /api/v1/usuarios/{id}/perfil`). O **primeiro** admin continua sendo criado por SQL (não existe admin para promovê-lo): cadastre o usuário normalmente e promova-o direto no banco:

```sql
UPDATE tb_usuario SET adm = 1, tipo_usuario = 3 WHERE email_usuario = 'admin@exemplo.com';  -- tipo: 1 VISITANTE, 2 PROFESSOR, 3 FUNCIONARIO, 4 ALUNO
```

O novo perfil entra no token no próximo login (o JWT já emitido continua com os papéis antigos até expirar). Depois do primeiro admin, use só a API:

```bash
curl -X PUT localhost:8080/api/v1/usuarios/42/perfil -H "Authorization: Bearer $TOKEN_ADMIN" -H 'Content-Type: application/json' -d '{"tipoUsuario":"FUNCIONARIO","adm":true}'
```

Só ADMIN chama; responde 200 com `{id, nome, tipoUsuario, authorities}`, 404 se o usuário não existe e 409 se o admin tentar retirar o próprio `adm`. Cada mudança gera um log `perfil alterado: usuario=.. tipoUsuario=.. adm=.. por=..` (só ids).

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
