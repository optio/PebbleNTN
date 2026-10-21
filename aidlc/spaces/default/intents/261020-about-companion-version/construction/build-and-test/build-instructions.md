# Build Instructions — #103

## Prerequisites

- Pebble SDK installed (`~/.pebble-sdk`, `pebble` CLI) with the ARM toolchain (`arm-none-eabi-gcc`).
- Host `gcc` for the watch unit tests.
- JDK 21 and the Android SDK for the full suite (`scripts/test-all.sh` also builds and tests the Android app).
- No environment variables or secrets are needed.

## Build

```bash
scripts/build-watchapp.sh   # builds watchapp/build/watchapp.pbw for emery, diorite, chalk, basalt, aplite
```

## Verify

- The build ends with `'build' finished successfully` and `Watchapp built: watchapp/build/watchapp.pbw`.
- `scripts/test-all.sh` runs spec assets, protocol consistency, version sync, watch unit tests, rules, regressions, route-capture and the Android build. It must end with `All tests finished.`

## Troubleshooting

- The `LOAD segment with RWX permissions` linker warnings come from the SDK and were there before this change.
- If the local Pebble emulator stops answering (libpebble2 TimeoutError), check for an orphaned `qemu-pebble` process and stop it before restarting.
