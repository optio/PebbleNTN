#!/usr/bin/env python3
"""Generate monochrome maneuver bitmaps (PNG) for the Pebble watchapp (REQ-WATCH-002).

Pure stdlib (zlib + struct) so it runs anywhere. Glyphs are drawn large and heavy on purpose: on a
144x168 watch, read at a glance while driving, thin strokes disappear. Arrows are one canonical up
arrow rotated to the maneuver's angle; u-turn, roundabout, arrive and unknown are distinct glyphs.

Run: python3 watchapp/tools/gen_maneuver_bitmaps.py
Output: watchapp/resources/images/*.png
"""
from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

SIZE = 64
CENTER = SIZE / 2.0
BLACK, WHITE = 0, 255
OUT_DIR = Path(__file__).resolve().parent.parent / "resources" / "images"

Canvas = list[list[int]]


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
    """Rasterize `predicate(u, v)` rotated by angle_deg about the centre."""
    theta = math.radians(-angle_deg)
    cos_t, sin_t = math.cos(theta), math.sin(theta)
    rows = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - CENTER, y - CENTER
            u = CENTER + dx * cos_t - dy * sin_t
            v = CENTER + dx * sin_t + dy * cos_t
            if predicate(u, v):
                rows[y][x] = BLACK
    return rows


def arrow(angle_deg: float) -> Canvas:
    return rotated(in_up_arrow, angle_deg)


# --- Primitives ---------------------------------------------------------------------------------


def fill_rect(rows: Canvas, x0: int, y0: int, x1: int, y1: int) -> None:
    for y in range(max(0, y0), min(SIZE, y1)):
        for x in range(max(0, x0), min(SIZE, x1)):
            rows[y][x] = BLACK


def fill_ring(rows: Canvas, cx: float, cy: float, radius: float, thickness: float,
              v_max: float = SIZE, v_min: float = -1.0) -> None:
    inner = radius - thickness
    for y in range(SIZE):
        for x in range(SIZE):
            if not (v_min <= y <= v_max):
                continue
            if inner <= math.hypot(x - cx, y - cy) <= radius:
                rows[y][x] = BLACK


def fill_disc(rows: Canvas, cx: float, cy: float, radius: float) -> None:
    for y in range(SIZE):
        for x in range(SIZE):
            if math.hypot(x - cx, y - cy) <= radius:
                rows[y][x] = BLACK


def fill_triangle(rows: Canvas, apex_x: float, apex_y: float, base_y: float, half_width: float) -> None:
    """Triangle with its apex at the top or bottom, growing towards base_y."""
    lo, hi = (apex_y, base_y) if base_y > apex_y else (base_y, apex_y)
    span = abs(base_y - apex_y)
    for y in range(int(lo), int(hi) + 1):
        if not 0 <= y < SIZE:
            continue
        half = abs(y - apex_y) / span * half_width
        for x in range(int(apex_x - half), int(apex_x + half) + 1):
            if 0 <= x < SIZE:
                rows[y][x] = BLACK


# --- Distinct glyphs ----------------------------------------------------------------------------


def uturn(mirror: bool) -> Canvas:
    """Hook up, round the top, and come back down with the arrow head pointing down."""
    rows = blank()
    # Upper half of a thick ring: the bend.
    fill_ring(rows, cx=CENTER, cy=26, radius=20, thickness=9, v_max=26)
    fill_rect(rows, int(CENTER + 11), 26, int(CENTER + 20), 56)   # right leg down to the bottom
    fill_rect(rows, int(CENTER - 20), 26, int(CENTER - 11), 34)   # left leg, short: it ends in the head
    fill_triangle(rows, apex_x=CENTER - 15.5, apex_y=61, base_y=34, half_width=17)  # head pointing down
    if mirror:
        rows = [list(reversed(row)) for row in rows]
    return rows


def roundabout() -> Canvas:
    """A ring with an entry stub at the bottom and an exit arrow leaving upwards."""
    rows = blank()
    fill_ring(rows, cx=CENTER, cy=38, radius=19, thickness=8)
    fill_rect(rows, int(CENTER - 6), 50, int(CENTER + 6), SIZE)   # entry from the bottom
    fill_rect(rows, int(CENTER - 6), 14, int(CENTER + 6), 26)     # exit stub upwards
    fill_triangle(rows, apex_x=CENTER, apex_y=2, base_y=18, half_width=16)  # exit arrow head
    return rows


def arrive() -> Canvas:
    """A target/bullseye — unmistakably not an arrow, and not a plain dot."""
    rows = blank()
    fill_ring(rows, cx=CENTER, cy=CENTER, radius=27, thickness=8)
    fill_disc(rows, CENTER, CENTER, 11)
    return rows


def unknown() -> Canvas:
    """Heavy hollow square: 'a maneuver we could not classify'."""
    rows = blank()
    fill_rect(rows, 8, 8, 56, 56)
    for y in range(17, 47):
        for x in range(17, 47):
            rows[y][x] = WHITE
    return rows


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
    "unknown": unknown,
}


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, builder in GLYPHS.items():
        write_png(OUT_DIR / f"{name}.png", builder())
        print(f"wrote {name}.png ({SIZE}x{SIZE})")


if __name__ == "__main__":
    main()
