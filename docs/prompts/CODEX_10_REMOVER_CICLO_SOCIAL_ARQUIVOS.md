# CODEX — Entrega 10 (CONDICIONAL): remover ciclo entre ComunidadeSocialService e ArquivoService

> **NÃO INICIAR durante as Entregas 08 ou 09.** Somente executar após homologação do fluxo demonstrativo, ou caso o PO priorize expressamente essa refatoração antes do prazo. A RA2 vence em 15/10; estabilidade P0 precede melhoria arquitetural.

## Achado verificado e motivação da aula

A aula de Diagrama de Pacotes (slides 15–19) destaca dependências direcionais, alta coesão e baixo acoplamento. O código atual possui **ciclo de serviços**, não apenas associação no UML:

- `ArquivoService` importa `ComunidadeSocialService`, mantém `private final ComunidadeSocialService social` e chama `social.validarAcessoComunidade(c, id, usuario)` ao enviar/ler mídia.
- `ComunidadeSocialService` mantém `private ArquivoService arquivos` mutável e o usa para `arquivos.vincular(...)` ao criar post e mensagem.
- `ApplicationBootstrap` compõe os dois via criação posterior e `comunidadeSocialService.configurarArquivos(arquivos)`.

O código funciona no CI, portanto **não afirmar bug atual**. Objetivo é retirar a dependência bidirecional preservando comportamento, SQL, autorizações, locks e transações.

## Branch e escopo

Branch exclusiva `refactor/10-desacoplamento-social-arquivos` criada a partir de `main` após todos os PRs anteriores. **Não alterar README/diagramas gerais como parte do refactor, a menos que a mudança de assinatura exija regeneração do UML**; quando exigir, sincronizar apenas os diagramas afetados e explicar no PR. Não executar merge automaticamente.

## Abordagem de arquitetura preferida

1. Identificar a lógica completa de `validarAcessoComunidade` e seus chamadores, inclusive a ordem de locks, verificações de ingressos `ativo`/`utilizado`, filiação ativa e propriedade de organizador, sem duplicar autorização.
2. Extrair a validação de acesso para um **colaborador específico, independente dos serviços de mídia**, por exemplo `AcessoComunidadeService`, usando `ComunidadeSocialRepositoryJdbc` ou outro contrato pequeno adequado. Essa política deverá receber `Connection` da transação externa e `Usuario`, não abrir transações paralelas que alterem a ordem de locks.
3. Injetar tal colaborador **tanto** em `ComunidadeSocialService` **quanto** em `ArquivoService`; o segundo deixa de depender do primeiro. O Social poderá continuar a chamar o serviço de mídia para vincular o arquivo. O grafo resultante deve ser **unidirecional**, sem dependência `social → arquivos → social`.
4. Substituir o setter pós-construtor `configurarArquivos` por composição explícita no construtor quando possível, sem estado inicializável parcialmente.
5. Preservar tipagem de exceções e códigos HTTP existentes, especialmente 401/403/404, inclusive a proteção contra vazamento da existência de imagens; não permitir que um usuário de outra comunidade leia referência privada.
6. Cuidado extremo com **linha bloqueada antes de arquivo**, revalidações após revenda do último ingresso, evento pós-data, ex-membro, upload pendente, 24h, posse do arquivo, imagem de evento pública e ordem de locks. Preservar semântica exata das regras **RF-COM-05, RF-COM-06** e da Entrega 06.

**NÃO basta** substituir a referência concreta por duas interfaces com implementações recíprocas: isso removeria parte do acoplamento de compilação, mas manteria ciclo de objetos e bootstrap em duas fases. Critério é **não haver referências mutáveis circulares em runtime**.

## Testes e critérios de aceite obrigatórios

- Primeiramente, adicionar ou identificar testes que reprovem caso a elegibilidade de arquivos comunitários seja relaxada: visitante, não membro, membro sem ingresso, conta inativa, vendedor após venda do último ingresso, ex-membro, organizador dono e não dono, pós-evento com ingresso utilizado, imagem pendente e vinculada, manipulação entre comunidades e corridas de acesso.
- Reexecutar `ZArquivosMySqlIT`, `ComunidadeMySqlIT` e `ComercioMySqlIT` no MySQL isolado (não demo), além de `mvn -Pmysql-it clean verify` integral em dois bancos MySQL 8.4. Verificar contagem real de testes; baseline histórico 468 não é resultado da nova branch automaticamente.
- Comparar contratos HTTP e fluxos de imagens/chat/feed antes/depois. Conferir `jdeps`/imports e construção em `ApplicationBootstrap` sem ciclos.
- Alteração pequena e revisável. Se extrair política revelar risco de deadlock ou quebrar mais de um módulo funcional, **parar e registrar bloqueio**, não forçar refatoração antes do prazo.
- Atualizar diagrama de classes/fragmento de pacotes para refletir as dependências finais somente se o PR for aprovado; não remover nota de risco do relatório prematuramente.
- Abrir PR com antes/depois UML, lista de tipos impactados, testes com evidência, limites, riscos e planos de rollback. **Não mesclar até auditoria do PO/Tech Lead.**

**GO** apenas com todos os regressivos verdes, ausência do ciclo real comprovada e risco para apresentação controlado. Caso contrário, adiar sem prejuízo ao cumprimento material da RA2.
