# RF-EVT-06 — Edição geral de eventos somente antes do início

> **Data de aprovação:** 10/10/2026  
> **Status:** aprovado pelo Product Owner para desenvolvimento; ainda não implementado  
> **Base:** [Requisitos consolidados](./ESPECIFICACAO_FUNCIONAL_v0.1.md), [Contratos HTTP](../api/CONTRATOS_HTTP_v0.1.md).

## Decisão

**RF-EVT-06:** o organizador proprietário pode editar as **informações gerais** de seu evento **somente enquanto o evento não tiver começado**. O limite é a data/hora de início do evento já cadastrada. A partir do início (inclusive), a edição dessas informações é bloqueada.

**RF-EVT-06A:** informações gerais abrangem nome, descrição, data, localidade/local, imagem, gênero musical e lineup/artistas. O backend deve verificar autenticação, status ativo do organizador, titularidade e horário de início **antes de persistir** a edição.

**RF-EVT-06B:** se o evento ainda não tiver começado, suas informações gerais podem ser editadas **mesmo que existam ingressos vendidos**, preservando as compras e os ingressos emitidos, conforme RF-EVT-02.

**RF-EVT-06C (complementado pela decisão posterior RF-EVT-07):** tipos que já tiveram ao menos uma venda permanecem bloqueados para edição manual (RF-EVT-03/04), e novos tipos podem ser adicionados após vendas de outros (RF-EVT-05), **mas somente antes do início do evento**. Ver [adendo RF-EVT-07](./ADENDO_RF-EVT-07_CATEGORIAS_APENAS_ANTES_INICIO.md).

**RF-EVT-06D:** a restrição deve ser verificada no backend Java/JDBC também em chamadas diretas da API, independentemente de o frontend ocultar opções.

## Cenários de aceite

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| CA-47 | Organizador dono edita nome de evento futuro, sem vendas | Edição permitida |
| CA-48 | Organizador dono edita descrição/local de evento futuro com vendas | Edição permitida; histórico preservado |
| CA-49 | Organizador dono tenta editar nome, imagem ou lineup depois que evento começou | Operação recusada, sem alteração de dados |
| CA-50 | Organizador dono tenta editar evento que já terminou | Operação recusada |
| CA-51 | Outro organizador tenta editar evento futuro | Operação recusada |
| CA-52 | Requisição de edição chega exatamente no início do evento | Bloqueada pela verificação de data/hora |

## Notas de implementação

- Comparar o instante atual do servidor ao início **persistido** do evento; definir a política de zona horária de forma coerente com o transporte HTTP e o MySQL.
- O frontend deve explicar por que a edição foi bloqueada; servidor deve retornar resposta de conflito do domínio quando evento tiver iniciado.
- Validar a condição novamente no contexto da transação ao persistir; evitar checagem apenas no frontend.
- **Resolvido por RF-EVT-07:** inclusão/edição de categorias após o início do evento são bloqueadas. **Resolvido por RF-EVT-08:** reagendamento de evento futuro exige nova data/hora também futura; data passada ou igual ao instante atual é rejeitada ([adendo](./ADENDO_RF-EVT-08_REAGENDAMENTO_DATA_FUTURA.md)).

> Nenhum código, script de banco ou teste foi executado por esta aprovação.
