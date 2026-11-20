# CD Configuration: Watch settings help text (#139)

No pipeline change is needed. The existing workflows ship this change as they are.

| Step | Workflow / action | Trigger | Output |
|---|---|---|---|
| 1. PR checks | `android.yml`, `watchapp.yml`, `rules.yml`, `codeql.yml` | PR from `feat/watch-settings-help` to `main` | tests, lint, spec assets, watchapp build, CodeQL |
| 2. Release | `release.yml` | merge (push) to `main` | version bump to 0.0.84, changelog, F-Droid changelog, signed APK and `watchapp.pbw`, GitHub release `v0.0.84` |
| 3. F-Droid | F-Droid build server | the `v0.0.84` tag | F-Droid update, usually about a day later; picks up the regenerated phone screenshots from `fastlane/` |
| 4. Obtainium / GitHub | GitHub release assets | step 2 | immediate |
| 5. Play closed test | `play-release.yml` (`workflow_dispatch`, input `tag: v0.0.84`) | run by hand after step 2 [Q1: A] | upload-key-signed AAB, artifact `release-aab` |
| 6. Play Console | upload the AAB to **Closed testing – Alpha**, create the release, send it for review. Optionally replace phone screenshots 2 and 3 in the store listing with the regenerated `fastlane/.../phoneScreenshots/2.png` and `3.png`. | by hand | testers get 0.0.84 after Google's review |
| 7. Pebble appstore | upload `watchapp.pbw` from the GitHub release in the Pebble developer portal | by hand, optional | watchapp 0.0.84. The watchapp code is unchanged and only its version moves with the release; uploading keeps the watch and phone versions equal, so the update card doesn't ask watch users to update |

## Notes

- **Phone-only change:** no protocol, watchapp code, rule pack or remote rule change. Nothing needs `minimumAppVersionCode`.
- **Branch rule:** code never goes straight to `main`, because every push releases. Record-only commits may go to `main` with `[skip ci]`.
- **Secrets:** the ones steps 2 and 5 use already exist in GitHub (release signing, and the Play upload key in the `release` environment). Nothing new is configured.
