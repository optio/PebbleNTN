# Tracing Configuration: OsmAnd street and arrival time (#134)

Not applicable: there is no distributed system to trace. The path of one notification is fully visible on the device:

notification → `DebugCaptureProcessor` → `RuleEngine` (with `ArrivalClock` for the ETA) → `NavigationController` → `ProtocolCodec` (`compactText ?: primaryText` when Compact) → watch.

Each step is shown in the debug history and recorded in the diagnostics export (see `log-queries.md`).

To get a reproducible trace of a report, run route-capture on the emulator in that language (`tools/route-capture/routecap.py run scenarios/osmand-m3.json --only '<locale>'`) and turn the card into a fixture.
