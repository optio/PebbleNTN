# Requirements Analysis Questions — MicroG Maps as a Google Maps variant (#113)

Context:
- **Decided already:** MicroG Maps (Google Maps patched for microG by the "Google Maps for MicroG-RE" Morphe patch, package `app.morphe.android.apps.maps`) becomes a variant of the Google Maps catalog entry. Every Google Maps rule must apply to it. No custom-package feature.
- **From the scan:**
  - Today each rule lists its own packages. All 150 Google Maps rules list only `com.google.android.apps.maps`, and a test requires every rule to list exactly its app's packages.
  - Enablement is per app, so MicroG Maps shares the Google Maps switch.
  - The turn icons on icon-only cards are looked up in the real installed app, and whether they resolve under the renamed package can only be seen on a device with MicroG Maps.

## Q1. [Product] How does MicroG Maps reuse the Google Maps rules?

A. Rules belong to an app, not a package: a rule written for an app's packages applies to every package of that app in the catalog. MicroG Maps is one new line in the catalog. No rule file changes, user-made Google Maps rules apply too, and the same holds for every multi-package app (OsmAnd, Organic Maps, CoMaps)
B. Add `app.morphe.android.apps.maps` to the package list of all 150 Google Maps rules, and to the rule generator (data only, no engine change; user-made rules don't apply)
X. Other (please specify)

[Answer]: A (person: "So with A , the rules for google maps also apply to MicroG and new user rules for either of both apply to both? If yes, then A" — yes, confirmed)

## Q2. [Product] Should MicroG Maps be named in the README and the store description?

A. Yes: "Google Maps (also the microG build)" in the supported-apps lists
B. No: keep the lists as they are
X. Other (please specify)

[Answer]: X. Add it as a separate item in the list. In the app itself, don't show it in the Google Play release but show it in the other release with a link to it's website instead of google play

## Q3. [Product] How do we confirm it works on a real MicroG Maps install?

A. After the release, comment on #113 asking the reporter to try it and to say whether the turn arrows show on icon-only cards
B. Comment on #113 now, asking for the exact package name and a debug export, before building
C. No follow-up on the issue
X. Other (please specify)

[Answer]: X. I will manually comment on the issue

## Q4. [Product] MicroG Maps is its own row in Navigation apps on non-Play installs. Which website does its link open instead of Google Play?

A. https://github.com/fangkampanat/gmaps-patches, the "Google Maps for MicroG-RE" patch that produces `app.morphe.android.apps.maps`
B. https://microg.org
X. Other (please specify)

[Answer]: A

## Q5. [Product] On a Google Play install, what happens if MicroG Maps is installed?

A. Not listed and no link, but its navigation still reaches the watch with the Google Maps rules, following the Google Maps switch
B. Not listed, no link, and its notifications are ignored on Play installs
X. Other (please specify)

[Answer]: A

## Q6. [Product] The store description file is shared by F-Droid and the Google Play listing. Where is MicroG Maps listed?

A. README only: a separate item in the README list; the store description stays unchanged
B. README and the shared store description
X. Other (please specify)

[Answer]: A
