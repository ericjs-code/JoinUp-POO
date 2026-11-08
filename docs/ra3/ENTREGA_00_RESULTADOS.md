# RA3 — Entrega 00: resultados e limites

Validação local em 08/11/2026, America/Sao_Paulo. Base mais recente da main no
início: `05130a12ef9ab5716faf3c0a99fe9c01c45c6334` (PR #11 já integrado).
Branch: `feat/ra3-00-fundacao-jpa`. Checkout de implementação separado das
alterações locais anteriores do usuário; nenhum histórico foi reescrito.
O commit da entrega e o PR são identificados pelo histórico Git/PR associado.

## Inspeção anterior à implementação

| Arquivos conferidos | Constatação pertinente |
| --- | --- |
| pom.xml | Java 21, Connector/J 8.4.0, JUnit 5.11.4, exec fixo em Main; sem JPA. Perfil mysql-it é da RA2. |
| README.md e .github/workflows/java.yml | Web/HttpServer/JDBC; CI original usa duas instâncias MySQL para integração RA2. Foi preservado. |
| schema_v0.2.sql | Banco joinup_poo; generos_musicais usa id_genero_musical INT AUTO_INCREMENT e nome VARCHAR(45) NOT NULL. |
| migration_06_midia_demo.sql | Acrescenta arquivos e demo_execucao ao banco antigo. Não aplicada no RA3. |
| Main.java e bootstrap/ApplicationBootstrap.java | Compõem servidor, rotas, serviços, conexão JDBC e demo. Não são usados pelo ponto de entrada RA3. |
| shared/db/ConnectionFactory.java e ExecutorTransacional.java | Configuração JDBC original, transações SQL explícitas e tratamento HTTP; preservados. |
| eventos/domain/Evento.java e TipoIngresso.java | Records de projeção; não podem receber anotações para fingir entidades JPA mutáveis. |
| usuarios/domain/Organizador.java e Participante.java | Classes finais com estado final/herança de Usuario; preservadas sem conversão ORM. |
| EventoRepositoryJdbc e TipoIngressoRepositoryJdbc | Consultas/mapeamentos explícitos e projeções do SQL; gênero ligado por ID/JOIN. |
| OrganizadorRepositoryJdbc, ParticipanteRepositoryJdbc e UsuarioRepositoryJdbc | Mapeamento manual por ResultSet, inserts parametrizados e conexões fornecidas pelos serviços. |
| docs/ra2/DIAGRAMA_DOMINIO_CARDINALIDADES.puml | Declara explicitamente misturar projeções Java/tabelas e cardinalidades SQL, sem referências ORM reais. |
| ADR-001-STACK-JAVA-PURO.md | Proibição histórica de ORM mantida. Exceção RA3 documentada em ADR-002. |

## Dependências e configuração efetivas

- `org.eclipse.persistence:eclipselink:4.0.4`, somente em `ra3`.
- `jakarta.persistence:jakarta.persistence-api:3.1.0`, explícita e única;
  dependência transitiva de mesma coordenada excluída do EclipseLink.
- `com.mysql:mysql-connector-j:8.4.0`, reutilizada sem duplicação.
- `org.junit.jupiter:junit-jupiter:5.11.4`, preexistente.
- Compiler 3.13.0, Surefire/Failsafe 3.5.2 e exec 3.5.0; plugins existentes
  reutilizados com perfil de integração RA3 separado.
- `RESOURCE_LOCAL`, unidade `joinup-ra3`, provider
  `org.eclipse.persistence.jpa.PersistenceProvider`; somente GeneroMusical.
- Descoberta não listada excluída, weaving desabilitado, cache compartilhado
  desabilitado, geração de DDL/scripts `none`, logs do provider `OFF` e
  parâmetros de log `false` para não publicar credenciais em falhas.
- Credenciais somente pelas três variáveis `JOINUP_RA3_*` de execução;
  URL restrita, checagem do catálogo real e tabela qualificada no catálogo RA3.
- Artefatos RA3 em `target/ra3`; Main web e manifesto original preservados.

A árvore de dependências e a enumeração de recursos `Entity.class` nos testes
confirmam uma única API 3.1.0. Os transitivos do bundle EclipseLink incluem
Jakarta XML Bind 4.0.2, JAXB XJC/Core 4.0.5, Angus Activation 2.0.2 e Jakarta
Activation 2.1.3; não são uma segunda API de persistência. Não há Hibernate,
Spring, servidor novo ou JAR/ZIP versionado.

## Comandos e resultados locais

Todos os comandos Maven utilizaram JDK **21.0.12.1** e Maven **3.9.9** pelos
executáveis locais equivalentes aos comandos abaixo; o Java 25 do PATH não foi
usado para os builds. MySQL **8.4.0** real, inicializado em novo diretório
temporário, loopback, porta efêmera **57926**, exclusivamente para esta entrega.
Docker estava indisponível; foi usado o mysqld local já instalado.

| Comando/verificação | Resultado |
| --- | --- |
| git fetch origin main; git rev-parse origin/main | Base registrada acima; branch criada diretamente desse commit. |
| mvn clean test | SUCESSO: 167 testes originais, zero falhas/erros/skips. |
| mvn clean verify | SUCESSO: 167 testes originais e JAR web empacotado. |
| Inspeção de target/joinup-poo.jar | Ausência de br/com/joinup/ra3, jakarta/persistence, META-INF/persistence.xml e db/ra3. |
| mvn -Pra3 clean test | SUCESSO: 198 testes (167 originais + 31 RA3), sem exigir MySQL. |
| mvn -Pra3 dependency:tree com filtro | EclipseLink 4.0.4, API Jakarta Persistence 3.1.0 e driver 8.4.0. |
| mvn dependency:tree com filtro EclipseLink/JPA | Nenhuma dependência ORM no perfil padrão. |
| Aplicação manual de schema_ra3.sql pelo mysql | SUCESSO; só banco RA3 e tabela generos_musicais. Conta do teste limitada a DML. |
| mvn '-Pra3,ra3-it' verify | SUCESSO: 198 Surefire + 1 Failsafe, zero falhas/erros/skips. |
| mvn -Pra3 exec:java com configuração válida | SUCESSO, RA3_CONEXAO_OK; encerra sem HTTP e sem insert. |
| Mesmo startup com porta 1 indisponível | FALHA ESPERADA (exit 1), RA3_CONEXAO_FALHOU; sem segredo sentinela nos logs. |
| Mesmo startup com joinup_poo na URL | RECUSA ESPERADA (exit 1), RA3_CONFIGURACAO_INVALIDA; antes de conectar. |
| Mesmo startup sem variáveis RA3 | FALHA ESPERADA (exit 1), instrução para configurar JOINUP_RA3_JDBC_URL. |
| SQL independente após smoke/startup | Uma tabela no RA3, zero gêneros restantes; grants somente SELECT/INSERT/UPDATE/DELETE. |
| git diff --check | Sem erro de whitespace. |

Evidência da execução final: `RA3_SMOKE_OK provider=4.0.4
catalogo=joinup_poo_ra3 id=5 leitura=outro-EntityManager+SQL`.
Os IDs 1 e 3 foram execuções anteriores bem-sucedidas, também limpas. A sequência
AUTO_INCREMENT avança, embora as fixtures sejam removidas; isso é esperado.
A consulta em outro EntityManager recuperou ID/nome após commit; a consulta SQL
adicional confirmou a linha física. O cenário de falha após flush verificou
rollback e ausência da segunda fixture. Ambas as limpezas verificaram ausência.
**Integração JPA/MySQL: EXECUTADA COM SUCESSO.**

Trechos extraídos dos logs e hashes estão em
[evidencias/validacao-00.txt](evidencias/validacao-00.txt). Os relatórios locais
Surefire/Failsafe ficam em `target/ra3`, e o workflow RA3 publica os relatórios
em artefato de CI. Resultados do CI são separados destes resultados locais e
devem ser consultados nos checks do PR.

## Falhas encontradas, materiais e limitações

O startup inicialmente deixou o daemon de limpeza do Connector/J no classloader
do exec, produzindo aviso e espera de 15 segundos. O ponto de entrada RA3 agora
chama `AbandonedConnectionCleanupThread.checkedShutdown()` após fechar os
recursos JPA. A repetição terminou em menos de um segundo sem aviso de thread.
Essa rotina não foi adicionada ao servidor web nem aos testes da RA2.

O Shade emite avisos de manifesto/licenças e module-info sobrepostos nos bundles,
inclusive nas dependências preexistentes; os builds passam e a execução
suportada do RA3 usa `exec:java`. Não foi criado um executável RA3 distribuível
separado. A opção de XML 3.0 é o formato de descriptor, compatível com a API
3.1.0 única, e não uma segunda dependência Jakarta 3.0.

Os cinco materiais de aula citados no pedido não foram encontrados no
repositório nem nos anexos acessíveis desta execução. As versões e o padrão
Jakarta foram implementados a partir das decisões fornecidas pelo PO. Não foi
possível comparar o código com detalhes internos desses materiais.
Documentação técnica primária consultada:
[EclipseLink 4.0](https://eclipse.dev/eclipselink/documentation/4.0/concepts/concepts.html),
[propriedades do EclipseLink](https://eclipse.dev/eclipselink/documentation/4.0/jpa/extensions/jpa-extensions.pdf) e
[propriedades Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-configuration-properties.html).

A confirmação textual do teste declara finalidade descartável, mas não prova
sozinha a origem do servidor; o operador deve escolher a instância correta.
A validação adicional de URL/catálogo e o usuário DML exclusivo limitam o
destino. Interrupção abrupta pode deixar fixture UUID; não há limpeza global.
As URLs permitidas são deliberadamente simples; IPv6 e recursos avançados do
Connector/J exigirão decisão futura caso necessários.

A suíte antiga `-Pmysql-it` não foi executada localmente nesta entrega: ela
exige duas instâncias com schema/fixtures da RA2, fora da instância RA3 criada.
Os 167 testes originais sem banco passaram; não se apresenta isso como execução
dos 301 testes JDBC/MySQL históricos. O workflow antigo continua disponível.

**Preservação:** nenhuma classe, rota, regra, SQL histórico, seed ou dado da
aplicação JDBC original foi alterado por esta entrega. O checkout original do
usuário conservou as duas alterações locais e três arquivos não versionados
encontrados no início. O repositório PHP `ericjs-code/JoinUp` não foi alterado.

**Parada:** somente Entrega 00. As outras seis entidades, relacionamentos e CRUD
completo não estão implementados. Próxima entrega depende da auditoria do PO.
