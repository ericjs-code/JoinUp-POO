# Contrato efetivamente implementado — Eventos, Entrega 02

Data: 10/10/2026. Complementa o contrato v0.1 com os refinamentos técnicos
autorizados no prompt 02; regras RF-EVT-H01/H02/H03 e RF-EVT-01 a 08 preservadas.

## Consultas

| Rota GET | Envelope e limites |
| --- | --- |
| `/api/eventos` | `{dados: EventoView[], pagina, tamanho, total}`; página padrão 1, até 100000; tamanho padrão 20, até 100 |
| `/api/organizador/eventos` | Mesmo envelope/filtros; sessão de organizador ativo; apenas sua autoria |
| `/api/eventos/{eventoId}` | `{dados: EventoView}` público |
| `/api/eventos/{eventoId}/tipos-ingresso` | `{dados: TipoIngresso[]}` público; 404 se evento não existe |
| `/api/generos-musicais` | `{dados: [{id,nome}]}`; 15 gêneros demo e adicionais do banco |
| `/api/artistas?busca=` | `{dados: [{id,nome}]}`; CPF nunca é selecionado |

Filtros de eventos: `busca` (nome/descrição, até 80 caracteres), `generoId`
(positivo ou vazio), `cidade` (até 60 caracteres), `pagina`, `tamanho`.
Texto de busca é literal via `LOCATE` parametrizado; `%`/`_` não ampliam busca.
Cidade e gênero são igualdade. Comparação de texto segue a collation existente
`utf8mb4_unicode_ci`. Ordem por início UTC e ID, incluindo eventos históricos e
de organizadores inativos. IDs de path devem estar em 1..2147483647, sem zeros
iniciais. Filtros desconhecidos ou paginação inválida retornam 400.

`EventoView` contém id, nome, data ISO UTC, descrição, imagem, localizacao,
cidade, estado, cep, genero `{id,nome}`, artistas `[{id,nome}]`, tiposIngresso,
comunidade `{id,nome,imagem,eventoId}` e editavel (condição temporal informativa).
Não contém organizadorId, CPF, CNPJ, e-mail, hashes, membros ou conteúdo privado.
`TipoIngresso`: id, nome, preco decimal, quantidade disponível, quantidadeVendida,
temVendas. `temVendas` considera contador, compra_itens e ingressos, qualquer status.

## Mutações

Exigem sessão ativa de organizador, CSRF/origem e JSON; titularidade deriva da
sessão e é revalidada na mesma transação. Perfil sem permissão retorna 403,
ausência/expiração/revogação de sessão 401. Dados de dono no corpo são recusados.

```json
{
  "nome": "Festival Aurora",
  "descricao": "Festival demonstrativo",
  "data": "2026-12-05T20:00:00-03:00",
  "localizacao": "Arena Demo",
  "cidade": "Curitiba",
  "estado": "PR",
  "cep": "80000000",
  "generoId": 2,
  "imagem": "/assets/evento-placeholder.svg",
  "artistaIds": [1, 3],
  "tiposIngresso": [{"nome": "Pista", "preco": 100.25, "quantidade": 100}]
}
```

- `POST /api/organizador/eventos`: payload acima, imagem opcional; ao menos um
  artista válido, sem repetição, e uma categoria. HTTP 201 com EventoView e IDs
  do evento, categorias e comunidade. Data futura é validação técnica do
  cadastro nesta etapa, explicitada no prompt, sem alterar eventos do seed.
- `PATCH /api/organizador/eventos/{eventoId}`: campos gerais/artistaIds opcionais;
  ao menos um informado. Lista substitui apenas associações de lineup, em ordem
  de ID (schema não tem posição de palco). Não aceita tiposIngresso. HTTP 200.
- `POST /api/organizador/eventos/{eventoId}/tipos-ingresso`: nome/preco/quantidade
  obrigatórios; HTTP 201 com TipoIngresso.
- `PATCH /api/organizador/eventos/{eventoId}/tipos-ingresso/{tipoId}`: alteração
  parcial de nome/preco/quantidade; HTTP 200 com TipoIngresso. Só categoria
  pertencente ao evento, sem nenhuma venda histórica.

PATCH preserva campos omitidos e recusa `null` explícito (400). Imagem vazia
remove referência do evento; comunidade existente permanece intacta. Só é
aceito o recurso público `/assets/evento-placeholder.svg` ou referência vazia;
upload multipart e referências remotas continuam pendentes. Não se busca imagem
no servidor nem se aceita caminho arbitrário.

Limites textuais seguem schema: nome/local 45, descrição 450, cidade 60,
categoria 45; CEP 8 dígitos e UF existente. Quantidade é inteiro 0..2147483647,
preço `BigDecimal` 0..999999.99, sem arredondamento de frações além de centavos.
Números não aceitam coerção de texto ou truncamento de quantidade/IDs fracionários.

Data transportada tem offset e é normalizada para `Instant`. Coluna DATETIME
sem frações usa segundos UTC (`LocalDateTime` JDBC explícito), sem depender do
fuso da JVM. Primeiro normaliza a precisão, depois compara com Clock UTC: um
instante que seria truncado para agora/passado é recusado. Organizador não
consegue mover evento já iniciado para o futuro. HTTP 409 EVENTO_JA_INICIADO
no início inclusive; 422 DATA_EVENTO_INVALIDA para destino passado/atual.

Evento é bloqueado `FOR UPDATE` antes das mutações. Ordem dos locks: conta →
sessão → evento → referências/categoria, com artistas em ID crescente. As regras
temporais são verificadas após esperar lock e novamente antes de concluir a
operação. Falha de subsistema provoca rollback; JDBC indisponível/falha SQL tem
resposta 503 pública, sem detalhes internos. Compra futura precisará respeitar
essa ordem e bloquear evento/categoria para proteger a mesma invariável.

Não há DELETE de evento/categoria, operação de compra ou acesso ao conteúdo de
comunidade nesta entrega. Consultas não criam compras nem emitem ingressos.
