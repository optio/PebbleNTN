# Build Instructions — Watchapp version in the dashboard status

## Prerequisites

- JDK 21 and the Android SDK.
- No environment variables or secrets.

This is an Android-only change, so the watchapp doesn't need to be rebuilt. `scripts/test-all.sh` still builds and tests both apps.

## Build

```bash
android/gradlew -p android :app:assembleDebug :app:lintDebug
```

## Verify

- `BUILD SUCCESSFUL` for assemble and lint.
- `scripts/test-all.sh` ends with `All tests finished.`

## Troubleshooting

- A REQ file listed in `MANIFEST.sha256.json` that changes without a hash update fails `scripts/validate-spec-assets.sh` and the pre-commit hook.
