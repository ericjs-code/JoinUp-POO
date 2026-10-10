# Entrega 02 — Eventos, categorias e Facade transacional

Data: **10/10/2026 (America/Sao_Paulo)**. Branch `feat/02-eventos-facade`, criada
da `main` atualizada, base `921d352`. Incremento concluído para **auditoria do
Tech Lead antes do merge**. Não equivale à homologação integral do JoinUp.

## Fontes e escopo

Lidos integralmente prompt 02, auditoria 01, especificação funcional e todos os
adendos, ADR-001, proposta Java puro, classes/GoF, PlantUML, contratos HTTP,
plano de execução, correção/validação MySQL e schema/seed. Os adendos posteriores
prevalecem sobre pendências históricas da especificação v0.1.

Referência PHP consultada somente para campos, navegação e identidade visual:
`ericjs-code/JoinUp`, snapshot `38f022c072e02116fd5a118cbb123de13255d97e`,
`src/php/criar-evento.php`, `src/js/criarEvento.js` e CSS de organizador. Nenhuma
alteração no projeto anterior, importação de banco legado ou mudança funcional
adicional aprovada por inferência.

Java 21 puro, HttpServer/JDBC, Jackson, Maven e JUnit preservados. Sem frameworks,
ORM, DI externa, novas dependências runtime, novo schema ou quinto GoF.

## Implementação e rastreabilidade

| Requisito / aceite | Implementação e evidência |
| --- | --- |
| RF-EVT-H01/H02, CA-03 | Facade cria evento → lineup → categorias → comunidade, numa Connection; retorno de IDs no EventoView; HTTP/JDBC com duas categorias e comunidade 1:1 |
| RF-EVT-H03 | Exploração pública/paginada/filtros, detalhe, gêneros/artistas do banco e lista somente do organizador; quatro eventos demo exibidos |
| RF-EVT-01/01A, CA-51/59/66 | Conta ativa/perfil/sessão/titularidade no servidor; dono nunca vem do corpo; 401/403 em chamadas diretas |
| RF-EVT-02, CA-04/48/65 | Evento futuro com vendas permite dados gerais, lineup e nova data futura; comparação integral de compras/itens/ingressos/comunidade/conteúdo antes/depois |
| RF-EVT-03/04, CA-05/06/55/56 | Categoria vendida bloqueada por contador OU histórico em compra_itens OU ingresso, inclusive cancelado; categoria sem venda editável |
| RF-EVT-05/07, CA-07/53/54 | Nova categoria permitida em evento futuro mesmo com vendas em outras; categorias existentes preservadas |
| RF-EVT-06/07, CA-49/50/52/57/58/60 | Início persistido comparado com Clock UTC após lock e antes de concluir; 409 no início inclusive; igualdade provada unitariamente e início/passado no MySQL |
| RF-EVT-08, CA-61/62/63/64 | Data ISO com offset normalizada; destino estritamente futuro; evento iniciado não pode ser reagendado |
| Segurança / CA-24 | CSRF da Entrega 01 reutilizado em todas as mutações; participante/suporte/visitante/outro organizador/inativo recusados; payload/IDs/referências inválidos testados |
| Atomicidade / concorrência | Triggers de falha em três subsistemas, rollback de edição/lineup e espera por lock real com data concorrente; testes preservam o seed |

Classes efetivas em `br.com.joinup.eventos`:

- `EventoRoutes`: tradução HTTP/DTO, origem/CSRF, validação estrita de transporte.
- `EventoFacade`: unidade transacional e coordenação dos quatro serviços; sem SQL.
- `EventoService`: campos, gênero, dono, temporalidade e persistência do evento.
- `ArtistaService`: referências válidas, duplicidade e sincronização de associações.
- `TipoIngressoService`: dinheiro BigDecimal, estoque inteiro, histórico de vendas.
- `ComunidadeService`: criação automática e referência pública; sem membros/feed.
- Quatro `...RepositoryJdbc`: PreparedStatement e Connection recebida; nenhum abre
  conexão própria. `JdbcEventos` é somente utilitário, sem alegação GoF.
- Records `Evento`, `ItemCatalogo`, `TipoIngresso`, `Comunidade` e DTOs explícitos.

Reutilizados `ExecutorTransacional` READ_COMMITTED, sessão/autorização,
Factory Method, Template Method e Singleton. A sessão é verificada antes do
CSRF pelo adaptador e revalidada sob a transação da mutação. Locks seguem conta
→ sessão → evento → referências/categoria; IDs de artistas em ordem crescente.
Evento `FOR UPDATE` impede check-then-act separado. Leitura de detalhe usa
`FOR SHARE`; sem expor contas ou conteúdo comunitário nos DTOs públicos.

Pequenas alterações à fundação: composição manual no bootstrap e MIME SVG no
servidor de recursos classpath para arte própria, sem scripts/fontes externas.
Autenticação, router, CSRF, executor e suas regras não foram reescritos. Os testes
anteriores continuaram verdes. DDL e seed mudaram somente comentários de
metadados, corrigindo A01 e apontando a validação 01.

Contrato final e mensagens/status: [EVENTOS_HTTP_02.md](../api/EVENTOS_HTTP_02.md).
Sem DELETE de eventos/categorias. Nenhuma compra, baixa automática de estoque,
revenda, filiação, feed/chat, suporte, gateway ou upload foi introduzido.

## Decisões técnicas explicitadas

**Regras aprovadas:** edição geral antes do início, inclusive após vendas;
categorias sem qualquer venda editáveis antes do início; novas categorias
permitidas antes do início; reagendamento estritamente futuro; autoria e conta
ativa; preservação dos históricos e comunidade existente.

**Validações técnicas, conforme prompt 02:** criação inicial exige data futura
para não criar evento já impossível de administrar. Não é apresentada como nova
regra do adendo RF-EVT-08 nem aplicada retroativamente ao seed. Data é
normalizada para segundos UTC, precisão do DATETIME atual, antes da comparação:
fração que cairia em agora/passado é recusada. OffsetDateTime → Instant →
LocalDateTime UTC JDBC, sem depender de TimeZone da JVM; transporte responde UTC.

Imagem é opcional, vazia ou recurso público exato
`/assets/evento-placeholder.svg`. Nenhum upload é simulado, caminho local é
aceito ou URL remota é buscada. O frontend mostra explicitamente arte padrão e
pendência de envio de foto. Mudança no evento não renomeia/apaga a comunidade
existente nem modifica conteúdo/membros; mantém sua identidade integral.

PATCH preserva omitidos, rejeita `null` explícito e aceita imagem vazia para
limpar referência. Lineup requer artista e só muda line_up; artistas permanecem
no catálogo. O schema não guarda ordem de palco, por isso exibição usa ID.
Preço 0..999999.99, precisão exata em centavos, estoque 0..2147483647; não se
modifica quantidade_vendida pela API. Autoria/campos desconhecidos são recusados.

## Testes executados localmente

Windows, Eclipse Temurin **21.0.12.1**, Maven **3.9.9**, **MySQL Community 8.4.0**
em instância isolada loopback porta 3307 e `joinup_poo`, sessão **+00:00**.
Binários/datadir existentes da Entrega 01 reutilizados; nenhum serviço global
instalado, nenhum DROP DATABASE/importação legado/TRUNCATE/constraints removidas.

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA/JoinUp-POO/tools/jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3307/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_DB_USER = 'joinup_it'
# JOINUP_DB_PASSWORD: credencial exclusiva de fixture no ambiente, fora do Git.
$env:JOINUP_IT_ADMIN_USER = 'root'
# JOINUP_IT_ADMIN_PASSWORD configurada na instância temporária, fora do Git.
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
& "$env:LOCALAPPDATA/JoinUp-POO/tools/apache-maven-3.9.9/bin/mvn.cmd" --batch-mode --no-transfer-progress -Pmysql-it clean verify
node --check src/main/resources/public/js/eventos.js
java -jar plantuml-1.2025.2.jar -checkonly docs/diagramas/CLASSES_GOF_v0.1.puml docs/diagramas/EVENTOS_DOMINIO_02.puml
java -jar plantuml-1.2025.2.jar -Playout=smetana -tsvg docs/diagramas/CLASSES_GOF_v0.1.puml docs/diagramas/EVENTOS_DOMINIO_02.puml
git diff --check
```

Resultado final local, **17:21:52 -03:00**:

```text
Surefire: Tests run: 111, Failures: 0, Errors: 0, Skipped: 0
Failsafe/MySqlIntegrationIT: Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — Total time: 32.120 s
```

**157 testes**: os **109 anteriores preservados**, mais 25 unitários e 23
HTTP/JDBC reais. Nenhuma substituição de MySQL por mocks/H2; doubles de
repositório somente nos quatro testes unitários de coordenação, com serviços
reais e identidade da mesma Connection. Testes finais incluem registro adicional
de gênero/artista persistido, CPF ausente e consultas sem compra/emissão.

Primeira verificação do backend: 86 unitários antigos verdes. A primeira tentativa
com novos testes parou em dois erros `var` com declaração composta; corrigidos
antes da execução integral. Execução intermediária: 156 verdes (111+45); após
acrescentar teste de catálogos adicionais, execução final 157 verdes. Warnings do
Shade sobre META-INF/module-info já existiam; não impediram empacotamento/JAR.
Log GRAVE no teste antigo de falha interna HTTP é proposital; não é erro de suíte.

[Evidência SQL textual](evidencias/entrega_02_mysql.txt) registra versão,
constraints, contagens e os três rollbacks, sem senhas/hashes/tokens. Artefato
do build em `target/mysql-validation/integracao.txt`. Idempotência de schema/seed
preservou todas as linhas, timestamps e hashes; 22 tabelas, 42 FKs, 15 CHECKs,
44 índices nomeados. Suite usa conta DML; admin separado apenas para fixtures.
Limpeza remove somente PKs/associações criadas por testes e volta às contagens
do seed (4 eventos, 6 categorias, 4 comunidades, 6 associações de lineup).

**Rollback comprovado:** trigger BEFORE INSERT em line_up, tipos_ingressos e
comunidades, acionado depois de inserir evento. Em cada falha HTTP 503, contagens
de todas as etapas inalteradas. Rollback de edição recupera nome e lineup
anteriores após DELETE/INSERT de associações. Gaps de AUTO_INCREMENT após
rollback são normais; nenhuma linha parcial fica persistida.

**Concorrência comprovada:** transação admin mantém lock do evento; teste observa
requisição SQL da aplicação esperando `FOR UPDATE` no processlist. Admin muda
data para UTC_TIMESTAMP e confirma. PATCH obtém lock e retorna 409, sem alteração
de nome. Teste adicional dispara edição geral e estoque em paralelo e confirma
ambas sem perda de campos. Isso não atesta concorrência de compra ainda inexistente.

## Frontend e roteiro executado no navegador

JAR local real em 127.0.0.1:8080, navegador integrado, dados MySQL:

1. Explorar: quatro cards reais do seed, gêneros, busca “Festival Aurora” retorna
   um card; detalhes mostram artistas, categorias/estoque e referência comunitária.
2. Login de organizador demo Aurora via UI de contas; “Meus eventos” retorna
   apenas seus dois eventos; evento encerrado tem gestão bloqueada.
3. Editar Festival Aurora: Pista/Camarote com venda estão desabilitados; VIP sem
   venda permanece editável; nova categoria disponível.
4. Criar “Encontro de auditoria 02” com data 20/12/2026 20:00 local, dois artistas,
   categoria e arte padrão. Retorno e navegação para evento **37**, comunidade **29**.
5. Editar nome/lineup, atualizar categoria para **89.90 / 25**, adicionar outra
   por **150.50 / 10**. Consultar detalhe público confirma o resultado.
6. Conferência SQL: evento 37 em **2026-12-20 23:00:00 UTC**, artistas 1/2,
   categorias 56/57 com valores exatos e comunidade 29 preservada.
7. Logout e limpeza somente do evento/associações/categorias/comunidade e sessão
   do teste no banco isolado. Fixtures originais preservadas, sem endpoint DELETE.

Durante este teste foi encontrada e corrigida colisão de nomes entre inputs de
evento e categoria: HTMLFormControlsCollection devolvia RadioNodeList vazio na
criação. Reteste do fluxo completo passou após ler o primeiro input de cada campo
geral explicitamente. Não se declarou o frontend validado apenas por servir HTML.

Capturas reais:

- [Criação com IDs retornados](evidencias/entrega_02_criacao.png).
- [Gestão de categorias sem vendas](evidencias/entrega_02_gestao.png).
- [Categorias vendidas bloqueadas](evidencias/entrega_02_categorias.png).
- [Detalhe público após criação/edição](evidencias/entrega_02_detalhe.png).

UI HTML/CSS/JS nativa, responsiva por CSS, textContent para conteúdo do banco,
fetch mesma origem/cookie HttpOnly, sem token em localStorage. Origin do navegador
é validado pelo backend. Validação de layout móvel não foi executada nesta sessão;
capturas/fluxo realizados em viewport desktop. Compra/participação/feed não
são apresentados como concluídos.

## UML e auditoria

[GoF atualizado](../diagramas/CLASSES_GOF_v0.1.puml) mostra Facade implementado,
assinaturas reais, GerenciadorSessao, quatro serviços/repositórios e transação.
[Fragmento de domínio RA2](../diagramas/EVENTOS_DOMINIO_02.puml) usa somente
classes existentes, projeções e associação por IDs (sem grafo ORM/classe LineUp
inventada). Ambos passaram no checkonly PlantUML **1.2025.2** e foram renderizados
em SVG com Smetana. [GoF SVG](../diagramas/CLASSES_GOF_v0.1.svg),
[domínio SVG](../diagramas/EVENTOS_DOMINIO_02.svg). São fragmentos para compor o
diagrama/relatório integral posterior, sujeito à revisão RA2 do Tech Lead.

## Riscos, limites e próximos incrementos

- **Produção:** mantém ressalva A03: TLS/proxy reais, rate limiting e controles de
  infraestrutura ainda precisam validação. Evidências desta entrega são locais/CI.
- **Imagem:** somente arte padrão/sem imagem; multipart segue pendente. Não há
  paridade visual integral com todas as telas PHP nem validação móvel executada.
- **Escala:** lista pagina no SQL mas enriquece eventos com três consultas por
  linha; adequado à demonstração, requer medir/otimizar N+1 para catálogo grande.
  Conta/sessão e JOIN de gênero nos locks são conservadores e podem reduzir
  paralelismo. Sem pool de conexões, conforme fundação existente.
- **Temporalidade:** precisão de segundos do schema exige normalização; clientes
  não podem confiar só no indicador editavel da UI. Clock UTC do servidor é a
  autoridade; próxima compra deve reutilizar política/ordem de locks e revalidação.
- **Demonstração automática:** JOINUP_DEMO continua reservado e seed manual,
  ressalva A04 do incremento anterior; automação restrita demo é trabalho posterior.
- **Histórico:** contas/eventos do seed são preservados. Próxima entrega de compra
  precisará bloquear evento/categoria e manter snapshots, com testes próprios.
- **Escopo integral/prazo RA2:** compra/revenda/social/suporte completos e relatório
  final continuam incrementos futuros. Este PR entrega eventos/catálogo/Facade.

## GitHub / revisão

- [PR #3 — Entrega 02](https://github.com/ericjs-code/JoinUp-POO/pull/3), aberto
  para `main`, **sem merge**, na branch `feat/02-eventos-facade`.
- Base: `921d352e9c9d2dd5ad137c44894cee9b6969bf25`; implementação:
  `1a10b8fee7297f43025af6406cad757b2c0656bb`.
- [CI remoto da implementação — execução 38083736991](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38083736991):
  **success**, em 10/10/2026, job `verificar`, Java 21, container MySQL **8.4.0**.
  Executou `-Pmysql-it clean verify`: **111 Surefire + 46 Failsafe = 157 testes**,
  zero falhas, erros ou ignorados, `BUILD SUCCESS`. Publicação das evidências
  Surefire/Failsafe/SQL também concluída. Esse resultado remoto confirma a
  execução local descrita acima; não representa aprovação de auditoria.

Este registro acrescenta documentação após o commit de implementação. O estado
atual dos checks da branch pode ser acompanhado no PR. Auditoria do Tech Lead
permanece obrigatória antes de integrar; merge não foi executado.
