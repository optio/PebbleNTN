# Performance Test Instructions — Prepare the first Google Play release (#114)

There are no load or latency targets.

- **aplite memory:** `pebble build` reports 3615 B free at load, up from 3597 B, because the About link text is shorter. The QR stays 29×29, so its bitmap size is unchanged.
- **Network on Play installs:** no request is made at all. `UpdateCheckRepository` returns `NOT_ALLOWED` without fetching (JVM-tested).
- **APK size:** the store images live in `fastlane/` and `tools/`, outside the APK, so the app doesn't grow.
