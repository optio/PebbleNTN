#!/usr/bin/env python3
"""Capture watchapp screenshots from the Pebble emulator.

Drives a real emulator per platform: installs the .pbw, injects a navigation
update over AppMessage, then walks the on-watch settings menu with button
presses to reach each appearance configuration and screenshots the result.

Settings live in persistent storage, so every platform starts from a wiped
emulator and the script tracks the resulting state to compute button paths.

Usage: python3 tools/capture_screenshots.py [platform ...]
"""

import collections
import datetime
import struct
import subprocess
import sys
import time
import zlib
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
WATCHAPP = REPO_ROOT / "watchapp"
OUT_DIR = REPO_ROOT / "screenshots"

# Emulator platform -> screen size used as the filename suffix.
PLATFORMS = {
    "basalt": "144x168",
    "chalk": "180x180",
    "emery": "200x228",
}

# theme.h AccentId row indices (identity mapping on colour watches).
ACCENT = {
    "green": 0, "blue": 1, "red": 2, "orange": 3, "purple": 4, "teal": 5,
    "pink": 6, "yellow": 7, "cyan": 8, "lime": 9, "tan": 10, "navy": 11,
    "amber": 12, "gray": 13, "black": 14, "white": 15,
}
# theme.h GlyphPack row indices.
PACK = {"classic": 0, "bold": 1, "outline": 2}

# Settings menu row indices (settings_window.c).
ROW_COLOUR, ROW_GLYPH, ROW_ARROW, ROW_INVERT, ROW_UNITS, ROW_ETA = range(6)

# Wall-clock shown in the status strip, pinned so screenshots are reproducible.
CLOCK = "12:35:00"

# Minutes until arrival, used to render the time-to-arrival mode as a stable "IN 0:25".
ETA_MINUTES = 25


def nav_message():
    """The navigation state injected before every screenshot (protocol.h keys).

    The arrival epoch and the arrival-time string are both derived from one
    instant — the pinned clock plus ETA_MINUTES — so they cannot contradict each
    other. That matters because the two ETA display modes render from different
    sources: arrival-time mode shows the string, time-to-arrival mode counts down
    to the epoch. Hardcoding the string alongside a computed epoch produced a set
    that claimed "ETA 14:35" and "IN 0:25" at the same 12:35 clock.

    They are computed rather than hardcoded because pinning the clock to CLOCK
    sets that time on *today's* date.
    """
    pinned = datetime.datetime.combine(
        datetime.date.today(), datetime.time.fromisoformat(CLOCK))
    arrival = pinned + datetime.timedelta(minutes=ETA_MINUTES)
    return [
        "--int", "0=1", "3=6", "4=450", "10=0", f"7={int(arrival.timestamp())}",
        "--string", "5=Rue de la Loi", f"6={arrival.strftime('%H:%M')}",
    ]

DEFAULTS = {
    "accent": "green", "pack": "classic",
    "arrow_left": False, "inverted": False, "imperial": False,
    "eta_duration": False,
}

# name -> settings overrides applied on top of DEFAULTS.
MAIN_SHOTS = [
    ("main-default-green-arrow-right", {}),
    ("main-accent-blue", {"accent": "blue"}),
    ("main-accent-red", {"accent": "red"}),
    ("main-accent-orange", {"accent": "orange"}),
    ("main-accent-purple", {"accent": "purple"}),
    ("main-accent-cyan", {"accent": "cyan"}),
    ("main-inverted-green", {"inverted": True}),
    ("main-inverted-blue", {"accent": "blue", "inverted": True}),
    ("main-inverted-red", {"accent": "red", "inverted": True}),
    ("main-swapped-arrow-left-green", {"arrow_left": True}),
    ("main-swapped-arrow-left-blue", {"accent": "blue", "arrow_left": True}),
    ("main-swapped-arrow-left-inverted-red",
     {"accent": "red", "arrow_left": True, "inverted": True}),
    ("main-glyph-pack-bold-green", {"pack": "bold"}),
    ("main-glyph-pack-outline-green", {"pack": "outline"}),
    ("main-imperial-units-green", {"imperial": True}),
    ("main-eta-time-to-arrival", {"eta_duration": True}),
]


def pebble(platform, *args, retries=2):
    """Run a pebble CLI command against `platform`'s emulator."""
    cmd = ["pebble", args[0], "--emulator", platform, *args[1:]]
    for attempt in range(retries + 1):
        result = subprocess.run(cmd, cwd=WATCHAPP, capture_output=True,
                                text=True, timeout=240)
        if result.returncode == 0:
            return result
        if attempt == retries:
            raise RuntimeError(f"{' '.join(cmd)} failed:\n{result.stderr}")
        time.sleep(2)


def click(platform, button, count=1):
    if count <= 0:
        return
    args = ["emu-button"]
    if count > 1:
        args += ["--repeat", str(count)]
    pebble(platform, *args, "click", button)
    time.sleep(0.3)


def shot(platform, name):
    size = PLATFORMS[platform]
    path = OUT_DIR / f"{name}-{size}.png"
    # Pin the clock before every grab so the status strip reads the same in
    # every screenshot instead of drifting with the capture run. The strip only
    # repaints on the resulting MINUTE_UNIT tick, so give that tick time to land
    # — grabbing too early captures the previous wall-clock time.
    try:
        pebble(platform, "emu-set-time", CLOCK)
    except Exception as exc:
        print(f"  WARNING: clock not pinned for {name}: {exc}")
    time.sleep(1.5)  # let the tick + redraw land before grabbing the framebuffer
    pebble(platform, "screenshot", "--no-open", str(path))
    print(f"  {path.name}")
    return path


# Region of the status strip holding the clock, as (x0, x1, y0, y1) per platform.
# Derived from the panel/strip heights in main.c window_load().
CLOCK_REGION = {
    "basalt": (4, 60, 70, 98),
    "chalk": (8, 70, 92, 120),
    "emery": (6, 90, 80, 120),
}


def _decode_png(path):
    """Minimal RGBA PNG reader — avoids a Pillow dependency for a few pixels."""
    data = path.read_bytes()
    idat, width, height, i = b"", 0, 0, 8
    while i < len(data):
        length = struct.unpack(">I", data[i:i + 4])[0]
        chunk = data[i + 4:i + 8]
        if chunk == b"IHDR":
            width, height = struct.unpack(">II", data[i + 8:i + 16])
        elif chunk == b"IDAT":
            idat += data[i + 8:i + 8 + length]
        i += 12 + length
    raw = zlib.decompress(idat)
    stride = width * 4
    # Screenshots come back unfiltered (filter byte 0 per row), so drop it and slice.
    return width, height, [raw[1 + y * (stride + 1):1 + y * (stride + 1) + stride]
                           for y in range(height)]


def clock_mask(path, platform):
    """Foreground/background shape of the rendered clock, ignoring theme colours.

    The emulator occasionally renders the pinned time in a different timezone,
    an hour off, so screenshots taken in one run can disagree. Comparing the
    glyph shape (rather than the pixels) detects that across accent colours.
    """
    x0, x1, y0, y1 = CLOCK_REGION[platform]
    _, _, rows = _decode_png(path)
    pixels = [rows[y][x * 4:x * 4 + 3] for y in range(y0, y1) for x in range(x0, x1)]
    background = collections.Counter(pixels).most_common(1)[0][0]
    return bytes(0 if p == background else 1 for p in pixels)


def move_to_row(platform, current, target):
    """Move the menu selection from `current` to `target`; returns `target`."""
    if target > current:
        click(platform, "down", target - current)
    elif target < current:
        click(platform, "up", current - target)
    return target


def apply_settings(platform, state, target):
    """Walk the settings menu to turn `state` into `target`, in place."""
    changes = {k: v for k, v in target.items() if state[k] != v}
    if not changes:
        return

    click(platform, "select")  # nav window -> settings menu, row 0
    row = 0

    if "accent" in changes:
        row = move_to_row(platform, row, ROW_COLOUR)
        click(platform, "select")            # open the colour door at row 0
        click(platform, "down", ACCENT[changes["accent"]])
        click(platform, "select")
        click(platform, "back")
        state["accent"] = changes["accent"]

    if "pack" in changes:
        row = move_to_row(platform, row, ROW_GLYPH)
        click(platform, "select")            # open the glyph door at row 0
        click(platform, "down", PACK[changes["pack"]])
        click(platform, "select")
        click(platform, "back")
        state["pack"] = changes["pack"]

    # The remaining rows toggle in place.
    for key, menu_row in (("arrow_left", ROW_ARROW),
                          ("inverted", ROW_INVERT),
                          ("imperial", ROW_UNITS),
                          ("eta_duration", ROW_ETA)):
        if key in changes:
            row = move_to_row(platform, row, menu_row)
            click(platform, "select")
            state[key] = changes[key]

    click(platform, "back")  # settings menu -> nav window


def capture_settings_screens(platform):
    """Screenshot the settings menu and both of its sub-windows."""
    click(platform, "select")
    shot(platform, "settings-menu")
    click(platform, "down", ROW_ETA)
    shot(platform, "settings-menu-scrolled")
    click(platform, "up", ROW_ETA)

    click(platform, "select")
    shot(platform, "settings-accent-colour-list")
    click(platform, "down", 8)
    shot(platform, "settings-accent-colour-list-scrolled")
    click(platform, "back")

    click(platform, "down")
    click(platform, "select")
    shot(platform, "settings-glyph-pack-list")
    click(platform, "back")

    click(platform, "back")  # back to the nav window


def send_nav_update(platform):
    pebble(platform, "send-app-message", *nav_message())
    time.sleep(0.5)


def capture_platform(platform):
    print(f"\n=== {platform} ({PLATFORMS[platform]}) ===")
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)
    subprocess.run(["pebble", "wipe"], cwd=WATCHAPP, capture_output=True)
    time.sleep(1)

    pebble(platform, "install")
    time.sleep(2)
    send_nav_update(platform)

    capture_settings_screens(platform)
    send_nav_update(platform)

    state = dict(DEFAULTS)
    masks = {}
    for name, overrides in MAIN_SHOTS:
        target = dict(DEFAULTS)
        target.update(overrides)
        apply_settings(platform, state, target)
        masks[name] = clock_mask(shot(platform, name), platform)

    repair_clock_outliers(platform, state, masks)
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)


def repair_clock_outliers(platform, state, masks):
    """Re-take any screenshot whose clock disagrees with the rest of the run.

    The emulator sometimes renders the pinned time an hour off (see clock_mask),
    which would leave the published set showing two different clocks. The
    majority rendering wins; the stragglers are re-captured.
    """
    majority, _ = collections.Counter(masks.values()).most_common(1)[0]
    outliers = [name for name, mask in masks.items() if mask != majority]
    if not outliers:
        return
    print(f"  repairing {len(outliers)} screenshot(s) with a mismatched clock")
    for name in outliers:
        target = dict(DEFAULTS)
        target.update(dict(MAIN_SHOTS)[name])
        for _ in range(3):
            apply_settings(platform, state, target)
            if clock_mask(shot(platform, name), platform) == majority:
                break
        else:
            print(f"  WARNING: {name} still shows a different clock")


def main():
    requested = sys.argv[1:] or list(PLATFORMS)
    unknown = [p for p in requested if p not in PLATFORMS]
    if unknown:
        sys.exit(f"unknown platform(s): {', '.join(unknown)}")
    OUT_DIR.mkdir(exist_ok=True)
    for platform in requested:
        capture_platform(platform)
    print(f"\nScreenshots written to {OUT_DIR}")


if __name__ == "__main__":
    main()
