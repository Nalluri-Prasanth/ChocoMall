import { createApi, esc, $, $$, toast, fail, modal, confirmDialog, readForm, pager, hashQ, pretty, badge, dt } from '/shared/ui.js';

export const session = { token: localStorage.getItem('choco.admin.token') || '', info: null };
export const events = new Set();

export const api = createApi({
  getToken: () => session.token,
  onUnauthorized: () => { if (session.token) { clearSession(); location.hash = '#/login'; toast('Your session has ended. Please sign in again.', 'error'); } },
});

export function clearSession() {
  session.token = ''; session.info = null;
  localStorage.removeItem('choco.admin.token');
  events.forEach(f => f());
}
export async function setToken(token) {
  session.token = token; localStorage.setItem('choco.admin.token', token);
  session.info = await api.get('/admin/info');
  events.forEach(f => f());
}
export async function restore() {
  if (!session.token) return;
  try { session.info = await api.get('/admin/info'); } catch { clearSession(); }
}

// ------------------------------------------------------------------ helpers
export const rowsOf = data => Array.isArray(data) ? data : (data?.list ?? []);
export const idMap = async (path, label = 'name', params) => {
  const rows = rowsOf(await api.get(path, { pageSize: 500, ...params }));
  return rows.map(r => ({ value: r.id, label: r[label] ?? r.name ?? r.title ?? r.id }));
};
export const enumOptions = list => list.map(v => ({ value: v, label: pretty(v) }));

export const ENUMS = {
  ProductKind: ['MADE_IN_SHOP', 'MADE_FRESH', 'MADE_TO_ORDER', 'SEALED_RETAIL', 'EXPERIENCE', 'PERSONALIZED'],
  Priority: ['CORE', 'EXPAND', 'TEST', 'SPECIALIST'],
  StockReason: ['PRODUCTION', 'WASTE', 'ADJUSTMENT', 'RETURN'],
  ClaimType: ['VEGAN', 'PLANT_BASED', 'GLUTEN_FREE', 'HALAL', 'KOSHER', 'ORGANIC', 'NO_ADDED_SUGAR', 'REDUCED_SUGAR', 'SINGLE_ORIGIN'],
  GuideStatus: ['TODO', 'IN_PROGRESS', 'DONE', 'NOT_APPLICABLE'],
  OrderStatus: ['PENDING_PAYMENT', 'PREPARING', 'READY_FOR_PICKUP', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'INVALID'],
  PayMethod: ['CARD', 'BANK_TRANSFER', 'CASH_AT_COUNTER'],
  ReturnStatus: ['PENDING', 'ACCEPTED', 'COMPLETED', 'REJECTED'],
  CouponType: ['ALL', 'CATEGORY', 'PRODUCT'],
  HomeSlotType: ['BANNER', 'BRAND', 'NEW_PRODUCT', 'RECOMMEND_PRODUCT', 'RECOMMEND_SUBJECT'],
  BrandStockingStatus: ['CANDIDATE', 'SAMPLING', 'AUTHORIZED', 'STOCKED', 'ROTATED_OUT'],
  PersonalizationStatus: ['SUBMITTED', 'QUOTED', 'PROOF_SENT', 'PROOF_APPROVED', 'DEPOSIT_PAID', 'IN_PRODUCTION', 'READY', 'COMPLETED', 'CANCELLED', 'REJECTED'],
  FulfillmentType: ['PICKUP', 'DELIVERY'],
};

/** "2026-10-08T14:30:00" -> value for a datetime-local input */
export const toLocalInput = v => v ? String(v).slice(0, 16) : '';

// ------------------------------------------------------------------ form builder
/**
 * Field spec: {name,label,type,options,required,hint,min,max,step,maxlength,def,full}
 * types: text, textarea, number, int, bool, select, datetime, date, email, password, ids (multi-select chips), image
 */
export function fieldHtml(f, values = {}) {
  const v = values[f.name] ?? f.def ?? '';
  const req = f.required ? 'required' : '';
  const label = `<label>${esc(f.label)}${f.required ? '' : ' <span class="muted">(optional)</span>'}</label>`;
  const hint = f.hint ? `<div class="hint">${esc(f.hint)}</div>` : '';
  const wrap = body => `<div class="field" style="${f.full ? 'grid-column:1/-1' : ''}">${body}${hint}</div>`;
  switch (f.type) {
    case 'textarea': return wrap(`${label}<textarea name="${f.name}" ${req} maxlength="${f.maxlength || 2000}" rows="${f.rows || 3}">${esc(v)}</textarea>`);
    case 'number': return wrap(`${label}<input name="${f.name}" data-type="number" type="number" step="${f.step || '0.01'}" ${f.min !== undefined ? `min="${f.min}"` : ''} value="${esc(v)}" ${req}>`);
    case 'int': return wrap(`${label}<input name="${f.name}" data-type="int" type="number" step="1" ${f.min !== undefined ? `min="${f.min}"` : ''} value="${esc(v)}" ${req}>`);
    case 'bool': return `<label class="check" style="${f.full ? 'grid-column:1/-1' : 'align-self:end;padding-bottom:10px'}"><input type="checkbox" name="${f.name}" ${(v === '' ? f.def : v) ? 'checked' : ''}> ${esc(f.label)}</label>`;
    case 'select': return wrap(`${label}<select name="${f.name}" ${req}>${f.required ? '' : '<option value="">—</option>'}${(f.options || []).map(o => `<option value="${esc(o.value)}" ${String(o.value) === String(v) ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>`);
    case 'datetime': return wrap(`${label}<input name="${f.name}" type="datetime-local" value="${esc(toLocalInput(v))}" ${req}>`);
    case 'date': return wrap(`${label}<input name="${f.name}" type="date" value="${esc(String(v).slice(0, 10))}" ${req}>`);
    case 'ids': {
      const set = new Set((Array.isArray(v) ? v : [...(v || [])]).map(String));
      return `<div class="field" style="grid-column:1/-1">${label}<div class="chips">${(f.options || []).map(o => `<label class="chip"><input type="checkbox" name="${f.name}[]" data-type="int" value="${esc(o.value)}" ${set.has(String(o.value)) ? 'checked' : ''}>${esc(o.label)}</label>`).join('')}</div>${hint}</div>`;
    }
    case 'image': return wrap(`${label}<div class="row"><input name="${f.name}" class="grow" value="${esc(v)}" maxlength="500" placeholder="https://… or upload"><label class="btn ghost small" style="margin:0">Upload<input type="file" accept="image/*" data-upload="${f.name}" hidden></label><img class="thumb" alt="" ${v ? `src="${esc(v)}"` : 'hidden'}></div>`);
    default: return wrap(`${label}<input name="${f.name}" type="${f.type === 'email' ? 'email' : f.type === 'password' ? 'password' : 'text'}" value="${esc(v)}" maxlength="${f.maxlength || 200}" ${f.minlength ? `minlength="${f.minlength}"` : ''} ${req} ${f.pattern ? `pattern="${f.pattern}"` : ''} autocomplete="off">`);
  }
}

export function buildForm(fields, values = {}) {
  const form = document.createElement('form');
  form.className = 'form-grid';
  form.noValidate = false;
  form.innerHTML = fields.map(f => fieldHtml(f, values)).join('');
  $$('[data-upload]', form).forEach(inp => inp.addEventListener('change', async () => {
    const file = inp.files[0]; if (!file) return;
    try {
      const { url } = await api.upload('/file/upload', file);
      const field = $(`[name="${inp.dataset.upload}"]`, form);
      field.value = url; const img = inp.closest('.field').querySelector('img'); img.src = url; img.hidden = false;
      toast('Image uploaded', 'ok');
    } catch (e) { fail(e); }
  }));
  return form;
}

/** Datetime-local gives "YYYY-MM-DDTHH:mm"; the API accepts it as is. Empty optional fields are dropped by readForm. */
export function collect(form, fields) {
  const out = readForm(form);
  fields.filter(f => f.type === 'ids').forEach(f => { out[f.name] = out[f.name] || []; });
  return out;
}

/** Opens a create/edit modal. `submit(values)` performs the request. */
export function openForm({ title, fields, values = {}, submit, wide = true, label = 'Save', after }) {
  const form = buildForm(fields, values);
  modal({
    title, body: form, wide, actions: [{ label: 'Cancel', kind: 'ghost' }, {
      label, kind: 'primary', run: async () => {
        if (!form.reportValidity()) return true;
        await submit(collect(form, fields));
        toast('Saved', 'ok');
        await after?.();
      },
    }],
  });
}

// ------------------------------------------------------------------ table
export function table(columns, rows, empty = 'Nothing here yet.') {
  if (!rows.length) return `<div class="empty"><p>${esc(empty)}</p></div>`;
  return `<div class="table-wrap"><table><thead><tr>${columns.map(c => `<th class="${c.num ? 'num' : ''}">${esc(c.label)}</th>`).join('')}</tr></thead>
    <tbody>${rows.map((r, i) => `<tr data-row="${i}">${columns.map(c => `<td class="${c.num ? 'num' : ''} ${c.cls || ''}">${c.html ? c.html(r, i) : esc(c.get ? c.get(r) : r[c.key] ?? '')}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
}

export const yesNo = v => v ? '<span class="badge ok">Yes</span>' : '<span class="badge">No</span>';
export const thumb = (url, name) => url ? `<img class="thumb" src="${esc(url)}" alt="${esc(name || '')}">` : '<span class="thumb"></span>';

export function pageHead(title, subtitle = '', actions = '') {
  return `<div class="page-head"><div><h1>${esc(title)}</h1>${subtitle ? `<div class="muted">${esc(subtitle)}</div>` : ''}</div><div class="row">${actions}</div></div>`;
}

export * from '/shared/ui.js';
