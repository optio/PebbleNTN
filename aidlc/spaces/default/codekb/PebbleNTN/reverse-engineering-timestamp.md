# Reverse Engineering Timestamp

## Run Record

- Date: 2026-11-20
- Commit: 073f51d446fee2d02ff2da9bab80e2d65ee85df3
- Breadth: full rescan of the whole repository (snapshot path `./`); depth Minimal; developer is the only link (scan and synthesis)
- Intent: `watch-settings-help` (GitHub issue #139: clear help text for each setting on the phone's Watch settings screen)
- Replaces: the prior store from commit `2afb066` (full scan for `osmand-bigtext`), wholesale
- Emphasis: the Watch settings area (`ui/watchsettings/`, `core/WatchSettingsModel.kt`, `core/WatchSettingsSync.kt`, strings, tests, REQ-ANDROID-021/-024/-025, AndroidUI.md, WatchUI.md)
- Not run: no builds, tests or programs (scan by reading only)
- Scan record: `aidlc/spaces/default/intents/261120-watch-settings-help/inception/reverse-engineering/developer-scan.md`

## Scope of Analysis

```yaml
scope_version: 1
kind: full
intent: watch-settings-help
fingerprint: 8e64f3916a1d6d016ff86db82772d306b6e550fa
analyzed:
  paths:
    - ./
  components:
    - Notification Intake
    - Large Icon Recognizer
    - Rule Engine
    - Navigation App Catalog
    - Session Core
    - Driving Detection
    - Watch Settings Sync
    - Watch Settings Screen
    - Watch Link
    - Persistence
    - Diagnostics Export
    - Updates
    - Companion UI
    - App Container
    - PebbleKit 2 Vendored
    - Fixture Publisher
    - Bundled Rule Packs
    - Rule Fixtures and Regression
    - Watchapp
    - Protocol Generator
    - Rule Workbench
    - Route Capture Harness
    - Store Graphics Generator
    - Build and Release Scripts
    - Specification and Contracts
    - Store and Distribution Assets
shallow:
  paths:
    - android/app/src/main/java/com/pebblentn/app/notification/
    - android/app/src/main/java/com/pebblentn/app/rules/
    - android/app/src/main/java/com/pebblentn/app/data/
    - android/app/src/main/java/com/pebblentn/app/export/
    - android/app/src/main/java/com/pebblentn/app/update/
    - android/pebblekit/
    - android/fixture-publisher/
    - watchapp/
    - rules/
    - tools/
    - spec/
    - docs/
    - fastlane/
    - fdroid/
    - screenshots/
```
