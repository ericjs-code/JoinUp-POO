'use strict';
const $ = id => document.getElementById(id);
const qs = new URLSearchParams(location.search);
const meus = qs.get('meus') === '1';
let pagina = 1, total = 0, evento = null, usuario = null, generos = [], artistas = [];
const moeda = n => Number(n).toLocaleString('pt-BR', {style:'currency',currency:'BRL'});
const horario = d => new Date(d).toLocaleString('pt-BR');
function aviso(texto, erro = false) { $('mensagem').textContent = texto; $('mensagem').className = erro ? 'erro' : 'sucesso'; }
async function api(path, method = 'GET', body) {
  const r = await fetch(path, {method, credentials:'same-origin', headers:body === undefined ? {} : {'Content-Type':'application/json'}, body:body === undefined ? undefined : JSON.stringify(body)});
  const j = await r.json(); if (!r.ok) throw new Error(j.erro?.mensagem || 'Não foi possível concluir a operação.'); return j;
}
function el(tag, texto, classe) { const n = document.createElement(tag); if (texto !== undefined) n.textContent = texto; if (classe) n.className = classe; return n; }
function link(texto, href) { const n = el('a', texto); n.href = href; return n; }
function imagem(e) { const n = document.createElement('img'); n.src = e.imagem === '/assets/evento-placeholder.svg' ? e.imagem : '/assets/evento-placeholder.svg'; n.alt = 'Arte padrão JoinUp'; return n; }
function preencherSelect(select, itens, vazio) { select.replaceChildren(); if(vazio) select.append(new Option(vazio,'')); for(const i of itens) select.append(new Option(i.nome,i.id)); }
async function listar() {
  const f = new URLSearchParams(new FormData($('filtros'))); f.set('pagina',pagina); f.set('tamanho',6);
  const j = await api((meus ? '/api/organizador/eventos?' : '/api/eventos?') + f); total=j.total;
  $('cards').replaceChildren();
  for(const e of j.dados) {
    const card=el('article',undefined,'card'), conteudo=el('div',undefined,'conteudo'); card.append(imagem(e));
    conteudo.append(el('span',e.genero.nome,'badge'),el('h3',e.nome),el('p',horario(e.data)),el('p',`${e.localizacao} · ${e.cidade}/${e.estado}`));
    const precos=e.tiposIngresso.filter(t => t.quantidade>0).map(t => Number(t.preco));
    conteudo.append(el('p',precos.length ? `A partir de ${moeda(Math.min(...precos))}` : 'Sem estoque disponível'));
    conteudo.append(link('Ver detalhes →',`/eventos.html?id=${e.id}`));
    if(meus) conteudo.append(link(e.editavel?'Editar e gerenciar':'Consultar gestão bloqueada',`/eventos.html?editar=${e.id}`));
    card.append(conteudo); $('cards').append(card);
  }
  $('lista-status').textContent=total ? `${total} evento(s) encontrado(s)` : 'Nenhum evento encontrado para estes filtros.';
  $('pagina').textContent=`Página ${pagina}`; $('anterior').disabled=pagina===1; $('proxima').disabled=pagina*6>=total;
}
async function detalhe(id) {
  const e=(await api('/api/eventos/'+id)).dados; $('explorar').hidden=true; $('detalhe').hidden=false;
  const c=$('detalhe-conteudo'); c.replaceChildren(imagem(e),el('h2',e.nome),el('p',e.descricao),el('p',`${horario(e.data)} · ${e.localizacao} · ${e.cidade}/${e.estado}`),el('p',`Gênero: ${e.genero.nome}`),el('h3','Lineup'),el('p',e.artistas.map(a=>a.nome).join(' · ')),el('h3','Categorias de ingresso'));
  for(const t of e.tiposIngresso) c.append(el('p',`${t.nome} · ${moeda(t.preco)} · ${t.quantidade} disponível(is)`));
  if(e.comunidade) c.append(el('p',`Comunidade: ${e.comunidade.nome} (referência #${e.comunidade.id})`));
  c.append(el('p','A consulta é pública. Compra e conteúdo da comunidade estarão disponíveis em próximas etapas.','muted'));
}
function linhaTipo(t = {}) {
  const row=el('div',undefined,'tipo');
  for(const [nome,titulo,type,max] of [['nome','Nome da categoria','text','45'],['preco','Preço (R$)','number',null],['quantidade','Quantidade disponível','number',null]]) {
    const label=el('label',titulo), input=document.createElement('input'); input.name=nome; input.type=type; input.required=true;
    if(max) input.maxLength=Number(max); if(type==='number') { input.min='0'; input.step=nome==='preco'?'0.01':'1'; input.max=nome==='preco'?'999999.99':'2147483647'; }
    input.value=t[nome] ?? ''; label.append(input); row.append(label);
  }
  return row;
}
function tipoDados(row) { return {nome:row.querySelector('[name=nome]').value.trim(),preco:Number(row.querySelector('[name=preco]').value),quantidade:Number(row.querySelector('[name=quantidade]').value)}; }
function addTipo() { $('categorias-novas').append(linhaTipo()); }
function localInput(iso) { const d=new Date(iso); return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,19); }
async function editor(id) {
  if(!usuario || usuario.perfil!=='ORGANIZADOR') throw new Error('Entre com uma conta de organizador ativa para administrar eventos.');
  if(id) {
    // A lista autorizada confirma titularidade para a UI; mutações revalidam no servidor.
    const f = new URLSearchParams({tamanho:'100',pagina:'1'}); let encontrado=false;
    do { const j=await api('/api/organizador/eventos?'+f); encontrado=j.dados.some(e=>String(e.id)===id); if(encontrado || Number(f.get('pagina'))*100>=j.total) break; f.set('pagina',Number(f.get('pagina'))+1); } while(true);
    if(!encontrado) throw new Error('Este evento não pertence ao organizador autenticado.');
    evento=(await api('/api/eventos/'+id)).dados;
  }
  $('explorar').hidden=true; $('editor').hidden=false;
  const form=$('form-evento'); preencherSelect(form.elements.generoId,generos,'Selecione o gênero');
  preencherSelect(form.elements.estado,'AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO'.split(' ').map(uf=>({id:uf,nome:uf})),'Selecione a UF');
  for(const a of artistas) { const label=el('label',a.nome), input=document.createElement('input'); input.type='checkbox'; input.value=a.id; input.checked=evento?.artistas.some(x=>x.id===a.id)??false; label.prepend(input); $('artistas').append(label); }
  if(evento) {
    $('titulo-editor').textContent='Editar evento'; $('salvar-evento').textContent='Salvar informações e lineup'; $('tipos-criacao').hidden=true;
    for(const nome of ['nome','descricao','localizacao','cidade','estado','cep','imagem']) form.elements[nome].value=evento[nome]??'';
    form.elements.data.value=localInput(evento.data); form.elements.generoId.value=evento.genero.id;
    $('campos-evento').disabled=!evento.editavel;
    if(!evento.editavel) $('limite-editor').textContent='Este evento já começou. Informações, lineup e categorias estão bloqueados para edição.';
    renderCategorias();
  } else addTipo();
}
function renderCategorias() {
  $('gestao-categorias').hidden=false; $('categorias-existentes').replaceChildren();
  for(const t of evento.tiposIngresso) {
    const form=document.createElement('form'), fields=document.createElement('fieldset'), row=linhaTipo(t);
    fields.disabled=!evento.editavel || t.temVendas; row.append(el('p',t.temVendas?'Bloqueada: esta categoria já registrou venda.':evento.editavel?'Sem vendas: edição permitida.':'Bloqueada: o evento já começou.','aviso'),el('button','Salvar categoria')); fields.append(row); form.append(fields);
    form.addEventListener('submit',async ev => { ev.preventDefault(); fields.disabled=true; try { await api(`/api/organizador/eventos/${evento.id}/tipos-ingresso/${t.id}`,'PATCH',tipoDados(row)); await recarregarCategorias(); aviso('Categoria atualizada.'); } catch(e) { aviso(e.message,true); fields.disabled=!evento.editavel || t.temVendas; } });
    $('categorias-existentes').append(form);
  }
  $('linha-nova-categoria').replaceChildren(linhaTipo()); $('campos-categoria').disabled=!evento.editavel;
}
async function recarregarCategorias() { evento=(await api('/api/eventos/'+evento.id)).dados; renderCategorias(); $('campos-evento').disabled=!evento.editavel; }
$('filtros').addEventListener('submit',async e=>{e.preventDefault();pagina=1;try { await listar(); } catch(err) { aviso(err.message,true); }});
for(const [id,delta] of [['anterior',-1],['proxima',1]]) $(id).addEventListener('click',async()=>{pagina+=delta;try { await listar(); } catch(e) { pagina-=delta; aviso(e.message,true); }});
$('mais-tipo').addEventListener('click',addTipo);
$('form-evento').addEventListener('submit',async ev=>{
  ev.preventDefault(); const f=ev.currentTarget, d={};
  for(const n of ['nome','descricao','localizacao','cidade','estado','cep','imagem']) d[n]=f.querySelector(`[name="${n}"]`).value;
  d.data=new Date(f.elements.data.value).toISOString(); d.generoId=Number(f.elements.generoId.value); d.artistaIds=[...$('artistas').querySelectorAll('input:checked')].map(i=>Number(i.value));
  if(!d.artistaIds.length) return aviso('Selecione ao menos um artista.',true);
  if(!evento) d.tiposIngresso=[...$('categorias-novas').children].map(tipoDados);
  $('salvar-evento').disabled=true;
  try {
    const j=await api('/api/organizador/eventos'+(evento?'/'+evento.id:''),evento?'PATCH':'POST',d);
    if(!evento) { location.href='/eventos.html?editar='+j.dados.id+'&criado=1'; return; }
    evento=j.dados; renderCategorias(); aviso('Informações e lineup salvos. Histórico e comunidade preservados.');
  } catch(e) { aviso(e.message,true); } finally { $('salvar-evento').disabled=false; }
});
$('nova-categoria').addEventListener('submit',async ev=>{ev.preventDefault();$('campos-categoria').disabled=true;try { await api(`/api/organizador/eventos/${evento.id}/tipos-ingresso`,'POST',tipoDados($('linha-nova-categoria'))); await recarregarCategorias(); aviso('Nova categoria adicionada.'); } catch(e) { aviso(e.message,true); $('campos-categoria').disabled=!evento.editavel; }});
(async()=>{
  try {
    [generos,artistas]=await Promise.all([api('/api/generos-musicais').then(j=>j.dados),api('/api/artistas').then(j=>j.dados)]);
    const r=await fetch('/api/auth/me'); if(r.ok) usuario=(await r.json()).dados;
    $('criar').hidden=usuario?.perfil!=='ORGANIZADOR'; preencherSelect($('filtros').elements.generoId,generos,'Todos os gêneros');
    if(qs.has('novo') || qs.has('editar')) await editor(qs.get('editar'));
    else if(qs.has('id')) await detalhe(qs.get('id'));
    else { if(meus) $('titulo-lista').textContent='Meus eventos'; await listar(); }
    if(qs.has('criado')) aviso(`Evento #${evento.id} publicado com categorias, lineup e comunidade #${evento.comunidade.id}.`);
  } catch(e) { aviso(e.message,true); }
})();
