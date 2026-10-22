# Security Test Instructions — Version-compare update detection

## Applicability

Not applicable. This work has no security requirement, and the test strategy is Minimal.

## Input Handling Note

- **Version strings:** they come from the paired app (key 11) and are only compared, never executed or formatted unchecked. A version must match the strict rule (an optional `v`, then digits separated by dots, at most 9 digits per part). Otherwise it is ignored. The watch tests this in `test_version_compare.c`, and the phone in `AppVersionsTest`.
- **Install source:** read with `getInstallSourceInfo` for the app's own package. A failed lookup falls back to GitHub (`InstallSourceTest`).
- **Destinations:** the URLs are fixed resources (`translatable="false"`). Obtainium is opened by its fixed package name through a launch intent. The manifest declares only those two package names in `<queries>`.
- **Network:** no new network access (NFR2).
