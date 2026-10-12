# JoinUp-POO — Confronto com o enunciado oficial RA2

> **Revisão:** 12/10/2026. **Fonte:** “Especificação do Projeto – Avaliação Somativa (RA2)”, Design de Software, Bacharelado em Engenharia de Software, PDF de 3 páginas fornecido diretamente pelo PO. O PDF não foi reproduzido neste repositório; a tabela abaixo é uma síntese rastreada por página. **Status:** plano de atendimento, não atestado de nota.

## 1. Exigências oficiais e diferença em relação às premissas anteriores

1. **Mínimo de 4 padrões GoF (p. 1), NÃO exatamente 4.** O enunciado avisa que cumprir o mínimo não garante pontuação máxima e exige justificativa técnica se outros padrões do conteúdo da disciplina forem pertinentes e não aplicados. Nossa seleção de **Singleton, Factory Method, Template Method e Facade** permanece a escolha arquitetural atual; **não** introduzir padrões artificiais para inflar quantidade. Se o professor/conteúdo de aula apontar aplicação adicional pertinente, submeter decisão ao PO com custo/benefício antes de alterar o sistema.
2. **Diagrama atualizado e coerente com classes reais (p. 1–2)**, evidenciando classes, interfaces, relacionamentos e padrões. UML geral e resumo já foram criados no PR #9, sincronizados com a base após PR #8; reconferir somente se houver alteração posterior.
3. **Relatório: identificação/contexto, funcionalidades, cada padrão, análise dos demais GoF vistos em sala e reflexão/lições aprendidas (p. 2).** Antes desta revisão, o relatório detalhava bem os quatro utilizados, mas `§3.5` tinha justificativa genérica para os não utilizados e `§8` era conclusão sem reflexão específica. São lacunas acadêmicas a corrigir.
4. **Código completo executável + instruções (p. 2)**. Não basta citar padrões no UML/relatório; devem aparecer e atuar nos fluxos de produção. Repositório contém Java 21/JDBC/HttpServer e quatro padrões exercitados por testes; validar instalação limpa e demo real da mesma versão.
5. **Arguição coletiva (p. 2)**: a professora pode perguntar aleatoriamente sobre qualquer módulo/padrão para **qualquer integrante**. Todos precisam compreender o sistema como um todo; dividir apresentações por módulo sem estudo cruzado não basta.
6. **Equipe: até 6 estudantes na instrução de formato da p. 1; “Equipe: 6 estudantes” no cronograma da p. 3.** Confirmar seis integrantes e seus nomes para o relatório; não os deduzir do GitHub.
7. **Cronograma (p. 3)**: entrega em **15/10/2026**, apresentação em **16/10/2026**. O enunciado não fixa duração em minutos; roteiro atual de 13 min é **proposta**, a confirmar com a professora.

## 2. Critérios e pesos oficiais (p. 2–3)

| Critério da avaliação | Pontos máximos | Situação / ação |
| --- | ---: | --- |
| Aplicação de padrões GoF: quantidade, pertinência, qualidade, classes e responsabilidades | **4,0** | Quatro padrões reais presentes; argumentar escolha e não adoção dos outros estudados. Mínimo não assegura 4,0. |
| Diagrama de classes completo/coerente | **1,0** | PR #9 publicou UML integral e visão resumida; sincronizar com código entregue. |
| Relatório e justificativas técnicas | **1,0** | Complementar aplicabilidade dos padrões não usados e reflexão; inserir identificação da equipe. |
| Código-fonte e qualidade da implementação | **2,0** | CI MySQL real reportou 468 testes; falta homologação E2E de release/instalação nova. |
| Apresentação, arguição e domínio técnico coletivo | **2,0** | Ensaiar com todos os seis estudantes, perguntas aleatórias, mapa mental dos quatro GoF e fluxo completo. |
| **Total** | **10,0** | Pontos são rubrica oficial, **não nota estimada**. |

## 3. Gap assessment e ordem prática

| ID | Prioridade | Lacuna / risco | Ação e evidência |
| --- | --- | --- | --- |
| RA2-01 | **P0** | Enunciado anteriormente interpretado como “exatamente quatro”; necessário “no mínimo quatro” | Corrigir terminologia nos materiais ativos e preservar justificativa de pertinência; não impor quinto padrão. |
| RA2-02 | **P0** | Conjunto de padrões GoF **efetivamente estudados em sala** desconhecido | PO/equipe fornece nomes/slides. Fazer tabela contextual **para cada padrão estudado** não aplicado. Não alegar que a turma estudou todos os 23 sem evidência. |
| RA2-03 | **P0** | Relatório `§3.5` insuficiente e reflexão não explícita | Complementar análise contextual e seção de dificuldades/decisões/mudanças/lições com evidência real (DDL CHECK/FK, isolamento JDBC, locks revenda/social, HTTP 413, uploads/seed). |
| RA2-04 | **P0** | Identificação da equipe não preenchida | Informar equipe, integrantes, instituição, turma e professora; conferir os seis participantes exigidos no cronograma. |
| RA2-05 | **P0** | Arguição coletiva pode comprometer até 2,0 | Todos ensaiam perguntas fora de seus módulos; documentar perguntas e respostas, rodízio aleatório. |
| RA2-06 | **P0** | Matriz de 45 casos ainda sem ensaio final executado | Executar no ambiente limpo; registrar status real e evidência; priorizar P0 do Prompt 08. |
| RA2-07 | **P1** | Tempo de apresentação não informado pelo enunciado | Confirmar com professora; adequar 13 minutos propostos. |
| RA2-08 | **P1** | Atualização do diagrama/relatório depois de correções de release | Revisar assinaturas GoF, refs Git e gerar SVG da versão final se código mudou. |

## 4. Critérios de qualidade da argumentação GoF

Para **cada padrão utilizado**: problema específico de design no JoinUp, intenção, classes/participantes, código real e fluxo concreto, benefício e tradeoff; responder por que a alternativa foi rejeitada.

Para **cada padrão estudado e não utilizado**: apontar um cenário do JoinUp em que **poderia** servir, qual é o mecanismo que o código utiliza hoje, **por que a introdução do GoF agora não traz benefício proporcional**, e em que evolução seria reconsiderado. Exemplo contextual (**não prova que esses padrões foram estudados**): *Strategy* para provedores de pagamento — só há simulação local; criar hierarquia de estratégias sem algoritmos distintos seria decorativo. *Observer* para notificações reais — a plataforma não publica eventos assíncronos/integrações; risco de infraestrutura desnecessária. *State* para tickets — enum `StatusTicket` com transições centralizadas é suficiente para quatro estados e poucas ações; classes por estado acrescentariam custo sem novos comportamentos de objetos. A lista final dependerá da cobertura curricular real.

## 5. Não alterar sem aprovação funcional

- Esta comparação não autoriza ampliar GoF nem alterar comportamento de compras, eventos, suporte ou comunidades.
- Se um quinto GoF for tecnicamente justificável e acadêmica/economicamente vantajoso, registrar proposta com caso de uso, classes, testes e impacto no UML; **PO aprova antes** de implementar.
- Não declarar homologado até instalação limpa, testes ponta a ponta e preparação da arguição coletiva.

**Fontes secundárias versionadas:** `docs/ra2/RELATORIO_TECNICO_RA2.md`, `docs/ra2/MATRIZ_VALIDACAO_FINAL.md`, `docs/ra2/ROTEIRO_APRESENTACAO.md`, `docs/prompts/CODEX_08_VALIDACAO_FINAL_RA2.md`.
