# Auditoria técnica — PR #6, Correção 04.1 Comunidades/Social

> **Data da análise:** 11/10/2026 (America/Sao_Paulo)  
> **PR:** [#6 — Correção 04.1: estabiliza Comunidades/Social com integração HTTP/MySQL](https://github.com/ericjs-code/JoinUp-POO/pull/6)  
> **Merge já realizado:** `276e9df6b266759be13088238e3c233bd574ab33` (main)  
> **Parecer:** **ACEITE TÉCNICO COM RESSALVA FUNCIONAL SOC-06**, suficiente para liberar a Entrega 05, mas **não** para declarar todo o módulo social 100% concluído.  
> **Método:** revisão estática de código Java/JS, testes e docs no GitHub, conferência dos estados GitHub Actions. O revisor não rodou Java, navegador ou MySQL em ambiente próprio; os resultados operacionais são do Codex e do GitHub Actions.

## 1. Evidências do PR e dos testes

- PR #6 está efetivamente merged. 20 arquivos alterados; classes relevantes: `ComunidadeSocialRepositoryJdbc`, `ComunidadeSocialService`, `ComunidadeRoutes`, `ComunidadeDetalheView`, UI e teste novo `ComunidadeMySqlIT`.
- [Workflow do PR final](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38191563910) — **completed: success** para commit `232e06ce2f13f6f1aa4391002aab4098f06d92d3`.
- A execução sobre `main` mergeada, [run 38191964982](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38191964982), estava **in_progress** na consulta efetuada durante esta auditoria. Confirmar que terminou `success` antes de usar a `main` como baseline de integração seguinte.
- Relatório `docs/execucao/CORRECAO_04_1_RESULTADOS.md` e evidências `docs/execucao/evidencias/correcao_04_1_ci.txt`: **310 testes** (136 Surefire + 46 `MySqlIntegrationIT` + 63 `ComercioMySqlIT` + 65 `ComunidadeMySqlIT`), sem falhas/erros/ignorado, com Java 21 e MySQL 8.4.0 real. O status do workflow confirma sucesso; os totais seguem o relatório e as evidências do Codex.
- Nenhum código de compras, revenda, autenticação ou schema/seed foi alterado no PR #6. Os quatro GoF permanecem presentes e a regressão existente integra o CI.

## 2. Conferência SOC-01 a SOC-08

| Achado | Parecer | Evidência técnica |
| --- | --- | --- |
| SOC-01 — testes sociais apenas mockados | **Corrigido** | Nova suíte `src/test/java/br/com/joinup/integration/ComunidadeMySqlIT.java`: HTTP, cookie, CSRF, JDBC e MySQL; 65 cenários incluindo FK/CHECK, paginação, rollback, permissões e concorrência. |
| SOC-02 — resposta a mensagem de outra comunidade | **Corrigido** | `ComunidadeSocialService.enviarMensagem` exige ID positivo, e `ComunidadeSocialRepositoryJdbc.respostaNaComunidade` verifica `id_comunidade` da mensagem-mãe sob `FOR SHARE`. A API retorna 404 indistinguível para mensagem inexistente/de outra comunidade. |
| SOC-03 — interleaving revenda/social | **Corrigido nos cenários testados** | `bloquearAcesso` bloqueia conta, evento, ingressos elegíveis e filiação na transação; revalida associação/ingresso. Teste `revendaUltimoIngressoSerializaComSocial` inclui 4 ordens de entrada/publicação vs venda, com espera de lock real; demais testes exercitam mensagens, comentários e curtidas após venda. Não há prova universal de ausência de deadlock em SQL futuro. |
| SOC-04 — link compartilhável de post | **Corrigido** | `comunidades.js` oferece Copiar link, query de post, consulta ao detalhe protegido e destaque/foco; visitante recebe metadados, não o conteúdo privado. Fallback de clipboard não foi exercitado automaticamente no navegador. |
| SOC-05 — divergência comprimento de post | **Corrigido** | Contrato agora indica 500 caracteres, igual a `VARCHAR(500)` e ao serviço/UI; teste HTTP de borda 500/501. |
| SOC-06 — imagens e post somente-imagem | **PENDENTE FUNCIONAL** | `validarImagem` rejeita qualquer `imagem != null`; URLs arbitrárias e arquivos não entram no banco, mas o requisito herdado de publicar somente imagem e fluxo seguro de upload continuam não atendidos. Não alterar escopo/requirement silenciosamente; validar com PO na consolidação. |
| SOC-07 — paginação e acesso efetivo | **Corrigido para demo** | Limites `pagina`/ `tamanho`, `busca` até 100, SELECT com LIMIT/OFFSET; view distingue `membroAtivo`, `elegivelParaEntrar`, `acessoConteudo`; UI usa acesso efetivo. Sem benchmark de carga/cursor. |
| SOC-08 — docs e wrapper | **Corrigido documentalmente** | `ENTREGA_04_VERIFICACAO.md` corrigida, nomes reais das classes, origem/hash do Maven Wrapper documentados em `.mvn/wrapper/ORIGEM.md`. SHA reportado pelo Codex, não calculado independentemente pelo revisor. |

## 3. Achados adicionais e riscos residuais

1. **Desempenho:** `bloquearAcesso` aplica locks conservadores inclusive às operações de leitura autenticada; gera serialização/espera desnecessária sob uso intenso, mas preserva segurança no escopo demonstrativo. Avaliar otimização após RA2 sem enfraquecer atomicidade de mutações.
2. **Paginação OFFSET:** não dá snapshot imutável; entradas novas podem deslocar itens entre páginas e o limite 10000 não substitui cursor para escala.
3. **Imagem/UX:** o sistema social executa o fluxo textual, mas UI de upload/post somente-imagem e mensagens com imagem não estão prontas. Risco de demonstração se esse caso for cobrado; explicitá-lo no quadro de pendências.
4. **Outros pendentes já documentados:** idempotência de POST de compra, `JOINUP_DEMO` automático, upload de evento/perfil, revisão do UML/relatório RA2 e validação end-to-end por papéis. Não confundir pendências com regressões introduzidas pelo PR #6.
5. **Processo:** PR #6 foi mesclado antes desta auditoria; definir que os próximos PRs aguardem review. Não reverter automaticamente um PR que passou checks e cuja inspeção não revelou bloqueio crítico.

## 4. Decisão

**ACEITAR TECNICAMENTE a Correção 04.1 COM RESSALVA SOC-06.** Os gates técnicos prioritários de integridade e acesso social foram tratados, incluindo teste real MySQL, resposta parent da própria comunidade e concorrência de revenda; nenhuma regressão crítica comprovada nesta auditoria.

**LIBERAR Entrega 05 — Suporte e Painéis** usando `docs/prompts/CODEX_05_SUPORTE_PAINEIS.md`, a partir da `main` mergeada, **após conferir a conclusão verde do CI pós-merge**. Não reabrir o PR #6 para adicionar novas funcionalidades; controlar SOC-06 na estabilização/mídia.

**Planejamento até 15/10:** Entrega 05 → demonstrativo automático BD-02 e política segura de imagens (escopo autorizado pelo PO) → relatório/UML RA2 e testes completos → apresentação. A cobertura integral da plataforma ainda depende dessas etapas.

**Nenhum código de produção foi modificado por esta auditoria**; apenas este parecer escrito foi acrescentado ao GitHub.
