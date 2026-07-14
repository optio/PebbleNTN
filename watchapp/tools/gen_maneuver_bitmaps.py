#!/usr/bin/env python3
"""Generate monochrome maneuver bitmaps (PNG) for the Pebble watchapp (REQ-WATCH-002, REQ-WATCH-012).

Pure stdlib (zlib + struct) so it runs anywhere. Glyphs are drawn large and heavy on purpose: on a
144x168 watch, read at a glance while driving, thin strokes disappear. Arrows are one canonical up
arrow rotated to the maneuver's angle; u-turn, roundabout, arrive and unknown are distinct glyphs.

Three built-in glyph packs are produced from one set of shapes (REQ-WATCH-012):
  - classic  — the filled shapes (written to images/*.png; the default pack).
  - bold     — the filled shapes dilated, for heavier strokes (images/bold/*.png).
  - outline  — the boundary band of each filled shape (images/outline/*.png).
The unknown glyph is a question mark, matching the "?" fallback the watch renders for an
unclassified maneuver.

Run: python3 watchapp/tools/gen_maneuver_bitmaps.py
Output: watchapp/resources/images/{,bold/,outline/}*.png
"""
from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

# Glyphs are designed in a 64x64 canonical space and rasterized at SIZE. SIZE is a third of a
# 144px screen: the watchapp draws the bitmap at its natural size (graphics_draw_bitmap_in_rect
# crops rather than scales), so the asset size *is* the on-screen size.
SIZE = 48
CANON = 64.0
CENTER = CANON / 2.0
BLACK, WHITE = 0, 255
IMAGES_DIR = Path(__file__).resolve().parent.parent / "resources" / "images"

Canvas = list[list[int]]


def to_canon(pixel: int) -> float:
    """Centre of pixel `pixel` expressed in the 64x64 design space."""
    return (pixel + 0.5) * CANON / SIZE


def blank() -> Canvas:
    return [[WHITE] * SIZE for _ in range(SIZE)]


def write_png(path: Path, pixels: Canvas) -> None:
    def chunk(typ: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + typ + data + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 0, 0, 0, 0)  # 8-bit grayscale
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type 0
        raw.extend(row)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)


# --- Canonical up arrow, drawn in (u, v) space then rotated ------------------------------------

SHAFT_HALF_WIDTH = 9.0   # 18px shaft
HEAD_HALF_WIDTH = 25.0   # 50px head at its base
HEAD_APEX_V = 3.0
HEAD_BASE_V = 34.0
SHAFT_BOTTOM_V = 61.0


def in_up_arrow(u: float, v: float) -> bool:
    if abs(u - CENTER) <= SHAFT_HALF_WIDTH and HEAD_BASE_V - 4 <= v <= SHAFT_BOTTOM_V:
        return True  # shaft (overlaps the head base slightly so they never separate)
    if HEAD_APEX_V <= v <= HEAD_BASE_V:
        half = (v - HEAD_APEX_V) / (HEAD_BASE_V - HEAD_APEX_V) * HEAD_HALF_WIDTH
        return abs(u - CENTER) <= half  # head
    return False


def rotated(predicate, angle_deg: float) -> Canvas:
    """Rasterize `predicate(u, v)` — defined in the 64x64 design space — rotated by angle_deg."""
    theta = math.radians(-angle_deg)
    cos_t, sin_t = math.cos(theta), math.sin(theta)
    rows = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = to_canon(x) - CENTER, to_canon(y) - CENTER
            u = CENTER + dx * cos_t - dy * sin_t
            v = CENTER + dx * sin_t + dy * cos_t
            if predicate(u, v):
                rows[y][x] = BLACK
    return rows


def arrow(angle_deg: float) -> Canvas:
    return rotated(in_up_arrow, angle_deg)


# --- Primitives ---------------------------------------------------------------------------------


# All primitives take coordinates in the 64x64 design space; `fill` maps them onto the raster.
def fill(rows: Canvas, predicate) -> None:
    for y in range(SIZE):
        for x in range(SIZE):
            if predicate(to_canon(x), to_canon(y)):
                rows[y][x] = BLACK


def fill_rect(rows: Canvas, x0: float, y0: float, x1: float, y1: float) -> None:
    fill(rows, lambda u, v: x0 <= u <= x1 and y0 <= v <= y1)


def fill_ring(rows: Canvas, cx: float, cy: float, radius: float, thickness: float,
              v_max: float = CANON, v_min: float = -1.0) -> None:
    inner = radius - thickness
    fill(rows, lambda u, v: v_min <= v <= v_max and inner <= math.hypot(u - cx, v - cy) <= radius)


def fill_disc(rows: Canvas, cx: float, cy: float, radius: float) -> None:
    fill(rows, lambda u, v: math.hypot(u - cx, v - cy) <= radius)


def fill_triangle(rows: Canvas, apex_x: float, apex_y: float, base_y: float, half_width: float) -> None:
    """Triangle with its apex at the top or bottom, growing towards base_y."""
    lo, hi = min(apex_y, base_y), max(apex_y, base_y)
    span = abs(base_y - apex_y)

    def inside(u: float, v: float) -> bool:
        if not lo <= v <= hi:
            return False
        half = abs(v - apex_y) / span * half_width
        return abs(u - apex_x) <= half

    fill(rows, inside)


# --- Distinct glyphs ----------------------------------------------------------------------------


def uturn(mirror: bool) -> Canvas:
    """Hook up, round the top, and come back down with the arrow head pointing down."""
    rows = blank()
    # Upper half of a thick ring: the bend.
    fill_ring(rows, cx=CENTER, cy=26, radius=20, thickness=9, v_max=26)
    fill_rect(rows, CENTER + 11, 26, CENTER + 20, 56)   # right leg down to the bottom
    fill_rect(rows, CENTER - 20, 26, CENTER - 11, 34)   # left leg, short: it ends in the head
    fill_triangle(rows, apex_x=CENTER - 15.5, apex_y=61, base_y=34, half_width=17)  # head pointing down
    if mirror:
        rows = [list(reversed(row)) for row in rows]
    return rows


def roundabout() -> Canvas:
    """A ring with an entry stub at the bottom and an exit arrow leaving upwards."""
    rows = blank()
    fill_ring(rows, cx=CENTER, cy=38, radius=19, thickness=8)
    fill_rect(rows, CENTER - 6, 50, CENTER + 6, CANON)   # entry from the bottom
    fill_rect(rows, CENTER - 6, 14, CENTER + 6, 26)     # exit stub upwards
    fill_triangle(rows, apex_x=CENTER, apex_y=2, base_y=18, half_width=16)  # exit arrow head
    return rows


def arrive() -> Canvas:
    """A target/bullseye — unmistakably not an arrow, and not a plain dot."""
    rows = blank()
    fill_ring(rows, cx=CENTER, cy=CENTER, radius=27, thickness=8)
    fill_disc(rows, CENTER, CENTER, 11)
    return rows


def question() -> Canvas:
    """A question mark: the watch's fallback for a maneuver it could not classify."""
    rows = blank()
    # Hook: the upper ~3/4 of a thick ring (open at the lower-left).
    fill(
        rows,
        lambda u, v: 8.0 <= math.hypot(u - CENTER, v - 22) <= 16.0
        and (v <= 22 or u >= CENTER),
    )
    # Neck: from the bottom of the hook down to the centre.
    fill_rect(rows, CENTER - 4, 30, CENTER + 4, 44)
    # Dot.
    fill_disc(rows, CENTER, 55, 5)
    return rows


# --- Morphology: derive the bold and outline packs from the filled (classic) shapes -------------


def _is_black(rows: Canvas, y: int, x: int) -> bool:
    return 0 <= y < SIZE and 0 <= x < SIZE and rows[y][x] == BLACK


def dilate(rows: Canvas, k: int) -> Canvas:
    """Thicken: a pixel becomes black if any pixel within Chebyshev distance k is black."""
    out = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            hit = any(
                _is_black(rows, y + dy, x + dx)
                for dy in range(-k, k + 1)
                for dx in range(-k, k + 1)
            )
            if hit:
                out[y][x] = BLACK
    return out


def erode(rows: Canvas, k: int) -> Canvas:
    """Shrink: a pixel stays black only if every pixel within Chebyshev distance k is black."""
    out = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            solid = all(
                _is_black(rows, y + dy, x + dx)
                for dy in range(-k, k + 1)
                for dx in range(-k, k + 1)
            )
            if solid:
                out[y][x] = BLACK
    return out


def outline_of(rows: Canvas, thickness: int = 4) -> Canvas:
    """Keep the boundary band of a filled shape: filled minus its eroded interior."""
    interior = erode(rows, thickness)
    out = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            if rows[y][x] == BLACK and interior[y][x] != BLACK:
                out[y][x] = BLACK
    return out


GLYPHS = {
    "straight": lambda: arrow(0),
    "slight_right": lambda: arrow(45),
    "right": lambda: arrow(90),
    "sharp_right": lambda: arrow(135),
    "slight_left": lambda: arrow(-45),
    "left": lambda: arrow(-90),
    "sharp_left": lambda: arrow(-135),
    "uturn_left": lambda: uturn(mirror=False),
    "uturn_right": lambda: uturn(mirror=True),
    "roundabout": roundabout,
    "arrive": arrive,
    "unknown": question,
}

# name -> canvas transform. classic is the shapes as drawn; bold/outline are derived.
PACKS = {
    "classic": lambda rows: rows,
    "bold": lambda rows: dilate(rows, 1),
    "outline": lambda rows: outline_of(rows, thickness=4),
}


def main() -> None:
    for pack, transform in PACKS.items():
        out_dir = IMAGES_DIR if pack == "classic" else IMAGES_DIR / pack
        out_dir.mkdir(parents=True, exist_ok=True)
        for name, builder in GLYPHS.items():
            write_png(out_dir / f"{name}.png", transform(builder()))
        print(f"wrote pack '{pack}' ({len(GLYPHS)} glyphs) -> {out_dir}")


if __name__ == "__main__":
    main()
