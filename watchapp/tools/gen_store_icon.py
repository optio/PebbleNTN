#!/usr/bin/env python3
"""Render the PebbleNTN store icon (white turn-right arrow on the brand green) at any size.

Pure stdlib: draws at 4x with coverage supersampling for smooth edges, then downsamples and
writes an RGBA PNG. The glyph is the app's signature maneuver arrow — a shaft that turns 90°
to the right and ends in a triangular head — so the icon reads as turn-by-turn navigation.

Run: python3 watchapp/tools/gen_store_icon.py
Output: watchapp/store/icon-{80,144}.png
"""
import struct, zlib, math
from pathlib import Path

STORE_DIR = Path(__file__).resolve().parent.parent / "store"

SS = 4                       # supersample factor
BG   = (0, 160, 66)          # brand green (Islamic-green family), slightly friendlier
BG2  = (0, 132, 52)          # a touch darker for a subtle vertical shade
FG   = (255, 255, 255)       # arrow

def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))

def make(size):
    N = size * SS
    r = 0.235 * N            # corner radius
    # Arrow geometry in the NxN canvas.
    sw   = 0.150 * N         # stroke (shaft) half handled below; this is full width
    vx   = 0.360 * N         # vertical-shaft centre x
    v_bot= 0.800 * N         # shaft bottom
    turn_y = 0.430 * N       # height of the horizontal run (its centre)
    h_end  = 0.590 * N       # where the shaft ends and the head begins
    head_apex_x = 0.820 * N
    head_half   = 0.150 * N  # half-height of the arrowhead base

    half = sw / 2.0

    def in_round_rect(x, y):
        # rounded square covering the whole canvas
        lo, hi = 0.0, float(N)
        cx = min(max(x, lo + r), hi - r)
        cy = min(max(y, lo + r), hi - r)
        # inside if within the inset rect, or within radius of the nearest corner centre
        if r <= x <= N - r or r <= y <= N - r:
            return 0 <= x <= N and 0 <= y <= N and (
                (r <= x <= N - r) or (r <= y <= N - r))
        return math.hypot(x - cx, y - cy) <= r

    def in_arrow(x, y):
        # vertical shaft
        if abs(x - vx) <= half and (turn_y - half) <= y <= v_bot:
            return True
        # horizontal run
        if abs(y - turn_y) <= half and (vx - half) <= x <= h_end:
            return True
        # triangular head, pointing right
        if h_end - half <= x <= head_apex_x:
            t = (head_apex_x - x) / (head_apex_x - (h_end - half))
            return abs(y - turn_y) <= head_half * t
        return False

    px = bytearray()
    for oy in range(size):
        for ox in range(size):
            ar = ag = ab = aa = 0
            for sy in range(SS):
                for sx in range(SS):
                    x = ox * SS + sx + 0.5
                    y = oy * SS + sy + 0.5
                    if not in_round_rect(x, y):
                        continue  # transparent outside the rounded square
                    if in_arrow(x, y):
                        col = FG
                    else:
                        col = lerp(BG, BG2, y / N)
                    ar += col[0]; ag += col[1]; ab += col[2]; aa += 255
            n = SS * SS
            px += bytes((ar // n, ag // n, ab // n, aa // n))
    return png_bytes(size, size, bytes(px))

def make_screen(w, h):
    """Full-bleed splash sized to a Pebble screen (e.g. 144x168): opaque green edge-to-edge with the
    arrow centred. Used for the store screenshot slot, which is the watch's own resolution."""
    W, H = w * SS, h * SS
    N = min(W, H)                # the arrow lives in a centred square of this side
    ox_off = (W - N) // 2
    oy_off = (H - N) // 2

    sw   = 0.150 * N
    vx   = 0.360 * N
    v_bot= 0.800 * N
    turn_y = 0.430 * N
    h_end  = 0.590 * N
    head_apex_x = 0.820 * N
    head_half   = 0.150 * N
    half = sw / 2.0

    def in_arrow(x, y):
        x -= ox_off; y -= oy_off
        if abs(x - vx) <= half and (turn_y - half) <= y <= v_bot:
            return True
        if abs(y - turn_y) <= half and (vx - half) <= x <= h_end:
            return True
        if h_end - half <= x <= head_apex_x:
            t = (head_apex_x - x) / (head_apex_x - (h_end - half))
            return abs(y - turn_y) <= head_half * t
        return False

    px = bytearray()
    for oy in range(h):
        for ox in range(w):
            ar = ag = ab = 0
            for sy in range(SS):
                for sx in range(SS):
                    x = ox * SS + sx + 0.5
                    y = oy * SS + sy + 0.5
                    col = FG if in_arrow(x, y) else lerp(BG, BG2, y / H)
                    ar += col[0]; ag += col[1]; ab += col[2]
            n = SS * SS
            px += bytes((ar // n, ag // n, ab // n, 255))  # fully opaque
    return png_bytes(w, h, bytes(px))

def png_bytes(w, h, rgba):
    def chunk(typ, data):
        return struct.pack(">I", len(data)) + typ + data + struct.pack(">I", zlib.crc32(typ + data) & 0xffffffff)
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)  # RGBA
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw += rgba[y * w * 4:(y + 1) * w * 4]
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")

if __name__ == "__main__":
    STORE_DIR.mkdir(parents=True, exist_ok=True)
    # Square rounded icons for the store's icon slots.
    for size in (80, 144):
        out = STORE_DIR / f"icon-{size}.png"
        out.write_bytes(make(size))
        print("wrote", out)
    # Full-bleed splashes at each Pebble screen resolution (aplite/basalt/diorite, chalk, emery).
    for w, h in ((144, 168), (180, 180), (200, 228)):
        out = STORE_DIR / f"splash-{w}x{h}.png"
        out.write_bytes(make_screen(w, h))
        print("wrote", out)
