# RF-EVT-07 — Categorias de ingresso somente antes do início do evento

> **Aprovação do Product Owner:** 10/10/2026  
> **Estado:** requisito aprovado para desenvolvimento; implementação ainda não iniciada.  
> **Relacionados:** RF-EVT-03, RF-EVT-04, RF-EVT-05, [RF-EVT-06](./ADENDO_RF-EVT-06_LIMITE_EDICAO_EVENTOS.md).

## Regra aprovada

**RF-EVT-07:** o organizador responsável só poderá **adicionar novas categorias de ingresso e editar categorias ainda sem vendas enquanto a data e hora de início do evento não tiver chegado**. Após o início do evento, ambas as operações são bloqueadas, mesmo que determinada categoria nunca tenha sido vendida.

**RF-EVT-07A:** em eventos futuros, é permitido acrescentar categorias mesmo quando outras categorias do mesmo evento já possuem vendas, conforme RF-EVT-05.

**RF-EVT-07B:** enquanto o evento ainda for futuro, é permitido editar nome, preço e quantidade manualmente de uma categoria **que nunca teve nenhuma venda**, conforme RF-EVT-04.

**RF-EVT-07C:** categorias com ao menos uma venda histórica não podem ter nome, preço ou quantidade configurada alterados manualmente, independentemente do estoque atual, conforme RF-EVT-03. A baixa de estoque por compras continua automática e permitida somente dentro das regras de compra.

**RF-EVT-07D:** as validações de **propriedade do evento**, **conta ativa**, **ausência de vendas** e **data/hora anterior ao início** devem ocorrer no backend Java, preferencialmente no contexto da transação JDBC, para prevenir requisições diretas ou alterações concorrentes.

## Matriz de decisão

| Evento | Categoria alvo | Criar nova categoria | Editar categoria alvo |
| --- | --- | --- | --- |
| Futuro | Sem vendas | Sim | Sim |
| Futuro | Com vendas | Sim (nova categoria) | Não |
| Futuro | Outra categoria do evento vendeu, mas a categoria alvo não | Sim | Sim, somente categoria alvo |
| Iniciado ou encerrado | Com ou sem vendas | Não | Não |
| Qualquer fase | Usuário não é o organizador responsável | Não | Não |

**Atenção:** não foi aprovado excluir categorias de ingresso. Não criar endpoint para exclusão sem nova decisão.

## Critérios de aceite adicionais

| ID | Cenário | Resultado |
| --- | --- | --- |
| CA-53 | Organizador dono cria categoria para evento futuro, sem vendas | Permitido |
| CA-54 | Organizador dono cria categoria para evento futuro com vendas em outras categorias | Permitido |
| CA-55 | Organizador dono edita categoria sem venda de evento futuro | Permitido |
| CA-56 | Organizador dono edita categoria que teve venda, mas está com estoque disponível | Bloqueado |
| CA-57 | Organizador tenta adicionar categoria depois do início do evento | Bloqueado sem criar registros |
| CA-58 | Organizador tenta editar categoria sem vendas depois do início | Bloqueado sem alterar registros |
| CA-59 | Organizador distinto tenta modificar categorias de evento alheio | Bloqueado |
| CA-60 | Requisição de criação ou edição ocorre exatamente na hora de início | Bloqueada |

## Consequências arquiteturais

- `POST /api/organizador/eventos/{eventoId}/tipos-ingresso`: autorização e verificação temporal obrigatórias.
- `PATCH /api/organizador/eventos/{eventoId}/tipos-ingresso/{tipoId}`: autorização, verificação temporal e ausência de qualquer venda histórica obrigatórias.
- Uso do mesmo instante de referência para verificar o início do evento e restrições de compra. Para impedir resultado parcialmente aplicado, realizar operações JDBC sob transação com rollback.
- **Nenhuma alteração ao schema v0.2 necessária exclusivamente por esta regra.**

> A especificação funcional original continua versionada e este adendo resolve a questão de categorias após o início que estava explicitamente pendente em RF-EVT-06.
