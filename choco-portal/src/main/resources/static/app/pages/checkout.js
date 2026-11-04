import { api, session, requireLogin, refreshCart, refreshSession, productArt, askCard, emptyState, nowLocalInput } from '/app/core.js';
import { esc, money, dt, day, $, $$, toast, fail, modal, readForm } from '/shared/ui.js';

// ------------------------------------------------------------------ basket
export async function cartPage() {
  if (!requireLogin()) return '';
  const el = document.createElement('div');
  async function draw() {
    const cart = await api.get('/cart/list');
    session.cartCount = cart.itemCount;
    if (!cart.items.length) {
      el.innerHTML = '<h1>Your basket</h1>' + emptyState('🛒', 'Your basket is empty', 'Add something sweet to get started.', '<a class="btn primary" href="#/shop">Browse the shop</a>');
      return;
    }
    const unavailable = cart.items.some(i => !i.available);
    el.innerHTML = `<h1>Your basket</h1>
    <div class="basket-layout">
      <div class="panel">${cart.items.map(i => `
        <div class="line">
          <div class="thumb">${productArt({ name: i.name, pic: null, kind: 'MADE_IN_SHOP' })}</div>
          <div><b><a href="#/product/${i.productId}">${esc(i.name)}</a></b> ${i.skuLabel && i.skuLabel !== 'Standard' ? `<span class="badge">${esc(i.skuLabel)}</span>` : ''}
            ${i.preorderOnly ? '<span class="badge ruby">Preorder</span>' : ''}${i.requiresColdChain ? '<span class="badge info">Chilled · pickup only</span>' : ''}
            ${!i.available ? '<div class="small" style="color:var(--danger)">Not available in this quantity. Change it or remove it.</div>' : ''}
            ${i.priceChanged ? '<div class="small muted">The price changed since you added this.</div>' : ''}
            ${i.box ? `<div class="small muted">${i.box.map(b => `${b.quantity} × ${esc(b.name)}`).join(', ')}</div>` : ''}
            <div class="row" style="margin-top:8px"><div class="qty"><button data-q="${i.id}" data-d="-1" aria-label="Fewer">−</button><input value="${i.quantity}" readonly aria-label="Quantity"><button data-q="${i.id}" data-d="1" aria-label="More">+</button></div>
              <button class="btn small ghost" data-rm="${i.id}">Remove</button></div></div>
          <div class="right"><b>${money(i.lineTotal)}</b><div class="small muted">${money(i.unitPrice)} each</div></div></div>`).join('')}</div>
      <aside class="panel summary stack"><h3 style="margin:0">Summary</h3>
        <div class="total-row"><span>Items (${cart.itemCount})</span><span>${money(cart.subtotal)}</span></div>
        <p class="small muted">Delivery, discounts, coupons and points are calculated at checkout.</p>
        ${unavailable ? '<div class="alert warn small">Some items are unavailable and are not included in the total.</div>' : ''}
        <a class="btn primary block" href="#/checkout">Go to checkout</a><a class="btn ghost block" href="#/shop">Keep shopping</a></aside>
    </div>`;
    $$('[data-q]', el).forEach(b => b.addEventListener('click', async () => {
      const line = cart.items.find(x => String(x.id) === b.dataset.q);
      const quantity = line.quantity + Number(b.dataset.d);
      if (quantity < 1) return;
      try { await api.postQ('/cart/update/quantity', { id: line.id, quantity }); await draw(); } catch (e) { fail(e); }
    }));
    $$('[data-rm]', el).forEach(b => b.addEventListener('click', async () => { try { await api.postQ('/cart/delete', { ids: [b.dataset.rm] }); await draw(); await refreshCart(); } catch (e) { fail(e); } }));
    await refreshCart();
  }
  await draw();
  return el;
}

// ------------------------------------------------------------------ checkout
export async function checkoutPage() {
  if (!requireLogin()) return '';
  const [cart, addresses, info, windows] = await Promise.all([api.get('/cart/list'), api.get('/member/address/list'), api.get('/shop/info'), api.get('/preorder/windows')]);
  const items = cart.items.filter(i => i.available);
  if (!items.length) return '<h1>Checkout</h1>' + emptyState('🛒', 'Nothing to check out', 'Your basket has no available items.', '<a class="btn primary" href="#/shop">Browse the shop</a>');
  const preorder = items.some(i => i.preorderOnly);
  const cold = items.some(i => i.requiresColdChain);
  const el = document.createElement('div');

  el.innerHTML = `<h1>Checkout</h1>
  <div class="checkout-layout">
    <form id="co" class="stack">
      <div class="panel"><h3>1 · How would you like it?</h3>
        <div class="choice-row">
          <label class="choice"><input type="radio" name="fulfillmentType" value="PICKUP" checked> <b>Pickup</b><span class="small muted">Ready in about ${info.pickupLeadMinutes} minutes</span></label>
          <label class="choice" ${info.deliveryEnabled && !cold ? '' : 'style="opacity:.5"'}><input type="radio" name="fulfillmentType" value="DELIVERY" ${info.deliveryEnabled && !cold ? '' : 'disabled'}> <b>Delivery</b><span class="small muted">${cold ? 'Not available for chilled items' : info.deliveryEnabled ? money(info.deliveryFee) + ', free over ' + money(info.freeDeliveryThreshold) : 'Currently unavailable'}</span></label>
        </div>
        <div id="pickup-box" style="margin-top:14px">
          ${preorder ? `<div class="field"><label>Preorder window</label><select name="preorderWindowId" data-type="int">${windows.length ? windows.map(w => `<option value="${w.id}" data-from="${w.fulfilFrom}" data-to="${w.fulfilTo}">${esc(w.name)} — order by ${dt(w.orderCutoff)} (${w.remainingUnits} left)</option>`).join('') : '<option value="">No open windows</option>'}</select></div>
             <div class="field"><label>Collection date</label><input type="date" name="preDate" id="pre-date"><div class="hint">Choose a day inside the window.</div></div>`
            : `<div class="field"><label>Pickup time <span class="muted">(optional)</span></label><input type="datetime-local" name="pickupSlot" min="${nowLocalInput(info.pickupLeadMinutes)}"><div class="hint">Leave empty for the earliest time.</div></div>`}
        </div>
        <div id="delivery-box" class="hidden" style="margin-top:14px">
          <div class="field"><label>Delivery address</label>
            <div class="row"><select name="addressId" data-type="int" class="grow">${addresses.map(a => `<option value="${a.id}" ${a.defaultStatus ? 'selected' : ''}>${esc(a.name)} · ${esc(a.detailAddress)}</option>`).join('') || '<option value="">No saved address</option>'}</select>
            <button type="button" class="btn small ghost" id="new-addr">+ New</button></div></div>
        </div>
      </div>
      <div class="panel"><h3>2 · Savings</h3>
        <div class="field"><label>Coupon</label><select name="couponHistoryId" data-type="int" id="coupon"><option value="">No coupon</option></select><div class="hint" id="coupon-hint"></div></div>
        <div class="field"><label>Use loyalty points <span class="muted" id="pts-info"></span></label><input type="number" name="useIntegration" id="pts" min="0" step="50" value="0"></div>
      </div>
      <div class="panel"><h3>3 · Extras</h3>
        <label class="check"><input type="checkbox" name="giftSleeve"> Gift sleeve (${money(info.giftPackagingFee)})</label>
        <div class="field"><label>Gift message <span class="muted">(optional)</span></label><input name="giftMessage" maxlength="300" placeholder="Happy birthday!"></div>
        <div class="field"><label>Note for the kitchen <span class="muted">(optional)</span></label><textarea name="note" maxlength="500" placeholder="Allergies, special requests…"></textarea></div>
      </div>
    </form>
    <aside class="panel summary stack" id="sum"></aside>
  </div>`;

  const form = $('#co', el);
  const toggle = () => {
    const delivery = form.fulfillmentType.value === 'DELIVERY';
    $('#delivery-box', el).classList.toggle('hidden', !delivery);
    $('#pickup-box', el).classList.toggle('hidden', delivery && !preorder);
  };

  function param() {
    const f = readForm(form);
    const p = { cartItemIds: items.map(i => i.id), fulfillmentType: f.fulfillmentType, giftSleeve: !!f.giftSleeve, giftMessage: f.giftMessage, note: f.note };
    if (f.fulfillmentType === 'DELIVERY' && f.addressId) p.addressId = f.addressId;
    if (f.couponHistoryId) p.couponHistoryId = f.couponHistoryId;
    if (f.useIntegration) p.useIntegration = Number(f.useIntegration);
    if (preorder) { if (f.preorderWindowId) p.preorderWindowId = f.preorderWindowId; if (f.preDate) p.pickupSlot = f.preDate + 'T10:00'; }
    else if (f.fulfillmentType === 'PICKUP' && f.pickupSlot) p.pickupSlot = f.pickupSlot;
    return p;
  }

  let timer, lastPreview = null;
  async function refresh() {
    toggle();
    try {
      const pv = lastPreview = await api.post('/order/confirm', param());
      // keep the coupon list current
      const sel = $('#coupon', el), chosen = sel.value;
      sel.innerHTML = '<option value="">No coupon</option>' + pv.coupons.map(c => `<option value="${c.couponHistoryId}" ${c.applicable ? '' : 'disabled'} ${String(c.couponHistoryId) === chosen ? 'selected' : ''}>${esc(c.name)}${c.applicable ? ' (−' + money(c.discount) + ')' : ' — ' + esc(c.reason)}</option>`).join('');
      $('#pts-info', el).textContent = `(you can use up to ${pv.maxRedeemablePoints})`;
      $('#pts', el).max = pv.maxRedeemablePoints;
      const row = (label, v, neg) => v && Number(v) ? `<div class="total-row"><span>${label}</span><span>${neg ? '−' : ''}${money(v)}</span></div>` : '';
      $('#sum', el).innerHTML = `<h3 style="margin:0">Order summary</h3>
        ${pv.items.map(i => `<div class="total-row small"><span>${i.quantity} × ${esc(i.name)}${i.skuLabel && i.skuLabel !== 'Standard' ? ' · ' + esc(i.skuLabel) : ''}</span><span>${money(i.lineTotal)}</span></div>`).join('')}
        <hr style="border:0;border-top:1px solid var(--line);width:100%">
        <div class="total-row"><span>Subtotal</span><span>${money(pv.subtotal)}</span></div>
        ${row('Bundle price', pv.bundleDiscount, true)}${row('Coupon', pv.couponDiscount, true)}${row('Loyalty points', pv.pointsDiscount, true)}
        ${row('Delivery', pv.deliveryFee)}${row('Gift sleeve', pv.giftPackagingFee)}
        <div class="total-row big"><span>Total</span><span>${money(pv.payAmount)}</span></div>
        <div class="small muted">You will earn about <b>${pv.pointsToEarn}</b> points.</div>
        ${pv.problems.length ? `<div class="alert error small">${pv.problems.map(esc).join('<br>')}</div>` : ''}
        ${pv.notices.length ? `<div class="alert small">${pv.notices.map(esc).join('<br>')}</div>` : ''}
        <button class="btn primary block" id="place" ${pv.problems.length ? 'disabled' : ''}>Place order</button>
        <p class="small muted">You will pay on the next step. Unpaid orders are released after ${info.paymentWindowMinutes} minutes.</p>`;
      $('#place', el).addEventListener('click', place);
    } catch (e) { $('#sum', el).innerHTML = `<div class="alert error">${esc(e.message)}</div>`; }
  }
  const later = () => { clearTimeout(timer); timer = setTimeout(refresh, 250); };

  async function place() {
    const b = $('#place', el); b.disabled = true;
    try {
      const order = await api.post('/order/generate', param());
      await refreshCart();
      location.hash = '#/pay/' + order.order.id;
    } catch (e) { fail(e); b.disabled = false; }
  }

  form.addEventListener('input', later); form.addEventListener('change', later);
  $('#new-addr', el)?.addEventListener('click', () => addressModal(async () => { location.reload(); }));
  toggle(); refresh();
  return el;
}

export function addressModal(done, existing = {}) {
  const body = document.createElement('form');
  body.innerHTML = `<div class="form-grid">
    <div class="field"><label>Name</label><input name="name" required value="${esc(existing.name || '')}"></div>
    <div class="field"><label>Phone</label><input name="phone" required placeholder="+15550001234" value="${esc(existing.phone || '')}"></div>
    <div class="field"><label>Street address</label><input name="detailAddress" required value="${esc(existing.detailAddress || '')}"></div>
    <div class="field"><label>City</label><input name="city" value="${esc(existing.city || '')}"></div>
    <div class="field"><label>Region / state</label><input name="province" value="${esc(existing.province || '')}"></div>
    <div class="field"><label>Postcode</label><input name="postCode" value="${esc(existing.postCode || '')}"></div></div>
    <label class="check"><input type="checkbox" name="defaultStatus" ${existing.defaultStatus ? 'checked' : ''}> Use as my default address</label>`;
  modal({
    title: existing.id ? 'Edit address' : 'New address', body,
    actions: [{ label: 'Cancel', kind: 'ghost' }, {
      label: 'Save', kind: 'primary', run: async () => {
        if (!body.reportValidity()) return true;
        await api.post(existing.id ? '/member/address/update/' + existing.id : '/member/address/add', readForm(body));
        toast('Address saved', 'ok'); await done();
      },
    }],
  });
}

// ------------------------------------------------------------------ payment
export async function payPage({ args }) {
  if (!requireLogin()) return '';
  const [d, info] = await Promise.all([api.get('/order/detail/' + args.id), api.get('/shop/info')]);
  const o = d.order;
  const el = document.createElement('div');
  if (o.status !== 'PENDING_PAYMENT') {
    el.innerHTML = `<div class="auth-box panel center stack"><div class="big" style="font-size:3rem">${o.status === 'CANCELLED' ? '⏰' : '🎉'}</div>
      <h2>${o.status === 'CANCELLED' ? 'This order was cancelled' : 'Thank you! Your order is confirmed'}</h2>
      <p class="muted">Order ${esc(o.orderSn)} · ${money(o.payAmount)}</p>
      ${o.status !== 'CANCELLED' ? `<p>We are preparing it now. ${o.fulfillmentType === 'PICKUP' ? 'We will have it ready for pickup' + (o.pickupSlotStart ? ' around ' + dt(o.pickupSlotStart) : '') + '.' : 'We will send it out soon.'}</p><p class="small muted">You earned ${o.integrationEarned} loyalty points.</p>` : ''}
      <div class="row" style="justify-content:center"><a class="btn primary" href="#/orders/${o.id}">View order</a><a class="btn ghost" href="#/shop">Keep shopping</a></div></div>`;
    return el;
  }
  const deadline = new Date(new Date(o.createTime).getTime() + info.paymentWindowMinutes * 60000);
  el.innerHTML = `<h1>Pay for your order</h1>
  <div class="checkout-layout"><div class="panel stack">
    <div class="alert warn" id="countdown"></div>
    <div class="choice-row">
      <label class="choice"><input type="radio" name="method" value="CARD" checked> <b>Card</b><span class="small muted">Charged now</span></label>
      <label class="choice"><input type="radio" name="method" value="CASH_AT_COUNTER"> <b>Cash at the counter</b><span class="small muted">Staff confirm when you pay</span></label>
      <label class="choice"><input type="radio" name="method" value="BANK_TRANSFER"> <b>Bank transfer</b><span class="small muted">Staff confirm when it arrives</span></label></div>
    <div class="field" id="card-field"><label>Card token</label><input id="token" value="tok_ok"><div class="hint">Demo payment. Any value succeeds; <code>tok_decline</code> simulates a declined card.</div></div>
    <button class="btn primary" id="pay">Pay ${money(o.payAmount)}</button></div>
  <aside class="panel stack"><h3 style="margin:0">Order ${esc(o.orderSn)}</h3>${d.items.map(i => `<div class="total-row small"><span>${i.productQuantity} × ${esc(i.productName)}</span><span>${money(i.realAmount)}</span></div>`).join('')}
    <div class="total-row big"><span>Total</span><span>${money(o.payAmount)}</span></div></aside></div>`;
  const tick = () => {
    const left = Math.max(0, Math.round((deadline - new Date()) / 1000));
    const box = $('#countdown', el);
    if (!box) return clearInterval(iv);
    box.textContent = left ? `Your items are reserved for ${Math.floor(left / 60)}:${String(left % 60).padStart(2, '0')} more minutes.` : 'The payment window has passed. The order may have been released.';
  };
  const iv = setInterval(tick, 1000); tick();
  $$('input[name=method]', el).forEach(r => r.addEventListener('change', () => $('#card-field', el).classList.toggle('hidden', r.value !== 'CARD')));
  $('#pay', el).addEventListener('click', async e => {
    const method = $('input[name=method]:checked', el).value;
    e.target.disabled = true;
    try {
      const p = await api.post('/payment/pay', { orderId: o.id, method, cardToken: $('#token', el).value });
      await refreshSession();
      if (p.status === 'SUCCEEDED') { toast('Payment received. Thank you!', 'ok'); clearInterval(iv); location.hash = '#/pay/' + o.id + '?done=1'; }
      else { toast('We will confirm your payment as soon as it reaches us.', 'ok'); location.hash = '#/orders/' + o.id; }
    } catch (err) { fail(err); e.target.disabled = false; }
  });
  return el;
}
