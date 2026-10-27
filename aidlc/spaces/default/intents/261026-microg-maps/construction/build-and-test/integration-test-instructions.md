# Integration Test Instructions — MicroG Maps as a Google Maps variant (#113)

## Covered by Automated Tests

- `RuleFamiliesMatchingTest` runs every Google Maps fixture with the MicroG package and gets identical results. A user rule for either package applies to both.
- `MicrogMapsEnablementTest`:
  - non-Play installs give MicroG Maps its own switch;
  - on Play installs it follows the Google Maps switch (on: matched `google-maps-turn-right-fr`; off: ignored).
- `MicrogMapsNavigationAppsTest` and `SetupProgressTest`:
  - GitHub installs show the MicroG row or the "Also supported" item, with the website link;
  - Play installs show neither.
- The workbench regression covers google-maps 195/195, including 4 MicroG fixtures.

## On a Phone with MicroG Maps (maintainer, through #113)

1. Install the release on a phone that has MicroG Maps (`app.morphe.android.apps.maps`) and is not a Play install. Navigation apps shows MicroG Maps with its own switch.
2. Start a route in MicroG Maps. Directions reach the watch, and the debug history labels the events "MicroG Maps".
3. **Icon-only cards:** check whether the turn arrow is right or the plain fallback arrow. If it's the fallback, ask for a debug export and fix the drawable lookup separately.
4. A user rule cloned from a Google Maps rule also applies to MicroG Maps.
