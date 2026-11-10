# Smoke Test Results: v0.0.83

| Check | Command / evidence | Result |
|---|---|---|
| GitHub release `v0.0.83` has the APK, PBW and checksums | `gh release view v0.0.83` | `pebble-ntn.apk`, `pebble-ntn.pbw`, `SHA256SUMS.txt` |
| Assets match the published checksums | `sha256sum -c SHA256SUMS.txt` | apk OK, pbw OK |
| Version on `main` after the release bump | `scripts/version.sh --check` | 0.0.83 (versionCode 83) |
| Served remote rules are the new packs, gated to 0.0.83 | `rules/bundled/osmand/en.json` on `main` (raw.githubusercontent.com) | `osmand-en-2026.11.10`, `minimumAppVersionCode` 83 |
| Play AAB built from the tag | `play-release.yml` run 42919905650 | success; `pebblentn-0.0.83.aab` in Downloads |

## Left for the person (needs a real phone and Pebble)

- **With OsmAnd navigating:** the watch shows "Turn … <Street>" and the arrival time. Switching Directions text to Compact shows the street only at once.
- **After installing the new watchapp:** Settings has a last row "Directions text", and changing it changes the phone's setting.
