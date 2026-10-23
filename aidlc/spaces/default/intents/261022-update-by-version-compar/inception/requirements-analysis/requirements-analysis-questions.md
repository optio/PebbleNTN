# Requirements Analysis Questions — Version comparison as the default update check

Context:
- **Already decided by you:**
  - The offline check (the watchapp version is higher than the companion app version) is the default.
  - The online GitHub check stays as an optional extra, run manually ("Check for updates now") or weekly (the existing toggle, off by default).
- **What the scan found:**
  - The offline check already drives the update card, the watch notice, the idle text and the subtitle on both sides, with no online check.
  - Since v0.0.74 we know the Core Devices app does pass the watchapp version to the phone.
  - What still makes the online check look like the default: the #94 "Check for updates" item at the top of the dashboard on every fresh install (and 30 days after the last online check), the weekly-check question after it, a toast that only looks at GitHub, and texts that present GitHub as the only source.
- **Corrected in any case:** the outdated "the Core Devices app doesn't forward watch messages" notes in code comments, REQ-ANDROID-019, `spec/200-architecture/Pebble.md` and the status doc.

## Q1. [Product] What should happen to the "Check for updates" item at the top of the dashboard (#94 stale check)?

A. Remove it. The offline check is the default; the online check stays available in the Updates section ("Check for updates now" and the weekly toggle)
B. Show it only while the phone has never received a watchapp version, so the offline check can't work yet
C. Keep it as it is (30 days since the last online check, or never checked)
X. Other (please specify)

[Answer]: C

## Q2. [Product] What should happen to the "Check for updates automatically every week?" question that follows that item?

A. Remove it; the weekly toggle in the Updates section stays
B. Keep it, shown after the first manual online check while weekly checks are off
X. Other (please specify)

[Answer]: B

## Q3. [Product] What should the "Check for updates now" result message say?

Today it only reflects GitHub, so it can say "PebbleNTN is up to date." while the card shows a newer version found from the watchapp.

A. Combine both sources: "up to date" only when neither finds a newer version; otherwise "PebbleNTN {version} is available"
B. Leave it as it is (GitHub only)
X. Other (please specify)

[Answer]: A

## Q4. [Product] Should the texts describe the offline check as the default?

Proposed wording:
- **Onboarding disclosure, network sentence:** "PebbleNTN notices a new version by comparing the phone app with the watchapp version, without using the internet. Optionally it can also check GitHub: when you tap Check for updates, or about once a week if you turn on weekly checks. That check sends no personal data."
- **Weekly toggle hint:** "Optional. PebbleNTN already notices new versions from your watchapp's version. When on, it also checks GitHub about once a week; nothing is sent."

A. Yes, use this wording
B. Yes, but shorter (I'll adjust it)
C. No, keep the current texts
X. Other (please specify)

[Answer]: B
