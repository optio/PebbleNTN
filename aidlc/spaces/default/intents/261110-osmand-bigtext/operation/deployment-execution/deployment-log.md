# Deployment Log: OsmAnd street and arrival time (#134, #136)

The person chose [Q1: A]: commit, push, PR, merge, follow the release, run `play-release`.

| Time (2026-11-10/11) | Step | Result |
|---|---|---|
| ~23:15 | Commit `0833b42` "feat: OsmAnd street and arrival time, Directions text setting (#134)" on `feat/osmand-bigtext`. Pre-commit checks: secrets, spec assets, protocol, version, rules, syntax, Android tests and lint. | ok |
| ~23:16 | Push, then PR #137 (https://github.com/optio/PebbleNTN/pull/137) | ok |
| ~23:16–23:25 | PR checks: Android build, watchapp build, rules validate, CodeQL (Analyze java-kotlin) | all pass |
| ~23:26 | Merge PR #137 (merge commit `a600a66`); branch deleted (no PR was based on it); #134 closed by the PR | ok |
| ~23:26–23:50 | `release.yml` run 42918258379 on `a600a66` | success: GitHub release **v0.0.83** with `pebble-ntn.apk`, `pebble-ntn.pbw`, `SHA256SUMS.txt` |
| ~23:50–00:07 | `play-release.yml` run 42919905650 (`tag=v0.0.83`) | success: artifact `release-aab` |
| 00:07 | AAB and PBW copied to `C:\Users\sacha\Downloads\pebblentn-0.0.83.aab` and `pebblentn-0.0.83.pbw` | ok |
| 00:10 | Release text for the Play closed test and the Pebble appstore given to the person (466 characters, under Play's 500 limit) | ok |

## Left to the person (by hand, as planned)

1. **Play Console:** Test and release → Closed testing – Alpha → Create new release → upload `pebblentn-0.0.83.aab` → add the release notes → Save and publish. Google then reviews it.
2. **Pebble developer portal:** PebbleNTN → new release → upload `pebblentn-0.0.83.pbw` → add the release notes → publish.
3. **F-Droid** builds `v0.0.83` by itself, usually within about a day.
4. **#136:** close it as not fixable from the app (see `construction/build-and-test/test-results.md`).

## Rollback

See `../deployment-pipeline/rollback-runbook.md`. A user can switch Directions text to Compact. A broken rule can be fixed through the remote packs. Otherwise, make a revert release.
