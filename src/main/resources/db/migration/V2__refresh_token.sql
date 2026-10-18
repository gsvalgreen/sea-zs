-- Fase 2 (seguranca): refresh tokens opacos, armazenados apenas como hash SHA-256 (hex, 64 chars).
-- IF NOT EXISTS: bancos de desenvolvimento em ddl-auto=update ja podem ter criado a tabela a partir da entidade.
-- familia agrupa a cadeia de rotacao; reuso de token revogado revoga a familia inteira.
CREATE TABLE IF NOT EXISTS `tb_refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_hash` varchar(64) NOT NULL,
  `familia` varchar(36) NOT NULL,
  `id_usuario` int NOT NULL,
  `criado_em` datetime(6) NOT NULL,
  `expira_em` datetime(6) NOT NULL,
  `revogado_em` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_refresh_token_hash` (`token_hash`),
  KEY `IDX_refresh_token_familia` (`familia`),
  KEY `IDX_refresh_token_usuario` (`id_usuario`),
  CONSTRAINT `FK_refresh_token_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `tb_usuario` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
