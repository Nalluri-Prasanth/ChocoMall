#!/usr/bin/env python3
"""
Converts the "Chocolate Shop Complete Guide" workbook into the CSV seed files that the
choco-model module loads on start-up (see CatalogSeeder).

Only the Python standard library is needed (an .xlsx file is a zip archive of XML parts).

Usage:
    python tools/xlsx_to_seed.py <path-to-workbook.xlsx> [output-dir]

The default output directory is choco-model/src/main/resources/seed.
"""
import csv
import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {
    "m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}
M = "{%s}" % NS["m"]


def read_workbook(path):
    """Returns {sheet name: [ {column letter: text} per non-empty row ]}."""
    z = zipfile.ZipFile(path)
    shared = []
    if "xl/sharedStrings.xml" in z.namelist():
        for si in ET.fromstring(z.read("xl/sharedStrings.xml")).findall("m:si", NS):
            shared.append("".join(t.text or "" for t in si.iter(M + "t")))
    rels = {r.get("Id"): r.get("Target")
            for r in ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))}
    sheets = {}
    for s in ET.fromstring(z.read("xl/workbook.xml")).find("m:sheets", NS):
        target = rels[s.get("{%s}id" % NS["r"])].lstrip("/")
        target = target if target.startswith("xl/") else "xl/" + target
        rows = []
        for row in ET.fromstring(z.read(target)).iter(M + "row"):
            cells = {}
            for c in row.findall("m:c", NS):
                v = c.find("m:v", NS)
                kind = c.get("t")
                if kind == "inlineStr":
                    text = "".join(t.text or "" for t in c.iter(M + "t"))
                elif v is None:
                    continue
                elif kind == "s":
                    text = shared[int(v.text)]
                else:
                    text = v.text
                cells[re.sub(r"\d+$", "", c.get("r"))] = " ".join(text.split())
            if cells:
                rows.append(cells)
        sheets[s.get("name")] = rows
    return sheets


def table(rows, id_prefix):
    """Rows of a catalog table: the ones whose column A looks like an id (e.g. T01, B001)."""
    pat = re.compile(r"^%s\d+$" % id_prefix)
    return [r for r in rows if pat.match(r.get("A", ""))]


def write(out_dir, name, header, records):
    with open(out_dir / name, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, quoting=csv.QUOTE_ALL, lineterminator="\n")
        w.writerow(header)
        w.writerows(records)
    print("%-26s %4d rows" % (name, len(records)))


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    src = Path(sys.argv[1])
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else (
        Path(__file__).resolve().parent.parent / "choco-model/src/main/resources/seed")
    out.mkdir(parents=True, exist_ok=True)
    wb = read_workbook(src)
    g = lambda r, c: r.get(c, "")

    write(out, "chocolate_types.csv",
          ["code", "name", "classification", "distinguishing", "best_shop_use", "priority"],
          [[g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), g(r, "E"), g(r, "F")]
           for r in table(wb["Chocolate Types"], "T")])

    write(out, "flavors.csv",
          ["code", "flavor_group", "name", "chocolate_base", "application", "priority"],
          [[g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), g(r, "E"), g(r, "F")]
           for r in table(wb["Flavor Variations"], "V")])

    write(out, "products.csv",
          ["code", "category", "name", "selling_format", "sales_purpose", "priority"],
          [[g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), g(r, "E"), g(r, "F")]
           for r in table(wb["Products to Sell"], "P")])

    write(out, "food_pairings.csv",
          ["code", "food_group", "food", "chocolate_combination", "shop_format", "priority"],
          [[g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), g(r, "E"), g(r, "F")]
           for r in table(wb["Foods and Drinks"], "F")])

    write(out, "serving_formats.csv",
          ["code", "eating_format", "how_to_serve", "suitable_product", "sales_application", "priority"],
          [[g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), g(r, "E"), g(r, "F")]
           for r in table(wb["Ways to Eat"], "E")])

    write(out, "brands.csv",
          ["code", "name", "region", "associated_market", "segment", "typical_formats",
           "shop_role", "evidence_status", "reference_url"],
          [[g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), g(r, "E"), g(r, "F"),
            g(r, "G"), g(r, "H"), g(r, "I")] for r in table(wb["Global Brands"], "B")])

    # ---- operational guides, merged into one table with a guide_type discriminator ----
    guide = []
    cols = ["guide_type", "code", "title", "detail", "purpose", "applies_to", "control_note",
            "phase", "priority", "reference_url"]
    for r in table(wb["Production Processes"], "M"):
        guide.append(["PRODUCTION", g(r, "A"), g(r, "B"), g(r, "C"), "", g(r, "D"), g(r, "E"),
                      "", g(r, "F"), g(r, "G")])
    for r in table(wb["3D Chocolate Printing"], "D"):
        guide.append(["PRINTING", g(r, "A"), g(r, "B"), g(r, "C"), "", g(r, "D"), g(r, "F"),
                      "", g(r, "E"), ""])
    for r in table(wb["Sales and Launch"], "S"):
        guide.append(["SALES_LAUNCH", g(r, "A"), g(r, "B"), g(r, "C"), g(r, "D"), "", g(r, "F"),
                      g(r, "E"), "", ""])

    rec = wb["Shop Recommendation"]
    # shop formats: rows whose column B is a decision word and column F is a catalog name
    fmt_rows = []
    for r in rec:
        if r.get("A") and r.get("B") and r.get("C") and r.get("D") and r.get("A") not in (
                "Format", "Shop format", "Sequence") and not re.match(r"^\d\.", r.get("A", "")):
            fmt_rows.append(r)
    # (the summary block has six single-label rows with only A/B; the format block has A..D)
    for i, r in enumerate(fmt_rows, 1):
        guide.append(["SHOP_FORMAT", "SF%02d" % i, g(r, "A"), g(r, "B"), g(r, "C"), "", g(r, "D"),
                      "", "", ""])
    for r in rec:
        m = re.match(r"^(\d)\.\s*(.+)$", r.get("A", ""))
        if m:
            guide.append(["ROADMAP", "RM%02d" % int(m.group(1)), m.group(2), g(r, "B"), g(r, "C"),
                          "", g(r, "D"), "", "", ""])
    concept_labels = ["Best starting concept", "Why this concept", "Starting assortment",
                      "Stocking brands", "3D printing", "Scope and assumptions", "Definitions",
                      "Practical buying shortlist"]
    for i, r in enumerate([r for r in rec if r.get("A") in concept_labels], 1):
        guide.append(["SHOP_CONCEPT", "SC%02d" % i, g(r, "A"), g(r, "B"), "", "", "", "", "", ""])
    write(out, "guide_items.csv", cols, guide)

    sources = []
    capture = False
    for r in wb["Production Processes"]:
        if r.get("B") == "Source" and r.get("C") == "Scope":
            capture = True
            continue
        if capture and r.get("B") and not r.get("A"):
            checked = g(r, "D")[:10]
            sources.append([g(r, "B"), g(r, "C"), checked, g(r, "E")])
    write(out, "guide_sources.csv", ["source", "scope", "checked", "reference_url"], sources)


if __name__ == "__main__":
    main()
