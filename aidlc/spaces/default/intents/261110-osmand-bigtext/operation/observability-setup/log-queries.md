# Log Queries: OsmAnd street and arrival time (#134)

## Diagnostics export (schema 7)

From a user's export JSON. Prefer the privacy-safe export: it keeps the card structure without street names.

```bash
# the user's Directions text setting
jq '.metadata.watchSettings' export.json
# what each OsmAnd capture sent: rule, full and compact road line, second line, ETA
jq -r '.captures[] | select(.packageName == "net.osmand.plus") | [.matchedRuleId, .watchOutput.primaryText, .watchOutput.compactText, .watchOutput.secondaryText, .watchOutput.etaEpochSeconds] | @tsv' export.json
# captures that matched no rule
jq '.captures[] | select(.matchedRuleId == null)' export.json
```

Field names follow `spec/300-data/ExportFormat.md` (schema 7). Check them there if the export layout changes.

## Rule workbench (reproduce a reported card)

```bash
python3 -I tools/rule-workbench/workbench.py regression   # all fixtures, both forms and the ETA
```

Add the reported card, with its street names made up, as a fixture in `rules/fixtures/osmand.json`, with `nowEpochSeconds` and `timeZone`, then fix the rule.

## Logcat (debug builds, adb)

```bash
adb logcat -s NavigationController:* RuleEngine:*
```
