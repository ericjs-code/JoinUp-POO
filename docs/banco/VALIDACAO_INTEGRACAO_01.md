# MySQL real — validação da Entrega 01

Data: 10/10/2026 (America/Sao_Paulo). Instância exclusiva para testes do JoinUp-POO.
Nenhuma operação executada no banco legado `joinup`.

## Ambiente local efetivamente usado

- Windows 11, JDK Eclipse Temurin 21.0.12.1, Maven 3.9.9.
- **MySQL Community Server 8.4.0**, pacote ZIP oficial para Windows:
  [arquivo oficial](https://cdn.mysql.com/archives/mysql-8.4/mysql-8.4.0-winx64.zip).
- SHA-256 do ZIP recebido: `FB1F0C6CCE229BE4B3208BEC8A605576BB47EABB00512BCCBCC4396AF332C25C`.
  Registro de integridade do download, sem alegar conferência de assinatura externa.
- Binários em `%LOCALAPPDATA%/JoinUp-POO/tools/mysql-8.4.0-winx64`, fora do Git.
- Datadir inicial `mysql-entrega01-data` falhou no DDL original e foi encerrado.
  Nova instância vazia `mysql-entrega01-validada`, bind 127.0.0.1, porta 3307,
  mysqlx desabilitado, default-time-zone=+00:00; nenhum serviço global instalado.
- Conta `joinup_it@127.0.0.1` com SELECT/INSERT/UPDATE/DELETE exclusivamente em
  joinup_poo para aplicação; root local usado apenas pelo roteiro de fixtures e
  triggers de teste. Credenciais locais de teste não servem para produção.

## Comandos realmente executados

```powershell
$mysqlBase = Join-Path $env:LOCALAPPDATA 'JoinUp-POO/tools/mysql-8.4.0-winx64'
$mysqlData = Join-Path $env:LOCALAPPDATA 'JoinUp-POO/mysql-entrega01-validada'
& "$mysqlBase/bin/mysqld.exe" --no-defaults --initialize-insecure "--basedir=$mysqlBase" "--datadir=$mysqlData" --console
Start-Process -FilePath "$mysqlBase/bin/mysqld.exe" -ArgumentList @('--no-defaults',"--basedir=$mysqlBase","--datadir=$mysqlData",'--port=3307','--bind-address=127.0.0.1','--mysqlx=0','--default-time-zone=+00:00',"--log-error=$mysqlData/mysql-error.log") -WindowStyle Hidden
& "$mysqlBase/bin/mysql.exe" --no-defaults --host=127.0.0.1 --port=3307 --user=root --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/schema_v0.2.sql"
& "$mysqlBase/bin/mysql.exe" --no-defaults --host=127.0.0.1 --port=3307 --user=root --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/seed_demo_v0.2.sql"
& "$mysqlBase/bin/mysql.exe" --no-defaults --host=127.0.0.1 --port=3307 --user=root --batch --execute="source docs/banco/validar_fundacao_00.sql"
```

Initialize-insecure gerou warning esperado de root local sem senha na instância
temporária de teste, ligada apenas em loopback. Não é roteiro de produção.
DDL original: ERROR 3823 (CHECK/FK); seed após essa falha: ERROR 1146 (tabela ausente).
Correção isolada e justificada em [CORRECAO_DDL_01.md](CORRECAO_DDL_01.md).
DDL corrigido em nova instância: quatro warnings 1681 de display width TINYINT(1),
sem erros. Primeira carga seed: **zero warnings/erros**.

JDBC efetivamente usado:

```powershell
$env:JAVA_HOME = Join-Path $env:LOCALAPPDATA 'JoinUp-POO/tools/jdk-21.0.12.1+1'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3307/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_DB_USER = 'joinup_it'
# JOINUP_DB_PASSWORD configurada somente no ambiente de teste
$env:JOINUP_IT_ADMIN_USER = 'root'
$env:JOINUP_IT_ADMIN_PASSWORD = ''
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
& "$env:LOCALAPPDATA/JoinUp-POO/tools/apache-maven-3.9.9/bin/mvn.cmd" --batch-mode --no-transfer-progress -Pmysql-it clean verify
```

`-Pmysql-it` exige opt-in e conexão real, falhando se indisponível; não ignora
testes de banco. A suíte faz DDL/seed sob administrador e usa apenas a conta DML
para o servidor HTTP. Após testes, limpa somente PKs criadas pela própria suíte
na instância isolada; não executa DROP DATABASE, TRUNCATE ou FOREIGN_KEY_CHECKS=0.

## Resultados conferidos no servidor

- VERSION() = **8.4.0**, DATABASE() = **joinup_poo**, sessão SQL = **+00:00**.
- **22 tabelas InnoDB**, **42 FKs** pelo nome do DDL, **15 CHECKs ENFORCED=YES**.
- **44 índices nomeados** conferidos; índice UNIQUE gerado de anúncio disponível
  por ingresso, estados ENUM e preços DECIMAL confirmados.
- Inserções/updates inválidos recusados efetivamente: CHECK 3819, FK 1452/1451,
  UNIQUE 1062. CHECK de autoria e preservação de autor testados após correção.
- Nove hashes PBKDF2 lidos das tabelas e validados com senha correta/incorreta.
- Reexecução: schema retorna 27 warnings esperados (1007/1050/1681: banco/tabelas
  já existentes e display width); seed retorna 97 warnings 1062 de duplicatas.
  Sem warning inesperado. Comparação JDBC de **todas as colunas e linhas**, incluindo
  timestamps/hashes, sem diferenças. Contagens preservadas:

| Tabela | Linhas | Tabela | Linhas |
| --- | ---: | --- | ---: |
| artistas | 10 | organizadores | 3 |
| generos_musicais | 15 | participantes | 4 |
| usuarios_suporte | 2 | eventos | 4 |
| line_up | 6 | tipos_ingressos | 6 |
| comunidades | 4 | compras | 6 |
| compra_itens | 6 | ingressos | 7 |
| revenda_anuncios | 4 | participante_comunidades | 5 |
| comunidade_mensagens | 3 | postagens | 2 |
| comentarios | 1 | postagem_curtidas | 1 |
| ticket | 3 | ticket_mensagens | 5 |
| sessoes | 0 | auditoria_reativacoes | 0 |

As contagens são do seed, antes dos fluxos HTTP e após limpeza da suíte.
Autenticação/transações foram executadas com HTTP real local: cadastro/login,
sessão revalidada, desativação, cancelamento de anúncios, reativação auditada,
rollback por falhas intermediárias e disputas concorrentes. Isso encerra a
ressalva de **ausência de execução MySQL** da auditoria 00 para esta implementação,
sem substituir o parecer de auditoria técnica do PR.

## CI reproduzível

Workflow `Java 21 e autenticação MySQL` provisiona serviço **mysql:8.4.0** isolado,
executa `mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify` e publica
Surefire/Failsafe e `target/mysql-validation/integracao.txt`. Usuário DML restringido
pela preparação da suíte, separado de root de teste. O arquivo de evidência
não contém senha/token/hash; registra versão, inventário, contagens e warnings.
Execução remota confirmada:
[run 38079625957](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38079625957),
commit `fe533890dc8eec85daf207c6e341fbe76c1a15a1`, job `114293668730`, success.
Logs mostram MySQL 8.4.0, 86 testes Surefire e 23 Failsafe sem falhas, erros ou
ignorados. [Artefato 11679946460](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38079625957/artifacts/11679946460)
contém relatórios e inventário SQL. Execução local final também passou com
**109 testes** (86 + 23), sem falhas/erros/ignorados.

Não confundir o health liveness UP, hashes offline ou workflow apenas configurado
com teste de banco. Os resultados acima decorrem da execução **local real**; a
execução remota possui o status/run e artefato específicos acima.
