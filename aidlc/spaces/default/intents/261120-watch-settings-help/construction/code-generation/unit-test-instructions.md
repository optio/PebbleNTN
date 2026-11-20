# Unit Test Instructions: Watch settings help text (#139)

## Framework and setup

- JUnit 4 with Robolectric (SDK 34) and the Compose UI test rule (`createComposeRule` / `createAndroidComposeRule`), already configured in `android/app/build.gradle.kts`. No new test dependency or configuration is needed.
- Requires JDK 21 and the Android SDK, as for the existing build (see `docs/IMPLEMENTATION_STATUS.md`). No emulator or device is needed.

## How to run this work's tests

Each command is scoped to the two test classes this change touches. Run them from the repository root.

Baseline, which must pass before Step 2, and the final run:

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.watchsettings.WatchSettingsUiTest' --tests 'com.pebblentn.app.ui.watchsettings.WatchSettingsScreenTest'
```

Business logic only (Step 3):

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.watchsettings.WatchSettingsUiTest'
```

Frontend only (Step 5):

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.watchsettings.WatchSettingsScreenTest'
```

Store screenshot rendering (Step 7). This is not a check; it is skipped unless the variable is set:

```bash
PEBBLENTN_STORE_SCREENSHOTS_DIR=$PWD/tools/play-store-sources/phone android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.StoreScreenshotsTest'
```

## Tests to write (Minimal strategy: one per requirement, about 11 in total)

| Test | Class | Requirement |
|---|---|---|
| every setting has a help and a details resource | WatchSettingsUiTest | FR1.4, FR2.3 |
| shortcut descriptions only for the 7 slots, same length as the actions | WatchSettingsUiTest | FR3.1, FR3.3 |
| early-warning details have an imperial variant | WatchSettingsUiTest | FR5.3 |
| Directions section has no note (updated) | WatchSettingsUiTest | FR4.1 |
| rows show value and help line | WatchSettingsScreenTest | FR1.1–FR1.3 |
| no touch or RGB help on a watch without them | WatchSettingsScreenTest | FR5.1, FR5.2 |
| Directions note gone, When driving paragraph kept, driving rows have help (updated) | WatchSettingsScreenTest | FR4.1–FR4.4 |
| value list shows details (Backlight mentions battery; Colour with preview) | WatchSettingsScreenTest | FR2.1, FR2.2 |
| shortcut list shows descriptions; none on non-shortcut lists; hidden action hidden | WatchSettingsScreenTest | FR3.1–FR3.3 |
| help readable at font scale 2.0 on 360 dp | WatchSettingsScreenTest | NFR2 |
| row semantics merge title, value and help; click label unchanged | WatchSettingsScreenTest | NFR1 |

## Coverage target

The express scope has no line-coverage floor. Every functional requirement has at least one test, and the existing tests in both classes stay green (NFR3). The full suite (`scripts/test-all.sh`) runs in Build and Test.

## Mocking and test data

- No mocks. Use the existing fixtures in the test classes: `WatchSettingsModel` / `WatchSettingsSyncState` built with chosen capabilities (colour, RGB backlight, touch), units and protocol minor.
- Font scale: Robolectric `@Config(qualifiers = "w360dp-...")`, and for the font scale wrap the content in a `CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f))`.
- Assert on string resources resolved through the test context, not on copied literals, so the copy can change without rewriting tests. The one exception is the When driving paragraph, which the existing test already asserts on.
