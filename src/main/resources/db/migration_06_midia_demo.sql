-- Aplicação administrativa separada; aditiva e repetível. Não altera schema/seed v0.2.
USE `joinup_poo`;
CREATE TABLE IF NOT EXISTS arquivos (
  id CHAR(32) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  contexto ENUM('evento','postagem','mensagem') NOT NULL,
  id_evento INT NULL,
  id_comunidade INT NULL,
  id_participante INT NULL,
  id_organizador INT NULL,
  id_postagem INT NULL,
  id_mensagem INT NULL,
  tamanho INT NOT NULL,
  sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_arquivo_postagem (id_postagem),
  UNIQUE KEY uk_arquivo_mensagem (id_mensagem),
  KEY idx_arquivo_pendente (criado_em),
  CONSTRAINT chk_arquivo_tamanho CHECK(tamanho BETWEEN 1 AND 1048576),
  CONSTRAINT chk_arquivo_dono CHECK((id_participante IS NULL) <> (id_organizador IS NULL)),
  CONSTRAINT chk_arquivo_contexto CHECK(
    (contexto='evento' AND id_evento IS NOT NULL AND id_comunidade IS NULL AND id_organizador IS NOT NULL AND id_postagem IS NULL AND id_mensagem IS NULL)
    OR (contexto='postagem' AND id_evento IS NULL AND id_comunidade IS NOT NULL AND id_participante IS NOT NULL AND id_mensagem IS NULL)
    OR (contexto='mensagem' AND id_evento IS NULL AND id_comunidade IS NOT NULL AND id_postagem IS NULL)),
  CONSTRAINT fk_arquivo_evento FOREIGN KEY(id_evento) REFERENCES eventos(id_evento),
  CONSTRAINT fk_arquivo_comunidade FOREIGN KEY(id_comunidade) REFERENCES comunidades(id_comunidade),
  CONSTRAINT fk_arquivo_participante FOREIGN KEY(id_participante) REFERENCES participantes(id_participante),
  CONSTRAINT fk_arquivo_organizador FOREIGN KEY(id_organizador) REFERENCES organizadores(id_organizador),
  CONSTRAINT fk_arquivo_postagem FOREIGN KEY(id_postagem) REFERENCES postagens(id_postagem),
  CONSTRAINT fk_arquivo_mensagem FOREIGN KEY(id_mensagem) REFERENCES comunidade_mensagens(id_mensagem)
) ENGINE=InnoDB;
-- Marcador preenchido somente pelo runner após conferir base vazia, na mesma transação do seed.
CREATE TABLE IF NOT EXISTS demo_execucao (
  id TINYINT PRIMARY KEY,
  versao VARCHAR(20) NOT NULL,
  aplicada_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_demo_id CHECK(id=1)
) ENGINE=InnoDB;
