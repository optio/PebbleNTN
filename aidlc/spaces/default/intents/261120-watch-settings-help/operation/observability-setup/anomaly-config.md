# Anomaly Configuration: Watch settings help text (#139)

Anomalies are found on the device or in reports; there is no remote detection.

## Expected edge behaviour

- **A setting is hidden by capability or state:** its help is hidden with it. Examples: Backlight colour without an RGB backlight, the gestures while Touch is off, the early-warning details while the distance is Off.
- **Imperial units:** the early-warning explanation shows feet; every other text is unit-neutral.
- **Very large font:** rows grow in height; nothing is truncated.
- **Older watchapp (protocol 1.3):** no When driving section, so no driving help. Everything else shows.

## Patterns to look for in reports

- **A help text that no longer matches the watch after a watchapp change:** a watchapp feature changed without the phone text. When a watchapp setting changes, check its help string in the same PR.
- **A new `WatchSetting` added without help:** compilation fails, because the mappings are exhaustive. Nothing can ship like that.
- **Text cut off on one device:** a layout or font issue, not a text issue.
- **A screenshot or spec that disagrees with the shipped text:** regenerate the store screenshots and update `spec/400-ui/AndroidUI.md`.
