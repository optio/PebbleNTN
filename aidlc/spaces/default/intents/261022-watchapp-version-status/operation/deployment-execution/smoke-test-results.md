# Smoke Test Results — v0.0.74

| Check | Result |
|---|---|
| Release v0.0.74 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` | Pass: APK OK, PBW OK |
| The APK resources contain the new strings ("Watchapp version", "Not received yet") | Pass: 2 matches in `resources.arsc` |
| PBW versionLabel | Pass: `0.0.74` |
| Post-merge CI on `main` (bb2d910) | Pass: android, codeql and release succeeded |

## Not Covered

The check on a phone, which is the purpose of this work:
1. Open the dashboard.
2. Open PebbleNTN on the watch from the launcher.
3. See whether the "Watchapp version" row changes to "0.0.74 (last seen)" (the Core Devices app forwards watch messages) or stays "Not received yet" (it doesn't).

Also check the tighter spacing of the Status rows.
