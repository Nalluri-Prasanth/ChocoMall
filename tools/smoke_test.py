#!/usr/bin/env python3
"""
End-to-end smoke test against running admin (8080) and portal (8085) apps with the demo data.
Walks through: register -> browse -> basket -> build-a-box -> checkout -> pay -> fulfil in the back office.
Usage: python tools/smoke_test.py
"""
import json
import random
import sys
import urllib.error
import urllib.parse
import urllib.request

ADMIN = "http://localhost:8080"
PORTAL = "http://localhost:8085"
failures = []
USER = "smoke_%06d" % random.randint(0, 999999)
PHONE = "+1555%07d" % random.randint(0, 9999999)


def call(base, method, path, body=None, token=None, expect=200):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            status, payload = r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        status, payload = e.code, json.loads(e.read() or b"{}")
    ok = status == expect
    print(("  ok   " if ok else "  FAIL ") + "%s %s -> %s" % (method, path, status) + ("" if ok else "  " + json.dumps(payload)[:300]))
    if not ok:
        failures.append((method, path, status, expect))
    return payload.get("data") if isinstance(payload, dict) else payload


def main():
    print("Back office sign-in")
    admin = call(ADMIN, "POST", "/admin/login", {"username": "admin", "password": "Admin@12345"})["token"]

    print("Storefront: browse")
    home = call(PORTAL, "GET", "/home/content")
    assert home["categories"], "categories missing"
    hits = call(PORTAL, "GET", "/product/search?keyword=pretzel")
    pretzel = hits["list"][0]
    print("    found:", pretzel["name"], pretzel["price"], "allergens:", pretzel["allergens"])
    nut_free = call(PORTAL, "GET", "/product/search?pageSize=100&excludeAllergens=5")
    print("    tree-nut-free products:", nut_free["total"])

    print("Storefront: register")
    phone = PHONE
    code = call(PORTAL, "GET", "/sso/getAuthCode?telephone=" + urllib.parse.quote(phone))
    token = call(PORTAL, "POST", "/sso/register", {"username": USER, "password": "Sm0ke-Pass!", "telephone": phone,
                                                    "authCode": code, "marketingConsent": False}, expect=201)["token"]
    call(PORTAL, "GET", "/sso/info", token=token)
    call(PORTAL, "POST", "/sso/register", {"username": USER, "password": "Sm0ke-Pass!", "telephone": phone,
                                            "authCode": "000000"}, expect=400)

    print("Storefront: basket")
    detail = call(PORTAL, "GET", "/product/detail/%d" % pretzel["id"])
    call(PORTAL, "POST", "/cart/add", {"productId": pretzel["id"], "skuId": detail["skus"][0]["id"], "quantity": 4}, token=token)
    box = [p for p in call(PORTAL, "GET", "/product/search?keyword=build-a-box")["list"]][0]
    box_detail = call(PORTAL, "GET", "/product/detail/%d" % box["id"])
    six = [s for s in box_detail["skus"] if s["units"] == 6][0]
    bonbons = call(PORTAL, "GET", "/product/search?categoryId=%d&pageSize=3" % 1)  # category 1 is a group; fall back below
    cats = call(PORTAL, "GET", "/product/categoryTreeList")
    bonbon_cat = [c for g in cats for c in g["children"] if c["name"] == "Bonbons"][0]["id"]
    bonbon_ids = [p["id"] for p in call(PORTAL, "GET", "/product/search?categoryId=%d" % bonbon_cat)["list"]][:2]
    call(PORTAL, "POST", "/cart/add", {"productId": box["id"], "skuId": six["id"], "quantity": 1,
                                       "box": [{"productId": bonbon_ids[0], "quantity": 4}, {"productId": bonbon_ids[1], "quantity": 2}]}, token=token)
    call(PORTAL, "POST", "/cart/add", {"productId": box["id"], "skuId": six["id"], "quantity": 1,
                                       "box": [{"productId": bonbon_ids[0], "quantity": 4}]}, token=token, expect=400)  # box not full
    cart = call(PORTAL, "GET", "/cart/list", token=token)
    ids = [i["id"] for i in cart["items"]]
    print("    basket subtotal:", cart["subtotal"])

    print("Storefront: checkout")
    coupons = call(PORTAL, "GET", "/member/coupon/claimable", token=token)
    welcome = [c for c in coupons if c["name"].startswith("Welcome")][0]
    call(PORTAL, "POST", "/member/coupon/claim/%d" % welcome["couponId"], token=token)
    mine = call(PORTAL, "GET", "/member/coupon/list?useStatus=UNUSED", token=token)
    param = {"cartItemIds": ids, "fulfillmentType": "PICKUP", "couponHistoryId": mine[0]["historyId"], "giftSleeve": True,
             "giftMessage": "Enjoy!"}
    preview = call(PORTAL, "POST", "/order/confirm", param, token=token)
    print("    preview pay amount:", preview["payAmount"], "coupon:", preview["couponDiscount"], "gift:", preview["giftPackagingFee"])
    order = call(PORTAL, "POST", "/order/generate", param, token=token, expect=201)
    oid = order["order"]["id"]
    print("    order", order["order"]["orderSn"], order["order"]["status"], "pay", order["order"]["payAmount"])
    call(PORTAL, "POST", "/payment/pay", {"orderId": oid, "method": "CARD", "cardToken": "tok_decline"}, token=token, expect=402)
    paid = call(PORTAL, "POST", "/payment/pay", {"orderId": oid, "method": "CARD", "cardToken": "tok_ok"}, token=token)
    assert paid["status"] == "SUCCEEDED"
    detail = call(PORTAL, "GET", "/order/detail/%d" % oid, token=token)
    assert detail["order"]["status"] == "PREPARING", detail["order"]["status"]
    me = call(PORTAL, "GET", "/sso/info", token=token)
    print("    points earned:", me["points"], "level:", me["levelName"])

    print("Back office: fulfil")
    call(ADMIN, "POST", "/order/update/ready?ids=%d" % oid, token=admin)
    call(ADMIN, "POST", "/order/update/complete?ids=%d" % oid, token=admin)
    final = call(ADMIN, "GET", "/order/%d" % oid, token=admin)
    assert final["order"]["status"] == "COMPLETED"
    print("    history:", [h["note"] for h in final["history"]])
    call(ADMIN, "GET", "/report/dashboard", token=admin)
    daily = call(ADMIN, "GET", "/report/sales/daily?from=2020-01-01&to=2099-01-01", token=admin, expect=400)  # range too long
    contrib = call(ADMIN, "GET", "/report/contribution?from=2026-01-01&to=2026-12-31", token=admin)
    print("    contribution rows:", len(contrib), contrib[0] if contrib else None)

    print("Access control")
    call(PORTAL, "GET", "/cart/list", expect=401)
    call(PORTAL, "GET", "/order/detail/%d" % oid, token=admin, expect=401)  # an admin token is not a member token
    call(ADMIN, "GET", "/report/dashboard", token=token, expect=401)        # a member token is not an admin token

    print()
    if failures:
        print("FAILED:", failures)
        sys.exit(1)
    print("All checks passed")


if __name__ == "__main__":
    main()
