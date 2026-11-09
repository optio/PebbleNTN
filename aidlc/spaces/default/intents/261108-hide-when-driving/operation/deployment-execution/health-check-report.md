# Health Check Report: "When driving" (#132)

PebbleNTN has no servers, so health means the release channels and the app's own signals.

| Signal | Status |
|---|---|
| GitHub release v0.0.82 published, assets present | healthy |
| CI on `main` after the merge (release workflow) | green |
| Play AAB for the closed test built and correctly signed | healthy; upload pending (person) |
| Pebble appstore watchapp 0.0.82 | pending (person uploads the PBW) |
| F-Droid | follows the tag automatically (about a day) |

## What to watch after release

- GitHub issues mentioning the watchapp closing on walks or bike rides, or not reopening: ask for a diagnostics export and read its `drivingLog` (speeds, threshold, decisions; no street names).
- Issue #132's reporter's feedback on the default 25 km/h and the 30 s / 2 min timings.
- Play closed-test feedback from testers.
