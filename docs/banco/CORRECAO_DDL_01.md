# Correção técnica de compatibilidade CHECK/FK — Entrega 01

Em 10/10/2026, a execução do schema original no MySQL 8.4.0 isolado falhou com
`ERROR 3823 (HY000)` na criação de `comunidade_mensagens`: a coluna
`id_participante` está no CHECK `chk_mensagem_autor` e em FK com ação referencial
`SET NULL`/`CASCADE`. O MySQL proíbe essa combinação. A execução interrompeu
o schema com 7 tabelas. O seed subsequente também falhou por tabelas ausentes;
essa primeira instância não foi usada para validar a aplicação.

A [documentação oficial de CHECKs do MySQL](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html)
descreve a incompatibilidade com ações referenciais sobre as colunas do CHECK.
A mesma combinação existia nas três FKs de autoria de `ticket_mensagens`.

Correção mínima: manter as cinco FKs e todos os CHECKs, usar `ON UPDATE NO ACTION`
nas cinco referências a autores e `ON DELETE RESTRICT` nas duas que usavam
`SET NULL`. Nenhuma coluna, índice, estado, fixture ou relação foi removida.
As outras ações referenciais permanecem como aprovadas.

Isso preserva RF-USR-01/01B: contas são desativadas logicamente e autoria/histórico
devem continuar registrados. A aplicação nunca exclui fisicamente usuários nem
altera suas PKs; editar CNPJ/e-mail não altera a PK numérica. O antigo SET NULL
também produziria uma linha incompatível com o próprio CHECK de autoria.
Esta é uma correção técnica do DDL inexequível, não autorização de exclusão de
usuários ou alteração funcional. Qualquer futuro caso de migração de PKs ou
exclusão física continua fora do escopo e precisará de decisão própria.

Evidências de reexecução em instância vazia, constraints efetivas e seed estão
em `VALIDACAO_INTEGRACAO_01.md` e na suíte MySQL real do PR.
