import { api, rowsOf, table, pageHead, openForm, esc, $, $$, toast, fail, confirmDialog, pager, hashQ } from '/app/core.js';

/**
 * Builds a list + create/edit/delete page from a small description.
 * cfg: {title, subtitle, list, listParams, paged, columns, fields(): spec[] | Promise, create, update(id), del(id),
 *       delBody (true => POST {ids:[id]}), toValues(row), toBody(values, row), filters:[{name,label,type,options}],
 *       rowActions:[{label, kind, show(row), run(row, reload)}], noun, canCreate, canEdit}
 */
export function crud(cfg) {
  return async ({ params }) => {
    const el = document.createElement('div');
    const page = Number(params.page || 1);
    const query = { ...(cfg.listParams || {}), pageNum: page, pageSize: cfg.pageSize || 15 };
    (cfg.filters || []).forEach(f => { if (params[f.name]) query[f.name] = params[f.name]; });
    const data = await api.get(typeof cfg.list === 'function' ? cfg.list(params) : cfg.list, query);
    const rows = rowsOf(data);
    const noun = cfg.noun || 'item';
    const fieldsCache = { v: null };
    const getFields = async () => (fieldsCache.v ??= await (typeof cfg.fields === 'function' ? cfg.fields() : cfg.fields));
    const reload = () => window.dispatchEvent(new HashChangeEvent('hashchange'));

    const hasActions = cfg.update || cfg.del || (cfg.rowActions || []).length;
    const columns = [...cfg.columns];
    if (hasActions) {
      columns.push({
        label: '', cls: 'actions nowrap', html: (r, i) => [
          ...(cfg.rowActions || []).filter(a => !a.show || a.show(r)).map((a, k) => `<button class="btn small ${a.kind || 'ghost'}" data-act="${i}:${k}">${esc(a.label)}</button>`),
          cfg.update && cfg.canEdit !== false ? `<button class="btn small ghost" data-edit="${i}">Edit</button>` : '',
          cfg.del ? `<button class="btn small ghost" data-del="${i}">Delete</button>` : '',
        ].join(' '),
      });
    }
    const filterHtml = (cfg.filters || []).length ? `<form class="filters" id="flt">${cfg.filters.map(f => f.type === 'select'
      ? `<select name="${f.name}"><option value="">${esc(f.label)}</option>${f.options.map(o => `<option value="${esc(o.value)}" ${params[f.name] === String(o.value) ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>`
      : `<input name="${f.name}" placeholder="${esc(f.label)}" value="${esc(params[f.name] || '')}">`).join('')}<button class="btn small">Filter</button></form>` : '';
    el.innerHTML = pageHead(cfg.title, cfg.subtitle, cfg.create && cfg.canCreate !== false ? `<button class="btn primary" id="add">+ Add ${esc(noun)}</button>` : '') + filterHtml
      + table(columns, rows, cfg.empty || `No ${noun}s yet. Add the first one.`);
    if (!Array.isArray(data) && data?.totalPage > 1) el.appendChild(pager(page, data.totalPage, n => { location.hash = location.hash.split('?')[0] + hashQ({ ...params, page: n }); }));

    $('#flt', el)?.addEventListener('submit', e => {
      e.preventDefault();
      const f = Object.fromEntries(new FormData(e.target)); location.hash = location.hash.split('?')[0] + hashQ(f);
    });
    $('#add', el)?.addEventListener('click', async () => {
      try {
        const fields = await getFields();
        openForm({ title: `Add ${noun}`, fields, values: cfg.defaults || {}, submit: v => api.post(cfg.create, cfg.toBody ? cfg.toBody(v) : v), after: reload });
      } catch (e) { fail(e); }
    });
    $$('[data-edit]', el).forEach(b => b.addEventListener('click', async () => {
      const row = rows[Number(b.dataset.edit)];
      try {
        const fields = await getFields();
        openForm({ title: `Edit ${noun}`, fields, values: cfg.toValues ? cfg.toValues(row) : row, submit: v => api.post(cfg.update(row.id), cfg.toBody ? cfg.toBody(v, row) : v), after: reload });
      } catch (e) { fail(e); }
    }));
    $$('[data-del]', el).forEach(b => b.addEventListener('click', async () => {
      const row = rows[Number(b.dataset.del)];
      if (!(await confirmDialog(`Delete this ${noun}?`, 'This cannot be undone.', 'Delete'))) return;
      try { await (cfg.delBody ? api.post(cfg.del(row.id), { ids: [row.id] }) : api.post(cfg.del(row.id))); toast('Deleted', 'ok'); reload(); } catch (e) { fail(e); }
    }));
    $$('[data-act]', el).forEach(b => b.addEventListener('click', async () => {
      const [i, k] = b.dataset.act.split(':').map(Number);
      const row = rows[i];
      const visible = cfg.rowActions.filter(a => !a.show || a.show(row));
      try { await visible[k].run(row, reload); } catch (e) { fail(e); }
    }));
    return el;
  };
}
