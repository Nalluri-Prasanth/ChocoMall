import { api, rowsOf, idMap, enumOptions, ENUMS, openForm, yesNo, thumb, badge, esc, dt, money, toast, fail, confirmDialog, pretty } from '/app/core.js';
import { crud } from '/app/crud.js';

const fmtDate = v => v ? dt(v) : '';
const priority = { key: 'priority', label: 'Tier', html: r => badge(r.priority) };

// ------------------------------------------------------------------ catalog references
export const brands = crud({
  title: 'Brands', subtitle: 'The global brand directory and each brand\'s stocking stage.', noun: 'brand',
  list: '/brand/list', pageSize: 20,
  filters: [{ name: 'keyword', label: 'Search name' }, { name: 'stockingStatus', label: 'Any stage', type: 'select', options: enumOptions(ENUMS.BrandStockingStatus) }],
  columns: [{ key: 'code', label: 'Code' }, { key: 'name', label: 'Brand' }, { key: 'region', label: 'Region' }, { key: 'segment', label: 'Segment' },
    { label: 'Stage', html: r => badge(r.stockingStatus) }, { label: 'Shown', html: r => yesNo(r.showStatus) }, { key: 'productCount', label: 'Products', num: true }],
  fields: () => [
    { name: 'code', label: 'Code', required: true, maxlength: 20 }, { name: 'name', label: 'Name', required: true },
    { name: 'region', label: 'Region' }, { name: 'associatedMarket', label: 'Market' }, { name: 'segment', label: 'Segment' },
    { name: 'typicalFormats', label: 'Typical formats' }, { name: 'shopRole', label: 'Role in the shop' }, { name: 'evidenceStatus', label: 'Evidence status' },
    { name: 'referenceUrl', label: 'Reference URL' }, { name: 'logo', label: 'Logo', type: 'image' },
    { name: 'sort', label: 'Sort order', type: 'int' }, { name: 'showStatus', label: 'Show on the storefront', type: 'bool' },
  ],
  create: '/brand/create', update: id => `/brand/update/${id}`, del: id => `/brand/delete/${id}`,
  rowActions: [{
    label: 'Stage…', run: (row, reload) => openForm({
      title: `Move ${row.name}`, wide: false, values: { status: row.stockingStatus, authorizedSupplier: row.authorizedSupplier, landedCostNote: row.landedCostNote },
      fields: [{ name: 'status', label: 'Stage', type: 'select', required: true, options: enumOptions(ENUMS.BrandStockingStatus) },
        { name: 'authorizedSupplier', label: 'Authorised supplier', hint: 'Needed before a brand can be stocked.' },
        { name: 'landedCostNote', label: 'Landed cost note', full: true }],
      submit: v => api.post(`/brand/update/stocking/${row.id}`, v), after: reload,
    }),
  }],
});

export const categories = crud({
  title: 'Categories', subtitle: 'Top-level categories appear in the shop navigation. Use ?parent=ID to browse sub-categories.', noun: 'category',
  list: p => `/productCategory/list/${p.parent || 0}`,
  columns: [{ key: 'name', label: 'Name', html: r => `<a href="#/categories?parent=${r.id}">${esc(r.name)}</a>` }, { key: 'level', label: 'Level', num: true },
    { key: 'productCount', label: 'Products', num: true }, { label: 'In menu', html: r => yesNo(r.navStatus) }, { label: 'Shown', html: r => yesNo(r.showStatus) }, { key: 'sort', label: 'Sort', num: true }],
  fields: async () => [
    { name: 'parentId', label: 'Parent', type: 'select', options: [{ value: 0, label: '— top level —' }, ...(await idMap('/productCategory/list/0'))], def: 0, required: true },
    { name: 'name', label: 'Name', required: true }, { name: 'keywords', label: 'Keywords' }, { name: 'icon', label: 'Icon', type: 'image' },
    { name: 'sort', label: 'Sort order', type: 'int', def: 100 }, { name: 'description', label: 'Description', type: 'textarea', full: true },
    { name: 'navStatus', label: 'Show in the menu', type: 'bool', def: true }, { name: 'showStatus', label: 'Visible', type: 'bool', def: true },
  ],
  toBody: v => ({ ...v, parentId: Number(v.parentId || 0) }),
  create: '/productCategory/create', update: id => `/productCategory/update/${id}`, del: id => `/productCategory/delete/${id}`,
});

export const flavors = crud({
  title: 'Flavours', subtitle: 'Reference list used when describing products.', noun: 'flavour', list: '/flavor/list', pageSize: 500,
  columns: [{ key: 'code', label: 'Code' }, { key: 'name', label: 'Flavour' }, { key: 'flavorGroup', label: 'Group' }, { key: 'chocolateBase', label: 'Base' }, { key: 'application', label: 'Used in' }, priority],
  fields: [{ name: 'code', label: 'Code', required: true, maxlength: 10 }, { name: 'name', label: 'Name', required: true }, { name: 'flavorGroup', label: 'Group' },
    { name: 'chocolateBase', label: 'Chocolate base' }, { name: 'application', label: 'Application', full: true }, { name: 'priority', label: 'Tier', type: 'select', required: true, options: enumOptions(ENUMS.Priority), def: 'CORE' }],
  create: '/flavor/create', update: id => `/flavor/update/${id}`, del: id => `/flavor/delete/${id}`,
});

export const chocolateTypes = crud({
  title: 'Chocolate types', noun: 'chocolate type', list: '/chocolateType/list',
  columns: [{ key: 'code', label: 'Code' }, { key: 'name', label: 'Type' }, { key: 'classification', label: 'Class' }, { key: 'bestShopUse', label: 'Best shop use' }, priority],
  fields: [{ name: 'code', label: 'Code', required: true, maxlength: 10 }, { name: 'name', label: 'Name', required: true }, { name: 'classification', label: 'Classification' },
    { name: 'distinguishing', label: 'What makes it different', full: true }, { name: 'bestShopUse', label: 'Best shop use', full: true },
    { name: 'priority', label: 'Tier', type: 'select', required: true, options: enumOptions(ENUMS.Priority), def: 'CORE' }],
  create: '/chocolateType/create', update: id => `/chocolateType/update/${id}`, del: id => `/chocolateType/delete/${id}`,
});

export const allergens = crud({
  title: 'Allergens', subtitle: 'Shown to shoppers on every product. Products cannot be published until reviewed.', noun: 'allergen', list: '/allergen/list',
  columns: [{ key: 'code', label: 'Code' }, { key: 'name', label: 'Allergen' }],
  fields: [{ name: 'code', label: 'Code', required: true, maxlength: 30 }, { name: 'name', label: 'Name', required: true }],
  create: '/allergen/create', update: id => `/allergen/update/${id}`, del: id => `/allergen/delete/${id}`,
});

// ------------------------------------------------------------------ marketing
export const coupons = crud({
  title: 'Coupons', subtitle: 'Offers shoppers can claim or enter at checkout.', noun: 'coupon', list: '/coupon/list',
  filters: [{ name: 'keyword', label: 'Search' }],
  columns: [{ key: 'name', label: 'Name' }, { key: 'code', label: 'Code' }, { label: 'Applies to', html: r => badge(r.couponType) }, { label: 'Off', num: true, html: r => money(r.amount) },
    { label: 'Min spend', num: true, html: r => money(r.minSpend) }, { label: 'Claimed / total', num: true, get: r => `${r.receivedCount} / ${r.totalCount}` }, { label: 'Ends', get: r => fmtDate(r.endTime) }, { label: 'Active', html: r => yesNo(r.status) }],
  fields: async () => [
    { name: 'name', label: 'Name', required: true }, { name: 'code', label: 'Code', hint: 'Shoppers may enter this at checkout.', maxlength: 64 },
    { name: 'couponType', label: 'Applies to', type: 'select', required: true, options: enumOptions(ENUMS.CouponType), def: 'ALL' },
    { name: 'amount', label: 'Amount off', type: 'number', required: true, min: 0.01 }, { name: 'minSpend', label: 'Minimum spend', type: 'number', required: true, def: 0, min: 0 },
    { name: 'totalCount', label: 'How many to issue', type: 'int', required: true, def: 100, min: 1 }, { name: 'perLimit', label: 'Per customer', type: 'int', required: true, def: 1, min: 1 },
    { name: 'startTime', label: 'Starts', type: 'datetime', required: true }, { name: 'endTime', label: 'Ends', type: 'datetime', required: true },
    { name: 'memberLevelId', label: 'Only for level', type: 'select', options: await idMap('/memberLevel/list') },
    { name: 'status', label: 'Active', type: 'bool', def: true },
    { name: 'categoryIds', label: 'Categories (when applies to category)', type: 'ids', options: await idMap('/productCategory/list/0') },
    { name: 'note', label: 'Note', type: 'textarea', full: true },
  ],
  defaults: { startTime: new Date().toISOString().slice(0, 16), endTime: new Date(Date.now() + 90 * 864e5).toISOString().slice(0, 16) },
  create: '/coupon/create', update: id => `/coupon/update/${id}`, del: id => `/coupon/delete/${id}`,
  rowActions: [{
    label: 'Give to…', run: row => openForm({
      title: `Give “${row.name}”`, wide: false, fields: [{ name: 'memberId', label: 'Member ID', type: 'int', required: true, hint: 'See the Members page for ids.' }],
      submit: v => api.post('/coupon/grant', { couponId: row.id, memberId: v.memberId }),
    }),
  }],
});

export const memberLevels = crud({
  title: 'Member levels', subtitle: 'Loyalty tiers. Members move up as they spend.', noun: 'level', list: '/memberLevel/list',
  columns: [{ key: 'name', label: 'Level' }, { key: 'growthPoint', label: 'Growth needed', num: true }, { key: 'pointsMultiplier', label: 'Points ×', num: true }, { label: 'Free delivery over', num: true, html: r => r.freeDeliveryThreshold != null ? money(r.freeDeliveryThreshold) : '' }, { label: 'Default', html: r => yesNo(r.defaultStatus) }, { key: 'note', label: 'Note' }],
  fields: [{ name: 'name', label: 'Name', required: true }, { name: 'growthPoint', label: 'Growth needed', type: 'int', required: true, def: 0, min: 0 },
    { name: 'pointsMultiplier', label: 'Points multiplier', type: 'number', required: true, def: 1, min: 0.5 }, { name: 'freeDeliveryThreshold', label: 'Free delivery over', type: 'number', min: 0 },
    { name: 'note', label: 'Note', full: true }, { name: 'defaultStatus', label: 'New members start here', type: 'bool' }],
  create: '/memberLevel/create', update: id => `/memberLevel/update/${id}`, del: id => `/memberLevel/delete/${id}`,
});

const homeSlots = crud({
  title: 'Home page slots', subtitle: 'Banners, brands and featured items on the storefront home page.', noun: 'slot', list: '/home/slot/list',
  filters: [{ name: 'type', label: 'Type', type: 'select', options: enumOptions(ENUMS.HomeSlotType) }],
  columns: [{ label: '', html: r => thumb(r.pic) }, { key: 'name', label: 'Name' }, { label: 'Type', html: r => badge(r.slotType) }, { key: 'refId', label: 'Links to id' }, { key: 'sort', label: 'Sort', num: true }, { label: 'On', html: r => yesNo(r.status) }],
  fields: [{ name: 'slotType', label: 'Type', type: 'select', required: true, options: enumOptions(ENUMS.HomeSlotType), def: 'BANNER' }, { name: 'name', label: 'Title' },
    { name: 'refId', label: 'Product / brand / story id', type: 'int', hint: 'Not needed for banners.' }, { name: 'pic', label: 'Picture', type: 'image' }, { name: 'url', label: 'Link' },
    { name: 'sort', label: 'Sort', type: 'int', def: 100 }, { name: 'startTime', label: 'Show from', type: 'datetime' }, { name: 'endTime', label: 'Show until', type: 'datetime' },
    { name: 'note', label: 'Note', full: true }, { name: 'status', label: 'Active', type: 'bool', def: true }],
  create: '/home/slot/create', update: id => `/home/slot/update/${id}`,
  del: id => `/home/slot/delete?ids=${id}`,
});
export const homeSlotsWithType = async ctx => { ctx.params = { type: 'BANNER', ...ctx.params }; return homeSlots(ctx); };

// ------------------------------------------------------------------ content
export const subjects = crud({
  title: 'Stories', subtitle: 'Editorial articles shown in the Guide, optionally linked to products.', noun: 'story', list: '/subject/list',
  filters: [{ name: 'keyword', label: 'Search' }],
  columns: [{ key: 'title', label: 'Title' }, { key: 'category', label: 'Category' }, { label: 'Shown', html: r => yesNo(r.showStatus) }, { label: 'Featured', html: r => yesNo(r.recommendStatus) }, { key: 'readCount', label: 'Reads', num: true }],
  fields: async () => [{ name: 'title', label: 'Title', required: true }, { name: 'category', label: 'Category' }, { name: 'pic', label: 'Picture', type: 'image' }, { name: 'sort', label: 'Sort', type: 'int', def: 100 },
    { name: 'description', label: 'Summary', type: 'textarea', full: true, maxlength: 500, rows: 2 }, { name: 'content', label: 'Article', type: 'textarea', full: true, maxlength: 4000, rows: 8 },
    { name: 'showStatus', label: 'Shown', type: 'bool', def: true }, { name: 'recommendStatus', label: 'Featured on home', type: 'bool' }],
  create: '/subject/create', update: id => `/subject/update/${id}`, del: id => `/subject/delete?ids=${id}`,
});

export const helpCategories = crud({
  title: 'Help topics', noun: 'topic', list: '/help/category/list',
  columns: [{ key: 'name', label: 'Topic' }, { key: 'sort', label: 'Sort', num: true }, { label: 'Shown', html: r => yesNo(r.showStatus) }],
  fields: [{ name: 'name', label: 'Name', required: true }, { name: 'sort', label: 'Sort', type: 'int', def: 100 }, { name: 'showStatus', label: 'Shown', type: 'bool', def: true }],
  create: '/help/category/create', update: id => `/help/category/update/${id}`, del: id => `/help/category/delete/${id}`,
});

export const helpArticles = crud({
  title: 'Help articles', subtitle: 'Answers shown in the storefront help centre.', noun: 'article', list: '/help/article/list',
  columns: [{ key: 'title', label: 'Question' }, { key: 'categoryId', label: 'Topic id' }, { label: 'Shown', html: r => yesNo(r.showStatus) }, { key: 'readCount', label: 'Reads', num: true }],
  fields: async () => [{ name: 'categoryId', label: 'Topic', type: 'select', required: true, options: await idMap('/help/category/list') }, { name: 'title', label: 'Question', required: true, full: true },
    { name: 'content', label: 'Answer', type: 'textarea', full: true, rows: 6, maxlength: 4000 }, { name: 'showStatus', label: 'Shown', type: 'bool', def: true }],
  create: '/help/article/create', update: id => `/help/article/update/${id}`, del: id => `/help/article/delete/${id}`,
});

export const pairings = crud({
  title: 'Food pairings', subtitle: 'What goes with chocolate. Shown in the storefront guide.', noun: 'pairing', list: '/pairing/list', pageSize: 20,
  filters: [{ name: 'keyword', label: 'Search' }],
  columns: [{ key: 'code', label: 'Code' }, { key: 'foodGroup', label: 'Group' }, { key: 'food', label: 'Food' }, { key: 'chocolateCombination', label: 'Chocolate' }, { key: 'shopFormat', label: 'In the shop' }, priority],
  fields: [{ name: 'code', label: 'Code', required: true, maxlength: 10 }, { name: 'foodGroup', label: 'Group' }, { name: 'food', label: 'Food', required: true }, { name: 'chocolateCombination', label: 'Chocolate' },
    { name: 'shopFormat', label: 'Shop format' }, { name: 'priority', label: 'Tier', type: 'select', required: true, options: enumOptions(ENUMS.Priority), def: 'CORE' }],
  create: '/pairing/create', update: id => `/pairing/update/${id}`, del: id => `/pairing/delete/${id}`,
});

export const servings = crud({
  title: 'Ways to serve', noun: 'serving idea', list: '/serving/list', pageSize: 20,
  columns: [{ key: 'code', label: 'Code' }, { key: 'eatingFormat', label: 'Format' }, { key: 'howToServe', label: 'How to serve' }, { key: 'suitableProduct', label: 'Suits' }, priority],
  fields: [{ name: 'code', label: 'Code', required: true, maxlength: 10 }, { name: 'eatingFormat', label: 'Eating format', required: true }, { name: 'howToServe', label: 'How to serve', full: true },
    { name: 'suitableProduct', label: 'Suitable product' }, { name: 'salesApplication', label: 'Sales application' }, { name: 'priority', label: 'Tier', type: 'select', required: true, options: enumOptions(ENUMS.Priority), def: 'CORE' }],
  create: '/serving/create', update: id => `/serving/update/${id}`, del: id => `/serving/delete/${id}`,
});

// ------------------------------------------------------------------ shop settings lists
export const returnReasons = crud({
  title: 'Return reasons', subtitle: 'Choices shoppers see when they ask for a refund.', noun: 'reason', list: '/returnReason/list',
  columns: [{ key: 'name', label: 'Reason' }, { key: 'sort', label: 'Sort', num: true }, { label: 'Active', html: r => yesNo(r.status) }],
  fields: [{ name: 'name', label: 'Reason', required: true }, { name: 'sort', label: 'Sort', type: 'int', def: 100 }, { name: 'status', label: 'Active', type: 'bool', def: true }],
  create: '/returnReason/create', update: id => `/returnReason/update/${id}`, del: id => `/returnReason/delete?ids=${id}`,
});

export const locations = crud({
  title: 'Shop locations', subtitle: 'Pickup and returns points.', noun: 'location', list: '/shopLocation/list',
  columns: [{ key: 'name', label: 'Name' }, { key: 'detailAddress', label: 'Address' }, { key: 'openingHours', label: 'Hours' }, { label: 'Pickup', html: r => yesNo(r.pickupEnabled) }, { label: 'Returns', html: r => yesNo(r.returnEnabled) }],
  fields: [{ name: 'name', label: 'Name', required: true }, { name: 'phone', label: 'Phone', maxlength: 20 }, { name: 'province', label: 'Province / state' }, { name: 'city', label: 'City' }, { name: 'region', label: 'Region' },
    { name: 'detailAddress', label: 'Street address', full: true, maxlength: 300 }, { name: 'openingHours', label: 'Opening hours' }, { name: 'pickupEnabled', label: 'Pickup here', type: 'bool', def: true }, { name: 'returnEnabled', label: 'Returns here', type: 'bool', def: true }],
  create: '/shopLocation/create', update: id => `/shopLocation/update/${id}`, del: id => `/shopLocation/delete/${id}`,
});

// ------------------------------------------------------------------ planning
const windowStatuses = ['OPEN', 'CLOSED', 'FULL'];
export const preorderWindows = crud({
  title: 'Preorder windows', subtitle: 'Seasonal ordering with a cut-off and a capacity cap, so you never promise more than the kitchen can make.', noun: 'window', list: '/preorderWindow/list',
  columns: [{ key: 'name', label: 'Window' }, { key: 'occasion', label: 'Occasion' }, { label: 'Order by', get: r => fmtDate(r.orderCutoff) }, { label: 'Fulfil', get: r => `${r.fulfilFrom} → ${r.fulfilTo}` },
    { label: 'Reserved / capacity', num: true, get: r => `${r.reservedUnits} / ${r.capacityUnits}` }, { label: 'Status', html: r => badge(r.status) }],
  fields: [{ name: 'name', label: 'Name', required: true }, { name: 'occasion', label: 'Occasion' }, { name: 'orderCutoff', label: 'Order cut-off', type: 'datetime', required: true },
    { name: 'capacityUnits', label: 'Capacity (units)', type: 'int', required: true, min: 1, def: 100 }, { name: 'fulfilFrom', label: 'Fulfil from', type: 'date', required: true }, { name: 'fulfilTo', label: 'Fulfil to', type: 'date', required: true },
    { name: 'note', label: 'Note', full: true }],
  create: '/preorderWindow/create', update: id => `/preorderWindow/update/${id}`, del: id => `/preorderWindow/delete/${id}`,
  rowActions: [{
    label: 'Open / close', run: async (row, reload) => { await api.postQ(`/preorderWindow/status/${row.id}`, { status: row.status === 'OPEN' ? 'CLOSED' : 'OPEN' }); toast('Updated', 'ok'); reload(); },
  }],
});

export const sessions = crud({
  title: 'Tastings & workshops', subtitle: 'Sessions shoppers can book and pay for online.', noun: 'session', list: '/experience/session/list',
  columns: [{ key: 'id', label: '#' }, { key: 'productId', label: 'Product id' }, { label: 'Starts', get: r => fmtDate(r.startsAt) }, { key: 'durationMinutes', label: 'Minutes', num: true },
    { label: 'Booked / seats', num: true, get: r => `${r.booked} / ${r.capacity}` }, { label: 'Price', num: true, html: r => money(r.price) }, { key: 'location', label: 'Where' }, { label: 'Status', html: r => badge(r.status) }],
  fields: async () => [{ name: 'productId', label: 'Experience product', type: 'select', required: true, options: (rowsOf(await api.get('/product/list', { kind: 'EXPERIENCE', pageSize: 100 }))).map(p => ({ value: p.id, label: p.name })), hint: 'Create a product of kind “Experience” first.' },
    { name: 'startsAt', label: 'Starts', type: 'datetime', required: true }, { name: 'durationMinutes', label: 'Minutes', type: 'int', def: 90, min: 15 }, { name: 'capacity', label: 'Seats', type: 'int', required: true, def: 8, min: 1 },
    { name: 'price', label: 'Price per seat', type: 'number', min: 0 }, { name: 'location', label: 'Location', def: 'Main boutique' }],
  create: '/experience/session/create', update: id => `/experience/session/update/${id}`,
  rowActions: [{
    label: 'Cancel', show: r => r.status !== 'CANCELLED',
    run: async (row, reload) => { if (!(await confirmDialog('Cancel this session?', 'Every booking is cancelled and refunded.', 'Cancel session'))) return; const n = await api.post(`/experience/session/cancel/${row.id}`); toast(`Cancelled. ${n} booking(s) refunded.`, 'ok'); reload(); },
  }],
});

export const bundles = async ctx => {
  const rows = rowsOf(await api.get('/bundle/list'));
  const products = rowsOf(await api.get('/product/list', { pageSize: 500, published: true }));
  const opts = products.map(p => ({ value: p.id, label: p.name }));
  const el = document.createElement('div');
  el.innerHTML = `<div class="page-head"><div><h1>Bundles</h1><div class="muted">Fixed-price combinations, e.g. a drink plus a treat. The saving shows at checkout automatically.</div></div><button class="btn primary" id="add">+ Add bundle</button></div>
    ${rows.length ? `<div class="table-wrap"><table><thead><tr><th>Name</th><th>Items</th><th class="num">Price</th><th class="num">Was</th><th>Active</th><th></th></tr></thead><tbody>${rows.map((r, i) => `<tr><td>${esc(r.bundle.name)}</td>
      <td class="small">${r.items.map(it => `${it.quantity} × ${esc(products.find(p => p.id === it.productId)?.name || '#' + it.productId)}`).join(', ')}</td><td class="num">${money(r.bundle.price)}</td><td class="num">${money(r.bundle.originalPrice)}</td><td>${yesNo(r.bundle.status)}</td>
      <td class="actions nowrap"><button class="btn small ghost" data-edit="${i}">Edit</button> <button class="btn small ghost" data-del="${i}">Delete</button></td></tr>`).join('')}</tbody></table></div>` : '<div class="empty"><p>No bundles yet.</p></div>'}`;
  const reload = () => window.dispatchEvent(new HashChangeEvent('hashchange'));
  const form = (row) => {
    const items = row ? row.items.map(i => ({ productId: i.productId, quantity: i.quantity })) : [{ productId: opts[0]?.value, quantity: 1 }];
    const f = document.createElement('form');
    const draw = () => {
      f.innerHTML = `<div class="form-grid"><div class="field"><label>Name</label><input name="name" required value="${esc(row?.bundle.name || '')}"></div>
        <div class="field"><label>Bundle price</label><input name="price" data-type="number" type="number" step="0.01" min="0.01" required value="${esc(row?.bundle.price ?? '')}"></div>
        <div class="field"><label>Normal price (optional)</label><input name="originalPrice" data-type="number" type="number" step="0.01" value="${esc(row?.bundle.originalPrice ?? '')}"></div>
        <div class="field"><label>Description</label><input name="description" value="${esc(row?.bundle.description || '')}"></div></div>
        <h4>Items</h4>${items.map((it, i) => `<div class="row" style="margin-bottom:6px"><select class="grow" data-i="${i}" data-k="productId">${opts.map(o => `<option value="${o.value}" ${o.value === it.productId ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>
        <input style="width:80px" type="number" min="1" value="${it.quantity}" data-i="${i}" data-k="quantity"><button type="button" class="btn small ghost" data-rm="${i}">✕</button></div>`).join('')}
        <button type="button" class="btn small ghost" id="more">+ Add item</button>
        <label class="check" style="margin-top:10px"><input type="checkbox" name="status" ${row?.bundle.status ?? true ? 'checked' : ''}> Active</label>`;
      f.querySelectorAll('[data-i]').forEach(inp => inp.addEventListener('change', () => { items[inp.dataset.i][inp.dataset.k] = Number(inp.value); }));
      f.querySelectorAll('[data-rm]').forEach(b => b.addEventListener('click', () => { items.splice(Number(b.dataset.rm), 1); draw(); }));
      f.querySelector('#more').addEventListener('click', () => { items.push({ productId: opts[0]?.value, quantity: 1 }); draw(); });
    };
    draw();
    return { f, items };
  };
  const open = row => {
    const { f, items } = form(row);
    import('/shared/ui.js').then(({ modal, readForm }) => modal({
      title: row ? 'Edit bundle' : 'Add bundle', body: f, wide: true, actions: [{ label: 'Cancel', kind: 'ghost' }, {
        label: 'Save', kind: 'primary', run: async () => {
          if (!f.reportValidity()) return true;
          const v = readForm(f);
          await api.post(row ? `/bundle/update/${row.bundle.id}` : '/bundle/create', { ...v, items: items.map(i => ({ productId: i.productId, quantity: i.quantity || 1 })) });
          toast('Saved', 'ok'); reload();
        },
      }],
    }));
  };
  el.querySelector('#add').addEventListener('click', () => open(null));
  el.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => open(rows[Number(b.dataset.edit)])));
  el.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', async () => {
    const r = rows[Number(b.dataset.del)];
    if (!(await confirmDialog('Delete this bundle?', r.bundle.name, 'Delete'))) return;
    try { await api.post(`/bundle/delete/${r.bundle.id}`); reload(); } catch (e) { fail(e); }
  }));
  return el;
};
