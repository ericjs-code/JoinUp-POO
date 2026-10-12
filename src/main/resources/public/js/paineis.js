'use strict';
const el = id => document.getElementById(id);
const estados = {aberto:'Aberto', em_atendimento:'Em atendimento', aguardando_usuario:'Aguardando usuário', fechado:'Fechado'};
const nomes = {ingressos:'Ingressos atuais', proximosIngressos:'Ingressos para próximos eventos', proximosEventos:'Próximos eventos', comprasConfirmadas:'Compras confirmadas', valorHistoricoCompras:'Valor histórico de compras', anunciosDisponiveis:'Anúncios disponíveis (status)', anunciosVendidos:'Anúncios vendidos', anunciosCancelados:'Anúncios cancelados', comunidadesAcessiveis:'Comunidades acessíveis', eventos:'Eventos próprios', eventosFuturos:'Eventos futuros', eventosIniciados:'Eventos iniciados', categorias:'Categorias de ingresso', estoqueDisponivel:'Estoque disponível', ingressosVendidos:'Ingressos vendidos (histórico)', comunidades:'Comunidades próprias', chamados:'Total de chamados', associadosAMim:'Associados a mim', semAssociacao:'Sem associação', respostasSuporte:'Respostas de suporte', minhasRespostas:'Minhas respostas', chamadosRespondidosPorMim:'Chamados respondidos por mim'};
let usuario, suporte = false, pagina = 1, mensagemPagina = 1, selecionado = null;
const data = s => s ? new Date(s).toLocaleString('pt-BR') : '';
const moeda = n => new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(n);
const raiz = () => suporte ? '/api/suporte/chamados' : '/api/me/chamados';
function aviso(texto, erro = false) { el('mensagem').textContent = texto; el('mensagem').className = erro ? 'erro' : 'sucesso'; }
async function api(path, method = 'GET', dados) {
  const options = {method, credentials:'same-origin', headers:{}};
  if (dados !== undefined) { options.headers['Content-Type'] = 'application/json'; options.body = JSON.stringify(dados); }
  const r = await fetch(path, options), b = r.status === 204 ? {} : await r.json();
  if (!r.ok) { const e = new Error(b.erro?.mensagem || 'Não foi possível concluir a operação.'); e.status = r.status; throw e; }
  return b;
}
let ocupado = false;
async function executar(acao) {
  if (ocupado) return;
  ocupado = true; el('conteudo').setAttribute('aria-busy','true');
  const botoes = [...document.querySelectorAll('button')];
  const anteriores = botoes.map(b => b.disabled); botoes.forEach(b => b.disabled = true);
  try { await acao(); }
  catch (e) {
    if (e.status === 401 || e.status === 403) { el('conteudo').hidden = true; el('atualizar').hidden = true; el('acesso').hidden = false; }
    aviso(e.message,true);
    // Conflito: buscar estado atual e retirar ações que já não são válidas. Sem retry da mutação.
    if (e.status === 409 && selecionado) { try { await detalhe(selecionado); await listar(); await painel(); } catch { el('detalhe').hidden = true; } }
  } finally {
    ocupado = false; el('conteudo').removeAttribute('aria-busy');
    botoes.forEach((b,i) => b.disabled = anteriores[i]);
    limites(); regras();
  }
}
let total = 0, totalMensagens = 0, ticket = null;
function limites() {
  el('anterior').disabled = pagina <= 1; el('proxima').disabled = pagina * 20 >= total;
  el('mensagens-anterior').disabled = mensagemPagina <= 1; el('mensagens-proxima').disabled = mensagemPagina * 50 >= totalMensagens;
}
function aba(nome) {
  el('resumo').hidden = nome !== 'painel'; el('chamados').hidden = nome !== 'chamados';
  for (const n of ['painel','chamados']) { if (n === nome) el('aba-'+n).setAttribute('aria-current','page'); else el('aba-'+n).removeAttribute('aria-current'); }
}
function card(label, valor, parent, acao) {
  const box = document.createElement(acao ? 'button' : 'div'); box.className = 'indicador';
  if (acao) { box.type = 'button'; box.addEventListener('click',()=>executar(acao)); }
  const strong = document.createElement('strong'), span = document.createElement('span'); strong.textContent = valor; span.textContent = label;
  box.append(strong,span); parent.append(box);
}
async function painel() {
  const p = (await api('/api/'+usuario.perfil.toLowerCase()+'/painel')).dados;
  el('consulta').textContent = 'Consultado em '+data(p.consultadoEm); el('indicadores').replaceChildren(); el('estados').replaceChildren();
  const nomesSuporte = {chamadosParticipantes:'Chamados de participantes', chamadosOrganizadores:'Chamados de organizadores', criadosHojeUtc:'Criados hoje (dia UTC)', mediaResolucaoSegundos:'Tempo médio até fechamento', percentualFechados:'Chamados fechados (%)'};
  for (const [k,v] of Object.entries(p.indicadores)) {
    const valor = v === null ? 'Sem dados' : k === 'valorHistoricoCompras' ? moeda(v) : k === 'mediaResolucaoSegundos' ? (Number(v)/3600).toFixed(1)+' h' : k === 'percentualFechados' ? Number(v).toLocaleString('pt-BR')+'%' : v;
    card(nomes[k] || nomesSuporte[k] || k, valor, el('indicadores'));
  }
  for (const [k,v] of Object.entries(p.chamadosPorStatus)) card(estados[k],v,el('estados'), async()=>{ el('filtro-status').value=k; pagina=1; await listar(); aba('chamados'); });
  el('eventos-painel').hidden = suporte; el('titulo-eventos').textContent = suporte ? '' : usuario.perfil === 'PARTICIPANTE' ? 'Próximos encontros' : 'Eventos e vendas';
  el('limite-eventos').textContent = usuario.perfil === 'PARTICIPANTE' ? 'Até 20 próximos eventos com ingressos ativos de sua titularidade.' : 'Até 20 eventos mais recentes. Os totais acima consideram todos os seus eventos.';
  el('eventos').replaceChildren();
  for (const e of p.eventos) {
    const li = document.createElement('li'), a = document.createElement('a'); a.textContent = e.nome; a.href = '/eventos.html?id='+e.id;
    const texto = document.createElement('p'); texto.textContent = data(e.data)+' · '+(e.situacao ? (e.situacao === 'futuro' ? 'Futuro' : 'Iniciado')+' · '+e.ingressosVendidos+' ingresso(s) vendido(s) · '+moeda(e.valorHistoricoCompras) : e.ingressos+' ingresso(s)');
    li.append(a,texto); el('eventos').append(li);
  }
  if (!p.eventos.length && !suporte) { const li=document.createElement('li'); li.textContent='Nenhum evento para exibir.'; el('eventos').append(li); }
}
async function listar() {
  const query = new URLSearchParams({status:el('filtro-status').value,pagina,tamanho:20});
  const p = await api(raiz()+'?'+query); total = p.total; el('lista').replaceChildren();
  el('lista-status').textContent = total ? total+' chamado(s) · página '+pagina : 'Nenhum chamado neste filtro.';
  for (const t of p.dados) {
    const box=document.createElement('article'); box.className='ticket'; const info=document.createElement('div'), h=document.createElement('h3'), p=document.createElement('p'), b=document.createElement('button');
    h.textContent='#'+t.id+' · '+t.titulo; p.textContent=estados[t.status]+' · '+data(t.criadoEm); b.type='button'; b.textContent='Ver conversa'; b.className='secundario';
    b.addEventListener('click',()=>executar(async()=>{ mensagemPagina=1; await detalhe(t.id,true); })); info.append(h,p); box.append(info,b); el('lista').append(box);
  }
  limites();
}
function regras() {
  if (!ticket) return;
  const espera=ticket.status === 'aguardando_usuario', fechado=ticket.status === 'fechado';
  el('form-mensagem').hidden = suporte ? fechado : !espera; el('resolver').hidden = suporte || !espera;
  el('encerrar').hidden = !suporte || !['aberto','em_atendimento'].includes(ticket.status); el('associar').hidden = !suporte;
  el('associar').disabled = suporte && ticket.suporteId === usuario?.id;
  el('enviar-mensagem').textContent = suporte ? 'Responder e aguardar usuário' : 'Retornar ao atendimento';
  el('detalhe-regra').textContent = fechado ? 'Chamado encerrado definitivamente. Abra outro para um novo problema.' : espera ? 'Aguardando o solicitante: Resolver encerra; Retornar exige mensagem e volta ao atendimento.' : suporte ? 'Você pode responder ou encerrar. A associação não restringe o atendimento.' : 'Aguarde uma resposta do suporte para Resolver ou Retornar.';
}
async function detalhe(id, foco = false) {
  const d = (await api(raiz()+'/'+id+'?pagina='+mensagemPagina+'&tamanho=50')).dados;
  selecionado=id; ticket=d.ticket; totalMensagens=d.totalMensagens; el('detalhe').hidden=false;
  el('detalhe-id').textContent='Chamado #'+id; el('detalhe-titulo').textContent=ticket.titulo;
  el('detalhe-estado').textContent=estados[ticket.status]+' · atualizado em '+data(ticket.atualizadoEm)+(ticket.fechadoEm ? ' · fechado em '+data(ticket.fechadoEm) : '');
  el('detalhe-associacao').textContent=ticket.suporteId ? 'Responsável pelo acompanhamento: suporte #'+ticket.suporteId : 'Ainda sem suporte associado.';
  el('conversa').replaceChildren();
  for (const m of d.mensagens) {
    const li=document.createElement('li'), autor=document.createElement('strong'), texto=document.createElement('p'), time=document.createElement('time');
    li.dataset.autor=m.autorTipo; autor.textContent=m.autorNome+' · '+m.autorTipo+' #'+m.autorId; texto.textContent=m.mensagem; time.textContent=data(m.enviadoEm); time.dateTime=m.enviadoEm;
    li.append(autor,texto,time); el('conversa').append(li);
  }
  el('mensagens-status').textContent='Página '+mensagemPagina+' · '+totalMensagens+' mensagem(ns)'; regras(); limites();
  if (foco) el('detalhe').focus();
}
async function mutacao(sufixo, metodo = 'POST', body = {}) {
  await api(raiz()+'/'+selecionado+'/'+sufixo,metodo,body); el('form-mensagem').reset();
  aviso('Operação concluída.'); await detalhe(selecionado); await listar(); await painel();
}
el('aba-painel').addEventListener('click',()=>aba('painel')); el('aba-chamados').addEventListener('click',()=>aba('chamados'));
el('filtros').addEventListener('submit',e=>{e.preventDefault(); executar(async()=>{pagina=1; await listar();});});
el('anterior').addEventListener('click',()=>executar(async()=>{pagina--; await listar();})); el('proxima').addEventListener('click',()=>executar(async()=>{pagina++; await listar();}));
el('mensagens-anterior').addEventListener('click',()=>executar(async()=>{mensagemPagina--; await detalhe(selecionado);})); el('mensagens-proxima').addEventListener('click',()=>executar(async()=>{mensagemPagina++; await detalhe(selecionado);}));
el('voltar').addEventListener('click',()=>{el('detalhe').hidden=true; selecionado=null; ticket=null; el('form-mensagem').reset();});
el('form-abertura').addEventListener('submit',e=>{e.preventDefault(); executar(async()=>{
  const t=(await api('/api/chamados','POST',Object.fromEntries(new FormData(e.target)))).dados;
  e.target.reset(); pagina=1; mensagemPagina=1; el('filtro-status').value=''; await listar(); await detalhe(t.id,true); await painel(); aviso('Chamado aberto. Aguarde o atendimento.');
});});
el('form-mensagem').addEventListener('submit',e=>{e.preventDefault(); executar(()=>mutacao('mensagens','POST',Object.fromEntries(new FormData(e.target))));});
el('associar').addEventListener('click',()=>executar(()=>mutacao('associacao','PATCH'))); el('resolver').addEventListener('click',()=>executar(()=>mutacao('resolucao'))); el('encerrar').addEventListener('click',()=>executar(()=>mutacao('encerramento')));
el('atualizar').addEventListener('click',()=>executar(async()=>{await painel(); await listar(); if(selecionado) await detalhe(selecionado); aviso('Dados atualizados.');}));
executar(async()=>{
  usuario=(await api('/api/auth/me')).dados; suporte=usuario.perfil === 'SUPORTE';
  el('acesso').hidden=true; el('conteudo').hidden=false; el('atualizar').hidden=false;
  el('titulo').textContent=suporte ? 'Central de suporte' : 'Meu painel'; el('perfil').textContent=usuario.perfil; el('saudacao').textContent='Olá, '+usuario.nome+'.';
  const links=suporte ? [['Contas e novos suportes','/conta.html']] : usuario.perfil === 'ORGANIZADOR' ? [['Meus eventos','/eventos.html?meus=1'],['Comunidades','/comunidades.html'],['Minha conta','/conta.html']] : [['Explorar eventos','/eventos.html'],['Meus ingressos','/ingressos.html'],['Revendas','/ingressos.html?aba=revendas'],['Comunidades','/comunidades.html'],['Minha conta','/conta.html']];
  el('nav-perfil').replaceChildren(); for(const [label,url] of links){const a=document.createElement('a'); a.textContent=label; a.href=url; el('nav-perfil').append(a);}
  el('abertura').hidden=suporte; el('titulo-chamados').textContent=suporte ? 'Atendimento de chamados' : 'Meus chamados';
  await painel(); await listar(); aba(new URLSearchParams(location.search).get('aba') === 'chamados' ? 'chamados' : 'painel');
});
