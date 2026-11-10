# Deployment Strategy: OsmAnd street and arrival time (#134, #136)

## Strategy

This is a mobile app and a watchapp, released as a whole new version (0.0.83) through the existing channels. There are no servers, so blue/green and canary do not apply.

- **What users see after updating:**
  - **OsmAnd users:** with the default Full setting, the road line shows the instruction with the street, e.g. "Turn right and go <Street>". German users change from street-only to that same full form. The second line now shows the arrival time.
  - **Google Maps and Vela users:** their road line stays the same at the default. They get the launcher arrival countdown.
  - **Organic Maps and CoMaps users:** nothing changes.
- **Per-user switch:** the Directions text setting (Full / Compact) on the phone and the watch. Compact gives street-only lines. No remote flag service is used (the app has no backend, by design).
- **Compatibility:**
  - The rule packs declare `minimumAppVersionCode` 83, so 0.0.82 apps never load them.
  - Protocol 1.5 key 44 is additive. An older watchapp ignores it, and an older phone app leaves the watch at Full.

## Promotion

| Stage | Who | Gate |
|---|---|---|
| PR | CI | all checks green |
| GitHub release (Obtainium, direct APK) | merge to `main` | the person's merge |
| F-Droid | F-Droid build server | reproducible build of the tag (APK unchanged by #136) |
| Play closed test (Alpha) | the person, via `play-release` and Play Console [Q1: A] | Google's review |
| Play production | later, once the closed test reaches 12 testers × 14 days | the person and Google's review |
| Pebble appstore (watchapp) | the person | upload in the developer portal |

## Smoke test after release

- The GitHub release `v0.0.83` has the APK and `watchapp.pbw`.
- **On a phone, with OsmAnd navigating:**
  - the watch shows "Turn … <Street>" and the arrival time;
  - with Directions text set to Compact on the phone, the watch shows only the street at once.
- **With the new watchapp:** Settings has a last row "Directions text", and changing it there changes the phone's setting.
