# Auditoria técnica — Entrega 01, PR #2 (após merge)

> **Data:** 10/10/2026 (America/Sao_Paulo)  
> **PR:** [#2 — Usuários, autenticação segura, GoF e MySQL real](https://github.com/ericjs-code/JoinUp-POO/pull/2)  
> **Merge:** `d5804301c1e72f2b138344c3baa27e353b61af2b`  
> **Parecer:** **ACEITO TECNICAMENTE COM RESSALVAS** para prosseguir à Entrega 02. Não confundir aceite do incremento com homologação do produto integral.  
> **Método:** inspeção estática de classes Java, configurações, alteração DDL, fluxo de requisitos e testes de código, além de evidências do PR/Actions; o Tech Lead **não executou Java/MySQL diretamente** nesta sessão. Evidências de execução pertencem aos relatórios do Codex e ao CI do GitHub.

## 1. Verificações fundamentadas

- PR #2 está efetivamente mesclado à `main`: 60 arquivos modificados, cerca de 2441 adições/135 remoções. CI do PR final: [run 38079916818](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38079916818), **success**. CI da `main` no commit do merge: [run 38081252204](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38081252204), **success**.
- Workflow executa `mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify` em Java 21, com MySQL **8.4.0** como service container e publicação de artefatos Surefire/Failsafe/SQL. O [relatório de execução da entrega](./ENTREGA_01_RESULTADOS.md) contabiliza **109 testes (86 Surefire + 23 Failsafe), sem falhas ou erros**, incluindo os 52 anteriores.
- MySQL real: [validação](../banco/VALIDACAO_INTEGRACAO_01.md) relata schema com **22 tabelas, 42 FKs e 15 CHECKs**, seed idempotente, nove hashes PBKDF2 persistidos, testes HTTP/JDBC e rolagem de transações.
- **Singleton GoF** `ConfiguracaoSistema`: permanece imutável, init-on-demand holder, sem `Connection` global.
- **Factory Method GoF** `CadastroUsuarioCreator` e `CadastroParticipanteCreator`/`CadastroOrganizadorCreator`/`CadastroSuporteCreator`: `cadastrar` final usa criação polimórfica protegida, em cadastro persistente. Autorização de suporte também é validada no creator.
- **Template Method GoF** `AutenticacaoTemplate`: `autenticar` final chama buscas especializadas por repositório, verificação de senha, situação ativa, hook por perfil e emissão de sessão; subclasses separadas por papel. Não é classe demonstrativa sem uso.
- **Controle de acesso:** `UsuarioRoutes`, `ContaService` e `GerenciadorSessao` fazem autenticação/perfil e conta ativa no servidor; sessão é token aleatório e hash armazenado em `sessoes`; logout e desativação revogam; rotas mutáveis têm verificação CSRF baseada em origem fixa, incluindo Origin/Referer e Fetch Metadata.
- **Transações:** `ExecutorTransacional` compartilha a mesma conexão JDBC, `READ_COMMITTED`, commit/rollback. Desativação de participante cancela anúncios disponíveis e sessões no mesmo fluxo, preservando dados anteriores. Reativação registra auditoria.
- **Rotas parametrizadas** foram adicionadas sem enfraquecer 404/405/traversal, com testes dedicados.
- **Compatibilidade SQL:** o MySQL apresentou erro real `3823` no esquema original. Alterações em `schema_v0.2.sql`: cinco FKs de autoria passaram a ações referenciais compatíveis com `CHECK`, preservando todas as FKs, regras de autoria e registros históricos. [Justificativa](../banco/CORRECAO_DDL_01.md). **Correção técnica compatível com os requisitos** de não exclusão física de contas.

## 2. Ressalvas e ações seguintes

| ID | Prioridade | Ponto | Encaminhamento |
| --- | --- | --- | --- |
| A01 | MÉDIA | Os documentos de schema ainda contêm no cabeçalho “NÃO EXECUTADO NO MYSQL”, embora MySQL 8.4 já tenha sido validado após correção. | Atualizar metadados de status/referências na Entrega 02, **sem alterar DDL funcional**. |
| A02 | ALTA para produto, não defeito desta etapa | Compra/eventos/feed/suporte ainda não existem como fluxos completos; o frontend atual é apenas mínimo. | Manter escopo por incremento, priorizando Facade/eventos no Prompt 02 e integração end-to-end até 15/10. |
| A03 | MÉDIA | CSRF, cookies e servidor foram testados em HTTP local e componente HTTPS, não há validação de proxy/TLS real; sem rate limiting. | Não expor publicamente antes de revisar infraestrutura/cabeçalhos; cenário acadêmico local não deve ser descrito como produção segura. |
| A04 | MÉDIA | `JOINUP_DEMO` está reservado; carga de demonstração é manual e não é iniciada automaticamente. | Implementar/validar carga idempotente restrita a ambiente demo antes da entrega RA2 (BD-02); jamais executar automaticamente em produção. |
| A05 | MÉDIA | UML parcial GoF foi ajustado aos métodos existentes, mas Facade e diagrama integral de classes ainda precisam refletir código final. | Atualizar ao implementar Facade no Prompt 02, manter classes/associações coerentes, gerar documento final RA2. |
| A06 | BAIXA | O fluxo de revisão do PR foi ultrapassado porque o merge ocorreu antes da auditoria. | Nos próximos PRs, aguardar o parecer técnico antes de mesclar; sem necessidade de reverter o #2 por esse motivo. |
| A07 | MÉDIA | Testes de segurança e transações são fortes para o escopo; ainda faltam testes de regressão quando mais handlers/conexões forem introduzidos. | Manter suite MySQL real no CI como gate permanente e exigir teste de rollback em toda operação multi-entidade. |
| A08 | MÉDIA | Login segue o padrão Template Method real; o hook específico verifica coerência de produto/classe, enquanto a busca no repositório é a especialização funcional relevante. | Na documentação RA2, explicar corretamente a distinção da busca por tipo; não alegar políticas exclusivas de login que não existem. |

## 3. Aprovação por tópico

| Tópico | Parecer |
| --- | --- |
| Java puro/JDK HttpServer/JDBC/Maven | **Conforme** |
| GoF Singleton | **Conforme** |
| GoF Factory Method | **Conforme** |
| GoF Template Method | **Conforme** |
| Banco MySQL isolado e seed | **Conforme para Entrega 01, com correção DDL documentada** |
| Proteção de rotas, sessão e transações | **Conforme no escopo testado** |
| Fidelidade completa da UI | **Não entregue ainda (fora do escopo do incremento)** |
| Sistema JoinUp integral | **Não homologado / em desenvolvimento** |

## 4. Próxima decisão técnica

**Liberar Entrega 02 — Eventos e categorias**, aplicando o quarto GoF (**Facade**) no fluxo real de cadastro/edição e criação automática de comunidade, com **transação única**, validações de titularidade e limites temporais RF-EVT-06/07/08, frontend funcional e testes no MySQL real. Não criar endpoints de compra/revenda/social/suporte nesta entrega.

Ao final do PR 02 da próxima entrega (provável PR #3), auditoria **antes do merge**. Não supor que um CI verde equivale a aceite sem inspeção do delta.
