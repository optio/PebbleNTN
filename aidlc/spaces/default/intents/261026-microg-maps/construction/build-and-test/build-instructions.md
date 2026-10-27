# Build Instructions — MicroG Maps as a Google Maps variant (#113)

## Prerequisites

JDK 21 and the Android SDK (`source ~/.local/pebblentn-env.sh`), plus Python 3 for the catalog and rule validators.

## Build and Check

```bash
android/gradlew -p android :app:assembleDebug
scripts/validate-rules.sh     # catalog (26 manifest packages), rules, regressions incl. the MicroG fixtures
scripts/test-all.sh           # everything CI runs
```

## Adding Another Re-Packaged Build Later

Add one catalog entry to `rules/catalog/navigation-apps.json` with:
- `rulesFrom`: the app whose rules it reuses;
- `website`: optional, replaces the Google Play link;
- `playListed`: optional, false hides it on Google Play installs.

Then add its package to the manifest `<queries>` and a lock-table row in `NavigationAppCatalogTest`. No rule file changes.
