# Entrega 05 — JavaFX mínimo

Data local: 08/11/2026 (America/Sao_Paulo). Base: `origin/main`
`6b3776d`, com PRs [12](https://github.com/ericjs-code/JoinUp-POO/pull/12),
[13](https://github.com/ericjs-code/JoinUp-POO/pull/13),
[14](https://github.com/ericjs-code/JoinUp-POO/pull/14),
[15](https://github.com/ericjs-code/JoinUp-POO/pull/15) e
[16](https://github.com/ericjs-code/JoinUp-POO/pull/16) consultados e integrados.
Branch: `feat/ra3-05-javafx-simples`, em worktree separado. Os arquivos locais
não versionados `COMO_EXECUTAR_RA3.md`, `Iniciar-RA3.cmd` e `Iniciar-RA3.ps1`
do checkout original foram preservados.

## Implementação

Uma janela `JoinUp-POO RA3`, ComboBox com sete entidades, TableView ID/Resumo,
páginas de até 20 registros por ID, Anterior/Próxima e seis botões de operação.
Dialogs/Alerts padrão, sem FXML, CSS, navegador ou novas regras de domínio.
Quando uma página cheia é a última, Próxima consulta a página seguinte e retorna
à anterior se ela estiver vazia. A troca de entidade retorna à primeira página.

`Ra3FxMain` abre uma fábrica por execução com `ConfiguracaoRa3`/`PersistenciaRa3`,
verifica a primeira leitura e fecha a fábrica em `stop()`. Cada CRUD usa somente
os cinco métodos de `ServicoCrudRa3`; não há SQL ou segundo CRUD na interface.
O formulário mostra valores atuais como resumo e solicita os novos valores,
sem tentar decompor o resumo textual nem alterar o contrato do serviço.
Relações e identidades ficam fora dos campos de atualização, como no CLI.
Cancelar não envia comandos; validação preserva o diálogo; exclusão envia SIM
somente após Yes. Operações são síncronas nesta interface acadêmica mínima;
a janela pode aguardar os timeouts do banco durante indisponibilidade.

`FormularioRa3` descreve campos e converte entradas sem importar JavaFX.
Os comandos imutáveis `DadosRa3` continuam validando os valores. O perfil
opt-in `ra3-gui`, usado junto de `ra3`, contém a única dependência direta
`javafx-controls:21.0.2` e `javafx-maven-plugin:0.0.8`; build-helper adiciona
somente as fontes gráficas. A saída `target/ra3-gui` evita classes residuais
JavaFX em `target/ra3`. O workflow RA3 compila a GUI sem iniciar display.

## Execução real e resultados

Windows, JDK 21.0.12.1, Maven 3.9.9, MySQL 8.4.0. Ferramentas já instaladas;
não se simulou computador novo. Maven executado pelo caminho absoluto da
instalação local, com JAVA_HOME apontando ao JDK 21. Argumentos abaixo foram
executados com `--batch-mode --no-transfer-progress`:

| Comando | Resultado |
| --- | --- |
| `mvn clean test` | Exit 0; 167 testes; 0 falhas/erros/ignorados. |
| `mvn -Pra3 clean test` | Exit 0; 304 testes; 0 falhas/erros/ignorados. |
| `mvn '-Pra3,ra3-it' verify` | Exit 0; 304 Surefire + 42 Failsafe; 0 falhas/erros/ignorados. |
| `mvn '-Pra3,ra3-gui' compile` | Exit 0; compilação real com release 21. |
| `mvn -Pra3 compile exec:java` | Exit 0; menu exibiu sete entidades, entrada `0` encerrou. |
| `mvn -Pra3 compile exec:java -Dexec.args=--diagnostico` | Exit 0; RA3_CONEXAO_OK. |
| `mvn '-Pra3,ra3-gui' javafx:run` | Janela Windows aberta; sessões completas encerradas por Sair com BUILD SUCCESS/exit 0. |
| `mvn dependency:tree '-Dincludes=org.openjfx:*'` | Exit 0; nenhuma dependência JavaFX. |
| `mvn -Pra3 dependency:tree '-Dincludes=org.openjfx:*'` | Exit 0; nenhuma dependência JavaFX. |

Os 36 testes novos cobrem sete cadastros e sete alterações com somente os campos
exibidos, formulários vazios, confirmação, IDs/integer overflow, preço com ponto,
zero monetário/quantidades, data UTC, ano fora do intervalo, nascimento e opcionais.
Eles rodam sem toolkit gráfico no perfil `ra3`. Os testes anteriores continuam
incluindo CRUD 7×4, restrições, rollback, concorrência e paginação real.

MySQL novo descartável, loopback porta 65521, schema aplicado manualmente e conta
DML exclusiva de `joinup_poo_ra3`; nenhum comando foi executado no banco RA2.
Consulta JDBC/SQL final independente confirmou sete tabelas com zero registros.
Instância encerrada ao terminar. Credenciais temporárias ficaram somente nos
processos locais; não foram versionadas.

## Demonstração gráfica observada

Verificação por automação de interface Windows com screenshots e árvore de
acessibilidade, não por um estudante operando presencialmente:

1. Janela clara com tabela vazia, títulos e botões previstos.
2. Cadastro `GUI-05-genero`: mensagem de sucesso, ID automático **59**, recarga
   mostrando o registro na tabela.
3. Consultar ID **59**: Alert mostrou `GUI-05-genero`.
4. Atualizar ID selecionado: diálogo mostrou resumo atual; nome alterado para
   `GUI-05-atualizado`, confirmação e recarga com novo valor.
5. Sair normalmente e iniciar outra JVM por `javafx:run`: tabela mostrou ID **59**
   e `GUI-05-atualizado`, comprovando persistência após reinício.
6. Excluir ID selecionado: confirmação **Sim/Não**, Sim, mensagem `Registro
   excluído. ID: 59` e tabela vazia.
7. ComboBox aberto mostrou GeneroMusical, Organizador, Participante, Evento,
   TipoIngresso, Comunidade e Postagem. Troca para Evento recarregou a tabela.
8. Formulário Evento exibiu campos de criação, IDs de organizador/gênero e
   rolagem. Enviar vazio mostrou data UTC inválida; após OK o formulário continuou
   aberto. Cancelar retornou à tabela sem gravar.
9. Sair fechou a janela e encerrou a execução Maven normalmente.

A primeira inspeção encontrou diálogo de gênero alto demais; a altura passou
a depender da quantidade de campos e foi reinspecionada antes da demonstração
acima. A ferramenta Windows precisou recuperar o cache de acessibilidade entre
diálogos; suas falhas de controle não foram contadas como operações aprovadas.

## Limites e auditoria

- CRUD gráfico completo das outras seis entidades: **NÃO TESTADO visualmente**.
  Cobertura: conversão dos 14 comandos em testes sem display, reutilização do
  serviço sem alteração e os testes MySQL existentes para todas as entidades.
- ID ausente, Não na exclusão, paginação acima de 20 linhas e falhas de MySQL/
  variáveis na GUI: **NÃO TESTADO visualmente**. Os caminhos foram inspecionados;
  validações/serviço/CLI mantêm seus testes, sem declarar ensaio gráfico desses casos.
- Integração JDBC RA2 local não executada nesta entrega; os 167 unitários RA2
  passaram. CI e ensaio presencial dos integrantes são evidências separadas.
- Maven emitiu aviso de seis problemas no modelo efetivo das dependências
  OpenJFX 21.0.2. Compilação e execução gráfica concluíram; não houve uso de FXML.
- Entidades, XMLs JPA, DDL, cardinalidades, regras de exclusão e classes RA2
  permanecem sem mudanças. Relatório recebeu apenas menção breve à GUI;
  reflexões dos integrantes e UML ORM preservados.
- **ZIP anterior desatualizado.** Novo pacote somente após revisão e integração,
  com código aprovado e Word/draw.io conferidos, excluindo prompts, `.git`,
  temporários, logs e ferramentas. Nenhum novo ZIP final foi produzido.
- Abrir PR para auditoria, sem merge e sem avançar para outra etapa.

Resumo sanitizado e hashes dos logs locais: [evidências 05](evidencias/validacao-05.txt).
