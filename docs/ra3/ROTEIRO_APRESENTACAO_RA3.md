# JoinUp-POO — Roteiro de apresentação e arguição RA3

Material de apoio, separado do [relatório exigido](RELATORIO_ACADEMICO_RA3.md).
Sugestão de **5–8 minutos**, ajustável à orientação efetiva do professor; essa
duração não é requisito do enunciado transcrito. Apresentação/arguição em
**13/11/2026**; toda a equipe deve compreender o projeto e estar apta a responder.

## Preparação antes de apresentar

- Preencher identificação da equipe e validar as reflexões do relatório.
- Ter Git, JDK **21**, Maven 3.9.x e servidor/cliente MySQL disponíveis. Conferir
  `mvn -version`: o Java do Maven deve ser 21. Apenas Workbench não basta.
- Usar a revisão aprovada do repositório JoinUp-POO, registrando `git rev-parse HEAD`.
  A base desta documentação é `a835c2e41b10606f75d425aba2bc4fbea9f11812`, após PR #15.
- Preparar administrativamente `joinup_poo_ra3` e suas **sete tabelas** com o
  [schema manual](../../src/main/resources/db/ra3/schema_ra3.sql); conferir o
  [SQL de inspeção](../../src/main/resources/db/ra3/verificar_schema_ra3.sql).
  Seguir o [guia de execução](EXECUCAO_RA3.md) para conta DML e configuração.
- Usar uma instância de demonstração RA3 diferente da instância descartável
  dos testes. O nome do catálogo permanece `joinup_poo_ra3` em ambas; a separação
  ocorre por instância/porta. Não executar a suíte de integração na demonstração.
- Configurar `JOINUP_RA3_JDBC_URL`, `JOINUP_RA3_DB_USER` e
  `JOINUP_RA3_DB_PASSWORD` somente no ambiente. `.env` não é carregado. A URL
  deve apontar exclusivamente a `joinup_poo_ra3`; não incluir credenciais na URL.
  O guia mostra leitura da senha sem digitá-la em texto aberto no histórico.
- Não definir `JOINUP_RA3_IT_DATABASE` na sessão da demonstração. Ela é exclusiva
  da suíte descartável, conforme o guia.

Na raiz do checkout, com configuração já feita:

```powershell
java -version
mvn -version
git rev-parse HEAD
mvn -Pra3 compile exec:java '-Dexec.args=--diagnostico'
# Esperado: RA3_CONEXAO_OK; termina sem ler entrada e sem inserir registros.
mvn -Pra3 compile exec:java
```

O diagnóstico verifica conexão e catálogo; **não certifica sozinho as sete
tabelas ou suas constraints**. Conferir também os metadados pelo SQL de inspeção.
Abrir previamente [UML SVG](DIAGRAMA_CLASSES_RA3.svg),
[persistence.xml](../../src/main/resources/META-INF/persistence.xml),
[Evento.java](../../src/main/java/br/com/joinup/ra3/modelo/Evento.java) e
[Comunidade.java](../../src/main/java/br/com/joinup/ra3/modelo/Comunidade.java).

## Sequência sugerida

| Tempo indicativo | Ação e explicação |
| --- | --- |
| 0:00–0:45 | Apresentar o propósito, apontar as sete entidades no UML e explicar que o módulo estuda ORM no domínio de eventos. |
| 0:45–1:30 | Mostrar Organizador 1 → 0..* Evento; Evento 1 → 0..* TipoIngresso; Evento 1 → 0..1 Comunidade. Apontar a FK no filho e o UNIQUE de comunidade. |
| 1:30–2:15 | Mostrar as sete entradas de persistence.xml, provider EclipseLink, RESOURCE_LOCAL e DDL none. Em código, apontar @Entity, @Id/IDENTITY, @ManyToOne, @OneToOne e @JoinColumn. |
| 2:15–4:15 | Iniciar `mvn -Pra3 compile exec:java`; cadastrar gênero, organizador e evento, anotando e consultando os IDs retornados. |
| 4:15–5:15 | Atualizar evento, consultar, sair e abrir outra JVM; reconsultar o mesmo ID e mostrar o nome persistido. |
| 5:15–6:30 | Criar/excluir um tipo de ingresso folha; consultar para comprovar ausência. Tentar excluir o organizador que ainda possui o evento; mostrar a recusa. |
| 6:30–7:30 | Explicar uma dificuldade comprovada, como UTC ou proteção de vínculos, e o aprendizado. Reservar o tempo restante à arguição. |

## Demonstração pelo terminal, sem IDs fixos

Os números dos menus são opções, não IDs. Usar um sufixo de sessão escolhido no
ensaio, por exemplo `A04`, e substituí-lo em nova execução se houver duplicidade.
Os documentos e e-mails abaixo são exclusivamente fictícios.

1. Menu principal **1 — Gêneros**, ação **1 — Cadastrar**: nome `Demo A04 Rock`.
   Anotar o ID exibido em `Cadastrado. ID: ...` como **G**. Ação **2**, informar G,
   conferir o nome. **0** volta ao menu principal.
2. Menu **2 — Organizadores**, ação **1**: nome `Organizador Ficticio A04`,
   e-mail `a04@demo.test`, CNPJ `000000000000000004` (18 caracteres fictícios).
   Anotar o ID como **O**, consultar com ação **2**, voltar com **0**. Se já
   existir e-mail/documento, consultar ou escolher outros dados fictícios únicos.
3. Menu **4 — Eventos**, ação **1**, informar nesta ordem: `Evento Demo A04`,
   `2030-11-12T18:30:00` (UTC), `Demonstracao academica`, `Local ficticio`,
   `Cidade ficticia`, `SP`, `00000000`, **O**, **G**. As listagens de pais
   aparecem antes dos respectivos IDs. Anotar o novo ID como **E** e consultar.
4. Ainda em Eventos, ação **4**, ID E, nome `Evento Demo A04 Atualizado`, imagem
   vazia (Enter). Ação **2**, E: conferir nome e referências. **0** volta,
   **0** encerra. Reiniciar com `mvn -Pra3 compile exec:java`; menu **4**,
   ação **2**, E: conferir o nome atualizado após reinício.
5. Voltar; menu **5 — Tipos de ingresso**, ação **1**: `Pista Demo A04`, `12.50`,
   `10`, E. Anotar **T**. Ação **2**, T; ação **5**, T, digitar exatamente `SIM`.
   Consultar T novamente: esperado `ID não encontrado`. **0** volta.
6. Menu **2 — Organizadores**, ação **5**, O, `SIM`: esperado
   `Exclusão recusada: remova primeiro os registros dependentes...`, porque E
   ainda referencia O. Consultar O e E para mostrar que foram preservados.
7. Encerrar com **0** para voltar e **0** para sair. Guardar G/O/E para as
   perguntas. Após a apresentação, remover somente os registros próprios,
   pelo menu e com confirmação: evento E, organizador O, gênero G, desde que
   não tenham outros dependentes. Não realizar limpeza geral.

Para recuperar um ID esquecido: submenu da entidade, ação **3 — Listar**,
página **1**; consultar páginas seguintes se necessário, com 20 registros por
página em ordem de ID. Não reutilizar IDs numéricos das evidências históricas.
Em qualquer campo de operação, `/cancelar` volta sem enviar mutações. `0`
continua válido para preço e quantidades; nos menus volta ou encerra.

Comunidade, participante e postagem podem ser demonstrados se houver tempo:
seguir a ordem dos campos no [guia](EXECUCAO_RA3.md). A evidência histórica
[sessao-cli-03.txt](evidencias/sessao-cli-03.txt) cobre as sete entidades e o
reinício; é registro de execução anterior, não de um ensaio da equipe.

## Perguntas e respostas para arguição

| Pergunta | Resposta vinculada ao projeto |
| --- | --- |
| O que é ORM? | É o mapeamento entre objetos/classes e tabelas/colunas/relações. Aqui as sete classes de modelo são persistidas em sete tabelas por EclipseLink. |
| JPA, EclipseLink e JDBC são a mesma coisa? | JPA/Jakarta Persistence define contratos e anotações; EclipseLink é a implementação usada; JDBC é a API de acesso ao banco utilizada pelo provider/driver. A aplicação RA2 usa JDBC diretamente. |
| O que faz @Entity? | Declara a classe como entidade persistente. As sete são listadas explicitamente em persistence.xml; auxiliares e conversor não são entidades. |
| @Id e @GeneratedValue? | @Id marca a chave do objeto; IDENTITY utiliza a identidade AUTO_INCREMENT do MySQL. O ID é Integer e não é digitado no cadastro. |
| O que significa @JoinColumn? | Nomeia a FK da associação no lado proprietário. Evento.organizador usa id_organizador, obrigatório. |
| Quais as cardinalidades? | Um organizador/gênero pode ter zero ou muitos eventos; um evento pode ter zero ou muitos tipos e zero ou uma comunidade; participante/comunidade podem ter zero ou muitas postagens. Cada filho possui exatamente um pai de cada relação. |
| Quem é o lado proprietário? | É o atributo que mapeia a FK: por exemplo TipoIngresso.evento. Evento.tiposIngressos usa mappedBy="evento" e é inverso. Comunidade.evento possui a FK UNIQUE; Evento não possui atributo comunidade. |
| Por que um evento pode não ter comunidade? | A FK é obrigatória em Comunidade, não em Evento. O UNIQUE impede duas comunidades para o mesmo evento; não obriga criar a primeira. |
| EntityManagerFactory e EntityManager? | AplicacaoRa3 mantém uma fábrica por execução. UnidadeTrabalhoRa3 cria e fecha um EntityManager por operação, delimitando seu contexto de persistência. |
| Como criar, consultar e excluir? | ServicoCrudRa3 usa persist para cadastrar, find por classe/ID para consultar e remove numa entidade gerenciada para excluir, após confirmação e verificação de vínculos. Listagens usam JPQL parametrizada. |
| Como a atualização chega ao banco? | O serviço busca a entidade gerenciada e chama mutadores existentes. O provider detecta as mudanças (dirty checking) e sincroniza na transação; não há UPDATE nativo para o CRUD. |
| begin, commit, rollback e flush? | Escritas iniciam transação com begin; commit confirma; rollback desfaz em falha. flush pode enviar SQL antes do commit, mas não confirma a transação. Há teste de reversão após flush. |
| Por que constraints/FKs se Java já valida? | O banco assegura integridade também sob concorrência e acessos externos. FK exige pai existente, UNIQUE limita comunidade por evento, e CHECKs restringem valores/quantidades e ativo. |
| Como funciona a exclusão? | Folhas são removidas fisicamente com SIM. Pais com filhos são recusados com lock e consulta de dependentes; a estratégia protege inclusive FKs SQL com CASCADE. A modalidade física é escolha do projeto. |
| Por que um conversor UTC? | Foi observado deslocamento de três horas na conversão padrão. DataHoraUtcConverter usa Timestamp.from e ZoneOffset.UTC; conexão/sessão usam UTC e testes com JDBC conferem o DATETIME físico. |
| Como foi preservada a RA2? | Pacotes, perfil Maven, recursos e catálogo próprios para RA3. O build padrão exclui RA3/JPA e continua iniciando Main web/JDBC. O workflow histórico verifica a RA2 separadamente. |
| Existe herança de usuário ou cascata JPA? | As sete entidades RA3 não possuem essa herança. Não há cascade PERSIST/REMOVE nem orphanRemoval. Os dois CASCADEs SQL históricos são tratados pela proteção do serviço. |
| As datas e tipos Java são idênticos aos do SQL? | Há adaptação: nascimentoIso é String em CHAR(14) e o getter oferece LocalDate; estado é String validada em Java e ENUM no SQL; dinheiro é BigDecimal/DECIMAL(8,2). |

## Participação e contingência

Preencher uma divisão flexível conforme a equipe real: **[integrantes: domínio/UML]**,
**[integrantes: mapeamentos/transações]**, **[integrantes: terminal/reflexões]**.
Os assuntos podem ser combinados e revezados; não pressupõem três integrantes.
Todos devem conhecer os demais assuntos e participar da arguição.

Antes da sessão, executar diagnóstico, abrir o SVG e conferir os arquivos/fontes
localmente. Se houver falha, verificar MySQL iniciado, porta, variáveis,
permissões e URL exclusiva RA3. Não redirecionar para o banco RA2. Depois de
restabelecer a conexão, encerrar o menu e reiniciar com
`mvn -Pra3 compile exec:java`; consultar os IDs já anotados.

Se o ambiente não se recuperar no tempo disponível, apresentar o UML e explicar
o fluxo usando o texto versionado da [sessão CLI 03](evidencias/sessao-cli-03.txt)
e os [logs de validação](evidencias/validacao-03.txt), identificando-os como
evidências históricas. Não há screenshots novos nem ensaio presencial comprovado
por esta entrega. **Não executar DROP, reset ou limpeza ampla perto da apresentação.**
