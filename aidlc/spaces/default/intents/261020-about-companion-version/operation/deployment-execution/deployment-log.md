# Deployment Log — #103

## Target

There is no separate deployment pipeline or environment for this project. The real pipeline is `.github/workflows/release.yml`: every push to `main` verifies the commit, bumps the version and publishes a public GitHub release with the APK and PBW. The Rebble store, F-Droid and Obtainium all take their builds from that release. The Deployment Pipeline and Environment Provisioning stages did not run in this plan, so their `cd-config`, `deployment-strategy` and `environment-inventory` artifacts do not exist; this log is based on the workflow file itself.

## Pre-Deployment Checks

| Check | Result |
|---|---|
| Local suite (`scripts/test-all.sh`) | exit 0 (see ../../construction/build-and-test/test-results.md) |
| PR #106 CI: build, CodeQL, Analyze | all success |
| Database migrations | none |
| Dependent services | none (watchapp only; no Android or protocol change) |
| Deployment window | none needed; a release does not touch installed apps until users update |

## Timeline (2026-10-21, UTC)

| Time | Event |
|---|---|
| before 22:17 | Branch `feat/about-companion-version` pushed; PR #106 opened against `main` |
| 22:17:30 | PR #106 merged at the person's request ("Ok merge all open pr's"); merge commit `8ba0c47` |
| 22:17:32 | Issue #103 closed automatically by the merge |
| 22:17:34 | Release run 39685417076 started on `8ba0c47` |
| 22:25:42 | Release `v0.0.70` published; version bump commit `7e321da chore(release): v0.0.70 [skip ci]` on `main` |

## Released Version

- Tag: `v0.0.70`, https://github.com/optio/PebbleNTN/releases/tag/v0.0.70
- Assets: `pebble-ntn.pbw`, `pebble-ntn.apk`, `SHA256SUMS.txt`
- v0.0.69 (published 2026-10-19) held only the Google Maps fix (#105). v0.0.70 is the first release with #103.

## Marketplace Changelog (since 0.0.68)

```
v0.0.70
- About now shows both versions: the watchapp and the phone app it last connected to (or "Phone app: never connected").
- Google Maps "toward <road>" directions now show the correct turn arrow.
```

## Rollback

- Code: revert merge commit `8ba0c47` on `main` (`git revert -m 1 8ba0c47`) through a PR. The merge to `main` publishes a new release (v0.0.71) without the change.
- Users: the earlier builds stay on the releases page (v0.0.69 and before), so a user can reinstall one.
- Data: the only new stored data is persist key 16 (the last phone version, at most 16 bytes). An older watchapp ignores the key, so a rollback needs no data migration.
