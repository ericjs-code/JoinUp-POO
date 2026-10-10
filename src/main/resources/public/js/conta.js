'use strict';
const el = id => document.getElementById(id);
let usuario = null;
let pagina = 1;
const mensagem = (texto, erro = false) => {
  el('mensagem').textContent = texto;
  el('mensagem').className = erro ? 'erro' : 'sucesso';
};
async function api(path, method = 'GET', dados) {
  const options = { method, credentials: 'same-origin', headers: {} };
  if (dados !== undefined) { options.headers['Content-Type'] = 'application/json'; options.body = JSON.stringify(dados); }
  const resposta = await fetch(path, options);
  const corpo = resposta.status === 204 ? {} : await resposta.json();
  if (!resposta.ok) {
    const erro = new Error(corpo.erro?.mensagem || 'Não foi possível concluir a operação.');
    erro.status = resposta.status;
    throw erro;
  }
  return corpo;
}
async function executar(botao, acao) {
  botao.disabled = true;
  try { await acao(); }
  catch (erro) {
    if (erro.status === 401 && usuario) { usuario = null; mostrarSessao(); }
    mensagem(erro.message, true);
  } finally { botao.disabled = false; }
}
function camposAcesso() {
  const tipo = el('tipo').value;
  const suporte = tipo === 'suporte';
  el('modo').querySelector('[value="cadastro"]').disabled = suporte;
  if (suporte) el('modo').value = 'login';
  const cadastro = el('modo').value === 'cadastro';
  el('aviso-suporte').hidden = !suporte;
  const regras = { nome: cadastro, cpf: cadastro && tipo === 'participantes', cnpj: cadastro && tipo === 'organizadores', nascimento: cadastro && tipo === 'participantes' };
  Object.entries(regras).forEach(([campo, visivel]) => {
    const label = el('campo-' + campo);
    label.hidden = !visivel;
    label.querySelector('input').required = visivel;
    label.querySelector('input').disabled = !visivel;
  });
  const form = el('form-acesso');
  form.elements.nome.maxLength = tipo === 'participantes' ? 60 : 80;
  form.elements.email.maxLength = tipo === 'organizadores' ? 40 : 45;
  form.elements.senha.autocomplete = cadastro ? 'new-password' : 'current-password';
  const nomeTipo = { participantes: 'participante', organizadores: 'organizador', suporte: 'suporte' }[tipo];
  el('titulo-acesso').textContent = (cadastro ? 'Cadastro ' : 'Acesso ') + nomeTipo;
  el('enviar-acesso').textContent = (cadastro ? 'Criar conta de ' : 'Entrar como ') + nomeTipo;
}
function mostrarSessao() {
  el('acesso').hidden = !!usuario;
  el('sessao').hidden = !usuario;
  el('form-acesso').elements.senha.value = '';
  el('form-perfil').elements.nova_senha.value = '';
  el('form-suporte').elements.senha.value = '';
  if (!usuario) { el('contas').replaceChildren(); camposAcesso(); return; }
  const suporte = usuario.perfil === 'SUPORTE';
  el('perfil-sessao').textContent = { PARTICIPANTE: 'Participante', ORGANIZADOR: 'Organizador', SUPORTE: 'Suporte' }[usuario.perfil];
  el('nome-sessao').textContent = usuario.nome;
  el('perfil-proprio').hidden = suporte;
  el('area-suporte').hidden = !suporte;
  if (!suporte) {
    const form = el('form-perfil');
    form.elements.nome.value = usuario.nome;
    form.elements.nome.maxLength = usuario.perfil === 'PARTICIPANTE' ? 60 : 80;
    form.elements.email.value = usuario.email;
    form.elements.email.maxLength = usuario.perfil === 'ORGANIZADOR' ? 40 : 45;
    el('perfil-cnpj').hidden = usuario.perfil !== 'ORGANIZADOR';
    form.elements.cnpj.disabled = usuario.perfil !== 'ORGANIZADOR';
    form.elements.cnpj.value = usuario.cnpj || '';
    el('documento').textContent = usuario.cpf ? 'CPF: ' + usuario.cpf + ' · Nascimento: ' + usuario.data_nascimento : '';
  }
}
el('tipo').addEventListener('change', camposAcesso);
el('modo').addEventListener('change', camposAcesso);
el('form-acesso').addEventListener('submit', event => {
  event.preventDefault();
  executar(el('enviar-acesso'), async () => {
    const tipo = el('tipo').value;
    const cadastro = el('modo').value === 'cadastro';
    const form = el('form-acesso');
    const dados = { email: form.elements.email.value, senha: form.elements.senha.value };
    if (cadastro) {
      dados.nome = form.elements.nome.value;
      if (tipo === 'participantes') { dados.cpf = form.elements.cpf.value; dados.data_nascimento = form.elements.data_nascimento.value; }
      if (tipo === 'organizadores') dados.cnpj = form.elements.cnpj.value;
    }
    await api(cadastro ? '/api/' + tipo : '/api/auth/' + tipo + '/login', 'POST', dados);
    if (cadastro) { el('modo').value = 'login'; camposAcesso(); mensagem('Conta criada. Entre com seu e-mail e senha.'); form.elements.senha.value = ''; }
    else { usuario = (await api('/api/auth/me')).dados; mostrarSessao(); mensagem('Você está conectado.'); }
  });
});
el('form-perfil').addEventListener('submit', event => {
  event.preventDefault();
  executar(event.submitter, async () => {
    const form = el('form-perfil');
    const dados = { nome: form.elements.nome.value, email: form.elements.email.value };
    if (form.elements.nova_senha.value) dados.nova_senha = form.elements.nova_senha.value;
    if (usuario.perfil === 'ORGANIZADOR') dados.cnpj = form.elements.cnpj.value;
    usuario = (await api('/api/me/perfil', 'PATCH', dados)).dados; mostrarSessao(); mensagem('Perfil atualizado.');
  });
});
el('sair').addEventListener('click', () => executar(el('sair'), async () => {
  await api('/api/auth/logout', 'POST'); usuario = null; mostrarSessao(); mensagem('Você saiu da sua conta.');
}));
el('desativar').addEventListener('click', () => executar(el('desativar'), async () => {
  await api('/api/me/desativacao', 'POST'); usuario = null; mostrarSessao(); mensagem('Conta desativada. Solicite ao suporte a reativação quando desejar voltar.');
}));
el('form-suporte').addEventListener('submit', event => {
  event.preventDefault();
  executar(event.submitter, async () => {
    await api('/api/suporte/usuarios', 'POST', Object.fromEntries(new FormData(el('form-suporte'))));
    el('form-suporte').reset(); mensagem('Novo suporte cadastrado. Já pode entrar e cadastrar outros suportes.');
  });
});
async function listar() {
  const tipo = el('tipo-contas').value;
  const resultado = await api('/api/suporte/contas?tipo=' + tipo + '&situacao=inativa&pagina=' + pagina);
  el('contas').replaceChildren();
  el('contas-status').textContent = resultado.total ? resultado.total + ' conta(s) desativada(s) · Página ' + pagina : 'Nenhuma conta desativada neste perfil.';
  el('anterior').disabled = pagina <= 1;
  el('proxima').disabled = pagina * resultado.tamanho >= resultado.total;
  resultado.dados.forEach(conta => {
    const li = document.createElement('li');
    const texto = document.createElement('span'); texto.textContent = conta.nome + ' · ' + conta.email;
    const botao = document.createElement('button'); botao.type = 'button'; botao.textContent = 'Reativar';
    botao.addEventListener('click', () => executar(botao, async () => {
      await api('/api/suporte/contas/' + tipo + '/' + conta.id + '/reativacao', 'POST');
      mensagem('Conta reativada. A operação foi registrada no histórico.');
      if (resultado.dados.length === 1 && pagina > 1) pagina--;
      await listar();
    }));
    li.append(texto, botao); el('contas').append(li);
  });
}
el('listar').addEventListener('click', () => executar(el('listar'), listar));
el('tipo-contas').addEventListener('change', () => { pagina = 1; el('contas').replaceChildren(); el('contas-status').textContent = ''; el('anterior').disabled = true; el('proxima').disabled = true; });
el('anterior').addEventListener('click', () => { pagina--; executar(el('listar'), listar); });
el('proxima').addEventListener('click', () => { pagina++; executar(el('listar'), listar); });
(async () => {
  try { usuario = (await api('/api/auth/me')).dados; mensagem('Você está conectado.'); }
  catch (erro) { mensagem(erro.status === 401 ? 'Escolha seu perfil para começar.' : erro.message, erro.status !== 401); }
  mostrarSessao();
})();
