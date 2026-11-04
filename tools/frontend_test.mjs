import { readFile } from 'node:fs/promises';
import { strict as assert } from 'node:assert';
import { test } from 'node:test';

const load = async relative => import('data:text/javascript;base64,' + Buffer.from(await readFile(new URL(relative, import.meta.url), 'utf8')).toString('base64'));
const ui = await load('../choco-common/src/main/resources/static/shared/ui.js');
const atelier = await load('../choco-portal/src/main/resources/static/app/atelier.js');

test('dynamic text is escaped and query values are encoded', () => {
  assert.equal(ui.esc('<script>"&'), '&lt;script&gt;&quot;&amp;');
  assert.equal(ui.hashQ({ q: 'milk & dark', empty: '' }), '?q=milk+%26+dark');
});
test('collection contains twelve raised pieces and an accessible reset control', () => {
  const html = atelier.collectionScene();
  assert.equal((html.match(/class="bonbon bonbon-/g) || []).length, 12);
  assert.match(html, /tabindex="0"/);
  assert.match(html, /data-reset-scene/);
});
test('collection arrow-key rotation is bounded and reset restores the view', () => {
  const handlers = {}, model = { style: {} }, reset = {};
  const viewport = { addEventListener: (key, fn) => handlers[key] = fn };
  const stage = { querySelector: selector => selector === '.scene-viewport' ? viewport : selector === '.collection-model' ? model : reset };
  reset.addEventListener = (_, fn) => reset.click = fn;
  atelier.mountAtelier({ querySelector: () => stage });
  const initial = model.style.transform;
  handlers.keydown({ key: 'ArrowRight', preventDefault() {} });
  assert.notEqual(model.style.transform, initial);
  for (let i = 0; i < 30; i++) handlers.keydown({ key: 'ArrowUp', preventDefault() {} });
  assert.match(model.style.transform, /rotateX\(75deg\)/);
  reset.click(); assert.equal(model.style.transform, initial);
});
test('API client aborts a stalled request and exposes a useful error', async () => {
  const original = globalThis.fetch;
  globalThis.fetch = (_, { signal }) => new Promise((resolve, reject) => signal.addEventListener('abort', () => reject(new Error('aborted'))));
  try {
    const api = ui.createApi({ getToken: () => null });
    await assert.rejects(api.get('/test', {}, { timeoutMs: 10 }), /took too long/);
  } finally { globalThis.fetch = original; }
});
