# Dashboards: "When driving" (#132)

There is no server-side dashboard (no telemetry, by design). The views that exist:

| View | Where | What it shows for this feature |
|---|---|---|
| Phone dashboard, Status | phone app | "Watch: hidden while driving (about … km/h)" while hidden (FR13) |
| Watch settings | phone app | the When driving switch and driving speed, and whether the watchapp supports it (section hidden below protocol 1.4) |
| Debug history | phone app | each captured Google Maps notification and how the rules matched it |
| Diagnostics export | shared file | `drivingLog`: time, measured and planned speed, threshold, decision (SAMPLE/HIDE/SHOW/KEPT_OPEN/RESET) |
| GitHub | issues and release downloads | user reports, release adoption |
| Play Console | closed test (later production) | installs, crashes and ANRs reported by Android vitals, tester feedback |
