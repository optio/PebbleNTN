# Requirements Analysis Questions — Vela Maps support (#120)

Context:
- **Issue #120 proposes:**
  - a catalog entry for `app.vela` (website https://github.com/PimpinPumpkin/Vela, `playListed: false`);
  - English rules in `rules/bundled/vela-maps/en.json`;
  - synthetic fixtures.
- **The person's added request (verbatim):** "Regarding #120 , also add if possible Vela Maps to the test suite that can run different routes in different apps to find unrecognised notifications"
- **What the scan found:**
  - No Kotlin change is needed: the catalog, the manifest `<queries>`, the rule folder, fixtures, a regression test, the workbench app list and some counts in tests.
  - With `playListed: false` and no shared rules, a Google Play install ignores Vela's notifications entirely.
  - The issue's rules are valid, and Vela's source confirms the format. They have gaps:
    - a bare-instruction title (distance 0) gets no arrow;
    - "Take the I 35 South exit" matches the roundabout rule;
    - "At the roundabout, turn right" gives a turn arrow;
    - exits are always slight right and U-turns always left;
    - no "bear left/right";
    - no arrival arrow for "Your destination is …".
  - Route capture: Vela is on GitHub releases and accepts `google.navigation:` and `geo:` links, so a driver can be added. Whether the link starts guidance straight away is only known on the emulator.

## Q1. [Product] Should Vela Maps work on Google Play installs?

A. Yes: Play installs process Vela's notifications too, but show no website link (a small code change)
B. No: hidden on Play, as the issue proposes; Play installs ignore Vela
X. Other (please specify)

[Answer]: X. Other: "Same as MicroG maps" (hidden in the app on Google Play installs but its notifications still work there; website link outside Play)

## Q2. [Product] Which rules ship?

A. The issue's rules, with the gaps above fixed: bare-instruction titles, exit vs roundabout, turns at roundabouts, left/right exits and U-turns from the wording, bear left/right, arrival
B. The issue's rules exactly as proposed
X. Other (please specify)

[Answer]: A

## Q3. [Quality] How far does the route-capture work go?

A. Add the Vela driver, app entry, scenario plan and the "Navigating" fixture, then try a run on the Windows emulator at Build and Test
B. Add it only; you run it later
C. Leave route capture out of this work
X. Other (please specify)

[Answer]: A

## Q4. [Quality] Which route-capture runs should Vela get?

A. The existing European routes in English (metric), plus a new US route for imperial units (feet and miles)
B. The existing European routes only
X. Other (please specify)

[Answer]: X. Other: "EU + US imperial and run for now only for Vela Maps but do include the US run in the code for future tests of the other apps"
