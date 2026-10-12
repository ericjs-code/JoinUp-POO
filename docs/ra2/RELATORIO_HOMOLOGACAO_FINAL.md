# Entrega 08 — homologação técnica RA2

**Parecer: GO COM RESSALVAS para auditoria técnica e demonstração local.**
**NO-GO para declarar fechamento acadêmico integral**, enquanto faltarem ensaio
coletivo, confirmação de duração e os casos sem execução integral abaixo.
Não foi reproduzido defeito funcional bloqueante na aplicação.

Execução técnica: **Codex**, 12/10/2026, America/Sao_Paulo. Nenhum integrante foi
representado como participante de ensaio. Branch `chore/08-validacao-final-ra2`,
criada da `main` atualizada `011ecb2dcdd8f9457d2e1c5eeceb00edee018ffd`. PR para
auditoria, **sem merge**. Entregas 09 e 10 não foram iniciadas.

## Versão, instalação e isolamento

| Item | Evidência desta execução |
| --- | --- |
| Código testado | `011ecb2dcdd8f9457d2e1c5eeceb00edee018ffd`; extraído por `git archive` em checkout limpo, sem `target`, arquivos ignorados ou alterações locais |
| Comparação com UML/inventário | `git diff --name-only 7efe29c1529a71e3c1a82a52716a5a32947a61fe 011ecb2dcdd8f9457d2e1c5eeceb00edee018ffd -- src pom.xml .github` vazio; produção/testes/SQL/recursos/build/CI idênticos |
| Runtime | Windows, JDK **21.0.12.1**, Maven **3.9.9**, MySQL **8.4.0** nativo, HTTP `127.0.0.1:8080` |
| Demo D | Instância nova em **3315**, banco `joinup_poo`; schema → migração 06 por administração; **0 participantes e 24 tabelas antes do boot** |
| Aplicação D | `JOINUP_ENV=demo`, `JOINUP_DEMO=true`, HTTP/JDBC loopback; conta `joinup_it` com **SELECT/INSERT/UPDATE/DELETE**; nenhum CREATE/ALTER/DROP/GRANT |
| Integração I | Duas outras instâncias novas em **3311/3312**, `JOINUP_DEMO=false`, guardas das fixtures e conta DML; nenhum trigger de teste em D |
| Mídia | Diretório absoluto privado em `%LOCALAPPDATA%/JoinUp-POO/entrega08-011ecb2/uploads-demo-final`, fora do repo/webroot, exclusivo da aplicação local; inclui arquivos de tentativas sacrificialmente separadas, sem limpeza automática |
| JAR demonstrado | SHA-256 **`106B4E2CF437AB8594F4845214C4FE0B1BACAFE5F3B4773685EFB61754EB0BBD`**; empacotado no checkout limpo e iniciado com `java -jar` |

[Registro da instalação](evidencias/entrega08/instalacao.txt). O schema foi
aplicado antes da migração. O seed não foi aplicado manualmente: o primeiro boot
carregou os quatro eventos/nove contas e marcador pelo `DemoRunner`. O health é
somente liveness. Cadastro, login, comércio e conteúdo validaram o banco real.

O Docker não estava iniciado; não foi necessário habilitá-lo. Três tentativas
iniciais em outras instâncias foram interrompidas por erros do **cliente de
homologação**, descritos adiante. Não foram reutilizadas essas bases para
repetir compras; a execução aprovada começou em outra instância vazia (3315).

## Resultados medidos

| Frente | Resultado | Alcance |
| --- | --- | --- |
| Build limpo da demo | **167 Surefire; BUILD SUCCESS**, 10:54:44 -03:00 | `clean verify` sem perfil; não equivale a MySQL |
| Regressão integral local | **468 = 167 Surefire + 301 Failsafe**, zero falhas/erros/ignorados; **BUILD SUCCESS**, 11:06:39 -03:00, 2 min 48 s | HTTP/JDBC/MySQL real, concorrência, rollback, mídia e JVMs de bootstrap |
| Homologação HTTP D | **258 assertivas aprovadas**, 11:01–11:02 -03:00 | Visitante, P/C/O/O2/S1/S2 com CookieContainers independentes; nenhum token publicado |
| Sanidade de navegador D | Participante, organizador e suporte autenticaram; ingresso transferido, feed/chat/imagem, comentário, texto HTML literal, categoria vendida bloqueada, upload e autoria visual conferidos | Navegador do Codex, desktop 1265×713; alternância por logout/login, não quatro contextos de navegador simultâneos |
| Reinício D | **Snapshot das 24 tabelas e 10 arquivos idênticos**; imagem pública 200, social Carla 200, visitante 401 | Comparação feita antes de novos logins; nenhum reset de dado alterado |
| PlantUML 1.2025.2 | `-checkonly` integral/resumo/GoF: exit 0; render SVG de resumo/GoF: exit 0 | Nenhuma classe mudou; não foi criado diagrama da Entrega 09. Inspeção visual nova desses SVGs não foi registrada |

Logs: [build](evidencias/entrega08/maven-build.txt),
[MySQL](evidencias/entrega08/maven-mysql.txt),
[20 suítes e totais](evidencias/entrega08/suites.json),
[468 casos extraídos dos XML](evidencias/entrega08/casos-automatizados.json),
[requisições/respostas sanitizadas](evidencias/entrega08/http.jsonl),
[assertivas](evidencias/entrega08/assertivas.jsonl),
[IDs efetivos](evidencias/entrega08/resumo.json),
[reinício](evidencias/entrega08/reinicio.txt),
[UML](evidencias/entrega08/uml-check.txt).

Logs publicados em UTF-8, com espaços finais de linha removidos; resultados,
horários, comandos Maven e mensagens foram preservados. XML brutos e dumps
ficaram no ambiente local; o extrato versionado omite propriedades de processo.

**42 de 45 casos EXECUTADO / APROVADO**, com modalidade explicitada
[caso a caso](EXECUCOES_VALIDACAO_08.md). A maior parte tem execução automática,
não ensaio manual integral. **V26, V44 e V45 permanecem NÃO EXECUTADO quanto ao
caso completo**, embora haja verificação parcial em V26/V44. Nenhum caso foi
reprovado por defeito confirmado da aplicação; três falhas do roteiro de teste
não foram ocultadas nem convertidas em aprovação retroativa.

## Fluxo P0 efetivamente comprovado

1. Visitante recebeu início, catálogo, filtros/detalhe/lineup e metadados da
   comunidade. Feed/chat/anúncios privados retornaram 401. Link da postagem
   para visitante exibiu apenas metadados e aviso de login no navegador.
2. Foram cadastrados um participante e um organizador novos, com dados fictícios;
   login dos três perfis em sessões independentes, senha incorreta/inativo
   bloqueados. Cookie HttpOnly; logout 204 e tentativa com cookie revogado 401.
3. Organizador novo criou **evento 5**, artistas 1/3, duas categorias e
   **comunidade 5** via Facade. P comprou a última Pista: **compra 7 / ingresso 8**,
   R$ 25,50, pagamento simulado, estoque 1→0. Nova compra, evento passado e
   organizador inativo retornaram 409.
4. Edição futura atualizou o nome/data do catálogo e manteve histórico original.
   Categoria vendida retornou 409; VIP sem venda foi editada e Extra adicionada.
   Data passada retornou 422; organizador terceiro recebeu 403.
5. Entrada foi manual; sair bloqueou o social até reentrada. P publicou
   **postagem 3 somente com imagem**, chat texto/imagem e resposta local;
   resposta de outra comunidade e reutilização de arquivo foram negadas.
   Curtida repetida conservou contagem 1, comentário foi persistido.
6. Anunciar não removeu o acesso. **Anúncio 5** foi cancelado e preservado;
   reanúncio **6** foi comprado por Carla por R$ 29,00. O **mesmo ingresso 8**
   mudou de titular; P recebeu 404 no ingresso e 403 no social/imagem. Carla
   recebeu 403 antes de entrar manualmente e acesso depois. A compra original
   permaneceu com P. Outros ingressos elegíveis e disputas foram testados em I.
7. O upload PNG público por HTTP e pelo formulário de gestão persistiu uma
   referência atual; visitante leu a imagem. A URL privada retornou 401 para
   visitante e 403 para suporte/ex-membro. JPEG/WebP e contexto/bytes/limites
   foram exercitados nos testes I, não em todos os formulários do navegador.
8. P/O abriram **chamados 4/5**. Suporte Um associou; Um e Dois responderam com
   autoria real. Retorno passou a em_atendimento, nova resposta a
   aguardando_usuario e resolução a fechado. Encerramento em aguardando e
   mensagens em fechado retornaram 409. A captura mostra ambos os autores.
9. Painel do vendedor: 0 ingressos e 1 compra histórica; organizador novo:
   somente 1 evento e R$ 25,50 de compra direta histórica. Revenda não aumentou
   vendas diretas. Suporte apresentou autoria própria separada da associação.
   Parâmetro extra de ID não trocou o escopo do painel.
10. Reinício manteve contas, comércio, conteúdo e imagem enviados/alterados.
    As guardas de base populada sem marcador e rollback do seed ocorreram em I,
    por `ZDemoMySqlIT`, sem interferir em D.

As etapas HTTP, inspeções de navegador e regressão são provas complementares.
Não foi executado um ensaio manual integral cronometrado com quatro navegadores
independentes, nem arguição com os estudantes. Não chamar essa frente de “100%
E2E manual” ou “100% homologado”.

## Capturas e limites da inspeção visual

- [Ingresso atual e snapshot](evidencias/entrega08/participante-ingressos.png),
  [feed somente-imagem](evidencias/entrega08/participante-feed.png),
  [chat e HTML literal](evidencias/entrega08/participante-chat.png).
- [Gestão/categorias](evidencias/entrega08/organizador-gestao.png),
  [painel do organizador](evidencias/entrega08/organizador-painel.png),
  [evento público após reinício](evidencias/entrega08/visitante-evento.png).
- [Dois autores no histórico](evidencias/entrega08/suporte-autoria.png),
  [painel suporte](evidencias/entrega08/suporte-painel.png).
- Viewport **390×844**, sem dispositivo físico nem leitor de tela:
  [suporte](evidencias/entrega08/suporte-mobile.png),
  [conta](evidencias/entrega08/conta-mobile.png),
  [visitante/metadados](evidencias/entrega08/visitante-mobile.png).

Os avisos de permissões e os dados principais ficaram visíveis. Não foram
concluídas as cinco telas em todos os perfis no móvel, navegação completa por
teclado/leitor de tela, nem a negação da Clipboard API para testar seu fallback.
O botão Copiar link exibiu confirmação; a leitura do clipboard pela ferramenta
não retornou conteúdo verificável. V26 continua parcial, sem certificar a cópia
real nem o fallback. Nenhuma captura é usada como medição de status HTTP.

## Achados, correções e decisões de escopo

| Achado | Reprodução / decisão |
| --- | --- |
| README mandava usar branch antiga da 06 | Corrigido para main com 00–07 integradas, schema→migração→seed no runner, distinção Surefire/Failsafe e referência às evidências atuais |
| Multipart do cliente PowerShell foi recusado (400) | `-Form` não produz o subconjunto estrito documentado; roteiro passou a montar disposição/MIME/CRLF explícitos. Upload real da UI foi confirmado. Nenhum parser/validação da aplicação afrouxado |
| Roteiro esperava 400 para `painel?id=1` | A aplicação ignora esse parâmetro e mantém identidade da sessão. A assertiva foi corrigida para comparar indicadores; nenhum requisito aprovado passou a aceitar trocar o usuário |
| Roteiro supostamente sem Origin retornou 201 | `WebRequestSession` havia conservado o header anterior. Cliente passou a limpar headers entre requisições. Sem origem **realmente ausente**, a nova execução retornou 403; a criação da tentativa anterior ficou somente na base sacrificial |
| Nome docente/equipe | Preenchidos com informação direta do PO; não inferidos dos slides ou GitHub |
| Código/SQL/GoF | Nenhuma alteração: não foi reproduzido defeito funcional que justificasse correção. Os quatro GoF foram preservados, sem padrões adicionais |

[Registro sanitizado das tentativas](evidencias/entrega08/tentativas-roteiro.json).
Não houve retry automático de compra/POST nem remoção de teste para obter verde.
N+1, ciclo social/mídia, idempotência de POST, autor de fechamento sem mensagem,
filesystem/MySQL sem transação distribuída, quotas/TLS/carga permanecem limites
conhecidos. A remoção do ciclo não foi iniciada (Entrega 10 depende de prioridade).

## Quatro GoF e rubrica oficial

| Critério | Peso | Verificação / pendência |
| --- | ---: | --- |
| GoF, pertinência e qualidade | 4,0 | Singleton: 16 testes de configuração; Factory Method: três produtos persistidos; Template Method: sequência real/login três perfis; Facade: criação integrada e rollback em três etapas no MySQL. Seção 3.5 do relatório revisada: 3 criacionais + 6 estruturais + 10 comportamentais não usados, total 19. MVC não foi contado como GoF; pertinência e qualidade argumentativa continuam sujeitas à avaliação docente |
| Código/qualidade | 2,0 | Instalação nova, JAR, 468 testes e P0 HTTP/navegador; sem mudança funcional. Não certifica produção |
| Apresentação/arguição coletiva | 2,0 | **PENDENTE DE PO/EQUIPE**: não houve rodízio de perguntas nem ensaio coletivo/cronometrado |
| UML de classes | 1,0 | Produção idêntica à origem do inventário 123 tipos/117 fontes; checkonly aprovado. Melhorias pedagógicas da Entrega 09 aguardam revisão desta entrega |
| Relatório | 1,0 | Identificação de cinco integrantes preenchida, 23/23 GoF analisados e reflexão técnica preservada; experiências individuais e duração autorizada pendentes |

**Equipe fornecida pelo PO:** Eric Julio da Silva; Matheus Martins; João Pedro
Oliveira; Pedro Henrique Rebechi; Kaun de Moraes de Liz. **PUCPR**, **4A noite**,
docente **Andeza de Sousa Vieira**, grafia informada pelo PO em 12/10/2026.
O enunciado/síntese distingue “até seis” de “seis” no cronograma: foram informados
cinco integrantes; o formato deve ser confirmado, sem inventar sexto estudante.
Entrega **15/10/2026**, apresentação **16/10/2026**; 13 minutos são proposta,
não duração autorizada. Pesos são rubrica, não estimativa de nota.

## Comandos e CI

Executados, com JDK 21/Maven 3.9.9 configurados no PATH:

```powershell
git fetch origin
git switch main
git pull --ff-only origin main
git switch -c chore/08-validacao-final-ra2
git archive --format=zip --output=<checkout.zip> 011ecb2dcdd8f9457d2e1c5eeceb00edee018ffd
# Expand-Archive em diretórios novos; mysqld --initialize-insecure em instâncias locais de fixture.
mysql --no-defaults --host=127.0.0.1 --port=3315 --user=root --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/schema_v0.2.sql"
mysql --no-defaults --host=127.0.0.1 --port=3315 --user=root --default-character-set=utf8mb4 --show-warnings --execute="source src/main/resources/db/migration_06_midia_demo.sql"
mvn --batch-mode --no-transfer-progress clean verify
java -jar target/joinup-poo.jar
# HTTP: instância D 3315; script versionado, 258 assertivas, sem retries.
# I: outro checkout limpo e instâncias 3311/3312:
mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify
java -jar plantuml-1.2025.2.jar -checkonly docs/ra2/DIAGRAMA_CLASSES_GERAL.puml docs/ra2/VISAO_ARQUITETURA.puml docs/diagramas/CLASSES_GOF_v0.1.puml
java -jar plantuml-1.2025.2.jar -tsvg -o <diretorio-externo> docs/ra2/VISAO_ARQUITETURA.puml docs/diagramas/CLASSES_GOF_v0.1.puml
```

Credenciais locais de fixture não estão nos logs publicados; configuração DML
foi feita privadamente. A conta administrativa serviu apenas ao preparo e às
fixtures I. O dump de dados ficou local; publicou-se só digest do snapshot.

[CI da main testada, run 38258173398](https://github.com/ericjs-code/JoinUp-POO/actions/runs/38258173398):
`completed/success` em `011ecb2`, conferido diretamente pela API do GitHub.
O gate anterior citado no Prompt 08, run 38252383206, também estava
`completed/success`. [Extrato](evidencias/entrega08/ci-main.json).
O CI do **head do PR** deve ser conferido separadamente; resultado final e URL
do run são entregues no fechamento/PR, sem atribuir um CI histórico ao novo SHA.

## Decisão para auditoria

Liberar revisão técnica deste PR com as evidências atuais. O diff altera
documentação, identificação, script de homologação e evidências; fonte Java,
testes Java, frontend, schema, seed, migração, pom e quatro GoF permanecem iguais.

Antes do aceite acadêmico integral: confirmar formato/equipe e duração,
realizar o roteiro cronometrado com contextos de navegador independentes,
arguição coletiva e reflexões dos estudantes, completar V26/V44/V45 e registrar
evidências. **Não fazer merge neste trabalho.** A Entrega 09 terá branch/PR
próprios depois da revisão da 08; a 10 exige nova decisão de prioridade.
