import { api, session, requireLogin, refreshSession, emptyState, statusBadge } from '/app/core.js';
import { esc, money, dt, day, $, $$, toast, fail, modal, confirmDialog, pager, hashQ, pretty, readForm } from '/shared/ui.js';

const TABS = [['', 'All'], ['PENDING_PAYMENT', 'To pay'], ['PREPARING', 'Preparing'], ['READY_FOR_PICKUP', 'Ready'], ['SHIPPED', 'On the way'], ['COMPLETED', 'Completed'], ['CANCELLED', 'Cancelled']];

export async function ordersPage({ params }) {
  if (!requireLogin()) return '';
  const page = Number(params.page || 1);
  const r = await api.get('/order/list', { status: params.status, pageNum: page, pageSize: 6 });
  const el = document.createElement('div');
  el.innerHTML = `<h1>My orders</h1>
    <div class="tabs">${TABS.map(([k, l]) => `<a class="tab ${(params.status || '') === k ? 'active' : ''}" href="#/orders${hashQ({ status: k })}">${l}</a>`).join('')}</div>
    ${r.list.length ? r.list.map(v => `<div class="panel" style="margin-bottom:14px">
      <div class="row between"><div><a href="#/orders/${v.order.id}"><b>${esc(v.order.orderSn)}</b></a> <span class="small muted">${dt(v.order.createTime)}</span></div>
        <div class="row">${statusBadge(v.order.status)}${v.order.preorder ? '<span class="badge ruby">Preorder</span>' : ''}<b>${money(v.order.payAmount)}</b></div></div>
      <div class="small muted" style="margin:8px 0">${v.items.map(i => `${i.productQuantity} × ${esc(i.productName)}${i.skuLabel && i.skuLabel !== 'Standard' ? ' (' + esc(i.skuLabel) + ')' : ''}`).join(' · ')}</div>
      <div class="row"><a class="btn small ghost" href="#/orders/${v.order.id}">Details</a>${v.order.status === 'PENDING_PAYMENT' ? `<a class="btn small primary" href="#/pay/${v.order.id}">Pay now</a>` : ''}</div></div>`).join('')
      : emptyState('📦', 'No orders here', 'When you order, it will show up here.', '<a class="btn primary" href="#/shop">Start shopping</a>')}`;
  el.appendChild(pager(page, r.totalPage, n => { location.hash = '#/orders' + hashQ({ ...params, page: n }); }));
  return el;
}

export async function orderPage({ args }) {
  if (!requireLogin()) return '';
  const [d, info] = await Promise.all([api.get('/order/detail/' + args.id), api.get('/shop/info')]);
  const o = d.order;
  const el = document.createElement('div');
  const canReturn = o.status === 'COMPLETED' && o.receiveTime && (Date.now() - new Date(o.receiveTime).getTime()) < info.returnWindowDays * 86400000;
  el.innerHTML = `
  <p class="small muted"><a href="#/orders">‹ My orders</a></p>
  <div class="row between"><h1 style="margin:0">Order ${esc(o.orderSn)}</h1><div class="row">${statusBadge(o.status)}${o.preorder ? '<span class="badge ruby">Preorder</span>' : ''}</div></div>
  <div class="grid cols-2" style="margin-top:16px">
    <div class="panel"><h3>Items</h3>${d.items.map(i => `
      <div class="line" style="grid-template-columns:1fr auto"><div><b><a href="#/product/${i.productId}">${esc(i.productName)}</a></b> ${i.skuLabel && i.skuLabel !== 'Standard' ? `<span class="badge">${esc(i.skuLabel)}</span>` : ''}
        <div class="small muted">${i.productQuantity} × ${money(i.productPrice)}</div>
        ${i.boxSelection ? '<div class="small muted">Build-a-box selection included</div>' : ''}
        ${canReturn ? `<button class="btn small ghost" data-return="${i.productId}" data-name="${esc(i.productName)}" data-max="${i.productQuantity}" style="margin-top:6px">Return / refund</button>` : ''}</div>
        <div class="right"><b>${money(i.realAmount)}</b></div></div>`).join('')}
      <div class="total-row"><span>Goods</span><span>${money(o.totalAmount)}</span></div>
      ${Number(o.promotionAmount) ? `<div class="total-row"><span>Bundle price</span><span>−${money(o.promotionAmount)}</span></div>` : ''}
      ${Number(o.couponAmount) ? `<div class="total-row"><span>Coupon</span><span>−${money(o.couponAmount)}</span></div>` : ''}
      ${Number(o.integrationAmount) ? `<div class="total-row"><span>Points (${o.useIntegration})</span><span>−${money(o.integrationAmount)}</span></div>` : ''}
      ${Number(o.freightAmount) ? `<div class="total-row"><span>Delivery</span><span>${money(o.freightAmount)}</span></div>` : ''}
      ${Number(o.giftPackagingAmount) ? `<div class="total-row"><span>Gift sleeve</span><span>${money(o.giftPackagingAmount)}</span></div>` : ''}
      <div class="total-row big"><span>Total</span><span>${money(o.payAmount)}</span></div></div>
    <div class="stack">
      <div class="panel"><h3>Details</h3><dl class="kv">
        <dt>Placed</dt><dd>${dt(o.createTime)}</dd>
        <dt>${o.fulfillmentType === 'PICKUP' ? 'Pickup' : 'Delivery'}</dt><dd>${o.fulfillmentType === 'PICKUP' ? (o.pickupSlotStart ? dt(o.pickupSlotStart) : 'As soon as it is ready') : esc([o.receiverName, o.receiverDetailAddress, o.receiverCity].filter(Boolean).join(', '))}</dd>
        ${o.deliverySn ? `<dt>Tracking</dt><dd>${esc(o.deliveryCompany)} ${esc(o.deliverySn)}</dd>` : ''}
        ${o.giftMessage ? `<dt>Gift message</dt><dd>${esc(o.giftMessage)}</dd>` : ''}
        ${o.note ? `<dt>Your note</dt><dd>${esc(o.note)}</dd>` : ''}
        ${o.integrationEarned ? `<dt>Points earned</dt><dd>${o.integrationEarned}</dd>` : ''}
        ${o.cancelReason ? `<dt>Cancelled</dt><dd>${esc(o.cancelReason)}</dd>` : ''}</dl>
        <div class="row" style="margin-top:12px">
          ${o.status === 'PENDING_PAYMENT' ? `<a class="btn primary" href="#/pay/${o.id}">Pay now</a><button class="btn ghost" id="cancel">Cancel order</button>` : ''}
          ${['SHIPPED', 'READY_FOR_PICKUP'].includes(o.status) ? `<button class="btn primary" id="receive">${o.status === 'SHIPPED' ? 'I received it' : 'I collected it'}</button>` : ''}
          ${['COMPLETED', 'CANCELLED', 'INVALID'].includes(o.status) ? '<button class="btn ghost" id="remove">Remove from list</button>' : ''}</div></div>
      <div class="panel"><h3>Progress</h3><ul class="timeline">${d.timeline.map(t => `<li><b>${esc(t.note || pretty(t.orderStatus))}</b><div class="small muted">${dt(t.createTime)} · ${esc(t.operator || '')}</div></li>`).join('')}</ul></div>
      ${d.payments.length ? `<div class="panel"><h3>Payments</h3>${d.payments.map(p => `<div class="total-row small"><span>${pretty(p.method)} · ${statusBadge(p.status)}</span><span>${money(p.amount)}</span></div>`).join('')}</div>` : ''}
    </div></div>`;

  $('#cancel', el)?.addEventListener('click', async () => {
    if (!(await confirmDialog('Cancel this order?', 'Your items go back on the shelf and any coupon or points are returned.', 'Cancel order'))) return;
    try { await api.post('/order/cancel/' + o.id, {}); await refreshSession(); toast('Order cancelled', 'ok'); location.reload(); } catch (e) { fail(e); }
  });
  $('#receive', el)?.addEventListener('click', async () => { try { await api.post('/order/confirmReceipt/' + o.id); toast('Thank you!', 'ok'); location.reload(); } catch (e) { fail(e); } });
  $('#remove', el)?.addEventListener('click', async () => { try { await api.post('/order/delete/' + o.id); location.hash = '#/orders'; } catch (e) { fail(e); } });
  $$('[data-return]', el).forEach(b => b.addEventListener('click', async () => {
    const reasons = await api.get('/returnApply/reasons');
    const form = document.createElement('form');
    form.innerHTML = `<div class="field"><label>Quantity</label><input name="quantity" type="number" min="1" max="${b.dataset.max}" value="1" data-type="int"></div>
      <div class="field"><label>Reason</label><select name="reason">${reasons.map(r => `<option>${esc(r.name)}</option>`).join('')}</select></div>
      <div class="field"><label>What happened?</label><textarea name="description" maxlength="500"></textarea></div>`;
    modal({ title: 'Return: ' + b.dataset.name, body: form, actions: [{ label: 'Cancel', kind: 'ghost' }, {
      label: 'Send request', kind: 'primary', run: async () => {
        const f = readForm(form);
        await api.post('/returnApply/create', { orderId: o.id, productId: Number(b.dataset.return), quantity: f.quantity, reason: f.reason, description: f.description });
        toast('Return request sent. We will reply soon.', 'ok');
      },
    }] });
  }));
  return el;
}
