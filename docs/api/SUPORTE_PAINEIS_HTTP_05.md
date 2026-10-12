# Entrega 05 — Contratos efetivos de suporte e painéis

Implementação Java 21 puro, JDBC/MySQL 8.4, baseada em RF-SUP-01..04 e nos
adendos aprovados. Os contratos abaixo complementam o baseline v0.1.

## Autenticação e transporte

Todas as rotas exigem cookie `joinup_sessao`, conta válida/ativa e perfil
apropriado. Suporte tem perfil independente e não possui desativação no schema.
Todas as mutações exigem a proteção CSRF existente (Origin/Referer de mesma
origem e Fetch Metadata). Corpos JSON exigem `application/json`.

IDs devem estar entre 1 e 2147483647. Campos desconhecidos, tipos incorretos,
JSON duplicado, controles indevidos e campos de autoria fornecidos pelo cliente
são rejeitados. Autor e solicitante sempre vêm da sessão revalidada no backend.
Datas de criação, atualização, envio e fechamento são UTC; transporte ISO 8601
com `Z`, e apresentação no fuso do navegador.

## Rotas e payloads

| Método | Caminho | Perfil e comportamento | Corpo |
| --- | --- | --- | --- |
| POST | `/api/chamados` | Participante/organizador; ticket aberto + primeira mensagem, atomicamente | `{"titulo":"Dúvida","descricao":"Descreva o problema"}` |
| GET | `/api/me/chamados` | Participante/organizador; apenas próprios chamados | — |
| GET | `/api/me/chamados/{ticketId}` | Solicitante titular; detalhe + histórico | — |
| POST | `/api/me/chamados/{ticketId}/mensagens` | Titular; Retornar somente aguardando usuário | `{"mensagem":"Ainda preciso de ajuda"}` |
| POST | `/api/me/chamados/{ticketId}/resolucao` | Titular; Resolver somente aguardando usuário | vazio ou `{}` |
| GET | `/api/suporte/chamados` | Suporte; chamados de ambos os perfis | — |
| GET | `/api/suporte/chamados/{ticketId}` | Suporte; detalhe + histórico | — |
| PATCH | `/api/suporte/chamados/{ticketId}/associacao` | Autoassociar suporte autenticado para acompanhamento | vazio ou `{}` |
| POST | `/api/suporte/chamados/{ticketId}/mensagens` | Qualquer suporte; resposta com autoria individual | `{"mensagem":"Orientação do atendimento"}` |
| POST | `/api/suporte/chamados/{ticketId}/encerramento` | Qualquer suporte; aberto ou em atendimento | vazio ou `{}` |

Título obrigatório até 100 caracteres; descrição e mensagem obrigatórias até
500 caracteres Unicode, após remover espaços externos. O limite das mensagens
mantém compatibilidade com os snapshots `VARCHAR(500)` legados. Nenhum cliente
pode fornecer IDs de solicitante, autor, suporte associado, status ou timestamps.

Criação retorna 201; consultas e demais ações, 200. Mutações retornam
`{"dados":Ticket}`. Não existe endpoint de reabertura, exclusão ou edição de
chamado/histórico. Encerramento e resolução não fabricam uma mensagem.

## Estado e concorrência

| Ação | Origem | Destino | HTTP |
| --- | --- | --- | --- |
| Criar | — | aberto | 201 |
| Suporte responde | aberto / em_atendimento / aguardando_usuario | aguardando_usuario | 200 |
| Suporte responde | fechado | preservado | 409 |
| Suporte encerra | aberto / em_atendimento | fechado | 200 |
| Suporte encerra | aguardando_usuario / fechado | preservado | 409 |
| Titular resolve | aguardando_usuario | fechado | 200 |
| Titular retorna com mensagem | aguardando_usuario | em_atendimento | 200 |
| Titular resolve/retorna | aberto / em_atendimento / fechado | preservado | 409 |

`TicketService` usa a ordem conta → sessão → ticket. O ticket é lido com
`SELECT ... FOR UPDATE`, e estado/titularidade são revalidados na mesma
Connection que atualiza o estado e insere a mensagem. Duas respostas válidas
podem concluir sequencialmente, conservando ambas. Resolver versus Retornar
permite apenas a primeira ação válida, e a segunda recebe 409. Rollback
restaura inclusive snapshots e estado quando a inserção da mensagem falha.

Associação reproduz `src/php/tickets.php` do PHP de referência: atribui o
**próprio** suporte autenticado, sem alterar estado, `atendido` ou mensagens.
Pode substituir o responsável e ser repetida, inclusive num ticket fechado.
Isso não reabre o chamado e não muda os autores do histórico. Não há atribuição
a terceiros nem desassociação por payload. Responder não autoassocia.

`resposta` e `retorno_usuario` guardam somente o último texto correspondente;
`atendido` vale 1 no fechamento e 0 nas transições não terminais. O histórico
confiável permanece `ticket_mensagens`, incluindo as mensagens preexistentes do
seed. `data_fechamento` é preenchida apenas ao fechar. A associação não deve
ser interpretada como identidade de quem respondeu ou encerrou.

## Listas e detalhe

As duas listas aceitam `status` opcional (valor exato do ENUM), `pagina` padrão
1, entre 1 e 10000, e `tamanho` padrão 20, entre 1 e 100. Status vazio significa
todos; valor inválido retorna 400. Filtro e titularidade entram na consulta
parametrizada, e total respeita ambos. Ordem: ID decrescente.

```json
{"dados":[{"id":1,"titulo":"Dúvida","descricao":"Solicitação","status":"aberto",
"participanteId":1,"organizadorId":null,"suporteId":null,
"criadoEm":"2026-10-11T20:00:00Z","atualizadoEm":"2026-10-11T20:00:00Z","fechadoEm":null}],
"pagina":1,"tamanho":20,"total":1}
```

Detalhes aceitam `pagina` e `tamanho` para **mensagens**, padrão 1/50, com os
mesmos limites. Ordem de mensagens: ID crescente, evitando ambiguidade quando
duas mensagens têm timestamp no mesmo segundo. Detalhe/histórico são lidos sob
lock do ticket, impedindo divergência com uma transição concorrente da API.

```json
{"dados":{"ticket":{"id":1,"titulo":"Dúvida","descricao":"Solicitação",
"status":"aguardando_usuario","participanteId":1,"organizadorId":null,"suporteId":1,
"criadoEm":"2026-10-11T20:00:00Z","atualizadoEm":"2026-10-11T20:01:00Z","fechadoEm":null},
"mensagens":[{"id":1,"autorTipo":"participante","autorId":1,"autorNome":"Pessoa demo",
"mensagem":"Solicitação","enviadoEm":"2026-10-11T20:00:00Z"}],
"pagina":1,"tamanho":50,"totalMensagens":2}}
```

Detalhe de terceiro e ID inexistente retornam o mesmo 404 sem divulgar conteúdo.
Perfil incorreto retorna 403 e sessão inválida/ausente, 401. Dados do ticket e
nomes dos autores são acessíveis somente ao titular ou suporte; não contêm
e-mail, CPF/CNPJ, nascimento ou hash de senha.

## Painéis

| Rota | Perfil obrigatório | Escopo |
| --- | --- | --- |
| `GET /api/participante/painel` | PARTICIPANTE | Conta atual, ingressos de titularidade atual, compras originais, anúncios próprios e comunidades autorizadas |
| `GET /api/organizador/painel` | ORGANIZADOR | Eventos próprios e suas categorias/compras/comunidades; chamados próprios |
| `GET /api/suporte/painel` | SUPORTE | Contagens globais de chamados e volume de mensagens; acompanhamento/autoria do suporte atual |

Formato: `{"dados":{"perfil":"...","consultadoEm":"...Z","indicadores":{},
"chamadosPorStatus":{"aberto":0,"em_atendimento":0,"aguardando_usuario":0,"fechado":0},
"eventos":[]}}`. Todos os quatro estados aparecem, mesmo com zero chamados.
Nenhuma rota aceita ID de usuário para trocar o escopo da sessão.

| Painel | Indicadores e definição |
| --- | --- |
| Participante | `ingressos`: todos os ingressos atuais; `proximosIngressos` e `proximosEventos`: somente ativos em eventos estritamente futuros; `comprasConfirmadas` e `valorHistoricoCompras`: compras originais confirmadas do usuário; anúncios disponíveis/vendidos/cancelados: contagem por status; `comunidadesAcessiveis`: filiação ativa **e** ingresso ativo/utilizado do mesmo evento, inclusive pós-evento e organizador inativo |
| Organizador | Total de eventos, futuros e iniciados (`data <= agora`), categorias, estoque disponível, comunidades; `ingressosVendidos`: soma das quantidades em itens de compras confirmadas; compras confirmadas e `valorHistoricoCompras`: valores registrados em compras confirmadas, sem multiplicação por categorias ou itens |
| Suporte | Total de chamados, associados ao próprio suporte, sem associação; respostas globais e do próprio suporte em `ticket_mensagens`; chamados distintos respondidos pelo próprio suporte; total de chamados por tipo de solicitante; criados no dia UTC; média em segundos entre criação e fechamento de tickets fechados com timestamps válidos; percentual global fechado, de 0 a 100 |

Média e percentual sem população retornam `null`; UI mostra “Sem dados”.
Percentual considera todo o universo de tickets, sem atribuir fechamento a um
atendente por associação. Contagem de respostas mede mensagens, não pessoas ou
resoluções. Não há SLA, lucro, faturamento recebido ou saldo para saque.

Eventos do participante: até 20 próximos eventos com contagem de ingressos,
ordenados por data crescente. Eventos do organizador: até 20 eventos por data
decrescente, com situação/categorias/vendas/valor histórico. Totais do painel
consideram todos os registros; o limite das listas é anunciado na UI.
Organizador A nunca recebe agregações ou listas de B.

Painéis fazem 3 consultas de domínio para participante/organizador e 2 para
suporte, além da validação de sessão. Não há consultas JDBC por evento no Java.
SQL parametrizado e agregações independentes evitam JOINs que multiplicariam
quantidades/valores. Um snapshot REPEATABLE_READ dá coerência entre consultas
da mesma resposta; uma única referência UTC decide eventos futuros/iniciados.

Uma transferência por revenda muda os ingressos atuais do vendedor/comprador,
mas não transfere compras históricas nem incrementa vendas diretas do organizador.
`anunciosDisponiveis` significa status armazenado, não garantia de elegibilidade
comercial de compra; organizador inativo pode bloquear um anúncio disponível.

## Erros relevantes

| HTTP | Código | Condição |
| --- | --- | --- |
| 400 | REQUISICAO_INVALIDA | Texto, corpo, ID, filtro ou paginação inválidos |
| 401 | NAO_AUTENTICADO | Sessão ausente, inválida, expirada ou revogada |
| 403 | PERMISSAO_NEGADA / CONTA_INATIVA / CSRF_NEGADO | Perfil, conta ou origem sem autorização |
| 404 | RECURSO_NAO_ENCONTRADO | Ticket inexistente ou de terceiro |
| 409 | TICKET_FECHADO | Ação de estado/mensagem sobre fechado |
| 409 | TICKET_AGUARDANDO_USUARIO | Suporte tenta encerrar aguardando usuário |
| 409 | TICKET_ESTADO_INVALIDO | Titular resolve/retorna fora de aguardando usuário |
| 409 | CONCORRENCIA_CONFLITO | Deadlock/timeout de lock, com rollback |
| 415 | TIPO_NAO_SUPORTADO | Corpo não é application/json |
| 503 | BANCO_INDISPONIVEL | Falha JDBC; detalhes internos não publicados |

## Interface e limites

`/painel.html` atende os três perfis e `/painel.html?aba=chamados` abre os
chamados. A navegação depende do perfil, e suporte encontra cadastro de outros
suportes/reativação em “Contas e novos suportes”, preservando as rotas da
Entrega 01. Abertura, histórico, filtros, paginação e ações usam as APIs acima.
UI usa `textContent` para dados, impede envio duplicado enquanto uma operação
está em andamento e recarrega estado em 409 sem repetir a mutação.

Saques, push, notificações externas, base de conhecimento e preferências são
informativos/demonstrativos. Não se criam gateways ou serviços externos.
Paginação OFFSET não é snapshot imutável entre requisições. Não foi adicionada
tabela de auditoria de cada transição/associação: encerramento sem texto não
produz mensagem nem autor de fechamento inferido. O schema/seed v0.2 permanecem
intactos. Veja [execução e riscos](../execucao/ENTREGA_05_RESULTADOS.md).
