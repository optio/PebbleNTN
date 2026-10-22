# Build Instructions — #94

## Prerequisites

- Pebble SDK installed (`~/.pebble-sdk`, `pebble` CLI) with the ARM toolchain (`arm-none-eabi-gcc`).
- Host `gcc` for the watch unit tests.
- JDK 21 and the Android SDK for the Android build and JVM tests (`scripts/test-all.sh` runs both).
- No environment variables or secrets are needed.

## Build

```bash
scripts/generate-protocol.sh   # only after editing protocol/protocol-definition.json; regenerates Protocol.kt and protocol.h
scripts/build-watchapp.sh      # builds watchapp/build/watchapp.pbw for emery, diorite, chalk, basalt, aplite
android/gradlew -p android :app:assembleDebug
```

## Verify

- The watch build ends with `'build' finished successfully` and `Watchapp built: watchapp/build/watchapp.pbw`.
- `scripts/test-all.sh` checks:
  - spec assets (including the `MANIFEST.sha256.json` hashes of `examples/protocol-definition.json`);
  - the protocol constants against the definition;
  - version sync;
  - the watch unit tests;
  - the rules and regressions;
  - route-capture;
  - the Android build, tests and lint.
  
  It must end with `All tests finished.`

## Troubleshooting

- Edit `protocol/protocol-definition.json` and `examples/protocol-definition.json` together, then update the hash of the examples file in `MANIFEST.sha256.json`. Otherwise the spec asset check fails.
- The `LOAD segment with RWX permissions` linker warnings come from the SDK and were there before this change.
- If the local Pebble emulator stops answering, look for an orphaned `qemu-pebble` or `pypkjs` process and stop it.
