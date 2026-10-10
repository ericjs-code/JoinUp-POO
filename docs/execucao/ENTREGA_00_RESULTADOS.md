# Entrega 00 — resultados reais para revisão técnica

**Data:** 10/10/2026 (America/Sao_Paulo). **Branch:** `feat/00-fundacao-java-puro`. **Base:** `main`/`origin/main`, commit `44c99ce` (`docs(api): registra aprovação da API como baseline para Codex`).

## Escopo e leitura

Lidos integralmente o prompt `docs/prompts/CODEX_00_FUNDACAO_JAVA_PURO.md`, a especificação funcional, os **sete** adendos, ADR-001, proposta Java puro, classes/GoF, PlantUML, contratos HTTP, parecer de banco e schema/seed v0.2. Nenhum arquivo obrigatório estava inacessível. Os documentos históricos que ainda dizem “proposta/sem implementação” foram preservados; esta entrega foi autorizada pelo prompt e pela solicitação do PO.

Implementada somente a fundação: Java 21, Maven, servidor do JDK, composição manual, configuração Singleton, JDBC configurável, health, estáticos, limites/erros HTTP e testes. Não existem rotas de negócio, autenticação, execução automática de seed ou classes decorativas dos outros GoF. Não houve alteração de requisitos, dos SQLs aprovados ou do repositório PHP de referência.

## Ambiente e comandos

O PATH inicial continha Java **25.0.2**, sem Maven. Para validar a versão obrigatória, foram preparados **Eclipse Temurin 21.0.12.1** e **Apache Maven 3.9.9** em `%LOCALAPPDATA%\JoinUp-POO\tools`, fora do repositório, obtidos das distribuições oficiais com verificação de SHA-256 do JDK e SHA-512 do Maven. Não houve mudança permanente de `JAVA_HOME`/PATH do usuário.

Prefixo PowerShell efetivamente usado nos comandos Maven:

```powershell
$env:JAVA_HOME = Join-Path $env:LOCALAPPDATA 'JoinUp-POO\tools\jdk-21.0.12.1+1'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
& "$env:LOCALAPPDATA\JoinUp-POO\tools\apache-maven-3.9.9\bin\mvn.cmd" --version
```

Saída confirmada: Maven `3.9.9`, Java `21.0.12.1`, Eclipse Adoptium, Windows 11 amd64, UTF-8.

| Comando / ação | Resultado efetivo |
| --- | --- |
| `git fetch origin main` e comparação dos commits | Local e remoto na mesma base `44c99ce` |
| Primeira execução `mvn clean test` | **Falhou**: 52 testes, 0 falhas de asserção, 1 erro, 0 ignorados; bind permaneceu após fechar servidor antes de iniciar |
| Correção do ciclo de vida | Alocação/bind de `HttpServer` passou para `iniciar()`, evitando reservar porta na construção; teste de regressão preservado |
| `mvn --batch-mode --no-transfer-progress clean test` após correção | **BUILD SUCCESS**: 52 testes, 0 falhas, 0 erros, 0 ignorados; compilação `release 21` |
| `mvn --batch-mode --no-transfer-progress package` | **BUILD SUCCESS**, repetiu os 52 testes e produziu `target/joinup-poo.jar` com dependências |
| `java -jar target/joinup-poo.jar` (Java 21) | HTTP disponível em `127.0.0.1:8080`, sem configurar usuário/senha ou conectar a banco |
| `mvn --batch-mode --no-transfer-progress compile exec:java`, ambiente `JOINUP_HTTP_PORT=8081` | Aplicação iniciou em `127.0.0.1:8081`; health retornou 200 |
| `Ctrl+C` nas duas execuções | Encerramento confirmado por novo bind TCP nas portas 8080/8081, ambas liberadas; saída 1 da sessão por interrupção intencional, sem alegar conclusão normal de build em processo de servidor |
| `jar tf target/joinup-poo.jar` | Confirmados `public/index.html`, `com/mysql/cj/jdbc/Driver.class` e `META-INF/services/java.sql.Driver` |
| `git diff --exit-code origin/main -- src/main/resources/db/schema_v0.2.sql src/main/resources/db/seed_demo_v0.2.sql` | Código 0; ambos os SQLs aprovados intactos |
| `git diff --check` | Sem problemas de whitespace |

O Maven Shade informou warnings de sobreposição de manifests/LICENSE/NOTICE e descritores `module-info` nas dependências. O projeto executa no **classpath**, com manifesto de Main e descritores de serviços combinados. O JAR foi executado com sucesso; modularização JPMS não foi reivindicada. Esses warnings não são erros de compilação/teste.

## Testes automatizados: unidades e integração HTTP

Resultados Surefire reais, armazenados localmente em `target/surefire-reports/`:

| Classe | Testes | Natureza / evidência |
| --- | ---: | --- |
| `ConfiguracaoSistemaTest` | 16 | Identidade em 100 chamadas concorrentes/16 threads, estrutura imutável, precedência/snapshot e configurações inválidas em JVM isolada; bloqueio de URL legado/ambígua |
| `HttpFoundationTest` | 33 | Integração real `HttpServer` + `HttpClient`, porta efêmera, health, query string, métodos, 404/405, HTML/CSS/JS/HEAD, traversal, JSON inválido, erro 500 sem vazamento, limite de corpo declarado/chunked e 32 corpos concorrentes isolados |
| `ApplicationBootstrapTest` | 2 | Composição real, estáticos/health, fechamento idempotente, encerramento e liberação de porta; fechamento antes de iniciar |
| `SeedDemoPasswordTest` | 1 | Verificação **offline** dos nove hashes presentes no arquivo SQL com `PBKDF2WithHmacSHA256`; senha demo aceita e senha incorreta rejeitada |
| **Total** | **52** | **0 falhas, 0 erros, 0 ignorados** |

Unidades/configuração/fixture: 17 testes. Integração HTTP/bootstrap **sem banco**: 35 testes. Não foram usados mocks para alegar integração MySQL. Rotas `/api/teste/json` e `/api/teste/falha` existem somente na composição dos testes, não no bootstrap de produção. A mensagem de log “Falha interna no processamento HTTP” durante a suíte é esperada no teste de 500 e não revela a exceção original.

## Verificação do artefato e navegador

Com o JAR ativo, `Invoke-WebRequest` confirmou:

```text
GET /api/health       200  application/json; charset=utf-8
{"dados":{"status":"UP"}}
GET /                 200  text/html; charset=utf-8
GET /css/inicio.css    200  text/css; charset=utf-8
GET /js/inicio.js      200  text/javascript; charset=utf-8
GET /api/ausente       404  RECURSO_NAO_ENCONTRADO
POST /api/health       405  METODO_NAO_PERMITIDO
```

Navegador integrado em `http://127.0.0.1:8080/`: título “JoinUp”, texto/estilo carregados e estado “Servidor disponível.”. O botão **Verificar novamente** foi acionado e o estado disponível foi observado novamente. Captura real:

![Página inicial da Entrega 00 com servidor disponível](evidencias/entrega_00_inicio.png)

## MySQL: NÃO EXECUTADO

`Get-Command mysql,docker -ErrorAction SilentlyContinue` não encontrou os comandos, e `Get-Service -Name '*mysql*' -ErrorAction SilentlyContinue` não encontrou serviço. Não foi identificada instância MySQL 8 de testes autorizada/disponível nesta sessão. **Não foram executados** DDL, seed, segunda carga, conferência efetiva de FKs/CHECKs ou conexões JDBC reais. Não houve qualquer comando contra `joinup` legado.

O teste dos hashes do seed é uma evidência Java offline; não demonstra persistência, autenticação ou sucesso no MySQL. Roteiro exato de instalação isolada, inspeção e reexecução: [VALIDACAO_FUNDACAO_00.md](../banco/VALIDACAO_FUNDACAO_00.md), com consultas auxiliares em `docs/banco/validar_fundacao_00.sql`.

## Limitações e decisões para revisão

- Validar schema/seed/FKs/contagens e hashes persistidos em instância real MySQL 8 antes dos fluxos persistentes. Nenhuma correção SQL foi proposta sem evidência de execução.
- `GET /api/health` é liveness HTTP, não readiness de banco. O modo demo é configuração reservada; o seed continua manual nesta entrega.
- Tamanho padrão de corpo: 64 KiB; threads: 8, fila: 128. São decisões de infraestrutura configuráveis, sem mudança de regras de negócio. Não houve teste de carga ou avaliação de clientes lentos.
- Cookies seguros, sessões, CSRF, uploads, regras de negócio e outros GoF aguardam os incrementos correspondentes, sem promessa de estarem prontos.
- CI Java 21 (`.github/workflows/java.yml`) executou `mvn --batch-mode --no-transfer-progress clean verify` com **sucesso em Linux** no commit de implementação `68effed513207f7a38dfff2569ce512d1a22d978`: [execução do push](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38077439772) e [execução do PR](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38077461433). São evidências adicionais aos testes locais Windows; nenhuma dessas execuções usa MySQL.
- A entrega deve ser revisada pelo Tech Lead no PR da branch indicada. **Sem merge automático na `main` e sem avanço para outras etapas.**

## Referências de auditoria

- [Pull Request #1 — Entrega 00 — Fundação Java 21 puro](https://github.com/ericjs-code/JoinUp-POO/pull/1), aberto para revisão técnica contra `main`.
- [Commit de implementação 68effed](https://github.com/ericjs-code/JoinUp-POO/commit/68effed513207f7a38dfff2569ce512d1a22d978). Esta atualização do relatório apenas registra os resultados posteriores de CI e a identificação do PR.
