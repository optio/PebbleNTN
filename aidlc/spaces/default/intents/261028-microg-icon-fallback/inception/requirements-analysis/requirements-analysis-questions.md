# Requirements Analysis Questions — MicroG arrows and user-added package names (v0.0.79)

Context:
- **Already approved:** FR1–FR2, the arrow fallback for MicroG Maps.
- **The person's new request (verbatim):** "The MicroG package name is often different, allow the user to manually add one or more package names that should be used to detect MicroG . Add a link next to 'View its rules' that states 'Packages'. Clickign it should list the default package name used by PebbleNTN and allow the user to add one or more packages names to use as detection. The app should not allow adding duplicate package names, unqiue within the app to avoid the same name is used for multiple apps. The link should also be visible for non instaleld apps lited under 'Also supported' in case an instaleld app is not detected. When sharing debug logs, it should also contain the package name used to detect the app and indication if the package name is custom so that we can consider adding those as building upon reviewing logs."
- **Decided by that request:**
  - a "Packages" link next to "View its rules", also for apps under "Also supported";
  - it lists the default package(s) and lets the user add more;
  - a package can belong to only one app, built-in or user-added;
  - debug exports name the matched package and whether it was user-added.
- **A known Android limit:**
  - Android only lets an app see installed apps it declares in advance (the manifest list). A user-added package isn't in that list, so PebbleNTN may not detect it as installed, and may not read its turn-arrow images.
  - Its notifications still arrive and match the text rules.
  - Declaring "see all apps" (`QUERY_ALL_PACKAGES`) isn't an option: Google Play restricts it.

## Q1. [Product] Which apps get the "Packages" link?

A. Every app with rules (Google Maps, MicroG Maps, OsmAnd, Organic Maps, CoMaps), so any variant can be added
B. Only MicroG Maps
X. Other (please specify)

[Answer]: A

## Q2. [Product] Can user-added packages be removed again?

A. Yes: user-added packages can be removed; the built-in default packages are shown but locked
B. No: add only
X. Other (please specify)

[Answer]: A

## Q3. [Product] On a Google Play install, where does the "Packages" link show?

A. On every app that's listed (MicroG Maps stays hidden on Play, so its packages can't be added there)
B. Nowhere on Play installs
X. Other (please specify)

[Answer]: A

## Q4. [Product] What does a debug export say about packages?

A. Per event: the posting package and "custom" when it was user-added; plus, at the top, every user-added package per app
B. Per event only
X. Other (please specify)

[Answer]: A
