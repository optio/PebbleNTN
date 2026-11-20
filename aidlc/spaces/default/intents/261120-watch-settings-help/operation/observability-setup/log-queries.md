# Log Queries: Watch settings help text (#139)

The help texts write nothing to logs and add nothing to the diagnostics export. These are the useful queries.

## Find a help text and what it claims

```bash
# All help, details and shortcut description strings
grep -n '_help"\|_details"\|_details_imperial"\|watch_values_shortcut_descriptions' android/app/src/main/res/values/strings.xml
# The wording check (setting → watch behaviour → source file)
sed -n '/## Wording check/,/^## /p' aidlc/spaces/default/intents/261120-watch-settings-help/construction/code-generation/code-summary.md
```

## Check a reported claim against the watch

```bash
# e.g. what a shortcut action really does, or the backlight durations
grep -rn 'BACKLIGHT\|backlight_ms\|SHORTCUT_' watchapp/src/c/ | head
```

## Crash triage (debug builds, adb)

```bash
adb logcat -s AndroidRuntime:E | grep -A20 'watchsettings'
```

## Reproduce the screen

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.watchsettings.*'
PEBBLENTN_STORE_SCREENSHOTS_DIR=$PWD/tools/play-store-sources/phone android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.StoreScreenshotsTest'
```
