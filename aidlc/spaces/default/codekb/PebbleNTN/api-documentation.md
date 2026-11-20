# API Documentation

PebbleNTN has no HTTP server. Its contracts are the notification input, the ruleset JSON, the Pebble AppMessage protocol, the diagnostics export, in-app navigation routes, and the internal Kotlin APIs of the Watch settings screen.

## 1. Notification Input (`NotificationListenerService`)

| Callback | Handler | Behaviour |
|---|---|---|
| `onNotificationPosted` | `NotificationDispatcher` | Master switch, then allowlist, then (only if both pass) a `NotificationSnapshot` is built and queued |
| `onNotificationRemoved` | `NotificationDispatcher` | Same gate; ends the session |
| `onListenerConnected` | `AppContainer` | Rediscovers installed navigation apps |

Extras read: title, text, subText, bigText, summaryText, infoText, template, progress, progress max, channel id, category, when, and the large icon (reduced to a drawable name).

## 2. Ruleset JSON (schema v1, `schemas/ruleset.schema.json`)

Strict parsing (`RulesetCodec`): unknown keys, operators or extractors fail; duplicate ids fail. Limits in `RuleValidator`.

- **Fields**: `packageName`, `title`, `text`, `subText`, `bigText`, `summaryText`, `infoText`, `category`, `channelId`, `template`, `combinedText`, `iconDrawable`.
- **Operators**: `exists`, `notExists`, `equals`, `equalsIgnoreCase`, `contains`, `containsIgnoreCase`, `startsWith`, `endsWith`, `regex` (SafeRegex: 4096-char input cap, 50 ms budget), `in`.
- **Extractors**: `literal`, `regexCapture`, `fieldCopy`, `firstNonEmpty`, `distance`, `duration`, `arrivalClock` (#134: clock text to the next epoch in the phone's zone), `maneuverMap`, `normalizeString`, `boundedJoin`.
- **Outputs**: `maneuver`, `distanceMeters`, `primaryText`, `compactText` (#134, street only), `secondaryText`, `etaEpochSeconds`, `stopsRemaining`.

## 3. Pebble AppMessage Protocol 1.5 (`protocol/protocol-definition.json`)

Generated into `protocol/Protocol.kt` and `watchapp/src/c/generated/protocol.h` by `scripts/generate-protocol.sh` (`--check` in CI and pre-commit).

| Event | Value | Direction | Keys |
|---|---|---|---|
| `NAVIGATION_UPDATE` | 1 | phone → watch | `MANEUVER` 3, `DISTANCE_METERS` 4, `PRIMARY_TEXT` 5 (Full or Compact road line), `SECONDARY_TEXT` 6, `ETA_EPOCH_SECONDS` 7, `STATE_TIMESTAMP_SECONDS` 8, `SESSION_ID` 9, `FLAGS` 10, `STOPS_REMAINING` 13 |
| `NAVIGATION_STOPPED` | 2 | phone → watch | `SESSION_ID`, `FLAGS` |
| `NO_ACTIVE_NAVIGATION` | 3 | phone → watch | `FLAGS` |
| `SETTINGS` | 4 | both | `SETTINGS_REVISION` 15, `SETTING_*` 18-44 (phone sends only pending ones) |
| `WATCH_READY` | 10 | watch → phone | `PROTOCOL_MAJOR` 1, `PROTOCOL_MINOR` 2, `APP_VERSION` 11, `WATCH_PLATFORM` 16, `WATCH_CAPABILITIES` 17, `SETTINGS_REVISION`, all `SETTING_*` (1.3+) |
| `WATCH_REQUEST_STATE` | 11 | watch → phone | — |
| `PHONE_COMPATIBILITY_ERROR` | 12 | phone → watch | `ERROR_CODE` 12 (`INCOMPATIBLE_PROTOCOL_MAJOR` = 1) |

Every phone message also carries the protocol version, `APP_VERSION` and `LATEST_PHONE_VERSION` 14 when an update exists.

**Setting keys** (value = key − 18 = `WatchSetting.ordinal`): 18 ACCENT, 19 UNITS, 20 INVERT, 21 GLYPH_PACK, 22 ARROW_LEFT, 23 ETA_MODE, 24 BACKLIGHT, 25 VIBE_PATTERN, 26 VIBE_INTENSITY, 27 BACKLIGHT_COLOR, 28 ETA_LABEL, 29 EARLY_WARN_DISTANCE, 30 EARLY_WARN_REPEAT, 31 EARLY_WARN_PATTERN, 32 EARLY_WARN_INTENSITY, 33 TOUCH, 34-40 SHORTCUT_UP / DOWN / SWIPE_LEFT / SWIPE_RIGHT / SWIPE_UP / SWIPE_DOWN / TAP, 41 DRIVING_MODE, 42 DRIVING_SPEED_METRIC, 43 DRIVING_SPEED_IMPERIAL (1.4), 44 DIRECTIONS_TEXT (1.5).

**Flags**: `EXIT_TO_WATCHFACE_ON_STOP` 0, `VIBRATE_ON_MANEUVER_CHANGE` 1, `ACTIVATE_BACKLIGHT` 2, `STATE_IS_STALE` 3, `PHONE_UPDATE_AVAILABLE` 4. **Capability bits**: COLOR 0, ROUND 1, RGB_BACKLIGHT 2, TOUCH 3. **Platforms**: UNKNOWN, APLITE, BASALT, CHALK, DIORITE, EMERY. **Maneuvers**: UNKNOWN 0 … TRANSIT 12.

## 4. Diagnostics Export (schema 7, `export/ExportBuilder.kt`, `spec/300-data/ExportFormat.md`)

Modes: full, privacy-safe (structural redaction by `Redactor`), rules-only. Size-capped (newest 10 MB). Carries versions, rulesets, user packages, install record, driving log, `metadata.watchSettings` by name, and `watchOutput.compactText`. Shared via the Sharesheet only.

## 5. Other External Calls

- GitHub releases feed (`update/ReleaseFetcher.kt`): optional, never on a Google Play install.
- Signed official ruleset updates (`update/RuleUpdateRepository.kt`, Ed25519): implemented, disabled.

## 6. In-app Routes (`ui/MainActivity.kt`)

`dashboard`, `debug`, debug detail, `watch-settings`, `watch-settings/{setting}` (setting = `WatchSetting.name`), `navigation-apps`, app packages, rules screens, `rules-review`, `rule-editor`, share diagnostics, onboarding.

## 7. Internal Watch Settings APIs (intent focus)

| API | Contract |
|---|---|
| `WatchSettingsUi.sections(model): List<WatchSettingsSection>` | Groups and rows the watch offers; `WatchSettingsSection(title: @StringRes Int, settings, note: @StringRes Int? = null)` |
| `WatchSettingsUi.title(setting): @StringRes Int` | Exhaustive mapping, 27 entries (both driving speeds share one title) |
| `WatchSettingsUi.valueLabels(setting, imperial): @ArrayRes Int` | Value labels indexed by value |
| `WatchSettingsUi.options(setting, model): List<Int>` | Values offered, watch-menu order |
| `WatchSettingsUi.preview(setting, value, platform): WatchPreview?` | Preview image or null |
| `WatchSettingsUi.status(state)`, `showsQueuedNotice`, `showsDrivingSection`, `drivingSpeedValue`, `drivingDefaultValue`, `showsDrivingReset` | Screen state helpers |
| `WatchSettingsScreen(state, watchappOpen, onOpenSetting, onBack, modifier, onChangeSetting)` | Composable entry |
| `OptionListScreen(setting, model, queued, onSelect, onBack, modifier)` | Composable value list |
| `WatchSettingsRules.isOffered(setting, caps)`, `isValid`, `options`, `defaultValue` | Capability and range rules (same as watch `sync_valid` / `sync_default`) |
| `WatchSettingsSync.onPhoneEdit / onWatchSettings / outgoing / decide` | Revisioned sync |
| `AppContainer.setWatchSetting(setting, value)` → `NavigationController.onPhoneSettingEdit` | Edit entry point from UI |
