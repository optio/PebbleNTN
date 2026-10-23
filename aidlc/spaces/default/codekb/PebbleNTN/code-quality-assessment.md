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

- `watchapp/src/c/main.c` (1,261 lines at c50710e) mixes decoding, rendering, timers and glance. `settings_window.c` is 741 lines.
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
