param(
  [Parameter(Mandatory)][ValidateSet('demo-isolada-entrega08')][string]$Confirmacao,
  [string]$BaseUrl = 'http://127.0.0.1:8080',
  [Parameter(Mandatory)][string]$OutputDir,
  [Parameter(Mandatory)][string]$ImagemPng,
  [Parameter(Mandatory)][string]$Sha
)
# Executar uma vez em demo sacrificial NOVA. Cria contas/evento/comércio/social/tickets;
# não apaga nem reseta dados. Não há retry de mutações. Requer PowerShell 7.
$ErrorActionPreference = 'Stop'
if ([uri]$BaseUrl -isnot [uri] -or ([uri]$BaseUrl).Host -ne '127.0.0.1') { throw 'Exige loopback' }
if (Test-Path "$OutputDir/http.jsonl") { throw 'Evidência já existe; não sobrescrever nem repetir compras' }
New-Item -ItemType Directory -Force $OutputDir | Out-Null
$script:checks = 0
$script:actor = 'visitante'
$script:case = 'P0'
$sessions = @{}
foreach ($name in @('visitante','P','C','O','O2','S1','S2','Bruno','temporario')) {
  $sessions[$name] = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
}
function Check($condition, [string]$description) {
  $script:checks++
  @{instante=[DateTimeOffset]::Now.ToString('o');caso=$script:case;assertiva=$description;aprovado=[bool]$condition} |
    ConvertTo-Json -Compress | Add-Content "$OutputDir/assertivas.jsonl" -Encoding utf8
  if (-not $condition) { throw "Falhou: $description" }
}
function Request([string]$method,[string]$path,[int]$expected,$body=$null,[string]$origin='self',$form=$null) {
  # WebRequestSession guarda headers; limpar impede herdar Origin anterior
  # nos cenários que verificam sua ausência. Cookies seguem no CookieContainer.
  $sessions[$script:actor].Headers.Clear()
  $args08 = @{Uri="$BaseUrl$path";Method=$method;WebSession=$sessions[$script:actor];SkipHttpErrorCheck=$true;TimeoutSec=25}
  if ($origin -eq 'self') { $args08.Headers=@{Origin=$BaseUrl} }
  elseif ($origin) { $args08.Headers=@{Origin=$origin} }
  if ($null -ne $form) {
    # Multipart explícito: PowerShell -Form inclui disposição/headers que não
    # pertencem ao subconjunto estrito documentado pelo servidor.
    $boundary08='JoinUpHomologacao08'
    $stream08=[System.IO.MemoryStream]::new()
    foreach ($field08 in @('contexto','alvoId')) {
      $bytes08=[Text.Encoding]::ASCII.GetBytes("--$boundary08`r`nContent-Disposition: form-data; name=`"$field08`"`r`n`r`n$($form[$field08])`r`n")
      $stream08.Write($bytes08,0,$bytes08.Length)
    }
    $bytes08=[Text.Encoding]::ASCII.GetBytes("--$boundary08`r`nContent-Disposition: form-data; name=`"arquivo`"; filename=`"fixture.png`"`r`nContent-Type: image/png`r`n`r`n")
    $stream08.Write($bytes08,0,$bytes08.Length)
    $bytes08=[IO.File]::ReadAllBytes($form.arquivo.FullName);$stream08.Write($bytes08,0,$bytes08.Length)
    $bytes08=[Text.Encoding]::ASCII.GetBytes("`r`n--$boundary08--`r`n");$stream08.Write($bytes08,0,$bytes08.Length)
    $args08.Body=$stream08.ToArray();$args08.ContentType="multipart/form-data; boundary=$boundary08";$stream08.Dispose()
  }
  elseif ($null -ne $body) { $args08.ContentType='application/json';$args08.Body=ConvertTo-Json -InputObject $body -Depth 12 -Compress }
  $r = Invoke-WebRequest @args08
  $json = $null
  if ($r.Headers['Content-Type'] -like '*application/json*') {
    $json = $r.Content | ConvertFrom-Json -Depth 30
    Check ($r.Content -notmatch '"(?:senha|hashSenha|token)"\s*:') 'Resposta sem senha/hash/token'
  }
  $flags = $null
  if ($r.Headers['Set-Cookie']) { $flags = ($r.Headers['Set-Cookie'] -join ';') -replace 'joinup_sessao=[^;]*','joinup_sessao=[OCULTO]' }
  @{instante=[DateTimeOffset]::Now.ToString('o');sha=$Sha;caso=$script:case;ator=$script:actor;metodo=$method;path=$path;esperado=$expected;http=[int]$r.StatusCode;cookie=$flags;resposta=$json} |
    ConvertTo-Json -Compress -Depth 30 | Add-Content "$OutputDir/http.jsonl" -Encoding utf8
  Check ([int]$r.StatusCode -eq $expected) "$script:actor $method $path HTTP $expected (observado $($r.StatusCode))"
  return $json
}
function Login([string]$actor,[string]$profile,[string]$email,[string]$password='JoinUpDemo#2026') {
  $script:actor=$actor
  $r=Request POST "/api/auth/$profile/login" 200 @{email=$email;senha=$password}
  $cookies=$sessions[$actor].Cookies.GetCookies([uri]$BaseUrl)
  Check (@($cookies | Where-Object { $_.Name -eq 'joinup_sessao' -and $_.HttpOnly }).Count -eq 1) 'Cookie HttpOnly no contexto independente'
  return $r.dados
}
try {
  $script:case='V01-V03'
  foreach ($p in @('/','/conta.html','/eventos.html','/ingressos.html','/comunidades.html','/painel.html','/api/health')) { $null=Request GET $p 200 }
  $catalog=Request GET '/api/eventos' 200
  Check ($catalog.total -eq 4) 'Demo nova possui os quatro eventos do seed'
  $null=Request GET '/api/eventos?busca=Aurora&pagina=1&tamanho=1' 200
  $null=Request GET '/api/artistas' 200
  $null=Request GET '/api/generos-musicais' 200
  $null=Request GET '/api/eventos/1' 200
  $public=Request GET '/api/comunidades/1' 200
  Check ($null -eq $public.dados.mensagens -and $null -eq $public.dados.postagens) 'Metadados não contêm feed/chat'
  foreach ($p in @('/api/comunidades/1/postagens','/api/comunidades/1/mensagens','/api/revendas')) { $null=Request GET $p 401 }

  $script:case='V04-V06'
  $suffix=[DateTimeOffset]::Now.ToUnixTimeSeconds().ToString()
  $pEmail="p$suffix@joinup.test";$oEmail="o$suffix@joinup.test"
  $p=Request POST '/api/participantes' 201 @{nome='Participante ensaio 08';email=$pEmail;senha='JoinUpDemo#2026';cpf="8$suffix";data_nascimento='1998-05-17'}
  $o=Request POST '/api/organizadores' 201 @{nome='Organizador ensaio 08';email=$oEmail;senha='JoinUpDemo#2026';cnpj="0008$suffix"}
  $pIdentity=Login P participantes $pEmail
  $cIdentity=Login C participantes 'carla.demo@joinup.test'
  $oIdentity=Login O organizadores $oEmail
  $null=Login O2 organizadores 'horizonte.demo@joinup.test'
  $null=Login S1 suporte 'suporte1.demo@joinup.test'
  $null=Login S2 suporte 'suporte2.demo@joinup.test'
  $script:actor='temporario'
  $null=Request POST '/api/auth/participantes/login' 401 @{email=$pEmail;senha='SenhaErrada08'}
  $null=Request POST '/api/auth/participantes/login' 403 @{email='inativo.demo@joinup.test';senha='JoinUpDemo#2026'}
  $null=Request POST '/api/auth/organizadores/login' 403 @{email='inativa.demo@joinup.test';senha='JoinUpDemo#2026'}

  $script:case='V08'
  $script:actor='O'
  $event=Request POST '/api/organizador/eventos' 201 @{nome='Homologacao 08';descricao='Evento ficticio isolado';data=[DateTimeOffset]::UtcNow.AddDays(40).ToString('o');localizacao='Arena Ensaio';cidade='Curitiba';estado='PR';cep='80000000';generoId=2;artistaIds=@(1,3);tiposIngresso=@(@{nome='Pista';preco=25.50;quantidade=1},@{nome='VIP';preco=40;quantidade=3})}
  $eid=$event.dados.id;$cid=$event.dados.comunidade.id;$tid=$event.dados.tiposIngresso[0].id
  Check ($event.dados.artistas.Count -eq 2 -and $event.dados.tiposIngresso.Count -eq 2 -and $cid -gt 0) 'Facade cria lineup/categorias/comunidade'

  $script:case='V12-V14'
  $script:actor='P'
  $purchase=Request POST '/api/compras' 201 @{eventoId=$eid;itens=@(@{tipoIngressoId=$tid;quantidade=1})}
  $iid=$purchase.dados.ingressoIds[0]
  Check ($purchase.dados.total -eq 25.50 -and $purchase.dados.pagamento -eq 'simulado') 'Preço de banco e pagamento simulado'
  $historyBefore=Request GET '/api/me/compras' 200
  $ticketBefore=Request GET "/api/me/ingressos/$iid" 200
  $stock=Request GET "/api/eventos/$eid/tipos-ingresso" 200
  Check (($stock.dados | Where-Object id -eq $tid).quantidade -eq 0) 'Baixa única da última unidade'
  $null=Request POST '/api/compras' 409 @{eventoId=$eid;itens=@(@{tipoIngressoId=$tid;quantidade=1})}
  $null=Request POST '/api/compras' 409 @{eventoId=4;itens=@(@{tipoIngressoId=5;quantidade=1})}
  $null=Request POST '/api/compras' 409 @{eventoId=3;itens=@(@{tipoIngressoId=4;quantidade=1})}

  $script:case='V09-V10-V13'
  $script:actor='O'
  $null=Request PATCH "/api/organizador/eventos/$eid" 200 @{nome='Homologacao 08 editada';data=[DateTimeOffset]::UtcNow.AddDays(42).ToString('o')}
  $null=Request PATCH "/api/organizador/eventos/$eid" 422 @{data=[DateTimeOffset]::UtcNow.AddDays(-1).ToString('o')}
  $null=Request PATCH "/api/organizador/eventos/$eid/tipos-ingresso/$tid" 409 @{preco=30}
  $untouched=$event.dados.tiposIngresso[1].id
  $null=Request PATCH "/api/organizador/eventos/$eid/tipos-ingresso/$untouched" 200 @{preco=41}
  $null=Request POST "/api/organizador/eventos/$eid/tipos-ingresso" 201 @{nome='Extra';preco=50;quantidade=2}
  $script:actor='O2'
  $null=Request PATCH "/api/organizador/eventos/$eid" 403 @{nome='Indevido'}
  $script:actor='P'
  $historyAfter=Request GET '/api/me/compras' 200
  Check ((ConvertTo-Json $historyBefore -Depth 20 -Compress) -eq (ConvertTo-Json $historyAfter -Depth 20 -Compress)) 'Histórico não reescrito após edição'
  $ticketAfter=Request GET "/api/me/ingressos/$iid" 200
  Check ($ticketAfter.dados.nomeEvento -eq 'Homologacao 08 editada' -and $ticketAfter.dados.nomeEventoNaCompra -eq $ticketBefore.dados.nomeEventoNaCompra) 'Catálogo atual e snapshot original separados'

  $script:case='V23-V25-V26-V29'
  $null=Request GET "/api/comunidades/$cid/postagens" 403
  $null=Request POST "/api/comunidades/$cid/participacao" 200 @{}
  $null=Request GET "/api/comunidades/$cid/postagens" 200
  $null=Request DELETE "/api/comunidades/$cid/participacao" 200 @{}
  $null=Request GET "/api/comunidades/$cid/mensagens" 403
  $null=Request POST "/api/comunidades/$cid/participacao" 200 @{}
  $postUpload=Request POST '/api/arquivos' 201 $null self @{contexto='postagem';alvoId="$cid";arquivo=Get-Item $ImagemPng}
  $post=Request POST "/api/comunidades/$cid/postagens" 201 @{descricao='';imagem=$postUpload.dados.imagem}
  $postid=$post.dados.id
  $null=Request GET $postUpload.dados.imagem 200
  $null=Request PUT "/api/postagens/$postid/curtida" 200 @{}
  $liked=Request PUT "/api/postagens/$postid/curtida" 200 @{}
  Check ($liked.dados.curtidas -eq 1) 'Curtida idempotente'
  $null=Request DELETE "/api/postagens/$postid/curtida" 200 @{}
  $null=Request POST "/api/postagens/$postid/comentarios" 201 @{texto='Comentario de ensaio'}
  $messageUpload=Request POST '/api/arquivos' 201 $null self @{contexto='mensagem';alvoId="$cid";arquivo=Get-Item $ImagemPng}
  $msg=Request POST "/api/comunidades/$cid/mensagens" 201 @{mensagem='Chat com imagem';imagem=$messageUpload.dados.imagem}
  $null=Request POST "/api/comunidades/$cid/mensagens" 201 @{mensagem='Resposta local';idRespostaA=$msg.dados.id}
  $null=Request POST "/api/comunidades/$cid/mensagens" 404 @{mensagem='Resposta alheia';idRespostaA=1}
  $null=Request POST "/api/comunidades/$cid/postagens" 404 @{imagem=$postUpload.dados.imagem}
  $script:actor='visitante'
  $null=Request GET $postUpload.dados.imagem 401
  $null=Request GET "/api/postagens/$postid" 401
  $script:actor='S1'
  $null=Request GET $postUpload.dados.imagem 403

  $script:case='V28'
  $script:actor='O'
  $eventUpload=Request POST '/api/arquivos' 201 $null self @{contexto='evento';alvoId="$eid";arquivo=Get-Item $ImagemPng}
  $script:actor='visitante'
  $null=Request GET $eventUpload.dados.imagem 200
  $script:actor='O2'
  $null=Request POST '/api/arquivos' 403 $null self @{contexto='evento';alvoId="$eid";arquivo=Get-Item $ImagemPng}
  $script:actor='O'
  $null=Request POST '/api/arquivos' 403 $null self @{contexto='evento';alvoId='4';arquivo=Get-Item $ImagemPng}

  $script:case='V17-V20'
  $script:actor='P'
  $ad=Request POST '/api/revendas' 201 @{ingressoIds=@($iid);precoUnitario=28}
  $aid=$ad.dados[0].id
  $null=Request POST '/api/revendas' 409 @{ingressoIds=@($iid);precoUnitario=29}
  $null=Request POST "/api/revendas/$aid/compra" 403 @{}
  $null=Request GET "/api/comunidades/$cid/postagens" 200
  $null=Request POST "/api/revendas/$aid/cancelamento" 200 @{}
  $newad=Request POST '/api/revendas' 201 @{ingressoIds=@($iid);precoUnitario=29}
  $aid2=$newad.dados[0].id
  Check ($aid2 -ne $aid) 'Reanúncio conserva linha cancelada'
  $null=Request GET '/api/me/revendas' 200
  $script:actor='O';$null=Request GET '/api/revendas' 403
  $script:actor='S1';$null=Request GET '/api/revendas' 403
  $script:actor='C'
  $null=Request GET '/api/revendas' 200
  $resale=Request POST "/api/revendas/$aid2/compra" 200 @{}
  Check ($resale.dados.ingressoId -eq $iid -and $resale.dados.total -eq 29) 'Mesmo ingresso transferido, sem nova emissão'
  $null=Request GET "/api/me/ingressos/$iid" 200
  $null=Request GET "/api/comunidades/$cid/postagens" 403
  $null=Request POST "/api/comunidades/$cid/participacao" 200 @{}
  $null=Request GET "/api/postagens/$postid" 200
  $script:actor='P'
  $null=Request GET "/api/me/ingressos/$iid" 404
  $null=Request GET "/api/comunidades/$cid/postagens" 403
  $null=Request GET $postUpload.dados.imagem 403
  $stillHistory=Request GET '/api/me/compras' 200
  Check ((ConvertTo-Json $historyAfter -Depth 20 -Compress) -eq (ConvertTo-Json $stillHistory -Depth 20 -Compress)) 'Compra original preservada após transferência'

  $script:case='V31-V34'
  $pt=Request POST '/api/chamados' 201 @{titulo='Ensaio P 08';descricao='Atendimento ficticio'}
  $ptid=$pt.dados.id
  $null=Request POST "/api/me/chamados/$ptid/resolucao" 409 @{}
  $script:actor='O'
  $ot=Request POST '/api/chamados' 201 @{titulo='Ensaio O 08';descricao='Atendimento organizador'}
  $otid=$ot.dados.id
  $null=Request GET "/api/me/chamados/$ptid" 404
  $script:actor='S1'
  $null=Request PATCH "/api/suporte/chamados/$ptid/associacao" 200 @{}
  $null=Request POST "/api/suporte/chamados/$ptid/mensagens" 200 @{mensagem='Orientacao suporte Um'}
  $script:actor='S2'
  $null=Request POST "/api/suporte/chamados/$ptid/mensagens" 200 @{mensagem='Orientacao suporte Dois'}
  $null=Request POST "/api/suporte/chamados/$ptid/encerramento" 409 @{}
  $detail=Request GET "/api/suporte/chamados/$ptid" 200
  Check (@($detail.dados.mensagens | Where-Object autorTipo -eq 'suporte' | Select-Object -ExpandProperty autorId -Unique).Count -eq 2) 'Histórico conserva autores reais de dois suportes'
  $script:actor='P'
  $null=Request POST "/api/me/chamados/$ptid/mensagens" 200 @{mensagem='Retorno do participante'}
  $null=Request POST "/api/me/chamados/$ptid/resolucao" 409 @{}
  $script:actor='S2'
  $null=Request POST "/api/suporte/chamados/$ptid/mensagens" 200 @{mensagem='Resposta final'}
  $script:actor='P'
  $resolved=Request POST "/api/me/chamados/$ptid/resolucao" 200 @{}
  Check ($resolved.dados.status -eq 'fechado') 'Resolução fecha aguardando_usuario'
  $null=Request POST "/api/me/chamados/$ptid/mensagens" 409 @{mensagem='Indevido'}
  $script:actor='S1'
  $null=Request POST "/api/suporte/chamados/$ptid/mensagens" 409 @{mensagem='Indevido'}
  $null=Request POST "/api/suporte/chamados/$otid/encerramento" 200 @{}
  $null=Request POST "/api/suporte/chamados/$otid/encerramento" 409 @{}
  $null=Request PATCH "/api/suporte/chamados/$otid/associacao" 200 @{}

  $script:case='V37-V39'
  $script:actor='P';$pp=Request GET '/api/participante/painel' 200
  $script:actor='C';$cp=Request GET '/api/participante/painel' 200
  Check ($pp.dados.indicadores.ingressos -eq 0 -and $pp.dados.indicadores.comprasConfirmadas -eq 1) 'Vendedor sem ingresso conserva compra histórica'
  Check ($cp.dados.indicadores.ingressos -ge 1) 'Comprador recebe ingresso no painel'
  $script:actor='O';$op=Request GET '/api/organizador/painel' 200
  Check ($op.dados.indicadores.eventos -eq 1 -and $op.dados.indicadores.valorHistoricoCompras -eq 25.50) 'Organizador recebe somente seu evento/compra direta'
  $script:actor='O2';$null=Request GET '/api/organizador/painel' 200
  $script:actor='S1';$null=Request GET '/api/suporte/painel' 200
  $script:actor='S2';$null=Request GET '/api/suporte/painel' 200
  $script:actor='P'
  $null=Request GET '/api/organizador/painel' 403
  $ignoredId=Request GET '/api/participante/painel?id=1' 200
  Check ((ConvertTo-Json $ignoredId.dados.indicadores -Compress) -eq (ConvertTo-Json $pp.dados.indicadores -Compress)) 'ID extra não troca escopo do painel'
  $null=Request POST '/api/chamados' 403 @{titulo='Indevido';descricao='Sem origem'} ''
  $null=Request POST '/api/chamados' 403 @{titulo='Indevido';descricao='Origem alheia'} 'http://exemplo.invalid'
  $null=Request POST '/api/chamados' 400 @{titulo='Indevido';descricao='ID forjado';participanteId=1}
  $null=Request GET '/api/recurso-inexistente' 404
  $script:case='V06-logout'
  $revokedSession08=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
  foreach ($cookie08 in $sessions['P'].Cookies.GetCookies([uri]$BaseUrl)) {
    $revokedSession08.Cookies.Add([uri]$BaseUrl,[Net.Cookie]::new($cookie08.Name,$cookie08.Value,$cookie08.Path))
  }
  $null=Request POST '/api/auth/logout' 204 @{}
  $null=Request GET '/api/auth/me' 401
  $sessions['temporario']=$revokedSession08;$script:actor='temporario'
  $null=Request GET '/api/auth/me' 401
  $null=Login P participantes $pEmail

  @{sha=$Sha;fim=[DateTimeOffset]::Now.ToString('o');checks=$script:checks;resultado='APROVADO';participanteEmail=$pEmail;organizadorEmail=$oEmail;participanteId=$pIdentity.id;organizadorId=$oIdentity.id;eventoId=$eid;comunidadeId=$cid;ingressoId=$iid;anuncioCanceladoId=$aid;anuncioVendidoId=$aid2;postagemId=$postid;ticketParticipanteId=$ptid;ticketOrganizadorId=$otid;imagemEvento=$eventUpload.dados.imagem;imagemSocial=$postUpload.dados.imagem} |
    ConvertTo-Json -Depth 10 | Set-Content "$OutputDir/resumo.json" -Encoding utf8
  "APROVADO: $script:checks assertivas; evidências em $OutputDir"
} catch {
  @{sha=$Sha;fim=[DateTimeOffset]::Now.ToString('o');checks=$script:checks;resultado='REPROVADO';caso=$script:case;falha=$_.Exception.Message} |
    ConvertTo-Json | Set-Content "$OutputDir/falha.json" -Encoding utf8
  throw
}
