const api = document.getElementById('api'), msg = document.getElementById('msg');
const say = (t, ok) => { msg.textContent = t; msg.className = ok ? 'ok' : 'bad'; };
chrome.storage.sync.get('apiBase').then(({ apiBase }) => { api.value = apiBase || ECD_DEFAULT_API; });
const clean = () => api.value.trim().replace(/\/+$/, '');

document.getElementById('save').onclick = async () => {
  let u;
  try { u = new URL(clean()); if (!/^https?:$/.test(u.protocol)) throw 0; } catch (e) { return say('Enter a full URL such as https://your-service.onrender.com', false); }
  // Ask Chrome for permission to talk to this server (needed for addresses other than *.onrender.com / localhost)
  const origin = u.origin + '/*';
  const has = await chrome.permissions.contains({ origins: [origin] });
  if (!has) { const granted = await chrome.permissions.request({ origins: [origin] }); if (!granted) return say('Permission was not granted, so the URL was not saved.', false); }
  await chrome.storage.sync.set({ apiBase: u.origin });
  say('Saved: ' + u.origin, true);
};
document.getElementById('test').onclick = async () => {
  say('Testing… (a sleeping Render server can take up to a minute)', true);
  try { const r = await fetch(clean() + '/api/health', { signal: AbortSignal.timeout(90000) }); say(r.ok ? 'Backend reachable.' : 'Backend answered with ' + r.status, r.ok); }
  catch (e) { say('Cannot reach the backend. Check the URL, then try again.', false); }
};
