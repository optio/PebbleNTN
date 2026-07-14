#!/usr/bin/env python3
"""Render a Markdown changelog for a git revision range, grouped by Conventional Commit type.

    scripts/changelog.py v0.1.0..HEAD
    scripts/changelog.py            # everything since the last tag, or all history if untagged

Commits whose subject does not parse as a Conventional Commit still appear, under "Other" — a
changelog that silently drops commits is worse than an untidy one. Release-bump commits and merge
commits are omitted: they describe the release process, not the release.
"""
from __future__ import annotations

import re
import subprocess
import sys

# Section title per Conventional Commit type, in the order they should be read.
SECTIONS: list[tuple[str, str]] = [
    ("feat", "Features"),
    ("fix", "Fixes"),
    ("perf", "Performance"),
    ("refactor", "Refactoring"),
    ("docs", "Documentation"),
    ("test", "Tests"),
    ("build", "Build"),
    ("ci", "CI"),
    ("chore", "Chores"),
]
SUBJECT = re.compile(r"^(?P<type>[a-z]+)(?:\((?P<scope>[^)]*)\))?(?P<breaking>!)?: (?P<summary>.+)$")
SKIP = re.compile(r"^chore\(release\):|^Merge ")


def default_range() -> str:
    tag = subprocess.run(
        ["git", "describe", "--tags", "--abbrev=0"],
        capture_output=True, text=True, check=False,
    ).stdout.strip()
    return f"{tag}..HEAD" if tag else "HEAD"


def commits(rev_range: str) -> list[tuple[str, str]]:
    out = subprocess.run(
        ["git", "log", "--no-merges", "--format=%h\x1f%s", rev_range],
        capture_output=True, text=True, check=True,
    ).stdout
    rows = []
    for line in out.splitlines():
        if not line.strip():
            continue
        sha, _, subject = line.partition("\x1f")
        if SKIP.search(subject):
            continue
        rows.append((sha, subject))
    return rows


def main() -> int:
    rev_range = sys.argv[1] if len(sys.argv) > 1 else default_range()
    rows = commits(rev_range)
    if not rows:
        print("_No changes._")
        return 0

    grouped: dict[str, list[str]] = {}
    breaking: list[str] = []

    for sha, subject in rows:
        m = SUBJECT.match(subject)
        if m:
            kind = m.group("type")
            scope = m.group("scope")
            summary = m.group("summary")
            entry = f"- {f'**{scope}:** ' if scope else ''}{summary} ({sha})"
            if m.group("breaking"):
                breaking.append(entry)
            grouped.setdefault(kind, []).append(entry)
        else:
            grouped.setdefault("other", []).append(f"- {subject} ({sha})")

    print("## What's changed")
    print()

    if breaking:
        print("### ⚠ Breaking changes")
        print()
        for entry in breaking:
            print(entry)
        print()

    for kind, title in SECTIONS:
        if kind in grouped:
            print(f"### {title}")
            print()
            for entry in grouped[kind]:
                print(entry)
            print()

    known = {kind for kind, _ in SECTIONS}
    leftovers = [k for k in grouped if k not in known]
    if leftovers:
        print("### Other")
        print()
        for kind in leftovers:
            for entry in grouped[kind]:
                print(entry)
        print()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
