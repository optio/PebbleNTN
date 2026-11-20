# Deployment Log: Watch settings help text (#139)

Full release, as chosen [Q1: A]. Times are UTC, 2026-11-20.

| Step | What | Result |
|---|---|---|
| 1 | Commit `bd8bd51` on `feat/watch-settings-help` | pre-commit passed: secrets, spec assets, protocol, version, syntax, unit tests and lint |
| 2 | Push, open PR #140 | https://github.com/optio/PebbleNTN/pull/140 |
| 3 | PR checks | `build` (android) pass 6m39s, `Analyze (java-kotlin)` pass, `CodeQL` pass |
| 4 | Merge PR #140 (merge commit `e4c8277`, 23:27) | merged; issue #139 closed by "Closes #139" |
| 5 | `release.yml` run 44836969909 | `verify` pass 4m18s, `release` pass 4m25s; release commit `6786c15 chore(release): v0.0.84 [skip ci]` |
| 6 | GitHub release `v0.0.84` | assets `pebble-ntn.apk`, `pebble-ntn.pbw`, `SHA256SUMS.txt`; notes list "help text for every Watch settings row (#139)" |
| 7 | `android.yml` and `codeql.yml` on `main` | success |
| 8 | `play-release.yml` run 44838122058 (`tag: v0.0.84`) | success, 5m7s; artifact `release-aab` (`app-release.aab`, 4,515,367 bytes, SHA-256 `055d8571…765f91`) |
| 9 | Hand-off files | `C:\Users\sacha\Downloads\pebblentn-0.0.84.aab` (checksum matches the artifact) and `pebblentn-0.0.84.pbw` from the release |

## Pending, done by the person

- Upload `pebblentn-0.0.84.aab` to Play Console → Closed testing – Alpha, create the release, send it for review. Optionally replace phone screenshots 2 and 3 with `fastlane/metadata/android/en-US/images/phoneScreenshots/2.png` and `3.png`.
- Optional: upload `pebblentn-0.0.84.pbw` to the Pebble appstore. The watchapp code is unchanged and only its version moved.
- F-Droid builds `v0.0.84` automatically, usually within a day.

No database migration, environment or service was involved.
