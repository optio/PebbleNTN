# PebbleNTN — Build & Test Guide (for humans)

This guide explains how to build, test, and run the two apps in this repo:

- the **Android phone app** (`android/app`) — reads navigation notifications and drives the watch;
- the **Pebble watchapp** (`watchapp/`) — renders the turn instructions.

It also covers the developer tooling (rule validators, protocol/bitmap generators, fixtures).

> Status: milestones M0–M11 are implemented. The phone↔watch link and the Google Maps rules still
> need on-device / real-capture verification — see `docs/IMPLEMENTATION_STATUS.md` for exactly what
> is verified vs. pending.

---

## 1. Prerequisites

| Tool | Version | Used for |
|---|---|---|
| JDK | 21 (LTS) | Android build |
| Android SDK | platform 36, build-tools 36.0.0, platform-tools | Android build/install |
| Pebble tool + SDK | `pebble-tool` 5.x with SDK 4.x | Watchapp build |
| Python | 3.10+ with `jsonschema` | Rule/catalog/spec validators, generators |

Check what you have:

```bash
./scripts/bootstrap.sh
```

It prints which tools are present and, for anything missing, the exact commands to install it on
your platform (apt/dnf/pacman/zypper/Homebrew are detected automatically). It never installs
anything itself, and it exits non-zero while something is still missing.

### Point Gradle at your Android SDK

Create `android/local.properties` (git-ignored) with your SDK path, or set `ANDROID_HOME`:

```properties
sdk.dir=/path/to/Android/sdk
```

---

## 2. Android phone app

All Gradle commands run from the `android/` directory (or with `-p android`).

### Build a debug APK

```bash
cd android
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

### Run the unit tests + lint

```bash
cd android
./gradlew test lint
```

The unit tests include Robolectric-backed Room/ViewModel tests and the Google Maps rule regression,
so no device is required for them.

### Install & launch on a device/emulator

```bash
./scripts/install-android-debug.sh      # builds + installs the debug APK (needs adb + a device)
# then open the "PebbleNTN" app
```

Or start an emulator first:

```bash
./scripts/run-android-emulator.sh [avd-name]
```

### Using the app

1. Open PebbleNTN and grant **notification access** when prompted (Onboarding screen → the exact
   disclosure is shown before you're sent to system settings).
2. In **Navigation Apps**, installed catalog apps (Google Maps, Waze, …) are enabled by default;
   toggle as you like. Google Maps has official English rules; others are capture-only.
3. **Dashboard** shows access status and the last eligible notification. **Debug history** lists
   captured events with their rule match + trace. **Rules** lets you clone official rules, write
   user rules in the expert JSON editor (validate / format / preview / save), and preview a
   candidate against your last captured notification.

### Instrumented tests (needs a device/emulator)

```bash
cd android
./gradlew connectedDebugAndroidTest
```

### Testing without a real navigation app — the fixture publisher

`android/fixture-publisher` is a debug app that posts synthetic navigation-like notifications.

```bash
cd android
./gradlew :fixture-publisher:installDebug     # needs a device/emulator
```

Open **PebbleNTN Fixtures**, tap a maneuver button (Turn right, Roundabout, …), then check
PebbleNTN's Debug history. Its package is registered in the catalog as capture-only, so it only
becomes active when this app is actually installed.

---

## 3. Pebble watchapp

From the `watchapp/` directory (Pebble SDK must be installed):

```bash
# or: ./scripts/build-watchapp.sh  from the repo root
cd watchapp
pebble build          # -> build/watchapp.pbw  (aplite, basalt, chalk, diorite, emery)
```

Run in the Pebble emulator or install to a watch:

```bash
pebble install --emulator basalt     # emulator
pebble install --phone <PHONE_IP>    # real watch via the Pebble phone app
pebble logs --emulator basalt        # view logs
```

The watchapp only renders normalized state it receives over AppMessage; it never parses notification
text. Maneuver bitmaps are generated (see §4).

---

## 4. Developer tooling

Run from the repo root.

```bash
# Validate spec asset integrity, generated protocol, catalog + rulesets:
./scripts/validate-spec-assets.sh
./scripts/generate-protocol.sh --check      # verify generated Protocol.kt / protocol.h are current
./scripts/validate-rules.sh                 # catalog + all rulesets vs. their JSON schemas

# Regenerate the read-only protocol constants (Kotlin + C) from the single JSON definition:
./scripts/generate-protocol.sh

# Regenerate the watch maneuver bitmaps:
python3 watchapp/tools/gen_maneuver_bitmaps.py
```

- **Rules** live in `rules/bundled/` (shipped), `rules/catalog/` (app catalog), and
  `rules/fixtures/` (test fixtures; **not** shipped in the APK).
- **Protocol** single source of truth: `protocol/protocol-definition.json` → generated
  `android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt` and
  `watchapp/src/c/generated/protocol.h` (both read-only; never hand-edit).

---

## 5. One-shot everything

```bash
./scripts/test-all.sh     # spec + protocol + rules validation, then Android unit tests + lint
./scripts/build-all.sh    # Android debug APKs + watchapp .pbw
```

`build-all.sh` ends with an artifact report giving the absolute path of every file it produced, and
`FAILED` for anything it did not; it exits non-zero if any artifact is missing:

```
Artifacts:
  OK      phone app (debug)      /…/android/app/build/outputs/apk/debug/app-debug.apk (27M)
  OK      fixture publisher      /…/android/fixture-publisher/build/outputs/apk/debug/fixture-publisher-debug.apk (6.0M)
  OK      watchapp               /…/watchapp/build/watchapp.pbw (48K)
```

---

## 6. CI

GitHub Actions build and gate PRs: `android.yml` (unit tests, lint, APK), `watchapp.yml` (.pbw),
`rules.yml` (schema/regression), `release.yml` (signed AAB, tag-triggered), `codeql.yml`.
