# PebbleNTN Privacy Policy

**Effective date:** 2026-10-26
**Contact:** pebblentn@mg.mijnesite.be, or an issue at https://github.com/optio/PebbleNTN/issues

PebbleNTN is an open-source Android app (MIT license) that shows turn-by-turn directions from
navigation apps on a Pebble watch. This policy covers the PebbleNTN phone app and its watchapp,
whichever store or page you installed them from: Google Play, GitHub releases, F-Droid or Obtainium.

## In short

- PebbleNTN works on your phone. It has no account, no analytics, no advertising and no tracking.
- The developer receives no data from the app. Nothing is uploaded automatically.
- The only network request PebbleNTN makes itself is an optional check for a newer version on
  GitHub, and only on installs that do not come from Google Play. It sends no personal data.
- Anything else leaves your phone only when you choose to send it: a diagnostic file you share, an
  email you send, or a web page you open.

## What PebbleNTN does

PebbleNTN reads notifications from the navigation apps you keep enabled, extracts the upcoming turn
(the maneuver, the distance, the road name and the arrival time), and sends a compact turn
instruction to your Pebble watch through the Pebble companion app on your phone.

## Notification access

PebbleNTN uses Android's notification-listener access as its core function. Before you grant it, the
app shows what it reads and why. For every notification it first checks the sending app's package
name: notifications from apps that are not enabled are ignored and never read. From enabled
navigation apps it reads only selected fields: the text fields that carry the directions (title,
text, sub-text, big text, summary and info text) and the progress value. For the few navigation apps
that show the turn as a picture, the notification's large icon is compared in memory with
PebbleNTN's own turn pictures and only the name of the match is kept. It never reads notification
actions, pending intents, contacts or attachments. You can
turn any navigation app off in **Navigation apps**, turn PebbleNTN off with its master switch, or
revoke notification access at any time in Android settings.

## Data processed on your device

- **Turn instructions.** Processed in memory and sent to your watch over the Pebble companion app.
  Only the current navigation state is kept, so the watch can resume after a restart.
- **Debug history.** Recent navigation notifications are kept on the phone, so you can check and
  improve the rules that read them. Notification identifiers are stored as one-way hashes. The
  history keeps the most recent events only (1,000 by default) and you can delete it at any time.
- **Settings and your rules.** Which navigation apps are enabled, the rules you create, the watch
  settings and the last watchapp version your watch reported.

None of this leaves your phone unless you export or send it yourself (see below).

## Network

- **Google Play installs make no network requests of their own.** Google Play delivers the updates,
  and the app compares its version with your watchapp's version, which needs no network.
- **Other installs (GitHub releases, F-Droid, Obtainium)** can check GitHub for a newer version. The
  check runs only when you tap **Check for updates**, or about once a week if you turn on **Check for
  updates weekly** (off by default). It asks GitHub's public API for the latest release
  (`https://api.github.com/repos/optio/PebbleNTN/releases/latest`). The request carries no personal
  data, no account and no app or device identifier; like any web request, it shows your IP address to
  GitHub, whose privacy statement applies to it. Turn the weekly check off in **Updates** to stop it.
- PebbleNTN downloads no code and no rules from the internet. Its rules ship inside the app.
- The connection to your watch runs through the Pebble companion app on the phone and Bluetooth,
  not through the internet.

## Links you open

Some buttons open a web page or another app, and only when you tap them:

- the app's download pages (GitHub releases, F-Droid, Obtainium, or its Google Play listing);
- the watchapp's page in the Pebble app store;
- a navigation app's Google Play page;
- the feedback pages on GitHub (report a bug, request a feature, suggest a name). The bug and
  feature links fill in the PebbleNTN version, and the bug link also the Android version, in the form
  you see before you submit it.

These pages are run by GitHub, Google, F-Droid, Obtainium or the Pebble app store, under their own
privacy policies. PebbleNTN sends them nothing beyond opening the page.

## Emails and diagnostic files you send

- **Feedback by email.** PebbleNTN opens a draft in your email app, addressed to
  pebblentn@mg.mijnesite.be, with the PebbleNTN and Android versions in the body. Nothing is sent
  until you press send in your email app.
- **Sharing diagnostics.** You can share a diagnostic file by email (the same address, prefilled) or
  with any app through the Android share sheet. You see what it contains before you share it. A
  "privacy-safe" export replaces road names and destinations; a "full" export includes the raw
  selected notification fields and asks you to confirm a warning first. Files are written to the
  app's temporary storage and removed at the next export.

When you send an email or a file to the developer, it is used only to answer you and to improve
PebbleNTN, is not shared with anyone else, and is deleted on request.

## Data retention and deletion

Everything PebbleNTN keeps is stored on your phone. Delete the debug history with **Debug history >
Delete all**, clear all of the app's data in Android settings, or uninstall the app to remove it all.
To have an email or diagnostic file you sent deleted, write to the contact address above.

## Children

PebbleNTN is not directed at children, and it collects no personal data for advertising or
profiling.

## Changes

If PebbleNTN's data practices change, this policy and its effective date are updated in the
repository (https://github.com/optio/PebbleNTN/blob/main/docs/PRIVACY_POLICY.md) before the
release that changes them.
