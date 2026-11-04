"""Maps every playbook row of the shop guide to what the platform provides.

status here means platform coverage at seed time:
  DONE         the software fully delivers the item
  IN_PROGRESS  the software supports it, but the shop still has to do something
  TODO         not covered by software (physical work or out of scope)
Staff update the status in the back office as the shop puts each item into practice.
"""
import csv, pathlib, sys

D, P, T = "DONE", "IN_PROGRESS", "TODO"

MAP = {
    # ---- production processes
    "M01": (P, "Brand directory with the stocking workflow: candidate, sampling, authorized, stocked"),
    "M05": (P, "Batch release records the temper test for tempered processes (M04-M12)"),
    "M06": (P, "Batch release records the temper test for tempered processes (M04-M12)"),
    "M07": (P, "Batch release records the temper test for tempered processes (M04-M12)"),
    "M08": (P, "Batch release records the temper test for tempered processes (M04-M12)"),
    "M09": (D, "A passed temper test is required to release a tempered batch; a failed test blocks release"),
    "M10": (P, "Production batches with process code, quantity and waste"),
    "M11": (P, "Production batches with process code, quantity and waste"),
    "M12": (P, "Production batches with process code, quantity and waste"),
    "M13": (P, "Production batches with process code, quantity and waste"),
    "M14": (P, "Batch best-before date is limited to the product's validated shelf life"),
    "M15": (P, "Production batches with allergen check (nut allergen segregation is a floor procedure)"),
    "M16": (P, "Production batches with process code, quantity and waste"),
    "M17": (P, "Production batches with process code, quantity and waste"),
    "M18": (P, "Production batches with process code, quantity and waste"),
    "M19": (P, "Production batches; shelf life must be validated before a best-before date is accepted"),
    "M20": (P, "Production batches with process code, quantity and waste"),
    "M22": (P, "Chilled flag on fresh-fruit products; pickup only; short sale window is a floor procedure"),
    "M23": (P, "Chilled flag on frozen products; Specialist tier keeps them in draft"),
    "M29": (P, "Personalisation workflow: quote, tested lead time, versioned proof, deposit, production"),
    "M31": (P, "Product shelf-life field; batch release enforces best-before within it"),
    "M32": (D, "Allergens per product; publishing is blocked until reviewed; allergen check required to release a batch; shown on the storefront"),
    "M33": (P, "Every batch has a traceable lot code; the storefront shows allergens"),
    "M34": (P, "Best-before per batch, stock ledger and waste report by product"),
    "M35": (P, "Chilled products are flagged and sold for pickup only"),
    # ---- 3D printing
    "D01": (D, "Personalisation kind NAME_PLAQUE with text-length limits"),
    "D02": (D, "Personalisation kind MONOGRAM_MEDALLION"),
    "D03": (D, "Personalisation kind LOGO_PLAQUE; a note recording permission to use the mark is mandatory when quoting"),
    "D04": (D, "Personalisation kind MESSAGE_STRIP with text-length limits"),
    "D05": (D, "Personalisation kind PLACE_NAME_CARD"),
    "D06": (D, "Personalisation kind SILHOUETTE"),
    "D14": (D, "Artwork proofs are versioned; the customer approves a specific version"),
    "D21": (P, "The quote records a tested lead time in days"),
    "D23": (D, "Quote, lead time, proof approval and deposit before production; nothing is charged against unagreed artwork"),
    "D26": (P, "Contribution report with materials, labour and fees per product"),
    # ---- sales and launch
    "S01": (P, "Preorder windows with paid orders; sales and repeat-order reports"),
    "S02": (D, "Launch tiers (Core, Expand, Test, Specialist) decide what is on the shelf; contribution report shows weak sellers"),
    "S04": (D, "SKU sizes (single, box of 6, box of 12) with box pricing"),
    "S05": (D, "Build-a-box: fixed capacity, choose pieces from bonbons in stock, pieces reserved individually"),
    "S06": (D, "Bundles priced below the sum of the parts, applied automatically at checkout"),
    "S07": (D, "Optional gift sleeve (priced) and gift message at checkout"),
    "S08": (P, "Corporate assortment is a preorder product; deposits run through personalisation requests"),
    "S09": (P, "Monogram medallions via personalisation; preorder windows for event dates"),
    "S10": (P, "Brand directory with stocking workflow and an imported-shelf category"),
    "S11": (P, "Single-origin chocolate types and editorial subjects; sourcing claims need recorded evidence"),
    "S12": (P, "Flavor catalog linked to products; storefront shows flavors"),
    "S14": (D, "Preorder windows with an order cut-off and a capacity cap that is enforced atomically"),
    "S15": (D, "Points earned on paid orders, redemption capped per order, levels and a transparent points history"),
    "S17": (D, "Experience sessions: seat booking, card payment, cancellation notice, refund when a session is cancelled"),
    "S19": (D, "Storefront with ingredients, allergens, collection time and capacity-checked preorders"),
    "S20": (P, "Delivery option with fee and free-delivery threshold; chilled items are pickup only; packing trials are a floor task"),
    "S21": (D, "Personalisation requests: quote, tested lead time, proof, deposit, production"),
    "S22": (D, "Flavor preferences are stored only with explicit consent and can be deleted at any time"),
    "S23": (D, "Category tree separates everyday treats, gifts and discovery items; prices and contents stay visible"),
    "S24": (D, "Sales, waste and contribution reports per product"),
    "S25": (D, "Per-product ingredient, packaging and labour cost plus a selling-fee rate; contribution per unit and per period"),
    "S26": (P, "Test tier products and new/recommended flags support one-flavor-at-a-time trials"),
    "S27": (P, "Brand stocking workflow records the authorized supplier and the landed cost"),
    "S30": (D, "A dietary or sourcing claim is hidden from shoppers until staff verify recorded evidence"),
    # ---- shop formats
    "SF01": (P, "Pickup orders, drinks, gift boxes and a small core assortment"),
    "SF02": (P, "Preorder windows, gift packaging, build-a-box"),
    "SF03": (P, "Brand directory and imported-shelf category"),
    "SF06": (P, "Personalisation workflow"),
    # ---- roadmap
    "RM01": (P, "Preorder windows and paid-order reporting for pop-up validation"),
    "RM02": (D, "Core-tier products are live; everything else stays in draft until proven"),
    "RM03": (P, "Expand-tier products can be published one at a time, backed by contribution reporting"),
    "RM04": (P, "Personalisation requests with quote, proof and deposit"),
    "RM05": (P, "Specialist-tier products stay in draft until capacity and demand are proven"),
}


def main(seed_dir, doc_path):
    seed = pathlib.Path(seed_dir)
    with open(seed / "guide_implementation.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, quoting=csv.QUOTE_ALL, lineterminator="\n")
        w.writerow(["code", "status", "implemented_by"])
        for code, (status, text) in MAP.items():
            w.writerow([code, status, text])

    rows = list(csv.DictReader(open(seed / "guide_items.csv", encoding="utf-8")))
    titles = {"PRODUCTION": "Production processes (M01-M36)", "PRINTING": "3D chocolate printing (D01-D26)",
              "SALES_LAUNCH": "Sales and launch (S01-S30)", "SHOP_FORMAT": "Shop formats (SF01-SF08)", "ROADMAP": "Roadmap (RM01-RM05)"}
    out = ["# Guide coverage", "",
           "How each row of the *Chocolate Shop Complete Guide* is supported by the platform. The status column is the value",
           "seeded into the back-office **Launch playbook**: **DONE** means the software fully delivers the item, **IN_PROGRESS**",
           "means the software supports it but the shop still has work to do, **TODO** means it is physical work or out of",
           "scope for the software. Staff update the status in the back office as each item is put into practice.", "",
           "This file is generated by `python tools/guide_coverage.py`, which also writes the seed file `guide_implementation.csv`. Change the mapping in that script and regenerate instead of editing this file by hand.", ""]
    totals = {}
    for gtype, heading in titles.items():
        section = [r for r in rows if r["guide_type"] == gtype]
        done = sum(1 for r in section if MAP.get(r["code"], (T,))[0] == D)
        prog = sum(1 for r in section if MAP.get(r["code"], (T,))[0] == P)
        totals[gtype] = (len(section), done, prog)
        out += ["## " + heading, "", "%d items: %d done, %d supported, %d not covered" % (len(section), done, prog, len(section) - done - prog), "",
                "| Code | Item | Status | How the platform supports it |", "|---|---|---|---|"]
        for r in section:
            status, text = MAP.get(r["code"], (T, ""))
            title = r["title"].replace("|", "/")
            out.append("| %s | %s | %s | %s |" % (r["code"], title, status, text.replace("|", "/") or "-"))
        out.append("")
    pathlib.Path(doc_path).write_text("\n".join(out) + "\n", encoding="utf-8")
    for k, v in totals.items():
        print("%-13s total %2d  done %2d  supported %2d" % (k, *v))


if __name__ == "__main__":
    root = pathlib.Path(__file__).resolve().parent.parent
    main(sys.argv[1] if len(sys.argv) > 1 else root / "choco-model/src/main/resources/seed",
         sys.argv[2] if len(sys.argv) > 2 else root / "docs/GUIDE_COVERAGE.md")
