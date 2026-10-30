# Diagnostic and Rule Export Format

## Export modes

### Rules only — default
Contains selected user rules and metadata. No notification content.

### Privacy-safe diagnostic
Contains rules, extraction trace, package, locale, Android/app versions, and structurally redacted notification fields. The UI explains that potential private information is limited to content that was displayed by the source navigation app in its notification, but such content may include destinations, road names, saved-place labels and route context.

### Full diagnostic
Contains selected raw notification fields. Requires:
1. explicit selection;
2. privacy warning;
3. preview;
4. confirmation;
5. Android share sheet.

No export is sent automatically.

## Redaction

Privacy-safe export replaces:
- road names;
- destination labels;
- free-form text not required for rule matching;
- exact timestamps if user selects coarse-time mode.

It preserves structural tokens, units, punctuation and anonymized placeholders so maintainers can write rules.

## Versions (schema 2+)

`metadata` carries `schemaVersion` (the format's version, 2 or later; 5 now), PebbleNTN's `appVersion` and `appVersionCode` at export time, the Android release and the UI locale. Each event carries `versions`, as recorded when the event was captured (REQ-DEBUG-012):

```json
"versions": {
  "appVersion": "0.0.60",
  "appVersionCode": 60,
  "sourceAppVersion": "26.40.01",
  "sourceAppVersionCode": 1234567,
  "rulesetVersions": ["google-maps-any-2026.10.7", "google-maps-de-2026.10.1"]
}
```

The navigation app's version ties a log to the build whose notification texts, layouts and drawables it shows; the per-event PebbleNTN version matters because the history survives app updates. Version strings are not personal data and are kept unredacted in privacy-safe exports. Events recorded before schema 2 have null versions and an empty ruleset list.

## Grouped occurrences (schema 3)

Consecutive repeats of the same card are one event (REQ-DEBUG-013). Each event carries `occurrences` (1 when it occurred once), `firstSeenMillis` and `lastSeenMillis` (the first and latest post times), and `lastSnapshot`, the latest notification (null when it occurred once). `snapshot` stays the first notification. In privacy-safe mode `lastSnapshot` is redacted exactly like `snapshot`. An export includes as many events as the debug retention keeps (all of them when unlimited), still trimmed to the newest events that fit the 10 MB attachment cap.

## User-added packages (schema 4)

`metadata.userAddedPackages` lists the package names the user added to navigation apps (REQ-ANDROID-023), per app id, as they are at export time; it is `{}` when there are none and is present in every mode, rules-only included (package names are not personal data). Each event's `packageName` is the package that posted the notification; an event whose package is user-added at export time also carries `"customPackage": true`, and an event from a built-in package has no `customPackage` field (REQ-DEBUG-014):

```json
"metadata": { "schemaVersion": 4, "userAddedPackages": { "microg-maps": ["app.revanced.android.apps.maps"] } },
"events": [ { "packageName": "app.revanced.android.apps.maps", "customPackage": true, "eventType": "POSTED" } ]
```

The flag reflects the packages at export time, not when the event was recorded: a package removed since then is not flagged.

## Install (schema 5)

`metadata.install` says what installed or last updated the running version, and when (REQ-DEBUG-015, classified as in REQ-ANDROID-020). It is present in every mode, rules-only included, and comes only from the device's package manager, read once at app start:

```json
"metadata": {
  "schemaVersion": 5,
  "install": {
    "installSource": "fdroid",
    "installingPackage": "org.fdroid.fdroid",
    "initiatingPackage": "org.fdroid.fdroid",
    "originatingPackage": null,
    "firstInstallTime": "2026-10-01T10:00:00Z",
    "lastUpdateTime": "2026-10-29T08:30:00Z",
    "updatedSinceInstall": true
  }
}
```

- `installSource`: `play`, `fdroid`, `obtainium`, `manual` (no installer, the system package installer, or adb), `other` (another installer, named by `installingPackage`) or `unknown` (the lookup failed).
- `installingPackage`, `initiatingPackage`, `originatingPackage`: the raw package names, or `null` where Android gives none (`originatingPackage` is usually `null`).
- `firstInstallTime`, `lastUpdateTime`: ISO 8601 UTC; `updatedSinceInstall`: whether they differ.
- After a failed lookup `installSource` is `unknown` and every other field is `null`.

Android keeps only the installer of the current version, so an earlier source is lost after an update from another source; `firstInstallTime` still shows the first install.
