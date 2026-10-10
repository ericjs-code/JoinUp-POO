# RF-EVT-08 — Impedir reagendamento para data/hora passada ou atual

> **Aprovação do Product Owner:** 10/10/2026  
> **Estado:** requisito aprovado para desenvolvimento, não implementado.  
> **Relacionados:** [RF-EVT-06](./ADENDO_RF-EVT-06_LIMITE_EDICAO_EVENTOS.md), [RF-EVT-07](./ADENDO_RF-EVT-07_CATEGORIAS_APENAS_ANTES_INICIO.md), [Contratos HTTP](../api/CONTRATOS_HTTP_v0.1.md).

## Regra aprovada

**RF-EVT-08:** organizador autenticado e ativo, **proprietário de evento cujo início ainda não ocorreu**, pode alterar a data/hora do evento somente para outro instante **estritamente futuro**. O sistema rejeita data/hora de destino passada **ou igual ao instante atual**.

**RF-EVT-08A:** a validação deve comparar instantes de tempo coerentes quanto ao fuso horário. A entrada pode conter offset ISO 8601, mas o backend deverá normalizá-la antes da persistência.

**RF-EVT-08B:** evento já iniciado ou encerrado não pode ser reagendado. O bloqueio de edição depois do início (RF-EVT-06) permanece, independentemente da data que se pretende atribuir.

**RF-EVT-08C:** se o evento ainda não tiver começado e já tiver ingressos vendidos, o reagendamento para data/hora futura pode ocorrer; as **compras registradas e os ingressos emitidos permanecem preservados**. Restrições a edição manual de categoria já vendida continuam válidas (RF-EVT-03).

**RF-EVT-08D:** aplicar a validação no backend Java, preferencialmente no momento de persistência da atualização dentro da transação JDBC; a UI pode apresentar mensagens explicativas, mas não é o único controle.

**Limite de escopo:** esta aprovação trata de **edição/reagendamento**. Regras para a criação inicial de eventos (ex.: data mínima), comunicação aos compradores ou políticas externas de remarcação não foram deliberadas nesta decisão e não devem ser inventadas como requisito novo.

## Matriz de aceite

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| CA-61 | Organizador dono reagenda evento ainda futuro para outra data futura | Permitido |
| CA-62 | Organizador tenta colocar evento futuro em data já passada | Bloqueado, nenhuma alteração persistida |
| CA-63 | Organizador tenta reagendar para exatamente o horário atual | Bloqueado |
| CA-64 | Organizador tenta reagendar evento que já começou, ainda que o destino seja futuro | Bloqueado |
| CA-65 | Organizador reagenda evento futuro com vendas existentes para data futura | Permitido, compras e ingressos históricos preservados |
| CA-66 | Terceiro organizador tenta reagendar evento alheio | Bloqueado |

## Consequências técnicas

- `PATCH /api/organizador/eventos/{eventoId}`: ao incluir a data no payload, validar: `instanteInicioExistente > instanteAtual` **e** `novoInstanteInicio > instanteAtual`.
- Recomendar `OffsetDateTime` ou `Instant` no domínio de transporte Java e política de conversão de fuso para MySQL. Não comparar apenas textos de datas.
- Resposta de conflito/validação de domínio para `DATA_EVENTO_INVALIDA` quando a data pretendida não for futura.
- Sem mudança exclusiva no esquema MySQL v0.2.

> A modelagem e a regra foram aprovadas pelo PO; **não há código executável nem scripts testados por esta decisão**.
