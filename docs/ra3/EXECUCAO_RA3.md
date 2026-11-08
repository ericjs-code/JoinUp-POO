# RA3 — Guia consolidado até a Entrega 02

Pré-requisitos: JDK 21 (`JAVA_HOME`), Maven 3.9.x, MySQL 8.0.16+ (homologação
com 8.4.0), `mvn` e `mysql` no PATH ou executados pelos caminhos absolutos.
Parta da raiz do checkout da branch `feat/ra3-02-crud-menu`.
A aplicação original continua em `br.com.joinup.Main`, sem dependências ORM.
O RA3 mantém EclipseLink 4.0.4, uma API Jakarta Persistence 3.1.0 e driver 8.4.0.

## Preparação manual das sete tabelas

Escolha uma instância **exclusiva do RA3**; para os testes, use instância
**descartável** sem dados definitivos, separada da RA2. Nos exemplos, porta 3309.
Não aponte estes procedimentos ao banco `joinup_poo` nem aplique seed/migração RA2.

```powershell
mysql --host=127.0.0.1 --port=3309 --user=administrador -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/ra3/schema_ra3.sql"
mysql --host=127.0.0.1 --port=3309 --user=administrador -p --default-character-set=utf8mb4 --execute="source src/main/resources/db/ra3/verificar_schema_ra3.sql"
```

Confira erros e warnings de SOURCE. O SQL cria somente `joinup_poo_ra3` e, em
ordem de dependências: organizadores, generos_musicais, participantes, eventos,
tipos_ingressos, comunidades e postagens. Não contém DROP, TRUNCATE, reset, seed
ou alteração de tabelas existentes. A tabela de gênero anterior é preservada.

`CREATE TABLE IF NOT EXISTS` não atualiza uma tabela incompatível. A inspeção
somente leitura mostra versão do MySQL, tabelas, colunas/tipos/defaults, índices,
FKs e CHECKs. Não há uma oitava tabela de versão: o contrato é conferido por
metadados. Se houver divergência, pare e avalie a instalação; não faça reset
automático. Na instância descartável, o teste somente leitura abaixo compara
os metadados instalados com os blocos de referência do schema original:

```powershell
# Após configurar o ambiente conforme a seção seguinte:
mvn '-Pra3,ra3-it' '-Dit.test=SchemaRa3MySqlIT' verify
```

O teste do grafo também exige essa verificação antes de persistir fixtures.
Configure uma conta dedicada com somente SELECT/INSERT/UPDATE/DELETE no RA3;
crie a conta e escolha a senha em sessão administrativa fora do repositório.

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON joinup_poo_ra3.* TO 'joinup_ra3_app'@'127.0.0.1';
```

## Configuração no PowerShell

```powershell
$env:JOINUP_RA3_JDBC_URL = 'jdbc:mysql://127.0.0.1:3309/joinup_poo_ra3?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_RA3_DB_USER = 'joinup_ra3_app'
$ra3Segredo = Read-Host 'Senha do usuário RA3' -AsSecureString
$ra3Credencial = [System.Net.NetworkCredential]::new('', $ra3Segredo)
$env:JOINUP_RA3_DB_PASSWORD = $ra3Credencial.Password
Remove-Variable ra3Credencial,ra3Segredo
```

As três variáveis são obrigatórias, sem fallback RA2; `.env` não é carregado.
Nunca inclua senha na URL, no XML, no Git, em argumento Maven ou em print.
Somente para fixture em loopback sem TLS, se necessário, acrescente
`&useSSL=false&allowPublicKeyRetrieval=true` à URL.

A validação aceita URL MySQL simples com schema exatamente `joinup_poo_ra3`,
host DNS/IPv4 e porta opcional. Os parâmetros permitidos continuam restritos:
timeouts de 1–99999, timezone UTC, forceConnectionTimeZoneToSession=true,
characterEncoding=UTF-8/utf8 e booleanos useSSL/allowPublicKeyRetrieval.
Não aceita credenciais na URL, multi-host, IPv6 ou parâmetros que redirecionem
o schema/transformem propriedades. `SELECT DATABASE()` confirma o destino real;
todas as tabelas JPA também são qualificadas com o catálogo RA3.

## Compilação, diagnóstico e integração opt-in

```powershell
java -version
mvn -version
mvn clean test                    # Unitários originais; sem JPA
mvn clean verify                  # JAR web original
mvn -Pra3 clean test              # Inclui as sete classes; sem exigir MySQL
mvn -Pra3 compile exec:java       # Menu interativo real; não inicia HTTP
mvn -Pra3 exec:java '-Dexec.args=--diagnostico' # Conecta e termina sem ler entrada

# Somente na instância descartável, com schema previamente aplicado:
$env:JOINUP_RA3_IT_DATABASE = 'instancia-descartavel-ra3'
mvn '-Pra3,ra3-it' verify
```

`exec:java` não compila automaticamente: após checkout limpo, use `compile`
ou `clean test` antes. Sem argumentos abre o menu. Com `--diagnostico`, não
insere fixtures, não abre menu e não lê entrada; use esse modo na CI.
As saídas `RA3_CONEXAO_OK`, `RA3_CONFIGURACAO_INVALIDA` e
`RA3_CONEXAO_FALHOU` são sanitizadas; falhas fazem o comando Maven falhar.
Não há carga automática de demonstração em nenhum dos dois modos.

Sem `-Pra3`, o compilador/recursos excluem RA3 e o JAR padrão permanece sem
classes ORM/descriptor/API EclipseLink. RA3 usa `target/ra3`, separado das classes
web. Não combine o perfil histórico `mysql-it` com `ra3-it`: a suíte RA2 tem
workflow próprio, sem duplicação no CI RA3.

Os testes de integração exigem confirmação textual e MySQL disponível; não
convertem ausência de banco em teste ignorado. Abrangem o smoke test de gênero
da Entrega 00, schema instalado, grafo de sete entidades, leitura em outro
EntityManager, JOIN por conexão JDBC independente, multiplicidades, UNIQUEs,
NOT NULL, FKs, CHECKs, rollback e limpeza apenas dos marcadores UUID próprios.
Incluem ainda os 28 caminhos CRUD do serviço real, paginação, IDs ausentes,
exclusões protegidas e duas disputas concorrentes de FK/remoção.
O teste de limpeza verifica que uma fixture de controle fora do marcador é
preservada e depois remove especificamente esse controle que ele próprio criou.
Uma fábrica é reutilizada por classe de integração; cada operação fecha seu EM.

Relatórios: `target/ra3/surefire-reports` e `target/ra3/failsafe-reports`.
O [workflow RA3](../../.github/workflows/ra3.yml) aplica sete tabelas manualmente
em MySQL de CI dedicado e publica os relatórios. A aplicação nunca executa DDL.
Interrupção abrupta pode deixar registros com prefixo `r1-`, `r2-` ou `ra3-it-`; não
há exclusão geral ou reset automático. AUTO_INCREMENT avança durante os testes.

## Contratos de tipo e carregamento

`Participante` armazena nascimento como String ISO `yyyy-MM-dd` no `CHAR(14)`
original e expõe `LocalDate` por conversão controlada; não há mudança para DATE.
`Evento.estado` é String Java validada contra as 27 UFs; o DDL continua ENUM.
Valores monetários usam BigDecimal sem arredondamento implícito para DECIMAL(8,2).
DATETIME é interpretado como UTC e recebe LocalDateTime explícito, com nanos
truncados para segundos. Não há `now()` baseado no fuso da máquina. O driver
recebe timezone UTC e sessão UTC, inclusive como padrão das propriedades RA3.
O conversor explícito `DataHoraUtcConverter` usa `Timestamp.from` e `ZoneOffset.UTC`
para impedir o deslocamento de horário observado com a conversão padrão.
Ele é registrado em `META-INF/orm-ra3.xml` porque a descoberta não listada está
desabilitada. `persistence.xml` mantém exatamente sete entradas de entidade;
o conversor não é entidade. O perfil padrão também exclui esse XML auxiliar.
Os defaults CURRENT_TIMESTAMP permanecem para inserts SQL que omitirem essas
colunas; os construtores Java recebem os instantes explicitamente.

As seis associações proprietárias e a coleção inversa de tipos usam EAGER
explícito com weaving=false. O teste confirma acesso após fechar o EM; não se
declara LAZY implementado. A coleção exposta é somente leitura, e criar um
TipoIngresso associa ambos os lados em memória, sem persistir automaticamente.
Não há cascade PERSIST/REMOVE ou orphanRemoval. As FKs SQL preservam CASCADE
somente em comunidade→evento e postagem→comunidade, conforme o original.
O serviço da Entrega 02 impede excluir pais com vínculos, inclusive nos dois
caminhos SQL com CASCADE, e sempre reabre o contexto na próxima operação.

Os cadastros de organizador/participante pelo menu usam o marcador fixo
`SEM_LOGIN_RA3_DEMO` no campo obrigatório `senha`. Ele não é senha pessoal,
não é hash seguro, não autentica e não serve para produção. Use somente dados
fictícios de CPF/CNPJ/e-mail. Este módulo não oferece hashing/login nem
getter público de hash. Entidades não são serializadas para JSON e não possuem
equals/hashCode/toString recursivos.

```powershell
Remove-Item Env:JOINUP_RA3_DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:JOINUP_RA3_IT_DATABASE -ErrorAction SilentlyContinue
```

## Navegação e campos do menu

O menu principal apresenta as sete entidades na ordem gênero, organizador,
participante, evento, tipo de ingresso, comunidade e postagem. `0` encerra.
Cada submenu oferece Cadastrar, Consultar por ID, Listar, Atualizar e Excluir;
`0` volta. Não há recursão entre menus. EOF encerra normalmente, inclusive no
meio de um formulário, sem confirmar uma operação incompleta.

Listagens usam páginas de **20 registros ordenados por ID**, indicam o limite
e pedem o número da página. Os formulários dependentes mostram os pais já
cadastrados e pedem seus IDs. Para pais fora da primeira página, consulte o
respectivo submenu antes e informe o ID conhecido; nenhum pai é criado em silêncio.

Use nascimento `2000-01-02`, data UTC `2030-11-12T18:30:00`, UF como `SP` e
valor como `125.50` (ponto, até duas casas). Campos obrigatórios em branco,
inteiros inválidos, datas e decimais inválidos são recusados sem matar o menu.
Imagem/descrição opcionais em branco limpam o campo durante atualização.
Postagem exige descrição e/ou imagem no serviço, sem mudar a nulabilidade SQL.
IDs ausentes, duplicidade, vínculos e concorrência produzem mensagens próprias,
sem SQL, URL de conexão, credenciais, hash ou stacktrace do driver.

Atualizações disponíveis: renomear gênero/organizador/participante; renomear e
alterar imagem de evento; nome/valor/quantidades de tipo; nome/descrição/imagem
de comunidade; descrição/imagem de postagem. Não há alteração de pais/identidades,
autenticação, compra ou venda real nesta entrega.

## Exclusão didática e ordem das associações

A remoção física de registros folha com `EntityManager.remove()` é uma escolha
técnica para o banco acadêmico isolado, **não uma imposição do enunciado**.
`ativo=false` não substitui a ação Excluir. Todas as sete entidades exigem
digitar exatamente `SIM`; outra resposta cancela. O serviço também verifica
essa confirmação, além do terminal.

| Entidade | Excluir somente quando |
| --- | --- |
| Gênero / Organizador | Não houver eventos associados. |
| Participante | Não houver postagens associadas. |
| Evento | Não houver tipos nem comunidade. |
| Comunidade | Não houver postagens. |
| Tipo / Postagem | São folhas no modelo RA3 atual. |

A transação bloqueia o pai com PESSIMISTIC_WRITE antes de consultar dependentes,
impedindo a janela em que um filho recém-inserido seria removido pelo CASCADE.
Falhas de FK ou deadlock concorrente fazem rollback; o programa informa a
falha e permite consultar novamente. Não existe exclusão de árvore silenciosa.

Cadastre nesta ordem: gênero e organizador; participante; evento com IDs dos
pais; tipo e comunidade com ID de evento; postagem com participante/comunidade.
Para excluir todo um conjunto próprio, use: postagens; tipos/comunidade;
evento; participante/organizador/gênero. Cada remoção requer confirmação.
Use essa ordem manualmente; não há comando de reset ou exclusão em lote.

Uma EntityManagerFactory pertence à execução CLI. Cada consulta/operação usa
um EntityManager novo e fechado; escrita usa begin/commit/rollback e verifica
o catálogo antes da transação. Atualizações usam dirty checking, sem bulk UPDATE;
exclusões usam remove, sem bulk DELETE. A próxima consulta observa o estado
persistido em novo contexto, inclusive após remoção de tipos da coleção EAGER.

Roteiro executado e matriz de provas: [resultados 02](ENTREGA_02_RESULTADOS.md)
e [transcrição CLI](evidencias/sessao-cli-02.txt).
Escopo encerrado na Entrega 02: relatório acadêmico final, reflexões da equipe,
UML final, integração web e Entrega 03 permanecem fora deste incremento.
