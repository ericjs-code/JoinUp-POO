# V01–V45 — registro de execução da Entrega 08

**Responsável técnico:** Codex. **Data/fuso:** 12/10/2026, America/Sao_Paulo.
**SHA:** `011ecb2dcdd8f9457d2e1c5eeceb00edee018ffd`. Java 21.0.12.1, Maven 3.9.9,
MySQL 8.4.0. JAR SHA-256
`106B4E2CF437AB8594F4845214C4FE0B1BACAFE5F3B4773685EFB61754EB0BBD`.
Instâncias e horários estão no [relatório](RELATORIO_HOMOLOGACAO_FINAL.md).

**D-HTTP:** demo nova 3315, HTTP 8080, cookies independentes. Log
[http.jsonl](evidencias/entrega08/http.jsonl), resultado esperado/observado,
ator, instante, SHA e resposta sanitizada por requisição; assertivas em
[assertivas.jsonl](evidencias/entrega08/assertivas.jsonl), 258 aprovadas.
**I:** regressão em 3311/3312; métodos abaixo foram efetivamente executados no
checkout dessa revisão. [casos-automatizados.json](evidencias/entrega08/casos-automatizados.json)
conserva cada invocação e resultado extraídos dos XML, inclusive parametrizações.
**D-UI:** sanidade visual, três perfis em login/logout sequencial; capturas no
relatório. Não equivale a navegadores simultâneos nem ensaio coletivo.

Abreviações de suítes: **M** = `MySqlIntegrationIT`, **C** = `ComercioMySqlIT`,
**S** = `ComunidadeMySqlIT`, **T** = `SuporteMySqlIT`, **P** = `PaineisMySqlIT`,
**A** = `ZArquivosMySqlIT`, **B** = `ZDemoMySqlIT`. Todas estão em
`src/test/java/br/com/joinup/integration`. “APROVADO automático” refere-se às
ações/asserções de integração desta revisão; não afirma execução do formulário
inteiro no navegador. Os testes históricos não foram reutilizados como resultado.

| Caso | Execução e resultado observado nesta revisão | Evidência efetiva / método executado | Resultado integral |
| --- | --- | --- | --- |
| V01 | D-HTTP/I: quatro eventos do seed, filtros, paginação, detalhe e lineup públicos. D-UI: início/catálogo/detalhe | M.`eventosCatalogosPublicosSeedFiltrosPaginacaoEPrivacidade`; HTTP V01–V03; captura visitante-evento | EXECUTADO / APROVADO (automático + sanidade UI) |
| V02 | D-HTTP/I/UI: metadados públicos; visitante não recebe chat/postagens | S.`descobertaPublicaNaoVazaConteudoEEntradaSaidaReentrada`; HTTP; visitante-mobile | EXECUTADO / APROVADO |
| V03 | D-HTTP/I: feed/chat/anúncios 401; arquivo social 401 após upload válido | HTTP V01–V03/V23–V29; A.`privadoExigeSessaoFiliacaoIngressoDonoEHeaders`; C.`autorizacaoTodasRotasCsrfIdorEPrecoNaoControladoPeloCliente` | EXECUTADO / APROVADO automático |
| V04 | D-HTTP/I: participante 5/organizador 4 criados 201, campos de perfil inválido rejeitados em I, produtos concretos persistidos | M.`factoryMethodTresProdutosPersistidosESuporteRevalidaAutorizacao`; `cadastroDuplicadoComprimentosFormatosESqlInjection`; HTTP V04–V06 | EXECUTADO / APROVADO automático |
| V05 | I: visitante/participante 401/403, suporte cria suporte e segundo suporte também cadastra | M.`suporte401403CadastroESegundoSuportePodeCadastrar` | EXECUTADO / APROVADO automático |
| V06 | D-HTTP/I/UI: três perfis 200; senha errada 401, inativos 403, cookie HttpOnly; logout 204, cookie revogado 401 | M.`loginCorretoErradoIdentidadeCookieEHashPersistido`, `contaInativaNaoAutentica`, `sessaoExpiradaRevogadaLogoutEReuso`; HTTP V04–V06/V06-logout | EXECUTADO / APROVADO |
| V07 | I: atualização própria; forja/IDOR negados, nenhum terceiro alterado | M.`perfilProprioIdorEAtualizacaoDosCamposHerdados` | EXECUTADO / APROVADO automático |
| V08 | D-HTTP/I: evento 5, dois artistas, duas categorias, comunidade 5 coerentes, dono isolado | M.`facadeCriaEventoLineupDuasCategoriasComunidadeUnicaEListaSomenteDono`; HTTP V08 | EXECUTADO / APROVADO automático |
| V09 | D-HTTP/I: edição/reagendamento futuro após venda 200, terceiro 403, passado 422, atual/iniciado bloqueados em I | M.`eventoComVendasPodeEditarReagendarMasSnapshotsIntegraisPermanecem`, `destinoPassadoAtualInvalidoEOffsetEquivalentePersistidoEmUtc`, `inicioInclusiveBloqueiaEdicaoReagendamentoECategorias`; HTTP V09–V10–V13 | EXECUTADO / APROVADO automático |
| V10 | D-HTTP/I/UI: VIP nunca vendida editada, Extra criada; Pista vendida 409 e controles desabilitados na UI | M.`categoriaSemVendasEditavelENovaPermitidaMesmoComOutrasVendidas`, `vendaHistoricaBloqueiaTodosCamposMesmoSemContadorOuComIngressoCancelado`; organizador-gestao | EXECUTADO / APROVADO |
| V11 | I: início inclusive e revalidação após lock impediram mutação parcial | M.`inicioInclusiveBloqueiaEdicaoReagendamentoECategorias`, `concorrenciaRevalidaInicioDepoisDeEsperarLockDeEvento` | EXECUTADO / APROVADO automático |
| V12 | D-HTTP/I: compra 7 total 25,50, ingresso 8, pagamento simulado; estoque Pista 1→0 | C.`variosTiposTotalExatoSnapshotsEmissaoEstoqueHistoricoEPrivacidade`; HTTP V12–V14 | EXECUTADO / APROVADO automático |
| V13 | D-HTTP/I/UI: histórico JSON idêntico após edição; ingresso mostra nome/data atuais e snapshot original | C.`variosTiposTotalExatoSnapshotsEmissaoEstoqueHistoricoEPrivacidade`; M.`eventoComVendasPodeEditarReagendarMasSnapshotsIntegraisPermanecem`; participante-ingressos | EXECUTADO / APROVADO |
| V14 | D-HTTP/I: sem estoque, passado e organizador inativo 409, nenhum efeito indevido | C.`elegibilidadeDiretaRevalidadaNoServidor`, `limiteDeTotalContadorEEstoqueZeroProtegidos`; HTTP V12–V14 | EXECUTADO / APROVADO automático |
| V15 | I: dois compradores, última unidade, somente uma emissão/compra, estoque 0 | C.`ultimaUnidadeDoisCompradoresSomenteUmaCompra` | EXECUTADO / APROVADO automático |
| V16 | I: falhas após baixa em compra/item/ingresso provocaram rollback integral | C.`rollbackCompraDepoisDaBaixaPorTrigger` (3 invocações) | EXECUTADO / APROVADO automático |
| V17 | D-HTTP/I: O/S 403, visitante 401, P/C 200 nas revendas, sem payload de terceiros | C.`autorizacaoTodasRotasCsrfIdorEPrecoNaoControladoPeloCliente`; HTTP V17–V20 | EXECUTADO / APROVADO automático |
| V18 | D-HTTP/I: anúncio próprio 201; duplicação 409, compra própria 403; feed permaneceu 200. Alheio/inelegíveis negados em I | C.`anuncioSomenteIngressoElegivelProprioSemDuplicacao`; HTTP V17–V20 | EXECUTADO / APROVADO automático |
| V19 | D-HTTP/I: anúncio 5 cancelado, novo 6 por 29; histórico/linha anterior preservados | C.`loteAnuncioAtomicoUniqueMySqlECancelarReanunciarHistorico`; HTTP V17–V20 | EXECUTADO / APROVADO automático |
| V20 | D-HTTP/I/UI: Carla recebeu mesmo ingresso 8; P 404 no detalhe e 403 social/arquivo; C 403 antes de entrada manual, 200 depois; compra original intacta | C.`comunidadeSomenteSaiAposVendaUltimoElegivel`; HTTP V17–V20; participante-ingressos/feed | EXECUTADO / APROVADO |
| V21 | I: mantém filiação quando outro ativo/utilizado existe; dois compradores de anúncio geram uma transferência, sem novo ingresso | C.`comunidadeSomenteSaiAposVendaUltimoElegivel`, `doisCompradoresMesmoAnuncioSoUmaTransferenciaSemNovoIngresso` | EXECUTADO / APROVADO automático |
| V22 | I: rollback de dono/anúncio/filiação nas falhas por trigger | C.`rollbackRevendaPorTriggerIncluiTransferenciaAnuncioEMembro` | EXECUTADO / APROVADO automático |
| V23 | D-HTTP/I: ingresso sozinho 403; entrar 200, sair bloqueia, reentrada elegível 200; comprador não entra automaticamente | S.`descobertaPublicaNaoVazaConteudoEEntradaSaidaReentrada`; HTTP V23–V25–V26–V29 | EXECUTADO / APROVADO automático |
| V24 | I: ativo/utilizado pós-evento habilitam conteúdo com filiação; cancelado sem outro elegível nega; comércio passado bloqueado | S.`posEventoIngressoElegivelEComercioBloqueado`; C.`passadoBloqueiaCompraERevendaMasPreservaIngressoUtilizadoEMembro` | EXECUTADO / APROVADO automático |
| V25 | D-HTTP/I: mensagem/resposta local 201; mensagem de outra sala/inexistente 404 igual, sem INSERT | S.`respostaSempreNaMesmaComunidade` (8 invocações); HTTP V23–V25–V26–V29 | EXECUTADO / APROVADO automático |
| V26 | D-HTTP/I/UI parcial: post/like idempotente/comentário/link protegido passaram. Botão exibiu “Link copiado”, mas clipboard da ferramenta sem valor verificável e fallback não provocado | HTTP; S.`bordasTextoSemEdicaoExclusaoEAutoriaComentarios`; participante-feed; **não prova cópia efetiva/fallback** | NÃO EXECUTADO integralmente; parcelas aprovadas |
| V27 | I: dono lê feed/chat/envia mensagem com autoria organizador, não cria post/comentário; terceiros/suporte negados | S.`organizadorDonoMensagemComAutorCorretoEFeedSomenteLeitura`, `permissoesEmTodasAsOperacoes` | EXECUTADO / APROVADO automático |
| V28 | D-HTTP/I/UI: PNG público persistido no evento/catálogo; formulário fez novo upload; JPEG/WebP, dono/início/conta verificados em I | A.`jpegWebpUploadHttpNormalizados`, `eventoPublicoCatalogoComunidadeEEdicaoPosterior`, `donoEventoFuturoEContaAtivaObrigatorios`; reinicio.txt; visitante-evento | EXECUTADO / APROVADO (formatos JPEG/WebP automáticos) |
| V29 | D-HTTP/I/UI: post só imagem/chat com imagem 201; visitante 401, suporte/ex-membro 403, referência reutilizada 404 | A.`postagemSomenteImagemNaoLiberaPostVazio`, `mensagemComImagem`, `pendingPreviewApenasDonoEReusoCruzadoRecusado`, `ultimaRevendaInvalidaLeituraEVinculacaoDePending`; participante-feed/chat | EXECUTADO / APROVADO |
| V30 | I: interleavings reais social/upload/vinculação × revenda; revalidação sob lock após perda de elegibilidade | S.`revendaUltimoIngressoSerializaComSocial`, `vendaPrimeiroRevalidaTodasAsMutacoes`; A.`revendaConcorrenteComUploadEVinculacao`, `perdaElegibilidadeDuranteUploadRevalidadaAposLock` | EXECUTADO / APROVADO automático |
| V31 | D-HTTP/I: chamados 4/5 P/O com primeira mensagem e vínculo; terceiro 404 | T.`aberturaAutoriaVinculoUnicoEHistorico`, `titularidadeDetalheRetornoResolucaoIDOR`; HTTP V31–V34 | EXECUTADO / APROVADO automático |
| V32 | D-HTTP/I/UI: Um associado, Um/Dois responderam; autores #1/#2 preservados e aguardando_usuario | T.`doisSuportesAutoriaRealAssociacaoNaoRestringeENaoMudaEstado`; HTTP; suporte-autoria | EXECUTADO / APROVADO |
| V33 | D-HTTP/I: Retornar → em_atendimento, Responder → aguardando, Resolver → fechado; fora do estado 409 | T.`matrizCompleta` (16 invocações), `limitesUnicodeETextoObrigatorio`; HTTP V31–V34; StatusTicketTest | EXECUTADO / APROVADO automático |
| V34 | D-HTTP/I/UI: encerra aberto/em_atendimento, aguardando/fechado negados; fechado terminal e associação não reabre | T.`matrizCompleta`, `fechadoTerminalSemRotasDeReaberturaEdicaoOuExclusao`; HTTP; suporte-autoria | EXECUTADO / APROVADO |
| V35 | I: trigger após ticket/transição/mensagem reverte todos os efeitos | T.`rollbackTriggerDepoisDeCriarTicketOuTransicionar` | EXECUTADO / APROVADO automático |
| V36 | I: resolver×retornar, suporte×solicitante e duas respostas conservaram somente transições válidas/histórico | T.`resolverVersusRetornarConcorrentesSoUmaTransicao`, `disputaSuporteSolicitanteRevalidaEstadoAtual`, `duasRespostasConcorrentesNaoPerdemMensagem` | EXECUTADO / APROVADO automático |
| V37 | D-HTTP/I/UI: vendedor 0 ingressos/1 compra; O 1 evento/25,50 histórico; suporte autoria própria separada; terceiro não muda escopo | P.`participantePosCompraRevendaEComunidadeAcessoReal`, `organizadoresIsoladosHistoricoSemMultiplicarItensECategorias`, `metricasSuporteGlobalEAutoriaDiferenteDaAssociacao`; HTTP; painéis capturados | EXECUTADO / APROVADO |
| V38 | I: desativação/revogação/cancelamento atômicos, reativação conserva PK/histórico e não reanuncia; O inativo conserva anúncio e permite cancelamento | M.`desativacaoCancelaTodosAnunciosRevogaTodasSessoesPreservaHistoricoEReativacaoAudita`, `organizadorDesativadoMantemEventosComunidadesAnuncios`; C.`organizadorInativoPreservaAnuncioCancelarPermitidoReativarSoDisponivel` | EXECUTADO / APROVADO automático |
| V39 | D-HTTP/I/UI: origem ausente/alheia 403, IDOR/JSON negados; texto `<img src=x onerror=alert(1)>` exibido literalmente no chat, sem alerta | M.`csrfNegadoEmTodasAsMutacoesIncluindoLoginECadastro`, `cadastroDuplicadoComprimentosFormatosESqlInjection`; HTTP V37–V39; participante-chat | EXECUTADO / APROVADO |
| V40 | I: corpo marginal/chunked, MIME/bytes/nomes/headers e referências cruzadas recusados; requisição válida posterior preservada | HttpFoundationTest; ImagemSeguraTest; A.`limiteMesmoEmChunked`, `mimeForjado`, `bytesInvalidos`, `nomesInvalidos`, `headersSuspeitos`, `pendingPreviewApenasDonoEReusoCruzadoRecusado` | EXECUTADO / APROVADO automático |
| V41 | D/I: boot novo, marcador único, snapshot completo preservado no reinício; false não conecta/aplica seed; três perfis autenticam | B.`trueBootstrapCargaAutomaticaEReinicioIdentico`, `reiniciarPreservaNomeSenhaDesativacaoEDadosRemovidos`, `falseNaoAbreBancoNemAplicaSeed`, `tresPerfisAutenticamComCargaDoBootstrapReal`; reinicio.txt | EXECUTADO / APROVADO automático + processo demo |
| V42 | I: base populada sem marcador e ambiente local/prod com demo recusados; falha no seed reverte carga/marcador; DML sem DDL | B.`trueRecusaBasePopuladaSemProveniencia`, `perfilProdOuLocalSemMarcadorDeAmbienteRecusados`, `erroDuranteSeedReverteTudoEMarcador`, `dmlNaoPodeMigrarSchema`; ConfiguracaoSistemaTest/SqlDemoTest para URL/host não locais | EXECUTADO / APROVADO automático |
| V43 | I/inspeção: 468 casos aprovados; Singleton, três creators, Template e Facade real/rollback ligados ao UML; src/pom/workflow idênticos à origem do inventário | maven-mysql.txt; ConfiguracaoSistemaTest; M.`factoryMethodTresProdutosPersistidosESuporteRevalidaAutorizacao`, `templateMethodSequenciaRealInterrompeAntesDaSessao`, `falhaSqlAposEventoReverteTodasEtapas`; EventoFacadeTest; uml-check.txt | EXECUTADO / APROVADO (CI da main conferido; head do PR conferido separadamente no fechamento) |
| V44 | M parcial: suporte/conta/metadados em 390×844 capturados; não realizadas cinco telas por perfis, teclado integral nem leitor de tela | suporte-mobile, conta-mobile, visitante-mobile; **não é certificação de acessibilidade** | NÃO EXECUTADO integralmente |
| V45 | Não houve ensaio cronometrado com estudantes e quatro contextos de navegador. Duração permitida não fornecida | Identificação informada pelo PO; roteiro é preparação e sanidade técnica, sem registro de participação coletiva | NÃO EXECUTADO |

**Totais:** 42 executados/aprovados, 0 reprovados por defeito confirmado da
aplicação, 3 sem execução integral. As correções do roteiro de teste e seus
resultados iniciais estão no relatório; não são omitidas nem contadas como
homologação negativa de uma regra que não foi corretamente exercitada.

**Recomendação:** GO COM RESSALVAS para revisão técnica local, NO-GO para
afirmar conclusão acadêmica integral. Sem merge; sem execução das Entregas 09/10.
