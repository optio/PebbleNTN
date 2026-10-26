#!/usr/bin/env python3
"""Capture watchapp screenshots from the Pebble emulator.

Drives a real emulator per platform: installs the .pbw, injects a navigation
update over AppMessage, then walks the on-watch settings menu with button
presses to reach each appearance configuration and screenshots the result.

Settings live in persistent storage, so every platform starts from a wiped
emulator and the script tracks the resulting state to compute button paths.

With --previews it instead writes the phone app's preview images (#102,
REQ-ANDROID-021): one per value of each visual setting, every other setting at
its default, for basalt, chalk and emery, into
android/app/src/main/res/drawable-nodpi/watch_preview_<setting>_<value>_<platform>.png.
The settings are set the way the phone sets them, with a protocol 1.3 SETTINGS
message, so the emulator is not wiped (a wipe resets every emulator, emery's
system touch setting included). PNGs are optimised losslessly when Pillow is
importable (the pebble tool's own Python has it); otherwise they are kept as the
emulator wrote them, with a warning.

With --store it writes the raw watch screenshots the Google Play / F-Droid images
are built from (#114, REQ-ANDROID-022) into tools/play-store-sources/watch/: the
navigation screen in a few appearances and the settings menu, set the same way
as --previews (SETTINGS messages, no wipe), then restores the defaults.
tools/play_store_graphics.py composes them into the store images.

Usage: python3 tools/capture_screenshots.py [platform ...]
       python3 tools/capture_screenshots.py --previews [platform ...]
       python3 tools/capture_screenshots.py --store [platform ...]
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

# Settings menu row indices (settings_window.c) — keep this order in sync with the #defines there.
ROW_ABOUT = 0
ROW_BACKLIGHT = 1
ROW_VIBE = 2
ROW_COLOUR = 3
ROW_INVERT = 4
ROW_ETA = 5
ROW_ETA_LABEL = 6
ROW_ARROW = 7
ROW_GLYPH = 8
ROW_UNITS = 9
ROW_SHORTCUTS = 10
LAST_MAIN_ROW = ROW_SHORTCUTS

# Wall-clock shown in the status strip, pinned so screenshots are reproducible.
CLOCK = "12:35:00"

# Minutes until arrival, used to render the time-to-arrival mode as a stable "IN 0:25".
ETA_MINUTES = 25


def nav_message(distance=450):
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
        "--int", "0=1", "3=6", f"4={distance}", "10=0", f"7={int(arrival.timestamp())}",
        "--string", "5=Rue de la Loi", f"6={arrival.strftime('%H:%M')}",
    ]

DEFAULTS = {
    "accent": "green", "pack": "classic",
    "arrow_left": False, "inverted": False, "imperial": False,
    "eta_duration": False,
    "eta_compact": False,
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
    ("main-eta-label-compact", {"eta_compact": True}),
    ("main-eta-label-compact-time-to-arrival", {"eta_compact": True, "eta_duration": True}),
]

# A short distance, where the spacing between the arrow and the distance shows most (#80).
SHORT_DISTANCE = 80
SHORT_DISTANCE_SHOTS = [
    ("main-short-distance-green-arrow-right", {}),
    ("main-short-distance-swapped-arrow-left-green", {"arrow_left": True}),
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
                          ("eta_duration", ROW_ETA),
                          ("eta_compact", ROW_ETA_LABEL)):
        if key in changes:
            row = move_to_row(platform, row, menu_row)
            click(platform, "select")
            state[key] = changes[key]

    click(platform, "back")  # settings menu -> nav window


def capture_settings_screens(platform):
    """Screenshot the settings menu and its sub-windows."""
    click(platform, "select")
    shot(platform, "settings-menu")
    click(platform, "down", LAST_MAIN_ROW)
    shot(platform, "settings-menu-scrolled")
    click(platform, "up", LAST_MAIN_ROW)
    row = 0

    row = move_to_row(platform, row, ROW_COLOUR)
    click(platform, "select")               # open the colour door
    shot(platform, "settings-accent-colour-list")
    click(platform, "down", 8)
    shot(platform, "settings-accent-colour-list-scrolled")
    if platform != "aplite":  # colour watches: the white row's focus frame (#81)
        click(platform, "down", ACCENT["white"] - 8)
        shot(platform, "settings-accent-colour-list-white")
    click(platform, "back")

    row = move_to_row(platform, row, ROW_GLYPH)
    click(platform, "select")               # open the glyph door
    shot(platform, "settings-glyph-pack-list")
    click(platform, "back")

    # REQ-WATCH-024: the shortcuts door and the action list behind its first row (Up button). The
    # emulator is wiped, so Touch is On (its default) and emery also shows the gesture rows.
    row = move_to_row(platform, row, ROW_SHORTCUTS)
    click(platform, "select")               # open the shortcuts door
    shot(platform, "settings-shortcuts")
    click(platform, "select")               # Up button -> its action list
    shot(platform, "settings-shortcuts-action-list")
    click(platform, "back")
    click(platform, "back")

    row = move_to_row(platform, row, ROW_ABOUT)
    click(platform, "select")               # open About
    time.sleep(0.5)
    shot(platform, "settings-about")
    click(platform, "back")

    click(platform, "back")  # back to the nav window


def send_nav_update(platform, distance=450):
    pebble(platform, "send-app-message", *nav_message(distance))
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

    send_nav_update(platform, SHORT_DISTANCE)
    for name, overrides in SHORT_DISTANCE_SHOTS:
        target = dict(DEFAULTS)
        target.update(overrides)
        apply_settings(platform, state, target)
        shot(platform, name)
    send_nav_update(platform)

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


# --- Phone app previews (#102, REQ-ANDROID-021) --------------------------------------------------

PREVIEW_DIR = REPO_ROOT / "android" / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Protocol 1.3 (protocol/protocol-definition.json): the SETTINGS event, the revision key, and the
# first SETTING_* key; a setting's key is that plus its SettingId (watchapp/src/c/settings_sync.h).
EVENT_SETTINGS = 4
KEY_SETTINGS_REVISION = 15
KEY_SETTING_FIRST = 18

# The visual settings: resource name, SettingId, and a name per stored value, in value order. Keep
# in sync with PreviewCatalog.kt, whose test fails on any image missing or left over.
PREVIEW_SETTINGS = [
    ("accent", 0, ["green", "blue", "red", "orange", "purple", "teal", "pink", "yellow",
                   "cyan", "lime", "tan", "navy", "amber", "grey", "black", "white"]),
    ("units", 1, ["metric", "imperial"]),
    ("invert", 2, ["off", "on"]),
    ("glyph", 3, ["classic", "bold", "outline"]),
    ("arrow", 4, ["right", "left"]),
    ("eta", 5, ["arrival", "remaining"]),
    ("label", 10, ["labelled", "compact"]),
]
# Every visual setting's default is its value 0 on a colour watch (Green, metric, …).
PREVIEW_DEFAULTS = {setting_id: 0 for _, setting_id, _ in PREVIEW_SETTINGS}


class SettingsInjector:
    """Sets the watch's visual settings as the phone does, with SETTINGS messages.

    The watch applies a message only when its revision is newer (1..49 ahead, wrapping at 100).
    Its stored revision is unknown on an emulator that keeps its data, but of 0, 33 and 66 at least
    one is newer whatever it is, and after sending all three the watch is at 66 (each one it took
    moved it on, each one it refused was not newer than where it already was). From there every
    message is one ahead.
    """

    def __init__(self, platform):
        self.platform = platform
        self.revision = None

    def apply(self, values):
        if self.revision is None:
            for revision in (0, 33, 66):
                self._send(revision, values)
            self.revision = 66
        else:
            self.revision = (self.revision + 1) % 100
            self._send(self.revision, values)

    def _send(self, revision, values):
        ints = [f"0={EVENT_SETTINGS}", f"{KEY_SETTINGS_REVISION}={revision}"]
        ints += [f"{KEY_SETTING_FIRST + setting_id}={value}" for setting_id, value in values.items()]
        pebble(self.platform, "send-app-message", "--int", *ints)
        time.sleep(0.5)


_pillow_warned = False


def optimise_png(path):
    """Rewrite `path` as a palette PNG with the same pixels, when Pillow is available.

    Emulator screenshots hold at most 64 colours, so an exact palette loses nothing; the result is
    checked pixel for pixel before it replaces the original.
    """
    global _pillow_warned
    try:
        from PIL import Image
    except ImportError:
        if not _pillow_warned:
            print("  WARNING: Pillow not found; previews kept unoptimised "
                  "(run with the pebble tool's Python to optimise)")
            _pillow_warned = True
        return
    rgb = Image.open(path).convert("RGB")
    colours = rgb.getcolors(256)
    if colours is None:
        print(f"  WARNING: {path.name} has over 256 colours; kept unoptimised")
        return
    palette = [colour for _, colour in colours]
    index = {colour: i for i, colour in enumerate(palette)}
    out = Image.new("P", rgb.size)
    out.putpalette([channel for colour in palette for channel in colour])
    out.putdata([index[pixel] for pixel in rgb.getdata()])
    if out.convert("RGB").tobytes() != rgb.tobytes():
        print(f"  WARNING: {path.name} would change when optimised; kept unoptimised")
        return
    out.save(path, optimize=True)


def preview_shot(platform, path):
    """Pin the clock, grab the screen into `path`, optimise it, and return its clock mask."""
    try:
        pebble(platform, "emu-set-time", CLOCK)
    except Exception as exc:
        print(f"  WARNING: clock not pinned for {path.name}: {exc}")
    time.sleep(1.5)  # let the tick + redraw land before grabbing the framebuffer
    pebble(platform, "screenshot", "--no-open", str(path))
    mask = clock_mask(path, platform)
    optimise_png(path)
    print(f"  {path.name} ({path.stat().st_size} B)")
    return mask


def capture_previews(platform):
    print(f"\n=== previews: {platform} ({PLATFORMS[platform]}) ===")
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)
    time.sleep(1)
    pebble(platform, "install")
    time.sleep(2)
    send_nav_update(platform)
    injector = SettingsInjector(platform)

    shots = {}  # path -> (settings, clock mask)
    for name, setting_id, value_names in PREVIEW_SETTINGS:
        for value, value_name in enumerate(value_names):
            settings = dict(PREVIEW_DEFAULTS)
            settings[setting_id] = value
            injector.apply(settings)
            path = PREVIEW_DIR / f"watch_preview_{name}_{value_name}_{platform}.png"
            shots[path] = (settings, preview_shot(platform, path))

    # As for the screenshots, the majority clock rendering wins and stragglers are re-taken.
    majority, _ = collections.Counter(mask for _, mask in shots.values()).most_common(1)[0]
    for path, (settings, mask) in shots.items():
        for _ in range(3):
            if mask == majority:
                break
            print(f"  re-taking {path.name}: mismatched clock")
            injector.apply(settings)
            mask = preview_shot(platform, path)
        else:
            print(f"  WARNING: {path.name} still shows a different clock")

    # Leave the emulator on the defaults the previews assume.
    injector.apply(dict(PREVIEW_DEFAULTS))
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)


STORE_DIR = REPO_ROOT / "tools" / "play-store-sources" / "watch"

# Store shots per platform: (name, {PREVIEW_SETTINGS id: value}); "settings-menu" opens the menu.
# Accent 1 is Blue, 2 Red; invert (id 2) 1 is on. Unlisted settings stay at their defaults.
STORE_SHOTS = {
    "basalt": [("nav-green", {}), ("settings-menu", {})],
    "chalk": [("nav-blue", {0: 1}), ("nav-inverted-red", {0: 2, 2: 1})],
    "emery": [("nav-green", {})],
}


def store_shot(platform, path):
    """Pin the clock and grab the screen into `path` (kept exactly as the emulator wrote it)."""
    try:
        pebble(platform, "emu-set-time", CLOCK)
    except Exception as exc:
        print(f"  WARNING: clock not pinned for {path.name}: {exc}")
    time.sleep(1.5)
    pebble(platform, "screenshot", "--no-open", str(path))
    print(f"  {path.relative_to(REPO_ROOT)}")


def capture_store(platform):
    print(f"\n=== store: {platform} ({PLATFORMS[platform]}) ===")
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)
    time.sleep(1)
    pebble(platform, "install")
    time.sleep(2)
    send_nav_update(platform)
    injector = SettingsInjector(platform)
    for name, overrides in STORE_SHOTS[platform]:
        settings = dict(PREVIEW_DEFAULTS)
        settings.update(overrides)
        injector.apply(settings)
        send_nav_update(platform)
        path = STORE_DIR / f"{name}-{PLATFORMS[platform]}.png"
        if name == "settings-menu":
            click(platform, "select")  # the navigation window's SELECT opens the settings menu
            time.sleep(0.5)
            store_shot(platform, path)
            click(platform, "back")
        else:
            store_shot(platform, path)
    # Leave the emulator on the defaults, as --previews does.
    injector.apply(dict(PREVIEW_DEFAULTS))
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)


def main():
    args = sys.argv[1:]
    previews = "--previews" in args
    store = "--store" in args
    requested = [a for a in args if a not in ("--previews", "--store")] or list(PLATFORMS)
    unknown = [p for p in requested if p not in PLATFORMS]
    if unknown:
        sys.exit(f"unknown platform(s): {', '.join(unknown)}")
    if store:
        STORE_DIR.mkdir(parents=True, exist_ok=True)
        for platform in requested:
            capture_store(platform)
        print(f"\nStore screenshots written to {STORE_DIR}")
        return
    if previews:
        PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
        for platform in requested:
            capture_previews(platform)
        total = sum(p.stat().st_size for p in PREVIEW_DIR.glob("watch_preview_*.png"))
        count = len(list(PREVIEW_DIR.glob("watch_preview_*.png")))
        print(f"\n{count} previews ({total} B) written to {PREVIEW_DIR}")
        return
    OUT_DIR.mkdir(exist_ok=True)
    for platform in requested:
        capture_platform(platform)
    print(f"\nScreenshots written to {OUT_DIR}")


if __name__ == "__main__":
    main()
