# rules

- `catalog/`   — bundled navigation-app catalog entries (app id, packages, capture/official flags).
- `bundled/`   — official rulesets shipped in the app (Google Maps first, from M6).
- `fixtures/`  — sanitized notification fixtures with expected extraction results.
- `test-cases/`— rule regression cases exercised by `scripts/validate-rules.sh`.

Schema: `schemas/ruleset.schema.json`. Example: `examples/example-ruleset.json`.
Rule engine and schemas land in **M4**; Google Maps rules in **M6**.
