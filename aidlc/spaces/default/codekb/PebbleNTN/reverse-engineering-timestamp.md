# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-21
- Commit: f8366a4b34a77c504df0194a9f31386553a74b8c
- Breadth requested: full rescan of the whole repo (snapshot paths `./`); depth Minimal
- Coverage recorded honestly as partial: the watchapp, protocol, phone-to-watch layer, watch/protocol specs and CI were read deeply; the rest of the Android app, tools, rules and docs were classified by listing and doc headers (see `shallow.paths`)

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: 261020-about-companion-version
fingerprint: 30f464f0185d26a004af27217139a88b157ef29f
analyzed:
  paths:
    - watchapp/src/c/main.c
    - watchapp/src/c/about_text.c
    - watchapp/src/c/about_text.h
    - watchapp/src/c/about_window.c
    - watchapp/src/c/about_window.h
    - watchapp/src/c/settings_window.h
    - watchapp/src/c/generated/
    - watchapp/tests/test_about_text.c
    - watchapp/package.json
    - watchapp/wscript
    - protocol/
    - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt
    - spec/000-overview.md
    - spec/200-architecture/Protocol.md
    - spec/200-architecture/Pebble.md
    - spec/200-architecture/System.md
    - spec/400-ui/WatchUI.md
    - requirements/REQ-WATCH.md
    - scripts/test-all.sh
    - scripts/test-watchapp-unit.sh
    - scripts/generate-protocol.sh
    - scripts/build-watchapp.sh
    - .github/workflows/watchapp.yml
    - .github/workflows/android.yml
    - .github/workflows/rules.yml
    - android/gradle/libs.versions.toml
    - android/app/build.gradle.kts
    - AGENTS.md
    - MANIFEST.sha256.json
  components:
    - Watchapp Main Renderer
    - Watchapp About Screen
    - Protocol Definition and Generator
    - Phone Protocol Codec
    - Phone Watch Transport
    - Navigation Controller
shallow:
  paths:
    - watchapp/src/c/
    - watchapp/tests/
    - watchapp/tools/
    - watchapp/resources/
    - watchapp/store/
    - android/app/src/
    - android/app/schemas/
    - android/pebblekit/
    - android/fixture-publisher/
    - spec/
    - requirements/
    - scripts/
    - .github/workflows/
    - .githooks/
    - rules/
    - schemas/
    - examples/
    - tools/rule-workbench/
    - tools/route-capture/
    - docs/
    - fastlane/
    - fdroid/
    - screenshots/
    - README.md
```
