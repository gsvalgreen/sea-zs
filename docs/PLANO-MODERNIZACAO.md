# Plano: modernização da stack, segurança e API orientada a telas (sea-zs)

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
6. Flyway com `ddl-auto=validate` nos ambientes não locais; Testcontainers no featureTest.

### Fase 2, segurança
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
