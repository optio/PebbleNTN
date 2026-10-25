# Code Generation Plan — Sync watchapp settings with the companion app (#102)

## Summary

- **Builds:**
  - protocol 1.3 with a two-way `SETTINGS` event;
  - a table-driven settings sync on the watch, with revision comparison, validation, outbox handling and FR8's honest vibration Off;
  - a phone mirror with an offline queue;
  - the Compose "Watch settings" screen with option lists and previews;
  - a preview mode in the screenshot tool and the generated drawables;
  - specs and docs.
- **Touches:** `protocol/`, `examples/protocol-definition.json`, the generated constants, `watchapp/src/c/` (new `settings_sync.c/.h`, `revision.c/.h`, plus `theme.c/.h`, `shortcuts.c`, `main.c`, `settings_window.c`), new watch host tests, and the Android packages `protocol/`, `pebble/`, `data/`, `core/`, `ui/`, `di/`, plus `res/`, `watchapp/tools/capture_screenshots.py`, `requirements/`, `spec/200-architecture/`, `MANIFEST.sha256.json` and `docs/IMPLEMENTATION_STATUS.md`.
- **Tests:** new watch host tests, Kotlin JVM tests, an emulator check, and the full `scripts/test-all.sh`.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR9, NFRs). The code knowledge base is at `aidlc/spaces/default/codekb/PebbleNTN/`, and the facts list is in `inception/reverse-engineering/developer-scan-PebbleNTN.md`.

### Design decisions

**Protocol (FR1):**
- Version 1.3. Event `SETTINGS` = 4.
- **Keys from 15 upward, in this order:**
  - `SETTINGS_REVISION`, `WATCH_PLATFORM`, `WATCH_CAPABILITIES`;
  - then one key per synced setting, in the watch's persist order: accent, units, invert, glyph pack, arrow left, ETA mode, backlight, vibe pattern, vibe intensity, backlight colour, ETA label, early-warning distance, repeat, pattern, intensity, touch;
  - then shortcut slots 0–6.
- **Value encodings:**
  - capability bits: 0 colour, 1 round, 2 RGB backlight, 3 touch;
  - platform values: 0 unknown, 1 aplite, 2 basalt, 3 chalk, 4 diorite, 5 emery.
- Edit both definition JSONs, regenerate with `scripts/generate-protocol.sh`, and keep `--check` green.

**Watch:**
- **`revision.c/.h` (pure, host-tested):** `revision_newer(a, b)` (wrap-around 0–99, where 50 is not newer), `revision_next(r)`.
- **`settings_sync.c/.h` (table-driven):**
  - **The table:** one `static const` table of `{key, getter, setter, validator}` for the 23 settings. The validator checks the range and the platform capability. The setters are the existing `theme.c` / Shortcuts setters.
  - **Pure parts (no `pebble.h`, host-tested):** `sync_valid(id, value, caps)`, the capability bits for this build, and the decision `sync_decide(watch_rev, phone_rev, differs)`, which returns apply-phone, keep-watch or same.
  - **Pebble glue:**
    - `settings_sync_write(DictionaryIterator*)` adds all keys;
    - `settings_sync_apply(DictionaryIterator*)` applies the changed, valid values with no revision bump and no echo;
    - `settings_sync_on_watch_edit()` bumps the revision (persist key 28) and asks for a send.
- **Change notification:** every watch-edit path (menus via `notify_change`, the Shortcuts door's setters, Touch, and `run_shortcut`) calls `settings_sync_on_watch_edit()`. The setters skip the flash write when the value is unchanged.
- **`main.c`:**
  - `send_ready()` includes the settings;
  - **Outbox queue:** one message at a time, with `app_message_register_outbox_sent/failed`, one retry, and a pending flag for ready, settings and confirm;
  - **Connection:** on reconnect, send `SETTINGS`;
  - **Inbound:** `SETTINGS` goes to the decision, then apply-and-confirm or reply-with-watch;
  - **Buffers:** size the AppMessage buffers to the largest real message (measured) on aplite if the heap requires it.
- **Settings window:** `settings_window_refresh()` reloads open menus. If row counts changed, it pops back to the root menu.
- **FR8:**
  - Add `VIBE_PATTERN_SILENT` = 5, labelled "Off"; value 0 is relabelled "Short pulse (default)".
  - The phone's vibrate flag is honoured for 0 only; 5 is silent.
  - In the menu order, Off is listed first.
  - Shortcut toggle: any non-silent pattern goes to silent, and silent goes back to the last pattern (key 27 now stores 0–4, default 0). `shortcuts.c` and its test are updated to match.

**Phone:**
- **`core/` (pure, JVM-tested):**
  - `WatchSettingsSync.kt`: the revision comparison, mirroring C;
  - `WatchSettingsModel`: 23 typed fields plus revision, platform and capabilities, with defaults and validation that match the watch;
  - the merge rules (adopt the watch, keep the queue for values the watch didn't send newer);
  - `PreviewCatalog`: (setting, value, platform) → drawable id, with static `R.drawable` references and the greyscale flag for aplite and diorite.
- **`ProtocolCodec`:** decode the settings keys on the watch's real READY and on `SETTINGS`; encode `SETTINGS`. A synthetic READY carries no settings and is ignored for the mirror.
- **`data/WatchSettingsMirrorRepository`:** SharedPreferences, following the existing StateFlow + `@Volatile` pattern. It holds the mirror, the phone revision, the pending flag, and "received real settings" for the waiting state.
- **`NavigationController` / transport:**
  - track "watchapp open" (a real READY in this session, cleared when the app is closed or disconnects);
  - send phone edits only while it is open, otherwise queue;
  - flush on the next real READY;
  - adopt watch settings by the rule.
- **UI (`ui/watchsettings/`):**
  - **The screens:** a `WatchSettingsScreen`, with groups as in FR5, capability gating and the waiting or update states; and an `OptionListScreen` with the preview above the list (a greyscale `ColorFilter` for black-and-white watches), the selected value, and colour thumbnails.
  - **Wiring:** a route in `MainActivity.AppNavHost`, and a dashboard Watch-section `NavRow`.
  - **Strings:** every string is a resource, every control has a `cd_*` description, and the preview's description names the value.

**Previews (FR7):**
- Add a `--previews` mode to `capture_screenshots.py`. Per platform (basalt, chalk, emery), it captures one image per visual value with every other setting at its default, using the existing pinned clock and instruction:
  - accent ×16 (basalt includes Black and White);
  - invert ×2, glyph pack ×3, arrow side ×2, ETA mode ×2, ETA label ×2, units ×2.
- It writes optimised PNGs to `android/app/src/main/res/drawable-nodpi/watch_preview_<setting>_<value>_<platform>.png`.
- A JVM test checks completeness against the model's value lists, so it runs in CI.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "express",
  "test_strategy": "minimal",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "minimal",
    "strategy_volume": [
      "One verifiable test per requirement at the narrowest effective level.",
      "At least one happy-path unit test per component.",
      "Unit tests are the default; a bugfix/security scope floor may require an integration or E2E regression when that is the narrowest level that reproduces the defect."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:c8f84e35190e007d9863fad236154d0269ab9bf633fc2a3ff106ed965ba4a9b9",
  "contract_sha256": "sha256:bb76c1b22f8307be6e5780d50cabab891684449d5b164bd0e825e9e4e5c61e5b"
}
```

## Steps

- [x] **Step 1: Project structure.** Create the new watch files (`revision.c/.h`, `settings_sync.c/.h`) and the new Kotlin files (`core/WatchSettingsSync.kt`, `core/WatchSettingsModel.kt`, `core/PreviewCatalog.kt`, `data/WatchSettingsMirrorRepository.kt`, `ui/watchsettings/`), with header comments.
- [x] **Step 2: Verify the test runners.** Run `scripts/test-watchapp-unit.sh` and `android/gradlew -p android :app:testDebugUnitTest`, and confirm both are green before any change.
- [x] **Step 3: API / protocol: implement (FR1).** Update both protocol definition JSONs to 1.3, regenerate, run `--check`, and extend `ProtocolCodec` to decode and encode.
- [x] **Step 4: API / protocol: tests (FR1).** JVM tests: a 1.2 READY without the settings keys still decodes; a 1.3 READY decodes all 23 settings, the revision, the platform and the capabilities; a synthetic READY gives no settings; `SETTINGS` round-trips.
- [x] **Step 5: Data model: implement (FR3, FR4, FR8).**
  - Watch: persist key 28 (revision), the setters skip unchanged writes, and `VIBE_PATTERN_SILENT`.
  - Phone: `WatchSettingsModel` with defaults and validation.
- [x] **Step 6: Data model: tests.** Watch host tests for the pattern names and the toggle (update `test_shortcuts`). JVM tests that the model validates by capability (black-and-white accents, RGB, touch).
- [x] **Step 7: Business logic: implement (FR2–FR4).** Watch: `revision.c`, `settings_sync.c` (table, decide, valid, write, apply), and the outbox queue and connection handling in `main.c`. Phone: `WatchSettingsSync`, the merge and queue rules, and the `NavigationController` wiring (track watchapp open, send or queue, flush on READY, adopt).
- [x] **Step 8: Business logic: tests.**
  - Watch host tests `test_revision.c` (rollover, 50, equal) and `test_settings_sync.c` (decide, valid).
  - Kotlin `WatchSettingsSyncTest` (the same cases), merge and queue tests (an offline edit is sent once on the next READY, never while closed), and a `NavigationController` test for no send to a closed watchapp.
- [x] **Step 9: Repository: implement and test (FR4).** `WatchSettingsMirrorRepository` and its `AppContainer` wiring. A JVM test that the mirror and pending edits survive a restart.
- [x] **Step 10: Frontend: implement (FR5, FR6).** `WatchSettingsScreen`, `OptionListScreen`, `PreviewCatalog`, the route, the dashboard `NavRow`, and strings and content descriptions. On the watch: `settings_window_refresh()`, and every edit path calls `settings_sync_on_watch_edit()`.
- [x] **Step 11: Frontend: tests (FR5, FR6).** Use the project's existing UI test approach, or pure state functions if no Compose test setup exists: capability gating, the waiting and update-watchapp states, the preview description names the value, and the black-and-white greyscale flag.
- [x] **Step 12: Previews (FR7).** Add `--previews` to `capture_screenshots.py`, run it on basalt, chalk and emery (Pebble emulators only), optimise the PNGs and write them to `drawable-nodpi/`. Add `PreviewCatalogTest`: every visual value on every platform has a drawable, and no drawable lacks a value. Close the emulators afterwards.
- [x] **Step 13: Build and memory (NFR).**
  - Run `pebble build` for all five platforms and `:app:assembleDebug :app:assembleRelease :app:lintDebug` (release keeps the previews).
  - On the aplite emulator, measure `heap_bytes_free()` with Settings and a submenu open, and record it. Size the AppMessage buffers if needed, so it is no worse than v0.0.76 minus 500 bytes.
- [x] **Step 14: Emulator sync check (FR2, FR3).** Using the Pebble emulator and `pebble` CLI AppMessage injection (no Android emulator):
  - a `SETTINGS` message with a newer revision changes the watch screen and gets a confirmation;
  - a stale revision gets the watch's settings back;
  - a watch edit sends `SETTINGS` (`pebble logs`);
  - silent Off doesn't vibrate with the phone flag set.
- [x] **Step 15: Specification and docs (FR9).**
  - REQ-WATCH-025 and REQ-ANDROID-021; amend REQ-WATCH-012/014/015/016/022/024 and REQ-ANDROID-009; update `Protocol.md` and `Pebble.md`.
  - Resync `MANIFEST.sha256.json`, run `scripts/validate-spec-assets.sh`, and add the `docs/IMPLEMENTATION_STATUS.md` entry.
  - Run the full `scripts/test-all.sh`.

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1 Protocol 1.3 | 3, 4 |
| FR2 Watch sends settings | 7, 8, 14 |
| FR3 Watch applies phone edits | 5, 7, 8, 10, 14 |
| FR4 Phone mirror and queue | 5, 7, 8, 9 |
| FR5 Watch settings screen | 10, 11 |
| FR6 Previews in lists | 10, 11, 12 |
| FR7 Preview generation and check | 12 |
| FR8 Honest vibration Off | 5, 6, 14 |
| FR9 Specs and docs | 15 |
| NFR aplite memory | 7, 13 |
| NFR reliability, flash | 5, 7, 8 |
