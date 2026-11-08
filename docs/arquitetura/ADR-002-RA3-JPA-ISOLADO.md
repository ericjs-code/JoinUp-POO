# ADR-002 — Exceção de ORM exclusiva do RA3

- Data: 08/11/2026.
- Status: implementação da decisão expressamente autorizada pelo PO na Entrega 00.
- Base: main `05130a12ef9ab5716faf3c0a99fe9c01c45c6334`.
- Escopo: pacote `br.com.joinup.ra3`, perfil Maven `ra3`, schema `joinup_poo_ra3`.

A [ADR-001](ADR-001-STACK-JAVA-PURO.md) permanece como registro histórico da
arquitetura web/JDBC da RA2. A autorização do PO permite JPA com EclipseLink
somente no RA3; não autoriza converter repositórios antigos ou substituir o
servidor HTTP. Java 21, Maven e o driver MySQL existente são reutilizados.

O perfil `ra3` habilita EclipseLink 4.0.4 e uma única API Jakarta Persistence
3.1.0, além da compilação das fontes/testes RA3 e dos seus recursos. O perfil
padrão exclui essas fontes, testes, descriptor e SQL; seu JAR não recebe JPA.
As saídas RA3 ficam em `target/ra3`, evitando reaproveitar classes ORM em uma
compilação incremental do modo original. `exec:java` usa propriedade de classe
principal: `br.com.joinup.Main` por padrão e `br.com.joinup.ra3.Ra3Main` no RA3.
O manifesto do JAR web continua apontando para a Main original; a execução RA3
suportada nesta entrega é pelo plugin exec.

O pacote `modelo` contém somente a classe mutável `GeneroMusical`, com acesso
JPA por campo, construtor protegido sem argumentos, PK `Integer`/IDENTITY e
operação controlada `renomear`. `persistencia` valida configuração e catálogo,
abre a fábrica e sanitiza falhas. `apresentacao` executa um diagnóstico de conexão
e encerra. Não há menu CRUD nem camadas abstratas sem consumidor.

DDL exige aplicação administrativa manual de `db/ra3/schema_ra3.sql`.
As configurações padrão JPA e EclipseLink desabilitam geração de DDL, inclusive
scripts. Não há seed nem INSERT no startup. A URL aceita somente conexão MySQL
simples com schema exato e parâmetros permitidos; opções que alteram catálogo,
executam comandos de sessão ou transformam propriedades são recusadas. O login
é confirmado com `SELECT DATABASE()`. A entidade também qualifica sua tabela
com catálogo RA3. O teste verifica o catálogo dentro de cada transação antes da
escrita. Recomenda-se conta com DML somente no schema RA3.

O teste real é opt-in (`ra3,ra3-it`) com confirmação textual de instância
descartável; exige schema preparado, grava nome UUID próprio, lê em outro
EntityManager com cache compartilhado desabilitado, confirma por SQL, verifica
rollback e remove apenas fixtures de sua própria execução. Não faz DROP,
TRUNCATE, reset de base ou DDL. O banco de desenvolvimento RA3 e a instância
descartável podem ter o mesmo nome de schema em servidores/portas diferentes.

Os PDFs, RAR, PNG e ZIP de aula enumerados pelo PO não foram localizados no
repositório nem nos anexos acessíveis desta tarefa. Foram usadas as decisões
técnicas transcritas pelo PO. Não se atribui conteúdo adicional a esses arquivos.
O XML usa a versão de descriptor 3.0 suportada pela API 3.1; isso não adiciona
uma segunda API 3.0. Não há bibliotecas binárias adicionadas ao Git.

As outras seis entidades, seus relacionamentos, CRUD, menu completo, diagrama
de classes final e relatório acadêmico final permanecem para entregas futuras.
A modalidade de exclusão não é inventada como exigência do enunciado.
