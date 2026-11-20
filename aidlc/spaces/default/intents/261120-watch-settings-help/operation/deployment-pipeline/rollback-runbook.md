# Rollback Runbook: Watch settings help text (#139)

## First response (no release needed)

- **A help text is wrong or unclear:** fix the string in `android/app/src/main/res/values/strings.xml` on a branch and merge it. The next release carries the fix. The text has no behaviour, so a wrong sentence is a documentation defect, not an outage.
- **Layout problem on a specific phone** (for example text overlapping at an extreme font size): ask for a screenshot and its font and display size, then fix the row layout in `WatchSettingsScreen.kt` and release.

## Revert the change

1. Revert the merge commit on a branch with `git revert -m 1 <merge-commit>`, then open a PR and merge it. `release.yml` publishes the next version (0.0.85) without the help texts. The Directions note comes back, along with the earlier spec text, the manifest hashes and the store screenshots.
2. **Play:** run `play-release` for the revert tag and upload it to the same track. Play has no rollback; only a higher version code replaces a release.
3. **F-Droid** follows the new tag automatically.
4. **Watchapp:** nothing to roll back. The watchapp code didn't change.

There is no data to migrate either way: the change stores nothing and sends nothing new.

## Verification after rollback

- The new release's Watch settings rows show only the title and the value again, and the Directions note is back under its card.
- `scripts/test-all.sh` is green on the revert branch, including `validate-spec-assets.sh`.

## Escalation

The project has one maintainer (the repository owner on GitHub). Issues and Play closed-test feedback reach them through GitHub issues and the Play Console.
