# JoinUp-POO — Especificação Consolidada de Requisitos

> **Versão:** 0.1 — 10/10/2026  
> **Situação:** APROVADA PELO PRODUCT OWNER PARA PLANEJAMENTO ARQUITETURAL (10/10/2026); implementação ainda não autorizada  
> **Projeto de destino:** [ericjs-code/JoinUp-POO](https://github.com/ericjs-code/JoinUp-POO) — repositório novo, sem implementação validada nesta versão.  
> **Referência funcional/visual:** [ericjs-code/JoinUp](https://github.com/ericjs-code/JoinUp), branch `main`, consultada em 10/10/2026.  
> **Disciplina:** Design de Software — Avaliação Somativa RA2.

## 1. Finalidade e política de escopo

O JoinUp-POO é uma nova implementação da plataforma social de eventos JoinUp. Deverá ter **backend Java**, **novo frontend visualmente próximo do original** e **banco independente**. O PHP, HTML/CSS/JS e SQL do JoinUp anterior são **referências para comportamentos, regras, fluxos, telas, dados e identidade visual**, não artefatos a serem traduzidos ou copiados obrigatoriamente linha a linha.

**Regra principal aprovada pelo PO:** reproduzir todas as funcionalidades existentes no projeto de referência, preservando seu comportamento, inclusive simulações e elementos demonstrativos, **exceto alterações expressamente aprovadas e registradas**. Mudanças internas necessárias ao design GoF são permitidas sem mudar o comportamento observável; mudanças funcionais adicionais requerem nova validação.

**Quatro padrões GoF obrigatórios e aprovados:** Facade, Template Method, Factory Method e Singleton. Não substituir por outros padrões sem aprovação do PO. Os locais exatos das classes e a justificativa técnica ainda serão homologados no desenho arquitetural.

**Prazo indicado na especificação RA2:** entrega em **15/10/2026**, apresentação em **16/10/2026**. A reimplementação integral traz **risco alto de prazo**; esta documentação registra o escopo, mas não constitui evidência de viabilidade técnica ou de conclusão.

### Legenda de origem e situação

- **[H] Herdado:** comportamento observado no código do JoinUp original; seu princípio de preservação foi aprovado, mas os detalhes devem ser verificados durante a implementação e testes.
- **[A] Alteração aprovada:** diferença explicitamente autorizada pelo PO nesta conversa.
- **[P] Pendente:** decisão, detalhe de design ou comportamento não confirmado. Não adotar hipóteses como requisitos definitivos.
- **[T] Proposta técnica:** alternativa a ser aprovada na arquitetura; não é requisito funcional aprovado.

> **Rastreabilidade de IDs:** para não renumerar as regras já aprovadas durante a conversa, este documento mantém identificadores como `RF-EVT-01` a `RF-EVT-05`, `RF-REV-01` a `RF-REV-04`, `RF-SUP-01` a `RF-SUP-03`, `RF-COM-01` a `RF-COM-04`, `RF-USR-01` a `RF-USR-05`, `RF-SOC-01`, `RF-ING-01`, `RF-AUTH-01`, `BD-01` e `BD-02`. Sub-regras levam sufixos (`A`, `B`, ...); requisitos herdados levam sufixo `Hnn`.

## 2. Atores e áreas

| Ator | Função | Situação |
| --- | --- | --- |
| Visitante | Acessa páginas públicas, busca eventos e acessa os fluxos públicos de cadastro/login | [H] |
| Participante | Consulta eventos, compra ingressos, revende, participa de comunidades, publica/interage e abre chamados | [H] |
| Organizador | Cria e acompanha eventos, gerencia sua comunidade e utiliza suporte | [H] |
| Suporte | Consulta e atende tickets; poderá cadastrar outros suportes e reativar contas de participantes e organizadores | [H] + [A] |

Não há perfil adicional de **suporte administrador**: todo suporte autenticado tem as permissões de suporte definidas nesta especificação [A].

## 3. Catálogo de requisitos funcionais

### 3.1 Autenticação, perfis e contas

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-AUTH-H01 | [H] | Possibilitar cadastro e autenticação de **participante**, **organizador** e **suporte**, respeitando campos específicos dos perfis. |
| RF-AUTH-H02 | [H] | Disponibilizar login por perfil, sessão/autenticação para acesso a áreas protegidas e logout. |
| RF-AUTH-H03 | [H] | Participante pode consultar e editar dados do próprio perfil; organizador também pode consultar e editar os seus. |
| RF-AUTH-01 | [A] | **Somente suporte autenticado** pode cadastrar novo suporte; retirar cadastro público de suporte, validar permissão no backend e expor função em área de suporte. |
| RF-AUTH-01A | [A] | **Qualquer suporte autenticado** poderá cadastrar outros suportes; não criar distinção entre suporte comum e administrador. |
| RF-USR-01 | [A] | Solicitar exclusão de conta de participante ou organizador equivale a **desativar** a conta, sem exclusão física de registros vinculados. |
| RF-USR-01A | [A] | Conta desativada não consegue fazer login; sessão ativa é encerrada ao desativar. |
| RF-USR-03 | [A] | Somente usuário de suporte autenticado poderá **reativar** conta desativada de participante ou organizador. Não permitir auto-reativação. |
| RF-USR-03A | [A] | Reativar conta existente **não cria nova conta**; restaura autenticação e mantém histórico original. |
| RF-USR-01B | [A] | Registros de compras, ingressos, eventos, posts, mensagens e suporte de contas desativadas são preservados. |

**Não decidido [P]:** ciclo de vida de contas de suporte (desativação, edição por terceiros), processo operacional para confirmar desativação e campos exatos da tela de reativação. Não extrapolar a regra de participantes/organizadores para suporte.

### 3.2 Eventos, artistas e ingressos configurados pelo organizador

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-EVT-H01 | [H] | Organizador autenticado cria evento com nome, descrição, data, localidade, gênero musical, imagem, artistas e tipos de ingressos/preços/quantidades. |
| RF-EVT-H02 | [H] | Criar automaticamente uma comunidade associada ao novo evento. |
| RF-EVT-H03 | [H] | Explorar, pesquisar e consultar eventos; listar eventos pertencentes ao organizador, incluindo informações para indicadores. |
| RF-EVT-01 | [A] | Organizador pode **editar apenas seus próprios eventos**. Excluir eventos permanece fora do escopo. |
| RF-EVT-02 | [A] | Informações gerais do evento continuam editáveis após vendas (nome, descrição, data/local, imagem, gênero, artistas). Compras e ingressos emitidos não são alterados retroativamente. |
| RF-EVT-03 | [A] | Tipo de ingresso que **já registrou ao menos uma venda** não pode ter nome/categoria, preço ou quantidade configurada editados manualmente. |
| RF-EVT-04 | [A] | Tipo de ingresso **sem nenhuma venda** pode ter nome, preço e quantidade editados. |
| RF-EVT-05 | [A] | Organizador pode adicionar **novos tipos de ingresso** mesmo quando outras categorias do evento já possuem vendas. |
| RF-EVT-03A | [A] | Bloqueio de edição manual de tipo vendido **não impede a baixa automática de estoque** durante compras. |
| RF-EVT-01A | [A] | Apenas organizador responsável pode realizar a edição; validar propriedade no backend. |

**Observação [P]:** exclusão individual de tipo de ingresso, alteração de quantidade de categoria não vendida abaixo de alguma reserva, edição de evento já iniciado e disponibilidade de determinados campos após o início do evento não foram decididos separadamente. Preservar referência onde houver comportamento e sinalizar lacunas, sem inventar controles.

### 3.3 Compra de ingressos e consultas

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-ING-H01 | [H] | Participante autenticado pode consultar tipos de ingresso, preço e disponibilidade do evento. |
| RF-ING-H02 | [H] | Compra direta valida quantidade e estoque, registra compra e itens, reduz disponibilidade e emite ingressos individuais; a operação deve ser consistente. |
| RF-ING-H03 | [H] | Participante consulta **meus ingressos**, incluindo agrupamento por evento/tipo/status e identificação de ingressos em revenda. |
| RF-ING-H04 | [H] | Fluxo de pagamento original é **simulado/confirmado localmente**, sem gateway externo de pagamento; não criar gateway não aprovado. |
| RF-ING-01 | [A] | Bloquear **compra direta e compra por revenda** quando a data e hora do evento já passaram. |
| RF-USR-02A | [A] | Bloquear compras diretas quando a conta do organizador responsável pelo evento estiver desativada. |
| RF-USR-02B | [A] | Ingressos emitidos antes da desativação de organizador continuam registrados e válidos, e seus históricos permanecem acessíveis conforme autenticação/permissões. |

**Critério transversal [T]:** integridade transacional e controle de concorrência para impedir vendas acima do estoque, reproduzindo a intenção de `SELECT ... FOR UPDATE` e atualização condicionada presente no PHP. A técnica em Java/banco será definida na arquitetura.

### 3.4 Revenda de ingressos

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-REV-H01 | [H] | Participante pode anunciar ingressos válidos de sua titularidade e definir preço de revenda, inclusive mais de uma unidade quando possuir quantidade suficiente. |
| RF-REV-H02 | [H] | Participantes consultam anúncios disponíveis e podem adquirir ingresso anunciado por outro participante, com transferência de titularidade e marcação do anúncio como vendido. |
| RF-REV-H03 | [H] | Proibir aquisição do próprio anúncio; proibir compra de ingresso de evento passado. |
| RF-REV-01 | [A] | Vendedor pode **cancelar anúncio disponível de sua titularidade**; anúncios vendidos não podem ser cancelados. Cancelar não altera o dono do ingresso. |
| RF-REV-02 | [A] | Vendedor pode **reativar/reanunciar** ingresso de anúncio cancelado e alterar seu preço, desde que ainda seja proprietário e o ingresso seja elegível; impedir anúncio disponível duplicado para o mesmo ingresso. |
| RF-USR-04 | [A] | Ao desativar participante, **cancelar automaticamente** seus anúncios de revenda ainda disponíveis; preservar histórico e titularidade dos ingressos não vendidos. |
| RF-USR-04A | [A] | Reativação posterior do participante **não reativa automaticamente** anúncios cancelados; é necessário anunciá-los novamente. |
| RF-USR-05 | [A] | Enquanto organizador do evento estiver desativado, bloquear **compras por revenda** de seus ingressos. |
| RF-REV-03 | [A] | Anúncios já existentes quando organizador for desativado permanecem registrados, com status original, porém **temporariamente indisponíveis para compra**. |
| RF-REV-03A | [A] | Após reativação do organizador, anúncios anteriormente existentes podem voltar a ser comprados se continuarem elegíveis; não reativar anúncio cancelado. |
| RF-REV-04 | [A] | Enquanto organizador estiver desativado, **bloquear criação de novos anúncios** vinculados aos seus eventos. |
| RF-REV-04A | [A] | Dono de anúncio anteriormente criado pode **cancelá-lo mesmo durante desativação do organizador**. |

**[P] Verificação a finalizar na implementação:** tratamento concorrente de compradores simultâneos e consistência entre anúncio, propriedade e transferência. A regra desejada é vender no máximo uma vez; técnica transacional será aprovada na arquitetura.

### 3.5 Comunidades e mensagens

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-COM-H01 | [H] | Comunidade de evento é criada automaticamente; participantes podem descobrir comunidades e organizadores acessar as comunidades de seus próprios eventos. |
| RF-COM-02 | [A] | Posse de ingresso válido **habilita** a entrada na comunidade, mas **não adiciona automaticamente** o participante. A entrada deve ser manual. |
| RF-COM-01 | [A] | Acesso do participante às mensagens e publicações da comunidade exige **associação ativa como membro e ingresso válido**; após sair, conteúdo e interações ficam bloqueados até voltar a entrar. |
| RF-COM-01A | [A] | Participante pode sair e retornar voluntariamente à comunidade, caso ainda possua ingresso válido. |
| RF-COM-H02 | [H] | Membros autorizados podem consultar/enviar mensagens de comunidade, responder mensagens e enviar imagem onde previsto; organizador acessa comunidades de eventos próprios. |
| RF-COM-04 | [A] | Enquanto ingresso estiver apenas **anunciado**, o vendedor mantém seu acesso à comunidade, desde que continue membro e elegível. |
| RF-COM-03 | [A] | Na venda concluída do **último ingresso válido**, remover automaticamente o vendedor da comunidade do evento. |
| RF-COM-03A | [A] | Se vendedor ainda possuir outro ingresso válido do evento, manter associação à comunidade. |
| RF-COM-03B | [A] | Comprador do ingresso transferido **não ingressa automaticamente** na comunidade; entrada manual. |
| RF-COM-03C | [A] | Remoção da associação após revenda não apaga mensagens e publicações históricas. |
| RF-USR-02C | [A] | A desativação do organizador **não apaga comunidades**; membros que satisfazem as regras de acesso continuam utilizando-as. |

**Nota de divergência:** no PHP, alguns endpoints dão acesso por posse de ingresso independentemente de filiação. **A regra [A] prevalece:** participação explícita + ingresso válido.

### 3.6 Feed social, comentários e notificações

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-SOC-H01 | [H] | Participante autorizado pode criar post com texto e/ou imagem em comunidade disponível. |
| RF-SOC-H02 | [H] | Visualizar feed de publicações acessíveis, inclusive informações sobre autor, evento/comunidade e comentários/curtidas. |
| RF-SOC-H03 | [H] | Curtir/descurtir publicação e adicionar/consultar comentários. |
| RF-SOC-H04 | [H] | Compartilhar link da publicação pelo comportamento já implementado no frontend original. |
| RF-SOC-H05 | [H] | Manter áreas e avisos demonstrativos de notificações/mensagens; não pressupor serviço real de push/e-mail. |
| RF-SOC-01 | [A] | **Não acrescentar edição ou exclusão** de publicações; preservar o fluxo original. |
| RF-COM-01B | [A] | A nova regra de associação à comunidade também restringe acesso e interação com suas publicações. |

### 3.7 Suporte e chamados

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-SUP-H01 | [H] | Participante/organizador podem abrir tickets e acompanhar os próprios chamados e conversas. |
| RF-SUP-H02 | [H] | Suporte autenticado pode listar tickets, visualizar detalhes/históricos, responder, associar-se ao ticket e realizar encerramento quando permitido pelo status. |
| RF-SUP-H03 | [H] | Estados existentes: `aberto`, `em_atendimento`, `aguardando_usuario`, `fechado`. |
| RF-SUP-02 | [A] | **Qualquer suporte autenticado** pode responder/encerrar chamados mesmo associados a outro suporte, respeitando o estado do ticket. |
| RF-SUP-02A | [A] | A associação identifica o responsável para acompanhamento; **não restringe** outras contas de suporte. |
| RF-SUP-03A | [A] | Uma resposta do suporte passa o chamado para `aguardando_usuario`. |
| RF-SUP-03 | [A] | Suporte **não pode fechar diretamente** ticket no estado `aguardando_usuario`. |
| RF-SUP-03B | [A] | Solicitante pode informar problema resolvido (ticket `fechado`) ou retornar com nova mensagem (ticket `em_atendimento`) quando aplicável. |
| RF-SUP-01 | [A] | Chamado `fechado` é definitivo: **não aceita novas mensagens nem reabertura**. Novo problema deve gerar novo chamado. |
| RF-SUP-02B | [A] | Preservar histórico e identificação de cada suporte que respondeu. |
| RF-SUP-H04 | [H] | Painel de suporte apresenta consulta, filtros, indicadores, histórico/chat e seções demonstrativas existentes. |

**[P]** Diagrama preciso de todas as transições de status (inclusive chamadas de associação e encerramento em outras condições) deve ser extraído de cada ação PHP e apresentado para homologação técnica; a tabela acima não autoriza novas transições.

### 3.8 Painéis e interface

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| RF-UI-01 | [A] | Criar **novo frontend**, mantendo aparência, identidade visual, organização de telas e navegação **próximas** do JoinUp original. |
| RF-UI-02 | [H] | Manter entrada pública, escolha de cadastro/login por perfil e seus fluxos próprios (com cadastro público de suporte removido conforme RF-AUTH-01). |
| RF-UI-03 | [H] | Painel de participante com feed, explorar eventos, meus ingressos, revenda, comunidades, notificações/mensagens, perfil e chamados. |
| RF-UI-04 | [H] | Painel do organizador com eventos, criação, indicadores, vendas, comunidades, perfil e chamados; acrescentar edição de eventos aprovada. |
| RF-UI-05 | [H] | Painel de suporte com tickets, chat, métricas, relatórios e seções informativas/demonstrativas; acrescentar cadastro de suporte e reativação de contas. |
| RF-UI-06 | [H] | Preservar as funcionalidades originalmente **simuladas** como tais: compra sem pagamento externo, avisos de saque, notificações calculadas localmente e partes informativas. |
| RF-UI-07 | [A] | Restrições de permissão/estado devem ser aplicadas no backend Java e refletidas visualmente no frontend; não depender apenas de botões ocultos. |

**Nota:** reprodução próxima de comportamento e navegação não obriga manter 18 arquivos HTML distintos, URLs idênticas, nem o mesmo framework. A decisão de tecnologia permanece pendente [P].

### 3.9 Banco de dados e carga de demonstração

| ID | Origem | Requisito aprovado / herdado |
| --- | --- | --- |
| BD-01 | [A] | Criar **banco independente**; não migrar registros reais ou de demonstração da instalação PHP original. |
| RF-DAD-02 | [A] | Preservar no novo modelo os conceitos/relacionamentos necessários a todas as funcionalidades. |
| BD-02 | [A] | Disponibilizar **carga automática de demonstração**, com participantes, organizadores, contas de suporte, eventos, gêneros/artistas, tipos de ingressos, compras, ingressos, revendas, comunidades, posts, curtidas, comentários, mensagens e tickets de diversos estados. |
| RF-DAD-04 | [A] | Garantir conta de **suporte inicial** na base de demonstração, já que o cadastro público será removido. |
| RF-DAD-05 | [T] | Fazer carga repetível/idempotente, controlada por perfil de execução ou mecanismo equivalente, para não duplicar dados ao reiniciar. A solução será definida na arquitetura. |

A base original contém, entre outras entidades: `participantes`, `organizadores`, `usuarios_suporte`, `eventos`, `generos_musicais`, `artistas`, `tipos_ingressos`, `compras`, `compra_itens`, `ingressos`, `revenda_anuncios`, `comunidades`, `participante_comunidades`, `comunidade_mensagens`, `postagens`, `comentarios`, `postagem_curtidas`, `line_up` e `ticket`; migrações incluem tabelas auxiliares de mensagens de ticket. **Não fixar esta lista como schema final obrigatório:** estrutura e tipos serão propostos na modelagem Java.

## 4. Matriz de diferenças deliberadas frente ao JoinUp PHP

| Ref. | Diferença funcional aprovada |
| --- | --- |
| CHG-01 | Cadastro de suporte deixa de ser público e passa a exigir autenticação de suporte. |
| CHG-02 | Organizador passa a editar os próprios eventos e categorias ainda sem vendas; ingressos que já venderam ficam bloqueados para edição manual. |
| CHG-03 | Possibilidade de adicionar nova categoria de ingresso em evento que já vendeu outras categorias. |
| CHG-04 | Vendedor pode cancelar e depois reanunciar/reprecificar ingresso ainda elegível. |
| CHG-05 | Tickets fechados não reabrem; qualquer suporte autenticado pode atendê-los respeitando estado. |
| CHG-06 | Participação em comunidade passa a exigir ingresso válido **e** associação ativa; entrada é manual. |
| CHG-07 | Venda do último ingresso válido remove associação do vendedor à comunidade. |
| CHG-08 | Exclusão de conta de participante/organizador passa a ser desativação lógica, com reativação exclusiva pelo suporte. |
| CHG-09 | Desativação de participante cancela automaticamente seus anúncios disponíveis. |
| CHG-10 | Organizador desativado bloqueia compras diretas, revendas e novos anúncios; anúncios preexistentes permanecem registrados e temporariamente indisponíveis. |
| CHG-11 | Compra direta passa a bloquear evento cuja data/hora já passou, como já ocorria na revenda. |
| CHG-12 | Novo frontend mantendo experiência semelhante ao anterior; novo banco com carga demonstrativa automática. |

## 5. Critérios de aceite integrados (cenários mínimos para teste)

| Teste | Cenário | Resultado esperado |
| --- | --- | --- |
| CA-01 | Visitante tenta cadastrar suporte | Rejeição pelo backend. |
| CA-02 | Suporte autenticado cadastra segundo suporte | Cadastro concluído; segundo suporte pode autenticar e cadastrar outro. |
| CA-03 | Organizador cria evento com duas categorias | Evento, categorias, artistas e comunidade criados coerentemente. |
| CA-04 | Organizador altera título de evento com vendas | Alteração aplicada sem modificar compras/ingressos anteriores. |
| CA-05 | Organizador altera preço de categoria com vendas | Operação rejeitada. |
| CA-06 | Organizador altera preço de categoria sem vendas | Operação aceita. |
| CA-07 | Organizador adiciona terceira categoria a evento com vendas nas outras duas | Nova categoria criada sem afetar as anteriores. |
| CA-08 | Dois participantes tentam comprar última unidade simultaneamente | No máximo uma compra confirmada; nenhum estoque negativo. |
| CA-09 | Participante compra ingresso e não entra na comunidade | Não acessa mensagens/posts da comunidade. |
| CA-10 | Participante com ingresso entra e depois sai da comunidade | Acesso liberado ao entrar e bloqueado após sair. |
| CA-11 | Participante membro anuncia seu último ingresso, mas não o vende | Mantém participação e acesso. |
| CA-12 | Anúncio do último ingresso é comprado por outra pessoa | Vendedor perde participação, comprador recebe ingresso e precisa entrar manualmente. |
| CA-13 | Vendedor cancela e reanuncia ingresso | Anúncio anterior não pode ser comprado; novo anúncio elegível pode ter novo preço. |
| CA-14 | Organizador é desativado | Eventos permanecem, mas compra direta/revenda e novos anúncios ficam bloqueados. |
| CA-15 | Existia anúncio quando organizador foi desativado; suporte reativa organizador | Anúncio continua registrado e volta a ser elegível, desde que demais regras estejam válidas. |
| CA-16 | Participante com anúncios é desativado | Anúncios disponíveis são cancelados junto da desativação; não há anúncios disponíveis do usuário. |
| CA-17 | Suporte reativa participante | Conta retorna à autenticação; anúncios cancelados não voltam automaticamente. |
| CA-18 | Participante tenta comprar ingresso após início/data-hora do evento | Rejeição em compra direta e revenda. |
| CA-19 | Suporte B responde a ticket associado ao suporte A | Resposta aceita, autor B identificado no histórico e ticket aguarda usuário. |
| CA-20 | Suporte tenta encerrar ticket `aguardando_usuario` | Rejeição. |
| CA-21 | Usuário encerra ticket e tenta reabrir ou enviar mensagem | Rejeição; novo atendimento requer outro ticket. |
| CA-22 | Aplicação inicia com carga demo em ambiente limpo | Principais fluxos podem ser apresentados com registros demonstrativos. |
| CA-23 | Aplicação reinicia com carga demo já existente | Não multiplica registros demonstrativos, se a proposta idempotente for homologada. |
| CA-24 | Usuário não autorizado chama diretamente operações protegidas | Backend impede execução, independentemente da interface. |

**Os critérios acima são mínimos, não substituem testes completos de cada endpoint, relacionamento ou regra herdada.**

## 6. Aplicação planejada dos quatro padrões GoF — ainda não é aprovação da arquitetura

| GoF | Candidato no domínio | Justificativa a demonstrar | Evidência exigida |
| --- | --- | --- | --- |
| **Facade** | Fluxo de criação/edição de evento; eventualmente operações coordenadas entre módulos | Simplificar operação que depende de cadastro de evento, tipos de ingresso, artistas e comunidade. | Fachada concreta coordenando subsistemas reais, sem abrigar toda a lógica indiscriminadamente. |
| **Template Method** | Autenticação para participante, organizador e suporte | Sequência estável de verificação, com diferenças de obtenção de usuário e contexto/perfil. | Algoritmo-base na classe abstrata; etapas variáveis implementadas por subclasses; funcionamento efetivo. |
| **Factory Method** | Construção de objetos/fluxos especializados por perfil | Encapsular pontos em que o sistema deve criar produtos polimórficos segundo o perfil. | Creator/factory method polimórfico e ConcreteCreators/ConcreteProducts; não confundir com `switch` de `new` em helper. |
| **Singleton** | Configuração compartilhada, se realmente exigir instância única | Compartilhar dados de configuração coerentes de uma única fonte sem criar conexões de banco globais. | Classe com ciclo de vida controlado e justificativa real; considerar inicialização e concorrência em Java. |

**Atenção:** todas são **aplicações candidatas**, ainda a confirmar em planejamento arquitetural. Para cada padrão haverá problema de design explícito, classes participantes, diagrama UML consistente, código funcional e teste demonstrativo. Se algum encaixe ficar artificial, submeter alternativa ao PO antes de implementar.

## 7. Requisitos de qualidade e restrições

- **[A]** Todas as funcionalidades implementadas no backend devem ser executáveis e demonstráveis via novo frontend, salvo recursos originais que sejam somente informativos/simulados.
- **[A]** Não alterar a experiência funcional para acomodar GoF sem autorização expressa.
- **[T]** Garantir autorização server-side; validações essenciais, consistência transacional e histórico de operações relevantes.
- **[T]** Evitar armazenar senha em texto puro e não utilizar credenciais demonstrativas em ambientes públicos. Autenticação, hash e sessão/token dependerão da stack homologada.
- **[T]** Criar testes automatizados para regras críticas e cada padrão; documentar execução reproduzível.
- **[T]** Registrar reativações com operador de suporte e timestamp para auditabilidade, conforme já discutido na validação; modelagem será definida na arquitetura.
- **[T]** Deixar diferenciados recursos visuais demonstrativos e operações persistidas, para não gerar promessa enganosa de pagamento/saque/notificações reais.

## 8. Entregáveis formais da disciplina (fonte: PDF RA2)

1. **Diagrama de classes UML completo e atualizado**, coerente com o código, indicando estruturas/classes/interface dos quatro padrões GoF e relacionamentos relevantes.
2. **Relatório técnico** com equipe/integrantes, domínio, público-alvo, problema, funcionalidades, aplicação de cada padrão (intenção, problema, escolha, participantes, implementação, código, benefícios), análise técnica dos GoF estudados e não aplicados e reflexão de dificuldades/decisões/lições aprendidas.
3. **Código completo e executável**, com instruções suficientes para reproduzir o ambiente, popular dados e rodar o sistema.
4. **Apresentação e arguição**, com domínio técnico de todo o projeto por todos os integrantes.

A especificação **não exige frontend como entregável isolado**, mas o PO o incluiu no escopo. **Não exige** como entregáveis obrigatórios separados os diagramas de pacotes, componentes e implantação, embora possam apoiar a documentação.

**Pesos do RA2:** padrões GoF 4,0; diagrama 1,0; relatório 1,0; código/qualidade 2,0; apresentação/arguição 2,0.

## 9. Pendências antes da arquitetura definitiva

| ID | Pendência | Motivo |
| --- | --- | --- |
| PEN-01 | Stack backend: versão Java, framework ou Java puro, build e execução | Impacta estrutura, prazo, demonstração e testes. |
| PEN-02 | Stack frontend e forma de integração | Deve preservar navegação/aparência sem obrigar duplicar HTML antigo. |
| PEN-03 | Banco independente e estratégia de persistência/migrations | Deve suportar relações e transações. |
| PEN-04 | Autenticação e autorização técnica: sessão, cookie, token, hashing e perfis | Essencial para permissões novas. |
| PEN-05 | Política detalhada de edição de evento já iniciado; exclusão de **categoria não vendida** | Não foi decidida explicitamente. |
| PEN-06 | Semântica de comunidade em caso de ingresso cancelado/utilizado, evento passado e múltiplos ingressos | Evitar conflito com regra de ingresso válido e membro ativo. |
| PEN-07 | Transições exatas de estado do ticket em todas as operações existentes | Gerar matriz precisa antes do código. |
| PEN-08 | Tratamento de exclusão/desativação de conta de suporte | Não definido e distinto de participante/organizador. |
| PEN-09 | Validação de telas originalmente simuladas e eventuais campos/ações sem backend | Garantir paridade sem transformar simulações em integrações reais. |
| PEN-10 | Nomes dos integrantes e identificação da equipe | Necessários para o relatório RA2. |
| PEN-11 | Detalhamento de quais GoF foram efetivamente estudados em sala para seção de padrões não aplicados | Material introdutório enumera 23; aulas específicas detalham Facade e Singleton. |
| PEN-12 | Aprovação das classes e papéis concretos dos quatro padrões GoF | Necessária antes do Codex. |

**Pendência transversal:** mapear endpoints/ações PHP a casos de uso, regras e telas na matriz de rastreabilidade de implementação. O inventário inicial identificou **41 arquivos PHP**, **18 páginas HTML** e **17 arquivos JS**; os números não equivalem à quantidade de funcionalidades.

## 10. Rastreamento inicial dos arquivos de referência

Os 41 arquivos PHP da branch `main` do JoinUp original foram classificados abaixo. **Este é um mapa inicial de fontes**, e não a proposta de estrutura do Java; os endpoints Java não precisam herdar os nomes PHP.

| Área de referência | Arquivos PHP de origem | Vínculo com requisitos |
| --- | --- | --- |
| Cadastro/autenticação | `cadastroOrganizador.php`, `cadastroParticipante.php`, `cadastroSuporte.php`, `loginOrganizador.php`, `loginParticipante.php`, `loginSuporte.php`, `check_auth.php`, `logout.php` | RF-AUTH-H01–H03, RF-AUTH-01/01A |
| Perfil/contas | `editar_perfil.php`, `editar_perfil_organizador.php`, `update_profile_participante.php`, `get_user_data.php`, `get_user_data_organizador.php`, `excluir_conta.php`, `excluir_conta_organizador.php`, `get_suporte_data.php` | RF-AUTH-H03, RF-USR-01/01A/01B/03/03A |
| Eventos/catálogos | `criar-evento.php`, `eventos.php`, `get_explorar_eventos.php`, `get_meus_eventos.php`, `get_artistas.php`, `get_generos.php`, `get_tipos_ingressos_evento.php` | RF-EVT-H01–H03, RF-EVT-01 a 05 |
| Compra/ingressos | `comprar_ingresso.php`, `get_meus_ingressos.php` | RF-ING-H01–H04, RF-ING-01 |
| Revenda | `create_revenda_anuncio.php`, `get_revenda_anuncios.php`, `comprar_ingresso_revenda.php` | RF-REV-H01–H03, RF-REV-01 a 04, RF-USR-04/05 |
| Comunidades/feed | `comunidades.php`, `comunidade_feed.php`, `social.php`, `get_minhas_postagens.php` | RF-COM-H01/H02, RF-COM-01 a 04, RF-SOC-H01–H05/01 |
| Atendimento | `create_ticket.php`, `get_my_support_tickets.php`, `get_support_tickets.php`, `respond_support_ticket.php`, `close_support_ticket.php`, `update_my_support_ticket.php`, `tickets.php`, `ticket_messages_helpers.php` | RF-SUP-H01–H04, RF-SUP-01 a 03 |
| Infraestrutura | `conexao.php` | Persistência e integração dos demais módulos |

**Frontend original:** 18 páginas HTML (`index.html`, `login*.html`, `cadastro*.html`, `feed.html`, `organizador.html`, `suporte.html`, `comunidades.html`, `dashboard*.html`, `editar*.html`) e 17 scripts JavaScript, entre eles `feed.js`, `organizador.js`, `suporte.js`, `comunidades.js` e `criarEvento.js`. Verificar cada tela/ação concreta na matriz de implementação, inclusive variantes de dashboard e formulário que podem estar disponíveis como páginas auxiliares.

**SQL original:** schema `src/sql/joinup_schema_atualizado.sql`, migrations e `seed_feed_social.sql`. A nova carga de demonstração não precisa importar o banco antigo.

## 11. Critérios de início de implementação e trilha de mudanças

1. **Concluído:** PO aprovou a **versão 0.1** para avanço ao planejamento arquitetural.
2. **Concluído:** esta especificação deve ser versionada no repositório `JoinUp-POO`.
3. Engenharia apresenta arquitetura proposta, tecnologias, mapeamento GoF, schema conceitual, contratos e plano de desenvolvimento; PO aprova.
4. Depois da aprovação arquitetural, registrar decisões arquiteturais, matriz de rastreabilidade e tarefas; só então iniciar implementação com Codex, preservando o JoinUp PHP como referência de consulta e controle de regressão.
5. Em toda alteração funcional posterior, registrar identificador, regra anterior, nova regra, justificativa e aprovação do PO.

### Fontes de referência

- [Projeto PHP original e README](https://github.com/ericjs-code/JoinUp)
- [Mapa de responsabilidades do projeto](https://github.com/ericjs-code/JoinUp/blob/main/docs/PROJECT_MAP.md)
- [Modelo de banco do PHP](https://github.com/ericjs-code/JoinUp/blob/main/src/sql/joinup_schema_atualizado.sql)
- [Criação de eventos](https://github.com/ericjs-code/JoinUp/blob/main/src/php/criar-evento.php)
- [Compra direta](https://github.com/ericjs-code/JoinUp/blob/main/src/php/comprar_ingresso.php)
- [Revenda](https://github.com/ericjs-code/JoinUp/blob/main/src/php/create_revenda_anuncio.php) e [compra de revenda](https://github.com/ericjs-code/JoinUp/blob/main/src/php/comprar_ingresso_revenda.php)
- [Comunidades](https://github.com/ericjs-code/JoinUp/blob/main/src/php/comunidades.php), [mensagens de comunidade](https://github.com/ericjs-code/JoinUp/blob/main/src/php/comunidade_feed.php) e [feed social](https://github.com/ericjs-code/JoinUp/blob/main/src/php/social.php)
- [Tickets de suporte](https://github.com/ericjs-code/JoinUp/blob/main/src/php/respond_support_ticket.php), [fechamento](https://github.com/ericjs-code/JoinUp/blob/main/src/php/close_support_ticket.php), [retorno do solicitante](https://github.com/ericjs-code/JoinUp/blob/main/src/php/update_my_support_ticket.php)
- Materiais fornecidos pelo PO: **Especificação do Projeto — RA2**, **Aula01 (UML)**, **Introdução GoF**, **Facade**, **Singleton**, **Diagrama de Pacotes**, **Diagrama de Componentes**, **Diagrama de Implantação**.

---

> **Homologação:** PO autorizou avanço ao planejamento arquitetural. Pendências [P] permanecem abertas e escolhas [T] dependem de aprovação. Este documento **não autoriza implementação nem cristaliza escolhas de stack**.