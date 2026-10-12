# CODEX — Entrega 09: correções documentais e adequação aos conteúdos das aulas

**Data-base:** 12/10/2026 · **RA2:** entregar 15/10; apresentar 16/10.  
**Branch:** `docs/09-conformidade-aulas-uml`, a partir da **`main` atualizada** após o PR de homologação 08, **ou** de `main` atual se o 08 ainda não existir, desde que sem sobrepor as mesmas edições.  
**Saída:** um PR documental específico, **não efetuar merge**, preservar código Java e dados (nenhuma mudança em `src/main/java`, `src/test/java`, `src/main/resources/db` ou `pom.xml`).  
**Coordenador/PO:** ChatGPT audita; Codex executa. Se o PR 08 ainda não foi iniciado, **priorizar 08 antes da 09**. O presente prompt não substitui `docs/prompts/CODEX_08_VALIDACAO_FINAL_RA2.md`.

## 0. O que foi auditado — leitura obrigatória

- `docs/ra2/AUDITORIA_CONFORMIDADE_AULAS_12-10-2026.md` e `docs/ra2/CONFORMIDADE_ENUNCIADO_OFICIAL.md`.
- `docs/ra2/RELATORIO_TECNICO_RA2.md`, `docs/ra2/DIAGRAMA_CLASSES_GERAL.puml`, `docs/ra2/VISAO_ARQUITETURA.puml`, `docs/diagramas/CLASSES_GOF_v0.1.puml`, `docs/diagramas/EVENTOS_DOMINIO_02.puml`, `docs/ra2/MATRIZ_VALIDACAO_FINAL.md`, `README.md`.
- Verificar o código efetivo antes de afirmar multiplicidades, interfaces ou implantação: `ApplicationBootstrap`, `EventoFacade`, `ConfiguracaoSistema`, `CadastroUsuarioCreator`, `AutenticacaoTemplate`, `ExecutorTransacional`, `ConnectionFactory`, `ArquivoService`, `ComunidadeSocialService`, SQL schema e migration 06.
- Materiais fornecidos ao Tech Lead pelo PO: *Aula01* (classes, página 7–19), *Padrões GoF — Introdução* (23 GoF p.12–19, MVC p.7–10), *Diagrama de Pacotes* (dependências e baixo acoplamento p.14–19), *Diagrama de Componentes* (interfaces fornecidas/requeridas e estereótipos p.10–24), *Diagrama de Implantação* (nós, artefatos, protocolos p.4–13), *Facade* (cliente → fachada → subsistemas), *Singleton* (construtor restrito/acesso global). O repositório **não recebe cópia dos slides de terceiros**; usar somente essa síntese do conteúdo.

**Regra acadêmica:** enunciado da RA2 exige **diagrama de classes**, quatro ou mais GoF e relatório, não obriga diagramas de pacotes/componentes/implantação. Estes três serão complementares de estudo; não descrevê-los como obrigatórios ou como novos padrões GoF.

## 1. Primeira correção: README coerente com `main`

- Remover instrução ainda existente que manda `git switch feat/06-midia-demo` e afirma que PR #8 não foi mergeado. Atualizar a redação para indicar **Entregas 00–07 integradas**, referências para docs RA2 (PR #9), auditoria e comandos reais.
- Não alterar a ordem de instalação: executar `schema_v0.2.sql` → `migration_06_midia_demo.sql` por administração na base isolada `joinup_poo`; app somente DML, sem DDL; `JOINUP_ENV=demo`, `JOINUP_DEMO=true`, JDBC loopback, HTTP 127.0.0.1, upload dir privado; o `seed_demo_v0.2.sql` é incorporado pelo DemoRunner somente se a demo foi habilitada e base elegível.
- Não inventar credenciais/servidor público. Destacar que comando `mvn clean verify` sem `-Pmysql-it` **não executa Failsafe/MySQL**. A suíte CI integral usa `mvn -Pmysql-it clean verify` em MySQL 8.4 isolado; **não rodar testes de integração destrutivos na demo da apresentação**.
- Se PR 08 já corrigiu README, simplesmente verificar consistência sem reeditá-lo e manter o PR 09 sem conflitos.

## 2. Criar diagrama de pacotes com dependências reais — `docs/ra2/DIAGRAMA_PACOTES.puml/.svg`

**UML de pacote, não mera lista.** Agrupar por conceitos de alto nível: `bootstrap`, `http` + `auth` + `usuarios`, `eventos`, `compras`/`revendas`, `comunidades`, `arquivos`, `suporte`/`paineis`, `shared.db`, `config` e `demo`. Mostrar **arestas tracejadas de dependência** para relações confirmadas no código, em escala suficiente para uma lâmina de apresentação.

- Destacar e explicar a dependência recíproca comprovada `ComunidadeSocialService ↔ ArquivoService`. **Não ocultá-la ou simular que não existe**. Uma nota mostra motivo (validar elegibilidade e vincular imagens), impacto (acoplamento/bootstrapping em duas etapas) e que refatoração é **trabalho separado, não nesta entrega**.
- Depêndencias entre subpacotes `domain`, `dto`, `service`, `repository` e `http` só onde realmente existem; não afirmar arquitetura de portas/adaptadores purista se não implementada.
- Não tratar o `ApplicationBootstrap` (composition root) como violação de acoplamento só por instanciar todos os módulos.
- Usar UML de pacotes com `package`, dependências `..>` e legenda; não criar classes fictícias.

## 3. Criar diagrama UML de componentes — `docs/ra2/DIAGRAMA_COMPONENTES.puml/.svg`

**Diagrama de componentes verdadeiro, não diagrama de classes com título modificado.** Utilizar notação `component`/interfaces e estereótipos pertinentes.

- Frontend estático e cliente navegador; `Router / Routes HTTP` como ponto de entrada; núcleo de aplicação representado por componentes (Autenticação e Contas, Eventos, Comércio/Revenda, Comunidades, Suporte/Painéis, Mídia); Persistência JDBC e `MySQL`; armazenamento de arquivos em pasta privada.
- Interfaces **fornecidas/requeridas** devem corresponder a contratos verificáveis, por exemplo API HTTP pública/privada, `UsuarioRepository`, `VerificadorSenha`, JDBC/SQL; distinguir API lógica (HTTP) de interface Java, sem inventar `IEventoService` ou módulos implantáveis separadamente.
- Este é um **monólito Java**, sem microserviços, filas, gateway real, serviço de notificações, cache distribuído ou storage cloud. Não usar lollipop/socket se o que existe é apenas uma chamada concreta sem contrato verificável; em caso de dúvida prefira dependência explicada.
- Arestas só de relações concretas e auditáveis; diagramar em 1 página legível e fornecer legenda.

## 4. Criar diagrama UML de implantação — `docs/ra2/DIAGRAMA_IMPLANTACAO.puml/.svg`

Representar **topologia local real da demonstração** (não uma arquitetura de produção hipotética):
- dispositivo do aluno com **navegador** (pode ser mesmo host ou cliente local, indicar o cenário efetivo, não inventar máquina adicional);
- nó host de demonstração com **Java 21/JVM**, `joinup-poo.jar`, `HttpServer` (HTTP em loopback `127.0.0.1:8080`);
- processo **MySQL 8.4** com banco isolado `joinup_poo`, ligação JDBC/MySQL/TCP loopback;
- **filesystem privado** com uploads fora da webroot;
- artefatos do projeto `joinup-poo.jar`, `schema_v0.2.sql` e `migration_06_midia_demo.sql` (aplicados administrativamente), navegador HTML/CSS/JS.
- Especificar nós, ambientes de execução, artefatos e protocolos. Repositório/CI não fazem parte da topologia demo; se representados, colocar uma **segunda vista separada** e rotular como CI de testes (MySQL 3306/3308), sem confundir com demonstração. **Sem HTTPS, proxy, cloud ou load balancer não comprovados**.

## 5. Adicionar fragmento UML pedagógico com cardinalidades confirmadas

Criar `docs/ra2/DIAGRAMA_DOMINIO_CARDINALIDADES.puml/.svg` **somente após verificar schema SQL, integridade FK/UNIQUE, colunas `NOT NULL`, serviços e regra funcional**. Sugestão de escopo: `Evento`, `TipoIngresso`, `Ingresso`, `Compra`, `Participante` e `Comunidade`.

- Usar multiplicidades `0..1`, `1`, `0..*` e `1..*` onde comprovadas. **Não confundir quantidade por pedido com cardinalidade histórica**, nem inferir composição porque existe FK.
- Diferenciar classes Java reais de conceitos persistidos somente como tabela SQL. Se `Compra` não existir como classe de domínio Java, **não inventá-la**: usar nota `<<table>>` ou preferir `CompraDados`/`Ingresso`, explicitando que é **visão conceitual de persistência** e não UML de classes espelhando Java.
- Colocar notas breves explicando opcionalidade, titularidade, evento/comunidade e ciclo de vida sem contradizer RFs aprovados.
- Explicar no relatório que UML geral mostra dependências de tipos e esse fragmento mostra cardinalidades **do domínio persistido**; não falsamente descrever diagramas como idênticos em escopo.

## 6. Corrigir relatório e preparar apresentação

- Integrar links dos novos diagramas ao `RELATORIO_TECNICO_RA2.md` e `ROTEIRO_APRESENTACAO.md`, opcionalmente README, com seção curta **“Articulação entre os conteúdos estudados”**: (a) classe = tipos/relacionamentos, (b) pacotes = agrupamento e direção das dependências, (c) componentes = contratos e módulos, (d) implantação = nós/artefatos/protocolos, (e) GoF = soluções de colaboração entre objetos. Explicar que não são a mesma visão.
- Preservar no relatório a cobertura **23 GoF (4 implementados + 19 justificados)** e a rubrica correta **4+2+2+1+1**, sem aumentar padrões por descrição de frameworks/camadas.
- Incluir **achado de acoplamento circular** no relatório como tradeoff técnico reconhecido, com proposta de solução posterior e motivo de não refatorar agora; o conteúdo deve ser honesto, não propaganda arquitetural.
- No material do seminário, prever no máximo 45–75 s opcionais para uma visão dos três diagramas (principalmente **componentes e implantação**) e deixar o tempo dos quatro GoF priorizado. A duração atual de 13 min é sugestão, não prazo confirmado.
- Identificação documental: slides da Aula01 identificam **professora Andreza Vieira e PUCPR**, mas nomes dos integrantes/turma requerem confirmação; não inventar ninguém. Preferir marcar `A CONFIRMAR COM A EQUIPE` a criar dados acadêmicos errados.
- Não marcar casos V01–V45 `EXECUTADO` sem reproduzir e anexar evidência real; essa é a frente 08.

## 7. Prova, checagem e PR

- Instalar/usar PlantUML + Graphviz disponíveis localmente; rodar `-checkonly` e `-tsvg` nas novas fontes. **Abrir os SVGs e verificar visualmente** que não são tela de erro, não estão cortados nem contêm linhas ilegíveis; ver evidências/limitações do PR #9.
- Confirmar referências do relatório/README/roteiro, existência dos arquivos, coerência de cardinalidades e sentido das setas. Rodar `mvn -q test` se o ambiente tiver Java 21/Maven; é regressão de confiança, mas o PR será exclusivamente documental. Não afirmar que executou se não pôde.
- Resumo PR: baseline SHA, lista de arquivos, conteúdo por aula, **rastreabilidade com arquivos reais**, comandos de render/teste executados, inspeção visual, known issues ACOP-01 e status do CI. Não fazer merge sem auditoria do Tech Lead/PO.
- Se houver conflito com a Entrega 08, rebasear no último `main` e fazer o menor diff possível; **não sobrescrever resultados de E2E feitos pela 08**.

**Aceite:** README sem branch obsoleta; três UML complementares renderizados, um fragmento de cardinalidades honesto, relatório e roteiro com links funcionais, nenhum GoF artificial, fonte Java/SQL/testes intacta, PR sem merge, validação documentada. Se P0 da Entrega 08 estiver incompleto, priorize o P0 antes destes desenhos.
