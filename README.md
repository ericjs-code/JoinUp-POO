# JoinUp-POO

Entrega 00 — fundação Java 21 puro. Servidor HTTP nativo do JDK, Jackson, JDBC/MySQL Connector/J e testes JUnit 5. O escopo autorizado está em [CODEX_00](docs/prompts/CODEX_00_FUNDACAO_JAVA_PURO.md).

Esta entrega disponibiliza a página inicial e o diagnóstico HTTP. Cadastros, autenticação, sessões, eventos, compras, revendas, comunidades e suporte serão implementados nas próximas entregas. Singleton está aplicado à configuração; os demais GoF aguardam seus fluxos reais.

## Pré-requisitos e instalação

- JDK **21**, com `JAVA_HOME` apontando para ele e `java` no PATH.
- Maven **3.9.x**, com `mvn` no PATH; acesso ao Maven Central na primeira execução.
- MySQL **8.0.16+** somente para validar/integrar banco. A página e `/api/health` funcionam sem MySQL.

```powershell
git clone https://github.com/ericjs-code/JoinUp-POO.git
cd JoinUp-POO
git switch feat/00-fundacao-java-puro
java -version
mvn --version
mvn clean test
mvn package
java -jar target/joinup-poo.jar
```

Abra **http://127.0.0.1:8080/**. O botão verifica `GET /api/health`, cuja resposta é `{"dados":{"status":"UP"}}`. Este endpoint informa apenas que o HTTP responde; **não verifica banco, schema, seed ou autenticação**. Encerre com `Ctrl+C`: shutdown hook para o servidor, encerra o executor e aguarda os recursos.

Alternativa em desenvolvimento, após compilar:

```powershell
mvn compile exec:java
```

Em Linux/macOS, os mesmos comandos de Maven/JAR se aplicam. Ajuste somente os exemplos de exportação de variáveis abaixo para `export NOME=valor`.

## Configuração

Precedência: **propriedade JVM `-D` → variável de ambiente → padrão**. Valores são lidos uma vez, na inicialização do Singleton `ConfiguracaoSistema`, com construtor privado e initialization-on-demand holder. Configurações inválidas impedem a inicialização; mensagens não incluem valores de credenciais. Não há conexão JDBC permanente ou global.

| Variável | Propriedade JVM | Padrão / limite |
| --- | --- | --- |
| `JOINUP_HTTP_HOST` | `joinup.http.host` | `127.0.0.1` |
| `JOINUP_HTTP_PORT` | `joinup.http.port` | `8080`; 1–65535 |
| `JOINUP_HTTP_THREADS` | `joinup.http.threads` | `8`; 1–64, fila de 128 tarefas |
| `JOINUP_MAX_REQUEST_BYTES` | `joinup.maxRequestBytes` | `65536` (64 KiB); 1–1048576 |
| `JOINUP_DEMO` | `joinup.demo` | `false`; aceita apenas `true`/`false` |
| `JOINUP_JDBC_URL` | `joinup.jdbc.url` | `jdbc:mysql://127.0.0.1:3306/joinup_poo?connectionTimeZone=UTC&connectTimeout=5000&socketTimeout=10000` |
| `JOINUP_DB_USER` | `joinup.db.user` | vazio; obrigatório ao abrir conexão |
| `JOINUP_DB_PASSWORD` | `joinup.db.password` | vazio; configurar conforme instância local |

`.env.example` é **referência**, sem leitura automática de `.env`. Não há arquivo de propriedades carregado automaticamente; as propriedades são as da JVM. Prefira ambiente para segredos, evitando senha na linha de comando/histórico. A URL JDBC aceita exclusivamente o banco `joinup_poo`; nunca aponte para `joinup` legado. Sem configuração de usuário, iniciar o HTTP é permitido, e tentar abrir conexão JDBC falha claramente.

```powershell
$env:JOINUP_HTTP_PORT = '8081'
$env:JOINUP_DEMO = 'false'
java -jar target/joinup-poo.jar
# Alternativa para parâmetros não sensíveis (prioridade sobre ambiente):
java -Djoinup.http.port=8082 -jar target/joinup-poo.jar
```

`JOINUP_DEMO` reserva a configuração do modo demonstrativo para os próximos incrementos. **Não executa seed automaticamente nesta entrega**, nem habilita login/rotas de negócio. A carga aprovada é aplicada manualmente apenas em instância local de testes.

## Organização e acesso a dados

- `Main` e `bootstrap/ApplicationBootstrap`: ciclo de vida, composição manual e executor controlado.
- `config/ConfiguracaoSistema`: configuração única, imutável e thread-safe, consumida pelo bootstrap e pela fábrica JDBC.
- `http`: roteador exato por método/path, limite comum de corpo, JSON, erro padronizado, health e estáticos.
- `shared/db/ConnectionFactory`: abre uma nova conexão por chamada. Nenhum SQL é executado na inicialização ou nos handlers.
- `resources/public`: únicos recursos públicos; schema/seed e demais arquivos do classpath não são expostos.

Exemplo para futuros casos de uso, com responsabilidade de fechamento/transação no chamador:

```java
try (var conexao = connectionFactory.abrirConexao()) {
    conexao.setAutoCommit(false);
    try {
        // Repositórios JDBC futuros recebem esta conexão e usam PreparedStatement.
        conexao.commit();
    } catch (Exception e) {
        conexao.rollback();
        throw e;
    }
}
```

## HTTP e limites de segurança

- API desconhecida: **404**; método não registrado em rota existente: **405** com `Allow`. Estáticos existentes aceitam `GET`/`HEAD`; recursos ausentes retornam 404 independentemente do método.
- Erros seguem `{"erro":{"codigo":"...","mensagem":"...","detalhes":{}}}`. Além dos códigos aprovados usados em 400/404, os códigos de transporte desta fundação são `METODO_NAO_PERMITIDO` (405), `REQUISICAO_MUITO_GRANDE` (413) e `ERRO_INTERNO` (500).
- Limite de corpo aplicado com `Content-Length` ou leitura limitada de corpo chunked. JSON rejeita sintaxe inválida, `null`, chaves duplicadas e tokens excedentes. A Entrega 00 não possui endpoint JSON de escrita; essa infraestrutura é exercitada por rotas exclusivas dos testes.
- Caminhos com `..`, `.`, barras invertidas, caracteres de controle ou codificação dupla são rejeitados antes do acesso a recursos. Estáticos usam apenas `/public` do classpath, também no JAR; sem fallback de SPA.
- Mesma origem; sem CORS irrestrito, cookies ou sessões. Respostas incluem `nosniff`, política de conteúdo restrita, `no-store` e `Referrer-Policy`.
- Próxima entrega de autenticação: cookie `HttpOnly`, `SameSite=Lax`, `Secure` em HTTPS, token imprevisível persistido somente como hash, expiração/revogação; proteção CSRF nas mudanças de estado conforme contrato. **Esses mecanismos ainda não estão implementados.**

O executor tem número de threads e fila limitados; saturação usa o dispatcher para aplicar contrapressão. Proteção contra clientes lentos, limites de cabeçalhos e infraestrutura HTTPS/reverse proxy precisam de validação para exposição pública; esta entrega é uma fundação local, sem teste de carga.

## Banco independente e validação MySQL

**Status real: NÃO EXECUTADO em MySQL.** Não foram encontrados cliente/serviço MySQL ou Docker nesta sessão. Nenhuma conexão efetiva, criação de tabela, FK ou execução/reexecução de seed foi validada. Os arquivos SQL aprovados permanecem intactos. A verificação PBKDF2 dos nove hashes do arquivo foi realizada com o JDK em teste offline, sem constituir teste de banco ou login.

Instruções exatas e verificações pendentes: [VALIDACAO_FUNDACAO_00.md](docs/banco/VALIDACAO_FUNDACAO_00.md). Resultados desta entrega: [ENTREGA_00_RESULTADOS.md](docs/execucao/ENTREGA_00_RESULTADOS.md).

## Testes

```powershell
mvn clean test
mvn package
```

O Surefire publica resultados em `target/surefire-reports/`. A suíte cobre identidade concorrente do Singleton, imutabilidade, configuração em JVM isolada, HTTP real em porta efêmera, health, 404/405, estáticos, traversal, erros de JSON/500, limite declarado/chunked, isolamento de corpos concorrentes, fechamento/liberação de porta e hashes PBKDF2 do seed. Ela **não usa nem simula MySQL**. As rotas de teste não são registradas na aplicação.
