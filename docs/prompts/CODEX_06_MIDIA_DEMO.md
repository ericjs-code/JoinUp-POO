# CODEX — Entrega 06: mídia segura + dados demonstrativos automatizados

> **JoinUp-POO** · Java 21 puro, `jdk.httpserver`, JDBC/MySQL 8.4, Maven, JUnit 5  
> **Data do planejamento:** 11/10/2026. **Entrega RA2:** 15/10/2026; apresentação: 16/10.  
> **Origem:** `main` atual após PR #7 + `docs/execucao/AUDITORIA_ENTREGA_05_PR07.md`.  
> **Branch:** `feat/06-midia-demo` com PR **aberto sem merge automático**.

## 0. Objetivo e limite de prazo

Completar a lacuna aprovada **SOC-06 — imagens e posts somente-imagem** e a regra **BD-02 — dados demonstrativos em carga automática idempotente** para uma apresentação acadêmica local segura. **Não reescrever eventos, comércio, autenticação, social, suporte ou dashboards**. Esta entrega deve permanecer pequena e auditável. Se surgir bloqueio de armazenamento/SQL, entregar implementação incremental com pendência explícita, sem alargar o escopo em silêncio.

**Leituras obrigatórias:**
- Todos os `docs/requisitos/ADENDO_*.md`, `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` (RF-SOC-H01, RF-COM-H02, RF-UI, BD-02).
- `docs/api/CONTRATOS_HTTP_v0.1.md`, `docs/api/COMUNIDADES_SOCIAL_HTTP_04.md`, `docs/execucao/AUDITORIA_CRUZADA_PR04_PR05.md`, `docs/execucao/AUDITORIA_CORRECAO_04_1_PR06.md`, `docs/execucao/AUDITORIA_ENTREGA_05_PR07.md`.
- `docs/execucao/PLANO_CODEX.md`, `docs/banco/VALIDACAO_INTEGRACAO_01.md`, schema/seed v0.2, `ApplicationBootstrap`, `ConfiguracaoSistema`, serviços de eventos/social e `StaticFileHandler`.
- Fonte PHP original `ericjs-code/JoinUp` apenas para campos, navegação e comportamento demonstrativo, nunca para importar dados reais.

**Arquitetura imutável:** Java 21 puro, HttpServer JDK, JDBC+MySQL 8, Jackson/JUnit/Maven; exatamente os quatro GoF aprovados (Facade, Template Method, Factory Method e Singleton) preservados nos fluxos reais. Não adicionar Spring/Javalin/Servlet/ORM/frameworks de mídia desnecessários.

## 1. Upload seguro de imagens (SOC-06)

### Casos funcionais

- Organizadores ativos podem anexar imagem de seus próprios **eventos ainda futuros**, respeitando RF-EVT-06/08; renderizar a imagem pública no catálogo e na comunidade pública onde pertinente, sem alterar vendas/histórico.
- Participantes ativos com filiação e ingresso `ativo`/`utilizado` podem enviar imagens para **postagens** e, onde aplicável, **mensagens** de suas comunidades. Organizador dono também pode enviar imagem em mensagem, respeitando regras de acesso. **Postagem só com imagem** deve ser válida (texto vazio), sem liberar post vazio de texto e imagem.
- Imagens de postagens/mensagens são **privadas à comunidade**; URL conhecida não autoriza acesso de visitante/ex-membro. Pós-evento vale RF-COM-05. Não misturar autorização de leitura pública do evento com autorização do conteúdo social.
- Quando a imagem for enviada separadamente da postagem/evento, ela deve ter dono, escopo e vinculação controlados pelo servidor, não pelo ID de proprietário fornecido no cliente.
- Imagens de perfil **não possuem campo próprio na tabela de participantes/organizadores do schema v0.2**. Não inventar uma nova regra de avatar; só implementar se for necessário à paridade original e houver proposta técnica separada para revisão. Priorize event/chat/post.

### Contrato HTTP e segurança

- `POST /api/arquivos` multipart/form-data (ou rota explícita versionada de upload), com **alvo de contexto tipado** e autorização calculada pelo servidor; integrar ao frontend real. `GET /api/arquivos/{arquivoId}` deve aplicar visibilidade **pública de evento** ou **privada da comunidade**.
- Usar **MIME permitido PNG/JPEG/WebP**; checar assinatura real/magic bytes, tamanho máximo explícito (sugestão: 1 MiB por imagem), tipo declarado, imagem íntegra quando possível. Rejeitar SVG/HTML, `data:`, `file:`, URL remota e extensões fora da lista; nenhum fetch remoto, SSRF, execução ou parsing HTML.
- Os uploads não entram no classpath, repositório nem pasta estática pública. Usar armazenamento local configurável fora da webroot, IDs aleatórios e nomes gerados no servidor; impedir path traversal/symlinks e não permitir sobrescrever/ler arquivos alheios.
- **Não elevar o limite global de 64 KiB para JSON** indiscriminadamente. Estabelecer limite por rota, lendo stream de forma limitada desde a origem; multipart parsing deve ser robusto quanto a boundary, tamanho, cabeçalhos e conteúdo binário. Não pressupor que converter todo multipart para `String` seja seguro.
- Se exigir registro persistido/visibilidade de imagens, propor **migração aditiva idempotente** em arquivo SQL separado para metadados, verificando FKs/índices e privilégios, com documentação; **não quebrar schema/seed v0.2 já validados**. Migrar em MySQL teste isolado por conta administrativa, não pela conta DML da aplicação.
- Integridade entre arquivo e registro/entidade: evitar metadata apontando para arquivo inexistente, conteúdo órfão após falha, vínculo cruzado entre evento e comunidade e reutilização do arquivo por outra pessoa. Testar condições parciais e limpeza. Permitir referência local somente a arquivo persistido, autorizado e de escopo correto. Não armazenar URL arbitrária em `imagem`.
- `Cache-Control` apropriado: arquivos privados sem cache público; `X-Content-Type-Options: nosniff`; `Content-Disposition: inline` apenas com MIME validado e nunca originário de nome fornecido pelo usuário. HTTP/HTTPS conforme configuração existente.
- Registrar `docs/api/ARQUIVOS_HTTP_06.md` com payload, erros, autorização, armazenamento, limites, e fluxo demonstrável.

### Testes obrigatórios de mídia

1. Upload legítimo de evento, catálogo público exibe; organizador não dono ou evento iniciado rejeitados.
2. Postagem somente-imagem autorizada; post sem texto **e sem imagem** rejeitado; mensagem com imagem validada.
3. Leitura de arquivo privado negada a visitante, participante sem filiação, ex-membro após última revenda, organizador alheio ou sessão inativa; permitida ao participante elegível e organizador dono.
4. Rejeitar MIME forjado, bytes não válidos, arquivo > limite, arquivo com cabeçalhos suspeitos, `../`, `file:`, `data:`, ID de arquivo de outra comunidade e upload com CSRF inválido; sem vazamento de caminho físico.
5. Concorrência de perda de elegibilidade durante upload/vinculação, rollback e ausência de referência quebrada em erro SQL/arquivo. Não alterar compras/ingressos.
6. UI de evento, chat e feed carrega a imagem autorizada no navegador; o texto continua escapado (evitar XSS).

## 2. Carga automática demonstrativa — BD-02

- O arquivo `src/main/resources/db/seed_demo_v0.2.sql` já contém 9 contas PBKDF2 demo, eventos, compras, revendas, comunidades, feed e tickets em vários estados; já foi validado e executado **manualmente** em MySQL 8.4.
- Implementar **opt-in local** controlado por `JOINUP_DEMO=true`/configuração imutável; `false` não deve executar nenhuma operação de seed. **Não criar/alterar tabelas automaticamente no bootstrap** e não carregar seed com perfil de produção.
- Antes de aplicar, conferir banco `joinup_poo`, schema existente e ambiente explicitamente demonstrativo; recusar silenciosa mistura em base não demonstrativa. Nunca usar legacy `joinup`. Uma conta DML pode aplicar INSERTs; não conceder DDL/GRANT à aplicação.
- Implementar runner SQL seguro ou estratégia de carga controlada, sem `SOURCE` dependente de cliente CLI, garantindo que `USE` e separação de statements/comentários não sejam interpretados incorretamente. Não executar SQL arbitrário do cliente. Se optar por runner simples, usar script confiável empacotado, regras claras e testes. Idempotência real: iniciar/reiniciar duas vezes sem duplicar nenhuma linha demo. Usar transação quando viável.
- **Evitar sobrescrever** registros que o usuário alterou durante demonstração; não resetar senhas nem reativar contas desativadas em reinicialização. `INSERT IGNORE` já estabelece parte desse comportamento; validar efeitos.
- Fornecer `README.md` de execução em máquina nova (JDK21/MySQL8, schema pela administração, demo=true, como acessar os três perfis), com credenciais **somente de fixture**. Sem senhas de produção nos logs.
- Automatizar validações em CI MySQL 8.4 com `JOINUP_DEMO=false` (sem side effect), `true` (seed aplicada) e repetição (não duplicada); não tornar execução acidental de seed possível contra bancos de outros nomes.
- O **seed** existente é fonte de verdade; modifique-o somente por correção técnica demonstrada e justificada, nunca para manipular métricas a favor do teste.

## 3. Regressão, UI e entregáveis

- Preservar os **397 testes** da Entrega 05 e adicionar integração HTTP/MySQL de mídia e carga demo. `mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify` em CI com `mysql:8.4.0`.
- Não inserir fluxos de pagamento real/saque/push/email; não alterar regras de estados de suporte, limites de revenda ou acesso pós-evento.
- Atualizar JS/HTML de eventos e comunidades com upload/preview/erro e sem injetar HTML não confiável. Roteiro de navegação mínima pelos três perfis em MySQL real, documentando o que foi executado vs não executado.
- `docs/execucao/ENTREGA_06_RESULTADOS.md`: arquivos, contrato/migração se necessária, regras SOC-06/BD-02 cumpridas, testes, evidências, comandos, capturas e pendências. Atualizar `README.md` para a `main` pós-merge do PR #7.
- **PR em branch `feat/06-midia-demo`, SEM MERGE automático**. Reportar link ao Tech Lead. Se prazo apertar, entregar PR parcial marcado com lacunas reais e priorizar a execução acadêmica estável.

## Definição de pronto

Imagens exibidas com autorização correta e upload seguro, inclusive **post somente-imagem**; massa demo auto-idempotente e isolada; regressão 397 testes verde; MySQL real e sanidade de navegador demonstradas; nenhuma mudança de regra funcional silenciosa. Não declarar app de produção certificado nem plataforma inteira concluída.
