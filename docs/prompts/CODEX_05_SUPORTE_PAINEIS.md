# CODEX — Entrega 05: suporte, chamados e painéis por perfil

> **JoinUp-POO** · Java 21 puro / JDBC / MySQL 8.4 · Prazo RA2: 15/10/2026  
> **Ramo:** `feat/05-suporte-paineis`, a partir da `main` atualizada **após incorporar a correção 04.1**.  
> **PR obrigatório sem merge automático** e com auditoria técnica prévia.

## Missão

Concluir suporte/chamados como módulo funcional com histórico e matriz de transições **aprovada pelo PO**, e disponibilizar indicadores reais para os três perfis (participante, organizador, suporte). Integrar a navegação ao frontend existente sem reescrever eventos/comércio/comunidades.

**Fontes de verdade:** ler integralmente `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md`, TODOS os `docs/requisitos/ADENDO_*.md` (principalmente `ADENDO_RF-SUP-04_TRANSICOES_SOLICITANTE.md`), `docs/api/CONTRATOS_HTTP_v0.1.md`, `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`, `docs/execucao/PLANO_CODEX.md`, auditorias 01/02 e `AUDITORIA_CRUZADA_PR04_PR05.md`, schema/seed v0.2, e código Java realmente existente na `main`. Consultar o PHP original apenas como referência de telas/UX.

**Não começar de branch anterior à correção 04.1 sem rebase/coordenação:** o bootstrap/nav são áreas compartilhadas. Se 04.1 ainda estiver em revisão, o desenvolvedor de 05 pode trabalhar em branch própria, mas só deve propor integração após rebase nos commits aprovados de 04.1 e retestar o CI.

## Arquitetura obrigatória

- Java 21 puro, `com.sun.net.httpserver.HttpServer`, Maven, JDBC/MySQL 8, Jackson/JUnit; **sem** frameworks, ORM/Servlet/gateway de e-mail/finanças.
- Preservar quatro GoF: Facade `EventoFacade`, Template Method `AutenticacaoTemplate`, Factory Method `CadastroUsuarioCreator`, Singleton `ConfiguracaoSistema`. Não criar quinto padrão decorativo.
- Camadas `suporte/http`, `suporte/service`, `suporte/repository`, DTOs/domínio. Handler não escreve SQL. `ExecutorTransacional` compartilhado; `PreparedStatement`, try-with-resources, rollback em mutação multi-etapa, data/hora UTC.
- Sessões, conta ativa, autor real e titularidade validados no backend; mutações protegidas por CSRF. O suporte é perfil independente; conta `usuarios_suporte` não tem status ativo/inativo no schema: não inventar desativação do suporte.

## 1. API de chamados

| Método | Rota | Acesso |
| --- | --- | --- |
| POST | `/api/chamados` | participante ou organizador |
| GET | `/api/me/chamados` | solicitante, apenas próprios tickets |
| GET | `/api/me/chamados/{ticketId}` | titular |
| POST | `/api/me/chamados/{ticketId}/mensagens` | titular, ação **Retornar**, apenas `aguardando_usuario` |
| POST | `/api/me/chamados/{ticketId}/resolucao` | titular, apenas `aguardando_usuario` |
| GET | `/api/suporte/chamados?status=&pagina=&tamanho=` | suporte |
| GET | `/api/suporte/chamados/{ticketId}` | suporte |
| PATCH | `/api/suporte/chamados/{ticketId}/associacao` | suporte |
| POST | `/api/suporte/chamados/{ticketId}/mensagens` | qualquer suporte |
| POST | `/api/suporte/chamados/{ticketId}/encerramento` | suporte, exceto `aguardando_usuario` ou `fechado` |

Tabela `ticket`: `id_ticket`, `titulo`, `descricao`, `resposta`, `atendido`, `status_ticket`, `retorno_usuario`, `id_participante`, `id_organizador`, `id_usuario_suporte`, timestamps. Tabela `ticket_mensagens`: mensagens e autor `participante`/`organizador`/`suporte`, IDs, timestamps. **O histórico confiável é `ticket_mensagens`**; campos legados da tabela ticket podem servir como compatibilidade/snapshot, sem perder autoria.

Ao abrir ticket, gravar `ticket` + primeira mensagem da solicitação transacionalmente, estado `aberto` e vínculo com exatamente um solicitante. Qualquer conta de suporte pode responder independentemente de `id_usuario_suporte` atribuído, gravando em `ticket_mensagens` o **ID do suporte que realmente respondeu**.

## 2. Matriz de estado RF-SUP-04 — SEM mudanças de regra

| Ação | Estado de origem | Estado de destino |
| --- | --- | --- |
| Criar ticket | — | `aberto` |
| Suporte responde | `aberto` ou `em_atendimento` | `aguardando_usuario` |
| Suporte responde novamente | `aguardando_usuario` | `aguardando_usuario` |
| Suporte encerra | `aberto`, `em_atendimento` | `fechado` |
| Suporte tenta encerrar | `aguardando_usuario` ou `fechado` | **bloqueado** |
| Solicitante **Resolve** | apenas `aguardando_usuario` | `fechado` |
| Solicitante **Retorna**, com mensagem não vazia | apenas `aguardando_usuario` | `em_atendimento` |
| Solicitante tenta Resolver/Retornar | `aberto`, `em_atendimento`, `fechado` | **bloqueado** |
| Qualquer nova mensagem em `fechado` | `fechado` | **bloqueado** |

- Revalidar `status_ticket` sob `SELECT ... FOR UPDATE` antes de mudar status e inserir mensagem; ambos em mesma transação e mesma Connection.
- Não permitir reabrir `fechado` ou apagar histórico. Encerramento de suporte pode não incluir texto novo; não fabricar resposta.
- Associação é apenas responsabilidade de acompanhamento; outro suporte pode responder/encerrar, com autoria real.
- Validar payload, IDs positivos, status, titularidade, dono, CSRF no servidor, não só no frontend.
- Quando funcionalidade/campo da associação não estiver determinado, conservar o comportamento do PHP original se compatível e documentar detalhes técnicos; perguntar ao PO somente por decisão funcional realmente bloqueante.

## 3. Indicadores e dashboards reais

Rotas:
- `GET /api/participante/painel`: participante atual; próximos eventos/ingressos, compras, anúncios, comunidades acessíveis e tickets; dados do **próprio** usuário.
- `GET /api/organizador/painel`: organizador atual; totais dos próprios eventos, categorias, ingressos vendidos, valores históricos de compra e estado de eventos, comunidades e chamados; agregações reais do MySQL, sem inventar faturamento recebido ou cobrança externa.
- `GET /api/suporte/painel`: suporte atual; tickets por status, abertos/aguardando/fechados, volume de atendimento, métricas proporcionais às tabelas, sem vazar senhas/dados pessoais.
- Painéis autenticados e por perfil, com SQL de leitura parametrizado e sem N+1 desnecessário.
- Páginas HTML/CSS/JS coerentes com JoinUp original: navegação de cada perfil, filtros e cartões; suporte com lista, detalhe/chat, responder, associar e encerrar; solicitante com abertura/histórico e Resolver/Retornar apenas em `aguardando_usuario`. Exibir mensagens claras de erro/estado, bloquear ações no frontend e **também** no backend.
- Ações demonstrativas (saques/push/notificações) permanecem claramente **simuladas**. Não criar integrações externas.

## 4. MySQL e testes obrigatórios

- Reutilizar CI `-Pmysql-it clean verify` com `mysql:8.4.0`, preservar todas as regressões existentes, inclusive integrações `ComunidadeMySqlIT` da correção 04.1.
- Criar `SuporteMySqlIT` com HTTP real, cookies, CSRF, JDBC real e fixture limpa, cobrindo toda matriz RF-SUP-04 (CA-19/20/21/41–46), autoria cruzada entre dois suportes, titularidade 401/403/404/IDOR, ticket fechado terminal e rollback com trigger proposital.
- Concorrência real: suporte responde ao mesmo chamado, solicitante resolve/retorna em paralelo; nunca perder mensagem nem transicionar a partir de estado inválido.
- Dashboards: diferentes perfis, filtros, isolamento do organizador A e B, dados pós-venda, conta inativa, métricas correctas, ausência de CPF/CNPJ/hash/segredos em payloads.
- Documentar comandos/testes reais e status do CI; não mascarar falhas. Preservar schema/seed; corrigir SQL se necessário só com justificativa técnica rastreável.
- Sanidade de interface no navegador local MySQL com contas fictícias de suporte e participantes/organizadores demo.

## 5. Entregáveis

1. PR da branch `feat/05-suporte-paineis` **não mesclado**.
2. Código testável, frontend navegável, documentação `docs/api/SUPORTE_PAINEIS_HTTP_05.md` e `docs/execucao/ENTREGA_05_RESULTADOS.md`.
3. Testes MySQL concorrentes/rollback e matriz RF-SUP-04 demonstrada.
4. UML fragmento de suporte e painéis refletindo classes reais, como parte da documentação RA2 final.
5. Lista de riscos/limites, inclusive funcionalidades não implementadas.
6. **NÃO MERGEAR**. Enviar PR para auditoria técnica antes de integrar.

**Pronto quando:** um participante/organizador consegue abrir e acompanhar ticket; dois suportes podem responder com autoria individual, o solicitante resolve/retorna somente no estado permitido, fechado é terminal; três painéis exibem dados reais do próprio perfil; CI MySQL completo aprovado.
