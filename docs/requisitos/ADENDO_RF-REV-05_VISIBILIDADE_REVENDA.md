# RF-REV-05 — Visibilidade de anúncios de revenda

> **Aprovado pelo Product Owner:** 10/10/2026
> **Estado:** requisito funcional aprovado para desenvolvimento; não implementado
> **Contrato HTTP:** [Contratos v0.1](../api/CONTRATOS_HTTP_v0.1.md)

## Regra aprovada

**RF-REV-05:** apenas usuários autenticados no perfil **participante**, com conta ativa, podem **visualizar anúncios de revenda** e realizar compras por revenda, observadas as regras já aprovadas.

- Visitantes sem autenticação não podem listar ou acessar detalhes dos anúncios.
- Organizador ou suporte autenticado não recebe automaticamente acesso aos anúncios por seu próprio perfil.
- No frontend, a seção de revendas deve exigir login de participante.
- No backend, endpoints de listagem, consulta e compra exigem autenticação, perfil de participante e conta ativa. Ocultar menus não substitui essa verificação.
- A autenticação não dispensa as regras comerciais: evento futuro, organizador ativo, anúncio disponível, comprador diferente do vendedor, titularidade e transação consistente.
- Essa decisão não altera a visibilidade pública dos **eventos em geral**, nem define por si só a visibilidade de **metadados de comunidades**, que segue pendente.

## Cenários de aceite

| ID | Condição | Resultado esperado |
| --- | --- | --- |
| CA-30 | Visitante sem login tenta consultar anúncios de revenda | HTTP 401; nenhum anúncio retornado |
| CA-31 | Organizador autenticado tenta consultar anúncios de revenda | HTTP 403; nenhum anúncio retornado |
| CA-32 | Suporte autenticado tenta consultar anúncios de revenda | HTTP 403; nenhum anúncio retornado |
| CA-33 | Participante autenticado e ativo consulta revendas | Consulta permitida, respeitando filtros e elegibilidade |
| CA-34 | Visitante tenta comprar diretamente pela URL da API | HTTP 401; nenhuma alteração de ingresso/anúncio |
| CA-35 | Participante desativado usa sessão anteriormente ativa para consultar/comprar | Solicitação negada; nenhuma transação realizada |

> Esta aprovação trata **somente da visibilidade e compra de revendas**; criação, cancelamento e reanúncio seguem as regras de titularidade já homologadas.
