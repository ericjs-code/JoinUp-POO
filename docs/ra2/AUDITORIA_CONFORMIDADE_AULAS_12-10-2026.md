# JoinUp-POO — Auditoria profunda de conformidade com as aulas de Design de Software

**Data da inspeção:** 12/10/2026 · **Baseline:** `main` em `01952f313554be047ce515c8171311028ce9e642`.  
**Natureza:** auditoria estática acadêmica e arquitetural + evidência do GitHub Actions, **não** homologação funcional/visual independente.  
**Parecer geral:** **ALINHADO AO NÚCLEO DA RA2 COM RESSALVAS DE DOCUMENTAÇÃO/VALIDAÇÃO**. Quatro GoF reais, relatório e UML existentes. Não equiparar a presença de conteúdos nas aulas com obrigatoriedade de entregar um diagrama de cada tipo.

## 1. Fontes primárias efetivamente recebidas e escopo

A análise confrontou **oito PDFs fornecidos pelo PO** com o código e a documentação atuais:
1. `Aula01(1).pdf` — 22 páginas: aula 1 de Design de Software 2026, UML de classes, visibilidades, multiplicidades, associações, agregação, composição, herança, classes associativas.
2. `Padrões de Projetos_GoF_Introdução.pptx (1)(2).pdf` — 22 páginas: origem, 23 GoF (slides 12–19), MVC arquitetural (slides 7–10), problema/solução/consequências.
3. `Diagrama de Implantação(1).pdf` — 15 páginas: topologia física, nós, ambientes, artefatos, protocolos e vínculos.
4. `Diagrama de Pacotes(1).pdf` — 21 páginas: agrupamento, dependências, alta coesão e baixo acoplamento, diagramas de pacotes em classes/componentes.
5. `Aula02_Diagrama de Componentes (1)(1).pdf` — 34 páginas: módulos substituíveis, interfaces requeridas/fornecidas, dependências, estereótipos, artefatos e relações.
6. `Facade(1).pdf` — 22 páginas: interface simplificada para subsistema, cliente, coordenação de vários colaboradores, não bloqueia acesso direto, reduz acoplamento.
7. `02 Singleton.pptx(1).pdf` — 7 páginas: única instância, construtor restrito, acesso global, uso Java e teste.
8. `Especificação do Projeto - RA2.docx(3).pdf` — 3 páginas: rubrica oficial, **mínimo quatro GoF**, UML **de classes** atualizado, relatório (inclusive 19 GoF não adotados), código executável e arguição coletiva.

Os PDF da professora são as fontes de conteúdo. Código e documentação estão no repositório `ericjs-code/JoinUp-POO`, branch `main`. **Não se inferiu enunciado de uma atividade formativa como requisito extra da RA2**. Atividades individuais/grupais das aulas de implantação/pacotes/componentes são exercícios distintos.

### Evidência de build

[GitHub Actions da main correspondente ao SHA auditado — run 38255255182](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38255255182): `completed/success`. Workflow usa Java 21, dois MySQL 8.4.0 e `mvn -Pmysql-it clean verify`. Histórico da Entrega 06 informa **468 testes** = 167 Surefire + 301 Failsafe, todos aprovados. **O auditor não rodou Java, MySQL, PlantUML ou navegador nessa revisão**; o resultado operacional advém dos workflows e relatórios do time. `/api/health` é liveness HTTP, não valida banco nem casos funcionais.

## 2. Matriz de aderência por aula

| Aula / referência | Expectativa conceitual | Evidência no JoinUp | Parecer |
| --- | --- | --- | --- |
| Aula 01 — classes (slides 7–19) | Atributos, métodos, visibilidade, relacionamentos, multiplicidade, herança e representação coerente | `docs/ra2/DIAGRAMA_CLASSES_GERAL.puml/.svg`: 123 tipos nomeados, 42 agrupamentos, ~515 relações; especializações e interfaces; fragmento GoF separado | **Aderente; melhorar legibilidade e multiplicidades seletivas** |
| GoF introdução (slides 3–5, 12–19) | Contexto, problema, solução, consequências; classificação 23 | Quatro padrões reais; relatório §3.1–3.5 analisa os 19 outros por cenário e não adoção | **Aderente em conteúdo**; qualidade da justificativa ainda sujeita à professora |
| GoF — MVC (slides 7–10) | Separação model/view/controller no desenho arquitetural | HTML/CSS/JS como interface, Routes para HTTP, serviços/domínio e repositórios JDBC | **Aderência conceitual parcial**; não é MVC formal de framework nem um quinto GoF |
| Facade (slides 2–21) | Interface de alto nível coordena subsistemas sem impedir acesso direto | `EventoFacade` coordena EventoService, ArtistaService, TipoIngressoService, ComunidadeService, usando mesma transação/Connection | **Aderência forte e comprovável** |
| Singleton (slides 2–6) | Um objeto com acesso global/restrição de instância; instanciar/testar | `ConfiguracaoSistema` final, construtor privado, `Holder` estático, `getInstancia`, campos `final`, testes `ConfiguracaoSistemaTest` | **Aderência forte**; variante Holder é mais segura sob concorrência que lazy-init simples ilustrado nos slides |
| Pacotes (slides 2, 14–19) | Organização coesa e direção de dependências, baixo acoplamento | Pacotes reais separados por domínio/camada, 42 pacotes no UML geral, diagramas de classes agrupados; dependência **bidirecional** ArquivoService ↔ ComunidadeSocialService | **Aderente estruturalmente; acoplamento circular concreto + sem diagrama exclusivo de pacotes** |
| Componentes (slides 4, 10, 12–16, 20–30) | Visão de módulos/contratos oferecidos/requeridos, interfaces e dependências | HttpServer/Routes, serviços, repositórios, Driver JDBC, módulos auth/eventos/comércio/social/suporte/mídia, JAR Maven Shade | **Implementação modular coerente**, **sem diagrama UML exclusivo de componentes** |
| Implantação (slides 4–13) | Nós, ambientes, protocolos, artefatos físicos e ligações | README: navegador local HTTP→JDK HttpServer→JDBC MySQL 8.4; pasta privada de mídia; JAR e SQL; CI com dois MySQL services | **Arquitetura inferível e executável, mas sem diagrama de implantação documentado** |
| RA2 (3 páginas) | 4+ GoF reais, UML de classes, relatório, executável e arguição | GoF e UML/comandos/documentos presentes; 468 testes reportados, 45 casos finais **NÃO EXECUTADO**; identificação/equipe e ensaio faltam | **Cumprimento material substancial; homologação/identificação pendente** |

**Distinção fundamental:** aulas abordam quatro diagramas UML (classes, pacotes, componentes e implantação), mas **o edital RA2 pede explicitamente apenas o diagrama de classes**. Adicionar os outros três é **enriquecimento acadêmico recomendado**, não bloqueio regulamentar demonstrado. Os slides de atividades formativas não transformam os diagramas em entregáveis da RA2 automaticamente.

## 3. GoF — quatro implementações reais

### 3.1 Singleton — `ConfiguracaoSistema`
- Classe `final`, construtor **privado**, apenas `Holder.INSTANCIA` construindo objeto, `getInstancia()` como ponto de acesso global e campos imutáveis. `Main` obtém instância; `ApplicationBootstrap` recebe dependência. Isso é um Singleton GoF **verdadeiro**, não campo `static` decorativo.
- Escolha pedagógica mais robusta que o `if (instancia==null)` simples presente no slide 3 do PDF Singleton, que sozinho não é thread-safe em concorrência.
- Acerto de desenho: **não armazenar `Connection` compartilhada no Singleton**, pois violaria isolamento e integridade transacional. `ConnectionFactory` cria conexões por operação.
- Tradeoff: dependência global dificulta substituir configuração de teste; o construtor do bootstrap permite injeção de dependência, mitigando o problema.

### 3.2 Factory Method — `CadastroUsuarioCreator`
- Método template concreto/final `cadastrar` invoca método-fábrica abstrato `criarUsuario`, implementado em três ConcreteCreators de participante, organizador e suporte; retorna subclasses de `Usuario`.
- Padrão efetivo, sem `switch` para instanciar produto. Serviços/repositórios variam conforme cadastro e segurança mantém regra própria de suporte.
- Conceito aparece na classificação GoF introdutória, não em um PDF específico de Factory Method dentre os anexados; a análise técnica não pode afirmar que o professor ministrou outro exemplo nessa remessa.

### 3.3 Template Method — `AutenticacaoTemplate`
- `autenticar` é final e realiza ordem invariável normalização → consulta de usuário por perfil (hook abstrato) → verificação PBKDF2/status ativo → validação de perfil (hook) → sessão. Três subclasses especializam a busca/regra.
- Aplicação real. Tradeoff: os hooks de regra especializada atualmente validam sobretudo o tipo do perfil; a variabilidade de autenticação é pequena. Defender a centralização da sequência, não sugerir MFA ou políticas de senha específicas inexistentes.
- Padrão aparece na classificação introdutória; não há aula de Template Method separada nos oito PDFs recebidos.

### 3.4 Facade — `EventoFacade`
- API de alto nível para criar/editar/consultar eventos e categorias; composição de quatro colaboradores reais e transação JDBC única de criação/edição integrada; falha gera rollback.
- Bate exatamente com slides de Façade: reduz a sequência de chamadas que o cliente precisa conhecer, preserva subsistemas especializados. **Facade não é mecanismo de segurança nem impede acesso direto ao subsistema**, conforme slides 11 e 16; segurança continua em backend.
- Tradeoffs: `listar` solicita lineup/tipos/comunidade por evento (consulta N+1), não impede usar serviços diretamente. Nenhum problema invalida o padrão acadêmico.

### 3.5 Alternativas GoF e MVC
- O relatório §3.5 cobre 23/23 estudados: quatro implementados e 19 contextualizados. Justificativas apropriadas incluem Strategy (pagamento somente simulado), Observer (notificação externa não implementada), State (transições de ticket tratadas por `StatusTicket`) e Memento (snapshot histórico ≠ rollback/restauração arbitrária de objeto).
- Quatro GoF = **mínimo da rubrica**, não promessa de todos os 4,0 pontos. Um quinto só seria útil se um problema real justificasse classes/comportamento adicional, com testes e UML sincronizados. Não forçar agora.
- MVC foi ensinado no PDF introdutório, mas está fora da lista dos 23; descrever a **separação inspirada em MVC** sem declarar Spring MVC/JSP/servlet ou contar como quinto GoF.

## 4. UML de classes — pontos fortes e gaps específicos

- `docs/ra2/DIAGRAMA_CLASSES_GERAL.puml`: **1.660 linhas**, 123 tipos nomeados, 42 agrupamentos de packages, representação de atributos/métodos, extensão de classes e implementação de interfaces, relações de dependência/associação. Fontes foram geradas/confrontadas via `JavacTask` e `jdeps` no PR #9, segundo `VALIDACAO_DOCUMENTAL.md`, e `-checkonly`/SVG também foram registrados naquele PR.
- UML específico de GoF: `docs/diagramas/CLASSES_GOF_v0.1.puml`. UML resumido: `docs/ra2/VISAO_ARQUITETURA.puml`. Esses complementos favorecem ensino/arguição.
- **Somente 5 conexões estão explicitamente rotuladas com multiplicidades** no UML integral; são principalmente listas em DTOs, não um mapeamento completo de todas as associações de domínio. A aula 1 destaca cardinalidades `0..1`, `1..1`, `0..*`, `1..*` e relações fortes/fracas. Não inventar multiplicidade por mera presença de FK ou construtor. **Melhoria recomendada:** acrescentar cardinalidades verificadas em um fragmento de domínio de eventos/ingressos/comunidade/ticket.
- Não adicionar composição/agregação com diamantes para todo uso de objeto; **as aulas distinguem semântica de ciclo de vida**. O PR #9 removeu diamantes indevidos dos diagramas antigos, o que é acerto técnico.
- O integral é muito denso (~515 relações). Para a apresentação, abrir **visão resumida + fragmentos dos 4 GoF**, mantendo o integral como entregável de consulta/alta resolução. Não depender de leitura projetada de todo SVG numa única tela.
- Última origem efetiva do inventário e diagrama: código do PR #8, SHA `7efe29c...`; commits posteriores até esta auditoria só alteraram documentação, então não exigem regeneração por mudança de classe.

## 5. Pacotes e acoplamento — achado real

A aula de pacotes orienta alta coesão/baixo acoplamento e dependências tracejadas. O projeto concretiza pacotes para `auth`, `usuarios`, `eventos`, `compras`, `revendas`, `comunidades`, `suporte`, `paineis`, `arquivos`, `demo`, `http`, `config`, `shared.db` e subdivisões. `ApplicationBootstrap` atua como **composition root**, portanto conhecer todos os módulos é esperado e não prova, por si só, God Object.

**ACOP-01 — Média, desvio conceitual de baixo acoplamento (comprovado):**
- `ArquivoService` importa e conserva `ComunidadeSocialService` para validar acesso comunitário.
- `ComunidadeSocialService` mantém referência mutável a `ArquivoService` e delega a `arquivos.vincular(...)` em mensagens/posts.
- `ApplicationBootstrap` primeiro constrói o social, depois mídia, e chama `comunidadeSocialService.configurarArquivos(arquivos)`. Há **dependência bidirecional em nível de serviços**, além de dependência de inicialização em duas etapas.
- Os testes atuais passaram; **não foi identificado defeito operacional** provocado pelo ciclo. Isso enfraquece o desenho modular e facilita futuras regressões. **Após a RA2**, extrair contrato mínimo de validação/vinculação, deslocar coordenação para um serviço de caso de uso ou reorganizar fronteiras para eliminar a dupla dependência. **Não fazer refatoração grande antes da entrega sem necessidade comprovada**.

O UML geral já expõe essas duas dependências; diagrama de pacotes resumido poderia destacá-las para mostrar que a equipe reconhece tradeoffs, sem esconder complexidade.

## 6. Componentes — conteúdo de aula ainda sem artefato dedicado

A aula de componentes pede identificar **módulos**, **interfaces fornecidas e requeridas**, **dependências** e artefatos (executable/library/file). O produto tem esses elementos concretos:
- interface de usuário estática HTML/CSS/JS,
- camada HTTP nativa (`Router`, `*Routes`),
- núcleo de aplicação (`EventoFacade`, services de usuários, comércio, comunidades, suporte, mídia),
- repositórios JDBC e `ExecutorTransacional`,
- MySQL 8.4, pasta de armazenamento privada, JAR Shade Maven,
- interfaces Java efetivas como `UsuarioRepository` e `VerificadorSenha`.

**DOC-COMP-01 — P1, documentação opcional:** nenhuma fonte `*COMPONENTE*.puml/.svg` no repositório. A visão `VISAO_ARQUITETURA.puml` utiliza **notação de classes dentro de pacotes**, e não representa contratos UML de componentes com interfaces lollipop/socket. **Não rotular a visão resumida como Diagrama de Componentes UML.** Se criada, separar componentes por responsabilidade e representar contratos que existem (HTTP, JDBC, repositórios/interfaces efetivas), sem inventar microserviços ou interfaces Java não implementadas.

## 7. Implantação — descrever topologia acadêmica real

A aula de implantação enfatiza **nós de execução, artefatos, protocolos e conexões**, inclusive estereótipos `<<device>>`, `<<ExecutionEnvironment>>`, `<<server>>` e armazenamento.

**Situação comprovada no README e código:**
- demo local inicia **JAR Java 21 + `jdk.httpserver`** em `127.0.0.1:8080`;
- navegador acessa recursos e API por **HTTP** na mesma origem;
- aplicação acessa **MySQL 8.4.0 por JDBC / TCP**, banco `joinup_poo`;
- imagens são armazenadas em **diretório local privado fora da webroot**;
- CI reproduz ambiente de teste com dois serviços MySQL independentes em portas 3306/3308, **não é a topologia de produção**.

**DOC-IMPL-01 — P1, documentação opcional:** falta `DIAGRAMA_IMPLANTACAO.puml/.svg`. Seria útil mostrar **um nó anfitrião local** com JVM/HttpServer, MySQL e filesystem, e navegador cliente; separar processos/ambientes, mas **não** afirmar Cloud/VPS, load balancer, firewall dedicado ou TLS deployados sem evidência. Pode-se anexar variante do CI rotulada como *ambiente de integração*, distinta da demo.

## 8. RA2 — conformidade com edital e riscos para a nota

| Item oficial | Peso | Evidência | Situação |
| --- | ---: | --- | --- |
| GoF: **mínimo quatro** pertinentes, implementados, com responsabilidades claras | **4,0** | Singleton, Factory Method, Template Method, Facade reais; §3.5 justifica outros 19 | **Conforme tecnicamente**; quantidade mínima não garante máximo |
| Diagrama de classes, interfaces/relações, coerente com implementação | **1,0** | PlantUML geral + SVG + versão resumida + fragmento GoF; inventário AST | **Conforme substancialmente**; exibição e multiplicidades podem melhorar |
| Relatório técnico: contexto, funcionalidades, usados e não usados, reflexão | **1,0** | `RELATORIO_TECNICO_RA2.md` completo em conteúdo técnico, com §3.5 e §8 | **Parcial documental**: equipe/identificação e reflexão vivida precisam de revisão |
| Código-fonte, legibilidade, funcionamento, evolução/refatoração | **2,0** | Java 21/HttpServer/JDBC/CI 468; PHP legada foi referência, Java é nova implementação | **Boa evidência**; não homologado em instalação/ensaio final |
| Apresentação e arguição: todos dominam sistema integral | **2,0** | `ROTEIRO_APRESENTACAO.md` e perguntas/respostas já preparados | **Não demonstrado**: falta ensaio e arguição cruzada dos integrantes |
| **Total possível** | **10,0** | A tabela indica maturidade, **não estima nota** | — |

**Ponto importante da aula 1:** apresentação da professora identifica **Andreza Vieira** (slide 2), curso/instituição **PUCPR**, disciplina Design de Software. O relatório ainda usa placeholder `Equipe, instituição, professor e turma: a confirmar`. A disciplina/instituição/docente podem ser preenchidos **após confirmação da equipe** (aula foi disponibilizada pelo PO); nomes dos integrantes e turma permanecem desconhecidos. O edital inicia com "**equipes de até 6 estudantes**"; cronograma menciona seis, mas não concluir automaticamente que o grupo real tem exatamente seis.

**P0 de homologação:** `docs/ra2/MATRIZ_VALIDACAO_FINAL.md` registra **V01–V45 como NÃO EXECUTADO** na frente final. Evidência de CI ou sanidade anterior **não torna automaticamente** esses 45 cenários executados. Testar ponta a ponta visitante, cadastro/login, evento, compra, revenda/transferência, acesso comunitário/mídia, atendimento entre suportes, painéis, reinício idempotente e quatro GoF; anexar evidências por SHA. Antes da apresentação, testar os três perfis em sessões independentes.

**DOC-RUN-01 — P1, documentação desatualizada:** `README.md` ainda diz que PR #8/Entrega 06 está em `feat/06-midia-demo` “antes do merge”; o PR #8 **já foi integrado na main**. O procedimento correto para novos avaliadores é usar `main`, aplicar schema/migração 06, DML e iniciar demo local. Corrigir a redação antes de entregar. Os relatórios históricos podem manter o estado que tinham na data em que foram escritos, desde que identificados como históricos.

## 9. Plano de prioridade para entrega 15/10 e apresentação 16/10

**P0 — antes de afirmar conformidade final**
1. Congelar SHA após CI verde e executar instalação limpa em máquina/instância de demo isolada; verificar bootstrap, schema+migração, reinício e três perfis com contas reais do seed.
2. Executar **primeiro cenários E2E essenciais**, depois V01–V45 possíveis; registrar PASSED/FAILED/não realizado sem maquiar histórico.
3. Completar nomes da equipe, professor(a), turma/instituição, incorporar a reflexão escrita pelos participantes e confirmar os 23 GoF analisados na apresentação.
4. Ensaiar quatro GoF com código real, explicar `Facade` vs camada de serviços, Singleton thread-safe vs simples, transações/rollback, por que State/Observer/Strategy não foram usados.
5. Regenerar SVGs se houver alteração Java depois do commit de origem; conferir PDF/Markdown final de entrega.

**P1 — melhorias acadêmicas rápidas sem mexer na aplicação**
6. Corrigir README e identificação; criar **diagrama de pacotes com dependências reais** (1 página); opcionalmente diagrama de componentes e de implantação (1 página cada) se não sacrificar P0. Deixar claro que são complementares ao diagrama de classes obrigatório.
7. Incluir **fragmento de domínio com cardinalidades comprovadas**, sem usar diamantes errados; preparar visual da comparação UML ↔ Java.
8. Registrar no relatório tradeoff ACOP-01 e porque a refatoração do ciclo social/mídia foi postergada para proteger estabilidade da demo.

**P2 — após RA2 ou somente com falha demonstrada**
9. Remover acoplamento circular entre social/mídia; otimizar N+1 do `EventoFacade.listar`; estratégias de paginação/carga; idempotência de POST e reclassificar dependências por interface; proxy TLS/rate limiting em eventual produção.

## 10. Veredito

**GO PARA HOMOLOGAÇÃO ACADÊMICA, NÃO “GO FINAL 100%”.** A evidência aponta bom domínio prático de GoF, separação de responsabilidades e UML de classes coerente. As lacunas com maior potencial de afetar a nota agora estão em **apresentação/arguição coletiva, demonstração reproduzível e identificação/relato da equipe**, e não na falta de três diagramas opcionais ou necessidade automática de um quinto GoF.

**Não houve mudança de código Java/SQL nem execução independente do sistema durante a auditoria**. A criação deste arquivo é apenas registro documental dos achados.
