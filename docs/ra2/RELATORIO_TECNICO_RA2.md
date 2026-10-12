# JoinUp-POO — Relatório técnico RA2

**Disciplina:** Design de Software, Avaliação Somativa RA2.

**Repositório:** [ericjs-code/JoinUp-POO](https://github.com/ericjs-code/JoinUp-POO).

**Equipe informada pelo PO em 12/10/2026:** Eric Julio da Silva; Matheus Martins;
João Pedro Oliveira; Pedro Henrique Rebechi; Kaun de Moraes de Liz.

**Instituição:** PUCPR. **Turma:** 4A noite. **Docente:** Andeza de Sousa Vieira
(grafia informada pelo PO). São cinco integrantes informados; não foi inventado
um sexto nome. Duração autorizada e ensaio/arguição coletiva: **PENDENTE DE PO**.

**Entrega prevista nos requisitos:** 15/10/2026; apresentação: 16/10/2026.

**Revisão documental:** 12/10/2026, America/Sao_Paulo.

**Base integral examinada:** `7efe29c1529a71e3c1a82a52716a5a32947a61fe`, `main` após PR #8 (Entrega 06). Branch documental: `docs/07-ra2-relatorio-uml`.

**Atualização da Entrega 08:** homologação técnica executada sobre
`011ecb2dcdd8f9457d2e1c5eeceb00edee018ffd`. Java, testes, SQL, recursos,
`pom.xml` e workflow são idênticos à base de código acima. Os resultados
históricos das seções seguintes conservam sua revisão de origem; os resultados
novos (468 testes locais, HTTP, navegador e reinício) estão no
[RELATORIO_HOMOLOGACAO_FINAL.md](RELATORIO_HOMOLOGACAO_FINAL.md). A matriz atual
distingue 42 casos executados, principalmente automáticos, de três casos sem
execução integral. Identificação preenchida não comprova participação em ensaio.

**Atualização da Entrega 09:** base documental `80a36c457ec4e46865ee456f388cb607297c3811`,
`main` após PR #10 (Entrega 08), branch `docs/09-conformidade-aulas-uml`.
As novas visões de pacotes, componentes, implantação e cardinalidades foram
confrontadas com essa base. O código continua idêntico ao inventário da 07;
Java, testes, SQL, frontend, `pom.xml` e workflow foram preservados. Comandos,
rastreabilidade, inspeção visual e limites estão em
[VALIDACAO_CONFORMIDADE_AULAS_UML.md](VALIDACAO_CONFORMIDADE_AULAS_UML.md).

O **enunciado oficial RA2** (PDF de 3 páginas) e a apresentação **Padrões de Projetos GoF — Introdução** (22 slides) foram disponibilizados ao Tech Lead pelo PO em **12/10/2026**. O enunciado exige **no mínimo quatro GoF**, análise contextual de todos os demais padrões estudados, UML coerente, código executável, reflexão e arguição coletiva; a rubrica tem **4,0 pontos para GoF; 2,0 para código; 2,0 para apresentação; 1,0 para UML; 1,0 para relatório**. A apresentação da disciplina inclui os **23 padrões clássicos** (slides 12–19), conforme confirmação do PO, e este relatório analisa os **19 não usados** no §3.5. A duração da apresentação não está fixada no PDF; integrar nomes/equipe e confirmar tempo com a professora. Esta análise não garante pontuação; ver [confronto com os documentos oficiais](CONFORMIDADE_ENUNCIADO_OFICIAL.md).

## 1. Problema, objetivos e atores

O JoinUp-POO integra descoberta de eventos, organização, compra simulada de ingressos, revenda, comunidades e atendimento. O problema técnico é manter essas atividades consistentes: uma compra não pode emitir mais ingressos que o estoque; a revenda deve transferir titularidade uma única vez; acesso social depende de filiação e ingresso atual; respostas de atendimento precisam conservar sua autoria.

O projeto reimplementa o JoinUp em Java com banco independente `joinup_poo`. O PHP original é referência de UX e requisitos herdados, não prova de implementação Java nem fonte de dados importados. Os objetivos funcionais derivam de [ESPECIFICACAO_FUNCIONAL_v0.1.md](../requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md) e dos sete adendos versionados (fontes S1).

| Ator | Responsabilidades implementadas | Fronteira de permissão |
| --- | --- | --- |
| Visitante | Explorar eventos, catálogos e metadados de comunidades; abrir cadastro/login | Não recebe anúncios de revenda nem conteúdo social privado |
| Participante | Cadastro/login, perfil, compras, ingressos atuais, revendas, filiação manual, feed/chat e chamados próprios | Conta ativa; titularidade; para social, membro ativo e ingresso ativo/utilizado |
| Organizador | Cadastro/login, criação/edição de eventos próprios futuros, categorias, mídia de evento, comunidade própria, painel e chamados | Conta ativa e propriedade; não cria posts/comentários como participante |
| Suporte | Login, cadastro de outro suporte, reativação de participante/organizador, atendimento e painel global | Associação ao chamado é acompanhamento; qualquer suporte autorizado pode responder |

Pagamento, saque e notificações externas continuam simulados/informativos. Não há gateway financeiro, cobrança efetiva ou push/e-mail. Não existe papel adicional de suporte administrador. Fontes de execução: S3–S9.

## 2. Stack e arquitetura efetiva

O [ADR-001](../arquitetura/ADR-001-STACK-JAVA-PURO.md) delimita Java puro: JDK e bibliotecas pontuais, sem Spring/Javalin, ORM, servlet container ou contêiner de injeção. O [pom.xml](../../pom.xml) da base fixa Java 21, Jackson Databind/JSR-310 2.18.3, MySQL Connector/J 8.4.0, JUnit Jupiter 5.11.4 e decoder ImageIO WebP TwelveMonkeys 3.12.0. Maven compila/testa e Shade empacota `joinup-poo.jar`; HTML/CSS/JS nativos no classpath fazem requisições na mesma origem. CI usa MySQL **8.4.0** (S2, S10).

| Camada/módulo real | Classes centrais | Responsabilidade |
| --- | --- | --- |
| Entrada/composição | `Main`, `bootstrap.ApplicationBootstrap` | Configuração, montagem manual, ciclo de vida HTTP e fechamento |
| HTTP comum | `Router`, `RouteParameters`, `RequestPath`, `RequestBody`, `JsonCodec`, `HttpResponses`, `StaticFileHandler`, `HealthHandler` | Correspondência por segmentos, transporte, erros e recursos públicos |
| Configuração/autenticação | `ConfiguracaoSistema`, `AutenticacaoTemplate`, `GerenciadorSessao`, `SegurancaHttp`, `SenhasPbkdf2` | Configuração imutável, login, sessão, origem e hash |
| Usuários | creators, `ContaService`, repositórios por perfil | Produtos de cadastro, atualização, desativação e reativação |
| Eventos | `EventoFacade` e quatro subsistemas | Criação integrada e edição sob titularidade/tempo |
| Comércio | `CompraService`, `RevendaService` | Estoque, emissão, histórico, transferência e saída comunitária |
| Comunidades/social | `ComunidadeSocialService`, `ComunidadeSocialRepositoryJdbc` | Descoberta pública, acesso privado, filiação, chat, posts e interações |
| Suporte/painéis | `TicketService`, `StatusTicket`, `PainelService`, repositórios JDBC | Estados/autoria de chamados e projeções por perfil |
| Mídia/demo | `ArquivoService`, `ArmazenamentoLocal`, `MultipartImagem`, `ImagemSegura`, `DemoRunner`, `SqlDemo` | Upload tipado, autorização binária e seed opt-in |
| Persistência transversal | `ConnectionFactory`, `ExecutorTransacional` | Uma conexão nova por operação; commit, rollback e fechamento |

`Main` obtém o Singleton; o bootstrap constrói repositórios, serviços e rotas por construtores. O social recebe `ArquivoService` uma única vez por `configurarArquivos`, pois a mídia também consulta a autorização social: existe dependência mútua explícita, não injeção automática. `DemoRunner` executa antes da alocação do servidor quando habilitado. `HttpServer` registra o roteador em `/`; o executor tem threads configuráveis (padrão 8), fila 128 e `CallerRunsPolicy`. Isso limita a fila, mas não é benchmark nem proteção completa contra sobrecarga. Fontes S2, S8.

Fluxo concreto: `POST /api/organizador/eventos` → `Router` → `EventoRoutes` (sessão, CSRF, JSON) → `EventoFacade.criarEvento` → `ExecutorTransacional` → `GerenciadorSessao` e subsistemas → repositórios JDBC → MySQL → `EventoView` → resposta HTTP. O adaptador HTTP não executa SQL. Os repositórios recebem a **mesma Connection**, não iniciam uma transação autônoma por etapa. Fontes S2, S5.

O executor abre conexão com `DriverManager`, seleciona `READ_COMMITTED`, desliga autocommit e confirma apenas se a operação inteira terminar. `SQLException` ou `RuntimeException` provoca rollback; `try-with-resources` fecha a conexão. Deadlock/timeout retorna 409; falha SQL genérica retorna 503 sem publicar detalhes. Não há repetição automática da mutação. `PainelService.consultar` muda a leitura para `REPEATABLE_READ` para coerência entre consultas da mesma resposta. Não existe pool JDBC global nem transação distribuída com filesystem. Fontes S2, S7, S8.

## 3. Comprovação dos quatro padrões GoF

São reivindicados **somente Singleton, Factory Method, Template Method e Facade**. Repository, DTO, camadas, composição manual e a função transacional são estruturas auxiliares, não novos GoF da avaliação. Os trechos abaixo são extratos literais pequenos da base `7efe29c`; o inventário associa nomes/assinaturas aos arquivos reais.

### 3.1 Singleton — configuração do processo

**Intenção:** manter um snapshot único de parâmetros estáveis. **Participante:** `ConfiguracaoSistema`, classe `final` com construtor privado e campos privados `final`; o tipo interno `Holder` é o detentor da referência estática.

```java
private static class Holder {
    private static final ConfiguracaoSistema INSTANCIA = new ConfiguracaoSistema();
}

public static ConfiguracaoSistema getInstancia() { return Holder.INSTANCIA; }
```

Fonte S3: [ConfiguracaoSistema.java](../../src/main/java/br/com/joinup/config/ConfiguracaoSistema.java). A inicialização da classe detentora pela JVM publica a instância de forma thread-safe. As propriedades JVM têm precedência sobre variáveis de ambiente, lidas na construção; mudanças posteriores não alteram o snapshot. Porta, limites, origem, PBKDF2, duração da sessão e parâmetros de banco/mídia são consumidos por `Main`, bootstrap e `ConnectionFactory`.

**Caso demonstrável:** mostrar `Main` obtendo a referência e o bootstrap distribuindo seus valores; executar o teste de chamadas concorrentes. **Evidência:** [ConfiguracaoSistemaTest.java](../../src/test/java/br/com/joinup/config/ConfiguracaoSistemaTest.java), métodos `mesmaInstanciaEmChamadasConcorrentes`, `estruturaImutavelEConstrutorPrivado` e `propriedadesTemPrecedenciaESnapshotNaoMuda`. São 16 casos Surefire na base; passaram na execução local desta frente e no CI (S10).

**Vantagens:** consistência de configuração e ausência de mutação concorrente. **Tradeoffs:** estado global dificulta isolamento de configurações no mesmo processo e impede recarga; testes de propriedades usam JVMs novas. A unicidade vale por classloader, não por todos os processos da instalação. A configuração possui credenciais de banco em memória, mas o relatório não as reproduz.

**Alternativa não adotada:** criar uma configuração em `Main` e injetá-la em todos os consumidores também seria viável e facilitaria testes; o Singleton foi escolhido para explicitar configuração única e atender à seleção acadêmica aprovada. Uma `Connection` Singleton seria inadequada: diferentes requisições compartilhariam autocommit, locks, rollback e fechamento. O código abre conexões independentes por operação, situação exercitada em `MySqlIntegrationIT.usuarioAplicacaoNaoPossuiDdlEConexoesSaoIndependentes` (S2, S10).

### 3.2 Factory Method — criação polimórfica no cadastro

**Intenção:** deixar subclasses concretas escolherem o produto do fluxo comum. **Creator:** `CadastroUsuarioCreator`; **ConcreteCreators:** `CadastroParticipanteCreator`, `CadastroOrganizadorCreator`, `CadastroSuporteCreator`; **Product:** `Usuario`; **ConcreteProducts:** `Participante`, `Organizador`, `UsuarioSuporte`. `UsuarioRepository` é colaborador de persistência.

```java
Usuario produto = criarUsuario(dados, senhas.gerar(ValidacaoUsuario.senha(dados.senha())));
produto.atribuirId(usuarios.salvar(c, produto));
return produto;
```

```java
protected abstract Usuario criarUsuario(CadastroDados dados, String hashSenha);
```

Fonte S4: [CadastroUsuarioCreator.java](../../src/main/java/br/com/joinup/usuarios/service/CadastroUsuarioCreator.java). `cadastrar` é `final`: executa autorização, fábrica polimórfica e persistência na transação. As subclasses constroem produtos distintos, com CPF/nascimento para participante, CNPJ para organizador e validação de ausência desses campos em suporte. `CadastroSuporteCreator.autorizar` exige sessão SUPORTE também no serviço. `UsuarioRoutes` recebe os creators prontos e chama o contrato base; a criação não é um helper estático com `switch` de construtores.

**Caso demonstrável:** cadastrar participante em `/conta.html`, mostrar o resultado de perfil; no painel de suporte, criar outro suporte. Cadastro público de suporte deve receber negação. O cadastro do organizador usa a terceira rota concreta, comprovado por teste HTTP/JDBC, sem alegar que todos serão cadastrados durante o curto ensaio.

**Evidência:** [MySqlIntegrationIT.java](../../src/test/java/br/com/joinup/integration/MySqlIntegrationIT.java), `factoryMethodTresProdutosPersistidosESuporteRevalidaAutorizacao` e `suporte401403CadastroESegundoSuportePodeCadastrar`. Esses casos estão nos 46 testes de integração dessa classe no CI da base (S10); não há teste local MySQL novo nesta frente.

**Vantagens:** produto correto e validação específica sem acoplar o cadastro genérico aos construtores concretos. **Tradeoffs:** hierarquia e três creators aumentam o número de classes; novo perfil também exige repo, rota, schema e permissões. Não se promete extensibilidade sem modificações nesses limites.

**Alternativa não adotada:** uma função única condicionando todos os construtores seria menor para três perfis, mas concentraria validações e criação e não demonstraria Factory Method polimórfico. O algoritmo comum do creator possui estrutura sequencial; nesta avaliação ele é classificado pelo ponto de criação sobrescrito, e não contado como um quinto padrão.

### 3.3 Template Method — algoritmo de login

**Intenção:** fixar a sequência de autenticação e especializar etapas delimitadas. **AbstractClass:** `AutenticacaoTemplate`; **ConcreteClasses:** `AutenticacaoParticipante`, `AutenticacaoOrganizador`, `AutenticacaoSuporte`. `VerificadorSenha`, `SenhasPbkdf2`, `GerenciadorSessao` e repositórios específicos são colaboradores.

```java
Usuario usuario = buscarUsuario(c, email);
boolean correta = senhas.verificar(senha, usuario == null ? null : usuario.hashSenha());
if (usuario == null || !correta) throw new HttpException(401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos");
if (!usuario.ativo()) throw new HttpException(403, "CONTA_INATIVA", "Conta desativada");
validarRegraDoPerfil(usuario);
return sessoes.criar(c, usuario);
```

Fonte S3: [AutenticacaoTemplate.java](../../src/main/java/br/com/joinup/auth/AutenticacaoTemplate.java). O método **`autenticar` é final; a classe é abstrata**, não final. Antes do trecho, normaliza e valida e-mail/senha. Os hooks abstratos são `buscarUsuario(Connection,String)` e `validarRegraDoPerfil(Usuario)`. Cada busca usa o repositório de seu perfil com bloqueio; o segundo hook verifica a coerência da classe concreta por `instanceof`. Não existem políticas exclusivas como MFA ou validação externa de CNPJ no login. Essa distinção atende à ressalva A08 da auditoria 01.

**Caso demonstrável:** entrar como participante, organizador e suporte; mostrar a chamada comum em `UsuarioRoutes.login`. Senha incorreta interrompe antes da sessão; participante/organizador inativo não autentica. Suporte não tem ciclo de desativação no schema aprovado.

**Evidência:** `MySqlIntegrationIT.templateMethodSequenciaRealInterrompeAntesDaSessao`, `loginCorretoErradoIdentidadeCookieEHashPersistido` (três perfis) e `contaInativaNaoAutentica` (dois perfis). [AuthenticationSecurityTest.java](../../src/test/java/br/com/joinup/auth/AuthenticationSecurityTest.java) cobre hash/transporte/origem; não equivale sozinho a testar login persistido. Fontes S3, S10.

**Vantagens:** ordem estável, mesma verificação de senha/status e emissão de sessão somente ao final. **Tradeoffs:** extensão por herança; o hook de coerência é simples, e a principal especialização funcional é a consulta ao repositório correto. Alterar a sequência exige alterar a classe base e validar todos os perfis.

**Alternativa não adotada:** três algoritmos copiados permitiriam divergência de verificação/status. Composição por funções de busca seria tecnicamente possível e evitaria subclasses, mas a sequência com hooks implementa o Template Method aprovado e mantém os três caminhos inspecionáveis.

### 3.4 Facade — evento e subsistemas em uma transação

**Intenção:** oferecer uma operação integrada de criação sem exigir que o cliente coordene quatro subsistemas. **Facade:** `EventoFacade`; **cliente:** `EventoRoutes`; **subsistemas:** `EventoService`, `ArtistaService`, `TipoIngressoService`, `ComunidadeService`, cada um com repositório JDBC. `ExecutorTransacional` e `GerenciadorSessao` oferecem infraestrutura.

```java
Evento e=eventos.criar(c,d,dono,clock.instant());
artistas.sincronizarLineup(c,e.id(),d.artistaIds());
tipos.cadastrarTipos(c,e.id(),d.tiposIngresso());
comunidades.criarParaEvento(c,e);
ValidacaoEvento.antesInicio(e.data(),clock.instant());
return view(c,e);
```

Fonte S5: [EventoFacade.java](../../src/main/java/br/com/joinup/eventos/facade/EventoFacade.java), `criarIntegrado`. `criarEvento` é a entrada pública que envolve a operação no executor e exige organizador ativo. O método interno não confirma etapas separadamente: todos recebem a mesma conexão. Uma falha SQL em lineup, categorias ou comunidade desfaz inclusive o evento criado. A fachada também oferece edição, categorias e consultas; conta, titularidade e instante são revalidados sob locks.

**Caso demonstrável:** criar evento futuro com artistas e categorias e abrir sua comunidade automaticamente criada. Consultar o código/testes para demonstrar rollback; não induzir erro/trigger no banco da apresentação.

**Evidência:** [EventoFacadeTest.java](../../src/test/java/br/com/joinup/eventos/facade/EventoFacadeTest.java), quatro casos de coordenação/mesma conexão/interrupção; passaram localmente. A prova de rollback persistido é `MySqlIntegrationIT.falhaSqlAposEventoReverteTodasEtapas`, parametrizado para `line_up`, `tipos_ingressos`, `comunidades`, além de `facadeCriaEventoLineupDuasCategoriasComunidadeUnicaEListaSomenteDono`. O unitário verifica propagação e ordem; não prova rollback MySQL isoladamente (S10).

**Vantagens:** interface simples para o HTTP e atomicidade coordenada. **Tradeoffs:** dependência da fachada em quatro serviços, acoplamento da composição e consultas N+1 em `listar` → `view` por evento. Não se promete alta escala. Compra/revenda, social e atendimento continuam em serviços próprios: a fachada não é uma classe que concentra todo o sistema.

**Alternativa não adotada:** encadear quatro chamadas HTTP ou inserir todas as tabelas no handler aumentaria acoplamento e permitiria estado parcial. Um serviço de aplicação com outra nomenclatura poderia executar a mesma coordenação; a justificativa GoF está na interface simplificada e nos subsistemas independentes, não no sufixo do nome.

### 3.5 Análise de aplicabilidade dos demais 19 padrões GoF estudados

**Fonte da disciplina:** apresentação *Padrões de Projetos GoF — Introdução*, fornecida pelo PO em 12/10/2026, slides **12 a 18**, que apresenta a classificação dos **23 padrões clássicos**, e slide **19**, com classificação complementar por intenção. O PO confirmou que **todos os padrões desse material foram estudados**. A seção **4.4 do enunciado oficial RA2** exige justificar objetivamente os padrões estudados mas não adotados. Logo, esta análise cobre os **19 não utilizados**, e não somente alternativas selecionadas.

Os **quatro GoF efetivamente implementados** e demonstrados nas seções 3.1–3.4 são **Singleton** e **Factory Method** (criação), **Facade** (estrutura) e **Template Method** (comportamento). O edital exige **no mínimo quatro**, não exatamente quatro; a seleção não exclui futuras aplicações pertinentes, mas padrões artificiais sem benefício são expressamente desaconselhados.

#### 3.5.1 Padrões de criação — 3 não adotados de 5

| Padrão estudado | Onde poderia ser cogitado no JoinUp | Justificativa contextual da não aplicação |
| --- | --- | --- |
| **Abstract Factory** | Produzir em conjunto famílias de produtos por perfil, como usuário, autenticação e repositório, com uma fábrica participante/organizador/suporte | O ponto concreto de variabilidade é **criar o tipo de usuário**, já atendido pelo **Factory Method** `CadastroUsuarioCreator`. `ApplicationBootstrap` compõe repositórios e autenticações por construtor; não há famílias intercambiáveis independentes que precisem de uma fábrica abstrata inteira. Adicioná-la duplicaria configuração e classes. |
| **Builder** | Montar eventos com lineup, gêneros e múltiplas categorias por etapas | Os campos chegam em DTOs tipados; a criação é validada e orquestrada pelo `EventoFacade` e serviços, em **uma transação JDBC**. Não há diversas representações do mesmo objeto complexo a construir nem estados fluentes intermediários que justifiquem Builder. Ele seria considerado para configurações de eventos muito variáveis, não para substituir o Facade. |
| **Prototype** | Clonar um evento/categoria para facilitar a criação de edições semelhantes | Eventos têm IDs, proprietário, ingressos, vendas e comunidade vinculados. Clonagem indiscriminada poderia copiar referências e histórico que **não devem ser compartilhados**. A implementação exige nova criação com validações e novas linhas por `EventoFacade`; caso seja adicionada função “duplicar evento”, seria necessário definir precisamente o que clonar e o que não clonar. |

#### 3.5.2 Padrões de estrutura — 6 não adotados de 7

| Padrão estudado | Onde poderia ser cogitado no JoinUp | Justificativa contextual da não aplicação |
| --- | --- | --- |
| **Adapter** | Integrar processadores de pagamento, provedores de eventos ou serviços de armazenamento com contratos externos incompatíveis | Pagamento é **simulado localmente** e o sistema utiliza JDBC/MySQL, sem duas APIs externas diferentes a adaptar ao mesmo contrato. Introduzir adaptadores específicos sem interfaces incompatíveis reais seria uma camada vazia; seria adequado em integrações efetivas. |
| **Bridge** | Variar de modo independente abstrações de publicação/atendimento e mecanismos de entrega ou persistência | Atualmente cada caso de uso usa serviços/repositórios JDBC e HTTP nativo bem definidos; não há duas hierarquias de variações ortogonais (por exemplo, múltiplos canais de entrega × múltiplos mecanismos de envio) que precisem evoluir separadamente. A separação existente de camadas **não constitui automaticamente Bridge GoF**. |
| **Composite** | Tratar eventos, categorias, artistas e posts como uma árvore com operações comuns aplicadas a “folhas” e “grupos” | Esses elementos são **entidades de domínios diferentes**, relacionadas por FKs, e não componentes de uma mesma interface recursiva. Consultas SQL, DTOs e serviços representam essas relações sem forçar evento e ingresso a implementar operações uniformes. |
| **Decorator** | Acrescentar combinações dinâmicas de cache, auditoria e restrições em torno de handlers/serviços | Verificação de sessão, CSRF, perfil e estado é explícita nos fluxos existentes, e não há necessidade de empilhar **responsabilidades opcionais intercambiáveis** em objetos em tempo de execução. Decorators adicionais aumentariam a complexidade de depuração sem ganho demonstrado; poderiam ser reconsiderados com filtros/camadas transversais configuráveis. |
| **Flyweight** | Compartilhar em memória objetos imutáveis repetidos como gênero musical ou metadados de eventos | O catálogo demonstrativo tem volume modesto; gêneros são referenciados por IDs e recuperados do MySQL. Não se demonstrou pressão de memória causada por milhares de instâncias idênticas; cache de objetos compartilhados ainda introduziria política de invalidação e concorrência desnecessárias. |
| **Proxy** | Controlar o acesso a arquivos privados da comunidade, potencialmente substituindo um storage remoto | `ArquivoService.ler` **já valida sessão, filiação, ingresso e referência do arquivo antes da leitura**. Não existe um objeto remoto/caro com a mesma interface para o qual um objeto substituto seja útil; inserir um Proxy apenas para chamar o serviço atual duplicaria a barreira de acesso. |

#### 3.5.3 Padrões de comportamento — 10 não adotados de 11

| Padrão estudado | Onde poderia ser cogitado no JoinUp | Justificativa contextual da não aplicação |
| --- | --- | --- |
| **Chain of Responsibility** | Encadear verificações HTTP de sessão, perfil, CSRF e validação do payload em handlers intercambiáveis | Essas verificações são conhecidas, obrigatórias e executadas em pontos determinados de `Routes` e `GerenciadorSessao`. Não há cadeia configurável na qual cada objeto decide tratar ou repassar uma requisição; uma implementação GoF adicional aumentaria indireção e dificultaria a rastreabilidade das autorizações. |
| **Command** | Representar comprar ingresso, revender e encerrar chamado como objetos de ação para fila, auditoria ou desfazer | As operações são **requisições síncronas** cujo resultado deve ser persistido/confirmado por transação JDBC imediata. Não existe mecanismo de fila, replay ou undo; a compensação de pagamento real também não foi aprovada. Transformar cada POST em objeto Command não acrescentaria capacidade usada. |
| **Interpreter** | Interpretar uma linguagem própria de filtros de eventos ou regras de venda | As buscas aceitam parâmetros HTTP tipados e geram consultas SQL parametrizadas; não há linguagem de domínio com gramática e expressões que precisem ser interpretadas. Construir AST/intérprete só para `busca`, `cidade` e `generoId` criaria infraestrutura desproporcional. |
| **Iterator** | Percorrer ingressos, artistas de um lineup e eventos paginados sem revelar sua coleção interna | O Java oferece `Iterable`/`Iterator` e `for-each` nas coleções já usadas, e o MySQL fornece linhas paginadas. O projeto não requer **iterador customizado** para estrutura composta ou coleção própria; usar um iterador da biblioteca Java não autoriza alegar implementação autoral do GoF para RA2. |
| **Mediator** | Centralizar todas as interações entre eventos, compra, revenda, ingresso e comunidade | O fluxo atual possui colaboradores com dependências delimitadas: `EventoFacade` para criação de evento e serviços de comércio/social para suas transações. Um mediador geral para todas as mudanças concentraria regras que hoje pertencem a módulos diferentes e poderia tornar-se um **God Object**; a coordenação existente é específica, não um Mediator GoF declarado. |
| **Memento** | Salvar estado anterior de ingresso/evento para restaurá-lo depois de uma edição ou revenda | O sistema **preserva snapshots históricos de compras** por integridade/auditoria, mas eles não permitem restaurar integralmente o estado interno anterior de objetos sob solicitação — portanto **não são Memento GoF**. Para desfazer erros de banco, usa-se rollback transacional, não um histórico de objetos restauráveis. |
| **Observer** | Avisar participantes de alteração de eventos, compra, revenda concluída ou resposta de suporte | Notificações são apenas demonstrativas/informativas; não existem assinantes reais de eventos de domínio ou múltiplos consumidores assíncronos. Observer exigiria política de entrega e consistência entre commit e notificação sem demanda atual. |
| **State** | Trocar o comportamento do chamado entre `aberto`, `em_atendimento`, `aguardando_usuario` e `fechado` | `StatusTicket` representa estados e centraliza **transições explícitas e testadas**; o conjunto atual tem poucas ações e regras estáveis. Uma classe concreta para cada estado aumentaria objetos e dispersaria a matriz sem ganho proporcional. State poderia valer com muitos comportamentos distintos por estado e novas extensões. |
| **Strategy** | Alternar algoritmo de cálculo/preço, política de pagamento, provedor ou forma de liquidação | Há regras únicas aprovadas de estoque, preço e **pagamento simulado**, sem estratégias concorrentes selecionáveis em tempo de execução. As variantes de perfil tratadas em `AutenticacaoTemplate` especializam etapas de um algoritmo fixo, não algoritmos intercambiáveis de Strategy. |
| **Visitor** | Aplicar relatórios novos sobre uma hierarquia estável de eventos, ingressos, posts e tickets sem alterar suas classes | Não existe árvore heterogênea de elementos com `accept(visitor)`; métricas e históricos são obtidos por **agregações SQL** e DTOs. Criar Visitor imporia um protocolo comum a entidades sem hierarquia coesa. Só faria sentido com múltiplas operações externas sobre uma estrutura fixa de elementos. |

**Balanço completo:** 5 padrões de criação = **2 aplicados + 3 justificados**; 7 estruturais = **1 aplicado + 6 justificados**; 11 comportamentais = **1 aplicado + 10 justificados**; **23/23 classificados e analisados**. Cada alternativa acima é uma avaliação técnica do caso de uso, **não uma declaração de implementação**. A seleção atual de quatro permanece justificável, mas a pertinência adicional poderá ser discutida na arguição e não é sinônimo de nota máxima garantida.

**MVC nos slides:** páginas 7 a 10 apresentam o **modelo arquitetural MVC**, incluindo exercício específico. Ele é importante para discutir a divisão de responsabilidades na interface, mas **não é um dos 23 padrões GoF** da tabela de classificação apresentada nos slides. No JoinUp, HTML/JS, handlers HTTP, serviços e repositórios possuem responsabilidades separadas; isso **não significa afirmar um MVC de framework** ou usá-lo como quinto GoF.

**Referência didática:** apresentação *Padrões de Projetos GoF — Introdução*, slides 7–19, disponibilizada pelo PO em 12/10/2026. Ver também [matriz de conformidade ao enunciado](CONFORMIDADE_ENUNCIADO_OFICIAL.md), com o mínimo de quatro e os pesos da avaliação.

## 4. Modelo MySQL e regras de consistência

O domínio Java tem `Usuario` abstrato, mas o SQL mantém **tabelas separadas** `participantes`, `organizadores`, `usuarios_suporte`; não existe tabela genérica `usuarios` nem ORM. O schema v0.2 possui 22 tabelas, 42 FKs e 15 CHECKs na validação da Entrega 01. A migração 06 acrescenta `arquivos` e `demo_execucao`: inventário estático combinado de 24 tabelas, 48 FKs e 19 CHECKs (três de arquivos e um de demo). Esses números combinados são extração do SQL versionado, não inventário MySQL local novo. A suíte 06 aplica/retira as tabelas aditivas na instância de regressão; o teste de 22 tabelas valida o baseline antes da mídia (S9, S10).

| Núcleo persistido | Relações/invariantes relevantes |
| --- | --- |
| Eventos e lineup | Evento referencia organizador/gênero; `line_up` relaciona evento e artista; `comunidades.id_evento` é UNIQUE. O Java usa `Evento`, `ItemCatalogo`, `Comunidade`, sem classe fictícia `LineUp`/ORM |
| Categoria/estoque | Preço e contadores não negativos por CHECK; edição bloqueada se contador ou histórico de `compra_itens`/`ingressos` registra venda; baixa comercial condicionada |
| Compra/itens/ingressos | Compra guarda nome/data/local de evento; item guarda preço unitário/subtotal; um ingresso emitido por unidade. A edição posterior de evento não reescreve esses snapshots |
| Revenda | FK para ingresso, vendedor e comprador; coluna gerada `id_ingresso_disponivel` com índice UNIQUE impede dois anúncios disponíveis; linhas vendidas/canceladas conservam histórico |
| Filiação e social | `participante_comunidades` conserva ativo/data_saida; autoria de mensagem participante/organizador tem CHECK de exclusividade; resposta a mensagem da mesma comunidade é checada no serviço/repositório, pois FK sozinha não impõe esse pertencimento |
| Chamado e mensagens | CHECK de solicitante participante/organizador; mensagens têm autoria tipada exclusiva. Associação ao suporte não substitui o autor real da mensagem |
| Sessão/auditoria | Hash de token, perfil/titular e expiração; reativação registra operador em `auditoria_contas`. Isso não é auditoria de toda transição de chamado |
| Arquivos/demo | Seis FKs de contexto/dono/vínculo, três CHECKs adicionais e índices únicos de vínculo; `demo_execucao` marca carga do runner |

Fonte S9: [schema_v0.2.sql](../../src/main/resources/db/schema_v0.2.sql), [migration_06_midia_demo.sql](../../src/main/resources/db/migration_06_midia_demo.sql) e [CORRECAO_DDL_01.md](../banco/CORRECAO_DDL_01.md). A correção histórica de cinco ações referenciais resolveu incompatibilidade MySQL entre FKs e CHECKs sem remover regras de autoria. Contas são desativadas, conservando dados vinculados.

Compra direta bloqueia contas, sessão, evento e categorias em ordem consistente, obtém preço no banco, baixa estoque e grava compra/itens/emissões atomicamente. `BigDecimal` evita cálculo monetário binário; quantidade, total e contador são limitados aos tipos SQL. Revenda bloqueia/revalida recursos após leituras de pistas, transfere ingresso, marca venda e inativa a filiação se o vendedor ficou sem ingresso elegível. O comprador **não entra automaticamente**; se o vendedor ainda tem outro ingresso ativo/utilizado, conserva a filiação. Fontes S6.

Pós-evento, participante com ingresso ativo ou utilizado **e** filiação ativa continua acessando a comunidade; ingresso cancelado não habilita. Anunciar não retira o direito, concluir a última venda retira. Organizador inativo bloqueia comércio do evento, mas não apaga comunidade nem invalida automaticamente o acesso dos participantes elegíveis. Regras e locks comunitários são independentes da condição temporal comercial (S1, S6).

| Ação do chamado | Estado de origem permitido | Destino |
| --- | --- | --- |
| Abrir (participante/organizador) | Sem chamado | `aberto` |
| Suporte responder | `aberto`, `em_atendimento`, `aguardando_usuario` | `aguardando_usuario` |
| Solicitante retornar com texto | `aguardando_usuario` | `em_atendimento` |
| Solicitante resolver | `aguardando_usuario` | `fechado` |
| Suporte encerrar | `aberto`, `em_atendimento` | `fechado` |
| Qualquer nova mensagem/transição de estado | `fechado` | Negada; sem reabertura |

`StatusTicket` define a matriz, `TicketService` a executa sob lock e atualiza estado/mensagem na mesma transação. Autoassociação é permitida inclusive em fechado e não altera estado. Dois suportes podem responder sucessivamente, com autores separados em `ticket_mensagens`; resolver/encerrar sem texto não fabrica mensagem e não registra ator de fechamento em log separado (ressalva R-05-01). Fontes S7.

## 5. Segurança e limites das garantias

| Decisão efetiva | Evidência e limite |
| --- | --- |
| PBKDF2WithHmacSHA256 | Salt individual 16 bytes, hash 256 bits, novas senhas com mínimo/default 600.000 iterações; verificação aceita fixtures antigas de 180.000. Comparação por `MessageDigest.isEqual`; sem claim de rehash automático (S3) |
| Sessão | Token aleatório de 32 bytes; só SHA-256 do token é persistido; cookie `HttpOnly`, `SameSite=Lax`, `Path=/`, `Max-Age`; `Secure` quando origem configurada HTTPS. HTTP local não tem TLS (S3) |
| Conta ativa | Login e operações revalidam conta/sessão; desativar cancela anúncios e revoga sessões atomicamente; reativar exige suporte e não reanuncia automaticamente (S3, S4) |
| CSRF | Origem fixa configurada, comparação de Origin ou Referer e Fetch Metadata; não depende de Host/X-Forwarded arbitrários e não usa um token CSRF separado (S3) |
| IDOR | Titularidade/perfil revalidados no servidor, inclusive detalhes, mutações e arquivos; recurso alheio frequentemente retorna 404 indistinguível. Conhecer URL não concede autorização (S4–S8) |
| SQL | Valores via PreparedStatement; identificadores internos de perfil vêm de enum/listas confiáveis. Não se concatena identificador arbitrário do cliente. Conta de aplicação DML sem DDL/GRANT (S2–S9) |
| JSON/HTTP | Jackson rejeita coerções/campos desconhecidos/duplicatas conforme adaptadores; roteamento por segmento; limite padrão 65.536 bytes. Upload possui limite próprio de 1.064.960, sem ampliar JSON indiscriminadamente (S2, S8) |
| Imagem | PNG/JPEG/WebP íntegros, no máximo 1 MiB entrada/saída, 4096 px por dimensão e 4 milhões de pixels; normalização PNG; IDs do servidor e armazenamento fora da árvore pública (S8) |
| Mídia privada | Leitura e vínculo verificam autorização atual, contexto/dono, tamanho/digest e uso único; pending expira em 24h. CREATE_NEW/NOFOLLOW e diretório protegido; não protege contra administrador local (S8) |
| Concorrência | Locks de conta/sessão/evento/ingresso/filiação/arquivo e estado no mesmo commit; testes de interleavings/rollback dão evidência dos casos exercitados, sem prova universal contra deadlocks futuros (S6–S10) |

O ambiente é uma demonstração acadêmica local, **não pronto para produção**. Faltam rate limiting, quota global de upload, testes de proxy/TLS real e carga; filesystem/MySQL não compartilham commit distribuído. Crash, arquivo pendente abandonado e imagens substituídas exigem reconciliação administrativa. Os requisitos não autorizam inventar novos serviços de pagamento ou antifraude (S1, S8, S11).

## 6. Qualidade e evidências por commit

O [CI da main](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38247779976), job `114805929131`, foi consultado nesta frente: `completed/success`. Os logs efetivos confirmam **167 Surefire + 301 Failsafe = 468**, zero falhas, erros ou ignorados, Java 21 e dois serviços MySQL 8.4.0, no **SHA completo da base**, concluído em 12/10/2026 às **09:19:01 -03:00**. [Extrato verificado](evidencias/CI_MAIN_7efe29c.txt).

| Execução vinculada | Commit de origem | Total e natureza |
| --- | --- | --- |
| Fundação, registro histórico | `137d6196f42f94531db593596b2696a7609e41d2` (merge #1) | 52 testes sem MySQL; auditoria 00 não homologava o banco |
| Correção social 04.1, registro histórico | `232e06ce2f13f6f1aa4391002aab4098f06d92d3` | 310 = 136 + 174, fonte auditoria 04.1/relatório e extrato |
| Entrega 05, registro histórico | `bb926ca1602f57d37079112dd492c3d5c42ef996` | 397 = 149 + 248; run 38193962732, fonte relatório 05 |
| Correção de transporte 413 da 06, registro histórico | `8b9d3bc99edd783aaa2417b80a7ab938855f531e` | 468 = 167 + 301; run 38198694371 e extrato final 06 |
| Main após integração 06, conferido nesta frente | `7efe29c1529a71e3c1a82a52716a5a32947a61fe` | 468 = 167 + 301; run 38247779976, logs consultados |
| Execução local desta frente 07 | Mesma base `7efe29c` | `mvn --batch-mode --no-transfer-progress test`: **167 Surefire**, sem MySQL/Failsafe; sucesso às 09:17:15 -03:00 |

Totais são resultados associados a revisões, não cobertura percentual de requisitos, linhas ou funcionalidades. Não foi executado `-Pmysql-it` localmente nesta frente, não se apagou/provisionou banco e não foi realizado E2E manual novo. [Extrato local](evidencias/TESTES_LOCAIS_7efe29c.txt). Fontes históricas S11.

| Suíte de integração na base | Casos | Evidência importante |
| --- | ---: | --- |
| `MySqlIntegrationIT` | 46 | Três produtos/fluxos de login, sessão/conta, Facade, constraints, UTC e rollback por triggers |
| `ComercioMySqlIT` | 63 | Compra/estoque concorrentes, venda única, snapshots e rollback de transferência/filiação |
| `ComunidadeMySqlIT` | 65 | Autorização pós-evento, resposta na mesma comunidade, quatro ordens social × última revenda |
| `SuporteMySqlIT` | 63 | Matriz de ações/estados, dois autores suporte, IDOR, rollback e disputas de estado |
| `PaineisMySqlIT` | 11 | Escopo por titular/organizador, histórico vs ingresso atual, métricas de autoria |
| `ZArquivosMySqlIT` | 45 | Upload/413, formatos, acesso privado, filesystem e perda de elegibilidade em corrida |
| `ZDemoMySqlIT` | 8 | JVM nova, opt-in, base alheia recusada, rollback de seed e idempotência sem sobrescrita |
| **Failsafe** | **301** | Soma conferida no CI da base |

A auditoria cruzada PR4+PR5 identificou falta de integração social real, resposta entre comunidades e corrida de autorização. A Correção 04.1 adicionou prova HTTP/JDBC e locks; a mídia permaneceu pendente até 06. Isso demonstra por que um CI verde com testes antigos não atesta um módulo novo. A falha intermediária de 413 da 06 está registrada e foi corrigida, sem excluir o teste; usar evidência final, não os runs que falharam (S11).

**Testes manuais históricos:** relatórios 03, 04.1, 05 e 06 contêm capturas/ambientes próprios. A sanidade 06 cobre post somente-imagem, chat com texto+imagem, upload de evento/catálogo, suporte e visitante após logout. `ERR_BLOCKED_BY_CLIENT` de navegação privada não é medição de status 401; esse status vem de teste HTTP. As capturas são sanidade desktop e não homologação móvel, leitor de tela ou roteiro integral RA2. Na frente 07, a matriz tinha todos os casos **NÃO EXECUTADO**. A [matriz final atual](MATRIZ_VALIDACAO_FINAL.md) conserva a execução real da 08: 42 aprovados, principalmente automáticos; V26/V44/V45 sem execução integral. A Entrega 09 não modifica esses status.

## 7. UML, sincronização e limitações abertas

O [diagrama integral](DIAGRAMA_CLASSES_GERAL.puml) representa **123 tipos nomeados em 117 fontes Java**, incluindo seis tipos internos. O [inventário](INVENTARIO_CLASSES.md) e seu [JSON de assinaturas](evidencias/INVENTARIO_JAVA_7efe29c.json) permitem conferência por arquivo/linha. A [visão resumida](VISAO_ARQUITETURA.puml) serve à apresentação; o [registro de validação](VALIDACAO_DOCUMENTAL.md) documenta geração, conferência e renderização. Campos que guardam objetos não recebem automaticamente diamante de composição; IDs SQL não viram grafo ORM. As multiplicidades publicadas são apenas as justificadas por coleções/FKs/contratos reais.

A Entrega 06 já está integrada e a 08 preencheu identificação e homologação técnica com ressalvas. Permanecem ensaio coletivo, revisão acadêmica, duração autorizada e V26/V44/V45. O relatório 06 descreve o PR aberto no momento de sua autoria: é registro histórico, não o estado atual do PR #8. O README foi corrigido na 08 e conferido na 09, sem nova edição. A auditoria das aulas também conserva seu baseline histórico; os novos resultados estão no registro da 09.

### 7.1 Articulação entre os conteúdos estudados

O enunciado RA2 exige **diagrama de classes**. Pacotes, componentes e implantação
são complementos de estudo das aulas, não novos requisitos oficiais nem GoF.
Cada visão responde a uma pergunta diferente:

| Conteúdo / visão | O que explica no sistema | Artefato para consulta |
| --- | --- | --- |
| Classes — Aula01, slides 7–19 | Tipos, visibilidade, herança, interfaces e dependências Java; o integral exige zoom | [geral SVG](DIAGRAMA_CLASSES_GERAL.svg) / [fonte](DIAGRAMA_CLASSES_GERAL.puml); [resumo SVG](VISAO_ARQUITETURA.svg) |
| Cardinalidades — Aula01 | Relações do **domínio persistido** e opcionalidade verificadas por FK/NOT NULL/UNIQUE; notas distinguem invariantes dos serviços | [fragmento SVG](DIAGRAMA_DOMINIO_CARDINALIDADES.svg) / [fonte](DIAGRAMA_DOMINIO_CARDINALIDADES.puml) |
| Pacotes — slides 14–19 | Pastas contendo 20 classes + 1 interface reais e dez dependências selecionadas; destaca ACOP-01 | [pacotes SVG](DIAGRAMA_PACOTES.svg) / [fonte](DIAGRAMA_PACOTES.puml); [anexo completo](DIAGRAMA_PACOTES_ANEXO.svg) / [fonte](DIAGRAMA_PACOTES_ANEXO.puml) |
| Componentes — slides 10–24 | Interfaces fornecidas/requeridas em bola/encaixe: SenhasPbkdf2 → VerificadorSenha → autenticação e repositórios → UsuarioRepository → cadastro | [componentes SVG](DIAGRAMA_COMPONENTES.svg) / [fonte](DIAGRAMA_COMPONENTES.puml); [contexto HTTP/módulos](DIAGRAMA_COMPONENTES_ANEXO.svg) / [fonte](DIAGRAMA_COMPONENTES_ANEXO.puml) |
| Implantação — slides 4–13 | Um dispositivo local, Windows/JVM ambientes, navegador/JoinUp/MySQL programas/serviço, JAR/recursos/SQL/PNG/dados artefatos e protocolos HTTP/JDBC/TCP/IO | [implantação SVG](DIAGRAMA_IMPLANTACAO.svg) / [fonte](DIAGRAMA_IMPLANTACAO.puml) |
| GoF — introdução slides 12–19, Facade e Singleton | Soluções de colaboração entre objetos; quatro implementados + 19 justificados no §3.5, rubrica **4+2+2+1+1** preservada | [fragmento GoF SVG](../diagramas/CLASSES_GOF_v0.1.svg) / [fonte](../diagramas/CLASSES_GOF_v0.1.puml); §3 deste relatório |

O UML geral mostra relações entre **tipos Java**. O fragmento de cardinalidades
mostra relações entre **registros persistidos**, mesmo quando um record mantém
somente IDs ou não projeta a FK: `TipoIngresso` não possui `eventoId` e `Ingresso`
não possui `compraId`. `compras` e `participante_comunidades` aparecem como
`<<table>>`; não existem classes Java `Compra` ou `ParticipanteComunidade`.
`CompraDados` é DTO de requisição. Nenhuma FK vira automaticamente composição.
O fragmento está identificado como **visão conceitual da persistência** e não
substitui o UML geral de classes obrigatório. Sua identificação foi reforçada
após a auditoria do PR #11, preservando todas as dez associações verificadas.

SQL permite evento com 0..1 comunidade, enquanto `EventoFacade.criarIntegrado`
confirma uma comunidade e ao menos uma categoria na criação. SQL permite compra
sem filhos; o fluxo confirmado de `CompraService` exige itens não vazios e emite
um ingresso por unidade, portanto 1..* nessa operação. Filiação é manual;
titular atual pode mudar na revenda e comprador original/histórico permanecem.
As associações e o acesso pós-evento estão justificados no
[registro de rastreabilidade](VALIDACAO_CONFORMIDADE_AULAS_UML.md).

Os componentes são módulos lógicos no **mesmo JAR/processo**, sem contratos Java
inventados para serviços concretos. HTTP é interface lógica, distinta das
interfaces Java `UsuarioRepository` e `VerificadorSenha`. A implantação mostra
o ambiente local realmente homologado: navegador no mesmo host, Java 21,
HttpServer em 127.0.0.1:8080, MySQL 8.4.0 em 3315 e uploads privados. O README
exemplifica 3306; o CI usa instâncias próprias e não integra essa vista.
Schema e migração são administrativos; o runner só aplica DML do seed elegível.
Após a auditoria didática do Tech Lead, a vista nuclear mostra bola no fornecedor
e encaixe no consumidor em dois contratos reais; contexto HTTP e demais módulos
ficaram no anexo. As implementações de UsuarioRepository são herdadas pelos
três repositórios concretos de UsuarioRepositoryJdbc, e o consumidor exibido é
CadastroUsuarioCreator. Na implantação, somente Windows/JVM recebem o estereótipo
de ambiente de execução; navegador e MySQL são software no mesmo host físico.

### 7.2 ACOP-01 — acoplamento circular reconhecido

`ArquivoService` conserva `ComunidadeSocialService` e chama
`validarAcessoComunidade` no upload/leitura. O social conserva referência mutável
a `ArquivoService`, valida a referência e chama `vincular` após INSERT de
postagem/mensagem na mesma transação. O bootstrap constrói social, depois mídia
e finalmente chama `configurarArquivos`. Isso combina validação de elegibilidade
e vínculo de imagens, mas exige inicialização em duas etapas e dificulta
substituição/testes isolados dos serviços. As duas setas estão visíveis nos
novos diagramas, além do UML geral.

A auditoria não comprovou falha operacional causada pelo ciclo. Extrair um
contrato mínimo de acesso/vínculo ou deslocar a coordenação para caso de uso
é proposta posterior, sujeita a preservar locks, permissões e atomicidade.
A Entrega 09 é documental: refatorar agora ampliaria o risco sobre a demo
homologada. `ApplicationBootstrap` conhecer os módulos é esperado como
composition root; essa responsabilidade não constitui o achado ACOP-01.

| Limitação/achado preservado | Consequência / próximo passo |
| --- | --- |
| N+1 de eventos (R-02-01), históricos comerciais sem paginação completa (COM-03) | Medir/otimizar se necessário; sem benchmark nesta frente |
| POST de compra e abertura de chamado sem chave de idempotência (COM-01, R-05-02) | Nova requisição pode repetir operação válida; não repetir POST automaticamente |
| Histórico de mensagens não registra ator de toda associação/encerramento (R-05-01) | Não atribuir fechamento ao suporte associado; evolução depende de requisito |
| Mídia, filesystem e base após crash | Reconciliação offline; sem coletor automático/antivírus/CDN/quota global |
| Seed automático preserva dados, não é reset | Datas relativas são calculadas na primeira carga; reinício não renova eventos nem restaura contas/linhas removidas |
| Avatar/perfil sem campo v0.2 | Fora do incremento 06; contrato inicial de upload de perfil não prova implementação |
| Mobile, acessibilidade assistiva, duração real de aula e E2E por três perfis | Inspeção parcial/HTTP registrados na 08; V26/V44/V45 e ensaio coletivo continuam pendentes |
| Equipe/tempo da apresentação e parecer de release | Cinco nomes/turma/docente informados pelo PO na 08 e preservados; formato, tempo efetivo e aceite acadêmico pendentes. Merge não comprova aceite técnico externo |
| ACOP-01: social ↔ mídia | Inicialização em duas etapas e referências mútuas; proposta posterior no §7.2, sem refatoração nesta entrega |

## 8. Reflexão e lições aprendidas

A reflexão a seguir registra **dificuldades e mudanças técnicas documentadas nos PRs e nos relatórios de execução**. A equipe deverá revisá-la para acrescentar as experiências reais de seus integrantes, sem atribuir a alguém dificuldades que não tenha vivenciado.

| Dificuldade evidenciada | Decisão/mudança realizada | Aprendizado de design |
| --- | --- | --- |
| MySQL rejeitou combinações de `CHECK`/ação referencial no DDL inicial | Correção compatível de FKs, sem descartar integridade ou histórico (Entrega 01) | Um esquema aparentemente correto precisa ser executado no SGBD real; restrições são parte do contrato do domínio |
| Criação integrada de evento poderia deixar dados parciais após erro em lineup/categoria/comunidade | `EventoFacade` coordena serviços com uma só `Connection` e rollback atômico (Entrega 02) | Facade simplifica a entrada e permite preservar transação sem concentrar SQL em handler |
| Compras e revendas concorrentes poderiam superar estoque ou transferir um ingresso duas vezes | Locks e revalidação sob JDBC, transações e testes de concorrência (Entrega 03) | Consultar o estado antes de bloquear a linha não substitui revalidação no instante de persistência |
| A primeira entrega social tinha cobertura unitária sem MySQL e permitia risco de elegibilidade ultrapassada | Testes HTTP/MySQL reais, validação de resposta na mesma comunidade e locks coordenados com revenda (Correção 04.1) | Testes mockados e CI de regressão não comprovam a consistência de um módulo novo; regras entre módulos devem ser testadas juntas |
| Upload de imagens e JSON acima do limite produziram falhas intermediárias de transporte HTTP 413 | Leitura limitada por rota, verificação de MIME/conteúdo, normalização para PNG e reexecução de testes do transporte (Entrega 06) | Restrições de segurança e transporte exigem testes com bytes reais, não apenas validação de cabeçalhos |
| Um seed manual era difícil de reproduzir sem contaminar a apresentação | `DemoRunner` com opt-in local, base vazia, lock, marcador e idempotência sem reset (Entrega 06) | Reprodutibilidade e isolamento são atributos de qualidade; automação de demonstração deve ser deliberadamente segura |
| UML e documentação poderiam divergir da implementação conforme novos módulos fossem incorporados | Inventário extraído de declarações Java, diagramas regenerados, evidências por SHA e revisão documental após PR #8 (Entrega 07) | O diagrama é artefato sincronizado com classes executáveis, não uma proposta arquitetural sem vínculo com o código |

**Limitações reconhecidas:** a solução Java 21 puro facilita rastrear o fluxo HTTP e JDBC, mas implica implementação manual de roteamento, segurança e transações. Os quatro GoF melhoraram organização em pontos de variabilidade identificados, mas nenhum deles substitui testes de integração, disciplina transacional ou validação de permissões. O sistema não é certificado para produção e o ensaio RA2 final ainda deve ser registrado.

**PENDENTE DA EQUIPE:** confirmar detalhes vivenciados durante programação, distribuição de responsabilidades e aprendizados individuais/coletivos para que esta seção represente a experiência dos estudantes — e não apenas inferências técnicas extraídas dos commits.

## 9. Conclusão e evolução

A implementação examinada oferece os quatro GoF em caminhos reais e separa os principais limites de permissão, estado e transação. O CI da base exercita HTTP/JDBC/MySQL e casos de concorrência/rollback; o relatório distingue essa prova automatizada do ensaio manual ainda pendente. Os entregáveis documentais tornam a modelagem e as justificativas revisáveis sem alterar a aplicação.

Antes da entrega acadêmica: confirmar formato/duração com a equipe já identificada, revisar as 19 justificativas dos GoF não implementados, completar V26/V44/V45 preservando as evidências da 08, ensaiar os participantes e suportes em contextos independentes e obter revisão externa. Evoluções possíveis incluem remoção de ACOP-01, idempotência de mutações, consultas em lote, auditoria de transições se exigida e manutenção de arquivos. Essas evoluções não são funcionalidades já entregues.

## 10. Fontes e convenção de rastreabilidade

**Convenção:** todas as fontes S1–S10 abaixo são os caminhos no commit `7efe29c1529a71e3c1a82a52716a5a32947a61fe`. Assim, cada remissão Sx vincula alegação a caminho e revisão, sem depender da futura movimentação de `main`. [Árvore congelada](https://github.com/ericjs-code/JoinUp-POO/tree/7efe29c1529a71e3c1a82a52716a5a32947a61fe). S11 contém avaliações/resultados históricos com seus commits explicitados no texto e nos próprios documentos.

| ID | Fontes na base |
| --- | --- |
| S1 | `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md`; todos os `ADENDO_*.md`: RF-COM-05/06, RF-EVT-06/07/08, RF-REV-05, RF-SUP-04 |
| S2 | `pom.xml`; `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`, `PROPOSTA_JAVA_PURO_v0.1.md`, `CLASSES_E_PADROES_GOF_v0.1.md`; `src/main/java/br/com/joinup/Main.java`, `bootstrap/ApplicationBootstrap.java`, `http/*.java`, `shared/db/*.java` |
| S3 | `src/main/java/br/com/joinup/config/ConfiguracaoSistema.java`; `auth/*.java`; `usuarios/domain/*.java` |
| S4 | `src/main/java/br/com/joinup/usuarios/service/*.java`, `usuarios/repository/*.java`, `usuarios/http/UsuarioRoutes.java`, `usuarios/dto/*.java` |
| S5 | `src/main/java/br/com/joinup/eventos/{facade,service,repository,http,dto,domain}/*.java`; `docs/api/EVENTOS_HTTP_02.md` |
| S6 | `src/main/java/br/com/joinup/compras/**`, `revendas/**`, `comunidades/**`; `docs/api/COMPRAS_REVENDA_HTTP_03.md`, `COMUNIDADES_SOCIAL_HTTP_04.md` |
| S7 | `src/main/java/br/com/joinup/suporte/**`, `paineis/**`; `docs/api/SUPORTE_PAINEIS_HTTP_05.md` |
| S8 | `src/main/java/br/com/joinup/arquivos/*.java`, `demo/*.java`; `docs/api/ARQUIVOS_HTTP_06.md`; `docs/prompts/CODEX_06_MIDIA_DEMO.md` |
| S9 | `src/main/resources/db/{schema_v0.2,seed_demo_v0.2,migration_06_midia_demo}.sql`; `docs/banco/VALIDACAO_INTEGRACAO_01.md`, `CORRECAO_DDL_01.md` |
| S10 | `src/test/java/br/com/joinup/**`; `.github/workflows/java.yml`; CI run 38247779976 e extratos desta pasta |
| S11 | `docs/execucao/AUDITORIA_ENTREGA_00.md`, `AUDITORIA_ENTREGA_01.md`, `AUDITORIA_ENTREGA_02.md`, `AUDITORIA_CRUZADA_PR04_PR05.md`, `AUDITORIA_CORRECAO_04_1_PR06.md`, `AUDITORIA_ENTREGA_05_PR07.md`; relatórios `ENTREGA_00/01/02/03/04/05/06_RESULTADOS.md`, `CORRECAO_04_1_RESULTADOS.md` e respectivos extratos/capturas |

As propostas iniciais e contratos v0.1 contêm nomes/estados futuros; foram confrontados com os adaptadores/serviços reais. Não se transforma uma proposta em evidência de entrega. Divergências documentais e procedimentos de atualização estão em `VALIDACAO_DOCUMENTAL.md`.
