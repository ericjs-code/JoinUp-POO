# JoinUp-POO

Java 21 puro com HttpServer do JDK, JDBC/MySQL 8, Jackson e JUnit. Entregas 00–06:
contas/autenticação, eventos, compra/revenda, comunidades sociais, chamados
e painéis por perfil. Os quatro GoF estão em fluxos executáveis:
Singleton, Factory Method, Template Method e Facade transacional.

Interfaces em `/conta.html`, `/eventos.html`, `/ingressos.html`,
`/comunidades.html` e `/painel.html`, com identidade visual escura/roxa próxima
do JoinUp PHP. Mídia segura e carga demo automática estão implementadas na Entrega 06; a consolidação RA2 permanece pendente. O projeto usa banco próprio
`joinup_poo`, sem importar registros legados.

## Executar

Pré-requisitos: **JDK 21**, Maven **3.9.x**, MySQL **8.0.16+**; os testes reais
foram executados com MySQL **8.4.0**. JAVA_HOME deve apontar para o JDK 21.

```powershell
git clone https://github.com/ericjs-code/JoinUp-POO.git
cd JoinUp-POO
git switch main
mvn clean verify
```

A `main` já incorpora o PR #7 (Entrega 05). A Entrega 06 está na branch
`feat/06-midia-demo` e depende de auditoria técnica antes do merge.
Prepare instância **isolada** de MySQL, aplique os SQLs e configure a conta DML:

```powershell
mysql --host=127.0.0.1 --port=3306 --user=administrador -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/schema_v0.2.sql"
mysql --host=127.0.0.1 --port=3306 --user=administrador -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/migration_06_midia_demo.sql"
# Conferir erros/warnings, não somente o exit code de SOURCE.
$env:JOINUP_DB_USER = 'joinup_app'
# Configure JOINUP_DB_PASSWORD no ambiente, sem publicar credenciais.
$env:JOINUP_ENV = 'demo'
$env:JOINUP_DEMO = 'true'
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3306/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
# JOINUP_UPLOAD_DIR opcional: diretório privado fora do repositório/webroot.
java -jar target/joinup-poo.jar
```

A conta da aplicação precisa somente de SELECT/INSERT/UPDATE/DELETE em
joinup_poo. Schema e migração são tarefas administrativas; o bootstrap nunca
executa DDL. Com `JOINUP_DEMO=true`, `JOINUP_ENV=demo`, JDBC em loopback e
HTTP em 127.0.0.1, aplica o seed empacotado em transação e registra a proveniência
em `demo_execucao`. Exige base vazia no primeiro uso; base populada sem marcador
é recusada, inclusive a antiga carga manual. Prepare uma nova instância demo
ou faça avaliação administrativa específica da instalação existente. Reinícios
com o marcador não reaplicam INSERTs, não resetam senhas, não reativam contas
e preservam dados editados/removidos. `false` não abre conexão nem aplica seed.
Nunca habilite modo demo em produção.

Credenciais **exclusivamente de fixture local**, senha `JoinUpDemo#2026`:

| Perfil | E-mail | Navegação inicial |
| --- | --- | --- |
| Participante | ana.demo@joinup.test | Comunidades → Festival Aurora → Chat/Feed |
| Organizador | aurora.demo@joinup.test | Meus eventos → editar → enviar imagem |
| Suporte | suporte1.demo@joinup.test | Meu painel → chamados |

Outras seis contas demonstrativas constam no seed. Inativos não autenticam.
O seed permanece integralmente igual ao v0.2 aprovado.

Abra [início local](http://127.0.0.1:8080/) ou
[contas](http://127.0.0.1:8080/conta.html). Cadastros públicos de participante e
organizador; novos suportes somente em sessão de suporte. A interface oferece
login por perfil, dados/sessão atual, edição/desativação própria e, para suporte,
cadastro de suporte e consulta/reativação de contas. Ctrl+C encerra servidor/executor.
Alternativa: `mvn compile exec:java`.

Em [eventos](http://127.0.0.1:8080/eventos.html), visitantes exploram os quatro
eventos demo, filtram e consultam detalhes, lineup e catálogo. Organizadores
acessam “Meus eventos”, criam evento completo e administram informações,
artistas e categorias. A criação grava evento, lineup, categorias e comunidade
em uma transação única; erros revertem todas as etapas. Categorias com qualquer
venda histórica não são editáveis; todas as mutações são bloqueadas no início
do evento, inclusive. Eventos futuros com vendas permitem edição geral e
reagendamento estritamente futuro, mantendo compras e ingressos históricos.

Datas HTTP exigem ISO 8601 com offset. No JDBC de eventos, `DATETIME` é gravado
e lido como `LocalDateTime` **UTC**, convertido para `Instant`, com precisão de
segundos; o navegador converte seu horário local ao enviar e exibir. Configure
a sessão MySQL em UTC, incluindo na URL `forceConnectionTimeZoneToSession=true`.
Criação exige data futura como validação técnica documentada na Entrega 02.
Na criação JSON, imagem aceita referência vazia ou `/assets/evento-placeholder.svg`.
Depois de criar, o organizador dono pode enviar PNG/JPEG/WebP (até 1 MiB) no
formulário de gestão do evento futuro. O upload vincula a imagem automaticamente,
sem alterar compras/ingressos. PATCH preserva a imagem omitida, recusa `null`
explícito e permite limpar com `"imagem":""`; referências de outro evento
não são aceitas. Comunidades públicas usam a imagem atual do evento.

`GET /api/health` informa apenas liveness HTTP: `{"dados":{"status":"UP"}}`.
Health e estáticos funcionam sem conexão de banco; operações de conta exigem MySQL.

## Configuração e segurança

Precedência: propriedade JVM `-D` → ambiente → padrão; snapshot imutável do
Singleton. `.env.example` é referência, não carregado automaticamente.

| Ambiente | Propriedade JVM | Padrão / limite |
| --- | --- | --- |
| JOINUP_HTTP_HOST | joinup.http.host | 127.0.0.1 |
| JOINUP_HTTP_PORT | joinup.http.port | 8080; 1–65535 |
| JOINUP_HTTP_THREADS | joinup.http.threads | 8; 1–64, fila 128 |
| JOINUP_MAX_REQUEST_BYTES | joinup.maxRequestBytes | 65536; limite global preservado |
| JOINUP_DEMO | joinup.demo | false; opt-in automático local |
| JOINUP_ENV | joinup.environment | local; demo exige valor demo |
| JOINUP_UPLOAD_DIR | joinup.upload.dir | ~/.joinup/uploads; fora do repositório/webroot |
| JOINUP_JDBC_URL | joinup.jdbc.url | jdbc:mysql://127.0.0.1:3306/joinup_poo?connectionTimeZone=UTC&connectTimeout=5000&socketTimeout=10000 |
| JOINUP_DB_USER | joinup.db.user | obrigatório ao conectar |
| JOINUP_DB_PASSWORD | joinup.db.password | segredo no ambiente |
| JOINUP_PUBLIC_ORIGIN | joinup.public.origin | vazio: http://127.0.0.1:porta efetiva |
| JOINUP_PBKDF2_ITERATIONS | joinup.pbkdf2.iterations | 600000; 600000–2000000 |
| JOINUP_SESSION_SECONDS | joinup.session.seconds | 28800 (8h); 60–86400 |

Para HTTPS por proxy, configure `JOINUP_PUBLIC_ORIGIN=https://seu-host` (sem
barra/caminho/query). O servidor exige Origin exato nas mutações; se ausente,
Referer da mesma origem. Sem ambos, origem null/distinta ou Sec-Fetch-Site
incompatível: **403 CSRF_NEGADO**. Clientes de API devem enviar Origin explícito
e Content-Type application/json para corpos JSON. Sem CORS indiscriminado; Host
e X-Forwarded-* não decidem a origem. API e frontend devem compartilhar origem.

Cookie `joinup_sessao`: HttpOnly, SameSite=Lax, Path=/ e Max-Age, Secure sob
origem HTTPS configurada. HTTP sem Secure é limitado a **127.0.0.1 local**.
Nunca guardar token no JavaScript/localStorage nem devolvê-lo em JSON: ele vai
somente pelo cookie; no MySQL persiste apenas SHA-256. Sessão expirada/revogada
e conta inativa são verificadas em todos os endpoints protegidos. Novas senhas
usam PBKDF2-SHA256/salt individual; fixture de 180 mil iterações permanece legível.

Rotas dinâmicas correspondem segmento a segmento, com parâmetros de path/query
imutáveis, estáticos prioritários e ambiguidades recusadas. 404 para recurso
desconhecido; 405 com Allow para método inválido. Erros JSON não expõem JDBC,
senhas ou hashes. IDs positivos, campos extras/IDOR, JSON duplicado/coerções,
traversal e barras codificadas são rejeitados; limite de corpo 64 KiB inclusive
chunked. Datas de nascimento usam ISO `yyyy-MM-dd`/LocalDate com Jackson JSR-310.
Sessões/auditoria/desativação usam UTC no SQL.

## Endpoints da Entrega 01

| Método e caminho | Permissão |
| --- | --- |
| POST /api/participantes, /api/organizadores | Público, com CSRF e JSON |
| POST /api/auth/participantes/login, /api/auth/organizadores/login, /api/auth/suporte/login | Público, com CSRF e JSON |
| GET /api/auth/me | Autenticado e ativo |
| POST /api/auth/logout | Autenticado e ativo, CSRF |
| GET/PATCH /api/me/perfil | Participante/organizador titular; PATCH com CSRF/JSON |
| POST /api/me/desativacao | Participante/organizador titular, CSRF |
| POST /api/suporte/usuarios | Suporte, CSRF/JSON |
| GET /api/suporte/contas?tipo=participante\|organizador&situacao=inativa | Suporte; pagina/tamanho opcionais (20, até 100) |
| POST /api/suporte/contas/{tipo}/{id}/reativacao | Suporte, CSRF; tipo participante/organizador |

PATCH permite nome/e-mail/nova_senha e, para organizador, CNPJ. Não permite
id/perfil/CPF/data_nascimento. Login retorna id/perfil/nome. Respostas de perfil
omitem senha/hash; listagem de suporte também omite CPF/CNPJ/nascimento.
Reativar conta já ativa retorna 409 CONTA_JA_ATIVA; não duplica auditoria.
Logout/desativação/reativação aceitam corpo vazio ou `{}`.

Desativação mantém histórico e revoga **todas** as sessões na mesma transação.
Para participante, cancela também todos os anúncios disponíveis sem trocar o
dono do ingresso. Reativação exige suporte, mantém PK e grava autor/timestamp;
anúncios cancelados permanecem cancelados. Suporte não possui ciclo de desativação.

## Testes e integração real

A Entrega 06 acrescenta `POST /api/arquivos` multipart e leitura autorizada
em `GET /api/arquivos/{id}`. PNG/JPEG/WebP são decodificados, limitados a
4096 px por dimensão / 4 milhões de pixels, regravados como PNG sem metadados
e guardados fora da webroot. Arquivos sociais exigem sessão ativa, filiação e
ingresso ativo/utilizado ou organizador dono; conhecer a URL não concede acesso.
Feed e chat permitem texto e/ou imagem; ambos vazios são recusados. A imagem
pendente pertence ao ator e contexto, expira para vinculação após 24h e só pode
ser consumida uma vez. Não há retry automático em POST.

Contratos e limites: [API 06](docs/api/ARQUIVOS_HTTP_06.md);
[resultados, migração e roteiro](docs/execucao/ENTREGA_06_RESULTADOS.md).
**468 testes**: 167 Surefire + 301 Failsafe/MySQL, preservando os 397 anteriores.
Os testes adicionais usam segunda instância MySQL 8.4 vazia para bootstrap demo.

A Entrega 05 disponibiliza `/painel.html` com indicadores reais por perfil e
`/painel.html?aba=chamados` com abertura/atendimento/histórico. Solicitantes
Resolvem ou Retornam somente em `aguardando_usuario`; fechado é terminal.
Qualquer suporte responde a chamado associado a outro, preservando autoria
individual. Contagens de comunidade usam acesso efetivo; valores de vendas
são históricos de compras confirmadas, sem prometer recebimento financeiro.

Contratos, limites e comandos: [API 05](docs/api/SUPORTE_PAINEIS_HTTP_05.md),
[resultados 05](docs/execucao/ENTREGA_05_RESULTADOS.md) e
[UML 05](docs/diagramas/SUPORTE_PAINEIS_05.puml). O perfil mysql-it inclui
`SuporteMySqlIT`, `PaineisMySqlIT` e todas as integrações anteriores, inclusive
os 65 cenários `ComunidadeMySqlIT` da correção 04.1. Execute a suíte num banco
isolado sem sessões/ações de navegador simultâneas; a sanidade visual deve ser
feita separadamente, pois a suíte de fundação exige a fixture original intacta.

A Entrega 03 acrescenta compra direta e revenda com **pagamento local simulado**.
Abra `/eventos.html` para comprar e `/ingressos.html` para ingressos, histórico,
anúncios e revendas. Apenas participantes ativos podem acessar as APIs de
revenda. Anunciar preserva titularidade; comprar transfere o ingresso e, após a
última venda elegível, inativa a associação do vendedor à comunidade. A entrada
do comprador continua manual. Nenhuma cobrança/transferência bancária ocorre.

Roteiro com Ana/Bruno do seed, capturas e ordem de locks:
[resultados 03](docs/execucao/ENTREGA_03_RESULTADOS.md). Contratos completos:
[compras/revendas](docs/api/COMPRAS_REVENDA_HTTP_03.md). Classes reais:
[PlantUML](docs/diagramas/COMPRAS_REVENDA_03.puml).
Regressão ampliada para **236 testes** (127 Surefire + 109 MySQL/Failsafe), com
os **157 anteriores preservados**. Exige **MySQL 8.4** real no perfil mysql-it.

```powershell
mvn clean verify             # Unitários/HTTP; não atesta banco
# Somente DUAS instâncias isoladas com joinup_it e administrador de fixtures:
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
$env:JOINUP_IT_ADMIN_USER = 'administrador_teste'
# JOINUP_IT_ADMIN_PASSWORD, JOINUP_DB_USER=joinup_it e senhas no ambiente
$env:JOINUP_DEMO = 'false'
$env:JOINUP_UPLOAD_DIR = Join-Path $env:TEMP 'joinup-midia-it06' # começar vazio
$env:JOINUP_DEMO_IT_JDBC_URL = 'jdbc:mysql://127.0.0.1:3308/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
# JOINUP_DEMO_IT_DB_PASSWORD no ambiente; segunda instância DEVE começar vazia
mvn -Pmysql-it clean verify  # Inclui MySQL real, falha se não disponível
```

Perfil mysql-it aplica/reexecuta schema/seed, verifica 22 tabelas/FKs/CHECKs/
índices, hashes persistidos e linhas/timestamps idênticos. Exige fixtures sem
dados de outra origem. Restringe joinup_it a DML, executa HTTP real, permissões,
CSRF, sessões, IDOR, rollback com triggers de teste e concorrência. **Não usar
esse perfil contra instalação com dados reais.** Suíte limpa somente suas PKs
criadas; nenhum DROP DATABASE/TRUNCATE nem remoção de constraints.

GitHub Actions usa dois service containers `mysql:8.4.0`, Java 21 e `-Pmysql-it clean
verify`. Evidências ficam em target/surefire-reports, target/failsafe-reports e
target/mysql-validation, publicadas como artefato. Os 52 testes da fundação
foram preservados. Resultados finais estão nos documentos abaixo.

A Entrega 02 mantém os **109 testes anteriores** e amplia para **157 testes**
(111 Surefire + 46 Failsafe/MySQL), incluindo coordenação Facade, autorização,
UTC, histórico de venda por três fontes, rollback em três etapas e concorrência
observada sob lock real. Capturas e roteiro de navegador complementam a suíte.

- [Resultados da Entrega 02](docs/execucao/ENTREGA_02_RESULTADOS.md).
- [Contrato implementado de eventos](docs/api/EVENTOS_HTTP_02.md).
- [GoF implementados](docs/diagramas/CLASSES_GOF_v0.1.puml) e [fragmento de domínio RA2](docs/diagramas/EVENTOS_DOMINIO_02.puml).
- [Resultados da Entrega 01](docs/execucao/ENTREGA_01_RESULTADOS.md).
- [Validação real MySQL](docs/banco/VALIDACAO_INTEGRACAO_01.md).
- [Correção técnica mínima CHECK/FK](docs/banco/CORRECAO_DDL_01.md).
- [Contratos baseline](docs/api/CONTRATOS_HTTP_v0.1.md) e [prompt](docs/prompts/CODEX_01_USUARIOS_AUTENTICACAO.md).

SQLs IF NOT EXISTS não migram instalações preexistentes. A correção CHECK/FK
da Entrega 01 deve ser aplicada em instância nova de desenvolvimento; migração
de outra instalação requer análise própria. Publicação HTTPS, proteção de borda,
rate limiting e limpeza operacional de sessões precisam de validação de implantação.
