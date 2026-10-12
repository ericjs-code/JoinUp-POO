# CODEX — Entrega 08: homologação E2E, correções pontuais e release acadêmico RA2

> **JoinUp-POO** — data-base 12/10/2026 · entrega acadêmica 15/10/2026 · apresentação 16/10/2026  
> **Repositório:** `ericjs-code/JoinUp-POO`  
> **Baseline:** `main` após PRs #8 (mídia/demo) e #9 (UML/relatório).  
> **Branch proposta:** `chore/08-validacao-final-ra2`; **PR aberto e não mesclado até auditoria**.  
> **Missão:** comprovar que o produto **funciona integrado em instalação nova**, corrigir SOMENTE problemas realmente encontrados, deixar roteiro, docs e binário de demonstração prontos. Não criar novas funcionalidades.

## 0. Leia integralmente

1. `docs/execucao/AUDITORIA_ESTADO_ATUAL_PR08_PR09.md`, `docs/execucao/AUDITORIA_ENTREGA_05_PR07.md`, resultados e auditorias precedentes.
2. `docs/ra2/MATRIZ_VALIDACAO_FINAL.md`, `docs/ra2/ROTEIRO_APRESENTACAO.md`, `docs/ra2/RELATORIO_TECNICO_RA2.md`, **`docs/ra2/CONFORMIDADE_ENUNCIADO_OFICIAL.md`**, `docs/ra2/DIAGRAMA_CLASSES_GERAL.puml` e `docs/ra2/VISAO_ARQUITETURA.puml`.
3. Requisitos em `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md` e **todos** os adendos.
4. `docs/api/`, `README.md`, `pom.xml`, `.github/workflows/java.yml`, `src/main/resources/db/schema_v0.2.sql`, `seed_demo_v0.2.sql`, `migration_06_midia_demo.sql`.
5. Código e testes implementados: fundação, usuários/auth, eventos, compras/revendas, comunidades/social, arquivos/demo, suporte/painéis. Repositório PHP original **somente leitura** para referência de UX.

**Conflito entre documentação e código?** Não inventar comportamento; esclarecer qual versão de regra foi aprovada e registrar achado. O código não pode ser usado para revogar silenciosamente um adendo do PO.

## 1. Gate CI — só avançar com baseline reproduzível

- Verificar conclusão da [run da main após #9](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38252383206). Se não concluir verde, diagnosticar primeiro e não certificar essa revisão.
- Registrar SHA exato da `main` testada, comandos, GitHub Actions final. Executar `mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify` em MySQL 8.4 isolado, ou registrar quando o único resultado realmente executado é CI. Baseline conhecido **468 testes** (167 Surefire + 301 Failsafe), mas **não assumir automaticamente** que novas versões conservam esse total.
- **Não executar** Failsafe com triggers na base destinada à apresentação.

## 2. Instalação limpa/reprodução (P0)

Preparar em ambiente isolado, sem reutilizar base antiga das entregas:
- Java 21 e Maven 3.9.x, MySQL 8.4 (ou 8.0.16+ suportado), pasta de upload exclusiva fora do repo.
- Administração aplica `schema_v0.2.sql` e **depois** `migration_06_midia_demo.sql` no banco novo `joinup_poo`; registrar resultado/falhas.
- Aplicação utiliza credencial de conta com **DML mínimo**, sem CREATE/ALTER/DROP, e variáveis `JOINUP_ENV=demo`, `JOINUP_DEMO=true`, `JOINUP_HTTP_HOST=127.0.0.1`, `JOINUP_JDBC_URL` correta e `JOINUP_UPLOAD_DIR` absoluta e privada.
- Iniciar app: `java -jar target/joinup-poo.jar`. Validar rotas estáticas, login dos três perfis e `/api/health` (este último é só liveness).
- Reiniciar app: confirmar **idempotência e preservação** de dados/contas, sem recarregar/remover alterações.
- Tentar rodar em banco populado sem marcador somente num ambiente sacrificial próprio; esse teste não deve ameaçar demo/seed original.
- Testar execução a partir de checkout limpo e versão documental `main` atual. Atualizar README removendo instruções obsoletas de branch de PR já mesclado, com passos explícitos de migração/seed e aviso do uso exclusivamente local.

## 3. E2E final — registrar resultado de verdade, não somente plano

A matriz existente `docs/ra2/MATRIZ_VALIDACAO_FINAL.md` contém **45 casos V01–V45** todos inicialmente `NÃO EXECUTADO`. Executar e classificar caso a caso. **Critério: resultados e evidências reais.** Ao concluir, a coluna de status e registro associado devem refletir o que de fato foi testado, sem simplesmente transformar tudo em APROVADO.

**Prioridade P0, fluxo manual essencial** — executar **antes de qualquer caso periférico**:
1. Visitante: início/catálogo público, detalhe evento, comunidade metadados; feed/chat/revendas privados recusados.
2. Participante novo: cadastro + login/logout/cookie, compra simulada em evento futuro, emissão/estoque/histórico próprios.
3. Organizador: login, criar evento via Facade com lineup/categoria/comunidade e editar evento futuro; categorias com vendas não editáveis, data passada bloqueada.
4. Revenda: dois participantes em sessões/cookies separados; anunciar, cancelar, reanunciar, segundo compra, transferência única, vendedor perde acesso comunitário **somente após vender o último ingresso elegível**.
5. Social: entrada manual, chat/feed/curtida/comentário, post somente-imagem, imagem privada bloqueada para visitante e ex-membro, arquivo público do evento acessível.
6. Suporte: abrir ticket como participante/organizador, suporte A associa, suporte B responde com autoria, solicitante retorna/responde e resolve apenas em aguardando_usuario; fechado terminal.
7. Painéis: números do participante e organizador isolados, suporte global com autoria real; três perfis sem acessar dados de terceiro.
8. Demo: reinício sem reset, imagens ainda disponíveis; eventos e credenciais só de fixture.
9. Segurança de navegação: 401/403/404 e tratamento amigável de erros, CSRF, links pós-login e navegação para trás.
10. Evidência acadêmica: mostrar os **quatro GoF reais** e relação de classes/UML, com CI verde referente ao commit em demonstração.

**Depois do P0**, executar os demais V01–V45 de prioridade possível, separando os casos que são integração/concorrência com trigger (devem usar CI/MySQL teste I) dos de navegador demo D. Não executar concorrência destrutiva diretamente na base da apresentação. Para cenário comprovado somente por testes automáticos, anotar evidência e marcar execução como automática **apenas se o caso da matriz tiver sido realmente executado naquela revisão**, caso contrário manter NÃO EXECUTADO, mencionando suíte histórica como apoio.

Usar **contextos/cookies realmente independentes**, contas demo e dados fictícios. Não reutilizar Ana como vendedor de “último ingresso” se ainda possui outro ingresso; conferir condições do seed antes de fazer afirmações. Evitar compras/revendas duplicadas por retry, pois a API não implementa idempotency key.

Evidências: SHA/JAR, ambiente, data/hora, passos executados, status HTTP quando mensurável, captura/log sanitized (sem tokens, hashes, senhas de DB), resultado **APROVADO/REPROVADO**, responsável, issue/PR se falhou.

## 4. Correção somente de problemas confirmados

- Se qualquer cenário P0 reprovar, documentar reprodução mínima e causa antes da correção. Corrigir em branch separável/diff pequeno, preservando quatro GoF, transações, regras aprovadas, testes anteriores e Java puro.
- Reexecutar os casos impactados e teste de regressão. Não alterar schema/seed se não houver defeito real comprovado e justificativa.
- Se o problema for apenas README/documentação, corrigir docs e não mexer no backend.
- Não começar otimização N+1, rate limiting de produção, novo gateway, redesign completo, novos padrões GoF, avatar ou sistema de push; todos estão fora deste gate sem exigência formal.
- Uma falha de UX/small-screen deve ser avaliada pela severidade à demonstração; priorizar evitar quebra de navegação no tamanho de tela usado na apresentação.

## 5. Fechamento acadêmico e ensaio — critérios OFICIAIS

- **Enunciado oficial RA2 recebido em 12/10/2026**, comparado e sintetizado em `docs/ra2/CONFORMIDADE_ENUNCIADO_OFICIAL.md`. Não afirmar que o enunciado está indisponível: o PDF original foi fornecido ao Tech Lead; o repositório preserva a síntese rastreada por páginas. Pesos: **4,0 GoF; 2,0 código/qualidade; 2,0 apresentação/arguição coletiva; 1,0 UML; 1,0 relatório**. A avaliação exige **no mínimo quatro padrões GoF, não exatamente quatro**; quatro já implementados são a solução atual, mas o mínimo não garante nota máxima. Não adicionar novos padrões artificialmente nem sem aprovação do PO.
- **Conteúdo estudado confirmado:** o PO disponibilizou os slides *Padrões de Projetos GoF — Introdução* e confirmou estudo dos **23 padrões clássicos** (slide 12, detalhados nos slides 13–18 e agrupados por intenção no 19). O relatório `§3.5` já contém justificativa contextual dos **19 padrões não implementados**. **Validar durante a RA2 a qualidade argumentativa** de cada uma das 19 justificativas, coerência com as classes reais e divisão criação/estrutura/comportamento. Quatro padrões reais são o **mínimo**, não garantia automática de todos os 4,0 pontos. MVC aparece nos slides 7–10 como modelo arquitetural, **não** quinto GoF.
- O relatório deve conter explicitamente **identificação da equipe**, funcionalidades, cada padrão (intenção, problema, classes, código, benefícios), **análise contextual dos não utilizados** e **reflexão/lições aprendidas** (seções 4.1–4.5 do enunciado). Já existe rascunho da reflexão baseado em problemas comprovados de SQL, concorrência, HTTP e UML, mas a equipe precisa confirmar vivências individuais.
- **Formato:** até seis estudantes indicado na página 1; cronograma oficial da página 3 aponta **equipe de seis estudantes**. Confirmar nomes completos dos integrantes, instituição, turma, professora e duração permitida. Datas oficiais: **entrega 15/10/2026; apresentação 16/10/2026**. A duração de 13 minutos do roteiro é proposta, não duração fixada pelo PDF.
- **Arguição coletiva vale 2,0 pontos:** todos os integrantes devem conhecer **o projeto inteiro**, não apenas seu módulo; a professora pode perguntar a qualquer integrante sobre padrões, classes, alternativas e decisões. Realizar ensaio com **rodízio de perguntas aleatórias** entre todos e registrar lacunas; não declarar domínio coletivo sem participação real de cada um.
- Campos de integrantes, docente, turma e duração que o PO ainda não forneceu devem permanecer **PENDENTE DE PO**, jamais ser inventados. **A lista de GoF estudados está confirmada pelo PDF da disciplina.**
- Auditar consistência UML ↔ classes `main` **do commit final**, os **quatro GoF efetivamente implementados** (mínimo oficial, não máximo), nomes reais de classes, assinaturas e testes. O UML geral de 123 tipos pode ser grande: garantir visão resumida legível e fragmento GoF para apresentar; não anunciar GoF não implementado como aplicado.
- Ensaiar `docs/ra2/ROTEIRO_APRESENTACAO.md` com cronômetro (planejado ~13min, ajustar ao professor), sem criar dependência de internet. Preparar plano B com capturas e logs **da versão testada**, sem vendê-los como execução ao vivo.
- Gerar `docs/ra2/RELATORIO_HOMOLOGACAO_FINAL.md`: versão/hash, P0 e V01–V45 aprovados/reprovados/não realizados, evidências, riscos, comandos realmente executados, CI, UML/renderização, tempo de apresentação, **rubrica oficial 4+2+2+1+1 e cobertura dos requisitos acadêmicos**, campos faltantes e recomendação **GO/NO-GO**.

## 6. Entregáveis e limites de atuação

1. PR `chore/08-validacao-final-ra2` **sem merge antes de auditoria**, com correções e documentação rastreáveis.
2. README coerente com `main`, docs atualizados apenas quando houver fato real.
3. `docs/ra2/MATRIZ_VALIDACAO_FINAL.md` atualizada caso a caso e `RELATORIO_HOMOLOGACAO_FINAL.md` com GO/NO-GO por risco.
4. Registros da instalação limpa, navegador (três perfis), upload, revenda e suporte, dados demo, suíte, CI, UML e ensaio.
5. Se executar parcialmente, **declarar parcialmente**. Não chamar “100% testado” apenas porque CI verde; não copiar resultados históricos como se executados agora.
6. Não fazer merge. Retornar URL do PR e itens que exigem aprovação do PO.

**Pronto para entrega acadêmica quando:** `main` CI verde, instalação nova reproduzível e fluxo completo demonstrado, P0 sem defeito bloqueante, documentação e quatro GoF coerentes, enunciado/identificação revisados, ensaio realizado e evidências reais registradas. Caso contrário, recomendar NO-GO ou GO COM RESSALVAS objetivas.
