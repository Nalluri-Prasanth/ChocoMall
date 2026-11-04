# Model generator

The database schema, the JPA entities, the Spring Data repositories and the enums of `choco-model` are generated from one
description, `schema.py`, so they cannot drift apart. The generated files are committed; you only need Python to *change*
the model.

| File | Purpose |
|---|---|
| `schema.py` | The domain: enums, tables, columns, relations, indexes and repository methods |
| `spec_lib.py` | The tiny DSL used by `schema.py` and the renderers for Java and SQL |
| `generate.py` | Writes `entity/`, `repository/`, `enums/` and `V1__baseline_schema.sql` |
| `rbac.py` | Writes `V2__baseline_rbac.sql` (resource categories, URL resources, menus and roles) |

```bash
python tools/model-generator/generate.py      # regenerate entities, repositories, enums and V1
python tools/model-generator/rbac.py          # regenerate V2 (baseline permissions)
```

## Important

* `V1` and `V2` are Flyway migrations. They are regenerated freely **before the first release**. Once a database exists,
  never edit an applied migration: add a new `V3__...sql` by hand instead, and mirror the change in `schema.py` so the entities
  stay in step.
* Do not edit the generated Java files by hand; the next run overwrites them. Custom queries belong in the `repo=[...]`
  lists of `schema.py`.
* Counters that must stay consistent under concurrency (stock, reserved capacity, loyalty points, coupon counts) are marked
  `updatable=False`: they are only changed by atomic UPDATE statements, never by saving an entity.
