# Smoke Test Results — v0.0.78

| Check | Result |
|---|---|
| Release v0.0.78 exists with the APK, PBW and `SHA256SUMS.txt` | Pass |
| `sha256sum -c SHA256SUMS.txt` | Pass: APK OK, PBW OK |
| APK: versionCode 78, versionName 0.0.78, targetSdk 36 | Pass (aapt2 badging) |
| PBW: versionLabel 0.0.78, five platform binaries | Pass |
| Post-merge CI on `main` (67ec730): rules, android, codeql, release | Pass |
| `play-release` for v0.0.78: AAB present, checksum matches, signed by the Play upload key, versionName 0.0.78 | Pass |

## Not Covered

A real MicroG Maps install (the maintainer follows up on #113) and the first Play upload.
