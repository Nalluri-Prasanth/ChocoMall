import { api, session, productCard, productArt, cardBadges, requireLogin, refreshCart, askCard, stars, emptyState } from '/app/core.js';
import { esc, money, dt, day, $, $$, toast, fail, pretty } from '/shared/ui.js';

const personalizedKind = name => /monogram|medallion/i.test(name) ? 'MONOGRAM_MEDALLION' : /photo/i.test(name) ? 'EDIBLE_PHOTO_PLAQUE' : /logo/i.test(name) ? 'LOGO_PLAQUE' : 'NAME_PLAQUE';

export async function product({ args }) {
  const d = await api.get('/product/detail/' + args.id);
  const c = d.card;
  const [related, reviews] = await Promise.all([api.get('/product/related/' + c.id), api.get('/product/' + c.id + '/reviews', { pageSize: 5 })]);
  const el = document.createElement('div');
  let favourite = d.favourite;
  let sku = d.skus[0];

  const claims = c.claims.map(x => `<span class="badge ok" title="${esc(x.evidence || '')}">✔ ${esc(pretty(x.type))}</span>`).join(' ');
  const facts = [
    d.chocolateType ? `<span class="badge">${esc(d.chocolateType)}</span>` : '',
    d.weightGrams ? `<span class="badge">${d.weightGrams} g</span>` : '',
    d.shelfLifeDays ? `<span class="badge">Best within ${d.shelfLifeDays} days</span>` : '',
    ...d.flavors.map(f => `<a class="badge" href="#/shop?flavor=${f.id}">${esc(f.name)}</a>`),
  ].join('');

  el.innerHTML = `
    <p class="small muted"><a href="#/shop">Shop</a> › <a href="#/shop?cat=${d.categoryId}">${esc(d.categoryName || '')}</a> › ${esc(c.name)}</p>
    <div class="product-layout">
      <div><div class="product-hero">${productArt(c)}</div></div>
      <div class="stack">
        <div class="row">${cardBadges(c)}</div>
        <h1 style="margin:0">${esc(c.name)}</h1>
        <p class="muted" style="margin:0">${esc(c.subTitle || '')}</p>
        <div class="row">${d.averageStar ? `${stars(d.averageStar)} <span class="small muted">${d.averageStar} · ${d.commentCount} review${d.commentCount === 1 ? '' : 's'}</span>` : '<span class="small muted">No reviews yet</span>'}</div>
        <div class="price-big" id="price">${money(sku?.price ?? c.price)}</div>
        <p>${esc(d.description || '')}</p>
        <div class="facts">${facts}${claims}</div>
        ${c.allergens.length ? `<div class="alert warn"><b>Contains:</b> ${esc(c.allergens.join(', '))}. Made in a kitchen that handles other allergens; ask in the shop if you are unsure.</div>` : '<div class="alert">No allergens recorded for this item. Always read the label.</div>'}
        <div id="buy"></div>
        <section class="product-activity" aria-label="Product activity"><div id="product-activity" aria-live="polite"><span class="small muted">Loading product activity…</span></div><p class="small muted">Unique browser visitors today · current customer baskets · paid buyers this calendar month. Counts are anonymous.</p></section>
      </div>
    </div>
    <div id="extra"></div>
    ${d.bundles.length ? `<div class="section-head"><h2>Part of a bundle</h2></div><div class="grid cols-2">${d.bundles.map(b => `<div class="card pad"><b>${esc(b.name)}</b> <span class="badge ok">${money(b.price)}</span><div class="small muted">${b.items.map(i => `${i.quantity} × ${esc(i.name)}`).join(' + ')}</div><div class="small">Add all the items to your basket and the bundle price applies automatically.</div></div>`).join('')}</div>` : ''}
    <div class="section-head"><h2>Reviews</h2></div>
    <div id="reviews"></div>
    ${related.length ? `<div class="section-head"><h2>You may also like</h2></div><div class="grid cards">${related.map(productCard).join('')}</div>` : ''}`;

  const buy = $('#buy', el);
  const extra = $('#extra', el);

  // ------------------------------------------------------------ purchase panel by kind
  if (c.kind === 'PERSONALIZED') {
    buy.innerHTML = `<div class="panel stack"><h3 style="margin:0">Made just for you</h3>
      <p class="muted" style="margin:0">Tell us the text and the date you need it. We reply with a quote and a realistic lead time, send you an artwork proof to approve, and only then ask for a deposit.</p>
      <a class="btn primary" href="#/personalize?kind=${personalizedKind(c.name)}">Request a quote</a></div>`;
  } else if (c.kind === 'EXPERIENCE') {
    buy.innerHTML = `<div class="panel stack"><h3 style="margin:0">Upcoming sessions</h3>${d.sessions.length ? d.sessions.map(s => `
      <div class="row between" style="padding:8px 0;border-bottom:1px solid var(--line)"><div><b>${dt(s.startsAt)}</b><div class="small muted">${s.durationMinutes} min · ${esc(s.location || '')}</div></div>
      <div class="row"><span><b>${money(s.price)}</b>/seat · ${s.seatsLeft} left</span>
      <select data-seats="${s.id}" style="width:auto">${[1, 2, 3, 4].filter(n => n <= s.seatsLeft).map(n => `<option>${n}</option>`).join('')}</select>
      <button class="btn small primary" data-book="${s.id}" ${s.seatsLeft ? '' : 'disabled'}>Book</button></div></div>`).join('') : '<p class="muted">No sessions are scheduled right now. Check back soon.</p>'}</div>`;
    $$('[data-book]', buy).forEach(b => b.addEventListener('click', async () => {
      if (!requireLogin()) return;
      const seats = Number($(`[data-seats="${b.dataset.book}"]`, buy).value);
      const session0 = d.sessions.find(s => String(s.id) === b.dataset.book);
      try {
        const booking = await api.post('/experience/book', { sessionId: session0.id, seats });
        const token = await askCard('Pay for your booking', money(booking.amount));
        if (token === null) { toast('Seats are held for a short time. Pay from Account › Bookings.'); return; }
        await api.post('/experience/pay/' + booking.id, { cardToken: token });
        toast('Booked! See you there.', 'ok');
        location.hash = '#/account/bookings';
      } catch (e) { fail(e); }
    }));
  } else {
    renderBuy();
  }

  function renderBuy() {
    const box = c.buildABox;
    const preorder = c.preorderOnly ? `<div class="alert"><b>Preorder item.</b> Choose a collection window at checkout.${d.preorderWindows.length ? '<ul style="margin:6px 0 0;padding-left:18px">' + d.preorderWindows.map(w => `<li>${esc(w.name)}: order by ${dt(w.orderCutoff)}, ${w.remainingUnits} places left</li>`).join('') + '</ul>' : ''}</div>` : '';
    buy.innerHTML = `${preorder}<div class="panel stack">
      ${d.skus.length > 1 ? `<div class="sku-list" role="radiogroup" aria-label="Size">${d.skus.map((s, i) => `<label class="sku"><input type="radio" name="sku" value="${s.id}" ${i === 0 ? 'checked' : ''}><span class="grow">${esc(s.label)}${s.units > 1 ? ` <span class="muted small">(${s.units} pieces)</span>` : ''}</span><b>${money(s.price)}</b></label>`).join('')}</div>` : ''}
      <div id="box"></div>
      <div class="row"><div class="qty"><button type="button" data-q="-1" aria-label="Fewer">−</button><input id="qty" type="number" min="1" max="50" value="1" aria-label="Quantity"><button type="button" data-q="1" aria-label="More">+</button></div>
        <button class="btn primary grow" id="add" ${c.inStock ? '' : 'disabled'}>${c.inStock ? 'Add to basket' : 'Sold out'}</button>
        <button class="icon-btn" id="fav" title="Save for later" aria-label="Favourite">${favourite ? '❤️' : '🤍'}</button></div>
      <div class="small muted" id="subtotal"></div></div>`;
    const qty = $('#qty', buy);
    const boxState = new Map();
    let candidates = [];
    const units = () => sku?.units ?? 0;
    const filled = () => [...boxState.values()].reduce((a, b) => a + b, 0);

    function updateTotals() {
      $('#price', el).textContent = money(sku.price);
      $('#subtotal', buy).textContent = `Line total: ${money(sku.price * Number(qty.value || 1))}`;
      if (box) {
        const left = units() - filled();
        $('#add', buy).disabled = !c.inStock || left !== 0;
      }
    }
    async function renderBox() {
      if (!box) return;
      if (!candidates.length) candidates = await api.get('/product/boxCandidates');
      const n = units();
      const slots = Array.from({ length: n }, (_, i) => `<span class="box-slot ${i < filled() ? 'full' : ''}">${i < filled() ? '🍫' : i + 1}</span>`).join('');
      $('#box', buy).innerHTML = `<div class="box-builder"><b>Fill your box: ${filled()} of ${n} chosen</b><div class="box-slots">${slots}</div>
        ${candidates.map(p => `<div class="box-item"><span>${esc(p.name)} <span class="muted small">${esc((p.subTitle || ''))}</span></span>
          <span class="qty"><button type="button" data-box="${p.id}" data-d="-1">−</button><input readonly value="${boxState.get(p.id) || 0}"><button type="button" data-box="${p.id}" data-d="1">+</button></span></div>`).join('')}</div>`;
      $$('[data-box]', buy).forEach(b => b.addEventListener('click', () => {
        const pid = Number(b.dataset.box), delta = Number(b.dataset.d);
        const next = (boxState.get(pid) || 0) + delta;
        if (next < 0 || (delta > 0 && filled() >= units())) return;
        if (next === 0) boxState.delete(pid); else boxState.set(pid, next);
        renderBox(); updateTotals();
      }));
      updateTotals();
    }
    $$('input[name=sku]', buy).forEach(r => r.addEventListener('change', () => { sku = d.skus.find(s => String(s.id) === r.value); boxState.clear(); renderBox(); updateTotals(); }));
    $$('[data-q]', buy).forEach(b => b.addEventListener('click', () => { qty.value = Math.min(50, Math.max(1, Number(qty.value || 1) + Number(b.dataset.q))); updateTotals(); }));
    qty.addEventListener('input', updateTotals);
    $('#fav', buy).addEventListener('click', async () => {
      if (!requireLogin()) return;
      try {
        await api.post(`/member/productCollection/${favourite ? 'delete' : 'add'}/${c.id}`);
        favourite = !favourite; $('#fav', buy).textContent = favourite ? '❤️' : '🤍';
        toast(favourite ? 'Saved to your favourites' : 'Removed from favourites', 'ok');
      } catch (e) { fail(e); }
    });
    $('#add', buy).addEventListener('click', async () => {
      if (!requireLogin()) return;
      try {
        const payload = { productId: c.id, skuId: sku.id, quantity: Number(qty.value || 1) };
        if (box) payload.box = [...boxState].map(([productId, quantity]) => ({ productId, quantity }));
        await api.post('/cart/add', payload);
        await refreshCart();
        api.get(`/product/${c.id}/activity`).then(renderActivity).catch(() => {});
        toast(`${c.name} added to your basket`, 'ok');
        boxState.clear(); renderBox();
      } catch (e) { fail(e); }
    });
    renderBox(); updateTotals();
  }

  // ------------------------------------------------------------ reviews
  const rv = $('#reviews', el);
  rv.innerHTML = `${reviews.list.length ? reviews.list.map(r => `<div class="panel" style="margin-bottom:10px">${stars(r.star)} <b>${esc(r.memberNickName || 'Customer')}</b> <span class="small muted">${dt(r.createTime)}</span><p style="margin:6px 0 0">${esc(r.content || '')}</p></div>`).join('') : '<p class="muted">Be the first to review this after your order is completed.</p>'}
    ${session.token ? `<details class="panel"><summary><b>Write a review</b> <span class="small muted">(available after you have collected this item)</span></summary>
      <form id="review-form" class="stack" style="margin-top:10px"><div class="field"><label>Rating</label><select name="star" data-type="int">${[5, 4, 3, 2, 1].map(n => `<option value="${n}">${'★'.repeat(n)}</option>`).join('')}</select></div>
      <div class="field"><label>Your review</label><textarea name="content" maxlength="1000"></textarea></div><button class="btn small">Post review</button></form></details>` : ''}`;
  $('#review-form', rv)?.addEventListener('submit', async e => {
    e.preventDefault();
    const f = new FormData(e.target);
    try { await api.post('/member/review/create', { productId: c.id, star: Number(f.get('star')), content: f.get('content') }); toast('Thank you for your review', 'ok'); location.reload(); }
    catch (err) { fail(err); }
  });
  const activityRoot = $('#product-activity', el);
  const renderActivity = a => {
    activityRoot.innerHTML = `<div class="activity-counts"><div><b>${esc(a.viewersToday)}</b><span>viewed today</span></div><div><b>${esc(a.shoppersWithInCart)}</b><span>shoppers have this in their basket</span></div><div><b>${esc(a.buyersThisMonth)}</b><span>buyers this month</span></div></div>`;
  };
  api.post(`/product/${c.id}/activity/view`).then(renderActivity).catch(() => { activityRoot.innerHTML = '<span class="small muted">Activity counts temporarily unavailable.</span>'; });
  return el;
}
