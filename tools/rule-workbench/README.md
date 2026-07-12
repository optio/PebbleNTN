# rule-workbench

Terminal tool for rule maintainers (REQ-DEBUG-008). Pure Python + `jsonschema`.

```bash
python3 tools/rule-workbench/workbench.py <command> [...]
```

| Command | Purpose |
|---|---|
| `inspect <ruleset.json>` | Summarize a ruleset (ids, priorities, locales, maneuvers). |
| `validate <ruleset.json>` | Validate against `schemas/ruleset.schema.json`. |
| `diff <a.json> <b.json>` | Added (`+`) / removed (`-`) / changed (`~`) rules by id. |
| `scaffold <id> --package <pkg>` | Print a new-rule JSON template. |
| `sanitize <export.json> [-o out]` | Redact a full-diagnostic export's notification text into fixture-safe content. |
| `promote <rule.json> <ruleset.json>` | Insert/replace a rule in a ruleset, canonically ordered. |
| `test <ruleset.json> <fixtures.json>` | Run fixtures through the engine subset; report pass/fail. |
| `regression` | Run the bundled Google Maps ruleset against its fixtures (used by CI). |

## Authoritative engine

`test`/`regression` use a **documented subset** re-implementation of the rule engine (the operators
and extractors the bundled rules use) for fast local iteration. The authoritative engine is the
Kotlin `RuleEngine` exercised by `GoogleMapsRulesRegressionTest`; the two are kept in sync (the
Python `regression` and the Kotlin test run the same fixtures and must agree).
