# RF-COM-06 — Descoberta pública de comunidades

> **Data de aprovação pelo Product Owner:** 10/10/2026  
> **Situação:** aprovado para desenvolvimento; ainda não implementado  
> **Referências:** [Requisitos funcionais](./ESPECIFICACAO_FUNCIONAL_v0.1.md), [RF-COM-05](./ADENDO_RF-COM-05_COMUNIDADES_POS_EVENTO.md), [Contratos HTTP](../api/CONTRATOS_HTTP_v0.1.md)

## Regra aprovada

**RF-COM-06:** qualquer visitante, incluindo pessoa **não autenticada**, pode listar comunidades existentes e consultar seus **metadados públicos**, como nome, imagem e evento associado.

**RF-COM-06A:** listagens e detalhes públicos **não divulgam** mensagens, conteúdo de publicações, comentários, informações privadas dos membros ou dados protegidos.

**RF-COM-06B:** visualizar uma comunidade não equivale a participar dela. Para entrar, um participante precisa autenticar-se e ser titular de pelo menos um ingresso elegível do evento; a entrada continua **manual** (RF-COM-02). Para ler ou interagir com mensagens e feed, é necessário ser **membro ativo e possuir ingresso elegível** (RF-COM-01, RF-COM-05). Organizador titular do evento mantém acesso autorizado conforme demais regras aprovadas.

**RF-COM-06C:** os mesmos critérios de acesso devem ser validados no backend Java: conhecer um ID de comunidade, URL ou endpoint não concede acesso ao conteúdo protegido.

## Aceite

| Código | Cenário | Resultado |
| --- | --- | --- |
| CA-36 | Visitante sem login consulta lista de comunidades | Recebe apenas metadados públicos |
| CA-37 | Visitante sem login consulta comunidade específica | Vê nome, imagem e referência do evento, sem posts/mensagens |
| CA-38 | Visitante tenta acessar mensagens e publicações diretamente por URL/API | Recebe bloqueio de autenticação/autorização; conteúdo não é retornado |
| CA-39 | Participante com ingresso, mas não membro, consulta mensagens/feed | Bloqueado até aderir manualmente à comunidade |
| CA-40 | Participante membro com ingresso ativo ou utilizado, inclusive após evento | Mensagens e posts acessíveis, conforme regras anteriores |

## Impacto técnico

- `GET /api/comunidades`: público, retorna metadados de descoberta.
- `GET /api/comunidades/{id}`: público, retorna detalhes públicos e, opcionalmente, indicador de elegibilidade para usuários autenticados; não embutir dados protegidos.
- `GET /api/comunidades/{id}/mensagens` e `GET /api/comunidades/{id}/postagens`: protegidos.
- **Sem alteração de schema** necessária apenas por esta decisão.

> **Não aprovado:** abertura de mensagens/posts ao público, ingresso automático na comunidade ou acesso de participante sem ingresso elegível.
