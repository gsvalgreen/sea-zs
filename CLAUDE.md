# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`sea-zs` (Gradle project `seazs`) is a Spring Boot / Java web app (currently Boot 3.5.x on Java 17, Gradle 8.14, being modernized per `docs/PLANO-MODERNIZACAO.md`; `build.gradle` and `.sdkmanrc` are the source of truth for versions) (FATEC ZS, ADS Noite 2016/1) for managing academic events: events, activities (`Atividade`), schedules (`Agendamento`), enrollments (`Inscricao`), staff assignments, courses/subjects, users/roles, attendance certificates and a semester score report. Domain names are in Portuguese.

## Commands

- The JDK version is pinned in `.sdkmanrc` (currently Java 17; run `sdk env`) and in the Gradle toolchain in `build.gradle`.
- Database: MySQL 8.4 on `localhost:3306`, db `seazs`, user `root` / password `system` (see `src/main/resources/application.properties`). Start it with the repo `Dockerfile`:
  `docker build -t seazs-mysql . && docker run -d -p 3306:3306 --name seazs-mysql seazs-mysql`, or `docker compose up -d` (see `README.md`)
- Tests (Gradle tasks, one per pyramid layer, split by package under `src/test`):
  - `./gradlew test` runs only `unit` (fast, no Spring/DB/Docker) and is finalized by the JaCoCo report.
  - `./gradlew integrationTest` runs `integration` (H2, `@DataJpaTest`, `@WebMvcTest`); no Docker needed.
  - `./gradlew featureTest` runs `featureTest` (full flows against a real MySQL 8.4 via Testcontainers). It **requires Docker**; without Docker these tests are skipped locally, but **fail when `CI=true`** (GitHub Actions) so CI never silently skips them.
  - Colima (macOS) + Testcontainers 1.21.4+: two environment variables are needed, otherwise `featureTest` is silently skipped (or fails with `CI=true`) because Docker is "not detected". (1) `export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`; (2) `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` (Ryuk cannot mount the host socket path otherwise). No `~/.docker-java.properties` is needed with Testcontainers 1.21.4 (works with Docker Engine 29, min API 1.44). Do not disable TLS verification (`DOCKER_TLS_VERIFY`/`DOCKER_CERT_PATH`) to work around Docker detection problems. Ryuk stays enabled and removes the containers at the end. Tip: run with `CI=true` to see failures instead of skips.
  - `./gradlew check` / `build` runs all three plus `jacocoTestCoverageVerification`.
  - Single class: `./gradlew test --tests 'br.com.fateczs.seazs.unit.service.UsuarioServiceImplTest'` (use the task of the matching layer).
- No lint tooling is configured.

## Testing

Tests follow the Test Pyramid (Martin Fowler): three layers, `unit`, `integration` and `featureTest`. The higher the layer, the broader its scope, the slower and costlier it is to run, and the fewer tests it should have. Push each scenario down to the lowest layer that can verify it, and don't repeat the same scenario across layers. When a higher-layer test catches a bug that no lower-layer test caught, add a lower-layer test for it.

- **unit** (base, the vast majority of tests): use Mockito and test classes in isolation. Cover business rules, edge cases and error paths here (e.g. `validouPresenca()` outcomes in `CertificadoServiceImpl`). Must be fast and need no Spring context, database or network.
- **integration** (middle, fewer tests): verify that classes talking to external resources integrate correctly, quickly. Also verify framework behavior: Spring dependency injection and wiring, retries, async, observability/tracing, etc. Replace external resources (APIs, databases, Kafka) with stubs and doubles: WireMock, H2 or another in-memory database, embedded Kafka. Avoid Docker and containers in this layer.
- **featureTest** (top, fewest tests): end-to-end tests that validate complete business use cases, entering through the controllers. Use Testcontainers for external resources (e.g. real MySQL 8.4 instead of H2); where that isn't possible, reuse the doubles from the integration layer. Cover the main flows, not every edge case.

## Revisão de código

Todo código criado por agentes deve ser revisado pelo subagent `revisor-codigo` (somente leitura) antes de qualquer PR. Quem orquestra aciona o revisor quando um subagent disser que vai abrir ou propor um PR; o PR só é criado após o veredito `APROVADO` (ou `APROVADO COM RESSALVAS`). Em caso de `REPROVADO`, os achados voltam ao agente autor e a revisão é repetida.

Depois do `revisor-codigo` e sempre antes de abrir o PR, quem orquestra aciona o subagent `seguranca-codigo-limpo`, que corrige riscos de segurança e code smells. Fluxo: autor → `revisor-codigo` → `seguranca-codigo-limpo` → PR. Se o `seguranca-codigo-limpo` alterar código, o `revisor-codigo` revê essas alterações antes do PR.

## Architecture

Layered Spring MVC under `br.com.fateczs.seazs`:

- `controller/` — one `@RestController` per entity (e.g. `/certificado/atividade/emitir`), using field `@Autowired` of the service interface.
- `service/` — interfaces; implementations live in `service/serviceImpl/` (`*ServiceImpl`).
- `repository/` — Spring Data JPA repositories, one per entity. Exception: `GerenciamentoRepository` is a plain class (not a JPA repository) whose score-report queries (`consultaPontuacaoSemestre`, `rodapeConsultaPontuacaoSemestre`) are WIP stubs returning `null`; they feed `RelatorioPontuacao` / `LinhaRelatorioPontuacao` / `RodapeRelatorioPontuacao` models via `GerenciamentoService`.
- `model/` — JPA entities plus non-persistent DTOs (`CertificadoAtividade`, `CertificadoStaff`, `CrachaUsuario`, report models). `model/mapper/` has hand-written mappers (instantiated with `new`, not beans) that build certificates/badges from entities. `UsuarioDisciplina` uses a composite key (`UsuarioDisciplinaID`).
- `utils/` — enums (`TipoUsuario`, `Turno`, `ModalidadeCurso`) and `DataUtils`.

Certificate flow: `CertificadoServiceImpl` loads the stored `Inscricao`/`StaffAgendamento` by ids and only emits a certificate (via the mapper) if `validouPresenca()` is true, otherwise returns `null`.

## Database / config gotchas

- Tables are generated by Hibernate (`ddl-auto=update`); `src/main/resources/schema.sql` is a reference schema that does `create schema seazs` and is intentionally not loaded.
- The MySQL image uses the default MySQL 8.4 settings (SSL on, `caching_sha2_password`) with driver `com.mysql:mysql-connector-j`; collation is kept at `utf8mb4_unicode_ci` explicitly (string comparison, including login password, is case-insensitive).
- `build.gradle` uses the Boot plugin BOM (no pinned starter versions) and the modern `implementation`/`runtimeOnly`/`developmentOnly`/`testImplementation` configurations; do not pin versions that the BOM manages.
- `application.properties` is plain ASCII (the old timezone comment was rewritten); keep it ASCII so it is safe under any encoding. Existing `.java` files under `src/main` use CRLF line endings (the tests use LF): preserve them when editing.
- Thymeleaf template/assets live in `src/main/resources/templates/` (`main.html`, `assets/`).
