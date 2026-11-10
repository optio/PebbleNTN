# Integration Test Instructions: #134

The express Minimal strategy needs no extra integration suite. The JVM and workbench fixture runs already test the whole path, from notification fields through the rules to the watch output. The checks below cover what fixtures cannot.

## 1. OsmAnd on the Android emulator (FR5.1)

```bash
cd tools/route-capture
./routecap.py run scenarios/osmand-m3.json --only 'osmand\.(en-GB|fr-FR)\.car\.'
```

- Runs OsmAnd by car in English (London) and French (Paris) on public streets with made-up trips. Records every notification and checks it against the bundled rules (Python engine).
- Pass:
  - no direction card is reported as missed;
  - re-evaluating the captured cards with the workbench gives, for each turn card, a full road line equal to bigText line 1 without the distance, and a compact road line that is the street;
  - the second line shows the arrival clock, with an ETA.
- Only the card structure goes into the record. Raw captures stay in `tools/route-capture/out/` (git-ignored).

## 2. Watch row (FR7.2)

On the basalt and aplite Pebble emulators:

```bash
cd watchapp
pebble install --emulator basalt build/watchapp.pbw
```

Then do these steps, and repeat them for aplite:

1. Press SELECT to open Settings.
2. Press DOWN to the last row, "Directions text", which should read Full.
3. Press SELECT: it should read Compact.
4. Press SELECT again: it should read Full.

Take a screenshot at each step. Stop the emulators afterwards with `pebble kill`.

## 3. Phone and watch (owned by the person's on-device check before release)

These need a real phone with a Pebble:
- With OsmAnd navigating, switch Directions text on the phone. The watch's road line should change at once, without waiting for the next notification.
- Switch it on the watch: the phone's setting should follow.
