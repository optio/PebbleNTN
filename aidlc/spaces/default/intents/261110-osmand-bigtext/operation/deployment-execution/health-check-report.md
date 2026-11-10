# Health Check Report: v0.0.83

PebbleNTN has no servers, so there is no service health to probe. The health signals are the release channels and the release pipeline.

| Signal | Status |
|---|---|
| PR #137 checks (Android, watchapp, rules, CodeQL) | all pass |
| `release.yml` run 42918258379 | success |
| `play-release.yml` run 42919905650 | success |
| Release assets against their checksums | OK |
| Remote rule packs on `main` | new versions, `minimumAppVersionCode` 83, so 0.0.82 apps keep their old rules |
| F-Droid | pending; it builds `v0.0.83` by itself (about a day) |
| Play closed test, Pebble appstore | pending the person's uploads |

**Verdict: healthy.** No rollback trigger is present. Watch for user reports or diagnostics exports that mention OsmAnd road lines or arrival times. The rollback runbook covers the responses.
