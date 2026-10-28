# Requirements — MicroG Maps turn arrows on icon-only cards (#113 follow-up)

## Sources

- Initial description: [desc] "MicroG Maps turn arrows (#113 follow-up): when a notification comes from a package that reuses another app rules (rulesFrom, e.g. MicroG Maps app.morphe.android.apps.maps) and the icon drawable is not found under its own package name, retry the lookup under the root app package name (com.google.android.apps.maps) in the same package context, so icon-only cards show the right arrow. Ship as v0.0.79."
- Workflow-selected scope: [scope] express
- The person chose "MicroG arrow fix" as the content of v0.0.79, which Google Play needs for a new version code.
- The code knowledge base (focused scan at 2fa2aa5) and `inception/reverse-engineering/developer-scan-PebbleNTN.md` ("Facts relevant to the fix", concerns 1–6)

## Intent Analysis

- **Request type:** a defensive fix in the notification icon recognition. Android only; no watch, protocol or rule data change.
- **Why:**
  - Icon-only Google Maps cards are recognised by looking up `maneuver_*` drawables by name in the posting app.
  - A re-packaged build (MicroG Maps) may keep Google Maps' resource-table package name. A lookup under `app.morphe.android.apps.maps` would then find nothing, and the card shows the fallback arrow.
- **No questions needed:** the scan settled the open points:
  - the retry is limited to `rulesFrom` entries;
  - the root package is recorded explicitly;
  - the retry stays in the posting app's context.
- **Requirements depth:** Minimal.

## Functional Requirements

### FR1 — Record each family's root package

- The rule families record which package is the family root:
  - **outside Play:** the first package of the `rulesFrom` target (`google-maps` → `com.google.android.apps.maps`);
  - **on Play** (`forGooglePlay`): the merged entry's first package (the same value).
- The root does not depend on catalog order.
- The existing family membership and rule matching (`familyOf`, `applies`) behave exactly as before.
- **Pass/fail:**
  - JVM tests show the root is `com.google.android.apps.maps` for both packages, outside Play and on Play, even when MicroG Maps is listed before Google Maps in a test catalog;
  - CoMaps, Organic Maps and OsmAnd variants have no `rulesFrom`, so they get no alternative root;
  - the existing family and matching tests stay green.

### FR2 — Retry the drawable lookup under the root package name

- When `AppIconReferences` finds no drawable for a candidate name under the posting package, and the posting package belongs to an entry with `rulesFrom` whose root package differs, it retries `getIdentifier(name, "drawable", rootPackage)` in the **same** posting-package context.
- It never opens the root app's own context, and never uses a package outside the catalog.
- **Order and caching:**
  - the posting package is tried first, the root second;
  - the first hit wins;
  - the cache key (posting package, version, density, candidates) is unchanged, so results, empty ones included, stay cached per installed version.
- **Pass/fail:** a pure function lists the resource packages to try, with JVM tests:
  - MicroG Maps → [MicroG package, Google Maps package];
  - Google Maps → [Google Maps package];
  - a CoMaps variant → [its own package];
  - an unknown package → [its own package].

  Recognition with an injectable identifier lookup:
  - a hit only under the root name gives the right maneuver;
  - a hit under the posting name never tries the root;
  - no hit anywhere gives the existing "no references" result.

### FR3 — Ship as v0.0.79

Merging releases v0.0.79 through the normal release workflow, so Google Play gets a new version code. Then the `play-release` bundle for `v0.0.79` is uploaded once the new upload key is valid (from 2026-10-30 00:14 UTC).

## Added at the Build and Test gate: user-added package names

Source: the person's request at the Build and Test gate (verbatim in `requirements-analysis-questions.md`) and answers Q1–Q4 (all A).

### FR4 — A "Packages" link per app [Q1, Q3]

- In Navigation apps, every app with rules gets a **Packages** link next to "View its rules": Google Maps, MicroG Maps, OsmAnd, Organic Maps and CoMaps.
- The link shows both for installed apps and for apps under "Also supported", so an installed app that wasn't detected can still be set up.
- **On Google Play installs** it shows on every listed app. MicroG Maps stays hidden there, so no packages can be added for it on Play.
- All strings are resources, and the link has an accessibility label.
- **Pass/fail:** Compose/JVM tests show the link for an installed app and for an "Also supported" app on a GitHub install, and the absence of MicroG Maps on Play.

### FR5 — The Packages screen [Q2]

- **The list:**
  - it shows the app's built-in default package(s), marked as built-in and locked;
  - then the user-added ones, each with a remove action.
- **Adding:** the user can add one or more package names. Each name is checked before it's stored:
  - the Android package-name format (segments of letters, digits and underscores, separated by dots, at least two segments);
  - not already used by **any** app, built-in or user-added (unique within PebbleNTN);
  - trimmed; stored exactly as typed, since Android package names are case-sensitive; the duplicate check is case-insensitive.
  - A clear inline message explains a rejection.
- **Storage:** user-added packages persist across restarts and app updates.
- **Pass/fail:** JVM tests for validation:
  - invalid format;
  - a duplicate of a built-in package of another app;
  - a duplicate of a user-added package of another app;
  - a duplicate within the same app;
  - add, remove, and persistence.

### FR6 — User-added packages work like built-in ones

- A user-added package joins its app's rule family. The app's bundled rules and the user's own rules apply to it, and its app's on/off switch controls it.
- The arrow fallback of FR2 also applies: the root is the app's first built-in package.
- **Limits** (Android package visibility; no `QUERY_ALL_PACKAGES`, which Play restricts):
  - a user-added package may not be detected as installed;
  - its turn-arrow images may not be readable;
  - its notifications still match the text rules.
- **Pass/fail:**
  - JVM tests: a notification from a user-added Google Maps package matches like Google Maps; with the app switched off it's ignored; after the package is removed it no longer matches;
  - the existing family tests stay green.

### FR7 — Packages in debug exports [Q4]

- **Each exported event** records the posting package and whether it was user-added ("custom").
- **The export header** lists every user-added package per app.
- **Pass/fail:** an export test shows the custom flag on an event from a user-added package, no flag for a built-in package, and the header list.

## Non-Functional Requirements

- **Compatibility:** Google Maps and every other app behave exactly as before. A MicroG build that already resolves names under its own package is unaffected, because its own package is tried first.
- **Safety:** the retry uses only catalog packages and the posting app's own resources.
- **Testability:** the new seam is pure, or takes an injectable lookup. The full `scripts/test-all.sh` stays green.

## Constraints

- The fix can't be verified end to end without a device with MicroG Maps. The debug-history diagnostic "no match: no references" shows whether the lookup missed.

## Assumptions

- If the Morphe patch renamed the resource-table package along with the app package, the retry never triggers. Nothing changes then, and the remaining cause would be elsewhere.

## Out of Scope

- Changes to the rules, the catalog data or the watch.
- The Play Console steps themselves.

## Open Questions

None.
