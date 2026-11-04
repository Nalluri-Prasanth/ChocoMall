import { api, productCard } from '/app/core.js';
import { esc, $ } from '/shared/ui.js';

/** Catalog-backed suggestions, with explicit preferences rather than opaque scoring. */
export async function discover() {
  const el = document.createElement('section');
  el.className = 'discovery stack';
  el.innerHTML = `<div class="discovery-intro"><p class="eyebrow">Your chocolate, your way</p><h1>Find your next favourite.</h1><p class="muted">Choose a flavour and budget. We’ll look through the current collection for you.</p></div>
    <form class="panel pad form-grid" id="discovery-form">
      <div class="field"><label for="taste">What sounds good?</label><select id="taste" name="taste"><option value="">Surprise me</option><option value="dark">Deep &amp; dark</option><option value="caramel">Caramel comfort</option><option value="fruit">Something fruity</option><option value="praline">Nutty praline</option><option value="milk">Smooth milk chocolate</option></select></div>
      <div class="field"><label for="budget">Budget per item</label><select id="budget" name="budget"><option value="">Any budget</option><option value="10">Up to $10</option><option value="25">Up to $25</option><option value="50">Up to $50</option></select></div>
      <div class="field"><label for="diet">Verified preference</label><select id="diet" name="diet"><option value="">No preference</option><option value="VEGAN">Vegan</option><option value="GLUTEN_FREE">Gluten free</option><option value="NO_ADDED_SUGAR">No added sugar</option></select></div>
      <div class="field"><label>&nbsp;</label><button class="btn primary" type="submit">Find my collection →</button></div>
    </form><p class="small muted">Suggestions use catalog filters, not a medical assessment. Read each product’s allergen information before ordering.</p><div id="discovery-results" aria-live="polite"></div>`;
  let revision = 0;
  $('form', el).addEventListener('submit', async e => {
    e.preventDefault();
    const mine = ++revision, form = e.target, button = $('button', form), result = $('#discovery-results', el);
    const data = new FormData(form);
    button.disabled = true; button.textContent = 'Finding your collection…';
    result.setAttribute('aria-busy', 'true');
    try {
      const response = await api.get('/product/search', { keyword: data.get('taste') || undefined, maxPrice: data.get('budget') || undefined, claim: data.get('diet') || undefined, pageSize: 8, sort: 2 });
      if (mine !== revision) return;
      result.innerHTML = response.list.length ? `<div class="section-head"><h2>Your shortlist</h2><span class="small muted">${response.total} matching products</span></div><div class="grid cards">${response.list.map(productCard).join('')}</div>` : `<div class="empty"><h2>No matches just yet</h2><p>Try another flavour or a wider budget.</p><a class="btn" href="#/shop">Browse the whole collection</a></div>`;
    } catch (error) { result.innerHTML = `<div class="alert warn">${esc(error.message)}. Please try again.</div>`; }
    finally { button.disabled = false; button.textContent = 'Find my collection →'; result.removeAttribute('aria-busy'); }
  });
  return el;
}
