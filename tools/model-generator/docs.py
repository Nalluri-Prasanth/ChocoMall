"""Writes docs/DATA_MODEL.md (tables, columns, relations and an ER overview) from schema.py."""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from schema import ENTITIES, ENUMS  # noqa: E402

GROUPS = [
    ("Back-office accounts and permissions (ums_)", ("ums_admin", "ums_role", "ums_menu", "ums_resource", "ums_admin_login_log")),
    ("Members and loyalty (ums_)", ("ums_member",)),
    ("Catalog and stock (pms_)", ("pms_",)),
    ("Operations (ops_)", ("ops_",)),
    ("Content (cms_)", ("cms_",)),
    ("Marketing (sms_)", ("sms_",)),
    ("Orders, payments, personalisation and bookings (oms_)", ("oms_",)),
]


def group_of(table):
    for title, prefixes in GROUPS:
        if any(table == p or table.startswith(p) for p in prefixes):
            return title
    return GROUPS[1][0]


def type_label(f):
    return {"S": "varchar(%d)" % f.n, "L": "bigint", "I": "int", "B": "boolean", "M": "decimal(%d,%d)" % (f.p, f.s),
            "T": "datetime", "D": "date", "V": "bigint (version)", "E": "enum %s" % f.enum}[f.kind]


def main(out):
    lines = ["# Data model", "",
             "Generated from `tools/model-generator/schema.py` by `python tools/model-generator/docs.py`. "
             "The Flyway migration `V1__baseline_schema.sql` creates exactly these tables.", "",
             "%d tables, %d enumerations. Tables are prefixed by area: `ums_` users and members, "
             "`pms_` products, `oms_` orders, `sms_` marketing, `cms_` content, `ops_` operations." % (
                 len(ENTITIES) + sum(len(e.colls) for e in ENTITIES), len(ENUMS)), "",
             "Conventions: money is `decimal(10,2)`; times are UTC `datetime(6)`; ids are `bigint` identity keys; enumerations are stored "
             "as their names in `varchar` columns; many-to-many links are two-column tables (`*_relation`, `*_flavor`...).", ""]

    lines += ["## Key relationships", "", "```mermaid", "erDiagram",
              "    ums_member ||--o{ oms_order : places", "    ums_member ||--o{ oms_cart_item : has",
              "    ums_member ||--o{ ums_member_receive_address : owns", "    ums_member ||--o{ sms_coupon_history : holds",
              "    ums_member ||--o{ oms_personalization_request : requests", "    ums_member ||--o{ oms_experience_booking : books",
              "    oms_order ||--|{ oms_order_item : contains", "    oms_order ||--o{ oms_order_operate_history : logs",
              "    oms_order ||--o{ oms_payment : paid_by", "    oms_order ||--o{ oms_order_return_apply : returned_by",
              "    pms_product ||--|{ pms_product_sku : sold_as", "    pms_product }o--|| pms_product_category : in",
              "    pms_product }o--o| pms_brand : by", "    pms_product }o--o{ pms_flavor : tastes_like",
              "    pms_product }o--o{ pms_allergen : contains", "    pms_product ||--o{ pms_product_claim : claims",
              "    pms_product ||--o{ pms_stock_movement : ledger", "    pms_product ||--o{ ops_production_batch : made_in",
              "    pms_product ||--o{ pms_experience_session : scheduled", "    pms_experience_session ||--o{ oms_experience_booking : booked",
              "    pms_preorder_window ||--o{ oms_order : reserves", "    sms_coupon ||--o{ sms_coupon_history : issued_as",
              "    oms_personalization_request ||--o{ oms_personalization_proof : proofs",
              "    ums_admin }o--o{ ums_role : has", "    ums_role }o--o{ ums_resource : grants", "    ums_role }o--o{ ums_menu : shows",
              "```", ""]

    by_group = {}
    for e in ENTITIES:
        by_group.setdefault(group_of(e.table), []).append(e)
    for title, _ in GROUPS:
        if title not in by_group:
            continue
        lines += ["## " + title, ""]
        for e in by_group[title]:
            lines += ["### `%s` (%s)" % (e.table, e.cls), "", "| Column | Type | Notes |", "|---|---|---|", "| id | bigint | primary key, identity |"]
            for f in e.fields:
                notes = []
                if not f.null:
                    notes.append("required")
                if f.unique:
                    notes.append("unique")
                if f.fk:
                    notes.append("-> `%s`" % f.fk)
                if f.default is not None:
                    notes.append("default `%s`" % f.default)
                if not f.updatable:
                    notes.append("changed only by atomic UPDATE")
                if f.auto:
                    notes.append("set automatically")
                lines.append("| %s | %s | %s |" % (f.col, type_label(f), ", ".join(notes)))
            for u in e.uniques:
                lines.append("| *(unique)* | | (%s) |" % ", ".join(u))
            lines.append("")
            for c in e.colls:
                lines += ["Link table `%s`: `%s` -> `%s`, `%s` -> `%s`." % (c.table, c.owner_col, e.table, c.value_col, c.value_fk), ""]

    lines += ["## Enumerations", ""]
    for name, values in ENUMS.items():
        lines.append("* **%s**: %s" % (name, ", ".join("`%s`" % v for v in values.split())))
    pathlib.Path(out).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("wrote", out)


if __name__ == "__main__":
    root = pathlib.Path(__file__).resolve().parent.parent.parent
    main(sys.argv[1] if len(sys.argv) > 1 else root / "docs/DATA_MODEL.md")
