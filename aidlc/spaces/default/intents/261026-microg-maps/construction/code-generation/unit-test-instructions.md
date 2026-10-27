# Unit Test Instructions — MicroG Maps as a Google Maps variant (#113)

## Framework and Setup

- **Phone:** JUnit 4 JVM tests through Gradle (JDK 21, `source ~/.local/pebblentn-env.sh`), with Robolectric for Compose (sdk 34).
- **Catalog and rules data:** `scripts/validate-rules.sh`, which runs the catalog and rule validators and the regressions.

## How to Run This Work's Tests

From the repo root:

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.rules.*' --tests 'com.pebblentn.app.notification.*' --tests 'com.pebblentn.app.data.*' --tests 'com.pebblentn.app.ui.*'
scripts/validate-rules.sh
scripts/test-all.sh
```

## Tests

| Test | Cases | Requirement |
|---|---|---|
| Catalog parser (NavigationAppCatalogTest or new) | MicroG entry parsed with `rulesFrom`, `website`, `playListed`; rejects an unknown target, self reference, chain, duplicate package; lock-table row for `app.morphe.android.apps.maps` | FR2 |
| Rule family resolver (new) | Google Maps ↔ MicroG Maps share a family; CoMaps packages share theirs; an unknown package is alone | FR1 |
| Rule matching (RuleEngine / regression) | MicroG icon-only card, English and non-English text cards give the same result as Google Maps; unrelated package → no match | FR1 |
| User rules | a user rule listing either package applies to both | FR1 |
| Bundled-rule layout (NavigationAppCatalogTest) | rules list exactly their root app's packages; a `rulesFrom` entry has no folder | FR1, FR2 |
| Navigation apps UI (Compose/JVM) | GitHub install: MicroG row with website link; Play install: no MicroG row, no patch-project link | FR2, FR3 |
| Allowlist on Play | Google Maps enabled → MicroG notification handled; disabled → ignored | FR3 |
| Debug labels | a MicroG event is labelled "MicroG Maps" | FR2 |
| validate_catalog.py | passes with 26 manifest packages; fails if the MicroG package is missing from `<queries>` | FR2 |

## Coverage Target

Minimal strategy: one test per unit-testable requirement (FR1–FR3), a happy path per new component, and at least two edge cases for the parser and the resolver. Express has no line-coverage floor.

## Mocking and Test Data

The real bundled catalog and rules from the assets, fixture snapshots in `rules/fixtures/google-maps.json`, fake repositories, and package names as plain strings.
