"""Routes: fetch a track from the public OSM routers, cache it in the repo, and walk it at a speed.

The track is fetched once per route and mode and committed under routes/cache/, so runs are
reproducible and need no router access.
"""
from __future__ import annotations

import json
import math
import urllib.request
from pathlib import Path

# FOSSGIS-hosted OSRM instances with one profile each (https://routing.openstreetmap.de).
ROUTERS = {
    "car": "https://routing.openstreetmap.de/routed-car/route/v1/driving",
    "bike": "https://routing.openstreetmap.de/routed-bike/route/v1/driving",
    "foot": "https://routing.openstreetmap.de/routed-foot/route/v1/driving",
}

# Simulated travel speed per mode, in metres per second.
SPEEDS = {"car": 11.0, "bike": 5.0, "foot": 1.6}

EARTH_M_PER_DEG = 111_320.0


def distance_m(a: tuple[float, float], b: tuple[float, float]) -> float:
    """Approximate distance between two (lon, lat) points; plenty for city-scale steps."""
    dx = (b[0] - a[0]) * EARTH_M_PER_DEG * math.cos(math.radians((a[1] + b[1]) / 2))
    dy = (b[1] - a[1]) * EARTH_M_PER_DEG
    return math.hypot(dx, dy)


def walk(points: list[tuple[float, float]], step_m: float) -> list[tuple[float, float]]:
    """Points every `step_m` metres along the polyline, starting at its first point and ending at
    its last, so feeding one per second moves at `step_m` m/s."""
    if not points:
        return []
    if step_m <= 0:
        raise ValueError("step_m must be positive")
    out = [points[0]]
    carry = 0.0  # metres already travelled since the last emitted point
    for a, b in zip(points, points[1:]):
        seg = distance_m(a, b)
        t = step_m - carry
        while seg > 0 and t <= seg:
            f = t / seg
            out.append((a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f))
            t += step_m
        carry = seg - (t - step_m)
    if out[-1] != points[-1]:
        out.append(points[-1])
    return out


def cache_path(routes_dir: Path, route_id: str, mode: str) -> Path:
    return routes_dir / "cache" / f"{route_id}.{mode}.json"


def fetch_track(origin: dict, destination: dict, mode: str, timeout: float = 30.0) -> dict:
    """Ask the OSM router for the track from origin to destination ({"lat", "lon"})."""
    url = (f"{ROUTERS[mode]}/{origin['lon']},{origin['lat']};{destination['lon']},{destination['lat']}"
           "?overview=full&geometries=geojson")
    req = urllib.request.Request(url, headers={"User-Agent": "PebbleNTN-route-capture/1 (+https://github.com/optio/PebbleNTN)"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        data = json.load(resp)
    route = data["routes"][0]
    return {
        "mode": mode,
        "distanceMeters": round(route["distance"]),
        "durationSeconds": round(route["duration"]),
        "source": url.split("?")[0],
        "coordinates": [[round(lon, 6), round(lat, 6)] for lon, lat in route["geometry"]["coordinates"]],
    }


def load_track(routes_dir: Path, route_id: str, mode: str) -> list[tuple[float, float]]:
    path = cache_path(routes_dir, route_id, mode)
    if not path.exists():
        raise FileNotFoundError(f"no cached track {path}; run: routecap.py fetch-routes")
    return [tuple(p) for p in json.loads(path.read_text())["coordinates"]]
