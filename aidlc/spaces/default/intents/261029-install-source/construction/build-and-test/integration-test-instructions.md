# Integration Test Instructions — install/update source (#119) and a second MicroG Maps package (#113)

## Automated

- **`InstallRecordTest`:** the classification for every installer case, `updatedSinceInstall`, and a failed lookup.
- **`PhoneAppVersionRowTest`:** the row text for each source, first install and update.
- **`DashboardUpdatesScreenTest`:** the row on Play and on GitHub, also while the "Check for updates" card is at the top; the check row shows its hint, not the version.
- **`ExportBuilderTest` / `DiagnosticExporterTest`:** the `install` section in all three modes, nulls after a failed lookup, both build paths, schema 5.
- **Catalog, family and enablement tests:** `org.ungoogled.android.apps.maps.microg` belongs to MicroG Maps, uses the Google Maps rules, can't be added as a user package, and is hidden on Play.

## On a Phone (after release)

1. **Updates section:** "Phone app version" is the first row. After installing the APK over an earlier version by hand, it reads "0.0.80, updated manually"; installed through Obtainium, "updated by Obtainium".
2. **Debug export:** `metadata.install` shows the installer, the times and `updatedSinceInstall`, with `schemaVersion` 5.
3. **With the ungoogled MicroG Maps build installed:** Navigation apps lists it as MicroG Maps, and its Packages screen shows both built-in packages, locked.
