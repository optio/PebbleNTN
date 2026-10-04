"""Navigation apps: how to install a recent build and how to start navigation in each.

Phase 1 drives Google Maps; the open-source apps have installers already, their navigation drivers
follow in phase 3 (#55).
"""
from __future__ import annotations

import json
import re
import time
import urllib.request
from pathlib import Path

from . import ui

FIRST_RUN_LABELS = ["Skip", "SKIP", "Got it", "OK", "Dismiss", "Accept", "Continue", "Allow",
                    "While using the app", "Only this time", "No thanks"]


def _get_json(url: str):
    req = urllib.request.Request(url, headers={"User-Agent": "PebbleNTN-route-capture/1"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


class Source:
    """Where a recent APK comes from."""

    def latest(self) -> tuple[str, str]:  # (version, url)
        raise NotImplementedError


class Preinstalled(Source):
    def latest(self):
        return ("preinstalled", "")


class FDroid(Source):
    def __init__(self, package: str):
        self.package = package

    def latest(self):
        info = _get_json(f"https://f-droid.org/api/v1/packages/{self.package}")
        code = info["suggestedVersionCode"]
        return (str(code), f"https://f-droid.org/repo/{self.package}_{code}.apk")


class ReleaseAsset(Source):
    """The newest release asset matching `pattern` on GitHub or Codeberg."""

    def __init__(self, api: str, pattern: str):
        self.api, self.pattern = api, re.compile(pattern)

    def latest(self):
        for rel in _get_json(self.api):
            for asset in rel.get("assets", []):
                if self.pattern.search(asset["name"]):
                    return (rel["tag_name"], asset["browser_download_url"])
        raise LookupError(f"no asset matching {self.pattern.pattern} in {self.api}")


class App:
    app_id = ""
    package = ""
    source: Source = Preinstalled()

    def version(self, host) -> str | None:
        m = re.search(r"versionName=(\S+)", host.shell(f"dumpsys package {self.package}", check=False))
        return m.group(1) if m else None

    def install(self, host) -> str:
        """Install or update to the source's latest build; returns the installed version."""
        tag, url = self.source.latest()
        if url:
            apk = host.scratch_dir() / f"{self.package}-{re.sub(r'[^0-9A-Za-z._-]', '_', tag)}.apk"
            if not apk.exists():
                urllib.request.urlretrieve(url, apk)
            host.adb("install", "-r", "-g", host.to_host_path(apk), timeout=600)
        return self.version(host) or "not installed"

    def prepare(self, host, locale: str) -> str:
        """Permissions, then the Android system language (#55: the app must follow the system
        language; Google Maps 26.x ignores a per-app language). Returns the language now active."""
        for perm in ("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "POST_NOTIFICATIONS"):
            host.shell(f"pm grant {self.package} android.permission.{perm}", check=False)
        host.shell(f"cmd locale set-app-locales {self.package}", check=False)  # none: follow the system
        active = host.set_system_locale(locale)
        host.shell(f"am force-stop {self.package}", check=False)
        return active

    def start_navigation(self, host, destination: dict, mode: str) -> bool:
        raise NotImplementedError(f"{self.app_id}: navigation driver arrives in phase 3")

    def stop(self, host) -> None:
        host.shell(f"am force-stop {self.package}", check=False)


class GoogleMaps(App):
    app_id = "google-maps"
    package = "com.google.android.apps.maps"
    MODES = {"car": "d", "bike": "b", "foot": "w"}

    def start_navigation(self, host, destination: dict, mode: str) -> bool:
        uri = f"google.navigation:q={destination['lat']},{destination['lon']}\\&mode={self.MODES[mode]}"
        out = host.shell(f"am start -a android.intent.action.VIEW -d {uri} {self.package}", check=False)
        return "Error" not in out


class OsmAnd(App):
    app_id, package, source = "osmand", "net.osmand.plus", FDroid("net.osmand.plus")


class OrganicMaps(App):
    app_id, package = "organic-maps", "app.organicmaps.web"
    source = ReleaseAsset("https://api.github.com/repos/organicmaps/organicmaps/releases?per_page=5", r"-web-release\.apk$")


class CoMaps(App):
    app_id, package = "comaps", "app.comaps"
    source = ReleaseAsset("https://codeberg.org/api/v1/repos/comaps/comaps/releases?limit=8", r"-main-release\.apk$")


APPS = {a.app_id: a for a in (GoogleMaps(), OsmAnd(), OrganicMaps(), CoMaps())}


def dismiss_first_run(host, attempts: int = 6) -> list[str]:
    """Tap through first-run and consent dialogs by their button text; returns what was tapped."""
    tapped = []
    for _ in range(attempts):
        host.shell("uiautomator dump /data/local/tmp/routecap-ui.xml", check=False, timeout=30)
        xml = host.shell("cat /data/local/tmp/routecap-ui.xml", check=False)
        hit = ui.find(xml, FIRST_RUN_LABELS)
        if not hit:
            break
        label, (x, y) = hit
        host.shell(f"input tap {x} {y}")
        tapped.append(label)
        time.sleep(3)
    return tapped
