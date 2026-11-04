import { api, productCard, cached, emptyState } from '/app/core.js';
import { esc, hashQ, pager, $, $$, pretty } from '/shared/ui.js';

const KINDS = { MADE_IN_SHOP: 'Chocolates & treats', MADE_FRESH: 'Fresh & chilled', MADE_TO_ORDER: 'Drinks', SEALED_RETAIL: 'Imported shelf', PERSONALIZED: 'Personalised', EXPERIENCE: 'Experiences' };
const SORTS = { 0: 'Featured', 1: 'Newest', 2: 'Best sellers', 3: 'Price: low to high', 4: 'Price: high to low' };
const CLAIMS = ['VEGAN', 'PLANT_BASED', 'GLUTEN_FREE', 'HALAL', 'KOSHER', 'ORGANIC', 'NO_ADDED_SUGAR', 'REDUCED_SUGAR', 'SINGLE_ORIGIN'];

export async function shop({ params, root }) {
  const [tree, allergens, flavors, types] = await Promise.all([
    cached('tree', () => api.get('/product/categoryTreeList')), cached('allergens', () => api.get('/product/allergens')),
    cached('flavors', () => api.get('/product/flavors')), cached('types', () => api.get('/product/chocolateTypes'))]);
  const page = Number(params.page || 1);
  const avoid = (params.avoid || '').split(',').filter(Boolean);
  const query = {
    keyword: params.q, categoryId: params.cat, kind: params.kind, flavorId: params.flavor, chocolateTypeId: params.type,
    claim: params.claim, minPrice: params.min, maxPrice: params.max, sort: params.sort || 0, pageNum: page, pageSize: 12,
    excludeAllergens: avoid.length ? avoid : undefined,
  };
  const result = await api.get('/product/search', query);

  const opt = (value, label, current) => `<option value="${esc(value)}" ${String(current ?? '') === String(value) ? 'selected' : ''}>${esc(label)}</option>`;
  const catOptions = '<option value="">All categories</option>' + tree.map(g => `<optgroup label="${esc(g.name)}">${opt(g.id, 'All ' + g.name, params.cat)}${g.children.map(c => opt(c.id, c.name, params.cat)).join('')}</optgroup>`).join('');

  const names = {};
  tree.forEach(g => { names[g.id] = g.name; g.children.forEach(c => names[c.id] = c.name); });
  const chips = [];
  if (params.q) chips.push(['q', `“${params.q}”`]);
  if (params.cat) chips.push(['cat', names[params.cat] || 'Category']);
  if (params.kind) chips.push(['kind', KINDS[params.kind] || params.kind]);
  if (params.flavor) chips.push(['flavor', flavors.find(f => String(f.id) === params.flavor)?.name || 'Flavor']);
  if (params.type) chips.push(['type', types.find(f => String(f.id) === params.type)?.name || 'Type']);
  if (params.claim) chips.push(['claim', pretty(params.claim) + ' (verified)']);
  if (params.min || params.max) chips.push(['min', `Price ${params.min || 0} – ${params.max || '∞'}`]);
  avoid.forEach(id => chips.push(['avoid:' + id, 'No ' + (allergens.find(a => String(a.id) === id)?.name || 'allergen').toLowerCase()]));

  const el = document.createElement('div');
  el.className = 'shop-layout';
  el.innerHTML = `
  <aside class="filters panel">
    <form id="filter-form">
      <div class="field"><label>Search</label><input name="q" value="${esc(params.q || '')}" placeholder="pretzel, caramel…"></div>
      <div class="field"><label>Category</label><select name="cat">${catOptions}</select></div>
      <div class="field"><label>Type</label><select name="kind"><option value="">Everything</option>${Object.entries(KINDS).map(([k, v]) => opt(k, v, params.kind)).join('')}</select></div>
      <div class="field"><label>Sort by</label><select name="sort">${Object.entries(SORTS).map(([k, v]) => opt(k, v, params.sort || 0)).join('')}</select></div>
      <details ${avoid.length ? 'open' : ''}><summary>Avoid allergens</summary><div class="opts">${allergens.map(a => `<label class="check"><input type="checkbox" name="avoid[]" value="${a.id}" ${avoid.includes(String(a.id)) ? 'checked' : ''}>${esc(a.name)}</label>`).join('')}</div></details>
      <details ${params.flavor || params.type ? 'open' : ''}><summary>Flavor &amp; chocolate</summary>
        <div class="field" style="margin-top:8px"><label>Flavor</label><select name="flavor"><option value="">Any</option>${flavors.map(f => opt(f.id, f.name, params.flavor)).join('')}</select></div>
        <div class="field"><label>Chocolate type</label><select name="type"><option value="">Any</option>${types.map(f => opt(f.id, f.name, params.type)).join('')}</select></div></details>
      <details ${params.claim ? 'open' : ''}><summary>Verified claims</summary><div class="field" style="margin-top:8px"><select name="claim"><option value="">Any</option>${CLAIMS.map(c => opt(c, pretty(c), params.claim)).join('')}</select><div class="hint">Only claims backed by evidence on file.</div></div></details>
      <details ${params.min || params.max ? 'open' : ''}><summary>Price</summary><div class="row" style="margin-top:8px"><input name="min" type="number" min="0" step="0.5" placeholder="Min" value="${esc(params.min || '')}"><input name="max" type="number" min="0" step="0.5" placeholder="Max" value="${esc(params.max || '')}"></div></details>
      <div class="row" style="margin-top:14px"><button class="btn small" type="submit">Apply</button><a class="btn small ghost" href="#/shop">Reset</a></div>
    </form>
  </aside>
  <section>
    <div class="row between"><h1 style="font-size:1.8rem;margin:0">Shop</h1><span class="muted">${result.total} product${result.total === 1 ? '' : 's'}</span></div>
    ${chips.length ? `<div class="active-filters" style="margin-top:12px">${chips.map(([k, label]) => `<span class="chip chip-remove" data-remove="${esc(k)}" title="Remove filter">${esc(label)} ✕</span>`).join('')}</div>` : ''}
    <div id="results" style="margin-top:16px">${result.list.length
      ? `<div class="grid cards">${result.list.map(productCard).join('')}</div>`
      : emptyState('🍫', 'Nothing matches', 'Try removing a filter or searching for something else.', '<a class="btn" href="#/shop">Clear filters</a>')}</div>
  </section>`;

  const current = () => {
    const data = new FormData($('#filter-form', el));
    const p = {};
    for (const [k, v] of data.entries()) { if (v !== '' && k !== 'avoid[]') p[k] = v; }
    const avoidIds = data.getAll('avoid[]');
    if (avoidIds.length) p.avoid = avoidIds.join(',');
    return p;
  };
  $('#filter-form', el).addEventListener('submit', e => { e.preventDefault(); location.hash = '#/shop' + hashQ(current()); });
  $$('select, input[type=checkbox]', $('#filter-form', el)).forEach(i => i.addEventListener('change', () => { location.hash = '#/shop' + hashQ(current()); }));
  $$('[data-remove]', el).forEach(c => c.addEventListener('click', () => {
    const p = { ...params }; delete p.page;
    const k = c.dataset.remove;
    if (k.startsWith('avoid:')) p.avoid = avoid.filter(x => x !== k.slice(6)).join(',');
    else { delete p[k]; if (k === 'min') delete p.max; }
    location.hash = '#/shop' + hashQ(p);
  }));
  $('#results', el).after(pager(page, result.totalPage, n => { location.hash = '#/shop' + hashQ({ ...params, page: n }); }));
  return el;
}
