# Developer Code Scan: PebbleNTN (full rescan, issue #139)

- Date: 2026-11-20
- Commit: 073f51d446fee2d02ff2da9bab80e2d65ee85df3
- Method: read only; files found folder by folder through the tracked file list (which honours every `.gitignore`); no build, test or program was run.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - android/app/src/main/java/com/pebblentn/app/ui/watchsettings/ (all 4 files, line by line)
  - android/app/src/main/java/com/pebblentn/app/core/WatchSettingsModel.kt, core/WatchSettingsSync.kt
  - android/app/src/main/res/values/strings.xml (Watch settings block, lines 473-675, and the file layout)
  - android/app/src/test/java/com/pebblentn/app/ui/watchsettings/ (WatchSettingsScreenTest, WatchSettingsUiTest)
  - android/app/src/test/java/com/pebblentn/app/ui/StoreScreenshotsTest.kt
  - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt (watch-settings routes), pebble/NavigationController.kt (settings sync paths)
  - requirements/REQ-ANDROID.md (REQ-ANDROID-021, -024, -025), spec/400-ui/AndroidUI.md, spec/400-ui/WatchUI.md (settings sections)
  - android/app/build.gradle.kts, android/build.gradle.kts, android/settings.gradle.kts, android/gradle/libs.versions.toml, android/gradle.properties, gradle wrapper properties
  - protocol/protocol-definition.json (keys, events, flags, capabilities, platforms)
  - .github/workflows/ (all 6), .githooks/pre-commit, scripts/test-all.sh, AGENTS.md, MANIFEST.sha256.json (protected file list)
- **Skimmed only**:
  - android/app/src/main/java/com/pebblentn/app/ (every other package: file list, sizes, headers; key classes known from the 2026-11-10 scan and the #134 diff)
  - android/app/src/test/ (file list, test counts)
  - android/pebblekit/, android/fixture-publisher/
  - watchapp/src/c/ (file list, sizes; settings_window.c menu labels), watchapp/tests/, watchapp/tools/, watchapp/package.json
  - rules/ (bundled, catalog, fixtures: file list), schemas/, examples/
  - tools/rule-workbench/, tools/route-capture/, tools/play_store_graphics.py, tools/play-store-sources/
  - scripts/ (names, purposes), protocol/ scripts
  - spec/ (other files), requirements/ (other files), docs/ (IMPLEMENTATION_STATUS head, FOR-HUMAN, README)
  - fastlane/, fdroid/, screenshots/, watchapp/store/, .github/ISSUE_TEMPLATE/
- **Left out**:
  - android/.gradle/, android/build/, android/app/build/, android/pebblekit/build/, android/fixture-publisher/build/ (build output and caches)
  - android/local.properties (ignored)
  - watchapp/build/ (Pebble build output, ignored)
  - protocol/__pycache__/, scripts/__pycache__/ (bytecode; one tracked .pyc noted as debt)
  - tools/route-capture/out/ (ignored capture output)
  - captures-private/, secrets/ (ignored; private)
  - .idea/ and other IDE folders (ignored)
  - AI-DLC install and workspace: .claude/, aidlc/, aidlc.settings.json, AI-DLC block of .gitignore, .mcp.json servers

### Packages Found
- `:app` (com.pebblentn.app) — Android application — Kotlin — companion app: notification intake, rule engine, session reducer, watch link, persistence, Compose UI (122 main files, about 14,500 lines)
- `:pebblekit` (io.rebble.pebblekit2) — Android library — Kotlin + AIDL — vendored Rebble PebbleKit Android 2 client
- `:fixture-publisher` (com.pebblentn.fixturepublisher) — debug Android app — Kotlin — posts synthetic navigation notifications
- `watchapp/` — Pebble watchapp — C (SDK 3) — renders directions, on-watch settings, sync; 5 platforms (aplite, basalt, chalk, diorite, emery)
- `protocol/` — code generator — Python — AppMessage protocol definition 1.5 to `Protocol.kt` and `protocol.h`
- `rules/` — data — JSON — 22 bundled rule packs, navigation app catalog, 5 fixture sets
- `tools/rule-workbench/` — CLI — Python — rule validate/regression/generate
- `tools/route-capture/` — CLI — Python — emulator route capture harness
- `tools/play_store_graphics.py` — CLI — Python (Pillow) — store graphics from screenshots
- `scripts/` — shell + Python — build, test, validation, release helpers

### Build System
- **Type**: Gradle 8.13 (Kotlin DSL, version catalog) for Android; Pebble SDK `pebble build` (waf, `watchapp/wscript`) for the watch; Python 3.12 scripts
- **Config Files**: android/settings.gradle.kts, android/build.gradle.kts, android/app/build.gradle.kts, android/pebblekit/build.gradle.kts, android/fixture-publisher/build.gradle.kts, android/gradle/libs.versions.toml, android/gradle.properties, watchapp/package.json, watchapp/wscript, VERSION
- **Build Dependencies**: `:app` → `:pebblekit`; `:app` assets ← `rules/catalog` + `rules/bundled` (Gradle `syncRuleData`); `:app` test resources ← `rules/fixtures` (`syncTestFixtures`); `Protocol.kt` and `watchapp/src/c/generated/protocol.h` ← `protocol/protocol-definition.json` (`scripts/generate-protocol.sh`, `--check` in CI); version kept in sync across VERSION, build.gradle.kts, package.json, main.c (`scripts/version.sh`)
- **Android baseline**: minSdk 31, compileSdk 36, targetSdk 36, Java/JVM 21, versionCode 83 / versionName 0.0.83

### APIs Discovered
- Pebble AppMessage protocol 1.5 — protocol/protocol-definition.json, protocol/ProtocolCodec.kt, watchapp main.c / settings_sync.c — 45 keys, 7 events, 5 flag bits, 13 maneuvers, 2 error codes, 4 capability bits, 6 platforms
- Android NotificationListenerService — notification/NavigationNotificationListenerService.kt — 1 inbound callback path
- PebbleKit 2 sender / listener service — pebble/PebbleWatchTransport.kt, pebble/WatchListenerService.kt — send, launch, close, receive, connection callbacks
- Rule JSON format — schemas/ruleset.schema.json, rules/RulesetModel.kt — rules, conditions, extractors (incl. `arrivalClock`), outputs (incl. `compactText`)
- Navigation app catalog JSON — schemas/app-catalog.schema.json — 21 entries
- Diagnostics export JSON, schema 7 — export/ExportBuilder.kt, spec/300-data/ExportFormat.md
- GitHub Releases API (optional, opt-in) — update/ReleaseFetcher.kt — 1 GET
- In-app navigation routes (Compose Navigation) — ui/MainActivity.kt — about 16 routes incl. `watch-settings` and `watch-settings/{setting}`

### Frameworks & Libraries
- Kotlin — 2.3.10 — language, Compose compiler plugin, serialization plugin
- Android Gradle Plugin — 8.13.2 — build
- KSP — 2.3.10 — Room annotation processing
- Jetpack Compose BOM — 2024.12.01 (Material 3 from the BOM, about 1.3.x) — UI
- Navigation Compose — 2.8.5; Activity Compose — 1.9.3; Lifecycle — 2.8.7; Core KTX — 1.15.0
- Room — 2.7.2 — debug history, rules, state (schema v8)
- DataStore Preferences — 1.1.1 — settings, watch settings mirror
- kotlinx.serialization JSON — 1.7.3; kotlinx.coroutines — 1.9.0
- Timber — 5.0.1 — logging
- Rebble PebbleKit Android 2 — vendored source (deps kermit 2.0.4, androidx.annotation 1.9.1)
- JUnit 4.13.2, Robolectric 4.14.1, Compose ui-test-junit4, kotlinx-coroutines-test 1.9.0, Room testing, AndroidX test core 1.6.1 / ext-junit 1.2.1, Espresso 3.6.1 — tests
- Pebble SDK 3 (pebble-tool) — watch build
- Python 3.12, jsonschema, Pillow (optional), PyYAML (optional) — tools

### Test Coverage
- **Test Directories**: android/app/src/test/ (106 Kotlin files, 753 `@Test`), android/app/src/androidTest/ (1 instrumented test), watchapp/tests/ (13 host C programs), scripts/test_*.py, tools/test_play_store_graphics.py, tools/route-capture/tests/
- **Test Frameworks**: JUnit 4, Robolectric (JVM Compose UI tests), Compose UI test, coroutines-test, Python unittest, plain C asserts
- **Coverage Config**: absent (no JaCoCo/Kover; no coverage floor configured)
- **Watch settings tests**: WatchSettingsUiTest (12 pure tests: status, sections per capability, early warning rows, options, labels, driving helpers); WatchSettingsScreenTest (14 Robolectric tests, text-based `onNodeWithText` lookups incl. the exact Directions text and When driving explanations); WatchSettingsModelTest, WatchSettingsSyncTest, WatchSettingsQueueTest, WatchSettingsMirrorRepositoryTest, PreviewCatalogTest; StoreScreenshotsTest renders `2-watch-settings` only when `PEBBLENTN_STORE_SCREENSHOTS_DIR` is set

### Code Quality Indicators
- **Linting**: Android Lint via `./gradlew lint` (default rules; no lint.xml or `lint {}` block); `kotlin.code.style=official`; no ktlint/detekt; no C linter
- **CI/CD**: .github/workflows/android.yml (protocol check, test, lint, assembleDebug on android/** changes), watchapp.yml (protocol check, pebble build), rules.yml (spec asset hashes, rule schemas, workbench regression), codeql.yml (java-kotlin, weekly + PR), release.yml (on push to main: checks, bump version, changelog, signed APK + pbw, GitHub release, F-Droid changelog), play-release.yml (manual: signed AAB); .githooks/pre-commit (secrets, spec hashes, protocol, version sync, rules, syntax, Gradle test+lint)
- **Documentation**: README.md, AGENTS.md (implementation contract), spec/ (20 files), requirements/ (5 REQ files), docs/ (IMPLEMENTATION_STATUS 3,300 lines, privacy, Play, release checklist), KDoc on nearly every Kotlin class and function citing REQ ids and issue numbers; 34 spec files hash-protected by MANIFEST.sha256.json

### Technical Debt Signals
- No code-coverage tooling or floor (org default for some scopes asks 80%; express scope does not)
- English only: `res/values/strings.xml` is the only string resource set (675 lines); no translations, so Lint's MissingTranslation never applies
- God files: watchapp/src/c/main.c (1,479 lines), settings_window.c (1,120), DashboardScreen.kt (673), MainActivity.kt (586), NavigationController.kt (518)
- `scripts/__pycache__/changelog.cpython-312.pyc` is tracked although `.gitignore` ignores `__pycache__/`
- Compose BOM 2024.12.01 and several AndroidX pins are about a year old (still compatible with the baseline)
- Watch settings: titles and value labels are duplicated in English in the watch C code and the phone strings, with no shared source
- Watch settings UI tests depend on exact English text, so any copy change edits tests too
- 0 TODO/FIXME/HACK in source; 1 `@SuppressLint("DiscouragedApi")` (AppIconReferences, deliberate resource lookup by name)

## Handoff Summary
- **Intent-relevant finding**: The Watch settings screen has no per-setting help text today. `WatchSettingsScreen.kt` `SettingRow` (lines 146-160) renders each row as a Material 3 `ListItem` with `headlineContent` = setting title and `supportingContent` = the current value label, so the supporting-text slot is already used for the value. Explanations exist only at three levels: the screen intro (`watch_settings_explanation`), a per-section `note` under the card (`WatchSettingsSection.note`, used once: `watch_directions_text_explanation` for Directions, WatchSettingsUi.kt lines 89-93, drawn by `SettingsSection` lines 123-143), and the When driving section's own explanation (`watch_driving_explanation`, DrivingSection.kt lines 101-106). `OptionListScreen.kt` has a preview and a queued card but no description area. Titles come from `WatchSettingsUi.title()` (an exhaustive `when` over all 27 `WatchSetting` entries, WatchSettingsUi.kt lines 149-177); a parallel `@StringRes` mapping for help text would follow the same pattern and stay exhaustive.
- **Risks / follow-up**: (1) Rows shown depend on capabilities and values: Backlight colour needs RGB backlight; Touch and the five gestures need touch, and gestures only while Touch is on; early warning repeat/pattern/strength only when a distance is chosen; When driving only for protocol 1.4+; Directions text for every watchapp that sends settings. Help text must exist for every entry but tests should cover the hidden cases. (2) Strings are English only in `values/strings.xml`; REQ-ANDROID-021 and AGENTS.md rule 16/17 require every string as a resource and accessibility labels. (3) Changing REQ-ANDROID.md or spec/400-ui/AndroidUI.md requires updating MANIFEST.sha256.json (`scripts/validate-spec-assets.sh`, pre-commit and rules.yml). (4) `WatchSettingsScreenTest` asserts exact English text; the Directions note test will break if that string or its placement changes. (5) The store screenshot `tools/play-store-sources/phone/2-watch-settings.png` and the fastlane phone screenshots derived from it (`play_store_graphics.py --check` in test-all.sh) will be out of date if the screen changes visually; regenerating is opt-in. (6) Watch-side menu (settings_window.c) has no help text either; issue #139 is phone-only unless the requirements say otherwise.
