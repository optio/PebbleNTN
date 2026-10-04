#!/usr/bin/env python3
"""Route-capture regression harness (#55).

Drives navigation apps along fixed, synthetic routes in an Android emulator, records their
notifications, checks them against the bundled rules, and reports what was missed, optionally as a
GitHub issue per app (created, or updated if one is open).

  routecap.py setup                       Install SDK tools, emulator and system image; create the AVD.
  routecap.py install <app>...            Install a recent build of each app into the running emulator.
  routecap.py fetch-routes                Fetch and cache the track of every route and mode.
  routecap.py run <scenario.json>         Boot, drive every scenario, write captures and the report.
  routecap.py report <run.json>           Re-evaluate a saved run with the current rules.

`run` and `report` take --publish to create/update the GitHub issue (gh CLI). See README.md.
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

from routecap import apps, geo, host as hostmod, issues, notifications, report

HERE = Path(__file__).resolve().parent
ROUTES_DIR = HERE / "routes"
OUT_DIR = HERE / "out"

AVD = "PebbleNTN_Capture_API_37"
IMAGE = "system-images;android-37.0;google_apis_playstore;x86_64"


def load_route(route_id: str) -> dict:
    return json.loads((ROUTES_DIR / f"{route_id}.json").read_text())


def cmd_setup(args) -> int:
    h = hostmod.detect()
    print(f"host: {h.name}, SDK {h.sdk}")
    hostmod.ensure_cmdline_tools(h)
    missing = [pkg for pkg in ("platform-tools", "emulator", args.image) if not hostmod.installed(h, pkg)]
    if missing:
        # A running adb server locks adb.exe, which makes a platform-tools install or update fail.
        h.adb("kill-server", check=False)
        h.sdkmanager(*missing)
    print(f"SDK packages: {'installed ' + ', '.join(missing) if missing else 'all present'}")
    avd_ini = h.avd_dir() / f"{args.avd}.ini"
    if not avd_ini.exists():
        h.avdmanager("create", "avd", "-n", args.avd, "-k", args.image, "-d", "medium_phone")
    config = h.avd_dir() / f"{args.avd}.avd" / "config.ini"
    config.write_text(hostmod.configure_avd(config.read_text(), ram_mb=4096, cores=4, data_gb=8))
    print(f"AVD {args.avd} ready ({config})")
    return 0


def cmd_install(args) -> int:
    h = hostmod.detect()
    for app_id in args.apps:
        print(f"{app_id}: {apps.APPS[app_id].install(h)}")
    return 0


def cmd_fetch_routes(args) -> int:
    for path in sorted(ROUTES_DIR.glob("*.json")):
        route = json.loads(path.read_text())
        for mode in route["modes"]:
            target = geo.cache_path(ROUTES_DIR, route["id"], mode)
            if target.exists() and not args.refresh:
                continue
            track = geo.fetch_track(route["origin"], route["destination"], mode)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(track, indent=1) + "\n")
            print(f"{route['id']}.{mode}: {track['distanceMeters']} m, {len(track['coordinates'])} points")
    return 0


def boot(h, avd: str) -> None:
    if h.booted():
        print("emulator already running")
        return
    print(f"booting {avd} on {h.name} ...")
    h.start_emulator(avd)
    h.adb("wait-for-device", timeout=600)
    h.wait_boot()
    h.shell("svc power stayon true", check=False)
    h.shell("input keyevent KEYCODE_WAKEUP", check=False)
    h.shell("wm dismiss-keyguard", check=False)


def drive(h, scenario: dict, max_seconds: int | None) -> dict:
    """Run one scenario: start navigation, walk the track, record the app's distinct notifications."""
    app = apps.APPS[scenario["app"]]
    route = load_route(scenario["route"])
    track = geo.walk(geo.load_track(ROUTES_DIR, route["id"], scenario["mode"]), geo.SPEEDS[scenario["mode"]])
    if max_seconds:
        track = track[:max_seconds]
    active = app.prepare(h, scenario["locale"])
    if not same_language(active, scenario["locale"]):
        print(f"  FAILED: system language is {active!r}, not {scenario['locale']}")
        return {**scenario, "failed": f"language {active!r} instead of {scenario['locale']}", "captures": []}
    origin = track[0]
    h.adb("emu", "geo", "fix", f"{origin[0]:.6f}", f"{origin[1]:.6f}")
    time.sleep(2)

    # A freshly booted image needs a minute before the app accepts the navigation intent; first-run
    # dialogs then sit in front of navigation, so dismiss them and ask again.
    for _ in range(10):
        if app.start_navigation(h, route["destination"], scenario["mode"]):
            break
        time.sleep(15)
    else:
        raise RuntimeError(f"{app.app_id} would not start navigation")
    time.sleep(8)
    tapped = apps.dismiss_first_run(h)
    if tapped:
        print(f"  dismissed first-run dialogs: {', '.join(tapped)}")
        h.adb("emu", "geo", "fix", f"{origin[0]:.6f}", f"{origin[1]:.6f}")
        app.start_navigation(h, route["destination"], scenario["mode"])
        time.sleep(8)
        apps.dismiss_first_run(h)

    captures, seen = [], set()
    for tick, (lon, lat) in enumerate(track):
        start = time.time()
        h.adb("emu", "geo", "fix", f"{lon:.6f}", f"{lat:.6f}", check=False)
        for rec in notifications.parse_dumpsys(h.shell("dumpsys notification --noredact", check=False), app.package):
            k = notifications.key(rec)
            if any(k) and k not in seen:
                seen.add(k)
                captures.append({**rec, "tick": tick})
        time.sleep(max(0.0, 1.0 - (time.time() - start)))
    app.stop(h)
    print(f"  {scenario['id']}: {len(captures)} distinct notifications over {len(track)} s")
    result = {**scenario, "appVersion": app.version(h), "seconds": len(track), "systemLocale": active, "captures": captures}
    if not captures:
        result["failed"] = "no notifications: navigation did not start"
        print(f"  FAILED: {result['failed']}")
    return result


def same_language(active: str, wanted: str) -> bool:
    return active.split("-")[0].lower() == wanted.split("-")[0].lower()


def warm_up(h, app_ids: set[str]) -> None:
    """In English, start each app once and dismiss its first-run dialogs, so they never show up in a
    scenario's language, where the harness doesn't know the button labels."""
    for app_id in sorted(app_ids):
        app = apps.APPS[app_id]
        app.prepare(h, "en-US")
        h.adb("emu", "geo", "fix", "4.352500", "50.846700")
        for _ in range(10):
            if app.start_navigation(h, {"lat": 50.84, "lon": 4.392}, "car"):
                break
            time.sleep(15)
        time.sleep(8)
        tapped = apps.dismiss_first_run(h)
        print(f"warm-up {app_id}: {'dismissed ' + ', '.join(tapped) if tapped else 'no first-run dialogs'}")
        app.stop(h)


def write_report(run: dict, run_dir: Path, publish: bool) -> int:
    failed = [sc for sc in run["scenarios"] if sc.get("failed")]
    for sc in failed:
        print(f"not reported, failed: {sc['id']} ({sc['failed']})")
    by_app: dict[str, list] = {}
    for sc in run["scenarios"]:
        if not sc.get("failed"):
            by_app.setdefault(sc["app"], []).append(sc)
    parts = []
    for app_id, scs in by_app.items():
        run_state = report.evaluate({**run, "scenarios": scs})
        name = next(a["displayName"] for a in report.catalog()["apps"] if a["appId"] == app_id)
        parts.append(f"# {name}\n\n{report.markdown(name, run_state)}")
        (run_dir / f"summary-{app_id}.json").write_text(json.dumps(run_state, indent=1, ensure_ascii=False) + "\n")
        result = issues.publish(app_id, name, run_state, run["startedAt"], dry_run=not publish)
        total = sum(s["total"] for s in run_state["scenarios"].values())
        matched = sum(s["matched"] for s in run_state["scenarios"].values())
        print(f"{name}: {matched}/{total} recognised, {len(report.unrecognised(run_state))} unrecognised shape(s); issue: {result}")
    if failed:
        parts.append("Failed scenarios (not reported): " + ", ".join(f"`{sc['id']}` ({sc['failed']})" for sc in failed))
    (run_dir / "report.md").write_text("\n\n".join(parts) + "\n", encoding="utf-8")
    (run_dir / "run.json").write_text(json.dumps(run, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"report: {run_dir / 'report.md'}")
    return 0


def cmd_run(args) -> int:
    plan = json.loads(Path(args.scenarios).read_text())
    h = hostmod.detect()
    boot(h, args.avd)
    run = {
        "plan": plan["id"],
        "startedAt": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
        "host": h.name,
        "android": h.shell("getprop ro.build.version.release").strip(),
        "image": args.avd,
        "scenarios": [],
    }
    try:
        warm_up(h, {s["app"] for s in plan["scenarios"]})
        for scenario in plan["scenarios"]:
            print(f"scenario {scenario['id']}")
            run["scenarios"].append(drive(h, scenario, args.max_seconds))
    finally:
        h.set_system_locale("en-US")
        if not args.keep_emulator:
            h.stop_emulator()
    run_dir = OUT_DIR / datetime.now().strftime("%Y%m%d-%H%M%S")
    run_dir.mkdir(parents=True, exist_ok=True)
    return write_report(run, run_dir, args.publish)


def cmd_report(args) -> int:
    path = Path(args.run)
    return write_report(json.loads(path.read_text()), path.parent, args.publish)


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("setup"); s.add_argument("--avd", default=AVD); s.add_argument("--image", default=IMAGE); s.set_defaults(func=cmd_setup)
    s = sub.add_parser("install"); s.add_argument("apps", nargs="+", choices=sorted(apps.APPS)); s.set_defaults(func=cmd_install)
    s = sub.add_parser("fetch-routes"); s.add_argument("--refresh", action="store_true"); s.set_defaults(func=cmd_fetch_routes)
    s = sub.add_parser("run"); s.add_argument("scenarios"); s.add_argument("--avd", default=AVD)
    s.add_argument("--publish", action="store_true", help="create/update the GitHub issue per app")
    s.add_argument("--keep-emulator", action="store_true"); s.add_argument("--max-seconds", type=int)
    s.set_defaults(func=cmd_run)
    s = sub.add_parser("report"); s.add_argument("run"); s.add_argument("--publish", action="store_true"); s.set_defaults(func=cmd_report)
    args = p.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
