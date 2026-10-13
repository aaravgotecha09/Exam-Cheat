const $ = (id) => document.getElementById(id);
const NAMES = { TAB_SWITCH: 'Tab switch', COPY_PASTE: 'Copy/paste', MULTIPLE_LOGIN: 'Multiple login', WINDOW_EXIT: 'Window exit', MULTIPLE_PERSON: 'Multiple person (simulated)' };
const cls = (r) => (r === 'HIGH RISK' ? 'HIGH' : r);
let current = null, apiBase = ECD_DEFAULT_API;

function li(text, right, muted) {
  const el = document.createElement('li'); if (muted) el.className = 'muted';
  const a = document.createElement('span'); a.textContent = text; el.appendChild(a);
  if (right !== undefined) { const b = document.createElement('b'); b.textContent = right; el.appendChild(b); }
  return el;
}
function setConn(ok, text) { const p = $('conn'); p.textContent = text; p.className = 'pill ' + (ok ? 'ok' : 'bad'); }

async function render() {
  try { const { apiBase: a } = await chrome.storage.sync.get('apiBase'); apiBase = (a || ECD_DEFAULT_API).replace(/\/$/, ''); } catch (e) { /* default */ }
  const { activeExam } = await chrome.storage.local.get('activeExam');
  current = activeExam || null;
  $('err').textContent = '';
  try {
    if (!current) { await fetch(apiBase + '/api/health', { signal: AbortSignal.timeout(8000) }); setConn(true, 'Backend connected'); show(false); return; }
    const res = await fetch(apiBase + '/api/exams/' + current.sessionId, { headers: { 'X-Session-Token': current.token }, signal: AbortSignal.timeout(8000) });
    if (res.status === 403 || res.status === 404) { await chrome.storage.local.remove('activeExam'); current = null; setConn(true, 'Backend connected'); show(false); return; }
    if (!res.ok) throw new Error('status ' + res.status);
    setConn(true, 'Backend connected');
    const s = await res.json(); show(true, s);
  } catch (e) {
    setConn(false, 'Backend offline');
    $('err').textContent = 'Cannot reach ' + apiBase + '. If it is on Render free plan, it may be waking up (about 1 minute). Check Settings.';
    show(false);
  }
}

function show(hasExam, s) {
  $('empty').classList.toggle('hidden', hasExam); $('main').classList.toggle('hidden', !hasExam);
  if (!hasExam) return;
  $('student').textContent = s.studentId + ' · ' + s.studentName;
  $('paper').textContent = s.paperName;
  $('session').textContent = s.sessionId;
  const alive = current && Date.now() - (current.beat || 0) < 15000;
  $('mon').textContent = s.status !== 'IN_PROGRESS' ? 'Finished' : alive ? 'Active (exam tab open)' : 'Paused: exam tab not open';
  $('score').textContent = s.cheatScore; $('count').textContent = s.eventCount;
  $('risk').textContent = s.risk; $('risk').className = 'risk ' + cls(s.risk);
  const bd = $('breakdown'); bd.replaceChildren();
  Object.entries(s.eventBreakdown).forEach(([k, v]) => bd.appendChild(li(NAMES[k] || k, v)));
  const rc = $('recent'); rc.replaceChildren();
  if (!s.events.length) rc.appendChild(li('No events yet.', undefined, true));
  s.events.slice(-5).reverse().forEach((e) => rc.appendChild(li(e.name, new Date(e.timestamp).toLocaleTimeString())));
  $('sim').disabled = s.status !== 'IN_PROGRESS';
}

$('sim').addEventListener('click', async () => {
  if (!current) return;
  await fetch(apiBase + '/api/exams/' + current.sessionId + '/events', {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Session-Token': current.token },
    body: JSON.stringify({ eventId: EcdEventMapper.makeEventId('MULTIPLE_PERSON'), type: 'MULTIPLE_PERSON', timestamp: new Date().toISOString(), source: 'SIMULATED_DEMO', details: 'Simulated from extension popup' }),
  });
  render();
});
$('open').addEventListener('click', () => chrome.tabs.create({ url: chrome.runtime.getURL('exam.html') }));
render(); setInterval(render, 3000);
