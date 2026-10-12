# JoinUp-POO — Roteiro de apresentação RA2

Preparado em 12/10/2026, America/Sao_Paulo. Base: `7efe29c1529a71e3c1a82a52716a5a32947a61fe`, main após Entrega 06. **Roteiro planejado, ainda não executado nesta frente.** Duração proposta: 13 minutos; ajustar para 10–15 minutos depois de confirmar a duração real com o professor. Enunciado integral não encontrado; identificação da equipe ainda deve ser preenchida no relatório.

## Preparação do ensaio

1. Usar uma instância local de demonstração separada da regressão, banco `joinup_poo`, Java 21/MySQL 8.4.0. O administrador prepara schema v0.2 e migração 06; a aplicação recebe conta somente DML. Não executar testes de integração contra o banco da apresentação e não apagar dados para repetir o roteiro.
2. Para carga nova, conferir base vazia, `JOINUP_ENV=demo`, `JOINUP_DEMO=true`, JDBC loopback e HTTP `127.0.0.1`. `DemoRunner` aplica o seed empacotado e marcador; reinício não reseta dados. Uma base já populada sem marcador é recusada. Conferir instruções e guardas em `README.md`, `DemoRunner` e `ARQUIVOS_HTTP_06.md`; o trecho do README que situa a 06 fora da main é histórico e não exige trocar para a branch antiga.
3. Fixar o commit/JAR usado no ensaio. Registrar origem real (padrão `http://127.0.0.1:8080`), porta MySQL, navegador, data/hora e versões, sem credenciais de banco ou cookies. Conferir que Festival Aurora ainda é futuro: as datas relativas vêm da primeira carga, não são renovadas no reinício.
4. Abrir contextos de navegador **com cookies independentes**: P (participante de ensaio), C (Carla compradora), O (Aurora) e S (atendimento; alternar suporte1/suporte2 por logout). Duas abas do mesmo contexto compartilham sessão e não representam dois usuários independentes.
5. Preparar uma imagem PNG/JPEG/WebP sem dados pessoais, até 1 MiB, 4096 px/dimensão e 4 milhões de pixels. Conferir upload e leitura na instância do ensaio. Não há imagem no seed nem avatar de perfil; a mídia de demonstração é criada pelo upload autorizado.
6. Reservar um participante de ensaio sem ingresso prévio do evento escolhido, com dados fictícios únicos válidos para o formulário. Durante a demo, cadastrar esse perfil e comprar **exatamente uma unidade** de Aurora. Não usar Ana para provar a última venda: ela começa com dois ingressos de Aurora. Conferir também que Carla não tem ingresso/filiação ativa de Aurora na instância usada; se já tiver após outro ensaio, escolher um novo evento criado pela Aurora e refazer a preparação sem reset.
7. Separar SVG resumido, SVG integral com zoom, quatro trechos de código e CI da base. Se a rede falhar durante a aula, mostrar o extrato versionado com SHA/run; não apresentar uma captura como execução ao vivo. Qualquer falha no ensaio deve virar registro na matriz.

Comandos de **preparação**, não executados contra banco por esta frente:

```powershell
# JDK 21/Maven configurados; recursos SQL preparados por administração.
mvn --batch-mode --no-transfer-progress clean verify
# Configure JOINUP_DB_USER e JOINUP_DB_PASSWORD privadamente.
$env:JOINUP_ENV = 'demo'
$env:JOINUP_DEMO = 'true'
$env:JOINUP_HTTP_HOST = '127.0.0.1'
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3306/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
# JOINUP_UPLOAD_DIR: diretório privado absoluto fora do projeto/webroot.
java -jar target/joinup-poo.jar
```

`clean verify` sem perfil MySQL empacota e executa Surefire; não equivale à suíte Failsafe. A regressão completa `-Pmysql-it` pertence a ambiente isolado/CI. O health é liveness, não prova de banco pronto.

## Contas do seed (somente demonstração local)

Fonte: `src/main/resources/db/seed_demo_v0.2.sql` na base. Senha **exclusivamente demo** de todas as contas da tabela: `JoinUpDemo#2026`. Não são contas/segredos de produção. Alterações durante ensaios são preservadas pelo runner; conferir se a senha foi modificada na instância.

| Perfil | E-mail demo | Utilização |
| --- | --- | --- |
| Participante Ana | `ana.demo@joinup.test` | Alternativa para mostrar histórico existente; dois ingressos Aurora, não caso de último ingresso |
| Participante Bruno | `bruno.demo@joinup.test` | Exemplo de ingresso utilizado e histórico de transferência; já tem vínculos do seed |
| Participante Carla | `carla.demo@joinup.test` | Compradora do anúncio de ensaio; conferir ausência de vínculo Aurora antes |
| Organizador Aurora | `aurora.demo@joinup.test` | Evento próprio, criação pela Facade, gestão/mídia e dashboard |
| Organizador Horizonte | `horizonte.demo@joinup.test` | Negação de edição de evento alheio, se houver tempo |
| Suporte Um | `suporte1.demo@joinup.test` | Autoassocia e responde |
| Suporte Dois | `suporte2.demo@joinup.test` | Responde ao mesmo chamado, comprova autoria independente |
| Participante inativo | `inativo.demo@joinup.test` | Login bloqueado; não usar como sessão principal |
| Organizador inativo | `inativa.demo@joinup.test` | Login bloqueado e comércio indisponível do evento correspondente |

Dados/senha do novo cadastro do ensaio são definidos no contexto local; este documento publica apenas credenciais do seed. Não reutilizar informação pessoal real nos formulários.

## Sequência proposta (13 minutos)

| Tempo | Ação e URL | O que mostrar/explicar | Prova relacionada |
| --- | --- | --- | --- |
| 0:00–0:45 | Visitante: `/`, `/eventos.html`, `/comunidades.html` | Problema resolvido, três perfis; catálogo e metadados públicos. Conteúdo social continua protegido | `EventoRoutes`, `ComunidadeRoutes`; matriz V01–V03 |
| 0:45–1:45 | P: `/conta.html`, cadastrar participante fictício de ensaio e entrar | Cadastro realmente produz `Participante`; login usa algoritmo final e cookie HttpOnly. Mostrar perfil retornado, sem expor cookie/hash | `CadastroParticipanteCreator`, `AutenticacaoParticipante`; V04–V06 |
| 1:45–3:00 | P: `/eventos.html?id=1`, compra de uma Pista; `/ingressos.html` e `?aba=compras` | Valor obtido no banco, pagamento **simulado**, ingresso individual e snapshot da compra | `CompraService`; V12–V14 |
| 3:00–4:30 | P: `/comunidades.html?id=1`, Entrar, Feed/Chat | Compra não aderiu automaticamente. Entrar habilita conteúdo; publicar texto ou só imagem. Mostrar Copiar link e comentário se couber | `ComunidadeSocialService`, `ArquivoService`; V23–V29 |
| 4:30–6:15 | P: `/ingressos.html`, anunciar a unidade recém-comprada; C: login Carla e `?aba=revendas`, comprar o anúncio certo | Anunciar mantém acesso do vendedor. Comprar transfere a unidade; P perde conteúdo ao recarregar porque era seu último ingresso. C entra manualmente; histórico do post e compra original continua | `RevendaService`, `IngressoRepositoryJdbc`; V18–V22 |
| 6:15–8:45 | P: `/painel.html?aba=chamados`, abrir chamado; S: suporte1 associa/responde, logout, suporte2 responde; P retorna; S responde; P resolve | Quatro estados, autoria real de dois suportes, associação não restringe resposta; suporte não encerra aguardando usuário. Fechado não recebe mensagem/reabertura | `StatusTicket`, `TicketService`; V31–V36 |
| 8:45–10:30 | O: login Aurora, `/painel.html`, `/eventos.html?meus=1`, criar evento futuro em `?novo=1` | Indicadores de vendas/histórico reais, sem dizer faturamento recebido. Criar com artista/categoria e abrir comunidade gerada; gestão permite imagem, bloqueia categoria vendida | `PainelService`, `EventoFacade`; V08–V11, V37 |
| 10:30–12:30 | Abrir `VISAO_ARQUITETURA.svg`, recortes GoF e CI | Explicar Singleton, Factory Method, Template Method e Facade, um benefício e um custo de cada. Mostrar 468 testes no commit da base e diferença entre unitário e rollback MySQL | Relatório seção 3; `CI_MAIN_7efe29c.txt` |
| 12:30–13:00 | Encerramento e arguição | Limites: sem gateway, carga/TLS/mobile não homologados, idempotência de POST pendente. Indicar relatório/matriz/UML integral | Relatório seções 5–8 |

Os IDs 1 são **somente da carga original conferida**; ingresso/anúncio/post/chamado e evento recém-criado devem usar IDs reais retornados, nunca uma constante presumida. Se a base de ensaio não corresponde ao seed inicial, anotar os IDs corretos antes da aula. Cada contexto precisa recarregar para exibir autorização atual depois da revenda.

Para **10 minutos**, abreviar filtros, comentários/compartilhamento, cadastro extra de suporte e upload de evento, mantendo compra, entrada manual, última revenda, dois autores de suporte e quatro GoF. Para **15 minutos**, incluir logout/negação social, edição de categoria vendida, leitura pública de imagem e comparação de snapshot antes/depois da edição de evento. Não tentar demonstrar concorrência real manualmente em poucos segundos: mostrar o teste e o CI correspondente.

## Perguntas prováveis e respostas sustentadas

| Pergunta | Resposta objetiva e ponto para abrir |
| --- | --- |
| Por que esses quatro GoF? | Foram os quatro aprovados nos requisitos e resolvem configuração, produtos de cadastro, sequência de login e coordenação de evento. Abrir relatório 3; não contar Repository/DTO como GoF |
| O que prova que a Factory não é um switch? | `cadastrar` chama `criarUsuario` abstrato; três subclasses retornam tipos concretos, e `UsuarioRoutes` chama a base. Teste persiste três produtos e revalida suporte |
| O que muda nas subclasses do Template? | Busca em repositório próprio por perfil e hook `instanceof` de coerência. Não afirmar MFA ou política de senha exclusiva por perfil |
| Por que o método é final? | Fixa ordem senha/status/hook/sessão, impedindo sobrescrita da sequência. A classe base é abstrata e não final |
| Por que não Connection Singleton? | Transação, fechamento e estado de conexão não podem ser compartilhados entre requisições; `ConnectionFactory` cria uma conexão por operação |
| Por que JDBC e HttpServer? | Decisão ADR-001 de Java puro torna regras/transações explícitas; custa implementar transporte/sessão/CSRF e testar essa infraestrutura |
| Como evitar estoque negativo e dupla venda? | FOR UPDATE, ordem de locks/revalidação, baixa condicionada e UNIQUE de anúncio disponível; transferência e filiação no mesmo commit. Abrir `ultimaUnidadeDoisCompradoresSomenteUmaCompra` e `doisCompradoresMesmoAnuncioSoUmaTransferenciaSemNovoIngresso` |
| Facade ou God Object? | A fachada integra quatro subsistemas de eventos; não implementa compras, feed ou suporte. Validações/persistência ficam nos colaboradores. N+1 da listagem é tradeoff reconhecido |
| Como comprovar rollback? | `EventoFacadeTest` só verifica ordem/conexão/propagação; `MySqlIntegrationIT.falhaSqlAposEventoReverteTodasEtapas` usa triggers em três etapas e confere tabelas no MySQL |
| Revender o ingresso elimina meus posts? | Não; perde filiação após último elegível, preserva histórico. Comprador entra manualmente; ingresso utilizado ainda permite comunidade pós-evento |
| Quem encerrou um chamado? | Há autor real de cada mensagem, mas não log separado de ator para fechamento sem texto. Suporte associado não é prova de autor de fechamento |
| Os 468 testes garantem tudo? | Comprovam os casos executados naquele SHA; não medem cobertura percentual nem substituem enunciado, E2E manual, mobile ou carga |
| O seed reseta a demo? | Não. Primeiro boot controlado carrega; reinício com marcador conserva alterações/exclusões/datas. Ambiente novo é preparo administrativo, não reset automático |
| Upload e SQL têm commit conjunto? | Vínculo/INSERT social são atômicos no SQL; filesystem não integra transação distribuída. Erros normais são tratados/testados; crash exige reconciliação offline |

## Registro e gate final de sincronização pós-06

- [x] Usar main que já inclui o PR #8 e incorporar classes de mídia/demo ao UML e relatório.
- [x] Conferir CI da base e preservar link/SHA/totais dos logs efetivos.
- [ ] Confirmar enunciado, integrantes, professor e duração.
- [ ] Conferir eventual auditoria externa da 06 sem inferir aprovação a partir do merge.
- [ ] Executar V01–V45 da matriz, registrar evidências e anotar falhas/bloqueios sem transformar planejamento em execução.
- [ ] Ensaiar a sequência completa com cookies independentes e verificar que os snapshots/IDs pertencem ao ensaio.
- [ ] Se outro PR alterar main, comparar fontes/testes/contratos, atualizar todos os documentos e regenerar UML/SVG; registrar novo SHA antes de congelar.
- [ ] Entregar para revisão acadêmica/técnica. Este PR documental permanece sem merge automático.

Os detalhes de rastreabilidade e resultados estão em [RELATORIO_TECNICO_RA2.md](RELATORIO_TECNICO_RA2.md), [MATRIZ_VALIDACAO_FINAL.md](MATRIZ_VALIDACAO_FINAL.md) e [VALIDACAO_DOCUMENTAL.md](VALIDACAO_DOCUMENTAL.md).
