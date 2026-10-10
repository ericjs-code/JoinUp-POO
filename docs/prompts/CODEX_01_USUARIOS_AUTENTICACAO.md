# CODEX — Entrega 01: usuários, Factory Method, Template Method e autenticação segura

**Papel:** você é o desenvolvedor Java do JoinUp-POO; implemente apenas a Entrega 01, preservando a fundação já incorporada em `main` pelo PR #1. O PO autorizou a implementação e aprovou os contratos HTTP como baseline. **Não faça merge sem revisão do Tech Lead.**

## 0. Repositório e leitura obrigatória

- **Destino:** `ericjs-code/JoinUp-POO`, branch nova `feat/01-usuarios-autenticacao`, iniciando na `main` atual.
- **Referência funcional/visual (somente leitura):** `ericjs-code/JoinUp`; confira `src/php/cadastroParticipante.php`, `cadastroOrganizador.php`, `cadastroSuporte.php`, `loginParticipante.php`, `loginOrganizador.php`, `loginSuporte.php` e telas correspondentes.
- Leia `docs/execucao/AUDITORIA_ENTREGA_00.md` e `docs/execucao/PLANO_CODEX.md`, além de **todos** os documentos `docs/requisitos/ADENDO_*.md`, `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md`, `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`, `docs/arquitetura/CLASSES_E_PADROES_GOF_v0.1.md`, `docs/api/CONTRATOS_HTTP_v0.1.md`, `docs/diagramas/CLASSES_GOF_v0.1.puml`, `src/main/resources/db/schema_v0.2.sql`, `seed_demo_v0.2.sql`, `docs/banco/VALIDACAO_FUNDACAO_00.md`.
- **Prioridades formais:** Java 21 puro, JDK `HttpServer`, JDBC + MySQL 8, Maven, Jackson, JUnit. Sem Spring Boot/Javalin/ORM/Servlet/DI. Manter os quatro GoF escolhidos; nesta entrega implementar **Factory Method e Template Method**, reutilizando o Singleton já existente, sem stubs decorativos para Facade.
- **Havendo conflito** entre documento-base e adendo aprovado, seguir o adendo mais recente. Se houver lacuna funcional, registrar decisão pendente para PO; não inventar regra.

## 1. Gate obrigatório: SQL real (antes de declarar aceitação da Entrega 01)

1. Provisione MySQL **8.0.16+** de teste isolado, preferencialmente via **service container MySQL 8 no GitHub Actions**, já que a Entrega 00 não tinha MySQL local. Alternativamente, valide em instância isolada local e forneça evidência reproduzível; não execute no banco legado `joinup`.
2. Execute o schema `schema_v0.2.sql` e o seed `seed_demo_v0.2.sql` no banco **`joinup_poo`**. Verifique 22 tabelas, FKs/CHECKs, índices, tipos/estados, linha de seed, hashes PBKDF2 realmente **persistidos** e reexecução sem crescimento inesperado ou inconsistências. Use `docs/banco/VALIDACAO_FUNDACAO_00.md`.
3. Registre versão exata de MySQL, comandos, erros/warnings e resultados em `docs/banco/VALIDACAO_INTEGRACAO_01.md`.
4. Se o DDL/seed apresentar defeito, isole o problema e faça a **menor correção possível** com justificativa e testes. Não remova FKs, CHECKs ou regras para “fazer passar”; não mude silenciosamente o modelo aprovado. Diferencie correção técnica da mudança funcional e peça decisão ao PO se afetar requisitos.
5. O CI de autenticação deve testar o banco real, não usar apenas mocks ou testes de hashes do arquivo seed. Se MySQL não estiver disponível, reporte **BLOQUEIO**, entregue somente alterações realmente testadas e não reivindique conclusão da Entrega 01.

## 2. Usuários e Factory Method — GoF real

Implemente entidades de domínio `Usuario`, `Participante`, `Organizador`, `UsuarioSuporte`, modelos/DTOs e repositórios JDBC alinhados às tabelas v0.2 (tabelas distintas por perfil; sem inventar tabela `usuarios` única). Campos de cadastro do PHP:
- Participante: `nome`, `email`, `cpf`, `data_nascimento`, `senha`.
- Organizador: `nome`, `email`, `cnpj`, `senha`.
- Suporte: `nome`, `email`, `senha`, **apenas por suporte autenticado**.

Crie `CadastroUsuarioCreator` abstrato e concrete creators para os três perfis, com **factory method polimórfico protegido** `criarUsuario(...)`, chamado por fluxo real de cadastro. A operação comum `cadastrar(...)` deve aplicar validações/repositório sem construir usuários concretos por `switch` em helper. Mantenha o desenho próximo do UML aprovado, com ajustes mínimos documentados.

Rotas:
- `POST /api/participantes` (público).
- `POST /api/organizadores` (público).
- `POST /api/suporte/usuarios` (**somente SUPORTE autenticado**).

Use queries parametrizadas, constraints únicas e tratamento coerente de e-mail/CPF/CNPJ duplicados; validação server-side do comprimento/formatos do banco. Não devolver `senha` nem hashes em respostas.

## 3. Autenticação — Template Method real

- `AutenticacaoTemplate` abstrata com método **final** `autenticar(credenciais)`: normalizar entrada → buscar usuário do perfil → validar hash PBKDF2 → validar conta ativa quando aplicável → regra específica → emitir sessão segura.
- Três subclasses reais: `AutenticacaoParticipante`, `AutenticacaoOrganizador`, `AutenticacaoSuporte`, com consulta por tipo/perfil. Não reduzir padrão a uma fachada com `switch`.
- PBKDF2WithHmacSHA256 com `salt` aleatório individual e iterações configuradas adequadamente; hashes compatíveis com fixture `pbkdf2_sha256$iteracoes$saltBase64$hashBase64`; comparar hashes em tempo constante; `PBEKeySpec.clearPassword()`; não logar senhas.
- Rotas:
  - `POST /api/auth/participantes/login`
  - `POST /api/auth/organizadores/login`
  - `POST /api/auth/suporte/login`
  - `GET /api/auth/me`
  - `POST /api/auth/logout`
- Sessões: token imprevisível com `SecureRandom`, guardar somente **hash do token** em `sessoes`, expirável/revogável. Cookie `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure` quando HTTPS (ambiente local HTTP 127.0.0.1 documentado); nunca token de sessão em JSON. Valide expiração, revogação e **status ativo** do titular em todos os endpoints protegidos.
- Validar permissões no backend, resposta 401 sem sessão e 403 perfil inadequado. Proteger mudanças de estado por estratégia CSRF segura e testes reais (Origin/Referer adequados e/ou token CSRF por sessão; nenhuma mutação sem proteção). Não aceitar CORS indiscriminado.
- Não executar consulta JDBC dentro de `ConfiguracaoSistema` nem inserir `Connection` em Singleton.

## 4. Conta própria, desativação e reativação — regras aprovadas

Rotas:
- `GET /api/me/perfil` e `PATCH /api/me/perfil`: somente participante/organizador **titular**, validações conforme campos e comportamento existentes; não alterar perfil de outro usuário.
- `POST /api/me/desativacao`: desativa conta atual participante/organizador; mantém histórico; bloqueia login; **revoga sessões** na mesma transação.
- **RF-USR-04:** ao desativar participante, **cancelar todos os anúncios de revenda `disponivel` do participante na mesma transação JDBC**, sem alterar titularidade de ingressos. É uma regra de desativação já aprovada; não precisa implementar o restante da API de revendas nesta entrega. Reativar posteriormente **não restabelece anúncios**.
- Organizador inativo: manter eventos e comunidades; futuras operações de venda validarão status (não implementar compra nesta entrega).
- `GET /api/suporte/contas?tipo=participante|organizador&situacao=inativa`: listagem mínima **somente SUPORTE**, evitando expor hashes.
- `POST /api/suporte/contas/{tipo}/{id}/reativacao`: **somente SUPORTE**; restabelecer `ativo` do mesmo registro, registrar autor e timestamp em `auditoria_reativacoes`, sem criar usuário novo; proibir reativação de suporte porque não foi autorizada.
- Status de suporte `usuarios_suporte` não possui coluna `ativo` no schema aprovado; **não inventar ciclo de desativação de suporte**.

## 5. Roteador HTTP — evolução necessária

O `Router` da entrega 00 aceita apenas rotas exatas. Evolua com segurança para rotas como `/api/suporte/contas/{tipo}/{id}/reativacao`, suporte a parâmetros de caminho e query string e precedência determinística para rotas estáticas. Não usar `startsWith` permissivo e não deixar padrões ambíguos. Testar 404/405/400, URL malformada, traversal, variação de métodos, IDs inválidos e autorização.

Preserve `GET /api/health`, página estática e testes da Entrega 00. Atualize README para recomendar branch **`main`** (PR #1 já foi mergeado). Se usar `LocalDate`/`OffsetDateTime` em JSON, adicionar suporte Jackson para tipos de data e testar serialização/deserialização. Não elevar o limite padrão de corpo sem justificativa; uploads não fazem parte desta etapa.

## 6. Testes obrigatórios

- **Factory Method:** validar `Creator → Product` com três tipos concretos, persistência real e cadastro de suporte 401/403 para visitante/outros perfis.
- **Template Method:** login correto/errado dos três perfis, identidade dos tipos e sequência real do algoritmo; conta participante/organizador inativa não autentica.
- **Sessões:** token enviado por cookie, hash persistido, expiração, logout, revogação na desativação, isolamento entre usuários e reuso inválido.
- **Autorização/IDOR:** acesso negado a perfil de terceiros, listagem/reativação somente suporte, reativação auditada, operação sem sessão e sessão de perfil incorreto; ataques CSRF negados quando aplicável.
- **Concorrência/transações:** desativação cancela anúncio e revoga sessões atomicamente; erro intermediário provoca rollback completo; reativação com auditoria atômica.
- **Banco real:** pelo menos cadastro/login/revalidação de sessão/desativação/reativação com MySQL 8 no CI ou ambiente real reproduzível, schema e seed aplicados, resultados rastreáveis.
- **Regressão:** manter 52 testes da Entrega 00 passando, ou justificar ajustes técnicos estritamente necessários com novos testes.
- **Frontend mínimo:** conectar cadastro/login, indicação de sessão e desativação/reativação de acordo com perfis, sem tentar desenvolver dashboards completos; respeitar identidade visual do PHP na medida razoável para esta etapa. **Não declarar frontend completo.**

## 7. Fora do escopo

Não implementar ainda casos completos de eventos, artistas, categorias, compras, publicação/compra de revendas, comunidades, feed, tickets de suporte, gateway real de pagamento ou notificações. A única operação sobre anúncios permitida neste sprint é cancelamento **automaticamente na desativação**.

Não alterar os quatro GoF aprovados. **Facade será implementado na Entrega 02**. Não criar banco novo com nome diferente, não importar registros legados e não executar SQL destrutivo contra `joinup`.

## 8. Entregáveis e formato de retorno

1. Branch `feat/01-usuarios-autenticacao`, PR **sem merge** para revisão.
2. Código Java, HTML/CSS/JS mínimos necessários, testes automatizados e README atualizado.
3. Comandos realmente executados e saídas resumidas: `mvn clean verify`, serviço MySQL real/versão, schema/seed, testes de integração.
4. Evidências de Factory Method e Template Method em **fluxos executáveis**, UML `docs/diagramas/CLASSES_GOF_v0.1.puml` atualizado **apenas se assinaturas realmente mudarem**, explicando diferenças.
5. Relatório `docs/execucao/ENTREGA_01_RESULTADOS.md` com funcionalidades, IDs de requisitos, segurança, testes reais/não executados, riscos/bloqueios e pontos para o PO.
6. **Não mesclar automaticamente**. Enviar link de PR para auditoria do Tech Lead.

**Definição de pronto:** funcionamento real com MySQL testado, dois GoF corretos, endpoints protegidos, regressões aprovadas, contratos aderentes e documentação consistente. Se falhar requisito crítico, entregar PR explicitamente parcial e listar bloqueios.
