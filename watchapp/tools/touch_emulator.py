#!/usr/bin/env python3
"""Send taps and swipes to the emery (Pebble Time 2) emulator, and check touch in the watchapp (#53).

The emulator's touchscreen follows the VNC pointer: start it with `--vnc` (every later `pebble`
command needs `--vnc` too, or it restarts the emulator without it) and this script talks RFB to it,
with the standard library only. The QEMU monitor's `mouse_move` doesn't move the touch point.

  tap      pointer down and up within 300 ms
  swipe    at least 30 px within 300 ms (sent as several moves while the button is down)

Usage:
  python3 tools/touch_emulator.py tap X Y
  python3 tools/touch_emulator.py swipe X1 Y1 X2 Y2
  python3 tools/touch_emulator.py check      # REQ-WATCH-021: install, touch, screenshot each step

`check` prints a screenshot path per step for review; #12 can reuse `tap`/`swipe`.

Touch reaches apps only while touch navigation is on in the watch's system settings. The SDK 4.33.1
emery emulator has no Settings app to turn it on, and `pebble wipe` resets the flash where it's
stored, after which nothing reacts to touch, not even the launcher. So this script never wipes:
keep an emulator whose flash has touch navigation on (`info mice` on the QEMU monitor shows
"Pebble Touch (absolute)" either way; it says nothing about the setting).
"""

import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
WATCHAPP = REPO_ROOT / "watchapp"
OUT_DIR = REPO_ROOT / "watchapp" / "build" / "touch-check"
VNC_PORT = 5901  # display :1, as the Pebble emulator opens it
WIDTH, HEIGHT = 200, 228  # emery


class Vnc:
    """The smallest RFB 3.8 client that can move the pointer: no-auth handshake, PointerEvents."""

    def __init__(self, host: str = "127.0.0.1", port: int = VNC_PORT):
        self.sock = socket.create_connection((host, port), timeout=10)
        version = self._read(12)
        if not version.startswith(b"RFB "):
            raise RuntimeError(f"not a VNC server: {version!r}")
        self.sock.sendall(b"RFB 003.008\n")
        count = self._read(1)[0]
        if count == 0:
            raise RuntimeError("VNC server refused the connection")
        types = self._read(count)
        if 1 not in types:  # 1 = no authentication
            raise RuntimeError(f"VNC server wants authentication {list(types)}")
        self.sock.sendall(bytes([1]))
        if struct.unpack(">I", self._read(4))[0] != 0:
            raise RuntimeError("VNC security handshake failed")
        self.sock.sendall(bytes([1]))  # ClientInit: shared
        w, h = struct.unpack(">HH", self._read(4))
        self._read(16)  # pixel format
        self._read(struct.unpack(">I", self._read(4))[0])  # desktop name
        self.size = (w, h)

    def _read(self, n: int) -> bytes:
        data = b""
        while len(data) < n:
            chunk = self.sock.recv(n - len(data))
            if not chunk:
                raise RuntimeError("VNC connection closed")
            data += chunk
        return data

    def pointer(self, x: int, y: int, down: bool) -> None:
        self.sock.sendall(struct.pack(">BBHH", 5, 1 if down else 0, max(0, x), max(0, y)))

    def tap(self, x: int, y: int) -> None:
        self.pointer(x, y, False)
        time.sleep(0.05)
        self.pointer(x, y, True)
        time.sleep(0.08)
        self.pointer(x, y, False)

    def swipe(self, x1: int, y1: int, x2: int, y2: int, steps: int = 6) -> None:
        self.pointer(x1, y1, False)
        time.sleep(0.05)
        self.pointer(x1, y1, True)
        for i in range(1, steps + 1):
            time.sleep(0.025)
            self.pointer(x1 + (x2 - x1) * i // steps, y1 + (y2 - y1) * i // steps, True)
        time.sleep(0.02)
        self.pointer(x2, y2, False)

    def close(self) -> None:
        self.sock.close()


def pebble(*args: str, check: bool = True) -> str:
    cmd = ["pebble", *args, "--emulator", "emery", "--vnc"]
    r = subprocess.run(cmd, cwd=WATCHAPP, capture_output=True, text=True, timeout=180)
    if check and r.returncode != 0:
        raise RuntimeError(f"{' '.join(cmd)} failed: {r.stderr.strip() or r.stdout.strip()}")
    return r.stdout


def screenshot(name: str) -> Path:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / f"{name}.png"
    pebble("screenshot", "--no-open", str(path))
    return path


def check() -> int:
    """REQ-WATCH-021 on emery: menu scroll, tap-to-highlight, tap-to-activate, swipe-right back,
    About scroll, and the navigation screen surviving a right swipe."""
    pbw = WATCHAPP / "build" / "watchapp.pbw"
    if not pbw.exists():
        print(f"build the watchapp first: {pbw} is missing")
        return 1
    pebble("install", str(pbw))  # no wipe: it would turn touch navigation off (see the docstring)
    time.sleep(4)
    vnc = Vnc()
    cx, cy = WIDTH // 2, HEIGHT // 2
    steps = [
        ("01-navigation", None),
        ("02-nav-swipe-right-stays-open", ("swipe", 40, cy, 170, cy)),
        ("03-settings-opened-by-select", ("button", "select")),
        ("04-menu-swipe-up-scrolls", ("swipe", cx, 180, cx, 60)),
        ("05-menu-tap-highlights-row", ("tap", cx, 60)),
        ("06-menu-tap-again-activates", ("tap", cx, 60)),
        ("07-sublist-swipe-right-back", ("swipe", 40, cy, 170, cy)),
        ("08-menu-swipe-right-back-to-nav", ("swipe", 40, cy, 170, cy)),
    ]
    for name, action in steps:
        if action:
            kind, *rest = action
            if kind == "button":
                pebble("emu-button", "click", rest[0])
            elif kind == "tap":
                vnc.tap(*rest)
            else:
                vnc.swipe(*rest)
            time.sleep(1.5)
        print(screenshot(name))
    vnc.close()
    return 0


def main() -> int:
    if len(sys.argv) >= 2 and sys.argv[1] == "check":
        return check()
    if len(sys.argv) == 4 and sys.argv[1] == "tap":
        v = Vnc(); v.tap(int(sys.argv[2]), int(sys.argv[3])); v.close(); return 0
    if len(sys.argv) == 6 and sys.argv[1] == "swipe":
        v = Vnc(); v.swipe(*map(int, sys.argv[2:6])); v.close(); return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main())
