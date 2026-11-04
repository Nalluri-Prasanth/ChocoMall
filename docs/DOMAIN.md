# How the shop guide became software

The source is *Chocolate Shop Complete Guide* (workbook with 10 sheets, prepared 08 Oct 2026). `tools/xlsx_to_seed.py` converts it into
CSV files (`choco-model/src/main/resources/seed/`) that the application loads on start-up; a second overlay maps the playbook rows to
platform features ([coverage](GUIDE_COVERAGE.md)).

```bash
python tools/xlsx_to_seed.py "path/to/Chocolate_Shop_Complete_Guide.xlsx"   # regenerate the seed CSVs from an updated workbook
python tools/guide_coverage.py                                              # regenerate the playbook overlay and GUIDE_COVERAGE.md
```

Row counts are verified by an automated test (`SeedDataTest`).

## Sheet by sheet

| Sheet | Rows | Becomes | In the product |
|---|---|---|---|
| **Shop Recommendation** | 8 formats, 5 steps, 8 notes | `ops_guide_item` (types `SHOP_FORMAT`, `ROADMAP`, `SHOP_CONCEPT`) | Visible in the back-office *Launch playbook*. The 5-step roadmap (Validate, Launch, Expand, Personalize, Specialize) is the model behind the launch tiers and the preorder-first approach. The "practical buying shortlist" (13 brands) starts in the *sampling* stage of the brand workflow. |
| **Chocolate Types** | 28 | `pms_chocolate_type` | Reference list; products are linked by keyword (milk, dark, white, ruby, blond, single-origin) and staff can change the link. Classifications (base, specialty, ingredient, sourcing style...) are preserved. |
| **Flavor Variations** | 50 | `pms_flavor` | Linked to products (name match), used for filtering ("raspberry"), shown on product pages, and chosen by customers as consent-based preferences for recommendations. |
| **Products to Sell** | 60 | `pms_product` + `pms_product_sku` + categories | The demo shop. Category, name, selling format, purpose and **priority** come from the sheet. See the visibility rules below. |
| **Foods and Drinks** | 80 | `cms_food_pairing` | Storefront pairing guide (Core and Expand rows only; Test/Specialist ideas stay internal), filterable by food group. |
| **Ways to Eat** | 45 | `cms_serving_format` | Storefront "ways to enjoy" guide and the basis for tasting and workshop experiences. |
| **Production Processes** | 36 + 8 sources | `ops_guide_item` (`PRODUCTION`), `ops_guide_source` | Checklist plus **enforced controls**: allergen check before releasing a batch (M32), temper test for tempered work (M09), best-before date within the validated shelf life (M31, M34), waste tracking (M34), chilled products restricted to pickup (M35). Reference links are kept. |
| **3D Chocolate Printing** | 26 | `ops_guide_item` (`PRINTING`) + the personalisation workflow | Name plaques, monograms, logo plaques, message strips, place-name cards and silhouettes are request kinds. Workflow: quote with a **tested lead time** (D21), **versioned proofs** the customer approves (D14), deposit **only after approval** (D23), permission note required for logos (D03). |
| **Sales and Launch** | 30 | `ops_guide_item` (`SALES_LAUNCH`) + features | 15 of 30 are fully delivered by the platform, 9 more are supported ([coverage](GUIDE_COVERAGE.md)). |
| **Global Brands** | 199 | `pms_brand` | Brand directory with region, market, segment, formats, shop role, evidence status and reference URL. Brands backed by official or government evidence are shown on the storefront; "candidate, range unverified" brands stay internal. A stocking workflow records the authorized supplier and landed cost. |

## Launch tier → visibility (Products to Sell)

The guide's *Priority* column decides what is on the shelf at launch ("start with a small core assortment", "remove weak sellers before adding more").

| Priority | Meaning in the guide | In the demo shop |
|---|---|---|
| Core | first launch shortlist | published, recommended |
| Expand | add after demand | published, marked new |
| Test | limited pilot | published only when sold by request (personalised items, tastings); otherwise draft |
| Specialist | needs dedicated skill or controls | draft (for example gelato, sorbet) |

Staff can publish or unpublish any product, but publishing is blocked until its allergens are reviewed and it has an active SKU.

## Product kinds and what they imply

| Kind | Examples | Behaviour |
|---|---|---|
| `MADE_IN_SHOP` | bonbons, bars, barks | tracked in stock; production batches add stock |
| `MADE_FRESH` | brownies, tarts, dipped strawberries, frozen items | tracked in stock; chilled ones are pickup only |
| `MADE_TO_ORDER` | hot chocolate, shakes | not stock-tracked |
| `SEALED_RETAIL` | imported shelf, world tasting set | tracked in stock |
| `PERSONALIZED` | name plaque, monogram, edible photo plaque | requested, quoted and approved instead of added to the basket |
| `EXPERIENCE` | tasting flight, make-your-own bark, pairing workshop | booked by session and seat |

Items whose selling format says *preorder* are preorder-only: they cannot be mixed with items available now and consume the capacity of a
preorder window. Bonbons are sold as single pieces and boxes of 6 and 12 from the same stock ("treat box sizes as packaging options, not
separate recipes"); **Build-a-box bonbons** (P47) lets a customer fill a box of 6, 12 or 24 with any bonbons that are on the shelf.

## Demo data: what is real and what is a placeholder

| Real (from the guide) | Added so the shop can trade (replace before real use) |
|---|---|
| names, categories, selling formats, sales purposes, priorities, all reference catalogs, brand evidence, process controls | prices, ingredient/packaging/labour cost percentages, starting stock, SKU discounts for boxes, coupons, bundles, sessions and preorder windows, help articles |
| flavor and chocolate-type links by name match | **allergen links by keyword** (must be verified against real recipes), image URLs (none) |

The guide states its own limits and the product respects them: dietary claims need evidence, shelf life needs validation (a product has no
default shelf life), and "recipes, shelf life, imports and local approvals require product-specific validation".
