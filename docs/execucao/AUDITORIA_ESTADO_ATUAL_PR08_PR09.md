# Auditoria de estado — PR #8 e PR #9 (base atual da RA2)

> **Conferência:** 12/10/2026, America/Sao_Paulo.  
> **Baseline de código:** PR #8 merge `7efe29c1529a71e3c1a82a52716a5a32947a61fe`, seguido do PR #9 merge `fdc8429d4919a2abefb1382c9fcd5d3a8aaa32e7` na `main`.  
> **Parecer:** **LIBERAR ESTABILIZAÇÃO/VALIDAÇÃO RA2** com ressalvas; **não homologar produto integral nem marcar 45 E2E como executados**.  
> **Método:** análise estática de repositório GitHub, relatórios/testes/código e estados GitHub Actions. O Tech Lead **não executou a aplicação em ambiente próprio** nesta auditoria.

## 1. O que realmente entrou

### PR #8 — [Mídia contextual e demo idempotente](https://github.com/ericjs-code/JoinUp-POO/pull/8)

- Código `arquivos/`: parser multipart binário estrito, validação MIME/assinatura, decodificação ImageIO e regravação PNG, limites de 1 MiB/4096 px/4 MP, arquivos em diretório privado (UUID, nofollow), `POST/GET /api/arquivos` com autorização por evento/comunidade.
- `ArquivoService` revalida conta/perfil, dono/evento futuro, filiação/ingresso elegível e vínculo/contexto com linha JDBC e transações. Imagem de evento só pública quando referencia a imagem atual, comunidade só legível por ator elegível. Posts e mensagens aceitam somente imagem ou imagem+texto; vazios são negados.
- DDL: `migration_06_midia_demo.sql` é **migração administrativa nova**, sem edição no schema/seed aprovados. Introduz `arquivos` e `demo_execucao`; requer execução administrativa na instalação.
- `DemoRunner` no bootstrap: `JOINUP_DEMO=true` e `JOINUP_ENV=demo`, banco `joinup_poo` em loopback, HTTP local, schema/migração pré-existentes; recusa base populada sem marcador; sem DDL na aplicação. Seed e marcador em uma transação, reinício não reseta dados.
- `RequestBody`: limite por rota, ainda 64 KiB para JSON, 413 quando excedido. Código final preservou cenário de transporte que falhou em versões intermediárias.
- [CI do código final](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38199379600) **completed/success** e [CI após merge #8](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38247779976) **completed/success**. Relatório: **468 testes** = 167 Surefire + 301 Failsafe, todos aprovados com Java 21 e **dois MySQL 8.4**, preservando os 397 anteriores; novos cenários de mídia/demo são HTTP/JDBC e JVM isolada.
- O relatório `docs/execucao/ENTREGA_06_RESULTADOS.md` inclui sanidade de navegador e capturas reais, mas não E2E integral de apresentação.

**Parecer #8:** aceito tecnicamente no **escopo acadêmico local** pelos resultados de CI e inspeção; sem defeito bloqueante comprovado na revisão. **Riscos conhecidos:** sistema de arquivos + MySQL não compartilham commit distribuído; limpar pending expirado e órfãos após crash é procedimento administrativo, sem reconciliação automática; sem quota/rate-limit/antivírus, sem garantia de produção. Não reabrir isso agora sem falha real ou exigência explícita.

### PR #9 — [Relatório RA2 e UML integral](https://github.com/ericjs-code/JoinUp-POO/pull/9)

- Somente documentação e diagramas. Relatório `docs/ra2/RELATORIO_TECNICO_RA2.md`, UML geral `DIAGRAMA_CLASSES_GERAL.puml/.svg`, visão resumida, inventário de classes, matriz de 45 casos e roteiro de apresentação.
- O trabalho examinou `main` **após #8**; inventário documental aponta **123 tipos nomeados em 117 fontes Java**, diagrama integral de ~99 KB e SVG correspondente. Visão resumida existente para uso em slides/apresentação.
- Quatro GoF estão associados a classes reais e casos de uso: `ConfiguracaoSistema` Singleton, `CadastroUsuarioCreator` Factory Method, `AutenticacaoTemplate` Template Method, `EventoFacade` Facade. Documento distingue tradeoffs e alternativas, sem reivindicar quinto GoF.
- `VALIDACAO_DOCUMENTAL.md` declara PlantUML `-checkonly`/renderização Graphviz concluídos, 167 Surefire executados localmente sem MySQL, e conferência do [CI pós-#8](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38247779976).
- [CI do PR #9](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38251275138) **completed/success**. O workflow da `main` recém-mergeada, [run 38252383206](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38252383206), **concluiu com sucesso** após a conferência final.
- **A matriz V01–V45 continua integralmente `NÃO EXECUTADO` quanto ao ensaio dessa frente**. As suites CI e sanidades anteriores são evidências separadas; não alterar rótulos sem nova execução e registro individual.
- **Enunciado integral RA2 não estava disponível** para a frente documental; equipe/instituição/turma/professor e duração real da apresentação ainda são campos a confirmar. Sem isso não garantir conformidade integral com critérios de nota.

**Parecer #9:** aceito como **base documental**, condicionado à confirmação acadêmica e à execução/correção da matriz/roteiro. A documentação não substitui o ensaio.

## 2. Quadro de prioridades até 15/10

| Pri. | ID | Ação concreta | Gate |
| --- | --- | --- | --- |
| **P0** | FIN-01 | Reconfirmar CI verde da `main` após PR #9 ao congelar o release; já verificado como completed/success nesta auditoria. | Checks `completed/success` |
| **P0** | FIN-02 | Instalação limpa em **nova base isolada**: JDK 21, Maven, MySQL 8, `schema_v0.2.sql` → `migration_06_midia_demo.sql`, conta DML, `JOINUP_ENV=demo` + `JOINUP_DEMO=true`, upload dir externo. | App inicia; três logins e recursos reais funcionam; versão e evidências |
| **P0** | FIN-03 | Executar roteiro integrado dos três perfis com **cookies separados** e matriz de 45 casos; registrar os realizados como `EXECUTADO / APROVADO` ou `REPROVADO` com evidência, mantendo não feitos como NÃO EXECUTADO/BLOQUEADO. | Fluxo público → evento/compra → ingresso/revenda → comunidade/feed/chat/mídia → chamados/dois suportes → dashboards |
| **P0** | FIN-04 | Confirmar enunciado original da RA2, integrantes, instituição/turma/professor, tempo e formato final. | Relatório conforme critérios reais, sem placeholders |
| **P1** | FIN-05 | Corrigir **README desatualizado**: orienta trocar à branch `feat/06-midia-demo` apesar de #8 já integrado. Documentar instalação e execução diretamente de `main`, migração/conta DML, sem segredos reais. | Pessoa nova consegue reproduzir execução |
| **P1** | FIN-06 | Revisar consistência final entre commit/JAR, `MATRIZ_VALIDACAO_FINAL.md`, UML integral/resumo, relatório, testes, PRs; ensaiar apresentação cronometrada. | Diagrama legível, quatro GoF demonstráveis, roteiro cabe no tempo |
| **P2** | FIN-07 | Verificar mobile, navegação por teclado, acessibilidade básica e casos de borda, caso tempo permita após P0/P1. | Evidência por dispositivo/resolução, registrar pendências |

## 3. O que NÃO reabrir sem justificativa

- Não criar novas features ou quinto GoF, não refatorar arquitetura inteira nem substituir servidor JDK/SQL.
- Não introduzir gateway de pagamento, notificações reais, deployment público, antivírus empresarial, CDN, reconciliação permanente ou grandes otimizações de escala como se fossem pré-requisitos acadêmicos, salvo enunciado explícito.
- Não executar testes destrutivos na base da apresentação. Testes SQL/concorrência com triggers devem usar ambiente `I` separado.
- Não fazer merge de correções antes de revisão; centralizar defeitos encontrados em relatório e PR pequeno.

## 4. Decisão

**PROSSEGUIR para a validação final** via `docs/prompts/CODEX_08_VALIDACAO_FINAL_RA2.md`, a partir da `main` atualizada. **Priorizar uma versão executável e demonstrável antes de 15/10**. Para o usuário/PO, solicitar o enunciado integral e os metadados acadêmicos antes de produzir a entrega final; o trabalho de testes pode começar sem essa confirmação.

Esta auditoria acrescenta apenas um registro documental. O produto não foi executado ou modificado pelo revisor.
