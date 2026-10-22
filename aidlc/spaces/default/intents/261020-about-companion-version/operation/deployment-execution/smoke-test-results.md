# Smoke Test Results — v0.0.70

Run on 2026-10-21 against the published release assets, downloaded with `gh release download v0.0.70`.

| Check | Command or evidence | Result |
|---|---|---|
| Asset integrity | `sha256sum -c SHA256SUMS.txt` | `pebble-ntn.apk: OK`, `pebble-ntn.pbw: OK` |
| Release version | `appinfo.json` in the PBW | `"versionLabel": "0.0.70"` |
| All platforms built | PBW contents | `aplite`, `basalt`, `chalk`, `diorite`, `emery` each have `pebble-app.bin` |
| #103 present in each build | `strings <platform>/pebble-app.bin`, searching for `never connected` and `(last seen)` | both strings found in all 5 platforms |
| Post-merge CI on `8ba0c47` | `gh run list --branch main` | `release`, `watchapp` and `codeql` all success |

## Not Covered

- On-wrist check with the real Android app: install v0.0.70, open Settings > About, and confirm the phone app version shows with "(last seen)". So far this was checked only in the emulator (`pebble send-app-message` for key 11). It remains a follow-up for the person.
