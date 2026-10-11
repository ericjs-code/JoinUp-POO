// Lógica do Módulo de Comunidades e Feed Social

(function () {
  let usuarioAtual = null;
  let comunidadeAtual = null;

  const urlParams = new URLSearchParams(window.location.search);
  const comunidadeIdParam = urlParams.get('id');

  const elMensagem = document.getElementById('mensagem');
  const secaoLista = document.getElementById('secao-lista');
  const secaoSala = document.getElementById('secao-sala');

  // Elementos da Lista
  const formBusca = document.getElementById('form-busca');
  const inputBusca = document.getElementById('input-busca');
  const statusLista = document.getElementById('status-lista');
  const gridComunidades = document.getElementById('grid-comunidades');

  // Elementos da Sala
  const salaNome = document.getElementById('sala-nome');
  const salaEvento = document.getElementById('sala-evento');
  const salaDescricao = document.getElementById('sala-descricao');
  const badgeMembro = document.getElementById('badge-membro');
  const btnParticipacao = document.getElementById('btn-participacao');
  const avisoAcesso = document.getElementById('aviso-acesso');

  // Abas
  const abaChatBtn = document.getElementById('aba-chat-btn');
  const abaFeedBtn = document.getElementById('aba-feed-btn');
  const abaChat = document.getElementById('aba-chat');
  const abaFeed = document.getElementById('aba-feed');

  // Chat
  const chatMensagens = document.getElementById('chat-mensagens');
  const formEnviarMsg = document.getElementById('form-enviar-msg');
  const inputMsgTexto = document.getElementById('input-msg-texto');

  // Feed
  const boxNovoPost = document.getElementById('box-novo-post');
  const formNovoPost = document.getElementById('form-novo-post');
  const inputPostTexto = document.getElementById('input-post-texto');
  const feedPostagens = document.getElementById('feed-postagens');

  function exibirMensagem(texto, erro = false) {
    if (!elMensagem) return;
    elMensagem.textContent = texto;
    elMensagem.className = erro ? 'erro' : 'sucesso';
    if (texto) {
      setTimeout(() => {
        if (elMensagem.textContent === texto) elMensagem.textContent = '';
      }, 5000);
    }
  }

  async function api(url, options = {}) {
    options.credentials = 'same-origin';
    const res = await fetch(url, options);
    const dados = await res.json().catch(() => ({}));
    if (!res.ok) {
      const msg = dados.erro?.mensagem || 'Falha ao processar operação.';
      throw new Error(msg);
    }
    return dados.dados !== undefined ? dados.dados : dados;
  }

  async function carregarSessao() {
    try {
      usuarioAtual = await api('/api/auth/me');
    } catch {
      usuarioAtual = null;
    }
  }

  // LISTAGEM DE COMUNIDADES
  async function carregarLista(busca = '') {
    secaoLista.hidden = false;
    secaoSala.hidden = true;
    statusLista.textContent = 'Carregando comunidades...';
    gridComunidades.innerHTML = '';

    try {
      const url = busca ? `/api/comunidades?busca=${encodeURIComponent(busca)}` : '/api/comunidades';
      const lista = await api(url);
      statusLista.textContent = '';

      if (!lista || lista.length === 0) {
        statusLista.textContent = 'Nenhuma comunidade encontrada.';
        return;
      }

      lista.forEach((c) => {
        const card = document.createElement('div');
        card.className = 'card-comunidade';
        card.innerHTML = `
          <div>
            <h3>${escapeHtml(c.nome)}</h3>
            <p class="evento-vinculado">★ Evento: ${escapeHtml(c.eventoNome)}</p>
            <p class="desc-comunidade">${escapeHtml(c.descricao || 'Comunidade oficial do evento.')}</p>
          </div>
          <div class="card-rodape">
            <span class="muted small">${c.dataCriacao ? new Date(c.dataCriacao).toLocaleDateString() : ''}</span>
            <a class="btn-abrir-sala" href="/comunidades.html?id=${c.id}">Acessar Comunidade →</a>
          </div>
        `;
        gridComunidades.appendChild(card);
      });
    } catch (e) {
      statusLista.textContent = e.message;
    }
  }

  // SALA DA COMUNIDADE
  async function carregarSala(id) {
    secaoLista.hidden = true;
    secaoSala.hidden = false;
    avisoAcesso.hidden = true;

    try {
      comunidadeAtual = await api(`/api/comunidades/${id}`);
      salaNome.textContent = comunidadeAtual.nome;
      salaEvento.textContent = '★ Evento: ' + comunidadeAtual.eventoNome;
      salaDescricao.textContent = comunidadeAtual.descricao || 'Comunidade oficial do evento.';

      atualizarStatusParticipacao();

      // Carregar conteúdo inicial
      if (comunidadeAtual.membroAtivo) {
        carregarMensagens(id);
        carregarFeed(id);
      } else {
        chatMensagens.innerHTML = '<p class="muted">Você precisa ser membro da comunidade para visualizar as mensagens.</p>';
        feedPostagens.innerHTML = '<p class="muted">Você precisa ser membro da comunidade para visualizar o feed.</p>';
      }
    } catch (e) {
      exibirMensagem(e.message, true);
    }
  }

  function atualizarStatusParticipacao() {
    btnParticipacao.hidden = true;
    btnParticipacao.onclick = null;
    boxNovoPost.hidden = true;
    formEnviarMsg.hidden = true;

    if (!usuarioAtual) {
      badgeMembro.textContent = 'Visitante';
      badgeMembro.className = 'badge-status';
      avisoAcesso.hidden = false;
      avisoAcesso.textContent = 'Você está visualizando a comunidade como visitante. Faça login com sua conta de participante para interagir.';
      return;
    }

    if (usuarioAtual.perfil === 'ORGANIZADOR') {
      if (comunidadeAtual.membroAtivo) {
        badgeMembro.textContent = 'Organizador Oficial';
        badgeMembro.className = 'badge-status membro';
        formEnviarMsg.hidden = false; // Organizador pode enviar comunicados
      } else {
        badgeMembro.textContent = 'Organizador Externo';
        badgeMembro.className = 'badge-status';
        avisoAcesso.hidden = false;
        avisoAcesso.textContent = 'Apenas o organizador responsável por este evento tem acesso à comunidade.';
      }
      return;
    }

    // Participante
    if (comunidadeAtual.membroAtivo) {
      badgeMembro.textContent = 'Membro Ativo';
      badgeMembro.className = 'badge-status membro';
      btnParticipacao.textContent = 'Sair da Comunidade';
      btnParticipacao.className = 'btn-participacao sair';
      btnParticipacao.hidden = false;
      btnParticipacao.onclick = sairDaComunidade;

      boxNovoPost.hidden = false;
      formEnviarMsg.hidden = false;
      avisoAcesso.hidden = true;
    } else if (comunidadeAtual.elegivelParaEntrar) {
      badgeMembro.textContent = 'Ingresso Elegível';
      badgeMembro.className = 'badge-status';
      btnParticipacao.textContent = 'Entrar na Comunidade';
      btnParticipacao.className = 'btn-participacao entrar';
      btnParticipacao.hidden = false;
      btnParticipacao.onclick = entrarNaComunidade;

      avisoAcesso.hidden = false;
      avisoAcesso.textContent = 'Você possui ingresso para este evento! Clique em "Entrar na Comunidade" para acessar o chat e o feed.';
    } else {
      badgeMembro.textContent = 'Sem Ingresso';
      badgeMembro.className = 'badge-status';
      avisoAcesso.hidden = false;
      avisoAcesso.textContent = 'Para entrar nesta comunidade, é necessário possuir um ingresso ativo ou utilizado deste evento (RF-COM-05).';
    }
  }

  async function entrarNaComunidade() {
    try {
      btnParticipacao.disabled = true;
      await api(`/api/comunidades/${comunidadeAtual.id}/participacao`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });
      exibirMensagem('Você entrou na comunidade!');
      await carregarSala(comunidadeAtual.id);
    } catch (e) {
      exibirMensagem(e.message, true);
    } finally {
      btnParticipacao.disabled = false;
    }
  }

  async function sairDaComunidade() {
    if (!confirm('Deseja realmente sair desta comunidade? Suas mensagens históricas serão preservadas.')) return;
    try {
      btnParticipacao.disabled = true;
      await api(`/api/comunidades/${comunidadeAtual.id}/participacao`, {
        method: 'DELETE'
      });
      exibirMensagem('Você saiu da comunidade.');
      await carregarSala(comunidadeAtual.id);
    } catch (e) {
      exibirMensagem(e.message, true);
    } finally {
      btnParticipacao.disabled = false;
    }
  }

  // MENSAGENS / CHAT
  async function carregarMensagens(comunidadeId) {
    try {
      const mensagens = await api(`/api/comunidades/${comunidadeId}/mensagens`);
      chatMensagens.innerHTML = '';
      if (!mensagens || mensagens.length === 0) {
        chatMensagens.innerHTML = '<p class="muted">Nenhuma mensagem enviada ainda. Seja o primeiro a falar!</p>';
        return;
      }

      mensagens.forEach((m) => {
        const item = document.createElement('div');
        item.className = `msg-item ${m.autorTipo === 'organizador' ? 'organizador' : 'participante'}`;
        const isOrg = m.autorTipo === 'organizador';
        item.innerHTML = `
          <div class="msg-autor">
            <span>${escapeHtml(m.autorNome || 'Anônimo')}</span>
            ${isOrg ? '<span class="badge-organizador">Organizador Oficial</span>' : ''}
            <span class="msg-hora">${m.dataEnvio ? new Date(m.dataEnvio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
          </div>
          <div class="msg-texto">${escapeHtml(m.mensagem)}</div>
        `;
        chatMensagens.appendChild(item);
      });

      chatMensagens.scrollTop = chatMensagens.scrollHeight;
    } catch (e) {
      chatMensagens.innerHTML = `<p class="erro">${escapeHtml(e.message)}</p>`;
    }
  }

  if (formEnviarMsg) {
    formEnviarMsg.addEventListener('submit', async (e) => {
      e.preventDefault();
      const texto = inputMsgTexto.value.trim();
      if (!texto) return;

      try {
        await api(`/api/comunidades/${comunidadeAtual.id}/mensagens`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ mensagem: texto })
        });
        inputMsgTexto.value = '';
        await carregarMensagens(comunidadeAtual.id);
      } catch (err) {
        exibirMensagem(err.message, true);
      }
    });
  }

  // FEED SOCIAL
  async function carregarFeed(comunidadeId) {
    try {
      const posts = await api(`/api/comunidades/${comunidadeId}/postagens`);
      feedPostagens.innerHTML = '';
      if (!posts || posts.length === 0) {
        feedPostagens.innerHTML = '<p class="muted">Nenhuma publicação no feed ainda. Compartilhe algo!</p>';
        return;
      }

      posts.forEach((p) => {
        const card = document.createElement('div');
        card.className = 'card-post';
        card.dataset.postId = p.id;
        card.innerHTML = `
          <div class="card-post-autor">
            <span class="post-autor-nome">${escapeHtml(p.autorNome || 'Participante')}</span>
            <span class="post-data">${p.dataPostagem ? new Date(p.dataPostagem).toLocaleString() : ''}</span>
          </div>
          <div class="post-conteudo">${escapeHtml(p.descricao)}</div>
          <div class="post-interacoes">
            <button class="btn-curtir ${p.curtidoPeloUsuario ? 'curtido' : ''}" type="button" data-post-id="${p.id}">
              <span>♥</span>
              <span class="qtd-curtidas">${p.curtidas || 0}</span>
            </button>
            <button class="btn-comentarios-toggle" type="button" data-post-id="${p.id}">
              💬 Comentários (<span class="qtd-comentarios">${p.totalComentarios || 0}</span>)
            </button>
          </div>
          <div class="post-comentarios-thread" id="thread-${p.id}" hidden>
            <div class="lista-comentarios" id="comentarios-${p.id}">
              <p class="muted small">Carregando comentários...</p>
            </div>
            ${usuarioAtual && usuarioAtual.perfil === 'PARTICIPANTE' && comunidadeAtual.membroAtivo ? `
              <form class="form-novo-comentario" data-post-id="${p.id}">
                <input name="texto" maxlength="500" placeholder="Escreva um comentário..." required>
                <button type="submit">Comentar</button>
              </form>
            ` : ''}
          </div>
        `;

        // Event listener de curtir
        const btnCurtir = card.querySelector('.btn-curtir');
        btnCurtir.addEventListener('click', () => alternarCurtida(p.id, btnCurtir));

        // Event listener de comentários
        const btnComentarios = card.querySelector('.btn-comentarios-toggle');
        const thread = card.querySelector(`#thread-${p.id}`);
        btnComentarios.addEventListener('click', () => alternarThreadComentarios(p.id, thread));

        // Form de novo comentário
        const formComentario = card.querySelector('.form-novo-comentario');
        if (formComentario) {
          formComentario.addEventListener('submit', (evt) => enviarComentario(evt, p.id, card));
        }

        feedPostagens.appendChild(card);
      });
    } catch (e) {
      feedPostagens.innerHTML = `<p class="erro">${escapeHtml(e.message)}</p>`;
    }
  }

  async function alternarCurtida(postId, btn) {
    if (!usuarioAtual || usuarioAtual.perfil !== 'PARTICIPANTE') {
      exibirMensagem('Apenas participantes podem curtir postagens.', true);
      return;
    }
    const jaCurtido = btn.classList.contains('curtido');
    try {
      const atualizado = await api(`/api/postagens/${postId}/curtida`, {
        method: jaCurtido ? 'DELETE' : 'PUT'
      });
      btn.classList.toggle('curtido', !jaCurtido);
      btn.querySelector('.qtd-curtidas').textContent = atualizado.curtidas;
    } catch (e) {
      exibirMensagem(e.message, true);
    }
  }

  async function alternarThreadComentarios(postId, threadEl) {
    const hidden = threadEl.hidden;
    threadEl.hidden = !hidden;
    if (!hidden) return;

    const listaEl = threadEl.querySelector(`#comentarios-${postId}`);
    try {
      const comentarios = await api(`/api/postagens/${postId}/comentarios`);
      listaEl.innerHTML = '';
      if (!comentarios || comentarios.length === 0) {
        listaEl.innerHTML = '<p class="muted small">Nenhum comentário ainda.</p>';
        return;
      }
      comentarios.forEach((c) => {
        const item = document.createElement('div');
        item.className = 'comentario-item';
        item.innerHTML = `
          <div class="comentario-header">
            <span class="comentario-autor">${escapeHtml(c.autorNome || 'Participante')}</span>
            <span class="comentario-data">${c.dataComentario ? new Date(c.dataComentario).toLocaleString() : ''}</span>
          </div>
          <div class="comentario-texto">${escapeHtml(c.texto)}</div>
        `;
        listaEl.appendChild(item);
      });
    } catch (e) {
      listaEl.innerHTML = `<p class="erro small">${escapeHtml(e.message)}</p>`;
    }
  }

  async function enviarComentario(evt, postId, card) {
    evt.preventDefault();
    const input = evt.target.querySelector('input[name="texto"]');
    const texto = input.value.trim();
    if (!texto) return;

    try {
      await api(`/api/postagens/${postId}/comentarios`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ texto })
      });
      input.value = '';
      const thread = card.querySelector(`#thread-${postId}`);
      thread.hidden = false;
      await alternarThreadComentarios(postId, thread);
      // Atualizar contagem
      const spanQtd = card.querySelector('.qtd-comentarios');
      spanQtd.textContent = parseInt(spanQtd.textContent || '0', 10) + 1;
    } catch (e) {
      exibirMensagem(e.message, true);
    }
  }

  if (formNovoPost) {
    formNovoPost.addEventListener('submit', async (e) => {
      e.preventDefault();
      const texto = inputPostTexto.value.trim();
      if (!texto) return;

      try {
        await api(`/api/comunidades/${comunidadeAtual.id}/postagens`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ descricao: texto })
        });
        inputPostTexto.value = '';
        exibirMensagem('Publicação criada no feed!');
        await carregarFeed(comunidadeAtual.id);
      } catch (err) {
        exibirMensagem(err.message, true);
      }
    });
  }

  // CONTROLE DE ABAS
  if (abaChatBtn && abaFeedBtn) {
    abaChatBtn.addEventListener('click', () => {
      abaChatBtn.classList.add('ativa');
      abaFeedBtn.classList.remove('ativa');
      abaChat.hidden = false;
      abaFeed.hidden = true;
    });

    abaFeedBtn.addEventListener('click', () => {
      abaFeedBtn.classList.add('ativa');
      abaChatBtn.classList.remove('ativa');
      abaFeed.hidden = false;
      abaChat.hidden = true;
    });
  }

  // BUSCA
  if (formBusca) {
    formBusca.addEventListener('submit', (e) => {
      e.preventDefault();
      carregarLista(inputBusca.value.trim());
    });
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // INICIALIZAÇÃO
  async function init() {
    await carregarSessao();
    if (comunidadeIdParam) {
      await carregarSala(comunidadeIdParam);
    } else {
      await carregarLista();
    }
  }

  init();
})();
