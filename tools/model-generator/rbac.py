"""Generates V2__baseline_rbac.sql: resource categories, URL resources, menus and roles."""
import sys, pathlib

NOW = "CURRENT_TIMESTAMP"

# category -> [(name, url pattern, description)]
RESOURCES = {
    "Catalog": [
        ("Products", "/product/**", "Products, SKUs, claims, publishing"),
        ("Product categories", "/productCategory/**", "Category tree"),
        ("Brands", "/brand/**", "Brand directory and stocking workflow"),
        ("Flavors", "/flavor/**", "Flavor catalog"),
        ("Chocolate types", "/chocolateType/**", "Chocolate type catalog"),
        ("Allergens", "/allergen/**", "Allergen reference list"),
        ("Stock", "/stock/**", "Stock levels, adjustments and ledger"),
        ("Preorder windows", "/preorderWindow/**", "Seasonal preorder windows and capacity"),
        ("Experience sessions", "/experience/**", "Tastings and workshops"),
    ],
    "Orders": [
        ("Orders", "/order/**", "Order handling"),
        ("Returns", "/returnApply/**", "Return and refund requests"),
        ("Return reasons", "/returnReason/**", "Return reason list"),
        ("Order settings", "/orderSetting/**", "Timeouts, fees, loyalty rules"),
        ("Shop locations", "/shopLocation/**", "Pickup and return points"),
        ("Personalisation", "/personalization/**", "Custom name and monogram requests"),
        ("Payments", "/payment/**", "Payment ledger"),
    ],
    "Operations": [
        ("Production", "/production/**", "Batches, release and waste"),
        ("Launch playbook", "/guide/**", "Production, 3D printing and launch guides"),
    ],
    "Members": [
        ("Members", "/member/**", "Member accounts"),
        ("Member levels", "/memberLevel/**", "Loyalty tiers"),
    ],
    "Marketing": [
        ("Coupons", "/coupon/**", "Coupon definitions"),
        ("Coupon history", "/couponHistory/**", "Issued coupons"),
        ("Home page slots", "/home/**", "Banners and recommendations"),
        ("Bundles", "/bundle/**", "Drink + treat and gift bundles"),
    ],
    "Content": [
        ("Subjects", "/subject/**", "Editorial topics"),
        ("Help centre", "/help/**", "Help categories and articles"),
        ("Food pairings", "/pairing/**", "Food and drink pairing guide"),
        ("Serving formats", "/serving/**", "Ways to eat and serve"),
    ],
    "Reports": [
        ("Reports", "/report/**", "Sales, contribution and stock reports"),
    ],
    "System": [
        ("Admin accounts", "/admin/**", "Back-office users"),
        ("Roles", "/role/**", "Role management"),
        ("Menus", "/menu/**", "Menu management"),
        ("Resources", "/resource/**", "API resource management"),
        ("Resource categories", "/resourceCategory/**", "Resource category management"),
        ("Files", "/file/**", "File upload"),
    ],
}

# (name, title, icon, children[(name, title)])
MENUS = [
    ("catalog", "Catalog", "product", [("product", "Products"), ("productCategory", "Categories"), ("brand", "Brands"),
                                       ("flavor", "Flavors"), ("chocolateType", "Chocolate types"), ("stock", "Stock")]),
    ("sales", "Orders", "order", [("order", "Orders"), ("returnApply", "Returns"), ("personalization", "Personalisation"),
                                   ("orderSetting", "Settings")]),
    ("operations", "Operations", "production", [("production", "Production batches"), ("guide", "Launch playbook")]),
    ("members", "Members", "member", [("member", "Members"), ("memberLevel", "Levels")]),
    ("marketing", "Marketing", "promotion", [("coupon", "Coupons"), ("homeSlot", "Home page"), ("bundle", "Bundles"),
                                              ("preorderWindow", "Preorder windows"), ("experience", "Experiences")]),
    ("content", "Content", "content", [("subject", "Subjects"), ("help", "Help centre"), ("pairing", "Pairings"),
                                        ("serving", "Serving formats")]),
    ("reports", "Reports", "chart", [("report", "Reports")]),
    ("system", "System", "settings", [("admin", "Admins"), ("role", "Roles"), ("menu", "Menus"), ("resource", "Resources")]),
]

# role -> (description, menu groups or None=all, url patterns or None=all)
ROLES = {
    "SUPER_ADMIN": ("Full access to every module", None, None),
    "CATALOG_MANAGER": ("Maintains products, brands, stock and preorder windows",
                        ["catalog", "marketing"],
                        ["/product/**", "/productCategory/**", "/brand/**", "/flavor/**", "/chocolateType/**", "/allergen/**",
                         "/stock/**", "/preorderWindow/**", "/experience/**", "/bundle/**", "/file/**", "/report/**"]),
    "ORDER_MANAGER": ("Handles orders, returns, personalisation requests and payments",
                      ["sales", "members"],
                      ["/order/**", "/returnApply/**", "/returnReason/**", "/orderSetting/**", "/shopLocation/**",
                       "/personalization/**", "/payment/**", "/member/**", "/memberLevel/**", "/report/**"]),
    "MARKETING_MANAGER": ("Runs coupons, home page slots and bundles",
                          ["marketing", "reports"],
                          ["/coupon/**", "/couponHistory/**", "/home/**", "/bundle/**", "/preorderWindow/**",
                           "/experience/**", "/report/**", "/file/**"]),
    "CONTENT_EDITOR": ("Edits subjects, help articles, pairings and serving guides",
                       ["content"],
                       ["/subject/**", "/help/**", "/pairing/**", "/serving/**", "/file/**"]),
    "PRODUCTION_LEAD": ("Plans batches, releases stock and follows the launch playbook",
                        ["operations", "catalog"],
                        ["/production/**", "/guide/**", "/stock/**", "/product/**", "/allergen/**", "/report/**"]),
}


def q(s):
    return "'" + s.replace("'", "''") + "'"


def main(out):
    sql = ["-- Baseline RBAC data: resource categories, API resources, menus and roles (generated).\n"]
    for i, cat in enumerate(RESOURCES, 1):
        sql.append("INSERT INTO ums_resource_category (name, sort, create_time) VALUES (%s, %d, %s);" % (q(cat), 100 - i, NOW))
    sql.append("")
    for cat, items in RESOURCES.items():
        for name, url, desc in items:
            sql.append("INSERT INTO ums_resource (category_id, name, url, description, create_time) "
                       "SELECT id, %s, %s, %s, %s FROM ums_resource_category WHERE name = %s;" % (q(name), q(url), q(desc), NOW, q(cat)))
    sql.append("")
    for i, (name, title, icon, kids) in enumerate(MENUS, 1):
        sql.append("INSERT INTO ums_menu (parent_id, title, level, sort, name, icon, hidden, create_time) "
                   "VALUES (0, %s, 0, %d, %s, %s, FALSE, %s);" % (q(title), 100 - i, q(name), q(icon), NOW))
    for name, title, icon, kids in MENUS:
        for j, (kname, ktitle) in enumerate(kids, 1):
            sql.append("INSERT INTO ums_menu (parent_id, title, level, sort, name, icon, hidden, create_time) "
                       "SELECT id, %s, 1, %d, %s, %s, FALSE, %s FROM ums_menu WHERE name = %s AND parent_id = 0;"
                       % (q(ktitle), 100 - j, q(kname), q(kname), NOW, q(name)))
    sql.append("")
    for i, (role, (desc, groups, urls)) in enumerate(ROLES.items(), 1):
        sql.append("INSERT INTO ums_role (name, description, admin_count, create_time, status, sort) VALUES (%s, %s, 0, %s, 1, %d);"
                   % (q(role), q(desc), NOW, 100 - i))
    sql.append("")
    for role, (desc, groups, urls) in ROLES.items():
        if urls is None:
            sql.append("INSERT INTO ums_role_resource_relation (role_id, resource_id) "
                       "SELECT r.id, s.id FROM ums_role r CROSS JOIN ums_resource s WHERE r.name = %s;" % q(role))
        else:
            sql.append("INSERT INTO ums_role_resource_relation (role_id, resource_id) "
                       "SELECT r.id, s.id FROM ums_role r CROSS JOIN ums_resource s WHERE r.name = %s AND s.url IN (%s);"
                       % (q(role), ", ".join(q(u) for u in urls)))
        if groups is None:
            sql.append("INSERT INTO ums_role_menu_relation (role_id, menu_id) "
                       "SELECT r.id, m.id FROM ums_role r CROSS JOIN ums_menu m WHERE r.name = %s;" % q(role))
        else:
            sql.append("INSERT INTO ums_role_menu_relation (role_id, menu_id) "
                       "SELECT r.id, m.id FROM ums_role r CROSS JOIN ums_menu m WHERE r.name = %s "
                       "AND (m.name IN (%s) OR m.parent_id IN (SELECT id FROM ums_menu WHERE parent_id = 0 AND name IN (%s)));"
                       % (q(role), ", ".join(q(g) for g in groups), ", ".join(q(g) for g in groups)))
    pathlib.Path(out).write_text("\n".join(sql) + "\n", encoding="utf-8")


main(sys.argv[1] if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent.parent / "choco-model/src/main/resources/db/migration/V2__baseline_rbac.sql")
