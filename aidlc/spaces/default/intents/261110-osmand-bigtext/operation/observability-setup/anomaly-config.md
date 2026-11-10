# Anomaly Configuration: OsmAnd street and arrival time (#134)

Anomalies are handled on the device; there is no remote detection.

- **Unreadable arrival clock:** `ArrivalClock` returns null, so the rule still matches and the card is sent without an ETA. The watch then falls back to the clock text.
- **Clock just passed:** a clock up to 60 s in the past counts as now. An earlier clock means tomorrow.
- **No street on the card** (the last step shows only the summary line): the title instruction is the road line, and Compact falls back to Full.
- **Road line too long:** it is fitted to 64 bytes by the existing truncation and never dropped.
- **Older watchapp (protocol < 1.5):** key 44 is ignored and the phone keeps its own value.

**Patterns to look for in reports:**
- A road line that keeps the distance ("… 200 m"): bigText line 1 has a new layout.
- A compact form with leftover instruction words: an unhandled connecting word in that language.
- An ETA off by an hour: a time zone or DST edge.
