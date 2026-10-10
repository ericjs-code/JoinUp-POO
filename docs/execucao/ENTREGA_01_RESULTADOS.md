# Entrega 01 — Usuários e autenticação

Data: 10/10/2026 (America/Sao_Paulo). Branch: `feat/01-usuarios-autenticacao`.
Base: `main`/`origin/main`, commit `6cfef3d`. Entrega submetida à auditoria técnica;
nenhum merge autorizado ou executado.

## Escopo, fontes e rastreabilidade

Lidos integralmente CODEX_01, AUDITORIA_ENTREGA_00, PLANO_CODEX, especificação
funcional, sete adendos, ADR-001, classes/GoF, contratos HTTP, PlantUML,
schema/seed v0.2 e VALIDACAO_FUNDACAO_00. A referência PHP foi consultada somente
para leitura em `38f022c072e02116fd5a118cbb123de13255d97e`: seis cadastros/logins,
respectivas telas e CSS, editar_perfil, editar_perfil_organizador e
update_profile_participante. Nenhum dado legado foi importado.

| Requisitos | Implementação e evidência |
| --- | --- |
| RF-AUTH-H01/H02 | Cadastros e logins dos três perfis, entidades distintas, JDBC parametrizado; Factory/Template e logins reais na suíte MySQL |
| RF-AUTH-01/01A, CA-01/02 | Cadastro de suporte exige suporte no handler e novamente dentro da transação do creator; novo suporte consegue cadastrar outro |
| RF-AUTH-H03 | GET/PATCH perfil próprio; participante nome/e-mail/nova_senha; organizador também CNPJ; DTO rejeita id/perfil/CPF/data_nascimento na edição |
| RF-USR-01/01A/01B, CA-16 | Desativação lógica e timestamp, revogação de todas as sessões no mesmo commit; histórico e propriedade preservados |
| RF-USR-04/04A, CA-16/17 | Todos os anúncios disponíveis cancelados com timestamp; vendidos/cancelados preservados; reativação não restaura anúncios |
| RF-USR-03/03A | Listagem de inativas somente suporte, reativação da mesma PK e auditoria de autor/data atômicas; suporte não possui desativação/reativação |
| RF-UI-01/02/07 | Página de contas com escolha de perfil, cadastro/login, indicação de sessão, edição/desativação, cadastro de suporte/listagem/reativação; identidade escura/roxa próxima do PHP |
| BD-01 e gate SQL | Banco exclusivamente joinup_poo, MySQL 8.4.0 isolado, 22 tabelas, 42 FKs, 15 CHECKs; seed idempotente e hashes persistidos verificados |

Eventos, compra, API de revendas, comunidades/feed, tickets, dashboards completos,
pagamento, upload e notificações continuam fora desta entrega. A operação sobre
revenda é somente o cancelamento automático na desativação. Frontend **mínimo**,
não declarado completo. JOINUP_DEMO continua reservado; não aplica seed em produção.

## Arquitetura e GoF em fluxos executáveis

Java 21 puro, HttpServer, JDBC, MySQL Connector/J, Maven, Jackson, JUnit.
Composição manual no bootstrap; handlers adaptam HTTP, casos de uso coordenam
transações, repositórios recebem Connection e fecham statements/resultsets.
Não foi introduzido framework, ORM, Servlet ou conexão global.

Factory Method: `CadastroUsuarioCreator.cadastrar` final executa autorização,
chama `criarUsuario` protegido/polimórfico e persiste seu produto. Os três
ConcreteCreators instanciam Participante, Organizador e UsuarioSuporte. O teste
obtém os três tipos e os relê do MySQL; não há helper com switch de construtores.

Template Method: `AutenticacaoTemplate.autenticar` final normaliza credenciais,
busca a conta do perfil sob lock, verifica PBKDF2, estado ativo e regra do perfil,
emite sessão. As três subclasses consultam seus próprios repositórios. O teste
de sequência usa JDBC real e comprova interrupção antes da regra/sessão para
senha incorreta ou conta inativa. A regra específica verifica a identidade do
produto; não acrescenta restrições comerciais não aprovadas.

Singleton de configuração preservado, imutável e sem JDBC/estado de sessão.
Facade permanece desenho futuro para Entrega 02, sem classe decorativa.

UML ajustado às assinaturas efetivas: credenciais e retorno SessaoEmitida,
Connection explícita na busca/persistência/criação de sessão, hashSenha como
argumento do factory method, tokenOperador como prova revalidada no serviço,
salvar retornando PK. Getters reais do Singleton corrigidos. Esses ajustes
mantêm os papéis GoF aprovados e não alteram regras funcionais.

## Segurança e decisões de transporte

- PBKDF2WithHmacSHA256: novos hashes com 600.000 iterações (configurável até
  2.000.000), salt individual de 16 bytes, hash de 32 bytes, comparação constante,
  clearPassword e limpeza dos buffers temporários. Fixtures aprovadas de 180.000
  continuam compatíveis, sem alterar o seed. Referência de custo:
  [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- Token de sessão de 32 bytes SecureRandom, Base64URL; somente SHA-256 persistido.
  Cookie HttpOnly, SameSite=Lax, Path=/, Max-Age; Secure quando origem pública HTTPS.
  Token nunca é retornado no JSON/log/toString. DTOs de saída não incluem senha/hash.
- Expiração de 8h por padrão, revogação persistida e titular ativo revalidados
  em todos os endpoints protegidos. Conta e sessão bloqueadas na mesma unidade
  de trabalho da mutação. Ordem de locks conta → sessão compartilhada pelo login.
- CSRF em **todas** as mutações, inclusive login/cadastro: Origin exatamente igual
  à origem configurada; somente na ausência de Origin admite Referer da mesma
  origem. Ausência, null, origem distinta, duplicatas e Sec-Fetch-Site diferente
  de same-origin rejeitados. Host e X-Forwarded-* não definem a origem autorizada.
  HTTPS via proxy usa JOINUP_PUBLIC_ORIGIN explícita; HTTP admitido só em 127.0.0.1.
- Queries parametrizadas e nomes de tabela/PK de enum fechada. Constraints únicas
  produzem 409 sem detalhes SQL. DTOs rejeitam campos extras, coerção numérica
  para texto, chaves duplicadas e JSON excedente. LocalDate ISO com Jackson JSR-310.
- IDs positivos de INT, queries sem chaves repetidas/UTF-8 inválido, caminhos sem
  traversal, barras codificadas ou sufixos permissivos. Router rejeita padrões
  ambíguos e prioriza estáticos antes de escolher o método (405 sem bypass).
- Conta DML do HTTP separada do administrador de fixtures; sem DDL. Falhas SQL
  retornam 503 genérico, com rollback, sem vazar erro do driver.

Refinamentos técnicos registrados: reativação de conta já ativa retorna 409
CONTA_JA_ATIVA (alternativa já prevista no contrato); corpo das ações vazio ou
{}; listagem paginada com 20 por padrão, até 100; respostas de suporte mínimas sem
documentos pessoais; PATCH parcial com campos herdados. CPF de 11 dígitos e CNPJ
de 14 dígitos, máscaras canônicas aceitas e normalizadas; sem dígitos verificadores,
idade mínima ou nova complexidade de senha. Limite técnico de senha 1024 caracteres
para conter uso de recursos; limite global de corpo permanece 64 KiB.

## Correção técnica do SQL aprovado

A primeira execução real falhou: MySQL ERROR 3823, CHECK de autoria com ações FK
SET NULL/CASCADE incompatíveis. A correção mínima está isolada em `10703b0` e
explicada em [CORRECAO_DDL_01.md](../banco/CORRECAO_DDL_01.md). Mantém todas as FKs,
CHECKs e fixtures, restringindo exclusão física e mudança de PK de autores.
Não se reutilizou a instância parcialmente criada; a validação foi em nova base
vazia. [VALIDACAO_INTEGRACAO_01.md](../banco/VALIDACAO_INTEGRACAO_01.md) registra
versão, comandos, warnings e resultados reproduzíveis.

## Testes executados e limites de evidência

Primeiro `mvn clean verify`: os 52 testes originais passaram intactos. A primeira
ampliação apresentou uma falha de coerção Jackson para String e um teste que
dependia da ordem de chaves JSON; corrigidos com coerção explícita FAIL e leitura
semântica do JSON. Reexecução com MySQL: 86 unitários/HTTP e 22 integrações,
zero falhas/erros/ignorados. Após reforço do histórico do organizador e privilégios,
a execução final está registrada abaixo e nas evidências do PR.

| Suíte | Evidência |
| --- | --- |
| Fundação (52 originais) | ConfiguracaoSistema 16, HttpFoundation 33, ApplicationBootstrap 2, SeedDemoPassword 1, intactos |
| AuthenticationSecurityTest (18) | PBKDF2, salts, senha incorreta, hash inválido/custo excessivo, origem, validação, LocalDate e DTOs |
| RouterParametersTest (16) | Rotas dinâmicas, 400/404/405, precedência, ambiguidades, IDs/query inválidos, CSRF/Referer, cookie Secure, isolamento concorrente |
| MySqlIntegrationIT | Schema/seed/idempotência, constraints e hashes persistidos, Factory/Template, HTTP real de todos os perfis, sessões, CSRF, IDOR, rollback de desativação/reativação com triggers de falha, duas disputas concorrentes, DML sem DDL |

Testes de rollback falham deliberadamente **depois** das primeiras alterações
usando triggers exclusivos da instância de teste; conferem estado ativo,
anúncios, timestamps, sessão e auditoria persistidos após rollback. Triggers
são removidos em finally e dados de testes são limpos ao final, preservando fixtures.

Não executados como aceite de produção: HTTPS com proxy real, carga/abuso e
end-to-end das próximas entregas. Cookies Secure são testados pelo componente
HTTP, mas isso não constitui validação TLS de produção. Rate limiting, limpeza
operacional de sessões históricas e atualização de custo de hashes antigos
ficam como cuidados de implantação; nenhum deles foi apresentado como implementado.

## Revisão técnica e PO

Nenhuma lacuna funcional bloqueou o escopo desta etapa; não foi criado ciclo de
vida de suporte nem comportamento de módulos futuros. A correção das cinco FKs
de autoria deve receber atenção do Tech Lead. Qualquer futura exclusão física ou
migração de PKs exige projeto/decisão própria. Processo operacional de confirmação
de desativação e telas completas continuam nas pendências documentais originais.

Aceite e merge dependem da revisão técnica/PO. As evidências finais de CI e de
navegação serão anexadas ao PR; nenhum resultado pendente é considerado aprovado.
