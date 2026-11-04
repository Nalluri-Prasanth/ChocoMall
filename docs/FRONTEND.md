# Frontend experience

The storefront is served by the portal on port 8085; the staff workspace is served by the admin application on port 8080.
Both use native JavaScript modules and the existing Spring Boot APIs. No separate frontend build or hosting service is needed.

## Visual system

Off-white surfaces, cocoa accents and teal navigation distinguish browsing from operational work. Shared tokens, controls,
focus outlines and compact tables live in `/shared/ui.css` and `/shared/refinement.css`. Each app adds its own layout layer.
The storefront adapts to small screens with a horizontally scrollable navigation bar and a two-column product collection.

## Collection display

`atelier.js` and `atelier.css` create an illustrative, dimensional gift box using CSS perspective and raised chocolates.
Drag the display or focus it and press arrow keys. Reset view restores its initial orientation. This is an illustration,
not a product photograph or a CAD model. It needs no WebGL dependency, external assets or continuous animation loop.
Unsupported 3D transforms fall back to a flat collection; reduced-motion preferences disable transitions.

## Discovery

`/#/discover` accepts flavour, maximum item price and verified dietary preference, then queries the live product-search API.
It is a deterministic catalog discovery tool, not a generative assistant. Empty and failed searches have explicit states.
Dietary filtering does not replace product allergen labels or advice from shop staff.

## Accessibility and maintenance

The skip link focuses content without altering the application route. Forms associate conventional field labels with inputs.
Dialogs trap keyboard focus and restore it on close. All controls have visible focus indicators.
Check storefront, search, product, basket, checkout, account and staff product editing when changing shared styles.
Run `./mvnw verify` for Java integration tests and `python tools/smoke_test.py` against local demo apps for the order lifecycle.
Run `node --test tools/frontend_test.mjs` for escaping, collection keyboard controls, reset behaviour and stalled-request tests.
The smoke script creates test customers and orders; use a development database, never production.
