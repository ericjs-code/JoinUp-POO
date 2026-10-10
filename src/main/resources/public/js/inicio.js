const status = document.getElementById('status');
const botao = document.getElementById('verificar');
async function verificar() {
  botao.disabled = true;
  status.textContent = 'Verificando disponibilidade…';
  try {
    const resposta = await fetch('/api/health', { signal: AbortSignal.timeout(5000) });
    const corpo = await resposta.json();
    if (!resposta.ok || corpo.dados?.status !== 'UP') throw new Error();
    status.textContent = 'Servidor disponível.';
  } catch {
    status.textContent = 'Servidor indisponível. Tente novamente em instantes.';
  } finally {
    botao.disabled = false;
  }
}
botao.addEventListener('click', verificar);
verificar();
