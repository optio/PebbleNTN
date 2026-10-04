"""Publish a run's missed captures as a GitHub issue: create one per app, or update the open one.

One rolling issue per app keeps the backlog in one place: its body always shows the latest run,
and each run adds a short comment, so the history stays visible.
"""
from __future__ import annotations

import base64
import json
import subprocess
import tempfile
import zlib

LABEL = "route-capture"
LABEL_COLOR = "5319e7"
LABEL_DESCRIPTION = "Found by the route-capture harness (#55)"


def marker(app_id: str) -> str:
    return f"<!-- route-capture:app={app_id} -->"


def title(app_name: str) -> str:
    return f"Route capture: unrecognised {app_name} notifications"


STATE_PREFIX = "<!-- route-capture-state:"


def encode_state(state: dict) -> str:
    """The merged report state, hidden in the issue body (base64, so it can't break the comment)."""
    data = base64.b64encode(zlib.compress(json.dumps(state, ensure_ascii=False).encode("utf-8"))).decode("ascii")
    return f"{STATE_PREFIX}{data} -->"


def decode_state(body: str) -> dict | None:
    i = body.find(STATE_PREFIX)
    if i < 0:
        return None
    data = body[i + len(STATE_PREFIX):body.index(" -->", i)]
    return json.loads(zlib.decompress(base64.b64decode(data)).decode("utf-8"))


def decide(existing: dict | None, unmatched_count: int) -> str:
    """'create', 'update', 'update-clear' (nothing unrecognised any more) or 'skip'."""
    if existing is None:
        return "create" if unmatched_count else "skip"
    return "update" if unmatched_count else "update-clear"


def run_comment(started_at: str, run_state: dict, merged_unrecognised: int) -> str:
    """What this run did; the description shows the merged result of all scenarios."""
    rows = ", ".join(f"`{s['id']}` {s['matched']}/{s['total']}" for s in run_state["scenarios"].values())
    found = sum(len(v) for v in run_state["shapes"].values())
    return (f"Route-capture run {started_at}: {rows} recognised; {found} unrecognised card shape(s) in this run. "
            f"Across all scenarios, {merged_unrecognised} shape(s) remain (see the description).")


def _gh(*args: str, input_text: str | None = None) -> str:
    return subprocess.run(["gh", *args], input=input_text, capture_output=True, text=True, check=True).stdout


def find_open_issue(app_id: str) -> dict | None:
    out = _gh("issue", "list", "--label", LABEL, "--state", "open", "--limit", "100", "--json", "number,title,body")
    return next((i for i in json.loads(out) if marker(app_id) in (i.get("body") or "")), None)


def ensure_label() -> None:
    labels = json.loads(_gh("label", "list", "--limit", "200", "--json", "name"))
    if not any(l["name"] == LABEL for l in labels):
        _gh("label", "create", LABEL, "--color", LABEL_COLOR, "--description", LABEL_DESCRIPTION)


def publish(app_id: str, app_name: str, run_state: dict, started_at: str, dry_run: bool) -> str:
    """Merge this run into the app's open issue (or a new one) and describe the run in a comment."""
    from . import report  # local import: report pulls in the rule engine

    existing = None if dry_run else find_open_issue(app_id)
    merged = report.merge(decode_state(existing["body"]) if existing else None, run_state)
    remaining = len(report.unrecognised(merged))
    action = decide(existing, remaining)
    if dry_run:
        return f"{action} (dry run)"
    if action == "skip":
        return "skip: nothing unrecognised, no open issue"
    body = report.markdown(app_name, merged) + "\n\n" + marker(app_id) + "\n" + encode_state(merged)
    with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False, encoding="utf-8") as f:
        f.write(body)
        body_file = f.name
    if action == "create":
        ensure_label()
        return "created " + _gh("issue", "create", "--title", title(app_name), "--label", LABEL, "--body-file", body_file).strip()
    number = str(existing["number"])
    _gh("issue", "edit", number, "--body-file", body_file)
    _gh("issue", "comment", number, "--body", run_comment(started_at, run_state, remaining))
    return f"{action} #{number}"
