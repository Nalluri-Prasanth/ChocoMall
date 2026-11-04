import { createRouter, esc, $, toast, fail, readForm, labelFields } from '/shared/ui.js';
import { session, api, events, restore, setToken, clearSession } from '/app/core.js';
import * as R from '/app/pages/resources.js';
import { productsPage, productDetail } from '/app/pages/products.js';
import { ordersPage, orderDetail, returnsPage, personalizationPage, settingsPage, members, payments } from '/app/pages/orders.js';
import { dashboard, reportsPage, stockPage, productionPage, playbookPage } from '/app/pages/ops.js';
import { staff, rolesPage } from '/app/pages/access.js';

const shell = $('#shell'), authBox = $('#auth'), side = $('#side'), top = $('#top'), view = $('#view');

const NAV = [
  ['Overview', [['/', 'Dashboard'], ['/reports', 'Reports'], ['/playbook', 'Launch playbook']]],
  ['Sales', [['/orders', 'Orders'], ['/returns', 'Returns'], ['/personalization', 'Personalised orders'], ['/payments', 'Payments']]],
  ['Catalog', [['/products', 'Products'], ['/categories', 'Categories'], ['/brands', 'Brands'], ['/flavors', 'Flavours'], ['/chocolate-types', 'Chocolate types'], ['/allergens', 'Allergens']]],
  ['Kitchen & stock', [['/stock', 'Stock'], ['/production', 'Production batches']]],
  ['Promotions', [['/coupons', 'Coupons'], ['/bundles', 'Bundles'], ['/preorders', 'Preorder windows'], ['/sessions', 'Tastings & workshops'], ['/home-slots', 'Home page']]],
  ['Customers', [['/members', 'Members'], ['/levels', 'Member levels']]],
  ['Content', [['/stories', 'Stories'], ['/pairings', 'Food pairings'], ['/serving', 'Ways to serve'], ['/help-topics', 'Help topics'], ['/help-articles', 'Help articles']]],
  ['Shop setup', [['/settings', 'Settings'], ['/locations', 'Locations'], ['/return-reasons', 'Return reasons']]],
  ['Access', [['/staff', 'Staff'], ['/roles', 'Roles']]],
];

function renderChrome() {
  const here = location.hash.slice(1).split('?')[0] || '/';
  side.innerHTML = `<a class="brand" href="#/">🍫 Choco<span>Mall</span> <span class="small muted" style="font-weight:400">back office</span></a>`
    + NAV.map(([title, items]) => `<h4>${title}</h4>${items.map(([href, label]) => `<a class="item ${here === href || (href !== '/' && here.startsWith(href + '/')) || (href !== '/' && here === href) ? 'active' : ''}" href="#${href}">${label}</a>`).join('')}`).join('')
    + `<h4>Elsewhere</h4><a class="item" href="http://localhost:8085/" target="_blank" rel="noopener">Open the shop ↗</a><a class="item" href="/swagger-ui.html" target="_blank" rel="noopener">API documentation ↗</a>`;
  top.innerHTML = `<button class="btn ghost small menu-btn" id="menu" aria-label="Menu" aria-expanded="false">☰</button><div class="workspace-jump"><label class="sr-only" for="workspace-jump">Jump to a workspace</label><select id="workspace-jump"><option value="">Jump to a workspace…</option>${NAV.map(([title, items]) => `<optgroup label="${esc(title)}">${items.map(([href, label]) => `<option value="${href}">${esc(label)}</option>`).join('')}</optgroup>`).join('')}</select></div>
    <span class="row"><span>👤 ${esc(session.info?.nickName || session.info?.username || '')}</span><span class="small muted">${esc((session.info?.roles || []).join(', '))}</span><button class="btn small ghost" id="out">Sign out</button></span>`;
  $('#workspace-jump').addEventListener('change', e => { if (e.target.value) location.hash = '#' + e.target.value; });
  $('#menu').addEventListener('click', e => { const open = side.classList.toggle('open'); e.currentTarget.setAttribute('aria-expanded', String(open)); });
  $('#out').addEventListener('click', async () => { try { await api.post('/admin/logout'); } catch { /* the token is dropped either way */ } clearSession(); location.hash = '#/login'; });
  side.addEventListener('click', e => { if (e.target.closest('a.item')) side.classList.remove('open'); });
}

function showLogin() {
  shell.classList.add('hidden'); authBox.classList.remove('hidden');
  authBox.innerHTML = `<div class="panel stack"><div class="center"><div style="font-size:2.4rem">🍫</div><h1 style="margin:0">ChocoMall back office</h1><p class="muted small">Staff sign-in</p></div>
    <form id="f" class="stack"><div class="field"><label>Username</label><input name="username" required autocomplete="username" autofocus></div>
    <div class="field"><label>Password</label><input name="password" type="password" required autocomplete="current-password"></div><button class="btn primary block">Sign in</button></form>
    ${['localhost', '127.0.0.1'].includes(location.hostname) ? '<button class="btn ghost small block" id="demo" type="button">Fill the local demo login</button>' : ''}</div>`;
  labelFields(authBox);
  $('#f', authBox).addEventListener('submit', async e => {
    e.preventDefault();
    try { const t = await api.post('/admin/login', readForm(e.target), { quiet401: true }); await setToken(t.token); toast('Signed in', 'ok'); } catch (err) { fail(err); }
  });
  $('#demo', authBox)?.addEventListener('click', () => { $('input[name=username]', authBox).value = 'admin'; $('input[name=password]', authBox).value = 'Admin@12345'; $('#f', authBox).requestSubmit(); });
}

const router = createRouter({
  root: view,
  routes: [
    ['/', dashboard], ['/reports', reportsPage], ['/playbook', playbookPage],
    ['/orders', ordersPage], ['/orders/:id', orderDetail], ['/returns', returnsPage], ['/personalization', personalizationPage], ['/payments', payments],
    ['/products', productsPage], ['/products/:id', productDetail], ['/categories', R.categories], ['/brands', R.brands], ['/flavors', R.flavors], ['/chocolate-types', R.chocolateTypes], ['/allergens', R.allergens],
    ['/stock', stockPage], ['/production', productionPage],
    ['/coupons', R.coupons], ['/bundles', R.bundles], ['/preorders', R.preorderWindows], ['/sessions', R.sessions], ['/home-slots', R.homeSlotsWithType],
    ['/members', members], ['/levels', R.memberLevels],
    ['/stories', R.subjects], ['/pairings', R.pairings], ['/serving', R.servings], ['/help-topics', R.helpCategories], ['/help-articles', R.helpArticles],
    ['/settings', settingsPage], ['/locations', R.locations], ['/return-reasons', R.returnReasons],
    ['/staff', staff], ['/roles', rolesPage],
  ],
  notFound: () => '<div class="empty"><h3>Page not found</h3><a class="btn" href="#/">Dashboard</a></div>',
  onBefore: path => {
    if (!session.token && path !== '/login') { location.hash = '#/login'; return false; }
    return true;
  },
  onAfter: () => { renderChrome(); view.focus({ preventScroll: true }); },
});

function enter() {
  authBox.classList.add('hidden'); authBox.innerHTML = ''; shell.classList.remove('hidden');
  if (!location.hash || location.hash === '#/login') location.hash = '#/';
  renderChrome();
  router.render();
}

events.add(() => { if (session.token) enter(); else { showLogin(); if (location.hash !== '#/login') location.hash = '#/login'; } });
window.addEventListener('unhandledrejection', e => { toast(e.reason?.message || 'Something went wrong', 'error'); });
await restore();
if (session.token) { enter(); } else { showLogin(); }
