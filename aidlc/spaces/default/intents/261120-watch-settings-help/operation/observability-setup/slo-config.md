# SLO Configuration: Watch settings help text (#139)

There is no service to measure continuously. These are release-quality objectives, checked on every build and for every report:

| Objective | Target | Measured by |
|---|---|---|
| Every setting has help | 100 % of `WatchSetting` entries map to a help and a details string | `WatchSettingsUiTest` on every build (CI and pre-commit) |
| Shortcut descriptions complete | exactly one description per shortcut action (15 of 15) | `WatchSettingsUiTest` |
| No help for missing features | 0 touch or RGB texts on a watch without them | `WatchSettingsScreenTest` |
| Readable at large font | help fully shown at font scale 2.0, 360 dp | `WatchSettingsScreenTest` |
| Help text accuracy | 0 open issues reporting a help text that contradicts the watch, 30 days after a release | GitHub issues |
| Stability | crash-free sessions ≥ 99.5 % over a 28-day window | Play Android vitals, once enough Play installs exist |
| APK size budget | each help-text change adds < 50 KB to the release APK | release build comparison (this release: +29,892 bytes) |

The 99.5 % crash-free target is set above Play's 98.91 % bad-behaviour line, so a regression is seen before Play flags it.
