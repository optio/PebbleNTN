# Watchapp screenshots

Captured from the Pebble emulator by `watchapp/tools/capture_screenshots.py`.
Regenerate with:

```bash
./scripts/build-watchapp.sh
python3 watchapp/tools/capture_screenshots.py            # all platforms
python3 watchapp/tools/capture_screenshots.py basalt     # one platform
```

Each screenshot exists in three sizes, named `<description>-<width>x<height>.png`:

| Suffix    | Emulator platform | Representative watch      |
| --------- | ----------------- | ------------------------- |
| `144x168` | basalt            | Pebble Time / Time Steel  |
| `180x180` | chalk             | Pebble Time Round         |
| `200x228` | emery             | Pebble Time 2             |

## Navigation screen

All navigation shots render the same injected state — a right turn in 450 m on
"Rue de la Loi", ETA 14:35, clock pinned to 12:35 — so only the setting under
test differs between them.

| File | Configuration |
| ---- | ------------- |
| `main-default-green-arrow-right` | Defaults: green accent, arrow right, distance left, metric |
| `main-accent-blue` / `-red` / `-orange` / `-purple` / `-cyan` | Accent colour |
| `main-inverted-green` / `-blue` / `-red` | Invert on (road area is light-on-dark) |
| `main-swapped-arrow-left-green` / `-blue` | Arrow side swapped: arrow left, distance right |
| `main-swapped-arrow-left-inverted-red` | Swapped **and** inverted |
| `main-glyph-pack-bold-green` / `-outline-green` | Glyph pack (classic is the default shot) |
| `main-imperial-units-green` | Imperial units (450 m renders as 0.2 mi) |

## Settings screens

| File | Screen |
| ---- | ------ |
| `settings-menu` | Top-level settings menu (SELECT from the navigation screen) |
| `settings-menu-scrolled` | Same menu scrolled to Invert / Distance units |
| `settings-accent-colour-list` | Accent-colour sub-window, top of the list |
| `settings-accent-colour-list-scrolled` | Accent-colour sub-window, scrolled |
| `settings-glyph-pack-list` | Glyph-pack sub-window, with per-pack arrow previews |

## Notes

- Colours look muted compared with the raw `GColor` values because the emulator
  applies the same display colour-correction a real Pebble does. This is what
  the watch actually shows; captured with correction left on deliberately.
- **Known issue visible in the `180x180` (chalk) shots:** the round display
  clips the layout. The distance and the maneuver arrow are cut off at the top
  corners, the road name is clipped at the left edge, and the settings menu
  rows run past the bezel. The rectangular layout is applied largely as-is on
  chalk; the round platform needs its own insets.
- **Also visible in `settings-glyph-pack-list`:** the arrow preview icons
  overlap the row labels ("Classic" / "Selected" are drawn under the glyph).
