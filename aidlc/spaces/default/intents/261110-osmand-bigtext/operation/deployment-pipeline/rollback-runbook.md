# Rollback Runbook: OsmAnd street and arrival time (#134, #136)

## First response (no release needed)

- **Road line too long or wrong for a user:** they switch **Directions text** to **Compact**, on the phone or the watch.
- **A wrong OsmAnd or Google Maps rule in one language:** fix the rule pack on a branch, merge it, and the remote rules reach 0.0.83 apps without a new app release. `rulesetVersion` must go up.

## Revert the change

1. Revert the merge commit on a branch with `git revert -m 1 <merge-commit>`, then open a PR and merge it. `release.yml` publishes the next version (0.0.84) without the change. The bundled packs go back to their old versions, which have no `compactText` or `arrivalClock`.
2. **Remote rules:** after the revert the served packs are the old ones again. Apps at 0.0.83 accept them, because their `minimumAppVersionCode` is lower.
3. **Watchapp:** upload the new `watchapp.pbw` to the Pebble appstore if the watch has to go back too. Key 44 is additive, so the phone can be rolled back alone.
4. **Play:** run `play-release` for the revert tag and upload it to the same track. Play has no rollback; only a higher version code replaces a release.
5. **F-Droid** follows the new tag automatically.

## Verification after rollback

- The new GitHub release has no Directions text setting on the phone.
- OsmAnd shows the title instruction again; German shows the street again.
- `scripts/test-all.sh` is green on the revert branch.
