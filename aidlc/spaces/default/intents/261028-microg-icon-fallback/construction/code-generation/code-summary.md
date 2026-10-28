# Code Summary — MicroG Maps turn arrows on icon-only cards (#113 follow-up)

Express scope, zero Units, test-after (Testing Contract `sha256:bb76c1b2…`). Nothing is committed;
`versionCode` / `versionName` stay 78 / 0.0.78 (the release workflow makes v0.0.79 on merge). No
rule file, catalog data, manifest or watch file changed.

## Files

**Android app**
- `catalog/NavigationAppCatalog.kt` (FR1):
  - `RuleFamilies` gains a per-package root map and `resourceFallbackFor(pkg)`: the root package for
    a package of an entry with `rulesFrom`, null for every other package (the root itself, CoMaps,
    Organic Maps and OsmAnd variants, unknown packages, `RuleFamilies.NONE`). `of(groups, roots)`
    takes the roots (default empty) and rejects a root outside the package's family, so the root is
    always a catalog package of the same family. `familyOf` / `applies` are unchanged.
  - `ruleFamilies()` builds the roots from each `rulesFrom` entry: the target entry's first package,
    found by `appId`, so catalog order does not matter.
  - `forGooglePlay()` records each joined package's root (the merged entry's first package) in the
    new `NavigationAppCatalog.joinedRoots` (`@Transient`, never read from JSON, empty outside the
    Play view); `ruleFamilies()` adds it to the roots.
- `notification/icon/IconResourcePackages.kt` (new, FR2): pure
  `resourcePackagesToTry(postingPackage, families)` = `[posting] + listOfNotNull(fallback)`.
- `notification/icon/AppIconReferences.kt` (FR2): constructor
  `AppIconReferences(context, families = RuleFamilies.NONE, identifiers = DrawableIdentifiers.SYSTEM)`.
  `render` still opens only `createPackageContext(postingPackage)`; for each candidate name it asks
  `identifiers` under each package of `resourcePackagesToTry`, first non-zero wins. The new
  `DrawableIdentifiers` fun interface wraps `Resources.getIdentifier(name, "drawable", pkg)` (the
  `DiscouragedApi` suppression moved onto it). The cache key and its eviction are unchanged.
- `di/AppContainer.kt` (FR2): the recognizer gets `AppIconReferences(appContext, families = ruleFamilies)`,
  the same families as the rule engine.
- `notification/icon/LargeIconRecognizer.kt`, `notification/icon/IconCandidates.kt`: KDoc only.

**Tests**
- `catalog/NavigationAppCatalogTest.kt` (+4): MicroG → Google Maps outside and on Play; Google
  Maps, CoMaps / Organic Maps / OsmAnd variants, unknown package and `NONE` → null (both views); a
  test catalog with MicroG listed first → root is the target's first package, also on Play, and the
  target's second package has no fallback; a root outside the family is rejected.
- `notification/icon/IconResourcePackagesTest.kt` (new, 5): the list for MicroG, Google Maps, a
  CoMaps variant, an unknown package (both views and `NONE`), MicroG without families.
- `notification/icon/AppIconReferencesTest.kt` (new, Robolectric, 5), with a fake
  `DrawableIdentifiers` that records the package names asked: a hit only under the root gives the
  `LEFT` reference and asks [posting, root]; a hit under the posting name never asks the root; no
  hit gives no references; `NONE` asks only the posting name; an empty result is cached (rendered
  once).
- `docs/IMPLEMENTATION_STATUS.md`: new entry.

## Design notes

- **Root on and off Play.** Off Play the root comes from `rulesFrom` (`microg-maps` →
  `google-maps` → `com.google.android.apps.maps`). The Play view has no `rulesFrom` left (MicroG's
  package is appended to the Google Maps entry), so `forGooglePlay()` keeps the information in
  `joinedRoots`. Both give `com.google.android.apps.maps`, independent of catalog order.
- **Only `rulesFrom` packages retry.** Multi-package entries (CoMaps, Organic Maps, OsmAnd) are
  families but have no root, so their behaviour is exactly as before.
- **Same context, catalog names only.** The fallback changes only the `defPackage` argument inside
  the posting app's context; no second app's context is ever opened, and the name is always a
  catalog package of the same family (checked in `RuleFamilies.of`).
- **Cache.** The packages to try are a pure function of the posting package and the process-wide
  families, so the existing key (posting package, version, density, candidates) stays correct.

## Verification

- Baseline `:app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests
  'com.pebblentn.app.notification.*'`: 63 tests, 0 failures. After: 77, 0 failures.
- `scripts/test-all.sh`: exit 0; debug and release unit tests 551 each, 0 failures, 4 skipped;
  lint has no new issue.
- `android/gradlew -p android :app:assembleDebug`: success.

## Deviations

- The Play-view root is kept on the catalog (`joinedRoots`) rather than on `NavigationAppEntry`, so
  entry equality, used across the UI and its tests, is unchanged.
- An extra guard: `RuleFamilies.of` rejects a root outside the package's family.

## Concerns

- Not verifiable without a device with MicroG Maps: if the patched app renamed its resource
  package too, the retry finds nothing and the card keeps the fallback arrow (debug history
  "no match: no references").
- FR3 (release v0.0.79, Play upload after 2026-10-30 00:14 UTC) happens at Deployment.

## Revision 1 — user-added package names (FR4–FR7)

Plan steps 7–11, test-after per layer. Still no commit; `versionCode` / `versionName` 78 / 0.0.78;
no manifest change, no `QUERY_ALL_PACKAGES`, no new library, no database migration.

### Files

**New (Android app)**
- `catalog/CustomPackageValidator.kt` (FR5): pure `check(input, appId, builtInOwners, userAdded)`
  returns `PackageCheck.Valid(name)` or `Invalid(reason, ownerAppId)`. The name is trimmed and
  lower-cased. It is refused when it is empty, longer than 255, or not `[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+`,
  or when it is already built in or user-added for this app or another one (case-insensitive).
- `data/CustomPackagesRepository.kt` (FR5): storage only, in private SharedPreferences
  `pebblentn_custom_packages`, as a JSON map of app id to a list of packages kept in insertion order.
  It exposes a `StateFlow`. An unreadable stored value is logged and read as empty.
- `catalog/EffectiveCatalog.kt` (FR6): `UserPackageStore` interface, plus `EffectiveCatalog(bundled, googlePlay, store)`.
  - Builds `view(bundled.withUserAddedPackages(stored))`, where `view` is the Play view on a Google Play install.
  - Exposes `catalog: StateFlow`, `current`, a live `families`, `builtIn`, `builtInPackagesOf`, `userAddedPackagesOf` and `displayNameOf`.
  - `add` validates the name, stores it and rebuilds; `remove` removes it and rebuilds. A rebuild updates the families first, then emits.
- `ui/apps/AppPackagesScreen.kt` (FR5): the built-in names with a lock, labelled "Built in, can't be removed".
  - The user-added names, each with a remove button labelled "Remove <pkg>".
  - An add field (no autocorrect, URI keyboard, Done adds). A refusal message appears under the field, in a polite live region.

**Changed (Android app)**
- `catalog/NavigationAppCatalog.kt`:
  - `RuleFamilies` is now an interface, built by the same `of(...)` / `NONE` plus the new `live { }` view.
  - The catalog gains `@Transient userAddedPackages` (package to app id), `isUserAdded`, `userAddedByApp`, and `withUserAddedPackages` (appended after the built-in packages; a claimed, malformed or unknown-app name is skipped).
  - `ruleFamilies()` gives a user-added package its entry's first package as root. The `rulesFrom` and joined roots still take precedence, so a MicroG one maps to Google Maps.
  - `forGooglePlay()` drops the user-added packages of an app it leaves out.
- `data/EnabledAppRepository.kt`: the primary constructor takes `StateFlow<NavigationAppCatalog>`; the old constructor taking a plain catalog is kept.
  - An app with a user-added package gets its row on sync and counts as installed.
  - `observeEnablement` also combines the catalog flow.
- `di/AppContainer.kt`: `effectiveCatalog`; `catalog` is now a getter for the current value; `ruleFamilies` is the live view.
  - Enablement uses the catalog flow, the install receiver a package provider, and the exporter a `userAddedPackages` lambda.
  - `observeCatalogChanges()`: on each change it calls `iconReferences.clear()` and `discoverInstalledApps()`.
  - New `addCustomPackage` / `removeCustomPackage`.
- `notification/icon/AppIconReferences.kt`: `clear()`. `system/NavigationAppInstallReceiver.kt`: a provider constructor; the old `Set` constructor is kept.
- `ui/apps/NavigationAppsScreen.kt` (FR4): `onOpenPackages` (null means no link).
  - A "Packages" `TextButton` next to "View its rules" on installed apps with rules, and under each "Also supported" app with rules.
  - Its accessibility label is "Package names for <app>".
- `ui/MainActivity.kt`: route `app-packages/{appId}`, which goes back for an unknown app or one without rules. "Also supported" is computed from the collected catalog. RulesViewModel gets the live families (`RulesViewModel.kt`: optional `families`).
- `export/ExportBuilder.kt`, `export/DiagnosticExporter.kt` (FR7): schema 4.
  - `metadata.userAddedPackages`, in every mode.
  - `ExportedEvent.customPackage`: `@EncodeDefault(NEVER)`, so only `true` is written.
- `res/values/strings.xml`: 22 strings.

**Specs:** REQ-ANDROID-023 gains "User-added package names" and its acceptance. New REQ-DEBUG-014
"Packages in exports". `spec/300-data/ExportFormat.md` gains "User-added packages (schema 4)".
`MANIFEST.sha256.json` resynced; `scripts/validate-spec-assets.sh` passes. `docs/IMPLEMENTATION_STATUS.md` has a Revision 1 entry.

**Tests (new or changed)**
- `catalog/CustomPackageValidatorTest.kt` (7): valid input trimmed and lower-cased; malformed, empty and too long; built in elsewhere (also a case-only clash with `com.baidu.BaiduMap`); built in here; added elsewhere; added here; the stored form.
- `data/CustomPackagesRepositoryTest.kt` (5, Robolectric): empty; order kept and repeats ignored; remove; persistence; unreadable storage.
- `catalog/EffectiveCatalogTest.kt` (11, Robolectric with real Room):
  - a user-added Google Maps package joins Google Maps, gets its label and matches the Google Maps rule `google-maps-turn-right-fr` through the dispatcher and rule engine;
  - ignored when Google Maps is off; after removal it no longer matches and the allowlist drops it;
  - the live families and the icon fallback (`[custom, google]`), MicroG to Google Maps, CoMaps to its first package, built-ins unchanged;
  - counted as installed; the Google Play view (the joined MicroG package counts as a Google Maps built-in; user-added packages come before the joined one);
  - rejected names are not stored; clashing stored packages are skipped; with no user-added packages, the catalog equals the bundled one.
- `ui/apps/AppPackagesUiTest.kt` (8, Compose):
  - the link on an installed app;
  - the link on the four "Also supported" apps with rules, and none on Waze;
  - no MicroG Maps link on Google Play;
  - the built-in names, locked;
  - add, then remove;
  - the refusal messages for a built-in elsewhere, an added elsewhere and a format error (the message clears on typing);
  - on Play, the joined package listed as built in without naming MicroG.
- `export/ExportBuilderTest.kt` (+3, and the schema assertion moved to 4); `export/DiagnosticExporterTest.kt` (+1: both the full and the capped export).

### How the effective catalog is built and refreshed

`bundled` (asset) → `withUserAddedPackages(store)` → `forGooglePlay()` on Play → `current`.
`EffectiveCatalog.add` and `remove` write to the store, then rebuild synchronously:
1. The live `RuleFamilies` answer with the new catalog straight away. The rule engine, the asset ruleset versions, the icon candidates, the icon references and the override analysis all hold the live view.
2. `catalog` then emits.
3. `AppContainer` reacts: it clears the icon-reference cache and runs discovery. Discovery creates the app's row and rebuilds the allowlist from the new catalog.

The debug labels (`container.catalog` getter) and the Navigation apps lists (collected flow) follow too.

### Verification

- Per-layer runs: catalog, data, notification, system and rules gave 280 tests, 0 failures. UI apps 14, export 23, 0 failures.
- `scripts/test-all.sh`: exit 0. Debug and release unit tests 586 each, 0 failures, 4 skipped (551 before Revision 1). Lint has no new issue in the touched code; the two it reported (modifier order, KTX `edit`) were fixed.
- `android/gradlew -p android :app:assembleDebug`: success.

### Deviations

- `RuleFamilies` became an interface, so the components built once can hold a live view. Every existing call site (`of`, `NONE`, `familyOf`, `applies`, `resourceFallbackFor`) is unchanged.
- The `customPackage` flag and the header list are computed **at export time**, not stored per event (no database migration). A package removed since an event was recorded is not flagged.
- On Google Play, a user-added package is appended before the Play view (as planned), so it comes before the joined MicroG package in the Google Maps entry. The Google Maps Packages screen there lists `app.morphe.android.apps.maps` as a built-in, without naming MicroG.
- Adding a package counts its app as installed and creates its row with the default switch. Without that, an undetected app would have no row and its notifications would stay blocked by the allowlist.
- The format requires each segment to start with a letter, as Android does. The requirement only listed letters, digits and underscores.
- `AppIconReferences.clear()` on a catalog change. The cache key itself is unchanged (FR2).

### Concerns

- Lower-casing (as FR5 asks) means a variant whose real package name has capitals cannot be added exactly. None of the five apps with rules uses capitals; duplicates are still caught case-insensitively.
- A user-added package outside the manifest `<queries>` is usually invisible: no icon, no install broadcast, and turn-arrow drawables probably unreadable. Text rules still match (FR6 limits).
- Build cache: one incremental compile right after the class-to-interface change produced stale Compose stability bytecode (`NoSuchFieldError RuleFamilies.$stable`), and the local Gradle build cache restored it after `:app:clean`. `--no-build-cache` fixed it. CI and clean checkouts are unaffected, but a local cache could hold that entry.

## Revision 2 — case-sensitive package names (plan step 12)

This revision supersedes Revision 1's lower-casing; it resolves the first Revision 1 concern. The
Google Play hiding of MicroG Maps is unchanged (policy risk), and so is the Play Packages screen.

### Files

- `catalog/CustomPackageValidator.kt`:
  - the input is only trimmed, so `Valid` carries the name exactly as typed;
  - the format accepts capitals (`[A-Za-z][A-Za-z0-9_]*`, at least two segments);
  - the duplicate check against every built-in and user-added package stays case-insensitive.
- `catalog/NavigationAppCatalog.kt`: a comment only. `withUserAddedPackages` already stored names as
  given and claimed them case-insensitively.
- `data/CustomPackagesRepository.kt`, `catalog/EffectiveCatalog.kt`, `export/*`: no change needed;
  they never lower-cased.
- Tests:
  - `catalog/CustomPackageValidatorTest.kt` (8): the case is kept; names differing only in case are
    duplicates (added here, added elsewhere, built in, `com.baidu.BaiduMap` itself); `isWellFormed`
    accepts capitals.
  - `catalog/EffectiveCatalogTest.kt` (+2, 13):
    - `com.baidu.BaiduMap` is added to Google Maps through the real repository, in a catalog without
      the Baidu entry. It is stored with its capitals, survives a restart, is enabled, and its
      notification matches `google-maps-turn-right-fr`. `com.baidu.baidumap` and
      `COM.BAIDU.BAIDUMAP` are then refused.
    - With the bundled catalog, `com.baidu.baidumap` is refused as Baidu Maps' built-in.
  - `ui/apps/AppPackagesUiTest.kt`: the screen shows `App.ReVanced.android.apps.maps` exactly as
    typed, and the same name in lower case is refused as already added.
- `requirements/REQ-ANDROID.md`:
  - REQ-ANDROID-023 now says "trimmed and otherwise stored exactly as typed, since Android package
    names are case-sensitive" and "compared without regard to case";
  - its acceptance criteria are updated to match;
  - `MANIFEST.sha256.json` is resynced and `scripts/validate-spec-assets.sh` passes.
- `inception/requirements-analysis/requirements.md` FR5: the wording is replaced as instructed.
- `docs/IMPLEMENTATION_STATUS.md`: Revision 2 entry.

### Verification

- `scripts/test-all.sh`: exit 0. Debug and release unit tests: 589 each, 0 failures, 4 skipped.
- `android/gradlew -p android :app:assembleDebug`: success.
- The stale-bytecode trap did not recur.

### Deviations

- `com.baidu.BaiduMap` is a built-in package of Baidu Maps in the bundled catalog, so it can't be
  user-added there. The "stored with capitals and matches" test therefore uses the bundled catalog
  minus the Baidu entry. The bundled catalog is still used to test that `com.baidu.baidumap` is
  refused as Baidu's built-in.
