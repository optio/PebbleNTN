"""Find on-screen elements by text in a `uiautomator dump`, to tap through first-run dialogs."""
from __future__ import annotations

import html
import re

_NODE = re.compile(r"<node [^>]*>")
_ATTR = re.compile(r'(\w[\w-]*)="([^"]*)"')


def nodes(xml: str) -> list[dict]:
    out = []
    for m in _NODE.finditer(xml):
        attrs = {k: html.unescape(v) for k, v in _ATTR.findall(m.group(0))}
        b = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", attrs.get("bounds", ""))
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            attrs["center"] = ((x1 + x2) // 2, (y1 + y2) // 2)
        out.append(attrs)
    return out


def find(xml: str, labels: list[str]) -> tuple[str, tuple[int, int]] | None:
    """The first node whose text or content-desc equals one of `labels` (in label order)."""
    all_nodes = nodes(xml)
    for label in labels:
        for n in all_nodes:
            if label in (n.get("text"), n.get("content-desc")) and "center" in n:
                return label, n["center"]
    return None


def find_id(xml: str, ids: list[str]) -> tuple[str, tuple[int, int]] | None:
    """The first node with one of these resource ids (in id order); unlike button text, an id is
    the same in every language."""
    all_nodes = nodes(xml)
    for rid in ids:
        for n in all_nodes:
            if n.get("resource-id") == rid and "center" in n:
                return rid, n["center"]
    return None


def texts(xml: str) -> list[str]:
    return [n["text"] for n in nodes(xml) if n.get("text")]
