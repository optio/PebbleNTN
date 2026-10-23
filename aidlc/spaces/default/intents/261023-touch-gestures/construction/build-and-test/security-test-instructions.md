# Security Test Instructions — Shortcuts (#12)

The feature adds no network, phone or data access. Its only input surfaces are the watch's own buttons, the touchscreen and its own persisted bytes.

- **Stored values:** a persisted action byte outside the valid range is read as None (`shortcut_valid_action`, host-tested with -1, `ACTION_COUNT` and 255).
- **Buffers:** the notice text is written with a bounded format into a fixed buffer, and a too-small buffer truncates and NUL-terminates (host-tested with 8 bytes).
- **Safety in use:** no gesture can close the app mid-route, because the system touch bridge stays off on the navigation screen. Touch is opt-in (default Off).
