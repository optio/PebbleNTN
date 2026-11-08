# Rollback Runbook: "When driving" (#132)

## First response (no release needed)

- Users affected by wrong guesses switch **When driving** back to **Show directions** (phone or watch), or change the driving speed. The default is Show, so users who never touched it are unaffected.

## Revert the feature

1. Revert the merge commit on a branch: `git revert -m 1 <merge-commit>`; open a PR and merge it. `release.yml` publishes the next version (0.0.83) without the feature.
2. Upload the new `watchapp.pbw` to the Pebble appstore if the watchapp has to go back too. Protocol 1.4 keys 41–43 are additive: an older watchapp simply ignores them, so the phone can be rolled back alone.
3. Play: run `play-release` for the revert tag and upload it to the same track. Play has no rollback; only a higher version code replaces a release. A release still in review can be replaced by uploading the next one.
4. F-Droid follows the new tag automatically.

## Partial fix instead of a revert

- Wrong timing or thresholds: change the constants in `DrivingPolicy.kt` / `DrivingSpeedEstimator.kt` and release; the replay test (`DrivingEmulatorReplayTest`) guards the known-good behaviour.
- Use the `drivingLog` section of a user's diagnostics export to see the speeds and decisions behind a report.

## Verification after rollback

- The new GitHub release has no When driving section on the phone (or the watch row, if the watchapp was reverted).
- `scripts/test-all.sh` green on the revert branch.
