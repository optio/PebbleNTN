# CD Configuration: "When driving" (#132)

No pipeline change is needed: the existing workflows ship this feature as they are.

| Step | Workflow / action | Trigger | Output |
|---|---|---|---|
| 1. PR checks | `android.yml`, `watchapp.yml`, `rules.yml`, `codeql.yml` | PR from `feat/hide-when-driving` to `main` | tests, lint, watchapp build for all platforms, CodeQL |
| 2. Release | `release.yml` | merge (push) to `main` | version bump to 0.0.82, changelog, F-Droid changelog, signed APK and `watchapp.pbw`, GitHub release `v0.0.82` |
| 3. F-Droid | F-Droid build server | the `v0.0.82` tag | F-Droid update, usually about a day later |
| 4. Obtainium / GitHub | GitHub release assets | step 2 | immediate |
| 5. Play closed test | `play-release.yml` (`workflow_dispatch`, input `tag: v0.0.82`) | run by hand after step 2 [Q1: A] | upload-key-signed AAB, artifact `release-aab` |
| 6. Play Console | upload the AAB to **Closed testing – Alpha**, create the release, send for review | by hand | testers get 0.0.82 after Google's review |
| 7. Pebble appstore | upload `watchapp.pbw` from the GitHub release in the Pebble developer portal | by hand (as for every watchapp change) | watchapp 0.0.82 for everyone; watches auto-update |

## Notes

- The watchapp must reach users for the phone's When driving section to appear (it shows only for watchapp protocol 1.4, FR4), so step 7 matters more than usual.
- Branch rule: code never goes straight to `main` (every push releases); record-only commits may go to `main` with `[skip ci]`.
- Secrets used by steps 2 and 5 already exist in GitHub (release signing, Play upload key in the `release` environment); nothing new is configured.
