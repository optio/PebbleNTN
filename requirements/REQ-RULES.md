# Rule Requirements

## REQ-RULE-001 — Declarative only
Rules SHALL be declarative JSON interpreted by fixed built-in operators and extractors.

## REQ-RULE-002 — No executable remote code
Rules SHALL NOT execute scripts, load classes, invoke reflection, run shell commands, or download code.

## REQ-RULE-003 — Source package
Every rule SHALL specify one or more exact package names.

## REQ-RULE-004 — Layer order
User overrides/additions SHALL take precedence over downloaded official rules, which SHALL take precedence over bundled official rules.

## REQ-RULE-005 — Immutable official rules
Official rules SHALL be read-only in the UI. Editing SHALL create a user clone.

## REQ-RULE-006 — Simple editor identity
The simple editor SHALL edit the same JSON-backed rule model used by the expert editor.

## REQ-RULE-007 — Expert validation
Invalid JSON or semantically invalid rules SHALL not be saved as enabled rules.

## REQ-RULE-008 — Trace
Each evaluation SHALL be capable of producing a condition/extractor trace suitable for debugging.

## REQ-RULE-009 — Preview
A rule SHALL be testable against retained captures without restarting navigation.

## REQ-RULE-010 — Locale-ready
The engine and schema SHALL support locale-specific rules from the first implementation. Initial target locales are English, Dutch, French and German.

## REQ-RULE-011 — Limits
Rule count, input size, pattern size and evaluation time SHALL be bounded.

## REQ-RULE-012 — Fixtures
Every official rule change SHALL include sanitized fixtures and expected normalized outputs.

## REQ-RULE-013 — Google Maps first
Google Maps is the first official rules target. Other catalog apps may remain capture-only until evidence-backed rules are merged.

## REQ-RULE-014 — Distance units
The distance extractor SHALL require an explicit unit and SHALL recognize both abbreviated (`m`, `km`, `ft`, `mi`, `yd`) and spelled-out (`metre(s)`/`meter(s)`, `kilometre(s)`/`kilometer(s)`, `foot`/`feet`, `mile(s)`, `yard(s)`) forms, with `.` or `,` as the decimal separator. The unit SHALL immediately follow the number so a road name containing a unit word does not match, and a bare number SHALL NOT be treated as a distance. The Kotlin and Python (`rule-workbench`) implementations SHALL stay in lockstep.

## REQ-RULE-015 — Bundled ruleset layout
Bundled official rulesets SHALL be organized per navigation app then per language as `rules/bundled/<app>/<language>.json`. The app SHALL discover every bundled ruleset recursively and MAY present them grouped by app then language. File layout SHALL NOT affect matching: rules are selected by their declared `packageNames` and `locales`, never by filename.

Rules are selected by **rule family** (#113). A catalog app MAY declare `rulesFrom` (the `appId` of another app with its own rules); an app's rule family is its own packages plus those of every app whose `rulesFrom` names it. A rule — bundled, downloaded or the user's own — SHALL apply to a notification when the notification's package is in the same rule family as any package the rule lists, so a Google Maps rule applies to MicroG Maps and a user rule written for MicroG Maps applies to Google Maps. A package outside the catalog is a family of its own. An app with `rulesFrom` SHALL have no `rules/bundled/<appId>/` folder, every bundled rule SHALL list exactly its own app's catalog packages, and no rule file is copied for a variant. The same family selects the large-icon drawable table and the ruleset versions recorded with a debug event; the drawables themselves are looked up in the posting package.

**Acceptance:** JVM tests: every Google Maps fixture gives the same result for `app.morphe.android.apps.maps`, including the icon-only card; a user rule for either package applies to both; an unrelated package matches nothing; the catalog parser rejects a `rulesFrom` naming an unknown app, itself, an app that uses another's rules, or an app without rules; `package-variant-app.morphe.android.apps.maps-*` fixtures pass in `GoogleMapsRulesRegressionTest` and the rule workbench regression.

## REQ-RULE-016 — Public-transit rides
Rule outputs MAY set `stopsRemaining`, the number of public-transit stops left on the current ride. It SHALL accept either a numeric extraction or text whose first integer is the count (for example "5 stops · 10 min"), SHALL be omitted when no count is found, and SHALL never be negative. The bundled Google Maps rules SHALL classify a transit ride card (`Ride to <station>`) as the TRANSIT maneuver, with the destination station as the primary text, the stop count as `stopsRemaining` and the arrival time as the secondary text. They SHALL also classify the boarding card (`Take the <line>` / `Get on the <line>`, with `Departs` in the text) as TRANSIT, with the line and the departure stop as the primary text, and the get-off card (`Get off at <stop>`) as TRANSIT, with 1 stop left when the text says `Next stop`. The Kotlin engine and the Python rule-workbench SHALL stay in lockstep.

## REQ-RULE-017 — Street line without a leading distance
The `firstNonEmpty` extractor MAY set `stripLeadingDistance`, which removes a leading distance followed by a separator (`·`, `•` or `|`) from the chosen text, for example "70 m · Slight left onto N155" → "Slight left onto N155". The Chinese units `公里`, `千米`, `公尺` and `米` count as units here too (OsmAnd's zh title "50 米 • 左转然后直行", #134). A distance without a separator, or a number that isn't followed by a unit, SHALL be kept. Bundled rules SHALL use it wherever the navigation app puts the distance before the instruction, since the watch already shows the distance. The Kotlin engine and the Python rule-workbench SHALL stay in lockstep.

## REQ-RULE-018 — Compact road line
Rule outputs MAY set `compactText`, the short form of the road line: only the street, without the instruction words (#134). It takes the same extractors as `primaryText`; when it is absent or extracts nothing, the instruction has no compact form. The phone sends it instead of `primaryText` when the Directions text setting is Compact (REQ-ANDROID-025), and `primaryText` otherwise. Bundled rules SHALL set it where the notification gives the instruction and the street apart: OsmAnd in every bundled language (the street from `bigText`'s first line; for a roundabout from the exit number on, `2e afslag Voorbeeldweg`), Google Maps in every language (the street after the language's last "onto / toward / on" word, through `gen_localized_google_maps.py` for the generated languages) and Vela Maps. Organic Maps and CoMaps already show only the street and do not set it. With Directions text at Full, every existing fixture's `primaryText` SHALL be unchanged for Google Maps, Vela Maps, Organic Maps and CoMaps. The Kotlin engine and the Python rule-workbench SHALL stay in lockstep.

**Acceptance:** fixtures in every OsmAnd language for a turn with a street, a continue step, a summary-only card, a first line without a street and a roundabout; Google Maps and Vela Maps fixtures with and without a street; `expected.compactText` and `noCompactText` checked by `OsmandRulesRegressionTest`, `GoogleMapsRulesRegressionTest`, `VelaMapsRulesRegressionTest` and `workbench.py regression`; every fixture's road line fits the watch's 64 bytes in both forms after `ProtocolCodec.fitText` (`FixtureTextFitTest`).

## REQ-RULE-019 — Arrival clock
The `arrivalClock` extractor reads an arrival clock from a field (`field`), from capture `group` (default 1) of an optional `pattern`, or else from the first clock in the field (#134). It SHALL accept 24-hour (`11:00`, `9:05`) and 12-hour clocks (`12:12 PM`, `9:05 am`, `3:05 p. m.`; `12:xx AM` is 0:xx, `12:xx PM` is 12:xx) and the Chinese day-period prefixes `上午` and `下午`; minutes are two digits and an hour above 23 (or above 12 with AM/PM) is no clock. As text it gives the clock as written. As `etaEpochSeconds` the engine turns it into the next time the clock shows it in the phone's time zone, at or after the evaluation time less one minute of grace (a clock earlier than that is the next day), in whole minutes, so an unchanged clock gives the same instruction and causes no new send. Text without a readable clock gives no value and the rule still matches. The validator SHALL reject an unknown field, a pattern the safe-regex limits refuse and a negative group. Every bundled OsmAnd rule SHALL take its second line (`secondaryText`) and its ETA from the arrival clock of `bigText`'s summary line (`<remaining> • <time left> • <arrival>[ • <speed>]`), in every language; German stops sending the remaining distance there. Every bundled Google Maps and Vela Maps rule that shows an arrival clock SHALL set `etaEpochSeconds` from the same capture as its `secondaryText`. Packs that use `arrivalClock` or `compactText` SHALL declare the `minimumAppVersionCode` of the release that ships them. The Kotlin engine and the Python rule-workbench SHALL stay in lockstep; fixtures MAY state `nowEpochSeconds` and `timeZone` for a deterministic ETA.

**Acceptance:** `ArrivalClockTest` (later today, next day, `12:12 PM`, `12:05 AM`, unreadable, Europe/Brussels and a daylight-saving change day, whole minutes); `RuleEngineTest` and `ExtractorRunnerTest`; `RuleValidatorTest`; the OsmAnd, Google Maps and Vela Maps fixtures' `secondaryText` and `etaEpochSeconds` in both engines.
