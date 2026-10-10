# JoinUp-POO — Análise e adaptação do banco de dados

**Data:** 10/10/2026  
**Situação:** APROVADA PELO PO PARA DESENVOLVIMENTO EM 10/10/2026. **Ainda não executada nem testada no MySQL.**  
**Fonte analisada:** `construir_banco_joinup_ajustado.sql` (anexo do PO; 389 linhas, 18 tabelas).  
**Artefatos:** `JoinUp-POO_SCHEMA_v0.2_RASCUNHO.sql` e `JoinUp-POO_SEED_DEMO_v0.2_RASCUNHO.sql`.

## Diagnóstico do arquivo recebido

O arquivo contém **18 tabelas**: `artistas`, `organizadores`, `generos_musicais`, `eventos`, `participantes`, `comunidades`, `participante_comunidades`, `comunidade_mensagens`, `postagens`, `comentarios`, `tipos_ingressos`, `compras`, `compra_itens`, `ingressos`, `line_up`, `revenda_anuncios`, `usuarios_suporte`, `ticket`. O modelo tem chaves estrangeiras e tipos úteis; foi **preservado como base**, sem substituir os conceitos existentes por um esquema inteiramente novo.

**Atenção crítica:** o script recebido usa `DROP SCHEMA IF EXISTS joinup`, o que apagaria a base antiga se executado. A proposta não utiliza `DROP` e cria o banco independente `joinup_poo`. **A proposta é para instalação nova**; executar o arquivo com `CREATE TABLE IF NOT EXISTS` em um banco já existente não faz migração de colunas.

### Alterações propostas, com motivação

| Mudança | Razão/requisitos |
| --- | --- |
| `joinup_poo` sem comando destrutivo | Isolar o novo projeto do banco PHP original |
| `ativo`, `data_desativacao` em participante/organizador | RF-USR-01/02/03: bloqueio de login e vendas, reativação |
| `sessoes` com hash de token e revogação | Invalidar sessões de contas desativadas; permissões Java puro |
| `participante_comunidades.ativo` e `data_saida` | RF-COM-01/03: associação efetiva, saída e remoção após última revenda |
| `tipos_ingressos.quantidade_vendida` | RF-EVT-03/04: bloquear edição manual após qualquer venda e não confundir com estoque |
| Colunas snapshot em `compras` | Preservar nome/data/local da compra após edição do evento; preço já existia em `compra_itens` |
| `postagem_curtidas` (ausente no anexo) | Curtir/descurtir e evitar curtidas duplicadas |
| Revenda: retirar UNIQUE absoluto de `id_ingresso` e criar chave única condicional via coluna gerada | Permitir anúncios históricos vendidos/cancelados e apenas um anúncio disponível por ingresso (RF-REV-02) |
| `revenda_anuncios.id_comprador`, datas de venda e cancelamento | Rastrear transferência de titularidade e histórico do anúncio |
| `ticket_mensagens` | Suportar chat/histórico de respostas de vários suportes sem perder autor (RF-SUP-02) |
| `auditoria_reativacoes` | Registrar suporte que reativou conta e horário (RF-USR-03) |
| CHECKs e índices pontuais | Coerência de autoria, estoque, preço e consultas. As regras transacionais ainda cabem ao Java |

O arquivo original inclui os campos legados `ticket.resposta` e `ticket.retorno_usuario` e o contador `postagens.curtidas`. Eles foram preservados por compatibilidade de modelo, mas a **fonte da verdade** das novas mensagens deve ser `ticket_mensagens`, e as curtidas devem ser atualizadas em conjunto com a tabela de relacionamento dentro de transação (ou o contador poderá ser substituído na modelagem final).

## Regras que exigem validação no Java, não só SQL

1. Compra direta/revenda apenas para evento ainda não ocorrido com organizador ativo; transação, bloqueio de linha e estoque não negativo.
2. Bloqueio de edição de tipo vendido mesmo com estoque restante; permitir novos tipos; histórico da compra preservado.
3. Desativação de participante + cancelamento dos anúncios disponíveis na **mesma transação**.
4. Organizador desativado impede novas compras e anúncios; **não cancela** anúncios existentes.
5. Acesso à comunidade requer **membro ativo e ingresso válido**; após última venda, inativar associação do vendedor; comprador entra manualmente.
6. Histórico do chamado e estado `fechado` imutável; suporte não encerra `aguardando_usuario`.
7. Reativação somente por suporte autenticado, com auditoria e revalidação de sessões.

## Dúvidas/limites não definidos pelo arquivo

- O SQL original possui `ingressos.status` = `ativo`, `cancelado` ou `utilizado`; a interpretação de **ingresso válido para comunidade** (especialmente após evento passado/utilizado) continua pendente do PO.
- O campo `participantes.data_nascimento` é `CHAR(14)` no anexo; preservado para evitar mudanças de contrato não solicitadas. Uma conversão futura para `DATE` pode ser avaliada quando o frontend for definido.
- O SQL original possuía exclusividade de anúncio por ingresso mesmo após cancelamento/venda; a versão nova adota múltiplas linhas históricas, e **reanunciar** cria um novo anúncio, preservando o cancelado. A API Java deve seguir isso.
- A carga demo usa datas de eventos relativas a `NOW()` apenas em **primeira execução**. Reexecutá-la dias depois não move as datas existentes.
- A especificação diz que compra é simulada; **não criar cobrança financeira real**.

## Dados demonstrativos

O `seed` é intencionalmente separado do esquema, contém usuários dos três perfis com senha PBKDF2 conhecida **somente para ambiente local**, quatro eventos com situações distintas, tipos com/sem vendas, revendas em três estados, mensagens, feed e chamados. Os exemplos de artistas mantêm os nomes do anexo, mas **não copiam CPFs fictícios atribuídos a pessoas reais**: permanecem `NULL`.

**Senha demo:** `JoinUpDemo#2026`. **Nunca habilitar seed demo em produção**, não reutilizar essa senha fora do ambiente local. As contas demonstrativas terminam em `@joinup.test`.

## Verificações efetuadas e pendentes

- Conferida a presença das 18 tabelas originais no DDL adaptado.
- DDL proposto contém **22 tabelas** (adicionais: ticket_mensagens, postagem_curtidas, auditoria_reativacoes, sessoes).
- Script original deixado intacto.
- **Não foi possível executar testes reais em MySQL neste ambiente**: não havia servidor/cliente MySQL disponível. Após homologação, é obrigatório validar a criação do banco, as FKs, a carga demo, o login PBKDF2 e as transações de compra/revenda.

**Homologação:** PO aprovou as alterações incrementais, banco `joinup_poo` e seed demo isolado. Arquivos versionados em `src/main/resources/db/` para posterior execução e testes em instância nova.