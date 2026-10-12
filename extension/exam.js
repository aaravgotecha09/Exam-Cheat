/* Exam page that lives INSIDE the extension. The backend decides every score, mark and risk level;
   this file only displays them and reports browser-observable events. */
(function () {
  'use strict';
  const { createMapper, makeEventId } = window.EcdEventMapper;
  const $ = (id) => document.getElementById(id);
  const cls = (r) => (r === 'HIGH RISK' ? 'HIGH' : r);
  const EVENT_NAMES = { TAB_SWITCH: 'Tab switch', COPY_PASTE: 'Copy/paste attempt', MULTIPLE_LOGIN: 'Multiple login', WINDOW_EXIT: 'Window exit', MULTIPLE_PERSON: 'Multiple person' };

  let API = ECD_DEFAULT_API;
  const store = (k, v) => { try { if (v === undefined) return sessionStorage.getItem(k); if (v === null) sessionStorage.removeItem(k); else sessionStorage.setItem(k, v); } catch (e) { /* blocked */ } return null; };
  // per-tab id: survives a refresh, differs between tabs (used by the backend multiple-login rule)
  let clientId = store('ecd.clientId');
  if (!clientId) { clientId = 'tab-' + Math.random().toString(36).slice(2, 12) + Date.now().toString(36); store('ecd.clientId', clientId); }

  const S = { student: null, paperId: null, sessionId: null, token: null, state: null, question: null, current: 1, live: false, backendOk: true, beat: null };
  const queue = [];                 // events that could not be sent yet (resent with the SAME id, so never double-scored)
  const mapper = createMapper();
  let listenersOn = false;

  // ---------------- API ----------------
  async function api(path, opts) {
    opts = opts || {};
    const headers = { 'Content-Type': 'application/json' };
    if (S.token) headers['X-Session-Token'] = S.token;
    let res;
    try {
      res = await fetch(API + path, { method: opts.method || 'GET', headers, body: opts.body ? JSON.stringify(opts.body) : undefined, signal: AbortSignal.timeout(opts.timeout || 60000) });
    } catch (e) {
      setBackend(false);
      const err = new Error('Cannot reach the backend at ' + API + '. If it is on Render free plan it may be waking up: wait about a minute and try again, or check the URL in Settings.');
      err.transient = true; throw err;
    }
    if (opts.blob) { if (!res.ok) throw new Error('Download failed (' + res.status + ')'); setBackend(true); return res.blob(); }
    let data = null; try { data = await res.json(); } catch (e) { /* empty */ }
    if (res.status >= 500 || res.status === 429) { setBackend(res.status !== 502 && res.status !== 503 && res.status !== 504); }
    else setBackend(true);
    if (!res.ok) { const err = new Error((data && data.message) || 'Request failed (' + res.status + ')'); err.status = res.status; err.transient = res.status >= 500 || res.status === 429; throw err; }
    return data;
  }
  function setBackend(ok) { S.backendOk = ok; const p = $('backendPill'); p.textContent = ok ? 'Backend connected' : 'Backend offline'; p.className = 'pill ' + (ok ? 'ok' : 'bad'); if (S.live) renderMon(); }

  // ---------------- screens ----------------
  const show = (name) => ['register', 'papers', 'exam', 'result'].forEach((n) => $('screen-' + n).classList.toggle('hidden', n !== name));
  function toast(msg) { const t = $('toast'); t.textContent = msg; t.classList.add('show'); setTimeout(() => t.classList.remove('show'), 2200); }

  // ---------------- registration ----------------
  $('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault(); $('registerErr').textContent = '';
    const id = $('studentId').value.trim(), name = $('studentName').value.trim();
    if (!/^\d+$/.test(id)) return ($('registerErr').textContent = 'Please enter a valid number for Student ID.');
    if (!name) return ($('registerErr').textContent = 'Student name is required.');
    $('registerBtn').disabled = true; $('registerErr').textContent = '';
    try {
      S.student = await api('/api/students/register', { method: 'POST', body: { studentId: Number(id), name } });
      await loadPapers(); show('papers');
    } catch (err) { $('registerErr').textContent = err.message; } finally { $('registerBtn').disabled = false; }
  });

  async function loadPapers() {
    const box = $('paperCards'); box.replaceChildren();
    const papers = await api('/api/papers');
    papers.forEach((p) => {
      const b = document.createElement('button'); b.type = 'button'; b.className = 'paper'; b.dataset.id = p.id;
      const t = document.createElement('b'); t.textContent = p.id + '. ' + p.name;
      const d = document.createElement('span'); d.className = 'muted'; d.textContent = p.questionCount + ' questions · ' + p.totalMarks + ' marks';
      b.append(t, d);
      b.addEventListener('click', () => { S.paperId = p.id; document.querySelectorAll('.paper').forEach((x) => x.classList.toggle('sel', x === b)); $('startBtn').disabled = false; });
      box.appendChild(b);
    });
    S.paperId = null; $('startBtn').disabled = true;
  }
  $('backBtn').addEventListener('click', () => show('register'));

  // ---------------- start / resume ----------------
  $('startBtn').addEventListener('click', async () => {
    $('startErr').textContent = '';
    try { await beginExam(S.student.studentId, S.paperId); } catch (err) { $('startErr').textContent = err.message; }
  });

  async function beginExam(studentId, paperId) {
    S.token = null;
    const r = await api('/api/exams/start', { method: 'POST', body: { studentId, paperId, clientId } });
    S.sessionId = r.sessionId; S.token = r.sessionToken; S.state = r.state; S.current = 1;
    store('ecd.exam', JSON.stringify({ studentId, paperId }));
    if (r.multipleLoginRecorded) toast('Multiple login detected for this Student ID');
    if (r.resumed) toast('Exam resumed');
    S.live = true; attachListeners(); publishToPopup(); startTimers();
    show('exam'); renderMon(); await loadQuestion(firstUnanswered()); renderSide();
  }
  const firstUnanswered = () => { for (let i = 1; i <= S.state.totalQuestions; i++) if (!S.state.answers[i]) return i; return S.state.totalQuestions; };

  // ---------------- monitoring: REAL browser signals, reported to the backend ----------------
  function attachListeners() {
    if (listenersOn) return; listenersOn = true;                       // never register twice
    document.addEventListener('visibilitychange', () => handle(mapper.visibility(document.hidden, Date.now())));
    window.addEventListener('blur', () => setTimeout(() => handle(mapper.blur(document.hidden, Date.now())), 300));
    window.addEventListener('focus', () => mapper.focus());
    ['copy', 'cut', 'paste'].forEach((kind) => document.addEventListener(kind, (ev) => {
      if (!S.live) return;
      if (S.state && S.state.policy && S.state.policy.blockClipboard) ev.preventDefault();
      handle(mapper.clipboard(kind, Date.now()));
    }, true));
    document.addEventListener('contextmenu', (ev) => { if (S.live && S.state && S.state.policy && S.state.policy.blockContextMenu) ev.preventDefault(); }, true);
  }
  const handle = (list) => list.forEach((e) => report(e.type, e.details, 'BROWSER_EXTENSION'));

  async function report(type, details, source) {
    if (!S.live) return;
    const ev = { eventId: makeEventId(type), type, timestamp: new Date().toISOString(), source, details };
    await send(ev);
  }
  async function send(ev) {
    try {
      await api('/api/exams/' + S.sessionId + '/events', { method: 'POST', body: ev, timeout: 20000 });
      await refreshState();
    } catch (e) {
      if (e.transient) { queue.push(ev); renderMon(); }                 // keep and retry later with the same id
      else if (e.status === 409) { S.live = false; renderMon(); }       // exam already finished
      else console.warn('event rejected:', e.message);
    }
  }
  async function flushQueue() {
    if (!queue.length || !S.live) return;
    const batch = queue.splice(0, queue.length);
    for (const ev of batch) await send(ev);
    renderMon();
  }
  function renderMon() {
    const box = $('monStatus'), help = $('monHelp');
    if (!S.live) { box.className = 'mon unknown'; box.textContent = 'Not monitoring'; help.textContent = ''; return; }
    if (S.backendOk && queue.length === 0) { box.className = 'mon on'; box.textContent = 'Monitoring active'; help.textContent = 'This tab is reporting tab switches, focus loss and copy/paste to the backend. Signals are indicators only.'; }
    else { box.className = 'mon err'; box.textContent = 'Backend unreachable'; help.textContent = queue.length + ' event(s) queued and will be retried automatically. Do not close this tab.'; }
  }

  // popup bridge: the popup reads this to show session info
  function publishToPopup() {
    try { chrome.storage.local.set({ activeExam: { sessionId: S.sessionId, token: S.token, apiBase: API, beat: Date.now() } }); } catch (e) { /* ignore */ }
  }
  function startTimers() {
    if (S.beat) return;
    S.beat = setInterval(() => {
      if (S.live) { try { chrome.storage.local.get('activeExam').then(({ activeExam }) => { if (activeExam && activeExam.sessionId === S.sessionId) { activeExam.beat = Date.now(); chrome.storage.local.set({ activeExam }); } }); } catch (e) { /* ignore */ } }
      flushQueue();
      if (S.live) refreshState().catch(() => {});
    }, 5000);
  }

  // ---------------- questions ----------------
  async function loadQuestion(n) {
    $('examErr').textContent = ''; S.current = n;
    S.question = await api('/api/exams/' + S.sessionId + '/questions/' + n);
    renderQuestion();
  }
  let chosen = null;
  function renderQuestion() {
    const q = S.question, st = S.state, answered = st.answers[q.number];
    chosen = answered || null;
    $('examTitle').textContent = st.paperName + ' · ' + st.totalMarks + ' marks';
    $('progress').style.width = (st.attempted / st.totalQuestions * 100) + '%';
    $('qText').textContent = 'Q' + q.number + '. ' + q.text;
    $('qCount').textContent = 'Question ' + q.number + ' of ' + q.totalQuestions + ' · ' + q.marks + ' marks · ' + st.attempted + ' answered';
    const nav = $('qNav'); nav.replaceChildren();
    for (let i = 1; i <= st.totalQuestions; i++) {
      const b = document.createElement('button'); b.type = 'button'; b.textContent = i;
      b.className = (st.answers[i] ? 'answered ' : '') + (i === q.number ? 'current' : '');
      b.setAttribute('aria-label', 'Question ' + i + (st.answers[i] ? ', answered' : ', not answered'));
      b.addEventListener('click', () => loadQuestion(i).catch((e) => ($('examErr').textContent = e.message)));
      nav.appendChild(b);
    }
    const box = $('options'); box.replaceChildren();
    q.options.forEach((o) => {
      const d = document.createElement('div'); d.className = 'opt' + (answered ? ' locked' : ''); d.tabIndex = 0; d.setAttribute('role', 'radio'); d.dataset.label = o.label;
      const l = document.createElement('b'); l.textContent = o.label; const t = document.createElement('span'); t.textContent = o.text; d.append(l, t);
      const select = () => { if (answered) return; chosen = o.label; paintOptions(); $('saveBtn').disabled = false; };
      d.addEventListener('click', select); d.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); select(); } });
      box.appendChild(d);
    });
    paintOptions();
    $('saveBtn').disabled = !!answered || !chosen; $('saveBtn').textContent = answered ? 'Answer saved' : 'Save answer';
    $('prevBtn').disabled = q.number === 1; $('nextBtn').disabled = q.number === q.totalQuestions;
  }
  function paintOptions() { document.querySelectorAll('.opt').forEach((d) => { const on = d.dataset.label === chosen; d.classList.toggle('sel', on); d.setAttribute('aria-checked', on); }); }

  $('saveBtn').addEventListener('click', async () => {
    if (!chosen) return; $('saveBtn').disabled = true;
    try {
      await api('/api/exams/' + S.sessionId + '/answers', { method: 'POST', body: { questionNumber: S.current, selectedOption: chosen } });
      await refreshState();
      if (S.current < S.state.totalQuestions) await loadQuestion(S.current + 1); else renderQuestion();
    } catch (err) { $('examErr').textContent = err.message; $('saveBtn').disabled = false; }
  });
  $('prevBtn').addEventListener('click', () => loadQuestion(S.current - 1).catch((e) => ($('examErr').textContent = e.message)));
  $('nextBtn').addEventListener('click', () => loadQuestion(S.current + 1).catch((e) => ($('examErr').textContent = e.message)));

  // ---------------- submit ----------------
  $('submitBtn').addEventListener('click', () => {
    const left = S.state.totalQuestions - S.state.attempted;
    $('confirmText').textContent = (left > 0 ? 'You have ' + left + ' unanswered question(s). ' : 'All questions are answered. ') + 'You cannot change anything after submitting.';
    $('confirmDlg').showModal();
  });
  $('confirmDlg').addEventListener('close', async () => {
    if ($('confirmDlg').returnValue !== 'ok') return;
    try { await finish(); } catch (err) { $('examErr').textContent = err.message; }
  });

  let result = null;
  async function finish() {
    await flushQueue();                                   // deliver anything still queued before closing the exam
    result = await api('/api/exams/' + S.sessionId + '/finish', { method: 'POST' });
    S.live = false; store('ecd.exam', null);
    try { chrome.storage.local.remove('activeExam'); } catch (e) { /* ignore */ }
    $('rMarks').textContent = result.marksObtained + ' / ' + result.totalMarks;
    $('rScore').textContent = result.cheatScore;
    $('rRisk').textContent = result.risk; $('rRisk').className = 'risk ' + cls(result.risk);
    const rows = $('reviewRows'); rows.replaceChildren();
    result.review.forEach((r) => {
      const tr = document.createElement('tr');
      const txt = (l) => (l ? l + '. ' + r.options['ABCD'.indexOf(l)] : '–');
      [r.number, r.question, txt(r.selectedOption), txt(r.correctOption)].forEach((v) => { const td = document.createElement('td'); td.textContent = v; tr.appendChild(td); });
      const m = document.createElement('td'); m.textContent = r.correct ? '+' + r.marksAwarded : '0'; m.className = r.correct ? 'good' : 'wrong'; tr.appendChild(m);
      rows.appendChild(tr);
    });
    const rep = await api('/api/exams/' + S.sessionId + '/report');
    $('reportText').textContent = rep.report; $('reportFile').textContent = 'Report file name: ' + rep.fileName;
    show('result');
  }
  $('downloadBtn').addEventListener('click', async () => {
    try {
      const blob = await api('/api/exams/' + S.sessionId + '/report/download', { blob: true });
      const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = result.reportFile; a.click(); URL.revokeObjectURL(a.href);
    } catch (err) { $('resultErr').textContent = err.message; }
  });
  $('newBtn').addEventListener('click', () => { S.token = null; S.sessionId = null; show('register'); });

  // ---------------- sidebar ----------------
  async function refreshState() { S.state = await api('/api/exams/' + S.sessionId); renderSide(); return S.state; }
  function renderSide() {
    const st = S.state; if (!st) return;
    $('score').textContent = st.cheatScore; $('risk').textContent = st.risk; $('risk').className = 'risk ' + cls(st.risk);
    const log = $('eventLog'); log.replaceChildren();
    if (!st.events.length) { const li = document.createElement('li'); li.className = 'muted'; li.textContent = 'No events yet.'; log.appendChild(li); }
    st.events.slice().reverse().forEach((e) => {
      const li = document.createElement('li'); const a = document.createElement('span');
      a.textContent = new Date(e.timestamp).toLocaleTimeString() + '  ' + (EVENT_NAMES[e.type] || e.name);
      const tagText = e.source === 'SIMULATED_DEMO' ? 'simulated' : e.source === 'SERVER_RULE' ? 'server' : '';
      if (tagText) { const t = document.createElement('span'); t.className = 'tag'; t.textContent = tagText; a.appendChild(t); }
      const b = document.createElement('b'); b.textContent = '+' + e.points; li.append(a, b); log.appendChild(li);
    });
    $('progress').style.width = (st.attempted / st.totalQuestions * 100) + '%';
  }

  // ---------------- simulated multiple-person (clearly a demo action) ----------------
  $('simBtn').addEventListener('click', async () => {
    const id = makeEventId('MULTIPLE_PERSON');
    try {
      await api('/api/exams/' + S.sessionId + '/events', { method: 'POST', body: { eventId: id, type: 'MULTIPLE_PERSON', timestamp: new Date().toISOString(), source: 'SIMULATED_DEMO', details: 'Simulated from exam page' } });
      await refreshState(); toast('Simulated multiple-person event recorded');
    } catch (err) { $('examErr').textContent = err.message; }
  });

  // ---------------- boot ----------------
  (async function boot() {
    try { const { apiBase } = await chrome.storage.sync.get('apiBase'); if (apiBase) API = apiBase.replace(/\/$/, ''); } catch (e) { /* default */ }
    $('apiShown').textContent = 'Backend: ' + API;
    $('backendPill').textContent = 'Connecting… (free servers can take ~1 min to wake up)';
    try { await api('/api/health', { timeout: 90000 }); } catch (e) { $('registerErr').textContent = e.message; }
    let saved = null; try { saved = JSON.parse(store('ecd.exam') || 'null'); } catch (e) { saved = null; }
    if (saved) { try { await beginExam(saved.studentId, saved.paperId); } catch (e) { store('ecd.exam', null); } }   // refresh = resume
  })();
})();
