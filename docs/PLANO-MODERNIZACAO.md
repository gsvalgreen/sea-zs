# Plano: modernização da stack, segurança e API orientada a telas (sea-zs)

## Checklist de progresso

Legenda: `[x]` feito, revisado (`revisor-codigo`) e com passada de `seguranca-codigo-limpo`; `[~]` em andamento; `[ ]` pendente.
Última atualização: 2026-10-17 (fim do dia). Tudo está em branches locais (nada foi enviado ao GitHub, nenhum PR aberto).

**Estado das branches (repositório local)**
- `master`: intacta, sem nada deste trabalho.
- `modernizacao`: integra apenas a Fase 0 (hotfix + CI/devops). Está atrasada.
- `feat/fase1-stack`: Fase 0 completa + Fase 1 inteira (6 passos).
- `feat/fase2-seguranca`: parte de `feat/fase1-stack`; contém a Fase 2, passo 2a. O passo 2b está implementado (HEAD `6c18719`), aguardando revisão e passada de segurança.
- Trabalho feito no worktree `.claude/worktrees/agent-a6c1b76aff9a45895`. Sugestão: um PR por fase, com squash-merge (alguns commits intermediários não compilam sozinhos).

### Fase 0, preparação
- [x] Hotfix: `Usuario.senha` WRITE_ONLY, credenciais do banco por `${DB_*}`, remoção de `System.out` com dados sensíveis
- [x] docker-compose do MySQL, `.env.example`, README
- [x] CI no GitHub Actions: build, testes, JaCoCo com gate de cobertura, Sonar (workflow criado e revisado)
- [x] Baseline de testes: unit, integration (H2) e featureTest (Testcontainers com MySQL real) no padrão da pirâmide
- [x] `java.time.Clock` injetável (semestres testados de forma determinística)

### Fase 1, stack incremental
- [x] 1. `build.gradle` limpo, Gradle 6.9
- [x] 2. Boot 2.7, Java 17, JUnit 5, MySQL 8.4, `mysql-connector-j`
- [x] 3. Boot 3.5: `jakarta.*`, `getReferenceById`, `GenerationType.IDENTITY`
- [x] 4. Java 25 e Gradle 9.8
- [x] 5. Boot 4.1.1 (Spring 7, Hibernate 7, Jackson 3, JUnit 6, Testcontainers 2)
- [x] 6. Flyway (`V1__baseline.sql`) com `ddl-auto=validate` fora do ambiente local

### Fase 2, segurança
- [x] 2a. Autenticação: Spring Security 7, JWT HS256, refresh token rotativo, BCrypt com migração de senhas legadas, `/api/v1/auth/*`, CORS global, `equals/hashCode` de `Usuario` por `id`, cadastro público sem escalada de privilégio, 409 para e-mail duplicado
- [x] 2b. Autorização: default-deny, matriz de perfis por endpoint, ownership/IDOR, promoção de perfil por admin (`PUT /api/v1/usuarios/{id}/perfil`), correção do 409 vs. campo obrigatório ausente. IMPLEMENTADO (commits 9a9a6fd, f236fab, 6c18719; 331 unit + 127 integration + 18 featureTest verdes). O `revisor-codigo` REPROVOU; achados B1, I1 a I4 e S1 a S4 CORRIGIDOS (ver abaixo) e matriz nova (ADMIN/FUNCIONARIO escrevem estruturas; PROFESSOR só cria evento/agendamento/atividade) aplicada. Passada do `seguranca-codigo-limpo` FEITA (achados abaixo); o `revisor-codigo` revisou as alterações dela e APROVOU COM RESSALVAS (ressalvas atendidas). 2b CONCLUÍDO
- [x] 2b. Rate limit por IP e lockout progressivo por e-mail (implementação própria em memória, sem Bucket4j), 429 com `Retry-After` (revisado pelo `revisor-codigo`, aprovado com ressalvas, e passado pelo `seguranca-codigo-limpo`)
- [~] 2c. EM ANDAMENTO. Frontend (`sea-zs-angular`, subagent `dev-frontend`, sem modernizar a stack do front): `type="password"`, interceptor Bearer, guard de rota, `environment.apiUrl`, não persistir dados pessoais, tratamento de falha de login, tratar a mudança de data `-0300` para `-03:00`
  - Achados do front no backend (2c), tratados: `Retry-After` agora exposto no CORS (`Access-Control-Expose-Headers`, sem curinga); claim `name` no JWT de acesso (so o nome; o front deixa de perder o nome ao recarregar; vale ate o token expirar); documentacao confere MySQL 8.4 (Dockerfile, compose, README, CLAUDE.md); 500 com stack trace NAO reproduzido no backend (teste de servidor real garante ausencia de trace/SQL/mensagem): provavel instancia/versao antiga ou perfil com `server.error.include-*` sobrescrito (nao verificado), reabrir se reaparecer com a versao atual
  - Itens do front que viram Fase 3: endpoint "meus dados" para o `form-usuario-log`; `IAgendamento.usuarioOrganizador` como string; radios de tipo de usuario sem efeito; refresh proativo do access token; `apiUrl` de producao; `ng build --prod` ja quebrado antes da integracao; testes antigos do front falhando
  - Dividas da passada de seguranca do front (2c; sem correcao por exigirem mudanca ampla ou da stack): (1) access e refresh token em `sessionStorage` ficam legiveis por qualquer XSS; mitigar na Fase 3 com refresh em cookie HttpOnly+SameSite (exige mudanca no backend) e CSP; (2) `npm audit --omit=dev` aponta 6 advisories (4 altos em `@angular/common`/`compiler`/`core`/`router` 6.1.0, corrigidos so em Angular 19.2.26+ ou 20+; 2 moderados em `bootstrap` 4.1.3 e `jquery` 3.3.1): relevantes sobretudo se o app passar a renderizar HTML dinamico (hoje nao ha `innerHTML` nem `bypassSecurityTrust*`); resolver ao modernizar o front; (3) o front envia `usuarioCriador` (id e nome do JWT) no corpo de `/evento/inserir`: o backend deve usar sempre o `sub` do token e ignorar o campo; (4) o checkbox 'Lembrar-me' do login nao faz nada (sessao so em `sessionStorage`); remover ou implementar; (5) 12 de 35 specs do front falham (specs gerados pelo CLI sem providers/imports, alguns agravados pela injecao do `AuthService`); os 23 novos de auth/datas passam; (6) `console.log` de dados de tela em `form-evento`, `form-curso`, `painel-eventos`, `consulta-evento` (anteriores a integracao) e `href` de exemplo (`facebook.com`, `google.com`) em `form-curso`
- [ ] Remover `{noop}` do encoder quando `select count(*) from tb_usuario where senha not like '{bcrypt}%'` retornar 0

**Achados da revisão do 2b (a corrigir ANTES de seguir; o `dev-backend` é o autor)**
- [x] B1 (BLOQUEANTE): o rate limit por IP é contornável com byte codificado na URL (`POST /usuario/%6cogin`, `/api/v1/auth/%6cogin`): `LimiteTentativasInterceptor` usa `getRequestURI()` (não decodificado) como chave do escopo. Resolver pelo caminho decodificado/normalizado (ex.: `UrlPathHelper`/`ServletRequestPathUtils`) e testar que `/usuario/%6cogin` também dá 429. Escrever o teste antes da correção (deve falhar)
- [x] I1: falha aberta no limitador: IPv6 por endereço completo (um /64 contorna) e, com o teto de 100000 entradas cheio, IPs/e-mails novos deixam de ser limitados. Agrupar IPv6 por /64, evictar expirados antes de desistir, falhar fechado no escopo de login
- [x] I2: a chave do lockout (trim + minúsculas) não acompanha a collation `utf8mb4_unicode_ci` do banco (acentos e caracteres ignoráveis autenticam a mesma conta). Normalizar (NFKD, sem marcas/ignoráveis) ou chavear pelo id do usuário resolvido
- [x] I3: corrida entre `exigirLiberado` e `registrarFalha` (rajada acima do limite) e varredura que pode perder contagem. Reservar a tentativa atomicamente (`compute`)
- [x] I4: proteção da última conta ADMIN: hoje só bloqueia o auto-rebaixamento; dois admins podem se rebaixar em paralelo e o admin pode se excluir via `/usuario/excluir`. Contar admins com lock e recusar se sobrar zero
- [x] S1: refresh tokens revogados ao mudar perfil (`alterarPerfil`) ou senha (`alterar`). [~] S2: `/usuario/alterar` segue sem exigir a senha atual (exigir seria quebra de contrato legado; documentado no README, resolver no `/api/v1` da Fase 3)
- [x] S3: o login legado pode devolver 429 (status novo no contrato legado): mantido e documentado no README; o front trata no 2c
- [x] S4: `ControllersProtegidosTest` não detecta `@PreAuthorize("permitAll()")` nem controller fora do pacote `controller`; faltam testes de caminho codificado e IPv6

**Passada de segurança e código limpo do 2b (`seguranca-codigo-limpo`)**
- [x] I-2: lost update entre `UsuarioServiceImpl.alterar` e `alterarPerfil`. `alterar` agora copia só os dados cadastrais para a entidade gravada e `Usuario` é `@DynamicUpdate` (o UPDATE não regrava `adm`/`tipoUsuario`); teste unitário
- [x] I-1: teste de concorrência do último ADMIN (`UltimoAdministradorConcorrenciaSpringBootTest`, H2, duas threads, 15 rodadas; falha sem o `@Lock`, passa com ele). Fidelidade ao InnoDB (MySQL) ainda não verificada: [ ] repetir como featureTest com MySQL (Testcontainers)
- [x] S-2 (README do lockout), S-3 (assert real ao excluir vínculo), S-4 (IP por contador no lugar de `Math.random()`)
- [ ] Dívida: `/usuario/alterar` trocando o e-mail para um já cadastrado gera `DataIntegrityViolationException` (500); mapear para 409 como no cadastro (Fase 3)

### Fase 3, API orientada a telas (`/api/v1`)
- [ ] DTOs, `@RestControllerAdvice` com `ProblemDetail`, exceções de domínio (404/409/422)
- [ ] Mappers (MapStruct) e paginação
- [ ] `open-in-view=false`, `fetch = LAZY` nos `@ManyToOne`, `@EntityGraph`/projeções, `findById` com 404 no lugar de proxy
- [ ] Endpoints por tela: painel, detalhe do evento, forms, minha área, inscrição, presença, certificado, crachá, inscritos, relatório
- [ ] springdoc/OpenAPI e cliente TypeScript gerado
- [ ] Migrar o front tela por tela, depreciar e remover os endpoints antigos; decidir o destino do `Gerenciamento*`
- [ ] `@Temporal` deprecado no Hibernate 7 (9 campos `Date`): migrar para `java.time` junto com os DTOs, avaliando o tipo da coluna

### Pendências e decisões em aberto
- [ ] Rodar o CI no GitHub pela primeira vez (nunca executou): conferir `setup-java` 25, Testcontainers no runner e se o Sonar aceita `sonar.java.source=25`
- [ ] Configurar no GitHub: secret `SONAR_TOKEN`; variáveis `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`
- [ ] Definir `JWT_SECRET` (mínimo de 32 bytes) em cada ambiente; em produção `DDL_AUTO=validate`, `FLYWAY_ENABLED=true`, `SHOW_SQL=false`
- [ ] Usuário de banco da aplicação (DML) separado do usuário de migração (DDL); remover `root/system` fora do dev local
- [ ] Banco legado existente: comparar com o `V1` e usar `FLYWAY_BASELINE_ON_MIGRATE=true` só no primeiro deploy
- [ ] Fixar as actions do CI por SHA (o Dependabot já está configurado)
- [ ] Remover os overrides de Tomcat e Jackson no `build.gradle` quando o BOM do Boot trouxer versões iguais ou maiores
- [ ] Avançar a branch `modernizacao`, abrir os PRs e fazer squash-merge
- [ ] Rate limit/lockout em memória não serve para várias instâncias e zera ao reiniciar (futuro: Redis ou JDBC)
- [ ] Dívidas herdadas apontadas no 2b: `certificado/emitir` dá NPE/500 para ADMIN quando o registro não existe; `usuario/buscar` exige e-mail e senha no corpo; `Curso(String)` quebra a desserialização; services sem `@Transactional` (Fase 3)
- [x] Decisão 1 (resolvida): ADMIN e FUNCIONARIO escrevem estruturas; PROFESSOR só cria evento, agendamento e atividade (matriz no README)
- [x] Decisão 2 (resolvida): cadastro público sempre VISITANTE/`adm=false`
- [ ] O JSON de `Evento.usuarioCriador` e `Agendamento.usuarioOrganizador` não traz mais dados pessoais: conferir o impacto no front (2c)
- [ ] Revisar `docs/PLANO-MODERNIZACAO.md` e o `CLAUDE.md` no fim de cada fase

### Como retomar
1. `cd` para o worktree acima e `git checkout feat/fase2-seguranca`.
2. Variáveis de ambiente para os featureTest no Colima: `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`, `JAVA_HOME=$HOME/.sdkman/candidates/java/25.0.4+1.1-tem`.
3. `CI=true ./gradlew clean check` deve estar verde antes de continuar.
4. Fluxo por tarefa: autor (`dev-backend`/`qa-testes`/`devops`/`dev-frontend`) → `revisor-codigo` → `seguranca-codigo-limpo` → `revisor-codigo` nas alterações → integração local.

---

## O que estudar antes de implementar

**Plataforma e build**
- Novidades do Java 9 a 25: módulos (JPMS), `var`, `record`, `sealed`, switch expressions, text blocks, virtual threads.
- Gradle moderno: configurações `implementation`/`runtimeOnly`/`developmentOnly`, toolchains, wrapper, BOM e `io.spring.dependency-management`, migração Gradle 4 -> 9.
- SDKMAN (`.sdkmanrc`) e gerenciamento de JDKs.

**Spring Boot e persistência**
- Guias oficiais de migração do Spring Boot 2.x -> 3.x e 3.x -> 4.x, e o que muda no Spring Framework 6/7.
- `javax.*` -> `jakarta.*` e ferramentas de migração automática (OpenRewrite).
- Auto-configuração, `application.properties` por perfil, externalização de configuração e segredos (variáveis de ambiente).
- JPA/Hibernate 5 -> 6/7: estratégias de geração de ID (`IDENTITY` x `SEQUENCE` x `AUTO`), `@GenericGenerator`, dialetos, `fetch` LAZY x EAGER, problema N+1, `@EntityGraph`, `JOIN FETCH`, projeções do Spring Data, open-in-view (OSIV), `@Transactional`.
- `getOne` x `getReferenceById` x `findById` e proxies LAZY.
- Migração de schema com Flyway ou Liquibase e `ddl-auto`.
- MySQL 5.7 -> 8: `caching_sha2_password`, collations, SQL mode (`ONLY_FULL_GROUP_BY`), driver `mysql-connector-j`, Docker.

**Testes**
- Pirâmide de testes (Fowler) como descrita no `CLAUDE.md`: unit, integration, featureTest.
- JUnit 5 (migração do JUnit 4), Mockito, `@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`.
- Testcontainers (`@ServiceConnection`), H2, WireMock, testes de caracterização para código legado.

**Segurança**
- Spring Security 6/7: `SecurityFilterChain`, `AuthenticationManager`, `UserDetailsService`, `PasswordEncoder`, `@PreAuthorize`/method security.
- Hash de senha: BCrypt, `DelegatingPasswordEncoder`, migração de hashes (`upgradeEncoding`), limite de 72 bytes do BCrypt.
- JWT: estrutura, assinatura, expiração, refresh token, armazenamento seguro no cliente (cookie `HttpOnly` x `sessionStorage`).
- Sessão x token (stateless), CSRF, CORS (preflight, `CorsConfigurationSource`).
- OWASP Top 10 e ASVS: IDOR, mass assignment, exposição excessiva de dados, rate limiting e lockout (Bucket4j).
- LGPD: dados pessoais e minimização na resposta da API.
- RBAC e modelagem de papéis/permissões.

**Design de API**
- REST: verbos HTTP, códigos de status, `Location`, idempotência, nomenclatura de recursos, versionamento (`/api/v1`).
- DTOs, padrão BFF (Backend for Frontend) e APIs orientadas a caso de uso/tela.
- MapStruct (mapeamento DTO <-> entidade).
- Bean Validation (`@Valid`, grupos, mensagens) e `@RestControllerAdvice`.
- `ProblemDetail` / RFC 7807 para erros.
- Paginação com `Pageable`/`Page`.
- OpenAPI/Swagger, springdoc e geração de cliente TypeScript (`openapi-generator`).
- Estratégias de depreciação e migração incremental de endpoints.

**Frontend (Angular)**
- `HttpClient`, interceptors, route guards, `environment` e proxy de desenvolvimento.
- Reactive Forms (`type="password"`), tratamento de erros HTTP.
- Fluxo de autenticação com JWT no cliente e consumo de cliente gerado por OpenAPI.

**Práticas gerais**
- Refatoração incremental com testes (Strangler Fig / branch by abstraction).
- Logging com SLF4J (sem `System.out`, sem dados sensíveis) e Actuator seguro.
- Git: branches por fase, commits pequenos.

---

## Context
O projeto roda em Java 8 e Spring Boot 2.1.6 (starters 2.0.3), Gradle 4.5, MySQL 5.7 (EOL). Três problemas motivam o trabalho:
1. Stack defasada (Java, Spring, Gradle, Hibernate, driver, Docker).
2. Segurança: senha em texto puro, login sem token, sem Spring Security, sem autorização, entidade `Usuario` (com senha e dados pessoais) serializada em várias respostas.
3. API centrada em entidades (11 controllers CRUD, só `POST`, entidades JPA sem DTO, sem paginação nem tratamento de erros), que força o Angular (`gsvalgreen/sea-zs-angular`) a over/under-fetching.

Resultado esperado: stack em Java 25 + Boot 4.x (via 3.5.x), autenticação com BCrypt + JWT, API `/api/v1` por tela com DTOs, paginação e erros padronizados.

## Achados (resumo das 3 investigações)

### Stack
- Alvo: Java 25 (já instalado: `25.0.4-tem`), Boot 4.0.x/4.1.x (confirmar versão exata em spring.io), Gradle 9.x. A linha 3.5 serve de etapa intermediária.
- 22 arquivos com `javax.*` (96 imports; só `persistence` e `validation`), sem `javax.servlet`.
- `build.gradle`: remover versões fixas dos starters, `compile/testCompile`, `apply plugin: 'maven'`, `groovy`. Adicionar `spring-boot-starter-validation` (44 `@Valid`), toolchain Java, `devtools` como `developmentOnly`.
- `application.properties`: remover `naming-strategy`, `MySQL5InnoDBDialect`, `testWhileIdle/validationQuery`. Driver `com.mysql.cj.jdbc.Driver`. Corrigir a codificação dos comentários.
- Teste `contextLoads`: JUnit 4 -> 5; depende de MySQL ativo (usar Testcontainers/H2).
- Dockerfile: `mysql:8.4`, remover `--ssl=0` e `--platform=linux/amd64`. `.sdkmanrc` -> `java=25.0.4-tem`.
- Riscos: `GenerationType.AUTO` + `@GenericGenerator` (9 entidades) muda para sequência emulada em Hibernate 6/7 (declarar `IDENTITY`); 11 `getOne` -> `getReferenceById/findById`; Hibernate 5->7 com 4 `@Query`; MySQL 5.7->8; cobertura de testes mínima.
- Esclarecer a divergência entre relatórios (Boot 2.0.3 vs plugin 2.1.6 + starters 2.0.3) lendo `build.gradle` antes de começar.

### Segurança
- Crítico: senha em texto puro (`UsuarioServiceImpl.java:27-29`); login por `getByEmailAndSenha` sem token e devolvendo a entidade; `senha` e dados pessoais em `/usuario/listar`, `/buscar`, `/login` e embutidos em `Inscricao`/`StaffAgendamento`/`Agendamento`; sem autorização (mass assignment de `adm`/`tipoUsuario`, IDOR).
- Alto: credenciais `root/system` versionadas (rotacionar); validação mínima; CORS pontual; só `POST`; actuator sem proteção.
- Médio: `show-sql`, `System.out.println`, `equals/hashCode` com `senha`, `ddl-auto=update`, dependências com CVEs.
- Sem SQL injection encontrado (queries parametrizadas).
- Front: campo de senha `type="text"`, senha em claro no POST e devolvida na resposta, `sessionStorage` com todos os dados, autorização confiada ao cliente, sem guard/interceptor, `toString()` em campos nulos quebra o login.

### API vs telas
- Front real consome pouco: login, painel de eventos (`/evento/listar`), detalhe do evento (`/agendamento/listarEvento`), form de evento/curso/usuário. Inscrição, presença, certificados, crachá, pontuação e staff não têm tela.
- Over-fetching: cada resposta traz Evento/Usuario/Curso completos. Chamada morta `getNumeroInscritos`. `consulta-atividade` passa o objeto pela URL.
- `@ManyToOne` EAGER por padrão, `findAll()` sem fetch, ciclo Agendamento<->Atividade cortado com `@JsonIgnoreProperties`, dependência do OSIV, sem `@Transactional`, `getOne` com proxy LAZY.
- `CertificadoServiceImpl` devolve 200 vazio / NPE; `Gerenciamento*` é código morto (semestre 1 começa em 31/01).

## Abordagem recomendada

### Fase 0, preparação
- Criar branch. Decidir o destino das alterações pendentes do wrapper.
- Confirmar a linha de base em Java 8 e MySQL via Docker. Escrever testes de caracterização mínimos (ver Testing do CLAUDE.md: unit/integration/featureTest), em especial para presença (`InscricaoServiceImpl.validaPresenca`) e certificados.
- Débito de testabilidade do baseline: `InscricaoServiceImpl.somaTotalDePontosNoSemestre` (e `DataUtils.getDataAtual`) dependem do relógio do sistema, então o teste unitário só exercita o semestre vigente a cada execução e deixa o outro sem cobertura. Injetar um `java.time.Clock` (bean) nesses pontos durante a Fase 1, para testar os dois semestres de forma determinística.
- Hotfix: `@JsonProperty(access = WRITE_ONLY)` em `Usuario.senha`; externalizar e rotacionar o segredo do banco (`${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`).

### Fase 1, stack incremental
1. Limpar `build.gradle` (Boot 2.1.6) e subir Gradle para 6.9/7.6.
2. Boot 2.7.x + Java 17, JUnit 5, MySQL 8, driver `mysql-connector-j`.
3. Boot 3.5.x: `jakarta.*` (OpenRewrite ou sed), starter validation, `getOne`, geradores de ID, dialeto, Gradle 8.x.
4. Java 25 + Gradle 9.x; `.sdkmanrc`/toolchain.
5. Boot 4.x (guia oficial; atenção a Jackson 3 e módulos de teste).
6. Flyway com `ddl-auto=validate` nos ambientes não locais; Testcontainers no featureTest. (Feito: `V1__baseline.sql`, `FLYWAY_ENABLED`/`DDL_AUTO`, featureTest com Flyway + `validate`.)

### Fase 2, segurança
- Passo 2a (autenticação) implementado: JWT HS256 + refresh rotativo, BCrypt com migração de senhas, CORS global, `equals/hashCode` de `Usuario` por `id`. Pendentes: 2b (RBAC e fechamento das rotas legadas, mass assignment, IDOR), rate limit/lockout de login, limpeza periódica de refresh tokens expirados, front.
- `spring-boot-starter-security`, `DelegatingPasswordEncoder` (BCrypt), `UserDetailsService` por e-mail.
- Migração de senhas: prefixo `{noop}` nas existentes + rehash no próximo login (`upgradeEncoding`) ou reset.
- `POST /api/v1/auth/login` -> JWT de vida curta (+ refresh), 401 genérico, rate limit/lockout.
- RBAC: `TipoUsuario`/`adm` -> authorities; `SecurityFilterChain` por rota, `@PreAuthorize` com checagem do dono; cadastro público força `tipoUsuario`/`adm` no servidor.
- CORS global via propriedade; actuator restrito (`health`); remover `println`/`show-sql`; `equals/hashCode` por `id`.
- Frontend: `type="password"`, interceptor com Bearer, guard de rota, `environment.apiUrl`, não persistir dados pessoais, tratar falha de login.

### Fase 3, API orientada a telas (`/api/v1`, em paralelo aos endpoints antigos)
- DTOs request/response, mappers como beans (MapStruct), `@RestControllerAdvice` com `ProblemDetail`, exceções de domínio (404/409/422), `@Transactional`, `open-in-view=false`, `@EntityGraph`/projeções, `fetch = LAZY` nos `@ManyToOne`, paginação, springdoc/OpenAPI + cliente TS gerado para o Angular.
- Mapeamento principal:
  - Login: `POST /auth/login`
  - Cadastro: `POST /usuarios`
  - Painel: `GET /eventos?disponiveis=true&page=`
  - Detalhe: `GET /eventos/{id}/detalhe`
  - Form de evento: `POST /eventos` + `GET /cursos`
  - Minha área: `GET /me/resumo`
  - Inscrição: `POST /atividades/{id}/inscricoes`
  - Presença: `POST /inscricoes/{id}/presenca`
  - Certificado: `GET /inscricoes/{id}/certificado` (404/409 sem presença)
  - Crachá: `GET /usuarios/{id}/cracha`
  - Inscritos (admin): `GET /atividades/{id}/inscritos`
  - Relatório: `GET /relatorios/pontuacao`
- Depreciar os endpoints antigos, migrar o front tela por tela e removê-los no fim. Decidir o destino do `Gerenciamento*` (completar ou remover).

### Ordem e esforço
P0 (hotfix de segurança + Fase 0) -> Fase 1 (3 a 5 dias) -> Fase 2 -> Fase 3 por tela. Segurança (Fase 2) e Boot 3+ (Fase 1.3) devem ser coordenados, pois o Spring Security 6 muda a configuração.

## Arquivos críticos
- `build.gradle`, `gradle/wrapper/*`, `.sdkmanrc`, `Dockerfile`, `src/main/resources/application.properties`
- `src/main/java/br/com/fateczs/seazs/`: `model/Usuario.java` (+ demais entidades, `@GenericGenerator`/`AUTO`), `controller/*`, `service/serviceImpl/*` (`UsuarioServiceImpl`, `InscricaoServiceImpl`, `CertificadoServiceImpl`), `repository/UsuarioRepository.java`, `model/mapper/*`
- `src/test/java/br/com/fateczs/seazs/SeazsApplicationTests.java`
- Frontend: `src/app/app.module.ts`, `eventos/evento.service.ts`, `_service/curso.service.ts`, `usuario/login/*`, `model/IUsuario.ts`

## Verificação
- Cada etapa: `./gradlew build`; `contextLoads` e featureTest passam; `./gradlew test`.
- Fase 1: subir MySQL 8 com dados populados e validar IDs e queries (`@Query` de `InscricaoRepository`).
- Fase 2: testes de integração Spring Security (login válido/inválido, 401/403, IDOR, mass assignment). Confirmar que nenhuma resposta contém `senha`.
- Fase 3: testes unit/integration por endpoint (DTOs, paginação, erros), verificação de N+1 contando selects, contrato OpenAPI contra o Angular, smoke test manual de cada tela.
