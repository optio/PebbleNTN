# Build Instructions — Version-compare update detection

## Prerequisites

- Pebble SDK installed (`~/.pebble-sdk`, `pebble` CLI) with the ARM toolchain.
- Host `gcc` for the watch unit tests.
- JDK 21 and the Android SDK for the Android build and JVM tests.
- No environment variables or secrets are needed.

## Build

```bash
scripts/build-watchapp.sh                    # watchapp/build/watchapp.pbw for emery, diorite, chalk, basalt, aplite
android/gradlew -p android :app:assembleDebug
```

There is no protocol change, so `scripts/generate-protocol.sh` is not needed.

## Verify

- The watch build ends with `'build' finished successfully`.
- `scripts/test-all.sh` ends with `All tests finished.` It covers spec assets, protocol consistency, version sync, watch units, rules, regressions, route-capture, and the Android tests and lint.

## Troubleshooting

- `scripts/validate-spec-assets.sh` fails if a file listed in `MANIFEST.sha256.json` (for example a REQ file) changed without its hash being updated.
- For emulator checks, run `pebble wipe` first. Otherwise a version stored by an earlier check in persist key 18 can hide the once-per-version notice.
- If the local Pebble emulator stops answering, stop any orphaned `qemu-pebble` or `pypkjs` process.
