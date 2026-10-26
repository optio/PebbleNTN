# Build Instructions — Prepare the first Google Play release (#114)

## Prerequisites

- JDK 21 and the Android SDK (`source ~/.local/pebblentn-env.sh`), Pebble Tool v5.0.40 with SDK 4.33.1.
- Pillow, for the store graphics. The pebble tool's Python has it.

## Build and Check

```bash
android/gradlew -p android :app:verifyReleaseTargetSdk   # targetSdk 36 meets the Play floor
android/gradlew -p android :app:assembleDebug :app:assembleRelease :app:bundleRelease
cd watchapp && pebble build                              # aplite heap stays positive (3615 B)
<pebble-tool-python> tools/play_store_graphics.py --check
scripts/test-all.sh                                      # everything CI runs
```

## Store Images

The raw captures are in `tools/play-store-sources/{phone,watch}/`.

- **Phone renders:** `PEBBLENTN_STORE_SCREENSHOTS_DIR=<dir> android/gradlew -p android :app:testDebugUnitTest --tests '*StoreScreenshotsTest'`.
- **Watch captures:** `python3 watchapp/tools/capture_screenshots.py --store`. It never wipes the emulator.
- **Build the store images:** `<pebble-tool-python> tools/play_store_graphics.py` writes `fastlane/metadata/android/en-US/images/`, which Google Play and F-Droid share.

## Play Bundle

Run the `play-release` workflow by hand for a tag. It needs the four `PLAY_UPLOAD_*` secrets in the `release` environment. Download the `release-aab` artifact and upload it in Play Console (`docs/PLAY_CONSOLE.md`).

## Delivery of This Work

Commits and the merge carry `[skip ci]`. Nothing is released, and the version stays 0.0.77.
