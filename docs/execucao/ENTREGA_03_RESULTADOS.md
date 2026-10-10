# Entrega 03 — Compra direta e revenda de ingressos

Data: **10/10/2026 (America/Sao_Paulo)**. Branch `feat/03-compras-revendas`,
criada da `main` atualizada, base **8cf9974**. Implementação para auditoria
técnica **antes do merge**. Não equivale à entrega integral do JoinUp ou a
homologação de pagamentos/segurança de produção.

## Escopo e fontes

Lidos integralmente prompt 03 e auditoria 02 na main, requisitos v0.1 e todos os
adendos, contratos HTTP, resultados 02, plano Codex, ADR-001, classes/GoF,
diagramas GoF/eventos, schema/seed e implementações de sessão, conta, evento,
catálogo, transação, UI e testes. Adendos mais recentes prevalecem.

Referência PHP somente leitura, snapshot local
`38f022c072e02116fd5a118cbb123de13255d97e`: `comprar_ingresso.php`,
`comprar_ingresso_revenda.php`, `create_revenda_anuncio.php`, fluxos de
ingresso/revenda em `feed.js` e identidade visual `feed.css`.
Regras PHP divergentes (reativar linha antiga, entrada automática, ausência de
lock na transferência) não foram reproduzidas. Banco legado não foi acessado.

Java 21 puro, HttpServer, composição manual, JDBC, Jackson, Maven e JUnit 5.
Sem novas dependências runtime, framework, ORM, gateway ou alterações de DDL/seed.
Os quatro GoF existentes continuam executáveis e cobertos pela regressão:
Facade de eventos, Template Method de login, Factory Method de cadastro,
Singleton de configuração. Comércio usa serviços/repositórios reais, sem
introduzir quinto padrão demonstrativo ou mover regras comerciais para Facade.

## Resultado funcional e rastreabilidade

| Requisito | Implementação / evidência |
| --- | --- |
| RF-ING-H01/H02/H04, CA-08 | Compra mult categoria com preço BigDecimal sob lock; baixa disponível/incremento vendido condicionados; compra confirmada/itens/snapshots/ingresso por unidade no mesmo commit; pagamento explicitamente simulado |
| RF-ING-H03 | Meus ingressos por titularidade atual, estados ativo/utilizado/cancelado, evento/tipo/snapshots, anúncio disponível; UI agrupada por evento/tipo/status |
| RF-EVT-02/03 | Compra lê preço atualizado sob lock e preserva valores históricos; categorias vendidas continuam bloqueadas para edição manual; evento pode editar sem modificar snapshots |
| RF-ING-01, RF-USR-02/05, CA-14/18 | Evento estritamente futuro, participante/organizador ativos; checagem temporal após locks e antes de concluir; início inclusive bloqueado |
| RF-REV-H01/H02/H03, RF-REV-01/02, CA-13 | Publicar lote próprio/elegível; preço positivo exato; uma linha disponível por ingresso; cancelar sem mudar dono; reanunciar cria novo histórico; compra transfere ingresso existente e marca vendido com comprador/data |
| RF-REV-05, CA-30..35 | Todas as consultas de anúncios (lista/detalhe/histórico), publicações e compras exigem participante autenticado ativo; API direta 401/403; nenhuma conta privada nos DTOs |
| RF-REV-03/04/04A, CA-15 | Organizador inativo preserva anúncios disponíveis, indicador compravel=false; novas publicações/compras bloqueadas; cancelamento permitido; reativação restaura elegibilidade só de linha disponível |
| RF-USR-04/04A, CA-16/17 | Desativação 01 preservada e coordenada pelos locks de conta; cancela anúncios e revoga sessões atomicamente; reativação não restaura anúncio cancelado |
| RF-COM-02/03/04/05, CA-09/11/12 | Publicação preserva membro; venda do último ingresso ativo/utilizado inativa associação com data_saida; outro ingresso elegível mantém associação; comprador não é inscrito; posts/mensagens preservados |
| RF-USR-01B, histórico | Compra direta original e compras por revenda disponíveis ao comprador respectivo, inclusive após transferência posterior; detalhe do ingresso fica restrito ao titular atual; IDOR retorna 404 |
| CA-24 / transporte | Cookie HttpOnly/same-origin/CSRF existentes; JSON estrito, IDs/números/campos extras/nulls rejeitados; SQL parametrizado e erro sem detalhes JDBC |

Contratos efetivos: [COMPRAS_REVENDA_HTTP_03.md](../api/COMPRAS_REVENDA_HTTP_03.md).
Histórico de revenda usa `id_comprador`, `valor_revenda` e `data_venda` já
persistidos no anúncio. Não gera uma compra direta/ingresso adicional nem altera
compra de emissão original. Snapshots de ingresso continuam sendo da emissão;
o schema não tem snapshot separado de cada revenda.

## Transações, concorrência e ordem de locks

Todas as mutações usam uma Connection de `ExecutorTransacional`, isolamento
READ_COMMITTED, commit só após o conjunto e rollback em SQLException ou erro de
domínio. Leituras iniciais de referências **não autorizam** a ação; servem para
conhecer as contas/eventos envolvidos e adquirir os locks em ordem.

Ordem do comércio:

1. Participantes envolvidos (comprador e vendedor), por ID crescente.
2. Organizadores envolvidos, por ID crescente, em publicação/compra.
3. Sessão do ator revalidada/bloqueada, após as contas.
4. Eventos por ID crescente, com SELECT simples FOR UPDATE sem JOIN no lock.
5. Categorias por ID crescente, coordenadas com edição/estoque da Entrega 02.
6. Ingressos por ID crescente; então anúncios e, na venda, filiação.

`GerenciadorSessao.exigirParticipanteComContas` amplia a autorização existente
para bloquear o conjunto de contas **antes** da sessão. Se cada comprador fosse
bloqueado primeiro, duas compras cruzadas A/B e B/A poderiam formar um ciclo.
Após locks de ingresso/anúncio, o serviço revalida dono, referências, estado,
preço e evento. Leitura de início usa Clock UTC depois da espera, repetida antes
de retornar o resultado ao executor.

Autorização HTTP inicial (`ContaService.identificar`) termina sua transação
antes do serviço; nenhum lock dessa verificação é carregado para a unidade de
compra/revenda. Login/desativação continuam conta → sessão. Organizador de edição
usa organizador → sessão → evento → categoria; publicação/compra já bloqueia
esse organizador antes de sessão/evento. Cancelamento usa participante → sessão
→ evento → categoria → ingresso → anúncio, **sem bloquear organizador mais
tarde**, permitindo cancelamento durante sua inatividade.

Desativação de participante pula evento/ingresso e atualiza anúncios após
conta/sessão; qualquer publicação/venda do mesmo participante precisa dessa
conta primeiro, portanto serializa com cancelamento automático. Reativação por
suporte mantém suporte/sessão → conta alvo: os fluxos de comércio não adquirem
conta/sessão de suporte em etapa posterior, portanto não fecham um ciclo com
esse caminho existente. Locks compartilhados de gêneros/artistas e FKs da
Entrega 02 permanecem; o comércio não altera catálogos de artistas/gêneros.

Evento bloqueado serializa compra com edição/reagendamento; categoria bloqueada
protege preço/estoque. UPDATE de baixa exige estoque suficiente e contador
vendido sem overflow. Lote valida/bloqueia todas as categorias antes de debitar.
Revenda não muda estoque/vendidos: transfere unidade já emitida. A restrição
única gerada do MySQL protege duplicação de anúncio mesmo fora da API.

Conta de vendedor e evento bloqueados tornam a consulta de seus ingressos
elegíveis estável durante a inativação de membro. Essa regra usa ativo **ou**
utilizado e não depende de data do evento. O teste com ingresso utilizado após
evento comprova preservação do estado/vínculo ao negar operação comercial;
endpoints de conteúdo comunitário continuam para a Entrega 04.

Fallback controlado: MySQL 1205/1213 ou SQLSTATE 40001 retornam **409
CONCORRENCIA_CONFLITO**, após rollback. Não há retry automático de pagamento ou
mutação. Timeout 1205 foi provocado no banco real, inclusive depois de uma
alteração anterior, e o rollback foi comprovado. 1213 está tratado; não foi
deliberadamente induzido nos fluxos finais. Compras cruzadas executaram sem
deadlock. Isto cobre os caminhos existentes, não prova ausência de deadlock para
futuras operações administrativas ou SQL externo em ordem diferente.

## Testes executados

Windows, Temurin **21.0.12.1**, Maven **3.9.9**, MySQL Community **8.4.0**,
instância já existente isolada em 127.0.0.1:3307, banco `joinup_poo`, sessão UTC
`+00:00`. Conta DML `joinup_it`; admin separado para fixtures/triggers. A conta
de fixture foi configurada com segredo exclusivo local, fora do repositório.
Nenhum serviço global, DROP DATABASE, TRUNCATE, remoção de constraint ou banco
PHP foi utilizado. Limpeza por PKs criadas pela suíte; fixtures seed preservadas.

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA/JoinUp-POO/tools/jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3307/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_DB_USER = 'joinup_it'
$env:JOINUP_IT_ADMIN_USER = 'root'
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
# JOINUP_DB_PASSWORD e JOINUP_IT_ADMIN_PASSWORD somente no ambiente isolado.
& "$env:LOCALAPPDATA/JoinUp-POO/tools/apache-maven-3.9.9/bin/mvn.cmd" --batch-mode --no-transfer-progress -Pmysql-it clean verify
node --check src/main/resources/public/js/ingressos.js
node --check src/main/resources/public/js/eventos.js
java -jar plantuml-1.2025.2.jar -checkonly docs/diagramas/COMPRAS_REVENDA_03.puml
java -jar plantuml-1.2025.2.jar -Playout=smetana -tsvg docs/diagramas/COMPRAS_REVENDA_03.puml
git diff --check
```

Resultado: **236 testes**, **zero falhas, erros ou ignorados**:

| Suíte | Quantidade |
| --- | --- |
| Surefire — regressão + ValidacaoComercioTest | 127 (111 preservados + 16 novos) |
| Failsafe — MySqlIntegrationIT original | 46, preservados sem editar a classe |
| Failsafe — ComercioMySqlIT | 63 novos HTTP/JDBC reais |
| Total | 236 (157 existentes + 79 novos) |

Os cenários novos incluem BigDecimal mult categoria, snapshots após edição,
unidade por ingresso, contadores/estoque, preço atualizado durante espera,
limites DECIMAL/INT, tipos de outro evento, vazios/duplicados/forja/coerções,
CSRF/IDOR/perfis, conta/sessão inativa, cancelamento/reanúncio, unique real,
publicação de lote atômica, organizador inativo/reativado, histórico recebido,
comunidade com outro ingresso ativo/utilizado/cancelado, e conteúdo preservado.

Concorrência real comprovada:

- Última unidade: dois compradores → **201/409**, estoque 0, vendido 1,
  exatamente uma compra e um ingresso.
- Mesmo anúncio: dois compradores → **200/409**, exatamente uma venda e uma
  transferência, sem compra direta/ingresso adicional.
- Compras cruzadas de A/B → **200/200**, dono correto em cada unidade.
- Desativação concorrente de vendedor, comprador (direta/revenda) e organizador:
  estados finais coerentes com a operação que obteve o lock primeiro.
- Espera FOR UPDATE observada no processlist para compra, publicação e revenda:
  data muda para início durante espera, operação revalida e retorna 409.
- Timeout 1205 real → 409; alteração anterior da transação revertida.

Rollback: triggers BEFORE INSERT em compras/compra_itens/ingressos, **depois da
baixa de estoque**, e BEFORE UPDATE em ingresso/anúncio/filiação da revenda.
Comparação de todas as colunas de estoque, compras, itens, ingressos, anúncios,
membership e conteúdo antes/depois; resposta 503 sem SQL e nenhum efeito parcial.
Gaps AUTO_INCREMENT após rollback são esperados; não significam registros parciais.

Primeiras tentativas falharam por credencial DML local ausente (não por falha de
regra comercial). Após configurar a fixture, execuções completas passaram:
226 testes intermediários; 236 após ampliar cenários. Warnings de Shade e log de
erro proposital HTTP da fundação são preexistentes. Evidência final:
[entrega_03_mysql.txt](evidencias/entrega_03_mysql.txt); relatórios de execução
em target/surefire-reports, target/failsafe-reports e target/mysql-validation.

## Frontend e roteiro de dois participantes

Páginas nativas, identidade escura/roxa do PHP, navegação integrada em início,
contas e eventos. Compra por seleção de quantidades/categorias e confirmação
simulada; página de ingressos/histórico, anúncios pessoais, cancelamento/
reanúncio e listagem protegida/paginada. Dados do banco usam textContent, URLs
locais conhecidas, fetch same-origin e cookie HttpOnly. Sem token em storage.
Confirmação modal de revenda informa preço e transferência e fecha/atualiza a
lista se perder a disputa, exibindo o erro ao usuário.

Teste no navegador integrado sobre JAR e MySQL reais:

1. Login **Ana**, `ana.demo@joinup.test`, senha demo **JoinUpDemo#2026**.
2. Evento de fixture **247 — Encontro de auditoria 03**: comprar 1 Pista por
   49.90 e 1 Camarote por 89.75. Compra **152**, total **139.65**, ingressos
   **146/147**, estoque passa de 3/2 para 2/1, vendidos 1/1.
3. Meus ingressos: selecionar **146**, publicar por **74.90** (anúncio **96**),
   cancelar e reanunciar por **79.90** (novo anúncio **97**). Linha 96 continua
   cancelada, preço antigo preservado, ingresso ainda de Ana.
4. Logout de Ana; login **Bruno**, `bruno.demo@joinup.test`, mesma senha demo.
   Listagem mostra anúncio de 79.90 comprável e anúncio do organizador inativo
   bloqueado. Filtrar evento, abrir confirmação e comprar anúncio 97.
5. Meus ingressos de Bruno passa a incluir 146. Histórico recebido inclui
   anúncio 97 / 79.90 / data de venda. SQL confirma dono 2, anúncio vendido,
   comprador 2, timestamp UTC, nenhum novo ingresso/compra direta e zero membros
   automáticos na comunidade de fixture.
6. Logout: área de revendas exibe acesso de participante e nenhum anúncio.
7. Limpeza exclusivamente da fixture 247 e suas associações/ingressos/compra/
   anúncios/sessões de teste; servidor de teste encerrado. Seed original intacto.

Para repetir no banco demo: usar evento futuro do seed, ingressos/categorias
livres, mesmos dois participantes e alternar login via “Minha conta”/“Sair”.
As ações persistem; a limpeza SQL acima pertence somente ao teste de fixture,
não é um endpoint de produto. Não usar contas pessoais ou banco legado.

Capturas reais, revisadas visualmente:

- [Compra e estoque atualizado](evidencias/entrega_03_compra.png).
- [Cancelamento e novo anúncio histórico](evidencias/entrega_03_reanuncio.png).
- [Revendas comprável/bloqueada](evidencias/entrega_03_revendas.png).
- [Confirmação simulada](evidencias/entrega_03_confirmacao.png).
- [Histórico de Bruno](evidencias/entrega_03_historico.png).
- [Ingresso transferido para Bruno](evidencias/entrega_03_transferencia.png).
- [Área protegida após logout](evidencias/entrega_03_acesso.png).

Capturas no viewport padrão do navegador integrado (699px de largura, duas
colunas responsivas). Não foi executado teste em celular físico ou em toda a
matriz de breakpoints. A troca entre participantes usa logout/login, sem
compartilhar cookies no armazenamento da aplicação.

## UML, riscos e auditoria

[Fragmento de classes reais](../diagramas/COMPRAS_REVENDA_03.puml) e
[SVG renderizado](../diagramas/COMPRAS_REVENDA_03.svg), validados pelo PlantUML
1.2025.2/Smetana. Complementa os diagramas GoF/eventos existentes; UML geral e
relatório final RA2 permanecem na consolidação.

- Pagamento local simulado: nenhum gateway, cobrança, repasse bancário ou saque.
- Histórico de revenda sem snapshot próprio de evento; usa preço/data de venda
  do anúncio e snapshot original do ingresso, conforme schema existente.
- Sem chave de idempotência HTTP: não reenviar automaticamente após resposta
  perdida/timeout de rede; uma segunda compra direta válida é nova compra.
  Locks impedem estoque negativo e compra dupla de anúncio, não duplicidade de
  intenção de duas requisições diretas distintas.
- Locks de conta/organizador/evento são conservadores e reduzem paralelismo.
  Não há pool; histórico pessoal não paginado e emissão de quantidades muito
  grandes não teve teste de carga/memória. Escala exige limites operacionais e
  revisão própria, sem introduzir política comercial adicional neste sprint.
- R-02-01 N+1 da exploração, R-02-03 upload real/paridade móvel, R-02-04
  JOINUP_DEMO automático e R-02-05 UML integral mantidos para estabilização RA2.
- Comunidades/feed/social/suporte/painéis completos seguem em entregas futuras.
  Esta entrega implementa somente a consequência transacional sobre filiação.
- TLS/proxy/rate limiting/produção não homologados por execução localhost/CI.
- Nenhuma dúvida funcional nova impediu esta entrega; decisões acima são
  técnicas e os limites estão explícitos para auditoria.

CI configurado com Java 21 e container **mysql:8.4.0**, comando
`-Pmysql-it clean verify`, publicando evidências Surefire/Failsafe/SQL. O resultado
remoto e URL do PR serão registrados após publicação da branch. **Não fazer
merge antes da auditoria técnica.**
