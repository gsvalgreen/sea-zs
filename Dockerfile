# MySQL para execução local do projeto e dos testes.
# Valores alinhados com src/main/resources/application.properties.
# MySQL 5.7 por causa do mysql-connector-java 5.1.46 e do MySQL5InnoDBDialect.
# A imagem 5.7 não tem build arm64 (Apple Silicon), por isso a plataforma fixa.
FROM --platform=linux/amd64 mysql:5.7

ENV MYSQL_ROOT_PASSWORD=system \
    MYSQL_DATABASE=seazs \
    TZ=America/Sao_Paulo

EXPOSE 3306

# Charset UTF-8; as tabelas são criadas pelo Hibernate (ddl-auto=update),
# por isso o schema.sql não é carregado (ele já faz "create schema seazs").
# SSL desabilitado no servidor: o driver 5.1.46 negocia TLS 1.0/1.1, que os JDKs
# recentes desabilitam (SSLHandshakeException). Sem SSL no servidor, o driver
# cai para conexão sem criptografia, o que é aceitável em ambiente local.
CMD ["--ssl=0", "--character-set-server=utf8mb4","--collation-server=utf8mb4_unicode_ci", "--default-time-zone=-03:00"]
