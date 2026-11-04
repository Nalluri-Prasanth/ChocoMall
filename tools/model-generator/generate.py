import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from spec_lib import java_entity, java_repo, sql_table
from schema import ENTITIES, ENUMS

def order(entities):
    by_table = {e.table: e for e in entities}
    done, out = set(), []
    def visit(e, stack=()):
        if e.table in done: return
        deps = [f.fk for f in e.fields if f.fk] + [c.value_fk for c in e.colls if c.value_fk]
        for d in deps:
            if d != e.table and d in by_table: visit(by_table[d], stack + (e.table,))
        done.add(e.table); out.append(e)
    for e in entities: visit(e)
    return out
ENTITIES = order(ENTITIES)

root = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent.parent / "choco-model"
java = root / "src/main/java/com/chocomall/model"
for d in ("entity", "repository", "enums"):
    (java / d).mkdir(parents=True, exist_ok=True)
(root / "src/main/resources/db/migration").mkdir(parents=True, exist_ok=True)

for name, values in ENUMS.items():
    vals = values.split()
    (java / "enums" / (name + ".java")).write_text(
        "package com.chocomall.model.enums;\n\npublic enum %s {\n    %s\n}\n" % (name, ",\n    ".join(vals)),
        encoding="utf-8")

sql = ["-- ChocoMall baseline schema (generated from the domain model; edit with care).",
       "-- Portable between MySQL 8 and H2 (MySQL mode).\n"]
for e in ENTITIES:
    (java / "entity" / (e.cls + ".java")).write_text(java_entity(e), encoding="utf-8")
    (java / "repository" / (e.cls + "Repository.java")).write_text(java_repo(e), encoding="utf-8")
    sql.append(sql_table(e))
(root / "src/main/resources/db/migration/V1__baseline_schema.sql").write_text("\n".join(sql), encoding="utf-8")
print("entities:", len(ENTITIES), "enums:", len(ENUMS))
