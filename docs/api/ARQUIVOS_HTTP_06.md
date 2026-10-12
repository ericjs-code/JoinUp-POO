# Arquivos HTTP — Entrega 06

11/10/2026 (America/Sao_Paulo). Java 21, HttpServer/JDBC. Complementa o
contrato de eventos 02 e social 04. Sem avatar, URL remota ou fetch externo.

## Upload e contexto

`POST /api/arquivos`: cookie de sessão ativo, CSRF Origin/Referer da mesma
origem e `Content-Type: multipart/form-data; boundary=...`. Exatamente três
partes: `contexto`, `alvoId`, `arquivo`. Nenhum ID de usuário é aceito.

| contexto | alvoId | Autorização / efeito |
| --- | --- | --- |
| evento | ID do evento persistido | Organizador ativo dono, evento estritamente futuro. Vincula no próprio upload; torna-se imagem pública do evento e da descoberta da comunidade. |
| postagem | ID da comunidade | Participante ativo, filiação ativa e ingresso ativo/utilizado. Cria arquivo pendente privado, para uma única postagem. |
| mensagem | ID da comunidade | Participante elegível ou organizador ativo dono. Cria arquivo pendente privado para uma única mensagem. |

O frontend publica primeiro o evento pela Facade, depois oferece upload na
gestão. O upload não edita compras, categorias, ingressos, datas ou snapshots.
O início é verificado sob lock e novamente após a gravação. IDs numéricos
positivos até 2147483647; escopos são strings exatas da tabela acima.

```javascript
const form = new FormData();
form.append('contexto', 'postagem');
form.append('alvoId', '1');
form.append('arquivo', arquivoPng, 'imagem.png');
// O navegador define boundary e Origin. Não definir Content-Type manualmente.
const resposta = await fetch('/api/arquivos', {
  method: 'POST', credentials: 'same-origin', body: form
});
```

Resposta 201:

```json
{"dados":{"id":"aabbccddaabbccddaabbccddaabbccdd","imagem":"/api/arquivos/aabbccddaabbccddaabbccddaabbccdd"}}
```

ID contém 32 caracteres hexadecimais aleatórios (UUID gerado pelo servidor).
Nome, caminho físico e proprietário não são retornados. `alvoId` não define
proprietário: esse vem exclusivamente da sessão, revalidada na transação.

## Vinculação social e leitura

Enviar a referência `dados.imagem` no campo `imagem` do POST de postagem ou
mensagem da mesma comunidade. `descricao`/`mensagem` aceita texto vazio ou
omitido quando uma imagem autorizada existe. Texto e imagem ausentes/vazios
em conjunto são recusados. Limites textuais permanecem 500/1000 caracteres.

```json
{"descricao":"","imagem":"/api/arquivos/aabbccddaabbccddaabbccddaabbccdd"}
```

A autorização comunitária precede o INSERT social. Arquivo é bloqueado e
verificado por proprietário, comunidade, contexto, conteúdo SHA-256, tamanho,
ausência de uso anterior e prazo de 24h. INSERT social e vínculo do arquivo
confirmam na mesma transação. Falha de vínculo reverte o post/mensagem e
mantém o upload pendente coerente, disponível para tentativa explícita.
Outro participante, outra comunidade ou uso como mensagem de arquivo de
postagem recebe 404, sem detalhes do recurso alheio. Nenhum retry automático.

`GET /api/arquivos/{arquivoId}`:

- Evento: público somente enquanto a referência for a imagem atual do evento.
  Substituição/limpeza faz o ID anterior retornar 404. Histórico físico é
  preservado para manutenção administrativa, sem apontador ativo.
- Social vinculado: sessão ativa + autorização atual da comunidade. Visitante
  recebe 401; sem filiação/ingresso, organizador alheio ou suporte recebe 403.
  Última revenda, saída e desativação retiram acesso em chamadas posteriores.
  Evento passado e ingresso utilizado continuam elegíveis (RF-COM-05).
- Social pendente: mesmas regras, adicionalmente somente o dono, por até 24h.
- Arquivo ausente, corrompido, com ID inválido ou pendente expirado: 404.

Resposta binária 200: `Content-Type: image/png`, `Cache-Control: no-store`
(inclusive evento, escolha conservadora local), `X-Content-Type-Options: nosniff`,
`Content-Disposition: inline; filename="imagem.png"`. Nunca usa nome do cliente.
Cookies HTTP local/HTTPS e CSRF seguem a configuração existente.

## Limites e normalização

Entrada permitida: PNG, JPEG ou WebP com extensão, MIME declarado e assinatura
real coerentes. Decode ImageIO completo, sem warnings; máximo 4096 px por
dimensão e 4.000.000 pixels. Saída normalizada PNG, sem EXIF/metadados ou bytes
anexados. O módulo WebP usa apenas o decoder puro Java
[TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys), versão fixada
3.12.0; PNG/JPEG usam o JDK. Não há plugin SVG/HTML nem framework de mídia.

Arquivo recebido e PNG resultante: até **1.048.576 bytes** cada. PNG normalizado
maior que o limite é recusado, mesmo que a entrada comprimida seja menor.
Corpo multipart: até **1.064.960 bytes**, lido por `readNBytes(limite+1)` na
origem, com ou sem Content-Length/chunked. Mesmo quando Content-Length já
indica excesso, a leitura da aplicação fica em limite+1 antes de recusar,
evita fechamento antecipado com corpo marginalmente acima do limite e não
aumenta o payload aceito. Corpos muito maiores ainda podem encerrar a conexão;
não há leitura/drenagem ilimitada. JSON continua em **65.536 bytes**.
Só POST da rota de upload tem exceção de limite; outros métodos mantêm o global.

Parser opera sobre bytes binários; apenas cabeçalhos e campos pequenos viram
texto. Boundary ASCII de 1..70 caracteres, cabeçalhos de parte até 4096 bytes,
campos até 32 bytes, três partes únicas, término CRLF estrito. Rejeita preâmbulo,
epílogo, nesting, headers duplicados/extras e Content-Transfer-Encoding. Arquivo
tem somente Content-Disposition e Content-Type; campos só Content-Disposition.
Nome ASCII até 100 caracteres, extensão png/jpg/jpeg/webp; `..`, barras, URI,
controles e extensões executáveis são recusados. UI envia nome técnico gerado
para permitir seleção de arquivos cujo nome local contém acentos.

## Armazenamento e migração

`JOINUP_UPLOAD_DIR` / `-Djoinup.upload.dir`, padrão `~/.joinup/uploads`, absoluto
e fora do diretório do repositório/webroot. Não usar a pasta pública, target,
classpath ou OneDrive do projeto. Arquivos gerados como `<uuid>.png`, com
CREATE_NEW e NOFOLLOW_LINKS; nenhuma resolução a partir do nome enviado.
Ancestrais com symlink e arquivo symlink são recusados. Diretório deve ser
exclusivo do processo e protegido pelas permissões do usuário do SO; não há
defesa contra administrador local que modifica arquivos/diretórios em corrida.

Administrador aplica separadamente `db/migration_06_midia_demo.sql` após schema
v0.2. Idempotente CREATE TABLE IF NOT EXISTS de `arquivos` e `demo_execucao`;
não modifica tabelas v0.2, seed ou privilégios. `arquivos` possui seis FKs,
CHECKs de contexto/dono/tamanho e índices para vínculo único e criação. A
conta HTTP/demo tem somente SELECT/INSERT/UPDATE/DELETE, nunca DDL/GRANT.
CREATE IF NOT EXISTS não corrige uma tabela preexistente incompatível.

Gravação física termina antes de INSERT metadata e atualização de evento;
erro SQL normal provoca rollback e limpeza do arquivo. Falha física não cria
metadata. Em erro de commit/fechamento com resultado incerto, o serviço confere
se o registro existe antes de apagar; se o banco continua indisponível, mantém
arquivo para reconciliação e registra apenas aviso genérico. SHA/tamanho são
checados na leitura e no vínculo, sem devolver caminho físico.

Não há transação distribuída entre InnoDB e filesystem nem coletor automático:
crash do processo, falta de energia, disco com erro, pending abandonado e imagens
de evento substituídas exigem reconciliação administrativa offline. Conferir
DB e arquivos antes de remover conteúdo; nunca apagar arquivo vinculado a
post/mensagem/evento ativo. Backup deve abranger ambos. Expiração de pending
limita uso/leitura, sem deletar silenciosamente seu histórico ou arquivo.

## Erros e demonstração

| HTTP | Código / situação |
| --- | --- |
| 400 | MULTIPART_INVALIDO, IMAGEM_INVALIDA, IMAGEM_NAO_SUPORTADA, REQUISICAO_INVALIDA |
| 401 | NAO_AUTENTICADO |
| 403 | CSRF_NEGADO, CONTA_INATIVA, PERMISSAO_NEGADA, ACESSO_COMUNIDADE_NEGADO |
| 404 | RECURSO_NAO_ENCONTRADO; sem informação de dono/caminho |
| 409 | EVENTO_JA_INICIADO ou CONCORRENCIA_CONFLITO |
| 413 | REQUISICAO_MUITO_GRANDE |
| 503 | ARMAZENAMENTO_INDISPONIVEL ou BANCO_INDISPONIVEL |

Roteiro: Ana → comunidade Aurora → Feed → selecionar imagem → publicar sem
texto; Chat → texto e/ou imagem → enviar. Aurora → Meus eventos → editar →
selecionar imagem → enviar → catálogo. Fazer logout: catálogo mantém imagem
pública; sala e URL social continuam protegidas. Suporte autentica e consulta
painel/chamados, sem ganhar acesso à mídia privada. Evidências e testes em
`docs/execucao/ENTREGA_06_RESULTADOS.md`.
