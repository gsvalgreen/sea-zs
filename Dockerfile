# MySQL para execução local do projeto e dos testes.
# Valores alinhados com src/main/resources/application.properties.
# MySQL 8.4 (LTS), compativel com o driver mysql-connector-j 8.x e o dialeto MySQL8 do Hibernate.
# A imagem tem build arm64 e amd64; nao ha mais necessidade de fixar a plataforma.
FROM mysql:8.4

ENV MYSQL_ROOT_PASSWORD=system \
    MYSQL_DATABASE=seazs \
    TZ=America/Sao_Paulo

EXPOSE 3306

# Charset UTF-8; as tabelas são criadas pelo Hibernate (ddl-auto=update),
# por isso o schema.sql não é carregado (ele já faz "create schema seazs").
# Collation utf8mb4_unicode_ci mantida explicita (o padrao do MySQL 8 seria utf8mb4_0900_ai_ci), para
# preservar o comportamento de comparacao de strings do ambiente anterior. O usuario root usa
# caching_sha2_password (padrao do 8.x); o driver 8.x negocia TLS normalmente.
CMD ["--character-set-server=utf8mb4","--collation-server=utf8mb4_unicode_ci", "--default-time-zone=-03:00"]
