# Developer Code Scan: PebbleNTN (intent 261028-microg-icon-fallback)

Focused scan at 2fa2aa53a3f4f1c167407a84967c91add183de2c, depth Minimal, against a STALE store (last scan at 1c7f4c1, before #113 merged). No builds or tests were run.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - android/app/src/main/java/com/pebblentn/app/notification/icon/ (AppIconReferences.kt, IconCandidates.kt, IconMatcher.kt, LargeIconRecognizer.kt)
  - android/app/src/main/java/com/pebblentn/app/catalog/ (NavigationAppCatalog.kt, including RuleFamilies)
  - android/app/src/test/java/com/pebblentn/app/notification/icon/ (IconCandidatesTest.kt, IconMatcherTest.kt)
  - android/app/src/test/java/com/pebblentn/app/notification/NotificationSnapshotFactoryTest.kt
- **Skimmed only**:
  - android/app/src/test/java/com/pebblentn/app/notification/ (the other five tests, by file name and grep: none touches the icon path)
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt (lines 55-70 and 130-140, outside the snapshot)
  - android/app/src/main/java/com/pebblentn/app/notification/NotificationSnapshotFactory.kt (recognizeIcon, lines 16-60)
  - android/app/src/test/java/com/pebblentn/app/catalog/NavigationAppCatalogTest.kt (lines 240-290), android/app/src/test/java/com/pebblentn/app/rules/RuleFamiliesMatchingTest.kt (grep), both outside the snapshot
  - rules/catalog/navigation-apps.json (entry order and rulesFrom), rules/bundled/google-maps/any.json (packageNames grep), android/app/src/main/AndroidManifest.xml (`<queries>` grep), android/gradle/libs.versions.toml (Robolectric version)
- **Left out**: build outputs (`android/**/build/`), `.gradle/`, and the AI-DLC install (`.claude/`, `aidlc/`)

### Packages Found
- `com.pebblentn.app.notification.icon` — library code in `:app` — Kotlin — large-icon maneuver recognition (#74)
- `com.pebblentn.app.catalog` — model in `:app` — Kotlin — navigation-app catalog, Play view and rule families (#113)

### Build System
- **Type**: Gradle (Kotlin DSL), unchanged from the store
- **Config Files**: android/app/build.gradle.kts, android/gradle/libs.versions.toml (not re-read beyond the Robolectric line)
- **Build Dependencies**: `:app` -> `:pebblekit`; rule data staged as assets and test resources (store, unchanged)

### APIs Discovered
- Internal Kotlin API: `LargeIconRecognizer` (fun interface, 1 method), `RuleDrivenLargeIconRecognizer`, `AppIconReferences.references` + `largeIconAlpha`, `IconCandidates.forPackage`, `IconMatcher.match`/`distance`, `NavigationAppCatalog.ruleFamilies`/`forGooglePlay`/`entryForPackage`, `RuleFamilies.familyOf`/`applies`/`of`/`NONE`
- Android platform: `Context.createPackageContext`, `Resources.getIdentifier(name, "drawable", pkg)`, `PackageManager.getPackageInfo(...).longVersionCode`

### Frameworks & Libraries
- kotlinx-serialization-json (catalog parse), Timber (match log), Robolectric 4.14.1 (tests), JUnit 4

### Test Coverage
- **Test Directories**: android/app/src/test/java/com/pebblentn/app/notification/icon/, .../notification/, .../catalog/, .../rules/
- **Test Frameworks**: JUnit 4; Robolectric for `NotificationSnapshotFactoryTest`
- **Coverage Config**: absent (store)

### Code Quality Indicators
- **Linting**: Android Lint; `@SuppressLint("DiscouragedApi")` on the by-name lookup (AppIconReferences.kt:34)
- **CI/CD**: unchanged (store)
- **Documentation**: KDoc on every class in scope, with issue numbers

### Technical Debt Signals
- `AppIconReferences` and `RuleDrivenLargeIconRecognizer` have no tests; nothing in the code base fakes `createPackageContext` or `getIdentifier`.
- `RuleFamilies` keeps only an unordered notion of membership; no "root package" is recorded (see Facts 6-8).
- The store's "no alias layer" prose is outdated: #113 shipped `rulesFrom` + `RuleFamilies` instead of the data-only plan.

## Facts relevant to the fix

1. **Lookup today.** `AppIconReferences.render` (android/app/src/main/java/com/pebblentn/app/notification/icon/AppIconReferences.kt:35-43): `createPackageContext(packageName, 0)` in a `runCatching`, empty list on failure (:36); per candidate `other.resources.getIdentifier(name, "drawable", packageName)` (:38); `id == 0` skips the drawable (:39); `getDrawable` failure skips it (:40). The package context and the `defPackage` argument are the same posting package; a fallback would only change the third argument, inside the same `other` context.
2. **Cache.** `ConcurrentHashMap<Key, List<IconReference>>` (:20-22), `Key(packageName, versionCode, densityDpi, candidates)`. `references` (:25-31) uses Kotlin `getOrPut`, and on a miss first evicts every key with the same `packageName` (:28). An empty result is cached as well (until version, density or candidates change). The key is the posting package; it does not include any resource package. A fallback computed inside `render` is cached under the posting package and is safe as long as the resource-package choice is a pure function of the posting package (the catalog is fixed per process in `AppContainer`, so it is).
3. **Version key.** `versionCode` (:45-46) is `getPackageInfo(posting).longVersionCode`, -1 when not visible. Unaffected by a fallback.
4. **Recognizer wiring.** `RuleDrivenLargeIconRecognizer(context, rules: () -> LayeredRules, references: AppIconReferences = AppIconReferences(context), families: RuleFamilies = RuleFamilies.NONE)` (LargeIconRecognizer.kt:24-30). `recognize` (:32-41): candidates from `IconCandidates.forPackage(rules(), packageName, families)` (:33), null when empty (icon never read), then `references.references(packageName, candidates)` (:37). The recognizer already holds `families`; `AppIconReferences` does not.
5. **Candidates.** `IconCandidates.forPackage(rules, packageName, families = NONE)` (IconCandidates.kt:23-33) includes every enabled rule where `families.applies(packageName, rule.packageNames)` (:26), so MicroG Maps already gets the 64-entry Google Maps `maneuver_*` table; the KDoc states drawables are "still looked up in [packageName] itself" (:18-22). The Google Maps rules list only `com.google.android.apps.maps` (rules/bundled/google-maps/any.json:12-14, :117-119).
6. **AppContainer (shallow).** `catalog = AssetCatalogSource(appContext).load().let { if (installSource == PLAY) it.forGooglePlay() else it }` (di/AppContainer.kt:64-65); `ruleFamilies = catalog.ruleFamilies()` (:68), shared by `AssetRuleRepository` (:81), `RuleEngine` (:117) and `RuleDrivenLargeIconRecognizer(appContext, ruleRepository::current, families = ruleFamilies)` (:137-138), which uses the default `AppIconReferences(context)`. `RulesViewModel.kt:85` builds its own `RuleEngine(families = catalog.ruleFamilies())`. One recognizer per process; it is used by `NavigationNotificationListenerService.kt:32` through `NotificationSnapshotFactory.create(sbn, container.largeIconRecognizer)`.
7. **Families have no root.** `RuleFamilies` (catalog/NavigationAppCatalog.kt:136-160) stores `Map<String, Set<String>>`; `familyOf` (:139) returns the group set (or `setOf(pkg)`), `applies` (:142-143), `NONE` (:147), `of(groups)` (:150-158) rejects a package in two groups. Nothing exposes which package is the root app's first package. `ruleFamilies()` (:59-61) builds each group with `apps.groupBy { it.rulesAppId }` and `flatMapTo(linkedSetOf())`, so the set's iteration order is catalog order of the entries, not "root first". It happens to be root first today because `google-maps` (index 0) precedes `microg-maps` (index 1) in rules/catalog/navigation-apps.json, but nothing (schema, `parse`, `validateRulesFrom` at :120-125) enforces that order. Taking `familyOf(pkg).first()` as the root would be order-fragile.
8. **Play view.** `forGooglePlay` (:71-82) drops the unlisted `microg-maps` entry and appends its packages to `google-maps` after the entry's own (:77), so on Play there is no `rulesFrom` at all: the family is one entry `[com.google.android.apps.maps, app.morphe.android.apps.maps]`. The root is then that entry's `packageNames.first()`. Off Play, the root is the `rulesFrom` target's `packageNames.first()` (`rulesAppId`, :42). `NavigationAppCatalogTest.onGooglePlayMicrogMapsIsNoEntryAndItsPackageJoinsGoogleMaps` (test/catalog/NavigationAppCatalogTest.kt:271-285) pins that both views give the same Google family. A fallback rule of "root = the first package of the entry whose rules apply" gives `com.google.android.apps.maps` in both views.
9. **Other families with several packages.** Only one `rulesFrom` exists (`microg-maps` -> `google-maps`). But multi-package entries are families too: OsmAnd (4), Organic Maps (2), CoMaps (4). If "root differs" is computed per family rather than per `rulesFrom`, `app.comaps.fdroid` etc. would retry with `app.comaps.google` and `app.organicmaps.web` with `app.organicmaps`. CoMaps and Organic Maps rules map icons (`iconDrawable` in rules/bundled/comaps/any.json and organic-maps/any.json; for Google Maps also en.json and zh.json); OsmAnd's rules do not. A retry there is harmless when the first lookup succeeds (it only runs on `id == 0`), but it widens behaviour beyond #113; limiting it to packages whose entry has `rulesFrom` (or, on Play, packages appended by `forGooglePlay`) needs the root to be recorded explicitly.
10. **Unsafe cases to keep out.** (a) A fallback must stay in the posting package's context (`other`); creating the root app's context would draw Google Maps' own drawables, which can differ from the patched build and would make the result depend on a second installed app. (b) Retrying with an arbitrary `defPackage` such as `android` could resolve framework drawables; the root is always a catalog navigation package, never `android`. (c) Per-drawable retry doubles the `getIdentifier` calls only on a miss and only once per cache key, since results are cached. (d) `RuleFamilies.NONE` (tests, tools) must keep today's behaviour: no fallback.
11. **Whether the fallback resolves at all is unverified.** It assumes the patched APK kept `com.google.android.apps.maps` as its resource-table package name and that `getIdentifier` in that context accepts it. The device check is the debug-history `iconMatch` diagnostic: with no drawable found `IconMatcher.match` returns `NoMatch("no references")` (IconMatcher.kt:84), shown as `no match: no references` (NotificationSnapshotFactory.kt withIcon).
12. **How tests fake recognition.** No test constructs `AppIconReferences` or `RuleDrivenLargeIconRecognizer`. `NotificationSnapshotFactoryTest` (Robolectric, :17) fakes the whole recognizer as a `LargeIconRecognizer { _, _ -> ... }` lambda (:111, :124, :132, :142, :153, :160). `IconCandidatesTest` is plain JUnit over `LayeredRules` and bundled JSON resources (:56-62, Google table size 64); it has no family test. `IconMatcherTest` builds `AlphaImage` masks in memory. So a fallback test needs a new seam: a pure "which resource packages to try, in order" function (testable with `RuleFamilies`/catalog only), and/or an injectable identifier lookup `(name, defPackage) -> Int` used by `render`. Faking `createPackageContext` for another package under Robolectric is not done anywhere in the repo.
13. **Privacy.** The fallback reads only resource identifiers of the posting package; REQ-SEC-003 (only the drawable name and diagnostic leave the recognizer, LargeIconRecognizer.kt:19-23) is unaffected. `<queries>` already lists both packages (AndroidManifest.xml:19-20).

## Handoff Summary
- **Intent-relevant finding**: the fallback point is `AppIconReferences.render` (AppIconReferences.kt:38-39): on `id == 0`, retry `other.resources.getIdentifier(name, "drawable", rootPackage)` in the same `other` context. The recognizer (LargeIconRecognizer.kt:24-37) already has `RuleFamilies`, but `RuleFamilies` (NavigationAppCatalog.kt:136-160) records no root, and its set order is catalog order, not root-first (:59-61). The root must be computed explicitly: the first package of the entry whose rules apply (`rulesFrom` target off Play; the merged entry's first package on Play, :71-82).
- **Risks / follow-up**: (1) recording the root changes `RuleFamilies.of`/`ruleFamilies()`, shared by `RuleEngine`, `AssetRuleRepository` and `RulesViewModel`; keep `familyOf`/`applies` unchanged. (2) Decide whether the retry applies only to `rulesFrom`/Play-merged packages or to every multi-package family (CoMaps, Organic Maps, OsmAnd). (3) The cache is keyed by the posting package and caches empty results; keep the fallback inside `render` so no cache change is needed. (4) No existing test seam for resource lookup; add one. (5) The underlying hypothesis (resource table kept the Google package name) is unverified until a device with MicroG Maps shows the diagnostic.
