# Deployment Strategy: "When driving" (#132)

## Strategy

A mobile app and a watchapp, released as a whole new version (0.0.82) through the existing channels; there are no servers, so blue/green or canary do not apply.

- **Safe by default:** the feature is off until a user turns on "Hide directions when driving" (NFR6). With the defaults, behaviour is the same as 0.0.81.
- **Feature flag:** the When driving setting itself acts as the per-user switch; no remote flag service is used (the app has no backend, by design).
- **Compatibility:** a 1.4 watchapp works with an older phone app (settings keep their defaults), and an older watchapp with the new phone app (the section stays hidden). The two can therefore update in any order.

## Promotion

| Stage | Who | Gate |
|---|---|---|
| PR | CI | all checks green |
| GitHub release (Obtainium, direct APK) | merge to `main` | the person's merge |
| F-Droid | F-Droid build server | reproducible build of the tag |
| Play closed test (Alpha) | the person, via `play-release` and Play Console [Q1: A] | Google's review |
| Play production | later, after the closed test meets 12 testers × 14 days | the person and Google's review |
| Pebble appstore (watchapp) | the person | upload in the developer portal |

## Smoke test after release

- The GitHub release `v0.0.82` has the APK and `watchapp.pbw`.
- On a phone with the new watchapp: Watch settings shows **When driving**; the watch's Settings list has the **When driving** row.
- With the default settings, a navigation behaves as before.
