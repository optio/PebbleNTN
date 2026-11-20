# Integration Test Instructions: Watch settings help text (#139)

## Scope

The test strategy is Minimal and this is zero-Unit express work, so no separate integration suite is generated. There is one component (the Watch settings screen) and no cross-unit boundary. The change touches no sync, protocol, persistence or other boundary.

## What covers the integration surface

- **Screen with real resources:** `WatchSettingsScreenTest` (Robolectric Compose) renders the real screen and value list with the real string resources, capabilities and units. That is the only integration this change has: UI rules, resources and Compose.
- **Sync unchanged:** the existing sync and codec tests in the full JVM suite (`scripts/test-all.sh` → `gradlew test`) show the settings sync is unchanged (NFR3).

## Manual check on a real phone (recommended before release)

1. Install the debug or release build on an Android phone with the PebbleNTN watchapp open.
2. Open Dashboard → Watch settings. Every row shows its value and a help line. Tapping a row shows the fuller explanation at the top of the list.
3. Open Up button. Each action has a description.
4. Turn on TalkBack. Each row is read as title, value, then help.
5. Set the system font size to the largest. Nothing is cut off.
6. Change a setting. It still reaches the watch, as before.

No integration test data is needed; the screens use the watch's mirrored settings.
