# Build Instructions: OsmAnd street and arrival time, Directions text (#134, #136)

## Prerequisites

- JDK 21 and the Android SDK (`android/` Gradle wrapper). No new dependency.
- Pebble SDK 4.33.1 with `pebble-tool` for the watchapp, and host `gcc` for the watch unit tests.
- Python 3 for `protocol/generate.py`, the validation scripts and the rule workbench.
- For the OsmAnd checks: the Windows Android emulator (`PebbleNTN_Capture_API_37`), driven from WSL by `tools/route-capture` (see its README). Never start an Android emulator inside WSL.

## Build

```bash
scripts/generate-protocol.sh --check          # Protocol.kt / protocol.h match protocol-definition.json (1.5)
scripts/build-watchapp.sh                     # all five platforms; prints aplite free heap
./android/gradlew -p android assembleDebug    # debug APK
```

Everything at once (what CI runs):

```bash
scripts/test-all.sh
```

## Verify

- `scripts/validate-spec-assets.sh`: the MANIFEST hashes, including `schemas/ruleset.schema.json` and `examples/protocol-definition.json`.
- `python3 -I tools/rule-workbench/workbench.py regression`: every app's fixtures through the Python engine.
- aplite free heap at load, printed by `scripts/build-watchapp.sh`. It must stay within REQ-WATCH-025's budget, no more than 200 B below 3,482 B.
- #136: `./android/gradlew -p android assembleRelease` gives the same APK as before the `debugSymbolLevel` line (compare the sha256).

## Troubleshooting

- **Protocol mismatch:** run `scripts/generate-protocol.sh`. Never edit `Protocol.kt` or `protocol.h` by hand.
- **Spec-asset hash failure:** a protected file (the rule schema, a protocol definition) changed. Update `MANIFEST.sha256.json` with the hash the validator reports.
- **"Unable to extract native debug metadata … already been stripped" during bundleRelease:** expected. The app's native libraries come already stripped from their AARs (see the summary for #136).
