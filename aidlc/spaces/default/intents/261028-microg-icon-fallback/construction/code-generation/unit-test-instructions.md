# Unit Test Instructions — MicroG Maps turn arrows on icon-only cards (#113 follow-up)

## Framework and Setup

JUnit 4 JVM tests through Gradle (JDK 21, `source ~/.local/pebblentn-env.sh`). The resource lookup is faked through the new injectable identifier resolver, so no device is needed.

## How to Run This Work's Tests

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.notification.*'
scripts/test-all.sh
```

## Tests

| Test | Cases | Requirement |
|---|---|---|
| RuleFamilies root / `resourceFallbackFor` | MicroG → `com.google.android.apps.maps` outside Play and on Play, also with MicroG listed first in a test catalog; Google Maps → null; a CoMaps variant → null; unknown package → null | FR1 |
| `resourcePackagesToTry` | MicroG → [MicroG, Google Maps]; Google Maps → [Google Maps]; CoMaps variant → [itself]; unknown → [itself] | FR2 |
| AppIconReferences with a fake resolver | hit only under the root → maneuver resolved; hit under the posting package → root never queried; no hit → empty result as today | FR2 |
| CustomPackageValidator / repository (revision 1) | invalid format; duplicate of another app's built-in; duplicate of another app's user-added; same-app duplicate; trim/lowercase; add, remove, persist | FR5 |
| Effective catalog (revision 1) | a user-added Google Maps package matches like Google Maps; ignored when the app is off; no match after removal; FR2 fallback applies | FR6 |
| Packages UI (revision 1) | link on an installed app and on an "Also supported" app; no MicroG Maps on Play; add/remove; validation message | FR4, FR5 |
| Export (revision 1) | custom flag on a user-added package, none on a built-in one; header lists user-added packages per app | FR7 |
| Existing family and matching tests | unchanged and green | NFR compatibility |

## Coverage Target

Minimal strategy: one test per requirement (FR1, FR2), a happy path per component, and at least two edge cases for each new function.

## Mocking and Test Data

The real bundled catalog for family tests, small in-test catalogs for the order case, and a lambda or fake identifier resolver for the lookup.
