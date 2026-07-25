#!/usr/bin/env python3
"""Generate the "install the companion app" QR bitmap shown when the watch cannot reach the phone.

The watchapp shows this QR (REQ-WATCH-017) after the connection handshake times out on a watch
whose Pebble mobile app IS connected but which never hears back from the PebbleNTN Android app —
the tell-tale of the companion app not being installed. The QR points at the GitHub releases page
where the APK lives.

The encoded URL is fixed, so the QR never changes: this tool is run once and its output committed
as a static resource. It is NOT part of the build — the build consumes the committed PNG, so CI
needs no QR library. Re-run it only if QR_URL changes.

Encoding: 43-byte URL -> QR version 3, error-correction level L (29x29 modules). Version 3 is the
smallest that holds the URL, which keeps the modules as large as possible so the code stays
scannable on a 144 px watch. No quiet zone is baked in: the watchapp draws the code on its own white
backing panel (theme-independent), so the module bitmap is pure black/white at MODULE_PX per module.

Output: watchapp/resources/images/connect_qr.png (8-bit grayscale, matching gen_maneuver_bitmaps).
"""

import struct
import sys
import zlib
from pathlib import Path

QR_URL = "https://github.com/optio/PebbleNTN/releases"
MODULE_PX = 4  # on-screen pixels per QR module; 29 * 4 = 116 px, fits the round chalk screen
BLACK, WHITE = 0, 255
OUT = Path(__file__).resolve().parent.parent / "resources" / "images" / "connect_qr.png"


def load_pyqrcode():
    """Import pyqrcode, falling back to the uv sdist cache if it is not installed.

    pyqrcode is a pure-Python, dependency-free QR encoder; using a real library rather than a
    hand-rolled encoder guarantees the committed code is correct. It only has to be importable on
    the machine that regenerates the asset, never in CI.
    """
    try:
        import pyqrcode  # noqa: F401
        return pyqrcode
    except ImportError:
        pass
    cache = Path.home() / ".cache" / "uv" / "sdists-v9" / "pypi" / "pyqrcode"
    for init in cache.glob("*/*/src/pyqrcode/__init__.py"):
        sys.path.insert(0, str(init.parents[1]))
        import pyqrcode  # noqa: F811
        return pyqrcode
    raise SystemExit(
        "pyqrcode not found. Install it (pip install pyqrcode) and re-run; the build itself does "
        "not need it — only this one-time asset generation does."
    )


def qr_matrix():
    pyqrcode = load_pyqrcode()
    code = pyqrcode.create(QR_URL, error="L", version=3)
    # pyqrcode.code is a list of rows of 0/1 (1 = dark module), no quiet zone.
    matrix = code.code
    n = len(matrix)
    if n != 29:
        raise SystemExit(f"expected a 29x29 (version 3) code, got {n}x{n}")
    return matrix


def write_png(path, pixels, size):
    """Write an 8-bit grayscale PNG (color type 0), matching gen_maneuver_bitmaps.write_png."""
    def chunk(typ, data):
        return struct.pack(">I", len(data)) + typ + data + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 0, 0, 0, 0)
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type 0
        raw.extend(row)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)


def main():
    matrix = qr_matrix()
    n = len(matrix)
    size = n * MODULE_PX
    pixels = [[WHITE] * size for _ in range(size)]
    for my, row in enumerate(matrix):
        for mx, dark in enumerate(row):
            if not dark:
                continue
            for py in range(my * MODULE_PX, (my + 1) * MODULE_PX):
                line = pixels[py]
                for px in range(mx * MODULE_PX, (mx + 1) * MODULE_PX):
                    line[px] = BLACK
    OUT.parent.mkdir(parents=True, exist_ok=True)
    write_png(OUT, pixels, size)
    print(f"wrote {OUT} ({size}x{size}, {n}x{n} modules @ {MODULE_PX}px) for {QR_URL!r}")


if __name__ == "__main__":
    main()
