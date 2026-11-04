// Small UI toolkit shared by the storefront and the staff dashboard (no dependencies, no build step).

// ---------------------------------------------------------------- formatting & safety
export const CURRENCY = '$';

/** Escapes text for safe use inside HTML. Every dynamic value in a template goes through this. */
export function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
export const money = v => (v === null || v === undefined || v === '') ? '' : CURRENCY + Number(v).toFixed(2);
export const dt = v => v ? new Date(v).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '';
export const day = v => v ? new Date(v + (String(v).length <= 10 ? 'T00:00:00' : '')).toLocaleDateString([], { dateStyle: 'medium' }) : '';
export const pretty = s => String(s ?? '').replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase());
export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

export const STATUS_TONE = {
  PENDING_PAYMENT: 'warn', PREPARING: 'info', READY_FOR_PICKUP: 'ok', SHIPPED: 'info', COMPLETED: 'ok', CANCELLED: 'danger', INVALID: 'danger',
  SUCCEEDED: 'ok', FAILED: 'danger', REFUNDED: 'warn', PENDING: 'warn', CONFIRMED: 'ok', OPEN: 'ok', CLOSED: 'danger', FULL: 'warn',
  UNUSED: 'ok', USED: 'info', EXPIRED: 'danger', AVAILABLE: 'ok', ACCEPTED: 'info', REJECTED: 'danger',
  SUBMITTED: 'warn', QUOTED: 'info', PROOF_SENT: 'info', PROOF_APPROVED: 'ok', DEPOSIT_PAID: 'ok', IN_PRODUCTION: 'info', READY: 'ok',
  PLANNED: 'warn', IN_PROGRESS: 'info', RELEASED: 'ok', DONE: 'ok', TODO: 'warn', NOT_APPLICABLE: '',
  CORE: 'ok', EXPAND: 'info', TEST: 'warn', SPECIALIST: 'ruby',
  CANDIDATE: '', SAMPLING: 'info', AUTHORIZED: 'warn', STOCKED: 'ok', ROTATED_OUT: 'danger',
};
export const badge = (status, label) => `<span class="badge ${STATUS_TONE[status] ?? ''}">${esc(label ?? pretty(status))}</span>`;

// ---------------------------------------------------------------- artwork (used when a product has no photo)
const ART = {
  bonbon: '<svg viewBox="0 0 100 100"><ellipse cx="50" cy="60" rx="34" ry="22" fill="#4d2e20"/><ellipse cx="50" cy="52" rx="34" ry="22" fill="#7d4d36"/><path d="M30 46c8-8 32-8 40 0" stroke="#c98a4b" stroke-width="5" fill="none" stroke-linecap="round"/><circle cx="62" cy="44" r="4" fill="#f0d9bd"/></svg>',
  bar: '<svg viewBox="0 0 100 100"><rect x="18" y="14" width="64" height="72" rx="8" fill="#4d2e20"/><g stroke="#2e1a11" stroke-width="2"><path d="M18 38h64M18 62h64M40 14v72M60 14v72"/></g><rect x="22" y="18" width="14" height="16" rx="3" fill="#7d4d36"/><rect x="64" y="66" width="14" height="16" rx="3" fill="#7d4d36"/></svg>',
  cup: '<svg viewBox="0 0 100 100"><path d="M24 36h46v26c0 14-10 24-23 24S24 76 24 62z" fill="#7d4d36"/><path d="M70 42h8a8 8 0 0 1 0 18h-8" fill="none" stroke="#7d4d36" stroke-width="6"/><path d="M34 28c0-6 6-6 6-12M48 28c0-6 6-6 6-12" stroke="#c98a4b" stroke-width="3" fill="none" stroke-linecap="round"/></svg>',
  cake: '<svg viewBox="0 0 100 100"><rect x="18" y="46" width="64" height="34" rx="6" fill="#4d2e20"/><path d="M18 52c10 10 14-4 24 6s14-8 24 4 12-4 16 0V46H18z" fill="#c98a4b"/><circle cx="50" cy="38" r="7" fill="#b3265e"/></svg>',
  gift: '<svg viewBox="0 0 100 100"><rect x="18" y="40" width="64" height="44" rx="5" fill="#b3265e"/><rect x="14" y="30" width="72" height="16" rx="4" fill="#c2406f"/><rect x="45" y="30" width="10" height="54" fill="#f0d9bd"/><path d="M50 30c-8-16-24-10-16 0M50 30c8-16 24-10 16 0" fill="none" stroke="#f0d9bd" stroke-width="5"/></svg>',
  fruit: '<svg viewBox="0 0 100 100"><path d="M50 84C26 70 22 44 34 34c8-6 12 0 16 0s8-6 16 0c12 10 8 36-16 50z" fill="#b3265e"/><path d="M50 34c0-8 4-14 10-16" stroke="#5d8a4a" stroke-width="5" fill="none" stroke-linecap="round"/><path d="M30 48c6 4 10 8 20 8s14-4 20-8" stroke="#4d2e20" stroke-width="9" fill="none" stroke-linecap="round"/></svg>',
  spark: '<svg viewBox="0 0 100 100"><path d="M50 12l9 26 27 2-21 17 7 27-22-15-22 15 7-27-21-17 27-2z" fill="#c98a4b"/></svg>',
  jar: '<svg viewBox="0 0 100 100"><rect x="26" y="26" width="48" height="8" rx="3" fill="#4d2e20"/><rect x="22" y="34" width="56" height="52" rx="10" fill="#7d4d36"/><rect x="30" y="50" width="40" height="22" rx="4" fill="#f0d9bd"/></svg>',
};
export function art(item) {
  const text = `${item.name ?? ''} ${item.categoryName ?? ''}`.toLowerCase();
  const kind = item.kind;
  let key = 'bar';
  if (kind === 'EXPERIENCE') key = 'spark';
  else if (kind === 'PERSONALIZED') key = 'spark';
  else if (/bonbon|truffle|praline|ganache|caramel|cluster|cup|pretzel|nuts|raisin/.test(text)) key = 'bonbon';
  else if (/drink|hot chocolate|shake|mocha|iced/.test(text)) key = 'cup';
  else if (/brownie|tart|mousse|cookie|croissant|sundae|dessert|bakery|gelato|sorbet|cake|waffle|crepe/.test(text)) key = 'cake';
  else if (/gift|box|collection|advent|seasonal|egg|assortment|set|shelf/.test(text)) key = 'gift';
  else if (/strawberr|fruit|banana|orange|peel/.test(text)) key = 'fruit';
  else if (/spread|sauce/.test(text)) key = 'jar';
  return ART[key];
}

// ---------------------------------------------------------------- API client
export class ApiError extends Error {
  constructor(message, status, code) { super(message); this.status = status; this.code = code; }
}

/** Creates a client for the {code, message, data} envelope. `getToken` supplies the bearer token. */
export function createApi({ getToken, onUnauthorized }) {
  async function request(method, path, body, opts = {}) {
    const headers = {};
    const token = getToken();
    if (token) headers.Authorization = 'Bearer ' + token;
    let payload;
    if (body instanceof FormData) payload = body;
    else if (body !== undefined && body !== null) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
    let res;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), opts.timeoutMs || 30000);
    let text;
    try { res = await fetch(path, { method, headers, body: payload, signal: controller.signal }); text = await res.text(); }
    catch { throw new ApiError(controller.signal.aborted ? 'The request took too long. Please try again.' : 'The server cannot be reached. Is it running?', 0, 0); }
    finally { clearTimeout(timeout); }
    let json = {};
    try { json = text ? JSON.parse(text) : {}; } catch { json = { message: text }; }
    if (res.status === 401 && !opts.quiet401) { onUnauthorized?.(); }
    if (!res.ok || (json.code && json.code !== 200)) throw new ApiError(json.message || ('Request failed (' + res.status + ')'), res.status, json.code);
    return json.data;
  }
  const qs = params => {
    const p = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') (Array.isArray(v) ? v : [v]).forEach(x => p.append(k, x)); });
    const s = p.toString();
    return s ? '?' + s : '';
  };
  return {
    get: (path, params, opts) => request('GET', path + qs(params), undefined, opts),
    post: (path, body, opts) => request('POST', path, body ?? {}, opts),
    postQ: (path, params, body) => request('POST', path + qs(params), body ?? {}),
    upload: (path, file) => { const f = new FormData(); f.append('file', file); return request('POST', path, f); },
    qs,
  };
}

// ---------------------------------------------------------------- toasts & dialogs
function toastRoot() {
  let el = $('.toasts');
  if (!el) { el = document.createElement('div'); el.className = 'toasts'; el.setAttribute('role', 'status'); document.body.appendChild(el); }
  return el;
}
export function toast(message, type = '') {
  const t = document.createElement('div');
  t.className = 'toast ' + type;
  t.textContent = message;
  toastRoot().appendChild(t);
  setTimeout(() => t.remove(), type === 'error' ? 6500 : 3500);
}
export const fail = e => toast(e?.message || String(e), 'error');

/** Opens a modal. `body` is an HTML string or element. Returns {close, el}. */
export function modal({ title, body, actions = [], wide = false, onClose }) {
  const overlay = document.createElement('div');
  overlay.className = 'overlay';
  overlay.innerHTML = `<div class="modal ${wide ? 'wide' : ''}" role="dialog" aria-modal="true" aria-label="${esc(title)}">
      <header><h3>${esc(title)}</h3><button class="icon-btn" data-close aria-label="Close">✕</button></header>
      <div class="content"></div>
      <footer></footer></div>`;
  const content = $('.content', overlay);
  if (typeof body === 'string') content.innerHTML = body; else content.appendChild(body);
  const footer = $('footer', overlay);
  const previousFocus = document.activeElement;
  const close = () => { overlay.remove(); document.removeEventListener('keydown', onKey); previousFocus?.focus(); onClose?.(); };
  function onKey(e) {
    if (e.key === 'Escape') { e.preventDefault(); close(); }
    if (e.key !== 'Tab') return;
    const controls = [...overlay.querySelectorAll('button,input,select,textarea,a[href],[tabindex="0"]')].filter(el => !el.disabled && el.getClientRects().length);
    const first = controls[0], last = controls[controls.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last?.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first?.focus(); }
  }
  document.addEventListener('keydown', onKey);
  actions.forEach(a => {
    const b = document.createElement('button');
    b.className = 'btn ' + (a.kind || '');
    b.textContent = a.label;
    b.addEventListener('click', async () => {
      b.disabled = true;
      try { const keepOpen = await a.run?.({ close, el: overlay }); if (!keepOpen && a.close !== false) close(); }
      catch (e) { fail(e); }
      finally { b.disabled = false; }
    });
    footer.appendChild(b);
  });
  if (!actions.length) footer.remove();
  overlay.addEventListener('click', e => { if (e.target === overlay || e.target.closest('[data-close]')) close(); });
  document.body.appendChild(overlay);
  labelFields(content);
  const first = $('input, select, textarea', content);
  first?.focus();
  return { close, el: overlay };
}
export const confirmDialog = (title, message, label = 'Confirm', kind = 'danger') => new Promise(resolve => {
  modal({
    title, body: `<p>${esc(message)}</p>`, onClose: () => resolve(false),
    actions: [{ label: 'Cancel', kind: 'ghost', run: () => resolve(false) }, { label, kind, run: () => { resolve(true); } }],
  });
});

// ---------------------------------------------------------------- forms
/** Reads a form into an object. data-type="number|int|bool|list|json|ids" controls conversion; empty strings become undefined. */
export function readForm(form) {
  const out = {};
  $$('[name]', form).forEach(el => {
    const name = el.name, type = el.dataset.type;
    if (el.type === 'radio' && !el.checked) return;
    if (el.type === 'checkbox' && !el.closest('.chips') && !name.endsWith('[]')) { out[name] = el.checked; return; }
    if (el.type === 'checkbox') { const key = name.replace('[]', ''); (out[key] ??= []); if (el.checked) out[key].push(type === 'int' ? Number(el.value) : el.value); return; }
    let v = el.value;
    if (v === '') { return; }
    if (type === 'number') v = Number(v);
    else if (type === 'int') v = parseInt(v, 10);
    else if (type === 'bool') v = v === 'true';
    else if (type === 'json') v = JSON.parse(v);
    out[name] = v;
  });
  return out;
}
export function pager(current, totalPages, onPage) {
  const el = document.createElement('div');
  el.className = 'pager';
  if (totalPages <= 1) return el;
  const add = (label, page, disabled = false, active = false) => {
    const b = document.createElement('button');
    b.className = 'btn small ' + (active ? '' : 'ghost');
    b.textContent = label; b.disabled = disabled;
    b.addEventListener('click', () => onPage(page));
    el.appendChild(b);
  };
  add('‹', current - 1, current <= 1);
  const start = Math.max(1, current - 2), end = Math.min(totalPages, current + 2);
  if (start > 1) add('1', 1);
  if (start > 2) el.appendChild(Object.assign(document.createElement('span'), { textContent: '…' }));
  for (let p = start; p <= end; p++) add(String(p), p, false, p === current);
  if (end < totalPages - 1) el.appendChild(Object.assign(document.createElement('span'), { textContent: '…' }));
  if (end < totalPages) add(String(totalPages), totalPages);
  add('›', current + 1, current >= totalPages);
  return el;
}

// ---------------------------------------------------------------- tiny hash router
export function createRouter({ routes, root, notFound, onBefore, onAfter }) {
  const compiled = routes.map(([pattern, handler]) => {
    const keys = [];
    const re = new RegExp('^' + pattern.replace(/:([a-zA-Z]+)/g, (_, k) => { keys.push(k); return '([^/]+)'; }) + '/?$');
    return { re, keys, handler };
  });
  let token = 0;
  async function render() {
    const mine = ++token;
    const raw = location.hash.slice(1) || '/';
    const [path, query = ''] = raw.split('?');
    const params = Object.fromEntries(new URLSearchParams(query));
    for (const r of compiled) {
      const m = path.match(r.re);
      if (!m) continue;
      const args = {}; r.keys.forEach((k, i) => args[k] = decodeURIComponent(m[i + 1]));
      if (onBefore && (await onBefore(path, args, params)) === false) return;
      root.innerHTML = '<div class="spinner" aria-label="Loading"></div>';
      try {
        const result = await r.handler({ args, params, path, root });
        if (mine !== token) return;
        if (typeof result === 'string') root.innerHTML = result;
        else if (result instanceof Node) { root.innerHTML = ''; root.appendChild(result); }
      } catch (e) {
        if (mine !== token) return;
        root.innerHTML = `<div class="empty"><div class="big">🍫</div><h3>Something went wrong</h3><p>${esc(e.message)}</p><a class="btn" href="#/">Back to start</a></div>`;
      }
      window.scrollTo({ top: 0 });
      labelFields(root);
      onAfter?.(path);
      return;
    }
    root.innerHTML = notFound ? notFound() : '<div class="empty"><div class="big">🔎</div><h3>Page not found</h3></div>';
  }
  window.addEventListener('hashchange', render);
  return { start: render, render, go: (hash) => { location.hash = hash; } };
}

/** Builds a query string for hash routes. */
export const hashQ = params => { const s = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')).toString(); return s ? '?' + s : ''; };

/** Associate conventional field labels with their controls for keyboard and screen-reader use. */
export function labelFields(root) {
  root.querySelectorAll('.field').forEach((field, index) => {
    const label = field.querySelector('label'), control = field.querySelector('input,select,textarea');
    if (!label || !control || label.contains(control) || label.htmlFor) return;
    if (!control.id) control.id = `field-${Date.now()}-${index}`;
    label.htmlFor = control.id;
  });
}
