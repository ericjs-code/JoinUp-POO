-- JoinUp-POO / carga demonstrativa v0.2: APROVADA PELO PO; VALIDADA NO MYSQL 8.4.0 após correção DDL.
-- Evidência e idempotência: docs/banco/VALIDACAO_INTEGRACAO_01.md.
-- EXCLUSIVAMENTE PARA INSTÂNCIA LOCAL/TESTES COM BANCO joinup_poo NOVO.
-- Não aplicar em produção nem em banco com dados existentes não demonstrativos.
-- Reexecutar é seguro apenas sobre a própria base de demonstração: INSERT IGNORE.
-- Usuários demo: emails *.demo@joinup.test (domínio reservado para testes).
-- Senha de todas as contas demo: JoinUpDemo#2026
-- Formato de senha proposto para serviço Java: pbkdf2_sha256$iteracoes$saltBase64$hashBase64.
-- A verificação Java deve usar PBKDF2WithHmacSHA256 e comparação em tempo constante.
-- 'INSERT IGNORE' impede registros duplicados por PK; NÃO atualiza fixtures anteriores.
USE `joinup_poo`;

INSERT IGNORE INTO `generos_musicais` (`id_genero_musical`,`nome`) VALUES
(1,'Sertanejo'),(2,'Pop'),(3,'Funk'),(4,'Rock'),(5,'Eletrônica'),
(6,'Pagode'),(7,'Samba'),(8,'MPB'),(9,'Trap'),(10,'Rap'),
(11,'Forró'),(12,'Axé'),(13,'Indie'),(14,'Gospel'),(15,'Reggae');

INSERT IGNORE INTO `artistas` (`id_artista`,`nome`,`cpf`) VALUES
(1,'Ana Castela',NULL),(2,'Alok',NULL),(3,'Ludmilla',NULL),
(4,'Jorge & Mateus',NULL),(5,'Matuê',NULL),(6,'Péricles',NULL),
(7,'Luísa Sonza',NULL),(8,'Djavan',NULL),(9,'Ivete Sangalo',NULL),
(10,'Vintage Culture',NULL);

INSERT IGNORE INTO `organizadores` (`id_organizador`,`nome`,`email`,`senha`,`cnpj`,`ativo`,`data_desativacao`) VALUES
(1,'Produtora Demo Aurora','aurora.demo@joinup.test','pbkdf2_sha256$180000$6O7Dlir+0K08tw4dgj9WhQ==$nPYNNZcrJAHzCM6qQBxuR+ajlDGgPUhSjKhhNTuqlQo=','00000000000001',1,NULL),
(2,'Produtora Demo Horizonte','horizonte.demo@joinup.test','pbkdf2_sha256$180000$lS26n0sZqN5Uoi3OxXYFRg==$iQsli/ZEpJwunJ/RZZNqWDX+nXgnW8nWM0MPAOU4HE0=','00000000000002',1,NULL),
(3,'Produtora Demo Inativa','inativa.demo@joinup.test','pbkdf2_sha256$180000$HNUYSZImJpJNj4NSP3sWig==$m4FtCNTk7zdF+7VejUcLkSxHf08OJ716e94wR9+vD/0=','00000000000003',0,CURRENT_TIMESTAMP);

INSERT IGNORE INTO `participantes` (`id_participante`,`cpf`,`nome`,`data_nascimento`,`email`,`senha`,`ativo`,`data_desativacao`) VALUES
(1,'00000000001','Participante Demo Ana','1998-02-10','ana.demo@joinup.test','pbkdf2_sha256$180000$5grheUxmjw0Kh+XBdJEmmQ==$RjcjVo2pt6Dcd/iMf9ZgumE1C7CwJ6bIOd2iVe4HGY8=',1,NULL),
(2,'00000000002','Participante Demo Bruno','1997-05-15','bruno.demo@joinup.test','pbkdf2_sha256$180000$2MJpxowGyyN1syns10Zeng==$BRmEtw+mpc7fc8aXlTbARS9yGVUcVokZIe8gIO6tHBc=',1,NULL),
(3,'00000000003','Participante Demo Carla','1999-04-11','carla.demo@joinup.test','pbkdf2_sha256$180000$brcD81SvtLu3GeVv2IaYFw==$RWZ9Lum9JTPnpZ9D9mfagGYer9+etdPJ8Jr9L8ZviKw=',1,NULL),
(4,'00000000004','Participante Demo Inativo','2000-07-20','inativo.demo@joinup.test','pbkdf2_sha256$180000$shrp/9BIYPIE4h0DOdzaYg==$75L7ejkzz8fM16jWCYXQj+dbsqB9vjFUpw8uc/Tgoyg=',0,CURRENT_TIMESTAMP);

INSERT IGNORE INTO `usuarios_suporte` (`id_usuario_suporte`,`nome`,`email`,`senha`) VALUES
(1,'Suporte Demo Um','suporte1.demo@joinup.test','pbkdf2_sha256$180000$M+86xgi5XU1EIHF4JsyE0w==$yg3Ka/O3Jva0TpVK6Tj/4R/Ft6yupWJOpNTt/Eo10pE='),
(2,'Suporte Demo Dois','suporte2.demo@joinup.test','pbkdf2_sha256$180000$XlZmkQQM3uqxf5Hr7rOpZw==$2pLn43BhlGV/9FI7Gt9OpBeP2egahEvEZc5AhUmDzYY=');

INSERT IGNORE INTO `eventos`
(`id_evento`,`nome`,`data`,`descricao`,`imagem`,`localizacao`,`cidade`,`estado`,`cep`,`id_organizador`,`id_genero_musical`) VALUES
(1,'Festival Aurora',DATE_ADD(NOW(),INTERVAL 45 DAY),'Evento demonstrativo com ingressos vendidos e comunidade.',NULL,'Arena Demo','Curitiba','PR','80000000',1,2),
(2,'Noite Horizonte',DATE_ADD(NOW(),INTERVAL 65 DAY),'Evento demonstrativo com revenda concluída.',NULL,'Espaço Demo','Curitiba','PR','80000001',2,5),
(3,'Evento do Organizador Inativo',DATE_ADD(NOW(),INTERVAL 80 DAY),'Vendas bloqueadas enquanto a conta do organizador estiver desativada.',NULL,'Centro de Eventos','Curitiba','PR','80000002',3,4),
(4,'Evento Encerrado',DATE_SUB(NOW(),INTERVAL 5 DAY),'Ingressos não podem ser comprados após a data e hora do evento.',NULL,'Teatro Demo','Curitiba','PR','80000003',1,1);

INSERT IGNORE INTO `line_up` (`id_evento`,`id_artista`) VALUES (1,1),(1,3),(2,2),(2,10),(3,8),(4,4);

INSERT IGNORE INTO `tipos_ingressos`
(`id_tipos_ingressos`,`nome_tipo`,`valor`,`quantidade_disponivel`,`quantidade_vendida`,`id_evento`) VALUES
(1,'Pista',100.00,98,2,1),(2,'Camarote',180.00,79,1,1),
(3,'Geral',90.00,199,1,2),(4,'Inteira',120.00,48,2,3),
(5,'Pista',60.00,9,1,4),(6,'VIP Sem Vendas',250.00,40,0,1);

INSERT IGNORE INTO `comunidades`
(`id_comunidade`,`nome`,`descricao`,`imagem`,`id_evento`) VALUES
(1,'Comunidade Festival Aurora','Grupo vinculado ao evento Festival Aurora',NULL,1),
(2,'Comunidade Noite Horizonte','Grupo vinculado ao evento Noite Horizonte',NULL,2),
(3,'Comunidade Organizador Inativo','Grupo preservado mesmo com conta do organizador inativa',NULL,3),
(4,'Comunidade Evento Encerrado','Comunidade de evento passado',NULL,4);

INSERT IGNORE INTO `compras`
(`id_compra`,`id_participante`,`id_evento`,`status`,`valor_total`,`nome_evento_na_compra`,`data_evento_na_compra`,`local_evento_na_compra`) VALUES
(1,1,1,'confirmada',200.00,'Festival Aurora',(SELECT `data` FROM `eventos` WHERE `id_evento`=1),'Arena Demo'),
(2,2,1,'confirmada',180.00,'Festival Aurora',(SELECT `data` FROM `eventos` WHERE `id_evento`=1),'Arena Demo'),
(3,3,2,'confirmada',90.00,'Noite Horizonte',(SELECT `data` FROM `eventos` WHERE `id_evento`=2),'Espaço Demo'),
(4,4,3,'confirmada',120.00,'Evento do Organizador Inativo',(SELECT `data` FROM `eventos` WHERE `id_evento`=3),'Centro de Eventos'),
(5,2,4,'confirmada',60.00,'Evento Encerrado',(SELECT `data` FROM `eventos` WHERE `id_evento`=4),'Teatro Demo'),
(6,3,3,'confirmada',120.00,'Evento do Organizador Inativo',(SELECT `data` FROM `eventos` WHERE `id_evento`=3),'Centro de Eventos');

INSERT IGNORE INTO `compra_itens`
(`id_compra_item`,`id_compra`,`id_tipo_ingresso`,`quantidade`,`valor_unitario`,`subtotal`) VALUES
(1,1,1,2,100.00,200.00),(2,2,2,1,180.00,180.00),(3,3,3,1,90.00,90.00),
(4,4,4,1,120.00,120.00),(5,5,5,1,60.00,60.00),(6,6,4,1,120.00,120.00);

-- Ingresso 4 comprado originalmente por Carla (compra 3), agora pertence a Bruno por revenda.
INSERT IGNORE INTO `ingressos`
(`id_ingresso`,`data_compra`,`status`,`id_participante`,`id_evento`,`id_tipo_ingresso`,`id_compra`) VALUES
(1,CURRENT_DATE,'ativo',1,1,1,1),(2,CURRENT_DATE,'ativo',1,1,1,1),
(3,CURRENT_DATE,'ativo',2,1,2,2),(4,CURRENT_DATE,'ativo',2,2,3,3),
(5,CURRENT_DATE,'ativo',4,3,4,4),(6,DATE_SUB(CURRENT_DATE,INTERVAL 10 DAY),'utilizado',2,4,5,5),
(7,CURRENT_DATE,'ativo',3,3,4,6);

-- 1: anúncio disponível; 2: venda concluída; 3: cancelado por desativação;
-- 4: anúncio existente indisponível por organizador desativado, sem mudar status.
INSERT IGNORE INTO `revenda_anuncios`
(`id_revenda_anuncios`,`valor_revenda`,`status`,`id_ingresso`,`id_participante`,`id_comprador`,`data_venda`,`data_cancelamento`) VALUES
(1,120.00,'disponivel',1,1,NULL,NULL,NULL),
(2,95.00,'vendido',4,3,2,CURRENT_TIMESTAMP,NULL),
(3,130.00,'cancelado',5,4,NULL,NULL,CURRENT_TIMESTAMP),
(4,135.00,'disponivel',7,3,NULL,NULL,NULL);

-- Só membros ativos E com ingresso válido acessam a comunidade.
INSERT IGNORE INTO `participante_comunidades`
(`id_participante`,`id_comunidade`,`ativo`,`data_saida`) VALUES
(1,1,1,NULL),(2,1,1,NULL),(2,2,1,NULL),
(3,2,0,CURRENT_TIMESTAMP),(4,3,0,CURRENT_TIMESTAMP);

INSERT IGNORE INTO `comunidade_mensagens`
(`id_mensagem`,`id_comunidade`,`autor_tipo`,`id_participante`,`id_organizador`,`mensagem`,`id_resposta_a`) VALUES
(1,1,'participante',1,NULL,'Bem-vindos ao Festival Aurora!',NULL),
(2,1,'organizador',NULL,1,'Confiram o line-up do festival!',1),
(3,2,'participante',2,NULL,'Animado para a Noite Horizonte!',NULL);

INSERT IGNORE INTO `postagens`
(`id_postagem`,`descricao`,`imagem`,`curtidas`,`id_participante`,`id_comunidade`) VALUES
(1,'Quem vai ao Festival Aurora?',NULL,1,1,1),
(2,'Preparação para a Noite Horizonte.',NULL,0,2,2);

INSERT IGNORE INTO `comentarios`
(`id_comentario`,`texto`,`id_postagem`,`id_participante`) VALUES
(1,'Eu vou!',1,2);

INSERT IGNORE INTO `postagem_curtidas` (`id_participante`,`id_postagem`) VALUES (2,1);

INSERT IGNORE INTO `ticket`
(`id_ticket`,`titulo`,`descricao`,`resposta`,`atendido`,`status_ticket`,`retorno_usuario`,`id_participante`,`id_usuario_suporte`,`id_organizador`,`data_fechamento`) VALUES
(1,'Dúvida sobre ingresso','Como vejo meus ingressos?',NULL,0,'aberto',NULL,1,NULL,NULL,NULL),
(2,'Erro no perfil','Preciso de ajuda para acessar meu perfil.',NULL,1,'aguardando_usuario',NULL,3,1,NULL,NULL),
(3,'Confirmação de evento','Gostaria de confirmar as informações do evento.',NULL,1,'fechado',NULL,NULL,2,1,CURRENT_TIMESTAMP);

INSERT IGNORE INTO `ticket_mensagens`
(`id_ticket_mensagem`,`id_ticket`,`autor_tipo`,`id_participante`,`id_organizador`,`id_usuario_suporte`,`mensagem`) VALUES
(1,1,'participante',1,NULL,NULL,'Como vejo meus ingressos?'),
(2,2,'participante',3,NULL,NULL,'Preciso de ajuda para acessar meu perfil.'),
(3,2,'suporte',NULL,NULL,2,'Vamos verificar seu acesso. Por favor, confirme se conseguiu entrar.'),
(4,3,'organizador',NULL,1,NULL,'Gostaria de confirmar as informações do evento.'),
(5,3,'suporte',NULL,NULL,2,'As informações do evento estão confirmadas.');
-- Encerramento do ticket 3 ocorreu após mensagem do suporte e ação de resolver do solicitante.
