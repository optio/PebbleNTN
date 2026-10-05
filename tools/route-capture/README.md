# Route-capture regression harness

Drives navigation apps along fixed, synthetic routes in an Android emulator, records every
navigation notification they post, checks each one against the bundled rules (the rule-workbench
engine, the same one the fixture regression uses), and reports what was missed. With `--publish`,
the missed card shapes go into **one GitHub issue per app** (label `route-capture`), created on the
first run that misses something. Later runs **merge per scenario**: each scenario they ran replaces
its earlier result, and the others stay. The description shows every scenario's latest result and
what is still unrecognised; a comment records each run. The merged state lives in the issue body as
a hidden block, so runs of different scenario plans (for example English, then Polish) never erase
each other's findings. Tracking issue: #55.

Routes run along public streets with made-up trips, so captures contain no personal data and can
become rule fixtures directly.

## Host

| Host | How |
|---|---|
| **WSL on Windows** (tested) | The emulator runs **on Windows** (WHPX) from the Windows SDK in `%LOCALAPPDATA%\Android\Sdk`, driven from WSL with `adb.exe`. Do **not** run the Android 17 Play image inside WSL: nested virtualisation crashed the whole WSL environment. |
| **Linux with KVM** | The local SDK (`$ANDROID_HOME`, or `~/Android/Sdk`). Same commands; not yet tested. |

Requirements: Python 3.10+ (standard library only), and Java on the host for `sdkmanager` / `avdmanager`.
`--publish` also needs the `gh` CLI, logged in to the repository.

## Commands

```bash
cd tools/route-capture
./routecap.py setup                      # SDK tools, emulator, Android 17 Play image, AVD PebbleNTN_Capture_API_37 (idempotent)
./routecap.py fetch-routes               # cache each route's track per mode under routes/cache/ (committed)
./routecap.py install osmand comaps      # optional: latest builds of the open-source apps (phase 3 drives them)
./routecap.py run scenarios/phase1.json  # boot, drive every scenario, write out/<timestamp>/{run.json,report.md}; run.json is saved after every scenario
./routecap.py run scenarios/phase1.json --publish     # ... and create/update the GitHub issue
./routecap.py report out/<timestamp>/run.json --publish  # re-evaluate a saved run with today's rules
```

`run` options: `--max-seconds N` (drive only the first N seconds of each route; a scenario's own `maxSeconds` applies otherwise), `--only REGEX` (scenario ids, e.g. `'en-GB|fr-FR'`), `--keep-emulator`.

## Files

| Path | What |
|---|---|
| `routes/<id>.json` | A route: `origin`, `destination` (`lat`, `lon`), the `modes` it supports (`car`, `bike`, `foot`). |
| `routes/cache/<id>.<mode>.json` | The track from the public OSM routers (routing.openstreetmap.de), fetched once and committed, so runs are reproducible. |
| `scenarios/<plan>.json` | A plan: scenarios of `app` × `locale` × `mode` × `route`. |
| `routecap/` | `host` (Windows-from-WSL / Linux), `apps` (drivers and installers), `geo`, `notifications` (`dumpsys` parsing), `ui` (first-run dialogs), `report`, `issues`. |
| `device/` | `SetSystemLocale.java` and its prebuilt `routecap-locale.dex` (2 KB; rebuild with `device/build.sh`): sets the Android system language without root. |
| `tests/` | Unit tests of the pure parts; part of `scripts/test-all.sh`. |
| `out/` | Run output (git-ignored). |

## How a scenario runs

0. **Warm-up** (once per run, in English): start each app **in every travel mode the plan uses** and tap through its first-run dialogs, so they never appear in a scenario's language, where the harness doesn't know the button labels. Modes have their own one-time dialogs: a first bike route showed one.
1. **Prepare the app:** grant location and notification permissions, and set the **Android system language** to the scenario's locale. Google Maps 26.x ignores a per-app language (`cmd locale set-app-locales`). Play images can't be rooted, so `device/SetSystemLocale` runs via `app_process` as the shell user. That user holds `CHANGE_CONFIGURATION` and `WRITE_SETTINGS` (the harness allows the `WRITE_SETTINGS` app-op for `com.android.shell`). It updates the persistent configuration like *Settings → System → Languages* does, and the harness checks the result with `am get-config`. After the run, the language goes back to English.
2. **Start navigation:** put the GPS at the route's origin (`emu geo fix`), then start navigation by deep link. A fresh image needs about a minute before Google Maps accepts the intent, so it retries.
3. **First-run dialogs:** tap through them by button text (sign-in "Skip", "OK", "Dismiss", …).
3b. **Wait for the first instruction** (up to 150 s, still tapping dialogs; navigation is requested again after a minute): building the route, or loading a large freshly pushed map after the language switch restarted the app, takes a while.
4. **Drive:** feed one GPS fix per second along the track, at the mode's speed (car 11 m/s, bike 5, foot 1.6). Every second, read `dumpsys notification --noredact` and keep each distinct card (title, text, subText, bigText, plus template and channel).
5. **Stop** the app. The report groups unrecognised cards by **shape** (numbers and the road name abstracted), with an example, a count, and the scenarios where they appeared.

**What counts as missed:** a card no rule matched *and* that looks like a direction by the app's own check (`ManeuverHeuristic`: an ETA clock time or a maneuver word, and not "Rerouting…"; its word list is read from the Kotlin source). Others, like "Starting navigation…", are listed as "not counted".

**Failed scenarios** (the system language didn't switch; no instruction appeared after starting; nothing was captured; or only status cards like "Navigation", meaning navigation didn't really run) are left out of the report and the issue, so a harness problem can't file wrong "missing rule" reports. If the emulator stops responding, the plan stops and the scenarios that ran are still reported.

**Emulator storage.** The capture image's data partition is 7.7 GB. Organic Maps and CoMaps keep every map they download, and accepting "download maps along the route" can pull in neighbouring regions too (Belgium and northern France for Amsterdam). The full storage then made Android's system services crash mid-run (`Can't find service: input`). The maps belong to the app, so the shell can't delete them, and the driver resets the app (`pm clear`) whenever the city changes instead. Running one language per invocation still works:

```sh
for l in en-GB fr-FR nl-NL de-DE it-IT es-ES; do ./routecap.py run scenarios/organic-maps-m3.json --only "\.$l\." --publish; done
```

**Cards a fixture already settles.** An unmatched card that a `matched: false` fixture in `rules/fixtures/<app>.json` describes (same fields, numbers aside) is listed as "left unshown on purpose", not as unrecognised. That's checked again on the issue's stored examples at every publish, so adding such a fixture clears the issue without rerunning anything.

## Plans and routes

| Plan | Scenarios |
|---|---|
| `scenarios/phase1.json` | Google Maps, en-US, car, Brussels |
| `scenarios/google-maps-new-languages.json` | Google Maps, pt-BR and pl-PL, car, Brussels (discovery) |
| `scenarios/osmand-m3.json` | OsmAnd × car / bike / foot × the same six languages and cities (milestone 3); about 1.4 GB of region maps on the first run |
| `scenarios/google-maps-m2.json` | Google Maps × car / bike / foot × en-GB (London), fr-FR (Paris), nl-NL (Amsterdam), de-DE (Berlin), it-IT (Milan), es-ES (Madrid); 18 scenarios. Each has a `maxSeconds` cap (car and bike 7 min, foot 5 min), and the whole plan takes about 2 h, so run it in parts with `--only` |

## Apps

| App | Source of the build | Driver |
|---|---|---|
| Google Maps | Preinstalled in the Play image (Android 17 ships 26.14; updating needs a Play sign-in) | `google.navigation:q=<lat>,<lon>&mode=d\|b\|w` |
| OsmAnd | F-Droid (`net.osmand.plus`) | The route's region map (`routes/<id>.json` → `maps.osmand`, e.g. `Germany_berlin_europe_2`) is downloaded from download.osmand.net once (cached in the host's temp folder) and pushed into `/sdcard/Android/data/net.osmand.plus/files/`. Navigation: end any previous route (`osmand.api://stop_navigation`; OsmAnd resumes the last route on start), then `google.navigation:q=…&mode=d\|b\|w` and the route preview's **Start**. OsmAnd's own `osmand.api://navigate` didn't start anything. OsmAnd follows the Android system language. **Open point:** OsmAnd seems to ignore the intent's `mode`, so its bike and foot scenarios may run with the car profile (the instruction strings are the same across profiles) |
| Organic Maps | GitHub release (`app.organicmaps.web`) | Maps come through the app, with the GPS at the route's origin: the first-run world overview map (its "Download <region>?" box ticked), then the region map a route request offers. Navigation: `om://route?sll=…&saddr=Start&dll=…&daddr=Destination&type=vehicle\|bicycle\|pedestrian`, sent to a stopped app (a running one ignores it), then the preview's **Start**, "plan from your current location?" → OK, and the one-time route disclaimer → Accept (positive buttons found by id, so any language works). The app follows the Android system language |
| CoMaps | Codeberg release (`app.comaps`) | A fork of Organic Maps: the same driver with the `cm://` scheme |

## Known limits

- **Which Google Maps card you get varies.** Google Maps has two layouts. The *classic* card has title `70 m`, text `<road>`, no turn word, and the direction only in its icon (#59). The *ProgressStyle* card has title `200 m · Turn left onto …` and subText `Arrive 3:12 AM`, like on real phones. Manual tests on fresh emulators (Android 14 / 16 / 17) only ever got the classic card. The first harness run on the Android 17 image (Maps 26.14, after per-app language and permissions were set) got the ProgressStyle card throughout. Google presumably decides the layout per install, so the report records the template of every capture.
- **Play-only apps** (Waze, Sygic, HERE, …) need a signed-in Play Store, so they aren't covered yet.
- **Public transport** (Google Maps only) is a later phase: it follows real timetables.
