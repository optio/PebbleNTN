# Code Quality Assessment

## Test Coverage

| Area | Tests | Framework |
|---|---|---|
| Android app | 69 test sources under `android/app/src/test/` covering core, data (including the Room v1-v8 migration test), notification, icon, rules (per-app regression suites), export, protocol, pebble controller, system, ui view models, update | JUnit 4 + Robolectric + coroutines-test + Compose UI test |
| Android instrumented | `ManualOpenWithAutoLaunchOffTest` | AndroidJUnit4 |
| Watchapp | 9 host tests (about_text, early_warning, eta_text, glance_text, panel_layout, phone_version, stops_text, update_notice, version_compare) | plain C, `-Werror` |
| Rules | fixtures through `workbench.py regression` and Kotlin `*RulesRegressionTest` | Python + JUnit |
| Tools | `tools/route-capture/tests/test_routecap.py` | unittest |

Coverage measurement: **none configured** (no JaCoCo or Kover, no gcov). Untested by design: `PebbleWatchTransport` (needs a bound companion) and the Pebble API parts of `main.c`, `settings_window.c` and `about_window.c`, which are verified in the emulator per the REQ acceptance criteria. `UpdateCheckRepository` is covered by Robolectric tests (throttle, force, failure retention, persistence, auto-check default off, and staleness since #94). `AppVersionsTest` covers newer, equal or lower, leading `v`, component counts, pre-release suffixes, and junk `latest`. `UpdatePromptTest`, `ProtocolCodecTest` (bit 4 / key 14) and `NavigationControllerTest` (`latestPhoneUpdate`) cover the #94 phone side. `test_update_notice.c` and `test_glance_text.c` cover the watch side. Since v0.0.73: `UpdateCheckRepositoryTest` covers the stored watchapp version (first write, no rewrite of the same value, blank ignored, unparsable or older is no update, GitHub/watch precedence); `ProtocolCodecTest` covers `watchAppVersion`; `NavigationControllerTest` covers that a genuine READY reports the version and the synthetic READYs do not; `InstallSourceTest` and `UpdateCardTest` cover install-source mapping and the card model. Since v0.0.74: `WatchAppVersionRowTest` (3) covers the row text. No test covers the stale prompt or the manual-check outcome against a version-compare update. `DashboardScreen` itself has no test; its logic is tested through pure helpers beside it.

## Linting and Static Analysis

- Android Lint (`./gradlew lint`) in CI, pre-commit and release verify; default configuration with no baseline file.
- CodeQL java-kotlin on push, PR and weekly.
- No ktlint, detekt or formatter check. C warnings are enforced only in host tests.

## CI/CD

| Workflow | Trigger | Gates |
|---|---|---|
| android.yml | push/PR on `android/**` | protocol `--check`, `test`, `lint`, `assembleDebug`, APK artifact |
| watchapp.yml | push/PR on `watchapp/**` | protocol `--check`, `pebble build`, PBW artifact |
| rules.yml | push/PR on rules, schemas, examples | spec-asset integrity, schema validation, rule regression |
| codeql.yml | push/PR on main, weekly | CodeQL analysis |
| release.yml | push to main (not docs/tools/md) or manual | verify job (spec assets, protocol, rules, version sync, tests, lint), then version bump, changelog, signed or debug APK, PBW, SHA256SUMS, tag `v<version>`, GitHub release |
| play-release.yml | manual | signed AAB from a protected environment |

`.githooks/pre-commit` runs secrets/keystore scanning, MANIFEST integrity, protocol `--check`, version sync, rules, syntax checks and Android tests + lint. Watchapp host tests run only in `scripts/test-all.sh`, not in any CI workflow.

## Documentation Quality

Good. There is a requirement-ID discipline (REQ-* with acceptance criteria), a MANIFEST-protected spec set, KDoc on public Kotlin APIs, a rationale header on every C module, an implementation status log per change, and human build guides (`docs/FOR-HUMAN.md`).

## Quick Ratings

| Criterion | Rating | Evidence |
|---|---|---|
| Naming clarity | good | descriptive Kotlin and C names, REQ cross-references |
| Function size | fair | most functions are small; `panel_update_proc`, `strip_update_proc` and `DashboardScreen` are long |
| Error handling | good | best-effort network returns null, transport failures are caught and logged, persist errors are logged; watch tuple types are not validated in `render_navigation` |
| Test presence | good | pure logic is tested on both devices; Pebble UI is emulator-verified only |
| Duplication | fair | three near-identical switch tables in `maneuver_resource_id`; repeated SharedPreferences repository boilerplate |
| Dead code | good | none found; datastore in `:app` is the one unused declared dependency |

## Technical Debt

- `watchapp/src/c/main.c` (1,261 lines at c50710e and still at 3727ddc) mixes decoding, rendering, timers, input and glance. `settings_window.c` is 741 lines. #12 adds recognizers, gesture timers and a change notice to `main.c` and a Touch door to `settings_window.c`, unless they go into new files.
- The settings row order is kept twice by hand: the `ROW_*` defines in `settings_window.c` and their copy in `watchapp/tools/capture_screenshots.py`.
- Tuple type checks are missing in `render_navigation` and `handle_stopped` (CodingStandards: "Validate every tuple and type").
- Phone-wide flags: resolved for the update bit by #94 (`encodeState` ORs bit 4 into every state message). Compatibility errors still carry no flags.
- `PebbleNtnDatabase` KDoc is stale (lists v1-v5; the schema is v8).
- `scripts/__pycache__/changelog.cpython-312.pyc` is committed against `.gitignore`.
- `rules/test-cases/` is documented but has no tracked content.
- `play-release.yml` swallows the target-SDK gate failure (`|| echo "TODO(M10) ..."`).
- No coverage tooling, so the org-default 80% floor (non-express scopes) cannot be measured.
- Pebble SDK version unpinned in CI (`pebble sdk install latest`).
- A committed binary `.dex` helper sits in `tools/route-capture/device/`.

### Version-compare update findings (intent 261022-version-compare-update, focused scan at 1ad9ec8)

Status at c50710e (v0.0.73): findings 1-5 were addressed by the merged work. The phone records the watch's key 11 from a genuine READY (finding 1; its claim that the Core Devices companion does not forward the real READY was disproved on a real phone with v0.0.74), each device derives its own effective update (2, 4), `AppVersions.parseStrict` / `isNewerStrict` and the watch `version_compare.c` reject unparsable versions (3), and `InstallSources` drives a per-source card (5). Finding 6 was not re-checked. The original findings are kept below for history.

1. **The phone has no reliable source for the watchapp version.** The watch sends key 11 only in its real `WATCH_READY`, once from `init()` (main.c:997-1007, 1197). `ProtocolCodec.decodeInbound` drops it, and `ReducerEvent.WatchReady` has no version field. The Core Devices companion does not forward the real READY (NavigationController.kt:138-148). The synthetic READYs (`scheduleAutonomousReady`, `WatchListenerService.appOpenedReadyMessage`) have no key 11. PebbleKit 2 exposes firmware version only. The phone half of the comparison is therefore best-effort; the watch half is reliable, because the phone sends key 11 on every message.
2. **The watch's update record is overwritten by every message.** `update_record` clears `s_latest_phone_version` whenever bit 4 is clear (update_notice.c:110-117). A watch-side comparison result needs its own derived "effective latest" that feeds the notice, the idle text and the glance.
3. **`AppVersions` does not reject unparsable versions.** Non-numeric components become 0 and suffixes are dropped (AppVersions.kt:26-35), so "garbage" compares as 0. The intent requires ignoring unparsable versions, which needs a strict parse. The watch has no comparator, so it needs a new pure C unit with its own `test_<unit>.c`.
4. **There are two "update available" sources on the phone.** The GitHub `latestVersion` drives the card text, `latestPhoneUpdate` and persistence. A watch-reported version needs a defined precedence (for example, the higher of the parsable versions) so the card version and key 14 agree.
5. **There is no install-source detection.** The card has a single hard-coded GitHub URL and GitHub-specific body text (strings.xml:40-41, 58; MainActivity.kt:157). The installer-package mapping (F-Droid clients, Obtainium, unknown or browser to GitHub) is not in code.
6. **Signatures can differ between sources.** Whether F-Droid ships the developer signature depends on reproducible builds (fdroid/REPRODUCIBLE_BUILDS.md). Switching source can fail to install over the current app, so the existing "uninstall first" warning needs a per-source equivalent.

### Watchapp version status row findings (intent 261022-watchapp-version-status, focused scan at c50710e)

1. **The data already reaches the dashboard host.** `MainActivity.AppNavHost` collects `updateCheckRepository.state` as `updateState` (MainActivity.kt:121) and passes `updateAvailable` and `effectiveLatest` (lines 155-157), but not `watchAppVersion`. One new defaulted `DashboardScreen` parameter and one `InfoRow` in the Status section (DashboardScreen.kt:218-255) cover it, using the `?.let { ... } ?: stringResource(..._none)` fallback of the "Last navigation notification" row.
2. **"Last seen" has no time.** `watch_app_version` is a bare string with no timestamp, and it is never cleared (UpdateCheckRepository.kt:97-106). The row can prove that forwarding worked at least once, not that it works now. The label must say "last seen".
3. **Raw value, not the strict parse.** `ProtocolCodec.watchAppVersion` and `recordWatchAppVersion` only trim and drop blanks, so a debug build's `0.0.72-debug` is stored. The diagnostic row should show it as stored rather than filter it with `parseStrict`, which only governs "is newer".
4. **No dashboard UI test exists.** Tests for dashboard decisions are pure helpers (`UpdateCardTest`, `UpdatePromptTest`, `SetupProgressTest`). A row test needs either a small pure formatter or the Robolectric Compose pattern of `NavigationAppsScreenTest` (activity registration rule chain, `@Config(qualifiers = "w411dp-h891dp")`).
5. **Strings are split.** `dashboard_watch*` live in the "Leftovers of #28, part 2" group (strings.xml:362-367), the other Status rows in the "#28" group (lines 219-228). English only.
6. **`DashboardScreen` keeps growing** (568 lines, about 40 parameters).

Status at 8f08e02 (v0.0.74): shipped as a pure `watchAppVersionText` (`WatchAppVersionRow.kt`, `WatchAppVersionRowTest`) and one `InfoRow`; the row then showed on a real phone that the Core Devices app forwards `WATCH_READY`.

### Version compare as the default update signal (intent 261022-update-by-version-compar, focused scan at 8f08e02)

1. **Version compare is already ungated on both devices.** Phone: `UpdateState.updateAvailable = githubNewer || watchNewer` and `effectiveLatest` (UpdateCheckRepository.kt:34-61) drive the card (DashboardScreen.kt:192) and bit 4/key 14 (AppContainer.kt:168) with no online check ever run. Watch: `effective_update()` (main.c:973) compares `APP_VERSION_STR` with the phone's key 11 independently. Key 14 then echoes the watch's own version back (harmless); key 14/bit 4 remain the only way the watch learns of a GitHub-only update.
2. **The #94 stale prompt makes the online check look like the default.** `isCheckStale` is true when `last_check_millis` is 0 or over 30 days old (UpdateCheckRepository.kt:134-137), so every fresh install gets "Check for updates — has never checked for a newer version ... check GitHub now" first on the dashboard (above the setup checklist and the update card), whatever version compare found; a check from it leads to the weekly-check question when weekly checks are off (UpdatePrompt.kt:23-27, MainActivity.kt:186-203). Pinned by `UpdatePromptTest` (4) and the staleness cases of `UpdateCheckRepositoryTest` (5); specified by REQ-ANDROID-018.
3. **The manual-check toast ignores version compare.** `checkForUpdate` returns `UP_TO_DATE` from the GitHub tag alone (UpdateCheckRepository.kt:164-168), so "PebbleNTN is up to date." can show while the card offers a version-compare update.
4. **Texts frame GitHub as the only source**: `onboarding_disclosure`, `dashboard_stale_check_*`, `dashboard_auto_update_hint`, the INTERNET comment in AndroidManifest.xml, and the `UpdateCheckRepository` / `AppContainer` KDoc.
5. **Outdated "Core Devices does not forward watch messages" statements**: `NavigationController.scheduleAutonomousReady` KDoc, `WatchListenerService.onAppOpened` KDoc, `UpdateCheckRepository.recordWatchAppVersion` KDoc, `NavigationControllerTest` comments (lines 50, 282); outside the analyzed code, REQ-ANDROID-019 ("best effort ..."), `spec/200-architecture/Pebble.md` "Companion limitation and synthesized readiness", `docs/IMPLEMENTATION_STATUS.md:97`. The implementation status log (line 2230) records the missing `companionApp` declaration as the cause of the original routing failure; it is now declared. The synthetic READY still serves auto-launch off and the settle delay, so only its rationale text is wrong.
6. **No signal when no watchapp version ever arrived.** If the phone never received a real READY (watchapp not installed, older companion), version compare has nothing to compare on the phone; only the optional online check (or the watch's own notice) can reveal an update. `watch_app_version` is never cleared, but `watchNewer` compares against the running phone version, so updating the phone clears the card.
7. **Two comparison rules** stay by design: tolerant `isNewer` for GitHub tags, strict `isNewerStrict` / `version_newer` for versions exchanged between the apps.

### Touch gestures findings (intent 261023-touch-gestures, focused scan at 3727ddc)

Issue #12 was opened on 2026-08-09 (at 50c4c3f the menu had 9 rows, with Vibration strength as its own row and no ETA label or About, and keys 1-10 were in use) and last edited on 2026-10-23. Findings against the code at 3727ddc (v0.0.75):

1. **Settings touch browsing has shipped (#53, af8fb66, REQ-WATCH-021).** `init()` calls `app_touch_navigation_enable(true)` and disables the bridge on the navigation window (main.c:1176-1196, emery only); every other window keeps it. Both preconditions the issue found for recognizers are in place. What #12 still needs is recognizers on the navigation window plus the actions. The issue's "Settings browsing by touch" section describes shipped behaviour.
2. **The swipe direction for BACK is contradictory.** The issue says the firmware maps a swipe left to BACK and a swipe right to SELECT. The main.c:1193 comment, REQ-WATCH-021's acceptance and `touch_emulator.py check` (steps 07-08) all use a swipe right as BACK. This matters for the defaults (swipe left and right both "Switch ETA / IN") and for what a gesture means in the menus. Check it in the emulator.
3. **Nothing checks the system touch setting.** `touch_service_is_enabled()` is never called; hiding the Touch submenu would be its first use. REQ-WATCH-021 says the menus have no touch switch of their own, so a watchapp "Touch" on/off must be described as governing the navigation screen only.
4. **Menu layout and screenshot indices.** The main menu has 10 rows that are the same on every platform, and About must stay last (REQ-WATCH-020). A Touch row shown only on emery, or only while system touch is on, makes the row indices depend on the platform or the runtime. `capture_screenshots.py` keeps one fixed `ROW_*` table for basalt, chalk and emery. Putting Touch just before About keeps rows 0-8 the same everywhere; only `ROW_ABOUT` / `LAST_MAIN_ROW` would then differ on emery.
5. **Persist keys.** Keys 1-15 belong to theme.c and 16-18 to main.c; the next free key is 19. Touch on/off plus 7 gestures, at one key each, takes 19-26. A vibration mute that "restores the previous pattern" across relaunches needs one more key, or it lives in memory only.
6. **Settings missing from the action list, or changed since.** The ETA label style (#13, key 11) and the four early-warning settings (keys 12-15) are new since the issue was opened. Backlight colour (key 10) and vibration strength (key 9) already existed then. The early-warning distances are Off, 10, 25, 50, 100 and 200 m (30-650 ft in imperial), with repeat, pattern and strength under them. The story "toggle the RGB background colour" matches no setting: the screen background is only black or white (Invert, road area) plus the accent panel. "RGB" fits the backlight colour (`PBL_RGB_BACKLIGHT`, emery), which the action list leaves out ("Backlight" there means `light_enable_interaction`).
7. **"Vibration off" is not one switch.** Pattern Off does not silence the watch: the phone's maneuver-change pulse (flag bit 1) still plays (main.c:896-904), and the early warning has its own Off distance. A mute action must say whether it covers the phone pulse and the early warning, and where it keeps the previous pattern.
8. **ETA naming collides.** The ETA display values are "Arrival time" and "Time to arrival". With the default labelled style the strip reads "ETA at" / "ETA in", and "ETA / IN" is now the menu label of the compact *ETA label* style. So the action name "Switch ETA / IN" and the notice "ETA → IN" read like the label setting. Whether the Pebble system fonts draw "→" (U+2192) was not checked.
9. **There is no short notice mechanism.** The message layer is full screen and hides the navigation layers (`set_message_mode`), and the update notice is its own full-screen window with a 10 s timer. A 1.5 s "what changed" notice needs a new overlay layer and timer that coexist with the Connecting, No navigation and QR states and with the update notice window on top.
10. **Accent cycling on black-and-white.** `settings_set_accent` accepts all 16 ids everywhere. Only the menu limits black-and-white to Black and White (`accent_for_row`, `row_for_accent`, `ACCENT_AVAILABLE_COUNT`), so a cycling action must go through those helpers.
11. **The apply path is ready.** Calling a setter and then `on_settings_changed()` gives the same result as the menu. The glyph cache flushes itself on a pack change; units, ETA mode and label, arrow side and colours are read when drawing; the early-warning labels follow the units.
12. **The pure-unit rule limits `touch_actions.c`.** It is linked alone and cannot include `theme.h` (which pulls in `pebble.h`), so it needs its own action ids and takes counts such as `ACCENT_AVAILABLE_COUNT` as parameters. The double-tap (300 ms) and long-press (500 ms, 10 px) decisions can be pure functions of timestamps and positions, with `AppTimer`s left in `main.c`. Host tests run only in `scripts/test-all.sh`, not in CI.
13. **The emulator tooling already exists.** `touch_emulator.py` has `tap`/`swipe` over VNC with no third-party dependency, so the issue's "add a small script" and `vncdotool` suggestion are already covered. Its `check` flow never wipes, while `capture_screenshots.py` wipes per platform, which turns emulator touch navigation off. Touch screenshots therefore need the `touch_emulator.py` flow, not the capture script as it stands.
14. **Platforms.** `targetPlatforms` lists aplite, basalt, chalk, diorite and emery; flint and gabbro appear nowhere. The touch guards test `PBL_PLATFORM_EMERY`, so adding flint later means widening them.
15. **Requirement numbering.** REQ-WATCH-001 to 023 exist (012 is listed after 013), and the newest is REQ-WATCH-023, so the next free ID is **REQ-WATCH-024**. REQ-WATCH-021 says the navigation screen opts out of the bridge and defers its gestures to #12, so the new requirement must cross-reference REQ-WATCH-021 (and may amend it).
16. **Buttons.** The navigation window leaves UP, DOWN and long presses unused and BACK at the system default (exit). With the bridge disabled, no touch can trigger BACK there.
