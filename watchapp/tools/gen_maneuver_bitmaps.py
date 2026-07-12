#!/usr/bin/env python3
"""Generate monochrome maneuver bitmaps (PNG) for the Pebble watchapp (REQ-WATCH-002).

Pure stdlib (zlib + struct) so it runs anywhere. Arrows are the same canonical up-arrow rotated to
the maneuver's angle; roundabout / arrive / unknown / u-turn use distinct glyphs. These are simple
functional placeholders that can be replaced with hand-drawn art later without code changes.

Run: python3 watchapp/tools/gen_maneuver_bitmaps.py
Output: watchapp/resources/images/*.png
"""
from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

SIZE = 48
CENTER = SIZE / 2.0
OUT_DIR = Path(__file__).resolve().parent.parent / "resources" / "images"


def write_png(path: Path, pixels: list[list[int]]) -> None:
    def chunk(typ: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + typ + data + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 0, 0, 0, 0)  # 8-bit grayscale
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type 0
        raw.extend(row)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)


def in_up_arrow(u: float, v: float) -> bool:
    # Shaft.
    if 20 <= u <= 28 and 20 <= v <= 44:
        return True
    # Head: triangle narrowing from base (y=24) to apex (y=6).
    if 6 <= v <= 24:
        half_width = (v - 6) / 18.0 * 15.0
        if abs(u - CENTER) <= half_width:
            return True
    return False


def rotated_arrow(angle_deg: float) -> list[list[int]]:
    theta = math.radians(angle_deg)
    cos_t, sin_t = math.cos(-theta), math.sin(-theta)
    rows = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            dx, dy = x - CENTER, y - CENTER
            u = CENTER + dx * cos_t - dy * sin_t
            v = CENTER + dx * sin_t + dy * cos_t
            row.append(0 if in_up_arrow(u, v) else 255)
        rows.append(row)
    return rows


def disc(radius: float, ring: float | None = None) -> list[list[int]]:
    rows = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            d = math.hypot(x - CENTER, y - CENTER)
            black = d <= radius if ring is None else (radius - ring) <= d <= radius
            row.append(0 if black else 255)
        rows.append(row)
    return rows


def roundabout() -> list[list[int]]:
    rows = disc(17, ring=4)  # ring
    # Add a short exit stub pointing up.
    for y in range(4, 16):
        for x in range(22, 26):
            rows[y][x] = 0
    return rows


def hollow_square() -> list[list[int]]:
    rows = [[255] * SIZE for _ in range(SIZE)]
    for y in range(8, 40):
        for x in range(8, 40):
            if x < 11 or x > 36 or y < 11 or y > 36:
                rows[y][x] = 0
    return rows


GLYPHS = {
    "straight": lambda: rotated_arrow(0),
    "slight_right": lambda: rotated_arrow(35),
    "right": lambda: rotated_arrow(90),
    "sharp_right": lambda: rotated_arrow(135),
    "slight_left": lambda: rotated_arrow(-35),
    "left": lambda: rotated_arrow(-90),
    "sharp_left": lambda: rotated_arrow(-135),
    "uturn_left": lambda: rotated_arrow(180),
    "uturn_right": lambda: rotated_arrow(180),
    "roundabout": roundabout,
    "arrive": lambda: disc(13),
    "unknown": hollow_square,
}


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, builder in GLYPHS.items():
        write_png(OUT_DIR / f"{name}.png", builder())
        print(f"wrote {name}.png")


if __name__ == "__main__":
    main()
