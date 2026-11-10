# CD Configuration: OsmAnd street and arrival time (#134, #136)

No pipeline change is needed. The existing workflows ship this change as they are.

| Step | Workflow / action | Trigger | Output |
|---|---|---|---|
| 1. PR checks | `android.yml`, `watchapp.yml`, `rules.yml`, `codeql.yml` | PR from `feat/osmand-bigtext` to `main` | tests, lint, rule validation, watchapp build for all platforms, CodeQL |
| 2. Release | `release.yml` | merge (push) to `main` | version bump to 0.0.83, changelog, F-Droid changelog, signed APK and `watchapp.pbw`, GitHub release `v0.0.83` |
| 3. Remote rules | rule packs served from `main` | step 2 | new OsmAnd, Google Maps and Vela packs (`minimumAppVersionCode` 83). Older apps skip them and keep their rules. |
| 4. F-Droid | F-Droid build server | the `v0.0.83` tag | F-Droid update, usually about a day later |
| 5. Obtainium / GitHub | GitHub release assets | step 2 | immediate |
| 6. Play closed test | `play-release.yml` (`workflow_dispatch`, input `tag: v0.0.83`) | run by hand after step 2 [Q1: A] | upload-key-signed AAB, artifact `release-aab` |
| 7. Play Console | upload the AAB to **Closed testing – Alpha**, create the release, send it for review | by hand | testers get 0.0.83 after Google's review |
| 8. Pebble appstore | upload `watchapp.pbw` from the GitHub release in the Pebble developer portal | by hand | watchapp 0.0.83 with the Directions text row; watches auto-update |

## Notes

- **Order of updates:**
  - The phone app alone already gives the new road lines and arrival times, and the Directions text setting on the phone.
  - The watchapp (step 8) adds the watch row and two-way sync (protocol 1.5).
  - An older watchapp keeps the phone's value (FR7.4).
- **#136:** the `debugSymbolLevel` line ships, but the Play warning stays, because the native libraries come already stripped. Close #136 with that explanation after the merge.
- **Branch rule:** code never goes straight to `main`, since every push releases. Record-only commits may go to `main` with `[skip ci]`.
- **Secrets:** the ones steps 2 and 6 use already exist in GitHub (release signing, and the Play upload key in the `release` environment). Nothing new is configured.
