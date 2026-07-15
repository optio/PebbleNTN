# PebbleNTN Consolidated Technical Specification

Generated 2026-07-12.


---

<!-- Source: README.md -->

# PebbleNTN — Engineering Specification

**PebbleNTN** means **Pebble Notification To Navigation**.

This repository specification is an implementation contract for an AI coding agent, principally Claude Code or Codex. It defines an Android companion application that reads selected navigation-app notifications, translates them with declarative rules, and sends normalized turn instructions to a Pebble watchapp.

## Product scope

- Android-only companion application.
- Pebble C watchapp.
- Google Maps rules in the first usable release.
- Other navigation apps may be selected for debug capture before rules exist.
- Notification content is processed only after the posting package passes the enabled-app allowlist.
- Material 3 Android UI with restrained Pebble-inspired styling.
- Declarative JSON rules, simple editor, expert JSON editor, preview, imports and exports.
- Bundled official rules; architecture supports signed official remote updates managed by the project maintainers.
- Debug history and privacy-safe diagnostic export.
- Automatic one-time watchapp launch when navigation starts, configurable by the user.
- Automatic return to watchface when navigation ends, configurable by the user.
- MIT licensed, GitHub-hosted monorepo.

## Normative language

`SHALL`, `SHALL NOT`, `SHOULD`, and `MAY` are normative. Requirement IDs are binding. When prose conflicts with a numbered requirement, the numbered requirement wins.

## Target baseline

- Android language: Kotlin.
- Android UI: Jetpack Compose and Material 3.
- Android minimum SDK: API 31 (Android 12).
- Compile SDK: latest stable installed SDK; baseline 36.
- Target SDK: at least the current Google Play submission requirement; baseline 35 and verified by release automation.
- Persistence: Room.
- JSON: kotlinx.serialization.
- Dependency injection: explicit constructor injection; no Hilt or Dagger.
- Watchapp: Pebble SDK, C, bitmap maneuver resources.
- Build: Gradle Wrapper for Android and `pebble` CLI for watchapp.
- CI: GitHub Actions.
- License: MIT.

## Read order for an AI coding agent

1. `AGENTS.md`
2. `spec/000-overview.md`
3. `requirements/`
4. `spec/200-architecture/`
5. `spec/300-data/`
6. `spec/400-ui/`
7. `spec/500-testing/`
8. `spec/600-security-release/`
9. `spec/800-roadmap/Milestones.md`

No follow-up product questions are required. Where implementation details remain open, this specification supplies a default.


---

<!-- Source: AGENTS.md -->

# AI Implementation Contract

This file applies to Claude Code, Codex, and other autonomous coding agents.

## Mission

Implement PebbleNTN exactly as specified. Do not redesign the product, substitute technologies, silently reduce scope, or invent external service behavior.

## Mandatory operating rules

1. Read the specification and requirement files before changing code.
2. Work milestone-by-milestone in `spec/800-roadmap/Milestones.md`.
3. Keep Android and watch builds green after every completed milestone.
4. Add or update automated tests for every behavior change.
5. Never edit generated protocol files directly.
6. Never introduce executable remote code. Remote rules are declarative data only.
7. Never inspect notification text before the package allowlist check.
8. Never store notifications from disabled packages.
9. Never automatically upload notification content.
10. Do not commit secrets, signing keys, tokens, account credentials, local SDK paths, or unredacted private captures.
11. No placeholder implementations, fake success paths, commented-out production code, or unresolved TODOs in completed milestones.
12. If an SDK/API dependency is unavailable, isolate it behind the specified interface and provide a compiling test double. Record the blocker in `docs/IMPLEMENTATION_STATUS.md`; do not fabricate an API.
13. Prefer small modules and pure functions. Parsing must be deterministic.
14. All rule changes require fixtures and regression tests.
15. All database migrations require migration tests.
16. All user-facing strings must use Android resources.
17. Accessibility labels and scalable text are required.
18. Run the definition-of-done commands before declaring a milestone complete.

## Required validation commands

```bash
./scripts/validate-spec-assets.sh
./android/gradlew -p android test lint assembleDebug
./scripts/validate-rules.sh
./scripts/build-watchapp.sh
```

When Android instrumentation infrastructure is available:

```bash
./android/gradlew -p android connectedDebugAndroidTest
```

## Commit discipline

Each commit should represent one coherent change and mention relevant requirement IDs. Example:

```text
feat(rules): add deterministic Google Maps distance extraction

Implements REQ-RULE-021, REQ-RULE-022.
```


---

<!-- Source: spec/000-overview.md -->

# 000 — Consolidated Technical Overview

## Problem

Phone navigation apps expose upcoming turns in ongoing notifications. PebbleNTN converts those notifications into a compact, normalized navigation state and presents it on a Pebble watch.

## System boundary

```text
Navigation app
  -> Android NotificationListenerService
  -> package allowlist
  -> immutable notification snapshot
  -> declarative rule engine
  -> normalized navigation state
  -> state repository and debug history
  -> Pebble transport
  -> Pebble watchapp renderer
```

The watch does not parse Android notifications and does not calculate routes.

## Core principles

- Event-driven, no polling service.
- Privacy by early package filtering.
- Declarative and testable rule extraction.
- Last-known-state synchronization instead of replaying stale instructions.
- Watch transport isolated behind an interface.
- Rules and protocol versioned independently.
- Development workflows work from the terminal.
- Real Google Maps captures and synthetic fixtures are both first-class test inputs.

## Product defaults

- Supported app definitions are bundled.
- Installed defined navigation apps are enabled by default.
- Users may disable any app.
- Google Maps has official parsing rules initially.
- Organic Maps, OsmAnd, HERE WeGo, and Waze may be listed for capture-only mode when installed; no correct extraction is claimed until official rules exist.
- Debug retention defaults to 500 eligible notification events.
- Automatic watchapp launch on navigation-session start is enabled.
- Automatic return to watchface when navigation ends is enabled.
- Automatic remote rule checks are disabled in the initial public version.
- Raw content export is opt-in and preceded by an explicit privacy warning.


---

<!-- Source: spec/100-product/PRD.md -->

# Product Requirements Document

## Users

1. Pebble owner who wants turn indications without keeping the phone screen visible.
2. Tester who captures unknown or changed notification formats.
3. Project maintainer who converts captures and community submissions into official rules.
4. Contributor who proposes rules through a GitHub pull request.

## Primary journey

1. User installs Android app and Pebble watchapp.
2. User opens Android app and grants notification-listener access.
3. App discovers installed navigation apps represented in the supported-app catalog.
4. Defined installed apps are enabled by default.
5. User starts Google Maps navigation.
6. Listener receives a notification event and checks package metadata.
7. Rules extract maneuver, distance, road, instruction and optional ETA.
8. Phone starts the watchapp once for the session, waits for READY, then sends current state.
9. Watch shows bitmap arrow, distance and road; maneuver changes may vibrate.
10. When navigation ends, the watch returns to the watchface unless configured otherwise.

## Secondary journey: rule debugging

1. An eligible app emits an unmatched or wrongly parsed notification.
2. The debug view shows raw selected fields, timestamp, match trace and extracted output.
3. User opens the rule test bench and chooses the captured item.
4. Simple editor changes conditions/extractors and updates underlying JSON.
5. Expert editor may edit validated JSON directly.
6. Preview reruns rules without restarting navigation.
7. User exports rules only, a privacy-safe diagnostic, or an explicitly confirmed full diagnostic.
8. Maintainer imports the export into the developer rule workbench, compares results, writes fixtures and promotes a reviewed rule.

## Non-goals

- Route calculation.
- GPS tracking by PebbleNTN.
- Map display on watch.
- Reading arbitrary notifications for analytics.
- iOS support.
- Uploading debug data without explicit user action.
- Arbitrary scripting in rules.
- Guaranteed extraction for capture-only apps.


---

<!-- Source: spec/200-architecture/System.md -->

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


---

<!-- Source: spec/200-architecture/Android.md -->

# Android Technical Design

## Service lifecycle

Use `NotificationListenerService`; do not run a permanent foreground service for ordinary listening. The service performs only lightweight metadata filtering on the callback thread, creates an immutable snapshot for enabled packages, and dispatches processing to an application coroutine scope.

## Early filter

```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    if (!enabledAppRepository.isEnabled(packageName)) return
    processingDispatcher.enqueue(NotificationEvent.Posted(sbn))
}
```

No notification title, text, extras, RemoteViews, actions, or content intents may be read before the allowlist decision.

## Supported-app catalog

Bundled declarative catalog entries define:
- stable app ID;
- package names;
- display name;
- capture availability;
- official-rules availability;
- optional channel hints;
- default enabled policy.

Installed entries are shown first. All installed catalog apps default to enabled on first run. Users can disable them independently.

## Persistence

Use Room for:
- debug events;
- user rules;
- active official downloaded rules metadata;
- app enablement;
- settings;
- cached latest navigation state;
- rule-test run summaries.

Use DataStore for small preferences only if it materially simplifies settings; Room remains authoritative for rule and debug entities.

## Concurrency

- One serialized processing queue for notification events.
- Pure rule evaluation may run on `Dispatchers.Default`.
- Room I/O on `Dispatchers.IO`.
- UI observes Flows.
- Pebble callbacks are converted into typed domain events.
- No global mutable singleton state.

## Process restoration

Persist the current session state and latest instruction. On service reconnect:
- query active notifications only for enabled packages when API behavior permits;
- reconstruct current state;
- never replay an ordered backlog;
- send only current state after watch READY.

## Logging

Use Timber in debug builds. Release logging must omit raw notification content. Debug history is an explicit product feature and separate from logcat.


---

<!-- Source: spec/200-architecture/Pebble.md -->

# Pebble Watchapp Technical Design

## Responsibilities

- Open AppMessage inbox/outbox.
- Send READY after initialization.
- Render current normalized state.
- Display connection/stale/no-navigation states.
- Vibrate according to settings included in state or protocol capability.
- Exit to watchface on navigation end when commanded/configured.
- Persist only minimal display state needed for brief restart continuity.

## UI

Use bitmap arrows:
- straight;
- slight left/right;
- left/right;
- sharp left/right;
- U-turn left/right;
- roundabout;
- arrive;
- unknown.

Layout:
- upper half: maneuver bitmap;
- prominent distance;
- one or two lines for road/instruction;
- compact status indicator.

No continuous animation. Redraw only when state changes.

## Startup handshake

1. Watchapp opens AppMessage.
2. Watchapp sends `WATCH_READY` with protocol version and watchapp version.
3. Phone validates compatibility.
4. Phone sends the current navigation state or `NO_ACTIVE_NAVIGATION`.
5. Watch acknowledges via AppMessage transport behavior and may send an application-level state receipt if needed.

## Launch policy

The phone may call the Pebble launch API only once per detected navigation session by default. It must not repeatedly steal focus after the user leaves. A new session may launch again.

## Navigation end

Phone sends `NAVIGATION_STOPPED` with `exitToWatchface` determined by user setting. The watch shows a brief completion state only if required for a reliable exit transition, then exits.


---

<!-- Source: spec/200-architecture/Protocol.md -->

# AppMessage Protocol

## Principles

- Numeric keys generated from a single JSON definition.
- Compact integer enums.
- UTF-8 strings length-limited on phone.
- Protocol version in every handshake.
- Unknown optional keys ignored.
- Incompatible major versions produce a visible compatibility error.

## Message keys

| Key | Name | Type |
|---:|---|---|
| 0 | event | int32 |
| 1 | protocolMajor | int32 |
| 2 | protocolMinor | int32 |
| 3 | maneuver | int32 |
| 4 | distanceMeters | int32 |
| 5 | primaryText | string |
| 6 | secondaryText | string |
| 7 | etaEpochSeconds | int32 |
| 8 | stateTimestampSeconds | int32 |
| 9 | sessionId | int32 |
| 10 | flags | int32 |
| 11 | appVersion | string |
| 12 | errorCode | int32 |

## Events

- `1 NAVIGATION_UPDATE`
- `2 NAVIGATION_STOPPED`
- `3 NO_ACTIVE_NAVIGATION`
- `10 WATCH_READY`
- `11 WATCH_REQUEST_STATE`
- `12 PHONE_COMPATIBILITY_ERROR`

## Flags

- bit 0: exit to watchface on stop;
- bit 1: vibrate on maneuver change;
- bit 2: activate backlight;
- bit 3: state is stale;
- remaining bits reserved.

## Delivery behavior

The phone caches exactly one latest state per active session. Failed delivery is retried with bounded exponential backoff while the watchapp is known ready. A newer state replaces an older pending state.


---

<!-- Source: spec/200-architecture/RuleEngine.md -->

# Declarative Rule Engine

## Security boundary

Rules are data interpreted by fixed code. The rule language SHALL NOT support JavaScript, Kotlin, Java reflection, downloadable classes, shell commands, templates capable of execution, arbitrary URLs, or dynamic dependency loading.

## Layers

1. User additions and overrides.
2. Active official downloaded rules.
3. Bundled official rules.

User rules have highest precedence. Bundled and downloaded rules remain immutable in the UI. Users clone an official rule to modify it.

## Evaluation

1. Select rules by exact source package.
2. Filter by enabled status, locale and optional channel/category conditions.
3. Sort by priority descending, then stable rule ID.
4. Evaluate conditions with short-circuit behavior.
5. Run fixed extractors.
6. Validate normalized output.
7. Return the first complete match unless a rule declares continuation.
8. Produce a structured trace for every evaluated rule in debug mode.

## Operators

- exists / not exists;
- equals / equals-ignore-case;
- contains / contains-ignore-case;
- starts-with / ends-with;
- safe-regex;
- integer comparison;
- membership in fixed list.

## Extractors

- literal;
- regex capture;
- field copy;
- first non-empty field;
- distance parser;
- duration parser;
- maneuver mapping;
- string normalization;
- bounded join.

## Regex safety

- Use Java regex initially with hard input, pattern, group and evaluation limits.
- Reject nested pathological constructs where detectable.
- Maximum pattern length: 1,024 characters.
- Maximum tested input per field: 4,096 characters.
- Maximum rules per package: 500.
- Evaluation is cancellable and time-budgeted.
- A timed-out rule is disabled for that run and logged in the trace.

## Simple editor

The simple editor is a projection over the same JSON domain model:
- form changes update an in-memory rule document;
- valid changes serialize to canonical JSON;
- unsupported advanced structures switch the editor to expert-only mode without data loss;
- simple editor never maintains a separate rule representation.

## Expert editor

- JSON syntax highlighting;
- schema validation;
- semantic validation;
- canonical-format command;
- diff from source official rule;
- test against one, selected, or all captures;
- cannot save invalid JSON.


---

<!-- Source: spec/300-data/Database.md -->

# Database Specification

## Tables

### `supported_app_settings`
- `appId` primary key
- `packageName`
- `enabled`
- `captureUnmatched`
- `firstSeenAt`
- `updatedAt`

### `notification_debug_event`
- auto ID
- event timestamp
- received timestamp
- package name
- notification key hash
- notification ID
- tag hash, nullable
- channel ID
- event type
- selected raw snapshot JSON
- active ruleset versions
- matched rule ID
- extraction JSON
- trace JSON
- disposition
- transport status
- privacy classification

### `user_rule`
- rule ID primary key
- source rule ID nullable
- package name
- canonical JSON
- enabled
- created/updated timestamps
- validation status

### `official_ruleset`
- version primary key
- source bundled/downloaded
- schema version
- signature status
- activation status
- installed timestamp
- payload hash

### `navigation_state`
- singleton ID
- session ID
- active flag
- normalized state JSON
- state timestamp
- watch launch status
- watch readiness status

## Retention

Default debug retention is 500 eligible events. The user may select 50, 100, 500, 1,000, or unlimited with a warning. Retention cleanup occurs transactionally after insert and through periodic maintenance.

## Privacy

Only enabled-package events enter the database. Notification keys and tags are hashed when their literal values are not needed. PendingIntents, actions, RemoteViews, icons, contact identifiers and arbitrary bundles are never serialized.


---

<!-- Source: spec/300-data/RulesJSON.md -->

# Rules JSON Contract

## Ruleset envelope

A ruleset contains:
- schema version;
- ruleset version;
- minimum app version code;
- creation timestamp;
- publisher;
- source type;
- array of rules;
- optional test vectors;
- signature metadata for downloaded official rules.

## Rule identifiers

IDs are lowercase ASCII kebab-case and globally stable. Updated behavior keeps the same ID when conceptually the same rule evolves. Breaking semantic replacements use a new ID and deprecate the old one.

## Canonicalization

- UTF-8;
- Unix newlines;
- two-space indentation;
- object keys emitted in schema order;
- rule arrays sorted by package, descending priority, ID;
- no insignificant numeric formats;
- canonical payload is what is signed.

## Compatibility

Unknown fields are rejected in strict official mode and preserved-but-ignored only in an explicit future-compatible import mode. Unknown operators or extractor types always fail validation.


---

<!-- Source: spec/300-data/ExportFormat.md -->

# Diagnostic and Rule Export Format

## Export modes

### Rules only — default
Contains selected user rules and metadata. No notification content.

### Privacy-safe diagnostic
Contains rules, extraction trace, package, locale, Android/app versions, and structurally redacted notification fields. The UI explains that potential private information is limited to content that was displayed by the source navigation app in its notification, but such content may include destinations, road names, saved-place labels and route context.

### Full diagnostic
Contains selected raw notification fields. Requires:
1. explicit selection;
2. privacy warning;
3. preview;
4. confirmation;
5. Android share sheet.

No export is sent automatically.

## Redaction

Privacy-safe export replaces:
- road names;
- destination labels;
- free-form text not required for rule matching;
- exact timestamps if user selects coarse-time mode.

It preserves structural tokens, units, punctuation and anonymized placeholders so maintainers can write rules.


---

<!-- Source: spec/400-ui/AndroidUI.md -->

# Android UI Specification

## Design language

Material 3 with subtle Pebble styling:
- monochrome or low-saturation surfaces;
- bitmap-inspired maneuver previews;
- compact status chips;
- no imitation of copyrighted third-party navigation UI;
- system dynamic color optional, with a Pebble monochrome theme available.

## Screens

### Onboarding
- product explanation;
- prominent notification-access disclosure;
- open system notification-listener settings;
- detect granted state;
- Pebble/watchapp setup guidance;
- privacy statement.

### Dashboard
- notification access status;
- watch connection/readiness;
- current navigation state;
- last eligible notification timestamp;
- active ruleset version;
- shortcuts to debug history and rules.

### Navigation Apps
- catalog apps grouped as installed/uninstalled;
- installed defined apps enabled by default on first discovery;
- per-app enable toggle;
- capture-only badge when no official rules;
- warning that capture-only does not provide navigation output;
- enable all / disable all.

### Debug History
- latest events first;
- filters: app, matched, unmatched, failed, sent;
- timestamp, source, event type and result summary;
- detail screen with raw selected fields, normalized output, trace and transport result;
- re-run using current or selected ruleset;
- create rule from event;
- export selected items;
- delete item/all.

### Rules
- tabs: bundled official, downloaded official, user;
- immutable official rule details;
- clone-to-user action;
- enable/disable user rule;
- simple editor;
- expert JSON editor;
- validation errors;
- rule priority;
- test bench and result diff.

### Settings
- automatic watchapp launch;
- return to watchface when navigation ends;
- vibrate on maneuver change;
- backlight option;
- debug retention;
- unmatched capture per app;
- remote official rule checks, hidden or disabled in initial release;
- export privacy default;
- delete all local data;
- licenses and privacy policy.

## Accessibility

All icons have semantic descriptions. Information is never conveyed by color alone. Touch targets meet Material guidance. Text supports font scaling.


---

<!-- Source: spec/400-ui/WatchUI.md -->

# Watch UI Specification

## States

- Starting
- No active navigation
- Active instruction
- Arriving
- Navigation finished
- Phone disconnected
- Stale instruction
- Protocol incompatible

## Active layout

- Large bitmap maneuver centered in upper region.
- Large formatted distance.
- Primary text: road or concise instruction.
- Secondary line only when space permits.
- Tiny phone/connection indicator.
- No map.

## Distance formatting

Phone sends meters. Watch formats using user/unit preference communicated by phone:
- metric: `850 m`, `1.2 km`;
- imperial future extension.
Initial release may be metric-only if explicitly shown in settings and protocol keeps room for unit flags.

## Vibration

Vibrate only when maneuver identity changes or an explicit threshold event is sent. Do not vibrate on every distance update.

## Stale data

If state age exceeds configured threshold, retain the instruction but visibly mark it stale. Never advance a maneuver locally based only on elapsed time.


---

<!-- Source: spec/500-testing/Testing.md -->

# Testing Strategy

## Pyramid

1. Pure JVM tests for rule engine, reducers, serializers and quantization.
2. Room and Compose instrumentation tests.
3. Synthetic notification publisher tests.
4. Google Maps emulator capture sessions.
5. Pebble emulator rendering tests.
6. Physical Android + Pebble end-to-end release tests.

## Mandatory test categories

- disabled package never reads/stores content;
- app catalog defaults;
- notification snapshot field extraction;
- locale handling;
- every rule operator and extractor;
- malformed/empty/oversized inputs;
- regex limits and timeout;
- layer precedence;
- canonical JSON;
- debug retention;
- privacy redaction;
- import/export roundtrip;
- rule preview trace;
- navigation session transitions;
- launch once;
- READY handshake;
- pending state replacement;
- stop and exit flag;
- protocol compatibility;
- database migrations.

## Golden tests

Watch layouts use screenshot/golden comparison where the Pebble toolchain permits deterministic captures. Android Compose screens may use screenshot tests if stable in CI; semantic tests are mandatory regardless.


---

<!-- Source: spec/500-testing/EmulatorWorkflow.md -->

# Emulator and GPS Workflow

## Android emulator

Use a Google Play-enabled system image so Google Maps can be installed and updated. Create and start AVDs from scripts. Account sign-in remains a manual local step and is never automated with stored credentials.

## GPS simulation

Support:
- Android Emulator Extended Controls route playback;
- GPX/KML import;
- `adb emu geo fix` for individual points;
- repository GPX fixtures containing no private route data.

## Google Maps capture procedure

1. Start clean AVD snapshot.
2. Install PebbleNTN debug build.
3. Grant notification-listener access manually or through documented test setup.
4. Enable Google Maps in Navigation Apps.
5. Start Google Maps navigation with a reproducible public route.
6. Play a matching emulator route.
7. Export selected captures in privacy-safe mode.
8. Convert captures to sanitized fixtures.
9. Never commit Google account data or private destinations.

## Synthetic publisher

The separate debug app SHALL:
- create/update/remove ongoing notifications;
- control title, text, big text, subtext, text lines, progress, category and channel;
- replay JSON fixture sequences;
- support rapid-update and malformed-input tests;
- use a debug-only package admitted by debug catalog configuration.

## Limitation

Android and Pebble emulators are not assumed to provide a production-equivalent PebbleKit Bluetooth path. Use a logging/fake watch transport for Android emulator tests and inject protocol messages into the Pebble emulator. Physical hardware is the release gate.


---

<!-- Source: spec/500-testing/RuleMaintainerWorkflow.md -->

# Developer Rule Maintenance Workflow

## Goal

A maintainer must be able to convert captured databases or exported JSON into reviewed official rules with minimal manual data wrangling and a clear before/after preview.

## Tool

Create a repository tool at `tools/rule-workbench/`. Preferred implementation is a Kotlin/JVM CLI sharing the production rule-engine module. A small local web UI may be added later, but the CLI is authoritative and CI-compatible.

## Inputs

- privacy-safe diagnostic JSON;
- full diagnostic JSON supplied intentionally;
- Android Room database copied from a debug build;
- existing fixture directories;
- one or more candidate rulesets.

The tool opens Room databases read-only. It never modifies the source database.

## Commands

```bash
./tools/rule-workbench/bin/pebblentn-rules inspect capture.json
./tools/rule-workbench/bin/pebblentn-rules inspect debug.db --package com.google.android.apps.maps
./tools/rule-workbench/bin/pebblentn-rules cluster debug.db --by field-shape
./tools/rule-workbench/bin/pebblentn-rules test rules/candidate.json --input debug.db
./tools/rule-workbench/bin/pebblentn-rules diff rules/bundled/current.json rules/candidate.json --input fixtures/
./tools/rule-workbench/bin/pebblentn-rules sanitize debug.db --output fixtures/proposed/
./tools/rule-workbench/bin/pebblentn-rules scaffold --from capture.json --output proposed-rule.json
./tools/rule-workbench/bin/pebblentn-rules promote proposed-rule.json --fixtures fixtures/proposed/
```

## Inspection output

For every event:
- source package/version where available;
- locale;
- field presence and values;
- matched rule and layer;
- condition/extractor trace;
- normalized output;
- expected output when annotated;
- nearby updates in the same notification/session;
- privacy warning.

## Clustering

The workbench groups unknown notifications by:
- package;
- locale;
- set of populated fields;
- normalized text shape;
- channel/category;
- recurring token pattern.

This helps discover new notification variants without reading every capture manually.

## Scaffold workflow

1. Select representative unmatched captures.
2. Generate a rule skeleton with exact package and field-presence conditions.
3. Maintainer adds minimal textual conditions and extractors.
4. Run preview against all related captures.
5. Annotate expected outputs.
6. Sanitize and save fixtures.
7. Run regression against the entire official corpus.
8. Promote into candidate official ruleset.
9. Open GitHub pull request with generated report.

## Preview report

Generate Markdown and optional static HTML showing:
- old result;
- new result;
- changed fields;
- captures newly matched;
- captures no longer matched;
- ambiguous matches;
- performance timing;
- privacy-safe sample display.

## Pull-request gate

A rule PR must include:
- rule JSON diff;
- at least one new sanitized fixture per new variant;
- expected output;
- generated regression report;
- schema validation;
- zero unexpected changes in existing fixtures;
- maintainer review.

## Initial Google Maps workflow

Before claiming Google Maps support:
- collect multiple maneuvers;
- collect distance units and thresholds;
- collect navigation start, progress, reroute, arrival and stop;
- collect English, Dutch, French and German samples;
- distinguish notification variants by Android and Maps version where observable;
- document unsupported variants explicitly.


---

<!-- Source: spec/600-security-release/SecurityPrivacy.md -->

# Security and Privacy

## Data minimization

The listener receives system callbacks broadly, but the app checks `StatusBarNotification.packageName` before accessing content. Disabled packages are discarded immediately.

## User disclosure

Before opening notification-listener settings, show:

> PebbleNTN reads notifications from navigation apps you enable, extracts upcoming turn information, and sends it to your Pebble watch. Processing occurs on this device. Notification content is not uploaded automatically. Diagnostic files are shared only when you explicitly export them.

## Debug-data explanation

The export UI clearly states:

> Diagnostic notification content can contain only information that the navigation app placed in its Android notification. This can still include private information such as destinations, road names, saved-place labels, route context, and travel timing.

Do not imply that notification-derived information is harmless merely because it appeared in a notification.

## Remote official rules

- Disabled initially.
- HTTPS required.
- Signed canonical payload required.
- Embedded public verification keys only.
- Bounded size and complexity.
- Validate schema and semantic limits before activation.
- Run embedded self-tests.
- Atomic activation.
- Retain last-known-good version.
- No executable code or code-like expressions.

## Sharing

Use Android Sharesheet. Do not silently choose a recipient. Do not bundle a developer email address as an automatic destination; a prefilled subject is allowed. Files use temporary content URIs and expire.

## Secrets

No secrets belong in rules, Remote Config, source code, fixtures or CI logs.


---

<!-- Source: spec/600-security-release/BuildRelease.md -->

# Build, CI and Release

## Terminal tools

- JDK 21 unless the chosen stable Android Gradle Plugin requires another supported LTS.
- Android SDK command-line tools.
- Gradle Wrapper.
- adb, emulator, sdkmanager, avdmanager.
- Pebble SDK and `pebble` CLI.
- Git and GitHub CLI optional.
- Python only for support scripts where Kotlin/shell is not suitable.

## Root commands

```bash
./scripts/bootstrap.sh
./scripts/build-all.sh
./scripts/test-all.sh
./scripts/run-android-emulator.sh
./scripts/install-android-debug.sh
./scripts/build-watchapp.sh
./scripts/validate-rules.sh
```

## Artifacts

- Android debug APK.
- Android release AAB.
- Optional signed release APK for direct testing.
- Pebble `.pbw`.
- Official rules JSON and signature.
- Rule regression report.
- SBOM and dependency license report where practical.

## GitHub Actions

Workflows:
- `android.yml`: unit tests, lint, debug APK.
- `watchapp.yml`: watch build.
- `rules.yml`: schema, canonicalization, fixtures, regression.
- `release.yml`: signed AAB using GitHub environment secrets, PBW, checksums and release notes.
- `codeql.yml` or equivalent static analysis.

Pull requests cannot merge unless all required checks pass.

## Play Store

- MIT license applies to source; privacy policy remains a separate product document.
- Complete Data Safety accurately.
- Justify notification access as core functionality.
- Provide prominent disclosure.
- Build an Android App Bundle.
- Use Play App Signing and protect upload key.
- Release automation verifies target SDK policy at release time.


---

<!-- Source: spec/700-ai/CodingStandards.md -->

# Coding Standards

## Kotlin

- Kotlin official style.
- Immutable data classes.
- Sealed interfaces for domain events/results.
- Explicit error types instead of broad exceptions.
- Public APIs documented with KDoc.
- No `GlobalScope`.
- No blocking I/O on main thread.
- No Android framework types in the pure rule engine.
- Coroutines and Flow for asynchronous streams.
- Version catalog for dependencies.

## Compose

- Unidirectional data flow.
- Stateless reusable components where possible.
- Screen state represented by immutable models.
- Navigation isolated from screen content.
- Previews use synthetic data only.

## C

- Compile with warnings treated seriously.
- Bounded string operations.
- Validate every tuple and type.
- No dynamic allocation unless justified.
- Keep renderer independent from AppMessage decoding.
- Generated protocol header is read-only.

## JSON and rules

- Schema-valid and canonical.
- No hand-edited duplicated protocol constants.
- Fixtures are sanitized and have expected results.
- Rule IDs stable.

## Error handling

User-visible failures provide an action:
- grant notification access;
- install/open watchapp;
- check connection;
- view invalid rule;
- roll back ruleset;
- export diagnostics.


---

<!-- Source: spec/700-ai/DefinitionOfDone.md -->

# Definition of Done

A requirement is done only when:

- implementation exists;
- automated tests cover success and failure paths;
- user-facing text and accessibility are complete;
- no privacy boundary is weakened;
- relevant spec/status docs are updated;
- Android unit tests and lint pass;
- watchapp builds if protocol/UI touched;
- rule regression passes if parsing touched;
- no secrets or private captures are committed;
- requirement ID is referenced in the commit or pull request.

A milestone is done only when all its listed requirements meet this definition.


---

<!-- Source: spec/800-roadmap/Milestones.md -->

# Implementation Roadmap

## M0 — Repository and builds
Create monorepo, licenses, wrappers, scripts, CI skeleton, empty Android app and empty Pebble watchapp.

## M1 — Domain model and protocol
Implement generated protocol constants, normalized navigation model, session reducer and fake watch transport.

## M2 — Notification access and early filtering
Onboarding, listener declaration, permission status, app catalog and strict package allowlist.

## M3 — Debug capture
Snapshot selected fields, Room history, retention, debug list/detail, deletion and synthetic publisher.

## M4 — Rule engine
Schemas, canonicalization, operators, extractors, trace, layer precedence, unit tests.

## M5 — Editors and preview
Simple editor backed by JSON, expert editor, validation, test bench and event rerun.

## M6 — Google Maps initial rules
Capture corpus, sanitize fixtures, implement English/Dutch/French/German variants that evidence supports, document gaps.

## M7 — Pebble integration
Pebble transport, launch once, READY handshake, current-state sync, watch renderer, bitmap assets, stop/exit behavior.

## M8 — Export and maintainer workflow
Rules-only, privacy-safe and full diagnostic exports; rule-workbench inspect/test/diff/scaffold/sanitize/promote commands.

## M9 — Hardening
Process restoration, connection loss, stale state, performance, regex limits, migration tests and privacy review.

## M10 — Release readiness
Play disclosures, privacy policy template, signed builds, release CI, physical-device test matrix and beta checklist.

## M11 — Remote official rules, post-v1
Signed HTTPS updates, compatibility checks, self-tests, atomic activation and rollback. Feature remains off until separately approved.


---

<!-- Source: requirements/REQ-ANDROID.md -->

# Android Requirements

## REQ-ANDROID-001 — Platform
The application SHALL be Kotlin-based, use Compose Material 3, have minSdk 31, and use a release-time-verified Play-compatible target SDK.

**Acceptance:** Gradle configuration and CI build succeed on the documented JDK/SDK.

## REQ-ANDROID-002 — System listener
The app SHALL use `NotificationListenerService` and SHALL NOT require a permanent foreground service for normal operation.

**Acceptance:** Eligible notifications are received while the activity is closed.

## REQ-ANDROID-003 — Early package filter
The listener SHALL inspect only source metadata needed for allowlisting before deciding eligibility. It SHALL NOT read notification content for disabled packages.

**Acceptance:** Instrumented spy proves snapshot factory, parser and database are never called for a disabled package.

## REQ-ANDROID-004 — Default app enablement
Every installed app represented by the bundled navigation-app catalog SHALL be enabled on first discovery by default. The user MAY disable each app.

**Acceptance:** Fresh-install test with mocked installed packages.

## REQ-ANDROID-005 — Capture-only apps
Catalog apps without official rules MAY capture debug notifications when enabled but SHALL be labeled capture-only and SHALL NOT claim valid watch output.

## REQ-ANDROID-006 — Event processing
Processing SHALL be serialized per application process and SHALL not block the main callback thread with database or rule evaluation work.

## REQ-ANDROID-007 — No polling
The app SHALL be event-driven and SHALL NOT poll notification state, Pebble connection, or GPS continuously.

## REQ-ANDROID-008 — Debug retention
Default eligible-event retention SHALL be 500 with configurable limits and delete-all.

## REQ-ANDROID-009 — Settings
Settings SHALL include per-app enablement, auto-launch, return-to-watchface, vibration, optional backlight, retention and export privacy default.

## REQ-ANDROID-010 — Process recovery
The app SHALL restore latest state safely and SHALL never replay obsolete turn events as a queue.


---

<!-- Source: requirements/REQ-RULES.md -->

# Rule Requirements

## REQ-RULE-001 — Declarative only
Rules SHALL be declarative JSON interpreted by fixed built-in operators and extractors.

## REQ-RULE-002 — No executable remote code
Rules SHALL NOT execute scripts, load classes, invoke reflection, run shell commands, or download code.

## REQ-RULE-003 — Source package
Every rule SHALL specify one or more exact package names.

## REQ-RULE-004 — Layer order
User overrides/additions SHALL take precedence over downloaded official rules, which SHALL take precedence over bundled official rules.

## REQ-RULE-005 — Immutable official rules
Official rules SHALL be read-only in the UI. Editing SHALL create a user clone.

## REQ-RULE-006 — Simple editor identity
The simple editor SHALL edit the same JSON-backed rule model used by the expert editor.

## REQ-RULE-007 — Expert validation
Invalid JSON or semantically invalid rules SHALL not be saved as enabled rules.

## REQ-RULE-008 — Trace
Each evaluation SHALL be capable of producing a condition/extractor trace suitable for debugging.

## REQ-RULE-009 — Preview
A rule SHALL be testable against retained captures without restarting navigation.

## REQ-RULE-010 — Locale-ready
The engine and schema SHALL support locale-specific rules from the first implementation. Initial target locales are English, Dutch, French and German.

## REQ-RULE-011 — Limits
Rule count, input size, pattern size and evaluation time SHALL be bounded.

## REQ-RULE-012 — Fixtures
Every official rule change SHALL include sanitized fixtures and expected normalized outputs.

## REQ-RULE-013 — Google Maps first
Google Maps is the first official rules target. Other catalog apps may remain capture-only until evidence-backed rules are merged.


---

<!-- Source: requirements/REQ-DEBUG.md -->

# Debug and Maintainer Requirements

## REQ-DEBUG-001 — History detail
Each retained eligible event SHALL show timestamp, package, event type, selected raw fields, active ruleset, match result, normalized output, trace and transport result.

## REQ-DEBUG-002 — No unrelated history
No debug record SHALL be created for a disabled package.

## REQ-DEBUG-003 — Rule creation
A user SHALL be able to start a new rule from a captured event.

## REQ-DEBUG-004 — Re-run
A retained event SHALL be rerunnable against current, bundled, downloaded or selected candidate rules.

## REQ-DEBUG-005 — Export modes
The app SHALL provide rules-only, privacy-safe diagnostic and full diagnostic exports.

## REQ-DEBUG-006 — Explicit sharing
The app SHALL use the Android Sharesheet and SHALL never automatically transmit diagnostics.

## REQ-DEBUG-007 — Privacy explanation
Before diagnostic export, the app SHALL explain that private content is limited to what the navigation app displayed in its notification, while clearly listing examples of private information that notification may contain.

## REQ-DEBUG-008 — Developer workbench
The repository SHALL include a CLI capable of inspecting exported JSON and read-only Room databases, testing candidate rules, generating diffs, sanitizing fixtures and producing regression reports.

## REQ-DEBUG-009 — Community contribution
The maintainer workflow SHALL support GitHub pull requests containing rules, fixtures, expected outputs and generated regression reports.


---

<!-- Source: requirements/REQ-WATCH.md -->

# Watch Requirements

## REQ-WATCH-001 — Renderer
The watchapp SHALL render normalized instructions and SHALL NOT parse Android notification text.

## REQ-WATCH-002 — Bitmap maneuvers
Maneuver graphics SHALL use bundled bitmap resources.

## REQ-WATCH-003 — READY
The watchapp SHALL send READY after AppMessage initialization.

## REQ-WATCH-004 — Current state
After READY, the phone SHALL send only the latest current state.

## REQ-WATCH-005 — Automatic launch
The phone SHALL automatically launch the watchapp once when a new navigation session starts by default. This SHALL be user-configurable.

## REQ-WATCH-006 — No focus stealing loop
The phone SHALL NOT repeatedly relaunch the watchapp during the same session after the user leaves it.

## REQ-WATCH-007 — Navigation stop
On navigation stop, the watch SHALL return to the watchface by default. This SHALL be user-configurable.

## REQ-WATCH-008 — Low power
The watchapp SHALL avoid polling, continuous animation and unnecessary redraws.

## REQ-WATCH-009 — Vibration
The watch SHALL not vibrate on every distance update; maneuver-change vibration is configurable.

## REQ-WATCH-010 — Compatibility
Protocol incompatibility SHALL produce an explicit state rather than silent failure.


---

<!-- Source: requirements/REQ-SECURITY.md -->

# Security and Privacy Requirements

## REQ-SEC-001 — Local processing
Notification parsing SHALL occur locally.

## REQ-SEC-002 — No automatic upload
Notification content and diagnostics SHALL never be uploaded automatically.

## REQ-SEC-003 — Minimal snapshot
The app SHALL serialize only the documented selected fields and SHALL exclude PendingIntents, actions, RemoteViews and arbitrary bundles.

## REQ-SEC-004 — Disclosure
The app SHALL show a prominent notification-access disclosure before sending the user to system settings.

## REQ-SEC-005 — Remote authenticity
Downloaded official rules, when enabled in a later milestone, SHALL be obtained over HTTPS and verified with an embedded public key before activation.

## REQ-SEC-006 — Rollback
A downloaded ruleset SHALL not replace the last-known-good ruleset until schema validation, signature validation and self-tests pass.

## REQ-SEC-007 — Secrets
No secret SHALL be included in the app, repository, rules, fixtures or export examples.

## REQ-SEC-008 — Temporary exports
Export files SHALL use temporary content URIs and have a documented cleanup policy.
