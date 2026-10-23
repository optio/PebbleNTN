# Build Instructions — Version comparison as the default update check

## Prerequisites

You need JDK 21 and the Android SDK. No environment variables or secrets are required. This change is Android-only.

## Build

```bash
android/gradlew -p android :app:assembleDebug :app:lintDebug
```

## Verify

- Assemble and lint both report `BUILD SUCCESSFUL`.
- `scripts/test-all.sh` ends with `All tests finished.`

## Troubleshooting

If a listed spec file (`requirements/REQ-ANDROID.md`, `spec/200-architecture/Pebble.md`) changes without a hash update in `MANIFEST.sha256.json`, spec validation and the pre-commit hook fail.
