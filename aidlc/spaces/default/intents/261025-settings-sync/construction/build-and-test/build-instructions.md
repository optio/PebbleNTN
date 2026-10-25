# Build Instructions — Settings sync (#102)

## Prerequisites

- Pebble Tool v5.0.40 with SDK 4.33.1.
- gcc for the host tests.
- JDK 21 and the Android SDK, for the full `scripts/test-all.sh`.
- Only to regenerate the previews: the Pebble emulators (basalt, chalk, emery). Never wipe emery.

## Build

```bash
scripts/generate-protocol.sh --check   # the generated constants match protocol-definition.json
cd watchapp && pebble build            # aplite, basalt, chalk, diorite, emery
android/gradlew -p android :app:assembleDebug :app:assembleRelease :app:lintDebug
```

Check the "Free RAM available (heap)" line for aplite: it must stay positive (3903 B with this work).

## Tests

```bash
scripts/test-watchapp-unit.sh   # watch host tests, including test_revision and test_settings_sync
scripts/test-all.sh             # everything CI runs, including the preview completeness test
```

## Previews

When the watch's navigation screen changes, regenerate the images and rerun the tests:

```bash
python3 watchapp/tools/capture_screenshots.py --previews
```

This writes `android/app/src/main/res/drawable-nodpi/watch_preview_<setting>_<value>_<platform>.png`. `PreviewCatalogTest` fails if a value has no image or an image has no value.

## Release

Merging to `main` runs `.github/workflows/release.yml`, which publishes the APK and the PBW. Both changed, so the PBW goes to the Pebble store by hand.
