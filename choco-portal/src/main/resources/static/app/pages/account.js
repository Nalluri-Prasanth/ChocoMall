import { api, session, requireLogin, refreshSession, logout, cached, productCard, emptyState, statusBadge } from '/app/core.js';
import { addressModal } from '/app/pages/checkout.js';
import { esc, money, dt, day, $, $$, toast, fail, confirmDialog, readForm, pager, pretty } from '/shared/ui.js';

const TABS = [['profile', 'Profile'], ['addresses', 'Addresses'], ['coupons', 'Coupons'], ['points', 'Points'], ['favourites', 'Favourites'], ['history', 'Recently viewed'], ['preferences', 'Taste'], ['requests', 'Personalised'], ['bookings', 'Bookings']];

export async function accountPage({ args }) {
  if (!requireLogin()) return '';
  const tab = args.tab || 'profile';
  await refreshSession();
  const el = document.createElement('div');
  el.innerHTML = `<h1>My account</h1><div class="tabs">${TABS.map(([k, l]) => `<a class="tab ${tab === k ? 'active' : ''}" href="#/account/${k}">${l}</a>`).join('')}</div><div id="pane"><div class="spinner"></div></div>`;
  const pane = $('#pane', el);
  const renderers = { profile, addresses, coupons, points, favourites, history, preferences, requests, bookings };
  (renderers[tab] || profile)(pane).catch(e => { pane.innerHTML = `<div class="alert error">${esc(e.message)}</div>`; });
  return el;
}

async function profile(pane) {
  const p = session.profile;
  pane.innerHTML = `<div class="grid cols-2">
    <div class="panel"><h3>${esc(p.nickname || p.username)}</h3>
      <div class="row"><span class="badge ok">${esc(p.levelName || 'Member')}</span><span class="muted small">@${esc(p.username)}</span></div>
      <div class="grid stats" style="margin-top:14px"><div class="panel stat"><div class="num">${p.points}</div><div class="label">Loyalty points</div></div>
        <div class="panel stat"><div class="num">${p.growth}</div><div class="label">Growth</div></div></div>
      ${p.nextLevelName ? `<p class="small muted" style="margin-top:12px">${p.growthToNextLevel} more growth to reach <b>${esc(p.nextLevelName)}</b> (better points and cheaper delivery).</p>` : '<p class="small muted">You are at the top level. 🎉</p>'}
      <button class="btn ghost" id="logout" style="margin-top:8px">Sign out</button></div>
    <form class="panel" id="pf"><h3>Your details</h3><div class="form-grid">
      <div class="field"><label>Nickname</label><input name="nickname" value="${esc(p.nickname || '')}"></div>
      <div class="field"><label>Email</label><input name="email" type="email" value="${esc(p.email || '')}"></div>
      <div class="field"><label>Mobile</label><input value="${esc(p.phone || '')}" disabled></div>
      <div class="field"><label>City</label><input name="city" value="${esc(p.city || '')}"></div>
      <div class="field"><label>Birthday</label><input name="birthday" type="date" value="${esc(p.birthday || '')}"></div></div>
      <label class="check"><input type="checkbox" name="marketingConsent" ${p.marketingConsent ? 'checked' : ''}> Send me news and offers</label>
      <button class="btn primary" style="margin-top:10px">Save changes</button></form></div>`;
  $('#logout', pane).addEventListener('click', () => { logout(); toast('Signed out'); location.hash = '#/'; });
  $('#pf', pane).addEventListener('submit', async e => {
    e.preventDefault();
    try { await api.post('/sso/profile', readForm(e.target)); await refreshSession(); toast('Saved', 'ok'); } catch (err) { fail(err); }
  });
}

async function addresses(pane) {
  const list = await api.get('/member/address/list');
  pane.innerHTML = `<div class="row between" style="margin-bottom:12px"><h3 style="margin:0">Saved addresses</h3><button class="btn primary small" id="add">+ Add address</button></div>
    ${list.length ? `<div class="grid cols-2">${list.map(a => `<div class="panel"><div class="row between"><b>${esc(a.name)}</b>${a.defaultStatus ? '<span class="badge ok">Default</span>' : ''}</div>
      <p class="muted" style="margin:6px 0">${esc(a.detailAddress)}<br>${esc([a.city, a.province, a.postCode].filter(Boolean).join(', '))}<br>${esc(a.phone)}</p>
      <div class="row"><button class="btn small ghost" data-edit="${a.id}">Edit</button><button class="btn small ghost" data-del="${a.id}">Delete</button></div></div>`).join('')}</div>` : emptyState('🏠', 'No addresses yet', 'Add one to use delivery.')}`;
  const again = () => addresses(pane);
  $('#add', pane).addEventListener('click', () => addressModal(again));
  $$('[data-edit]', pane).forEach(b => b.addEventListener('click', () => addressModal(again, list.find(a => String(a.id) === b.dataset.edit))));
  $$('[data-del]', pane).forEach(b => b.addEventListener('click', async () => { if (await confirmDialog('Delete address?', 'This cannot be undone.', 'Delete')) { try { await api.post('/member/address/delete/' + b.dataset.del); again(); } catch (e) { fail(e); } } }));
}

async function coupons(pane) {
  const [mine, open] = await Promise.all([api.get('/member/coupon/list'), api.get('/member/coupon/claimable')]);
  pane.innerHTML = `<h3>Coupons you can claim</h3>${open.length ? `<div class="grid cols-2">${open.map(c => `<div class="panel row between"><div><b>${esc(c.name)}</b><div class="small muted">${money(c.amount)} off · min spend ${money(c.minSpend)} · until ${day(c.endTime)}</div></div><button class="btn small primary" data-claim="${c.couponId}">Claim</button></div>`).join('')}</div>` : '<p class="muted">No coupons to claim right now.</p>'}
    <h3 style="margin-top:24px">My coupons</h3>${mine.length ? `<div class="table-wrap"><table><thead><tr><th>Coupon</th><th>Value</th><th>Min spend</th><th>Valid until</th><th>Status</th></tr></thead><tbody>${mine.map(c => `<tr><td>${esc(c.name)}</td><td>${money(c.amount)}</td><td>${money(c.minSpend)}</td><td>${day(c.endTime)}</td><td>${statusBadge(c.useStatus)}</td></tr>`).join('')}</tbody></table></div>` : '<p class="muted">You have no coupons yet.</p>'}`;
  $$('[data-claim]', pane).forEach(b => b.addEventListener('click', async () => { try { await api.post('/member/coupon/claim/' + b.dataset.claim); toast('Coupon added', 'ok'); coupons(pane); } catch (e) { fail(e); } }));
}

async function points(pane) {
  const [h] = await Promise.all([api.get('/member/points', { pageSize: 30 })]);
  const info = await cached('shop', () => api.get('/shop/info'));
  pane.innerHTML = `<div class="grid stats"><div class="panel stat"><div class="num">${session.profile.points}</div><div class="label">Points balance</div></div>
    <div class="panel stat"><div class="num">${money(session.profile.points / info.pointsPerCurrencyUnit)}</div><div class="label">Worth (up to ${info.maxRedeemPercent}% of an order)</div></div></div>
    <h3 style="margin-top:20px">History</h3>${h.list.length ? `<div class="table-wrap"><table><thead><tr><th>Date</th><th>What</th><th class="right">Points</th><th class="right">Balance</th></tr></thead><tbody>${h.list.map(x => `<tr><td>${dt(x.createTime)}</td><td>${esc(x.note || pretty(x.changeType))}</td><td class="right"><b style="color:${x.changeCount < 0 ? 'var(--danger)' : 'var(--ok)'}">${x.changeCount > 0 ? '+' : ''}${x.changeCount}</b></td><td class="right">${x.balanceAfter}</td></tr>`).join('')}</tbody></table></div>` : '<p class="muted">Points you earn and spend will appear here.</p>'}`;
}

async function favourites(pane) {
  const r = await api.get('/member/productCollection/list', { pageSize: 24 });
  pane.innerHTML = r.list.length ? `<div class="grid cards">${r.list.map(f => `<div class="card pad stack"><a href="#/product/${f.productId}"><b>${esc(f.name)}</b></a><div>${money(f.price)}</div><button class="btn small ghost" data-un="${f.productId}">Remove</button></div>`).join('')}</div>` : emptyState('❤️', 'No favourites yet', 'Tap the heart on a product to save it.', '<a class="btn primary" href="#/shop">Browse</a>');
  $$('[data-un]', pane).forEach(b => b.addEventListener('click', async () => { await api.post('/member/productCollection/delete/' + b.dataset.un); favourites(pane); }));
}

async function history(pane) {
  const r = await api.get('/member/readHistory/list', { pageSize: 24 });
  pane.innerHTML = r.list.length ? `<div class="row between" style="margin-bottom:12px"><h3 style="margin:0">Recently viewed</h3><button class="btn small ghost" id="clear">Clear history</button></div><div class="table-wrap"><table><tbody>${r.list.map(h => `<tr><td><a href="#/product/${h.productId}">${esc(h.name)}</a></td><td>${money(h.price)}</td><td class="muted small">${dt(h.viewedAt)}</td></tr>`).join('')}</tbody></table></div>` : emptyState('👀', 'Nothing viewed yet', 'Products you look at will be listed here.');
  $('#clear', pane)?.addEventListener('click', async () => { await api.post('/member/readHistory/clear'); history(pane); });
}

async function preferences(pane) {
  const [flavors, pref, picks] = await Promise.all([cached('flavors', () => api.get('/product/flavors')), api.get('/member/preference'), api.get('/member/forYou', { limit: 8 })]);
  const chosen = new Set(pref.flavorIds.map(String));
  pane.innerHTML = `<div class="panel"><h3 style="margin-top:0">Tell us what you love</h3>
    <p class="muted small">We use this only to suggest products. You can delete it at any time${pref.consentGivenAt ? `. Saved ${dt(pref.consentGivenAt)}` : ''}.</p>
    <form id="pf"><div class="chips">${flavors.map(f => `<label class="chip"><input type="checkbox" name="flavors[]" value="${f.id}" ${chosen.has(String(f.id)) ? 'checked' : ''}>${esc(f.name)}</label>`).join('')}</div>
      <label class="check" style="margin-top:14px"><input type="checkbox" name="consent"> I agree that ChocoMall stores these favourite flavors to personalise suggestions.</label>
      <div class="row" style="margin-top:12px"><button class="btn primary">Save</button><button type="button" class="btn ghost" id="del">Delete my preferences</button></div></form></div>
    <div class="section-head"><h2>Picked for you</h2></div><div class="grid cards">${picks.map(productCard).join('')}</div>`;
  $('#pf', pane).addEventListener('submit', async e => {
    e.preventDefault();
    const f = readForm(e.target);
    try { await api.post('/member/preference', { flavorIds: (f['flavors'] || []).map(Number), consent: !!f.consent }); toast('Preferences saved', 'ok'); preferences(pane); } catch (err) { fail(err); }
  });
  $('#del', pane).addEventListener('click', async () => { await api.post('/member/preference/delete'); toast('Deleted', 'ok'); preferences(pane); });
}

async function requests(pane) {
  const list = await api.get('/personalization/list');
  pane.innerHTML = `<div class="row between" style="margin-bottom:12px"><h3 style="margin:0">Personalised requests</h3><a class="btn primary small" href="#/personalize">New request</a></div>
    ${list.length ? `<div class="table-wrap"><table><thead><tr><th>Request</th><th>Text</th><th>Status</th><th>Quote</th><th></th></tr></thead><tbody>${list.map(r => `<tr><td>${esc(pretty(r.kind))} × ${r.quantity}</td><td>${esc(r.textContent)}</td><td>${statusBadge(r.status)}</td><td>${r.quoteAmount ? money(r.quoteAmount) : '—'}</td><td class="actions"><a class="btn small ghost" href="#/personalize?open=${r.id}">Open</a></td></tr>`).join('')}</tbody></table></div>` : emptyState('✨', 'No requests yet', 'Names, monograms and message strips made just for you.', '<a class="btn primary" href="#/personalize">Start a request</a>')}`;
}

async function bookings(pane) {
  const list = await api.get('/experience/my');
  pane.innerHTML = `<h3>My bookings</h3>${list.length ? `<div class="table-wrap"><table><thead><tr><th>Reference</th><th>Seats</th><th>Amount</th><th>Status</th><th></th></tr></thead><tbody>${list.map(b => `<tr><td>${esc(b.bookingSn)}</td><td>${b.seats}</td><td>${money(b.amount)}</td><td>${statusBadge(b.status)}</td><td class="actions">${b.status === 'PENDING_PAYMENT' ? `<button class="btn small primary" data-pay="${b.id}" data-amt="${b.amount}">Pay</button>` : ''}${b.status !== 'CANCELLED' ? `<button class="btn small ghost" data-cancel="${b.id}">Cancel</button>` : ''}</td></tr>`).join('')}</tbody></table></div>` : emptyState('🎟️', 'No bookings yet', 'Join a tasting or workshop.', '<a class="btn primary" href="#/experiences">See sessions</a>')}`;
  const { askCard } = await import('/app/core.js');
  $$('[data-pay]', pane).forEach(b => b.addEventListener('click', async () => {
    const token = await askCard('Pay for your booking', money(b.dataset.amt));
    if (token === null) return;
    try { await api.post('/experience/pay/' + b.dataset.pay, { cardToken: token }); toast('Booking confirmed', 'ok'); bookings(pane); } catch (e) { fail(e); }
  }));
  $$('[data-cancel]', pane).forEach(b => b.addEventListener('click', async () => {
    if (!(await confirmDialog('Cancel booking?', 'Your seats are released and a paid booking is refunded.', 'Cancel booking'))) return;
    try { await api.post('/experience/cancel/' + b.dataset.cancel); toast('Booking cancelled', 'ok'); bookings(pane); } catch (e) { fail(e); }
  }));
}
