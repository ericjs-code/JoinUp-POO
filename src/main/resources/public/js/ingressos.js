'use strict';
const $ = id => document.getElementById(id);
const aba = new URLSearchParams(location.search).get('aba') || 'ingressos';
let pagina = 1, anuncioCompra = null;
const moeda = n => Number(n).toLocaleString('pt-BR', {style:'currency',currency:'BRL'});
const data = d => d ? new Date(d).toLocaleString('pt-BR') : 'Não registrado';
function el(tag, texto, classe) { const n=document.createElement(tag); if(texto!==undefined) n.textContent=texto; if(classe) n.className=classe; return n; }
function aviso(texto, erro=false) { $('mensagem').textContent=texto; $('mensagem').className=erro?'erro':'sucesso'; }
async function api(path,method='GET',body) {
  const r=await fetch(path,{method,credentials:'same-origin',headers:body===undefined?{}:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)});
  const j=await r.json(); if(!r.ok) { const e=new Error(j.erro?.mensagem || 'Não foi possível concluir.'); e.status=r.status; throw e; } return j;
}
async function acao(botao,executar) {
  botao.disabled=true;
  try { await executar(); }
  catch(e) { aviso(e.message,true); if(e.status===401 || e.status===403) { $('area').hidden=true; $('acesso').hidden=false; } }
  finally { botao.disabled=false; }
}
function vazio(container, texto) { if(!container.children.length) container.append(el('p',texto,'vazio')); }
async function tickets() {
  const j=await api('/api/me/ingressos'); $('tickets').replaceChildren(); let grupo='',elegiveis=0;
  for(const t of j.dados) {
    const chave=`${t.eventoId}:${t.tipoIngressoId}:${t.status}`;
    if(chave!==grupo) { $('tickets').append(el('h3',`${t.nomeEvento} · ${t.nomeTipo} · ${t.status}`)); grupo=chave; }
    const row=el('article',undefined,'ticket'), conteudo=el('div',undefined,'conteudo');
    const elegivel=t.status==='ativo' && t.organizadorAtivo && new Date(t.dataEvento)>new Date() && !t.anuncioDisponivelId;
    if(elegivel) { const label=el('label'),input=document.createElement('input'); input.type='checkbox'; input.value=t.id; input.name='ingresso'; input.setAttribute('aria-label',`Selecionar ingresso ${t.id} para revenda`); label.append(input); row.append(label); elegiveis++; }
    conteudo.append(el('h3',`Ingresso #${t.id}`),el('p',`${data(t.dataEvento)} · ${t.localEvento}`),el('p',`Estado: ${t.status}${t.anuncioDisponivelId?' · Em revenda #'+t.anuncioDisponivelId:''}`));
    conteudo.append(el('p',`Na emissão: ${t.nomeEventoNaCompra || 'Sem snapshot'} · ${data(t.dataEventoNaCompra)} · ${t.localEventoNaCompra || 'Não registrado'}`));
    if(!elegivel && !t.anuncioDisponivelId) conteudo.append(el('p',t.status!=='ativo'?'Este status não permite revenda.':!t.organizadorAtivo?'Revenda bloqueada enquanto o organizador estiver inativo.':'O evento já começou; revenda bloqueada.'));
    if(t.anuncioDisponivelId) { const a=el('a','Gerenciar anúncio →'); a.href='/ingressos.html?aba=anuncios'; conteudo.append(a); }
    row.append(conteudo); $('tickets').append(row);
  }
  $('publicar').hidden=!elegiveis; vazio($('tickets'),'Você ainda não possui ingressos. Explore eventos para encontrar seu próximo encontro.'); atualizarSelecao();
}
function atualizarSelecao() { const n=$('tickets').querySelectorAll('input:checked').length; $('selecionados').textContent=`${n} ingresso(s) selecionado(s). O preço informado será aplicado a cada ingresso.`; $('anunciar').disabled=!n; }
$('tickets').addEventListener('change',atualizarSelecao);
$('form-anuncio').addEventListener('submit',ev=>{ ev.preventDefault(); acao($('anunciar'),async()=>{
  const ids=[...$('tickets').querySelectorAll('input:checked')].map(n=>Number(n.value));
  const j=await api('/api/revendas','POST',{ingressoIds:ids,precoUnitario:Number($('preco').value)}); await tickets(); aviso(`${j.dados.length} anúncio(s) publicado(s). Você permanece titular até a venda.`);
}); });
async function historico() {
  const j=await api('/api/me/compras'); $('compras').replaceChildren(); $('recebidas').replaceChildren();
  for(const c of j.dados) {
    const row=el('article',undefined,'compra'); row.append(el('h3',`Compra #${c.compraId} · ${c.nomeEventoNaCompra || 'Evento #'+c.eventoId}`),el('p',`${c.status} · ${moeda(c.total)} · ${data(c.dataCompra)}`),el('p',`Evento na compra: ${data(c.dataEventoNaCompra)} · ${c.localEventoNaCompra || 'Não registrado'}`));
    for(const i of c.itens) row.append(el('p',`${i.quantidade} × categoria #${i.tipoIngressoId} · ${moeda(i.valorUnitario)} por unidade · Subtotal ${moeda(i.subtotal)}`)); $('compras').append(row);
  }
  for(const c of j.revendasRecebidas) $('recebidas').append(el('p',`Anúncio #${c.anuncioId} · Ingresso #${c.ingressoId} · ${moeda(c.total)} · ${data(c.dataVenda)} · Pagamento simulado`,'compra'));
  vazio($('compras'),'Nenhuma compra direta registrada.'); vazio($('recebidas'),'Nenhuma compra por revenda registrada.');
}
function card(a,meus) {
  const row=el('article',undefined,'card'), c=el('div',undefined,'conteudo');
  c.append(el('span',a.status,'badge'),el('h3',a.nomeEvento),el('p',`${a.nomeTipo} · Ingresso #${a.ingressoId}`),el('p',`${data(a.dataEvento)} · ${a.localEvento}`),el('h3',moeda(a.preco)),el('p',`Anúncio #${a.id} · ${data(a.dataCriacao)}`));
  if(a.dataVenda) c.append(el('p',`Vendido em ${data(a.dataVenda)}`)); if(a.dataCancelamento) c.append(el('p',`Cancelado em ${data(a.dataCancelamento)}`));
  if(meus && a.status==='disponivel') { const b=el('button','Cancelar anúncio','secundario'); b.type='button'; b.addEventListener('click',()=>acao(b,async()=>{await api(`/api/revendas/${a.id}/cancelamento`,'POST',{});await meusAnuncios();aviso('Anúncio cancelado. Seu ingresso permanece com você.');})); c.append(b); }
  else if(!meus) {
    const b=el('button','Comprar por revenda'); b.type='button'; b.disabled=!a.compravel;
    b.addEventListener('click',()=>{ anuncioCompra=a; $('resumo-compra').textContent=`${a.nomeEvento} · ${a.nomeTipo} · Ingresso #${a.ingressoId} · Total ${moeda(a.preco)}. A titularidade será transferida para você.`; $('confirmacao').showModal(); }); c.append(b);
  }
  if(a.status==='disponivel' && !a.compravel) c.append(el('p',a.proprio?'Este anúncio é seu.':!a.organizadorAtivo?'Temporariamente indisponível: organizador inativo.':'Compra indisponível; o ingresso ou evento não atende às condições.','bloqueio'));
  row.append(c); return row;
}
async function listarRevendas() {
  const q=new URLSearchParams({pagina,tamanho:6,eventoId:$('evento-filtro').value}),j=await api('/api/revendas?'+q);
  $('revendas').replaceChildren(...j.dados.map(a=>card(a,false))); vazio($('revendas'),'Nenhum anúncio disponível neste filtro.'); $('revendas-status').textContent=`${j.total} anúncio(s) · Anúncios bloqueados permanecem no histórico.`;
  $('pagina').textContent=`Página ${pagina}`; $('anterior').disabled=pagina<=1; $('proxima').disabled=pagina*6>=j.total;
}
async function meusAnuncios() { const j=await api('/api/me/revendas'); $('anuncios').replaceChildren(...j.dados.map(a=>card(a,true))); vazio($('anuncios'),'Você ainda não publicou anúncios.'); }
$('confirmar').addEventListener('click',()=>acao($('confirmar'),async()=>{
  const a=anuncioCompra;
  try { await api(`/api/revendas/${a.id}/compra`,'POST',{}); }
  catch(e) { $('confirmacao').close(); try { await listarRevendas(); } catch {} throw e; }
  $('confirmacao').close(); await listarRevendas(); aviso(`Ingresso #${a.ingressoId} adquirido. Consulte Meus ingressos. Pagamento simulado confirmado.`);
}));
$('filtro').addEventListener('submit',ev=>{ev.preventDefault();pagina=1;acao(ev.submitter,listarRevendas);});
for(const [id,delta] of [['anterior',-1],['proxima',1]]) $(id).addEventListener('click',()=>acao($(id),async()=>{pagina+=delta;await listarRevendas();}));
(async()=>{
  try {
    const u=(await api('/api/auth/me')).dados; if(u.perfil!=='PARTICIPANTE') throw new Error('Esta área exige uma conta de participante.');
    $('area').hidden=false; $('identidade').textContent=`Conectado como ${u.nome}`;
    const atual=['ingressos','compras','revendas','anuncios'].includes(aba)?aba:'ingressos'; $('painel-'+atual).hidden=false;
    const links=$('area').querySelectorAll('.abas a'); links[['ingressos','compras','revendas','anuncios'].indexOf(atual)].setAttribute('aria-current','page');
    if(atual==='ingressos') await tickets(); else if(atual==='compras') await historico(); else if(atual==='anuncios') await meusAnuncios();
    else { let p=1; do { const j=await api('/api/eventos?tamanho=100&pagina='+p); for(const e of j.dados) $('evento-filtro').append(new Option(e.nome,e.id)); if(p*100>=j.total) break; p++; } while(true); await listarRevendas(); }
  } catch(e) { $('area').hidden=true; $('acesso').hidden=false; aviso(e.message,true); }
})();
