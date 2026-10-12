# JoinUp-POO — Relatório técnico RA2

**Disciplina:** Design de Software, Avaliação Somativa RA2.

**Repositório:** [ericjs-code/JoinUp-POO](https://github.com/ericjs-code/JoinUp-POO).

**Equipe, instituição, professor e turma:** a confirmar com os integrantes; não inferidos do proprietário do repositório.

**Entrega prevista nos requisitos:** 15/10/2026; apresentação: 16/10/2026.

**Revisão documental:** 12/10/2026, America/Sao_Paulo.

**Base integral examinada:** `7efe29c1529a71e3c1a82a52716a5a32947a61fe`, `main` após PR #8 (Entrega 06). Branch documental: `docs/07-ra2-relatorio-uml`.

O enunciado integral RA2 **não foi encontrado** entre os arquivos rastreados do repositório nem entre os anexos disponibilizados nesta conversa. A especificação cita materiais fornecidos anteriormente, mas esses anexos não estão acessíveis nesta frente. A fundamentação acadêmica usa somente os requisitos já documentados: **exatamente quatro GoF**, classes coerentes com código, justificativas e tradeoffs, sistema executável e apresentação. Pesos, duração definitiva e demais critérios precisam ser conferidos no enunciado original; este relatório não garante todos os critérios de avaliação.

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

### 3.5 Análise de aplicabilidade dos padrões GoF não adotados

**Critério oficial (RA2, seção 4.4):** apresentar **todos os demais padrões efetivamente estudados em sala** e justificar objetivamente a não aplicação com base no contexto do JoinUp-POO. **A lista da disciplina ainda não foi fornecida à equipe de documentação.** A tabela a seguir é uma análise técnica preliminar de alternativas GoF relevantes; **não afirma que todos esses padrões foram ensinados**. A equipe deve confrontá-la com os slides/aulas e acrescentar qualquer padrão estudado que esteja ausente antes de entregar o relatório. O enunciado exige **no mínimo quatro GoF**, não exatamente quatro; a decisão atual de implementar quatro não impede um quinto, mas a aplicação artificial é penalizável.

| Padrão GoF alternativo | Possível aplicação no JoinUp | Por que não foi adotado no produto efetivo | Quando reconsiderar |
| --- | --- | --- | --- |
| **Strategy** | Alternar algoritmos/provedores para efetivar o pagamento de ingressos | Compras e revendas têm **pagamento local simulado**, sem vários algoritmos reais para intercambiar; subclasses vazias seriam decorativas | Se surgir mais de um meio de pagamento com regras distintas |
| **Observer** | Notificar atores quando uma compra ou resposta de suporte ocorrer | Notificações são demonstrativas e não há subsistema assíncrono de subscrição/entrega; adicionar observadores complicaria a consistência do commit JDBC | Com notificações reais, eventos de domínio e consumidores múltiplos |
| **State** | Encapsular por classe as transições de tickets `aberto`, `em_atendimento`, `aguardando_usuario` e `fechado` | `StatusTicket` já centraliza regras finitas e testadas de transição; objetos por estado acrescentariam classes sem variação de comportamento proporcional | Com fluxos de atendimento mais complexos e lógica extensa por estado |
| **Abstract Factory** | Criar famílias relacionadas de cadastro/autenticação/repositório por perfil | No caso atual, o ponto relevante de variabilidade é a **criação de usuário**, atendida por Factory Method; não há famílias independentes de múltiplos produtos trocadas em conjunto | Se produtos/serviços correlacionados fossem instanciados por configuração de cada perfil |
| **Builder** | Montar solicitações de eventos com artistas, ingressos e dados opcionais | Dados de requisição são DTOs/records; validação e transação ficam em serviços/Facade. Uma montagem incremental fluente não reduziria complexidade do caso | Com formulários altamente configuráveis e muitos estados válidos de montagem |
| **Adapter** | Integrar gateways de pagamento ou plataformas externas de eventos | Não há integração externa em produção; JDBC usa a interface padronizada e o Connector/J, sem necessidade de adaptador autoral extra | Ao conectar provedores com contratos incompatíveis com a API interna |
| **Decorator** | Empilhar comportamentos opcionais de auditoria/cache em handlers HTTP | Segurança e erros possuem pontos fixos de verificação; decorar dinamicamente muitas combinações de comportamento não é exigido | Quando a composição dinâmica de filtros independentes se justificar por múltiplas rotas |
| **Command** | Enfileirar compra, suporte ou ações reversíveis para execução posterior | As mutações atuais são HTTP/JDBC síncronas, exigem confirmação ou rollback imediato e **não** possuem fila/undo | Para processamento assíncrono confiável e histórico de execução de comandos |
| **Proxy** | Mediar a leitura das imagens privadas da comunidade | `ArquivoService.ler` já faz verificação de sessão, elegibilidade e vínculo antes da leitura; criar proxy separado duplicaria a barreira sem abstração adicional útil | Com cache remoto, armazenamento externo ou acesso transparente por mais de uma origem |

**Conclusão contextual:** esses padrões representam possibilidades, não um catálogo de padrões implementados ou estudados. As quatro aplicações concretas e comprovadas permanecem **Singleton, Factory Method, Template Method e Facade**. Ver [confronto com a rubrica oficial](CONFORMIDADE_ENUNCIADO_OFICIAL.md). **PENDENTE ACADÊMICO:** confirmar a lista real da disciplina e completar esta análise para cada padrão estudado, sem inventar conteúdo de aula.

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

**Testes manuais históricos:** relatórios 03, 04.1, 05 e 06 contêm capturas/ambientes próprios. A sanidade 06 cobre post somente-imagem, chat com texto+imagem, upload de evento/catálogo, suporte e visitante após logout. `ERR_BLOCKED_BY_CLIENT` de navegação privada não é medição de status 401; esse status vem de teste HTTP. As capturas são sanidade desktop e não homologação móvel, leitor de tela ou roteiro integral RA2. A [matriz final](MATRIZ_VALIDACAO_FINAL.md) mantém seus casos como **NÃO EXECUTADO**, mesmo quando há evidência automatizada/histórica relacionada.

## 7. UML, sincronização e limitações abertas

O [diagrama integral](DIAGRAMA_CLASSES_GERAL.puml) representa **123 tipos nomeados em 117 fontes Java**, incluindo seis tipos internos. O [inventário](INVENTARIO_CLASSES.md) e seu [JSON de assinaturas](evidencias/INVENTARIO_JAVA_7efe29c.json) permitem conferência por arquivo/linha. A [visão resumida](VISAO_ARQUITETURA.puml) serve à apresentação; o [registro de validação](VALIDACAO_DOCUMENTAL.md) documenta geração, conferência e renderização. Campos que guardam objetos não recebem automaticamente diamante de composição; IDs SQL não viram grafo ORM. As multiplicidades publicadas são apenas as justificadas por coleções/FKs/contratos reais.

A Entrega 06 já está integrada na base. A sincronização documental de código/mídia/demo foi feita a partir dessa main, mas ainda faltam validação manual integral, revisão acadêmica e identificação da equipe. O relatório 06 descreve o PR aberto no momento de sua autoria: esse texto é registro histórico, não o estado atual do PR #8. Não alterar README nem os registros históricos nesta frente.

| Limitação/achado preservado | Consequência / próximo passo |
| --- | --- |
| N+1 de eventos (R-02-01), históricos comerciais sem paginação completa (COM-03) | Medir/otimizar se necessário; sem benchmark nesta frente |
| POST de compra e abertura de chamado sem chave de idempotência (COM-01, R-05-02) | Nova requisição pode repetir operação válida; não repetir POST automaticamente |
| Histórico de mensagens não registra ator de toda associação/encerramento (R-05-01) | Não atribuir fechamento ao suporte associado; evolução depende de requisito |
| Mídia, filesystem e base após crash | Reconciliação offline; sem coletor automático/antivírus/CDN/quota global |
| Seed automático preserva dados, não é reset | Datas relativas são calculadas na primeira carga; reinício não renova eventos nem restaura contas/linhas removidas |
| Avatar/perfil sem campo v0.2 | Fora do incremento 06; contrato inicial de upload de perfil não prova implementação |
| Mobile, acessibilidade assistiva, duração real de aula e E2E por três perfis | Planejados na matriz, não executados nesta frente |
| Enunciado/equipe e parecer externo 06 | Confirmar documentos acadêmicos e auditoria; merge não comprova aceite técnico externo |

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

Antes da entrega acadêmica: confirmar identificação da equipe e lista de GoF ensinados, executar e registrar a matriz no commit final, ensaiar os participantes e suportes em contextos independentes, conferir SVG/UML e obter revisão externa. Evoluções possíveis incluem idempotência de mutações, consultas em lote, auditoria de transições se exigida e manutenção de arquivos. Essas evoluções não são funcionalidades já entregues.

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
