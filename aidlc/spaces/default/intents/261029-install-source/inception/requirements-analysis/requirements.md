# Requirements — install/update source (#119) and a second MicroG Maps package (#113)

## Sources

- Initial description: [desc] "Work on GitHub issue #119: show install/update source in the app (dashboard Updates row) and in debug exports"
- Workflow-selected scope: [scope] express
- Issue #119 (its body as refined with the person: the GUI shows only the source of the current version; "installed manually" without an example)
- The person's added request: "Also work on #113 by including org.ungoogled.android.apps.maps.microg as a buildin package name for MicroG"
- Answers Q1–Q4 in `requirements-analysis-questions.md` (A, C, A, A)
- The code knowledge base (focused scan at 494a6dd) and `inception/reverse-engineering/developer-scan.md`

## Intent Analysis

- **Request type:** an enhancement to the Updates section and the debug export, plus a catalog data change. Android only; no watch or protocol change.
- **Why:**
  - Users and the maintainer can see where the running version came from. Today only Google Play installs show it.
  - Debug exports tell the maintainer the full install picture when a report comes in.
  - MicroG Maps builds use more than one package name; `org.ungoogled.android.apps.maps.microg` is a known one.
- **Requirements depth:** Minimal.

## Functional Requirements

### FR1 — An install record

- At app start the app reads, once and with no network, from `PackageManager`:
  - `getInstallSourceInfo`: the installing, initiating and originating package names;
  - `getPackageInfo`: `firstInstallTime` and `lastUpdateTime`.
- It classifies the installing package:
  - **play:** `com.android.vending`;
  - **fdroid:** the existing F-Droid client list;
  - **obtainium:** `dev.imranr.obtainium` and `dev.imranr.obtainium.*`;
  - **manual:** no installer, or the system package installer (`com.google.android.packageinstaller`, `com.android.packageinstaller`), or the shell (adb, `com.android.shell`);
  - **other:** any other installer, whose package name is kept;
  - **unknown:** the lookup failed.
- `updatedSinceInstall` is true when the two times differ.
- The existing `InstallSource` (the update destination, REQ-ANDROID-020) is unchanged: manual, other and unknown keep pointing to GitHub, and update cards and buttons behave exactly as before.
- **Pass/fail:** JVM tests of the classification for each package above, a look-alike package (not Play), null, the package installer, adb, another store, and a failed lookup; and of `updatedSinceInstall` for equal and differing times.

### FR2 — The "Phone app version" row [Q1, Q2, Q3]

- The Updates section starts with a **Phone app version** row for every source, always shown, the "Check for updates" card at the top included.
- Its text is the version plus the current version's source, "installed by" when `firstInstallTime == lastUpdateTime` and "updated by" otherwise:

| Source | First install | Later update |
|---|---|---|
| Google Play | `0.0.79, installed by Google Play` | `0.0.79, updated by Google Play` |
| F-Droid (and its clients) | `0.0.79, installed by F-Droid` | `0.0.79, updated by F-Droid` |
| Obtainium | `0.0.79, installed by Obtainium` | `0.0.79, updated by Obtainium` |
| Manual APK install | `0.0.79, installed manually` | `0.0.79, updated manually` |
| Another installer | `0.0.79, installed by com.aurora.store` | `0.0.79, updated by com.aurora.store` |
| Failed lookup | `0.0.79` | `0.0.79` |

- No dates in the row.
- "Check for updates now" keeps its button and behaviour but no longer shows "Version 0.0.79" as its summary; it shows a short hint instead, so the version isn't shown twice.
- On Google Play the section still has no GitHub check (REQ-ANDROID-022).
- All text is Android resources; package names aren't translated.
- **Pass/fail:**
  - JVM tests of the row text for each source, for both "installed" and "updated", and for a failed lookup;
  - the Compose tests show the row on Google Play and on a GitHub install, also while the "Check for updates" card is at the top;
  - the existing Play row tests are updated to the new text.

### FR3 — The install section in debug exports [Q4]

- Every export mode adds an `install` object to the metadata:
  - `installSource`: `play` / `fdroid` / `obtainium` / `manual` / `other` / `unknown`;
  - `installingPackage`, `initiatingPackage`, `originatingPackage`: the raw names, or null where Android gives none;
  - `firstInstallTime`, `lastUpdateTime`: ISO 8601 UTC, or null after a failed lookup;
  - `updatedSinceInstall`: true / false, or null after a failed lookup.
- The export schema version goes from 4 to 5.
- `spec/300-data/ExportFormat.md` describes the section and the new version; its stale "schema version (2)" wording is corrected.
- The privacy docs that list what an export contains gain a line about the install section.
- **Pass/fail:** an export test shows the section with every field, in each export mode, and the null values after a failed lookup; the schema test expects 5.

### FR4 — A second built-in MicroG Maps package

- The MicroG Maps catalog entry lists `org.ungoogled.android.apps.maps.microg` as a built-in package after `app.morphe.android.apps.maps`.
- The manifest `<queries>` declares it, so it's detected as installed and its turn-arrow images can be read.
- Everything that applies to MicroG Maps applies to it:
  - the Google Maps rules (`rulesFrom`);
  - the arrow fallback;
  - its locked built-in place on the Packages screen;
  - the uniqueness check: it can't be added as a user package;
  - hidden on Google Play installs.
- **Pass/fail:**
  - catalog tests: the package belongs to MicroG Maps and its family root is `com.google.android.apps.maps`;
  - a notification from it matches the Google Maps rules;
  - adding it as a user package is refused as a duplicate;
  - the catalog validator and the manifest check pass.

### FR5 — Requirements, spec and release

- **Requirements:**
  - REQ-ANDROID-020 is amended for the row (FR2) and the classification (FR1);
  - the debug-export requirement in `requirements/REQ-DEBUG.md` is amended for the install section and schema 5 (FR3);
  - REQ-ANDROID-023 (MicroG Maps) names the second package (FR4).
- `MANIFEST.sha256.json` is resynced for every protected file that changed.
- Merging releases the next version (v0.0.80) through the normal release workflow; the Play bundle follows through `play-release`.

## Non-Functional Requirements

- **NFR1 — Privacy:** the install data comes only from the device's package manager. No network, no new permission. Nothing is uploaded; it's only in exports the user shares.
- **NFR2 — Compatibility:** update cards, buttons and destinations behave exactly as before for every source. Rule matching for every other app is unchanged.
- **NFR3 — Testability:** classification and row text are pure functions with JVM tests. The full `scripts/test-all.sh` stays green.

## Constraints

- Android keeps only the installer of the **current** version; an earlier source is lost after an update from another source. `firstInstallTime` in the export still shows the first install.
- What the F-Droid privileged extension and some stores report as installer can't be verified here; an unexpected one shows as "another installer" with its package name, which is still informative.

## Assumptions

- `originatingPackage` is usually null for this app; it's exported as null then.
- The system package installer's package names above cover AOSP and Google builds; another vendor's installer shows as "another installer" with its name.

## Out of Scope

- Remembering earlier install sources across updates.
- Changing which destination an update button opens.
- Watch, protocol and rule changes, and the Play Console steps.

## Open Questions

None.
