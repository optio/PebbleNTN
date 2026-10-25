# Smoke Test Results — v0.0.77

| Check | Result |
|---|---|
| Release v0.0.77 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` | Pass: APK OK, PBW OK |
| PBW versionLabel | Pass: `0.0.77` |
| PBW holds all five platform binaries | Pass: 5 |
| The emery binary contains the new About text ("Ideas or feedback") | Pass |
| Post-merge CI on `main` (efd4c1a) | Pass: rules, watchapp, android, codeql and release succeeded |

## Not Covered

The reconnect resend (FR2c) on a real phone and watch. See `construction/build-and-test/integration-test-instructions.md`.
