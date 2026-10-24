# Smoke Test Results — v0.0.76

| Check | Result |
|---|---|
| Release v0.0.76 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` | Pass: APK OK, PBW OK |
| PBW versionLabel | Pass: `0.0.76` |
| PBW holds all five platform binaries | Pass: 5 |
| The emery binary contains the "Shortcuts" menu text | Pass |
| Post-merge CI on `main` (44f1feb) | Pass: watchapp, codeql and release succeeded |

## Not Covered

The checks on a real Pebble Time 2 (swipe directions, no Tap on a swipe, touch off in the watch settings). See `construction/build-and-test/integration-test-instructions.md`.
