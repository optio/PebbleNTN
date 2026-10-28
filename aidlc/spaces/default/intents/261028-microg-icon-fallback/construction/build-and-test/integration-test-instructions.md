# Integration Test Instructions — MicroG arrows and user-added package names (v0.0.79)

## Automated

- **Arrow fallback:** `AppIconReferencesTest`, with a fake identifier lookup, covers a hit under the root only, a hit under the posting package, no hit, and caching.
- **`EffectiveCatalogTest`:**
  - a user-added package joins its app's rules and switch, and stops matching after removal;
  - the case is kept;
  - duplicates are refused across apps.
- **`AppPackagesUiTest`:** the Packages link and screen, with add, remove and validation messages.
- **`ExportBuilderTest`:** the `customPackage` flag and the `metadata.userAddedPackages` header (schema 4).

## On a Phone (after release)

1. **Navigation apps:** "Packages" shows next to "View its rules" for installed apps and under "Also supported". Open Google Maps → Packages: the built-in package is locked.
2. **Validation:**
   - add an invalid name (e.g. `abc`): an inline message appears;
   - add `com.google.android.apps.maps` to OsmAnd: it's refused as Google Maps' built-in package;
   - add a valid new name: it's listed with a remove button.
3. **Debug export:** the top lists the added packages per app, and events from them show `"customPackage": true`.
4. **On a phone with MicroG Maps (through #113):** icon-only cards show the right turn arrow.
