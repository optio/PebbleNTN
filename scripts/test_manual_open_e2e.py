#!/usr/bin/env python3
"""Two-emulator check: with auto-launch off, a watchapp opened by hand still gets directions.

Neither emulator has a real PebbleKit Bluetooth path, so the phone<->watch hop is bridged by this
script (spec: "use a logging/fake watch transport for Android emulator tests and inject protocol
messages into the Pebble emulator"):

1. Android emulator — runs ManualOpenWithAutoLaunchOffTest: the persisted setting is off, a
   navigation instruction arrives, nothing is launched or sent; then the companion's
   "watchapp opened" callback fires and the current state is sent. The test logs that exact
   outbound message.
2. Pebble emulator — installs and opens the watchapp (the user opening it by hand; the phone never
   launched it), injects the logged message, and checks the screen changed from the waiting screen
   to rendered navigation. Both screenshots are kept for a look.

Prerequisites: a running Android emulator or device (./scripts/run-android-emulator.sh), JDK 21 on
the environment for Gradle, and the Pebble SDK (`pebble`). The Pebble emulator is started by
`pebble install` and killed at the end.

Usage: scripts/test_manual_open_e2e.py [--platform basalt|chalk|emery] [--skip-android LOGFILE]
"""

import argparse
import json
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
WATCHAPP = REPO_ROOT / "watchapp"
OUT_DIR = WATCHAPP / "build" / "e2e-manual-open"  # git-ignored
TEST_CLASS = "com.pebblentn.app.pebble.ManualOpenWithAutoLaunchOffTest"
LOG_TAG = "PebbleNtnE2E"
MARKER = "OUTBOUND "
# Share of the screen that must change once navigation renders. The waiting screen (text or the
# install QR) and the maneuver layout differ over most of the display; the clock strip alone is ~5%.
MIN_CHANGED_FRACTION = 0.15

sys.path.insert(0, str(WATCHAPP / "tools"))
from capture_screenshots import _decode_png  # noqa: E402  (minimal PNG reader, no Pillow)


def run(cmd, **kwargs):
    print("+", " ".join(str(c) for c in cmd), flush=True)
    return subprocess.run(cmd, check=True, text=True, **kwargs)


def find_adb():
    out = subprocess.run(
        ["bash", "-c", f'source "{REPO_ROOT}/scripts/lib/android-sdk.sh" && '
                       f'find_sdk_tool adb platform-tools "{REPO_ROOT}"'],
        capture_output=True, text=True)
    if out.returncode != 0 or not out.stdout.strip():
        sys.exit("ERROR: adb not found (install platform-tools; see ./scripts/bootstrap.sh)")
    return out.stdout.strip()


def android_half():
    """Run the instrumented test and return the state message it sent, as parsed JSON."""
    adb = find_adb()
    state = subprocess.run([adb, "get-state"], capture_output=True, text=True).stdout.strip()
    if state != "device":
        sys.exit("ERROR: no Android emulator/device attached (./scripts/run-android-emulator.sh)")
    run([adb, "logcat", "-c"])
    run([str(REPO_ROOT / "android" / "gradlew"), "-p", str(REPO_ROOT / "android"),
         ":app:connectedDebugAndroidTest",
         f"-Pandroid.testInstrumentationRunnerArguments.class={TEST_CLASS}"])
    log = run([adb, "logcat", "-d", "-s", f"{LOG_TAG}:I"], capture_output=True).stdout
    return parse_outbound(log)


def parse_outbound(log):
    lines = [line for line in log.splitlines() if MARKER in line]
    if not lines:
        sys.exit(f"ERROR: the Android test logged no '{MARKER.strip()}' message under {LOG_TAG}")
    return json.loads(lines[-1].split(MARKER, 1)[1])


def pebble(platform, *args):
    return run(["pebble", args[0], "--emulator", platform, *args[1:]], cwd=WATCHAPP,
               capture_output=True, timeout=240)


def screenshot(platform, name):
    path = OUT_DIR / f"{name}-{platform}.png"
    pebble(platform, "screenshot", "--no-open", str(path))
    return path


def changed_fraction(a, b):
    wa, ha, rows_a = _decode_png(a)
    wb, hb, rows_b = _decode_png(b)
    if (wa, ha) != (wb, hb):
        return 1.0
    changed = sum(
        rows_a[y][x * 4:x * 4 + 3] != rows_b[y][x * 4:x * 4 + 3]
        for y in range(ha) for x in range(wa))
    return changed / (wa * ha)


def pebble_half(platform, message):
    args = ["--int", *(f"{k}={v}" for k, v in message["int"].items())]
    if message["string"]:
        args += ["--string", *(f"{k}={v}" for k, v in message["string"].items())]

    if not list((WATCHAPP / "build").glob("*.pbw")):
        run([str(REPO_ROOT / "scripts" / "build-watchapp.sh")])
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)
    try:
        # `pebble install` opens the watchapp on the emulator: the stand-in for the user opening it
        # from the launcher, with no launch request from the phone.
        pebble(platform, "install")
        time.sleep(6)  # past the watch's 5 s no-reply watchdog, so the waiting screen is settled
        waiting = screenshot(platform, "1-waiting")

        pebble(platform, "send-app-message", *args)
        time.sleep(2)
        navigating = screenshot(platform, "2-after-phone-state")
    finally:
        subprocess.run(["pebble", "kill"], cwd=WATCHAPP, capture_output=True)

    fraction = changed_fraction(waiting, navigating)
    print(f"Screen changed over {fraction:.0%} of pixels ({waiting.name} -> {navigating.name})")
    if fraction < MIN_CHANGED_FRACTION:
        sys.exit("FAIL: the watch did not switch to navigation after the phone's state message")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--platform", default="basalt", choices=["basalt", "chalk", "emery"])
    parser.add_argument("--skip-android", metavar="LOGFILE", type=Path,
                        help="reuse a saved logcat from a previous Android run instead of running it")
    opts = parser.parse_args()

    message = parse_outbound(opts.skip_android.read_text()) if opts.skip_android else android_half()
    print("Phone sent:", json.dumps(message))
    pebble_half(opts.platform, message)
    print(f"PASS — screenshots in {OUT_DIR.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
