# CODEX — Correção 04.1: auditoria de comunidades + integração social real

> **Data:** 11/10/2026 | **Prioridade:** ALTA antes de declarar comunidades/feed concluídos  
> **Repositório:** `ericjs-code/JoinUp-POO`  
> **Branch:** `fix/04-1-comunidades-auditoria` desde `main` atual; **PR sem merge automático**.  
> **Precedente:** [Auditoria cruzada PR #4 + PR #5](../execucao/AUDITORIA_CRUZADA_PR04_PR05.md).

## Missão

Corrigir **de forma direcionada** os gaps e riscos SOC-01..SOC-08 identificados na auditoria do PR #5, mantendo intactos os fluxos de compra/revenda do PR #4. **Não** iniciar tickets/suporte nesta branch; a Entrega 05 começará após validação.

## Leia antes de alterar

1. `docs/execucao/AUDITORIA_CRUZADA_PR04_PR05.md`, inclusive as ressalvas do PR #4.
2. `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md`, `ADENDO_RF-COM-05_COMUNIDADES_POS_EVENTO.md`, `ADENDO_RF-COM-06_DESCOBERTA_PUBLICA.md` e TODOS os demais adendos.
3. `docs/api/CONTRATOS_HTTP_v0.1.md`, `docs/api/COMUNIDADES_SOCIAL_HTTP_04.md`.
4. `docs/execucao/ENTREGA_03_RESULTADOS.md`, `docs/execucao/ENTREGA_04_RESULTADOS.md`, `docs/prompts/CODEX_04_COMUNIDADES_SOCIAL.md`.
5. Classes reais de `comunidades`, `compras`, `revendas`, `GerenciadorSessao`, `ExecutorTransacional`, esquema MySQL e tests existentes.

Respeitar Java 21 puro + `HttpServer`, JDBC, Jackson, Maven, JUnit 5; exatamente os quatro GoF já aprovados. Não inventar frameworks, ORM ou novo GoF.

## A. Gate obrigatório: HTTP/JDBC/MySQL 8.4 reais (SOC-01)

Adicionar `src/test/java/br/com/joinup/integration/ComunidadeMySqlIT.java` (**Failsafe**), executado automaticamente pelo perfil já usado no CI:

`mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify`.

Testes sob **HTTP real com sessão/cookie e banco MySQL 8.4 real**:
- GET de lista e detalhe para visitante sem cookie, somente metadados públicos (RF-COM-06); negar posts/chat público (401) e aos não membros (403).
- Entrada voluntária exigindo ingresso `ativo`/`utilizado`; só ingresso `cancelado` não autoriza; saída desabilita conteúdo; reentrada manual autoriza.
- Acesso pós-evento para ingresso `ativo` ou `utilizado`; compra/revenda pós-evento continuam bloqueadas.
- Organizador dono lê/escreve mensagens onde autorizado; organizador alheio e suporte bloqueados; somente participante elegível publica/comenta/curte.
- Mensagem com resposta válida na mesma comunidade; outra comunidade/inexistente/ID inválido → 400/404/422 previsível e nenhuma inserção; não vazar conteúdo de comunidade alheia.
- Postagens (sem editar/deletar), comentários, curtir repetido (idempotente), descurtir repetido, contador consistente com `postagem_curtidas` e disputas entre chamadas concorrentes.
- CSRF para cada mutação, 401/403/404, SQL/FK/CHECK, rollback de falha intermediária, IDs/números/corpos inválidos.
- Concorrência **revenda último ingresso x publicação** e **revenda último ingresso x entrada na comunidade** usando locks/threads reais; o evento, ticket, status, vínculo e novo conteúdo devem terminar coerentes com ordem de commits.
- Autor das mensagens/posts persistido corretamente; conteúdo histórico mantido após perda de ingresso, sem acesso indevido.
- Nada de modificar/excluir fixtures originais. Criar e limpar apenas dados próprios de teste; sem usar `DROP DATABASE`, `TRUNCATE` ou `FOREIGN_KEY_CHECKS=0`.
- Preservar e executar todos os 236 testes anteriores, além dos 9 unitários do PR #5. Registrar novos totais e não reivindicar teste que não tenha rodado.

## B. Correções de integridade e concorrência (SOC-02 e SOC-03)

- Em `ComunidadeSocialService.enviarMensagem`: `idRespostaA` opcional; se preenchido, exigir ID válido e **mensagem-mãe da mesma comunidade**, mesmo que o usuário conheça o ID de outra comunidade. Fazer validação em JDBC parametrizado e no contexto transacional. Ajustar resposta de erro.
- **Não reutilizar** o ID de comunidade do request para validar resposta sem comparar com `comunidade_mensagens.id_comunidade` da mensagem-mãe.
- Corrigir TOCTOU na **entrada** e nas **mutações comunitárias**: a autorização de elegibilidade por ingresso não pode tornar-se falsa por transferência concorrente entre o check e o INSERT/UPDATE. Aplicar e documentar a **mesma ordem de locks** do comércio (contas/participantes, organizador/evento, ingresso e filiação quando necessário) e evitar `FOR UPDATE` em join desnecessário que gere deadlock.
- Proteger contra ingresso transferido, anúncio vendido e usuário desativado concorrentes; não quebrar acesso pós-evento com `utilizado` (não negociável). Leitura poderá ser otimizada separadamente; mutações precisam de revalidação forte.
- Corrigir status de UI se ela indicar acesso habilitado por mera filiação sem ingresso elegível. Distinga “associado” de “autorizado a ler conteúdo”.
- Se um deadlock ocorrer apesar da ordem, retornar erro recuperável seguro com rollback, sem desabilitar FK ou afrouxar autorização.

## C. Contratos, frontend e feature-gap (SOC-04..08)

- **Compartilhamento RF-SOC-H04:** implementar um botão de **copiar link do post** (frontend). O link deve levar à comunidade e focalizar o post somente **após verificar no backend** que o visitante é membro com ingresso elegível. Pode empregar query `post` e `navigator.clipboard` com fallback apropriado; não expor conteúdo no link.
- **Comprimento de postagem:** SQL `postagens.descricao` é `VARCHAR(500)`; serviço e UI já usam 500, porém `COMUNIDADES_SOCIAL_HTTP_04.md` erroneamente declara 2000. Ajustar contrato **para 500**, sem mudar o banco, e testar limites.
- **Imagem/mensagem:** a regra RF-SOC-H01 admite texto e/ou imagem, mas upload real não foi feito; `imagem` aceita qualquer texto no serviço atual. Implementar ao menos validação de referência **local aprovada** por allowlist explícita ou rejeitar `imagem` diferente de `null` enquanto não há endpoint de upload; não aceitar URL externa, `file:`, `data:` ou caminhos arbitrários. Não afirmar suporte completo a posts somente-imagem sem mídia realmente disponível. Documentar o que falta para completar imagem-only e upload em uma etapa posterior, sem alterar silenciosamente a regra do PO.
- **Paginação:** limite seguro de `busca` e limites razoáveis de retornos de comunidades, chat e feed; não quebrar frontend existente nem forçar novo formato incompatível sem ajustar contrato e testes.
- **Documentação:** corrigir nomes inexatos `CriadorUsuario`, `AutenticadorBase`, `EnvelopeResposta` para classes reais; atualizar `ENTREGA_04_VERIFICACAO.md` com MySQL 8.4 isolado/conta DML existente e remover recomendação de root sem senha/Docker inválido.
- **Maven Wrapper:** verificar origem/integridade do JAR binário `maven-wrapper.jar`, decidir manter somente se confiável ou substituir pelo `mvn` padrão já validado no CI. Não publicar novas dependências não auditadas.
- **Não criar edição/exclusão de posts.** Manter API pública somente de metadados; nunca expor posts, autores de conversas ou comentários a visitante sem permissão.

## D. Evidências, PR e relatório

Entregar:
1. PR `fix/04-1-comunidades-auditoria` **aberto e não mesclado**.
2. `docs/execucao/CORRECAO_04_1_RESULTADOS.md` com cada achado SOC-01..SOC-08 marcado corrigido/parcial/pendente, justificativa e testes, comando/resultado do CI e MySQL.
3. Testes de integração HTTP/MySQL novos rodando no workflow real; novos testes de concorrência e rollback. Evidência da execução do PR no GitHub Actions com contagens.
4. Frontend com botão de compartilhamento validado e screenshots/roteiro real de dois participantes.
5. Contrato `COMUNIDADES_SOCIAL_HTTP_04.md` alinhado a backend e schema, e UML atualizado **apenas se necessário**.
6. Nenhuma mudança funcional silenciosa: se for impossível cumprir a regra de imagem sem upload seguro, tratar como **pendência explicitamente aprovada pelo PO**, não como função concluída.
7. **Sem merge.** Entregar URL e principais riscos para auditoria do Tech Lead.

**Pronto quando:** integração HTTP/MySQL real do social verde, mesmas regras de acesso antes/depois de revenda sob concorrência, resposta a mensagem na própria comunidade, share link protegido, sem permissões mais abertas que o aprovado, regressão integral verde.
