# PebbleNTN Privacy Policy (template)

_This is a template to be finalized by the project maintainers before Play Store submission. Fill
in the bracketed fields. The MIT license covers the source; this policy is a separate product
document (spec/600-security-release/BuildRelease.md)._

**Effective date:** [DATE]
**Contact:** [MAINTAINER EMAIL / URL]

## What PebbleNTN does

PebbleNTN reads notifications from navigation apps you explicitly enable, extracts upcoming turn
information, and sends a compact, normalized turn instruction to your Pebble watch.

## Notification access

PebbleNTN uses Android's notification-listener access as its **core functionality**. It checks the
source app's package name and only processes notifications from apps you have enabled. Notifications
from all other apps are ignored and never read.

## Data we process

- **On your device only.** All notification parsing happens locally. Notification content and
  diagnostics are **never uploaded automatically** and PebbleNTN contains no analytics or tracking.
- **Selected notification fields.** For enabled navigation apps, PebbleNTN reads only selected text
  fields (e.g. the maneuver instruction and distance). It never reads pending intents, actions,
  app widgets, icons, contacts, or arbitrary data.
- **Debug history.** Recent eligible events are stored locally so you can inspect and improve rules.
  Notification identifiers are stored as one-way hashes. You can delete history at any time.

## Sharing

Nothing is shared unless **you** explicitly export it. Exports use the Android share sheet and
temporary files. A "privacy-safe" export redacts road names and destinations; a "full" export
includes raw selected fields and requires an explicit warning and confirmation before sharing.

## Your controls

- Enable/disable each navigation app.
- Change or delete debug retention and delete all local data.
- Revoke notification access at any time in Android settings.

## Children

PebbleNTN is not directed at children and collects no personal data for advertising or profiling.

## Changes

We will update this policy and its effective date if our data practices change.
