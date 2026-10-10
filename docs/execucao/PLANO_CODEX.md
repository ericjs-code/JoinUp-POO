# JoinUp-POO — Plano de execução orientado por Codex

> **Data:** 10/10/2026  
> **Status:** execução AUTORIZADA pelo PO; cada entrega sujeita à auditoria técnica e aceitação.  
> **Prazo acadêmico informado:** entrega 15/10/2026, apresentação 16/10/2026.

## Papéis

- **Product Owner:** aprova requisitos e mudanças no comportamento, avalia os resultados de cada incremento.
- **Tech Lead (ChatGPT):** organiza a execução, redige prompts, mantém rastreabilidade, revisa PRs, aponta desvios e recomenda aceite/retrabalho.
- **Codex:** escreve código e testes, roda verificações, documenta resultados e entrega em branches/PRs.
- **Repositório:** `ericjs-code/JoinUp-POO`. Referência PHP somente leitura: `ericjs-code/JoinUp`.

## Fontes de verdade e ordem de precedência

1. Requisitos aprovados: `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` **e TODOS os arquivos `docs/requisitos/ADENDO_*.md`**, incluindo regras RF-COM-05/06, RF-REV-05, RF-SUP-04 e RF-EVT-06/07/08.
2. Stack aprovada: `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`.
3. API como referência de desenvolvimento: `docs/api/CONTRATOS_HTTP_v0.1.md` (aprovado para iniciar, refinável com registro, sem alterar regras do PO).
4. Banco: `src/main/resources/db/schema_v0.2.sql`, `seed_demo_v0.2.sql`. Scripts versionados e aprovados **para desenvolvimento, ainda não verificados em MySQL**.
5. Classes e GoF propostos: `docs/arquitetura/CLASSES_E_PADROES_GOF_v0.1.md`, `docs/diagramas/CLASSES_GOF_v0.1.puml`, além do draw.io completo fornecido ao PO, ainda a sincronizar com a implementação.
6. Repositório PHP anterior como referência de comportamento/identidade visual, nunca como código para migração automática.

Havendo conflito de documentação ou regra imprecisa, **não tomar decisão funcional em silêncio**. Registrar `BLOQUEIO/PERGUNTA AO PO` e continuar por aspectos independentes.

## Restrições obrigatórias

- Java 21 **puro**, `jdk.httpserver`, JDBC/MySQL 8, Maven, Jackson e JUnit 5. Sem Spring Boot, Javalin, ORM ou servidor de Servlet.
- **Somente estes quatro GoF como foco formal da RA2:** Facade, Template Method, Factory Method e Singleton. Padrões devem estar aplicados a fluxos reais, com testes e UML compatível; não construir classes decorativas.
- API valida autenticação, perfil, propriedade, regras de status e transações, mesmo que a UI esconda opções.
- Não incluir gateway real de pagamento, notificações push ou integração de saque; preservar comportamento demonstrativo do PHP.
- Não executar scripts contra o banco antigo `joinup`; usar **`joinup_poo` isolado**.
- Não commitar credenciais reais, dados pessoais de produção, arquivos de IDE, binários ou outputs de build.
- Não considerar verificado um teste que não foi executado. Diferenciar unitário, HTTP e integração real com MySQL.

## Sprints / entregas

| Entrega | Foco técnico | Evidências de aceite |
| --- | --- | --- |
| **00 — Fundação** | Maven Java21, HttpServer, rotas, JSON/erros, configuração Singleton, JDBC, documentação para executar, validação de schema e seed quando houver MySQL | `mvn clean test` passa; `GET /api/health`; testes de configuração; relatório de SQL executado **ou** bloqueio verificado |
| **01 — Usuários e autenticação** | Cadastro dos três perfis com Factory Method; login dos três via Template Method; hash PBKDF2, sessões, permissões, perfis, desativação/reativação | Testes de login por perfil, criação de suporte restrita, desativação/reativação e auditoria |
| **02 — Eventos e ingressos de catálogo** | Eventos, artistas/line-up, categorias, comunidades automáticas via Facade; edição e restrições RF-EVT | Testes de evento+categorias+comunidade e rollback; categorias vendidas bloqueadas; data futura |
| **03 — Compra e revenda** | Emissão, estoque, transações, concorrência, anúncios, compra/revenda e regras de contas inativas | Dois compradores/uma unidade, transferência atômica, cancelamento, bloqueio pós-evento, titularidade |
| **04 — Comunidades e social** | Associação voluntária, mensagens e acesso, posts, comentários, curtidas, imagens; pós-evento | Testes de filiação+ingresso ativo/utilizado e remoção após última revenda; frontend correspondente |
| **05 — Suporte e painéis** | Tickets, estados, mensagens por autoria, qualquer suporte responde, indicadores e telas completas | RF-SUP-01 a 04 e transições; perfis e dashboards utilizáveis |
| **06 — Consolidação RA2** | Interface com paridade visual/funcional, ajustes, UML coerente com classes reais, relatório, guia de execução e roteiro de apresentação | Auditoria integrada por requisitos, testes/documentação completos e apresentação ensaiável |

**Observação de prazo:** as atividades de frontend e documentação devem começar desde os primeiros módulos, e não ser deixadas inteiramente para o sprint 06. O plano é uma sequência lógica, não garantia de entrega integral até 15/10.

## Fluxo de cada incremento

1. Tech Lead redige **prompt de escopo delimitado** com fontes, critérios de aceite e restrições.
2. Codex abre branch `feat/NN-assunto` a partir de `main`, inspeciona fontes, implementa e executa verificações que forem possíveis.
3. Codex apresenta PR ou referência de commit/diff e relatório objetivo: arquivos, decisões, comandos+resultados, testes não executados, riscos e pendências.
4. Tech Lead revisa via GitHub, confronta requisitos/segurança/GoF, solicita correções ou recomenda aceite.
5. PO aprova mudanças funcionais e marcos relevantes. Após aceite técnico, integrar à `main` conforme fluxo de trabalho do repositório.

**Definição de pronto:** fluxo funcional demonstrável; segurança no backend; verificações executadas e informadas; sem pendência crítica omitida; alterações rastreadas por IDs de requisitos.

## Primeira ação

Utilizar `docs/prompts/CODEX_00_FUNDACAO_JAVA_PURO.md` como prompt inicial para a **entrega 00**. Não pedir o projeto inteiro em um único prompt.
