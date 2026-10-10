# JoinUp-POO — Proposta de Arquitetura (Java puro)

> **Versão:** 0.1 — 10/10/2026  
> **Situação:** PROPOSTA PARA VALIDAÇÃO DO PO — não autoriza implementação  
> **Base funcional:** [Especificação homologada v0.1](../requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md)  
> **Decisão do PO:** usar **Java puro**, sem Spring Boot, Javalin ou outros frameworks HTTP/de injeção de dependência.

## 1. Objetivos e princípios

Reimplementar o JoinUp em Java, com frontend renovado mas visual e fluxos próximos do original, banco MySQL independente, seed demonstrativo e preservação de todas as regras aprovadas. Priorizar código orientado a objetos legível, consistência transacional, defesa no backend e prova concreta dos **exatamente quatro padrões GoF escolhidos**: Facade, Template Method, Factory Method e Singleton.

Não migrar código PHP literalmente, nem transformar operações simuladas do original em gateways/integrações não aprovados.

## 2. Stack proposta

| Responsabilidade | Escolha | Observações |
| --- | --- | --- |
| Linguagem/execução | **Java 21 (JDK)** | Sistema de módulos Java opcional; não é obrigatório usar module-info para organizar pacotes |
| Servidor HTTP | `com.sun.net.httpserver.HttpServer` do módulo **`jdk.httpserver`** | API disponível no JDK; rotas, filtros, cookies, tratamento de erros e multipart exigem implementação explícita |
| Persistência | JDBC (`java.sql`) + transações SQL | Sem JPA/Hibernate |
| Driver de banco | MySQL Connector/J | Dependência técnica, não é framework |
| Banco | MySQL 8 | Instância/banco novos, sem importar registros antigos |
| Frontend | HTML, CSS, JavaScript nativos | Arquivos estáticos servidos pelo backend; mesma origem para API e interface |
| JSON | **Jackson Databind**, biblioteca de serialização recomendada | Não é framework HTTP nem substitui a arquitetura Java; caso “Java puro” signifique proibição total de bibliotecas, essa escolha precisa ser explicitada antes do código; JSON manual não é recomendado |
| Build | Maven | Gerenciador de dependências, não framework |
| Testes | JUnit 5 + testes HTTP/JDBC | Dependência de desenvolvimento |
| Diagramas | PlantUML | Classes refletindo o código final |

**Delimitação de Java puro:** não empregar Spring, Javalin, Jakarta servlet container, ORM ou contêiner de injeção de dependência. Usar bibliotecas pontuais (driver JDBC, Jackson, testes) sem delegar a elas arquitetura ou regras de negócio.

## 3. Arquitetura em camadas e direção de dependências

```text
Browser (HTML/CSS/JS)
      │ HTTP/JSON + cookie de sessão
      ▼
JDK HttpServer
      ├── Filtros: erros, sessão, autorização, proteção de requisição
      ▼
Handlers HTTP (rotas e DTOs; sem SQL e sem regras de domínio)
      ▼
Casos de uso / Services / Facade (validação, transações e coordenação)
      ▼
Domínio (entidades, invariantes e quatro GoF)
      ▼
Repositories JDBC + SQL (injetados explicitamente por construtores)
      ▼
MySQL 8
```

**Injeção manual:** uma `ApplicationBootstrap` monta `DataSource/ConnectionFactory`, repositórios, serviços e handlers por construtor. Não usar `new` de repositório em handlers, nem conexões JDBC singleton.

**HTTP:** `HttpServer` escuta a porta configurada, usa pool de executores, handlers por prefixo e roteador interno simples por método+rota; todos os endpoints de negócio recebem/retornam JSON. Páginas, CSS, JS e imagens públicas serão expostos sob rotas estáticas controladas. Arquivos enviados por usuários serão salvos fora da árvore pública e servidos com checagens apropriadas. Tratamento de `multipart/form-data` requer parser seguro ou biblioteca auxiliar isolada, sujeito a decisão técnica.

## 4. Organização sugerida

```text
pom.xml
README.md
src/main/java/br/com/joinup/
    Main.java
    bootstrap/ApplicationBootstrap.java
    config/ConfiguracaoSistema.java
    http/{Router,JsonCodec,ErrorHandler,StaticFileHandler,SessionFilter}.java
    auth/{AutenticacaoTemplate,AutenticacaoParticipante,AutenticacaoOrganizador,AutenticacaoSuporte,...}
    usuario/{domain,creator,service,repository,http}
    evento/{domain,facade,service,repository,http}
    ingresso/{domain,service,repository,http}
    revenda/{domain,service,repository,http}
    comunidade/{domain,service,repository,http}
    social/{domain,service,repository,http}
    suporte/{domain,service,repository,http}
    shared/{db,security,errors,util}
src/main/resources/
    public/{index.html,css/,js/,assets/}
    db/{schema.sql,seed.sql}
src/test/java/br/com/joinup/
docs/{requisitos/,arquitetura/,diagramas/,relatorio/}
```

A organização final pode agrupar packages menores caso beneficie legibilidade; nenhum artefato de negócio deve existir apenas para decorar a apresentação GoF.

## 5. Mapeamento dos quatro padrões GoF

### 5.1 Facade — operação integrada de eventos

**Motivação:** criar evento exige coordenar evento, lineup/artistas, tipos de ingressos e comunidade com consistência transacional.

**Papéis propostos:**
- `EventoFacade`: interface simplificada `criarEvento(CriarEventoRequest, OrganizadorAutenticado)` e `editarEvento(...)`; valida dono e centraliza transação multi-repositório.
- `EventoService`, `ArtistaService`, `TipoIngressoService`, `ComunidadeService`: subsistemas reais, com suas responsabilidades e validações.
- `EventoRepository`, `TipoIngressoRepository` etc. executam persistência JDBC.

**Transação:** uma única `Connection` é aberta para o caso de uso, `setAutoCommit(false)`, passada para as operações persistentes, e confirmada somente após todas concluírem. Qualquer erro causa `rollback`. O fechamento da conexão ocorre em `finally/try-with-resources`.

**Observação:** fachada não significa classe monolítica; regras de estoque e ingresso continuam nos serviços adequados.

### 5.2 Template Method — login por perfil

**Motivação:** os três perfis partilham uma sequência de autenticação com etapas específicas de consulta/validação por perfil.

**Papéis propostos:**
- `AutenticacaoTemplate`: método **final** `autenticar(credenciais)` fixa sequência: normalizar entrada; buscar usuário do perfil; verificar hash e status ativo; executar hook do perfil; criar sessão; devolver usuário autenticado.
- `AutenticacaoParticipante`, `AutenticacaoOrganizador`, `AutenticacaoSuporte`: implementam a etapa abstrata `buscarUsuarioDoPerfil` e hooks comprovadamente necessários.
- Serviços auxiliares de hash, sessões e dados persistidos são colaboradores, não subclasses.

**Prova necessária:** testes dos três caminhos e de impedimento de login de conta desativada; evitar subclasses que só retornam enum sem mudança real no algoritmo.

### 5.3 Factory Method — cadastro polimórfico de usuários

**Motivação:** usuário em domínio e fluxo de cadastro dependem do perfil; a lógica chamadora deve depender de um contrato abstrato e não montar entidades condicionais diretamente.

**Papéis propostos:**
- `Usuario`: produto abstrato, com tipos concretos `Participante`, `Organizador`, `UsuarioSuporte`.
- `CadastroUsuarioCreator`: criador abstrato com a operação `cadastrar(...)` e método fábrica **abstrato/protegido** `criarUsuario(DadosCadastro)`.
- `CadastroParticipanteCreator`, `CadastroOrganizadorCreator`, `CadastroSuporteCreator`: concrete creators com métodos fábrica sobrescritos.

O serviço/handler público de cadastro usa o concrete creator previamente escolhido na composição da aplicação. O cadastro de suporte só pode ser invocado por suporte autenticado; o controle de autorização ocorre **antes** de persistir o produto.

**Prova necessária:** `Creator → Product`, `ConcreteCreator → ConcreteProduct`, criação real no cadastro e testes; não substituir Factory Method por helper estático `switch`/`if`.

### 5.4 Singleton — configuração compartilhada

**Motivação:** fornecer uma leitura única e coerente de parâmetros estáveis de configuração a múltiplos módulos, sem permitir instâncias divergentes ou estados concorrentes mutáveis.

**Papéis propostos:** `ConfiguracaoSistema` `final`, construtor privado, inicialização thread-safe via *initialization-on-demand holder* e `getInstancia()`. Dados imutáveis, derivados de variáveis de ambiente/arquivo de propriedades lido uma vez, como limites de arquivo, porta e modo de demonstração.

**Contraindicação:** não encapsular `Connection`, contexto da requisição, usuário logado, sessão, estado mutável de compras ou repositórios. Aplicabilidade precisa ser justificada no relatório (configuração única da aplicação), com teste de identidade e segurança de inicialização. Se a exigência parecer artificial após projeto de classes, propor revisão antes de implementar.

## 6. Autenticação, segurança e autorização

- Login por participante, organizador e suporte; sessão em cookie **HttpOnly**, `SameSite=Lax`, `Secure` sob HTTPS, escopo de cookie restrito. Token de sessão aleatório criptograficamente seguro; persistir somente hash do identificador de sessão e prazo de expiração no banco.
- Hash de senha com **PBKDF2WithHmacSHA256**, sal individual e fator de trabalho adequado, implementável com API nativa do JDK; nunca senha em texto claro. Rate limit de login quando viável.
- O servidor sempre valida usuário ativo, perfil e propriedade do recurso; desativação invalida sessões existentes no mesmo fluxo transacional.
- Requisições que modificam estado precisam de defesa contra CSRF (token ou verificação rigorosa de origem, conforme contrato final). Validar tamanho de JSON/uploads e restringir CORS à própria origem.
- Logs de operações administrativas como reativação indicam quem realizou, qual usuário e quando.
- Proibir acesso a conteúdo da comunidade sem membro ativo **e** ingresso válido, checado no backend em cada recurso.
- Suporte autenticado cria outro suporte e pode reativar participante/organizador; não criar papel especial de suporte-administrador.

## 7. Banco, atomicidade e concorrência

**Entidades conceituais:** `usuarios`/detalhes por perfil, `sessoes`, `eventos`, `artistas`, `evento_artistas`, `tipos_ingresso`, `compras`, `itens_compra`, `ingressos`, `anuncios_revenda`, `comunidades`, `comunidade_membros`, `mensagens`, `postagens`, `curtidas`, `comentarios`, `tickets_suporte`, `mensagens_suporte`, `auditoria`. O diagrama entidade-relacionamento e colunas são sujeitos a aprovação e compatibilidade com telas originais.

**Compra direta:** bloquear categoria/estoque por `SELECT ... FOR UPDATE` em transação; validar status de organizador, data do evento, disponibilidade; inserir compra/ingressos e reduzir estoque atomicamente. Histórico de vendas mantém tipo de ingresso bloqueado para edição manual mesmo com estoque remanescente.

**Revenda:** bloquear anúncio, ingresso e proprietários relevantes na mesma transação; validar vendedor ativo, anúncio disponível, organizador ativo, evento futuro, preço/elegibilidade e comprador diferente do vendedor; marcar anúncio vendido e transferir ingresso atomicamente; ao transferir último ingresso válido do vendedor, remover sua associação de comunidade sem apagar histórico.

**Desativação:** participante desativa conta e cancela anúncios disponíveis numa transação; organizador desativado bloqueia compras diretas, compras de revenda e criação de anúncios, mantendo anúncios já existentes em status `disponivel` porém inelegíveis temporariamente. Reativação por suporte não religa anúncios cancelados automaticamente.

**Suporte:** status final `fechado`, `aguardando_usuario` não pode ser fechado por suporte; guardar autor verdadeiro de cada mensagem; todas as transições seguirão diagrama de estados.

**Seed demo:** schema e seed idempotentes para ambiente de demonstração, com conta inicial de suporte, registros variados e cenários completos. Credenciais documentadas somente para execução local, sem publicar segredos reais.

## 8. Estratégia de interface

O frontend será construído em HTML, CSS e JavaScript sem framework, inspirado na identidade visual do PHP. Telas acessam API JSON por `fetch` no mesmo host/porta. Manter rotas e navegação funcionalmente equivalentes, inclusive telas demonstrativas; não implementar pagamento real, serviço push/e-mail de notificações ou solicitação bancária efetiva de saque.

## 9. Testes e avaliação RA2

- Testes unitários Java dos quatro padrões e suas classes participantes.
- Testes de integração com MySQL (transação multi-etapas, rollback, estoque concorrente, revenda única, cancelamento por desativação, suporte e comunidades).
- Testes HTTP de autorização, autenticação e estados; testes manuais de navegação do frontend usando a carga demo.
- PlantUML de classes consistente com classes e assinaturas reais; relatório motiva cada GoF, papéis, tradeoffs e motivos de não aplicação dos demais estudados; preparar roteiro de demonstração de funcionalidades e padrões.

## 10. Questões de aprovação antes de implementação

1. **Aprovação da fronteira de “Java puro”**: JDK `HttpServer` nativo, JDBC, Maven e bibliotecas pontuais (MySQL Connector/J, Jackson e JUnit), sem frameworks.
2. Ajustes definitivos em permissões de contas de suporte e estados de ticket, somente quando forem realmente necessários para concluir telas e casos de uso.
3. Aprovação do diagrama de classes dos quatro padrões (depois de refinado), entidades e relações do banco e contratos principais.
4. Teste de capacidade para manter escopo integral até 15/10/2026; organizar priorização e comprovação acadêmica, sem cortar funcionalidades sem autorização.

> **Status:** decisão de Java puro registrada; restante da arquitetura ainda é proposta. Não contém implementação.
