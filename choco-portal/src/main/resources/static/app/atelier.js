/** Lightweight dimensional collection display; no rendering library required. */
export function collectionScene() {
  const pieces = Array.from({ length: 12 }, (_, i) => `<span class="bonbon bonbon-${i % 3}"><i></i></span>`).join('');
  return `<div class="atelier" data-atelier>
    <div class="atelier-label"><span>THE COLLECTION / 01</span><span>Interactive 3D</span></div>
    <div class="scene-viewport" tabindex="0" role="group" aria-label="Rotate the illustrated chocolate box. Drag, or use arrow keys.">
      <div class="scene-shadow"></div><div class="collection-model" aria-hidden="true"><div class="box-base"></div><div class="box-edge edge-front"></div><div class="box-edge edge-back"></div><div class="box-edge edge-left"></div><div class="box-edge edge-right"></div><div class="bonbon-grid">${pieces}</div></div>
    </div>
    <div class="scene-controls"><span>Drag to discover · arrow keys to rotate</span><button class="btn small ghost" type="button" data-reset-scene>Reset view</button></div>
    <noscript><img src="/app/collection.svg" alt="Illustrated chocolate assortment"></noscript>
  </div>`;
}

export function mountAtelier(root) {
  const stage = root.querySelector('[data-atelier]');
  if (!stage) return;
  const viewport = stage.querySelector('.scene-viewport');
  const model = stage.querySelector('.collection-model');
  let x = 52, z = -24, drag = null;
  const paint = () => { model.style.transform = `rotateX(${x}deg) rotateZ(${z}deg)`; };
  const move = (dx, dy) => { z += dx; x = Math.max(20, Math.min(75, x + dy)); paint(); };
  viewport.addEventListener('pointerdown', e => {
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    drag = { x: e.clientX, y: e.clientY }; viewport.setPointerCapture(e.pointerId);
    viewport.classList.add('dragging');
  });
  viewport.addEventListener('pointermove', e => {
    if (!drag) return;
    move((e.clientX - drag.x) * .4, (e.clientY - drag.y) * -.25);
    drag = { x: e.clientX, y: e.clientY };
  });
  const stop = () => { drag = null; viewport.classList.remove('dragging'); };
  viewport.addEventListener('pointerup', stop); viewport.addEventListener('pointercancel', stop);
  viewport.addEventListener('keydown', e => {
    const delta = { ArrowLeft: [-8, 0], ArrowRight: [8, 0], ArrowUp: [0, 5], ArrowDown: [0, -5] }[e.key];
    if (delta) { e.preventDefault(); move(...delta); }
  });
  stage.querySelector('[data-reset-scene]').addEventListener('click', () => { x = 52; z = -24; paint(); });
  paint();
}
