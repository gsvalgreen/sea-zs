# sea-zs

## Participantes

1. Artur Anderson 
2. Daniel Campos
3. Gustavo Valverde
4. Felipe Moura
5. Juliane Santos

### Turma de 2016 (1º Semestre) - ADS Noite			

## Ambiente local

Requisitos: Java 17 (`sdk env`) e Docker.

```bash
cp .env.example .env          # ajuste se necessário; .env não é versionado
docker compose up -d          # sobe o MySQL (8.4) com healthcheck e volume nomeado
docker compose ps             # STATUS deve ficar "healthy"
./gradlew bootRun             # a aplicação usa localhost:3306, db seazs, root/system
docker compose down           # para (use -v para apagar o volume de dados)
```

O MySQL fica exposto apenas em `127.0.0.1`. A versão da imagem é controlada por `MYSQL_VERSION` no `.env`.

## Testes e cobertura

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

O relatório fica em `build/reports/jacoco/test/`. O build falha se a cobertura ficar abaixo do limiar definido em `build.gradle` (instruções 90%, branches 70%). O teste `contextLoads` precisa do MySQL do `docker compose` ativo.

## CI (GitHub Actions)

`.github/workflows/ci.yml` roda build, testes (com MySQL 8.4 como service), verificação de cobertura JaCoCo e análise SonarCloud em push e pull request. Configure em *Settings > Secrets and variables > Actions*:

| Nome | Tipo | Uso |
| --- | --- | --- |
| `SONAR_TOKEN` | secret | token do SonarCloud |
| `SONAR_ORGANIZATION` | variable | chave da organização no SonarCloud |
| `SONAR_PROJECT_KEY` | variable | chave do projeto no SonarCloud |
| `SONAR_HOST_URL` | variable (opcional) | padrão `https://sonarcloud.io`; defina para SonarQube próprio |

Sem `SONAR_TOKEN` (ex.: PR de fork) o passo do Sonar é pulado sem falhar.
