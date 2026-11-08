# RA3 — Entrega 02: CRUD e menu Java

Data: 08/11/2026, America/Sao_Paulo. Gate obrigatório verificado antes de
codificar: PR #13 consta integrado, merge **1dfd4d2f6a4cb1268170be274d688b2c07e0b0ee**,
e o commit revisado 78e6ab4 é ancestral da main atualizada. Esse merge é o
SHA-base efetivamente utilizado. Nova branch: `feat/ra3-02-crud-menu`.
Nenhum merge foi executado pelo Codex. O commit e o PR desta entrega constam
do histórico Git/PR associado; não reutilizam a branch da Entrega 01.

## Inspeção e limites

Foram lidos o pom, workflows RA3/RA2, ADR-002, resultados 00/01, guia consolidado,
fontes e testes RA3, os dois XMLs e schema_ra3.sql. O modelo real possui as sete
entidades esperadas e mutadores suficientes para a atualização mínima aprovada;
não foi necessário alterar entidades, DDL, conversor ou descritores.
Enunciado/anexos de aula não estavam disponíveis como arquivos acessíveis;
foram usadas as exigências e decisões transcritas pelo PO. Não se alega leitura
dos slides, PDF, RAR ou ZIP inacessíveis. Não há relatório acadêmico/UML final.

## Arquitetura implementada

- `apresentacao/MenuRa3`: loops principal/submenu, parsing e streams injetáveis.
  Campos inválidos são recuperáveis, `0` navega e EOF encerra. Sem armazenamento
  de domínio em memória ou serialização de entidades.
- `apresentacao/AplicacaoRa3`: configuração, seleção menu/--diagnostico e uma
  única fábrica por execução. O diagnóstico antigo permanece como adaptador.
  Ra3Main mantém encerramento da thread de cleanup do Connector/J.
- `servico/DadosRa3`: comandos imutáveis tipados, validados antes de transação;
  não são entidades adicionais. `OperacoesRa3` é o contrato concreto do terminal
  com o serviço JPA e permite testar o menu sem MySQL.
- `servico/ServicoCrudRa3`: casos de uso das sete classes, DTOs de saída sem hash,
  busca de pais existentes, atualização gerenciada e exclusão protegida.
- `persistencia/UnidadeTrabalhoRa3`: EM por operação, catálogo validado antes da
  transação, begin/commit, rollback em falha e fechamento. Exceções próprias não
  encadeiam detalhes do driver. A fábrica é recebida, não criada por operação.

`persist` cria, `find` consulta, JPQL com valores parametrizados lista, mutadores
em entidade gerenciada + commit atualizam, `remove` exclui. Identificadores de
entidade/associação vêm exclusivamente de enum/constants, nunca do texto do
usuário. Não há CRUD por SQL nativo, bulk DELETE/UPDATE ou reflexão sobre entrada.
O SELECT DATABASE() preexistente continua como verificação de isolamento.

## Matriz dos 28 caminhos CRUD

Todos usam `ServicoCrudRa3`. O teste parametrizado
`CrudRa3MySqlIT.quatroAcoesCrudPorEntidadeEmContextosNovos` executa cada linha
em MySQL real, com novo EM por operação e consulta independente após remoção.
Os logs contêm uma linha `RA3_CRUD_OK entidade=... acao=...` por célula: **28 provas**.

| Entidade | Criar: Cadastro | Consultar | Atualizar: Alteracao | Excluir |
| --- | --- | --- | --- | --- |
| GeneroMusical | NovoGenero → persist | consultar(GENERO,id) → find | AlterarGenero → renomear | excluir(GENERO,id,SIM) → remove |
| Organizador | NovoOrganizador → persist | consultar(ORGANIZADOR,id) | AlterarOrganizador → renomear | excluir(ORGANIZADOR,id,SIM) |
| Participante | NovoParticipante → persist | consultar(PARTICIPANTE,id) | AlterarParticipante → renomear | excluir(PARTICIPANTE,id,SIM) |
| Evento | NovoEvento → persist com pais existentes | consultar(EVENTO,id) | AlterarEvento → nome/imagem | excluir(EVENTO,id,SIM) |
| TipoIngresso | NovoTipo → persist com evento | consultar(TIPO_INGRESSO,id) | AlterarTipo → nome/valor/quantidades | excluir(TIPO_INGRESSO,id,SIM) |
| Comunidade | NovaComunidade → persist com evento exclusivo | consultar(COMUNIDADE,id) | AlterarComunidade → nome/descrição/imagem | excluir(COMUNIDADE,id,SIM) |
| Postagem | NovaPostagem → persist com autor/comunidade | consultar(POSTAGEM,id) | AlterarPostagem → descrição/imagem | excluir(POSTAGEM,id,SIM) |

Os sete submenus oferecem ainda Listar, com 20 registros por página em ordem
de ID e indicação do limite. Os testes de menu percorrem os cinco caminhos
de cada entidade, incluindo cancelamento e confirmação de exclusão.

## Exclusão e concorrência

Escolha didática aprovada: remoção física de folhas por remove, com texto `SIM`.
Não se apresenta exclusão física como obrigação do enunciado. Ativo continua
sendo atributo do modelo, sem confundir desativação com exclusão. Nenhuma coluna
`excluido`, oitava entidade ou tabela adicional foi criada.

Antes de remover, o serviço obtém lock PESSIMISTIC_WRITE do registro e consulta
dependentes reais: gênero/organizador→eventos, participante→postagens,
evento→tipos ou comunidade, comunidade→postagens. Com vínculo, lança falha própria
e faz rollback sem remover nada. A checagem é essencial nas FKs SQL CASCADE
comunidade→evento e postagem→comunidade, que continuam no DDL histórico.

Testes específicos cobrem todos os cinco tipos de pai, evento com tipo e as
duas disputas concorrentes:

1. Uma comunidade é inserida e mantém sua transação aberta; a remoção do evento
   aguarda. Após commit do filho, o serviço encontra o vínculo e recusa remover.
   Consultas JDBC confirmam pai e filho preservados, sem CASCADE silencioso.
2. Uma transação lê o evento e altera/flush um gênero de controle independente;
   outra exclui o evento ainda folha. O INSERT de tipo com a referência antiga
   falha realmente com MySQL **1452**. A unidade de trabalho traduz para VINCULO
   e reverte também a alteração anterior do controle, confirmada em outro EM.

O teste de rollback adicional provoca falha após flush de atualização e comprova
restauração. Duplicidade é traduzida por 1062; FKs por 1451/1452/1048 e disputas
por 1213/1205. Mensagens do terminal não publicam SQL, stacktrace JDBC, URL ou
segredos. A próxima tentativa usa EM novo, sem coleção inversa obsoleta.

## Sessão CLI real executada

A transcrição está em [evidencias/sessao-cli-02.txt](evidencias/sessao-cli-02.txt).
Foi executado o processo Maven real com stdin/stdout, sem substituir o serviço
por fake: `mvn -Pra3 compile exec:java`. Um condutor local externo ao produto
enviou as opções e leu os IDs devolvidos, sem gravar credenciais na transcrição.
As respostas extraídas são reais; menus repetidos e logs Maven de rotina foram
omitidos na versão compacta. Dados utilizados são exclusivamente fictícios.

| Registro | ID observado |
| --- | --- |
| Gênero CLI02 | 52 |
| Organizador CLI02 | 29 |
| Evento CLI02 | 25 |
| Participante CLI02 | 19 |
| Tipo CLI02 | 16 |
| Comunidade CLI02 | 20 |
| Postagem CLI02 | 17 |

O roteiro cadastrou os sete, consultou as associações, alterou o evento para
`CLI02 Evento Atualizado`/`cli02.png`, reconsultou, tentou excluir o evento com
filhos (recusado), excluiu a postagem com SIM e confirmou ID ausente. Encerrou
o processo e abriu outra JVM/fábrica: evento atualizado, gênero e organizador
continuaram recuperáveis. Executou --diagnostico sem interação. Somente depois
da prova de reinício, removeu pelo menu os outros seis registros próprios, na
ordem tipo/comunidade/evento/participante/organizador/gênero, todos com SIM.
Não foi usado reset, DELETE geral ou limpeza de registros alheios.

## Comandos e resultados

Ambiente local: JDK 21.0.12.1, Maven 3.9.9, MySQL 8.4.0, Windows 11. JAVA_HOME
foi configurado por comando; não foi utilizado o Java 25 do PATH. MySQL novo
em diretório temporário exclusivo, loopback **58274**, schema aplicado manualmente
antes da suíte; conta DML somente em joinup_poo_ra3. Nenhum SQL local foi enviado
ao banco original joinup_poo. O banco de demonstração com dados úteis não foi usado.

| Comando / inspeção | Resultado local |
| --- | --- |
| git fetch origin main; consulta PR #13; merge-base --is-ancestor | Gate cumprido, base registrada acima. |
| mvn clean test | 167 testes originais, zero falhas/erros/skips. |
| mvn clean verify | 167 testes e JAR web original; sucesso. |
| Inspeção do JAR/dependency:tree padrão | Ausência de RA3/JPA, descriptors e EclipseLink no artefato padrão. |
| mvn -Pra3 clean test | 243 testes (167 originais + 76 RA3), sem exigir MySQL. |
| mvn '-Pra3,ra3-it' verify | 243 Surefire + 39 Failsafe; zero falhas/erros/skips na execução final. |
| mvn -Pra3 compile exec:java | Menu real, sete cadastros e roteiro completo; exit 0. |
| mvn -Pra3 exec:java, nova JVM | Persistência dos registros mantidos/atualizados confirmada; exit 0. |
| mvn -Pra3 exec:java -Dexec.args=--diagnostico | RA3_CONEXAO_OK, sem aguardar entrada ou inserir; exit 0. |
| Diagnóstico com porta indisponível / schema antigo | Falhas esperadas, exit 1 em ambos; sem URL/usuário/senha sentinela nos logs. |
| Consulta SQL independente final | Sete tabelas, sem fixtures restantes. |
| mysqladmin shutdown da instância descartável | Instância encerrada e arquivo temporário de credencial removido. |
| git diff --check | Sem erro de whitespace. |

Os 39 testes integrados incluem os **14 anteriores** (gênero, relacionamentos e
schema), mais **25** de CRUD/integridade/paginação/rollback/concorrência.
Os unitários novos cobrem menus, parsing, EOF, confirmação, modo diagnóstico,
fábrica única/fechada e sanitização. O serviço foi testado no banco; os doubles
existem apenas nos testes sem MySQL do terminal.

Trechos extraídos e hashes dos logs: [evidencias/validacao-02.txt](evidencias/validacao-02.txt).
O workflow RA3 aplica o schema administrativamente, roda as suítes e chama
explicitamente --diagnostico. O workflow RA2 permanece intacto. Resultados de CI
deste PR devem ser consultados nos checks; não são inferidos dos números locais.

## Configuração preservada e riscos

Java 21, EclipseLink 4.0.4, API Jakarta Persistence 3.1.0 única, Connector/J 8.4.0,
sete @Entity e sete entradas no persistence.xml. ORM XML, conversor UTC,
weaving=false, cache NONE e DDL automático desabilitado permanecem inalterados.
Nascimento continua CHAR(14)/LocalDate; DATETIME usa o conversor UTC existente.
Não foram adicionadas dependências ou alterado o pom/schema nesta entrega.

Campos `senha` dos cadastros do terminal recebem SEM_LOGIN_RA3_DEMO, marcador
didático sem autenticação, jamais solicitado como senha pessoal ou exibido.
CPF/CNPJ/e-mails devem ser fictícios; não há validação de identidade real,
login, segurança de produção ou integração web. O módulo não é uma aplicação
de autenticação. Valores/strings/quantidades são validados antes da transação;
o MySQL continua como garantia final de integridade.

EAGER serve ao grafo acadêmico pequeno, sem promessa de desempenho em grande
volume. Paginação por offset pode mudar sob inserções/exclusões concorrentes.
Locks podem produzir espera/deadlock; o serviço faz rollback e pede nova consulta,
sem retry automático ou exclusão em lote. CREATE IF NOT EXISTS não migra schema
incompatível; mantenha inspeção administrativa e testes de metadata.
Uma interrupção abrupta pode deixar fixture r2-/CLI02; não existe reset automático.

Não há relatório acadêmico final, reflexões inventadas, UML final, deploy,
autenticação, compra/revenda, nova tabela ou mudança em regras/GoF da RA2.
Documentação primária de apoio: [JPA 3.1](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)
e [locks de FK InnoDB](https://dev.mysql.com/doc/refman/8.4/en/innodb-locks-set.html).

Preservação: classes/rotas/repositórios JDBC, SQLs/seed históricos, .env,
frontend, workflow java.yml e banco original da RA2 não foram alterados.
O checkout original do usuário conservou suas alterações locais anteriores.
O repositório PHP JoinUp permaneceu intocado.

Parada: concluir somente Entrega 02, abrir PR para main, **não fazer merge** e
aguardar auditoria do Tech Lead/PO antes de iniciar Entrega 03.
