# Performance Test Instructions — Settings sync (#102)

There are no load or latency targets. The resource checks that matter:

- **aplite memory (NFR1):** `pebble build` must report a positive aplite heap at load (3597 B, v0.0.76 had 3916 B). On aplite, scroll the Shortcuts action list to its end: it must scroll and not fault. With Settings and a submenu open, the free heap must be no worse than v0.0.76 minus 500 B. Code Generation measured, with identical temporary logging: top menu 1764 vs 1780, doors 1020 vs 1036, Shortcuts action list 424 vs 440, glyph-pack door 1020 (it faulted in the instrumented v0.0.76).
- **Message size:** a full `SETTINGS` message is 26 small integers, well within the AppMessage buffers.
- **Flash (NFR3):** a sync writes only changed values (`settings_store`, host-tested).
- **APK size:** the 87 preview images add 94,235 bytes.
