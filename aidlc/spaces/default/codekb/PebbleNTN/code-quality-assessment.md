# Code Quality Assessment

## Test Coverage

| Area | Tests | Framework |
|---|---|---|
| Android app | 98 test sources under `android/app/src/test/java/com/pebblentn/app/` at 61787f5, covering core (reducer, settings model and sync, previews), data (Room v1-v8 migration, settings mirror), notification and icon, rules (per-app regression suites), export, protocol, pebble controller and settings queue, system, ui (view models, Compose screens), update | JUnit 4 + Robolectric + coroutines-test + Compose UI test |
| Android instrumented | `ManualOpenWithAutoLaunchOffTest` | AndroidJUnit4 |
| Watchapp | 12 host tests at 61787f5 (about_text, early_warning, eta_text, glance_text, panel_layout, phone_version, revision, settings_sync, shortcuts, stops_text, update_notice, version_compare) | plain C, `-Werror` |
| Rules | fixtures through `workbench.py regression` and Kotlin `*RulesRegressionTest`; catalog checks in `NavigationAppCatalogTest`; schema and `<queries>` checks in `scripts/validate-rules.sh` | Python + JUnit |
| Tools | `tools/route-capture/tests/test_routecap.py` | unittest |

Coverage measurement: **none configured** (no JaCoCo or Kover, no gcov). Untested by design: `PebbleWatchTransport` (needs a bound companion) and the Pebble API parts of `main.c`, `settings_window.c`, `settings_sync_glue.c` and `about_window.c`, which are verified in the emulator per the REQ acceptance criteria.

In the #132 area (61787f5): `NavigationControllerTest` (20 tests: launch once and defer the send, autonomous READY, READY sends state, stop with the exit flag, latest phone update on every send, genuine versus made-up READY, retries, compatibility error, a failing inbound stream, auto-launch off with manual READY and with a hand-opened watchapp, settings never sent to a closed watchapp, a real READY flushing the queue); `WatchSettingsQueueTest` (5: queued while closed and sent once on the next real READY, sent at once while open, newer watch settings replace the queue, a failed send keeps the edit, an outdated watchapp gets nothing); `WatchSettingsSyncTest` (10), `WatchSettingsModelTest` (7, pins 23 settings and the watch's defaults), `WatchSettingsMirrorRepositoryTest`, `WatchSettingsUiTest` (8), `WatchSettingsScreenTest` (7), `ProtocolCodecTest` (23 settings decoded, SETTINGS round trip), `NotificationSnapshotFactoryTest.capturesDocumentedProgressWhenPresentAndNullOtherwise`, `ExportBuilderTest`, `DiagnosticExporterTest`, `test_settings_sync.c` (pins 23 settings and persist keys 1, 15, 19, 26), `test_revision.c`, `test_early_warning.c`. `FakeWatchTransport` records launches, sends and open-app sends and emits inbound messages and closes, and `runTest` virtual time drives the autonomous READY delay, so time-based controller behaviour is testable today. Nothing covers speed, driving or hiding.

Earlier areas: update check, version compare, install source and record, update card and prompts, catalog and rule families, per-app rules and icon matching are covered as recorded in the findings below.

## Linting and Static Analysis

- Android Lint (`./gradlew lint`) in CI, pre-commit and release verify; default configuration with no baseline file.
- CodeQL java-kotlin on push, PR and weekly.
- No ktlint, detekt or formatter check. C warnings are enforced only in host tests; `_Static_assert`s in `settings_sync.c` and `theme.c` fail the watch build when the settings table and enums disagree.

## CI/CD

| Workflow | Trigger | Gates |
|---|---|---|
| android.yml | push/PR on `android/**` | protocol `--check`, `test`, `lint`, `assembleDebug`, APK artifact |
| watchapp.yml | push/PR on `watchapp/**` | protocol `--check`, `pebble build`, PBW artifact |
| rules.yml | push/PR on rules, schemas, examples | spec-asset integrity, schema validation, rule regression |
| codeql.yml | push/PR on main, weekly | CodeQL analysis |
| release.yml | push to main (not docs/tools/md) or manual | verify job, then version bump, changelog, signed APK, PBW, SHA256SUMS, tag `v<version>`, GitHub release |
| play-release.yml | manual (input `tag`) | target-SDK gate, signed AAB as a workflow artifact |

(Not re-read at 61787f5.) `.githooks/pre-commit` runs secrets scanning, MANIFEST integrity, protocol `--check`, version sync, rules, syntax checks and Android tests + lint. Watchapp host tests run only in `scripts/test-all.sh`, not in any CI workflow.

## Documentation Quality

Good. Requirement-ID discipline (REQ-* with acceptance criteria), a MANIFEST-protected spec set, KDoc on public Kotlin APIs, a rationale header on every C module citing REQ IDs and issues, an implementation status log per change, and human build guides.

## Quick Ratings

| Criterion | Rating | Evidence |
|---|---|---|
| Naming clarity | good | descriptive Kotlin and C names, REQ cross-references |
| Function size | fair | most functions are small; `render_navigation`, `panel_update_proc`, `strip_update_proc` and `DashboardScreen` are long |
| Error handling | good | best-effort network returns null, transport failures are caught and logged, persist errors are logged, settings tuples are type-checked; navigation tuple types are not validated in `render_navigation` |
| Test presence | good | pure logic is tested on both devices; Pebble UI is emulator-verified only |
| Duplication | fair | the settings table exists twice by hand (Kotlin and C); repeated SharedPreferences boilerplate; three near-identical switch tables in `maneuver_resource_id` |
| Dead code | fair | `ReducerEvent.FreshnessChecked` is never emitted; `transportStatus` is always null; datastore in `:app` is unused |

## Technical Debt

- `watchapp/src/c/main.c` (1,479 lines at 61787f5) mixes decoding, rendering, timers, input, shortcuts, outbox and glance. `settings_window.c` is 966 lines.
- `sync_persist_key` (`settings_sync.c:140-142`) is `id + 4` from `SET_TOUCH` on, so appended settings collide with persist keys 27 and 28 (next free 29, `theme.c:3-7`).
- The count 23 is pinned in `settings_sync.c` (`_Static_assert`, lines 62-65), `theme.c:11-15`, `test_settings_sync.c:114`, `WatchSettingsModelTest`, `ProtocolCodecTest.kt:277` and REQ-WATCH-025's text.
- `WatchSettingsModel.fromWatch` cannot distinguish a setting an older watchapp did not send from a default.
- The settings row order is kept twice by hand: `ROW_*` in `settings_window.c` (About first since #102) and its copy in `watchapp/tools/capture_screenshots.py`.
- No periodic tick reaches the reducer (`FreshnessChecked` unused), so the stale flag is only set on a resend.
- Navigation tuple type checks are missing in `render_navigation` and `handle_stopped`.
- `PebbleNtnDatabase` KDoc is stale (lists v1-v5; the schema is v8). `scripts/__pycache__/changelog.cpython-312.pyc` is committed against `.gitignore`. `rules/test-cases/` is documented but empty. `rules/README.md` keeps milestone history as status. (Not re-checked at 61787f5.)
- No coverage tooling, so the org-default 80% floor (non-express scopes) cannot be measured. Pebble SDK version unpinned in CI. A committed binary `.dex` helper sits in `tools/route-capture/device/`.
- An app's package list is copied into its catalog entry, its rules, the manifest `<queries>` and the fixtures (two checks hold the first three); hand-kept app lists and counts in the tools and tests (checked at c7b6ba0).
- The export schema version is hard-coded in a protected requirement (REQ-DEBUG-015 "The export schema version is 5."), so each bump edits it; `MANIFEST.sha256.json` has no resync script.

### Hide when driving findings (intent 261108-hide-when-driving, focused scan at 61787f5)

The full fact list with line numbers is the intent's developer scan handoff (`inception/reverse-engineering/developer-scan.md`); the points that shape the change:

1. **Two synced settings, end to end.** Append after `SETTING_SHORTCUT_TAP` (key 40) as keys 41 and 42 in `protocol/protocol-definition.json` and the protected `examples/protocol-definition.json` (plus its MANIFEST hash), and bump the minor (1.3 to 1.4) so the phone can offer the rows only to a watchapp that has them; run `generate.py`. Watch: two `SettingId`s, `kRules` and `kDefaults` rows (When driving: 2 values, default Show = 0; Driving speed: a value count of at most 31), an enum and `_Static_assert` in `theme.c`, getter and setter macros, the `_Static_assert` counts raised from 23, and `sync_persist_key` changed so the new ids get keys 29 and 30. Phone: two `WatchSetting` entries with `valueCount`/`defaultValue`, `WatchSettingsUi.title`/`valueLabels` branches, string arrays (a metric and an imperial speed list, like the early-warning distance arrays), a section in `sections`, and the tests that pin 23.
2. **Units.** The only units setting is the watch's `SETTING_UNITS` (the phone shows distances by `model.units == 1`). 10-60 km/h is 11 values, 5-40 mph 8, and the defaults differ (25 km/h, 15 mph), so the stored value needs a meaning that survives a units change: for example an index whose label and threshold follow the units, with the imperial list clamped, or two speed settings. To decide in requirements.
3. **The watch menu.** No main row is free of meaning; adding one moves `MAIN_ROW_COUNT` to 12 and changes the hand copy in `capture_screenshots.py`. A "When driving" door (mode, speed) fits the Backlight door shape; the speed label follows `early_warn_label`. Code is heap on aplite (REQ-WATCH-025's 500-byte budget).
4. **Where the decision lives.** The speed inputs are in `NotificationSnapshot` (`progress`, `progressMax`, `subText`, `postTimeMillis`) and reach only `DebugCaptureProcessor.onPosted`, which forwards the parsed instruction alone (`DebugCaptureProcessor.kt:28-38`, wired at `AppContainer.kt:244`). The instruction's distance is quantized in the reducer, so it is too coarse for speed. A pure `core/` detector (state in, samples and clock in, decision out, like the reducer) with JUnit tests fits the codebase; the controller would own it and the mirror's settings.
5. **Closing without relaunching.** `WatchTransport` has no close call; vendored PebbleKit 2 has `stopAppOnTheWatch` (unused). The alternative is a watch-side exit on a new message, as `handle_stopped` does with `window_stack_pop_all(true)` (`main.c:997-1014`), which needs the watchapp open to receive it. Either way, while hidden every `SendState` must not go through `send`, which relaunches on `FailedDifferentAppOpen` when auto-launch is on (`PebbleWatchTransport.kt:77-82`, `NavigationController.kt:242-252`): drop it, or use `sendToOpenApp`. A closed watchapp vibrates for nothing, so "stops vibrations" follows from closing; whether the Pebble mobile app also mirrors the navigation app's own notification to the watch (its own buzz) is outside PebbleNTN (hypothesis).
6. **Manual open.** The phone sees an open as `onAppOpened` (a synthetic READY on the inbound bus) and a real READY, with no flag saying who opened it. Only the controller knows it launched (`LaunchWatchApp` effect, autonomous READY 1.5 s later), so "opened while hidden and not launched by us within a few seconds" is the available test; it needs an explicit opened signal (not the made-up READY) and a per-session "kept open" flag cleared at session stop.
7. **Timing.** Nothing emits a periodic event; Google Maps reposts its card every few seconds while moving, but when the car stops it may post less often, so "below the threshold for about 2 minutes" needs a controller timer (coroutine `delay`, testable with `runTest` virtual time) or a decision taken on each notification with a fallback.
8. **Speed sources.** Progress gives metres along the route (per the issue); `subText` "Arrive h:mm" gives an ETA at minute resolution (localized wording; the rules already extract it as `secondaryText` for Google Maps); the instruction distance countdown resets at each maneuver. `DebugEventGrouping` ignores progress, so debug history keeps only the first and last snapshot of a run, which is not enough to replay speed; the export needs its own log.
9. **Launch policy and requirements.** REQ-WATCH-006 forbids relaunching "merely to bring it to the foreground ... after the user leaves it" and "on a timer"; re-showing after 2 minutes needs that requirement and `spec/200-architecture/Pebble.md`'s launch policy amended. REQ-WATCH-005 (auto-launch off) and REQ-WATCH-007 must say how they combine with hiding. New IDs: REQ-WATCH-026, REQ-ANDROID-024, REQ-DEBUG-016 (export schema 6 if a log section is added to `spec/300-data/ExportFormat.md`). REQ-WATCH-025's "23 user settings" and REQ-ANDROID-021's group list change.
10. **Diagnostics.** `ExportBuilder` has no non-notification section; a decision log (time, estimated speed, source, threshold, decision) fits as a new `ExportBundle` field fed by a new `DiagnosticExporter` lambda wired in `AppContainer`, in privacy-safe and full modes. Speed and times carry no notification text; whether they belong in rules-only exports is a privacy decision.
11. **"No navigation detected".** The watch shows `IDLE_TEXT` on NO_ACTIVE_NAVIGATION. When the phone relaunches the watchapp after a hide, the READY makes the reducer send the current `Navigating` state, so the idle text should not appear unless the session ended while hidden.

### Vela Maps findings (intent 261030-vela-maps, focused scan at c7b6ba0)

The full fact list is that intent's developer scan handoff; the points that shaped the change:

1. **Registration.** Catalog entry, `<queries>` line, `rules/bundled/vela-maps/en.json` with `packageNames: ["app.vela"]`, `rules/fixtures/vela-maps.json`, a `VelaMapsRulesRegressionTest`, the workbench regression tuple, the catalog lock table, `test_validate_catalog.py` and `SetupProgressTest`. No main-source Kotlin change.
2. **Google Play.** `playListed: false` without `rulesFrom` dropped the entry on Play at c7b6ba0; REQ-ANDROID-023 now keeps its own hidden switch there.
3. **The issue's rules validate** against Vela's source (`In %1$s · %2$s`, `Arrived`, `Navigating`; text `<duration> · <distance> · Arrive <time>`).
4. **Rule problems** found by reading: bare-instruction titles, Valhalla exit wording, fixed exit and U-turn directions, turn versus roundabout priority, missing ARRIVE and "Bear" forms, repeated extractors.
5. **ETA.** `secondaryText` is only the clock; no bundled rule sets `etaEpochSeconds`.
6. **Evidence.** Every fixture synthetic; English only.
7. **Route capture.** A `VelaMaps(App)` driver with a `ReleaseAsset` installer, an `APPS` entry, a plan and README rows; unverified until run on the Windows emulator.

### Install source findings (intent 261029-install-source, focused scan at 494a6dd)

1. **Detection kept an enum only**; a plain install record built once serves the row and the export.
2. **Known installers**: Play, F-Droid clients, Droid-ify, Neo Store, Obtainium.
3. **`manual` versus `unknown`** needed their own mapping beside the enum.
4. **The row was Play-only**; adding it for every source meant deciding the "Version X" summary.
5. **Row tests** in `DashboardUpdatesScreenTest`; a pure text helper covers the wording.
6. **Export: one metadata field**, schema 4 to 5, passed through both exporter paths.
7. **Specs and IDs**: `ExportFormat.md` section, REQ-DEBUG-015, REQ-ANDROID-020, MANIFEST hashes.
8. **Privacy**: install times are device-specific timestamps in privacy-safe exports.
9. **Platform facts from the Android docs** (hypothesis at the time).

### MicroG icon fallback findings (intent 261028-microg-icon-fallback, focused scan at 2fa2aa5; shipped in v0.0.79)

1. The retry belongs in `AppIconReferences.render`, same context, only `defPackage` changed. 2. `RuleFamilies` had no root; record it explicitly. 3. The root reaches `render` through the constructor or an argument. 4. No cache change needed. 5. Keyed on `rulesFrom` (or the Play merge) the retry stays MicroG-only. 6. Never open the root app's own context. 7. Testable with an injectable identifier lookup. 8. The renamed resource table was a hypothesis.

### MicroG Maps findings (intent 261026-microg-maps, focused scan at 1c7f4c1; #113 later shipped `rulesFrom` families instead)

At 1c7f4c1 no alias mechanism existed, so a variant had to be appended to the catalog entry, every rule, `<queries>`, the fixtures and the lock table, keeping the Google Maps package first (`packageNames.first()` drives the settings row, the Play link, the Rules grouping, the canonical sort and stored user rules). Enablement is per `appId`. Icon matching under the renamed package was unverified. Ruleset versions are not compared. The localized generator hard-codes one package. No MANIFEST-protected file needed to change.

### Version-compare update findings (intent 261022-version-compare-update, focused scan at 1ad9ec8)

Status at c50710e (v0.0.73): addressed by the merged work (the phone records the watch's key 11 from a genuine READY, each device derives its own effective update, strict parsing on both sides, per-source card). Finding 6 (signatures can differ between sources, so switching source can fail to install) was not re-checked.

### Watchapp version status row findings (intent 261022-watchapp-version-status, focused scan at c50710e)

Shipped in v0.0.74 as a pure `watchAppVersionText` and one `InfoRow`; "last seen" has no timestamp and the raw stored value is shown.

### Version compare as the default update signal (intent 261022-update-by-version-compar, focused scan at 8f08e02)

Version compare was already ungated; the #94 stale prompt and the GitHub-only toast made the online check look like the default; several texts framed GitHub as the only source; outdated "Core Devices does not forward watch messages" statements; no signal when no watchapp version ever arrived; two comparison rules by design.

### Play release findings (intent 261025-play-release, focused scan at 6dec020)

Status at 1c7f4c1: merged (#115). At 6dec020: target SDK 35 with a swallowed gate; low Android 16 exposure; Play installs counted as GitHub; a Play install saw every GitHub update control; stored GitHub state carried over; the offline notice is source-neutral; one HTTP call plus feedback links and email; privacy docs and the disclosure text did not mention network use; listing graphics needed a feature graphic and 2:1 screenshots; release key equalled upload key; coupled protected files; release triggers; reviewer access without a watch.

### Settings sync findings (intent 261025-settings-sync, focused scan at 4779760)

Shipped in v0.0.77 (#102, protocol 1.3, REQ-WATCH-025, REQ-ANDROID-021) and re-checked at 61787f5. The design the findings asked for is in place: one table per side, `settings_set` as the single watch edit path with `settings_store` for phone edits (no echo, writes only changed values), an outbox with sent/failed handlers and retry, SETTINGS on reconnect, real-READY detection by key 11, sends through `sendToOpenApp`, platform and capability reporting, `settings_window_refresh`, and the bundled previews. Still open from those findings: the phone's fixed `WatchSettings` flags (the watch now honours bit 1 only when its New instruction pattern is Short pulse, so dropping the flag would silence that choice), and the row order kept by hand in `capture_screenshots.py`.

### Touch gestures findings (intent 261023-touch-gestures, focused scan at 3727ddc)

Shipped in v0.0.76 as REQ-WATCH-024 (Shortcuts: UP/DOWN everywhere, Touch and five gestures on emery, `shortcuts.c`, `shortcut_apply.c`, a 1.5 s banner); since #102 the slots are synced settings and Touch defaults On.
