# RA3 — Entrega 01: sete entidades e relacionamentos

Data: 08/11/2026, America/Sao_Paulo. Base realmente obtida por `git fetch origin
main`: **15813887c5709e6f227bcc434bff464cf1d5ae9d**, merge do PR #12.
Branch nova: `feat/ra3-01-entidades-relacionamentos`. Implementação em worktree
separada para preservar as alterações locais anteriores do usuário. O histórico
Git e o PR associado identificam o commit exato desta entrega; não há merge
automático nem autorização para iniciar a Entrega 02.

## Inspeção e continuidade

Foram conferidos as ADRs 001/002, os resultados e guia da Entrega 00, pom.xml,
os dois workflows, persistence.xml, schema_ra3.sql e todas as classes RA3.
O schema_v0.2.sql foi lido como referência, não executado. O diagrama conceitual
RA2 continua descrevendo cardinalidades SQL/projeções, sem substituir um modelo
de entidades JPA. Não foi alterada sua representação histórica.

A consulta atual do workflow histórico do commit ba07300 da Entrega 00 mostrou
**success**: [run 42499049630](https://github.com/ericjs-code/JoinUp-POO/actions/runs/42499049630).
Esse resultado resolve a pendência citada na auditoria; não é usado como prova
da regressão desta entrega, que foi executada novamente.

Os cinco materiais de aula citados não foram localizados no repositório nem
nos anexos acessíveis. Foram seguidas as decisões técnicas transcritas pelo PO;
não se afirma leitura desses arquivos. Não foram adicionados JARs/ZIPs ao Git.

## Matriz das sete entidades efetivas

Todas pertencem a `br.com.joinup.ra3.modelo`, com @Entity, @Table qualificada
em `joinup_poo_ra3`, PK Integer/IDENTITY, campos privados e construtor protegido
sem argumentos. Não há herança JPA ou oitava entidade. GeneroMusical mantém o
contrato existente, sem inverso adicional desnecessário.

| Entidade / tabela | Atributos-chave | Associações Java efetivas |
| --- | --- | --- |
| GeneroMusical / generos_musicais | id_genero_musical; nome(45) | Referenciada por Evento.generoMusical; sem coleção inversa. |
| Organizador / organizadores | id_organizador; nome(80), email(40) UNIQUE, senha(255), ativo, data_desativacao, cnpj CHAR(18) UNIQUE | Referenciada por Evento.organizador; sem herança do usuário RA2. |
| Participante / participantes | id_participante; cpf CHAR(11) UNIQUE, nome(60), nascimento CHAR(14), email(45) UNIQUE, senha(255), ativo, data_desativacao | Referenciada por Postagem.participante. |
| Evento / eventos | id_evento; nome(45), data DATETIME, descricao(450), imagem(255) opcional, localizacao(45), cidade(60), UF ENUM, cep(8) | ManyToOne Organizador e GeneroMusical; OneToMany tiposIngressos mappedBy="evento". |
| TipoIngresso / tipos_ingressos | id_tipos_ingressos; nome_tipo(45), valor BigDecimal DECIMAL(8,2), quantidades não negativas | ManyToOne Evento, proprietário de id_evento. |
| Comunidade / comunidades | id_comunidade; nome(80), descricao(300) opcional, imagem(255) opcional, data_criacao DATETIME | OneToOne Evento, proprietário de id_evento obrigatório e UNIQUE. |
| Postagem / postagens | id_postagem; descricao(500)/imagem(255) opcionais, curtidas Integer nullable/default 0, data_postagem DATETIME | ManyToOne Participante e Comunidade, ambos obrigatórios. |

Arquivos das entidades: `src/main/java/br/com/joinup/ra3/modelo/`.
Getters e alterações controladas atendem o modelo, sem criar repositórios CRUD,
menu, autenticação ou venda. A coleção de tipos é somente leitura para o
chamador; o construtor de TipoIngresso liga os dois lados após validar atributos.
Não há criação/persistência automática dos pais nem equals/hashCode/toString
recursivos ou serialização de entidades para JSON.

## FKs, UNIQUEs e ressalvas do SQL original

Todas as associações proprietárias são `optional=false`, EAGER e usam
`@JoinColumn(nullable=false)`. Não há cascade PERSIST/REMOVE ou orphanRemoval.

| FK SQL → PK | Mapeamento Java | ON DELETE / ON UPDATE |
| --- | --- | --- |
| eventos.id_organizador → organizadores.id_organizador | Evento.organizador: ManyToOne | NO ACTION / NO ACTION |
| eventos.id_genero_musical → generos_musicais.id_genero_musical | Evento.generoMusical: ManyToOne | NO ACTION / NO ACTION |
| tipos_ingressos.id_evento → eventos.id_evento | TipoIngresso.evento: ManyToOne; inverso Evento.tiposIngressos mappedBy | NO ACTION / NO ACTION |
| comunidades.id_evento → eventos.id_evento | Comunidade.evento: OneToOne, JoinColumn unique=true | CASCADE / CASCADE |
| postagens.id_participante → participantes.id_participante | Postagem.participante: ManyToOne | NO ACTION / NO ACTION |
| postagens.id_comunidade → comunidades.id_comunidade | Postagem.comunidade: ManyToOne | CASCADE / CASCADE |

UNIQUE de organizadores.email/cnpj e participantes.cpf/email são expressos
também em @Column(unique=true). O de comunidade.evento está em @JoinColumn.
PKs são únicas por definição; o índice SQL redundante `uk_id_evento` foi
preservado, sem inventar outra regra Java. Todos os índices/FKs/CHECKs das sete
tabelas foram copiados dos blocos originais, com ordem válida de criação.

O SQL permite Evento com **zero ou uma** Comunidade e zero tipos; o teste grava
explicitamente um evento sem comunidade/tipos. A obrigatoriedade de Evento é
do lado Comunidade, não uma exigência de criar Comunidade para todo Evento.
Organizador/gênero podem ter zero ou muitos eventos; participante/comunidade
podem ter zero ou muitas postagens. Não foram adicionadas tabelas de filiação,
compra, ingresso emitido, usuário abstrato ou junção de gênero.

Os CASCADEs de banco são preservados conscientemente, mas exclusões funcionais
e atualização do contexto JPA após exclusões em cascata não foram implementadas
ou demonstradas como CRUD neste incremento. A limpeza de fixture respeita FKs
explicitamente. A modalidade de exclusão do enunciado fica para a Entrega 02.

## Compatibilidade de tipos e configuração

Nascimento é String ISO yyyy-MM-dd armazenada no CHAR(14) original, com
construtor/getter LocalDate e remoção controlada do padding na leitura. UF é
String Java validada nas 27 opções; o SQL mantém ENUM. Quantias BigDecimal
respeitam 0..999999.99 e duas casas sem arredondamento implícito. Quantidades
disponível/vendida são não negativas, sem implementar estoque concorrente.
Os nulls originais de texto/imagem/curtidas são preservados; não se importam
regras de acesso, texto obrigatório ou fluxo de negócio da RA2.

Todos os DATETIME representam UTC com precisão de segundos. Foi detectado,
por leitura JDBC independente no Windows UTC−3, deslocamento de três horas na
conversão padrão LocalDateTime→Timestamp. A correção usa DataHoraUtcConverter
com Timestamp.from/ZoneOffset.UTC em cada campo temporal e driver/sessão UTC.
O converter é registrado em META-INF/orm-ra3.xml, sem adicionar outra entidade
às sete entradas de persistence.xml. O perfil padrão exclui esse XML auxiliar.
O teste confirma horário físico e objetos recuperados, sem depender do fuso da JVM.

Com weaving=false, foi escolhido EAGER explícito nas seis associações e na
coleção inversa. Acesso após fechar o EntityManager foi testado. Não há alegação
de LAZY validado; volume/performance de um sistema maior fica fora do escopo.

Versões preservadas: EclipseLink **4.0.4**, uma única Jakarta Persistence API
**3.1.0**, Connector/J **8.4.0**, JUnit **5.11.4**. Descritores XML 3.0 são formato
de metadata, não outra API. RESOURCE_LOCAL, exclude-unlisted-classes=true,
cache NONE e DDL/scripts `none` permanecem. Credenciais exclusivas JOINUP_RA3_*,
validação de URL/catálogo e logs sanitizados continuam ativos. O diagnóstico
Ra3Main permanece sem CRUD e sem INSERT.

## Validação executada

Ambiente local: Windows 11, JDK 21.0.12.1, Maven 3.9.9 e MySQL 8.4.0. Foram
usados executáveis locais por caminho absoluto, não o Java 25 do PATH.
MySQL novo em diretório temporário exclusivo, loopback **54964**, schema aplicado
manualmente antes da suíte; usuário de teste com SELECT/INSERT/UPDATE/DELETE
somente em joinup_poo_ra3. Não foi usada instância com dados definitivos.

| Comando / verificação | Resultado |
| --- | --- |
| mvn clean test | 167 testes originais; sucesso, zero falhas/erros/skips. |
| mvn clean verify | 167 testes originais e JAR padrão; sucesso. |
| Inspeção do JAR e dependency:tree padrão | Sem classes RA3, descriptor, orm-ra3.xml ou dependências EclipseLink/JPA. |
| mvn -Pra3 dependency:tree com filtro | EclipseLink 4.0.4, Jakarta Persistence API 3.1.0 e Connector/J 8.4.0; sucesso. |
| mvn -Pra3 clean test | 214 testes (167 originais + 47 RA3); sucesso sem exigir MySQL. |
| Aplicação manual de schema_ra3.sql e verificar_schema_ra3.sql | Sete tabelas, tipos/defaults/índices e seis FKs preservados. |
| mvn '-Pra3,ra3-it' verify | 214 Surefire + 14 Failsafe; sucesso, zero falhas/erros/skips. |
| mvn -Pra3 exec:java | RA3_CONEXAO_OK; sem HTTP, sem insert, encerramento dos recursos. |
| Consulta JDBC/mysql independente após a suíte | Sete tabelas, sem fixtures restantes. |
| mysqladmin shutdown na porta descartável | Instância encerrada; arquivo temporário de credencial removido. |
| git diff --check e diff de caminhos RA2 protegidos | Sem erro de whitespace ou alteração de classes/SQL/workflow RA2. |

Os 14 cenários reais compreendem: 1 smoke de gênero preservado, 1 verificação
de schema e 12 de grafo/integridade/limpeza. A verificação de schema compara
colunas, tipos, nulabilidade, defaults, identidades e índices com os blocos do
SQL RA2 lido como referência, além de seis FKs e cinco CHECKs. É executada
também antes de criar o grafo; não confia apenas no exit code de SOURCE.

O grafo é persistido sem cascade PERSIST, em transação, e recuperado em outro
EntityManager. JPQL/JOIN confirma multiplicidades, e uma conexão JDBC física
independente une as sete tabelas para conferir IDs, valor, nascimento e datas.
O metamodelo real contém exatamente as sete classes esperadas. O cenário
principal usa dois tipos e duas postagens e um evento adicional sem comunidade.
Na execução final local, os IDs lidos e confirmados por JOIN foram: gênero 38,
organizador 30, participante 30, evento 28, tipo 28, comunidade 28 e postagem 28.
Esses IDs avançaram em execuções anteriores de desenvolvimento e não são
fixtures fixas ou requisito dos testes.

As falhas esperadas exigem PersistenceException com SQLException de código
e constraint específicos, não catch genérico: 1062 para UNIQUE de evento da
comunidade, CPF/email de participante e email/CNPJ de organizador; 1048 para
FK NULL; 1452 para FK inexistente; 3819 para os três CHECKs não negativos.
Uma falha UNIQUE após INSERT+flush de gênero verifica rollback desse INSERT
por JDBC. Consultas independentes confirmam FKs preservadas após as falhas.

Cada execução usa UUID próprio; @AfterEach remove postagens, comunidades,
tipos, eventos, participantes, organizadores e gêneros nessa ordem e verifica
ausência nas sete tabelas por JDBC. Um controle fora do marcador permanece após
essa limpeza e é removido depois especificamente por ID+nome pelo teste que o
criou. EntityManagers/conexões são fechados; a fábrica é reutilizada por classe.
Interrupção abrupta continua podendo deixar fixture identificável; não há reset.

Trechos de logs e hashes: [evidencias/validacao-01.txt](evidencias/validacao-01.txt).
Execução reproduzível: [EXECUCAO_RA3.md](EXECUCAO_RA3.md). Relatórios Maven em
target/ra3/surefire-reports e failsafe-reports. O workflow RA3 aplica/inspeciona
as sete tabelas e publica os relatórios, sem repetir a suíte JDBC. O resultado
de cada CI deste PR deve ser consultado nos checks; os números acima são locais.

## Falhas corrigidas e limites

Durante o desenvolvimento, o comparador de schema inicialmente normalizava
também os literais ENUM, produzindo divergência artificial; foi corrigido para
preservar os valores UF originais. A leitura independente detectou e permitiu
corrigir o deslocamento temporal real descrito acima. Com descoberta restrita,
o EclipseLink exigiu registro explícito do converter via mapping-file; o teste
de bootstrap sem banco cobre esse registro. As execuções finais passaram.

Não há regra de autenticação/hashing nesta entrega: campos senha recebem hash
produzido fora do modelo; fixtures usam marcador fictício sem login. Não foram
adicionadas senhas reais ou em texto puro. EAGER é adequado ao grafo pequeno
demonstrativo, sem alegação de otimização geral. Avisos preexistentes do Shade
sobre licenças/manifests/module-info não impedem build/execução suportada.

Não há tabela de versão de schema: a inspeção do contrato funciona como barreira
de compatibilidade. CREATE IF NOT EXISTS não migra banco incompatível. A
confirmação de instância descartável não substitui a seleção correta pelo
operador; conta DML e validações de URL/catálogo limitam o destino.

Documentação primária de apoio: [Jakarta Persistence 3.1](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)
e [EclipseLink 4.0](https://eclipse.dev/eclipselink/documentation/4.0/concepts/concepts.html).
A compatibilidade específica com arquivos das aulas inacessíveis não foi verificada.

Nenhuma classe/rota/repositório JDBC, schema/seed histórico, .env ou workflow
java.yml da RA2 foi alterado. Nenhum SQL desta execução local foi dirigido ao
banco original joinup_poo, e o repositório PHP JoinUp não foi alterado. As
alterações locais anteriores do usuário foram preservadas no checkout original.

Parada obrigatória: Entrega 01 concluída para revisão. Não há menu funcional,
CRUD geral, UML final ou relatório acadêmico final. Não fazer merge nem iniciar
a Entrega 02 antes da auditoria do Tech Lead/PO.
