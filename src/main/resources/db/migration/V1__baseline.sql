-- Baseline do schema do SEA-ZS: reproduz o que o Hibernate 7 gera (ddl-auto) para as entidades JPA no MySQL 8.4.
-- Nomes de FK/UNIQUE sao os que o Hibernate gera, para que bancos criados por ele (baseline-on-migrate) sejam identicos.
-- Sem "create schema": o banco (seazs) e criado fora da migracao. Collation explicita utf8mb4_unicode_ci.
-- tb_usuario.turno e varchar(1) (atributo Character; decisao do lead, antes era char(1)); tipo_usuario/modalidade_curso sao int (ordinal).

CREATE TABLE `tb_curso` (
  `id_curso` int NOT NULL AUTO_INCREMENT,
  `modalidade_curso` int NOT NULL,
  `nome_curso` varchar(255) NOT NULL,
  `qtd_semestre` int NOT NULL,
  PRIMARY KEY (`id_curso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_papel` (
  `id_papel` int NOT NULL AUTO_INCREMENT,
  `descricao_papel` varchar(255) DEFAULT NULL,
  `nome_papel` varchar(255) NOT NULL,
  PRIMARY KEY (`id_papel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_usuario` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `adm` bit(1) NOT NULL,
  `celular_usuario` varchar(255) NOT NULL,
  `data_nascimento` date NOT NULL,
  `email_usuario` varchar(255) NOT NULL,
  `endereco_usuario` varchar(255) NOT NULL,
  `genero_usuario` varchar(255) NOT NULL,
  `matricula_usuario` varchar(255) DEFAULT NULL,
  `nome_usuario` varchar(255) NOT NULL,
  `rg_usuario` varchar(255) NOT NULL,
  `semestre` int DEFAULT NULL,
  `senha` varchar(255) NOT NULL,
  `tipo_usuario` int NOT NULL,
  `turno` varchar(1) DEFAULT NULL,
  `id_curso` int DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `UKfudv57wn1m68hm816yebp0sai` (`email_usuario`),
  KEY `FK1m9r3l1txsq7clps1cp4vo9n9` (`id_curso`),
  CONSTRAINT `FK1m9r3l1txsq7clps1cp4vo9n9` FOREIGN KEY (`id_curso`) REFERENCES `tb_curso` (`id_curso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_disciplina` (
  `id_disciplina` int NOT NULL AUTO_INCREMENT,
  `codigo_siga` varchar(255) NOT NULL,
  `nome_disciplina` varchar(255) NOT NULL,
  `id_curso` int NOT NULL,
  PRIMARY KEY (`id_disciplina`),
  KEY `FK1d3cfdsljpcvem7s9bmlyniua` (`id_curso`),
  CONSTRAINT `FK1d3cfdsljpcvem7s9bmlyniua` FOREIGN KEY (`id_curso`) REFERENCES `tb_curso` (`id_curso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_evento` (
  `id_evento` int NOT NULL AUTO_INCREMENT,
  `data_inclusao` date NOT NULL,
  `descricao_evento` varchar(255) DEFAULT NULL,
  `data_fim` datetime(6) NOT NULL,
  `data_inicio` datetime(6) NOT NULL,
  `nome_evento` varchar(255) NOT NULL,
  `id_curso` int DEFAULT NULL,
  `id_usuario_criador` int NOT NULL,
  PRIMARY KEY (`id_evento`),
  KEY `FKt0qaq7em9mvt9uvjyl78lu159` (`id_curso`),
  KEY `FK33g9uxjfoir3k6c1jndhu02c9` (`id_usuario_criador`),
  CONSTRAINT `FK33g9uxjfoir3k6c1jndhu02c9` FOREIGN KEY (`id_usuario_criador`) REFERENCES `tb_usuario` (`id_usuario`),
  CONSTRAINT `FKt0qaq7em9mvt9uvjyl78lu159` FOREIGN KEY (`id_curso`) REFERENCES `tb_curso` (`id_curso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_agendamento` (
  `id_agendamento` int NOT NULL AUTO_INCREMENT,
  `data_fim` datetime(6) NOT NULL,
  `data_inicio` datetime(6) NOT NULL,
  `pontuacao_staff` int NOT NULL,
  `id_evento` int NOT NULL,
  `id_usuario_organizador` int NOT NULL,
  PRIMARY KEY (`id_agendamento`),
  KEY `FK51d1kmpnv999mbn5kyum5rcew` (`id_evento`),
  KEY `FK6elac648q29pvs9tr0s3rbtle` (`id_usuario_organizador`),
  CONSTRAINT `FK51d1kmpnv999mbn5kyum5rcew` FOREIGN KEY (`id_evento`) REFERENCES `tb_evento` (`id_evento`),
  CONSTRAINT `FK6elac648q29pvs9tr0s3rbtle` FOREIGN KEY (`id_usuario_organizador`) REFERENCES `tb_usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_atividade` (
  `id_atividade` int NOT NULL AUTO_INCREMENT,
  `carga_horaria` int NOT NULL,
  `categoria_atividade` varchar(255) NOT NULL,
  `descricao_atividade` varchar(255) DEFAULT NULL,
  `data_fim` datetime(6) NOT NULL,
  `flag_continua` bit(1) DEFAULT NULL,
  `flag_cumulativa` bit(1) DEFAULT NULL,
  `data_inicio` datetime(6) NOT NULL,
  `local_atividade` varchar(255) NOT NULL,
  `nome_atividade` varchar(255) NOT NULL,
  `pontuacao_participante` int NOT NULL,
  `qtd_checkin` int DEFAULT NULL,
  `qtd_vagas` int NOT NULL,
  `id_agendamento` int NOT NULL,
  PRIMARY KEY (`id_atividade`),
  KEY `FK7b1hphkqj3dkyc2rey2sdbsqe` (`id_agendamento`),
  CONSTRAINT `FK7b1hphkqj3dkyc2rey2sdbsqe` FOREIGN KEY (`id_agendamento`) REFERENCES `tb_agendamento` (`id_agendamento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_inscricao` (
  `sq_inscricao` int NOT NULL AUTO_INCREMENT,
  `check_in` bit(1) DEFAULT NULL,
  `check_out` bit(1) DEFAULT NULL,
  `data_inscricao` datetime(6) NOT NULL,
  `pontuacao_participante` int DEFAULT NULL,
  `id_atividade` int NOT NULL,
  `id_papel` int NOT NULL,
  `id_usuario` int NOT NULL,
  PRIMARY KEY (`sq_inscricao`),
  UNIQUE KEY `UKjhmy930anf89eimt2ovsqekc1` (`id_atividade`,`id_usuario`),
  KEY `FKdeoqd96dploi72xmyb4g9c21y` (`id_papel`),
  KEY `FKj79h1mrk3yd55vjfkb8jj1dkn` (`id_usuario`),
  CONSTRAINT `FK6fsol1303ca17nfmgfxkjbuds` FOREIGN KEY (`id_atividade`) REFERENCES `tb_atividade` (`id_atividade`),
  CONSTRAINT `FKdeoqd96dploi72xmyb4g9c21y` FOREIGN KEY (`id_papel`) REFERENCES `tb_papel` (`id_papel`),
  CONSTRAINT `FKj79h1mrk3yd55vjfkb8jj1dkn` FOREIGN KEY (`id_usuario`) REFERENCES `tb_usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_staff_agendamento` (
  `sq_staff` int NOT NULL AUTO_INCREMENT,
  `check_in` bit(1) NOT NULL DEFAULT b'0',
  `check_out` bit(1) NOT NULL DEFAULT b'0',
  `data_inscricao` datetime(6) NOT NULL,
  `pontuacao_participante` int DEFAULT NULL,
  `id_agendamento` int NOT NULL,
  `id_usuario` int NOT NULL,
  PRIMARY KEY (`sq_staff`),
  UNIQUE KEY `UKs38ghd3jmqelpknr5vqbf6aac` (`id_agendamento`,`id_usuario`),
  KEY `FKom0dawmsb3gfen5ep69w0x5py` (`id_usuario`),
  CONSTRAINT `FK65agl6rppxmkb8jf7yslleaax` FOREIGN KEY (`id_agendamento`) REFERENCES `tb_agendamento` (`id_agendamento`),
  CONSTRAINT `FKom0dawmsb3gfen5ep69w0x5py` FOREIGN KEY (`id_usuario`) REFERENCES `tb_usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_usuario_disciplina` (
  `data_inicio_disciplina` date NOT NULL,
  `data_fim_disciplina` date DEFAULT NULL,
  `id_disciplina` int NOT NULL,
  `id_usuario` int NOT NULL,
  PRIMARY KEY (`id_disciplina`,`data_inicio_disciplina`,`id_usuario`),
  KEY `FKh8bemqkwkknri9p3vg80fksqe` (`id_usuario`),
  CONSTRAINT `FKh8bemqkwkknri9p3vg80fksqe` FOREIGN KEY (`id_usuario`) REFERENCES `tb_usuario` (`id_usuario`),
  CONSTRAINT `FKs0kxm8affjukltq5veefc7nc1` FOREIGN KEY (`id_disciplina`) REFERENCES `tb_disciplina` (`id_disciplina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
