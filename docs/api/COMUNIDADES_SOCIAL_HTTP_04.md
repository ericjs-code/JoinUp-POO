# Comunidades e feed social — contratos efetivos da Entrega 04

Data: 11/10/2026. **Mídia evoluída na Entrega 06:** os limites textuais permanecem;
texto e/ou imagem local autorizada são aceitos no feed e chat, conforme
[ARQUIVOS_HTTP_06.md](ARQUIVOS_HTTP_06.md). As restrições de imagem abaixo
descrevem o estado histórico da Correção 04.1.
 Java 21 / HttpServer / JDBC / MySQL 8. Complementa o
[contrato aprovado](CONTRATOS_HTTP_v0.1.md) e adendos RF-COM-05 e RF-COM-06.

---

## 1. Visão Geral de Permissões

1. **Rotas Públicas (Descoberta RF-COM-06):**
   - `GET /api/comunidades`
   - `GET /api/comunidades/{id}`
   - Visitantes e qualquer perfil podem consultar metadados públicos (nome, descrição, imagem, evento vinculado). Mensagens e feed são estritamente restritos.
   - Quando o chamador está autenticado como participante ativo, `GET /api/comunidades/{id}` enriquece a resposta com `membroAtivo` (associação), `elegivelParaEntrar` (ingresso ativo/utilizado) e `acessoConteudo` (ambas as condições). Para organizador dono, os três indicadores são verdadeiros. A UI usa `acessoConteudo`; o servidor revalida a autorização a cada operação.

2. **Acesso ao Conteúdo Interno (Chat e Feed):**
   - **Participante:** exige autenticação, conta ativa, associação ativa à comunidade (`participante_comunidades.ativo = 1`) e ingresso elegível do evento vinculado (`status IN ('ativo', 'utilizado')`, conforme RF-COM-05).
   - **Organizador dono:** exige autenticação, conta ativa e ser o criador/dono do evento vinculado à comunidade (`eventos.id_organizador = organizador.id`). Possui acesso de leitura a feed e mensagens, e permissão de envio de mensagens no chat como organizador (`autor_tipo = 'organizador'`). Não pode criar posts ou comentários (exclusivos de participantes).
   - Não membros ou participantes sem ingresso elegível recebem `403 Forbidden` com `erro.codigo: "ACESSO_COMUNIDADE_NEGADO"`.

3. **Mutações (CSRF e Segurança):**
   - POST, PUT, DELETE exigem origem/Referer segura (`same-origin`) e cookie de sessão `HttpOnly`. Criação de mensagem, publicação e comentário exige `Content-Type: application/json`. Participação e curtida não recebem dados de domínio e podem enviar corpo vazio.
   - IDs de usuário são sempre extraídos da sessão autenticada no servidor (nunca de payload do cliente).

---

## 2. Tabela de Endpoints

| Método | Rota | Autenticação / Perfil | Resposta de Sucesso | Descrição |
| --- | --- | --- | --- | --- |
| `GET` | `/api/comunidades` | Pública | 200 `{dados:[comunidades]}` | Lista comunidades disponíveis |
| `GET` | `/api/comunidades/{id}` | Pública | 200 `{dados:comunidade}` | Detalhes públicos e status de filiação |
| `POST` | `/api/comunidades/{id}/participacao` | Participante ativo | 200 `{dados:{sucesso:true}}` | Entrada manual na comunidade (RF-COM-01) |
| `DELETE` | `/api/comunidades/{id}/participacao` | Participante membro | 200 `{dados:{sucesso:true}}` | Saída voluntária da comunidade (RF-COM-02) |
| `GET` | `/api/comunidades/{id}/mensagens` | Membro ou Organizador dono | 200 `{dados:[mensagens]}` | Histórico cronológico de mensagens |
| `POST` | `/api/comunidades/{id}/mensagens` | Membro ou Organizador dono | 201 `{dados:mensagem}` | Envio de mensagem no chat |
| `GET` | `/api/comunidades/{id}/postagens` | Membro ou Organizador dono | 200 `{dados:[postagens]}` | Feed da comunidade com curtidas e comentários |
| `POST` | `/api/comunidades/{id}/postagens` | Participante membro ativo | 201 `{dados:postagem}` | Criação de publicação no feed |
| `GET` | `/api/postagens/{id}` | Membro ou Organizador dono | 200 `{dados:postagem}` | Detalhe de uma postagem |
| `PUT` | `/api/postagens/{id}/curtida` | Participante membro ativo | 200 `{dados:{curtidas:N,curtido:true}}` | Curtida idempotente (atômica) |
| `DELETE` | `/api/postagens/{id}/curtida` | Participante membro ativo | 200 `{dados:{curtidas:N,curtido:false}}` | Remoção de curtida |
| `GET` | `/api/postagens/{id}/comentarios` | Membro ou Organizador dono | 200 `{dados:[comentarios]}` | Lista cronológica de comentários |
| `POST` | `/api/postagens/{id}/comentarios` | Participante membro ativo | 201 `{dados:comentario}` | Inclusão de comentário na postagem |

---

## 3. Estruturas de Payload

### POST `/api/comunidades/{id}/mensagens`
```json
{
  "mensagem": "Olá a todos! Nos vemos no evento!",
  "imagem": null,
  "idRespostaA": null
}
```
*Validação:* `mensagem` obrigatória (1 a 1000 caracteres; texto não branco).
`imagem` deve estar ausente ou `null`. `idRespostaA` opcional/null; quando
presente deve ser número JSON inteiro positivo até 2147483647 e referenciar
mensagem da mesma comunidade. ID inválido/coerção/fracionário → 400; mensagem
inexistente ou de outra comunidade → 404 com a mesma resposta, sem conteúdo
alheio nem inserção. Comparação JDBC parametrizada na transação sob lock.

### POST `/api/comunidades/{id}/postagens`
```json
{
  "descricao": "Ansioso para o show!",
  "imagem": null
}
```
*Validação:* `descricao` obrigatória (1 a **500** caracteres; texto não branco),
coerente com `postagens.descricao VARCHAR(500)`. `imagem` ausente ou `null`.
*Regra RF-SOC-01:* **Sem edição e sem exclusão** de postagens após criação.

### POST `/api/postagens/{id}/comentarios`
```json
{
  "texto": "Eu também estarei lá!"
}
```
*Validação:* `texto` obrigatório (1 a 500 caracteres).

---

## 4. Tratamento de Erros e Códigos HTTP

- `400 Bad Request`: Payload JSON malformado, campos ausentes ou inválidos.
- `401 Unauthorized`: Chamador não autenticado em rotas que exigem sessão.
- `403 Forbidden`: Perfil incompatível, participante não associado ou sem ingresso elegível, conta inativa.
- `404 Not Found`: Comunidade ou postagem não encontrada.
- `409 Conflict`: Violação de integridade ou estado de domínio.
- `500 Internal Server Error`: Erro interno no servidor sem exposição de detalhes de SQL ou banco.

## 5. Paginação e compartilhamento — Correção 04.1

As quatro listas (comunidades, mensagens, postagens, comentários) mantêm o
envelope `{dados:[]}` e aceitam `pagina` (1..10000, padrão 1) e `tamanho`
(1..100, padrão 50). SQL usa LIMIT/OFFSET parametrizados. Valores fora da faixa,
fracionários ou não numéricos retornam 400. `busca` limita-se a 100 caracteres.
Comunidades, mensagens e comentários ordenam por ID crescente; feed por ID
decrescente. A interface oferece Anterior/Próxima. Não há contagem total; uma
página cheia pode ser seguida por página vazia. Não há teste de carga de escala.
Criação de mensagem/comentário recupera o novo registro por ID, independentemente
da página atual. Detalhe individual de postagem mantém acesso a posts antigos.

O botão **Copiar link** produz `/comunidades.html?id=<comunidade>&post=<post>`.
Usa Clipboard API e, se indisponível/negada, prompt para cópia manual. O link
carrega somente metadados para visitante. O detalhe de post é buscado em
`GET /api/postagens/{id}` após autorização; o frontend compara a comunidade,
abre a aba Feed e destaca o post, inclusive fora da página atual.

## 6. Limite de mídia e sincronização

**RF-SOC-H01/RF-COM-H02 continuam parciais:** upload e posts somente com imagem
aguardam endpoint seguro, armazenamento, limites de tipo/tamanho e autorização
da leitura do arquivo. Nesta correção, qualquer `imagem != null` retorna
400 `IMAGEM_NAO_SUPORTADA` antes de persistir. Não há allowlist nem upload
implementado; URLs externas, `file:`, `data:` e caminhos locais são rejeitados.
Essa contenção foi prevista no prompt 04.1 e não representa aprovação do PO
para eliminar o requisito de imagem. A pendência precisa de homologação do PO.

Leituras protegidas e mutações usam a mesma transação. Ordem: conta do ator,
sessão revalidada, evento, ingressos elegíveis em ID crescente e filiação.
O social não bloqueia categorias ou anúncios, pois não os altera. Comércio
mantém contas/organizador/sessão/evento/categorias/ingressos/anúncio/filiação.
Nenhum lock adicional de conta é adquirido depois do evento; locks são SELECTs
simples, sem JOIN. A conta e o evento serializam transferência, entrada/saída,
desativação e publicação. Não se exige evento futuro ou organizador ativo para
participante no social. Curtidas bloqueiam a postagem antes da linha da curtida
e atualizam contador na mesma transação. Locks conservadores limitam paralelismo.

MySQL timeout/deadlock → 409 `CONCORRENCIA_CONFLITO`, após rollback. Falha JDBC
→ 503 `BANCO_INDISPONIVEL`, sem detalhes SQL. Não há retry automático; atualizar
estado antes de tentar novamente. O contador/linha da curtida são indivisíveis.
