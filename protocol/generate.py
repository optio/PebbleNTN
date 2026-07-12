#!/usr/bin/env python3
"""Generate read-only protocol constants for the phone (Kotlin) and watch (C).

Single source of truth: protocol/protocol-definition.json. The emitted files carry a
DO-NOT-EDIT banner (AGENTS.md rule 5). Use --check to verify the committed generated files are
up to date without writing them (used by CI and scripts/test-all.sh).

Usage:
  generate.py            # write generated files
  generate.py --check    # exit non-zero if regeneration would change anything
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFINITION = REPO_ROOT / "protocol" / "protocol-definition.json"
KOTLIN_OUT = REPO_ROOT / "android" / "app" / "src" / "main" / "java" / "com" / "pebblentn" / "app" / "protocol" / "Protocol.kt"
C_OUT = REPO_ROOT / "watchapp" / "src" / "c" / "generated" / "protocol.h"

BANNER_LINES = [
    "GENERATED FILE — DO NOT EDIT.",
    "Source: protocol/protocol-definition.json",
    "Regenerate: scripts/generate-protocol.sh (protocol/generate.py)",
]


def load_definition() -> dict:
    return json.loads(DEFINITION.read_text(encoding="utf-8"))


def _kt_const_block(indent: str, entries: dict[str, int], suffix: str = "") -> str:
    return "\n".join(f"{indent}const val {name}{suffix} = {value}" for name, value in entries.items())


def render_kotlin(defn: dict) -> str:
    keys = defn["keys"]
    events = defn["events"]
    flag_bits = defn["flagBits"]
    maneuvers = defn["maneuvers"]
    error_codes = defn["errorCodes"]

    banner = "\n".join(f"// {line}" for line in BANNER_LINES)
    flag_masks = "\n".join(
        f"        const val {name}_MASK = 1 shl {bit}" for name, bit in flag_bits.items()
    )
    return f"""{banner}
package com.pebblentn.app.protocol

/** AppMessage protocol constants shared with the Pebble watchapp. */
object Protocol {{
    const val MAJOR = {defn["protocolMajor"]}
    const val MINOR = {defn["protocolMinor"]}

    /** AppMessage dictionary keys. */
    object Keys {{
{_kt_const_block("        ", keys)}
    }}

    /** Values for the [Keys.EVENT] field. */
    object Events {{
{_kt_const_block("        ", events)}
    }}

    /** Bit positions for the [Keys.FLAGS] field. */
    object FlagBits {{
{_kt_const_block("        ", flag_bits)}
    }}

    /** Precomputed masks for the [Keys.FLAGS] field. */
    object FlagMasks {{
{flag_masks}
    }}

    /** Values for the [Keys.MANEUVER] field. */
    object ManeuverCodes {{
{_kt_const_block("        ", maneuvers)}
    }}

    /** Values for the [Keys.ERROR_CODE] field. */
    object ErrorCodes {{
{_kt_const_block("        ", error_codes)}
    }}
}}
"""


def _c_define_block(prefix: str, entries: dict[str, int]) -> str:
    return "\n".join(f"#define {prefix}{name} {value}" for name, value in entries.items())


def render_c(defn: dict) -> str:
    banner = "\n".join(f"// {line}" for line in BANNER_LINES)
    flag_masks = "\n".join(
        f"#define PBNTN_FLAG_{name}_MASK (1 << {bit})" for name, bit in defn["flagBits"].items()
    )
    return f"""{banner}
#pragma once

#define PBNTN_PROTOCOL_MAJOR {defn["protocolMajor"]}
#define PBNTN_PROTOCOL_MINOR {defn["protocolMinor"]}

// AppMessage dictionary keys.
{_c_define_block("PBNTN_KEY_", defn["keys"])}

// Values for the EVENT key.
{_c_define_block("PBNTN_EVENT_", defn["events"])}

// Bit positions for the FLAGS key.
{_c_define_block("PBNTN_FLAG_BIT_", defn["flagBits"])}

// Precomputed masks for the FLAGS key.
{flag_masks}

// Values for the MANEUVER key.
{_c_define_block("PBNTN_MANEUVER_", defn["maneuvers"])}

// Values for the ERROR_CODE key.
{_c_define_block("PBNTN_ERROR_", defn["errorCodes"])}
"""


def write_or_check(path: Path, content: str, check: bool) -> bool:
    """Return True if the file is (or was made) up to date."""
    rel = path.relative_to(REPO_ROOT)
    if check:
        if not path.exists():
            print(f"OUT-OF-DATE {rel}: file missing", file=sys.stderr)
            return False
        if path.read_text(encoding="utf-8") != content:
            print(f"OUT-OF-DATE {rel}: regeneration differs from committed file", file=sys.stderr)
            return False
        print(f"OK          {rel}")
        return True
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"WROTE       {rel}")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify committed files are current")
    args = parser.parse_args()

    defn = load_definition()
    kotlin = render_kotlin(defn)
    c_header = render_c(defn)

    ok = True
    ok &= write_or_check(KOTLIN_OUT, kotlin, args.check)
    ok &= write_or_check(C_OUT, c_header, args.check)

    if not ok:
        if args.check:
            print("Generated protocol files are stale. Run scripts/generate-protocol.sh.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
