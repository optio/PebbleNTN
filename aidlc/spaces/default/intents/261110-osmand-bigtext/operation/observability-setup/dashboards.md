# Dashboards: OsmAnd street and arrival time (#134)

There is no server-side dashboard, because there is no telemetry, by design. These are the views that exist:

| View | Where | What it shows for this change |
|---|---|---|
| Debug history | phone app | each captured notification (OsmAnd title and bigText included), the rule that matched it, and the text sent |
| Watch settings | phone app | the Directions text setting (Full / Compact) |
| Diagnostics export | shared file | `metadata.watchSettings` (Directions text included) and, per capture, `watchOutput.primaryText`, `compactText`, `secondaryText` and `etaEpochSeconds` (schema 7) |
| Route-capture report | `tools/route-capture/out/<run>/report.md`, and the per-app GitHub issue with `--publish` | unrecognised OsmAnd / Google Maps / Vela card shapes per language |
| GitHub | issues, release downloads | user reports, release adoption |
| Play Console | closed test | installs, crashes and ANRs from Android vitals, tester feedback |
