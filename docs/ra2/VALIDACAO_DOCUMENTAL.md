# Frente 07 — Validação documental e sincronização

12/10/2026, America/Sao_Paulo. Branch `docs/07-ra2-relatorio-uml`, criada de `origin/main` no SHA `7efe29c1529a71e3c1a82a52716a5a32947a61fe`. Checkout isolado; a branch no diretório original da Entrega 06 não foi trocada. PR exclusivamente documental para revisão **sem merge**.

## Entregáveis e conferência

| Documento | Conteúdo / limite |
| --- | --- |
| `RELATORIO_TECNICO_RA2.md` | Objetivos/atores, arquitetura, quatro GoF com trechos literais, testes, tradeoffs/alternativas, banco, segurança, evidências por commit e limitações |
| `DIAGRAMA_CLASSES_GERAL.puml/.svg` | 123 tipos nomeados em 117 fontes, 42 pacotes Java; inclui seis tipos internos, campos, assinaturas explícitas e relações reais |
| `VISAO_ARQUITETURA.puml/.svg` | Resumo de apresentação, não substitui integral |
| `INVENTARIO_CLASSES.md` e JSON de evidência | Tipo, fonte/linha, campos/assinaturas da base para inspeção e atualização |
| `MATRIZ_VALIDACAO_FINAL.md` | 45 casos por requisito, papel, ambiente, pré-condições, URL, esperado e evidência; **todos NÃO EXECUTADO nesta frente** |
| `ROTEIRO_APRESENTACAO.md` | 13 minutos ajustáveis para 10–15, preparação, contas só demo, perguntas de arguição e gate pós-06 |
| `evidencias/CI_MAIN_7efe29c.txt`, `TESTES_LOCAIS_7efe29c.txt` | CI e execução local realmente conferidos, separados de E2E planejado |

O diff autorizado contém apenas `docs/ra2/**` e três pares PlantUML/SVG anteriores corrigidos. README, Java, SQL, testes, pom, workflow e interface não foram alterados.

## Método e alcance do UML

O prompt 07 foi lido diretamente na main após fetch; foram confrontados especificação e **todos os sete adendos**, ADR/proposta/classes GoF, diagramas GoF/eventos/suporte, contratos HTTP, auditorias 00/01/02, cruzada PR4+PR5, correção 04.1, auditoria 05, relatórios/evidências e prompt/resultado 06. Propostas iniciais não se tornam prova de implementação. PHP é somente referência de requisitos/UX herdados.

A inspeção percorreu composição, transporte, configuração, creators/produtos, autenticação, Facade/subsistemas, repositórios, comércio/social, suporte/painéis, DTOs, SQL/seed, mídia/demo e testes. As declarações de **todos** os arquivos `src/main/java` foram extraídas pela API do compilador JDK 21 (`JavacTask.parse`, `ClassTree`, `MethodTree`, `VariableTree`, posições de fonte), usando auxiliar temporário fora do checkout, sem adicionar código Java ao produto. O JSON de saída preserva as declarações e localização para auditoria; não se usa regex para interpretar declarações Java.

Após compilação efetiva por Maven, dependências de bytecode foram conferidas com:

```powershell
jdeps -verbose:class -filter:none --ignore-missing-deps target/classes
```

`ignore-missing-deps` omite dependências externas indisponíveis na inspeção do bytecode; o build Maven com dependências passou. Arestas de corpo e de campos/assinaturas são combinadas: **529 pares de tipos relacionados** na extração; um par pode mostrar dois papéis (gênero/artistas). Bibliotecas/JDK externos não são novas classes do produto no diagrama.

O integral inclui seis tipos internos nomeados: `ArquivoService.ArquivoView`, `SessaoRepositoryJdbc.Titular`, `CompraDados.Item`, `ConfiguracaoSistema.Holder`, `Router.Rota`, `ExecutorTransacional.Operacao`. Classes anônimas/lambdas e métodos sintéticos/accessors gerados dos records não são entidades adicionais. Métodos privados são omitidos do desenho, exceto o construtor privado do Singleton; o inventário guarda as declarações privadas explícitas.

Relação de campo não implica propriedade exclusiva: não há diamantes inventados. Herança/interfaces seguem extends/implements. Uso de tipo é dependência. IDs SQL permanecem atributos, sem grafo ORM. `EventoView.de` produz gênero não nulo; listas podem ser vazias (`0..*`); comunidade é referência nullable. Validação mínima na criação não prova multiplicidade mínima em toda consulta.

## PlantUML e inspeção visual

Ferramentas locais disponíveis: Temurin **21.0.12.1+1**, PlantUML **1.2025.2**, Graphviz **2.44.1** detectado pelo PlantUML. Sem instalação de plugin ou dependência do produto. Comandos executados no checkout documental:

```powershell
$java = "$env:LOCALAPPDATA/JoinUp-POO/tools/jdk-21.0.12.1+1/bin/java.exe"
$plantuml = "$env:LOCALAPPDATA/JoinUp-POO/tools/plantuml-1.2025.2.jar"
$diagramas = @(
  'docs/ra2/DIAGRAMA_CLASSES_GERAL.puml',
  'docs/ra2/VISAO_ARQUITETURA.puml',
  'docs/diagramas/CLASSES_GOF_v0.1.puml',
  'docs/diagramas/EVENTOS_DOMINIO_02.puml',
  'docs/diagramas/SUPORTE_PAINEIS_05.puml'
)
& $java -jar $plantuml -testdot
& $java -DPLANTUML_LIMIT_SIZE=50000 -Xmx2g -jar $plantuml -charset UTF-8 -checkonly @diagramas
& $java -DPLANTUML_LIMIT_SIZE=50000 -Xmx2g -jar $plantuml -charset UTF-8 -tsvg @diagramas
```

`-testdot`: instalação/geração OK. `-checkonly`: retorno 0, sem erro de sintaxe. `-tsvg` Graphviz: retorno 0, SVGs de classes/XML conferidos, sem imagem de erro. O integral exige zoom por seu tamanho; a visão resumida dá leitura inicial. A visão resumida e regiões do integral foram inspecionadas por rasterização temporária; PNG de preview não integra os entregáveis.

Uma tentativa inicial com Smetana lançou `UnsupportedOperationException` no integral **apesar de exit code 0**. O SVG de erro foi identificado e substituído por Graphviz. A validação verifica o conteúdo, não só o exit code.

## Correções dos diagramas antigos

| Fonte | Divergência | Correção documental |
| --- | --- | --- |
| `docs/diagramas/CLASSES_GOF_v0.1.puml` | id Long/campos públicos de Usuario; bootstrap indicado como obtentor do Singleton | id `long`, campos privados/accessors reais; `Main` obtém, bootstrap recebe configuração; hook tipado e fábrica abstrata |
| `docs/diagramas/EVENTOS_DOMINIO_02.puml` | Diamantes implicavam ciclo de vida não comprovado; coleções `1..*` extrapolavam criação | Associações, listas `0..*`, gênero da view produzida `1`, comunidade `0..1` |
| `docs/diagramas/SUPORTE_PAINEIS_05.puml` | Composição em records/projeções | Associações de referência/List; estados/assinaturas preservados |

SVGs regenerados com os fontes. Documentos arquiteturais v0.1 continuam como propostas históricas; o relatório RA2 descreve o estado efetivo.

## Testes, CI e verificação documental

| Evidência | Resultado | Limite |
| --- | --- | --- |
| Main `7efe29c1529a71e3c1a82a52716a5a32947a61fe`, [run 38247779976](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38247779976), job 114805929131 | completed/success; **468** = 167 Surefire + 301 Failsafe, zero falhas/erros/ignorados; Java 21 + dois mysql:8.4.0; logs consultados | CI do commit de origem, não ensaio manual |
| `mvn --batch-mode --no-transfer-progress test`, Java 21/Maven 3.9.9/Windows, JOINUP_DEMO=false | **167**, zero falhas/erros/ignorados, BUILD SUCCESS, 12/10/2026 09:17:15 -03:00 | Execução local desta frente, sem MySQL/Failsafe |
| Integridade documental | Tipos/assinaturas, links locais, trechos literais, nomes de métodos de testes, contagens SQL estáticas e escopo do diff conferidos | Não substitui revisão acadêmica, carga, E2E/mobile/assistiva |

O log proposital de falha HTTP em `HttpFoundationTest` não é falha da suíte: os testes terminaram com zero erros. Falhas intermediárias de transporte 413 da 06 permanecem identificadas no relatório histórico; evidencia-se o CI final.

## Achados e pendências

| ID | Achado | Encaminhamento |
| --- | --- | --- |
| DOC-07-01 | Enunciado integral RA2 não encontrado; equipe/turma/professor/duração faltam | Confirmar externamente; relatório não garante todos os critérios |
| DOC-07-02 | Prompt pressupõe frente paralela pré-06, mas main já inclui #8 | Consolidação documental **pós-06** feita na base; ensaio e revisão externos pendentes |
| DOC-07-03 | README/resultado 06 situam #8 aberto quando foram escritos | Registrar contexto histórico aqui, sem editar README. Merge não comprova parecer externo |
| DOC-07-04 | Contratos antigos citam mídia pendente e upload de perfil | Estado vigente é código/contrato 06 de evento/post/chat; avatar não implementado |
| DOC-07-05 | Ressalvas COM-01/03, R-02-01, R-05-01/02/03 continuam | Preservar idempotência, N+1/OFFSET, auditoria limitada, escala/mobile/TLS no relatório |
| DOC-07-06 | Matriz e roteiro sem execução manual nova | Executar com evidência individual; nunca converter histórico em homologação atual |

Nenhum novo defeito funcional foi comprovado que exigisse mudança de implementação nesta frente. Achados documentais e riscos residuais ficam registrados para revisão; não foi aberta issue duplicada de ressalvas já rastreadas.

## Procedimento de sincronização após novos PRs

1. Conferir SHA/main e parecer externo da 06/sucessores. Registrar revisão no relatório, roteiro, matriz e inventário. Este procedimento não autoriza merge.
2. Comparar fontes/testes/pom/contratos/SQL/UI read-only. Atualizar somente documentação; não editar Java/SQL/UI/README nesta branch.
3. Conferir tipos/assinaturas e dependências em fontes compiladas; atualizar integral/resumo/fragmentos; regenerar SVG e executar checkonly/conferência de conteúdo.
4. Confirmar CI do novo SHA e totais em logs. Não extrapolar 468 para código diferente; distinguir unitário, integração, sanidade manual e E2E.
5. Conferir instância/contas/datas/IDs/imagens do ensaio. Reinício não reaplica seed; não resetar/apagar banco como atalho.
6. Executar os 45 casos e ensaio final, registrar resultado/evidência por caso e confirmar enunciado/identificação/duração. Submeter PR à revisão técnica/acadêmica **sem merge automático**.
