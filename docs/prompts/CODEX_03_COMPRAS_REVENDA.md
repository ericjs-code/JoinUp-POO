# CODEX — Entrega 03: compra direta e revenda transacionais

> **JoinUp-POO:** `ericjs-code/JoinUp-POO`  
> **Data:** 10/10/2026 | entrega acadêmica 15/10/2026 | apresentação 16/10/2026  
> **Situação:** PRs #1/#2/#3 já integrados à main; Entrega 02 [auditada com ressalvas](../execucao/AUDITORIA_ENTREGA_02.md).  
> **Branch:** `feat/03-compras-revendas` partindo de `main` atualizada. **NÃO FAÇA MERGE ANTES DA AUDITORIA**.

## 0. Resultado exigido

Implementar os dois fluxos centrais de negócios ainda ausentes: **compra direta de ingressos (pagamento local simulado)** e **publicação, listagem, cancelamento, reanúncio e compra por revenda com transferência de titularidade**, integrados ao frontend e ao MySQL real. Preservar todos os 4 GoF reais (Facade, Template Method, Factory Method, Singleton) sem alterá-los por conveniência; compras/revendas usam casos de uso/serviços/repositórios adequados, sem inventar um quinto padrão demonstrativo.

O principal critério é **consistência sob concorrência**: nunca vender mais unidades que o estoque; nunca permitir dois compradores adquirirem o mesmo anúncio; nunca deixar transações parcialmente aplicadas. Documentar a estratégia de locks e as razões.

## 1. Leia integralmente antes de codificar

- `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` **e TODOS** os adendos `docs/requisitos/ADENDO_*.md`; dar atenção a **RF-REV-05** (anúncios só visíveis a participantes autenticados), RF-COM-05 (acesso após evento/ingresso utilizado), RF-USR-01 a 05, RF-ING-01 e RF-REV-01 a 04.
- `docs/api/CONTRATOS_HTTP_v0.1.md`, especialmente seções **4 e 5** e as regras transacionais.
- `docs/execucao/AUDITORIA_ENTREGA_02.md`, `docs/execucao/ENTREGA_02_RESULTADOS.md` e `docs/execucao/PLANO_CODEX.md`.
- `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`, `docs/arquitetura/CLASSES_E_PADROES_GOF_v0.1.md`, `docs/diagramas/CLASSES_GOF_v0.1.puml`, `docs/diagramas/EVENTOS_DOMINIO_02.puml`.
- Schema/seed atuais `src/main/resources/db/schema_v0.2.sql` e `seed_demo_v0.2.sql`, validados em MySQL 8.4.0; examinar tabelas `compras`, `compra_itens`, `tipos_ingressos`, `ingressos`, `revenda_anuncios`, `participante_comunidades`, `eventos`, `organizadores`, `participantes`.
- Código atual: `ExecutorTransacional`, `GerenciadorSessao`, `ContaService.desativar`, `EventoFacade`, `EventoRepositoryJdbc`, `TipoIngressoRepositoryJdbc`, UI atual e testes MySQL.
- Consulte os arquivos de compra/revenda e suas telas do repositório PHP original `ericjs-code/JoinUp`, **somente leitura**, para checar formatos, navegação e identidade visual. Não copiar regras contraditórias com decisões recentes do PO.

**Conflitos ou lacunas:** documentos de adendo mais recentes prevalecem. Não inventar pagamento externo, nova política de elegibilidade ou alterações de estados sem sinalizar ao Tech Lead/PO. Documentar decisões puramente técnicas.

## 2. Restrições técnicas

Java **21 puro**, JDK `HttpServer`, JDBC/MySQL 8.4, Jackson, Maven, JUnit 5. **Não** adicionar Spring, Javalin, Servlet, Hibernate, JPA/ORM, injeção automática ou gateway real de pagamento. Preservar arquitetura modular e composição manual. Reutilizar `ExecutorTransacional` e DTOs/HTTP existentes, sem SQL nos handlers.

Autorização obrigatória **no servidor** (sessão, perfil, conta ativa, propriedade, titularidade, situação do organizador, instante do evento). Mutáveis devem validar CSRF/origem e corpo JSON. Nunca confiar em `id_participante`, preço, `valor_total`, status, estoque ou proprietário informados pelo cliente.

**Ordem de locks consistente entre módulos**: inspecionar as ordens que já existem em login/desativação e edição de eventos. Definir/documentar uma ordem global segura entre conta/sessão, evento, tipo, ingresso, anúncio e filiação; não presumir que fazer `SELECT FOR UPDATE` isoladamente elimina deadlock. Implementar fallback controlado/erro claro para lock timeout/deadlock onde necessário. Testar contra compra simultânea e desativação concorrente.

## 3. Compra direta — RF-ING-H01 a H04, RF-ING-01, RF-USR-02

**Rotas:**
- `POST /api/compras` — **somente participante autenticado e ativo**, compra simulada persistente.
- `GET /api/me/compras` — histórico das próprias compras.
- `GET /api/me/ingressos` — próprios ingressos, inclusive status `ativo`/`utilizado`/`cancelado`, referência ao evento/tipo e indicador de anúncio disponível.
- `GET /api/me/ingressos/{ingressoId}` — ingresso da titularidade atual do participante, sem vazamento entre usuários.

**Payload alinhado ao contrato:**

```json
{"eventoId":1,"itens":[{"tipoIngressoId":1,"quantidade":2}]}
```

**Comportamento:** validar evento e organizador ativos, data de início **estritamente futura**, categorias pertencentes ao mesmo evento, quantidade positiva e estoque suficiente. Preço é lido e calculado no backend como `BigDecimal`; proibir adulteração do preço/total do cliente. Efetuar sob **uma transação**: bloquear evento e categorias em ordem, debitar `quantidade_disponivel`, aumentar `quantidade_vendida`, inserir `compras` (confirmada local), `compra_itens` com `valor_unitario` e `subtotal` históricos, gerar ingressos individuais `ativo`, gravar snapshots `nome_evento_na_compra`, `data_evento_na_compra`, `local_evento_na_compra`. Revalidar elegibilidade e estoque sob lock; falha em qualquer etapa = rollback integral.

Pedidos inválidos (itens vazios, tipos duplicados, quantidade não inteira, dados extra não previstos, preço negativo ou estoque zero) rejeitados de forma previsível. Não comprar ingressos em nome de outro usuário e não criar assinatura de pagamento real.

**Importante:** compra de ingresso **não inscreve automaticamente** em comunidade (RF-COM-02). Publicar compra e ingressos ao próprio titular, sem tornar públicos seus dados.

## 4. Revenda — RF-REV-H01/H02/H03 e RF-REV-01 a 05

**Rotas (contrato v0.1):**
- `GET /api/revendas?eventoId=&pagina=&tamanho=` — **exige PARTICIPANTE autenticado ativo**, inclusive consulta de lista. Visitante, organizador e suporte **não** visualizam anúncios.
- `GET /api/me/revendas` — histórico dos próprios anúncios, vendidos/cancelados/disponíveis.
- `POST /api/revendas` — anunciar um ou vários ingressos próprios elegíveis; preço unitário configurado pelo vendedor.
- `POST /api/revendas/{anuncioId}/cancelamento` — somente vendedor titular do anúncio **disponível**, mesmo se organizador do evento estiver desativado. Preserve titularidade do ingresso.
- `POST /api/revendas/{anuncioId}/compra` — **outro participante autenticado e ativo** compra um anúncio disponível, a preço persistido pelo anúncio, com transferência de ingresso e atualização de estado `vendido`.
- Se a UI original tiver detalhes de anúncio, pode incluir `GET /api/revendas/{anuncioId}`, protegido com o mesmo papel/permissão e documentado.

**Payload para anúncio:**

```json
{"ingressoIds":[17,18],"precoUnitario":120.00}
```

**Invariantes:**
- Anunciar somente ingresso **`ativo`** de titularidade do vendedor, de evento ainda futuro, com organizador ativo. Ingresso `utilizado` ou `cancelado` não é elegível à revenda; mesmo após o evento, ingresso `utilizado` pode continuar dando acesso à comunidade (RF-COM-05).
- Histórico preservado: cancelar anúncio não reativa ingresso diferente nem transfere dono. Reanunciar um ingresso anteriormente cancelado cria **novo anúncio**, com preço atualizado. A coluna gerada `id_ingresso_disponivel` + unique limita **um anúncio ativo** por ingresso.
- Anúncio vendido jamais cancela; anúncio disponível não vende duas vezes; vendedor não compra seu anúncio; após compra, anúncio guarda `id_comprador` e `data_venda`; titularidade `ingressos.id_participante` muda somente após venda concluída.
- **Participante vendedor inativo:** sua desativação já cancela anúncios disponíveis **transacionalmente** (Entrega 01). Compra de revenda deve impedir inconsistência se desativação ocorrer em paralelo.
- **Organizador inativo:** bloquear novas publicações de anúncio e **compras** diretas/revenda. Anúncios existentes continuam `disponivel` em banco e podem reaparecer como compráveis após reativação, se ainda elegíveis. **Cancelar anúncio existente continua permitido** durante inatividade do organizador.
- Compra só antes do início do evento, com event time revalidado sob lock. Ao **anunciar** último ingresso, vendedor **permanece na comunidade**; após revenda consumada do **último ingresso elegível** (`ativo` ou `utilizado`, RF-COM-05), inativar sua associação em `participante_comunidades` com `data_saida`. Se ainda possuir outro ingresso elegível do mesmo evento, manter associação. Posts/mensagens históricos nunca são apagados.
- **Comprador não entra automaticamente** na comunidade. Somente será elegível a entrar manualmente em Entrega 04.
- **A listagem** pode retornar anúncios temporariamente indisponíveis com marcador `compravel=false` quando organizador estiver inativo. Não divulgar informações privadas de vendedor/comprador ou compradores anteriores.

**Preço e pagamento:** preço persistido deve ser decimal exato, positivo e dentro de `DECIMAL(8,2)`; esta compra também é confirmação **local simulada**, sem cobrança/transferência financeira real. Não introduzir integrações externas. O esquema v0.2 transfere titularidade de um ingresso existente; se precisar de registro financeiro adicional, demonstrar necessidade e obter revisão antes de alterar o schema.

## 5. Convivência com eventos e contas

- A edição de eventos/categorias da Entrega 02 usa lock `FOR UPDATE` no evento e em categorias. Compras diretas devem coordenar-se com essas operações para não alterar estoque sobre preço/data em edição.
- A compra deve ler o preço atualizado **sob lock** e preservar valor unitário histórico. Não modificar o histórico de ingressos emitidos quando organizador editar o evento.
- A desativação de organizador preserva eventos e listagens históricas. Reativação não reinicia estoque/ingressos e **não restaura anúncio cancelado**.
- Participante com anúncio disponível continua proprietário e elegível à comunidade enquanto tem ingresso, como já aprovado; não auto-remover na publicação.
- Devido à possibilidade de ingresso `utilizado` após evento, tratar **elegibilidade comunitária e comercial como regras distintas**. Não excluir associação/histórico físico.

## 6. Frontend mínimo integral dos módulos

Usar visual consistente com o PHP e as páginas existentes; disponibilizar seleção de categorias/quantidade e confirmação de compra simulada, página de meus ingressos e histórico, listagem de revendas protegida, criação/cancelamento/reanúncio, e compra de anúncio por outro participante. Mostrar estado real de preço, estoque e anúncios bloqueados, atualizar após cada ação. Usar `textContent` e URLs seguras para dados do banco, `fetch` same-origin com cookie HttpOnly, sem token no storage. Conectar navegação das telas já implementadas.

Expor claramente no frontend que **pagamento é simulado** e não prometer transações bancárias. Incluir instruções para executar fluxo entre **dois participantes reais do seed** no navegador sem usar dados pessoais.

## 7. Testes obrigatórios (MySQL real no CI)

Preservar os **157 testes existentes** e adicionar testes Java unitários/HTTP e MySQL 8.4 reais para:
1. Compra direta com vários tipos, total exato `BigDecimal`, snapshots históricos, ingresso por unidade, estoque e contador vendidos coerentes.
2. Dois compradores tentando comprar **última unidade** ao mesmo tempo: somente um confirmado, nenhum ingresso adicional/estoque negativo.
3. Compra de evento começado, de organizador inativo, tipo de outro evento, dados inválidos, quantidade acima do estoque, sessão ausente/errada, IDOR e falha de SQL após atualizar estoque: rollback de tudo.
4. Listagem de revenda negada (401/403) para visitante/organizador/suporte e permitida somente ao participante ativo (RF-REV-05), mesmo acessando API diretamente.
5. Anúncio de ingresso próprio, bloqueio de status `cancelado`/`utilizado`, evento passado, organizador inativo e preço inválido; bloqueio de anúncio ativo duplicado no banco real.
6. Cancelamento do vendedor, anúncio vendido não cancelável, reanúncio após cancelamento por preço diferente, sem reativar linha antiga.
7. **Dois compradores no mesmo anúncio** ao mesmo tempo; só uma transferência e uma venda, sem duplo pagamento fictício ou novo ingresso.
8. Organizador desativado com anúncio existente: anúncio preservado, compra bloqueada, cancelamento pelo vendedor permitido, reativação restaurando apenas elegibilidade de anúncio disponível.
9. Participante desativado durante compra/revenda: proteção de status/locks; cancelamento automático transacional da Entrega 01 preservado.
10. Vendedor permanece membro após publicar anúncio e só sai **após venda do último ingresso elegível**; quando tem outro ingresso `ativo` ou `utilizado`, a associação continua; comprador **não é inscrito automaticamente**.
11. Evento passado: compra direta/revenda bloqueadas, sem remover acesso histórico por ingresso utilizado. Teste contraprova de estado.
12. Rollback por trigger em etapa intermediária tanto da compra direta como revenda: estoque/compra/itens/ingressos/anúncio/membership idênticos ao início.
13. Regressão: quatro GoF continuam executáveis e o CI `-Pmysql-it clean verify` segue passando. Nenhuma fixture de produção ou banco legado alterada.

Incluir testes que verificam ausência de vazamento de CPF/CNPJ, hashes, tokens, e preço/total não controlados pelo cliente. Usar apenas fixtures locais, limpar dados criados pela suíte.

## 8. Infraestrutura e decisões técnicas

- Não eliminar/recriar banco. O schema/seed já foram validados na Entrega 01; alterar DDL somente se absolutamente necessário, isolando proposta/justificativa.
- Atualizar `docs/api/CONTRATOS_HTTP_v0.1.md` apenas para detalhamento técnico sem mudar regras aprovadas; registrar contratos efetivos de compras e revendas separadamente.
- Atualizar UML somente com classes realmente implementadas, incluindo colaboração com o módulo de eventos e contas.
- **Riscos conhecidos da auditoria 02:** listagem de eventos contém N+1, UI não tem upload real, `JOINUP_DEMO` automático pendente. Não ampliar escopo deste sprint para corrigir tudo; documentar o que fica para estabilização RA2.
- Não prometer segurança de produção/HTTPS sem teste real. Manter testes de CSRF, sessão, lock/rollback e logs sem segredos.

## 9. Entregáveis

- Branch `feat/03-compras-revendas`, PR **aberto, não mesclado**.
- Código Java, DTOs, repositórios, HTTP, UI e testes integrados.
- Relatório `docs/execucao/ENTREGA_03_RESULTADOS.md`: requisitos satisfeitos, comandos, testes reais, CI e flags de dúvidas, transações/locks, riscos, capturas de navegador.
- Contratos `docs/api/COMPRAS_REVENDA_HTTP_03.md` e UML atualizados quando pertinentes.
- Evidência de MySQL real e testes concorrentes/rollback, e separação clara entre compra local simulada e gateway real não implementado.
- **Não fazer merge**. Fornecer URL do PR para revisão técnica **antes** de integrar a `main`.

**Definição de pronto:** compra direta e revenda funcionam de ponta a ponta no MySQL 8.4 real, com locks e atomicidade sob concorrência, participantes/permissões corretos, snapshots/histórico preservados, UI utilizável e regressão verde; caso contrário, PR parcial com bloqueios identificados.
