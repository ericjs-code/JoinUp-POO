# Compras e revendas — contratos efetivos da Entrega 03

Data: 10/10/2026. Java 21 / HttpServer / JDBC / MySQL 8.4. Complementa o
[contrato aprovado](CONTRATOS_HTTP_v0.1.md), sem mudar requisitos ou estados.

Todas as rotas abaixo exigem **sessão de participante ativo** no servidor.
Visitante recebe 401; organizador/suporte recebem 403. Sessão revogada/expirada
recebe 401, conta inativa com sessão ainda não revogada recebe 403. As mutações
exigem origem/Referer de mesma origem e `Content-Type: application/json`.
Cookie HttpOnly; sem token no JSON ou storage. GETs não alteram estado.

| Método | Rota | Resposta de sucesso |
| --- | --- | --- |
| POST | `/api/compras` | 201 `{dados:{compraId,eventoId,status,total,itens,ingressoIds,pagamento}}` |
| GET | `/api/me/compras` | 200 `{dados:[compras diretas],revendasRecebidas:[compras de anúncios]}` |
| GET | `/api/me/ingressos` | 200 `{dados:[ingressos de titularidade atual]}` |
| GET | `/api/me/ingressos/{ingressoId}` | 200 `{dados:ingresso}`; 404 se inexistente/de outro titular |
| GET | `/api/revendas?eventoId=&pagina=&tamanho=` | 200 `{dados:[anúncios disponíveis],pagina,tamanho,total}` |
| GET | `/api/revendas/{anuncioId}` | 200 `{dados:anúncio}`; mesmo perfil obrigatório |
| GET | `/api/me/revendas` | 200 `{dados:[anúncios do vendedor, todos os estados]}` |
| POST | `/api/revendas` | 201 `{dados:[novos anúncios]}`; um anúncio por ingresso |
| POST | `/api/revendas/{anuncioId}/cancelamento` | 200 `{dados:anúncio cancelado}` |
| POST | `/api/revendas/{anuncioId}/compra` | 200 `{dados:{anuncioId,ingressoId,status,total,pagamento}}` |

Compra direta:

```json
{"eventoId":1,"itens":[{"tipoIngressoId":1,"quantidade":2}]}
```

Anunciar/reanunciar:

```json
{"ingressoIds":[17,18],"precoUnitario":120.00}
```

Compra de anúncio e cancelamento exigem corpo **`{}`**. Não enviar preço, total,
comprador, vendedor ou status. Corpo vazio/malformado, campos desconhecidos,
duplicados, null explícito, números inteiros enviados como string/decimal e
parâmetros inesperados recebem 400. Identificadores positivos até INT_MAX.
Itens vazios/duplicados, quantidade não positiva, ingresso repetido, referências
inválidas e valor fora do domínio recebem 422. Falta de estoque recebe 409.

Preços são BigDecimal, sem arredondamento de fração de centavo. Revenda exige
`0.01..999999.99`; catálogo de compra direta permite zero conforme schema/
Entrega 02. Total/subtotal limitados a `DECIMAL(10,2)` (`99999999.99`). A baixa
condicionada também impede overflow do contador INT de vendidos. Nenhum campo
monetário de compra direta vem do cliente.

Histórico de compra direta contém `compraId`, `eventoId`, `status`, `total`,
`dataCompra`, `nomeEventoNaCompra`, `dataEventoNaCompra`, `localEventoNaCompra`,
`pagamento` e `itens` (`tipoIngressoId`, `quantidade`, `valorUnitario`, `subtotal`).
Permanece com o comprador original mesmo após revenda. A lista
`revendasRecebidas` registra `anuncioId`, `ingressoId`, `total`, `dataVenda` e
`pagamento`, inclusive se o comprador posteriormente revender o mesmo ingresso.
Utiliza o registro financeiro/local já existente em `revenda_anuncios`; não
duplica `compras`, `compra_itens` ou ingressos e não modifica o schema.

Ingresso mostra ID, estado, evento/tipo atuais, data de emissão, snapshots de
evento da compra original, `anuncioDisponivelId` (null se ausente) e
`organizadorAtivo`. Datas DATETIME normalizadas em UTC; transporte ISO 8601.
O snapshot é da emissão original, não da data de cada revenda. O detalhe do
ingresso muda de autorização imediatamente após transferência; vendedor
anterior recebe 404. A história da compra permanece acessível ao comprador
original, sem revelar contas dos titulares seguintes.

Anúncio mostra `id`, `ingressoId`, `preco`, `status`, `eventoId`, `nomeEvento`,
`dataEvento`, `localEvento`, `nomeTipo`, `proprio`, `compravel`,
`organizadorAtivo`, `dataCriacao`, `dataVenda`, `dataCancelamento`. Projeções não
incluem vendedor/comprador IDs, CPF/CNPJ, e-mail, hashes, tokens ou contas.
`compravel` depende de estado disponível, ingresso ativo, titular igual ao
vendedor, contas ativas, evento futuro e usuário diferente do vendedor.
É indicador da consulta; o serviço revalida tudo dentro da transação.

Listagem de revendas retorna somente estado `disponivel`, inclusive registros
temporariamente bloqueados com `compravel=false`. Filtro evento opcional;
paginação padrão 1/20, tamanho 1..100, offset dentro de INT_MAX. Histórico
pessoal não paginado nesta entrega. Cancelamento exige vendedor do anúncio
disponível e titular atual; organizador inativo ou evento iniciado não impedem
cancelar. Anúncio vendido/cancelado não cancela novamente. Reanunciar cria linha
nova e deixa o registro anterior intacto.

| HTTP | Código | Condição |
| --- | --- | --- |
| 400/422 | `REQUISICAO_INVALIDA` | Transporte/domínio inválido |
| 401 | `NAO_AUTENTICADO` | Sessão ausente, inválida, revogada ou expirada |
| 403 | `CONTA_INATIVA` / `PERMISSAO_NEGADA` | Conta inativa, papel errado, próprio anúncio |
| 404 | `RECURSO_NAO_ENCONTRADO` | Recurso inexistente ou ingresso/cancelamento de outro titular |
| 409 | `EVENTO_ENCERRADO` | Data de início <= agora, inclusive |
| 409 | `ORGANIZADOR_INATIVO` | Nova compra/publicação com organizador inativo |
| 409 | `ESTOQUE_INSUFICIENTE` | Estoque indisponível ou contador excederia INT |
| 409 | `REVENDA_INDISPONIVEL` | Anúncio/ingresso/status/dono/vendedor inelegível ou duplicado ativo |
| 409 | `CONCORRENCIA_CONFLITO` | Deadlock (1213/40001) ou lock timeout (1205); rollback antes da resposta |
| 503 | `BANCO_INDISPONIVEL` | Falha JDBC, sem mensagem SQL ou dados privados |

Pagamento é **confirmação local simulada**, com `pagamento="simulado"`.
Não existe cobrança, repasse financeiro, gateway ou endpoint de pagamento.
Compra/publicação nunca cria filiação. Venda do último ingresso `ativo` ou
`utilizado` inativa a associação existente do vendedor e grava `data_saida`,
sem excluir mensagens/posts. Conta desativada cancela seus anúncios disponíveis
e revoga suas sessões na transação da Entrega 01.

As garantias transacionais/ordem de locks e seus limites estão no
[relatório da entrega](../execucao/ENTREGA_03_RESULTADOS.md).
