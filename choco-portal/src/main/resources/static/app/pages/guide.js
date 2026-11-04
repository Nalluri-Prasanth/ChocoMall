import { api } from '/app/core.js';
import { esc, hashQ, pager, $, $$ } from '/shared/ui.js';

export async function guidePage({ params }) {
  const tab = params.tab || 'pairings';
  const el = document.createElement('div');
  el.innerHTML = `<h1>Guide</h1><p class="muted" style="max-width:70ch">What goes with chocolate, and the best ways to enjoy it. Drawn from our own tasting notes.</p>
    <div class="tabs">${[['pairings', 'Food pairings'], ['serving', 'Ways to enjoy'], ['stories', 'Stories']].map(([k, l]) => `<a class="tab ${tab === k ? 'active' : ''}" href="#/guide?tab=${k}">${l}</a>`).join('')}</div><div id="pane"><div class="spinner"></div></div>`;
  const pane = $('#pane', el);
  const page = Number(params.page || 1);
  if (tab === 'pairings') {
    const [groups, r] = await Promise.all([api.get('/guide/pairingGroups'), api.get('/guide/pairings', { group: params.group, keyword: params.q, pageNum: page, pageSize: 24 })]);
    pane.innerHTML = `<form class="row" id="pf" style="margin-bottom:16px"><select name="group" style="max-width:220px"><option value="">All foods</option>${groups.map(g => `<option ${params.group === g ? 'selected' : ''}>${esc(g)}</option>`).join('')}</select>
      <input name="q" placeholder="Search a food, e.g. strawberry" value="${esc(params.q || '')}" style="max-width:300px"><button class="btn small">Search</button></form>
      <div class="grid cards">${r.list.map(p => `<div class="pairing"><span class="badge">${esc(p.foodGroup)}</span><h3 style="margin:6px 0 2px">${esc(p.food)}</h3><div class="muted small">With ${esc(p.chocolateCombination)}</div><div class="small" style="margin-top:4px">${esc(p.shopFormat)}</div></div>`).join('') || '<p class="muted">No matches.</p>'}</div>`;
    $('#pf', pane).addEventListener('submit', e => { e.preventDefault(); const f = new FormData(e.target); location.hash = '#/guide' + hashQ({ tab, group: f.get('group'), q: f.get('q') }); });
    pane.appendChild(pager(page, r.totalPage, n => { location.hash = '#/guide' + hashQ({ ...params, tab, page: n }); }));
  } else if (tab === 'serving') {
    const r = await api.get('/guide/serving', { pageNum: page, pageSize: 24 });
    pane.innerHTML = `<div class="grid cards">${r.list.map(s => `<div class="pairing"><h3 style="margin:0 0 4px">${esc(s.eatingFormat)}</h3><div>${esc(s.howToServe)}</div><div class="muted small" style="margin-top:6px">Best with: ${esc(s.suitableProduct)}</div></div>`).join('')}</div>`;
    pane.appendChild(pager(page, r.totalPage, n => { location.hash = '#/guide' + hashQ({ ...params, tab, page: n }); }));
  } else {
    const r = await api.get('/home/subjectList', { pageSize: 12 });
    pane.innerHTML = `<div class="grid cols-2">${r.list.map(s => `<a class="panel" href="#/subject/${s.id}" style="color:inherit"><span class="badge">${esc(s.category || 'Story')}</span><h3 style="margin:8px 0 4px">${esc(s.title)}</h3><p class="muted small" style="margin:0">${esc(s.description || '')}</p></a>`).join('')}</div>`;
  }
  return el;
}

export async function subjectPage({ args }) {
  const { subject, content, products } = await api.get('/home/subject/' + args.id);
  const { productCard } = await import('/app/core.js');
  return `<p class="small muted"><a href="#/guide?tab=stories">‹ Stories</a></p><span class="badge">${esc(subject.category || 'Story')}</span><h1>${esc(subject.title)}</h1>
    <p class="muted">${esc(subject.description || '')}</p>
    <div class="panel" style="max-width:75ch;white-space:pre-line">${esc(content)}</div>
    ${products.length ? `<div class="section-head"><h2>Related</h2></div><div class="grid cards">${products.map(productCard).join('')}</div>` : ''}`;
}

export async function helpPage({ args }) {
  const centre = await api.get('/help/centre');
  let article = null;
  if (args.id) article = await api.get('/help/article/' + args.id);
  return `<h1>Help centre</h1><div class="grid" style="grid-template-columns:minmax(220px,300px) 1fr;gap:24px;align-items:start">
    <nav class="panel">${centre.map(c => `<h3 style="margin:10px 0 4px;font-size:1rem">${esc(c.name)}</h3>${c.articles.map(a => `<div><a href="#/help/${a.id}" ${String(a.id) === args.id ? 'style="font-weight:800"' : ''}>${esc(a.title)}</a></div>`).join('')}`).join('')}</nav>
    <div class="panel">${article ? `<h2 style="margin-top:0">${esc(article.title)}</h2><p style="white-space:pre-line">${esc(article.content)}</p>` : '<p class="muted">Pick a question on the left, or <a href="#/shop">go back to the shop</a>.</p>'}</div></div>`;
}
