# Build Instructions: Watch settings help text (#139)

## Prerequisites

- JDK 21 and the Android SDK (compileSdk/targetSdk 36), as for every PebbleNTN build (`docs/IMPLEMENTATION_STATUS.md`).
- Python 3. Pillow is needed for the store graphics. The system Python usually lacks it, so `scripts/test-all.sh` falls back to the pebble tool's Python.
- No new dependency, environment variable or local service is needed. The change is Kotlin, string resources, specs and PNGs only.

## Environment setup

- Branch: `feat/watch-settings-help`.
- No signing material is needed for local builds. Without `RELEASE_KEYSTORE`, `assembleRelease` produces an unsigned APK, which is enough to measure size.

## Build commands

```bash
# Debug build (used by the unit tests)
android/gradlew -p android :app:assembleDebug

# Release build, minified and shrunk, for the APK size check (NFR4)
android/gradlew -p android :app:assembleRelease
ls -l android/app/build/outputs/apk/release/
```

## Build verification

- `scripts/test-all.sh` runs everything offline:
  - spec assets;
  - protocol constants and version sync;
  - watchapp C tests;
  - rule validation and regression;
  - the tool tests;
  - workflow YAML;
  - store graphics;
  - Android `test` and `lint`.
- `scripts/validate-spec-assets.sh` must pass, because `REQ-ANDROID.md` and `AndroidUI.md` are hash-protected through `MANIFEST.sha256.json`.

## Troubleshooting

- **`validate-spec-assets.sh` fails:** a protected file changed without its manifest entry. Re-run the repository's manifest update and commit both together.
- **Store graphics check skipped:** no Python with Pillow was found. Use `~/.local/share/uv/tools/pebble-tool/bin/python tools/play_store_graphics.py --check`.
- **Robolectric font-scale test fails only on CI:** check that the test sets its own `Density(fontScale = 2f)` and does not depend on the host's settings.
