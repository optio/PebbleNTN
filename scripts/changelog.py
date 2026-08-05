#!/usr/bin/env python3
"""Render a changelog for a git revision range, grouped by Conventional Commit type.

    scripts/changelog.py v0.1.0..HEAD          # Markdown, for the GitHub release body
    scripts/changelog.py v0.1.0..HEAD --fdroid # plain text, <=500 chars, for F-Droid
    scripts/changelog.py                       # everything since the last tag, or all history

Commits whose subject does not parse as a Conventional Commit still appear, under "Other" — a
changelog that silently drops commits is worse than an untidy one. Release-bump commits and merge
commits are omitted: they describe the release process, not the release.

`--fdroid` writes what an F-Droid user sees per version: user-facing changes only (features, fixes,
performance), one bullet each, no commit hashes, capped at F-Droid's 500-character limit. The
release workflow writes it to fastlane/metadata/android/en-US/changelogs/<versionCode>.txt so each
tagged release ships its own changelog with no manual step.
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


# F-Droid changelog: the types a user cares about, in order, and the hard character limit.
FDROID_TYPES = ("feat", "fix", "perf")
FDROID_MAX_CHARS = 500


def render_fdroid(parsed: list[dict]) -> str:
    """A plain, user-facing changelog capped at F-Droid's 500-char limit."""
    summaries: list[str] = []
    seen: set[str] = set()
    for entry in parsed:
        if entry["breaking"] or entry["kind"] in FDROID_TYPES:
            s = entry["summary"].strip()
            if s and s not in seen:
                seen.add(s)
                summaries.append(s)
    if not summaries:
        return "Maintenance and internal improvements."
    lines: list[str] = []
    total = 0
    for s in summaries:
        bullet = f"• {s}"
        cost = len(bullet) + (1 if lines else 0)  # + newline once we have a line
        if total + cost > FDROID_MAX_CHARS:
            break
        lines.append(bullet)
        total += cost
    return "\n".join(lines)


def main() -> int:
    args = sys.argv[1:]
    fdroid = "--fdroid" in args
    positional = [a for a in args if not a.startswith("--")]
    rev_range = positional[0] if positional else default_range()
    rows = commits(rev_range)

    parsed: list[dict] = []
    for sha, subject in rows:
        m = SUBJECT.match(subject)
        if m:
            parsed.append({
                "kind": m.group("type"),
                "scope": m.group("scope"),
                "summary": m.group("summary"),
                "sha": sha,
                "breaking": bool(m.group("breaking")),
            })
        else:
            parsed.append({"kind": "other", "scope": None, "summary": subject, "sha": sha, "breaking": False})

    if fdroid:
        print(render_fdroid(parsed))
        return 0

    if not rows:
        print("_No changes._")
        return 0

    grouped: dict[str, list[str]] = {}
    breaking: list[str] = []
    for entry in parsed:
        scope, summary, sha = entry["scope"], entry["summary"], entry["sha"]
        if entry["kind"] == "other":
            grouped.setdefault("other", []).append(f"- {summary} ({sha})")
            continue
        line = f"- {f'**{scope}:** ' if scope else ''}{summary} ({sha})"
        if entry["breaking"]:
            breaking.append(line)
        grouped.setdefault(entry["kind"], []).append(line)

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
