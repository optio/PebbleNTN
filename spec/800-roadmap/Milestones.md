# Implementation Roadmap

## M0 — Repository and builds
Create monorepo, licenses, wrappers, scripts, CI skeleton, empty Android app and empty Pebble watchapp.

## M1 — Domain model and protocol
Implement generated protocol constants, normalized navigation model, session reducer and fake watch transport.

## M2 — Notification access and early filtering
Onboarding, listener declaration, permission status, app catalog and strict package allowlist.

## M3 — Debug capture
Snapshot selected fields, Room history, retention, debug list/detail, deletion and synthetic publisher.

## M4 — Rule engine
Schemas, canonicalization, operators, extractors, trace, layer precedence, unit tests.

## M5 — Editors and preview
Simple editor backed by JSON, expert editor, validation, test bench and event rerun.

## M6 — Google Maps initial rules
Capture corpus, sanitize fixtures, implement English/Dutch/French/German variants that evidence supports, document gaps.

## M7 — Pebble integration
Pebble transport, launch once, READY handshake, current-state sync, watch renderer, bitmap assets, stop/exit behavior.

## M8 — Export and maintainer workflow
Rules-only, privacy-safe and full diagnostic exports; rule-workbench inspect/test/diff/scaffold/sanitize/promote commands.

## M9 — Hardening
Process restoration, connection loss, stale state, performance, regex limits, migration tests and privacy review.

## M10 — Release readiness
Play disclosures, privacy policy template, signed builds, release CI, physical-device test matrix and beta checklist.

## M11 — Remote official rules, post-v1
Signed HTTPS updates, compatibility checks, self-tests, atomic activation and rollback. Feature remains off until separately approved.
