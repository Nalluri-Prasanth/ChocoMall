"""Tiny schema DSL: one definition -> Flyway SQL + JPA entity + Spring Data repository."""
import re

class Field:
    def __init__(self, name, kind, **kw):
        self.name, self.kind = name, kind
        self.n = kw.get("n", 255)
        self.p, self.s = kw.get("p", 10), kw.get("s", 2)
        self.null = kw.get("null", True)
        self.unique = kw.get("unique", False)
        self.default = kw.get("default", None)
        self.fk = kw.get("fk", None)
        self.idx = kw.get("idx", False)
        self.auto = kw.get("auto", None)      # 'create' | 'update'
        self.enum = kw.get("enum", None)
        self.updatable = kw.get("updatable", True)

    @property
    def col(self):
        return re.sub(r"([A-Z])", lambda m: "_" + m.group(1).lower(), self.name)

    @property
    def java_type(self):
        return {"S": "String", "L": "Long", "I": "Integer", "B": "boolean" if not self.null else "Boolean",
                "M": "BigDecimal", "T": "LocalDateTime", "D": "LocalDate", "V": "Long",
                "E": self.enum}[self.kind]

    @property
    def sql_type(self):
        return {"S": "VARCHAR(%d)" % self.n, "L": "BIGINT", "I": "INT", "B": "BOOLEAN",
                "M": "DECIMAL(%d,%d)" % (self.p, self.s), "T": "DATETIME(6)", "D": "DATE",
                "V": "BIGINT", "E": "VARCHAR(%d)" % self.n}[self.kind]

    def java_default(self):
        d = self.default
        if d is None:
            return ""
        if self.kind == "S":
            return ' = "%s"' % d
        if self.kind == "B":
            return " = %s" % ("true" if d else "false")
        if self.kind == "M":
            return " = new BigDecimal(\"%s\")" % d
        if self.kind == "E":
            return " = %s.%s" % (self.enum, d)
        if self.kind == "V":
            return ""
        return " = %sL" % d if self.kind == "L" else " = %s" % d

    def sql_default(self):
        d = self.default
        if d is None:
            return ""
        if self.kind in ("S", "E"):
            return " DEFAULT '%s'" % d
        if self.kind == "B":
            return " DEFAULT %s" % ("TRUE" if d else "FALSE")
        return " DEFAULT %s" % d


def S(name, n=255, **kw): return Field(name, "S", n=n, **kw)
def L(name, **kw): return Field(name, "L", **kw)
def I(name, **kw): return Field(name, "I", **kw)
def Bo(name, default=False, null=False, **kw): return Field(name, "B", default=default, null=null, **kw)
def M(name, p=10, s=2, **kw): return Field(name, "M", p=p, s=s, **kw)
def T(name, **kw): return Field(name, "T", **kw)
def Dt(name, **kw): return Field(name, "D", **kw)
def Ver(): return Field("version", "V", null=False, default=0)
def En(name, enum, n=30, **kw): return Field(name, "E", enum=enum, n=n, **kw)


class Coll:
    """Element collection of Long ids stored in a link table."""
    def __init__(self, name, table, owner_col, value_col, value_fk, owner_table=None):
        self.name, self.table, self.owner_col, self.value_col = name, table, owner_col, value_col
        self.value_fk = value_fk


class Entity:
    def __init__(self, cls, table, fields, colls=(), uniques=(), repo=(), spec=False,
                 annotations=(), imports=(), extra=""):
        self.cls, self.table, self.fields = cls, table, list(fields)
        self.colls, self.uniques, self.repo = list(colls), list(uniques), list(repo)
        self.spec, self.annotations, self.imports, self.extra = spec, list(annotations), list(imports), extra


ENUM_IMPORT = "com.chocomall.model.enums."


def java_entity(e):
    imp = {"jakarta.persistence.*", "lombok.Getter", "lombok.Setter", "lombok.NoArgsConstructor"}
    body = []
    kinds = {f.kind for f in e.fields}
    if "M" in kinds: imp.add("java.math.BigDecimal")
    if "T" in kinds: imp.add("java.time.LocalDateTime")
    if "D" in kinds: imp.add("java.time.LocalDate")
    for f in e.fields:
        if f.kind == "E": imp.add(ENUM_IMPORT + f.enum)
        if f.kind == "E":
            imp.add("org.hibernate.annotations.JdbcTypeCode")
            imp.add("org.hibernate.type.SqlTypes")
        if f.auto == "create": imp.add("org.hibernate.annotations.CreationTimestamp")
        if f.auto == "update": imp.add("org.hibernate.annotations.UpdateTimestamp")
    if e.colls:
        imp.update({"java.util.LinkedHashSet", "java.util.Set", "org.hibernate.annotations.Fetch",
                    "org.hibernate.annotations.FetchMode"})
    imp.update(e.imports)

    for f in e.fields:
        ann = []
        if f.auto == "create": ann.append("@CreationTimestamp")
        if f.auto == "update": ann.append("@UpdateTimestamp")
        if f.kind == "V": ann.append("@Version")
        col = ["name = \"%s\"" % f.col]
        if not f.null: col.append("nullable = false")
        if f.unique: col.append("unique = true")
        if f.kind in ("S", "E"): col.append("length = %d" % f.n)
        if f.kind == "M": col.append("precision = %d, scale = %d" % (f.p, f.s))
        if f.auto == "create" or not f.updatable: col.append("updatable = false")
        if f.kind == "E": ann.append("@Enumerated(EnumType.STRING)")
        if f.kind == "E": ann.append("@JdbcTypeCode(SqlTypes.VARCHAR)")
        ann.append("@Column(%s)" % ", ".join(col))
        body.append("    " + "\n    ".join(ann) + "\n    private %s %s%s;\n" % (f.java_type, f.name, f.java_default()))
    for c in e.colls:
        body.append(
            "    @ElementCollection(fetch = FetchType.EAGER)\n"
            "    @Fetch(FetchMode.SUBSELECT)\n"
            "    @CollectionTable(name = \"%s\", joinColumns = @JoinColumn(name = \"%s\"))\n"
            "    @Column(name = \"%s\")\n"
            "    private Set<Long> %s = new LinkedHashSet<>();\n" % (c.table, c.owner_col, c.value_col, c.name))
    cls_ann = "\n".join(e.annotations)
    if cls_ann: cls_ann += "\n"
    return ("package com.chocomall.model.entity;\n\n" +
            "\n".join("import %s;" % i for i in sorted(imp)) +
            "\n\n@Getter\n@Setter\n@NoArgsConstructor\n@Entity\n" + cls_ann +
            "@Table(name = \"%s\")\n" % e.table +
            "public class %s {\n\n" % e.cls +
            "    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    private Long id;\n\n" +
            "\n".join(body) + e.extra + "}\n")


def java_repo(e):
    imp = {"org.springframework.data.jpa.repository.JpaRepository", "com.chocomall.model.entity." + e.cls}
    ext = "JpaRepository<%s, Long>" % e.cls
    if e.spec:
        imp.add("org.springframework.data.jpa.repository.JpaSpecificationExecutor")
        ext += ", JpaSpecificationExecutor<%s>" % e.cls
    methods = []
    for r in e.repo:
        methods.append("    " + r.strip() + "\n")
        for pkg in ("java.util.List", "java.util.Optional", "org.springframework.data.domain.Page",
                    "org.springframework.data.domain.Pageable", "java.util.Collection",
                    "java.time.LocalDateTime", "java.time.LocalDate", "java.math.BigDecimal",
                    "org.springframework.data.jpa.repository.Query", "org.springframework.data.repository.query.Param",
                    "org.springframework.data.jpa.repository.Modifying", "java.util.Set"):
            if re.search(r"\b%s\b" % pkg.split(".")[-1], r): imp.add(pkg)
        for word in re.findall(r"\b([A-Z][A-Za-z]+)\b", r):
            if word in ENUM_NAMES: imp.add("com.chocomall.model.enums." + word)
            if word in ENTITY_NAMES and word != e.cls: imp.add("com.chocomall.model.entity." + word)
    return ("package com.chocomall.model.repository;\n\n" +
            "\n".join("import %s;" % i for i in sorted(imp)) +
            "\n\npublic interface %sRepository extends %s {\n" % (e.cls, ext) +
            ("\n" + "\n".join(methods) if methods else "") + "}\n")


def sql_table(e):
    lines = ["    id BIGINT AUTO_INCREMENT PRIMARY KEY"]
    fks, idxs = [], []
    for f in e.fields:
        d = "    %s %s" % (f.col, f.sql_type)
        if not f.null: d += " NOT NULL"
        d += f.sql_default()
        if f.unique: d += " UNIQUE"
        lines.append(d)
        if f.fk: fks.append("    CONSTRAINT fk_%s_%s FOREIGN KEY (%s) REFERENCES %s (id)" % (e.table, f.col, f.col, f.fk))
        if f.idx or f.fk: idxs.append(f)
    for u in e.uniques:
        lines.append("    CONSTRAINT uq_%s_%s UNIQUE (%s)" % (e.table, "_".join(u), ", ".join(u)))
    out = "CREATE TABLE %s (\n%s\n);\n" % (e.table, ",\n".join(lines + fks))
    for f in idxs:
        out += "CREATE INDEX idx_%s_%s ON %s (%s);\n" % (e.table, f.col, e.table, f.col)
    for c in e.colls:
        out += ("CREATE TABLE %s (\n    %s BIGINT NOT NULL,\n    %s BIGINT NOT NULL,\n"
                "    PRIMARY KEY (%s, %s),\n    CONSTRAINT fk_%s_owner FOREIGN KEY (%s) REFERENCES %s (id)%s\n);\n") % (
            c.table, c.owner_col, c.value_col, c.owner_col, c.value_col, c.table, c.owner_col, e.table,
            (",\n    CONSTRAINT fk_%s_value FOREIGN KEY (%s) REFERENCES %s (id)" % (c.table, c.value_col, c.value_fk)) if c.value_fk else "")
    return out


ENUM_NAMES = set()
ENTITY_NAMES = set()
