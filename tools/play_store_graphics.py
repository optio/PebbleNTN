#!/usr/bin/env python3
"""Google Play store graphics for PebbleNTN (REQ-ANDROID-022, #114).

Reads the raw captures in tools/play-store-sources/ and writes into
fastlane/metadata/android/en-US/images/ (Google Play and F-Droid share these images):

- phoneScreenshots/1.png..N.png: the phone screens in tools/play-store-sources/phone/, in file-name
  order, as 1080x2160 (Play's 2:1 limit). They come from StoreScreenshotsTest (the current app's
  screens rendered by Robolectric at exactly 1080x2160) and are copied as they are. A taller phone
  capture (a device's 1080x2280) is cropped: the status bar goes, the rest of the rows come off the
  bottom, and every cut runs through background rows only (see crop_rows); anything else is scaled
  and padded.
- the next two phoneScreenshots: 1080x2160 images of the watch captures in
  tools/play-store-sources/watch/ (watchapp/tools/capture_screenshots.py --store), scaled x4 with
  nearest-neighbour (crisp pixels) under a short caption.
- featureGraphic.png: 1024x500, from the app icon, the name, the short description and a watch
  capture scaled x2.

Numbered screenshots the run did not write are removed, so the folder holds exactly this set.

`--check` validates every image against Play's rules instead (icon 512x512; feature graphic
1024x500 without alpha; at least two phone screenshots, each side 320-3840 px, the long side at
most twice the short one, without alpha) and exits 1 on a violation.

Needs Pillow; the pebble tool's Python has it (`head -1 "$(command -v pebble)"` names it). The
output depends only on the inputs, Pillow and the caption font, so a run is repeatable.

Usage:
    <python with Pillow> tools/play_store_graphics.py           # write the images
    <python with Pillow> tools/play_store_graphics.py --check   # validate them
"""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parent.parent
IMAGES = REPO / "fastlane/metadata/android/en-US/images"
PHONE = IMAGES / "phoneScreenshots"
SOURCES = REPO / "tools/play-store-sources"
PHONE_SOURCES = SOURCES / "phone"
WATCH = SOURCES / "watch"
SHORT_DESCRIPTION = REPO / "fastlane/metadata/android/en-US/short_description.txt"

PHONE_SIZE = (1080, 2160)
FEATURE_SIZE = (1024, 500)
ICON_SIZE = (512, 512)
MIN_SIDE, MAX_SIDE, MAX_RATIO = 320, 3840, 2.0
MIN_PHONE_SCREENSHOTS = 2

# The icon's background, so the composed images sit with it in the listing.
BACKGROUND = (28, 27, 31)
BEZEL = (58, 57, 64)
TEXT = (255, 255, 255)
TEXT_DIM = (200, 198, 210)

# Status bar: rows above this are dropped; content keeps at least this much space above it.
STATUS_BAR_END = 51
MIN_TOP_MARGIN = 40

# Caption fonts, first found wins; without any, captions are left out with a warning.
FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
    "/Library/Fonts/Arial Bold.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
]
FONT_REGULAR_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/TTF/DejaVuSans.ttf",
    "/Library/Fonts/Arial.ttf",
    "C:/Windows/Fonts/arial.ttf",
]


@dataclass(frozen=True)
class Composite:
    """A phone-size image of watch screenshots under a caption."""

    caption: str
    shots: tuple[str, ...]


COMPOSITES = [
    Composite(
        "Turn-by-turn directions\non your Pebble",
        ("nav-green-144x168.png", "nav-blue-180x180.png"),
    ),
    Composite(
        "Set it up on the watch\nor in the phone app",
        ("settings-menu-144x168.png", "nav-inverted-red-180x180.png"),
    ),
]
FEATURE_SHOT = "nav-green-200x228.png"


# --- checks (pure: size and mode only, so they are unit-tested without files) ---

def screenshot_problems(name: str, size: tuple[int, int], mode: str) -> list[str]:
    """Play's rules for a phone screenshot: sides 320-3840 px, ratio at most 2:1, no alpha."""
    width, height = size
    problems = []
    if min(width, height) < MIN_SIDE or max(width, height) > MAX_SIDE:
        problems.append(f"{name}: {width}x{height} has a side outside {MIN_SIDE}-{MAX_SIDE} px")
    if max(width, height) > MAX_RATIO * min(width, height):
        problems.append(f"{name}: {width}x{height} is longer than 2:1")
    if "A" in mode:
        problems.append(f"{name}: has an alpha channel (Play wants 24-bit PNG or JPEG)")
    return problems


def exact_size_problems(name: str, size: tuple[int, int], expected: tuple[int, int]) -> list[str]:
    return [] if size == expected else [f"{name}: {size[0]}x{size[1]}, expected {expected[0]}x{expected[1]}"]


def check(images: Path = IMAGES) -> list[str]:
    """Every problem with the store images under [images]; empty when Play would accept them."""
    problems: list[str] = []
    icon = images / "icon.png"
    if not icon.exists():
        problems.append("icon.png is missing")
    else:
        with Image.open(icon) as im:
            problems += exact_size_problems("icon.png", im.size, ICON_SIZE)
    feature = images / "featureGraphic.png"
    if not feature.exists():
        problems.append("featureGraphic.png is missing")
    else:
        with Image.open(feature) as im:
            problems += exact_size_problems("featureGraphic.png", im.size, FEATURE_SIZE)
            if "A" in im.mode:
                problems.append("featureGraphic.png: has an alpha channel (Play wants 24-bit PNG or JPEG)")
    shots = sorted((images / "phoneScreenshots").glob("*.png"))
    if len(shots) < MIN_PHONE_SCREENSHOTS:
        problems.append(f"phoneScreenshots: {len(shots)} found, Play needs at least {MIN_PHONE_SCREENSHOTS}")
    for shot in shots:
        with Image.open(shot) as im:
            problems += screenshot_problems(f"phoneScreenshots/{shot.name}", im.size, im.mode)
    return problems


# --- phone screenshots ---

def blank_rows(im: Image.Image, background: tuple[int, int, int]) -> set[int]:
    """Rows that are all [background]."""
    width = im.width
    plain = Image.new("RGB", (width, 1), background)
    return {
        y for y in range(im.height)
        if ImageChops.difference(im.crop((0, y, width, y + 1)), plain).getbbox() is None
    }


def crop_rows(height: int, target: int, blank: set[int]) -> int | None:
    """
    The rows to drop from the top, the rest coming off the bottom, for a [height] to [target] crop:
    the most top rows that drop the whole status bar, leave [MIN_TOP_MARGIN] above the first content
    and cut through blank rows only. None when no split does that.
    """
    excess = height - target
    first_content = min((y for y in range(STATUS_BAR_END, height) if y not in blank), default=height)
    for top in range(min(excess, first_content - MIN_TOP_MARGIN), STATUS_BAR_END - 1, -1):
        bottom_cut = height - (excess - top)
        if top in blank and bottom_cut in blank and bottom_cut - 1 in blank:
            return top
    return None


def scale_and_pad(im: Image.Image, background: tuple[int, int, int]) -> Image.Image:
    """Fit [im] inside the phone size, keeping its aspect ratio, centred on [background]."""
    scale = min(PHONE_SIZE[0] / im.width, PHONE_SIZE[1] / im.height)
    body = im.resize((round(im.width * scale), round(im.height * scale)), Image.LANCZOS)
    out = Image.new("RGB", PHONE_SIZE, background)
    out.paste(body, ((PHONE_SIZE[0] - body.width) // 2, (PHONE_SIZE[1] - body.height) // 2))
    return out


def fit_phone_screenshot(im: Image.Image) -> tuple[Image.Image, str]:
    """[im] as exactly 1080x2160 without cutting content, and how that was done."""
    im = im.convert("RGB")
    if im.size == PHONE_SIZE:
        return im, "copied"
    background = im.getpixel((im.width // 2, min(STATUS_BAR_END + 20, im.height - 1)))
    if im.width == PHONE_SIZE[0] and im.height > PHONE_SIZE[1]:
        top = crop_rows(im.height, PHONE_SIZE[1], blank_rows(im, background))
        if top is not None:
            out = im.crop((0, top, im.width, top + PHONE_SIZE[1]))
            return out, f"cropped {top} rows from the top and {im.height - PHONE_SIZE[1] - top} from the bottom"
        # No clean cut: drop the status bar, then scale and pad.
        return scale_and_pad(im.crop((0, STATUS_BAR_END, im.width, im.height)), background), \
            "no clean cut; status bar dropped, scaled and padded"
    return scale_and_pad(im, background), f"{im.width}x{im.height} scaled and padded"


def phone_screenshots() -> list[tuple[Image.Image, str]]:
    sources = sorted(PHONE_SOURCES.glob("*.png"))
    if not sources:
        raise SystemExit(f"no phone screens in {PHONE_SOURCES}; render them with StoreScreenshotsTest")
    shots = []
    for path in sources:
        with Image.open(path) as source:
            image, note = fit_phone_screenshot(source)
        shots.append((image, f"from {path.name}, {note}"))
    return shots


# --- composed images ---

def load_font(size: int, candidates: list[str]) -> ImageFont.FreeTypeFont | None:
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return None


def watch_shot(name: str, scale: int) -> Image.Image:
    path = WATCH / name
    if not path.exists():
        raise SystemExit(f"{path} is missing; regenerate the watch screenshots (screenshots/README.md)")
    with Image.open(path) as source:
        shot = source.convert("RGBA")
    return shot.resize((shot.width * scale, shot.height * scale), Image.NEAREST)


def is_round(name: str) -> bool:
    return name.endswith("-180x180.png")


def paste_with_bezel(canvas: Image.Image, shot: Image.Image, round_face: bool, centre_x: int, top: int, bezel: int) -> int:
    """Draw a watch bezel and the screenshot on [canvas]; returns the bottom edge."""
    draw = ImageDraw.Draw(canvas)
    left = centre_x - shot.width // 2
    box = (left - bezel, top - bezel, left + shot.width + bezel, top + shot.height + bezel)
    if round_face:
        draw.ellipse(box, fill=BEZEL)
        mask = Image.new("L", shot.size, 0)
        ImageDraw.Draw(mask).ellipse((0, 0, shot.width - 1, shot.height - 1), fill=255)
        alpha = ImageChops.multiply(shot.getchannel("A"), mask)
        canvas.paste(shot.convert("RGB"), (left, top), alpha)
    else:
        draw.rounded_rectangle(box, radius=bezel * 2, fill=BEZEL)
        canvas.paste(shot.convert("RGB"), (left, top), shot.getchannel("A"))
    return box[3]


def compose(item: Composite) -> Image.Image:
    canvas = Image.new("RGB", PHONE_SIZE, BACKGROUND)
    draw = ImageDraw.Draw(canvas)
    font = load_font(76, FONT_CANDIDATES)
    y = 120
    if font is None:
        print(f"warning: no caption font found; a composite has no caption", file=sys.stderr)
    else:
        draw.multiline_text((PHONE_SIZE[0] // 2, y), item.caption, font=font, fill=TEXT, anchor="ma", align="center", spacing=20)
        y = draw.multiline_textbbox((PHONE_SIZE[0] // 2, y), item.caption, font=font, anchor="ma", spacing=20)[3]
    shots = [watch_shot(name, 4) for name in item.shots]
    bezel = 24
    room = PHONE_SIZE[1] - y - 80
    gap = (room - sum(s.height + 2 * bezel for s in shots)) // (len(shots) + 1)
    if gap < 20:
        raise SystemExit(f"{item.shots}: the watch screenshots do not fit")
    for name, shot in zip(item.shots, shots):
        y = paste_with_bezel(canvas, shot, is_round(name), PHONE_SIZE[0] // 2, y + gap + bezel, bezel)
    return canvas


def wrap(text: str, font: ImageFont.FreeTypeFont, width: int, draw: ImageDraw.ImageDraw) -> str:
    lines, line = [], ""
    for word in text.split():
        candidate = f"{line} {word}".strip()
        if line and draw.textlength(candidate, font=font) > width:
            lines.append(line)
            line = word
        else:
            line = candidate
    return "\n".join(lines + [line])


def feature_graphic() -> Image.Image:
    canvas = Image.new("RGB", FEATURE_SIZE, BACKGROUND)
    draw = ImageDraw.Draw(canvas)
    with Image.open(IMAGES / "icon.png") as source:
        icon = source.convert("RGBA").resize((160, 160), Image.LANCZOS)
    canvas.paste(icon.convert("RGB"), (56, 56), icon.getchannel("A"))
    shot = watch_shot(FEATURE_SHOT, 2)
    paste_with_bezel(canvas, shot, False, FEATURE_SIZE[0] - 56 - 16 - shot.width // 2, (FEATURE_SIZE[1] - shot.height) // 2, 16)
    title = load_font(72, FONT_CANDIDATES)
    body = load_font(32, FONT_REGULAR_CANDIDATES)
    if title is None or body is None:
        print("warning: no caption font found; featureGraphic.png has no text", file=sys.stderr)
        return canvas
    draw.text((56, 250), "PebbleNTN", font=title, fill=TEXT)
    description = SHORT_DESCRIPTION.read_text(encoding="utf-8").strip()
    draw.multiline_text((56, 350), wrap(description, body, 440, draw), font=body, fill=TEXT_DIM, spacing=10)
    return canvas


def generate() -> None:
    images = phone_screenshots()
    images += [(compose(item), f"from {', '.join(item.shots)}") for item in COMPOSITES]
    written = set()
    PHONE.mkdir(parents=True, exist_ok=True)
    for number, (image, note) in enumerate(images, start=1):
        name = f"{number}.png"
        image.save(PHONE / name, optimize=True)
        written.add(name)
        print(f"{name}: {PHONE_SIZE[0]}x{PHONE_SIZE[1]} {note}")
    for stale in sorted(PHONE.glob("*.png")):
        if stale.name not in written:
            stale.unlink()
            print(f"{stale.name}: removed (not part of this set)")
    feature_graphic().save(IMAGES / "featureGraphic.png", optimize=True)
    print(f"featureGraphic.png: {FEATURE_SIZE[0]}x{FEATURE_SIZE[1]}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--check", action="store_true", help="validate the images against Play's rules")
    args = parser.parse_args(argv)
    if args.check:
        problems = check()
        for problem in problems:
            print(f"FAIL {problem}", file=sys.stderr)
        if problems:
            return 1
        print("Play store images OK")
        return 0
    generate()
    problems = check()
    for problem in problems:
        print(f"FAIL {problem}", file=sys.stderr)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
