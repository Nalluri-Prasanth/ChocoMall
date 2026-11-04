import { api, rowsOf, idMap, enumOptions, ENUMS, table, pageHead, buildForm, openForm, yesNo, badge, esc, $, $$, money, dt, toast, fail, modal, pager, hashQ, pretty, session } from '/app/core.js';

const reload = () => window.dispatchEvent(new HashChangeEvent('hashchange'));
const iso = d => d.toISOString().slice(0, 10);

// ------------------------------------------------------------------ dashboard
export async function dashboard() {
  const [d, daily, top, brandSummary, progress] = await Promise.all([
    api.get('/report/dashboard'),
    api.get('/report/sales/daily', { from: iso(new Date(Date.now() - 13 * 864e5)), to: iso(new Date()) }),
    api.get('/report/products/top', { from: iso(new Date(Date.now() - 29 * 864e5)), to: iso(new Date()), limit: 5 }),
    api.get('/brand/stocking/summary').catch(() => null),
    api.get('/guide/progress').catch(() => null),
  ]);
  const max = Math.max(1, ...daily.map(x => Number(x.revenue)));
  const el = document.createElement('div');
  const stat = (num, label, href) => `<a class="stat panel" ${href ? `href="${href}"` : ''} style="color:inherit"><div class="num">${num}</div><div class="label">${label}</div></a>`;
  el.innerHTML = pageHead(`Hello, ${session.info?.nickName || session.info?.username || 'there'}`, 'Here is what needs you today.')
    + `<div class="grid stats">${stat(money(d.revenueToday), 'Revenue today')}${stat(d.ordersToday, 'Paid orders today', '#/orders')}${stat(money(d.revenueLast7Days), 'Last 7 days')}${stat(money(d.revenueLast30Days), 'Last 30 days')}
       ${stat(d.lowStockProducts, 'Items low on stock', '#/stock?lowOnly=true')}${stat(d.pendingReturns, 'Returns to review', '#/returns?status=PENDING')}${stat(d.openPersonalizationRequests, 'Personalised requests open', '#/personalization')}${stat(d.upcomingSessions, 'Upcoming sessions', '#/sessions')}</div>
     <div class="grid cols-2" style="margin-top:18px"><div class="panel"><h3>Order queue</h3>${Object.entries(d.orderQueue).map(([k, v]) => `<div class="total-row"><a href="#/orders?status=${k}">${esc(pretty(k))}</a><b>${v}</b></div>`).join('')}</div>
     <div class="panel"><h3>Revenue, last 14 days</h3>${daily.map(x => `<div class="bar-row"><span class="small muted" style="width:70px">${x.date.slice(5)}</span><div class="bar"><span style="width:${(Number(x.revenue) / max) * 100}%"></span></div><span class="small" style="width:70px;text-align:right">${money(x.revenue)}</span></div>`).join('')}</div>
     <div class="panel"><h3>Best sellers (30 days)</h3>${top.length ? top.map(t => `<div class="total-row"><span>${esc(t.name)} <span class="muted small">× ${t.units}</span></span><b>${money(t.revenue)}</b></div>`).join('') : '<p class="muted">No sales yet.</p>'}</div>
     <div class="panel"><h3>Launch progress</h3>${progress ? Object.entries(progress).map(([k, v]) => `<div class="bar-row"><span class="small" style="width:110px">${esc(pretty(k))}</span><div class="bar"><span style="width:${v.percentDone}%"></span></div><span class="small" style="width:70px;text-align:right">${v.done}/${v.total}</span></div>`).join('') + '<p class="small"><a href="#/playbook">Open the playbook →</a></p>' : ''}
     ${brandSummary ? `<p class="small muted">Brands: ${Object.entries(brandSummary.byStatus).map(([k, v]) => `${v} ${pretty(k).toLowerCase()}`).join(' · ')}. Suggested start: ${esc(brandSummary.recommendedStartingRange)}.</p>` : ''}</div></div>`;
  return el;
}

// ------------------------------------------------------------------ reports
export async function reportsPage({ params }) {
  const from = params.from || iso(new Date(Date.now() - 29 * 864e5)), to = params.to || iso(new Date());
  const [contrib, daily] = await Promise.all([api.get('/report/contribution', { from, to }), api.get('/report/sales/daily', { from, to })]);
  const el = document.createElement('div');
  const total = daily.reduce((a, x) => a + Number(x.revenue), 0), orders = daily.reduce((a, x) => a + x.orders, 0);
  const totalContribution = contrib.reduce((a, x) => a + Number(x.contribution), 0);
  el.innerHTML = pageHead('Reports', 'Contribution is what is left of each sale after ingredients, packaging, labour and selling fees.')
    + `<form class="filters" id="f"><label>From <input type="date" name="from" value="${from}"></label><label>To <input type="date" name="to" value="${to}"></label><button class="btn small">Update</button></form>
       <div class="grid stats"><div class="stat panel"><div class="num">${money(total)}</div><div class="label">Revenue</div></div><div class="stat panel"><div class="num">${orders}</div><div class="label">Paid orders</div></div>
       <div class="stat panel"><div class="num">${money(totalContribution)}</div><div class="label">Contribution</div></div><div class="stat panel"><div class="num">${total ? Math.round(totalContribution / total * 100) : 0}%</div><div class="label">Margin</div></div></div>
       <h3 style="margin-top:20px">Contribution by product</h3>`
    + table([{ label: 'Product', key: 'name' }, { label: 'Units', key: 'units', num: true }, { label: 'Revenue', num: true, html: r => money(r.revenue) }, { label: 'Costs', num: true, html: r => money(r.costs) }, { label: 'Fees', num: true, html: r => money(r.sellingFees) },
      { label: 'Contribution', num: true, html: r => `<b>${money(r.contribution)}</b>` }, { label: 'Margin', num: true, get: r => r.marginPercent + '%' }, { label: '', html: r => r.costsComplete ? '' : '<span class="badge warn">costs missing</span>' }], contrib, 'No sales in this period.');
  $('#f', el).addEventListener('submit', e => { e.preventDefault(); location.hash = '#/reports' + hashQ(Object.fromEntries(new FormData(e.target))); });
  return el;
}

// ------------------------------------------------------------------ stock
export async function stockPage({ params }) {
  const page = Number(params.page || 1);
  const data = await api.get('/stock/list', { keyword: params.keyword, lowOnly: params.lowOnly === 'true' ? true : undefined, pageNum: page, pageSize: 15 });
  const el = document.createElement('div');
  el.innerHTML = pageHead('Stock', 'Every change is written to a ledger. Use “Adjust” to add or remove pieces and “Count” after a stock-take.')
    + `<form class="filters" id="f"><input name="keyword" placeholder="Search" value="${esc(params.keyword || '')}"><label class="check"><input type="checkbox" name="lowOnly" value="true" ${params.lowOnly ? 'checked' : ''}> Low stock only</label><button class="btn small">Filter</button></form>`
    + table([{ label: 'Code', key: 'productSn' }, { label: 'Product', key: 'name' }, { label: 'On hand', key: 'stock', num: true }, { label: 'Reserved', key: 'locked', num: true }, { label: 'Available', num: true, html: r => `<b class="${r.low ? 'badge warn' : ''}">${r.available}</b>` },
      { label: 'Warn at', key: 'lowStock', num: true }, { label: '', cls: 'actions nowrap', html: (r, i) => `<button class="btn small ghost" data-adj="${i}">Adjust</button> <button class="btn small ghost" data-cnt="${i}">Count</button> <button class="btn small ghost" data-led="${i}">Ledger</button>` }], data.list, 'No products.');
  if (data.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = '#/stock' + hashQ({ ...params, page: n }); }));
  $('#f', el).addEventListener('submit', e => { e.preventDefault(); const f = {}; new FormData(e.target).forEach((v, k) => { if (v) f[k] = v; }); location.hash = '#/stock' + hashQ(f); });
  $$('[data-adj]', el).forEach(b => b.addEventListener('click', () => { const r = data.list[Number(b.dataset.adj)]; openForm({ title: `Adjust ${r.name}`, wide: false, fields: [
    { name: 'delta', label: 'Pieces to add (negative to remove)', type: 'int', required: true }, { name: 'reason', label: 'Reason', type: 'select', required: true, options: enumOptions(ENUMS.StockReason), def: 'ADJUSTMENT' }, { name: 'reference', label: 'Reference (optional)', maxlength: 64 }],
    submit: v => api.post('/stock/adjust', { productId: r.productId, ...v }), after: reload }); }));
  $$('[data-cnt]', el).forEach(b => b.addEventListener('click', () => { const r = data.list[Number(b.dataset.cnt)]; openForm({ title: `Count ${r.name}`, wide: false, values: { counted: r.stock }, fields: [{ name: 'counted', label: 'Pieces counted on the shelf', type: 'int', required: true, min: 0, hint: `The system says ${r.stock}.` }], submit: v => api.post('/stock/count', { productId: r.productId, counted: v.counted }), after: reload }); }));
  $$('[data-led]', el).forEach(b => b.addEventListener('click', async () => {
    const r = data.list[Number(b.dataset.led)];
    try { const led = await api.get('/stock/ledger/' + r.productId, { pageSize: 30 }); modal({ title: `Ledger: ${r.name}`, wide: true, body: table([{ label: 'When', get: x => dt(x.createTime) }, { label: 'Change', num: true, get: x => (x.delta > 0 ? '+' : '') + x.delta }, { label: 'Reason', get: x => pretty(x.reason) }, { label: 'Balance', key: 'balanceAfter', num: true }, { label: 'Reference', key: 'reference' }, { label: 'By', key: 'operator' }], rowsOf(led), 'No movements yet.') }); } catch (e) { fail(e); }
  }));
  return el;
}

// ------------------------------------------------------------------ production
export async function productionPage({ params }) {
  const page = Number(params.page || 1);
  const [data, products] = await Promise.all([api.get('/production/list', { status: params.status, pageNum: page, pageSize: 12 }), api.get('/product/list', { pageSize: 500 }).then(rowsOf)]);
  const name = id => products.find(p => p.id === id)?.name || '#' + id;
  const el = document.createElement('div');
  el.innerHTML = pageHead('Production batches', 'A batch can only be released to stock after the allergen check, a temper test and a best-before date.', '<button class="btn primary" id="add">+ Plan a batch</button><button class="btn ghost" id="waste">Waste report</button>')
    + table([{ label: 'Batch', key: 'batchCode' }, { label: 'Product', get: r => name(r.productId) }, { label: 'Units', key: 'quantityUnits', num: true }, { label: 'Waste', key: 'wasteUnits', num: true }, { label: 'Best before', key: 'bestBefore' },
      { label: 'Checks', html: r => `${r.allergenChecked ? '✔ allergens' : '✘ allergens'} · ${r.temperTestPassed == null ? '— temper' : r.temperTestPassed ? '✔ temper' : '✘ temper'}` }, { label: 'Status', html: r => badge(r.status) },
      { label: '', cls: 'actions nowrap', html: (r, i) => `${r.status === 'PLANNED' ? `<button class="btn small primary" data-start="${i}">Start</button>` : ''}${r.status === 'IN_PROGRESS' ? `<button class="btn small primary" data-rel="${i}">Release</button> <button class="btn small ghost" data-rej="${i}">Reject</button>` : ''}` }], data.list, 'No batches yet.');
  if (data.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = '#/production' + hashQ({ ...params, page: n }); }));
  $('#add', el).addEventListener('click', () => openForm({ title: 'Plan a batch', wide: false, fields: [
    { name: 'productId', label: 'Product', type: 'select', required: true, options: products.filter(p => ['MADE_IN_SHOP', 'MADE_FRESH', 'MADE_TO_ORDER'].includes(p.kind)).map(p => ({ value: p.id, label: p.name })) },
    { name: 'quantityUnits', label: 'Pieces to make', type: 'int', required: true, min: 1 }, { name: 'processCode', label: 'Process code (from the playbook)', maxlength: 10 }, { name: 'producedAt', label: 'Planned for', type: 'datetime' }, { name: 'note', label: 'Note', type: 'textarea', full: true }],
    submit: v => api.post('/production/create', v), after: reload }));
  $$('[data-start]', el).forEach(b => b.addEventListener('click', async () => { try { await api.post(`/production/${data.list[Number(b.dataset.start)].id}/start`); reload(); } catch (e) { fail(e); } }));
  $$('[data-rel]', el).forEach(b => b.addEventListener('click', () => { const r = data.list[Number(b.dataset.rel)]; openForm({ title: `Release ${r.batchCode}`, wide: false, label: 'Release to stock', values: { allergenChecked: r.allergenChecked }, fields: [
    { name: 'allergenChecked', label: 'Allergens checked against the recipe and labels', type: 'bool', full: true }, { name: 'temperTestPassed', label: 'Temper test passed', type: 'select', options: [{ value: 'true', label: 'Passed' }, { value: 'false', label: 'Failed' }], hint: 'Required for tempered products.' },
    { name: 'bestBefore', label: 'Best before', type: 'date' }, { name: 'wasteUnits', label: 'Pieces wasted', type: 'int', min: 0, def: 0 }],
    submit: v => api.post(`/production/${r.id}/release`, { ...v, temperTestPassed: v.temperTestPassed === undefined ? undefined : v.temperTestPassed === 'true' }), after: reload }); }));
  $$('[data-rej]', el).forEach(b => b.addEventListener('click', () => { const r = data.list[Number(b.dataset.rej)]; openForm({ title: `Reject ${r.batchCode}`, wide: false, label: 'Reject', fields: [{ name: 'reason', label: 'Why', required: true, maxlength: 500, type: 'textarea', full: true }], submit: v => api.post(`/production/${r.id}/reject`, v), after: reload }); }));
  $('#waste', el).addEventListener('click', async () => {
    try { const w = await api.get('/production/report/waste'); modal({ title: 'Waste against released output', wide: true, body: table(Object.keys(rowsOf(w)[0] || { product: 1 }).map(k => ({ label: pretty(k), key: k })), rowsOf(w), 'No waste recorded.') }); } catch (e) { fail(e); }
  });
  return el;
}

// ------------------------------------------------------------------ playbook
const GUIDE_TYPES = [['SHOP_CONCEPT', 'Shop concept'], ['PRODUCTION', 'Production'], ['PRINTING', 'Printing'], ['SALES_LAUNCH', 'Sales launch'], ['SHOP_FORMAT', 'Shop format'], ['ROADMAP', 'Roadmap']];
export async function playbookPage({ params }) {
  const type = params.type || 'SALES_LAUNCH';
  const [items, progress, sources] = await Promise.all([api.get('/guide/list', { type }), api.get('/guide/progress'), api.get('/guide/sources').catch(() => [])]);
  const el = document.createElement('div');
  el.innerHTML = pageHead('Launch playbook', 'The shop guide as a checklist. Mark items as you complete them.')
    + `<div class="tabs">${GUIDE_TYPES.map(([k, l]) => `<a class="tab ${k === type ? 'active' : ''}" href="#/playbook?type=${k}">${l} <span class="muted small">${progress[k]?.done ?? 0}/${progress[k]?.total ?? 0}</span></a>`).join('')}</div>`
    + table([{ label: 'Code', key: 'code' }, { label: 'Phase', key: 'phase' }, { label: 'Item', html: r => `<b>${esc(r.title)}</b><div class="small muted">${esc(r.detail || '')}</div>` }, { label: 'Why / control', html: r => `<div class="small">${esc(r.purpose || '')}</div><div class="small muted">${esc(r.controlNote || '')}</div>` },
      { label: 'Status', html: (r, i) => `<select data-st="${i}" style="width:auto">${ENUMS.GuideStatus.map(s => `<option value="${s}" ${r.status === s ? 'selected' : ''}>${pretty(s)}</option>`).join('')}</select>` }], items, 'Nothing in this section.')
    + (sources.length ? `<h3 style="margin-top:24px">Sources checked</h3>${table([{ label: 'Source', html: s => s.referenceUrl ? `<a href="${esc(s.referenceUrl)}" target="_blank" rel="noopener">${esc(s.source)}</a>` : esc(s.source) }, { label: 'Used for', key: 'scope' }, { label: 'Checked', key: 'checkedOn' }], sources)}<p class="small muted">Always confirm food-safety and labelling rules with your local authority.</p>` : '');
  $$('[data-st]', el).forEach(s => s.addEventListener('change', async () => { try { await api.post('/guide/update/' + items[Number(s.dataset.st)].id, { status: s.value }); toast('Saved', 'ok'); } catch (e) { fail(e); reload(); } }));
  return el;
}
