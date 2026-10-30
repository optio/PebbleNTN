# Code Summary — install/update source (#119) and a second MicroG Maps package (#113)

Express scope, zero Units, test-after (Testing Contract `sha256:bb76c1b2…`). Nothing is committed;
`versionCode` / `versionName` are unchanged (the release workflow makes v0.0.80 on merge). No watch,
protocol or rule file changed.

## Files

**Android app**
- `update/InstallRecord.kt` (new, FR1, NFR1):
  - `enum class InstallKind { PLAY, FDROID, OBTAINIUM, MANUAL, OTHER, UNKNOWN }`;
  - `data class InstallRecord(kind, installingPackage, initiatingPackage, originatingPackage,
    firstInstallTime, lastUpdateTime)` with `updatedSinceInstall` (null when either time is null) and
    `InstallRecord.UNKNOWN`;
  - `InstallRecords.classify(pkg)`: null, blank, `com.google.android.packageinstaller`,
    `com.android.packageinstaller` and `com.android.shell` are MANUAL; otherwise Play, F-Droid and
    Obtainium come from the existing `InstallSources.fromInstaller` lists; anything else is OTHER;
  - `from(...)` (pure), `detectWith(lookup)` (a thrown lookup becomes UNKNOWN, logged with Timber) and
    `detect(context)` (`getInstallSourceInfo` + `getPackageInfo(packageName, 0)`; no network, no
    permission, no `<queries>` entry).
- `di/AppContainer.kt`: `installRecord` detected once per start, next to the unchanged
  `installSource` (NFR2); passed to `DiagnosticExporter`.
- `ui/dashboard/PhoneAppVersionRow.kt` (new, FR2): pure `phoneAppVersionText(version, record)` returning
  `PhoneAppVersionText(version, format, sourceName, sourcePackage)`: "installed by" / "updated by" with
  a `dashboard_update_source_*` name or the OTHER installer's raw package; "installed manually" /
  "updated manually"; a null format (bare version) for UNKNOWN or an OTHER record without a package.
- `ui/dashboard/DashboardScreen.kt` (FR2): new `installRecord` parameter (default
  `InstallRecord.UNKNOWN`); the Updates section starts with the "Phone app version" `InfoRow` for every
  source, the stale state included; on non-Play installs the check row (still hidden while stale) shows
  `dashboard_check_update_hint`; Play still has no GitHub controls (REQ-ANDROID-022).
- `ui/MainActivity.kt`: passes `container.installRecord`.
- `res/values/strings.xml`: `dashboard_app_version_installed_by`, `_updated_by`,
  `_installed_manually`, `_updated_manually`, `dashboard_check_update_hint`; the Play-only
  `dashboard_app_version_play` and the unused `dashboard_version` removed.
- `export/ExportBuilder.kt` (FR3): `ExportInstall` (seven fields; lower-case kind; ISO 8601 UTC times via
  `Instant.ofEpochMilli`; nulls written as `null`), `ExportMetadata.install`, `SCHEMA_VERSION = 5` with
  its KDoc history, `build(..., installRecord = InstallRecord.UNKNOWN)`.
- `export/DiagnosticExporter.kt` (FR3): `installRecord: () -> InstallRecord` passed to both `build` and
  `buildCapped`'s `render`.
- `AndroidManifest.xml` (FR4): `<package android:name="org.ungoogled.android.apps.maps.microg" />`.

**Catalog** — `rules/catalog/navigation-apps.json` (FR4): `microg-maps` lists
`org.ungoogled.android.apps.maps.microg` after `app.morphe.android.apps.maps`. README and rules README
name MicroG Maps but no package, so they are unchanged.

**Requirements, spec, docs** (FR5)
- `requirements/REQ-ANDROID.md`: REQ-ANDROID-020 gains "Install record" and "Phone app version row"
  paragraphs and their acceptance; REQ-ANDROID-023 names both MicroG Maps packages and 27 queried
  packages.
- `requirements/REQ-DEBUG.md`: REQ-DEBUG-014 no longer states the current schema; new REQ-DEBUG-015
  "Install record in exports" (schema 5).
- `spec/300-data/ExportFormat.md`: the "(2)" wording fixed; new "Install (schema 5)" section.
- `spec/400-ui/AndroidUI.md`: the Updates row added to the dashboard list.
- `docs/PRIVACY_POLICY.md`, `docs/PLAY_DATA_SAFETY.md`: a line on the export's install data.
- `MANIFEST.sha256.json`: hashes of the four protected files resynced.
- `docs/IMPLEMENTATION_STATUS.md`: the entry for this work.

## Tests

- `update/InstallRecordTest` (new, 7): each Play/F-Droid/Obtainium package (suffix included);
  look-alikes are OTHER; null, blank, both package installers and the shell are MANUAL;
  `com.aurora.store` is OTHER with its names; `updatedSinceInstall` equal / differing / unknown; a
  failed lookup is UNKNOWN with nulls; a successful lookup is returned as is.
- `ui/dashboard/PhoneAppVersionRowTest` (new, 5): each named source installed and updated, manual
  installed and updated, another installer's package, unknown, unknown times.
- `ui/dashboard/DashboardUpdatesScreenTest`: the row on Play ("0.0.77, updated by Google Play"), on
  GitHub, on GitHub with the stale card; the check row shows the hint and no "Version 0.0.77"; new
  Obtainium and failed-lookup cases.
- `export/ExportBuilderTest`: the section with every field in each mode, each kind in lower case, nulls
  for UNKNOWN, schema 5. `export/DiagnosticExporterTest`: `build` and `buildCapped` both carry it; no
  record gives `unknown`.
- Catalog/families/custom packages: `NavigationAppCatalogTest` (the package belongs to MicroG Maps, its
  family root is Google Maps outside and on Play, joins Google Maps on Play),
  `RuleFamiliesMatchingTest` (icon and French fixtures match the Google Maps rules),
  `EffectiveCatalogTest` (built in, refused as a user package here and elsewhere, hidden on Play).
- Updated for the 27th package: `NavigationAppCatalogTest` (family set, Play package list, count 27),
  `EffectiveCatalogTest` (two package lists), `MicrogMapsEnablementTest` (row packages),
  `scripts/test_validate_catalog.py` (27), `StoreScreenshotsTest` (passes a Play install record so the
  screenshot still reads "updated by Google Play").

## Results

- Baseline (update, export, dashboard, catalog): 146 tests, 0 failures.
- `:app:testDebugUnitTest`: 611 tests, 0 failures, 4 skipped.
- `scripts/validate-rules.sh`: OK (manifest covers all 27 catalog packages).
- `scripts/validate-spec-assets.sh`: OK (34 files).
- `scripts/test-all.sh`: exit 0 (debug and release unit tests 611 each, 0 failures, 4 skipped; lint
  without new issues).
- `:app:assembleDebug`: OK.

## Notes

- `InstallSource` and `InstallSourceTest` are unchanged, so update cards, buttons and destinations
  behave as before (NFR2).
- The row and the install data are only checked in JVM and Robolectric tests; what the F-Droid
  privileged extension and other stores report as installer is unverified on a device.
