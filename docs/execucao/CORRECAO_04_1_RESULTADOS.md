# Correção 04.1 — Auditoria e estabilização de Comunidades/Social

Data: **11/10/2026, America/Sao_Paulo**. Branch
`fix/04-1-comunidades-auditoria`, criada de `main` atualizada em
`74b732bc12823bdab9201a3ecbd926f17f8ae789`. PR separado, **sem merge**;
homologação depende da auditoria do Tech Lead.

## Escopo e decisão técnica

Lidos auditoria cruzada e prompt 04.1 na main, requisitos/adendos, contratos,
resultados 03/04, prompt 04, esquema/seed, serviços, repositórios, sessão,
executor e testes existentes. Java 21 puro, HttpServer, JDBC, Jackson e JUnit 5;
nenhuma dependência runtime, framework, ORM ou padrão GoF adicional.
Compras, revendas e autenticação **não tiveram código alterado**. DDL/seed e
as duas suítes de integração anteriores também permanecem intactos.

| Achado | Estado | Correção e evidência |
| --- | --- | --- |
| SOC-01 | Corrigido | `ComunidadeMySqlIT`: 65 casos HTTP/sessão/cookie/JDBC/MySQL 8.4 reais, incorporados automaticamente ao Failsafe do workflow existente. Regressão de 245 testes preservada. |
| SOC-02 | Corrigido | ID de resposta inteiro positivo, sem coerção/fracionário, e mensagem-mãe comparada à comunidade persistida em JDBC sob FOR SHARE. Válida → 201; inexistente/alheia → mesma resposta 404; inválida → 400; zero inserções nos casos recusados. |
| SOC-03 | Corrigido nos fluxos testados | Conta/evento/ingressos/filiação bloqueados na transação de autorização até o commit. Disputas determinísticas nas duas ordens de entrada/publicação x revenda; revenda antes de mensagem/comentário/curtir/descurtir; conta desativada durante espera; sem membro ativo após perda do último ingresso. |
| SOC-04 | Corrigido | Botão Copiar link, Clipboard API e fallback manual; query `post`, detalhe autorizado pelo backend, comparação de comunidade e destaque/foco após abrir Feed. Teste real de dois participantes e capturas abaixo. |
| SOC-05 | Corrigido | Contrato corrigido para 500; HTTP 500 caracteres aceito e 501 rejeitado, sem mudança de coluna. |
| SOC-06 | Parcial, pendente de homologação do PO | Referência de imagem diferente de null é rejeitada antes de gravar. URLs externas/file/data/caminhos arbitrários e imagem isolada testados. Upload e postagem somente-imagem **não implementados**; RF-SOC-H01/RF-COM-H02 não estão integralmente concluídos. |
| SOC-07 | Corrigido com limites operacionais | Quatro listas com LIMIT/OFFSET, padrão 50, máximo 100, página até 10000; busca até 100. UI Anterior/Próxima, campo `acessoConteudo` distinto de associação; consulta de criação por ID evita perder registros após a primeira página. |
| SOC-08 | Corrigido | Nomes reais e descrição de JdbcComunidades corrigidos; roteiro sem root sem senha/Docker inválido. JAR wrapper idêntico ao Maven Central por SHA-256; origem registrada em `.mvn/wrapper/ORIGEM.md`. |

## Integridade, autorização e locks

O caminho HTTP revalida/bloqueia a conta e a sessão com `GerenciadorSessao` na
transação do serviço. O social depois bloqueia evento, ingressos elegíveis em
ID crescente e filiação com SELECTs simples, sem JOIN. O método JDBC repete
validação/bloqueio de conta para os caminhos que recebem Connection/Usuario.
Ele não busca novas contas depois de bloquear evento. Categorias/anúncios não
são alterados pelo social e são pulados nessa ordem; comércio continua com
contas por ID → organizadores → sessão → evento → categorias → ingressos →
anúncio → filiação. Organizador social bloqueia sua própria conta/evento.

A autorização permanece válida até o commit, inclusive para entrada e saída.
Conta do vendedor/evento são recursos comuns à revenda e ao social. Assim, a
operação social que obtém os locks primeiro termina antes da venda; se a venda
obtém primeiro, o social revalida e recusa. Venda inativa a filiação e não
inscreve comprador automaticamente. Conta desativada não publica.

As leituras privadas também usam essa proteção conservadora. Participante
precisa de associação ativa e ingresso ativo/utilizado; não se exige evento
futuro nem organizador ativo para manter autorização social. Organizador dono
lê feed/comentários e envia mensagens; criação de post, comentário e curtida
continua exclusiva de participante elegível. Visitante recebe só metadados.

Resposta a mensagem verifica o `id_comunidade` da mensagem-mãe no banco,
com lock compartilhado até inserção/commit. Erros alheio/inexistente são
indistinguíveis e não incluem conteúdo de outra comunidade. IDs de autores
provêm da sessão. Curtida bloqueia postagem antes da linha da curtida; linha e
contador mudam juntos. Timeout/deadlock continua 409 após rollback no executor
existente; falhas JDBC são 503 sem SQL. Não há retry automático.

## Execução local e regressão

Temurin 21.0.12.1, Maven 3.9.9, MySQL Community **8.4.0** real em
127.0.0.1:3307, banco isolado `joinup_poo`, aplicação com conta DML `joinup_it`,
admin separado. Segredos do ambiente local não são incluídos nas evidências.

```text
mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify
node --check src/main/resources/public/js/comunidades.js
git diff --check
```

Resultado final local: **11/10/2026, 22:14:02 -03:00**, duração **1min21s**. **BUILD SUCCESS**, **310 testes**, zero falhas/erros/ignorados.

| Suíte | Casos |
| --- | ---: |
| Surefire: todas as classes anteriores, incluindo 9 unitários sociais | 136 |
| Failsafe: MySqlIntegrationIT, intacta | 46 |
| Failsafe: ComercioMySqlIT, intacta | 63 |
| Failsafe: ComunidadeMySqlIT, nova | 65 |
| Total | **310** |

Todos os nove cenários unitários sociais foram preservados; o fake recebeu
somente implementação vazia do novo método de lock. Esse fake não serve como
evidência concorrente: tal evidência vem exclusivamente da nova suíte real.
A primeira execução ampliada teve um erro na asserção do teste (nome do campo
`idParticipante` em vez de `participanteId`); foi corrigida, sem alterar o DTO de
autoria. Execuções completas posteriores passaram. Warnings preexistentes de
Shade e o log proposital de falha HTTP da fundação continuam sem falha de teste.

Evidência versionada: [correcao_04_1_mysql.txt](evidencias/correcao_04_1_mysql.txt).
Relatórios XML/texto completos e evidências SQL ficam em `target/surefire-reports`,
`target/failsafe-reports`, `target/mysql-validation`, publicados pela CI.

Casos novos incluem 401/403/404, CSRF das sete mutações, payloads/IDs/números
inválidos, busca parametrizada, autoria real, entrada/saída/reentrada, pós-evento
ativo/utilizado/cancelado, comércio pós-evento bloqueado, permissão por perfil,
500/501, imagens inseguras, limite/páginas, persistência além da primeira página,
ausência de edição/exclusão, histórico preservado após revenda e FK/CHECK reais.

Concorrência controlada: um admin mantém lock do evento, a primeira operação
HTTP inicia e sua espera é observada em `performance_schema.data_lock_waits`.
A segunda inicia; exige-se duas esperas reais antes de liberar o lock. Não há
apenas sleep presumindo a ordem. Resultados observados:

| Ordem dos commits | HTTP | Estado final |
| --- | --- | --- |
| Publicação → venda | 201/200 | Post histórico presente; ingresso de comprador; vendedor inativo |
| Venda → publicação | 200/403 | Nenhum post novo; vendedor inativo |
| Entrada → venda | 200/200 | Entrada seguida de inativação; vendedor inativo |
| Venda → entrada | 200/403 | Entrada recusada; nenhum membro indevido |
| Venda → mensagem/comentário/curtir/descurtir | 200/403 | Conteúdo/interação anterior preservado; nenhuma nova mutação |

Doze curtidas repetidas simultâneas dos dois participantes terminam em contador
2; doze PUT/DELETE mistos mantêm contador igual ao COUNT de linhas; oito
comentários concorrentes persistem com autoria; DELETE repetido termina em zero.
Triggers provocam rollback depois do INSERT/DELETE da curtida, na atualização
do contador, preservando tanto linha quanto contador. Triggers adicionais
rejeitam inserção de mensagem/post/comentário sem efeitos parciais. MySQL impõe
FK (1452), CHECK de autoria e filiação (3819). Nada de DROP DATABASE, TRUNCATE
ou FOREIGN_KEY_CHECKS=0. Limpeza social usa somente IDs exatos próprios.

## Frontend — execução real com dois participantes

Navegador integrado, servidor Java/JAR e MySQL reais. Fixture própria: comunidade
599, postagem 189, participantes `p40000000001@joinup.test` e
`p40000000002@joinup.test`. Contas de teste, senha demo, sem dados pessoais reais.
Setup por HTTP criou evento/compras/participações/post/mensagem. O roteiro
executado no navegador foi:

1. Visitante abre `comunidades.html?id=599&post=189`: metadados e aviso de
   autenticação; sem conteúdo de post/chat.
2. Primeiro participante autentica, abre o link, vê aba Feed/destaque e aciona
   Copiar link: toast confirma cópia. Clipboard de sistema não foi validado
   separadamente; fallback de prompt implementado e inspecionado, sem execução
   forçada neste navegador.
3. Logout/login do segundo participante, mesmo link autorizado: curte (0→1),
   abre comentários e publica “Segundo participante abriu o link protegido.”
4. Segundo sai da comunidade: conteúdo some do mesmo link; metadados e botão
   de entrada permanecem. Reentrada manual restaura acesso, post e curtida.
5. Capturas salvas e revisadas visualmente; servidor de fixture encerrado e
   registros próprios limpos. Revenda/concorrência foram exercitadas nos testes
   HTTP/MySQL, não repetidas como um fluxo de navegador nesta sessão.

Capturas reais:

- [Compartilhamento](evidencias/correcao_04_1_share.jpg).
- [Dois participantes, curtida e comentário](evidencias/correcao_04_1_dois_participantes.jpg).
- [Acesso bloqueado após saída](evidencias/correcao_04_1_acesso_negado.jpg).

Roteiro reproduzível ampliado: [ENTREGA_04_VERIFICACAO.md](ENTREGA_04_VERIFICACAO.md).
Sem homologação de celular físico ou matriz completa de breakpoints.

## Pendências e auditoria

- **SOC-06 parcial:** contenção de referência insegura autorizada pelo prompt
  04.1; nenhum consentimento do PO para considerar upload/imagem-only concluído
  ou retirar o requisito. Aprovação dessa pendência deve constar na homologação
  funcional; implementação futura exige contrato de mídia seguro.
- Locks conservadores serializam operações no mesmo evento; sem benchmark de
  carga, pool JDBC, paginação por cursor ou comprovação universal de ausência de
  deadlock em SQL administrativo fora da ordem documentada.
- Paginação OFFSET sem snapshot entre páginas: inserções concorrentes podem
  deslocar registros entre consultas; máximo de 10000 páginas limita exploração.
- Os quatro GoF/relacionamentos estruturais foram preservados; esta correção
  não exigiu mudança dos diagramas. UML integral/RA2 segue a consolidação prevista.
- Ressalvas COM-01..03 continuam: compra sem chave de idempotência, limites de
  escala e respeito à ordem de locks por futuros módulos. Sem reescrita de comércio.

Evidência GitHub Actions e URL do PR serão acrescentadas após a execução remota.
**Não fazer merge antes da auditoria do Tech Lead.**
