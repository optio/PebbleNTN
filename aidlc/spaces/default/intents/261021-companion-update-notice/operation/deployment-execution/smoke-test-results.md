# Smoke Test Results — #94 (v0.0.71)

| Check | Result |
|---|---|
| Release v0.0.71 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` on the downloaded files | Pass: `pebble-ntn.apk: OK`, `pebble-ntn.pbw: OK` |
| PBW `appinfo.json` versionLabel | Pass: `0.0.71` |
| Every platform binary contains the notice text and the update idle text | Pass: basalt, chalk, diorite and emery each have 3 matches, including the `Update: %s` subtitle |
| Aplite binary | Pass: 2 matches (notice and idle text). Aplite has no app glance, as FR6 expects |
| Post-merge CI on `main` (2c98fe1) | Pass: watchapp, android, rules, codeql and release all succeeded |
| Issue #94 | Closed by PR #107 |

## Not Covered

An on-wrist check with the real phone app is still a manual follow-up: the notice, the idle text, the launcher subtitle, and the dashboard prompts on a phone.
