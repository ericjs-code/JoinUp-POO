# Entrega 04 — Comunidades, Feed Social e Mensagens

Data: **11/10/2026 (America/Sao_Paulo)**. Branch `feat/04-comunidades-social`,
criada a partir da `main` atualizada. Implementação submetida para auditoria técnica
do Tech Lead **antes do merge**, conforme plano orientado por Codex.

---

## 1. Escopo e Fontes de Verdade

- **Documentação de referência:**
  - `docs/prompts/CODEX_04_COMUNIDADES_SOCIAL.md` (especificação do sprint).
  - `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` (seções 3.5 e 3.6).
  - `docs/requisitos/ADENDO_RF-COM-05_COMUNIDADES_POS_EVENTO.md` (permanência e acesso pós-evento para ingressos ativos ou utilizados).
  - `docs/requisitos/ADENDO_RF-COM-06_DESCOBERTA_PUBLICA.md` (descoberta pública de comunidades; bloqueio de mensagens/feed para não autorizados).
  - `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md` (Java 21 puro, `jdk.httpserver`, JDBC puro, Jackson, Maven, JUnit 5).
  - `docs/arquitetura/CLASSES_E_PADROES_GOF_v0.1.md`.
  - Schema MySQL `src/main/resources/db/schema_v0.2.sql` (tabelas `comunidades`, `participante_comunidades`, `comunidade_mensagens`, `postagens`, `comentarios`, `postagem_curtidas`).

- **Restrições respeitadas:**
  - Nenhum framework externo (sem Spring Boot, Javalin, Hibernate/JPA, external DI).
  - **Preservação exata dos 4 padrões GoF:** Singleton (ConfiguracaoSistema), Factory Method (CadastroUsuarioCreator), Template Method (AutenticacaoTemplate) e Facade (EventoFacade). **Nenhum 5º padrão GoF introduzido**.
  - Proibição absoluta de endpoints para alteração ou exclusão de postagens (**RF-SOC-01**).
  - Transações atômicas com `ExecutorTransacional` e isolamento READ_COMMITTED.
  - Segurança server-side: autorização em todas as rotas protegidas por perfil, titularidade e integridade de sessão.

---

## 2. Rastreabilidade de Requisitos e Critérios de Aceite

| Requisito / Critério | Descrição Funcional | Implementação e Evidência |
| --- | --- | --- |
| **RF-COM-06** / **CA-36..38** | Descoberta pública de comunidades | `GET /api/comunidades` e `GET /api/comunidades/{id}` abertos a visitantes sem autenticação, retornando metadados do evento. Conteúdo interno (mensagens, postagens) bloqueado com HTTP 401/403. |
| **RF-COM-01** / **RF-COM-01A** / **CA-39..40** | Associação voluntária e manual | `POST /api/comunidades/{id}/participacao` restrito a participantes autenticados com ingresso elegível (`status IN ('ativo', 'utilizado')`). Não há entrada automática na compra. |
| **RF-COM-02** / **CA-28** | Saída voluntária da comunidade | `DELETE /api/comunidades/{id}/participacao` realiza inativação lógica (`ativo = 0`, `data_saida = UTC_TIMESTAMP()`). Bloqueia acesso ao feed e chat até nova entrada voluntária. |
| **RF-COM-05** / **CA-25..27** | Preservação de acesso pós-evento | Participantes com ingresso `ativo` ou `utilizado` mantêm acesso contínuo às comunidades mesmo após a data do evento. Ingressos `cancelado` são explicitamente rejeitados. |
| **RF-COM-H02** / **RF-COM-01** | Mensagens da comunidade (Chat) | `GET` e `POST /api/comunidades/{id}/mensagens` acessíveis por membros participantes e pelo organizador proprietário do evento vinculado. Gravação correta de `autor_tipo` ('participante' ou 'organizador'). |
| **RF-SOC-H01..H04** / **RF-COM-01B** | Feed social, comentários e curtidas | `GET` e `POST /api/comunidades/{id}/postagens`, `GET` e `POST /api/postagens/{id}/comentarios`, `PUT` e `DELETE /api/postagens/{id}/curtida`. Curtidas atômicas e idempotentes atualizando contador na tabela `postagens`. |
| **RF-SOC-01** | Proibição de edição e exclusão de posts | Não existem rotas de `PUT`/`PATCH`/`DELETE` em `/api/postagens/{id}` para alteração textual ou remoção do feed. Regra garantida por desenho na API e no serviço. |

---

## 3. Arquitetura e Componentes Desenvolvidos

### 3.1 Camada de Domínio e DTOs (`br.com.joinup.comunidades`)
- **`domain.ComunidadeResumo`**: Registro imutável de metadados da comunidade e evento associado.
- **`domain.MensagemComunidade`**: Mensagem do chat com discriminação de autor (`participante` / `organizador`), referências e timestamp.
- **`domain.Postagem`**: Publicação do feed com contadores de curtidas e comentários e flag `curtidoPeloUsuario`.
- **`domain.Comentario`**: Comentário cronológico de uma postagem com identificação do autor participante.
- **`dto.ComunidadeDetalheView`**: View enriquecida para detalhe de comunidade, contendo flags de contexto `membroAtivo` e `elegivelParaEntrar`.
- **`dto.EnviarMensagemRequest`**, **`dto.CriarPostagemRequest`**, **`dto.CriarComentarioRequest`**: DTOs validados para payloads de entrada.

### 3.2 Persistência e Repositório (`br.com.joinup.comunidades.repository`)
- **`JdbcComunidades`**: Classe utilitária com métodos estáticos de inserção, execução e mapeamento JDBC.
- **`ComunidadeSocialRepositoryJdbc`**: Implementação JDBC pura com queries parametrizadas (`PreparedStatement`), mapeamento de `ResultSet` seguro e controle transacional. Inclui:
  - Checagem de elegibilidade por ingresso (`status IN ('ativo', 'utilizado')`).
  - Checagem de propriedade de evento para organizadores donos.
  - Associação e desassociação com gerenciamento de `data_entrada`, `data_saida` e flag `ativo`.
  - Inserção/deleção idempotente em `postagem_curtidas` coordenada com `UPDATE postagens SET curtidas = ...`.

### 3.3 Serviço e Regras de Negócio (`br.com.joinup.comunidades.service`)
- **`ComunidadeSocialService`**:
  - Validações de segurança, perfil e autorização de acesso ao conteúdo da comunidade.
  - Execução via `ExecutorTransacional` para mutações consistentes.
  - Restrição de postagens e comentários exclusivamente a participantes ativos (organizadores têm acesso de leitura ao feed e participação no chat).

### 3.4 Rotas HTTP (`br.com.joinup.comunidades.http`)
- **`ComunidadeRoutes`**: Registro no `Router` de todos os 13 endpoints sob `/api/comunidades` e `/api/postagens`. Validação de sessões através de `GerenciadorSessao` e respostas padronizadas em JSON com `HttpResponses`.

### 3.5 Integração no Bootstrap (`br.com.joinup.bootstrap`)
- Composição de instâncias no `ApplicationBootstrap`: injeção de `ComunidadeSocialRepositoryJdbc`, `ComunidadeSocialService` e registro de `ComunidadeRoutes`.

### 3.6 Frontend e Interface com o Usuário
- **`src/main/resources/public/comunidades.html`**: Interface moderna e responsiva com listagem de comunidades, painel de detalhes, status de filiação com botões de entrar/sair, aba de Chat/Mensagens e aba de Feed Social com publicações, comentários expansíveis e botão de curtir com contagem reativa.
- **`src/main/resources/public/css/comunidades.css`**: Estilização alinhada com o design system do JoinUp (`conta.css`, `eventos.css`).
- **`src/main/resources/public/js/comunidades.js`**: Lógica de cliente em Vanilla JS para consumo das rotas da API, alternância de abas, renderização de cards e tratamento de erros.
- **Atualização dos menus de navegação:** Links para `/comunidades.html` adicionados nas páginas existentes (`index.html`, `conta.html`, `eventos.html`, `ingressos.html`).

---

## 4. Evidências de Testes e Qualidade

A suíte completa de testes automatizados foi executada localmente via Maven wrapper com Java 21:

```bash
mvnw.cmd test
```

### Resultados da Execução
- **Total de testes executados:** 136
- **Falhas:** 0
- **Erros:** 0
- **Ignorados / Skipped:** 0
- **Status da compilação:** `BUILD SUCCESS`

### Detalhamento por Classe de Teste
1. `br.com.joinup.comunidades.ComunidadeSocialTest`: **9 testes** (todos passaram)
   - `participanteComIngressoAtivoOuUtilizadoMantemAcessoPosEvento` (CA-25, CA-26)
   - `participanteApenasComIngressoCanceladoNaoTemAcesso` (CA-27)
   - `participanteComSaidaVoluntariaFicaBloqueado` (CA-28)
   - `participanteComIngressoElegivelSemEntradaManualNaoAcessaConteudo` (CA-39, CA-40)
   - `organizadorDonoDoEventoTemAcessoAComunidade`
   - `organizadorDeOutroEventoNaoTemAcessoAComunidade`
   - `curtidaEmPostagemEIdempotente`
   - `descurtidaEmPostagemDecrementaCurtidasSemNegativo`
   - `naoPermiteCriacaoDePostagemSemDescricaoValida`
2. `br.com.joinup.auth.AuthenticationSecurityTest`: **18 testes** (regressão OK)
3. `br.com.joinup.bootstrap.ApplicationBootstrapTest`: **2 testes** (regressão OK)
4. `br.com.joinup.compras.ValidacaoComercioTest`: **16 testes** (regressão OK)
5. `br.com.joinup.config.ConfiguracaoSistemaTest`: **16 testes** (regressão OK)
6. `br.com.joinup.eventos.facade.EventoFacadeTest`: **4 testes** (regressão OK)
7. `br.com.joinup.eventos.ValidacaoEventoTest`: **21 testes** (regressão OK)
8. `br.com.joinup.http.HttpFoundationTest`: **33 testes** (regressão OK)
9. `br.com.joinup.http.RouterParametersTest`: **16 testes** (regressão OK)
10. `br.com.joinup.shared.db.SeedDemoPasswordTest`: **1 teste** (regressão OK)

---

## 5. Próximos Passos Recomendados

O resultado de 136 testes acima é histórico de testes unitários, não evidência
HTTP/MySQL do módulo. A homologação está condicionada à Correção 04.1, incluindo
compartilhamento e pendências de mídia; ver [resultados](CORRECAO_04_1_RESULTADOS.md).

1. **Revisão e Auditoria pelo Tech Lead** da branch `feat/04-comunidades-social`.
2. Após aprovação técnica, integração na `main`.
3. Início da **Entrega 05 — Suporte e Painéis** (`docs/prompts/CODEX_05_SUPORTE_PAINEIS.md`), cobrindo tickets de atendimento, mensagens de suporte e dashboards administrativos.
