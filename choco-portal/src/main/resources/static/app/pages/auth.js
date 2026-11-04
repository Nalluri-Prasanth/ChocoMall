import { api, setToken } from '/app/core.js';
import { esc, $, toast, fail, readForm } from '/shared/ui.js';

const isLocal = ['localhost', '127.0.0.1'].includes(location.hostname);

export function loginPage({ params }) {
  const el = document.createElement('div');
  el.innerHTML = `<div class="auth-box panel stack"><h1 style="font-size:1.8rem;margin:0">Welcome back</h1>
    <form id="f" class="stack"><div class="field"><label>Username</label><input name="username" required autocomplete="username" autofocus></div>
    <div class="field"><label>Password</label><input name="password" type="password" required autocomplete="current-password"></div>
    <button class="btn primary block">Sign in</button></form>
    <div class="row between small"><a href="#/forgot">Forgot password?</a><a href="#/register">Create an account</a></div>
    ${isLocal ? '<button class="btn ghost small block" id="demo" type="button">Use the demo account</button>' : ''}</div>`;
  $('#f', el).addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const t = await api.post('/sso/login', readForm(e.target), { quiet401: true });
      await setToken(t.token);
      toast('Signed in', 'ok');
      location.hash = '#' + (params.next ? decodeURIComponent(params.next) : '/');
    } catch (err) { fail(err); }
  });
  $('#demo', el)?.addEventListener('click', () => { $('input[name=username]', el).value = 'demo'; $('input[name=password]', el).value = 'Demo@12345'; $('#f', el).requestSubmit(); });
  return el;
}

export function registerPage() {
  const el = document.createElement('div');
  el.innerHTML = `<div class="auth-box panel stack"><h1 style="font-size:1.8rem;margin:0">Create your account</h1>
    <p class="muted small" style="margin:0">We send a verification code to your phone. Earn loyalty points on every order.</p>
    <form id="f" class="stack">
      <div class="field"><label>Mobile number</label><div class="row"><input name="telephone" class="grow" placeholder="+15550001234" required pattern="\\+?[0-9]{8,15}" title="8 to 15 digits, optionally starting with +"><button type="button" class="btn ghost small" id="send">Send code</button></div>
        <div class="hint" id="code-hint"></div></div>
      <div class="field"><label>Verification code</label><input name="authCode" required inputmode="numeric" autocomplete="one-time-code"></div>
      <div class="field"><label>Username</label><input name="username" required minlength="3" maxlength="64" pattern="[A-Za-z0-9._-]+" autocomplete="username"></div>
      <div class="field"><label>Email <span class="muted">(optional)</span></label><input name="email" type="email"></div>
      <div class="field"><label>Password</label><input name="password" type="password" required minlength="8" autocomplete="new-password"><div class="hint">At least 8 characters.</div></div>
      <label class="check"><input type="checkbox" name="marketingConsent"> Send me news and offers (optional)</label>
      <button class="btn primary block">Create account</button></form>
    <p class="small center">Already registered? <a href="#/login">Sign in</a></p></div>`;
  $('#send', el).addEventListener('click', async () => {
    const phone = $('input[name=telephone]', el).value.trim();
    if (!/^\+?[0-9]{8,15}$/.test(phone)) return toast('Enter a valid mobile number first', 'error');
    try {
      const code = await api.get('/sso/getAuthCode', { telephone: phone });
      $('#code-hint', el).textContent = code ? `Development mode: your code is ${code}` : 'Code sent. It expires in 5 minutes.';
      if (code) $('input[name=authCode]', el).value = code;
    } catch (e) { fail(e); }
  });
  $('#f', el).addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const body = readForm(e.target);
      const t = await api.post('/sso/register', body);
      await setToken(t.token);
      toast('Welcome to ChocoMall!', 'ok');
      location.hash = '#/';
    } catch (err) { fail(err); }
  });
  return el;
}

export function forgotPage() {
  const el = document.createElement('div');
  el.innerHTML = `<div class="auth-box panel stack"><h1 style="font-size:1.8rem;margin:0">Reset your password</h1>
    <form id="f" class="stack">
      <div class="field"><label>Mobile number</label><div class="row"><input name="telephone" class="grow" required><button type="button" class="btn ghost small" id="send">Send code</button></div><div class="hint" id="code-hint"></div></div>
      <div class="field"><label>Verification code</label><input name="authCode" required></div>
      <div class="field"><label>New password</label><input name="password" type="password" required minlength="8" autocomplete="new-password"></div>
      <button class="btn primary block">Set new password</button></form></div>`;
  $('#send', el).addEventListener('click', async () => {
    try {
      const code = await api.get('/sso/getAuthCode', { telephone: $('input[name=telephone]', el).value.trim() });
      $('#code-hint', el).textContent = code ? `Development mode: your code is ${code}` : 'Code sent.';
      if (code) $('input[name=authCode]', el).value = code;
    } catch (e) { fail(e); }
  });
  $('#f', el).addEventListener('submit', async e => {
    e.preventDefault();
    try { await api.post('/sso/resetPassword', readForm(e.target)); toast('Password updated. Please sign in.', 'ok'); location.hash = '#/login'; } catch (err) { fail(err); }
  });
  return el;
}
