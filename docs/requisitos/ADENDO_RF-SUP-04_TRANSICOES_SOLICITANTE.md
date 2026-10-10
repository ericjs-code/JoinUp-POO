# RF-SUP-04 — Resolver e retornar somente em aguardando_usuario

> **Data:** 10/10/2026  
> **Status:** aprovado pelo Product Owner, sem implementação.
> **Origem:** revisão do fluxo de atendimento do JoinUp PHP original e decisão explícita do PO.

## Regra

**RF-SUP-04:** o solicitante (participante ou organizador titular do ticket) poderá executar as ações **Resolver** ou **Retornar** **somente** quando o ticket estiver no estado `aguardando_usuario`.

**RF-SUP-04A:** `Resolver` em `aguardando_usuario` encerra o chamado, alterando o status para `fechado`.

**RF-SUP-04B:** `Retornar` em `aguardando_usuario` exige texto de retorno não vazio, registra a mensagem com autoria do solicitante e altera o status para `em_atendimento`.

**RF-SUP-04C:** em `aberto`, `em_atendimento` ou `fechado`, as ações `Resolver` e `Retornar` do solicitante são rejeitadas. `fechado` continua terminal, não pode ser reaberto e não aceita mensagens.

## Matriz das transições estabelecidas até aqui

| Papel e ação | Estado atual | Novo estado | Resultado |
| --- | --- | --- | --- |
| Participante/organizador abre ticket | — | `aberto` | cria ticket e registra solicitação |
| Suporte autenticado responde | `aberto` | `aguardando_usuario` | registra resposta e autor |
| Suporte autenticado responde | `em_atendimento` | `aguardando_usuario` | registra resposta e autor |
| Suporte autenticado responde | `aguardando_usuario` | `aguardando_usuario` | pode responder novamente; não implica fechamento |
| Suporte autenticado responde | `fechado` | — | rejeita |
| Suporte encerra | `aberto` ou `em_atendimento` | `fechado` | permitido |
| Suporte encerra | `aguardando_usuario` ou `fechado` | — | rejeita |
| Solicitante resolve | `aguardando_usuario` | `fechado` | permitido |
| Solicitante retorna com mensagem | `aguardando_usuario` | `em_atendimento` | permitido |
| Solicitante resolve ou retorna | `aberto`, `em_atendimento`, `fechado` | — | rejeita |

Esta tabela combina regras anteriormente aprovadas (RF-SUP-01 a 03) com RF-SUP-04. Qualquer mecanismo separado de associação de suporte não altera as permissões de resposta ou encerramento.

## Critérios de aceite adicionais

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| CA-41 | Solicitante tenta resolver ticket `aberto` | Rejeição; estado preservado |
| CA-42 | Solicitante tenta retornar ticket `em_atendimento` | Rejeição; estado preservado |
| CA-43 | Solicitante resolve ticket `aguardando_usuario` | Estado passa a `fechado` |
| CA-44 | Solicitante retorna ticket `aguardando_usuario` com mensagem | Mensagem persistida; estado passa a `em_atendimento` |
| CA-45 | Solicitante tenta resolver/retornar ticket `fechado` | Rejeição; sem novas mensagens |
| CA-46 | Solicitante tenta retornar sem mensagem | Rejeição; estado inalterado |

## Consequências para Java e MySQL

- Validar **titularidade do solicitante e estado atual do ticket no backend**, não apenas pelos botões do frontend.
- Implementar troca de estado e gravação de mensagem **na mesma transação JDBC**, protegendo contra alterações concorrentes.
- `POST /api/me/chamados/{ticketId}/resolucao` apenas se `status_ticket = 'aguardando_usuario'`.
- `POST /api/me/chamados/{ticketId}/mensagens` corresponde à ação `Retornar` do solicitante e só aceita `aguardando_usuario`; o contrato de criação de ticket segue separado.
- As mensagens antigas e o histórico de estados continuam preservados.
- Não foram alteradas as tabelas ou enum de status aprovados no schema v0.2; a restrição é regra transacional.

> O PHP original permitia Resolver/Retornar em outros estados não fechados; o comportamento para Java foi intencionalmente modificado pelo PO nesta decisão.
