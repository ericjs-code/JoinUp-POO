# Adendo RF-COM-05 — Comunidades continuam após o evento

> **Aprovado pelo Product Owner:** 10/10/2026  
> **Origem:** validação funcional após proposta dos contratos HTTP v0.1.  
> **Escopo:** regra de acesso às comunidades e ao feed social; não altera condições de venda/revenda.

## Regra aprovada

**RF-COM-05:** a comunidade de um evento **não deixa de ser acessível automaticamente** quando a data e hora do evento passam. Mensagens, publicações, comentários e interações continuam disponíveis após o evento, desde que o usuário mantenha autorização.

**RF-COM-05A:** para um **participante**, o acesso ao conteúdo e a interação exigem simultaneamente: (1) conta autenticada e ativa; (2) vínculo de participação ativo na comunidade; (3) titularidade de ao menos **um ingresso** desse evento em situação `ativo` **ou** `utilizado`. O status `utilizado` **não tira o direito de acesso pós-evento**. O status `cancelado` não é ingresso elegível para esse acesso.

**RF-COM-05B:** o participante pode sair voluntariamente da comunidade; ao sair, perde acesso ao conteúdo até ingressar manualmente novamente, caso ainda tenha ingresso elegível. Quem adquiriu ingresso, inclusive em revenda, não ingressa automaticamente. O organizador acessa a comunidade do evento que organiza enquanto sua própria conta puder autenticar, como previsto nos requisitos existentes.

**RF-COM-05C:** o bloqueio de **compras diretas, compras por revenda e novos anúncios** após a data do evento não deve ser confundido com o acesso à comunidade; são validações independentes. A permanência na comunidade não habilita revenda de ingresso utilizado nem permite comprar ingresso de evento passado.

## Casos de aceite adicionados

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| CA-25 | Evento terminou; participante com ingresso `ativo` e vínculo ativo tenta ler/enviar mensagem | Operação permitida. |
| CA-26 | Evento terminou; ingresso do participante está `utilizado`; vínculo ativo | Acesso e interações permitidos. |
| CA-27 | Evento terminou; participante tem apenas ingresso `cancelado` | Acesso às mensagens/feed protegido negado. |
| CA-28 | Participante com ingresso `utilizado` sai da comunidade e tenta acessar posts | Negado até entrada manual válida. |
| CA-29 | Participante possui ingresso `utilizado` após o evento e tenta nova compra/revenda para esse evento | Compra/revenda bloqueada pelas regras comerciais. |

## Verificação com modelagem de dados v0.2

O campo `ingressos.status` do schema v0.2 já contém `ativo`, `cancelado` e `utilizado`. Não é necessária uma coluna nova para esta regra. O acesso é verificado em `comunidade_membros`/equivalente mais existência de ingresso elegível do mesmo evento pertencente ao participante. Usar checagem de permissão no backend em **todos** os endpoints de mensagens, feed e comentários.

## Estado e limites

**Aprovado para orientar implementação, sem código produzido.** A regra não reativa conta desativada, não modifica propriedade de ingresso e não torna públicos posts de uma comunidade. A política de listagem pública de metadados de comunidades e anúncios ainda é pendente.
