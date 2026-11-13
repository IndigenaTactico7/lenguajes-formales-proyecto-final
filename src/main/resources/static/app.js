const $ = function (id) { return document.getElementById(id); };

async function api(path, body) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const data = await response.json().catch(function () { return {}; });
  if (!response.ok) throw new Error(translateError(data.error || ('Error ' + response.status)));
  return data;
}

function translateError(message) {
  if (message.indexOf('number of nonterminals') >= 0) return 'La primera línea debe ser el número de no terminales.';
  if (message.indexOf('end of input') >= 0) return 'Faltan líneas: la primera línea dice cuántos no terminales siguen.';
  if (message.indexOf("Missing '->'") >= 0) return 'Cada línea debe tener la forma A -> alt1 alt2.';
  if (message.indexOf('Invalid nonterminal') >= 0) return 'La cabeza de cada producción debe ser una letra mayúscula.';
  if (message.indexOf('The grammar is empty') >= 0) return 'Escriba una gramática.';
  if (message.indexOf('The alphabet is empty') >= 0) return 'El alfabeto está vacío.';
  if (message.indexOf('cannot contain the empty string') >= 0) return 'El alfabeto no puede contener ε: ε solo se usa en las transiciones.';
  const initial = message.match(/The initial state (.*) is not part of the states/);
  if (initial) return 'El estado inicial ' + initial[1] + ' no está en la lista de estados.';
  if (message === 'Failed to fetch') return 'No hay conexión con el programa. ¿Está corriendo java -jar target/lenguajes-formales.jar --web?';
  return message;
}

function escapeHtml(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function htmlTable(rows) {
  let html = '<table>';
  rows.forEach(function (row, i) {
    const tag = i === 0 ? 'th' : 'td';
    html += '<tr>' + row.map(function (c) { return '<' + tag + '>' + escapeHtml(c) + '</' + tag + '>'; }).join('') + '</tr>';
  });
  return html + '</table>';
}

function showTab(name) {
  ['grammar', 'sdd', 'automata'].forEach(function (tab) {
    $('page-' + tab).classList.toggle('hidden', name !== tab);
    $('tab-' + tab).classList.toggle('active', name === tab);
  });
}
$('tab-grammar').onclick = function () { showTab('grammar'); };
$('tab-sdd').onclick = function () { showTab('sdd'); };
$('tab-automata').onclick = function () { showTab('automata'); };

const EXAMPLES = {
  ex1: '3\nS -> S+T T\nT -> T*F F\nF -> (S) i',
  ex2: '3\nS -> AB\nA -> aA d\nB -> bBc e',
  ex3: '1\nS -> SS a'
};
let grammarText = null;

async function analyze() {
  $('grammar-error').textContent = '';
  $('grammar-result').classList.add('hidden');
  grammarText = null;
  const text = $('grammar').value;
  let a;
  try {
    a = await api('/api/analyze', { grammar: text });
  } catch (e) {
    $('grammar-error').textContent = e.message;
    return;
  }
  grammarText = text;

  if (a.ll1 && a.slr1) $('verdict').textContent = 'La gramática es LL(1) y SLR(1).';
  else if (a.ll1) $('verdict').textContent = 'Grammar is LL(1).';
  else if (a.slr1) $('verdict').textContent = 'Grammar is SLR(1).';
  else $('verdict').textContent = 'Grammar is neither LL(1) nor SLR(1).';

  const lines = [];
  a.nonterminals.forEach(function (n) { lines.push('First(' + n + ') = ' + a.first[n]); });
  a.nonterminals.forEach(function (n) { lines.push('Follow(' + n + ') = ' + a.follow[n]); });
  $('sets').textContent = lines.join('\n');

  const columns = a.terminals.concat(['$']);
  const llRows = [[''].concat(columns)];
  a.nonterminals.forEach(function (n) {
    const row = a.llTable[n] || {};
    llRows.push([n].concat(columns.map(function (c) { return row[c] || ''; })));
  });
  $('ll1-table').innerHTML = htmlTable(llRows);

  const lr0 = [];
  a.states.forEach(function (state) {
    lr0.push('I' + state.id + ':');
    state.items.forEach(function (item) { lr0.push('    ' + item); });
    Object.keys(state.transitions).forEach(function (x) {
      lr0.push('    goto(' + x + ') = I' + state.transitions[x]);
    });
  });
  $('lr0').textContent = lr0.join('\n');

  const slrRows = [['state'].concat(columns, a.nonterminals)];
  a.action.forEach(function (row, i) {
    slrRows.push([String(i)].concat(
      columns.map(function (c) { return row[c] || ''; }),
      a.nonterminals.map(function (n) { return a.gotoTable[i][n] !== undefined ? String(a.gotoTable[i][n]) : ''; })));
  });
  $('slr1-table').innerHTML = htmlTable(slrRows);

  $('parser').options[0].disabled = !a.ll1;
  $('parser').options[1].disabled = !a.slr1;
  $('parser').value = a.ll1 ? 'll1' : 'slr1';
  $('parse').disabled = !a.ll1 && !a.slr1;
  $('answer').textContent = '';
  $('trace').innerHTML = '';
  $('grammar-result').classList.remove('hidden');
}

async function parseString() {
  if (grammarText === null) return;
  const w = $('input').value.trim();
  if (w === '') return;
  let result;
  try {
    result = await api('/api/parse', {
      grammar: grammarText,
      parser: $('parser').value === 'll1' ? 'LL1' : 'SLR1',
      input: w
    });
  } catch (e) {
    $('answer').textContent = e.message;
    $('answer').className = 'answer no';
    $('trace').innerHTML = '';
    return;
  }
  $('answer').textContent = result.accepted ? 'yes' : 'no';
  $('answer').className = 'answer ' + (result.accepted ? 'yes' : 'no');
  $('trace').innerHTML = htmlTable([result.header].concat(result.rows));
}

$('analyze').onclick = analyze;
$('parse').onclick = parseString;
['ex1', 'ex2', 'ex3'].forEach(function (id) {
  $(id).onclick = function () { $('grammar').value = EXAMPLES[id]; analyze(); };
});

let evaluation = null;
let shownSteps = 0;

async function evaluate() {
  $('sdd-result').classList.add('hidden');
  $('sdd-message').innerHTML = '';
  const w = $('sdd-input').value.trim();
  if (w === '') return;
  let r;
  try {
    r = await api('/api/sdd', { input: w, circular: $('circular').checked });
  } catch (e) {
    $('sdd-message').innerHTML = '<p class="error">' + escapeHtml(e.message) + '</p>';
    return;
  }
  if (r.circular) {
    $('sdd-message').innerHTML = '<p class="error">The SDD is circular: the dependency graph has a cycle.</p>';
    return;
  }
  if (!r.accepted) {
    $('sdd-message').innerHTML = '<p class="answer no">no</p>';
    return;
  }
  evaluation = r;
  shownSteps = r.order.filter(function (s) { return s.attribute === 'lexval'; }).length;
  showSteps();
  $('sdd-result').classList.remove('hidden');
}

function treeLines(tree, shown) {
  const lines = [];
  (function walk(node, depth) {
    let line = '  '.repeat(depth) + '[' + node.id + '] ' + node.symbol;
    const names = Object.keys(node.attributes).sort().filter(function (n) { return shown[node.id + '.' + n]; });
    if (names.length > 0) {
      line += '  ' + names.map(function (n) { return n + '=' + node.attributes[n]; }).join(', ');
    }
    lines.push(line);
    node.children.forEach(function (c) { walk(c, depth + 1); });
  })(tree, 0);
  return lines;
}

function showSteps() {
  const r = evaluation;
  const shown = {};
  for (let i = 0; i < shownSteps; i++) shown[r.order[i].node + '.' + r.order[i].attribute] = true;
  const last = shownSteps > 0 ? r.order[shownSteps - 1] : null;

  const lines = treeLines(r.tree, shown);
  const ids = [];
  (function walk(node) { ids.push(node.id); node.children.forEach(walk); })(r.tree);
  $('sdd-tree').innerHTML = lines.map(function (line, i) {
    const changed = last && last.attribute !== 'lexval' && ids[i] === last.node;
    return changed ? '<span class="mark">' + escapeHtml(line) + '</span>' : escapeHtml(line);
  }).join('\n');

  $('sdd-order').innerHTML = r.order.map(function (o, i) {
    const cls = (i === shownSteps - 1 && last.attribute !== 'lexval') ? ' class="mark"' : '';
    const style = i < shownSteps ? '' : ' style="color:#9AA1AE"';
    return '<li' + cls + style + '>' + escapeHtml(o.symbol + '.' + o.attribute + ' (node ' + o.node + ')') + '</li>';
  }).join('');

  $('step-count').textContent = shownSteps + ' de ' + r.order.length + ' atributos';
  $('sdd-value').textContent = shownSteps === r.order.length ? 'S.val = ' + r.value : '';
  $('step').disabled = shownSteps === r.order.length;
  $('all').disabled = shownSteps === r.order.length;
}

$('evaluate').onclick = evaluate;
$('reset').onclick = evaluate;
$('step').onclick = function () { shownSteps++; showSteps(); };
$('all').onclick = function () { shownSteps = evaluation.order.length; showSteps(); };

const NFA_EXAMPLES = {
  nfa1: 'estados: 0 1 2 3 4 5 6 7 8\nalfabeto: a b\ninicial: 0\nfinales: 2 6 8\n'
    + '0 ε 1\n0 ε 3\n0 ε 7\n1 a 2\n3 a 4\n4 b 5\n5 b 6\n7 a 7\n7 b 8\n8 b 8',
  nfa2: 'estados: 0 1 2 3\nalfabeto: a b\ninicial: 0\nfinales: 3\n0 a 0\n0 b 0\n0 a 1\n1 b 2\n2 b 3'
};
let dfaResponse = null;
let minResponse = null;

function readNfa(text) {
  const nfa = { states: null, alphabet: null, initial: null, accepting: [], transitions: [] };
  const lines = text.replace(/\r/g, '').split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line === '') continue;
    const colon = line.indexOf(':');
    if (colon >= 0) {
      const key = line.substring(0, colon).trim();
      const values = line.substring(colon + 1).trim().split(/\s+/).filter(function (t) { return t !== ''; });
      if (key === 'estados') nfa.states = values.map(toInt);
      else if (key === 'alfabeto') nfa.alphabet = values;
      else if (key === 'inicial') nfa.initial = toInt(values[0] || '');
      else if (key === 'finales') nfa.accepting = values.map(toInt);
      else throw new Error('Línea ' + (i + 1) + ': no se reconoce "' + key + '".');
      continue;
    }
    const parts = line.split(/\s+/);
    if (parts.length !== 3) throw new Error('Línea ' + (i + 1) + ': la transición debe ser "origen símbolo destino".');
    const symbol = (parts[1] === 'ε' || parts[1] === 'eps') ? '' : parts[1];
    nfa.transitions.push({ from: toInt(parts[0]), symbol: symbol, to: toInt(parts[2]) });
  }
  if (!nfa.states) throw new Error('Falta la línea "estados:".');
  if (!nfa.alphabet) throw new Error('Falta la línea "alfabeto:".');
  if (nfa.initial === null) throw new Error('Falta la línea "inicial:".');
  return nfa;
}

function toInt(text) {
  if (!/^\d+$/.test(text)) throw new Error('"' + text + '" no es un número de estado.');
  return parseInt(text);
}

function dfaTable(d) {
  const delta = {};
  d.dfaStates.forEach(function (s) { delta[s] = {}; });
  d.transitions.forEach(function (t) { delta[t.from][t.symbol] = t.to; });
  const subsets = {};
  d.stateDetails.forEach(function (s) { if (s.subset.length > 0) subsets[s.name] = s.subset; });
  const withSubsets = Object.keys(subsets).length > 0;

  const rows = [['Estado'].concat(withSubsets ? ['Subconjunto del AFN'] : [], d.alphabet)];
  d.dfaStates.forEach(function (s) {
    const label = (s === d.initialState ? '→ ' : '') + (d.acceptingStates.indexOf(s) >= 0 ? '* ' : '') + s;
    const cells = d.alphabet.map(function (a) { return delta[s][a] !== undefined ? delta[s][a] : '∅'; });
    rows.push([label].concat(withSubsets ? ['{' + subsets[s].join(', ') + '}'] : [], cells));
  });
  return htmlTable(rows);
}

async function convert() {
  $('nfa-error').textContent = '';
  $('automata-result').classList.add('hidden');
  try {
    dfaResponse = await api('/api/automata/convert', readNfa($('nfa').value));
    minResponse = await api('/api/automata/minimize', dfaResponse);
  } catch (e) {
    dfaResponse = null;
    $('nfa-error').textContent = e.message;
    return;
  }
  $('dfa-table').innerHTML = dfaTable(dfaResponse);

  const states = minResponse.markingTable.states;
  const round = minResponse.markingTable.rounds;
  if (states.length > 1) {
    const rows = [[''].concat(states.slice(0, -1))];
    for (let i = 1; i < states.length; i++) {
      const row = [states[i]];
      for (let j = 0; j < states.length - 1; j++) {
        row.push(j >= i ? '' : (round[i][j] === -1 ? '≡' : String(round[i][j])));
      }
      rows.push(row);
    }
    $('mark-table').innerHTML = htmlTable(rows);
  } else {
    $('mark-table').innerHTML = '<p class="sub">El AFD tiene un solo estado.</p>';
  }
  const pairs = minResponse.equivalentStates;
  $('pairs').textContent = pairs.length > 0
    ? 'Pares equivalentes: ' + pairs.map(function (p) { return '(' + p.first + ', ' + p.second + ')'; }).join(' ') + '. AFD minimizado:'
    : 'No hay pares equivalentes: el AFD ya es mínimo.';
  $('min-table').innerHTML = dfaTable(minResponse.minimizedDfa);
  $('automata-result').classList.remove('hidden');
  simulate();
}

function translateSimulation(message) {
  let m = message.match(/No transition from state '(.*)' on symbol '(.*)'/);
  if (m) return 'no hay transición desde ' + m[1] + ' con ' + m[2];
  m = message.match(/state '(.*)' is not accepting/);
  if (m) return 'se leyó toda la cadena y ' + m[1] + ' no es final';
  return message;
}

async function simulate() {
  if (!dfaResponse) return;
  const dfa = $('sim-which').value === 'min' ? minResponse.minimizedDfa : dfaResponse;
  const input = $('sim-input').value.trim();
  let r;
  try {
    r = await api('/api/automata/simulate', { dfa: dfa, input: input });
  } catch (e) {
    $('sim-answer').textContent = '';
    $('sim-path').textContent = e.message;
    return;
  }
  $('sim-answer').textContent = r.accepted ? 'yes' : 'no';
  $('sim-answer').className = 'answer ' + (r.accepted ? 'yes' : 'no');
  let path = r.path[0];
  for (let i = 1; i < r.path.length; i++) path += ' —' + input[i - 1] + '→ ' + r.path[i];
  $('sim-path').textContent = 'Recorrido: ' + path + (r.message ? '\n(' + translateSimulation(r.message) + ')' : '');
}

async function runBatch() {
  $('batch-error').textContent = '';
  $('batch-output').textContent = '';
  let response;
  try {
    response = await fetch('/api/automata/minimize/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: $('batch').value
    });
  } catch (e) {
    $('batch-error').textContent = translateError(e.message);
    return;
  }
  if (!response.ok) {
    const data = await response.json().catch(function () { return {}; });
    $('batch-error').textContent = translateError(data.error || ('Error ' + response.status));
    return;
  }
  const out = await response.text();
  $('batch-output').textContent = out.trim() === '' ? '(sin pares equivalentes)' : out.trim();
}

$('convert').onclick = convert;
$('simulate').onclick = simulate;
$('sim-which').onchange = simulate;
$('batch-run').onclick = runBatch;
['nfa1', 'nfa2'].forEach(function (id) {
  $(id).onclick = function () {
    $('nfa').value = NFA_EXAMPLES[id];
    $('sim-input').value = id === 'nfa1' ? 'abb' : 'aabb';
    convert();
  };
});

$('nfa').value = NFA_EXAMPLES.nfa1;
convert();
runBatch();
analyze();
