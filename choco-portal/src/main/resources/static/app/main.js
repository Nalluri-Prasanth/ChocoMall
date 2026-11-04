import { createRouter, esc, $, $$, toast } from '/shared/ui.js';
import { session, api, listeners, refreshSession, logout, cached } from '/app/core.js';
import { home } from '/app/pages/home.js';
import { mountAtelier } from '/app/atelier.js';
import { discover } from '/app/pages/discover.js';
import { shop } from '/app/pages/shop.js';
import { product } from '/app/pages/product.js';
import { cartPage, checkoutPage, payPage } from '/app/pages/checkout.js';
import { ordersPage, orderPage } from '/app/pages/orders.js';
import { accountPage } from '/app/pages/account.js';
import { personalizePage, experiencesPage } from '/app/pages/custom.js';
import { guidePage, subjectPage, helpPage } from '/app/pages/guide.js';
import { loginPage, registerPage, forgotPage } from '/app/pages/auth.js';

const header = $('#top');
const footer = $('#foot');
const view = $('#view');
$('.skip-link')?.addEventListener('click', e => { e.preventDefault(); view.focus(); view.scrollIntoView(); });

function renderHeader() {
  const here = location.hash.slice(1) || '/';
  const link = (href, label) => `<a href="#${href}" class="${here === href || (href !== '/' && here.startsWith(href)) ? 'active' : ''}">${label}</a>`;
  const account = session.token
    ? `<a href="#/account">${esc(session.profile?.nickname || session.profile?.username || 'Account')}</a>`
    : `<a href="#/login">Sign in</a>`;
  header.innerHTML = `<div class="container bar">
    <a class="brand" href="#/" aria-label="ChocoMall home"><span class="brand-mark" aria-hidden="true">CM</span><b>Choco<span>Mall</span></b></a>
    <form class="search" role="search" id="search-form"><input type="search" name="q" placeholder="Search chocolates, drinks, gifts…" aria-label="Search" autocomplete="off"></form>
    <nav class="nav" aria-label="Main">
      ${link('/shop', 'Shop')}${link('/discover', 'Find your favourite')}${link('/personalize', 'Personalise')}${link('/experiences', 'Experiences')}${link('/guide', 'Guide')}${link('/help', 'Help')}
      ${account}
      <a class="cart-link" href="#/cart" aria-label="Basket"><svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 8h14l1 12H4L5 8Zm3 0V6a4 4 0 0 1 8 0v2" fill="none" stroke="currentColor" stroke-width="1.5"/></svg>${session.cartCount ? `<span class="count">${session.cartCount}</span>` : ''}</a>
    </nav></div>`;
  $('#search-form').addEventListener('submit', e => {
    e.preventDefault();
    const q = new FormData(e.target).get('q').trim();
    location.hash = '#/shop' + (q ? '?q=' + encodeURIComponent(q) : '');
  });
}

async function renderFooter() {
  let info = {};
  try { info = await cached('shop', () => api.get('/shop/info')); } catch { /* the footer is optional */ }
  footer.innerHTML = `<div class="container cols">
    <div><h4>ChocoMall</h4><p>Small-batch chocolate, bonbons, drinks, gifts and personalised pieces.</p></div>
    <div><h4>Visit</h4><p>${esc(info.name || 'Main boutique')}<br>${esc(info.address || '')}<br>${esc(info.openingHours || '')}</p></div>
    <div><h4>Ordering</h4><p>Pickup ready in about ${esc(info.pickupLeadMinutes ?? 30)} minutes.<br>Delivery ${info.deliveryEnabled ? `${esc(info.deliveryFee)} (free over ${esc(info.freeDeliveryThreshold)})` : 'currently unavailable'}.<br>${esc(info.returnWindowDays ?? 7)}-day returns.</p></div>
    <div><h4>Help</h4><p><a href="#/help">Help centre</a><br><a href="#/guide">Pairing &amp; serving guide</a><br><a href="/swagger-ui.html">API documentation</a></p></div></div>`;
}

const router = createRouter({
  root: view,
  routes: [
    ['/', home], ['/shop', shop], ['/discover', discover], ['/product/:id', product],
    ['/cart', cartPage], ['/checkout', checkoutPage], ['/pay/:id', payPage],
    ['/orders', ordersPage], ['/orders/:id', orderPage],
    ['/account', accountPage], ['/account/:tab', accountPage],
    ['/personalize', personalizePage], ['/experiences', experiencesPage],
    ['/guide', guidePage], ['/subject/:id', subjectPage], ['/help', helpPage], ['/help/:id', helpPage],
    ['/login', loginPage], ['/register', registerPage], ['/forgot', forgotPage],
  ],
  notFound: () => '<div class="empty"><div class="big">🔎</div><h3>Page not found</h3><a class="btn" href="#/">Home</a></div>',
  onAfter: () => { renderHeader(); mountAtelier(view); view.focus({ preventScroll: true }); },
});

listeners.add(renderHeader);
window.addEventListener('unhandledrejection', e => { toast(e.reason?.message || 'Something went wrong', 'error'); });

renderHeader();
renderFooter();
await refreshSession();
router.start();
