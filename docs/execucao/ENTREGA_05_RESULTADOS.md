# Entrega 05 — Suporte e painéis: resultados para auditoria

> Data: **11/10/2026**, America/Sao_Paulo.
> Branch: `feat/05-suporte-paineis`, criada da `main` atualizada em
> **cdf4207**, após merge da correção 04.1 e auditoria do PR #6.
> Situação: implementação e verificação local concluídas; **aguarda auditoria
> do Tech Lead e aceite antes de qualquer merge**. PR #7 aberto e CI confirmado
> conforme registro abaixo.

## Escopo entregue

- Todas as dez rotas de chamados do Prompt 05, com sessão/conta ativa, perfil,
  titularidade, CSRF, campos válidos, IDs positivos e paginação.
- Criação de ticket + primeira solicitação na mesma transação. Exatamente um
  vínculo com participante ou organizador, definido pela sessão.
- Matriz RF-SUP-04 sem mudança funcional: Resolver/Retornar somente aguardando
  usuário, Retornar exige mensagem, suporte responde sem depender da associação,
  suporte encerra somente aberto/em atendimento, fechado é terminal.
- `ticket_mensagens` preserva autoria e nome do participante/organizador/suporte
  que realmente enviou cada mensagem. Responsável associado não substitui autor.
- Revalidação do estado e dono sob `SELECT ... FOR UPDATE`. Estado, snapshots
  e mensagem compartilham Connection/commit/rollback. Encerramento sem mensagem
  não inventa resposta.
- Três dashboards autenticados, com agregações reais, isolamento de usuário e
  organizador, valores históricos de compras confirmadas, ingresso atual,
  comunidades efetivamente acessíveis, tickets por status e volume de suporte.
- `/painel.html` e aba Chamados: cards, filtros, lista, abertura, detalhe/chat,
  paginação de mensagens e ações conforme papel/estado. Navegação integrada em
  início, contas, eventos, ingressos e comunidades; suporte acessa cadastro e
  reativação da Entrega 01 pela mesma interface já existente.
- [Contrato HTTP](../api/SUPORTE_PAINEIS_HTTP_05.md),
  [PlantUML](../diagramas/SUPORTE_PAINEIS_05.puml) e
  [SVG renderizado](../diagramas/SUPORTE_PAINEIS_05.svg).

Sem alteração de schema/seed ou das classes dos quatro GoF. Alteração Java em
código anterior limita-se à composição das novas rotas no bootstrap.
`EventoFacade`, `AutenticacaoTemplate`, `CadastroUsuarioCreator` e
`ConfiguracaoSistema` continuam usados nos fluxos originais e testados.
Não se introduz quinto GoF, framework, ORM, Servlet ou integração externa.

## Decisões técnicas rastreáveis

1. **Associação:** referência PHP local somente leitura,
   `src/php/tickets.php` da revisão `38f022c072e02116fd5a118cbb123de13255d97e`.
   Autoassocia suporte autenticado, inclusive quando fechado; não modifica estado
   nem histórico. Outros suportes conservam as mesmas permissões de atendimento.
   Não aceitar ID de terceiro no payload. Detalhes no contrato.
2. **Mensagens:** até 500 caracteres Unicode para compatibilidade com snapshots
   de ticket e UI original. Campos legados recebem apenas o último texto; toda
   mensagem nova conserva autoria independente na tabela de histórico.
3. **Locks:** conta → sessão → ticket, seguindo a infraestrutura existente.
   Atendimento não bloqueia a conta de outro solicitante ou suporte associado.
   Detalhe também bloqueia ticket para ler estado/histórico coerentemente.
4. **Indicadores:** 3 consultas de domínio no participante/organizador e 2 no
   suporte, sem N+1 no Java; agregações independentes evitam multiplicação por
   joins. Snapshot REPEATABLE_READ para consultas do painel e referência UTC
   única para eventos futuros/iniciados. Listas limitadas a 20 eventos; totais
   sem esse limite.
5. **Valores:** compras confirmadas e itens históricos são as fontes de vendas.
   Revenda transfere ingresso atual, sem transferir compra original ou duplicar
   a receita histórica do organizador. Não representar esses valores como
   dinheiro recebido, lucro ou saldo de saque.
6. **Privacidade/UI:** painéis não incluem CPF/CNPJ/e-mail/hash/tokens ou textos
   privados. Conversas ficam exclusivamente ao titular e suporte. Renderização
   de dados via `textContent`, sem inserir conteúdo de mensagem como HTML.
7. **Conflitos:** UI busca estado atual em 409 e não repete mutação. Suporte não
   recebe permissão de participante, e conta de suporte não ganha desativação.

## Verificação real executada

Ambiente: JDK **21.0.12.1**, Maven **3.9.9**, MySQL Community **8.4.0** local em
127.0.0.1:3307, `joinup_poo` isolado. Aplicação com `joinup_it`, restrita a
SELECT/INSERT/UPDATE/DELETE; administrador de fixtures somente nos testes,
inclusive triggers e leitura de `performance_schema.data_lock_waits`.
Nenhum acesso ao banco antigo `joinup`, importação de dados reais ou mudança
de serviço global. Credenciais de fixture permaneceram fora do Git/logs.

Comando Maven real, executado pelo binário local 3.9.9 com JAVA_HOME no JDK 21:

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA/JoinUp-POO/tools/jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3307/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_DB_USER = 'joinup_it'
$env:JOINUP_IT_ADMIN_USER = 'root'
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
# Senhas exclusivas da fixture configuradas no ambiente, sem imprimir.
& "$env:LOCALAPPDATA/JoinUp-POO/tools/apache-maven-3.9.9/bin/mvn.cmd" --batch-mode --no-transfer-progress -Pmysql-it clean verify
node --check src/main/resources/public/js/paineis.js
java -jar plantuml-1.2025.2.jar -checkonly docs/diagramas/SUPORTE_PAINEIS_05.puml
java -jar plantuml-1.2025.2.jar -Playout=smetana -tsvg docs/diagramas/SUPORTE_PAINEIS_05.puml
git diff --check
```

Resultado final local: **BUILD SUCCESS**, **22:51:39 -03:00**, duração 2m05s.

| Suíte | Testes | Falhas / erros / ignorados |
| --- | ---: | --- |
| Surefire anterior | 136 | 0 / 0 / 0 |
| `StatusTicketTest` novo | 13 | 0 / 0 / 0 |
| `MySqlIntegrationIT` preservado | 46 | 0 / 0 / 0 |
| `ComercioMySqlIT` preservado | 63 | 0 / 0 / 0 |
| `ComunidadeMySqlIT` preservado | 65 | 0 / 0 / 0 |
| `SuporteMySqlIT` novo | 63 | 0 / 0 / 0 |
| `PaineisMySqlIT` novo | 11 | 0 / 0 / 0 |
| **Total** | **397** | **0 / 0 / 0** |

**310 testes anteriores preservados + 87 novos**. Testes novos usam HTTP,
cookies, CSRF, servidor real e JDBC; não usam mocks para provar integrações.
Fixture `MySql05Fixture` cria suas próprias contas/recursos, limpa as próprias
PKs e inicializa schema/seed idempotente para também executar isoladamente.

Evidência textual do build:
[entrega_05_mysql.txt](evidencias/entrega_05_mysql.txt).
Relatórios completos gerados em `target/surefire-reports` e
`target/failsafe-reports`; CI publica esses mesmos artefatos.

### Falhas observadas durante desenvolvimento, sem omissão

- Primeira compilação dos novos testes rejeitou `var` em declaração múltipla;
  corrigida em `PaineisMySqlIT`.
- Primeira integração apontou 3 cenários IDOR com expectativa 404, mas payload
  inválido na ação Resolver (campo `mensagem` indevido) retornava 400. O teste
  foi corrigido para enviar `{}` e exercitar de fato a titularidade, sem relaxar
  a validação do backend.
- Outra execução coincidiu com login de navegador no banco de teste e falhou
  na pré-condição antiga `sessoes == 0`. A compilação daquele ciclo ainda
  continha a versão anterior do teste IDOR. Sanidade e integração foram
  separadas; dados específicos do roteiro e suas sessões removidos, recuperando
  os 3 tickets do seed e zero sessões antes do build final. Não mudar nem
  desabilitar os testes anteriores para acomodar a interferência.
- Primeira repetição de `clean` encontrou o JAR ainda usado pelo servidor de
  sanidade no Windows. Servidor encerrado e comando repetido integralmente.
- Sintaxe inicial de declarações em uma linha no PlantUML foi rejeitada;
  fragmento corrigido e novamente validado/renderizado, exit code 0.
- Warnings de recursos sobrepostos no Maven Shade permanecem os já conhecidos
  de empacotamento; não houve falha no build final.

## Matriz de aceite e segurança comprovadas

| Regra / cenário | Evidência executada |
| --- | --- |
| CA-19 / RF-SUP-02B | Suporte B responde ao chamado associado a A; IDs individuais em mensagens e associação preservada; resposta adicional de A |
| CA-20 / RF-SUP-03 | Encerramento de suporte aguardando usuário retorna 409; estado/histórico intactos |
| CA-21 / RF-SUP-01 | Fechamento pelo titular; novas mensagens, resolução e reabertura rejeitadas; sem exclusão/edição |
| CA-41..46 / RF-SUP-04 | 16 combinações ação/estado executadas por HTTP para **participante e organizador**; vazio/whitespace e estado indevido rejeitados |
| Abertura atômica | Ticket aberto com vínculo único + primeira mensagem/autoria; trigger falha depois de inserir ticket e reverte ambos |
| Rollback de resposta/retorno | Trigger de INSERT em mensagem depois de UPDATE de ticket; restaura status, resposta/retorno e contagem do histórico |
| Permissões e IDOR | Visitante 401, perfil errado 403, titular alheio/inexistente 404 indistinguível; ambas as rotas de solicitante e todas as rotas de suporte |
| CSRF | As seis mutações testadas sem origem, origem maliciosa e origem null, todas 403 sem efeito |
| Payload/IDs | Campos extras/autoria falsa, JSON duplicado/null/array/malformado, escalar indevido, texto vazio, overflow e bordas 100/500/Unicode |
| Contas inativas | Leitura/mutações negadas; inativação durante espera de lock impede retorno; suporte conserva acesso ao histórico |
| Filtros/páginas | Status parametrizado, tamanho/página limitados, total por proprietário, mensagens na segunda página e ordem por ID |
| Painéis por perfil | Visitante/sessão inválida 401, perfil distinto 403, inativo 403, nenhuma informação pessoal/segredo no payload |
| Isolamento A/B | Eventos/vendas/comunidades/tickets do organizador A separados de B; contagem não multiplicada por categorias |
| Indicadores pós-venda | Compra direta, venda por revenda, titularidade atual e compra original separadas; preço atual alterado não muda total histórico |
| Acesso comunitário real | Ativo/utilizado + filiação, pós-evento, cancelado/saída e organizador inativo; não contar acesso apenas por vínculo |
| Métricas suporte | Volume por autor real diferente da associação; contagens por tipo, UTC, média de resolução e percentual fechado conferidos no SQL |

### Concorrência observada no MySQL

Testes bloqueiam o ticket por uma Connection administrativa e aguardam evidência
em `performance_schema.data_lock_waits` antes de liberar os workers HTTP.
Não são apenas testes sequenciais ou `sleep` para presumir concorrência.

- **Dois suportes respondem:** duas esperas reais, ambos 200; duas respostas
  persistidas, autores A/B distintos, total de 3 mensagens e aguardando usuário.
- **Resolver primeiro versus Retornar:** primeiro 200 e segundo 409; fechado e
  nenhuma mensagem de retorno. **Retornar primeiro versus Resolver:** primeiro
  200 e segundo 409; em atendimento e exatamente uma mensagem nova. Duas
  sessões do mesmo titular também demonstram a serialização de conta/sessão.
- **Resposta versus encerramento:** as duas ordens, 200/409; nunca fechar a
  partir de aguardando usuário nem inserir resposta após fechado.
- **Resolução antes de resposta:** 200/409, fechado terminal.
- **Retorno antes de resposta:** ambas válidas sequencialmente, 200/200;
  3 mensagens e aguardando usuário, com histórico completo.

Os testes comprovam esses interleavings, não ausência universal de deadlock
em SQL futuro. Executor existente converte timeout/deadlock em conflito com
rollback, sem retry automático.

## Sanidade de navegador local

Navegador in-app, servidor Java 21/JAR com MySQL real, contas exclusivamente
fictícias do seed. Resultado observado em 11/10/2026 entre 22:46 e 22:48 -03:00:

1. Visitante recebeu aviso de sessão inválida e link para login.
2. Ana: painel exibiu 2 ingressos atuais, 1 compra/R$200 e 1 comunidade acessível;
   abriu chamado **#124** intitulado “Sanidade Entrega 05 — navegador”. Aberto
   mostrava somente histórico, sem Resolver/Retornar.
3. Suporte 1: associou-se; estado permaneceu aberto. Suporte 2: respondeu com
   autoria **suporte #2**, responsabilidade continuou **suporte #1**. Estado
   aguardando usuário e botão de encerramento ausente.
4. Ana retornou: em atendimento e ações Resolver/Retornar ausentes. Suporte 2
   respondeu novamente, Ana resolveu: fechado, timestamp e **4 mensagens
   preservadas**, sem campo para nova mensagem ou reabertura.
5. Suporte: painel refletiu respostas e estados reais. Organizador Aurora:
   2 eventos, 4 categorias, 4 ingressos vendidos, 3 compras/R$440; lista separou
   Festival Aurora R$380 e evento iniciado R$60.
6. Console do navegador sem erros/warnings ao fim do roteiro. Capturas salvas
   abaixo e inspecionadas; ações e nomes exibidos correspondem aos dados reais.
   Logout ao concluir; servidor de sanidade encerrado e fixture restaurada
   antes da execução final dos 397 testes.

- [Autoria suporte 2 / associação suporte 1](evidencias/entrega_05_autoria.png).
- [Resolver/Retornar do solicitante](evidencias/entrega_05_solicitante.png).
- [Chamado fechado](evidencias/entrega_05_fechado.png).
- [Indicadores suporte](evidencias/entrega_05_suporte_painel.png).
- [Indicadores organizador](evidencias/entrega_05_organizador.png).

## CI e processo

O workflow existente continua usando Java 21, service container `mysql:8.4.0`
e `mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify`, sem
alterações de workflow ou seleção/exclusão de suítes.
[PR #7](https://github.com/ericjs-code/JoinUp-POO/pull/7) aberto contra `main`,
**não mesclado**, aguardando revisão do Tech Lead.

**CI remoto confirmado: completed / success.**
[Run 38193962732](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38193962732),
evento pull_request, SHA `bb926ca1602f57d37079112dd492c3d5c42ef996`, job `verificar`
`114632476996`. A consulta à API e aos logs confirmou Java 21/MySQL 8.4.0,
**149 Surefire + 248 Failsafe = 397 testes**, zero falhas/erros/ignorados,
BUILD SUCCESS às **23:02:15 -03:00 de 11/10/2026**. Inclui 65 testes sociais,
63 de suporte e 11 de painéis. Publicação das evidências também concluiu com
sucesso. Trechos verificáveis:
[entrega_05_ci.txt](evidencias/entrega_05_ci.txt).

O commit posterior a esse run registra esta evidência e normaliza apenas
espaços finais do log local; não muda código, testes ou workflow. O CI desse
commit documental será novamente acompanhado, sem fazer merge.

## Riscos, limites e pendências

- **SOC-06 permanece pendente:** imagens/upload/post somente-imagem do módulo
  social não fazem parte desta entrega. Não declarar o produto integral pronto.
- `JOINUP_DEMO` automático/BD-02, política segura de imagens, UML geral e
  relatório/apresentação RA2 continuam para consolidação; seed manual preservado.
- Base de conhecimento, preferências, saques, push e notificações externas são
  informativos/simulados. Não existem integrações financeiras ou de mensagens.
- Paginação OFFSET não fixa snapshot entre páginas; eventos do painel limitados
  a 20. Sem benchmark de carga/cursor, validação móvel real, leitor de tela,
  proxy/TLS em produção ou testes de grande volume.
- Detalhe mantém lock conservador e pode aguardar outro atendimento. Painéis
  evitam N+1 no Java, mas subconsultas agregadas ainda dependem de volume/índices.
- Associação e encerramento sem texto não criam uma trilha de auditoria de
  operador/estado separada. Não inferir autor de fechamento do responsável
  associado nem do último autor de mensagem; nova auditoria exigiria evolução
  de schema/contrato. Autoria das **mensagens** é preservada integralmente.
- Sem idempotency key para abertura; UI impede cliques simultâneos, mas repetir
  POST pode criar outro chamado válido. Não há retry automático de mutação.
- Nenhum requisito funcional novo foi delegado ao PO nem redefinido; associação
  foi conservada do PHP e seus detalhes foram documentados. Merge fica reservado
  à revisão técnica solicitada pelo usuário.
