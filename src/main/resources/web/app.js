'use strict';

/*
 * Interface web do projeto. Toda a lógica de grafo roda no Java (POST /api/analisar);
 * aqui só ficam o cadastro, o desenho do grafo e a reprodução dos passos que os
 * algoritmos registraram.
 */

const $ = (sel) => document.querySelector(sel);
const NS = 'http://www.w3.org/2000/svg';
const CHAVE = 'ordenacao-disciplinas:v1';

const estado = {
  disciplinas: [],   // [{codigo, nome, periodo, preRequisitos: []}]
  analise: null,     // resposta do servidor
  cenarios: [],
  editando: null,    // código em edição no formulário
  alg: 'kahn',       // algoritmo mostrado na animação
  passo: 0,
  aba: 'comparacao',
  colunas: 'grade',  // 'grade' (período cadastrado) ou 'kahn' (período mínimo)
  maisOrdenacoes: false,
  timer: null,
  foco: null,        // vértice sob o mouse
  pedido: 0,
};

// ------------------------------------------------------------------ utilidades

function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function el(nome, attrs = {}, pai = null) {
  const e = document.createElementNS(NS, nome);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  if (pai) pai.appendChild(e);
  return e;
}

function sequencia(lista, classe = '', classesPorItem = null) {
  if (!lista.length) return '<span class="sep">(nenhuma)</span>';
  return `<div class="sequencia ${classe}">` + lista.map((v, i) =>
    (i ? '<span class="sep">→</span>' : '') +
    `<span class="item ${classesPorItem ? classesPorItem(v) : ''}">${esc(v)}</span>`).join('') + '</div>';
}

function nomeDe(codigo) {
  const d = estado.disciplinas.find((x) => x.codigo === codigo);
  return d ? d.nome : codigo;
}

function formatarGrande(texto) {
  try { return BigInt(texto).toLocaleString('pt-BR'); } catch (e) { return texto; }
}

/** Número curto para caber no cartão: até 15 dígitos por extenso, acima disso ≈ a,bc × 10^n. */
function formatarCurto(texto, html = true) {
  if (texto.length <= 15) return formatarGrande(texto);
  const exp = texto.length - 1;
  const mantissa = `${texto[0]},${texto.slice(1, 3)}`;
  return html ? `≈ ${mantissa} × 10<sup>${exp}</sup>` : `≈ ${mantissa} × 10^${exp}`;
}

function clonar(lista) {
  return lista.map((d) => ({ codigo: d.codigo, nome: d.nome, periodo: d.periodo || 0, preRequisitos: [...d.preRequisitos] }));
}

function salvarLocal() {
  try { localStorage.setItem(CHAVE, JSON.stringify(estado.disciplinas)); } catch (e) { /* sem armazenamento */ }
}

function lerLocal() {
  try {
    const t = localStorage.getItem(CHAVE);
    const v = t ? JSON.parse(t) : null;
    return Array.isArray(v) ? v : null;
  } catch (e) { return null; }
}

// ------------------------------------------------------------------------ API

async function api(caminho, corpo) {
  const opcoes = corpo === undefined ? {} : {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(corpo),
  };
  let resp;
  try {
    resp = await fetch(caminho, opcoes);
  } catch (e) {
    throw new Error('Não foi possível falar com o servidor Java. Confira se a janela do "java -jar ... --web" continua aberta.');
  }
  const dados = await resp.json().catch(() => ({}));
  if (!resp.ok) throw new Error(dados.erro || `erro ${resp.status}`);
  return dados;
}

async function analisar() {
  salvarLocal();
  pararAnimacao();
  const pedido = ++estado.pedido;
  if (!estado.disciplinas.length) {
    estado.analise = null;
    mostrarAviso(null);
    renderizar();
    return;
  }
  try {
    const r = await api('/api/analisar', { disciplinas: estado.disciplinas });
    if (pedido !== estado.pedido) return;
    estado.analise = r;
    mostrarAviso(null);
  } catch (e) {
    if (pedido !== estado.pedido) return;
    estado.analise = null;
    mostrarAviso(e.message);
  }
  estado.passo = Math.max(0, passos().length - 1); // começa mostrando o resultado final
  renderizar();
}

function mostrarAviso(msg) {
  const a = $('#aviso');
  a.hidden = !msg;
  a.textContent = msg ? `Erro: ${msg}` : '';
}

// -------------------------------------------------------------------- cadastro

function erroForm(msg) {
  $('#erro-form').textContent = msg;
}

function lerPreRequisitos(texto) {
  return [...new Set(texto.split(/[,;]/).map((s) => s.trim().toUpperCase()).filter(Boolean))];
}

function salvarFormulario() {
  const codigo = $('#f-codigo').value.trim().toUpperCase();
  const nome = $('#f-nome').value.trim();
  const textoPeriodo = $('#f-periodo').value.trim();
  const pre = lerPreRequisitos($('#f-pre').value);

  if (!codigo) return erroForm('Informe o código.');
  if (/[,;]/.test(codigo)) return erroForm('O código não pode ter vírgula nem ponto e vírgula.');
  if (!nome) return erroForm('Informe o nome.');
  let periodo = 0;
  if (textoPeriodo) {
    periodo = Number(textoPeriodo);
    if (!Number.isInteger(periodo) || periodo < 0) return erroForm('O período precisa ser um número inteiro (0 ou mais).');
  }
  const outros = estado.disciplinas.filter((d) => d.codigo !== estado.editando);
  if (outros.some((d) => d.codigo === codigo)) return erroForm(`Já existe a disciplina ${codigo}.`);
  const validos = new Set([...outros.map((d) => d.codigo), codigo]);
  const faltando = pre.filter((p) => !validos.has(p));
  if (faltando.length) return erroForm(`Cadastre antes o pré-requisito: ${faltando.join(', ')}.`);

  const nova = { codigo, nome, periodo, preRequisitos: pre };
  if (estado.editando) {
    const antigo = estado.editando;
    estado.disciplinas = estado.disciplinas.map((d) => (d.codigo === antigo ? nova : d));
    if (antigo !== codigo) {
      // renomeia o código também nos pré-requisitos das outras disciplinas
      estado.disciplinas.forEach((d) => { d.preRequisitos = d.preRequisitos.map((p) => (p === antigo ? codigo : p)); });
    }
  } else {
    estado.disciplinas.push(nova);
  }
  limparFormulario();
  $('#sel-cenario').value = '';
  analisar();
}

function limparFormulario() {
  estado.editando = null;
  $('#form-disc').reset();
  erroForm('');
  $('#titulo-form').textContent = 'Cadastrar disciplina';
  $('#btn-salvar').textContent = 'Adicionar';
  $('#btn-cancelar').hidden = true;
  renderizarLista();
}

function editar(codigo) {
  const d = estado.disciplinas.find((x) => x.codigo === codigo);
  if (!d) return;
  estado.editando = codigo;
  $('#f-codigo').value = d.codigo;
  $('#f-nome').value = d.nome;
  $('#f-periodo').value = d.periodo ? d.periodo : '';
  $('#f-pre').value = d.preRequisitos.join(', ');
  erroForm('');
  $('#titulo-form').textContent = `Editar ${d.codigo}`;
  $('#btn-salvar').textContent = 'Salvar alterações';
  $('#btn-cancelar').hidden = false;
  renderizarLista();
  $('#form-disc').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  $('#f-nome').focus();
}

function remover(codigo) {
  estado.disciplinas = estado.disciplinas.filter((d) => d.codigo !== codigo);
  estado.disciplinas.forEach((d) => { d.preRequisitos = d.preRequisitos.filter((p) => p !== codigo); });
  if (estado.editando === codigo) limparFormulario();
  $('#sel-cenario').value = '';
  analisar();
}

function adicionarDependencia() {
  const pre = $('#d-pre').value;
  const disc = $('#d-disc').value;
  const erro = $('#erro-dep');
  if (!pre || !disc) { erro.textContent = 'Cadastre disciplinas primeiro.'; return; }
  if (pre === disc) { erro.textContent = 'Escolha duas disciplinas diferentes.'; return; }
  const d = estado.disciplinas.find((x) => x.codigo === disc);
  if (d.preRequisitos.includes(pre)) { erro.textContent = `${pre} já é pré-requisito de ${disc}.`; return; }
  erro.textContent = '';
  d.preRequisitos.push(pre);
  $('#sel-cenario').value = '';
  analisar();
}

function carregarCenario(i) {
  const c = estado.cenarios[i];
  if (!c) return;
  estado.disciplinas = clonar(c.disciplinas);
  estado.maisOrdenacoes = false;
  limparFormulario();
  analisar();
}

// ------------------------------------------------------------------- render

function renderizar() {
  renderizarLista();
  renderizarSelects();
  const a = estado.analise;
  const temDisc = estado.disciplinas.length > 0;
  $('#vazio').hidden = temDisc;
  $('#resultado').hidden = !a;
  if (!a) return;
  renderizarResumo();
  renderizarAnimacao();
  renderizarAba();
}

function renderizarLista() {
  const ul = $('#lista-disc');
  $('#contagem').textContent = estado.disciplinas.length;
  if (!estado.disciplinas.length) {
    ul.innerHTML = '<li class="lista-vazia">Nenhuma disciplina ainda.</li>';
    return;
  }
  ul.innerHTML = estado.disciplinas.map((d) => `
    <li class="${d.codigo === estado.editando ? 'editando' : ''}">
      <div class="nome"><span class="cod">${esc(d.codigo)}</span> ${esc(d.nome)}</div>
      <div class="det">${d.periodo ? `Período ${d.periodo} · ` : ''}${d.preRequisitos.length
        ? `pré-requisitos: ${d.preRequisitos.map(esc).join(', ')}` : 'sem pré-requisitos'}</div>
      <div class="acoes">
        <button type="button" class="icone" data-editar="${esc(d.codigo)}" title="Editar ${esc(d.codigo)}">✎</button>
        <button type="button" class="icone perigo" data-remover="${esc(d.codigo)}" title="Remover ${esc(d.codigo)}">✕</button>
      </div>
    </li>`).join('');
}

function renderizarSelects() {
  const opcoes = estado.disciplinas.map((d) => `<option value="${esc(d.codigo)}">${esc(d.codigo)} – ${esc(d.nome)}</option>`).join('');
  for (const id of ['#d-pre', '#d-disc']) {
    const s = $(id);
    const atual = s.value;
    s.innerHTML = opcoes;
    if (estado.disciplinas.some((d) => d.codigo === atual)) s.value = atual;
  }
  $('#lista-codigos').innerHTML = estado.disciplinas.map((d) => `<option value="${esc(d.codigo)}">${esc(d.nome)}</option>`).join('');
}

function renderizarResumo() {
  const a = estado.analise;
  const cartoes = [
    { rotulo: 'Disciplinas (V)', valor: a.v, sub: 'vértices do grafo' },
    { rotulo: 'Dependências (E)', valor: a.e, sub: 'arestas pré-requisito → disciplina' },
  ];
  if (a.possuiCiclo) {
    cartoes.push({ rotulo: 'Situação', valor: 'Ciclo', classe: 'erro',
      sub: a.dfs.ciclo.length ? `dependência circular: ${a.dfs.ciclo.join(' → ')}` : 'dependência circular' });
    cartoes.push({ rotulo: 'Ordenações válidas', valor: '0', classe: 'erro', sub: 'com ciclo não existe nenhuma' });
  } else {
    cartoes.push({ rotulo: 'Situação', valor: 'Sem ciclo', classe: 'ok', sub: `grade possível em ${a.periodos.length} período(s) no mínimo` });
    const t = a.ordenacoes.total;
    cartoes.push({ rotulo: 'Ordenações válidas',
      valorHtml: t === null ? `&gt; ${a.ordenacoes.exemplos.length}` : formatarCurto(t),
      sub: t === null ? 'grafo grande demais para a contagem exata'
        : (t.length > 15 ? `número exato com ${t.length} dígitos (aba Todas as ordenações)` : 'ordenações topológicas distintas') });
  }
  $('#resumo').innerHTML = cartoes.map((c) => `
    <div class="indicador ${c.classe || ''}">
      <div class="rotulo">${c.rotulo}</div>
      <div class="valor">${c.valorHtml !== undefined ? c.valorHtml : esc(c.valor)}</div>
      <div class="sub">${esc(c.sub)}</div>
    </div>`).join('');
}

// ----------------------------------------------------- animação dos passos

function passos() {
  return estado.analise ? estado.analise[estado.alg].passos : [];
}

function listaJava(texto) {
  const t = texto.trim();
  return t ? t.split(', ') : [];
}

/** Reexecuta os passos registrados pelo Java até o passo k e devolve o estado de cada vértice. */
function estadoNoPasso(k) {
  const a = estado.analise;
  const ps = a[estado.alg].passos;
  const classe = new Map();
  const numero = new Map();
  const ordem = [];
  let atual = null;
  let fimDfs = false;
  let cicloVisto = false;
  for (let i = 0; i <= k && i < ps.length; i++) {
    const p = ps[i];
    atual = null;
    let m;
    if (estado.alg === 'kahn') {
      if ((m = p.match(/^Fila inicial \(grau zero\): \[(.*)\]$/))) {
        listaJava(m[1]).forEach((v) => classe.set(v, 'fila'));
      } else if ((m = p.match(/^Retira (.+?) da fila/))) {
        ordem.push(m[1]);
        classe.set(m[1], 'feito');
        numero.set(m[1], ordem.length);
        atual = m[1];
        const f = p.match(/Fila: \[(.*)\]$/);
        if (f) listaJava(f[1]).forEach((v) => classe.set(v, 'fila'));
      } else if (p.startsWith('Fila vazia')) {
        cicloVisto = true;
      }
    } else if ((m = p.match(/^Entra em (.+?) \(em processamento\)/))) {
      classe.set(m[1], 'processando');
      atual = m[1];
    } else if ((m = p.match(/^Finaliza (.+?)\. Finalizados:/))) {
      ordem.push(m[1]);
      classe.set(m[1], 'feito');
      numero.set(m[1], ordem.length);
      atual = m[1];
    } else if (p.startsWith('Aresta de retorno')) {
      cicloVisto = true;
    } else if (p.startsWith('Ordem inversa')) {
      fimDfs = true;
    }
  }
  if (fimDfs) {
    numero.clear();
    a.dfs.ordem.forEach((v, i) => numero.set(v, i + 1));
  }
  return { classe, numero, ordem: fimDfs ? a.dfs.ordem : ordem, atual, fimDfs, cicloVisto };
}

function renderizarAnimacao() {
  const ps = passos();
  estado.passo = Math.min(Math.max(0, estado.passo), Math.max(0, ps.length - 1));
  const st = estadoNoPasso(estado.passo);

  document.querySelectorAll('#alt-alg button').forEach((b) => b.classList.toggle('ativo', b.dataset.alg === estado.alg));
  $('#alt-colunas').hidden = !podeUsarPeriodoDaGrade(estado.analise);
  document.querySelectorAll('#alt-colunas button').forEach((b) => b.classList.toggle('ativo', b.dataset.colunas === estado.colunas));
  $('#contador-passo').textContent = `Passo ${estado.passo + 1} de ${ps.length}`;
  $('#texto-passo').textContent = ps[estado.passo] || '';
  const ultimo = estado.passo >= ps.length - 1;
  $('#b-inicio').disabled = $('#b-anterior').disabled = estado.passo === 0;
  $('#b-proximo').disabled = $('#b-fim').disabled = ultimo;
  $('#b-tocar').textContent = estado.timer ? '⏸ Pausar' : (ultimo ? '↺ Do início' : '▶ Reproduzir');

  let rotulo;
  if (estado.alg === 'kahn') rotulo = 'Ordem até aqui:';
  else rotulo = st.fimDfs ? 'Ordenação (inverso da finalização):' : 'Finalizados, em ordem:';
  $('#rotulo-ordem').textContent = rotulo;
  $('#ordem-parcial').innerHTML = sequencia(st.ordem, 'pequena', () => 'feito');

  const legenda = estado.alg === 'kahn'
    ? [['', 'Ainda com pré-requisito pendente'], ['fila', 'Na fila (grau de entrada 0)'], ['feito', 'Já ordenada (nº = posição)']]
    : [['', 'Não visitada'], ['processando', 'Em processamento (na pilha)'], ['feito', st.fimDfs ? 'Finalizada (nº = posição na ordem)' : 'Finalizada (nº = ordem de finalização)']];
  if (estado.analise.possuiCiclo) legenda.push(['ciclo', 'Faz parte do ciclo / bloqueada']);
  $('#legenda').innerHTML = legenda.map(([c, t]) => `<li><i class="${c}"></i>${t}</li>`).join('');

  desenharGrafo(st);
  if (estado.aba === 'passos') renderizarAba();
}

function pararAnimacao() {
  if (estado.timer) clearInterval(estado.timer);
  estado.timer = null;
}

function irPara(k) {
  pararAnimacao();
  estado.passo = k;
  renderizarAnimacao();
}

function alternarReproducao() {
  if (estado.timer) {
    pararAnimacao();
    renderizarAnimacao();
    return;
  }
  const ultimo = passos().length - 1;
  if (estado.passo >= ultimo) estado.passo = 0;
  estado.timer = setInterval(() => {
    if (estado.passo >= passos().length - 1) {
      pararAnimacao();
    } else {
      estado.passo++;
    }
    renderizarAnimacao();
  }, 900);
  renderizarAnimacao();
}

// ---------------------------------------------------------- desenho do grafo

const W = 172;   // largura do nó
const H = 50;    // altura do nó
const GX = 66;   // espaço entre colunas
const GY = 16;   // espaço entre nós da mesma coluna
const MARGEM = 22;
const TOPO = 40;

/** Dá para organizar as colunas pelo período cadastrado quando há período informado e não há ciclo. */
function podeUsarPeriodoDaGrade(a) {
  return a.temPeriodoInformado && !a.possuiCiclo;
}

function usarPeriodoDaGrade(a) {
  return podeUsarPeriodoDaGrade(a) && estado.colunas === 'grade';
}

/**
 * Colunas pelo período da grade, pelo período mínimo (Kahn por níveis) ou, com ciclo, por nível;
 * o que fica preso no ciclo vai para a última coluna.
 */
function calcularLayout(a) {
  const vertices = a.disciplinas.map((d) => d.codigo);
  const indice = new Map(vertices.map((v, i) => [v, i]));
  const pred = new Map(vertices.map((v) => [v, []]));
  a.arestas.forEach(({ de, para }) => pred.get(para).push(de));

  const porGrade = usarPeriodoDaGrade(a);
  const nivel = new Map();
  let titulo = (i) => `${a.possuiCiclo ? 'Nível' : 'Período'} ${i + 1}`;
  if (porGrade) {
    // colunas pelo período (SE) cadastrado; quem não tem período vai para a última
    const maxPeriodo = Math.max(...a.disciplinas.map((d) => d.periodo));
    a.disciplinas.forEach((d) => nivel.set(d.codigo, d.periodo > 0 ? d.periodo - 1 : maxPeriodo));
    titulo = (i) => (i < maxPeriodo ? `Período ${i + 1}` : 'Sem período');
  } else if (!a.possuiCiclo) {
    a.periodos.forEach((per, i) => per.forEach((v) => nivel.set(v, i)));
  } else {
    a.kahn.ordem.forEach((v) => {
      const ps = pred.get(v);
      nivel.set(v, ps.length ? Math.max(...ps.map((p) => nivel.get(p))) + 1 : 0);
    });
  }
  // disciplinas sem nenhuma aresta vão para uma faixa separada embaixo, para não alongar o 1º período
  const ligados = new Set();
  a.arestas.forEach(({ de, para }) => { ligados.add(de); ligados.add(para); });
  const semLigacao = vertices.filter((v) => !ligados.has(v));
  // (pelo período da grade, cada uma já fica no seu semestre)
  const isolados = !porGrade && a.arestas.length && semLigacao.length >= 4 ? semLigacao : [];
  const soltos = new Set(isolados);

  const niveis = nivel.size ? Math.max(...nivel.values()) + 1 : 0;
  const colunas = [];
  const titulos = [];
  for (let i = 0; i < niveis; i++) {
    colunas.push([]);
    titulos.push(titulo(i));
  }
  vertices.forEach((v) => { if (nivel.has(v) && !soltos.has(v)) colunas[nivel.get(v)].push(v); });
  const presos = vertices.filter((v) => !nivel.has(v));
  if (presos.length) {
    colunas.push(presos);
    titulos.push('Bloqueadas pelo ciclo');
  }

  // heurística do baricentro: aproxima cada nó da altura média dos seus pré-requisitos
  const linha = new Map();
  const bari = (v) => {
    const ps = pred.get(v).filter((p) => linha.has(p));
    return ps.length ? ps.reduce((s, p) => s + linha.get(p), 0) / ps.length : 1e9;
  };
  colunas.forEach((col, c) => {
    if (c > 0) col.sort((x, y) => (bari(x) - bari(y)) || (indice.get(x) - indice.get(y)));
    col.forEach((v, i) => linha.set(v, i));
  });

  const maxLinhas = Math.max(1, ...colunas.map((c) => c.length));
  const pos = new Map();
  colunas.forEach((col, c) => {
    const deslocamento = ((maxLinhas - col.length) * (H + GY)) / 2;
    col.forEach((v, i) => pos.set(v, { x: MARGEM + c * (W + GX), y: TOPO + deslocamento + i * (H + GY), col: c }));
  });
  let altura = TOPO + maxLinhas * (H + GY) - GY + MARGEM;
  let faixa = null;
  const porLinha = Math.max(colunas.length, 3);
  if (isolados.length) {
    const y0 = altura + 34;
    isolados.forEach((v, i) => pos.set(v, {
      x: MARGEM + (i % porLinha) * (W + GX), y: y0 + Math.floor(i / porLinha) * (H + GY), col: -1,
    }));
    faixa = { y: y0 - 14, titulo: `Sem pré-requisito e sem dependentes (${isolados.length})` };
    altura = y0 + Math.ceil(isolados.length / porLinha) * (H + GY) - GY + MARGEM;
  }
  const nColunas = isolados.length ? Math.max(colunas.length, porLinha) : colunas.length;
  // com ciclo, sobra espaço à direita para as arestas que voltam na mesma coluna
  const largura = MARGEM * 2 + nColunas * W + Math.max(0, nColunas - 1) * GX + (a.possuiCiclo ? 90 : 0);
  return { colunas, titulos, pos, largura, altura, faixa };
}

function caminhoAresta(p1, p2, propria) {
  const y1 = p1.y + H / 2;
  const y2 = p2.y + H / 2;
  if (propria) {
    const x = p1.x + W;
    return `M${x},${p1.y + 14} C${x + 46},${p1.y - 8} ${x + 46},${p1.y + H + 8} ${x + 3},${p1.y + H - 14}`;
  }
  if (p2.col > p1.col) {
    const x1 = p1.x + W;
    const x2 = p2.x - 3;
    const dx = Math.max(28, (x2 - x1) / 2);
    return `M${x1},${y1} C${x1 + dx},${y1} ${x2 - dx},${y2} ${x2},${y2}`;
  }
  // mesma coluna (só acontece com ciclo)
  if (p2.col === p1.col && p2.y - p1.y === H + GY) {
    // vizinho logo abaixo: seta reta para baixo
    const x = p1.x + W / 2;
    return `M${x},${p1.y + H} L${x},${p2.y - 3}`;
  }
  // demais: contorna pela direita, mais aberto quanto mais longe
  const x1 = p1.x + W;
  const x2 = p2.x + W + 3;
  const curva = 34 + Math.min(56, Math.abs(y2 - y1) / 3);
  return `M${x1},${y1} C${x1 + curva},${y1} ${x2 + curva},${y2} ${x2},${y2}`;
}

function encurtar(texto, max) {
  return texto.length > max ? `${texto.slice(0, max - 1)}…` : texto;
}

function desenharGrafo(st) {
  const a = estado.analise;
  const svg = $('#grafo');
  svg.innerHTML = '';
  const L = calcularLayout(a);
  svg.setAttribute('width', L.largura);
  svg.setAttribute('height', L.altura);
  svg.setAttribute('viewBox', `0 0 ${L.largura} ${L.altura}`);
  // encolhe para caber, mas no máximo até 70% do tamanho; daí em diante a área rola
  svg.style.minWidth = `${Math.round(L.largura * 0.7)}px`;

  const defs = el('defs', {}, svg);
  [['seta', '#a3a097'], ['seta-ciclo', '#c0261c'], ['seta-realce', '#0f5c55']].forEach(([id, cor]) => {
    const m = el('marker', { id, viewBox: '0 0 10 10', refX: 9, refY: 5, markerWidth: 7, markerHeight: 7, orient: 'auto-start-reverse' }, defs);
    el('path', { d: 'M0,0 L10,5 L0,10 z', fill: cor }, m);
  });

  L.titulos.forEach((t, c) => {
    const tx = el('text', { x: MARGEM + c * (W + GX) + W / 2, y: 22, 'text-anchor': 'middle', class: 'cabeca-coluna' }, svg);
    tx.textContent = t;
  });
  if (L.faixa) {
    el('line', { x1: MARGEM, x2: L.largura - MARGEM, y1: L.faixa.y - 16, y2: L.faixa.y - 16, class: 'divisoria' }, svg);
    const tx = el('text', { x: MARGEM, y: L.faixa.y, class: 'cabeca-coluna' }, svg);
    tx.textContent = L.faixa.titulo;
  }

  const ciclo = a.dfs.ciclo;
  const arestasCiclo = new Set();
  for (let i = 0; i + 1 < ciclo.length; i++) arestasCiclo.add(`${ciclo[i]}|${ciclo[i + 1]}`);
  const noCiclo = new Set(ciclo);
  const pendentes = new Set(a.kahn.pendentes);

  const gArestas = el('g', {}, svg);
  a.arestas.forEach(({ de, para }) => {
    const ehCiclo = arestasCiclo.has(`${de}|${para}`);
    const p = el('path', {
      d: caminhoAresta(L.pos.get(de), L.pos.get(para), de === para),
      class: `aresta${ehCiclo ? ' ciclo' : ''}`,
      'marker-end': `url(#${ehCiclo ? 'seta-ciclo' : 'seta'})`,
      'data-de': de, 'data-para': para,
    }, gArestas);
    p.dataset.ciclo = ehCiclo ? '1' : '';
  });

  const gNos = el('g', {}, svg);
  a.disciplinas.forEach((d) => {
    const v = d.codigo;
    const p = L.pos.get(v);
    const classes = ['no'];
    if (st.classe.has(v)) classes.push(st.classe.get(v));
    if (st.atual === v) classes.push('atual');
    if (noCiclo.has(v)) classes.push('ciclo');
    else if (pendentes.has(v) && (estado.alg === 'dfs' || st.cicloVisto)) classes.push('pendente');
    const g = el('g', { class: classes.join(' '), transform: `translate(${p.x},${p.y})`, 'data-v': v, tabindex: 0 }, gNos);
    const titulo = el('title', {}, g);
    titulo.textContent = `${v} – ${d.nome}\n${d.preRequisitos.length ? `Pré-requisitos: ${d.preRequisitos.join(', ')}` : 'Sem pré-requisitos'}\nClique para editar.`;
    el('rect', { class: 'caixa', width: W, height: H, rx: 8 }, g);
    const c = el('text', { class: 'codigo', x: 11, y: 20 }, g);
    c.textContent = encurtar(v, 18);
    const n = el('text', { class: 'nome', x: 11, y: 37 }, g);
    n.textContent = encurtar(d.nome, 25);
    if (st.numero.has(v)) {
      const s = el('g', { class: 'selo', transform: `translate(${W - 4},4)` }, g);
      el('circle', { r: 10 }, s);
      const t = el('text', {}, s);
      t.textContent = st.numero.get(v);
    }
  });
  aplicarFoco();
}

function aplicarFoco() {
  const f = estado.foco;
  const vizinhos = new Set(f ? [f] : []);
  document.querySelectorAll('#grafo .aresta').forEach((p) => {
    const toca = f && (p.dataset.de === f || p.dataset.para === f);
    if (toca) { vizinhos.add(p.dataset.de); vizinhos.add(p.dataset.para); }
    p.classList.toggle('realce', !!toca && !p.dataset.ciclo);
    p.classList.toggle('apagada', !!f && !toca);
    if (!p.dataset.ciclo) p.setAttribute('marker-end', `url(#${toca ? 'seta-realce' : 'seta'})`);
  });
  document.querySelectorAll('#grafo .no').forEach((g) => g.classList.toggle('apagado', !!f && !vizinhos.has(g.dataset.v)));
}

// ---------------------------------------------------------------------- abas

function renderizarAba() {
  document.querySelectorAll('.abas button').forEach((b) => b.classList.toggle('ativa', b.dataset.aba === estado.aba));
  const a = estado.analise;
  const alvo = $('#aba');
  const fn = { comparacao: abaComparacao, periodos: abaPeriodos, passos: abaPassos, ordenacoes: abaOrdenacoes,
    adjacencia: abaAdjacencia, complexidade: abaComplexidade }[estado.aba];
  alvo.innerHTML = fn(a);
  if (estado.aba === 'passos') {
    const atual = alvo.querySelector('li.atual');
    if (atual) atual.scrollIntoView({ block: 'nearest' });
  }
}

function abaComparacao(a) {
  const noCiclo = new Set(a.dfs.ciclo);
  const bloco = (r, titulo, sub) => {
    let corpo;
    let selo;
    if (!r.possuiCiclo) {
      selo = r.valida ? '<span class="selo-txt ok">ordenação válida</span>' : '<span class="selo-txt erro">inválida</span>';
      corpo = sequencia(r.ordem);
    } else if (r.algoritmo === 'Kahn') {
      selo = '<span class="selo-txt erro">ciclo detectado</span>';
      corpo = `<p>Conseguiu ordenar: ${sequencia(r.ordem)}</p>
               <p>A fila esvaziou e estas ficaram com grau de entrada positivo:</p>
               ${sequencia(r.pendentes, '', (v) => (noCiclo.has(v) ? 'ciclo' : ''))}`;
    } else {
      selo = '<span class="selo-txt erro">ciclo detectado</span>';
      corpo = `<p>Encontrou uma aresta para um vértice ainda em processamento. Ciclo:</p>${sequencia(r.ciclo, '', () => 'ciclo')}`;
    }
    return `<div class="alg"><div class="alg-topo"><h3>${titulo}<small>${sub}</small></h3>${selo}</div>${corpo}</div>`;
  };
  let conclusao;
  if (a.possuiCiclo) {
    conclusao = `<p class="conclusao erro">Não existe ordenação topológica: há dependência circular. O Kahn percebe porque a fila
      esvazia antes de ordenar todas as disciplinas; o DFS, ao achar uma aresta de retorno.</p>`;
  } else if (a.mesmaSequencia) {
    conclusao = '<p class="conclusao">Os dois algoritmos chegaram à mesma sequência.</p>';
  } else {
    const t = a.ordenacoes.total;
    conclusao = `<p class="conclusao">As sequências são diferentes e as duas estão corretas: o grafo tem
      ${t === null ? 'muitas' : formatarCurto(t)} ordenações topológicas, e cada algoritmo encontra uma.
      O Kahn segue a ordem de chegada na fila; o DFS, a ordem inversa em que as buscas terminam.</p>`;
  }
  return `<div class="comparacao">
    ${bloco(a.kahn, 'Kahn', 'fila FIFO de vértices com grau de entrada 0')}
    ${bloco(a.dfs, 'DFS', 'busca em profundidade com três estados')}
    ${conclusao}</div>`;
}

function abaPeriodos(a) {
  if (a.possuiCiclo) {
    return '<p class="conclusao erro">Com ciclo não dá para montar os períodos: as disciplinas do ciclo nunca ficam liberadas.</p>';
  }
  let html = `<p>Kahn executado nível a nível: cada disciplina cai no primeiro período em que todos os pré-requisitos
    já foram cursados. O mínimo é <b>${a.periodos.length} período(s)</b>, o número de disciplinas do caminho mais longo do grafo.</p>
    <div class="periodos">${a.periodos.map((per, i) => `
      <div class="periodo"><h3>${i + 1}º período</h3><ul>${per.map((v) =>
        `<li><span class="cod">${esc(v)}</span>${esc(nomeDe(v))}</li>`).join('')}</ul></div>`).join('')}
    </div>`;
  if (!a.temPeriodoInformado) {
    html += '<p class="dica-bloco">Nenhuma disciplina tem período cadastrado. Informe os períodos para conferir se a grade respeita os pré-requisitos.</p>';
    return html;
  }
  const cadastrados = Math.max(...a.disciplinas.map((d) => d.periodo));
  if (cadastrados > a.periodos.length) {
    html += `<p>A grade cadastrada usa <b>${cadastrados} períodos</b>; pelos pré-requisitos, bastariam
      <b>${a.periodos.length}</b>. O que estende a grade além disso não são os pré-requisitos, e sim a forma
      como as disciplinas foram distribuídas pelos semestres.</p>`;
  }
  if (!a.inconsistencias.length) {
    html += '<p><span class="selo-txt ok">ok</span> Os períodos cadastrados respeitam todos os pré-requisitos.</p>';
  } else {
    html += `<h3>Períodos cadastrados com problema</h3><ul class="inconsistencias">${a.inconsistencias.map((t) => `<li>${esc(t)}</li>`).join('')}</ul>`;
  }
  return html;
}

function abaPassos(a) {
  const r = a[estado.alg];
  return `<p>Registro gerado pelo algoritmo <b>${r.algoritmo}</b> em Java (${r.passos.length} passos).
    Clique em um passo para vê-lo no grafo; use os botões Kahn/DFS acima do grafo para trocar de algoritmo.</p>
    <ol class="lista-passos">${r.passos.map((p, i) => `<li data-passo="${i}" class="${i === estado.passo ? 'atual' : ''}
      ${/ciclo|Ciclo/.test(p) && r.possuiCiclo ? 'ciclo' : ''}">${esc(p)}</li>`).join('')}</ol>`;
}

function abaOrdenacoes(a) {
  if (a.possuiCiclo) return '<p class="conclusao erro">Nenhuma: com ciclo não existe ordenação topológica.</p>';
  const o = a.ordenacoes;
  const qtd = estado.maisOrdenacoes ? o.exemplos.length : Math.min(10, o.exemplos.length);
  const igual = (x, y) => x.length === y.length && x.every((v, i) => v === y[i]);
  let total;
  if (o.total === null) {
    total = '<p>Este grafo é grande demais para a contagem exata.</p>';
  } else {
    total = `<p>Total: <b class="numero-longo">${formatarGrande(o.total)}</b> ordenação(ões) topológica(s)${
      o.total.length > 15 ? ` (${formatarCurto(o.total)}, ${o.total.length} dígitos)` : ''}.</p>
      <p class="dica-bloco">Contadas sem listar: o grafo tem ${o.componentes} componente(s) independente(s); cada um é contado
      por programação dinâmica sobre os conjuntos de disciplinas já cursadas, e os resultados se combinam pelo
      coeficiente multinomial, porque disciplinas de componentes diferentes podem se intercalar livremente.</p>`;
  }
  return `${total}<p>Abaixo, as primeiras encontradas por backtracking: em cada passo, qualquer disciplina com grau de
      entrada 0 pode ser a próxima.</p>
    <ol class="lista-ordenacoes">${o.exemplos.slice(0, qtd).map((ord, i) => `<li><span class="num">${i + 1}</span>
      <div>${sequencia(ord, 'pequena')}${igual(ord, a.kahn.ordem) ? ' <span class="selo-txt neutro">= Kahn</span>' : ''}${
        igual(ord, a.dfs.ordem) ? ' <span class="selo-txt neutro">= DFS</span>' : ''}</div></li>`).join('')}</ol>
    ${o.exemplos.length > 10 ? `<button type="button" class="botao secundario" id="b-mais">${estado.maisOrdenacoes
      ? 'Mostrar só 10' : `Mostrar ${o.exemplos.length}`}</button>` : ''}`;
}

function abaAdjacencia(a) {
  return `<p>Representação usada pelos algoritmos: para cada disciplina, a lista das que dependem dela (sucessores).
    O grau de entrada é o número de pré-requisitos diretos.</p>
    <div class="tabela-rolagem"><table class="tabela"><thead><tr><th>Disciplina</th><th>Lista de adjacência (sucessores)</th>
    <th>Grau de entrada</th><th>Pré-requisitos</th></tr></thead><tbody>${a.disciplinas.map((d) => `<tr>
      <td><span class="cod">${esc(d.codigo)}</span> ${esc(d.nome)}</td>
      <td>${a.adjacencia[d.codigo].length ? `[ ${a.adjacencia[d.codigo].map((v) => `<span class="cod">${esc(v)}</span>`).join(', ')} ]` : '[ ]'}</td>
      <td class="num">${a.grausEntrada[d.codigo]}</td>
      <td>${d.preRequisitos.length ? d.preRequisitos.map(esc).join(', ') : '—'}</td></tr>`).join('')}</tbody></table></div>`;
}

function abaComplexidade(a) {
  const v = a.v;
  const e = a.e;
  return `<div class="numeros">
      <div><b>${v}</b><span>vértices (V)</span></div>
      <div><b>${e}</b><span>arestas (E)</span></div>
      <div><b>${v + e}</b><span>V + E: lista de adjacência</span></div>
      <div><b>${v * v}</b><span>V²: matriz de adjacência</span></div>
    </div>
    <p><b>Kahn e DFS são O(V + E)</b> com listas de adjacência: cada vértice é processado uma vez e cada aresta é
      examinada uma vez. Neste grafo isso dá cerca de ${v + e} operações básicas.</p>
    <p>Com <b>matriz de adjacência</b>, descobrir os sucessores de um vértice custa O(V), porque é preciso varrer a linha
      inteira. Os dois algoritmos passariam a <b>O(V²)</b>: ${v * v} aqui.</p>
    <p><b>Listar todas as ordenações</b> é exponencial no pior caso: um grafo sem arestas com n vértices tem n! ordenações.
      Por isso a página lista no máximo 50 e conta o total sem listar: separa o grafo em componentes independentes
      e, em cada um, percorre só os conjuntos de disciplinas que respeitam os pré-requisitos.</p>`;
}

// -------------------------------------------------------------------- eventos

function ligarEventos() {
  $('#form-disc').addEventListener('submit', (e) => { e.preventDefault(); salvarFormulario(); });
  $('#btn-cancelar').addEventListener('click', limparFormulario);
  $('#form-dep').addEventListener('submit', (e) => { e.preventDefault(); adicionarDependencia(); });

  $('#lista-disc').addEventListener('click', (e) => {
    const b = e.target.closest('button');
    if (!b) return;
    if (b.dataset.editar) editar(b.dataset.editar);
    if (b.dataset.remover) remover(b.dataset.remover);
  });

  $('#sel-cenario').addEventListener('change', (e) => {
    if (e.target.value !== '') carregarCenario(Number(e.target.value));
  });
  $('#btn-limpar').addEventListener('click', () => {
    estado.disciplinas = [];
    $('#sel-cenario').value = '';
    limparFormulario();
    analisar();
  });

  document.querySelectorAll('#alt-alg button').forEach((b) => b.addEventListener('click', () => {
    pararAnimacao();
    estado.alg = b.dataset.alg;
    estado.passo = passos().length - 1;
    renderizarAnimacao();
  }));
  document.querySelectorAll('#alt-colunas button').forEach((b) => b.addEventListener('click', () => {
    estado.colunas = b.dataset.colunas;
    renderizarAnimacao();
  }));
  $('#b-inicio').addEventListener('click', () => irPara(0));
  $('#b-anterior').addEventListener('click', () => irPara(estado.passo - 1));
  $('#b-proximo').addEventListener('click', () => irPara(estado.passo + 1));
  $('#b-fim').addEventListener('click', () => irPara(passos().length - 1));
  $('#b-tocar').addEventListener('click', alternarReproducao);

  const svg = $('#grafo');
  svg.addEventListener('mouseover', (e) => {
    const g = e.target.closest('.no');
    const v = g ? g.dataset.v : null;
    if (v !== estado.foco) { estado.foco = v; aplicarFoco(); }
  });
  svg.addEventListener('mouseleave', () => { estado.foco = null; aplicarFoco(); });
  svg.addEventListener('click', (e) => {
    const g = e.target.closest('.no');
    if (g) editar(g.dataset.v);
  });

  document.querySelectorAll('.abas button').forEach((b) => b.addEventListener('click', () => {
    estado.aba = b.dataset.aba;
    renderizarAba();
  }));
  $('#aba').addEventListener('click', (e) => {
    const li = e.target.closest('li[data-passo]');
    if (li) irPara(Number(li.dataset.passo));
    if (e.target.id === 'b-mais') {
      estado.maisOrdenacoes = !estado.maisOrdenacoes;
      renderizarAba();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (!estado.analise || e.target.closest('input, select, textarea')) return;
    if (e.key === 'ArrowRight') { e.preventDefault(); irPara(Math.min(estado.passo + 1, passos().length - 1)); }
    if (e.key === 'ArrowLeft') { e.preventDefault(); irPara(Math.max(estado.passo - 1, 0)); }
  });
}

async function iniciar() {
  ligarEventos();
  try {
    const r = await api('/api/cenarios');
    estado.cenarios = r.cenarios;
    $('#sel-cenario').insertAdjacentHTML('beforeend',
      r.cenarios.map((c, i) => `<option value="${i}">${esc(c.titulo)}</option>`).join(''));
  } catch (e) {
    mostrarAviso(e.message);
  }
  const salvo = lerLocal();
  if (salvo) {
    estado.disciplinas = clonar(salvo);
    // se o que estava salvo é um dos cenários, mostra o nome dele no seletor
    const igual = estado.cenarios.findIndex((c) => JSON.stringify(clonar(c.disciplinas)) === JSON.stringify(estado.disciplinas));
    if (igual >= 0) $('#sel-cenario').value = String(igual);
  } else if (estado.cenarios.length) {
    estado.disciplinas = clonar(estado.cenarios[0].disciplinas);
    $('#sel-cenario').value = '0';
  }
  analisar();
}

iniciar();
