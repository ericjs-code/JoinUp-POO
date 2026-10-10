# JoinUp-POO — Contratos HTTP propostos (Java puro)

> **Versão:** 0.1 — 10/10/2026  
> **Estado:** APROVADO PELO PRODUCT OWNER COMO BASE DO DESENVOLVIMENTO EM 10/10/2026; contrato evolutivo sujeito às regras e adendos aprovados; **ainda sem implementação**.  
> **Referências:** [Requisitos homologados](../requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md), [Arquitetura Java puro](../arquitetura/ADR-001-STACK-JAVA-PURO.md), [Schema MySQL](../../src/main/resources/db/schema_v0.2.sql), [Parecer do banco](../banco/PARECER_BANCO_v0.2.md).

## 1. Convenções e limites

- Prefixo das rotas: `/api`; HTTP e JSON UTF-8. O frontend HTML/CSS/JavaScript e a API estarão na mesma origem via `HttpServer` do JDK.
- Roteamento HTTP próprio: handlers → services / casos de uso → JDBC. Operações de múltiplas tabelas usam uma transação.
- **Papéis:** `PUBLICO`, `PARTICIPANTE`, `ORGANIZADOR`, `SUPORTE`; autorizações por perfil e **titularidade do recurso**, verificadas no servidor a cada operação.
- Login por perfil cria cookie de sessão `HttpOnly`, `SameSite=Lax` e `Secure` sob HTTPS; identificador imprevisível, persistido somente como hash em `sessoes`. Logout e desativação revogam sessões. Operações com alteração de estado exigem proteção CSRF conforme implementação final.
- `GET` não altera estado. `POST` cria ou aciona transição; `PATCH` altera parcialmente; `DELETE` remove **participação**, não registros históricos.
- Identificadores: inteiros positivos (ex.: `eventoId`, `ingressoId`). Data/hora: formato ISO 8601 com offset no transporte; conversão/fuso do MySQL padronizados antes do código. Valores monetários convertidos pelo backend a `BigDecimal`.
- Respostas de listas: `{ "dados": [], "pagina": 1, "tamanho": 20, "total": 0 }` quando pagináveis; detalhes: `{ "dados": { ... } }`. O envelope é **proposta** e não é a resposta do PHP original.
- Respostas de erro: `{ "erro": { "codigo": "ESTOQUE_INSUFICIENTE", "mensagem": "Quantidade indisponível", "detalhes": {} } }`.
- Status esperados: 200 consulta/ação, 201 criação, 204 logout/saída, 400 corpo inválido, 401 não autenticado, 403 falta de permissão, 404 não localizado ou recurso não acessível, 409 conflito de estado/concorrência, 422 validação de campos. Não expor dados internos do SQL.
- Operações que no PHP são simulações (pagamento externo, pedido de saque, notificações) continuam **sem integração real**. A confirmação local da compra persiste ingressos e estoque conforme os requisitos.

## 2. Autenticação, cadastro, perfis e contas

| Método | Rota | Acesso | Finalidade / requisito |
| --- | --- | --- | --- |
| POST | `/api/auth/participantes/login` | Público | Login do participante via Template Method; RF-AUTH-H02 |
| POST | `/api/auth/organizadores/login` | Público | Login do organizador via Template Method |
| POST | `/api/auth/suporte/login` | Público | Login do suporte via Template Method |
| GET | `/api/auth/me` | Autenticado | Perfil e dados mínimos da sessão corrente |
| POST | `/api/auth/logout` | Autenticado | Revoga sessão corrente |
| POST | `/api/participantes` | Público | Cadastro público de participante via Factory Method |
| POST | `/api/organizadores` | Público | Cadastro público de organizador via Factory Method |
| GET | `/api/me/perfil` | Participante/Organizador | Consultar próprio perfil |
| PATCH | `/api/me/perfil` | Participante/Organizador | Atualizar campos permitidos do próprio perfil |
| POST | `/api/me/desativacao` | Participante/Organizador | Desativa a própria conta, revoga sessões; participante cancela anúncios disponíveis **na mesma transação** (RF-USR-01/04) |
| POST | `/api/suporte/usuarios` | **Suporte** | Cadastrar outro suporte (qualquer conta de suporte autenticada); RF-AUTH-01 |
| GET | `/api/suporte/contas?tipo=participante|organizador&situacao=inativa` | **Suporte** | Localizar contas para reativação; apenas informações necessárias |
| POST | `/api/suporte/contas/{tipo}/{id}/reativacao` | **Suporte** | Reativar participante/organizador, registrar auditoria; RF-USR-03 |

**Contrato resumido de login:** `{ "email": "ana.demo@joinup.test", "senha": "***" }`. Resposta 200 inclui `{ "dados": { "id": 1, "perfil": "PARTICIPANTE", "nome": "..." } }`; o token **não aparece no JSON**, apenas em cookie HttpOnly. Nunca retornar hash de senha.

**Desativação e auditoria:** `POST /api/me/desativacao` não é exclusão física. Após a conclusão, cookie é invalidado. `POST /api/suporte/contas/participante/4/reativacao` deve ser idempotente semanticamente ou retornar conflito claro quando a conta já estiver ativa; definir comportamento exato antes do código.

## 3. Explorar eventos, administração de eventos e artistas

| Método | Rota | Acesso | Finalidade / requisito |
| --- | --- | --- | --- |
| GET | `/api/eventos?busca=&generoId=&cidade=&pagina=` | Público | Explorar/pesquisar; RF-EVT-H03 |
| GET | `/api/eventos/{eventoId}` | Público | Detalhes do evento, preços e disponibilidade |
| GET | `/api/eventos/{eventoId}/tipos-ingresso` | Público | Tipos e estoque consultável |
| GET | `/api/generos-musicais` | Público | Categorias de gênero musical |
| GET | `/api/artistas?busca=` | Público | Buscar artistas para line-up |
| GET | `/api/organizador/eventos` | **Organizador** | Lista apenas eventos de sua autoria |
| POST | `/api/organizador/eventos` | **Organizador** | **Facade** cria evento, lineup, tipos e comunidade em **uma transação**; RF-EVT-H01/H02 |
| PATCH | `/api/organizador/eventos/{eventoId}` | **Organizador dono, conta ativa** | Edita dados gerais e lineup **somente antes do início**; se reagendar, exige nova data/hora **estritamente futura**, mesmo com vendas, preservando históricos; RF-EVT-01/02/06/08 |
| POST | `/api/organizador/eventos/{eventoId}/tipos-ingresso` | **Organizador dono e ativo** | Adiciona categoria **somente antes do início do evento**, inclusive após vendas de outras categorias; RF-EVT-05/07 |
| PATCH | `/api/organizador/eventos/{eventoId}/tipos-ingresso/{tipoId}` | **Organizador dono e ativo** | Edita nome, preço e quantidade **somente antes do início do evento e se a categoria nunca teve venda**; RF-EVT-03/04/07 |

**Criar evento, exemplo de payload (ilustrativo, sujeito ao formulário original):**

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
  "artistaIds": [1, 3],
  "tiposIngresso": [
    {"nome": "Pista", "preco": 100.00, "quantidade": 100}
  ]
}
```

Imagem será tratada pelo contrato de upload; o campo de referência da imagem entra em fluxo separado. A edição de dados gerais **não modifica compras anteriores**. **RF-EVT-06:** só aceitar alterações enquanto o evento ainda não começou; rejeitar a partir da data/hora de início cadastrada, inclusive, mesmo por chamada direta da API. **RF-EVT-08:** ao alterar a data/hora, exigir que o destino seja **estritamente posterior ao instante atual**, com normalização de fuso. Ver [RF-EVT-06](../requisitos/ADENDO_RF-EVT-06_LIMITE_EDICAO_EVENTOS.md) e [RF-EVT-08](../requisitos/ADENDO_RF-EVT-08_REAGENDAMENTO_DATA_FUTURA.md). Excluir evento não é uma rota prevista.

## 4. Compra direta e meus ingressos

| Método | Rota | Acesso | Finalidade / requisito |
| --- | --- | --- | --- |
| POST | `/api/compras` | **Participante ativo** | Compra simulada local: estoque, compra, itens, ingressos e baixa em transação |
| GET | `/api/me/compras` | **Participante** | Histórico das próprias compras |
| GET | `/api/me/ingressos` | **Participante** | Próprios ingressos e indicações de revenda |
| GET | `/api/me/ingressos/{ingressoId}` | **Participante titular** | Consultar ingresso individual |

**Exemplo:** `POST /api/compras`:

```json
{
  "eventoId": 1,
  "itens": [{"tipoIngressoId": 1, "quantidade": 2}]
}
```

**Invariantes:** evento ainda não ocorrido (`data > agora`), organizador ativo, categoria associada ao evento, quantidade inteira positiva, estoque suficiente, comprador ativo; `SELECT ... FOR UPDATE`, histórico/snapshot e rollback se algo falhar. Resposta contém `compraId`, itens, total e IDs dos ingressos emitidos. Não criar gateway real de pagamento.

## 5. Revenda e transferência

**RF-REV-05 — Acesso restrito:** listagem e detalhes de anúncios de revenda não são públicos; apenas participantes autenticados com conta ativa podem visualizá-los e comprar. A permissão deve ser aplicada no backend, inclusive em APIs acessadas diretamente. Organizador e suporte não herdam esse acesso por terem login de outro perfil. A compra continua sujeita às demais validações.

| Método | Rota | Acesso | Finalidade / requisito |
| --- | --- | --- | --- |
| GET | `/api/revendas?eventoId=&pagina=` | **Participante autenticado e ativo** | Listar anúncios; acesso negado a visitantes sem login, organizadores e suporte (RF-REV-05); indicar/filtrar impossibilidade de compra por organizador inativo |
| GET | `/api/me/revendas` | **Participante** | Meus anúncios, inclusive cancelados e vendidos |
| POST | `/api/revendas` | **Participante proprietário** | Anunciar um ou vários ingressos elegíveis, sem anúncio disponível duplicado |
| POST | `/api/revendas/{anuncioId}/cancelamento` | **Vendedor proprietário** | Cancela anúncio ainda disponível; permanece titular do ingresso |
| POST | `/api/revendas/{anuncioId}/compra` | **Outro participante ativo** | Compra por revenda, transfere ingresso, marca vendido e verifica saída da comunidade |

**Exemplo:** `POST /api/revendas`:

```json
{ "ingressoIds": [1, 2], "precoUnitario": 120.00 }
```

**Reanunciar:** após cancelamento, a proposta é usar novamente `POST /api/revendas`, criando **novo anúncio histórico**, com preço novo e sem reativar a linha cancelada. Isso reproduz a decisão sobre manter histórico do banco v0.2.

**Invariantes:** não anunciar se organizador desativado; anúncios existentes continuam no banco com status `disponivel` mas inelegíveis para compra enquanto organizador inativo; compra bloqueada se evento ocorrido, anúncio vendido/cancelado, vendedor não titular, comprador = vendedor ou organizador inativo. Transação de revenda deve transferir o ingresso e inativar a associação do vendedor à comunidade se ele ficou sem qualquer ingresso válido. Anúncio apenas publicado **não retira** acesso à comunidade. Comprador entra na comunidade **manualmente**.

## 6. Comunidades, mensagens e feed

**RF-COM-06 — Descoberta pública:** listar comunidades ou consultar seus dados públicos não exige login; restringir a nome, imagem e referência do evento (e outros campos explicitamente classificados como públicos). Não embutir mensagens, posts, comentários ou dados privados. Acesso ao conteúdo requer autorização independente. Ver [adendo aprovado](../requisitos/ADENDO_RF-COM-06_DESCOBERTA_PUBLICA.md).

| Método | Rota | Acesso | Finalidade / requisito |
| --- | --- | --- | --- |
| GET | `/api/comunidades` | **Público, inclusive sem login** | Listar comunidades com metadados públicos (nome, imagem, evento); RF-COM-06 |
| GET | `/api/comunidades/{id}` | **Público, inclusive sem login** | Retornar somente metadados públicos; nunca incluir mensagens/posts privados; RF-COM-06 |
| POST | `/api/comunidades/{id}/participacao` | **Participante com ingresso válido** | Entrada manual |
| DELETE | `/api/comunidades/{id}/participacao` | **Participante membro** | Saída voluntária; histórico preservado |
| GET | `/api/comunidades/{id}/mensagens` | **Membro ativo com ingresso válido**, ou organizador dono | Consultar mensagens |
| POST | `/api/comunidades/{id}/mensagens` | Idem | Enviar mensagem, resposta e/ou imagem onde suportado |
| GET | `/api/comunidades/{id}/postagens` | Idem | Feed da comunidade |
| POST | `/api/comunidades/{id}/postagens` | **Participante habilitado** | Criar postagem de texto/imagem |
| GET | `/api/postagens/{id}` | Acesso autorizado à comunidade | Detalhes e interações |
| PUT | `/api/postagens/{id}/curtida` | **Participante habilitado** | Curtir, sem duplicar |
| DELETE | `/api/postagens/{id}/curtida` | **Participante habilitado** | Remover curtida |
| GET | `/api/postagens/{id}/comentarios` | Acesso autorizado | Listar comentários |
| POST | `/api/postagens/{id}/comentarios` | **Participante habilitado** | Criar comentário |

**Regra determinante aprovada (RF-COM-05):** autorização de conteúdo comunitário = participante com conta ativa, `membro.ativo == true` **e** ingresso do evento de sua titularidade com status `ativo` **ou** `utilizado`; organizador dono mantém acesso enquanto autenticado e ativo. **O evento ter ocorrido não encerra a comunidade.** Ingresso `cancelado` não dá acesso. Sem mudança nas proibições de compra/revenda após a data do evento. Ver [adendo aprovado](../requisitos/ADENDO_RF-COM-05_COMUNIDADES_POS_EVENTO.md).

**Sem endpoints para editar/excluir publicações**, conforme RF-SOC-01. Compartilhar post é função do frontend. Não criar autoingresso em comunidade após compra ou revenda.

## 7. Suporte e chamados

| Método | Rota | Acesso | Finalidade / requisito |
| --- | --- | --- | --- |
| POST | `/api/chamados` | **Participante/Organizador** | Abrir ticket de suporte |
| GET | `/api/me/chamados` | **Participante/Organizador** | Listar apenas próprios chamados |
| GET | `/api/me/chamados/{ticketId}` | **Solicitante titular** | Consultar detalhes e histórico |
| POST | `/api/me/chamados/{ticketId}/mensagens` | **Solicitante titular** | Ação **Retornar** somente em `aguardando_usuario`: exige mensagem, registra autor e passa a `em_atendimento` (RF-SUP-04) |
| POST | `/api/me/chamados/{ticketId}/resolucao` | **Solicitante titular** | Ação **Resolver** somente em `aguardando_usuario`: encerra como `fechado` (RF-SUP-04) |
| GET | `/api/suporte/chamados?status=&pagina=` | **Suporte** | Listar chamados de todos com filtros |
| GET | `/api/suporte/chamados/{ticketId}` | **Suporte** | Detalhes, autores e mensagens |
| PATCH | `/api/suporte/chamados/{ticketId}/associacao` | **Suporte** | Associar atendente para acompanhamento |
| POST | `/api/suporte/chamados/{ticketId}/mensagens` | **Qualquer suporte autenticado** | Responder mesmo associado a outro; grava autor real; passa a `aguardando_usuario` |
| POST | `/api/suporte/chamados/{ticketId}/encerramento` | **Suporte** | Fechar apenas onde permitido pelo status |

**Estados:** `aberto`, `em_atendimento`, `aguardando_usuario`, `fechado`. `fechado` é terminal e não permite mensagens. Suporte não fecha `aguardando_usuario`; somente o solicitante pode **Resolver** (`→ fechado`) ou **Retornar** com mensagem (`→ em_atendimento`), e **apenas se o estado atual for `aguardando_usuario`**. As transições confirmadas estão documentadas em [RF-SUP-04](../requisitos/ADENDO_RF-SUP-04_TRANSICOES_SOLICITANTE.md). O fluxo aprovado substitui o comportamento original de aceitar retorno/resolução em outros estados.

## 8. Painéis, arquivos e funções demonstrativas

| Método | Rota | Acesso | Finalidade |
| --- | --- | --- | --- |
| GET | `/api/organizador/painel` | Organizador | Indicadores reais derivados de eventos e compras do próprio organizador |
| GET | `/api/suporte/painel` | Suporte | Contagens de tickets, estados e métricas suportadas |
| GET | `/api/participante/painel` | Participante | Resumo para cards e navegação |
| POST | `/api/arquivos` | Autenticado, com regras de perfil | Upload de imagem de perfil, evento, mensagem ou post; contrato multipart pendente |
| GET | `/api/arquivos/{arquivoId}` | Conforme titularidade/publicidade | Recuperar imagem com validação de acesso |
| GET | `/api/health` | Público | Diagnóstico mínimo sem revelar configuração ou segredos |

**Sem API bancária de saque, push ou cobrança**, enquanto a especificação mantiver essas partes como simuladas. Notificações locais poderão continuar no frontend.

## 9. Códigos de erro de domínio propostos

| Código | HTTP sugerido | Quando ocorre |
| --- | --- | --- |
| `REQUISICAO_INVALIDA` | 400 ou 422 | Corpo/identificadores inválidos |
| `NAO_AUTENTICADO` | 401 | Sessão ausente, expirada ou inválida |
| `PERMISSAO_NEGADA` | 403 | Perfil/titularidade insuficiente |
| `CONTA_INATIVA` | 403 | Usuário desativado |
| `RECURSO_NAO_ENCONTRADO` | 404 | ID inexistente ou inacessível |
| `EVENTO_ENCERRADO` | 409 | Evento passado na compra/revenda |
| `EVENTO_JA_INICIADO` | 409 | Organizador tenta editar informações gerais, adicionar categoria ou editar categoria após o início do evento (RF-EVT-06/07) |
| `DATA_EVENTO_INVALIDA` | 422 | Organizador tenta reagendar evento futuro para instante passado ou atual (RF-EVT-08) |
| `ORGANIZADOR_INATIVO` | 409 | Compra/anúncio de evento com organizador inativo |
| `ESTOQUE_INSUFICIENTE` | 409 | Não há estoque para compra solicitada |
| `TIPO_INGRESSO_COM_VENDAS` | 409 | Categoria vendida não pode ser editada manualmente |
| `REVENDA_INDISPONIVEL` | 409 | Anúncio cancelado/vendido ou sem titularidade válida |
| `ACESSO_COMUNIDADE_NEGADO` | 403 | Falta filiação ativa e/ou ingresso válido |
| `TICKET_FECHADO` | 409 | Tentativa de interação com ticket terminal |
| `TICKET_AGUARDANDO_USUARIO` | 409 | Suporte tenta fechar enquanto aguarda solicitante |

## 10. Regras transacionais e rastreabilidade

| Caso | Operações no mesmo commit/rollback | Critérios de aceite |
| --- | --- | --- |
| Criar evento (Facade) | evento → lineup → tipos → comunidade | CA-03 |
| Compra direta | checar organizador/evento/estoque → compra/itens → emissão → redução de estoque | CA-08, CA-18 |
| Comprar revenda | reservar anúncio/ingresso → marcar vendido → trocar dono → eventual saída do vendedor da comunidade | CA-12, CA-15 |
| Desativar participante | marcar inativo → cancelar anúncios disponíveis → revogar sessões | CA-16 |
| Desativar organizador | marcar inativo → revogar sessões; anúncios existem porém compras são bloqueadas por validação | CA-14 |
| Reativar conta por suporte | marcar ativo → registrar autor da reativação | CA-17 |
| Responder suporte | inserir mensagem com autor real → ajustar estado | CA-19, CA-20, CA-21 |

## 11. Pendências antes de congelar API / Codex

1. **[A] RESOLVIDO:** ingresso `ativo` ou `utilizado` permite acesso à comunidade após o evento, com associação ativa (RF-COM-05).
2. **[A] RESOLVIDO:** revendas visíveis apenas a participantes autenticados (RF-REV-05), e comunidades descobertas publicamente só por metadados (RF-COM-06); mensagens e feed permanecem protegidos.
3. **[P]** Fechar todas as transições de status dos tickets conforme PHP e regras aprovadas.
4. **[T]** Decidir tamanho máximo de payload, upload multipart e persistência de imagens no `HttpServer`.
5. **[T]** Testar DDL/seed em MySQL 8 real e validar senha PBKDF2 da carga demonstrativa com o Java.
6. **[T]** Criar matriz de telas/endpoints/entidades/testes para a implementação integral.

**Próxima aprovação solicitada:** validar estrutura das rotas e regras de acesso. O nome preciso dos DTOs pode ser refinado no código desde que o comportamento aprovado permaneça inalterado.
