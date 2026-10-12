# Auditoria cruzada — PR #4 (Comércio) e PR #5 (Comunidades e social)

> **Data:** 11/10/2026  
> **Tech Lead:** ChatGPT (inspeção estática + conferência de evidências GitHub Actions)  
> **Base examinada:** `main` após merge do PR #5, commit `0b74bbc2d21018d4363878f4291e892293f14846`  
> **Status:** **PR #4 aceito com ressalvas; PR #5 aceito somente como incremento parcial — correções obrigatórias antes de homologar o módulo social.**

## Método e evidências

- [PR #4](https://github.com/ericjs-code/JoinUp-POO/pull/4), merge `3d3e679eb6e897d4b41a0072889af799e0a5ccf8`, elaborado pelo usuário `ericjs-code`; [CI após merge](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38086368376) **success**. Evidência do PR/relatório: **127 unitários + 109 HTTP/JDBC = 236 testes**, MySQL 8.4 real, zero falhas, erros ou ignorados.
- [PR #5](https://github.com/ericjs-code/JoinUp-POO/pull/5), merge `0b74bbc2d21018d4363878f4291e892293f14846`, elaborado pelo usuário `PedroHenriqueRebechi`; [CI após merge](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38183840138) **success**.
- PR #5 adicionou **9 testes unitários isolados**, rodados via `mvnw.cmd test`, que totalizam **136 testes Surefire** (127 anteriores + 9). Como o CI principal mantém o perfil `-Pmysql-it`, há também regressão das **109 integrações MySQL existentes**, porém **não foram adicionados testes HTTP/JDBC MySQL do módulo social**. Portanto o CI verde **não** comprova os novos fluxos em MySQL real.
- Inspecionados: `CompraService`, `RevendaService`, `CompraRepositoryJdbc`, `IngressoRepositoryJdbc`, `RevendaRepositoryJdbc`, `GerenciadorSessao`, `ExecutorTransacional`, `ComunidadeSocialService`, `ComunidadeSocialRepositoryJdbc`, `ComunidadeRoutes`, `comunidades.js/html`, testes e relatórios/contratos dos dois PRs. Comparação com especificação e adendos RF-REV-05, RF-COM-05/06, RF-SOC-H01/H04, RF-SOC-01.
- **Não foi executada neste ambiente** uma instância adicional de Java/MySQL nem testes de navegador; não atribuir a inspeção independente um resultado de execução inexistente.

## PR #4 — Compra direta e revenda

### Pontos conformes

1. **Compra direta:** `CompraService.comprar` bloqueia contas, organizador, evento e categorias; preço e total em `BigDecimal` calculados do banco; baixa de estoque condicional; compra, itens, snapshots e ingressos individuais na mesma transação.
2. **Concorrência/rollback:** cenários documentados/testados no MySQL real para última unidade (201/409), mesmo anúncio (200/409), alterações simultâneas, falhas por triggers e timeout/deadlock tratado em `ExecutorTransacional`; rollback ao ocorrer erro.
3. **Revenda:** `RevendaService` garante vendedor titular, status ativo, evento futuro, organizador ativo, venda única e transferência na mesma transação; cancelamento mantém propriedade, reanúncio cria nova linha histórica.
4. **Perfil RF-REV-05:** todas as operações/consultas de revenda do serviço exigem participante autenticado ativo; sem exposições públicas não autorizadas.
5. **Comunidade na revenda:** `IngressoRepositoryJdbc.sairSemIngressoElegivel` inativa o vínculo após venda concluída do último ingresso `ativo` ou `utilizado`; não remove associação quando o anúncio é somente criado; comprador não é associado automaticamente.
6. Quatro GoF anteriores preservados. Sem integração real de pagamento.

### Ressalvas — PR #4

| ID | Gravidade | Ponto | Ação |
| --- | --- | --- | --- |
| COM-01 | Média | Sem idempotency key para POST de compra direta; uma nova requisição após resposta perdida pode fazer outra compra válida. | Documentar comportamento de não retry automático e considerar token/chave técnica para evolução, sem inventar regra de cobrança real. |
| COM-02 | Média | Leitura inicial de pistas é sem lock e revalidada depois; a estratégia é razoável nos cenários testados, mas a ordem de locks entre novos módulos deverá ser respeitada para não criar ciclos. | Testes de concorrência cruzada com comunidade/saída/transferência na correção 04.1. |
| COM-03 | Baixa-média | Rotas/históricos de volume elevado não possuem paginação completa e emissão muito grande sem teste de carga. | Limites operacionais/otimização na estabilização, sem ampliar escopo agora. |

**Parecer PR #4: ACEITO COM RESSALVAS**. Fluxo comercial central compatível com requisitos; não há defeito crítico comprovado nos caminhos revisados.

## PR #5 — Comunidades, chat e feed

### Pontos implementados corretamente em estrutura

1. Listagem e detalhe de **metadados públicos** sem obrigar login, conforme RF-COM-06.
2. Entrar/sair manualmente; `participante_comunidades` inativa logicamente e pode reativar.
3. Conteúdo exige conta ativa + filiação ativa + ingresso `ativo` ou `utilizado`; não depende de evento futuro, conforme RF-COM-05.
4. Organizador do evento acessa conteúdo próprio; conta de suporte não obtém permissão.
5. Postagens não têm endpoints de edição/exclusão; curtidas usam chave única de `postagem_curtidas` e incremento condicional do contador no mesmo contexto transacional.
6. Frontend conecta comunidade/chat/feed à navegação; mensagens de banco são escapadas antes de compor `innerHTML`, reduzindo risco de XSS em texto.

### Achados obrigatórios da correção

| ID | Gravidade | Achado técnico fundamentado | Correção requerida |
| --- | --- | --- | --- |
| SOC-01 | **Alta (gate de homologação)** | Apenas nove testes de `ComunidadeSocialTest` usando `fakeConnection` e repositório sobrescrito; não há integração HTTP/MySQL dos endpoints novos no `ComercioMySqlIT` ou `MySqlIntegrationIT`. Relatório `ENTREGA_04_RESULTADOS.md` diz `mvnw.cmd test` com 136 testes, **não** demonstra MySQL para o módulo 04. | Criar `ComunidadeMySqlIT` com MySQL 8.4 real no CI, usando cookies/sessões/origem HTTP, autorização 401/403, FK/CHECK, inclusão/remoção, comentários/curtidas concorrentes, pós-evento, reentrada e rollback. |
| SOC-02 | **Alta (integridade funcional)** | `ComunidadeSocialService.enviarMensagem` encaminha `idRespostaA` diretamente a `ComunidadeSocialRepositoryJdbc.enviarMensagem`. A FK `comunidade_mensagens.id_resposta_a` verifica apenas que a mensagem existe; **não** garante pertencimento à mesma comunidade. Uma resposta da comunidade A pode referenciar mensagem B de outra comunidade. | Validar `idRespostaA` existente, positiva e pertencente à **mesma** comunidade, no backend; aplicar lock/revalidação quando necessário; testes de resposta válida/ID inexistente/ID de outra comunidade. |
| SOC-03 | **Alta para concorrência de revenda/social** | A autorização do `ComunidadeSocialService.validarAcessoComunidade` lê filiação e ingresso por SELECTs simples, depois grava mensagens/posts/curtidas/comentários; não mantém lock do ingresso e/ou da filiação. Uma revenda do último ingresso pode ser confirmada **entre a checagem e a gravação**, permitindo publicação após perda de elegibilidade. A entrada também verifica ingresso sem sincronizar com a transferência. **Risco identificado por análise de interleaving; ainda não reproduzido em teste.** | Definir mesma ordem de locks que o comércio, serializar/revalidar a elegibilidade na transação de mutação e testar duas disputas reais: publicação x venda de último ingresso; entrada x venda. Garantir que o membro que perdeu o último ingresso não permaneça ativo por corrida. |
| SOC-04 | **Média (desvio funcional)** | `RF-SOC-H04` exige compartilhar link da publicação; `comunidades.js/html` não implementa botão/ação de compartilhar. | Adicionar cópia/compartilhamento de URL interna para `/comunidades.html?id=...&post=...`, validando que a abertura do post continua protegida pela elegibilidade. |
| SOC-05 | **Média (contrato divergente)** | `docs/api/COMUNIDADES_SOCIAL_HTTP_04.md` declara `descricao` até **2000** caracteres, mas a coluna `postagens.descricao` é `VARCHAR(500)`, `ComunidadeSocialService.criarPostagem` rejeita >500 e o `textarea` tem `maxlength=500`. | Corrigir documentação para 500 (se preservado schema) e adicionar teste da borda. Não aumentar coluna silenciosamente. |
| SOC-06 | **Média (fluxo de imagens incompleto)** | `RF-SOC-H01` permite postagem com texto **e/ou imagem**, mas `criarPostagem` exige descrição não vazia, mesmo quando há imagem. `imagem` é persistida sem validação/allowlist ou tratamento de upload, e a interface só publica texto. O chat também aceita `imagem` sem fluxo de upload exequível. | Preservar o requisito aprovado: especificar/implementar caminho seguro para imagem ou declarar claramente feature parcial; validar referência antes de persistir, rejeitar URL externa/local arbitrária; testar imagem isolada quando recurso estiver disponível. Não aceitar strings arbitrárias sem política. |
| SOC-07 | **Média (desempenho/consistência)** | Listagem de comunidades, mensagens e feed não tem paginação nem limites por página; busca usa `LIKE` sem tamanho máximo no backend; `membroAtivo` na view significa vínculo ativo, não necessariamente acesso a conteúdo (ingresso também necessário). | Limitar/paginar resultados no contrato evolutivo; distinguir indicador de associação de autorização efetiva no frontend. |
| SOC-08 | **Baixa (documentação e distribuição)** | Relatório do PR #5 menciona `CriadorUsuario`, `AutenticadorBase`, `EnvelopeResposta`, nomes que não correspondem às classes Java efetivas (`CadastroUsuarioCreator`, `AutenticacaoTemplate`, `HttpResponses`). Incluiu Maven Wrapper/JAR binário não coberto por evidência de origem/hash ou execução CI direta do wrapper. `ENTREGA_04_VERIFICACAO.md` recomenda root sem senha e comando Docker de configuração não apropriada para MySQL 8. | Corrigir nomes de classes; atualizar instruções para o profile MySQL 8.4 isolado, conta DML e CI já existente; verificar origem do wrapper ou removê-lo se supérfluo. |

### Não interpretar como resolvido

- CI verde do PR #5 confirma compilação e **regressão** com MySQL da suíte preexistente, mas não substitui teste de operações comunitárias novas.
- O relatório do PR #5 considera o módulo finalizado; para homologação funcional, exigir SOC-01, SOC-02 e SOC-03. SOC-04/05/06 devem ser tratados explicitamente como faltas de requisito/contrato.
- Não afirmar que foram comprovados exploits, bugs concorrentes reproduzidos ou XSS; há **riscos identificados estaticamente** e gaps verificáveis a testar.
- RF-COM-05 mantém ingresso `utilizado` apto à comunidade após o evento, mas **não** permite revenda desse ingresso.

## Parecer e sequência

1. **PR #4: aceito com ressalvas.** Não voltar a reimplementar compra/revenda.
2. **PR #5: incremento integrado, mas homologação condicionada à correção 04.1**, sobretudo testes reais MySQL e integridade de referências e concorrência.
3. Liberar `docs/prompts/CODEX_04_1_CORRECOES_COMUNIDADES.md` em branch nova `fix/04-1-comunidades-auditoria`; criar PR separado, **sem merge prévio**.
4. Depois iniciar **Entrega 05 — Suporte e Painéis**, seguida de demo automática, UML/relatório RA2, testes E2E completos e revisão final antes de 15/10.

**Nenhum código foi alterado nesta auditoria**. Documentação de auditoria e prompt são artefatos de coordenação técnica; não equivalem à correção implementada.
