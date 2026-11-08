-- RA3 Entrega 01. Aplicacao administrativa manual, nao destrutiva.
-- Somente sete tabelas; sem seed, reset ou execucao automatica pela aplicacao.
CREATE DATABASE IF NOT EXISTS `joinup_poo_ra3` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `joinup_poo_ra3`;

CREATE TABLE IF NOT EXISTS `organizadores` (
  `id_organizador` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(80) NOT NULL,
  `email` VARCHAR(40) NOT NULL,
  `senha` VARCHAR(255) NOT NULL,
  `ativo` TINYINT(1) NOT NULL DEFAULT 1,
  `data_desativacao` DATETIME NULL DEFAULT NULL,
  `cnpj` CHAR(18) NOT NULL,
  UNIQUE INDEX (`email` ASC),
  UNIQUE INDEX (`cnpj` ASC),
  CONSTRAINT `chk_organizadores_ativo` CHECK (`ativo` IN (0,1)),
  PRIMARY KEY (`id_organizador`)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `generos_musicais` (
  `id_genero_musical` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id_genero_musical`)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `participantes` (
  `id_participante` INT NOT NULL AUTO_INCREMENT,
  `cpf` CHAR(11) NOT NULL,
  `nome` VARCHAR(60) NOT NULL,
  `data_nascimento` CHAR(14) NOT NULL,
  `email` VARCHAR(45) NOT NULL,
  `senha` VARCHAR(255) NOT NULL,
  `ativo` TINYINT(1) NOT NULL DEFAULT 1,
  `data_desativacao` DATETIME NULL DEFAULT NULL,
  UNIQUE INDEX (`email` ASC),
  UNIQUE INDEX `cpf_UNIQUE` (`cpf` ASC),
  CONSTRAINT `chk_participantes_ativo` CHECK (`ativo` IN (0,1)),
  PRIMARY KEY (`id_participante`)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `eventos` (
  `id_evento` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(45) NOT NULL,
  `data` DATETIME NOT NULL,
  `descricao` VARCHAR(450) NOT NULL,
  `imagem` VARCHAR(255) NULL DEFAULT NULL,
  `localizacao` VARCHAR(45) NOT NULL,
  `cidade` VARCHAR(60) NOT NULL,
  `estado` ENUM('AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO') NOT NULL,
  `cep` VARCHAR(8) NOT NULL,
  `id_organizador` INT NOT NULL,
  `id_genero_musical` INT NOT NULL,
  PRIMARY KEY (`id_evento`),
  UNIQUE INDEX `uk_id_evento` (`id_evento` ASC),
  INDEX `fk_eventos_generos_musicais1_idx` (`id_genero_musical` ASC),
  INDEX `fk_eventos_organizadores1_idx` (`id_organizador` ASC),
  CONSTRAINT `fk_eventos_generos_musicais1`
    FOREIGN KEY (`id_genero_musical`)
    REFERENCES `generos_musicais` (`id_genero_musical`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_eventos_organizadores1`
    FOREIGN KEY (`id_organizador`)
    REFERENCES `organizadores` (`id_organizador`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `tipos_ingressos` (
  `id_tipos_ingressos` INT NOT NULL AUTO_INCREMENT,
  `nome_tipo` VARCHAR(45) NOT NULL,
  `valor` DECIMAL(8,2) NOT NULL,
  `quantidade_disponivel` INT NOT NULL,
  `quantidade_vendida` INT NOT NULL DEFAULT 0,
  `id_evento` INT NOT NULL,
  CONSTRAINT `chk_tipos_valor` CHECK (`valor` >= 0),
  CONSTRAINT `chk_tipos_estoque` CHECK (`quantidade_disponivel` >= 0),
  CONSTRAINT `chk_tipos_vendido` CHECK (`quantidade_vendida` >= 0),
  PRIMARY KEY (`id_tipos_ingressos`),
  INDEX `fk_tipos_ingressos_eventos1_idx` (`id_evento` ASC),
  CONSTRAINT `fk_tipos_ingressos_eventos1`
    FOREIGN KEY (`id_evento`)
    REFERENCES `eventos` (`id_evento`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `comunidades` (
  `id_comunidade` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(80) NOT NULL,
  `descricao` VARCHAR(300) NULL DEFAULT NULL,
  `imagem` VARCHAR(255) NULL DEFAULT NULL,
  `id_evento` INT NOT NULL,
  `data_criacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_comunidade`),
  UNIQUE INDEX `uk_comunidade_evento` (`id_evento` ASC),
  CONSTRAINT `fk_comunidades_eventos`
    FOREIGN KEY (`id_evento`)
    REFERENCES `eventos` (`id_evento`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `postagens` (
  `id_postagem` INT NOT NULL AUTO_INCREMENT,
  `descricao` VARCHAR(500) NULL DEFAULT NULL,
  `imagem` VARCHAR(255) NULL DEFAULT NULL,
  `curtidas` INT NULL DEFAULT 0,
  `id_participante` INT NOT NULL,
  `id_comunidade` INT NOT NULL,
  `data_postagem` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_postagem`),
  INDEX `fk_postagem_participantes1_idx` (`id_participante` ASC),
  INDEX `fk_postagens_comunidades_idx` (`id_comunidade` ASC),
  CONSTRAINT `fk_postagem_participantes1`
    FOREIGN KEY (`id_participante`)
    REFERENCES `participantes` (`id_participante`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_postagens_comunidades`
    FOREIGN KEY (`id_comunidade`)
    REFERENCES `comunidades` (`id_comunidade`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE = InnoDB;
