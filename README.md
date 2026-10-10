# JoinUp-POO

Java 21 puro com HttpServer do JDK, JDBC/MySQL 8, Jackson e JUnit. Entregas 00–02:
contas/autenticação, sessão, desativação/reativação e eventos com lineup,
categorias e comunidade automática. Os quatro GoF estão em fluxos executáveis:
Singleton, Factory Method, Template Method e Facade transacional.

Interfaces em `/conta.html` e `/eventos.html`, com identidade visual escura/roxa
próxima do JoinUp PHP. Compra, conteúdo de comunidades, revendas, tickets,
uploads e dashboards finais pertencem às próximas entregas. O projeto usa banco próprio
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

Para avaliar a Entrega 02 antes do merge, selecione a branch do PR
`feat/02-eventos-facade`. O merge depende da auditoria do Tech Lead.
Prepare instância **isolada** de MySQL, aplique os SQLs e configure a conta DML:

```powershell
mysql --host=127.0.0.1 --port=3306 --user=administrador -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/schema_v0.2.sql"
mysql --host=127.0.0.1 --port=3306 --user=administrador -p --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/seed_demo_v0.2.sql"
# Conferir erros/warnings, não somente o exit code de SOURCE.
$env:JOINUP_DB_USER = 'joinup_app'
# Configure JOINUP_DB_PASSWORD no ambiente, sem publicar credenciais.
java -jar target/joinup-poo.jar
```

A conta da aplicação precisa somente de SELECT/INSERT/UPDATE/DELETE em
joinup_poo. DDL/seed são tarefas separadas de administração, nunca executadas
automaticamente no bootstrap. Seed contém credenciais demonstrativas e deve
ser usado **somente em ambiente de demonstração/teste local**; consulte o SQL
para e-mails/senha. JOINUP_DEMO continua reservado e não executa carga automática.

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
Imagem aceita somente referência vazia ou `/assets/evento-placeholder.svg`;
não há upload nesta etapa. PATCH preserva campos omitidos, recusa `null`
explícito e limpa a referência de imagem com `"imagem":""`.

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
| JOINUP_DEMO | joinup.demo | false; reservado |
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

```powershell
mvn clean verify             # Unitários/HTTP; não atesta banco
# Somente instância isolada com joinup_it e administrador de fixtures:
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
$env:JOINUP_IT_ADMIN_USER = 'administrador_teste'
# JOINUP_IT_ADMIN_PASSWORD, JOINUP_DB_USER=joinup_it e senhas no ambiente
mvn -Pmysql-it clean verify  # Inclui MySQL real, falha se não disponível
```

Perfil mysql-it aplica/reexecuta schema/seed, verifica 22 tabelas/FKs/CHECKs/
índices, hashes persistidos e linhas/timestamps idênticos. Exige fixtures sem
dados de outra origem. Restringe joinup_it a DML, executa HTTP real, permissões,
CSRF, sessões, IDOR, rollback com triggers de teste e concorrência. **Não usar
esse perfil contra instalação com dados reais.** Suíte limpa somente suas PKs
criadas; nenhum DROP DATABASE/TRUNCATE nem remoção de constraints.

GitHub Actions usa service container `mysql:8.4.0`, Java 21 e `-Pmysql-it clean
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
