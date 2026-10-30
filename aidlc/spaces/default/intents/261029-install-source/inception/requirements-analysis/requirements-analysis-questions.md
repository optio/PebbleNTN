# Requirements Analysis Questions — install/update source (#119) and a second MicroG Maps package (#113)

Context:
- **Issue #119 decides:**
  - the Updates section shows the source of the current version only: "installed by" on a first install, "updated by" otherwise, and no dates;
  - debug exports get installSource, installingPackage, initiatingPackage, originatingPackage, firstInstallTime, lastUpdateTime and updatedSinceInstall, in every export mode.
- **The person's added request (verbatim):** "Also work on #113 by including org.ungoogled.android.apps.maps.microg as a buildin package name for MicroG"
- **What the code does today:**
  - only a Google Play install shows "0.0.79, updated by Google Play";
  - other sources show "Version 0.0.79" as the summary of "Check for updates now", which is hidden while the "Check for updates" card is at the top;
  - every installer that isn't Google Play, F-Droid or Obtainium counts as GitHub, including a failed lookup, and the installer package name isn't kept.

## Q1. [Product] Where does the source line go for F-Droid, Obtainium and manual installs?

A. A separate "Phone app version" row first in Updates, for every source and always shown ("0.0.79, updated by F-Droid"); "Check for updates now" keeps its button but drops its "Version 0.0.79" summary, so the version isn't shown twice
B. No new row: the "Check for updates now" summary becomes "0.0.79, updated by F-Droid" (hidden while the card is at the top)
X. Other (please specify)

[Answer]: A

## Q2. [Product] What does the row say for an installer that isn't Google Play, F-Droid, Obtainium or the system package installer (for example Aurora Store), and when the lookup fails?

A. Another installer: "0.0.79, installed by another app" / "updated by another app"; a failed lookup: only "0.0.79"
B. Treat both like a manual install: "installed manually"
C. Another installer: show its package name ("updated by com.aurora.store"); a failed lookup: only "0.0.79"
X. Other (please specify)

[Answer]: C

## Q3. [Product] A manual APK installed over an earlier version: "installed manually" or "updated manually"?

A. Follow the same rule as the other sources: "installed manually" on a first install, "updated manually" otherwise
B. Always "installed manually", as in the issue table
X. Other (please specify)

[Answer]: A

## Q4. [Privacy] The install times (first install, last update) are about PebbleNTN itself, not about navigation. Which exports carry the new install section?

A. Every export mode, as the issue says, with a line added to the privacy docs that list what an export contains
B. Only the full export; privacy-safe exports carry installSource and updatedSinceInstall but no times or package names
X. Other (please specify)

[Answer]: A
