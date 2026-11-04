import { api, session, requireLogin, askCard, emptyState, statusBadge, nowLocalInput } from '/app/core.js';
import { esc, money, dt, day, $, $$, toast, fail, modal, confirmDialog, readForm, pretty } from '/shared/ui.js';

const KINDS = { NAME_PLAQUE: 'Name plaque', MONOGRAM_MEDALLION: 'Monogram medallion', LOGO_PLAQUE: 'Logo plaque', MESSAGE_STRIP: 'Message strip', PLACE_NAME_CARD: 'Place-name card', SILHOUETTE: 'Flat silhouette', EDIBLE_PHOTO_PLAQUE: 'Edible photo plaque' };

export async function personalizePage({ params }) {
  const el = document.createElement('div');
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
  el.innerHTML = `<h1>Personalise a gift</h1>
    <p class="muted" style="max-width:70ch">Names, monograms and short messages in chocolate, made to order. Here is how it works:</p>
    <div class="grid cards" style="margin-bottom:22px">${[['1', 'Tell us what you want', 'Choose a piece, the text and the date you need it.'], ['2', 'We quote', 'You get a price and a realistic lead time.'], ['3', 'Approve the proof', 'We send an artwork proof. Nothing is made until you approve it.'], ['4', 'Pay a deposit', 'Only after you approve. The rest is due on collection.'], ['5', 'We make it', 'We tell you when it is ready.']].map(([n, t, d]) => `<div class="panel"><span class="badge ruby">Step ${n}</span><h3 style="margin:6px 0">${t}</h3><p class="small muted" style="margin:0">${d}</p></div>`).join('')}</div>
    <div class="grid cols-2"><form class="panel" id="pf"><h3 style="margin-top:0">New request</h3>
      <div class="field"><label>What would you like?</label><select name="kind">${Object.entries(KINDS).map(([k, v]) => `<option value="${k}" ${params.kind === k ? 'selected' : ''}>${v}</option>`).join('')}</select></div>
      <div class="field"><label>Text</label><input name="text" required maxlength="60" placeholder="Happy Birthday Mia"><div class="hint">Up to 40 characters (60 for message strips). Thin or long designs break easily.</div></div>
      <div class="form-grid"><div class="field"><label>How many?</label><input name="quantity" type="number" data-type="int" min="1" max="500" value="1"></div>
      <div class="field"><label>Needed by <span class="muted">(optional)</span></label><input name="neededBy" type="date" min="${tomorrow}"></div></div>
      <div class="field"><label>Anything else we should know?</label><textarea name="occasionNote" maxlength="500" placeholder="Cake topper for a birthday, gold lettering…"></textarea></div>
      <button class="btn primary">${session.token ? 'Send request' : 'Sign in to send'}</button></form>
      <div id="mine" class="stack"></div></div>`;
  $('#pf', el).addEventListener('submit', async e => {
    e.preventDefault();
    if (!requireLogin()) return;
    try {
      const f = readForm(e.target);
      const r = await api.post('/personalization/submit', { kind: f.kind, text: f.text, quantity: f.quantity || 1, occasionNote: f.occasionNote, neededBy: f.neededBy });
      toast('Request sent. We will send a quote shortly.', 'ok');
      e.target.reset(); loadMine(r.id);
    } catch (err) { fail(err); }
  });
  async function loadMine(openId) {
    const box = $('#mine', el);
    if (!session.token) { box.innerHTML = '<div class="panel"><h3 style="margin-top:0">Your requests</h3><p class="muted">Sign in to see your requests and approve proofs.</p><a class="btn ghost" href="#/login?next=%2Fpersonalize">Sign in</a></div>'; return; }
    const list = await api.get('/personalization/list');
    const details = await Promise.all(list.map(r => api.get('/personalization/' + r.id)));
    box.innerHTML = `<h3 style="margin:0">Your requests</h3>` + (details.length ? details.map(d => requestCard(d, String(d.request.id) === String(openId ?? params.open))).join('') : '<p class="muted">No requests yet.</p>');
    $$('[data-act]', box).forEach(b => b.addEventListener('click', () => act(b.dataset.act, Number(b.dataset.id), Number(b.dataset.v), () => loadMine(b.dataset.id))));
  }
  loadMine();
  return el;
}

function requestCard({ request: r, proofs }, open) {
  const latest = proofs[proofs.length - 1];
  return `<details class="panel" ${open ? 'open' : ''}><summary><b>${esc(KINDS[r.kind] || pretty(r.kind))}</b> “${esc(r.textContent)}” × ${r.quantity} ${statusBadge(r.status)}</summary>
    <dl class="kv" style="margin-top:12px"><dt>Reference</dt><dd>${esc(r.requestSn)}</dd>
      ${r.requestedDate ? `<dt>Needed by</dt><dd>${day(r.requestedDate)}</dd>` : ''}
      ${r.quoteAmount ? `<dt>Quote</dt><dd><b>${money(r.quoteAmount)}</b> · deposit ${money(r.depositAmount)} · lead time ${r.leadTimeDays} days</dd>` : '<dt>Quote</dt><dd class="muted">Waiting for our reply</dd>'}
      ${latest ? `<dt>Proof v${latest.version}</dt><dd>${esc(latest.artworkNote)}${latest.approvedByMember ? ' <span class="badge ok">Approved</span>' : ''}</dd>` : ''}
      ${r.adminNote ? `<dt>Notes</dt><dd style="white-space:pre-line">${esc(r.adminNote)}</dd>` : ''}</dl>
    <div class="row" style="margin-top:10px">
      ${r.status === 'PROOF_SENT' ? `<button class="btn small primary" data-act="approve" data-id="${r.id}" data-v="${r.proofVersion}">Approve proof v${r.proofVersion}</button><button class="btn small ghost" data-act="changes" data-id="${r.id}">Ask for changes</button>` : ''}
      ${r.status === 'PROOF_APPROVED' ? `<button class="btn small primary" data-act="deposit" data-id="${r.id}" data-amt="${r.depositAmount}">Pay deposit ${money(r.depositAmount)}</button>` : ''}
      ${['SUBMITTED', 'QUOTED', 'PROOF_SENT', 'PROOF_APPROVED'].includes(r.status) ? `<button class="btn small ghost" data-act="cancel" data-id="${r.id}">Cancel</button>` : ''}
      ${r.status === 'READY' ? '<span class="badge ok">Ready for collection — we saved it for you</span>' : ''}</div></details>`;
}

async function act(kind, id, version, reload) {
  try {
    if (kind === 'approve') { await api.post(`/personalization/${id}/approve`, { version }); toast('Proof approved. You can pay the deposit now.', 'ok'); }
    else if (kind === 'cancel') { if (!(await confirmDialog('Cancel this request?', 'You can start a new one at any time.', 'Cancel request'))) return; await api.post(`/personalization/${id}/cancel`); toast('Cancelled', 'ok'); }
    else if (kind === 'deposit') {
      const amt = $(`[data-act=deposit][data-id="${id}"]`)?.dataset.amt;
      const token = await askCard('Pay the deposit', money(amt));
      if (token === null) return;
      await api.post(`/personalization/${id}/payDeposit`, { cardToken: token }); toast('Deposit received. We will start on it.', 'ok');
    } else if (kind === 'changes') {
      const f = document.createElement('form');
      f.innerHTML = '<div class="field"><label>What should we change?</label><textarea name="note" required maxlength="500"></textarea></div>';
      await new Promise(done => modal({ title: 'Ask for changes', body: f, onClose: done, actions: [{ label: 'Cancel', kind: 'ghost' }, { label: 'Send', kind: 'primary', run: async () => { if (!f.reportValidity()) return true; await api.post(`/personalization/${id}/changes`, readForm(f)); toast('Sent. We will send a new proof.', 'ok'); } }] }));
    }
    await reload();
  } catch (e) { fail(e); }
}

export async function experiencesPage() {
  const sessions = await api.get('/experience/list');
  const el = document.createElement('div');
  el.innerHTML = `<h1>Tastings &amp; workshops</h1><p class="muted" style="max-width:70ch">Taste, compare and make. Seats are limited; book and pay online to confirm. You can cancel up to 24 hours before.</p>
  ${sessions.length ? `<div class="grid cols-2">${sessions.map(s => `<div class="panel stack"><div class="row between"><h3 style="margin:0"><a href="#/product/${s.productId}">${esc(s.productName)}</a></h3><span class="badge ${s.seatsLeft < 4 ? 'warn' : 'ok'}">${s.seatsLeft} seats left</span></div>
    <div>📅 <b>${dt(s.startsAt)}</b> · ${s.durationMinutes} min</div><div class="muted small">📍 ${esc(s.location || 'Main boutique')}</div>
    <div class="row between"><span><b>${money(s.price)}</b> per seat</span><span class="row"><select data-seats="${s.id}" style="width:auto">${[1, 2, 3, 4].filter(n => n <= s.seatsLeft).map(n => `<option>${n}</option>`).join('')}</select><button class="btn primary small" data-book="${s.id}" data-price="${s.price}">Book</button></span></div></div>`).join('')}</div>`
    : emptyState('🎟️', 'No sessions scheduled', 'New dates are added regularly. Check back soon.')}`;
  $$('[data-book]', el).forEach(b => b.addEventListener('click', async () => {
    if (!requireLogin()) return;
    const seats = Number($(`[data-seats="${b.dataset.book}"]`, el).value);
    try {
      const booking = await api.post('/experience/book', { sessionId: Number(b.dataset.book), seats });
      const token = await askCard('Pay for your booking', money(booking.amount));
      if (token === null) { toast('Your seats are held for a short time. Pay from Account › Bookings.'); location.hash = '#/account/bookings'; return; }
      await api.post('/experience/pay/' + booking.id, { cardToken: token });
      toast('Booked! See you there.', 'ok'); location.hash = '#/account/bookings';
    } catch (e) { fail(e); }
  }));
  return el;
}
