# Smoke Test Results — v0.0.75

| Check | Result |
|---|---|
| Release v0.0.75 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` | Pass: APK OK, PBW OK |
| The APK resources contain the new texts ("spots new versions offline", "… is available.") | Pass: 2 matches |
| PBW versionLabel | Pass: `0.0.75` |
| Post-merge CI on `main` (a4daf7d) | Pass: android, codeql and release succeeded |

## Not Covered

The check on a phone: the result of "Check for updates now" and the new onboarding and weekly-hint texts.
