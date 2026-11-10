# SLO Configuration: OsmAnd street and arrival time (#134)

There is no service to measure continuously. These are release-quality objectives, checked for each release and each report:

| Objective | Target | Measured by |
|---|---|---|
| OsmAnd direction cards recognised | 0 unrecognised direction cards per route-capture run, in each language run | route-capture report (en and fr: 0 in this release) |
| Road line fits | 100 % of fixtures within 64 bytes (road line) and 24 bytes (second line), both forms | `FixtureTextFitTest` on every build |
| Arrival time correct | the ETA equals the next occurrence of the shown clock in 100 % of fixtures, including the next day and DST | `ArrivalClockTest`, fixture regressions |
| Unchanged default for the other apps | Google Maps, Vela, Organic Maps and CoMaps primaryText unchanged at Full | regression fixtures (NFR7) |
| Rule cost | < 5 ms per notification on the JVM | OsmAnd regression timing (about 0.17 ms) |
| Stability | crash-free sessions ≥ 99.5 % over a 28-day window | Play Android vitals (once enough Play installs exist) |
