# JoinUp-POO — Detalhamento inicial de classes e padrões GoF

> **Versão:** 0.1 — 10/10/2026  
> **Status:** PROPOSTA PARA VALIDAÇÃO PELO PO, sem implementação  
> **Diagrama:** [Classes GoF — PlantUML](../diagramas/CLASSES_GOF_v0.1.puml)  
> **Stack aceita:** [ADR-001 — Java puro](./ADR-001-STACK-JAVA-PURO.md)

## Critérios de organização

1. **Handlers HTTP** somente traduzem pedidos/respostas JSON, validam dados de transporte e exigem sessão/permissão.
2. **Services/casos de uso** implementam regras de negócio e lidam com transações conforme necessidade.
3. **Entidades e objetos de domínio** representam usuários, eventos, tipos de ingresso, ingressos, anúncio, comunidade, postagens e chamados.
4. **Repositories JDBC** encapsulam queries SQL e recebem uma `Connection` da unidade de trabalho, nunca usam conexão JDBC global.
5. **Bootstrap** compõe objetos manualmente por construtor: `UsuarioRepositoryJdbc`, `EventoRepositoryJdbc`, services, handlers e rotas. Evitar framework DI.
6. **Coordenadores de transação** executam `commit/rollback` para múltiplas alterações relacionadas.
7. Estrutura de pacotes dividida pelos módulos funcionais reais; o GoF deve resolver um problema real e possuir evidências e testes próprios.

## Facade — EventoFacade

**Problema:** criar eventos não se limita a salvar `Evento`: exige lineup, categorias de ingresso e comunidade, sem resultados parciais em caso de falha.

- `EventoFacade`: entrada única para o caso de uso criar/editar, autoriza organizador dono, recebe o executor transacional e coordena subsistemas.
- `EventoService`: propriedade e campos gerais de evento.
- `ArtistaService`: lineup/artistas.
- `TipoIngressoService`: tipos de ingresso e validação de bloqueio de tipos com vendas; permite inclusão de novos tipos após vendas de outros.
- `ComunidadeService`: cria comunidade do evento automaticamente.
- `ExecutorTransacional`: abre uma conexão JDBC, cria transação, faz commit ou rollback, fecha conexão.

**Limite:** não centralizar todas as regras na fachada; validações de estoque e emissão de ingressos continuam no módulo de ingressos.

**Teste:** criar evento com categoria/artista/comunidade; forçar falha na última etapa e comprovar rollback completo.

## Template Method — AutenticacaoTemplate

**Problema:** três logins muito parecidos precisam de fluxo uniforme, mas consultas por papel e restrições de perfil são diferentes.

- `AutenticacaoTemplate.autenticar()` é `final` e define sequência: normalizar credenciais → localizar conta no repositório específico → verificar senha → verificar conta ativa → validar regras do perfil → criar sessão.
- `AutenticacaoParticipante`, `AutenticacaoOrganizador` e `AutenticacaoSuporte`: subclasses reais que implementam busca e validação específicas.
- `VerificadorSenha` e `GerenciadorSessao` são colaboradores reutilizáveis.

**Teste:** os três perfis autenticam de forma independente; senha errada e conta inativa são rejeitadas; sessão invalidada após desativação.

## Factory Method — CadastroUsuarioCreator

**Problema:** cadastrar participante, organizador e suporte produz entidades diferentes e exige extensibilidade sem instanciar tipo concreto nos fluxos genéricos.

- `Usuario`: abstração base, estendida por `Participante`, `Organizador` e `UsuarioSuporte`.
- `CadastroUsuarioCreator`: criador abstrato com `cadastrar(...)` que chama o método fábrica protegido e polimórfico `criarUsuario(...)`.
- `CadastroParticipanteCreator`, `CadastroOrganizadorCreator`, `CadastroSuporteCreator`: subclasses com implementação concreta do método fábrica.
- `UsuarioRepository`: porta de persistência; adaptação JDBC é separada.
- Regra de acesso ao cadastro de suporte obrigatoriamente aplicada na entrada (handler/caso de uso) e revalidada no nível do serviço, não apenas por ocultação de tela.

**Teste:** para cada criador, produto do tipo correto; cadastro de suporte público rejeitado; sem retorno de usuário por um único `switch` estático.

## Singleton — ConfiguracaoSistema

**Problema:** inicialização única e leitura consistente de configuração imutável por componentes da aplicação.

- `ConfiguracaoSistema` é `final`, construtor privado, acesso estático `getInstancia()` com inicialização thread-safe e getters imutáveis.
- Configuração deriva de ambiente/propriedades, com defaults adequados para execução local. Não carregar senhas como strings publicadas.
- O bootstrap utiliza a instância e repassa parâmetros aos componentes que precisam deles.
- Não armazenar sessão, usuários, `Connection`, serviços ou estado mutável de compra.

**Teste:** mesma referência obtida de diversas chamadas e threads; valores de configuração imutáveis.

## Contratos de módulos e validações fundamentais

| Módulo | Entrada/caso de uso | Invariantes |
| --- | --- | --- |
| Usuário | cadastrar, autenticar, desativar, reativar por suporte | login só conta ativa; suporte pode cadastrar suporte; desativar participante cancela anúncios em transação |
| Evento | criar, editar, explorar/listar | só dono edita; edição de tipo vendido bloqueada; novos tipos permitidos |
| Ingresso | comprar e consultar | bloqueio se evento passado ou organizador inativo; estoque não negativo; histórico preservado |
| Revenda | anunciar, cancelar, reanunciar, comprar | dono legítimo; anúncio único ativo por ingresso; bloquear novo anúncio e compra se organizador inativo; última venda remove membro |
| Comunidade | aderir, sair, ler e enviar mensagens | posse de ingresso válido + associação ativa; ingresso anunciado preserva acesso |
| Social | publicar, curtir/descurtir, comentar, visualizar | autorização idêntica ao controle de comunidade; sem edição/exclusão de post |
| Suporte | abrir, listar, responder, resolver/retornar, fechar | fechado definitivo; qualquer suporte autenticado responde; suporte não fecha aguardando usuário |

## Qualidade, modelagem e prazos

**Antes de implementar:** validar classes e padrões; detalhar entidades/relacionamentos e endpoints; conferir estados de suporte; definir contratos de sessão, resposta de erro e upload; preparar matriz de rastreabilidade entre funcionalidades PHP, telas, Java e testes.

**Critérios mínimos de demonstração acadêmica:** quatro GoF no fluxo executável, UML coerente com código, motivação, participantes, prós/contras e exemplos, mais execução local e dados demonstrativos. A especificação de requisitos continua a autoridade para regras e comportamentos.

**Risco:** preservar todo o sistema original em poucos dias é escopo elevado; acompanhamento de cobertura por requisito e cortes apenas após aprovação do PO.

> Este arquivo e o PlantUML são **propostas**, não diagrama final nem autorização de código.
