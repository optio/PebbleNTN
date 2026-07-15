# rules

- `catalog/`   — bundled navigation-app catalog entries (app id, packages, capture/official flags).
- `bundled/`   — official rulesets shipped in the app, organized **per app then per language**:
  `bundled/<app>/<language>.json` (e.g. `bundled/google-maps/en.json`). The app loads every
  `.json` under `bundled/` recursively and flattens the rules into one bundled layer, and the Rules
  screen groups them app → language so the list stays short as more apps/languages are added.
- `fixtures/`  — sanitized notification fixtures with expected extraction results.
- `test-cases/`— rule regression cases exercised by `scripts/validate-rules.sh`.

Schema: `schemas/ruleset.schema.json`. Example: `examples/example-ruleset.json`.
Rule engine and schemas land in **M4**; Google Maps rules in **M6**.
