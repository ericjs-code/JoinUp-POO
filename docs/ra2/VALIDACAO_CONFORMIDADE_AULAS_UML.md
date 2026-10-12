# Entrega 09 — conformidade com as aulas e UML

12/10/2026, America/Sao_Paulo. Responsável pela execução: Codex.
Branch `docs/09-conformidade-aulas-uml`, criada de `main` atualizada no SHA
**`80a36c457ec4e46865ee456f388cb607297c3811`**, após integração da Entrega 08
pelo PR #10. PR documental para **auditoria técnica, sem merge**.

## Revisão solicitada pelo Tech Lead no mesmo PR #11

O [comentário de auditoria](https://github.com/ericjs-code/JoinUp-POO/pull/11#issuecomment-6127161117)
foi lido integralmente. A revisão parte do head auditado
`860492cc5b4eadee07adcb05dd9cd94c2e6cc189`; main/código continuam em `80a36c4`.
O parecer solicitou aproximação didática aos slides, sem defeito funcional
crítico ou alteração de cardinalidades. Este registro descreve as correções
e suas novas evidências; os registros iniciais abaixo conservam seu contexto.

| Pedido da auditoria | Correção / artefato para nova revisão |
| --- | --- |
| Pacotes com conteúdo, pastas e menos cruzamentos | Vista [PUML](DIAGRAMA_PACOTES.puml) / [SVG](DIAGRAMA_PACOTES.svg): **20 classes + 1 interface** dentro de nove pacotes Java reais, dez dependências selecionadas, notação de pasta e duas setas ACOP-01. Sem namespace artificial `http + auth + usuarios` |
| Fonte completa como anexo | [Pacotes — PUML](DIAGRAMA_PACOTES_ANEXO.puml) / [SVG](DIAGRAMA_PACOTES_ANEXO.svg): 14 pacotes reais, **38 classes + 1 interface** representativas e **62 dependências agregadas**, todas confrontadas com jdeps. Consulta com zoom; a vista reduzida é a de apresentação |
| Contrato real com interface fornecida e requerida | Vista [PUML](DIAGRAMA_COMPONENTES.puml) / [SVG](DIAGRAMA_COMPONENTES.svg): duas conexões **bola/encaixe**. SenhasPbkdf2 fornece VerificadorSenha e AutenticacaoTemplate requer; repositórios concretos fornecem UsuarioRepository por herança de UsuarioRepositoryJdbc, e CadastroUsuarioCreator requer |
| Separar contexto HTTP da vista nuclear | [Componentes — anexo PUML](DIAGRAMA_COMPONENTES_ANEXO.puml) / [SVG](DIAGRAMA_COMPONENTES_ANEXO.svg): navegador/frontend/HTTP, seis áreas, JDBC/IO e ACOP-01. Consumo por dependência no contexto; fornecimento/requisição explícitos no recorte nuclear. Sem socket para chamada concreta |
| Dispositivo, ambiente, software e artefato distintos | [Implantação](DIAGRAMA_IMPLANTACAO.svg): computador `<<device>>`, **Windows/JVM** `<<executionEnvironment>>`, navegador/JoinUp/MySQL como programas/serviço, JAR/HTML/CSS/JS/SQL/PNG/dados como artefatos. Um único host; sem caminho absoluto de teste; 3315 somente como configuração da homologação 08 |
| Cardinalidades aceitas com identificação do escopo | Título/legenda: **visão conceitual da persistência**, não espelho dos campos Java. Dez associações, extremos e rótulos preservados integralmente do head auditado. O fragmento não substitui DIAGRAMA_CLASSES_GERAL.svg |

As vinte classes adotam como referência didática a organização mostrada na
atividade de pacotes, **sem tornar esse exercício um requisito obrigatório da
RA2**. O UML geral de classes obrigatório, os quatro GoF e os 19 não adotados
continuam preservados. O anexo de pacotes inclui todos os pares entre os 14
agrupamentos do extrato jdeps; dependências internas continuam no UML geral.

Verificação de elementos: o script confere cada caminho Java/namespace dos
dois diagramas de pacotes, cada seta de pacote contra jdeps e igualdade das dez
cardinalidades com o head auditado. O bytecode original continua aplicável:
`git diff 860492c -- src pom.xml .github` é vazio; `mvn -q test` foi reexecutado
após a revisão. Nenhuma alegação de classes/contratos novos foi acrescentada.

A notação de conexão `-0)-` foi verificada na
[referência oficial de sintaxe PlantUML](https://plantuml.com/deployment-diagram),
e a notação de componentes na [documentação oficial](https://plantuml.com/component-diagram).
A fonte pedagógica é a síntese dos slides no parecer do Tech Lead; não se
afirma ter aberto PDFs indisponíveis. A bola está no fornecedor e o encaixe
no consumidor; as assinaturas exibidas são operações reais dos contratos.

**Evidências da revisão:** [validação de seis fontes, links e modelagem](evidencias/entrega09/revisao-auditoria/validacao-documental.txt),
[inspeção visual](evidencias/entrega09/revisao-auditoria/inspecao-visual.md),
[hashes dos seis pares](evidencias/entrega09/revisao-auditoria/sha256-diagramas.txt),
[Maven local](evidencias/entrega09/revisao-auditoria/maven-test.txt) e
[resumo dos 167 testes](evidencias/entrega09/revisao-auditoria/surefire-resumo.json).
O resultado do CI será registrado no próprio PR vinculado ao novo head/run;
o CI verde anterior não comprova o novo commit. Sem merge: pronto para nova
auditoria somente após os gates de validação e CI, sem antecipar aprovação.

Comando de revisão (preserva os logs iniciais):

```powershell
./docs/ra2/scripts/validar-uml09.ps1 -Java $taskJava -PlantUml $taskPlantuml `
  -EvidenceDir docs/ra2/evidencias/entrega09/revisao-auditoria
# -checkonly/-tsvg em quatro vistas + dois anexos, XML, elementos/arestas,
# cardinalidades, links e escopo protegido; inspeção visual separada.
```

Foram lidos integralmente o [prompt 09](../prompts/CODEX_09_CONFORMIDADE_AULAS_UML.md)
e a [auditoria das aulas](AUDITORIA_CONFORMIDADE_AULAS_12-10-2026.md).
Confrontados enunciado, relatório, roteiro, matriz, diagramas anteriores,
README e código/SQL da base. A síntese dos slides fornecida pelo PO é a fonte
didática; não foram copiados materiais de terceiros nem consultados PDFs
indisponíveis nesta execução.

## Escopo e preservação da homologação

- Quatro novos pares PlantUML/SVG: pacotes, componentes, implantação e domínio
  persistido com cardinalidades. Relatório e roteiro recebem links e articulação
  entre as visões; roteiro mantém prioridade dos quatro GoF.
- README já corrigido pela 08: 00–07 integradas, PRs #8/#9, main, schema →
  migração 06 por administração, aplicação DML, seed pelo runner, loopback e
  uploads privados. Verificado, **sem edição**.
- A homologação 08 comprovou P0 técnico sem defeito funcional bloqueante;
  [relatório e ressalvas](RELATORIO_HOMOLOGACAO_FINAL.md). V26/V44/V45 continuam
  sem execução integral, duração e ensaio coletivo pendentes. A 09 não executa
  esses casos e não altera matriz/evidências/resultados da 08.
- Cinco integrantes, PUCPR, 4A noite e grafia docente informados diretamente pelo
  PO na 08 foram preservados. A síntese de Aula01 identifica Andreza Vieira;
  prevalece aqui a identificação fornecida pelo PO, sem inferir substituições.
- Nenhuma mudança em `src`, SQL, testes Java, frontend, `pom.xml` ou workflow;
  nenhum novo GoF ou regra funcional. Rubrica **4+2+2+1+1** e **23 = 4 + 19**
  preservadas. UML de classes é o diagrama exigido; os outros três são estudo
  complementar. Auditoria/validação 07/08 conservam seu contexto histórico.

## Artefatos por conteúdo das aulas

| Conteúdo | Fonte e render | Alcance |
| --- | --- | --- |
| Pacotes, direção das dependências e baixo acoplamento | [PUML](DIAGRAMA_PACOTES.puml) / [SVG](DIAGRAMA_PACOTES.svg) | Nove pastas reais contendo vinte classes e uma interface; dez setas selecionadas, ACOP-01 explícito e anexo completo |
| Componentes e interfaces fornecidas/requeridas | [PUML](DIAGRAMA_COMPONENTES.puml) / [SVG](DIAGRAMA_COMPONENTES.svg) | Recorte nuclear com dois contratos Java em bola/encaixe; contexto HTTP e demais áreas no anexo |
| Implantação, nós, ambientes, artefatos e protocolos | [PUML](DIAGRAMA_IMPLANTACAO.puml) / [SVG](DIAGRAMA_IMPLANTACAO.svg) | Um dispositivo; Windows/JVM ambientes, navegador/MySQL programas/serviço; JAR/recursos/dados/SQL/PNG artefatos; protocolos loopback |
| Classes e multiplicidades da Aula01 | [PUML](DIAGRAMA_DOMINIO_CARDINALIDADES.puml) / [SVG](DIAGRAMA_DOMINIO_CARDINALIDADES.svg) | Fragmento de persistência, distinto das dependências de tipos do UML geral; tabelas sem classe marcadas `<<table>>` |
| GoF / MVC | [relatório §3 e §7](RELATORIO_TECNICO_RA2.md) / [roteiro](ROTEIRO_APRESENTACAO.md) | Quatro GoF reais e 19 alternativas; camadas/MVC não contam como GoF; bloco UML opcional 45–75 s sem reduzir os dois minutos dos padrões |

## Rastreabilidade inicial das dependências de pacotes (head 860492c)

A tabela desta seção descreve a primeira vista de 25 setas, conservada como
registro do head auditado. A revisão passou a pacotes reais com elementos e
vista reduzida + anexo, como documentado acima; o extrato jdeps do código
inalterado sustenta também todas as novas setas. As capturas/validação inicial
não são apresentadas como prova visual dos renders revisados.

Prefixo dos caminhos Java abaixo: `src/main/java/br/com/joinup/` na base
`80a36c4`. Cada seta desenhada é consumidor → dependência, conferida no fonte
e no [jdeps de pacotes compilados](evidencias/entrega09/jdeps-pacotes.txt).
O jdeps foi executado **depois de `mvn -q test`**, que compila produção.
`--ignore-missing-deps` só permite analisar bytecode sem classpath externo;
o Maven resolveu as bibliotecas na execução. Não representa dependência ausente
no produto. O diagrama seleciona arestas para leitura em uma página; o extrato
preserva todas as relações internas de pacotes, inclusive subpacotes.
As **25 arestas desenhadas** também foram cruzadas automaticamente com pares
do bytecode: [resultado por seta](evidencias/entrega09/rastreabilidade-arestas.txt).

| Aresta(s) do diagrama | Fonte verificável / relação |
| --- | --- |
| bootstrap → entrada, config, shared.db, demo | `bootstrap/ApplicationBootstrap.java`: construtores de Router/UsuarioRoutes, configuração recebida, ConnectionFactory/ExecutorTransacional e DemoRunner; também compõe demais módulos, como informa a nota |
| entrada → shared.db | `auth/AutenticacaoTemplate.java`, `usuarios/service/ContaService.java`: ExecutorTransacional |
| eventos → entrada, shared.db | `eventos/facade/EventoFacade.java`: GerenciadorSessao, Perfil e ExecutorTransacional; `eventos/http/EventoRoutes.java`: transporte/ContaService |
| comércio → entrada, shared.db | `compras/service/CompraService.java`, `revendas/service/RevendaService.java`: sessão/perfis/transações; `compras/http/ComercioRoutes.java`: transporte |
| comunidades → entrada, shared.db | `comunidades/service/ComunidadeSocialService.java`: GerenciadorSessao, Usuario/Perfil, HttpException e ExecutorTransacional |
| arquivos → entrada, shared.db | `arquivos/ArquivoService.java`, `ArquivoRoutes.java`: sessão/usuário, transporte/ContaService e executor; ArquivoService contém SQL direto |
| suporte/painéis → entrada, shared.db | `suporte/service/TicketService.java`, `paineis/service/PainelService.java` e respectivas rotas: sessão/perfil/transações/HTTP |
| comércio → eventos | CompraService/RevendaService: TipoIngressoRepositoryJdbc e ValidacaoEvento; valida categoria/evento sob a mesma conexão |
| arquivos → eventos | ArquivoService: EventoService para editabilidade/propriedade e imagem atual |
| suporte/painéis → comércio | `suporte/repository/TicketRepositoryJdbc.java`, `paineis/repository/PainelRepositoryJdbc.java`: utilitários de JdbcComercio |
| comunidades → arquivos (ACOP-01) | ComunidadeSocialService: campo mutável, configurarArquivos, idReferencia e vincular após INSERT social |
| arquivos → comunidades (ACOP-01) | ArquivoService: campo social e validarAcessoComunidade no upload/leitura |
| shared.db → config, entrada | ConnectionFactory recebe ConfiguracaoSistema; ExecutorTransacional converte SQLException em HttpException |
| config → entrada | `config/ConfiguracaoSistema.java`: validarOrigemPublica chama SegurancaHttp.origin |
| demo → shared.db, config | `demo/DemoRunner.java`: ConnectionFactory, ConfiguracaoSistema e guardas locais |

Não se afirma portas/adaptadores puristas: serviços/infraestrutura conhecem
`HttpException`, configuração conhece `auth`, comércio usa repositório de
eventos e há ciclo social/mídia. As dependências internas `http` → `service/dto`,
`service` → `repository/domain/dto` e `repository` → `domain/dto` variam por
módulo: conferir o extrato, sem impor a mesma camada a todos. Bootstrap é
composition root e sua montagem manual não é, por si só, defeito de acoplamento.

## Contratos de componentes e implantação

| Elemento | Prova e interpretação |
| --- | --- |
| API HTTP lógica | Router e `*Routes.registrar`: contratos em `docs/api`; JSON/HTTP, multipart e PNG. Não é uma interface Java inventada |
| Frontend / navegador | `http/StaticFileHandler.java`, `src/main/resources/public/`: arquivos servidos do classpath e JavaScript executado no navegador |
| Seis módulos do núcleo | ApplicationBootstrap constrói creators/auth/ContaService, EventoFacade, CompraService/RevendaService, ComunidadeSocialService, TicketService/PainelService e ArquivoService; rotas chamam esses colaboradores concretos |
| UsuarioRepository | `usuarios/repository/UsuarioRepository.java`; ParticipanteRepositoryJdbc, OrganizadorRepositoryJdbc e SuporteRepositoryJdbc implementam; creators e autenticações requerem esse contrato |
| VerificadorSenha | `auth/VerificadorSenha.java`: verificar; SenhasPbkdf2 implementa; AutenticacaoTemplate consome o contrato, creators usam SenhasPbkdf2 diretamente |
| JDBC/SQL | ConnectionFactory usa DriverManager; repositórios e ArquivoService usam java.sql.Connection/PreparedStatement; Connector/J 8.4.0 empacotado no JAR pelo pom. Driver provê implementação JDBC e fala MySQL/TCP |
| Dependências do núcleo | Cada área usa ExecutorTransacional e GerenciadorSessao; setas concretas entre social/mídia, mídia/eventos e comércio/eventos são confirmadas nos fontes acima; utilitários/sessão resumidos na legenda |
| Armazenamento privado | `arquivos/ArmazenamentoLocal.java`: java.nio.file.Files, fora do user.dir, sem links simbólicos, ID servidor + .png; ArquivoService autoriza conteúdo via rota |
| JAR e JVM/HttpServer | pom: Java 21/Maven Shade/finalName joinup-poo; ApplicationBootstrap: HttpServer, endereço e contexto `/`; configuração default HTTP 127.0.0.1:8080 |
| Topologia efetiva | [instalação 08](evidencias/entrega08/instalacao.txt), [ambiente 08](evidencias/entrega08/ambiente.txt), relatório 08: mesmo host Windows, MySQL 8.4.0 3315, diretório privado e navegador local |
| SQL administrativo e seed | schema_v0.2.sql → migration_06_midia_demo.sql por administração; DemoRunner exige demo/loopback, base elegível e marcador, e aplica apenas DML do seed; não reseta no reinício |

Componentes são responsabilidades internas do mesmo monólito/JAR, sem
implantação independente. Interfaces nomeadas usam realização/ligação de
fornecimento e dependência de consumo; chamadas concretas não recebem sockets
fictícios. Não há microserviços, fila, gateway, TLS, proxy ou cloud implantados
nessa vista. O CI com instâncias MySQL 3306/3308 é outro ambiente e foi omitido.

## Cardinalidades comprovadas

Fonte SQL: [schema v0.2](../../src/main/resources/db/schema_v0.2.sql), confrontado
integralmente com [migração 06](../../src/main/resources/db/migration_06_midia_demo.sql),
que adiciona arquivos/demo sem mudar as FKs do fragmento. São associações do
domínio persistido: IDs/FKs não são referências entre objetos Java nem prova
de propriedade exclusiva. Não foram acrescentados diamantes.

| Associação / limites desenhados | Fundamento SQL e limite funcional |
| --- | --- |
| Evento 1 ↔ TipoIngresso 0..* | tipos_ingressos.id_evento NOT NULL + FK; nenhum requisito de filho no DDL. TipoIngressoService.cadastrarTipos recusa lista vazia na criação integrada: 1..* naquele fluxo |
| Evento 1 ↔ Ingresso 0..* | ingressos.id_evento NOT NULL + FK; evento pode nunca ter vendido |
| Evento 1 ↔ compras 0..* | compras.id_evento NOT NULL + FK, sem UNIQUE; quantidade por pedido não limita o histórico |
| Evento 1 ↔ Comunidade 0..1 | comunidades.id_evento NOT NULL + FK e uk_comunidade_evento UNIQUE; ausência de filho é permitida pelo SQL. EventoFacade.criarIntegrado cria exatamente uma no mesmo commit |
| TipoIngresso 1 ↔ Ingresso 0..* | ingressos.id_tipo_ingresso NOT NULL + FK; categoria pode não ter emissão; TipoIngresso não projeta eventoId no record |
| Participante 1 ↔ Ingresso 0..* | ingressos.id_participante NOT NULL + FK: titular atual, transferido por RevendaService; um participante pode não possuir ingressos |
| Participante 1 ↔ compras 0..* | compras.id_participante NOT NULL + FK: comprador original, preservado na revenda |
| compras 1 ↔ Ingresso 0..* (SQL) | ingressos.id_compra NOT NULL + FK, sem UNIQUE. SQL não exige filhos; CompraService.comprar valida itens não vazios, quantidade >0 e emite um por unidade em transação: 1..* no sucesso desse fluxo |
| Participante 1 ↔ filiação 0..* | participante_comunidades.id_participante NOT NULL + FK; nenhum ingresso/cadastro cria filiação automaticamente |
| Comunidade 1 ↔ filiação 0..* | participante_comunidades.id_comunidade NOT NULL + FK; PK composta limita uma linha por par e guarda ativo/data_saida |

`Evento`, `TipoIngresso`, `Comunidade`, `Ingresso` e `Participante` são tipos
Java reais com projeções diferentes das tabelas. **Não existem classes Java
Compra ou ParticipanteComunidade**: tabelas estão marcadas `<<table>>`.
`CompraDados`/Item são DTOs de entrada e histórico usa Map; Ingresso não tem
campo compraId. FKs não garantem sozinhas que ingresso, categoria e compra
referenciem o mesmo evento: CompraService valida categoria no evento e emite
com IDs coerentes. O fragmento não promete essa constraint adicional no SQL.

ComunidadeSocialService exige filiação/conta ativas e ingresso ativo/utilizado
inclusive pós-evento. RevendaService transfere o mesmo ingresso e chama
IngressoRepositoryJdbc.sairSemIngressoElegivel; último elegível inativa vínculo
sem apagar histórico, outro elegível conserva acesso. O comprador entra
manualmente. CASCADE da comunidade/filiação é ação referencial de banco, não
composição Java ou endpoint de exclusão de evento aprovado.

## Execução inicial PlantUML, conferência e regressão (head 860492c)

Ferramentas existentes no host: Temurin **21.0.12.1+1**, Maven **3.9.9**,
PlantUML **1.2025.2** e Graphviz **2.44.1**. Não se adicionou dependência ao pom.
Comandos efetivamente executados:

```powershell
git fetch origin
git switch main
git pull --ff-only origin main
git switch -c docs/09-conformidade-aulas-uml
$env:JAVA_HOME = "$env:LOCALAPPDATA/JoinUp-POO/tools/jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME/bin;$env:LOCALAPPDATA/JoinUp-POO/tools/apache-maven-3.9.9/bin;$env:PATH"
$env:JOINUP_DEMO = 'false'
mvn -q test
$taskJava = "$env:JAVA_HOME/bin/java.exe"
$taskPlantuml = "$env:LOCALAPPDATA/JoinUp-POO/tools/plantuml-1.2025.2.jar"
& $taskJava -jar $taskPlantuml -version
& $taskJava -jar $taskPlantuml -testdot
./docs/ra2/scripts/validar-uml09.ps1 -Java $taskJava -PlantUml $taskPlantuml
# O script roda -charset UTF-8 -checkonly e -tsvg nas quatro fontes,
# verifica XML/marcadores de erro, links e diff protegido.
& "$env:JAVA_HOME/bin/jdeps.exe" -verbose:package -filter:none --ignore-missing-deps target/classes
git diff --check
git diff --name-only 80a36c457ec4e46865ee456f388cb607297c3811 -- src pom.xml .github README.md docs/ra2/MATRIZ_VALIDACAO_FINAL.md docs/ra2/evidencias/entrega08
```

| Prova | Resultado / evidência |
| --- | --- |
| PlantUML + Graphviz | [versão](evidencias/entrega09/plantuml-versao.txt), [testdot](evidencias/entrega09/graphviz.txt): instalação e geração OK |
| Sintaxe e render | -checkonly e -tsvg: exit 0 nos quatro novos fontes; [registro consolidado](evidencias/entrega09/validacao-documental.txt). Logs de sucesso vazios são esperados; não usados como única prova |
| XML e conteúdo SVG | Quatro SVGs válidos, dimensões positivas e ausência de tela de erro; fonte e render preservados juntos |
| Integridade dos diagramas | [SHA-256 dos quatro pares fonte/render](evidencias/entrega09/sha256-diagramas.txt), para conferir os artefatos inspecionados |
| Inspeção visual | Abrir cada SVG renderizado/rasterizado, conferir margens, setas, notas, multiplicidades e texto; resultado final no [registro visual](evidencias/entrega09/inspecao-visual.md) |
| Regressão local | `mvn -q test`, exit 0, **167 Surefire**, zero falhas/erros/ignorados; [log](evidencias/entrega09/maven-test.txt) e [resumo sanitizado dos XML](evidencias/entrega09/surefire-resumo.json) |
| Links/escopo | Script verifica referências locais README/relatório/roteiro/registro 09 e diff protegido; `git diff --check` sem erro. Arquivos e sentido das setas/cardinalidades confrontados com os fontes |

Esta execução local **não** rodou Failsafe/MySQL nem ensaio acadêmico.
A suíte integral é `mvn -Pmysql-it clean verify` no CI isolado; a base da
apresentação não recebeu testes destrutivos. Resultado do CI do novo head
deve ser vinculado a seu próprio SHA/run no PR; CI histórico da 08 não é
atribuído à 09.

## Achados e decisão para revisão

**ACOP-01 permanece reconhecido**: mídia exige validação social; social exige
vínculo pela mídia. Montagem social → mídia → configurarArquivos impõe ordem,
campo mutável e acoplamento de teste. Não foi comprovado defeito operacional
bloqueante. Extrair contrato mínimo ou deslocar coordenação para caso de uso
é proposta de entrega posterior, preservando permissões, locks e atomicidade.
Refatorar está fora deste PR e ampliaria risco sobre a demo homologada.

O integral continua denso e exige zoom; as novas vistas são seletivas e têm
legendas de escopo. A inspeção verifica legibilidade em viewport/zoom, sem
certificar leitura de projeção em sala ou acessibilidade assistiva. A equipe
deve ensaiar no equipamento real e confirmar a duração. A 09 não fecha
V26/V44/V45 nem transforma GO com ressalvas em aceite acadêmico integral.

Entregar o PR para auditoria Tech Lead/PO; **não fazer merge antes da auditoria
técnica**. Fontes/artefatos/resultados novos são documentais; regras e aplicação
continuam preservadas.
