# Requirements Analysis Questions — #103 About screen: last connected phone app version

Context: issue #103 already fixes the behaviour (read key 11 `APP_VERSION`, persist it, show it on About, keep the About row subtitle as the watchapp version, no protocol change). The questions below cover only what the issue leaves open.

## Q1. [Product] How should the About lines read once two versions are shown?

Today About shows `Version 0.0.68`, `Protocol 1.1`, then the phone status line (`Phone app connected` / `Phone app not answering` / `Pebble app not connected`). The issue asks for `Phone app 0.0.67` under the watchapp version, and `Phone app: never connected` before any phone message was ever received. With two versions on screen, a bare "Version" becomes ambiguous, and two lines start with "Phone app".

A. Keep the issue's wording as is: `Version 0.0.68` / `Phone app 0.0.67` (or `Phone app: never connected`) / `Protocol 1.1` / status line
B. Name both versions: `Watchapp 0.0.68` / `Phone app 0.0.67` (or `Phone app: never connected`) / `Protocol 1.1` / status line
C. Name both versions, and say "last seen" for the phone: `Watchapp 0.0.68` / `Phone app 0.0.67 (last seen)` (or `Phone app: never connected`) / `Protocol 1.1` / status line
X. Other (please specify)

[Answer]: C

## Q2. [Developer] Should the About screen update while it is open?

About takes a snapshot of what it shows when it opens (the phone status already works this way). A phone message could arrive while About is open, for example the first message after a relaunch.

A. Snapshot when About opens, like the phone status today (simplest; reopen About to refresh)
B. Refresh the phone version and status live while About is open
X. Other (please specify)

[Answer]: A
