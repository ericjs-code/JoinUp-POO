# Inventário de classes e assinaturas RA2

Base: `7efe29c1529a71e3c1a82a52716a5a32947a61fe`. Extração da árvore sintática Java 21 (`JavacTask.parse`), sem regex para interpretar declarações. Dependências de corpo conferidas por `jdeps -verbose:class -filter:none --ignore-missing-deps target/classes`, compiladas por `mvn test`.

**123 tipos nomeados**, em **117 arquivos Java**; inclui classes internas, records, enums e interfaces; não inclui classes anônimas/lambdas nem tipos de bibliotecas. O diagrama geral contém todos esses tipos. 529 relações entre tipos do projeto.

Este inventário registra caminhos e linhas da base. Após novas alterações, conferir novamente as assinaturas; números de linha não são fixos.

## arquivos

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ArmazenamentoLocal` | CLASS | [src/main/java/br/com/joinup/arquivos/ArmazenamentoLocal.java:10](../../src/main/java/br/com/joinup/arquivos/ArmazenamentoLocal.java) |
| `ArquivoRoutes` | CLASS | [src/main/java/br/com/joinup/arquivos/ArquivoRoutes.java:9](../../src/main/java/br/com/joinup/arquivos/ArquivoRoutes.java) |
| `ArquivoService` | CLASS | [src/main/java/br/com/joinup/arquivos/ArquivoService.java:16](../../src/main/java/br/com/joinup/arquivos/ArquivoService.java) |
| `ArquivoService.ArquivoView` | RECORD | [src/main/java/br/com/joinup/arquivos/ArquivoService.java:27](../../src/main/java/br/com/joinup/arquivos/ArquivoService.java) |
| `ImagemSegura` | CLASS | [src/main/java/br/com/joinup/arquivos/ImagemSegura.java:9](../../src/main/java/br/com/joinup/arquivos/ImagemSegura.java) |
| `MultipartImagem` | RECORD | [src/main/java/br/com/joinup/arquivos/MultipartImagem.java:9](../../src/main/java/br/com/joinup/arquivos/MultipartImagem.java) |

## auth

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `AutenticacaoOrganizador` | CLASS | [src/main/java/br/com/joinup/auth/AutenticacaoOrganizador.java:8](../../src/main/java/br/com/joinup/auth/AutenticacaoOrganizador.java) |
| `AutenticacaoParticipante` | CLASS | [src/main/java/br/com/joinup/auth/AutenticacaoParticipante.java:8](../../src/main/java/br/com/joinup/auth/AutenticacaoParticipante.java) |
| `AutenticacaoSuporte` | CLASS | [src/main/java/br/com/joinup/auth/AutenticacaoSuporte.java:8](../../src/main/java/br/com/joinup/auth/AutenticacaoSuporte.java) |
| `AutenticacaoTemplate` | CLASS | [src/main/java/br/com/joinup/auth/AutenticacaoTemplate.java:11](../../src/main/java/br/com/joinup/auth/AutenticacaoTemplate.java) |
| `GerenciadorSessao` | CLASS | [src/main/java/br/com/joinup/auth/GerenciadorSessao.java:12](../../src/main/java/br/com/joinup/auth/GerenciadorSessao.java) |
| `SegurancaHttp` | CLASS | [src/main/java/br/com/joinup/auth/SegurancaHttp.java:9](../../src/main/java/br/com/joinup/auth/SegurancaHttp.java) |
| `SenhasPbkdf2` | CLASS | [src/main/java/br/com/joinup/auth/SenhasPbkdf2.java:9](../../src/main/java/br/com/joinup/auth/SenhasPbkdf2.java) |
| `SessaoEmitida` | RECORD | [src/main/java/br/com/joinup/auth/SessaoEmitida.java:7](../../src/main/java/br/com/joinup/auth/SessaoEmitida.java) |
| `SessaoRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/auth/SessaoRepositoryJdbc.java:6](../../src/main/java/br/com/joinup/auth/SessaoRepositoryJdbc.java) |
| `SessaoRepositoryJdbc.Titular` | RECORD | [src/main/java/br/com/joinup/auth/SessaoRepositoryJdbc.java:7](../../src/main/java/br/com/joinup/auth/SessaoRepositoryJdbc.java) |
| `VerificadorSenha` | INTERFACE | [src/main/java/br/com/joinup/auth/VerificadorSenha.java:3](../../src/main/java/br/com/joinup/auth/VerificadorSenha.java) |

## bootstrap

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ApplicationBootstrap` | CLASS | [src/main/java/br/com/joinup/bootstrap/ApplicationBootstrap.java:28](../../src/main/java/br/com/joinup/bootstrap/ApplicationBootstrap.java) |

## br.com.joinup

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `Main` | CLASS | [src/main/java/br/com/joinup/Main.java:6](../../src/main/java/br/com/joinup/Main.java) |

## compras.domain

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `EventoComercial` | RECORD | [src/main/java/br/com/joinup/compras/domain/EventoComercial.java:5](../../src/main/java/br/com/joinup/compras/domain/EventoComercial.java) |
| `Ingresso` | RECORD | [src/main/java/br/com/joinup/compras/domain/Ingresso.java:3](../../src/main/java/br/com/joinup/compras/domain/Ingresso.java) |

## compras.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `CompraDados` | RECORD | [src/main/java/br/com/joinup/compras/dto/CompraDados.java:5](../../src/main/java/br/com/joinup/compras/dto/CompraDados.java) |
| `CompraDados.Item` | RECORD | [src/main/java/br/com/joinup/compras/dto/CompraDados.java:6](../../src/main/java/br/com/joinup/compras/dto/CompraDados.java) |

## compras.http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ComercioRoutes` | CLASS | [src/main/java/br/com/joinup/compras/http/ComercioRoutes.java:16](../../src/main/java/br/com/joinup/compras/http/ComercioRoutes.java) |

## compras.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `CompraRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/compras/repository/CompraRepositoryJdbc.java:10](../../src/main/java/br/com/joinup/compras/repository/CompraRepositoryJdbc.java) |
| `IngressoRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/compras/repository/IngressoRepositoryJdbc.java:8](../../src/main/java/br/com/joinup/compras/repository/IngressoRepositoryJdbc.java) |
| `JdbcComercio` | CLASS | [src/main/java/br/com/joinup/compras/repository/JdbcComercio.java:7](../../src/main/java/br/com/joinup/compras/repository/JdbcComercio.java) |

## compras.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `CompraService` | CLASS | [src/main/java/br/com/joinup/compras/service/CompraService.java:17](../../src/main/java/br/com/joinup/compras/service/CompraService.java) |
| `ValidacaoComercio` | CLASS | [src/main/java/br/com/joinup/compras/service/ValidacaoComercio.java:10](../../src/main/java/br/com/joinup/compras/service/ValidacaoComercio.java) |

## comunidades.domain

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `Comentario` | RECORD | [src/main/java/br/com/joinup/comunidades/domain/Comentario.java:5](../../src/main/java/br/com/joinup/comunidades/domain/Comentario.java) |
| `ComunidadeResumo` | RECORD | [src/main/java/br/com/joinup/comunidades/domain/ComunidadeResumo.java:5](../../src/main/java/br/com/joinup/comunidades/domain/ComunidadeResumo.java) |
| `MensagemComunidade` | RECORD | [src/main/java/br/com/joinup/comunidades/domain/MensagemComunidade.java:5](../../src/main/java/br/com/joinup/comunidades/domain/MensagemComunidade.java) |
| `Postagem` | RECORD | [src/main/java/br/com/joinup/comunidades/domain/Postagem.java:5](../../src/main/java/br/com/joinup/comunidades/domain/Postagem.java) |

## comunidades.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ComunidadeDetalheView` | RECORD | [src/main/java/br/com/joinup/comunidades/dto/ComunidadeDetalheView.java:5](../../src/main/java/br/com/joinup/comunidades/dto/ComunidadeDetalheView.java) |
| `CriarComentarioRequest` | RECORD | [src/main/java/br/com/joinup/comunidades/dto/CriarComentarioRequest.java:3](../../src/main/java/br/com/joinup/comunidades/dto/CriarComentarioRequest.java) |
| `CriarPostagemRequest` | RECORD | [src/main/java/br/com/joinup/comunidades/dto/CriarPostagemRequest.java:3](../../src/main/java/br/com/joinup/comunidades/dto/CriarPostagemRequest.java) |
| `EnviarMensagemRequest` | RECORD | [src/main/java/br/com/joinup/comunidades/dto/EnviarMensagemRequest.java:3](../../src/main/java/br/com/joinup/comunidades/dto/EnviarMensagemRequest.java) |

## comunidades.http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ComunidadeRoutes` | CLASS | [src/main/java/br/com/joinup/comunidades/http/ComunidadeRoutes.java:14](../../src/main/java/br/com/joinup/comunidades/http/ComunidadeRoutes.java) |

## comunidades.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ComunidadeSocialRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/comunidades/repository/ComunidadeSocialRepositoryJdbc.java:8](../../src/main/java/br/com/joinup/comunidades/repository/ComunidadeSocialRepositoryJdbc.java) |
| `JdbcComunidades` | CLASS | [src/main/java/br/com/joinup/comunidades/repository/JdbcComunidades.java:6](../../src/main/java/br/com/joinup/comunidades/repository/JdbcComunidades.java) |

## comunidades.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ComunidadeSocialService` | CLASS | [src/main/java/br/com/joinup/comunidades/service/ComunidadeSocialService.java:15](../../src/main/java/br/com/joinup/comunidades/service/ComunidadeSocialService.java) |

## config

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ConfiguracaoSistema` | CLASS | [src/main/java/br/com/joinup/config/ConfiguracaoSistema.java:6](../../src/main/java/br/com/joinup/config/ConfiguracaoSistema.java) |
| `ConfiguracaoSistema.Holder` | CLASS | [src/main/java/br/com/joinup/config/ConfiguracaoSistema.java:45](../../src/main/java/br/com/joinup/config/ConfiguracaoSistema.java) |

## demo

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `DemoRunner` | CLASS | [src/main/java/br/com/joinup/demo/DemoRunner.java:11](../../src/main/java/br/com/joinup/demo/DemoRunner.java) |
| `SqlDemo` | CLASS | [src/main/java/br/com/joinup/demo/SqlDemo.java:6](../../src/main/java/br/com/joinup/demo/SqlDemo.java) |

## eventos.domain

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `Comunidade` | RECORD | [src/main/java/br/com/joinup/eventos/domain/Comunidade.java:4](../../src/main/java/br/com/joinup/eventos/domain/Comunidade.java) |
| `Evento` | RECORD | [src/main/java/br/com/joinup/eventos/domain/Evento.java:6](../../src/main/java/br/com/joinup/eventos/domain/Evento.java) |
| `ItemCatalogo` | RECORD | [src/main/java/br/com/joinup/eventos/domain/ItemCatalogo.java:4](../../src/main/java/br/com/joinup/eventos/domain/ItemCatalogo.java) |
| `TipoIngresso` | RECORD | [src/main/java/br/com/joinup/eventos/domain/TipoIngresso.java:5](../../src/main/java/br/com/joinup/eventos/domain/TipoIngresso.java) |

## eventos.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `CriarEventoDados` | RECORD | [src/main/java/br/com/joinup/eventos/dto/CriarEventoDados.java:6](../../src/main/java/br/com/joinup/eventos/dto/CriarEventoDados.java) |
| `EditarEventoDados` | RECORD | [src/main/java/br/com/joinup/eventos/dto/EditarEventoDados.java:7](../../src/main/java/br/com/joinup/eventos/dto/EditarEventoDados.java) |
| `EventoView` | RECORD | [src/main/java/br/com/joinup/eventos/dto/EventoView.java:8](../../src/main/java/br/com/joinup/eventos/dto/EventoView.java) |
| `FiltroEventos` | RECORD | [src/main/java/br/com/joinup/eventos/dto/FiltroEventos.java:7](../../src/main/java/br/com/joinup/eventos/dto/FiltroEventos.java) |
| `TipoIngressoDados` | RECORD | [src/main/java/br/com/joinup/eventos/dto/TipoIngressoDados.java:5](../../src/main/java/br/com/joinup/eventos/dto/TipoIngressoDados.java) |

## eventos.facade

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `EventoFacade` | CLASS | [src/main/java/br/com/joinup/eventos/facade/EventoFacade.java:14](../../src/main/java/br/com/joinup/eventos/facade/EventoFacade.java) |

## eventos.http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `EventoRoutes` | CLASS | [src/main/java/br/com/joinup/eventos/http/EventoRoutes.java:13](../../src/main/java/br/com/joinup/eventos/http/EventoRoutes.java) |

## eventos.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ArtistaRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/eventos/repository/ArtistaRepositoryJdbc.java:7](../../src/main/java/br/com/joinup/eventos/repository/ArtistaRepositoryJdbc.java) |
| `ComunidadeRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/eventos/repository/ComunidadeRepositoryJdbc.java:6](../../src/main/java/br/com/joinup/eventos/repository/ComunidadeRepositoryJdbc.java) |
| `EventoRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/eventos/repository/EventoRepositoryJdbc.java:9](../../src/main/java/br/com/joinup/eventos/repository/EventoRepositoryJdbc.java) |
| `JdbcEventos` | CLASS | [src/main/java/br/com/joinup/eventos/repository/JdbcEventos.java:6](../../src/main/java/br/com/joinup/eventos/repository/JdbcEventos.java) |
| `TipoIngressoRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/eventos/repository/TipoIngressoRepositoryJdbc.java:8](../../src/main/java/br/com/joinup/eventos/repository/TipoIngressoRepositoryJdbc.java) |

## eventos.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ArtistaService` | CLASS | [src/main/java/br/com/joinup/eventos/service/ArtistaService.java:9](../../src/main/java/br/com/joinup/eventos/service/ArtistaService.java) |
| `ComunidadeService` | CLASS | [src/main/java/br/com/joinup/eventos/service/ComunidadeService.java:7](../../src/main/java/br/com/joinup/eventos/service/ComunidadeService.java) |
| `EventoService` | CLASS | [src/main/java/br/com/joinup/eventos/service/EventoService.java:12](../../src/main/java/br/com/joinup/eventos/service/EventoService.java) |
| `TipoIngressoService` | CLASS | [src/main/java/br/com/joinup/eventos/service/TipoIngressoService.java:12](../../src/main/java/br/com/joinup/eventos/service/TipoIngressoService.java) |
| `ValidacaoEvento` | CLASS | [src/main/java/br/com/joinup/eventos/service/ValidacaoEvento.java:8](../../src/main/java/br/com/joinup/eventos/service/ValidacaoEvento.java) |

## http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `HealthHandler` | CLASS | [src/main/java/br/com/joinup/http/HealthHandler.java:8](../../src/main/java/br/com/joinup/http/HealthHandler.java) |
| `HttpException` | CLASS | [src/main/java/br/com/joinup/http/HttpException.java:4](../../src/main/java/br/com/joinup/http/HttpException.java) |
| `HttpResponses` | CLASS | [src/main/java/br/com/joinup/http/HttpResponses.java:7](../../src/main/java/br/com/joinup/http/HttpResponses.java) |
| `JsonCodec` | CLASS | [src/main/java/br/com/joinup/http/JsonCodec.java:12](../../src/main/java/br/com/joinup/http/JsonCodec.java) |
| `ParameterizedRouteHandler` | INTERFACE | [src/main/java/br/com/joinup/http/ParameterizedRouteHandler.java:6](../../src/main/java/br/com/joinup/http/ParameterizedRouteHandler.java) |
| `RequestBody` | CLASS | [src/main/java/br/com/joinup/http/RequestBody.java:6](../../src/main/java/br/com/joinup/http/RequestBody.java) |
| `RequestPath` | CLASS | [src/main/java/br/com/joinup/http/RequestPath.java:5](../../src/main/java/br/com/joinup/http/RequestPath.java) |
| `RouteHandler` | INTERFACE | [src/main/java/br/com/joinup/http/RouteHandler.java:6](../../src/main/java/br/com/joinup/http/RouteHandler.java) |
| `RouteParameters` | RECORD | [src/main/java/br/com/joinup/http/RouteParameters.java:9](../../src/main/java/br/com/joinup/http/RouteParameters.java) |
| `Router` | CLASS | [src/main/java/br/com/joinup/http/Router.java:9](../../src/main/java/br/com/joinup/http/Router.java) |
| `Router.Rota` | RECORD | [src/main/java/br/com/joinup/http/Router.java:11](../../src/main/java/br/com/joinup/http/Router.java) |
| `StaticFileHandler` | CLASS | [src/main/java/br/com/joinup/http/StaticFileHandler.java:8](../../src/main/java/br/com/joinup/http/StaticFileHandler.java) |

## paineis.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `PainelView` | RECORD | [src/main/java/br/com/joinup/paineis/dto/PainelView.java:8](../../src/main/java/br/com/joinup/paineis/dto/PainelView.java) |

## paineis.http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `PainelRoutes` | CLASS | [src/main/java/br/com/joinup/paineis/http/PainelRoutes.java:9](../../src/main/java/br/com/joinup/paineis/http/PainelRoutes.java) |

## paineis.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `PainelRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/paineis/repository/PainelRepositoryJdbc.java:11](../../src/main/java/br/com/joinup/paineis/repository/PainelRepositoryJdbc.java) |

## paineis.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `PainelService` | CLASS | [src/main/java/br/com/joinup/paineis/service/PainelService.java:12](../../src/main/java/br/com/joinup/paineis/service/PainelService.java) |

## revendas.domain

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `Anuncio` | RECORD | [src/main/java/br/com/joinup/revendas/domain/Anuncio.java:5](../../src/main/java/br/com/joinup/revendas/domain/Anuncio.java) |

## revendas.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `AnuncioDados` | RECORD | [src/main/java/br/com/joinup/revendas/dto/AnuncioDados.java:6](../../src/main/java/br/com/joinup/revendas/dto/AnuncioDados.java) |
| `FiltroRevendas` | RECORD | [src/main/java/br/com/joinup/revendas/dto/FiltroRevendas.java:6](../../src/main/java/br/com/joinup/revendas/dto/FiltroRevendas.java) |

## revendas.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `RevendaRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/revendas/repository/RevendaRepositoryJdbc.java:10](../../src/main/java/br/com/joinup/revendas/repository/RevendaRepositoryJdbc.java) |

## revendas.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `RevendaService` | CLASS | [src/main/java/br/com/joinup/revendas/service/RevendaService.java:19](../../src/main/java/br/com/joinup/revendas/service/RevendaService.java) |

## shared.db

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ConnectionFactory` | CLASS | [src/main/java/br/com/joinup/shared/db/ConnectionFactory.java:9](../../src/main/java/br/com/joinup/shared/db/ConnectionFactory.java) |
| `ExecutorTransacional` | CLASS | [src/main/java/br/com/joinup/shared/db/ExecutorTransacional.java:8](../../src/main/java/br/com/joinup/shared/db/ExecutorTransacional.java) |
| `ExecutorTransacional.Operacao` | INTERFACE | [src/main/java/br/com/joinup/shared/db/ExecutorTransacional.java:11](../../src/main/java/br/com/joinup/shared/db/ExecutorTransacional.java) |

## suporte.domain

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `MensagemTicket` | RECORD | [src/main/java/br/com/joinup/suporte/domain/MensagemTicket.java:5](../../src/main/java/br/com/joinup/suporte/domain/MensagemTicket.java) |
| `StatusTicket` | ENUM | [src/main/java/br/com/joinup/suporte/domain/StatusTicket.java:6](../../src/main/java/br/com/joinup/suporte/domain/StatusTicket.java) |
| `Ticket` | RECORD | [src/main/java/br/com/joinup/suporte/domain/Ticket.java:5](../../src/main/java/br/com/joinup/suporte/domain/Ticket.java) |

## suporte.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `AbrirTicketDados` | RECORD | [src/main/java/br/com/joinup/suporte/dto/AbrirTicketDados.java:2](../../src/main/java/br/com/joinup/suporte/dto/AbrirTicketDados.java) |
| `MensagemTicketDados` | RECORD | [src/main/java/br/com/joinup/suporte/dto/MensagemTicketDados.java:2](../../src/main/java/br/com/joinup/suporte/dto/MensagemTicketDados.java) |
| `PaginaTickets` | RECORD | [src/main/java/br/com/joinup/suporte/dto/PaginaTickets.java:6](../../src/main/java/br/com/joinup/suporte/dto/PaginaTickets.java) |
| `TicketDetalhe` | RECORD | [src/main/java/br/com/joinup/suporte/dto/TicketDetalhe.java:6](../../src/main/java/br/com/joinup/suporte/dto/TicketDetalhe.java) |

## suporte.http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `TicketRoutes` | CLASS | [src/main/java/br/com/joinup/suporte/http/TicketRoutes.java:14](../../src/main/java/br/com/joinup/suporte/http/TicketRoutes.java) |

## suporte.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `TicketRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/suporte/repository/TicketRepositoryJdbc.java:11](../../src/main/java/br/com/joinup/suporte/repository/TicketRepositoryJdbc.java) |

## suporte.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `TicketService` | CLASS | [src/main/java/br/com/joinup/suporte/service/TicketService.java:13](../../src/main/java/br/com/joinup/suporte/service/TicketService.java) |

## usuarios.domain

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `Organizador` | CLASS | [src/main/java/br/com/joinup/usuarios/domain/Organizador.java:3](../../src/main/java/br/com/joinup/usuarios/domain/Organizador.java) |
| `Participante` | CLASS | [src/main/java/br/com/joinup/usuarios/domain/Participante.java:5](../../src/main/java/br/com/joinup/usuarios/domain/Participante.java) |
| `Perfil` | ENUM | [src/main/java/br/com/joinup/usuarios/domain/Perfil.java:4](../../src/main/java/br/com/joinup/usuarios/domain/Perfil.java) |
| `Usuario` | CLASS | [src/main/java/br/com/joinup/usuarios/domain/Usuario.java:6](../../src/main/java/br/com/joinup/usuarios/domain/Usuario.java) |
| `UsuarioSuporte` | CLASS | [src/main/java/br/com/joinup/usuarios/domain/UsuarioSuporte.java:4](../../src/main/java/br/com/joinup/usuarios/domain/UsuarioSuporte.java) |

## usuarios.dto

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `CadastroDados` | RECORD | [src/main/java/br/com/joinup/usuarios/dto/CadastroDados.java:5](../../src/main/java/br/com/joinup/usuarios/dto/CadastroDados.java) |
| `Credenciais` | RECORD | [src/main/java/br/com/joinup/usuarios/dto/Credenciais.java:3](../../src/main/java/br/com/joinup/usuarios/dto/Credenciais.java) |
| `PerfilDados` | RECORD | [src/main/java/br/com/joinup/usuarios/dto/PerfilDados.java:3](../../src/main/java/br/com/joinup/usuarios/dto/PerfilDados.java) |
| `UsuarioView` | RECORD | [src/main/java/br/com/joinup/usuarios/dto/UsuarioView.java:7](../../src/main/java/br/com/joinup/usuarios/dto/UsuarioView.java) |

## usuarios.http

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `UsuarioRoutes` | CLASS | [src/main/java/br/com/joinup/usuarios/http/UsuarioRoutes.java:14](../../src/main/java/br/com/joinup/usuarios/http/UsuarioRoutes.java) |

## usuarios.repository

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `ContaRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/usuarios/repository/ContaRepositoryJdbc.java:6](../../src/main/java/br/com/joinup/usuarios/repository/ContaRepositoryJdbc.java) |
| `OrganizadorRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/usuarios/repository/OrganizadorRepositoryJdbc.java:6](../../src/main/java/br/com/joinup/usuarios/repository/OrganizadorRepositoryJdbc.java) |
| `ParticipanteRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/usuarios/repository/ParticipanteRepositoryJdbc.java:7](../../src/main/java/br/com/joinup/usuarios/repository/ParticipanteRepositoryJdbc.java) |
| `RepositoriosUsuarios` | CLASS | [src/main/java/br/com/joinup/usuarios/repository/RepositoriosUsuarios.java:6](../../src/main/java/br/com/joinup/usuarios/repository/RepositoriosUsuarios.java) |
| `SuporteRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/usuarios/repository/SuporteRepositoryJdbc.java:6](../../src/main/java/br/com/joinup/usuarios/repository/SuporteRepositoryJdbc.java) |
| `UsuarioRepository` | INTERFACE | [src/main/java/br/com/joinup/usuarios/repository/UsuarioRepository.java:8](../../src/main/java/br/com/joinup/usuarios/repository/UsuarioRepository.java) |
| `UsuarioRepositoryJdbc` | CLASS | [src/main/java/br/com/joinup/usuarios/repository/UsuarioRepositoryJdbc.java:8](../../src/main/java/br/com/joinup/usuarios/repository/UsuarioRepositoryJdbc.java) |

## usuarios.service

| Tipo real | Natureza | Fonte na base |
| --- | --- | --- |
| `CadastroOrganizadorCreator` | CLASS | [src/main/java/br/com/joinup/usuarios/service/CadastroOrganizadorCreator.java:9](../../src/main/java/br/com/joinup/usuarios/service/CadastroOrganizadorCreator.java) |
| `CadastroParticipanteCreator` | CLASS | [src/main/java/br/com/joinup/usuarios/service/CadastroParticipanteCreator.java:9](../../src/main/java/br/com/joinup/usuarios/service/CadastroParticipanteCreator.java) |
| `CadastroSuporteCreator` | CLASS | [src/main/java/br/com/joinup/usuarios/service/CadastroSuporteCreator.java:10](../../src/main/java/br/com/joinup/usuarios/service/CadastroSuporteCreator.java) |
| `CadastroUsuarioCreator` | CLASS | [src/main/java/br/com/joinup/usuarios/service/CadastroUsuarioCreator.java:11](../../src/main/java/br/com/joinup/usuarios/service/CadastroUsuarioCreator.java) |
| `ContaService` | CLASS | [src/main/java/br/com/joinup/usuarios/service/ContaService.java:11](../../src/main/java/br/com/joinup/usuarios/service/ContaService.java) |
| `ValidacaoUsuario` | CLASS | [src/main/java/br/com/joinup/usuarios/service/ValidacaoUsuario.java:7](../../src/main/java/br/com/joinup/usuarios/service/ValidacaoUsuario.java) |
