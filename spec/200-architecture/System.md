# System Architecture

## Monorepo layout

```text
PebbleNTN/
├── android/
│   ├── app/
│   ├── fixture-publisher/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
├── watchapp/
├── protocol/
├── rules/
│   ├── catalog/
│   ├── bundled/
│   ├── fixtures/
│   └── test-cases/
├── schemas/
├── tools/rule-workbench/
├── scripts/
├── docs/
├── spec/
├── requirements/
├── AGENTS.md
├── CLAUDE.md
└── LICENSE
```

## Android modules

- `app`: production companion.
- `fixture-publisher`: debug-only app that posts controlled navigation-like notifications.
- Optional later modules:
  - `core-model`
  - `rule-engine`
  - `pebble-transport`
  - `rule-workbench-core`

Begin as a small multi-module Gradle project only when module boundaries reduce compile coupling; do not prematurely create dozens of modules.

## Runtime components

- `NavigationNotificationListenerService`
- `EnabledAppRepository`
- `NotificationSnapshotFactory`
- `RuleRepository`
- `RuleEngine`
- `NavigationSessionReducer`
- `NavigationStateRepository`
- `DebugHistoryRepository`
- `WatchTransport`
- `PebbleWatchTransport`
- `RuleUpdateRepository`
- Compose UI and view models

## State reducer

All notification and transport events enter a deterministic reducer. The reducer owns:
- session start/end detection;
- last normalized instruction;
- deduplication;
- distance quantization;
- launch-once behavior;
- READY synchronization;
- stale-state detection;
- navigation stop behavior.

Do not distribute session flags across services and activities.
