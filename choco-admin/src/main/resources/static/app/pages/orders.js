import { api, rowsOf, enumOptions, ENUMS, table, pageHead, buildForm, openForm, yesNo, badge, esc, $, $$, money, dt, toast, fail, modal, confirmDialog, pager, hashQ, pretty, readForm } from '/app/core.js';
import { crud } from '/app/crud.js';

const reload = () => window.dispatchEvent(new HashChangeEvent('hashchange'));

export async function ordersPage({ params }) {
  const page = Number(params.page || 1);
  const data = await api.get('/order/list', { orderSn: params.orderSn, receiver: params.receiver, status: params.status, fulfillmentType: params.fulfillmentType, preorder: params.preorder, pageNum: page, pageSize: 12 });
  const el = document.createElement('div');
  const sel = (name, label, opts) => `<select name="${name}"><option value="">${label}</option>${opts.map(o => `<option value="${esc(o.value)}" ${String(params[name]) === String(o.value) ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>`;
  el.innerHTML = pageHead('Orders', 'Newest first. Open an order to confirm payment, mark it ready, ship it or cancel it.')
    + `<form class="filters" id="flt"><input name="orderSn" placeholder="Order number" value="${esc(params.orderSn || '')}"><input name="receiver" placeholder="Customer" value="${esc(params.receiver || '')}">
       ${sel('status', 'Any status', enumOptions(ENUMS.OrderStatus))}${sel('fulfillmentType', 'Pickup or delivery', enumOptions(ENUMS.FulfillmentType))}${sel('preorder', 'Any timing', [{ value: 'true', label: 'Preorders' }, { value: 'false', label: 'Same-day' }])}<button class="btn small">Filter</button></form>`
    + table([
      { label: 'Order', html: r => `<a href="#/orders/${r.id}"><b>${esc(r.orderSn)}</b></a>` }, { label: 'Placed', get: r => dt(r.createTime) }, { label: 'Customer', key: 'memberUsername' },
      { label: 'Type', html: r => `${esc(pretty(r.fulfillmentType))}${r.preorder ? ' <span class="badge ruby">Preorder</span>' : ''}` }, { label: 'Total', num: true, html: r => money(r.payAmount) },
      { label: 'Status', html: r => badge(r.status) }, { label: '', cls: 'actions', html: r => `<a class="btn small ghost" href="#/orders/${r.id}">Open</a>` },
    ], data.list, 'No orders match.');
  if (data.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = '#/orders' + hashQ({ ...params, page: n }); }));
  $('#flt', el).addEventListener('submit', e => { e.preventDefault(); const f = {}; new FormData(e.target).forEach((v, k) => { if (v) f[k] = v; }); location.hash = '#/orders' + hashQ(f); });
  return el;
}

export async function orderDetail({ args }) {
  const d = await api.get('/order/' + args.id);
  const o = d.order;
  const el = document.createElement('div');
  const pickup = o.fulfillmentType === 'PICKUP';
  const act = (id, label, kind = 'ghost') => `<button class="btn ${kind}" id="${id}">${label}</button>`;
  const actions = [];
  if (o.status === 'PENDING_PAYMENT') actions.push(act('cash', 'Confirm cash payment', 'primary'), act('bank', 'Confirm bank transfer'));
  if (o.status === 'PREPARING') actions.push(pickup ? act('ready', 'Mark ready for pickup', 'primary') : act('ship', 'Hand to courier…', 'primary'));
  if (['READY_FOR_PICKUP', 'SHIPPED'].includes(o.status)) actions.push(act('complete', pickup ? 'Customer collected' : 'Delivered', 'primary'));
  if (['PENDING_PAYMENT', 'PREPARING', 'READY_FOR_PICKUP'].includes(o.status)) actions.push(act('close', 'Cancel order', 'danger'));
  el.innerHTML = `<p class="small muted"><a href="#/orders">‹ Orders</a></p>${pageHead('Order ' + o.orderSn, `Placed ${dt(o.createTime)} by ${o.memberUsername || 'a customer'}`, actions.join(''))}
    <div class="grid cols-2"><div class="stack"><div class="panel"><h3>Items ${badge(o.status)} ${o.preorder ? '<span class="badge ruby">Preorder</span>' : ''}</h3>
      ${d.items.map(i => `<div class="total-row"><span>${i.productQuantity} × <b>${esc(i.productName)}</b> ${i.skuLabel && i.skuLabel !== 'Standard' ? `<span class="badge">${esc(i.skuLabel)}</span>` : ''}${i.boxSelection ? '<div class="small muted">Build-a-box selection included</div>' : ''}</span><span>${money(i.realAmount)}</span></div>`).join('')}
      <div class="total-row"><span>Goods</span><span>${money(o.totalAmount)}</span></div>
      ${Number(o.promotionAmount) ? `<div class="total-row"><span>Bundle saving</span><span>−${money(o.promotionAmount)}</span></div>` : ''}${Number(o.couponAmount) ? `<div class="total-row"><span>Coupon</span><span>−${money(o.couponAmount)}</span></div>` : ''}
      ${Number(o.integrationAmount) ? `<div class="total-row"><span>Points used (${o.useIntegration})</span><span>−${money(o.integrationAmount)}</span></div>` : ''}${Number(o.freightAmount) ? `<div class="total-row"><span>Delivery</span><span>${money(o.freightAmount)}</span></div>` : ''}
      ${Number(o.giftPackagingAmount) ? `<div class="total-row"><span>Gift sleeve</span><span>${money(o.giftPackagingAmount)}</span></div>` : ''}<div class="total-row big"><span>Total</span><span>${money(o.payAmount)}</span></div></div>
      <div class="panel"><h3>Payments</h3>${d.payments.length ? table([{ label: 'Method', html: p => esc(pretty(p.method)) }, { label: 'Status', html: p => badge(p.status) }, { label: 'Amount', num: true, html: p => money(p.amount) }, { label: 'Reference', key: 'transactionRef' }], d.payments) : '<p class="muted">No payment yet.</p>'}</div></div>
    <div class="stack"><div class="panel"><h3>Fulfilment</h3><dl class="kv"><dt>${pickup ? 'Pickup' : 'Delivery'}</dt><dd>${pickup ? (o.pickupSlotStart ? dt(o.pickupSlotStart) : 'As soon as ready') : esc([o.receiverName, o.receiverPhone, o.receiverDetailAddress, o.receiverCity].filter(Boolean).join(', '))}</dd>
      ${o.deliverySn ? `<dt>Tracking</dt><dd>${esc(o.deliveryCompany)} ${esc(o.deliverySn)}</dd>` : ''}${o.giftMessage ? `<dt>Gift message</dt><dd>${esc(o.giftMessage)}</dd>` : ''}${o.note ? `<dt>Customer note</dt><dd>${esc(o.note)}</dd>` : ''}
      <dt>Staff note</dt><dd>${esc(o.adminNote || '—')} <button class="btn small ghost" id="note">Edit</button></dd>${o.cancelReason ? `<dt>Cancelled</dt><dd>${esc(o.cancelReason)}</dd>` : ''}</dl></div>
      <div class="panel"><h3>History</h3><ul class="timeline">${d.history.map(h => `<li><b>${esc(h.note || pretty(h.orderStatus))}</b><div class="small muted">${dt(h.createTime)} · ${esc(h.operator || '')}</div></li>`).join('')}</ul></div></div></div>`;
  const run = (id, fn) => $('#' + id, el)?.addEventListener('click', async () => { try { await fn(); reload(); } catch (e) { fail(e); } });
  run('cash', async () => { await api.post('/order/update/confirmPayment', { orderId: o.id, method: 'CASH_AT_COUNTER' }); toast('Payment confirmed', 'ok'); });
  run('bank', async () => { await api.post('/order/update/confirmPayment', { orderId: o.id, method: 'BANK_TRANSFER' }); toast('Payment confirmed', 'ok'); });
  run('ready', async () => { await api.postQ('/order/update/ready', { ids: [o.id] }); toast('Marked ready. The customer is notified.', 'ok'); });
  run('complete', async () => { await api.postQ('/order/update/complete', { ids: [o.id] }); toast('Order completed', 'ok'); });
  $('#ship', el)?.addEventListener('click', () => openForm({
    title: 'Hand to courier', wide: false, fields: [{ name: 'company', label: 'Courier', required: true }, { name: 'trackingNumber', label: 'Tracking number', required: true }],
    submit: v => api.post('/order/update/delivery', [{ orderId: o.id, ...v }]), after: reload,
  }));
  $('#close', el)?.addEventListener('click', () => openForm({
    title: 'Cancel this order', wide: false, label: 'Cancel order', fields: [{ name: 'reason', label: 'Reason (shown in history)', required: true, maxlength: 200 }],
    submit: v => api.post('/order/update/close', { ids: [o.id], reason: v.reason }), after: reload,
  }));
  $('#note', el).addEventListener('click', () => openForm({ title: 'Staff note', wide: false, values: { note: o.adminNote }, fields: [{ name: 'note', label: 'Note', type: 'textarea', maxlength: 500 }], submit: v => api.post('/order/update/note/' + o.id, v), after: reload }));
  return el;
}

// ------------------------------------------------------------------ returns
export async function returnsPage({ params }) {
  const page = Number(params.page || 1);
  const data = await api.get('/returnApply/list', { status: params.status, orderSn: params.orderSn, pageNum: page, pageSize: 12 });
  const el = document.createElement('div');
  el.innerHTML = pageHead('Returns', 'Refund requests from shoppers. Completing a return writes the refund and can put the items back in stock.')
    + `<form class="filters" id="flt"><select name="status"><option value="">Any status</option>${enumOptions(ENUMS.ReturnStatus).map(o => `<option value="${o.value}" ${params.status === o.value ? 'selected' : ''}>${o.label}</option>`).join('')}</select><input name="orderSn" placeholder="Order number" value="${esc(params.orderSn || '')}"><button class="btn small">Filter</button></form>`
    + table([{ label: 'Order', key: 'orderSn' }, { label: 'Product', key: 'productName' }, { label: 'Qty', key: 'productCount', num: true }, { label: 'Reason', key: 'reason' }, { label: 'Amount', num: true, html: r => money(r.returnAmount) },
      { label: 'Status', html: r => badge(r.status) }, { label: 'Requested', get: r => dt(r.createTime) }, { label: '', cls: 'actions nowrap', html: (r, i) => ['PENDING', 'ACCEPTED'].includes(r.status) ? `<button class="btn small primary" data-h="${i}">Handle</button>` : '' }], data.list, 'No return requests.');
  if (data.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = '#/returns' + hashQ({ ...params, page: n }); }));
  $('#flt', el).addEventListener('submit', e => { e.preventDefault(); const f = {}; new FormData(e.target).forEach((v, k) => { if (v) f[k] = v; }); location.hash = '#/returns' + hashQ(f); });
  $$('[data-h]', el).forEach(b => b.addEventListener('click', () => {
    const r = data.list[Number(b.dataset.h)];
    openForm({
      title: `Return for ${r.orderSn}`, wide: false, label: 'Update', values: { status: r.status === 'PENDING' ? 'ACCEPTED' : 'COMPLETED', returnAmount: r.returnAmount },
      fields: [{ name: 'status', label: 'Decision', type: 'select', required: true, options: [{ value: 'ACCEPTED', label: 'Accept' }, { value: 'COMPLETED', label: 'Complete (refund now)' }, { value: 'REJECTED', label: 'Reject' }] },
        { name: 'returnAmount', label: 'Refund amount', type: 'number', min: 0 }, { name: 'handleNote', label: 'Note to the customer', type: 'textarea', full: true, maxlength: 500 },
        { name: 'restock', label: 'Put the items back in stock', type: 'bool', hint: 'Only if they are unopened and in good condition.' }],
      submit: v => api.post('/returnApply/update/status/' + r.id, v), after: reload,
    });
  }));
  return el;
}

// ------------------------------------------------------------------ personalisation
export async function personalizationPage({ params }) {
  const page = Number(params.page || 1);
  const data = await api.get('/personalization/list', { status: params.status, pageNum: page, pageSize: 12 });
  const el = document.createElement('div');
  el.innerHTML = pageHead('Personalised orders', 'Quote → send a proof → customer approves → they pay a deposit → make it. Nothing is made before the proof is approved.')
    + `<form class="filters" id="flt"><select name="status"><option value="">Any status</option>${enumOptions(ENUMS.PersonalizationStatus).map(o => `<option value="${o.value}" ${params.status === o.value ? 'selected' : ''}>${o.label}</option>`).join('')}</select><button class="btn small">Filter</button></form>`
    + table([{ label: 'Ref', key: 'requestSn' }, { label: 'What', html: r => `${esc(pretty(r.kind))}<div class="small muted">“${esc(r.textContent)}” × ${r.quantity}</div>` }, { label: 'Needed by', key: 'requestedDate' },
      { label: 'Quote', num: true, html: r => r.quoteAmount ? money(r.quoteAmount) : '—' }, { label: 'Status', html: r => badge(r.status) }, { label: '', cls: 'actions nowrap', html: (r, i) => `<button class="btn small ghost" data-open="${i}">Open</button>` }], data.list, 'No requests.');
  if (data.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = '#/personalization' + hashQ({ ...params, page: n }); }));
  $('#flt', el).addEventListener('submit', e => { e.preventDefault(); const f = {}; new FormData(e.target).forEach((v, k) => { if (v) f[k] = v; }); location.hash = '#/personalization' + hashQ(f); });
  $$('[data-open]', el).forEach(b => b.addEventListener('click', () => personalizationModal(data.list[Number(b.dataset.open)].id)));
  return el;
}

async function personalizationModal(id) {
  const d = await api.get('/personalization/' + id);
  const r = d.request, proofs = d.proofs || [];
  const body = document.createElement('div');
  body.innerHTML = `<dl class="kv"><dt>Customer wants</dt><dd>${esc(pretty(r.kind))}: “${esc(r.textContent)}” × ${r.quantity}</dd>${r.occasionNote ? `<dt>Note</dt><dd>${esc(r.occasionNote)}</dd>` : ''}<dt>Status</dt><dd>${badge(r.status)}</dd>
    ${r.quoteAmount ? `<dt>Quote</dt><dd>${money(r.quoteAmount)} · deposit ${money(r.depositAmount)} · ${r.leadTimeDays} days</dd>` : ''}
    ${proofs.map(p => `<dt>Proof v${p.version}</dt><dd>${esc(p.artworkNote)} ${p.approvedByMember ? '<span class="badge ok">Approved</span>' : ''}</dd>`).join('')}${r.adminNote ? `<dt>Our note</dt><dd>${esc(r.adminNote)}</dd>` : ''}</dl><div class="row" id="acts"></div>`;
  const acts = $('#acts', body);
  const m = modal({ title: `Request ${r.requestSn}`, body, wide: true });
  const add = (label, kind, fn) => { const b = document.createElement('button'); b.className = 'btn ' + kind; b.textContent = label; b.addEventListener('click', async () => { try { await fn(); m.close(); reload(); } catch (e) { fail(e); } }); acts.appendChild(b); };
  if (['SUBMITTED', 'QUOTED'].includes(r.status)) add(r.status === 'QUOTED' ? 'Re-quote' : 'Send quote', 'primary', () => new Promise((res, rej) => openForm({ title: 'Quote', wide: false, fields: [
    { name: 'quoteAmount', label: 'Total price', type: 'number', required: true, min: 0.01 }, { name: 'depositAmount', label: 'Deposit', type: 'number', required: true, min: 0, def: 0 },
    { name: 'leadTimeDays', label: 'Lead time (days, tested)', type: 'int', required: true, min: 1, def: 3 }, { name: 'adminNote', label: 'Message to the customer', type: 'textarea', full: true, maxlength: 500 }],
    submit: v => api.post(`/personalization/${id}/quote`, v), after: () => res() })));
  if (['QUOTED', 'PROOF_SENT'].includes(r.status)) add('Send proof', 'primary', () => new Promise(res => openForm({ title: 'Send artwork proof', wide: false, fields: [{ name: 'artworkNote', label: 'Describe the artwork (font, size, layout)', type: 'textarea', required: true, full: true, maxlength: 1000 }], submit: v => api.post(`/personalization/${id}/proof`, v), after: res })));
  if (r.status === 'DEPOSIT_PAID') add('Start production', 'primary', () => api.post(`/personalization/${id}/start`));
  if (r.status === 'IN_PRODUCTION') add('Mark ready', 'primary', () => api.post(`/personalization/${id}/ready`));
  if (r.status === 'READY') add('Collected / completed', 'primary', () => api.post(`/personalization/${id}/complete`));
  if (['SUBMITTED', 'QUOTED'].includes(r.status)) add('Decline', 'danger', () => new Promise(res => openForm({ title: 'Decline request', wide: false, fields: [{ name: 'reason', label: 'Reason', required: true }], submit: v => api.postQ(`/personalization/${id}/reject`, { reason: v.reason }), after: res })));
  if (['SUBMITTED', 'QUOTED', 'PROOF_SENT', 'PROOF_APPROVED', 'DEPOSIT_PAID'].includes(r.status)) add('Cancel', 'ghost', () => new Promise(res => openForm({ title: 'Cancel request', wide: false, fields: [{ name: 'reason', label: 'Reason', required: true }], submit: v => api.postQ(`/personalization/${id}/cancel`, { reason: v.reason }), after: res })));
}

// ------------------------------------------------------------------ settings
export async function settingsPage() {
  const s = await api.get('/orderSetting');
  const el = document.createElement('div');
  const F = [
    ['Orders', [{ name: 'normalOrderTimeoutMinutes', label: 'Unpaid order expires after (minutes)', type: 'int', min: 5 }, { name: 'autoConfirmDays', label: 'Auto-complete after (days)', type: 'int', min: 1 }, { name: 'commentTimeoutDays', label: 'Reviews open for (days)', type: 'int', min: 1 },
      { name: 'returnWindowDays', label: 'Return window (days)', type: 'int', min: 0 }, { name: 'experienceCancelHours', label: 'Free session cancellation (hours before)', type: 'int', min: 0 }]],
    ['Pickup and delivery', [{ name: 'pickupLeadMinutes', label: 'Pickup ready in (minutes)', type: 'int', min: 0 }, { name: 'preorderLeadDays', label: 'Minimum preorder lead (days)', type: 'int', min: 0 }, { name: 'deliveryEnabled', label: 'Offer delivery', type: 'bool' },
      { name: 'deliveryFee', label: 'Delivery fee', type: 'number', min: 0 }, { name: 'freeDeliveryThreshold', label: 'Free delivery over', type: 'number', min: 0 }, { name: 'giftPackagingFee', label: 'Gift sleeve fee', type: 'number', min: 0 }]],
    ['Loyalty points', [{ name: 'pointsPerCurrencyUnit', label: 'Points worth 1 unit of currency', type: 'int', min: 1, hint: 'e.g. 100 points = 1.00 off' }, { name: 'pointsPerCurrencySpent', label: 'Points earned per 1 spent', type: 'int', min: 0 }, { name: 'maxRedeemPercent', label: 'Max share of an order payable with points (%)', type: 'int', min: 0 }]],
  ];
  el.innerHTML = pageHead('Settings', 'These change how the storefront behaves straight away.') + '<form id="f" class="stack"></form>';
  const form = $('#f', el);
  F.forEach(([title, fields]) => { const panel = document.createElement('div'); panel.className = 'panel'; panel.innerHTML = `<h3>${title}</h3>`; const g = buildForm(fields, s); g.id = ''; panel.appendChild(g); form.appendChild(panel); });
  form.insertAdjacentHTML('beforeend', '<div><button class="btn primary">Save settings</button></div>');
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const body = {}; $$('form.form-grid', form).forEach(g => Object.assign(body, readForm(g)));
    try { await api.post('/orderSetting/update', body); toast('Settings saved', 'ok'); } catch (err) { fail(err); }
  });
  return el;
}

// ------------------------------------------------------------------ members
export const members = crud({
  title: 'Members', subtitle: 'Registered shoppers. Adjust points or suspend an account.', noun: 'member', list: '/member/list', pageSize: 15,
  filters: [{ name: 'keyword', label: 'Search username, phone, email' }],
  columns: [{ key: 'id', label: 'ID' }, { key: 'username', label: 'Username' }, { key: 'nickname', label: 'Name' }, { key: 'phone', label: 'Phone' }, { key: 'levelName', label: 'Level' },
    { key: 'integration', label: 'Points', num: true }, { label: 'Active', html: r => yesNo(r.status === 1) }],
  rowActions: [
    { label: 'Points…', run: (row, reload2) => openForm({ title: `Adjust points for ${row.username}`, wide: false, fields: [{ name: 'delta', label: 'Points to add (negative to remove)', type: 'int', required: true }, { name: 'note', label: 'Reason', required: true }], submit: v => api.post(`/member/${row.id}/points`, v), after: reload2 }) },
    { label: 'Enable / disable', run: async (row, reload2) => { await api.postQ(`/member/updateStatus/${row.id}`, { status: row.status === 1 ? 0 : 1 }); toast('Updated', 'ok'); reload2(); } },
  ],
});

export const payments = crud({
  title: 'Payments', subtitle: 'Every card, cash and transfer payment recorded against an order.', list: '/payment/list', pageSize: 15,
  columns: [{ key: 'orderSn', label: 'Order' }, { label: 'Method', html: r => esc(pretty(r.method)) }, { label: 'Status', html: r => badge(r.status) }, { label: 'Amount', num: true, html: r => money(r.amount) }, { label: 'Paid', get: r => dt(r.paidTime) }, { key: 'transactionRef', label: 'Reference' }],
});
