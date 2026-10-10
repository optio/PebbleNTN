"""Read posted notifications from `dumpsys notification --noredact` output.

Polling dumpsys needs no app on the device and shows the same extras PebbleNTN reads (title, text,
subText, bigText), plus the template and channel that tell Google Maps' card layouts apart.
"""
from __future__ import annotations

import re

_EXTRA = re.compile(r"^\s*android\.(title|text|subText|bigText|template|summaryText|infoText)=\S+ \((.*)\)\s*$")
# A value with line breaks (OsmAnd's bigText: the instruction, then the trip summary) opens on the
# extra's line and closes with ")" on a later one.
_EXTRA_OPEN = re.compile(r"^\s*android\.(title|text|subText|bigText|template|summaryText|infoText)=\S+ \((.*)$")
_ANY_EXTRA = re.compile(r"^\s*android\.\w+=")
_CATEGORY = re.compile(r"category=(\S+)")

FIELDS = ("title", "text", "subText", "bigText")


def parse_dumpsys(output: str, package: str | None = None) -> list[dict]:
    """One dict per notification record (optionally only `package`), with the text extras present."""
    records: list[dict] = []
    current: dict | None = None
    pending: tuple[str, list[str]] | None = None  # a multi-line value being read: (field, lines)
    for line in output.splitlines():
        if pending is not None:
            field, parts = pending
            if _ANY_EXTRA.match(line) or re.match(r"^\s*NotificationRecord\(", line):
                pending = None  # never closed: keep what was read, then handle this line normally
                if current is not None and field not in current:
                    current[field] = "\n".join(parts) or None
            elif line.rstrip().endswith(")"):
                parts.append(line.rstrip()[:-1])
                if current is not None and field not in current:
                    current[field] = "\n".join(parts) or None
                pending = None
                continue
            else:
                parts.append(line)
                continue
        m = re.match(r"^\s*NotificationRecord\(0x[0-9a-f]+: pkg=(\S+)", line)
        if m:
            current = {"packageName": m.group(1)}
            ch = re.search(r"channel=(\S+)", line)
            if ch:
                current["channelId"] = ch.group(1)
            cat = _CATEGORY.search(line)
            if cat:
                current["category"] = cat.group(1)
            records.append(current)
            continue
        if current is None:
            continue
        e = _EXTRA.match(line)
        if e and e.group(1) not in current:
            value = e.group(2)
            if e.group(1) == "template":
                value = value.rsplit("$", 1)[-1]
            current[e.group(1)] = value if value != "" else None
            continue
        o = _EXTRA_OPEN.match(line)
        if o and o.group(1) not in current:
            pending = (o.group(1), [o.group(2)])
    if package:
        records = [r for r in records if r["packageName"] == package]
    return records


def key(record: dict) -> tuple:
    """Identity of a card's content, to keep each distinct notification once."""
    return tuple(record.get(f) for f in FIELDS)
