# Code Quality Assessment

## Test Coverage

| Area | Tests | Framework |
|---|---|---|
| Android app | 67 test sources under `android/app/src/test/` covering core, data (including the Room v1-v8 migration test), notification, icon, rules (per-app regression suites), export, protocol, pebble controller, system, ui view models, update | JUnit 4 + Robolectric + coroutines-test + Compose UI test |
| Android instrumented | `ManualOpenWithAutoLaunchOffTest` | AndroidJUnit4 |
| Watchapp | 8 host tests (about_text, early_warning, eta_text, glance_text, panel_layout, phone_version, stops_text, update_notice) | plain C, `-Werror` |
| Rules | fixtures through `workbench.py regression` and Kotlin `*RulesRegressionTest` | Python + JUnit |
| Tools | `tools/route-capture/tests/test_routecap.py` | unittest |

Coverage measurement: **none configured** (no JaCoCo or Kover, no gcov). Untested by design: `PebbleWatchTransport` (needs a bound companion) and the Pebble API parts of `main.c`, `settings_window.c` and `about_window.c`, which are verified in the emulator per the REQ acceptance criteria. `UpdateCheckRepository` is covered by Robolectric tests (throttle, force, failure retention, persistence, auto-check default off, and staleness since #94). `AppVersionsTest` covers newer, equal or lower, leading `v`, component counts, pre-release suffixes, and junk `latest`. `UpdatePromptTest`, `ProtocolCodecTest` (bit 4 / key 14) and `NavigationControllerTest` (`latestPhoneUpdate`) cover the #94 phone side. `test_update_notice.c` and `test_glance_text.c` cover the watch side.

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

- `watchapp/src/c/main.c` (1,248 lines) mixes decoding, rendering, timers and glance. `settings_window.c` is 741 lines.
- Tuple type checks are missing in `render_navigation` and `handle_stopped` (CodingStandards: "Validate every tuple and type").
- Phone-wide flags: resolved for the update bit by #94 (`encodeState` ORs bit 4 into every state message). Compatibility errors still carry no flags.
- `PebbleNtnDatabase` KDoc is stale (lists v1-v5; the schema is v8).
- `scripts/__pycache__/changelog.cpython-312.pyc` is committed against `.gitignore`.
- `rules/test-cases/` is documented but has no tracked content.
- `play-release.yml` swallows the target-SDK gate failure (`|| echo "TODO(M10) ..."`).
- No coverage tooling, so the org-default 80% floor (non-express scopes) cannot be measured.
- Pebble SDK version unpinned in CI (`pebble sdk install latest`).
- A committed binary `.dex` helper sits in `tools/route-capture/device/`.

### Version-compare update findings (intent 261022, focused scan at 1ad9ec8)

1. **The phone has no reliable source for the watchapp version.** The watch sends key 11 only in its real `WATCH_READY`, once from `init()` (main.c:997-1007, 1197). `ProtocolCodec.decodeInbound` drops it, and `ReducerEvent.WatchReady` has no version field. The Core Devices companion does not forward the real READY (NavigationController.kt:138-148). The synthetic READYs (`scheduleAutonomousReady`, `WatchListenerService.appOpenedReadyMessage`) have no key 11. PebbleKit 2 exposes firmware version only. The phone half of the comparison is therefore best-effort; the watch half is reliable, because the phone sends key 11 on every message.
2. **The watch's update record is overwritten by every message.** `update_record` clears `s_latest_phone_version` whenever bit 4 is clear (update_notice.c:110-117). A watch-side comparison result needs its own derived "effective latest" that feeds the notice, the idle text and the glance.
3. **`AppVersions` does not reject unparsable versions.** Non-numeric components become 0 and suffixes are dropped (AppVersions.kt:26-35), so "garbage" compares as 0. The intent requires ignoring unparsable versions, which needs a strict parse. The watch has no comparator, so it needs a new pure C unit with its own `test_<unit>.c`.
4. **There are two "update available" sources on the phone.** The GitHub `latestVersion` drives the card text, `latestPhoneUpdate` and persistence. A watch-reported version needs a defined precedence (for example, the higher of the parsable versions) so the card version and key 14 agree.
5. **There is no install-source detection.** The card has a single hard-coded GitHub URL and GitHub-specific body text (strings.xml:40-41, 58; MainActivity.kt:157). The installer-package mapping (F-Droid clients, Obtainium, unknown or browser to GitHub) is not in code.
6. **Signatures can differ between sources.** Whether F-Droid ships the developer signature depends on reproducible builds (fdroid/REPRODUCIBLE_BUILDS.md). Switching source can fail to install over the current app, so the existing "uninstall first" warning needs a per-source equivalent.
