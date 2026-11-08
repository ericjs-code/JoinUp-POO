-- RA3 Entrega 00. Aplicação manual, administrativa, somente na instância escolhida.
-- Sem seed, reset ou reaproveitamento do schema RA2.
CREATE DATABASE IF NOT EXISTS `joinup_poo_ra3` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `joinup_poo_ra3`;

CREATE TABLE IF NOT EXISTS `generos_musicais` (
  `id_genero_musical` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id_genero_musical`)
) ENGINE = InnoDB;
