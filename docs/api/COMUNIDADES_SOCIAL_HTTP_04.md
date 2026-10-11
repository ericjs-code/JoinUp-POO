# Comunidades e feed social — contratos efetivos da Entrega 04

Data: 11/10/2026. Java 21 / HttpServer / JDBC / MySQL 8. Complementa o
[contrato aprovado](CONTRATOS_HTTP_v0.1.md) e adendos RF-COM-05 e RF-COM-06.

---

## 1. Visão Geral de Permissões

1. **Rotas Públicas (Descoberta RF-COM-06):**
   - `GET /api/comunidades`
   - `GET /api/comunidades/{id}`
   - Visitantes e qualquer perfil podem consultar metadados públicos (nome, descrição, imagem, evento vinculado). Mensagens e feed são estritamente restritos.
   - Quando o chamador está autenticado como participante ativo, `GET /api/comunidades/{id}` enriquece a resposta com `membroAtivo` e `elegivelParaEntrar` (se possui ingresso com status `ativo` ou `utilizado` no evento).

2. **Acesso ao Conteúdo Interno (Chat e Feed):**
   - **Participante:** exige autenticação, conta ativa, associação ativa à comunidade (`participante_comunidades.ativo = 1`) e ingresso elegível do evento vinculado (`status IN ('ativo', 'utilizado')`, conforme RF-COM-05).
   - **Organizador dono:** exige autenticação, conta ativa e ser o criador/dono do evento vinculado à comunidade (`eventos.id_organizador = organizador.id`). Possui acesso de leitura a feed e mensagens, e permissão de envio de mensagens no chat como organizador (`autor_tipo = 'organizador'`). Não pode criar posts ou comentários (exclusivos de participantes).
   - Não membros ou participantes sem ingresso elegível recebem `403 Forbidden` com `codigo: "ACESSO_NEGADO"`.

3. **Mutações (CSRF e Segurança):**
   - POST, PUT, DELETE exigem origem/Referer segura (`same-origin`), cookie de sessão `HttpOnly` e `Content-Type: application/json`.
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
*Validação:* `mensagem` obrigatória (1 a 1000 caracteres). `imagem` e `idRespostaA` opcionais.

### POST `/api/comunidades/{id}/postagens`
```json
{
  "descricao": "Ansioso para o show!",
  "imagem": null
}
```
*Validação:* `descricao` obrigatória (1 a 2000 caracteres). `imagem` opcional.
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
