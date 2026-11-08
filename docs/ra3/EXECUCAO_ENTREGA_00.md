# RA3 — Execução da Entrega 00 no PowerShell

Pré-requisitos: JDK 21 (`JAVA_HOME`), Maven 3.9.x e MySQL 8.0.16+.
Homologado nesta entrega com JDK 21.0.12.1, Maven 3.9.9 e MySQL 8.4.0.
`mvn` e `mysql` devem estar no PATH; caminhos absolutos equivalentes funcionam.
Execute a partir da raiz do checkout da branch `feat/ra3-00-fundacao-jpa`.

## Compilar e testar sem MySQL

```powershell
java -version
mvn -version
mvn clean test                 # Aplicação original: sem classes/dependências JPA
mvn clean verify               # JAR web original em target/joinup-poo.jar
mvn -Pra3 clean test           # Inclui RA3; não abre conexão com MySQL
mvn -Pra3 dependency:tree '-Dincludes=org.eclipse.persistence:*,jakarta.persistence:*,com.mysql:*'
```

O perfil RA3 usa `target/ra3/classes` e `target/ra3/test-classes`, com relatórios
em `target/ra3/surefire-reports`. A fábrica criada no teste unitário de provider
carrega o descriptor sem login (inicialização adiada do EclipseLink). Esse teste
**não comprova persistência**. Somente o perfil de integração abaixo faz isso.

O `mysql-it` existente continua reservado à RA2. Não combine `mysql-it` com
`ra3-it`; o procedimento RA3 não precisa nem deve executar schema/seed antigos.

## Preparar manualmente o banco RA3

Escolha uma instância/porta do RA3. O exemplo usa a porta 3309; para integração
use uma instância descartável separada da aplicação e dos dados de uso normal.
Nenhum comando Java cria tabelas. Com uma conta administrativa dessa instância:

```powershell
mysql --host=127.0.0.1 --port=3309 --user=administrador -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/ra3/schema_ra3.sql"
# Confira erros/warnings de SOURCE e depois a estrutura criada:
mysql --host=127.0.0.1 --port=3309 --user=administrador -p --execute="SHOW CREATE TABLE joinup_poo_ra3.generos_musicais;"
```

O script cria apenas `joinup_poo_ra3` e `generos_musicais`, se ausentes.
Não corrige silenciosamente uma tabela preexistente incompatível. Não aplique
`schema_v0.2.sql`, `migration_06_midia_demo.sql` ou seed da RA2 nesse banco.
Crie uma conta dedicada com SELECT, INSERT, UPDATE e DELETE exclusivamente em
`joinup_poo_ra3.*`; não conceda CREATE, ALTER ou DROP ao usuário da aplicação.
Escolha a senha fora do repositório, por uma sessão administrativa interativa.

```sql
-- Modelo de concessão após criar a conta; adapte host e usuário à instância.
GRANT SELECT, INSERT, UPDATE, DELETE ON joinup_poo_ra3.* TO 'joinup_ra3_app'@'127.0.0.1';
```

## Configuração exclusiva e diagnóstico

```powershell
$env:JOINUP_RA3_JDBC_URL = 'jdbc:mysql://127.0.0.1:3309/joinup_poo_ra3?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_RA3_DB_USER = 'joinup_ra3_app'
$ra3Segredo = Read-Host 'Senha do usuário RA3' -AsSecureString
$ra3Credencial = [System.Net.NetworkCredential]::new('', $ra3Segredo)
$env:JOINUP_RA3_DB_PASSWORD = $ra3Credencial.Password
Remove-Variable ra3Credencial,ra3Segredo

mvn -Pra3 clean test           # Necessário compilar antes do primeiro exec:java
mvn -Pra3 exec:java
```

O comando imprime `RA3_CONEXAO_OK` após abrir EclipseLink e consultar o catálogo,
fecha EntityManager/fábrica e encerra. Não inicia o HTTP e não persiste fixture.
`exec:java` sozinho não compila fontes; após checkout limpo, execute o `clean test`
acima ou `mvn -Pra3 compile exec:java`.

As três variáveis são obrigatórias; não há fallback para `JOINUP_JDBC_URL`,
`JOINUP_DB_USER` ou `JOINUP_DB_PASSWORD`. `.env` não é carregado. Não colocar
credenciais na URL, XML, argumentos Maven, prints ou arquivos versionados.
Falha de configuração imprime `RA3_CONFIGURACAO_INVALIDA`; indisponibilidade ou
erro de login imprime `RA3_CONEXAO_FALHOU`. O comando retorna falha Maven, sem
propagar mensagens brutas do driver/provider que possam incluir segredo.
Verifique serviço/porta, credenciais, grants e aplicação manual do SQL.

A URL aceita um host DNS/IPv4 simples, porta opcional 1–65535 e exatamente
`/joinup_poo_ra3`. Não aceita multi-host, IPv6, load-balance, percent-encoding,
userinfo, fragmento ou schema alternativo. Parâmetros opcionais, sem repetição:

| Parâmetro | Valores permitidos |
| --- | --- |
| connectTimeout, socketTimeout | 1–99999 milissegundos; padrões 5000/10000 |
| connectionTimeZone | UTC |
| forceConnectionTimeZoneToSession | true |
| characterEncoding | UTF-8 ou utf8 |
| useSSL, allowPublicKeyRetrieval | true ou false |

Para **fixture local** sem TLS, se o servidor exigir obtenção da chave de
autenticação, acrescente `&useSSL=false&allowPublicKeyRetrieval=true` à URL.
A execução verificada usou isso somente na instância descartável em loopback.
URLs com `databaseName`, `sessionVariables`, `propertiesTransform`, `user` ou
`password`, entre outras opções não listadas, são recusadas antes de conectar.

## Smoke test real opt-in

Prepare uma **nova instância MySQL descartável**, aplique somente o SQL RA3,
crie a conta DML e aponte as três variáveis RA3 para essa instância.
Não use o banco RA3 com dados definitivos. Confirme a finalidade:

```powershell
$env:JOINUP_RA3_IT_DATABASE = 'instancia-descartavel-ra3'
mvn '-Pra3,ra3-it' clean verify
```

O Failsafe falha se faltar confirmação, configuração ou MySQL; não pula o teste
para produzir sucesso aparente. Há um cenário integrado:

1. Valida URL e `SELECT DATABASE()` igual a `joinup_poo_ra3`.
2. Inicia transação, verifica catálogo, faz `persist` e `commit` de gênero com UUID.
3. Fecha o primeiro EntityManager e faz `find` em outro, sem cache compartilhado.
4. Consulta a linha por SQL no catálogo qualificado, confirmando ID/nome.
5. Persiste outra fixture, força falha após `flush`, faz rollback e verifica ausência.
6. Em `finally`, remove por nome UUID apenas as fixtures próprias e verifica ausência.
7. Fecha todos os EntityManagers e a fábrica, inclusive nas falhas.

O `DELETE` pertence à limpeza do teste descartável; não constitui CRUD de domínio.
Uma interrupção abrupta do processo pode deixar uma fixture UUID, identificável
pelo prefixo `ra3-it-`; não há reset ou limpeza ampla automática.

Evidências: `RA3_SMOKE_OK`, `RA3_FIXTURE_LIMPA`, XML/TXT de
`target/ra3/failsafe-reports`, com um teste e zero falhas/erros/skips.
O workflow [ra3.yml](../../.github/workflows/ra3.yml) repete o procedimento numa
instância MySQL 8.4 de CI dedicada, com conta DML e senhas apenas de fixture pública.
O workflow JDBC existente não foi alterado.

Ao terminar a sessão, remova segredos do ambiente:

```powershell
Remove-Item Env:JOINUP_RA3_DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:JOINUP_RA3_IT_DATABASE -ErrorAction SilentlyContinue
```

Somente Entrega 00 está implementada. Não há outras seis entidades, relacionamentos,
menu de operações, CRUD completo, integração de frontend ou relatório acadêmico final.
