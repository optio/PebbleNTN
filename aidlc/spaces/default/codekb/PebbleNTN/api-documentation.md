# API Documentation

PebbleNTN has no HTTP server API. Its contracts are: the Android notification input, the declarative ruleset JSON, the Pebble AppMessage protocol, the diagnostics export, and a few internal Kotlin APIs that changes to rules touch.

## 1. Notification Input (Android `NotificationListenerService`)

| Callback | Handler | Behaviour |
|---|---|---|
| `onNotificationPosted(sbn)` | `NotificationDispatcher.onPosted(pkg, buildEligible)` | Master switch, then allowlist, then (only if both pass) build a `NotificationSnapshot` and enqueue it |
| `onNotificationRemoved(sbn)` | `NotificationDispatcher.onRemoved(pkg)` | Same gate; ends the session (`NavigationStopped`) |
| `onListenerConnected()` | `AppContainer.onListenerConnected()` | Rediscovers installed navigation apps |

Extras read (`NotificationSnapshotFactory`): `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_SUB_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_SUMMARY_TEXT`, `EXTRA_INFO_TEXT`, `EXTRA_TEMPLATE`, `EXTRA_PROGRESS`, `EXTRA_PROGRESS_MAX`, plus `channelId`, `category`, `when`, and the large icon (matched to a drawable name only).

## 2. Ruleset JSON (schema v1, `schemas/ruleset.schema.json`)

```json
{ "schemaVersion": 1, "rulesetVersion": "osmand-de-2026.10.10", "minimumAppVersionCode": 1,
  "createdAt": "…", "publisher": "PebbleNTN maintainers",
  "rules": [ { "id": "kebab-case", "enabled": true, "priority": 100,
    "packageNames": ["net.osmand.plus"], "locales": ["de"], "comment": "…",
    "conditions": [ { "field": "combinedText", "operator": "regex", "value": "(?i)\\brechts\\b" } ],
    "output": { "maneuver": {…}, "distanceMeters": {…}, "primaryText": {…},
                "secondaryText": {…}, "etaEpochSeconds": {…}, "stopsRemaining": {…} } } ] }
```

Parsing is strict (`RulesetCodec`): unknown keys, operators or extractor types fail; duplicate ids fail; `schemaVersion` must be 1. Semantic limits (`RuleValidator`): id kebab-case up to 120 characters, 1-20 packages, priority within ±100000, up to 50 conditions, up to 30 locales, regex compiles and is at most 1024 characters.

### Fields (`SnapshotFields.resolve`)

`packageName`, `title`, `text`, `subText`, `bigText`, `summaryText`, `infoText`, `category`, `channelId`, `template`, `combinedText` (title, text, bigText, subText, summaryText, infoText joined by a space), `iconDrawable`. Unknown names resolve to null. Unicode space separators become ASCII spaces; newlines stay (OsmAnd `bigText` has two lines).

### Operators (`ConditionOperator`)

`exists`, `notExists`, `equals`, `equalsIgnoreCase`, `contains`, `containsIgnoreCase`, `startsWith`, `endsWith`, `regex` (find, via `SafeRegex`: input capped at 4096 characters, 50 ms budget), `in` (uses `values`).

### Extractors (`Extractor`, discriminator `type`)

| Type | Parameters | Result |
|---|---|---|
| `literal` | `value` | Text |
| `fieldCopy` | `field` | Text of the field |
| `firstNonEmpty` | `fields[]`, `stripLeadingDistance` | First non-empty field, optionally without a leading `<distance> ·/•/|` |
| `regexCapture` | `field`, `pattern`, `group` (1) | Text of the capture group |
| `distance` | `field` | Num metres (`DistanceParser`: unit required; m/km/ft/mi/yd, spelled-out forms, `pi`, 公里/千米/公尺/米; `.` or `,` decimals) |
| `duration` | `field` | Num seconds (`DurationParser`: English `h|hr|hrs|hour|hours`, `min|mins|minute|minutes`, `s|sec|secs|second|seconds`, summed) |
| `maneuverMap` | `field`, `mapping{}`, `default` | Mapped text (exact, then case-insensitive) |
| `normalizeString` | `field`, `lowercase`, `collapseWhitespace`, `trim` | Text |
| `boundedJoin` | `fields[]`, `separator`, `maxLength` (64) | Joined text, cut at a surrogate-safe boundary |

### Output assembly (`RuleEngine.assemble`)

| Output | Accepted result | Notes |
|---|---|---|
| `maneuver` | Text token | `Maneuver.fromToken`, unknown → `UNKNOWN` |
| `distanceMeters` | Num | Coerced to at least 0 |
| `primaryText`, `secondaryText` | Text | A Num result is dropped |
| `etaEpochSeconds` | Num | `duration` → `nowEpochSeconds + seconds`; any other Num → absolute epoch; Text (for example a `regexCapture` of `18:55`) → dropped, so no clock text can become an ETA today |
| `stopsRemaining` | Num or Text | First integer of text, at least 0 |

Engine entry point: `RuleEngine(regexBudgetMillis = 50, families = RuleFamilies.NONE).evaluate(snapshot, LayeredRules, locale: String?, nowEpochSeconds: Long): RuleEvaluation(instruction, matchedRuleId, matchedLayer, trace)`. The locale's language part (before `-` or `_`) must be in `rule.locales` when `locales` is non-empty. In production `locale` is `Locale.getDefault().toLanguageTag()` and `now` is the processing time.

## 3. Pebble AppMessage Protocol v1.4 (`protocol/protocol-definition.json`)

Generated into `android/.../protocol/Protocol.kt` and `watchapp/src/c/generated/protocol.h` by `scripts/generate-protocol.sh` (CI checks they are current).

| Event | Value | Direction | Keys |
|---|---|---|---|
| `NAVIGATION_UPDATE` | 1 | phone → watch | `MANEUVER`, `DISTANCE_METERS`, `PRIMARY_TEXT` (≤64 B), `SECONDARY_TEXT` (≤24 B), `ETA_EPOCH_SECONDS` (only when set), `STOPS_REMAINING`, `STATE_TIMESTAMP_SECONDS`, `SESSION_ID`, `FLAGS`, plus version keys |
| `NAVIGATION_STOPPED` | 2 | phone → watch | `SESSION_ID`, `FLAGS` |
| `NO_ACTIVE_NAVIGATION` | 3 | phone → watch | `FLAGS` |
| `SETTINGS` | 4 | both | `SETTINGS_REVISION`, `SETTING_*` (accent, units, invert, glyph pack, arrow side, ETA mode, ETA label, backlight, vibration, early warning, touch, shortcuts, driving) |
| `WATCH_READY` | 10 | watch → phone | `PROTOCOL_MAJOR/MINOR`, `APP_VERSION`, `WATCH_PLATFORM`, `WATCH_CAPABILITIES` |
| `WATCH_REQUEST_STATE` | 11 | watch → phone | — |
| `PHONE_COMPATIBILITY_ERROR` | 12 | phone → watch | `ERROR_CODE` (`INCOMPATIBLE_PROTOCOL_MAJOR` = 1) |

Every phone message carries `PROTOCOL_MAJOR`, `PROTOCOL_MINOR`, `APP_VERSION`, and `LATEST_PHONE_VERSION` when an update is available. Flags: `EXIT_TO_WATCHFACE_ON_STOP` 0, `VIBRATE_ON_MANEUVER_CHANGE` 1, `ACTIVATE_BACKLIGHT` 2, `STATE_IS_STALE` 3, `PHONE_UPDATE_AVAILABLE` 4. Maneuver codes: `UNKNOWN` 0 … `TRANSIT` 12. The watch's ETA strip uses `ETA_EPOCH_SECONDS` for the time-left countdown when non-zero, else the first valid `H:MM` (with optional AM/PM) in `SECONDARY_TEXT` (REQ-WATCH-014). The app glance countdown runs only with an epoch.

## 4. Diagnostics Export (`export/ExportBuilder.kt`, `spec/300-data/ExportFormat.md`)

Three modes: full (raw snapshots), privacy-safe (text structurally redacted by `Redactor`), rules-only (user rules, no events). Size-capped; carries app and navigation-app versions, ruleset versions, user-added packages, install record, and the When driving log. Shared through the Android Sharesheet only, never uploaded.

## 5. Other External Calls

- **GitHub releases feed** (`update/ReleaseFetcher.kt`): optional update check, never on a Google Play install.
- **Signed official ruleset updates** (`update/RuleUpdateRepository.kt`, Ed25519): implemented, disabled (no embedded public keys).

## 6. Internal APIs a Rule Change Touches

| API | Location | Contract |
|---|---|---|
| `DurationParser.parseSeconds(text): Int?` | `rules/DurationParser.kt` | First match of each hour/minute/second pattern, summed; null when none |
| `DistanceParser.parseMeters(text): Int?`, `stripLeadingDistance(text)` | `rules/DistanceParser.kt` | Unit mandatory; leading distance dropped only before `·`, `•` or `|` |
| `DrivingSpeedEstimator.arrivalMillis(subText, postTimeMillis, zone): Long?` | `core/DrivingSpeedEstimator.kt` | Last `h:mm` (AM/PM aware) on the post day in `zone`, next day if already past by more than 1 min; the existing clock-to-epoch logic |
| `NavigationSessionReducer.reduce(state, event)` | `core/NavigationSessionReducer.kt` | Resends only on instruction change; ETA carry-over for 5 min within a session |
| Python engine subset `evaluate(snap, rules, locale, families)` | `tools/rule-workbench/workbench.py` | Mirrors the Kotlin engine for `literal`, `fieldCopy`, `firstNonEmpty`, `boundedJoin`, `distance`, `regexCapture`, `maneuverMap`; returns no ETA |
