# Auditoria técnica — Entrega 02: Eventos e Facade

**Data:** 10/10/2026 · **Repositório:** `ericjs-code/JoinUp-POO`  
**PR:** [#3 — Eventos e Facade transacional](https://github.com/ericjs-code/JoinUp-POO/pull/3)  
**Merge já realizado antes desta auditoria:** `70908d38a64b8781fcb3c07bbbdce6a12cca6911`  
**Parecer:** **ACEITO COM RESSALVAS**, sem defeitos críticos comprovados na revisão desta entrega. Aceite **não significa** produto integral pronto.

## 1. Escopo efetivamente auditado

Inspeção estática diretamente no GitHub das seguintes classes: `EventoFacade`, quatro services (`EventoService`, `ArtistaService`, `TipoIngressoService`, `ComunidadeService`), quatro repositórios JDBC, `JdbcEventos`, `ValidacaoEvento`, `EventoRoutes`, DTOs/domínio, bootstrap, `eventos.js`/`eventos.html`, testes `EventoFacadeTest`/`ValidacaoEventoTest` e novos cenários do `MySqlIntegrationIT`; confronto com documentos RF-EVT-01 a 08, prompt 02 e contratos HTTP.

**Evidência operacional:** [CI do PR final](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38083960496) e [CI da main após o merge](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38084244953) com status `success`. Relatório do Codex: **111 testes Surefire + 46 testes MySQL/Failsafe = 157 testes**, zero falhas/erros/ignorados, em Java 21 e MySQL 8.4.0; ver `docs/execucao/ENTREGA_02_RESULTADOS.md`. **O Tech Lead não executou Java ou MySQL em máquina própria nesta auditoria**; não confundir inspeção, resultado do CI e teste executado pelo revisor.

## 2. Checklist

| Item / requisito | Evidência no código e testes | Veredito |
| --- | --- | --- |
| Facade GoF real | `EventoFacade` coordena `EventoService`, `ArtistaService`, `TipoIngressoService` e `ComunidadeService` sobre uma `Connection` fornecida por `ExecutorTransacional`, com funções de criar/editar/categorias/consultar; usada no bootstrap e handlers | **Conforme** |
| Criação integrada e comunidade automática | `criarIntegrado` cria evento → lineup → tipos → comunidade e constrói DTO; testes unitários de ordem e mesmo Connection; teste HTTP/JDBC exige comunidade 1:1 e duas categorias | **Conforme** |
| Rollback entre subsistemas | Transações compartilhadas e propagação de `SQLException`. Testes MySQL injetam triggers em lineup, tipos e comunidade após INSERT do evento; contagens permanecem inalteradas | **Conforme nos cenários testados** |
| RF-EVT-01 / titularidade | `sessoes.exigir(..., ORGANIZADOR)` e `EventoService.bloquearEditavel` verificam titularidade; CSRF e perfil repetidos no HTTP; testes 401/403, conta inativa e outro organizador | **Conforme** |
| RF-EVT-02 / histórico | PATCH altera evento/lineup mantendo `compras`, `compra_itens`, `ingressos`, comunidade e conteúdo; testado com snapshot de tabelas em MySQL | **Conforme** |
| RF-EVT-03/04/05 / categoria vendida | SELECT de categoria combina `quantidade_vendida>0` com EXISTS `compra_itens` e `ingressos`; bloqueia edição histórica e libera nova categoria em evento futuro | **Conforme** |
| RF-EVT-06/07 / evento já iniciado | SELECT `FOR UPDATE` do evento, checagem temporal antes e após alterações de evento/categoria, resposta `EVENTO_JA_INICIADO`; testes inclusive data atual e concorrência com lock | **Conforme** |
| RF-EVT-08 / reagendamento futuro | `OffsetDateTime` normalizado em UTC, `Instant` e `LocalDateTime` no banco; rejeita data atual/passada e edição de evento iniciado; teste de offset equivalente | **Conforme** |
| Consultas públicas e meus eventos | Filtros parametrizados, paginação, titularidade em `/api/organizador/eventos`; DTOs não incluem CPF/CNPJ/e-mail/organizadorId nem posts/mensagens de comunidade | **Conforme para o escopo** |
| Frontend integrado | `eventos.html`/`eventos.js`, categorias e lineup editáveis, exibição de bloqueios, HTML seguro por `textContent`, capturas e roteiro de teste manual | **Conforme parcialmente; paridade completa pendente** |
| GoF/UML | PlantUML atualiza Facade real e contém quatro GoF; fragmento de classes de eventos existente e SVG renderizado | **Conforme no fragmento; UML geral RA2 pendente** |
| Schema/seed | Alterações deste PR são comentários de status/documentação, sem nova mudança DDL/fixtures | **Conforme** |

## 3. Ressalvas priorizadas

**R-02-01 · Média — N+1 nas listagens.** `EventoFacade.listar` chama `view` para cada evento e faz buscas separadas de lineup, tipos e comunidade, além de consulta de listagem/contagem. Com 100 eventos por página pode gerar centenas de consultas SQL. Não quebra o escopo demonstrativo, mas deve ser monitorado; otimizar se volume/latência aumentarem (consulta em lote por IDs ou projeção agregada). Não refatorar sem testes de regressão.

**R-02-02 · Alta para a entrega integral — funcionalidades centrais ainda ausentes.** Compra direta, emissão de ingressos, revenda, participação/feed comunitários, suporte e dashboards completos **não foram implementados**, por limite explícito do Prompt 02. Prioridade de implementação para o prazo acadêmico: compra e revenda transacionais na Entrega 03, depois acesso comunitário e suporte. Não apresentar esta entrega como JoinUp completo.

**R-02-03 · Média — paridade visual e upload.** Imagem de evento está restrita ao placeholder empacotado e upload multipart segue fora do escopo; CSS responsivo existe mas não houve validação visual móvel real. O formulário atende ao fluxo principal da Entrega 02, **não** à reprodução integral do frontend PHP.

**R-02-04 · Média — demo automático.** `JOINUP_DEMO` não inicializa carga de demonstração automaticamente; seed MySQL é validado, mas ainda aplicado manualmente. Fechar BD-02 antes da apresentação, sem ativar seed em produção.

**R-02-05 · Média — UML/RA2 final.** Diagramas PlantUML representam os quatro GoF e um fragmento real de eventos, mas o trabalho acadêmico ainda exige diagrama geral de classes coerente com a versão final, relatório de tradeoffs e apresentação. Revisar multiplicidades e relações sem inventar associações ORM.

**R-02-06 · Baixa — indicador público `editavel`.** O `EventoView.editavel` deriva somente de `data > agora`, não de titularidade/estado do organizador. É indicador temporal, **não autorização**; servidor revalida corretamente. Evitar que interfaces futuras o usem como única regra de permissão.

**R-02-07 · Processo — auditoria pós-merge.** O PR #3 já estava `merged=true` ao começar a revisão, apesar da orientação de aguardar. Não reverter apenas por isso; **próximos PRs devem aguardar auditoria antes do merge**.

## 4. Decisão do Tech Lead

**ACEITO COM RESSALVAS**, sem bloqueio crítico comprovado nesta revisão. O padrão **Facade GoF** está efetivamente utilizado em um fluxo real, com coordenação/rollback verificáveis, cumprindo o quarto padrão escolhido para RA2 em conjunto com Singleton, Factory Method e Template Method.

**Liberar elaboração da Entrega 03 — Compra direta e revenda** a partir da `main` atual. Exigir locks e ordem consistente com `EventoService`/categorias, proteção contra estoque negativo e compra dupla, snapshots históricos, regras de desativação e data/evento, transferência atômica da titularidade, restrição de visualização RF-REV-05 e desligamento da comunidade após a última revenda. Preservar CI com MySQL real.

**Cautela:** nenhuma alteração direta de código foi autorizada ou realizada nesta auditoria. O parecer não homologa UI integral, pagamentos reais, operações pós-evento nem release de produção.
