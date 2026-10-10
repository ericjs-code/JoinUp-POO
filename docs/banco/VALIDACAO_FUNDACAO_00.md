# Entrega 00 — instalação isolada e validação MySQL

Data: 10/10/2026. **NÃO EXECUTADO em MySQL** nesta sessão. A inspeção de comandos (`mysql`, `docker`) e serviços (`*mysql*`) não encontrou uma instância de teste disponível. Nenhuma operação foi executada no legado `joinup`.

Os SQLs `src/main/resources/db/schema_v0.2.sql` e `seed_demo_v0.2.sql` foram lidos integralmente e **não alterados**. Leitura e compilação Java não comprovam execução de DDL ou aplicação de FKs/CHECKs no MySQL.

## Pré-condições

1. Disponibilizar servidor e cliente **MySQL 8.0.16+**, preferencialmente instância local isolada exclusiva para testes, sem dados preexistentes de `joinup_poo`.
2. Usar credenciais locais com permissão para criar **somente `joinup_poo`** e suas tabelas. Conta da aplicação deve ter privilégios restritos ao banco novo; não precisa de DDL no funcionamento HTTP.
3. Executar a partir da raiz do repositório. Os scripts aprovados contêm `CREATE DATABASE IF NOT EXISTS joinup_poo`/`USE joinup_poo`; não usar dumps/importações do legado, `DROP` ou migrations não aprovadas. `IF NOT EXISTS` não migra tabelas antigas.
4. Não aplicar o seed em ambiente público. `INSERT IGNORE` é repetível apenas sobre a própria base demonstrativa e pode ocultar erros como warnings; inspecionar toda a saída da primeira execução.

## Comandos exatos (PowerShell, da raiz do repositório)

Troque `usuario_teste` pela conta local autorizada. `-p` solicita senha sem incluí-la na linha de comando.

```powershell
New-Item -ItemType Directory -Force target/mysql-validation | Out-Null
mysql --version
mysql --host=127.0.0.1 --port=3306 --user=usuario_teste -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/schema_v0.2.sql" > target/mysql-validation/schema.txt
# Conferir exit code, stdout e stderr; SOURCE pode continuar após erro.
$LASTEXITCODE
Get-Content target/mysql-validation/schema.txt

mysql --host=127.0.0.1 --port=3306 --user=usuario_teste -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/seed_demo_v0.2.sql" > target/mysql-validation/seed-primeira.txt
$LASTEXITCODE
Get-Content target/mysql-validation/seed-primeira.txt

mysql --host=127.0.0.1 --port=3306 --user=usuario_teste -p --batch --execute="source docs/banco/validar_fundacao_00.sql" > target/mysql-validation/antes.txt
$LASTEXITCODE
Get-Content target/mysql-validation/antes.txt

mysql --host=127.0.0.1 --port=3306 --user=usuario_teste -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/seed_demo_v0.2.sql" > target/mysql-validation/seed-segunda.txt
$LASTEXITCODE
Get-Content target/mysql-validation/seed-segunda.txt

mysql --host=127.0.0.1 --port=3306 --user=usuario_teste -p --batch --execute="source docs/banco/validar_fundacao_00.sql" > target/mysql-validation/depois.txt
$LASTEXITCODE
Compare-Object (Get-Content target/mysql-validation/antes.txt) (Get-Content target/mysql-validation/depois.txt)
mvn -Dtest=SeedDemoPasswordTest test
```

O último teste lê o arquivo SQL e valida com `PBKDF2WithHmacSHA256` e comparação `MessageDigest.isEqual`, incluindo senha incorreta; **não lê hashes persistidos**. Após a importação real, conferir que os nove valores persistidos nas tabelas de usuários coincidem com a fixture e validar também esses valores com o JDK. Autenticação/login funcional fica fora da Entrega 00.

Não confiar somente em `$LASTEXITCODE` ao usar `source`: inspecionar também os erros impressos pelo cliente e os warnings. Interromper o roteiro se houver erro de DDL, faltarem tabelas/FKs ou as contagens divergirem. Para anexar stderr às evidências em PowerShell, use `*> arquivo.txt` em lugar de `> arquivo.txt` nos comandos de importação.

## Evidências a conferir quando o banco estiver disponível

- `VERSION()` confirma MySQL 8 compatível e `DATABASE()` é exatamente `joinup_poo`.
- **22 tabelas**; comparar cada FK listada com o DDL aprovado, CHECKs `ENFORCED=YES` e coluna gerada/índice único condicional da revenda.
- Primeira aplicação sem warnings de truncamento, CHECK/FK ou inserções ignoradas inesperadas. Na segunda aplicação, duplicatas de PK/UNIQUE das fixtures são esperadas; as contagens não aumentam.
- Contagens esperadas após a primeira carga (derivadas do arquivo, ainda não verificadas em banco):

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

- Sem diferenças entre `antes.txt` e `depois.txt`. Contagens iguais são uma evidência de idempotência; não dispensam conferência de warnings e integridade dos relacionamentos.
- Conexões JDBC reais independentes e fechadas por operação; rollback/transações de negócio só serão validados quando esses fluxos forem implementados.

Se algum erro objetivo impedir o DDL/seed, registrar versão, comando, saída/erro e diagnóstico antes de propor correção mínima em arquivo/commit isolado. Não editar silenciosamente os SQLs aprovados.

## Pendência desta entrega

Provisionar/identificar MySQL 8 de testes e executar o roteiro acima, anexando evidências ao PR. O health check e a verificação offline dos hashes não encerram essa pendência. Os documentos históricos de aprovação permanecem preservados, sem substituir seu status por alegação de teste não realizado.
