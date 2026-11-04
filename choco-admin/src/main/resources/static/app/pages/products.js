import { api, rowsOf, idMap, enumOptions, ENUMS, table, pageHead, buildForm, collect, fieldHtml, yesNo, thumb, badge, esc, $, $$, money, toast, fail, modal, confirmDialog, pager, hashQ, pretty, readForm } from '/app/core.js';

const reload = () => window.dispatchEvent(new HashChangeEvent('hashchange'));

export async function productsPage({ params }) {
  const page = Number(params.page || 1);
  const [cats, brands] = await Promise.all([idMap('/productCategory/list/0'), idMap('/brand/list', 'name', { showStatus: true })]);
  const q = { keyword: params.keyword, categoryId: params.categoryId, kind: params.kind, launchTier: params.launchTier, published: params.published, lowStock: params.lowStock, pageNum: page, pageSize: 12 };
  const data = await api.get('/product/list', q);
  const rows = rowsOf(data);
  const el = document.createElement('div');
  const sel = (name, label, opts) => `<select name="${name}"><option value="">${label}</option>${opts.map(o => `<option value="${esc(o.value)}" ${String(params[name]) === String(o.value) ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>`;
  el.innerHTML = pageHead('Products', 'Everything you sell. A product needs reviewed allergens and at least one active size before it can be published.', '<button class="btn primary" id="add">+ Add product</button>')
    + `<form class="filters" id="flt"><input name="keyword" placeholder="Search name or code" value="${esc(params.keyword || '')}">${sel('categoryId', 'All categories', cats)}${sel('kind', 'Any kind', enumOptions(ENUMS.ProductKind))}
       ${sel('launchTier', 'Any tier', enumOptions(ENUMS.Priority))}${sel('published', 'Any visibility', [{ value: 'true', label: 'Published' }, { value: 'false', label: 'Hidden' }])}
       <label class="check"><input type="checkbox" name="lowStock" value="true" ${params.lowStock ? 'checked' : ''}> Low stock</label><button class="btn small">Filter</button></form>`
    + table([
      { label: '', html: r => thumb(r.pic, r.name) }, { label: 'Code', key: 'productSn' }, { label: 'Product', html: r => `<b>${esc(r.name)}</b><div class="muted mini">${esc(r.subTitle || '')}</div>` },
      { label: 'Kind', html: r => esc(pretty(r.kind)) }, { label: 'Tier', html: r => badge(r.launchTier) }, { label: 'Price', num: true, html: r => money(r.price) },
      { label: 'Stock', num: true, html: r => `<span class="${r.stock - r.lockStock <= r.lowStock ? 'badge warn' : ''}">${r.stock - r.lockStock}</span>` },
      { label: 'Live', html: r => `<label class="check" style="margin:0"><input type="checkbox" data-pub="${r.id}" ${r.published ? 'checked' : ''}> </label>` },
      { label: '', cls: 'actions nowrap', html: (r, i) => `<button class="btn small ghost" data-open="${i}">Open</button> <button class="btn small ghost" data-edit="${i}">Edit</button>` },
    ], rows, 'No products match. Add your first product.');
  if (data.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = '#/products' + hashQ({ ...params, page: n }); }));

  $('#flt', el).addEventListener('submit', e => {
    e.preventDefault();
    const f = {}; new FormData(e.target).forEach((v, k) => { if (v) f[k] = v; }); location.hash = '#/products' + hashQ(f);
  });
  $('#add', el).addEventListener('click', () => productForm(null));
  $$('[data-edit]', el).forEach(b => b.addEventListener('click', () => productForm(rows[Number(b.dataset.edit)].id)));
  $$('[data-open]', el).forEach(b => b.addEventListener('click', () => { location.hash = '#/products/' + rows[Number(b.dataset.open)].id; }));
  $$('[data-pub]', el).forEach(c => c.addEventListener('change', async () => {
    try { await api.post('/product/update/publishStatus', { ids: [Number(c.dataset.pub)], value: c.checked }); toast(c.checked ? 'Published' : 'Hidden from the shop', 'ok'); }
    catch (e) { c.checked = !c.checked; fail(e); }
  }));
  return el;
}

// ------------------------------------------------------------------ create / edit
async function productForm(id) {
  const [cats, brands, types, flavors, allergens] = await Promise.all([
    api.get('/productCategory/list/withChildren'), idMap('/brand/list', 'name'), idMap('/chocolateType/list'), idMap('/flavor/list'), idMap('/allergen/list'),
  ]);
  const flat = [];
  (function walk(nodes, depth) { nodes.forEach(n => { flat.push({ value: n.id, label: '— '.repeat(depth) + n.name }); walk(n.children || [], depth + 1); }); })(cats, 0);
  let d = null;
  if (id) d = await api.get('/product/' + id);
  const p = d?.product || {};
  const skus = d ? d.skus.map(s => ({ skuCode: s.skuCode, label: s.label, units: s.units, price: s.price, active: s.active })) : [{ skuCode: '', label: 'Standard', units: 1, price: '', active: true }];
  const fields = [
    { name: 'name', label: 'Name', required: true, def: p.name }, { name: 'productSn', label: 'Product code', required: true, maxlength: 20, def: p.productSn, hint: 'Unique, e.g. P101.' },
    { name: 'categoryId', label: 'Category', type: 'select', required: true, options: flat, def: p.categoryId },
    { name: 'kind', label: 'How it is made', type: 'select', required: true, options: enumOptions(ENUMS.ProductKind), def: p.kind || 'MADE_IN_SHOP' },
    { name: 'launchTier', label: 'Launch tier', type: 'select', required: true, options: enumOptions(ENUMS.Priority), def: p.launchTier || 'CORE', hint: 'Core = the first launch range.' },
    { name: 'brandId', label: 'Brand (for resold products)', type: 'select', options: brands, def: p.brandId }, { name: 'chocolateTypeId', label: 'Chocolate type', type: 'select', options: types, def: p.chocolateTypeId },
    { name: 'price', label: 'Price', type: 'number', required: true, min: 0.01, def: p.price }, { name: 'originalPrice', label: 'Was price (optional)', type: 'number', def: p.originalPrice },
    { name: 'unit', label: 'Unit', def: p.unit || 'piece' }, { name: 'weightGrams', label: 'Weight (g)', type: 'int', def: p.weightGrams }, { name: 'shelfLifeDays', label: 'Shelf life (days)', type: 'int', min: 1, def: p.shelfLifeDays },
    { name: 'lowStock', label: 'Low-stock warning at', type: 'int', min: 0, def: p.lowStock ?? 10 }, ...(id ? [] : [{ name: 'initialStock', label: 'Opening stock', type: 'int', min: 0, def: 0 }]),
    { name: 'subTitle', label: 'Short line', full: true, maxlength: 300, def: p.subTitle }, { name: 'description', label: 'Description', type: 'textarea', full: true, def: p.description },
    { name: 'pic', label: 'Photo', type: 'image', def: p.pic }, { name: 'keywords', label: 'Search keywords', def: p.keywords },
    { name: 'sellingFormat', label: 'Selling format', def: p.sellingFormat }, { name: 'salesPurpose', label: 'Purpose', def: p.salesPurpose },
    { name: 'ingredientCost', label: 'Ingredient cost / unit', type: 'number', min: 0, def: p.ingredientCost }, { name: 'packagingCost', label: 'Packaging cost / unit', type: 'number', min: 0, def: p.packagingCost },
    { name: 'laborCost', label: 'Labour cost / unit', type: 'number', min: 0, def: p.laborCost }, { name: 'sellingFeeRate', label: 'Selling fee rate (0-1)', type: 'number', min: 0, step: '0.001', def: p.sellingFeeRate },
    { name: 'flavorIds', label: 'Flavours', type: 'ids', options: flavors, def: d?.flavors.map(f => f.id) || [] },
    { name: 'allergenIds', label: 'Contains allergens', type: 'ids', options: allergens, def: d?.allergens.map(a => a.id) || [] },
    { name: 'allergensReviewed', label: 'Allergens reviewed by a person', type: 'bool', def: p.allergensReviewed, full: true, hint: 'Required before publishing.' },
    { name: 'preorderOnly', label: 'Preorder only', type: 'bool', def: p.preorderOnly }, { name: 'buildABox', label: 'Can go in a build-a-box', type: 'bool', def: p.buildABox }, { name: 'requiresColdChain', label: 'Keep cold', type: 'bool', def: p.requiresColdChain },
  ];
  const form = buildForm(fields, Object.fromEntries(fields.map(f => [f.name, f.def])));
  const skuBox = document.createElement('div');
  const draw = () => {
    skuBox.innerHTML = `<h4 style="margin:16px 0 6px">Sizes and prices</h4><div class="muted small" style="margin-bottom:8px">Each product needs at least one. Units is how many pieces the size contains (stock is counted in pieces).</div>
      ${skus.map((s, i) => `<div class="sku-row"><div class="field"><label>SKU code</label><input data-i="${i}" data-k="skuCode" value="${esc(s.skuCode)}" required maxlength="50"></div>
        <div class="field"><label>Label</label><input data-i="${i}" data-k="label" value="${esc(s.label)}" required placeholder="Box of 9"></div>
        <div class="field"><label>Units</label><input data-i="${i}" data-k="units" type="number" min="1" value="${esc(s.units)}" required></div>
        <div class="field"><label>Price</label><input data-i="${i}" data-k="price" type="number" step="0.01" min="0.01" value="${esc(s.price)}" required></div>
        <button type="button" class="btn small ghost" data-rm="${i}" ${skus.length < 2 ? 'disabled' : ''}>✕</button></div>`).join('')}
      <button type="button" class="btn small ghost" id="more">+ Add size</button>`;
    $$('[data-i]', skuBox).forEach(inp => inp.addEventListener('input', () => { skus[inp.dataset.i][inp.dataset.k] = inp.type === 'number' ? (inp.value === '' ? '' : Number(inp.value)) : inp.value; }));
    $$('[data-rm]', skuBox).forEach(b => b.addEventListener('click', () => { skus.splice(Number(b.dataset.rm), 1); draw(); }));
    $('#more', skuBox).addEventListener('click', () => { skus.push({ skuCode: '', label: '', units: 1, price: '', active: true }); draw(); });
  };
  draw();
  const wrap = document.createElement('div'); wrap.append(form, skuBox);
  // keep the default size price and the product price in step for new products
  const priceInput = $('[name=price]', form);
  priceInput.addEventListener('input', () => { if (!id && skus.length === 1 && (skus[0].price === '' || skus[0].autofilled)) { skus[0].price = Number(priceInput.value) || ''; skus[0].autofilled = true; draw(); } });
  const nameInput = $('[name=name]', form), snInput = $('[name=productSn]', form);
  snInput.addEventListener('input', () => { if (!id && skus.length === 1 && (!skus[0].skuCode || skus[0].codeAuto)) { skus[0].skuCode = snInput.value + '-1'; skus[0].codeAuto = true; draw(); } });

  modal({
    title: id ? `Edit ${p.name}` : 'Add product', body: wrap, wide: true, actions: [{ label: 'Cancel', kind: 'ghost' }, {
      label: id ? 'Save changes' : 'Create product', kind: 'primary', run: async () => {
        if (!form.reportValidity()) return true;
        if ($$('.sku-row input', skuBox).some(i => !i.checkValidity())) { $$('.sku-row input', skuBox).find(i => !i.checkValidity()).reportValidity(); return true; }
        const v = collect(form, fields);
        const body = { ...v, skus: skus.map((s, i) => ({ skuCode: s.skuCode, label: s.label, units: Number(s.units), price: Number(s.price), active: s.active ?? true, sort: i })) };
        await api.post(id ? `/product/update/${id}` : '/product/create', body);
        toast(id ? 'Product saved' : 'Product created. Publish it from the list when it is ready.', 'ok'); reload();
      },
    }],
  });
}

// ------------------------------------------------------------------ detail page
export async function productDetail({ args }) {
  const [d, contribution] = await Promise.all([api.get('/product/' + args.id), api.get(`/product/${args.id}/contribution`).catch(() => null)]);
  const p = d.product;
  const el = document.createElement('div');
  el.innerHTML = `<p class="small muted"><a href="#/products">‹ Products</a></p>${pageHead(p.name, `${p.productSn} · ${d.categoryName || ''}${d.brandName ? ' · ' + d.brandName : ''}`,
    `<button class="btn ghost" id="edit">Edit</button><button class="btn ${p.published ? 'ghost' : 'primary'}" id="pub">${p.published ? 'Hide from shop' : 'Publish'}</button><a class="btn ghost" target="_blank" href="http://localhost:8085/#/product/${p.id}">View in shop ↗</a>`)}
    <div class="grid cols-2"><div class="stack">
      <div class="panel"><h3>Overview</h3><dl class="kv"><dt>Status</dt><dd>${p.published ? '<span class="badge ok">Live</span>' : '<span class="badge warn">Hidden</span>'} ${badge(p.launchTier)}</dd>
        <dt>Kind</dt><dd>${esc(pretty(p.kind))}</dd><dt>Price</dt><dd>${money(p.price)}</dd><dt>In stock</dt><dd>${d.available} available (${p.stock} on hand, ${p.lockStock} reserved)</dd>
        <dt>Allergens</dt><dd>${d.allergens.length ? d.allergens.map(a => `<span class="badge warn">${esc(a.name)}</span>`).join(' ') : 'None listed'} ${p.allergensReviewed ? '<span class="badge ok">Reviewed</span>' : '<span class="badge danger">Not reviewed</span>'}</dd>
        <dt>Flavours</dt><dd>${d.flavors.map(f => esc(f.name)).join(', ') || '—'}</dd></dl></div>
      <div class="panel"><h3>Sizes</h3>${table([{ label: 'Code', key: 'skuCode' }, { label: 'Label', key: 'label' }, { label: 'Units', key: 'units', num: true }, { label: 'Price', num: true, html: s => money(s.price) }, { label: 'Active', html: s => yesNo(s.active) }], d.skus)}</div></div>
    <div class="stack">
      <div class="panel"><h3>Contribution per unit</h3>${contribution ? `<dl class="kv"><dt>Price</dt><dd>${money(contribution.price)}</dd><dt>Ingredients</dt><dd>−${money(contribution.ingredientCost)}</dd><dt>Packaging</dt><dd>−${money(contribution.packagingCost)}</dd>
        <dt>Labour</dt><dd>−${money(contribution.laborCost)}</dd><dt>Selling fees</dt><dd>−${money(contribution.sellingFee)}</dd><dt><b>Contribution</b></dt><dd><b>${money(contribution.contributionPerUnit)}</b> (${contribution.marginPercent}%)</dd></dl>
        ${contribution.costsComplete ? '' : '<p class="small muted">Some costs are missing, so this is optimistic. Edit the product to add them.</p>'}` : '<p class="muted">No cost data.</p>'}</div>
      <div class="panel"><div class="row between"><h3 style="margin:0">Dietary claims</h3><button class="btn small ghost" id="claim">+ Add claim</button></div>
        <p class="small muted">Claims are only shown to shoppers once verified against supplier or process evidence.</p>
        ${d.claims.length ? d.claims.map(c => `<div class="total-row"><span><b>${esc(pretty(c.claimType))}</b> ${c.verified ? '<span class="badge ok">Verified</span>' : '<span class="badge warn">Unverified</span>'}<div class="small muted">${esc(c.evidence || 'No evidence recorded')}</div></span>
          <span class="nowrap">${c.verified ? '' : `<button class="btn small primary" data-verify="${c.id}">Verify</button>`} <button class="btn small ghost" data-rmclaim="${c.id}">Remove</button></span></div>`).join('') : '<p class="muted">No claims recorded.</p>'}</div></div></div>`;
  $('#edit', el).addEventListener('click', () => productForm(p.id));
  $('#pub', el).addEventListener('click', async () => { try { await api.post('/product/update/publishStatus', { ids: [p.id], value: !p.published }); toast('Updated', 'ok'); reload(); } catch (e) { fail(e); } });
  $('#claim', el).addEventListener('click', () => {
    const f = buildForm([{ name: 'claimType', label: 'Claim', type: 'select', required: true, options: enumOptions(ENUMS.ClaimType) }, { name: 'evidence', label: 'Evidence (supplier statement, certificate, process)', type: 'textarea', full: true, maxlength: 500 }]);
    modal({ title: 'Record a claim', body: f, actions: [{ label: 'Cancel', kind: 'ghost' }, { label: 'Add', kind: 'primary', run: async () => { if (!f.reportValidity()) return true; await api.post(`/product/${p.id}/claim`, readForm(f)); reload(); } }] });
  });
  $$('[data-verify]', el).forEach(b => b.addEventListener('click', async () => { try { await api.post('/product/claim/verify/' + b.dataset.verify); toast('Verified', 'ok'); reload(); } catch (e) { fail(e); } }));
  $$('[data-rmclaim]', el).forEach(b => b.addEventListener('click', async () => { try { await api.post('/product/claim/delete/' + b.dataset.rmclaim); reload(); } catch (e) { fail(e); } }));
  return el;
}
