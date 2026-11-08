# RA3 — Entrega 04: validação documental e rastreabilidade

Data: **08/11/2026**, America/Sao_Paulo. Escopo: UML, relatório acadêmico e
preparação da apresentação/entrega. Base efetiva obtida por `git fetch origin main`:
**a835c2e41b10606f75d425aba2bc4fbea9f11812**. Branch:
**docs/ra3-04-uml-relatorio-apresentacao**, em worktree separado do checkout
do usuário. Commit final e PR constam do histórico Git e do PR documental.

## Gate e fontes examinadas

O [PR #15](https://github.com/ericjs-code/JoinUp-POO/pull/15) foi consultado antes
da edição: `merged=true`, integrado em 08/11/2026 às **21:21:11 UTC (18:21:11 −03)**,
head `9664e31394ca30d901d5e61c8fa5df14c313c887`. Seus workflows associados ao
head estavam `completed/success`: [RA3 42524319355](https://github.com/ericjs-code/JoinUp-POO/actions/runs/42524319355)
e [Java 42524319487](https://github.com/ericjs-code/JoinUp-POO/actions/runs/42524319487).
O commit de implementação auditado `fef7f7830ad401e62692eb8b5def279e8dd42cc0`
está incluído na base integrada. Esta entrega não executou merge.

Foram confrontados com a documentação: as sete entidades e ValidacaoModelo;
PersistenciaRa3, ConfiguracaoRa3, UnidadeTrabalhoRa3, DataHoraUtcConverter;
ServicoCrudRa3, DadosRa3, TipoRegistro, OperacoesRa3; AplicacaoRa3, MenuRa3 e
Ra3Main; persistence.xml/orm-ra3.xml; schema_ra3.sql/verificar_schema_ra3.sql;
pom/perfis e workflows ra3.yml/java.yml; testes RA3, especialmente CRUD,
relacionamentos, schema, menu e ponto de entrada; resultados 00/01/02,
homologação 03, guia e evidências CLI/testes. O diagrama RA2 foi lido apenas
como contexto e não foi reutilizado.

O enunciado utilizado é a **transcrição fornecida pelo usuário**. Não foi
declarada leitura dos PDFs, imagem, RAR ou ZIP de aula inacessíveis nesta execução.
Não se atribuem ao professor requisitos novos de duração, formato de anexo,
exclusão física ou MVC obrigatório.

## Diagrama: código, JPA e SQL

Fonte: [PlantUML](DIAGRAMA_CLASSES_RA3.puml). Exportação real:
[SVG](DIAGRAMA_CLASSES_RA3.svg). Inspeção visual:
[PNG produzido pelo mesmo renderizador](evidencias/diagrama-classes-ra3.png).
As sete declarações de classe correspondem às sete @Entity e às sete entradas
de persistence.xml; todos os atributos de instância do modelo e os mutadores
públicos exibidos foram confrontados com os arquivos Java. Construtores,
getters e a constante estática de UFs são omitidos para legibilidade.

| Associação no UML (pai → filho) | Lado proprietário e FK | JPA / multiplicidade / SQL |
| --- | --- | --- |
| Organizador 1 → 0..* Evento | Evento.organizador / eventos.id_organizador | ManyToOne, optional=false, nullable=false; FK para organizadores.id_organizador. |
| GeneroMusical 1 → 0..* Evento | Evento.generoMusical / eventos.id_genero_musical | ManyToOne obrigatório; FK para generos_musicais.id_genero_musical. |
| Evento 1 → 0..* TipoIngresso | TipoIngresso.evento / tipos_ingressos.id_evento | ManyToOne obrigatório; Evento.tiposIngressos é OneToMany(mappedBy="evento"). |
| Evento 1 → 0..1 Comunidade | Comunidade.evento / comunidades.id_evento | OneToOne obrigatório na comunidade; JoinColumn unique=true e uk_comunidade_evento no SQL. Evento pode não ter comunidade e não tem atributo inverso comunidade. |
| Participante 1 → 0..* Postagem | Postagem.participante / postagens.id_participante | ManyToOne obrigatório; FK para participantes.id_participante. |
| Comunidade 1 → 0..* Postagem | Postagem.comunidade / postagens.id_comunidade | ManyToOne obrigatório; FK para comunidades.id_comunidade. |

Todas as PKs são Integer/IDENTITY. As seis associações proprietárias e a coleção
inversa usam EAGER explícito; não há cascade JPA PERSIST/REMOVE ou orphanRemoval.
Os CASCADEs SQL de comunidades→eventos e postagens→comunidades permanecem no
DDL, mas o serviço protege pais com filhos. Não são composição no UML.
O conversor UTC é registrado no orm-ra3.xml, sem constituir oitava entidade.
Participante.nascimentoIso é String/CHAR(14), com getter LocalDate; estado é
String/ENUM; valor é BigDecimal/DECIMAL(8,2).

Ferramentas reutilizadas fora do repositório: **PlantUML 1.2025.2**, Graphviz
**2.44.1** e JDK **21.0.12.1**. Comandos executados com os caminhos locais
equivalentes a:

```powershell
java -jar C:\Ferramentas\plantuml-1.2025.2.jar -version
java -jar C:\Ferramentas\plantuml-1.2025.2.jar -charset UTF-8 -checkonly docs/ra3/DIAGRAMA_CLASSES_RA3.puml
java -jar C:\Ferramentas\plantuml-1.2025.2.jar -charset UTF-8 -tsvg docs/ra3/DIAGRAMA_CLASSES_RA3.puml
java -jar C:\Ferramentas\plantuml-1.2025.2.jar -charset UTF-8 -tpng docs/ra3/DIAGRAMA_CLASSES_RA3.puml
python docs/ra3/scripts/validar-documentacao-ra3.py
git diff --check
```

O PNG de evidência foi gerado em diretório temporário e copiado para evidências;
o SVG está ao lado da fonte. Sintaxe e exportações concluíram com exit 0.
O SVG foi analisado como XML, sem tela de erro, com todas as entidades presentes.
A inspeção do PNG confirmou nomes, atributos, operações, seis associações,
multiplicidades e legenda legíveis, sem cortes ou cruzamentos de associações.
O layout é horizontal; para impressão, conferir ajuste a uma página em paisagem.
Não foi criado .drawio. O relatório permanece Markdown editável; não foi
produzido PDF nesta entrega. Uma eventual exportação final deve ocorrer após
validação/identificação da equipe e ser aberta e conferida antes do envio.

## Relatório e reflexão humana pendente

O [relatório](RELATORIO_ACADEMICO_RA3.md) possui somente os dois tópicos
solicitados. A nota editorial antecede o corpo, identifica o texto-base e pede
validação dos integrantes, sem inventar suas experiências ou identidades.
Foram selecionadas quatro reflexões:

| Reflexão | Evidência documental e confirmação no código/teste |
| --- | --- |
| Preservar a RA2 ao adotar JPA | [Resultados 00](ENTREGA_00_RESULTADOS.md), perfil ra3 no pom e pacotes próprios; build padrão exclui RA3/JPA. |
| Cardinalidades, FK UNIQUE e tipos SQL | [Resultados 01](ENTREGA_01_RESULTADOS.md); anotações das entidades, DDL, RelacionamentosMySqlIT e SchemaRa3MySqlIT. |
| Deslocamento temporal de três horas | Dificuldade registrada nos resultados 01; DataHoraUtcConverter e propriedades UTC; teste confronta LocalDateTime físico por JDBC independente. Não se afirma ter reproduzido novamente o defeito antigo. |
| Transações, exclusão protegida e concorrência | [Resultados 02](ENTREGA_02_RESULTADOS.md), [homologação 03](ENTREGA_03_HOMOLOGACAO.md); UnidadeTrabalhoRa3/ServicoCrudRa3 e testes de rollback após flush, pais com dependentes e disputas de FK. |

Os integrantes ainda devem confirmar o que vivenciaram, ajustar a redação e
preencher nomes/identificação/turma. A verificação técnica não substitui essa
revisão humana. Cancelamento de formulário e correção dos testes que presumiam
banco vazio constam das evidências e do roteiro/guia, sem ampliar o relatório
para um histórico de sprints.

## Verificações locais da Entrega 04

Ambiente: Windows, JDK **21.0.12.1**, Maven **3.9.9**, MySQL **8.4.0**.
Ferramentas/cache já disponíveis; não é simulação de máquina nova.
Nova instância descartável em loopback **64211**, datadir temporário exclusivo,
sem instalar serviço e sem usar as instâncias de demonstração/RA2. Aplicação
administrativa manual do schema; conta efêmera com somente DML no catálogo RA3.
Segredos foram mantidos apenas no ambiente dos processos e não estão versionados.

| Verificação efetivamente executada | Resultado |
| --- | --- |
| `mvn clean test` | exit 0; 167 testes, zero falhas/erros/skips. |
| `mvn -Pra3 clean test` | exit 0; 268 testes, zero falhas/erros/skips. |
| `mvn '-Pra3,ra3-it' verify` | exit 0; 268 Surefire + 42 Failsafe, zero falhas/erros/skips. |
| `mvn -Pra3 compile exec:java '-Dexec.args=--diagnostico'` | exit 0; RA3_CONEXAO_OK na instância descartável. |
| Inspeção SQL / schema nos testes | Sete tabelas, seis FKs, cinco CHECKs, índices/defaults/tipos esperados. |
| Matriz CRUD 7×4 | 28 marcadores reais CRIAR/CONSULTAR/ATUALIZAR/EXCLUIR no log integrado, além de vínculos, rollback e concorrência. |
| Consulta SQL independente após suíte e diagnóstico | Sete tabelas com zero registros remanescentes. |
| Encerramento da instância temporária | mysqladmin shutdown concluído; processo encerrado. |
| PlantUML + XML SVG + conferência documental | Fonte/exportações reais; atributos, métodos, FKs e links locais conferidos pelo script e por inspeção. |

Trechos de saída e hashes dos logs locais completos:
[validacao-04.txt](evidencias/validacao-04.txt). Logs completos e ferramentas
permanecem fora do Git. A [matriz 7×4](ENTREGA_03_HOMOLOGACAO.md) e a
[sessão CLI 03](evidencias/sessao-cli-03.txt) foram mantidas como evidências
históricas. Não houve nova sessão completa de demonstração nem ensaio presencial
da equipe nesta entrega. O perfil JDBC `mysql-it` não foi executado localmente;
a regressão integrada RA2 deve ser observada no workflow Java do PR atual.

## CI, escopo e encerramento

O sucesso do PR #15 foi usado somente no gate. Os checks da Entrega 04 devem
ser consultados por seu próprio SHA: o resultado final por workflow/run é
registrado na descrição do PR documental e na resposta de encerramento, depois
do push e da conclusão dos jobs. Essa distinção evita atribuir a um novo head
os resultados de um commit anterior. Não há contagens de CI presumidas aqui.

Nenhuma alteração em classes, testes, modelos, pom, SQL, workflows ou arquivos
RA2. O diff desta entrega se limita a docs/ra3. O checkout original conserva
as duas modificações Java e os três arquivos não versionados preexistentes.
Não foi identificado novo defeito funcional durante estas verificações.

O [checklist](CHECKLIST_ENTREGA_RA3.md) mantém as ações humanas pendentes:
revisão das reflexões/identificação, decisão sobre integração, conferência dos
anexos aceitos pelo Canvas, submissão em **12/11/2026** e preparação da equipe
para **13/11/2026**. O [roteiro](ROTEIRO_APRESENTACAO_RA3.md) usa IDs gerados,
dados fictícios, diagnóstico e contingência, sem exigir duração não prevista.
Não houve envio ao Canvas, acesso a contas acadêmicas, merge ou início de outra entrega.
