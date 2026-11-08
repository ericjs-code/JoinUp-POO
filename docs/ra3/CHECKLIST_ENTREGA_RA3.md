# JoinUp-POO — Checklist final para Canvas e apresentação RA3

Conforme o enunciado transcrito disponibilizado: Canvas em **12/11/2026**;
apresentação/arguição em **13/11/2026**; equipes de até seis pessoas.
Implementação vale 8/10, diagrama 1/10 e relatório 1/10. MVC/três camadas é
preferencial; o projeto usa camadas. Os PDFs originais de aula não foram
declarados lidos nesta entrega. Os itens abaixo são para conferência final
do aluno; resultados técnicos estão na [validação 04](VALIDACAO_ENTREGA_04.md).

## Conferência dos entregáveis

- [ ] Abrir o [diagrama SVG](DIAGRAMA_CLASSES_RA3.svg), conferir legibilidade em
  uma página e as sete @Entity. Manter o [PlantUML editável](DIAGRAMA_CLASSES_RA3.puml)
  junto do material. Conferir as seis associações, FKs e multiplicidades.
- [ ] Confirmar que o código-fonte Java RA3 está disponível na revisão final
  aprovada do repositório, sem credenciais pessoais, caches, target ou JARs gigantes.
- [ ] Revisar o [relatório](RELATORIO_ACADEMICO_RA3.md): somente os dois tópicos
  exigidos, reflexões confirmadas/ajustadas pelos integrantes e identificação
  real da equipe. Retirar a nota editorial da cópia final após a validação humana.
- [ ] Demonstrar instalação/configuração no computador da apresentação e
  reproduzir `mvn -Pra3 compile exec:java` conforme o [guia](EXECUCAO_RA3.md).
  A execução em worktree com ferramentas existentes não comprova instalação
  em computador novo.
- [ ] Confirmar `mvn clean test`, `mvn -Pra3 clean test` e
  `mvn '-Pra3,ra3-it' verify` em **instância descartável**. Registrar SHA e
  resultado real; manter a demonstração em instância separada.
- [ ] Conferir os workflows RA3 e Java original concluídos no **SHA final do PR**;
  checks de um commit anterior não aprovam automaticamente outro head.
- [ ] Conferir [persistence.xml](../../src/main/resources/META-INF/persistence.xml),
  [orm-ra3.xml](../../src/main/resources/META-INF/orm-ra3.xml), EclipseLink 4.0.4,
  API 3.1.0, schema manual e [matriz CRUD 7×4](ENTREGA_03_HOMOLOGACAO.md).
- [ ] Revisar o PR documental e concluir a decisão humana de integração.
  Este trabalho não realiza merge automático.

## Anexos e revisão antes do envio

| Entregável do professor | Arquivo/revisão a preparar |
| --- | --- |
| Diagrama de classes | DIAGRAMA_CLASSES_RA3.svg; incluir .puml como fonte editável se o Canvas permitir. |
| Relatório com reflexões | Cópia final de RELATORIO_ACADEMICO_RA3.md, identificada e validada pelos integrantes; se exportada para PDF, abrir e conferir todas as páginas antes do envio. |
| Código-fonte | Revisão final aprovada de JoinUp-POO: URL e SHA, mais arquivo de fontes caso o campo de entrega do Canvas exija anexo. Um link isolado pode não atender ao formato escolhido pelo professor. |

- [ ] Conferir diretamente no Canvas os formatos/campos aceitos e eventuais
  orientações adicionais. Nenhum formato de anexo não transcrito é presumido aqui.
- [ ] Se for necessário ZIP de fontes, usar `git archive` da revisão aprovada,
  inspecionar seu conteúdo e incluir instruções de execução. Não anexar arquivos
  locais não versionados, `.env`, dados reais, ferramentas ou banco de dados.
- [ ] Confirmar identificação, turma e prazo **12/11/2026**; realizar pessoalmente
  o envio e conferir o comprovante/arquivos disponíveis no Canvas.
- [ ] Ensaiar a [apresentação](ROTEIRO_APRESENTACAO_RA3.md), anotar os IDs reais
  da demonstração e preparar toda a equipe para a arguição em **13/11/2026**.

O roteiro e a validação são materiais de apoio; não acrescentam seções ao
relatório exigido. **Envio ao Canvas, validação pessoal das reflexões e ensaio
presencial permanecem pendentes.** Não foi iniciada integração com o Canvas.
