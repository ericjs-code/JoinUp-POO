# CODEX — Entrega 04: comunidades, feed social e mensagens

> **Projeto:** `ericjs-code/JoinUp-POO`  
> **Data:** 11/10/2026 | **Prazo RA2:** 15/10/2026; apresentação 16/10/2026  
> **Situação:** PRs #1/#2/#3/#4 integrados à `main`. Entregas 00, 01, 02 e 03 concluídas.  
> **Ramo de trabalho:** `feat/04-comunidades-social` a partir de `main` atualizada. **NÃO MERGEAR** sem auditoria do Tech Lead.

## 0. Missão

Implementar o módulo completo de **Comunidades e Feed Social**:
1. Descoberta pública de comunidades (RF-COM-06: apenas metadados públicos).
2. Associação voluntária manual e saída voluntária de participante (RF-COM-01, RF-COM-02, RF-COM-01A).
3. Chat/mensagens da comunidade com suporte a participante e organizador dono (RF-COM-H02, RF-COM-01).
4. Feed social da comunidade: postagens, comentários e curtidas/descurtidas idempotentes (RF-SOC-H01 a H04, RF-COM-01B).
5. Validação rigorosa de elegibilidade e regras pós-evento (RF-COM-05: ingresso `ativo` ou `utilizado` permite acesso; evento passado não encerra comunidade).
6. **Proibição estrita de edição e exclusão de postagens** (RF-SOC-01).
7. Frontend correspondente integrado (`/comunidades.html`), respeitando a identidade visual existente.

---

## 1. Documentos e Regras Obrigatórias

- `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` (Seções 3.5 e 3.6).
- `docs/requisitos/ADENDO_RF-COM-05_COMUNIDADES_POS_EVENTO.md` (Fundamental: ingresso utilizado mantém acesso após o evento; cancelado não; compra/revenda após evento continua bloqueada).
- `docs/requisitos/ADENDO_RF-COM-06_DESCOBERTA_PUBLICA.md` (Fundamental: visitante sem login pode listar e ver detalhes públicos de comunidade; mensagens e feed são bloqueados).
- `docs/api/CONTRATOS_HTTP_v0.1.md` (Seção 6).
- `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`.
- Schema MySQL `src/main/resources/db/schema_v0.2.sql`:
  - `comunidades` (`id_comunidade`, `nome`, `descricao`, `imagem`, `id_evento`, `data_criacao`)
  - `participante_comunidades` (`id_participante`, `id_comunidade`, `data_entrada`, `ativo`, `data_saida`)
  - `comunidade_mensagens` (`id_mensagem`, `id_comunidade`, `autor_tipo`, `id_participante`, `id_organizador`, `mensagem`, `imagem`, `id_resposta_a`, `data_envio`)
  - `postagens` (`id_postagem`, `descricao`, `imagem`, `curtidas`, `id_participante`, `id_comunidade`, `data_postagem`)
  - `comentarios` (`id_comentario`, `texto`, `id_postagem`, `id_participante`, `data_comentario`)
  - `postagem_curtidas` (`id_participante`, `id_postagem`, `data_curtida`)

---

## 2. Restrições Técnicas

- Java **21 puro**, JDK `HttpServer`, JDBC/MySQL 8, Jackson, Maven, JUnit 5.
- Sem frameworks (Spring, Javalin, Hibernate/JPA, CDI/Guice). Composição manual via construtores no `ApplicationBootstrap`.
- Preservar os 4 padrões GoF existentes (Singleton, Factory Method, Template Method, Facade). **Não inventar quinto padrão demonstrativo**.
- Preservar integridade transacional com `ExecutorTransacional`.
- Autorização server-side em todas as mutações e leituras protegidas:
  - Validar sessão, CSRF nas mutações, titularidade e perfil.
  - Não confiar em IDs de usuário informados pelo cliente.

---

## 3. Endpoints a Implementar

### A. Comunidades e Participação
- `GET /api/comunidades`: **Público** (RF-COM-06). Lista comunidades com id, nome, descricao, imagem, eventoId, eventoNome, dataCriacao.
- `GET /api/comunidades/{id}`: **Público**. Retorna dados públicos da comunidade. Se autenticado como participante ativo, inclui indicadores booleanos `membroAtivo` e `elegivelParaEntrar` (possui ingresso `ativo` ou `utilizado` do evento).
- `POST /api/comunidades/{id}/participacao`: **Participante autenticado e ativo**. Entrada manual. Valida titularidade de ingresso do evento (`status IN ('ativo', 'utilizado')`). Insere ou atualiza linha em `participante_comunidades` com `ativo = 1`, `data_saida = NULL`.
- `DELETE /api/comunidades/{id}/participacao`: **Participante membro**. Saída voluntária. Atualiza `participante_comunidades` para `ativo = 0`, `data_saida = UTC_TIMESTAMP()`.

### B. Mensagens da Comunidade (Chat)
- `GET /api/comunidades/{id}/mensagens`: **Membro ativo com ingresso elegível** OU **Organizador dono do evento vinculado**. Retorna lista cronológica de mensagens com autor, tipo de autor, texto, imagem, idRespostaA e timestamp.
- `POST /api/comunidades/{id}/mensagens`: Mesma permissão. Envia mensagem de chat. Se for organizador, grava `autor_tipo = 'organizador'` e `id_organizador`. Se for participante, grava `autor_tipo = 'participante'` e `id_participante`.

### C. Feed Social, Comentários e Curtidas
- `GET /api/comunidades/{id}/postagens`: Mesma permissão de comunidade. Lista postagens com autor, texto, imagem, contagem de curtidas, contagem de comentários, data e indicador booleano `curtidoPeloUsuario` (para participantes).
- `POST /api/comunidades/{id}/postagens`: **Participante ativo, membro ativo com ingresso elegível**. Cria postagem.
- `GET /api/postagens/{id}`: Autorizado para quem tem acesso à comunidade da postagem.
- `PUT /api/postagens/{id}/curtida`: **Participante habilitado**. Operação idempotente de curtir postagem. Insere em `postagem_curtidas` e incrementa atômica e transacionalmente `postagens.curtidas`.
- `DELETE /api/postagens/{id}/curtida`: **Participante habilitado**. Remove curtida. Exclui de `postagem_curtidas` e decrementa `postagens.curtidas` (sem deixar negativo).
- `GET /api/postagens/{id}/comentarios`: Acesso autorizado à comunidade da postagem. Lista comentários em ordem cronológica.
- `POST /api/postagens/{id}/comentarios`: **Participante habilitado**. Cria comentário na postagem.

---

## 4. Frontend e Interface

- Criar `/comunidades.html` (e `/comunidades.js`) e/ou integrar navegação nas páginas existentes (`/conta.html`, `/eventos.html`, `/ingressos.html`).
- Apresentar:
  - Lista de comunidades públicas com card do evento.
  - Indicador de membro / botão para "Entrar na comunidade" ou "Sair da comunidade".
  - Aba de **Mensagens / Chat** da comunidade.
  - Aba de **Feed Social** da comunidade com posts, campo para nova publicação, botão de curtir com contador em tempo real e seção expansível de comentários.

---

## 5. Testes Automatizados e Critérios de Aceite

- Testes unitários e de integração MySQL para:
  - CA-25/CA-26: Acesso pós-evento com ingresso ativo ou utilizado.
  - CA-27: Rejeição de participante apenas com ingresso cancelado.
  - CA-28: Bloqueio de participante que saiu voluntariamente até retorno manual.
  - CA-36/CA-37/CA-38: Descoberta pública de comunidades (metadados permitidos, conteúdo bloqueado).
  - CA-39/CA-40: Participante com ingresso não membro bloqueado até entrada manual; membro liberado.
  - Curtida idempotente e descurtida atômica.
  - Sem edição/exclusão de posts (RF-SOC-01).
- Todos os testes das entregas anteriores (127 unitários) devem continuar passando.
