# Security Test Instructions — MicroG Maps as a Google Maps variant (#113)

- **Allowlist:** a notification is processed only when its package is in the catalog and its app is enabled. On Play installs, MicroG Maps is enabled exactly when Google Maps is (tested). Packages outside the catalog still match no rule.
- **Catalog validation:**
  - `rulesFrom` must name an existing app with its own rules, with no self reference and no chain;
  - `website` must be https;
  - each package must be unique.
  - These are tested in the parser and in `scripts/validate_catalog.py`.
- **Store policy:** a Play install never shows or links to the patched app. The link opens only in the browser, on user tap.
- **No new permissions:** the only manifest change is one more `<queries>` package.
