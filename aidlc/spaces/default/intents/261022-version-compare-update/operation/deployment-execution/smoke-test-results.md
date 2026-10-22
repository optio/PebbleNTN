# Smoke Test Results — Version-compare update detection (v0.0.73)

| Check | Result |
|---|---|
| Release v0.0.73 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` on the downloaded files | Pass: `pebble-ntn.apk: OK`, `pebble-ntn.pbw: OK` |
| PBW `appinfo.json` versionLabel | Pass: `0.0.73` |
| The watch binary carries its own version `0.0.73`, used for the comparison | Pass: found in basalt |
| The update texts are present in every platform binary | Pass: basalt, chalk, diorite and emery each have 3 matches; aplite has 2 (no app glance) |
| Post-merge CI on `main` (0ab636d) | Pass: android, watchapp, codeql and release succeeded |

## Not Covered

These are manual follow-ups:
- on a phone: the update card's layout, the "Other sources" list, and the GitHub, F-Droid and Obtainium destinations;
- on a watch: the update notice when the watchapp auto-updates ahead of the phone app.
