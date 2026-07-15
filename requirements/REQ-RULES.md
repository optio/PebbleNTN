# Rule Requirements

## REQ-RULE-001 — Declarative only
Rules SHALL be declarative JSON interpreted by fixed built-in operators and extractors.

## REQ-RULE-002 — No executable remote code
Rules SHALL NOT execute scripts, load classes, invoke reflection, run shell commands, or download code.

## REQ-RULE-003 — Source package
Every rule SHALL specify one or more exact package names.

## REQ-RULE-004 — Layer order
User overrides/additions SHALL take precedence over downloaded official rules, which SHALL take precedence over bundled official rules.

## REQ-RULE-005 — Immutable official rules
Official rules SHALL be read-only in the UI. Editing SHALL create a user clone.

## REQ-RULE-006 — Simple editor identity
The simple editor SHALL edit the same JSON-backed rule model used by the expert editor.

## REQ-RULE-007 — Expert validation
Invalid JSON or semantically invalid rules SHALL not be saved as enabled rules.

## REQ-RULE-008 — Trace
Each evaluation SHALL be capable of producing a condition/extractor trace suitable for debugging.

## REQ-RULE-009 — Preview
A rule SHALL be testable against retained captures without restarting navigation.

## REQ-RULE-010 — Locale-ready
The engine and schema SHALL support locale-specific rules from the first implementation. Initial target locales are English, Dutch, French and German.

## REQ-RULE-011 — Limits
Rule count, input size, pattern size and evaluation time SHALL be bounded.

## REQ-RULE-012 — Fixtures
Every official rule change SHALL include sanitized fixtures and expected normalized outputs.

## REQ-RULE-013 — Google Maps first
Google Maps is the first official rules target. Other catalog apps may remain capture-only until evidence-backed rules are merged.

## REQ-RULE-014 — Distance units
The distance extractor SHALL require an explicit unit and SHALL recognize both abbreviated (`m`, `km`, `ft`, `mi`, `yd`) and spelled-out (`metre(s)`/`meter(s)`, `kilometre(s)`/`kilometer(s)`, `foot`/`feet`, `mile(s)`, `yard(s)`) forms, with `.` or `,` as the decimal separator. The unit SHALL immediately follow the number so a road name containing a unit word does not match, and a bare number SHALL NOT be treated as a distance. The Kotlin and Python (`rule-workbench`) implementations SHALL stay in lockstep.

## REQ-RULE-015 — Bundled ruleset layout
Bundled official rulesets SHALL be organized per navigation app then per language as `rules/bundled/<app>/<language>.json`. The app SHALL discover every bundled ruleset recursively and MAY present them grouped by app then language. File layout SHALL NOT affect matching: rules are selected by their declared `packageNames` and `locales`, never by filename.
