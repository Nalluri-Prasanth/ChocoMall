import { api, rowsOf, idMap, openForm, yesNo, badge, esc, $, $$, dt, toast, fail, modal, session, pretty } from '/app/core.js';
import { crud } from '/app/crud.js';

const flattenMenus = (nodes, depth = 0, out = []) => { nodes.forEach(n => { out.push({ value: n.id, label: '— '.repeat(depth) + n.title }); flattenMenus(n.children || [], depth + 1, out); }); return out; };

export const staff = crud({
  title: 'Staff', subtitle: 'People who can sign in to this back office. What they can do depends on their roles.', noun: 'staff member', list: '/admin/list',
  columns: [{ key: 'username', label: 'Username' }, { key: 'nickName', label: 'Name' }, { key: 'email', label: 'Email' }, { label: 'Last sign-in', get: r => dt(r.loginTime) }, { label: 'Active', html: r => yesNo(r.status === 1) }],
  fields: [{ name: 'username', label: 'Username', required: true, minlength: 3, pattern: '[A-Za-z0-9._-]+' }, { name: 'password', label: 'Password', type: 'password', minlength: 8, maxlength: 72, hint: 'At least 8 characters. Leave empty when editing to keep it.' },
    { name: 'nickName', label: 'Display name' }, { name: 'email', label: 'Email', type: 'email' }, { name: 'note', label: 'Note', full: true }],
  create: '/admin/register', update: id => `/admin/update/${id}`, del: id => `/admin/delete/${id}`,
  toBody: (v, row) => ({ ...v, status: row?.status ?? 1 }),
  rowActions: [
    {
      label: 'Roles…', run: async row => {
        const [roles, mine] = await Promise.all([idMap('/role/listAll'), api.get('/admin/role/' + row.id)]);
        const have = new Set(rowsOf(mine).map(r => r.id));
        openForm({ title: `Roles for ${row.username}`, wide: false, values: { roleIds: [...have] }, fields: [{ name: 'roleIds', label: 'Roles', type: 'ids', options: roles }], submit: v => api.post('/admin/role/update', { adminId: row.id, roleIds: v.roleIds }) });
      },
    },
    { label: 'Password…', run: row => openForm({ title: `New password for ${row.username}`, wide: false, fields: [{ name: 'newPassword', label: 'New password', type: 'password', required: true, minlength: 8 }, ...(row.id === session.info?.id ? [{ name: 'oldPassword', label: 'Current password', type: 'password', required: true }] : [])], submit: v => api.post('/admin/password', { username: row.username, ...v }) }) },
    { label: 'Enable / disable', run: async (row, reload) => { await api.postQ(`/admin/updateStatus/${row.id}`, { status: row.status === 1 ? 0 : 1 }); toast('Updated', 'ok'); reload(); } },
  ],
});

export async function rolesPage() {
  const roles = rowsOf(await api.get('/role/list', { pageSize: 100 }));
  const el = document.createElement('div');
  const reload = () => window.dispatchEvent(new HashChangeEvent('hashchange'));
  el.innerHTML = `<div class="page-head"><div><h1>Roles</h1><div class="muted">A role is a set of menus and the URL patterns it may call. Every request is checked against them.</div></div><button class="btn primary" id="add">+ Add role</button></div>
    <div class="table-wrap"><table><thead><tr><th>Role</th><th>Description</th><th class="num">Staff</th><th class="num">Menus</th><th class="num">Permissions</th><th>Active</th><th></th></tr></thead><tbody>${roles.map((r, i) => `<tr><td><b>${esc(r.name)}</b></td><td class="small">${esc(r.description || '')}</td>
      <td class="num">${r.adminCount}</td><td class="num">${r.menuIds.length}</td><td class="num">${r.resourceIds.length}</td><td>${yesNo(r.status === 1)}</td>
      <td class="actions nowrap"><button class="btn small ghost" data-menu="${i}">Menus</button> <button class="btn small ghost" data-res="${i}">Permissions</button> <button class="btn small ghost" data-edit="${i}">Edit</button> ${r.name === 'SUPER_ADMIN' ? '' : `<button class="btn small ghost" data-del="${i}">Delete</button>`}</td></tr>`).join('')}</tbody></table></div>`;
  const fields = [{ name: 'name', label: 'Name', required: true }, { name: 'description', label: 'Description', full: true }, { name: 'sort', label: 'Sort', type: 'int', def: 0 }];
  $('#add', el).addEventListener('click', () => openForm({ title: 'Add role', wide: false, fields, submit: v => api.post('/role/create', v), after: reload }));
  $$('[data-edit]', el).forEach(b => b.addEventListener('click', () => { const r = roles[Number(b.dataset.edit)]; openForm({ title: 'Edit role', wide: false, fields, values: r, submit: v => api.post(`/role/update/${r.id}`, v), after: reload }); }));
  $$('[data-del]', el).forEach(b => b.addEventListener('click', async () => { const r = roles[Number(b.dataset.del)]; if (!confirm(`Delete the role “${r.name}”?`)) return; try { await api.post(`/role/delete/${r.id}`); reload(); } catch (e) { fail(e); } }));
  $$('[data-menu]', el).forEach(b => b.addEventListener('click', async () => {
    const r = roles[Number(b.dataset.menu)];
    try {
      const tree = await api.get('/menu/treeList');
      openForm({ title: `Menus for ${r.name}`, fields: [{ name: 'ids', label: 'Visible menus', type: 'ids', options: flattenMenus(tree) }], values: { ids: r.menuIds }, submit: v => api.post('/role/allocMenu', { roleId: r.id, ids: v.ids }), after: reload });
    } catch (e) { fail(e); }
  }));
  $$('[data-res]', el).forEach(b => b.addEventListener('click', async () => {
    const r = roles[Number(b.dataset.res)];
    try {
      const all = await api.get('/resource/listAll');
      openForm({ title: `Permissions for ${r.name}`, fields: [{ name: 'ids', label: 'Allowed URL patterns', type: 'ids', options: all.map(x => ({ value: x.id, label: `${x.name}  ${x.url}` })) }], values: { ids: r.resourceIds }, submit: v => api.post('/role/allocResource', { roleId: r.id, ids: v.ids }), after: reload });
    } catch (e) { fail(e); }
  }));
  return el;
}
