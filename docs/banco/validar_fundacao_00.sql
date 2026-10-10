-- Somente consultas na base independente. Não modifica o schema/seed aprovados.
USE `joinup_poo`;
SELECT VERSION() AS versao_mysql, DATABASE() AS banco_atual;
SELECT COUNT(*) AS total_tabelas FROM information_schema.tables
WHERE table_schema = 'joinup_poo' AND table_type = 'BASE TABLE';
SELECT table_name, constraint_name, referenced_table_name, referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = 'joinup_poo' AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name, ordinal_position;
SELECT constraint_name, enforced FROM information_schema.table_constraints
WHERE table_schema = 'joinup_poo' AND constraint_type = 'CHECK'
ORDER BY constraint_name;
SELECT column_name, generation_expression FROM information_schema.columns
WHERE table_schema = 'joinup_poo' AND table_name = 'revenda_anuncios'
  AND column_name = 'id_ingresso_disponivel';
SELECT index_name, non_unique, column_name FROM information_schema.statistics
WHERE table_schema = 'joinup_poo' AND table_name = 'revenda_anuncios'
  AND index_name = 'uk_revenda_ingresso_disponivel';
SELECT 'artistas' AS tabela, COUNT(*) AS linhas FROM artistas
UNION ALL SELECT 'auditoria_reativacoes', COUNT(*) FROM auditoria_reativacoes
UNION ALL SELECT 'comentarios', COUNT(*) FROM comentarios
UNION ALL SELECT 'compras', COUNT(*) FROM compras
UNION ALL SELECT 'compra_itens', COUNT(*) FROM compra_itens
UNION ALL SELECT 'comunidades', COUNT(*) FROM comunidades
UNION ALL SELECT 'comunidade_mensagens', COUNT(*) FROM comunidade_mensagens
UNION ALL SELECT 'eventos', COUNT(*) FROM eventos
UNION ALL SELECT 'generos_musicais', COUNT(*) FROM generos_musicais
UNION ALL SELECT 'ingressos', COUNT(*) FROM ingressos
UNION ALL SELECT 'line_up', COUNT(*) FROM line_up
UNION ALL SELECT 'organizadores', COUNT(*) FROM organizadores
UNION ALL SELECT 'participantes', COUNT(*) FROM participantes
UNION ALL SELECT 'participante_comunidades', COUNT(*) FROM participante_comunidades
UNION ALL SELECT 'postagens', COUNT(*) FROM postagens
UNION ALL SELECT 'postagem_curtidas', COUNT(*) FROM postagem_curtidas
UNION ALL SELECT 'revenda_anuncios', COUNT(*) FROM revenda_anuncios
UNION ALL SELECT 'sessoes', COUNT(*) FROM sessoes
UNION ALL SELECT 'ticket', COUNT(*) FROM ticket
UNION ALL SELECT 'ticket_mensagens', COUNT(*) FROM ticket_mensagens
UNION ALL SELECT 'tipos_ingressos', COUNT(*) FROM tipos_ingressos
UNION ALL SELECT 'usuarios_suporte', COUNT(*) FROM usuarios_suporte;
