# ADR-001 — Stack Java puro com bibliotecas auxiliares

- **Data:** 10/10/2026
- **Status:** ACEITO PELO PRODUCT OWNER
- **Escopo:** JoinUp-POO
- **Bases:** [Especificação funcional aprovada](../requisitos/ESPECIFICACAO_FUNCIONAL_v0.1.md), [Proposta Java puro](./PROPOSTA_JAVA_PURO_v0.1.md)

## Contexto

O JoinUp-POO deve ser uma nova implementação em Java do JoinUp original, com novo frontend próximo do original, banco independente e quatro padrões GoF demonstráveis (Facade, Template Method, Factory Method e Singleton). O PO rejeitou Spring Boot e Javalin em favor de **Java puro**, mas autorizou bibliotecas auxiliares.

## Decisão

| Área | Tecnologia / opção |
| --- | --- |
| Runtime | Java 21 / JDK |
| Servidor e rotas HTTP | `com.sun.net.httpserver.HttpServer` (módulo `jdk.httpserver`) e roteamento próprio |
| Persistência | JDBC / `java.sql`; consultas SQL explícitas |
| Banco de dados | MySQL 8, independente do banco PHP original |
| Driver JDBC | MySQL Connector/J |
| Serialização JSON | Jackson |
| Gerenciamento de dependências | Maven |
| Testes | JUnit 5 e testes de integração |
| Frontend | HTML/CSS/JavaScript nativos, servidos pelo backend |
| UML | PlantUML |

**Não utilizar:** Spring Boot, Javalin, ORM/JPA/Hibernate, servidor Jakarta Servlet ou framework externo de injeção de dependências. Bibliotecas pontuais podem ser adicionadas com justificativa técnica e sem substituir a arquitetura do sistema, desde que não alterem o acordo de Java puro.

## Consequências e limites

**Vantagens:** dependências reduzidas, classes de domínio e GoF visíveis, maior controle do fluxo HTTP e transacional, demonstração acadêmica direta.

**Custos:** escrever roteamento, autenticação/sessão, validações, manipulação HTTP, autorização, tratamento de exceções e upload de forma explícita. Requer testes desses componentes.

**Regras essenciais:** uso de autenticação/autorizações no backend, hash de senhas, sessão segura, proteção CSRF quando cabível, validação de arquivos, transações JDBC, prevenção de concorrência indevida em estoque e revenda.

**Sem autorização de implementação:** a stack está aprovada; modelo de classes, desenho de banco, contratos HTTP e detalhamento dos quatro GoF continuam sujeitos à validação técnica/PO.
