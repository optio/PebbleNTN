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

`metadata` carries `schemaVersion` (2), PebbleNTN's `appVersion` and `appVersionCode` at export time, the Android release and the UI locale. Each event carries `versions`, as recorded when the event was captured (REQ-DEBUG-012):

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
