# Requirements — MicroG Maps as a Google Maps variant (#113)

## Sources

- Initial description: [desc] "Implement GitHub issue #113: support MicroG Maps (Google Maps patched for microG, package app.morphe.android.apps.maps) as a Google Maps variant in the navigation-app catalog, so that all Google Maps rules apply to it."
- Workflow-selected scope: [scope] express
- GitHub issue #113 ("It's just google maps, same navigation…")
- Conversation: the person chose option 1 (a catalog variant, no custom-package feature), and said that "all Google Maps rules should apply to Microg Maps" and that rule files must not be duplicated.
- The code knowledge base (focused scan at 1c7f4c1) and `inception/reverse-engineering/developer-scan-PebbleNTN.md` ("Facts relevant to #113")
- Answers Q1–Q6 in `requirements-analysis-questions.md`

## Intent Analysis

- **Request type:** support for a new navigation app that reuses an existing app's rules. Brownfield. Android app, rule data and catalog only; no watch or protocol change.
- **Shape:**
  - Rules belong to an app family, not to single packages [Q1].
  - MicroG Maps is its own catalog entry and list row that reuses the Google Maps rules [Q2].
  - It's hidden on Google Play installs but still works there [Q5].
  - Its link goes to the patch project [Q4].
  - It's listed in the README only [Q6].
- **Follow-up on the issue:** the maintainer comments on #113 personally [Q3].
- **Requirements depth:** Minimal.

## Functional Requirements

### FR1 — Rules apply to the whole app family [Q1]

- **The family:** a catalog entry may declare that it uses another entry's rules (for example `"rulesFrom": "google-maps"`). An app's rule family is the app itself plus every entry that uses its rules.
- **Matching:** a rule applies to a notification when the notification's package belongs to the same family as any package the rule lists. This holds for bundled rules and for the user's own rules.
  - Every Google Maps rule applies to MicroG Maps, with no change to any rule file.
  - A user rule written for Google Maps applies to MicroG Maps, and one written for MicroG Maps applies to Google Maps.
  - For apps with several packages (OsmAnd, Organic Maps, CoMaps) nothing changes, because their rules already list every package.
- **Diagnostics:** the debug history and the export record the real posting package and app, and the rulesets that matched.
- **Pass/fail:**
  - JVM tests: a MicroG Maps notification matches the same rule and gives the same result as the Google Maps one, for a sample per language file and for the icon-only card;
  - a user rule for either package applies to both;
  - an unrelated package still matches nothing;
  - the Google Maps regression suite stays green.

### FR2 — MicroG Maps in the catalog [Q2, Q4]

- **The entry:**
  - `appId` `microg-maps`;
  - `displayName` "MicroG Maps";
  - package `app.morphe.android.apps.maps`;
  - uses the Google Maps rules;
  - a website link `https://github.com/fangkampanat/gmaps-patches` that replaces the Google Play "Get" link;
  - default enabled, like Google Maps.
- **Discovery:** the package is added to the manifest `<queries>`, so the app can be discovered (the count goes from 25 to 26).
- **On non-Play installs:**
  - Navigation apps shows MicroG Maps as its own row, with its own name, icon and on/off switch;
  - its link opens the website;
  - the debug history shows "MicroG Maps".
- **Validation:** the catalog schema, `scripts/validate_catalog.py` and `NavigationAppCatalogTest` accept the new fields and check that `rulesFrom` names an existing entry with rules, and that an entry using another's rules has no rule folder of its own.
- **Pass/fail:**
  - catalog validation passes;
  - a JVM or Compose test shows the MicroG Maps row with the website link on a GitHub install;
  - the "Get" link stays Google Play for other apps.

### FR3 — Hidden on Google Play installs [Q5]

- On a Play install (`InstallSource.PLAY`), MicroG Maps is not listed in Navigation apps and its link is never shown.
- If it is installed anyway, its notifications are still processed with the Google Maps rules and follow the **Google Maps** switch.
- **Pass/fail:** tests for the Play install show:
  - no MicroG Maps row and no link to the patch project anywhere in the UI;
  - a MicroG Maps notification is handled when Google Maps is enabled, and ignored when it's disabled.

### FR4 — Documentation [Q6]

- The README's supported-apps list gets MicroG Maps as a separate item: it uses the Google Maps rules, and links to the patch project.
- `fastlane/.../full_description.txt` is unchanged, because it's shared with the Play listing.
- Specs:
  - amend REQ-RULE-015 (rules are selected by package family) and REQ-RULE-003 if needed;
  - add or amend the catalog requirement for `rulesFrom`, the website link and hiding on Play;
  - resync `MANIFEST.sha256.json`;
  - add a `docs/IMPLEMENTATION_STATUS.md` entry before coding.
- **Pass/fail:** `scripts/validate-spec-assets.sh` passes.

## Non-Functional Requirements

- **No duplication:** no rule file is copied, and no rule's package list changes for this work.
- **Compatibility:** behaviour for every existing app and package is unchanged. The rule order and the "first package" uses (settings row, Rules grouping, user-rule storage) keep working.
- **Testability:** the family resolution is a pure function with JVM tests. The full `scripts/test-all.sh` stays green.
- **Store policy:** a Play install neither shows nor links to a patched Google Maps.

## Constraints

- MANIFEST-protected files are resynced in the same change. Generated files are not edited by hand.
- The turn arrows of icon-only cards are looked up in the real installed package. Whether they resolve under the renamed package can only be verified on a device with MicroG Maps; without a match, the card shows distance, road and ETA with the fallback arrow.

## Assumptions

- The MicroG-RE patch keeps Google Maps' notification texts and layout, because it's the same app code.
- `app.morphe.android.apps.maps` is the package of the current MicroG Maps build. Other re-packaged builds are out of scope and can be added later as one catalog entry each.

## Out of Scope

- A user-defined custom package feature.
- Commenting on #113: the maintainer does that personally.
- Changing the shared store description.

## Open Questions

None.
