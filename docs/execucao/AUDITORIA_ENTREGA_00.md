# Auditoria técnica — Entrega 00 (após merge)

> **Data:** 10/10/2026  
> **PR auditado:** [#1 — Entrega 00: Fundação Java 21 puro](https://github.com/ericjs-code/JoinUp-POO/pull/1)  
> **Commit de merge na main:** `137d6196f42f94531db593596b2696a7609e41d2`  
> **Parecer:** **ACEITO COM RESSALVAS** como **fundação HTTP/Java**, **NÃO HOMOLOGADO** como banco funcional.  
> **Natureza da auditoria:** inspeção estática do código e testes do GitHub + resultados informados pelo Codex e CI. O Tech Lead não executou `mvn`, Java nem MySQL neste ambiente; não apresentar inspeção como execução independente.

## Evidências conferidas

- O PR #1 está `merged=true`, com 29 arquivos alterados (+1301 linhas), integrado à `main`.
- O workflow `Fundação Java 21` em `main` concluiu com `success` após o merge (run `38077670849`). O CI executa `mvn --batch-mode --no-transfer-progress clean verify` sem MySQL.
- O relatório `docs/execucao/ENTREGA_00_RESULTADOS.md` registra 52 testes concluídos: 16 de `ConfiguracaoSistemaTest`, 33 de `HttpFoundationTest`, 2 de `ApplicationBootstrapTest` e 1 de `SeedDemoPasswordTest`. Código dos testes inspecionado; testes usam HTTP real local, sem banco.
- `pom.xml` usa Java 21, Maven, Jackson, MySQL Connector/J, JUnit 5 e empacotamento JAR; não há Spring, Javalin, Hibernate nem ORM.
- `ConfiguracaoSistema` usa Singleton thread-safe via *initialization-on-demand holder*, campos privados `final` e construtor privado; é efetivamente consumido em `Main`, bootstrap e JDBC. Nenhuma conexão ou sessão fica no Singleton.
- `ApplicationBootstrap` compõe componentes manualmente, inicia `HttpServer` em executor controlado e fecha recursos. Testes verificam liberação de porta.
- `Router`, `RequestBody`, `RequestPath`, `StaticFileHandler`, `JsonCodec` e `HttpResponses` implementam fundação HTTP: 404/405, erros JSON, limite de corpo, caminhos estáticos do classpath, testes de traversal e requisições concorrentes.
- `ConnectionFactory` abre conexões por operação e não guarda `Connection` global. Ainda não há chamada JDBC real.
- `HealthHandler` retorna liveness `UP` e, corretamente, não atesta conectividade ao banco.
- Schema e seed v0.2 permaneceram inalterados no PR conforme relatório/diff; teste PBKDF2 leu **somente arquivo SQL no classpath**, não dados persistidos.

## Ressalvas obrigatórias para próxima etapa

**R01 — Prioridade ALTA: validação MySQL ausente.** Não foram efetivamente executados schema, FKs/CHECKs, seed idempotente, conexão JDBC ou login com contas do banco. O próprio relatório informa `NÃO EXECUTADO`. Antes de declarar usuários/autenticação completos, a Entrega 01 deve validar contra MySQL 8 real, preferencialmente com serviço CI isolado, e registrar os resultados. Sem isso, o aceite funcional de banco/autenticação fica bloqueado.

**R02 — Prioridade ALTA: rotas dinâmicas ausentes.** O roteador atual registra caminhos literais. Os contratos incluem `{id}` e `{tipo}`, como `/api/suporte/contas/{tipo}/{id}/reativacao`. Evoluir roteador para parâmetros com correspondência exata por segmento e testes (evitar `startsWith` permissivo que cause bypass/rotas ambíguas).

**R03 — Prioridade ALTA: autenticação, autorização e CSRF ainda não existem.** Por escopo da Entrega 00, não é defeito da primeira entrega; são *gates* da 01. Testar 401/403, titularidade, sessão revogada, conta inativa e proteção de operações mutáveis. Evitar segurança apenas na interface.

**R04 — Prioridade MÉDIA: limite global de 64 KiB.** É adequado à fundação JSON, mas uploads de imagens previstos no JoinUp exigirão limites específicos e parser multipart seguro; não elevar limite irrestritamente. O roteador lê corpo antes de identificar rota; revisar para rotas de upload quando forem implementadas.

**R05 — Prioridade BAIXA: README pós-merge.** O passo `git switch feat/00-fundacao-java-puro` não deve ser recomendado para novos clones depois do merge; atualizar para `main` durante a próxima entrega.

**R06 — Prioridade MÉDIA: serialização de datas em JSON.** Caso a API utilize `LocalDate`/`OffsetDateTime`/`Instant`, configurar suporte Jackson JSR-310 ou mapear DTOs explicitamente, com teste de payload; não alterar o contrato de datas silenciosamente.

**R07 — Prioridade MÉDIA: Bootstrap HTTP de produção deve manter segredos fora de logs/cookies.** Não publicar senhas de seed como credenciais públicas de ambiente real. `JOINUP_DEMO` está reservado, ainda não aplica seed; a carga automatizada continua pendente do escopo integral.

## Conclusão e sequência

**Aceitar a Entrega 00 como fundação Java puro**: os recursos definidos no escopo são compatíveis com a stack, e CI passou. **Não marcar integração com MySQL como aprovada/testada**.

Próxima ação: executar `docs/prompts/CODEX_01_USUARIOS_AUTENTICACAO.md` em branch própria, com validação MySQL como **fase obrigatória anterior ao aceite funcional**. Se houver falha de SQL, apresentar a causa e correção mínima rastreada, sem editar requisitos silenciosamente.

**Fluxo a partir daqui:** Codex abre PR → Tech Lead audita **antes** do merge → PO valida mudanças de escopo quando necessárias.
