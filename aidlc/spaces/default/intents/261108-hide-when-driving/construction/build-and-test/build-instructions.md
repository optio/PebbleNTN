# Build Instructions: "When driving" (#132)

## Prerequisites

- JDK 21 and the Android SDK (see `android/` Gradle wrapper); no new dependency was added.
- Pebble SDK 4.33.1 with `pebble-tool` for the watchapp; host `gcc` for the watch unit tests.
- Python 3 for `protocol/generate.py` and the validation scripts.

## Build

```bash
scripts/generate-protocol.sh --check          # generated Protocol.kt / protocol.h match protocol-definition.json (1.4)
scripts/build-watchapp.sh                     # all five platforms; prints aplite free heap
./android/gradlew -p android assembleDebug    # debug APK: android/app/build/outputs/apk/debug/app-debug.apk
```

Everything at once (what CI runs locally):

```bash
scripts/test-all.sh
```

## Verify

- `scripts/validate-spec-assets.sh` (MANIFEST hashes, including `examples/protocol-definition.json`).
- `scripts/version.sh --check` (version stays 0.0.81 on the branch; the release workflow bumps it on merge).
- aplite free heap at load is printed by `scripts/build-watchapp.sh`: it must stay within REQ-WATCH-025's budget (3,482 B free at this change).

## Troubleshooting

- A protocol mismatch error means `protocol-definition.json` and `examples/protocol-definition.json` differ, or the generated files were edited by hand: run `scripts/generate-protocol.sh`.
- A spec-asset hash failure means a protected file changed without updating `MANIFEST.sha256.json`.
- Do not start Android emulators inside WSL; use the Windows emulator or a real phone.
