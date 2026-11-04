import { api, productCard } from '/app/core.js';
import { esc, money, art, dt, day } from '/shared/ui.js';
import { collectionScene } from '/app/atelier.js';

export async function home() {
  const [h, sessions] = await Promise.all([api.get('/home/content'), api.get('/experience/list')]);
  const gifts = h.categories.find(c => /gift/i.test(c.name));
  const window0 = h.preorderWindows[0];
  const tiles = h.categories.map(c => `<a class="cat-tile" href="#/shop?cat=${c.id}">${art({ name: c.name })}${esc(c.name)}<div class="small muted">${c.children.map(x => esc(x.name)).slice(0, 3).join(' · ')}</div></a>`).join('');

  const bundles = h.bundles.map(b => `<div class="card pad stack">
      <h3 style="margin:0">${esc(b.name)}</h3><p class="muted small" style="margin:0">${esc(b.description || '')}</p>
      <ul class="small" style="margin:0;padding-left:18px">${b.items.map(i => `<li>${i.quantity} × <a href="#/product/${i.productId}">${esc(i.name)}</a></li>`).join('')}</ul>
      <div class="row between"><span><b>${money(b.price)}</b> ${b.originalPrice ? `<s class="muted">${money(b.originalPrice)}</s>` : ''}</span><span class="badge ok">Bundle price applied in the basket</span></div></div>`).join('');

  const subjects = h.subjects.map(s => `<a class="card pad" href="#/subject/${s.id}" style="color:inherit"><span class="badge">${esc(s.category || 'Story')}</span><h3 style="margin:8px 0 4px">${esc(s.title)}</h3><p class="muted small" style="margin:0">${esc(s.description || '')}</p></a>`).join('');

  const experiences = sessions.slice(0, 3).map(s => `<div class="card pad row between"><div><b>${esc(s.productName)}</b><div class="small muted">${dt(s.startsAt)} · ${s.durationMinutes} min</div></div>
      <div class="right"><div><b>${money(s.price)}</b> <span class="small muted">per seat</span></div><div class="small ${s.seatsLeft < 4 ? '' : 'muted'}">${s.seatsLeft} seats left</div></div></div>`).join('');

  return `
  <section class="boutique-hero" aria-labelledby="collection-title">
    <div class="hero-copy">
      <p class="eyebrow">The chocolate boutique</p>
      <h1 id="collection-title">A little box.<br>A lot of joy.</h1>
      <p>Silky ganache, toasted praline, a bright hint of fruit. Discover our small-batch chocolates, or choose every piece in a box of your own.</p>
      <div class="row"><a class="btn primary" href="#/shop">Explore the collection →</a><a class="text-link" href="#/shop?q=build-a-box">Build your box</a></div>
    </div>
    <div class="hero-display">${collectionScene()}</div>
  </section>
  <div class="service-strip"><span><b>Made in small batches</b> · Bonbons, bars &amp; barks</span><span><b>Your choice of pieces</b> · Build a gift box</span><span><b>On your schedule</b> · Pickup or delivery</span></div>
  ${window0 ? `<div class="promo" style="margin-top:18px"><div><b>🎁 ${esc(window0.name)}</b><div class="small muted">Order by ${dt(window0.orderCutoff)} · collect ${day(window0.fulfilFrom)} to ${day(window0.fulfilTo)} · ${window0.remainingUnits} places left</div></div>${gifts ? `<a class="btn" href="#/shop?cat=${gifts.id}">See gifts &amp; seasonal</a>` : ''}</div>` : ''}

  <div class="section-head"><h2>Shop by category</h2><a href="#/shop">All products →</a></div>
  <div class="cat-tiles">${tiles}</div>

  ${h.recommendProducts.length ? `<div class="section-head"><h2>Our favourites</h2><a href="#/shop?sort=2">Best sellers →</a></div><div class="grid cards">${h.recommendProducts.map(productCard).join('')}</div>` : ''}
  ${h.newProducts.length ? `<div class="section-head"><h2>New on the shelf</h2><a href="#/shop?sort=1">Newest →</a></div><div class="grid cards">${h.newProducts.map(productCard).join('')}</div>` : ''}
  ${bundles ? `<div class="section-head"><h2>Bundles</h2></div><div class="grid cols-2">${bundles}</div>` : ''}
  ${experiences ? `<div class="section-head"><h2>Tastings &amp; workshops</h2><a href="#/experiences">All sessions →</a></div><div class="grid cols-2">${experiences}</div>` : ''}
  ${subjects ? `<div class="section-head"><h2>Chocolate stories</h2><a href="#/guide">Pairing guide →</a></div><div class="grid cols-2">${subjects}</div>` : ''}
  ${h.brands.length ? `<div class="section-head"><h2>Brands we taste and trust</h2></div><div class="chips">${h.brands.map(b => `<span class="chip">${esc(b.name)}</span>`).join('')}</div>` : ''}`;
}
