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

FIRST_RUN_LABELS = ["Skip", "SKIP", "SKIP DOWNLOAD", "Got it", "OK", "Dismiss", "Accept", "Continue", "Allow",
                    "While using the app", "Only this time", "No thanks",
                    "Keep active",  # OsmAnd's one-time speed-camera choice
                    "Start", "START"]
# Buttons found by resource id, which doesn't change with the language (a route preview's "Start" is
# "Démarrer" in French).
FIRST_RUN_IDS = ["net.osmand.plus:id/start_button",
                 "app.organicmaps.web:id/start", "app.comaps:id/start",
                 # A dialog's positive button (OK / Accept in any language): Organic Maps' "plan from your
                 # current location?" and its one-time route disclaimer.
                 "android:id/button1"]


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

    def storage(self) -> str:
        """The app's external files folder, where its offline maps live."""
        return f"/sdcard/Android/data/{self.package}/files"

    def provision(self, host, route: dict) -> None:
        """Make sure the device has what navigating `route` needs (offline maps); default: nothing."""

    def start_navigation(self, host, route: dict, mode: str) -> bool:
        raise NotImplementedError(f"{self.app_id}: no navigation driver yet")

    def stop(self, host) -> None:
        host.shell(f"am force-stop {self.package}", check=False)


class GoogleMaps(App):
    app_id = "google-maps"
    package = "com.google.android.apps.maps"
    MODES = {"car": "d", "bike": "b", "foot": "w"}

    def start_navigation(self, host, route: dict, mode: str) -> bool:
        destination = route["destination"]
        uri = f"google.navigation:q={destination['lat']},{destination['lon']}\\&mode={self.MODES[mode]}"
        out = host.shell(f"am start -a android.intent.action.VIEW -d {uri} {self.package}", check=False)
        return "Error" not in out


class OsmAnd(App):
    """OsmAnd routes offline: the route's region map (routes/<id>.json "maps.osmand") is downloaded
    from OsmAnd's server once, cached, and pushed into the app's storage, where it is indexed on the
    next start."""

    app_id, package, source = "osmand", "net.osmand.plus", FDroid("net.osmand.plus")
    MAP_URL = "https://download.osmand.net/download?standard=yes&file={name}.obf.zip"

    def provision(self, host, route: dict) -> None:
        name = route.get("maps", {}).get("osmand")
        if not name:
            raise LookupError(f"route {route['id']} names no OsmAnd map (maps.osmand)")
        present = host.shell(f"ls {self.storage()}", check=False).split()
        # Keep only this route's region map: a few big regions fill the emulator's data partition (a
        # push then fails). OsmAnd's own World_basemap is left alone.
        for f in present:
            if f.endswith(".obf") and not f.startswith("World_") and f != f"{name}.obf":
                host.shell(f"rm {self.storage()}/{f}", check=False)
        if f"{name}.obf" in present:
            return
        cache = host.scratch_dir() / "osmand-maps"
        cache.mkdir(exist_ok=True)
        obf = cache / f"{name}.obf"
        if not obf.exists():
            zipped = cache / f"{name}.obf.zip"
            urllib.request.urlretrieve(self.MAP_URL.format(name=name), zipped)
            import zipfile
            with zipfile.ZipFile(zipped) as z:
                z.extractall(cache)
            zipped.unlink()
        host.shell(f"am start -n {self.package}/net.osmand.plus.activities.MapActivity", check=False)  # creates storage
        time.sleep(5)
        host.adb("push", host.to_host_path(obf), f"{self.storage()}/", timeout=900)
        host.shell(f"am force-stop {self.package}", check=False)  # re-index on the next start

    def start_navigation(self, host, route: dict, mode: str) -> bool:
        # A route still active from an earlier run is resumed when OsmAnd starts and would win, so end
        # it first. OsmAnd's own osmand.api://navigate did nothing here; the google.navigation intent it
        # also handles starts from the current GPS fix and opens the route preview ("Start" is tapped
        # with the first-run labels).
        self.stop(host)
        d = route["destination"]
        uri = f"google.navigation:q={d['lat']},{d['lon']}\\&mode={GoogleMaps.MODES[mode]}"
        out = host.shell(f'am start -a android.intent.action.VIEW -d "{uri}" {self.package}', check=False)
        return "Error" not in out

    def stop(self, host) -> None:
        host.shell(f'am start -a android.intent.action.VIEW -d "osmand.api://stop_navigation" {self.package}', check=False)
        time.sleep(3)
        host.shell(f"am force-stop {self.package}", check=False)


class OrganicMaps(App):
    """Organic Maps' route URL (libs/map/mwm_url.cpp): route?sll=<lat,lon>&saddr=<name>&dll=<lat,lon>
    &daddr=<name>&type=vehicle|pedestrian|bicycle|transit, with the parameters in that order."""

    app_id, package, scheme = "organic-maps", "app.organicmaps.web", "om"
    source = ReleaseAsset("https://api.github.com/repos/organicmaps/organicmaps/releases?per_page=5", r"-web-release\.apk$")
    TYPES = {"car": "vehicle", "bike": "bicycle", "foot": "pedestrian"}

    def provision(self, host, route: dict) -> None:
        """Download the maps through the app itself, with the GPS at the route's origin: the
        first-run world map (ticking its "Download <region>?" box), then, for a region not yet on
        the device, the map a route request offers (the dialog's positive button is "Download").
        Done once the route preview shows its start button. Any language works: buttons are found
        by resource id."""
        if getattr(self, "_region_of", None) != route["id"]:
            # A new city: drop the other cities' region maps first (World*.mwm stays), or a few of
            # them fill the emulator's storage, which hung it mid-run.
            host.shell(f"am force-stop {self.package}", check=False)
            host.shell(f"for f in {self.storage()}/*/*.mwm; do case $(basename \"$f\") in World*) ;; *) rm \"$f\";; esac; done",
                       check=False)
            self._region_of = route["id"]
        o = route["origin"]
        host.adb("emu", "geo", "fix", str(o["lon"]), str(o["lat"]))
        time.sleep(2)
        rid = lambda name: f"{self.package}:id/{name}"
        host.shell(f"monkey -p {self.package} -c android.intent.category.LAUNCHER 1", check=False)
        deadline = time.time() + 900
        requested = 0.0  # when the route was last requested
        while time.time() < deadline:
            time.sleep(5)
            xml = screen(host)
            if ui.find_id(xml, [rid("start")]):
                break
            if hit := ui.find_id(xml, [rid("btn_download_resources")]):
                box = next((n for n in ui.nodes(xml) if n.get("resource-id") == rid("chb_download_country")), None)
                if box and box.get("checked") == "false":
                    tap(host, (None, box["center"]))
                    time.sleep(1)
                tap(host, hit)
                continue
            if hit := ui.find_id(xml, ["android:id/button1"]):
                tap(host, hit)
                continue
            if hit := ui.find_id(xml, ["android:id/button2"]):
                # Only a negative button: "Unable to create route", seen right after a region map
                # downloaded. A restarted app routes fine, so close it and ask again.
                tap(host, hit)
                self.start_navigation(host, route, "car")
                continue
            if time.time() - requested > 30 and ui.find_id(xml, [rid("my_position")]):
                # The world map is in: ask for the route, which offers this region's map if missing.
                # Asked again while no preview shows: a request sent while the app starts is dropped.
                self.start_navigation(host, route, "car")
                requested = time.time()
        else:
            raise RuntimeError(f"{self.app_id}: no route preview after 15 min of map downloads")
        host.shell(f"am force-stop {self.package}", check=False)

    def start_navigation(self, host, route: dict, mode: str) -> bool:
        o, d = route["origin"], route["destination"]
        q = f"sll={o['lat']},{o['lon']}&saddr=Start&dll={d['lat']},{d['lon']}&daddr=Destination&type={self.TYPES[mode]}"
        # The app ignores a route URL while it's open; it only acts on one that starts it.
        host.shell(f"am force-stop {self.package}", check=False)
        time.sleep(2)
        # Inside the double quotes the device shell takes "&" literally; escaping it would reach the app.
        out = host.shell(f'am start -a android.intent.action.VIEW -d "{self.scheme}://route?{q}" {self.package}', check=False)
        return "Error" not in out


class CoMaps(OrganicMaps):
    """A fork of Organic Maps with the same route URL, under its own cm:// scheme."""

    app_id, package, scheme = "comaps", "app.comaps", "cm"
    source = ReleaseAsset("https://codeberg.org/api/v1/repos/comaps/comaps/releases?limit=8", r"-main-release\.apk$")


APPS = {a.app_id: a for a in (GoogleMaps(), OsmAnd(), OrganicMaps(), CoMaps())}


def screen(host) -> str:
    """The current UI hierarchy as uiautomator XML."""
    host.shell("uiautomator dump /data/local/tmp/routecap-ui.xml", check=False, timeout=30)
    return host.shell("cat /data/local/tmp/routecap-ui.xml", check=False)


def tap(host, hit) -> None:
    _, (x, y) = hit
    host.shell(f"input tap {x} {y}")


def dismiss_first_run(host, attempts: int = 6, done=None) -> list[str]:
    """Tap through first-run and consent dialogs by their button text; returns what was tapped.
    Stops as soon as `done()` holds: once navigation runs, a stale dump can still show the route
    preview's start button, and tapping its spot on the navigation screen ends navigation."""
    tapped = []
    for _ in range(attempts):
        if done and done():
            break
        xml = screen(host)
        hit = ui.find_id(xml, FIRST_RUN_IDS) or ui.find(xml, FIRST_RUN_LABELS)
        if not hit:
            break
        label, (x, y) = hit
        host.shell(f"input tap {x} {y}")
        tapped.append(label)
        time.sleep(3)
    return tapped
