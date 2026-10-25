# Developer Code Scan: PebbleNTN (intent 261025-settings-sync, #102)

Focused scan at commit 4779760ac7d0cba527fe1c14753ef2839d426720 (v0.0.76), depth Minimal. Lead-only run: the same link also wrote the nine-artifact candidate in `aidlc/spaces/default/intents/261025-settings-sync/.aidlc-engine/codekb-stage-PebbleNTN/`.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - protocol/ (protocol-definition.json, generate.py, check_consistency.py, README.md)
  - scripts/generate-protocol.sh
  - scripts/test-all.sh
  - watchapp/package.json
  - watchapp/src/c/main.c (AppMessage, handshake, inbox, watchdog, settings and shortcut paths, init; drawing procedures skimmed)
  - watchapp/src/c/theme.c
  - watchapp/src/c/theme.h
  - watchapp/src/c/shortcuts.c
  - watchapp/src/c/shortcuts.h
  - watchapp/src/c/shortcut_apply.c
  - watchapp/src/c/shortcut_apply.h
  - watchapp/src/c/settings_window.c (change paths, Shortcuts door, main menu; door drawing skimmed)
  - watchapp/src/c/settings_window.h
  - watchapp/src/c/early_warning.h
  - watchapp/src/c/generated/protocol.h
  - watchapp/tools/capture_screenshots.py
  - android/app/src/main/java/com/pebblentn/app/pebble/ (NavigationController, WatchListenerService, PebbleWatchTransport, PebbleAppMessageMapper)
  - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/AppMessage.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
  - android/app/src/main/java/com/pebblentn/app/core/WatchSettings.kt
  - android/app/src/main/java/com/pebblentn/app/data/WatchSettingsRepository.kt
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt
  - android/app/build.gradle.kts
  - spec/200-architecture/Protocol.md
  - spec/200-architecture/Pebble.md
- **Skimmed only**:
  - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt (READY handling, flags)
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt (row helpers, Watch section), ui/MainActivity.kt (routes), ui/format/DisplayLabels.kt, ui/apps/NavigationAppsScreen.kt
  - android/app/src/main/res/values/strings.xml (conventions), android/app/src/main/res/drawable-nodpi/ (listing)
  - android/app/src/test/java/com/pebblentn/app/ (listing; FakeWatchTransport.kt, WatchSettingsRepositoryTest.kt, DistanceQuantizerTest.kt)
  - watchapp/src/c/ and watchapp/tests/ (listings), watchapp/tests/test_shortcuts.c, watchapp/tools/touch_emulator.py and the gen_*.py headers
  - requirements/REQ-WATCH.md (011, 024, every "SHALL NOT change the protocol" line), requirements/REQ-ANDROID.md (headings, 009), spec/200-architecture/Android.md (grep)
  - outside the snapshot, read only for context: MANIFEST.sha256.json (key list), AGENTS.md (rules 15-18), scripts/test-watchapp-unit.sh, screenshots/ (listing and README)
- **Left out**:
  - watchapp/build/
  - watchapp/tools/__pycache__/
  - scripts/__pycache__/
  - the AI-DLC install (.claude/, aidlc/ and its sections of shared files)

### Packages Found
- `:app` (com.pebblentn.app) — Android application — Kotlin — companion app; the scan covered `pebble/`, `protocol/`, `core/WatchSettings`, `data/WatchSettingsRepository`, `di/AppContainer`
- `watchapp` — Pebble watchapp — C (SDK 3) — renderer, settings, shortcuts
- `protocol/` — code generator — Python — emits `Protocol.kt` and `watchapp/src/c/generated/protocol.h` from one JSON file
- `watchapp/tools/` — maintainer tools — Python (stdlib) — emulator screenshots, touch over VNC, asset generators

### Build System
- **Type**: Gradle (AGP 8.13.2, Kotlin 2.3.10) for Android; waf via `pebble build` for the watchapp; Python scripts for generation
- **Config Files**: android/app/build.gradle.kts (minSdk 31, target 35, compile 36, versionCode 76; release `isMinifyEnabled` + `isShrinkResources`; unit tests `isIncludeAndroidResources = true`), watchapp/package.json (targets aplite, basalt, chalk, diorite, emery; `messageKeys: ["dummy"]`; `companionApp` = com.pebblentn.app)
- **Build Dependencies**: protocol-definition.json -> Protocol.kt + protocol.h (`scripts/generate-protocol.sh`, which first runs `check_consistency.py` against `examples/protocol-definition.json`); `:app` -> `:pebblekit`

### APIs Discovered
- AppMessage protocol 1.2 — protocol/protocol-definition.json — 15 keys (0-14), 6 events (1, 2, 3, 10, 11, 12), 5 flag bits (0-4)
- Watch persisted settings — watchapp/src/c/theme.c — 23 user settings under persist keys 1-15 and 19-26, plus internal keys 16-18 and 27
- PebbleKit 2 transport — android/.../pebble/ — `send`, `launchApp`, `inbound`, `watchLink`, `linkDiagnostics`

### Frameworks & Libraries
- Pebble C SDK 3 (installed `latest`) — AppMessage, persist, MenuLayer, recognizers on emery
- PebbleKit Android 2 — 1.2.0, vendored — watch transport
- Jetpack Compose (BOM 2024.12.01), Material 3, Navigation Compose 2.8.5 — UI
- DataStore Preferences 1.1.1 — declared by `:app`, unused in app code
- JUnit 4, Robolectric 4.14.1, coroutines-test, Compose UI test — tests

### Test Coverage
- **Test Directories**: android/app/src/test/ (71 sources), watchapp/tests/ (10 host tests incl. test_shortcuts.c)
- **Test Frameworks**: JUnit 4 + Robolectric; plain C under `gcc -std=c11 -Wall -Wextra -Werror` (one unit per test binary, no `pebble.h`)
- **Coverage Config**: absent

### Code Quality Indicators
- **Linting**: Android Lint (default config); C warnings only in host tests
- **CI/CD**: .github/workflows (android, watchapp, rules, codeql, release, play-release); `scripts/test-all.sh` is local only
- **Documentation**: REQ-ID comments in every module; MANIFEST-protected spec

### Technical Debt Signals
- `main.c` is 1,407 lines and `settings_window.c` 971; #102 would add to `main.c` unless it gets its own unit.
- Setting enum values will exist in four places (theme.h/shortcuts.h, the protocol keys, the phone mirror, capture_screenshots.py) with no generated or checked link; the menu row order is already copied by hand into capture_screenshots.py.
- `settings_set_*` write flash even when the value is unchanged.
- No outbox handlers on the watch; int tuples read without type checks.
- Phone `WatchSettings` has three fields with no storage or UI, though REQ-ANDROID-009 calls them settings.

## Handoff Summary
- **Intent-relevant finding**: everything #102 needs already funnels through a few seams: the `theme.c` setters (watch, theme.c:300-404), `send_ready()` and `inbox_received_handler` (main.c:1020-1100), `ProtocolCodec` + `NavigationController.handleInbound` (phone), and the generator (`protocol/`). Nothing on the phone knows any watch setting, the platform or the capabilities today, and the watch sends `WATCH_READY` only once per launch.
- **Risks / follow-up**: see "Facts relevant to #102" below, items 9-11 in particular (synthetic READY, relaunch on send, aplite heap).

### Facts relevant to #102

1. **Watch settings (persist key: name, range, default).** All in `theme.c` unless noted; ints are enum indices from `theme.h` / `early_warning.h` / `shortcuts.h`.
   - 1 accent: 0-15 (Green, Blue, Red, Orange, Purple, Teal, Pink, Yellow, Cyan, Lime, Tan, Navy, Amber, Grey, Black, White); default Green (0) on colour, Black (14) on black-and-white; the menu offers only 14/15 on black-and-white, the setter accepts all 16.
   - 2 units: 0 metric (default), 1 imperial.
   - 3 invert: bool, default false ("Dark on light").
   - 4 glyph pack: 0 Classic (default), 1 Bold, 2 Outline.
   - 5 arrow left: bool, default false (arrow right).
   - 6 ETA mode: 0 Arrival time (default), 1 Time to arrival ("Time left" in Shortcut texts).
   - 7 backlight: 0 Watch default (default), 1 Low 3 s, 2 Medium 10 s, 3 High until app closes.
   - 8 vibe pattern: 0 Off (default), 1 Single, 2 Double, 3 Triple, 4 Long.
   - 9 vibe intensity: 0 Light, 1 Medium (default), 2 Strong.
   - 10 backlight colour: 0 Watch default (default), 1 White, 2 Warm white, 3 Red, 4 Orange, 5 Yellow, 6 Green, 7 Cyan, 8 Blue, 9 Purple, 10 Pink; offered only with `PBL_RGB_BACKLIGHT` (emery).
   - 11 ETA label style: 0 labelled "ETA at / ETA in" (default), 1 compact "ETA / IN".
   - 12 early-warning distance: 0 Off (default), 1 10 m, 2 25 m, 3 50 m, 4 100 m, 5 200 m.
   - 13 early-warning repeat: 0 Once (default), 1 Every update.
   - 14 early-warning pattern: 1-4 only, default 2 Double.
   - 15 early-warning intensity: 0-2, default 1 Medium.
   - 19 Touch: bool, default false; meaningful only on emery while `touch_service_is_enabled()`.
   - 20-26 shortcut slots in order Up, Down, Swipe left, Swipe right, Swipe up, Swipe down, Tap: `ShortcutAction` 0-14 (0 None, 1 ETA mode, 2 ETA label, 3 Next colour, 4 Previous colour, 5 Invert, 6 Arrow side, 7 Next glyph, 8 Vibration on/off, 9 Early warning next, 10 Early warning previous, 11 Backlight now, 12 Next backlight colour (RGB only), 13 Units, 14 Open settings); defaults None except Swipe left/right = 1. Swipe and tap slots exist only on emery.
   - Internal, not to sync: 16 phone version, 17 latest phone version, 18 notice shown (main.c); 27 last non-Off vibe pattern (1-4, maintained by `settings_set_vibe_pattern`). Total synced: 15 + 1 + 7 = 23. Next free persist key: **28** (for a stored revision).
2. **Protocol.** Version 1.2 (`protocolMajor` 1, `protocolMinor` 2); keys 0-14 in use, **15+ free**; events 1-3, 10-12 in use, **4-9 and 13+ free**; flag bits 0-4 in use, **5-31 free**. Generated: `android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt` and `watchapp/src/c/generated/protocol.h` (constants only; no types or optionality). `check_consistency.py` needs `protocolMajor`, `protocolMinor`, `keys`, `events` equal in `examples/protocol-definition.json` (MANIFEST-protected), so every new key/event means editing both JSON files and the manifest hash. `flagBits`, `maneuvers`, `errorCodes` are outside the check. `--check` runs in test-all.sh, the pre-commit hook and the android/watchapp workflows.
3. **WATCH_READY today.** Watch: `send_ready()` (main.c:1020) writes int32 EVENT=10, MAJOR, MINOR and cstring APP_VERSION, called once at the end of `init()`; a busy outbox drops it silently; never resent on reconnect. Phone: `WatchListenerService.onMessageReceived` -> `PebbleAppMessageMapper.fromDictionary` (any int width -> Int, Bytes dropped) -> `WatchInboundBus` -> `NavigationController.handleInbound` -> `ProtocolCodec.decodeInbound` (`ReducerEvent.WatchReady(major, minor)`; missing keys read as 0) and `ProtocolCodec.watchAppVersion` (key 11, real READY only) -> `UpdateCheckRepository.recordWatchAppVersion`. The reducer checks only the major and answers with the current state. The phone also synthesizes READY (`scheduleAutonomousReady`, `appOpenedReadyMessage`) carrying its own MAJOR/MINOR and no key 11.
4. **AppMessage buffers.** `app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum())`, both at the platform maximum, from the app heap; exact sizes are not in code. Only the inbox handler is registered. READY is about 48 bytes now; with 23 settings + revision + platform + capabilities as int32 tuples about 334 bytes (about 256 with uint8). Phone outbound is always Int32/Text.
5. **Phone settings storage and the `WatchSettings` overlap.** `WatchSettingsRepository` (SharedPreferences `pebblentn_settings`) stores only `watch_auto_launch`. `vibrateOnManeuverChange` = true, `activateBacklight` = false and `exitToWatchfaceOnStop` = true are fixed defaults with no UI or storage; they only set FLAGS bits 1, 2 and 0. The watch plays the bit-1 pulse only while its own New instruction pattern is Off (the default), and honours bit 2 only while its Backlight is Watch default. So "merge into the synced model" means: drop or keep two hard-wired flags, not reconcile two edited values; removing bit 1 would silence default watches. REQ-ANDROID-009 still lists vibration, backlight and return-to-watchface as phone settings. Repositories follow the StateFlow + `@Volatile` cache pattern; DataStore is on the classpath but unused; `AppContainer` is the manual DI wiring point.
6. **Compose settings UI patterns.** `MainActivity.AppNavHost` with string routes and flows collected there; stateless screens with lambdas; sub-screens use `Scaffold` + `TopAppBar` with back `cd_back`; English-only string resources with `cd_*` content descriptions (AGENTS rules 16-17); `Section`, `ToggleRow` (whole row `toggleable`, `Role.Switch`, one TalkBack node), `NavRow`, `InfoRow` are private to `DashboardScreen.kt`; the Dashboard "Watch" section holds only the auto-launch toggle; enum-to-drawable via `DisplayLabels.maneuverIcon` and `drawable-nodpi/` (13 maneuver PNGs). No list-of-options picker exists yet; `NavigationAppsScreen` (with `NavigationAppsScreenTest`) is the closest list screen.
7. **Previews: what `capture_screenshots.py` produces vs what is missing.** Produces per basalt/chalk/emery, from a wiped emulator with a fixed 450 m right turn on "Rue de la Loi" and the clock pinned to 12:35 (ETA 13:00 / 0:25), by menu clicks: the default shot (covers every default value), accents blue/red/orange/purple/cyan, inverted (3), arrow left (3 incl. inverted), bold and outline packs, imperial, time to arrival, compact label (2), two short-distance shots and settings-screen shots; output `screenshots/<name>-<WxH>.png`, unoptimised RGBA, 0.9-2.4 KB. Missing: accents teal, pink, yellow, lime, tan, navy, amber, grey, black, white; a per-value naming scheme valid as an Android resource name (hyphens and the size suffix are not); a mode writing to `android/app/src/main/res/drawable-nodpi/`; PNG optimisation; aplite/diorite (the issue wants basalt greyscale instead, which needs basalt black and white accent shots). The clock is pinned and outliers re-shot via `clock_mask`, not masked. Count: 29 values per platform (23 distinct images if defaults share one), roughly 125-160 KB for three platforms. The committed folder lacks the v0.0.76 `settings-shortcuts*` shots.
8. **Where a preview-completeness check could go.** `scripts/test-all.sh` runs: spec integrity, protocol `--check`, version sync, watch host tests, rule schema, rule regression, route-capture tests, `gradlew test lint`. It is not run by CI. A Python check slots in beside the protocol `--check` but would be local-only unless also wired into a workflow; a JVM test runs in android.yml but cannot read `theme.h`. Release resource shrinking removes drawables only looked up by built names.
9. **Conflicts between the issue and the code.**
   - "On every connect, the watch includes its settings in WATCH_READY": READY is sent once per launch; a reconnect sends nothing.
   - "Show the settings screen once the watch reports the new protocol minor": the phone's synthetic READYs report the phone's own minor; gate on the real READY's settings/revision keys.
   - "Offline phone edits are queued": an immediate send to a closed watchapp relaunches it (`PebbleWatchTransport.send` on `FailedDifferentAppOpen`, auto-launch on by default), against the Pebble.md launch policy and REQ-WATCH-006.
   - "The watch validates every incoming value, as `settings_load` already does": bools are unchecked, the accent setter accepts colours a black-and-white watch cannot show, `shortcut_valid_action` keeps the RGB-only action on other watches, the accent default is platform-dependent.
   - "WatchSettings overlaps ... which already override it": three of its four fields are fixed defaults with no UI (item 5).
   - "capture_screenshots.py already ... writes most of these variants": 6 of 16 accents; names and location unsuitable for resources.
   - Protocol.md: "Minor versions only add optional keys and flag bits" must allow a new event; REQ-WATCH-024 says "SHALL NOT change the protocol **or the phone app**"; REQ-WATCH-017 also says "SHALL NOT change the protocol" but is the watchdog and need not change. No REQ-PROTOCOL file exists; next free IDs are REQ-WATCH-025 and REQ-ANDROID-021.
   - Naming: the menu says "Time to arrival", the Shortcut notice and action name say "Time left".
10. **Watch-side change hooks.** Every edit ends in a `theme.c` setter; the menus then call `notify_change` -> `on_settings_changed`, `run_shortcut` calls `on_settings_changed`, but the Shortcuts door's slot and Touch setters call neither. Applying a phone SETTINGS through the setters must not bump the watch revision or echo. Setters write flash unconditionally, so apply only changed values. Open menus have no refresh API, and the Vibration and Shortcuts doors change row counts with the early-warning distance and Touch.
11. **Risks.**
    - aplite heap (about 3.9 KB free at load after v0.0.76): code and data share the app RAM, so the sync code costs heap one for one. A table-driven encoder/decoder over the 23 settings (key, validator, getter, setter per entry) is likely under 1 KB of code plus about 276 bytes of table; 23 hand-written write/find/validate blocks could reach 2-3 KB; a revision variable, a pending flag and a pure wrap-compare unit cost tens of bytes. Measure with `heap_bytes_free()` on aplite with the Settings menu and a submenu open. The maximum-size AppMessage buffers are a possible lever (size them to the largest real message, about 350 bytes) but are not measured.
    - Watch outbox collisions: READY, the confirmation reply and a watch-edit SETTINGS share one outbox with no sent/failed handler.
    - Echo and ping-pong: a confirmation reply that the phone treats as a new watch edit, or a phone apply that bumps the watch revision.
    - Flash wear from 23 unconditional writes per sync.
    - MANIFEST: every spec/requirement/example edit must be resynced in `MANIFEST.sha256.json` in the same change.
