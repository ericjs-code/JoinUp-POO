# JoinUp-POO — Frente paralela 07: documentação RA2, UML integral e validação final

> **Preparado em 11/10/2026** · Entrega RA2 em 15/10/2026, apresentação 16/10/2026.  
> **Destinatário:** segundo colaborador ou outro chat Codex — trabalho **paralelo** ao Prompt 06 (mídia e demo).  
> **Branch sugerida:** `docs/07-ra2-relatorio-uml` partindo de `main` após PR #7, **PR sem merge** até auditoria e sincronização com Prompt 06.

## Missão

Preparar a documentação acadêmica definitiva do JoinUp-POO **com base no código Java real**, sem inventar classes, padrões, testes, requisitos ou fluxos implementados. Criar UML completo e relatório RA2 consistente; preparar um roteiro de demonstração e matriz de testes end-to-end para execução depois da Entrega 06.

**Não editar código Java/SQL nem UI durante esta frente, para evitar conflitos com o Codex 06.** Também não alterar README enquanto o 06 o atualiza. Se constatar divergência funcional, registrar issue/achado, não corrigir silenciosamente.

## Fontes obrigatórias

- Ler o enunciado RA2 anexado ao projeto, se estiver disponível. Se não estiver, **registrar explicitamente que o enunciado integral não foi encontrado** e fundamentar somente nos requisitos acadêmicos já documentados: **exatamente quatro padrões GoF**, diagrama de classes coerente com código, justificativas e tradeoffs, sistema executável e apresentação.
- Ler especificação funcional e **todos** os adendos `docs/requisitos/ADENDO_*.md`.
- ADR-001 Java 21 puro, proposta arquitetural, `CLASSES_E_PADROES_GOF_v0.1.md`, `CLASSES_GOF_v0.1.puml`, `EVENTOS_DOMINIO_02.puml`, `SUPORTE_PAINEIS_05.puml`, contratos HTTP.
- Ler `src/main/java` **real** e `pom.xml`, inspecionar classes concretas de Singleton/Factory/Template/Facade, serviços, controllers/handlers, repositórios JDBC, DTOs e relacionamento real de domínio.
- Revisar auditorias 00/01/02, cruzada PR4+PR5, correção 04.1, auditoria 05, relatórios de testes/CI e prompt 06; distinguir funcionalidades finalizadas daquelas ainda pendentes.
- PHP original somente como referência de UX e requisitos herdados, nunca como evidência de código Java.

## A. Relatório RA2 — documento editável em Markdown

Criar `docs/ra2/RELATORIO_TECNICO_RA2.md`, com:
1. Identificação do projeto, problema resolvido, objetivos funcionais e principais atores.
2. Stack e arquitetura: Java 21 puro, `HttpServer`, JDBC/MySQL 8.4, Maven/Jackson/JUnit, HTML/CSS/JS; camadas reais, bootstrap/composição, fronteiras, fluxo HTTP → service/facade → repository, transações JDBC, mecanismos de segurança.
3. **Somente quatro GoF requeridos**:
   - **Singleton:** `ConfiguracaoSistema`, inicialização thread-safe, imutabilidade, casos de uso reais, crítica ao uso em `Connection`.
   - **Factory Method:** `CadastroUsuarioCreator` e subclasses, criação polimórfica concreta no cadastro dos três perfis.
   - **Template Method:** `AutenticacaoTemplate` final + subclasses de autenticação específicas; algoritmo e hooks realmente presentes.
   - **Facade:** `EventoFacade` coordenando evento/lineup/categorias/comunidade em transação.
   Para **cada padrão**, fornecer intenção/participantes, trecho curto **fiel ao código versionado**, caso de uso demonstrável, vantagens, limitações/tradeoffs, alternativa rejeitada e teste/arquivo relacionado.
4. Modelagem MySQL, FKs/CHECKs, snapshots de compras, estoque, revenda, comunidade após revenda, chamado com estados e histórico/autoria.
5. Decisões de segurança: PBKDF2, cookie HttpOnly, status ativo, CSRF, titularidade IDOR, queries parametrizadas, atomicidade, locks e limites. Não chamar ambiente acadêmico de \"pronto para produção\".
6. Evidência de qualidade: CI Java21/MySQL8.4, totais verificados por **commit**, casos de concorrência/rollback, testes manuais **realmente executados** vs planejados.
7. Limitações explícitas: imagens até Prompt 06, seed autodemonstrativo, UX/móvel e carga; atualizar status só depois de PRs posteriores aprovados.
8. Conclusão e evolução. Citar caminhos e commits de origem de cada alegação relevante.

**Não copiar literalmente grandes trechos de arquivos desnecessários.** Usar trechos mínimos fiéis a código e referências precisas; evitar comparações sem evidência.

## B. UML integral e coerente

Criar `docs/ra2/DIAGRAMA_CLASSES_GERAL.puml` e gerar SVG exportável se PlantUML disponível:
- Pacotes `bootstrap`, `http`, `config`, `auth`, `usuarios`, `eventos`, `compras`, `revendas`, `comunidades`, `suporte`, `paineis`, `shared/db` conforme código.
- Relações reais de dependência/herança/composição, assinaturas mais relevantes e multiplicidades **somente fundamentadas**; não inventar ORM/agregações não existentes.
- Exibir padrões GoF visíveis e marcados sem rotular Repository/DTO/MVC como novos GoF necessários.
- Se diagrama geral for grande, produzir também visão resumida legível de arquitetura, mantendo diagrama geral completo para avaliação.
- Confirmar por inspeção de classes Java que nomes e métodos existem; executar `PlantUML -checkonly` (se disponível) e registrar comando/resultado; não declarar renderização se não feita.

## C. Roteiro de apresentação e matriz E2E (preparação paralela)

Criar `docs/ra2/ROTEIRO_APRESENTACAO.md` e `docs/ra2/MATRIZ_VALIDACAO_FINAL.md`:
- Roteiro **10–15 minutos**, ajustável conforme duração real fornecida pelo professor; demo pública → participante cadastro/login → evento e compra simulada → ingresso → comunidade/participação manual/feed → revenda com segundo participante → chamado e duas contas de suporte → dashboard organizador → explicação objetiva dos quatro GoF e evidência do CI/UML.
- Contas e senhas apenas **demo** do seed; não incluir segredos reais.
- Registro por requisito importante: ação, papel, ambiente, estado esperado, URL, evidência e status: `EXECUTADO` / `NÃO EXECUTADO` / `BLOQUEADO`. Inicialmente registrar somente planejado; atualizar após validação real.
- Gravar possíveis perguntas do professor (por que esses quatro GoF? quais tradeoffs? por que JDBC e HttpServer? como evitar dupla venda? como o Facade difere de God Object?).
- Reservar etapa final para sincronizar com a implementação **após o PR 06** (mídia/seed), sem fingir que recursos pendentes já existem.

## D. Testes e entrega documental

- Verificar ao menos o estado do CI da `main`; se não executar Java/MySQL, registrar análise documental, não executar `mvn` por declaração.
- Conferir que diagramas GoF antigos ainda correspondem às classes após PR #7. Se houver divergências, apontar e corrigir **somente arquivos documentais** em sua branch.
- Não apagar dados, não alterar schema ou seed e não usar edição Java/JS nessa frente.
- Entregar PR `docs/07-ra2-relatorio-uml` **sem merge automático**; relatório de quais documentos, evidências e itens pendentes de sincronização após Entrega 06.

## Aceite

Documentação tecnicamente precisa, quatro GoF comprovados em código real, UML legível/coerente, roteiro reproduzível e matriz E2E pronta para validação pós-06. **Não** afirmar que o relatório garante todos os critérios de avaliação sem confirmar o enunciado da disciplina.
