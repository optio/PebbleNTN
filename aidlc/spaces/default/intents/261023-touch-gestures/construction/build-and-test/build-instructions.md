# Build Instructions — Shortcuts (#12)

## Prerequisites

- Pebble Tool v5.0.40 with SDK 4.33.1.
- gcc for the host tests.
- JDK 21 and the Android SDK, for the full `scripts/test-all.sh`.

## Build

```bash
cd watchapp && pebble build
```

This builds aplite, basalt, chalk, diorite and emery. Check the "Free RAM available (heap)" line for aplite: it must stay positive. It was 3916 B with this work.

## Tests

```bash
scripts/test-watchapp-unit.sh   # watchapp host tests, including test_shortcuts
scripts/test-all.sh             # everything CI runs
```

## Release

Merging to `main` runs `.github/workflows/release.yml`, which publishes the APK and the PBW. The PBW then goes to the Pebble store by hand, because the watch code changed.
