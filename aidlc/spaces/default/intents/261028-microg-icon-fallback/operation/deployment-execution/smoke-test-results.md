# Smoke Test Results — v0.0.79

| Check | Result |
|---|---|
| Release v0.0.79 with the APK, PBW and `SHA256SUMS.txt`; checksums | Pass |
| APK: versionCode 79, versionName 0.0.79, targetSdk 36, signed by the release key `75:EF:A3:EA:…` | Pass |
| PBW: versionLabel 0.0.79 | Pass |
| Post-merge CI on `main` (440eb50): android, codeql, release | Pass |
| `play-release` v0.0.79 AAB: checksum, upload-key signature (`D6:35:…` / SHA-1 `BC:DA:…`), version 0.0.79 | Pass |

## Not Covered

The Play upload (it waits for the upload key, valid 2026-10-30 00:14 UTC), the Packages screen on a device, and MicroG Maps arrows on a device.
