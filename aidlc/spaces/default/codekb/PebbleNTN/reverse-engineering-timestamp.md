# Reverse Engineering Timestamp

## Run Record

- Date: 2026-11-10
- Commit: 2afb066a6453db76d188fd00b51f4fbdd9d2d930
- Breadth requested: full rescan of the whole repository (snapshot path `./`); depth Minimal; developer link only (scan and synthesis by the developer agent)
- Intent: `osmand-bigtext` (GitHub issue #134: OsmAnd street from `bigText` line 1 and arrival clock from `bigText` line 2 for en, fr, es, it, nl, pt, pl, zh, de; `etaEpochSeconds` for OsmAnd and Google Maps)
- Replaces: the prior store from commit `61787f5` (focused scan for `hide-when-driving`), wholesale
- Emphasis: the rule engine (`rules/` Kotlin package), bundled rule packs, fixtures, both regression engines, the ETA path from rule output to the watch strip
- Not run: no builds, tests or programs (scan by reading only)

## Scope of Analysis

```yaml
scope_version: 1
kind: full
intent: osmand-bigtext
fingerprint: 9cf02d7489a1b37fc0a0b9e5f8cf9a4c32f76680
analyzed:
  paths:
    - ./
  components:
    - Notification Intake
    - Large Icon Recognizer
    - Rule Engine
    - Bundled Rule Packs
    - Rule Fixtures and Regression
    - Navigation App Catalog
    - Session Core
    - Driving Detection
    - Watch Settings Sync
    - Watch Link
    - Persistence
    - Diagnostics Export
    - Updates
    - Companion UI
    - App Container
    - PebbleKit 2 Vendored
    - Fixture Publisher
    - Watchapp
    - Protocol Generator
    - Rule Workbench
    - Route Capture Harness
    - Build and Release Scripts
    - Specification and Contracts
    - Store and Distribution Assets
shallow:
  paths:
    - android/app/src/main/java/com/pebblentn/app/ui/
    - android/app/src/main/java/com/pebblentn/app/update/
    - android/app/src/main/java/com/pebblentn/app/export/
    - android/app/src/main/java/com/pebblentn/app/data/
    - android/pebblekit/
    - android/fixture-publisher/
    - watchapp/resources/
    - tools/route-capture/
    - spec/
    - docs/
    - fastlane/
    - fdroid/
    - screenshots/
```
