# CODEX — Entrega 00: fundação Java puro para JoinUp-POO

**Papel:** você é o desenvolvedor responsável por implementar o primeiro incremento do JoinUp-POO. O Product Owner já autorizou o início da implementação. Trabalhe com autonomia técnica **dentro do escopo deste prompt**, sem criar regras de negócio novas.

## Contexto e repositórios

- **Destino:** `https://github.com/ericjs-code/JoinUp-POO`. Inicie a partir da versão mais recente de `main`; se possível, trabalhe em branch `feat/00-fundacao-java-puro` e proponha um PR para revisão.
- **Projeto PHP de referência (somente leitura):** `https://github.com/ericjs-code/JoinUp`. Não copiar código PHP para Java nem alterar a aplicação original.
- **Prazo acadêmico:** 15/10/2026 para entrega; 16/10/2026 para apresentação. O sistema final precisará demonstrar Facade, Template Method, Factory Method e Singleton e preservar a plataforma original, com exceções aprovadas.

## Leia antes de programar

1. `docs/requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md`
2. **Todos** os arquivos `docs/requisitos/ADENDO_*.md` (não apenas a especificação base).
3. `docs/arquitetura/ADR-001-STACK-JAVA-PURO.md`
4. `docs/arquitetura/PROPOSTA_JAVA_PURO_v0.1.md`
5. `docs/arquitetura/CLASSES_E_PADROES_GOF_v0.1.md`
6. `docs/diagramas/CLASSES_GOF_v0.1.puml`
7. `docs/api/CONTRATOS_HTTP_v0.1.md`
8. `docs/banco/PARECER_BANCO_v0.2.md`
9. `src/main/resources/db/schema_v0.2.sql` e `src/main/resources/db/seed_demo_v0.2.sql`.

Se algum arquivo não estiver acessível, informe a restrição. Não presuma seu conteúdo.

## Stack obrigatória

- Java **21 puro**, sem Spring Boot, Javalin, frameworks HTTP, Jakarta Servlet, Hibernate/JPA ou injeção de dependência automática.
- Servidor HTTP: `com.sun.net.httpserver.HttpServer` do JDK.
- SQL explícito via JDBC com MySQL 8 e MySQL Connector/J.
- Maven, Jackson para JSON, JUnit 5 para testes.
- Frontend HTML/CSS/JavaScript sem framework; arquivos estáticos servidos pelo mesmo processo Java.
- Pacotes sob `br.com.joinup`; composição manual via construtores no bootstrap.

## Escopo EXCLUSIVO desta entrega 00

**A. Projeto executável**
1. Criar `pom.xml` com Java 21, dependências mínimas, plugins de compilação/teste e forma clara de executar o `Main` por Maven ou JAR. Evitar dependências supérfluas.
2. Criar `Main` e `ApplicationBootstrap`. Iniciar/encerrar `HttpServer` com executor controlado e fechamento correto dos recursos. Incluir tratamento simples de exceções.
3. Criar roteador por método+path ou solução manual equivalente, handlers HTTP desacoplados de SQL, utilitário JSON (Jackson), respostas de erros padronizadas de acordo com `docs/api` e handlers estáticos com proteção contra path traversal.
4. Expor `GET /api/health` retornando JSON com status e sem vazar credenciais/dados internos. Métodos/rotas desconhecidos retornam 404/405 apropriadamente.
5. Servir uma página HTML inicial mínima, verificável no navegador, sem tentar reproduzir todas as telas nesta entrega.

**B. Configuração e acesso a banco**
6. Configurar porta, URL JDBC, usuário/senha de banco, modo demo e configurações não sensíveis por variáveis de ambiente/propriedades. Não publicar segredos. Fornecer `.env.example` apenas como referência, se útil, sem presumir leitura automática de `.env`.
7. Aplicar **Singleton GoF** de verdade à configuração imutável `ConfiguracaoSistema`, com construtor privado e inicialização thread-safe; demonstrar em teste que `getInstancia()` retorna a mesma instância. Não usar Singleton para `java.sql.Connection`, sessões, repositórios ou dados mutáveis de negócio.
8. Criar `ConnectionFactory` JDBC com conexões independentes por operação, e estrutura adequada para `try-with-resources` e futuras transações; sem ORM.
9. Preparar instruções reprodutíveis para criar **somente** `joinup_poo`, executar `schema_v0.2.sql` e `seed_demo_v0.2.sql`. O schema/seed aprovados **ainda não foram validados em MySQL**. Se houver MySQL 8 de teste disponível, execute-os em banco isolado, confira tabelas/FKs, seed repetida sem duplicações e senha PBKDF2. Se não houver, registre claramente **NÃO EXECUTADO**, comandos exatos e dependência pendente. Nunca rode contra `joinup` legado.
10. Não modifique os SQLs aprovados silenciosamente. Se algum erro objetivo impedir execução, documente diagnóstico e proponha correção mínima em arquivo/commit isolado, preservando histórico e justificando a alteração.

**C. Testes, segurança e documentação**
11. Testes automatizados JUnit para configuração Singleton (identidade), roteamento HTTP básico, `/api/health`, 404/405 e leitura de estáticos. Acrescentar testes de proteção contra path traversal e de erros de JSON onde aplicável.
12. Garantir limites razoáveis de tamanho de requisição, sem habilitar CORS irrestrito; planejar cookies seguros e proteção CSRF para as próximas entregas, mas **não fingir que a autenticação já existe**.
13. Criar/atualizar `README.md` com pré-requisitos, instalação, env vars, execução, URL local, teste e status real da integração com MySQL.
14. Adicionar `.gitignore` para `target/`, arquivos IDE, logs, dados locais e segredos. Não colocar senhas de produção no repositório nem usar o seed de demonstração em ambiente público.

## Fora do escopo desta entrega

- NÃO implementar cadastros, logins, sessões completas, compra, revenda, eventos reais, feed, comunidades, suporte ou dashboards.
- NÃO criar stubs decorativos dos outros três GoF. Facade, Template Method e Factory Method serão aplicados quando implementarmos os fluxos de negócio.
- NÃO alterar a stack aprovada, as regras de negócio ou os quatro padrões escolhidos.
- NÃO substituir os documentos e adendos por decisões próprias.
- NÃO declarar que testou MySQL sem executar os comandos em instância efetiva.

## Critérios de aceite

- `mvn clean test` passa em Java 21, e a aplicação sobe pelo comando documentado.
- `GET /api/health` responde com JSON; frontend estático é carregado; erros HTTP têm tratamento consistente.
- Implementação Java pura sem frameworks não autorizados.
- Singleton não é apenas objeto estático cosmético; possui uso real, teste de identidade e é imutável/thread-safe.
- Conexão JDBC configurável, sem credenciais fixas e sem conexão global permanente.
- Relatório claro sobre validação **real** (ou impedimento) de schema/seed MySQL.
- Código pequeno, organizado por responsabilidade e passível de continuar os demais sprints.
- Branch/PR ou referência clara a commits, **sem efetuar merge automático sem revisão**.

## Formato da sua resposta final

1. **Resumo objetivo** do que foi implementado.
2. **Arquivos criados/alterados**, com justificativa breve.
3. **Comandos executados e seus resultados**; distinguir testes unitários de integração.
4. **Status real do MySQL**: executado / não executado, erros e evidências.
5. **Riscos, lacunas e decisões pendentes**, sem ocultar problemas.
6. **Link ou identificação do PR/commits** para auditoria do Tech Lead.

**Importante:** implemente apenas a entrega 00. Em caso de ambiguidade funcional, documente a dúvida e não assuma mudanças sem o PO.
