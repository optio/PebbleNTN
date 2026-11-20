# Tracing Configuration: Watch settings help text (#139)

## Applicability

Not applicable: there is no distributed system to trace, and the help texts are static resources with no runtime path.

## The whole path of one help text

`WatchSetting` → `WatchSettingsUi.help()` / `details(imperial)` / `valueDescriptions()` → string resource → `SettingRow` / `OptionListScreen` / `DrivingSection` → screen.

To trace a reported text, find its resource name with the first query in `log-queries.md`. The mapping functions in `WatchSettingsUi.kt` show which setting uses it.
