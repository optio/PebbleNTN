# Deployment Log: "When driving" (#132)

Choice [Q1: A]: commit, push, PR, merge, follow the release, run `play-release`.

| Time (UTC, 2026-11-09) | Step | Result |
|---|---|---|
| ~00:30 | Commit `b651800` "feat: hide the watchapp while driving (#132)" on `feat/hide-when-driving` (pre-commit: secrets, spec assets, protocol, version, syntax, Android tests + lint all ok) | ok |
| ~00:31 | Push and PR #133 (https://github.com/optio/PebbleNTN/pull/133) | ok |
| ~00:31–00:37 | PR checks: Android build, watchapp build, rules validate, CodeQL (Analyze java-kotlin) | all pass |
| ~00:38 | Merge PR #133 (merge commit `be05f56`), branch deleted | ok |
| ~00:38–01:05 | `release.yml` run 42538364335 on `be05f56` | success: GitHub release **v0.0.82** with `pebble-ntn.apk`, `pebble-ntn.pbw`, `SHA256SUMS.txt` |
| ~01:05–01:20 | `play-release.yml` run 42539265409 (`tag=v0.0.82`) | success: artifact `release-aab` |
| 01:21 | AAB and PBW copied to `C:\Users\sacha\Downloads\pebblentn-0.0.82.aab` and `pebblentn-0.0.82.pbw` | ok |

## Left to the person (by hand, as planned)

1. Play Console → Test and release → Closed testing – Alpha → Create new release → upload `pebblentn-0.0.82.aab` → release notes → Save and publish (Google reviews it).
2. Pebble developer portal → PebbleNTN → new release → upload `pebblentn-0.0.82.pbw` → release notes → publish.
3. F-Droid builds `v0.0.82` by itself (about a day).

## Rollback

See `../deployment-pipeline/rollback-runbook.md` (per user: switch back to Show directions; otherwise a revert release).
