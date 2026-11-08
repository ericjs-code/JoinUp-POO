# RA3 — Entrega 03: homologação funcional e correções finais

Homologação em 08/11/2026, America/Sao_Paulo. Gate consultado no GitHub antes
de implementar: [PR #14](https://github.com/ericjs-code/JoinUp-POO/pull/14)
`merged=true`, integrado às 20:55:59 UTC (17:55:59 −03), com o head auditado
`bbfa3d702d4dda2166b6a989fec0fd50e4a4b7f0`. `git fetch origin main` retornou
**0ebc1518a423077bd4793537dea353806299c3a3**, base real desta entrega.
Branch nova: **feat/ra3-03-homologacao**, criada diretamente de `origin/main`.
O histórico Git e o PR associado identificam os commits; nenhum merge foi feito
pelo Codex. A entrega permanece sujeita à auditoria do Tech Lead/PO.

## Fontes, ambiente e limites

Foram lidos resultados 00/01/02, guia e evidências 02, ADR-002, pom, workflows
RA3 e Java/JDBC original, todo o pacote Java RA3 e seus testes, os dois XMLs,
schema e SQL de inspeção. Os materiais de aula Especificação do RA3, Introdução
ORM, Tutorial EclipseLink/JPA, ORM Exemplo Biblioteca, Biblioteca.png e
Bibliotecas/JARs não foram localizados nos anexos/repositório acessíveis nesta
execução. Não se afirma sua leitura; o enunciado transcrito pelo PO é a fonte
acadêmica usada. Não há relatório acadêmico final ou UML final nesta entrega.

Ambiente: Windows, JDK **21.0.12.1**, Maven **3.9.9**, MySQL **8.4.0**, EclipseLink
**4.0.4**, Jakarta Persistence **3.1.0**, Connector/J **8.4.0**. Foi criado um
worktree separado em `C:\Users\ericj\JoinUp-POO-ra3-03`, inicialmente sem `target`,
preservando as alterações locais do checkout original. Ferramentas já instaladas
e cache Maven foram reutilizados; não foi simulada uma máquina nova.

MySQL novo, sem serviço instalado, datadir temporário próprio, loopback porta
**50064**, sem Docker. Schema aplicado administrativamente; conta temporária
com somente SELECT/INSERT/UPDATE/DELETE em `joinup_poo_ra3.*`. Gênero,
organizador e evento de controle foram inseridos **antes** das suítes e ficaram
presentes durante integração e CLI. Consulta independente final confirmou os
mesmos controles e nenhuma fixture remanescente. A instância é encerrada ao
final da homologação; credencial temporária não faz parte dos artefatos.

## Defeitos reproduzidos e alterações restritas

| Achado comprovado | Reprodução | Correção / alcance |
| --- | --- | --- |
| Paginação do teste presume tabela vazia | Teste antigo falha com gênero de controle preexistente: primeira página inclui o controle, corretamente. | Somente teste: comparar todas as páginas com snapshot JDBC global por ID; limite 20, página excedente vazia e preservação dos controles. Serviço de paginação intacto. |
| Teste de FK exige tabela de eventos vazia | Teste antigo falha com evento de controle preexistente, embora o cadastro inválido seja recusado. | Somente teste: comparar contagem antes/depois em novos EMs, sem exigir vazio global. |
| Formulários não permitem desistir durante campos | Novos testes de cancelamento, ainda sem mudança de produto: 14 execuções parametrizadas falharam. | `MenuRa3` reconhece `/cancelar` durante uma operação, avisa explicitamente e volta ao submenu. Nenhuma mutação é enviada. `0` continua válido onde permitido. |
| Diagnóstico negativo imprime stacktrace | Porta indisponível e URL de catálogo legado: exit 1, mensagem sanitizada, porém a exceção do ponto de entrada gera frames no Java/Maven. | `Ra3Main` usa exceção própria sem causa/stacktrace, mantendo falha e exit não zero. Duas execuções em JVM separada e os comandos Maven negativos comprovam o resultado. |

São **duas correções de comportamento do aplicativo** (cancelamento e stacktrace)
e **duas correções de testes** que presumiam banco vazio. Os demais acréscimos
são provas: coleção EAGER após mutação, cinco CHECKs reais e EOF/cancelamento.
Não foram alterados entidades, fetch, conversor, DDL/CASCADE, XMLs, pom,
workflows ou módulos da RA2. Não houve refatoração ampla nem nova funcionalidade
acadêmica fora do pedido.

## Matriz de rastreabilidade 7 × 4

Código comum: [ServicoCrudRa3](../../src/main/java/br/com/joinup/ra3/servico/ServicoCrudRa3.java),
comandos em [DadosRa3](../../src/main/java/br/com/joinup/ra3/servico/DadosRa3.java),
entrada em [MenuRa3](../../src/main/java/br/com/joinup/ra3/apresentacao/MenuRa3.java).
Cada célula abaixo foi executada no teste parametrizado
[`CrudRa3MySqlIT.quatroAcoesCrudPorEntidadeEmContextosNovos`](../../src/test/java/br/com/joinup/ra3/CrudRa3MySqlIT.java),
com sete parâmetros, cobrindo **28 ações**, não 28 métodos de teste.

| Entidade | Criar | Consultar | Atualizar | Excluir |
| --- | --- | --- | --- | --- |
| GeneroMusical | NovoGenero → cadastrar/persist | consultar(GENERO,id)/find | AlterarGenero → renomear/commit | excluir(GENERO,id,SIM)/remove |
| Organizador | NovoOrganizador → cadastrar/persist | consultar(ORGANIZADOR,id)/find | AlterarOrganizador → renomear/commit | excluir(ORGANIZADOR,id,SIM)/remove |
| Participante | NovoParticipante → cadastrar/persist | consultar(PARTICIPANTE,id)/find | AlterarParticipante → renomear/commit | excluir(PARTICIPANTE,id,SIM)/remove |
| Evento | NovoEvento → persist com pais existentes | consultar(EVENTO,id)/find | AlterarEvento → nome/imagem/commit | excluir(EVENTO,id,SIM)/remove |
| TipoIngresso | NovoTipo → persist com Evento | consultar(TIPO_INGRESSO,id)/find | AlterarTipo → nome/valor/quantidades/commit | excluir(TIPO_INGRESSO,id,SIM)/remove |
| Comunidade | NovaComunidade → persist com Evento exclusivo | consultar(COMUNIDADE,id)/find | AlterarComunidade → nome/descrição/imagem/commit | excluir(COMUNIDADE,id,SIM)/remove |
| Postagem | NovaPostagem → persist com autor/comunidade | consultar(POSTAGEM,id)/find | AlterarPostagem → descrição/imagem/commit | excluir(POSTAGEM,id,SIM)/remove |

Após criar, a linha é confirmada por JDBC independente e lida em novo EM pelo
serviço; após atualizar, outro EM confere os atributos; após excluir, novo EM e
JDBC confirmam ausência. Confirmação negativa preserva a linha. Logs
`RA3_CRUD_OK entidade=... acao=...` registram cada célula em
[validacao-03.txt](evidencias/validacao-03.txt). A listagem das sete entidades
também foi exercitada no menu real; paginação é verificada em MySQL.

## Vínculos JPA, schema e EAGER

| Associação Java real | Integridade SQL / prova |
| --- | --- |
| Evento.organizador → Organizador | ManyToOne obrigatório, FK; remoção de organizador com evento recusada. |
| Evento.generoMusical → GeneroMusical | ManyToOne obrigatório, FK; remoção de gênero com evento recusada. |
| TipoIngresso.evento ↔ Evento.tiposIngressos | ManyToOne / OneToMany mappedBy; objetos navegáveis, coleção EAGER destacada, dois tipos e identidade do pai. |
| Comunidade.evento → Evento | OneToOne obrigatório do lado Comunidade, FK UNIQUE; Evento aceita zero ou uma comunidade. Duplicidade real 1062 reverte insert anterior. |
| Postagem.participante → Participante | ManyToOne obrigatório; autor navegável e pai protegido. |
| Postagem.comunidade → Comunidade | ManyToOne obrigatório; navegação até evento, gênero, organizador e tipos; pai protegido apesar do CASCADE SQL. |

[`RelacionamentosMySqlIT`](../../src/test/java/br/com/joinup/ra3/RelacionamentosMySqlIT.java)
reconsulta o grafo em outro EM e confirma sete IDs/joins por JDBC. Também
comprova FK ausente (1452), NOT NULL (1048), UNIQUE de CPF/e-mail/CNPJ e
comunidade-evento (1062), além dos **cinco CHECKs** (3819: três limites de tipo
e ativo de organizador/participante). SchemaRa3MySqlIT compara sete tabelas,
PKs IDENTITY, seis FKs, índices/UNIQUEs, defaults, tipos e CHECKs com o contrato
original lido como referência.

O novo teste `colecaoEagerEmNovoContextoRefleteAtualizacaoEExclusaoDoTipo`
carrega o evento, fecha EM, altera o tipo pelo serviço, consulta novamente e
confere nome/valor/quantidade e identidade da associação; após remove, novo EM
obtém coleção vazia e JDBC confirma a ausência. Não se promete atualização de
um objeto já destacado. Fetch EAGER e cache compartilhado NONE foram preservados;
não há alegação de LAZY. UTC continua explícito, com sessão `+00:00` e horário
físico conferido por JDBC; nascimento continua CHAR(14), e UF continua ENUM.

## Transações, exclusão e interrupções

UnidadeTrabalhoRa3 recebe uma fábrica; cria/fecha EM com try-with-resources a
cada operação, verifica catálogo, usa begin/commit na escrita e rollback em
falhas. AplicacaoRa3 cria uma única fábrica por execução CLI e a fecha mesmo
com EOF; Ra3Main encerra a thread de cleanup do Connector/J em finally.
Os testes de aplicação verificam fábrica fechada e diagnóstico sem leitura
ou operação de banco no double, separadamente da conexão real.

`paisComDependentesNaoSaoRemovidosNemPorCascadeSql` cobre gênero, organizador,
participante, evento com **somente comunidade** e comunidade com postagem.
`eventoComTipoTambemTemExclusaoRecusada` cobre o outro filho do evento. JDBC
confirma todos os registros próprios preservados após a recusa.
Os CASCADEs do DDL não foram alterados: o serviço obtém PESSIMISTIC_WRITE no
pai e consulta dependentes antes do remove. A sessão CLI recusou os cinco pais.
Remoção física com SIM continua sendo uma escolha didática para folhas; o
enunciado não foi reinterpretado como obrigação de exclusão física.

Provas adicionais no MySQL:

- Atualização enviada por flush e falha posterior: valor anterior recuperado
  em novo EM. UNIQUE de comunidade-evento também reverte insert anterior.
- Filho concorrente com transação aberta: remoção do pai aguarda; após commit
  do filho, recusa e preserva pai/filho, impedindo CASCADE destrutivo.
- Pai removido entre leitura e INSERT: FK real 1452; alteração de controle já
  enviada por flush é revertida. Mensagem própria VINCULO sem causa SQL.
- MySQL desligado **após abrir o menu**: consulta e cadastro falham, sem linhas
  de sucesso; menu permite sair. Banco reiniciado com os mesmos controles e
  sem o cadastro recusado. A saída final 0 corresponde ao encerramento normal
  do menu, não a sucesso da operação. Diagnóstico negativo retorna exit 1.

`MenuRa3Test` cobre os sete menus, entrada inválida, IDs, datas, dinheiro,
quantidades, confirmação negativa, `/cancelar` em **50 posições de campos**
(29 de cadastro e 21 de atualização incluindo IDs), campos opcionais, EOF no
último campo de cada cadastro/atualização e ausência de comandos de mutação.
Preço/quantidades zero são explicitamente aceitos. Ctrl+Z físico não foi
acionado; o caminho EOF foi testado com streams e processo real.

## Resultados efetivamente executados

| Comando / camada | Resultado final local |
| --- | --- |
| `mvn clean test` — unitários RA2 | exit 0; **167**, zero falhas/erros/skips. |
| `mvn clean verify` — build padrão | exit 0; **167**, zero falhas/erros/skips; JAR web empacotado. |
| JAR + `mvn dependency:tree` filtrado | Sem classes RA3, API JPA, EclipseLink, descriptors ou db/ra3; manifesto Main web. |
| `mvn -Pra3 clean test` — unitários | exit 0; **268** (167 originais + 101 RA3), zero falhas/erros/skips. Sem depender de MySQL disponível. |
| `mvn '-Pra3,ra3-it' verify` — MySQL real | exit 0; **268 Surefire + 42 Failsafe**, zero falhas/erros/skips. |
| `mvn -Pra3 compile exec:java` — demonstração | exit 0; sete cadastros, relações, listagens, alterações, cancelamentos, pais recusados e folha removida. |
| `mvn -Pra3 exec:java` — outra JVM | exit 0; evento atualizado, gênero e organizador continuam presentes; depois limpeza somente dos IDs próprios. |
| `mvn -Pra3 exec:java '-Dexec.args=--diagnostico'` | exit 0; RA3_CONEXAO_OK; processo terminou com stdin **ainda aberto**, sem menu/insert. |
| Diagnóstico com porta indisponível / catálogo legado | exit 1 em ambos; status próprio, sem URL, senha, SQL ou frames de stacktrace. |
| Menu com formulário parcial + stdin fechado | exit 0; Fim da entrada; nenhum cadastro parcial. |
| Queda real de MySQL durante uso | Consulta/cadastro recusados sem sucesso falso; processo encerra e banco reinicia preservado. |
| Dados preexistentes e limpeza | Controles de gênero/organizador/evento preservados; demais quatro tabelas vazias e nenhuma fixture nas sete. |
| `git diff --check` e inspeção de escopo/segredos | Sem erro de whitespace; somente código/testes/docs RA3, sem credenciais reais ou binários. |

Detalhes, comandos, horários e hashes de logs estão em
[validacao-03.txt](evidencias/validacao-03.txt); inputs e respostas reais,
incluindo reinício de JVM, em [sessao-cli-03.txt](evidencias/sessao-cli-03.txt).
Unitários usam validações/doubles/processos isolados; os 42 Failsafe acessam
MySQL; a demonstração executa menu/serviço reais. Nenhuma dessas camadas
substitui a observação do CI. O perfil JDBC `-Pmysql-it` **não foi executado
localmente**: requer duas instâncias RA2. Sua regressão será observada no
workflow histórico, preservado, após push. CI desta entrega é registrado
separadamente, por SHA/run, sem reutilizar números anteriores.

## Isolamento, execução reproduzível e pendências

Credenciais exclusivamente JOINUP_RA3_*, URL e catálogo real validados,
DDL/scripts `none`, sete @Entity explícitas, sem seed no startup. Nenhum SQL
local foi enviado aos catálogos `joinup_poo`/`joinup`. O SQL RA2 foi lido pelos
testes como contrato, sem aplicação no MySQL local. A instância temporária
não contém esses catálogos. O workflow histórico usa suas próprias instâncias
descartáveis RA2; isso é distinto do banco original do usuário.

O [guia Windows](EXECUCAO_RA3.md) inclui ferramentas/versões, repositório e
branch aprovada/em revisão, seleção do JDK 21 por caminho, schema manual,
conta DML, leitura segura da senha, variáveis, portas, diagnóstico, integração
descartável, roteiro e limpeza do ambiente. Os exemplos contêm apenas marcadores
fictícios. Maven é o modo de execução; não se distribui um JAR RA3 isolado.

**Bloqueantes de processo:** auditoria Tech Lead/PO e autorização de integração
do PR desta entrega. Os checks de CI precisam ser conferidos por SHA; a seção
de evidência de CI registra a consulta efetiva. Não há defeito funcional local
bloqueante identificado após as verificações descritas.

**Pendências acadêmicas:** Entrega 04 ainda necessária para UML final e relatório
curto com reflexões reais dos membros. Não foram produzidos nesta entrega.
Prazos transcritos do PO: entrega 12/11/2026; apresentação 13/11/2026.

**Não bloqueantes / limites:** materiais de aula inacessíveis para confronto;
instalação em computador novo não executada; Ctrl+Z físico não automatizado;
EAGER adequado ao grafo acadêmico pequeno sem benchmark; paginação por offset
pode variar sob escritas concorrentes; quedas abruptas podem deixar fixtures;
criação IF NOT EXISTS não migra schema incompatível. Na preparação, uma senha
temporária exclusivamente hexadecimal foi recusada pelo provider como
criptografada; outra composição resolveu a conexão. Não se ampliou esta entrega
para alterar o mecanismo de senhas do EclipseLink. Avisos de sobreposição do
Shade permanecem sem impedir o build; não se apresenta o JAR web como JAR RA3.

Parada obrigatória: abrir PR para `main`, **não mesclar**, aguardar auditoria.
