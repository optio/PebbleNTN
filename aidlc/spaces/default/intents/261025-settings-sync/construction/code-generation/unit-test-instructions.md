# Unit Test Instructions — Sync watchapp settings with the companion app (#102)

## Framework and Setup

- **Watch:** plain C host tests. Each `watchapp/tests/test_<unit>.c` is compiled with gcc (`-std=c11 -Wall -Wextra -Werror`) together with only `watchapp/src/c/<unit>.c`, so it needs no SDK and no emulator. The new units (`revision.c`, and the pure part of `settings_sync.c`) must not include `pebble.h` in the code under test. Split the Pebble glue into its own file if needed.
- **Phone:** JUnit 4 JVM tests through Gradle, which need JDK 21 and the Android SDK. UI tests follow whatever the project already uses. If there's no Compose test setup, test the screens' state functions as pure Kotlin.

## How to Run This Work's Tests

From the repo root:

```bash
scripts/test-watchapp-unit.sh
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.core.*' --tests 'com.pebblentn.app.protocol.*' --tests 'com.pebblentn.app.pebble.*' --tests 'com.pebblentn.app.data.*' --tests 'com.pebblentn.app.ui.*'
```

Plan Step 2 runs both before any change. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test | Cases | Requirement |
|---|---|---|
| test_revision (C) | next wraps 99 → 0; newer for diff 1 and 49; not newer for 0, 50 and 51–99; rollover (0 is newer than 99) | FR3 |
| test_settings_sync (C) | decide: phone newer gives apply, watch newer gives keep, equal with differing values gives keep, equal and same gives same; valid: out of range is rejected, a colour accent is rejected on black-and-white, backlight colour and RGB action are rejected without RGB, touch slots are rejected without touch | FR3 |
| test_shortcuts (C, updated) | the vibration toggle with silent Off: a pattern gives Off, Off gives the last pattern, Off with Short pulse last gives Short pulse | FR8 |
| ProtocolCodecTest (extended) | 1.2 READY without settings; 1.3 READY with all keys; a synthetic READY gives no mirror update; SETTINGS encode/decode round trip | FR1 |
| WatchSettingsSyncTest | the same comparison cases as C | FR3, FR4 |
| WatchSettingsModelTest | defaults match the watch; validation by capability | FR3, FR5 |
| WatchSettingsQueueTest | an edit while closed is queued and sent once on the next real READY; never sent while closed; watch-newer settings replace the queued values they carry | FR4 |
| NavigationController test | no SETTINGS is sent to a closed watchapp; a real READY flushes the queue | FR4 |
| WatchSettingsMirrorRepositoryTest | the mirror, revision and pending edits survive a new instance | FR4 |
| Watch settings UI state tests | no backlight colour without RGB; no touch rows without touch; black-and-white accents only; waiting and update-watchapp states | FR5 |
| PreviewCatalogTest | every visual value on every platform maps to an existing drawable, and every `watch_preview_*` drawable maps back; aplite and diorite use basalt with greyscale; the content description names the value | FR6, FR7 |

The emulator checks (Step 14) cover the AppMessage glue, the redraw, the menu refresh and the vibration.

## Coverage Target

Minimal strategy: one test per unit-testable requirement (FR1, FR3–FR8), and a happy path per new component, plus at least two edge cases for each comparison and validator. Express has no line-coverage floor.

## Mocking and Test Data

- **Watch:** plain integers and capability bitmasks.
- **Phone:** fake `WatchTransport` and fake repositories, following the existing `NavigationControllerTest` and data tests. Dictionaries are built as `Map<Int, Any>`.
