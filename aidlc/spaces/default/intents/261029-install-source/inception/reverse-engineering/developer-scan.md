# Developer Code Scan: install source (#119)

Focused scan at 494a6dd (v0.0.79) for intent `261029-install-source`: show where the phone app was installed or last updated from, on the dashboard "Phone app version" row for every source and in every debug export.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - android/app/src/main/java/com/pebblentn/app/update/
  - android/app/src/main/java/com/pebblentn/app/export/
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/
  - android/app/src/test/java/com/pebblentn/app/update/
  - android/app/src/test/java/com/pebblentn/app/export/
  - android/app/src/test/java/com/pebblentn/app/ui/dashboard/
  - requirements/REQ-ANDROID.md
  - requirements/REQ-DEBUG.md
  - spec/300-data/ExportFormat.md
  - android/app/src/main/res/values/strings.xml (the update, dashboard and export string groups; the rest by grep)
- **Skimmed only**:
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt (lines 55-135, 165-180: `installSource`, `updatePolicy`, `updateCheckRepository`, `diagnosticExporter`)
  - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt (the `DashboardScreen(...)` call, `openUpdateSource`)
  - android/app/src/main/java/com/pebblentn/app/data/EventVersions.kt (the `getPackageInfo` pattern)
  - android/app/src/main/AndroidManifest.xml (`<queries>`, Obtainium entries)
  - android/app/build.gradle.kts (SDK levels)
  - MANIFEST.sha256.json, scripts/validate-spec-assets.sh, .githooks/pre-commit (protected-file check)
  - spec/400-ui/AndroidUI.md (Updates section wording, by grep)
  - docs/IMPLEMENTATION_STATUS.md (the #114 entry, by grep)
- **Left out**:
  - android/app/build/, android/.gradle/ and other build output
  - .claude/, .kiro/, .codex/, .cursor/, .opencode/, .aidlc/ and the aidlc/ workspace (except the codekb store read for the merge)

### Packages Found
- `com.pebblentn.app.update` — Service + Adapter — Kotlin — install source (`InstallSource`, `InstallSources`), per-source `UpdatePolicy`, `UpdateCheckRepository`/`UpdateState`, `AppVersions`, `UpdateCheckMessage`, `ReleaseFetcher`, and the disabled remote-rules path (`RuleUpdateRepository`, `SignedRuleset`, `RulesetSignatureVerifier`)
- `com.pebblentn.app.export` — Service — Kotlin — `ExportBuilder` (metadata, modes, schema version), `DiagnosticExporter` (gathers data, size cap), `Redactor`, `DiagnosticShareManager`
- `com.pebblentn.app.ui.dashboard` — View + pure helpers — Kotlin/Compose — `DashboardScreen` (633 lines), `UpdateCard.kt`, `UpdatePrompt.kt`, `WatchAppVersionRow.kt`, `SetupChecklist.kt`, `FeedbackCard.kt`

### Build System
- **Type**: Gradle 8.13 (Kotlin DSL), AGP 8.13.2
- **Config Files**: android/app/build.gradle.kts (`compileSdk 36`, `minSdk 31`, `targetSdk 36` at 494a6dd), android/gradle/libs.versions.toml
- **Build Dependencies**: `ui.dashboard` -> `update`; `di` -> `update`, `export`; `export` -> `data`, `notification`, `rules`

### APIs Discovered
- Android platform — `update/InstallSource.kt:52` — one call: `PackageManager.getInstallSourceInfo(packageName).installingPackageName`; nothing reads `initiatingPackageName`, `originatingPackageName`, `PackageInfo.firstInstallTime` or `lastUpdateTime` anywhere in app code
- Export JSON — `export/ExportBuilder.kt` — `ExportBundle { metadata, userRules, events }`, schema 4
- Compose — `DashboardScreen(...)` — about 45 parameters, all but two defaulted

### Frameworks & Libraries
- kotlinx-serialization-json — 1.7.3 — export JSON (`encodeDefaults = true`, pretty print)
- Jetpack Compose BOM — 2024.12.01 — dashboard
- Timber — 5.0.1 — the logged detection failure
- Robolectric — 4.14.1 (runs at `sdk=34`) — repository and Compose tests

### Test Coverage
- **Test Directories**: android/app/src/test/java/com/pebblentn/app/update/ (7 files, 55 tests), export/ (3 files, 23 tests), ui/dashboard/ (5 files, 22 tests)
- **Test Frameworks**: JUnit 4, Robolectric, Compose UI test, coroutines-test
- **Coverage Config**: absent

### Code Quality Indicators
- **Linting**: Android Lint, default rules (CI, pre-commit)
- **CI/CD**: .github/workflows/android.yml (test, lint); release.yml verify job; pre-commit checks MANIFEST hashes
- **Documentation**: KDoc on every class and pure helper, citing REQ IDs; strings carry REQ comments

### Technical Debt Signals
- `InstallSource.GITHUB` means four things: no installer, the package installer or a browser (a manual install), another store (Aurora Store, Amazon, Accrescent and so on), and a failed lookup (`InstallSource.kt:38-45, 51-57`). The card needs only the destination, but the new GUI and export need "manual" and "unknown" apart.
- `InstallSources.detect` returns only the enum and drops the installer package name (`InstallSource.kt:51-54`), so nothing else can report it.
- The strings comment says the "Phone app version" pair is for Google Play only (`strings.xml:55-57`), and the row has no pure helper or unit test, unlike `watchAppVersionText`.
- `spec/300-data/ExportFormat.md:33` still says `schemaVersion` (2) under "Versions (schema 2+)", and REQ-DEBUG-014 hard-codes "The export schema version is 4." (`REQ-DEBUG.md:45`), so every schema bump edits a requirement.
- `DashboardScreen.kt` is 633 lines with about 45 parameters (the codekb already records this).
- There is no MANIFEST resync script: `scripts/validate-spec-assets.sh` only checks, so resync means `sha256sum` and a hand edit of `MANIFEST.sha256.json`.

### Intent findings (file:line at 494a6dd)

1. **Detection today.** `InstallSources.fromInstaller` (`InstallSource.kt:38-45`) maps `com.android.vending` to PLAY, `org.fdroid.fdroid`, `org.fdroid.basic`, `com.looker.droidify` (Droid-ify) and `com.machiav3lli.fdroid` (Neo Store) to FDROID (`:17-22`), `dev.imranr.obtainium` and any `dev.imranr.obtainium.*` (for example `.fdroid`, both in `OBTAINIUM_PACKAGES` `:31` and the manifest `<queries>`) to OBTAINIUM, and everything else, null included, to GITHUB. `detect` (`:51-57`) catches any exception, logs it and returns GITHUB. Pinned by `InstallSourceTest` (5 tests, including look-alikes and `""`). Not mapped: the F-Droid privileged extension (`org.fdroid.fdroid.privileged`), Aurora Store (`com.aurora.store`) and other F-Droid-repo clients; what the privileged extension reports as installer is unverified.
2. **One detection per start.** `AppContainer.installSource` (`AppContainer.kt:63`) is read by the catalog view (`:76`), `updatePolicy` (`:120`), `UpdateCheckRepository` (`:127-131`), `MainActivity` (the dashboard and onboarding). The installer package, the other `InstallSourceInfo` fields and the install times would need a richer detection result (a plain data class built from `InstallSourceInfo` and `PackageInfo`), computed once like the enum. `getPackageInfo(context.packageName, 0)` for the app itself needs no `<queries>` entry; `SystemEventVersionsProvider` (`EventVersions.kt:40`) shows the `runCatching` pattern.
3. **UpdatePolicy** (`UpdatePolicy.kt:12-44`) is a pure per-source table (GitHub check allowed, stored GitHub version ignored, other sources offered, delay note). PLAY is store-only; FDROID adds the delay note; GITHUB and OBTAINIUM are the same. `UpdatePolicyTest` (3). Nothing in it concerns the version row; the row is gated on `allowsGithubCheck` only.
4. **The "Phone app version" row.** `DashboardScreen.kt:362-393`, Updates section: when `!githubCheckAllowed` (Play only) an `InfoRow(dashboard_app_version, dashboard_app_version_play(appVersion))` (`:365-368`) shows "0.0.79, updated by Google Play". Every other source gets the "Check for updates now" `NavRow` with summary `dashboard_version` "Version %1$s" (`:371-379`, `strings.xml:34`), which is hidden while the stale prompt is shown (`:371`), and the weekly toggle. So a stale GitHub, F-Droid or Obtainium install shows no version anywhere in Updates. Strings: `dashboard_app_version` "Phone app version" and `dashboard_app_version_play` "%1$s, updated by Google Play" (`strings.xml:56-57`), `dashboard_update_source_*` names (`:51-54`). `DashboardScreen` takes `installSource` and `appVersion` (default `BuildConfig.VERSION_NAME`) but no install times; `MainActivity` passes `container.installSource` and no `appVersion`.
5. **Tests on the row.** `DashboardUpdatesScreenTest` (Robolectric Compose, 6 tests): `playShowsNoGitHubControlsEvenWhenAskedForTheStaleItem` asserts "0.0.77, updated by Google Play" (`:72`); `gitHubKeepsTheCheckRowTheToggleAndTheStaleItem` asserts that text is absent on GitHub (`:87`). Both change once every source shows a row. The text logic has no pure test; `watchAppVersionText` + `WatchAppVersionRowTest` is the pattern for one.
6. **Export metadata.** `ExportMetadata` (`ExportBuilder.kt:23-50`): `schemaVersion` (`SCHEMA_VERSION = 4`, `:48`, with a KDoc history of 2-4 at `:25-28`), `mode`, `exportedAt`, `appVersion`, `appVersionCode`, `androidRelease`, `locale`, `userAddedPackages`. `ExportBuilder.build` (`:123-151`) builds the same metadata for all three modes, so a new metadata field is in rules-only too with no mode logic. The `Json` has `encodeDefaults = true` (`:121`): null fields are written as `null` unless marked `@EncodeDefault(NEVER)` (used for `customPackage`, `:89`).
7. **Exporter wiring.** `DiagnosticExporter` (`DiagnosticExporter.kt:28-39`) takes its export-time inputs as constructor values or lambdas (`userAddedPackages: () -> Map`, `:38`) and passes them to `builder.build` in both `build` (`:40-54`) and `buildCapped`'s `render` (`:73-83`); a new input must go through both. `AppContainer.kt:169-176` constructs it.
8. **Export tests.** `ExportBuilderTest.eachEventCarriesTheVersionsItWasRecordedWith` asserts `schemaVersion` 4 (`ExportBuilderTest.kt:146`) and fails on a bump; `theHeaderListsTheUserAddedPackagesPerAppInEveryMode` (`:108-116`) is the every-mode pattern; `DiagnosticExporterTest.bothExportsCarryTheUserAddedPackagesAtExportTime` (`:121-156`) checks `build` and `buildCapped` together.
9. **Spec and requirements.** `spec/300-data/ExportFormat.md` adds one section per schema ("Versions (schema 2+)", "Grouped occurrences (schema 3)", "User-added packages (schema 4)"); schema 5 follows as a new section. REQ-DEBUG-014 ends "The export schema version is 4." (`REQ-DEBUG.md:45`); the newest is REQ-DEBUG-014, so the next free ID is REQ-DEBUG-015. REQ-ANDROID-020 (`REQ-ANDROID.md:86-91`) covers detection and the card, not the row; the Play row text is only in code and `docs/IMPLEMENTATION_STATUS.md:173`. The newest Android ID is REQ-ANDROID-023. `spec/400-ui/AndroidUI.md:31` lists the Updates controls without the row (optional amendment, also protected).
10. **MANIFEST.** `REQ-ANDROID.md` (`MANIFEST.sha256.json:8`), `REQ-DEBUG.md` (`:9`), `ExportFormat.md` (`:22`) and `AndroidUI.md` (`:24`) are protected. `scripts/validate-spec-assets.sh` compares `sha256sum` with the JSON (via `jq`); the pre-commit hook fails on a mismatch. Resync is a hand edit of the hash in the same commit.
11. **Platform facts (from Android docs, not verified on a device).** `InstallSourceInfo` (API 30, minSdk 31) has `getInstallingPackageName`, `getInitiatingPackageName`, `getOriginatingPackageName` (null unless the caller holds `INSTALL_PACKAGES`, so likely always null for this app), plus `getUpdateOwnerPackageName` (API 34) and `getPackageSource` (API 33). The installing package is the installer of the latest install or update, which is what "updated by" needs. `PackageInfo.firstInstallTime` and `lastUpdateTime` are equal until the first update installed over the app.

### Handoff Summary
- **Intent-relevant finding**: The row exists only on Play (`DashboardScreen.kt:362-368`, strings `dashboard_app_version*` at `strings.xml:56-57`), and detection keeps only an enum from `installingPackageName` (`InstallSource.kt:51-54`, read once at `AppContainer.kt:63`). Both the GUI and the export need a richer, once-per-start install record (installer, initiating, originating, first install and last update times) beside the enum. The export side is one metadata field in `ExportMetadata` (`ExportBuilder.kt:23-50`, schema 4 to 5) passed through `DiagnosticExporter`'s two build paths.
- **Risks / follow-up**: (a) GITHUB covers manual installs, other stores and failed lookups, so "installed manually" would be wrong for Aurora Store and similar; the export's `manual` versus `unknown` needs a rule (for example, null or a known system installer is manual, another package is unknown, a failed lookup is unknown). (b) The wording for a manual update ("updated manually"?) and whether the row also covers unknown is not stated in the intent. (c) On non-Play installs the row has to coexist with "Check for updates now" ("Version X") and disappear-while-stale rules (REQ-ANDROID-018). (d) `ExportBuilderTest.kt:146` and `DashboardUpdatesScreenTest.kt:72, 87` must change. (e) REQ-DEBUG-014's "schema version is 4" sentence and `ExportFormat.md:33`'s "(2)" need amending; four protected files may change, each with its MANIFEST hash. (f) Install times are device-specific timestamps in privacy-safe exports; the intent asks for them in every mode, so the privacy docs (`docs/PRIVACY_POLICY.md`, `docs/PLAY_DATA_SAFETY.md`, not scanned) may need a line. (g) Robolectric's `ShadowPackageManager` support for `InstallSourceInfo` was not checked; a pure mapper over a plain data class keeps the tests JVM-only.
