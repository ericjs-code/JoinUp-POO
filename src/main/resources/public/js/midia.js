'use strict';
window.JoinUpMidia = {
  referencia(valor) { return typeof valor === 'string' && /^\/api\/arquivos\/[a-f0-9]{32}$/.test(valor); },
  mostrar(container, referencia, alt = 'Imagem da comunidade') {
    if (!this.referencia(referencia)) return;
    const img = document.createElement('img'); img.src = referencia; img.alt = alt; img.className = 'imagem-midia';
    img.addEventListener('error', () => { img.replaceWith(document.createTextNode('Imagem indisponível ou acesso encerrado.')); });
    container.append(img);
  },
  validar(file) {
    if (!file || !['image/png','image/jpeg','image/webp'].includes(file.type) || file.size > 1048576) throw new Error('Selecione PNG, JPEG ou WebP de até 1 MiB.');
  },
  preparar(input) {
    const preview = document.createElement('img'); preview.hidden = true; preview.className = 'imagem-midia'; preview.alt = 'Prévia da imagem selecionada'; input.after(preview);
    let url = null;
    const limpar = () => { if (url) URL.revokeObjectURL(url); url = null; preview.removeAttribute('src'); preview.hidden = true; };
    input.addEventListener('change', () => {
      limpar(); input.setCustomValidity('');
      if (!input.files[0]) return;
      try { this.validar(input.files[0]); url = URL.createObjectURL(input.files[0]); preview.src = url; preview.hidden = false; }
      catch (e) { input.setCustomValidity(e.message); input.reportValidity(); }
    });
    input.form?.addEventListener('reset', limpar);
    window.addEventListener('pagehide', limpar, {once:true});
  },
  async enviar(input, contexto, alvoId) {
    const file = input.files[0]; if (!file) return null; this.validar(file);
    const ext = {'image/png':'png','image/jpeg':'jpg','image/webp':'webp'}[file.type];
    const form = new FormData(); form.append('contexto',contexto); form.append('alvoId',String(alvoId)); form.append('arquivo',file,'imagem.'+ext);
    const r = await fetch('/api/arquivos',{method:'POST',credentials:'same-origin',body:form});
    const j = await r.json(); if (!r.ok) throw new Error(j.erro?.mensagem || 'Falha no envio da imagem.'); return j.dados.imagem;
  }
};
document.querySelectorAll('input[data-midia]').forEach(input => JoinUpMidia.preparar(input));
