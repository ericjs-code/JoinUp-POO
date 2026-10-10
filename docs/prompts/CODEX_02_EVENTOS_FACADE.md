# CODEX — Entrega 02: eventos, categorias, artistas e Facade real

> **Projeto:** `ericjs-code/JoinUp-POO` — Java 21 puro + JDK HttpServer + JDBC/MySQL 8  
> **Data:** 10/10/2026 | **Prazo RA2:** 15/10/2026; apresentação 16/10/2026  
> **Contexto:** PR #1 e PR #2 já estão mergeados. A Entrega 01 foi [auditada e aceita com ressalvas](../execucao/AUDITORIA_ENTREGA_01.md).  
> **Ramo de trabalho:** `feat/02-eventos-facade` partindo de **`main` atual**. **NÃO MERGEAR** sem auditoria do Tech Lead.

## 1. Missão e material obrigatório

Implementar um **fluxo demonstrável e persistente de eventos**, incluindo explorar eventos públicos, criar evento como organizador, editar dados gerais/lineup, administrar categorias de ingressos e criar a comunidade de forma automática e transacional. Aplicar o padrão GoF **Facade** como fachada útil coordenando subsistemas reais, e ajustar UML/testes. Usar telas reais, ainda que a paridade visual integral do PHP continue sendo incrementada.

**Leia integralmente antes de modificar:**
- `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` **e TODOS** `docs/requisitos/ADENDO_*.md`; fundamentais os adendos `RF-EVT-06`, `RF-EVT-07`, `RF-EVT-08`.
- `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`, `docs/arquitetura/PROPOSTA_JAVA_PURO_v0.1.md` e `docs/arquitetura/CLASSES_E_PADROES_GOF_v0.1.md`.
- `docs/diagramas/CLASSES_GOF_v0.1.puml` e outros diagramas versionados; comparar com classes reais.
- `docs/api/CONTRATOS_HTTP_v0.1.md`.
- `docs/execucao/AUDITORIA_ENTREGA_01.md`, `docs/banco/CORRECAO_DDL_01.md`, `docs/banco/VALIDACAO_INTEGRACAO_01.md` e `docs/execucao/PLANO_CODEX.md`.
- `src/main/resources/db/schema_v0.2.sql`, `seed_demo_v0.2.sql`.
- Frontend/fluxos de eventos e artistas do **repositório PHP anterior** `ericjs-code/JoinUp` somente para consulta de campos, navegação e identidade visual. **Não alterar o repo anterior**.

Respeitar as decisões vigentes, mesmo que o texto original de requisitos tenha pendências desatualizadas: adendos posteriores prevalecem. Não criar comportamento funcional novo sem registrar bloqueio/pergunta objetiva ao PO.

## 2. Restrições arquiteturais obrigatórias

- **Java 21 puro** (sem Spring Boot, Javalin, Servlet, ORM, Hibernate ou DI framework), HTTP nativo, JDBC com `PreparedStatement`, Jackson/JUnit, Maven.
- Reutilizar a fundação já existente, o Singleton `ConfiguracaoSistema`, Factory Method e Template Method implementados. **Não reescrever** cadastro/autenticação ou mudar o GoF aplicado. Com esta entrega os **quatro padrões GoF escolhidos** passam a estar presentes no sistema.
- Camadas e responsabilidades concretas: `eventos/http` ↔ `EventoFacade` (orquestração transacional) ↔ `EventoService`, `ArtistaService`, `TipoIngressoService`, `ComunidadeService` ↔ `...RepositoryJdbc`. A fachada não deve conter todo SQL/validação; subsistemas devem ter propósito real e ser testáveis. Repositórios e serviços recebem a mesma `Connection` transacional na operação integrada. Não abrir conexões próprias dentro de subsistemas chamados pela Facade.
- Autenticação, CSRF (mutáveis), papel, proprietário e conta ativa **validados no servidor**; não confiar em IDs do cliente para indicar dono. Transações coerentes com `ExecutorTransacional` e ordenação de locks, com rollback. Nunca expor hashes/senhas, dados pessoais privados ou stacktraces.
- Não adicionar um quinto padrão GoF artificial para cumprir RA2. Técnicas comuns de arquitetura (DTO, Repository) não devem ser falsamente anunciadas como quatro padrões formais.

## 3. Rotas HTTP e casos de uso

**Consultas públicas** sem login (RF-EVT-H03):
- `GET /api/eventos?busca=&generoId=&cidade=&pagina=&tamanho=`: explorar e filtrar, ordenar de forma determinística, paginação com validação. Não expor dados privados do organizador. Resposta paginada consistente com API existente.
- `GET /api/eventos/{eventoId}`: descrição, data, local, imagem se disponível, gênero, artistas/lineup, categorias disponíveis e referência à comunidade, sem expor membros/posts.
- `GET /api/eventos/{eventoId}/tipos-ingresso`: estoque e preços de catálogo. **Consulta não compra**.
- `GET /api/generos-musicais`: lista dos 15 gêneros demo e adicionais persistidos.
- `GET /api/artistas?busca=`: artistas do banco, sem expor CPF.
- `GET /api/organizador/eventos`: **organizador autenticado e ativo**, somente eventos do próprio usuário.

**Mutações** (CSRF/origem validada; organizador ativo e titular):
- `POST /api/organizador/eventos`: criar evento, lineup, uma ou mais categorias, **comunidade automaticamente**. Facade reúne tudo **em uma transação**, com retorno de IDs para exibir sucesso e navegação.
- `PATCH /api/organizador/eventos/{eventoId}`: editar campos gerais e lista de artistas/lineup conforme contrato; verificar que o evento cadastrado **ainda não começou** (RF-EVT-06), inclusive se já existirem compras; se a data for alterada, o novo instante precisa ser **estritamente futuro** (RF-EVT-08); preservar compras, ingressos, preço histórico e comunidade.
- `POST /api/organizador/eventos/{eventoId}/tipos-ingresso`: adicionar nova categoria **somente antes do início**; permitido se **outras** categorias tiverem vendas (RF-EVT-05/07).
- `PATCH /api/organizador/eventos/{eventoId}/tipos-ingresso/{tipoId}`: editar nome, preço e quantidade **somente se o evento ainda não começou e a categoria nunca teve nenhuma venda**, considerando histórico em `compra_itens`/ingressos e `quantidade_vendida` (RF-EVT-03/04/07). Quantidade de estoque não pode ser negativa. Não modificar `quantidade_vendida` manualmente.

**Não criar** `DELETE /api/eventos` nem remoção de categoria; essas ações não foram aprovadas.

## 4. Modelo real e validações

- Schema existente: `eventos` (`nome VARCHAR(45)`, `data DATETIME`, `descricao VARCHAR(450)`, `imagem VARCHAR(255)`, `localizacao VARCHAR(45)`, `cidade VARCHAR(60)`, `estado ENUM UF`, `cep VARCHAR(8)`, `id_organizador`, `id_genero_musical`), `artistas`, `line_up`, `generos_musicais`, `tipos_ingressos` (`nome_tipo`, `valor DECIMAL(8,2)`, `quantidade_disponivel`, `quantidade_vendida`), `comunidades` (`id_evento` único).
- Respeitar limites dos campos, referências válidas de gênero/artistas, artista repetido no lineup, preço com `BigDecimal` (sem `double`), estoque inteiro não negativo, CEP/UF, validações de datas. Dados de requisição sem autor/organizador do corpo; autoria sempre derivada da sessão.
- Interpretar instantes de data por `OffsetDateTime`/`Instant` ou equivalente, com política UTC em JDBC/MySQL documentada, sem depender do fuso local do servidor. A regra temporal vale em cada mutação. **Criar evento com data já passada não deve ser permitido se inviabilizar os fluxos demonstrativos**; se não estiver expressamente definido, trate como validação técnica coerente com cadastro e documente, sem extrapolar alterações de regra para eventos históricos do seed.
- Para edição de evento já vendido, não mudar `compras`, `compra_itens` ou `ingressos`; preservar snapshots de histórico. Mudanças de lineup não removem artistas de `artistas`, apenas associações; nunca apagar comunidade ou postagens.
- O frontend pode usar uma imagem já acessível via caminho público permitido, mas **não fingir upload implementado**. O endpoint multipart de upload continua pendente; não permitir caminho de arquivo local arbitrário, SSRF ou conteúdo remoto inseguro só para preencher o campo. Se necessário, exibir placeholder documentado.
- **Pontos relevantes de segurança:** permissões para visualizar evento são públicas, mas dados privados de usuários, comentários de comunidade e chamados não devem aparecer nos DTOs públicos. Organizador inativo não pode administrar, mas os eventos continuam consultáveis.

## 5. Casos especiais/erros esperados

Teste validações **por backend**, não apenas frontend:
- Evento futuro sem vendas → criar/editar permitido ao dono.
- Evento futuro com vendas → dados gerais e lineup editáveis, **sem alterar registros históricos**.
- Evento já iniciado/encerrado → edição geral, criação e edição de categorias bloqueadas (RF-EVT-06/07).
- Reagendamento de evento futuro para data passada ou agora → rejeitar (RF-EVT-08). Novo instante futuro → aceitar.
- Categoria vendida → nome, preço e quantidade bloqueados, mesmo se houver estoque.
- Categoria sem vendas → edição permitida antes do evento, mesmo se outra categoria foi vendida.
- Categoria nova → permitida antes do evento, mesmo após vendas em outras categorias.
- Usuário com perfil de participante/suporte, organizador diferente, organizador desativado e visitante → rejeição adequada de mutações sem vazamento; tentativas diretas em URL não passam.
- Evento inexistente, tipo de ingresso pertencente a outro evento, artista/gênero inválido, payload sem dados, preço inválido, duplicidade de artista, IDs não positivos e limite temporal na borda → 400/403/404/409/422 pertinentes e estáveis.
- **Rollback proposital:** provocar falha ao inserir lineup, categoria ou comunidade depois de inserir o evento; conferir que **nenhuma etapa** foi persistida e que o estado do banco permanece íntegro.
- Edição concorrente com alterações de data/estoque → serializar ou revalidar sob lock da linha de evento; evitar check-then-act separado da transação.

## 6. Frontend útil e verificável

- Reproduzir progressivamente identidade visual/navegação do JoinUp PHP original, integrando à UI de contas da Entrega 01.
- Tela pública de explorar eventos com filtros/pesquisa, cards reais a partir da API e detalhes de evento.
- Área de organizador: lista de seus eventos, formulário completo de criação, formulário de edição, componentes para lineup e categorias com bloqueios explicados.
- Reutilizar login e sessão existentes, tratar erros do backend, incluir header Origin implícito do navegador no fetch mutável, não armazenar token em `localStorage`.
- Mostrar dados reais persistidos (inclusive 4 eventos demo), não somente componentes demonstrativos com arrays fixos. Registrar capturas ou roteiro de teste de navegador.
- Não declarar pronta a compra direta ou revenda: preço/estoque nesta etapa são somente exibição e gestão.

## 7. Testes e CI obrigatórios

- Preservar testes **109** existentes ou justificar regressões com provas. O CI da `main` já executa `-Pmysql-it clean verify` com `mysql:8.4.0`; manter e ampliar, sem substituí-lo por mocks.
- Criar testes unitários para `EventoFacade` verificando **coordenação real** dos serviços, e testes HTTP/JDBC MySQL para fluxos integrados.
- Testar CRUD aprovado de eventos sem DELETE, criação de comunidade 1:1, lineup, tipagem de preço/estoque, validação de titularidade, eventos e categorias vendidos, rejeição após início, reagendamento futuro, normalização de fuso e tentativas concorrentes.
- Usar fixtures da base demonstrativa e dados de teste isolados, sem deixar poluição irreversível. Não executar `DROP DATABASE` nem importar banco legado.
- Documentar resultados concretos local/CI: total de testes, versão MySQL, comando, PR e eventuais limitações; evidência de rollback. Não confundir `/api/health` com conectividade ao MySQL.
- Atualizar PlantUML GoF mostrando `EventoFacade` **implementado**, assinaturas corretas e dependências reais; preparar fragmento de UML de classes/domínio para o relatório RA2 (a composição final será revisada posteriormente).

## 8. Ajustes pequenos da auditoria anterior

- Cabeçalho do `schema_v0.2.sql` e `seed_demo_v0.2.sql` ainda diz **NÃO EXECUTADO**; a correção DDL e integração já foram documentadas e validadas em MySQL 8.4. Atualizar **somente os comentários de metadados** para refletir validação pós-correção, apontando `docs/banco/VALIDACAO_INTEGRACAO_01.md`; **não refatorar SQL** sem necessidade objetiva.
- Não refazer autenticação/rotas dinâmicas/CSRF já auditadas. Mudanças inevitáveis nessas áreas requerem justificativa e testes de regressão.

## 9. Fora do escopo desta entrega

- **Não implementar ainda:** compra direta, baixa de estoque por compra, emissão de ingressos, compra/criação/cancelamento de revenda, filiação manual a comunidade, feed/posts/chat de comunidade, chamados de suporte, saque/notificações, pagamento real, dashboards finais ou upload multipart.
- Não criar novo schema, não migrar dados do PHP, não mudar fluxos aprovados e não abrir integração externa real.
- Não adicionar novo GoF que substitua um dos quatro acordados.

## 10. Entregáveis e retorno

1. Branch `feat/02-eventos-facade` e **Pull Request para revisão sem merge**.
2. Java, repositórios JDBC, telas HTML/CSS/JS e testes de integração demonstrando Facade transacional.
3. Documentação `docs/execucao/ENTREGA_02_RESULTADOS.md` com IDs de RF-EVT, limites efetivamente cumpridos, commits, comandos e saídas reais.
4. UML de classe GoF atualizado para a implementação verdadeira; nenhuma classe de apresentação sem uso.
5. Explicitar diferença entre **regras aprovadas** e hipóteses técnicas (especialmente horário de criação e tratamento de imagem).
6. Status CI com links, lacunas de frontend e próximos passos.
7. **Não fazer merge automático.** Retornar URL do PR à revisão técnica.

**Definição de pronto:** criar e editar um evento completo em transação única com Facade real, categoria/lineup/comunidade persistidos e testados no MySQL real, validação de autorização e datas comprovada, consulta pública e interface utilizável, regressão verde e UML consistente. Se algum item faltar, abrir PR parcial claramente identificado.
