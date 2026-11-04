// Shared state and helpers for the storefront.
import { createApi, esc, money, art, badge, toast, modal, $ } from '/shared/ui.js';

export const session = {
  token: localStorage.getItem('choco.token') || null,
  profile: null,
  cartCount: 0,
};

export const api = createApi({
  getToken: () => session.token,
  onUnauthorized: () => { if (session.token) { logout(); toast('Your session expired. Please sign in again.', 'error'); location.hash = '#/login'; } },
});

export const listeners = new Set();
const notify = () => listeners.forEach(fn => fn());

export async function setToken(token) {
  session.token = token;
  localStorage.setItem('choco.token', token);
  await refreshSession();
}
export function logout() {
  session.token = null; session.profile = null; session.cartCount = 0;
  localStorage.removeItem('choco.token');
  notify();
}
export async function refreshSession() {
  if (!session.token) { notify(); return; }
  try {
    session.profile = await api.get('/sso/info', null, { quiet401: true });
    await refreshCart(false);
  } catch { logout(); }
  notify();
}
export async function refreshCart(announce = true) {
  if (!session.token) { session.cartCount = 0; if (announce) notify(); return null; }
  const cart = await api.get('/cart/list');
  session.cartCount = cart.itemCount;
  if (announce) notify();
  return cart;
}

/** Sends the visitor to the sign-in page and back again afterwards. */
export function requireLogin() {
  if (session.token) return true;
  location.hash = '#/login?next=' + encodeURIComponent(location.hash.slice(1) || '/');
  return false;
}

// ---- reference lists (cached for the visit)
const cache = {};
export const cached = (key, loader) => (cache[key] ??= loader());

// ---- rendering helpers
export function productArt(p) {
  return p.pic ? `<img src="${esc(p.pic)}" alt="${esc(p.name)}" loading="lazy">` : art({ name: p.name, categoryName: p.categoryName, kind: p.kind });
}

export function cardBadges(p) {
  const b = [];
  if (!p.inStock) b.push('<span class="badge danger">Sold out</span>');
  else if (p.lowStock) b.push('<span class="badge warn">Few left</span>');
  if (p.preorderOnly) b.push('<span class="badge ruby">Preorder</span>');
  if (p.requiresColdChain) b.push('<span class="badge info">Chilled · pickup</span>');
  if (p.kind === 'EXPERIENCE') b.push('<span class="badge info">Experience</span>');
  if (p.kind === 'PERSONALIZED') b.push('<span class="badge ruby">Personalised</span>');
  return b.join('');
}

export function productCard(p) {
  const claims = (p.claims || []).slice(0, 2).map(c => `<span class="badge ok" title="${esc(c.evidence || '')}">${esc(c.type.replace(/_/g, ' ').toLowerCase())}</span>`).join(' ');
  const from = p.skuCount > 1 ? '<span class="muted small">from </span>' : '';
  return `<a class="card product-card" href="#/product/${p.id}">
    <div class="art">${productArt(p)}<div class="corner">${cardBadges(p)}</div></div>
    <div class="body">
      <div class="small muted">${esc(p.categoryName || '')}</div>
      <div class="name">${esc(p.name)}</div>
      <div class="small muted">${esc(p.subTitle || '')}</div>
      <div class="chips">${claims}</div>
      <div class="foot"><span class="price">${from}${money(p.price)}</span>
        ${p.allergens?.length ? `<span class="small muted" title="Contains: ${esc(p.allergens.join(', '))}">⚠ ${p.allergens.length} allergen${p.allergens.length > 1 ? 's' : ''}</span>` : ''}</div>
    </div></a>`;
}

export const statusBadge = badge;
export function stars(n) { const k = Math.round(n || 0); return '<span class="stars" aria-label="' + k + ' of 5 stars">' + '★'.repeat(k) + '☆'.repeat(5 - k) + '</span>'; }
export const nowLocalInput = (minutesAhead = 0) => {
  const d = new Date(Date.now() + minutesAhead * 60000 - new Date().getTimezoneOffset() * 60000);
  return d.toISOString().slice(0, 16);
};
export const emptyState = (icon, title, text, action = '') => `<div class="empty"><div class="big">${icon}</div><h3>${esc(title)}</h3><p>${esc(text)}</p>${action}</div>`;

/** Asks for a card token. The mock gateway accepts any token except "tok_decline", which simulates a declined card. */
export function askCard(title, amountText) {
  return new Promise(resolve => {
    modal({
      title,
      body: `<p>Amount to pay: <b>${esc(amountText)}</b></p>
        <div class="field"><label>Card token</label><input id="card-token" value="tok_ok" autocomplete="off"><div class="hint">Demo payment: any value works. Type <code>tok_decline</code> to see a declined card.</div></div>`,
      onClose: () => resolve(null),
      actions: [{ label: 'Cancel', kind: 'ghost', run: () => resolve(null) }, { label: 'Pay now', kind: 'primary', run: ({ el }) => resolve($('#card-token', el).value.trim() || 'tok_ok') }],
    });
  });
}
