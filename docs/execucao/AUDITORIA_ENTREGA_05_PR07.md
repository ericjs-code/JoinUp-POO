# Auditoria técnica — Entrega 05 (PR #7): Suporte e Painéis

> **Data da auditoria:** 11/10/2026 (America/Sao_Paulo)  
> **PR:** [#7 — Chamados transacionais, autoria e painéis por perfil](https://github.com/ericjs-code/JoinUp-POO/pull/7)  
> **Commit de merge:** `7e3c52b1e0f0c606792a1959a8615920f259c69d`; PR **já mesclado antes do parecer**.  
> **Veredito:** **ACEITO TECNICAMENTE COM RESSALVAS** para o incremento 05; **produto total ainda não homologado**.  
> **Método:** inspeção estática de classes, cenários de teste, contratos, SQL e frontend versionados; conferência de status GitHub Actions. O auditor não executou Java/MySQL/navegador localmente nesta sessão.

## 1. Evidências verificáveis

- PR #7: 39 arquivos alterados, 1.842 adições/13 remoções. Implementação em `suporte/`, `paineis/`, interface `painel.html`/`paineis.js`, integração no bootstrap, testes e documentação.
- [CI do PR final, run 38194306803](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38194306803): **completed/success**, Java 21 + MySQL 8.4.0 e perfil `-Pmysql-it clean verify`; relatório `docs/execucao/ENTREGA_05_RESULTADOS.md` informa **397 testes** aprovados (149 Surefire + 248 Failsafe), sem falhas/erros/ignorados. 310 casos anteriores preservados; 13 unitários e 74 integrações novos: 63 suporte + 11 painéis.
- O [run da `main` após merge, 38194687005](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38194687005) **concluiu com sucesso**; conferência posterior confirmou `completed/success` após o merge do PR #7.
- `SuporteMySqlIT` contém matriz de 16 ações/estados para participante e organizador, testes de IDOR, CSRF, texto inválido, conta inativa, autoria entre dois suportes, rollback com trigger e casos de concorrência com espera real de lock; `PaineisMySqlIT` cobre os três perfis, dados históricos, titularidade após revenda e métricas globais.

## 2. Achados conformes

| Área | Evidência concreta | Parecer |
| --- | --- | --- |
| RF-SUP-01 e 04 | `StatusTicket`: `fechado` terminal; resolver/retornar exclusivamente `aguardando_usuario`; suporte encerra somente `aberto` ou `em_atendimento` | Conforme |
| RF-SUP-02/03 | `TicketService`: qualquer suporte pode responder sem ser associado; autor vem da sessão; responde mudando estado para `aguardando_usuario` | Conforme |
| Criação e histórico | `TicketRepositoryJdbc`: INSERT em `ticket` e primeira linha em `ticket_mensagens` na mesma transação; alteração de estado e mensagem posteriores também atômicas | Conforme |
| Titularidade, sessão, CSRF | `TicketRoutes`/serviço validam perfil, titularidade, conta ativa e origem da mutação; recurso inexistente e de terceiro usam 404; testes HTTP/MySQL | Conforme |
| Corridas e rollback | `SELECT ... FOR UPDATE` no ticket no caminho de mudança; testes de resolver vs retornar, resposta vs encerramento, duas respostas de suporte e falhas injetadas | Conforme **nos interleavings testados** |
| Dashboards | `PainelService` e `PainelRepositoryJdbc`: agregações reais isoladas por titular/organizador, compras históricas separadas do proprietário atual, acesso comunitário efetivo, métricas de suporte por autor real | Conforme |
| UI | `painel.html`/`paineis.js`: formulários e navegação por perfil; textos e dados de usuário apresentados por `textContent`, sem executar HTML de conversas | Conforme para o incremento; validação de dispositivos pendente |
| GoF | Quatro GoF já usados em fluxos reais preservados; novos serviços não introduzem padrão fictício | Conforme |
| MySQL/DDL | PR não altera schema nem seed; CI executa MySQL 8.4 real | Conforme |

## 3. Ressalvas, classificadas

**R-05-01 — MÉDIA, auditoria das transições:** existe histórico **de mensagens por autor real**, mas não um log separado por ator para cada associação/encerramento sem texto. `id_usuario_suporte` significa acompanhamento, não quem encerrou. Não afirmar rastreabilidade completa de cada transição; se nova exigência acadêmica aparecer, propor mecanismo específico documentado.

**R-05-02 — MÉDIA, idempotência:** POST de abertura não possui idempotency key; repetição de requisição pode abrir dois chamados legítimos. UI restringe duplo clique, mas isso não equivale a deduplicação no servidor. Não criar retry automático em POST.

**R-05-03 — MÉDIA, escala/UX:** listas com OFFSET e limite de 20 eventos de painel; leitura de detalhe usa lock do ticket, podendo serializar consultas com atendimento. Sem teste de carga, UX móvel/leitor de tela ou infraestrutura TLS reversa; não confundir com garantia de produção.

**R-05-04 — ALTA para conclusão integral:** continuam pendentes **mídia segura, upload e posts somente-imagem** (SOC-06), imagens em eventos/perfil onde exigido, e **BD-02 — carga demonstrativa automática segura**. O seed existente é manual. A entrega 05 não pretende resolvê-los.

**R-05-05 — ALTA para avaliação RA2:** documentação final consolidada (diagrama geral de classes atualizado com a implementação, relatório dos quatro GoF, tradeoffs, evidências e roteiro de apresentação) e testes end-to-end de navegação completa dos três perfis ainda não estão homologados.

**R-05-06 — BAIXA, README pós-merge:** ainda contém instrução para trocar à branch `feat/05-suporte-paineis` “antes do merge” e outros estados antigos. Atualizar à `main` antes de finalizar.

**R-05-07 — Processo:** embora PR #7 diga aguardar auditoria, o merge ocorreu antes desta revisão. Não reverter um incremento com CI aprovado e sem problema crítico identificado; **evitar merge antecipado nos próximos PRs**.

## 4. Decisão e sequência

**ENTREGA 05 ACEITA COM RESSALVAS**. O módulo de suporte está alinhado à matriz RF-SUP-04 e os painéis fornecem indicadores de escopo real, com forte evidência de integração MySQL. Nenhum bug bloqueante foi **comprovado** nesta auditoria, mas o escopo do produto permanece aberto.

**Próxima execução prioritária:** `docs/prompts/CODEX_06_MIDIA_DEMO.md` — tratamento seguro de imagem (SOC-06 e eventos/perfis previstos), carga de demonstração idempotente opt-in (BD-02), e testes integrais desses fluxos. **Em paralelo** preparar documentação RA2 e planejar bateria end-to-end; não esperar o último commit para começar relatório/UML.

**Gate:** CI pós-merge da main já confirmado verde; exigir PR da Entrega 06 sem merge automático; nova auditoria antes de integrar. Programar ensaio e congelamento de funcionalidades antes da entrega acadêmica de 15/10.

**Não foi alterado código de produção nesta auditoria; esta página é o parecer de revisão.**
