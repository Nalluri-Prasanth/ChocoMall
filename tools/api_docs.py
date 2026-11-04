#!/usr/bin/env python3
"""
Writes docs/API.md from the OpenAPI documents of the running services, so the reference always matches the code.

Start the services first (see README), then:
    python tools/api_docs.py
"""
import json
import pathlib
import urllib.request

SERVICES = [
    ("Back office (choco-admin, port 8080)", "http://localhost:8080/v3/api-docs", "Staff JWT from `POST /admin/login`; every path needs a role that grants its resource pattern."),
    ("Storefront (choco-portal, port 8085)", "http://localhost:8085/v3/api-docs", "Customer JWT from `POST /sso/login`; browsing endpoints are public."),
    ("Search (choco-search, port 8081)", "http://localhost:8081/v3/api-docs", "Public reads; `/search/index/**` needs the `X-Search-Admin-Key` header."),
]


def main():
    out = ["# API reference", "",
           "Generated from the live OpenAPI documents by `python tools/api_docs.py`. Interactive documentation is served by every service at "
           "`/swagger-ui.html`; the raw specification is at `/v3/api-docs`.", "",
           "All responses use one envelope: `{\"code\": 200, \"message\": \"...\", \"data\": ...}`. The `code` mirrors the HTTP status "
           "(400 validation, 401 not signed in, 403 not allowed, 404 not found, 409 conflict with current state, 429 too many attempts). "
           "Lists are paged with `pageNum` (from 1) and `pageSize`.", ""]
    for title, url, auth in SERVICES:
        try:
            spec = json.load(urllib.request.urlopen(url, timeout=10))
        except Exception as e:  # noqa: BLE001
            out += ["## " + title, "", "_Not generated: %s is not reachable (%s)._" % (url, e), ""]
            continue
        out += ["## " + title, "", "Authentication: " + auth, ""]
        by_tag = {}
        for path, ops in sorted(spec["paths"].items()):
            for method, op in ops.items():
                tag = (op.get("tags") or ["Other"])[0]
                by_tag.setdefault(tag, []).append((path, method.upper(), op.get("summary", "")))
        for tag in sorted(by_tag):
            out += ["### " + tag, "", "| Method | Path | Description |", "|---|---|---|"]
            for path, method, summary in by_tag[tag]:
                out.append("| %s | `%s` | %s |" % (method, path, summary.replace("|", "/")))
            out.append("")
    target = pathlib.Path(__file__).resolve().parent.parent / "docs" / "API.md"
    target.write_text("\n".join(out) + "\n", encoding="utf-8")
    print("wrote", target)


if __name__ == "__main__":
    main()
