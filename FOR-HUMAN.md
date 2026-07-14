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

### Install & launch

```bash
./scripts/install-android-debug.sh                # build + install + launch on the connected device/emulator
./scripts/install-android-debug.sh --fixtures     # also install the fixture publisher (see below)
./scripts/install-android-debug.sh --no-launch    # install only
```

It finds `adb` inside the SDK (via `ANDROID_HOME`/`ANDROID_SDK_ROOT` or `android/local.properties`),
so you do not need it on `PATH`. With several devices connected, set `ANDROID_SERIAL` to pick one.

### Seeing a crash ("PebbleNTN keeps stopping")

`adb` lives at `$ANDROID_HOME/platform-tools/adb` (the install script prints these two commands too):

```bash
adb logcat -b crash -v time                          # the crash buffer: the last fatal stack trace
adb logcat --pid=$(adb shell pidof -s com.pebblentn.app)   # everything the running app logs
```

Add `-d` to dump the buffer and exit instead of following it, and `adb logcat -c` to clear it before
reproducing.

### Run it in an emulator

The emulator has **no Bluetooth stack**, so PebbleKit cannot reach a watch there. It still exercises
everything upstream of the watch — notification access, the package allowlist gate, rule matching,
and the debug history with the match trace — which is what you want for rule work. Pair it with the
fixture publisher (below) to inject synthetic turn notifications. For the phone↔watch half you need
a real phone and a Pebble.

**One-time setup**

```bash
# 1. Hardware acceleration (Linux/WSL2). Without KVM the emulator is unusably slow.
sudo usermod -aG kvm $USER
#    then re-login — on WSL2 run 'wsl --shutdown' from Windows so the group takes effect.
#    (WSL2 also needs nested virtualisation; check that /dev/kvm exists.)

# 2. Emulator + a system image (~1.5 GB).
sdkmanager 'emulator' 'system-images;android-34;google_apis;x86_64'

# 3. The AVD. Use this name — it is the default that run-android-emulator.sh expects.
avdmanager create avd -n pebblentn-api34 -k 'system-images;android-34;google_apis;x86_64'
```

`sdkmanager` and `avdmanager` live in `$ANDROID_HOME/cmdline-tools/latest/bin`. Do not run two
`sdkmanager` installs at once — concurrent runs corrupt the downloaded image.

**Every time**

```bash
./scripts/run-android-emulator.sh              # blocks; give it its own terminal. [avd-name] optional
./scripts/install-android-debug.sh             # in a second terminal: build + install + launch hint
```

On WSL2 the emulator window appears via WSLg. `run-android-emulator.sh` finds the `emulator` binary
under the SDK even when it is not on `PATH`, and warns if `/dev/kvm` is not usable.

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

## 5. Git hooks (recommended)

```bash
./scripts/install-git-hooks.sh     # once per clone
```

Points git at the tracked hooks in `.githooks/`. The pre-commit hook runs, cheapest first:

1. **secret scan** — refuses keystores, `local.properties`, `.env`, and private keys / signing
   passwords / API tokens in the staged diff;
2. **spec-asset integrity** — protected files (`requirements/`, `schemas/`, `spec/`) must match
   `MANIFEST.sha256.json`. Changing one is fine; you must update its hash in the same commit;
3. **generated protocol** — `Protocol.kt` / `protocol.h` must match `protocol-definition.json`;
4. **rules** — schema validation + the fixture regression, when `rules/` is touched;
5. **syntax** — shell, YAML, Python, JSON;
6. **Android unit tests + lint** — only when `android/` is touched (the slow step).

```bash
git commit --no-verify      # skip the hook
PEBBLENTN_SKIP_GRADLE=1 …   # skip only the slow Android step
```

## 6. One-shot everything

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

## 7. Versioning and releases

The version lives in **`VERSION`** at the repo root — the single source of truth. Three other files
must agree with it (the phone reports its version to the watch, and vice versa, in the handshake):
`android/app/build.gradle.kts` (`versionName` + `versionCode`), `watchapp/package.json`, and
`watchapp/src/c/main.c` (`APP_VERSION_STR`).

```bash
./scripts/version.sh                # print the current version
./scripts/version.sh --check        # verify all four files agree (pre-commit + CI run this)
./scripts/version.sh bump patch     # 0.1.2 -> 0.1.3, and sync every file
./scripts/version.sh set 1.0.0      # exact version
```

`versionCode` is derived from the semver (`major*10000 + minor*100 + patch`), so it is monotonic and
needs no separate bookkeeping.

**Every successful push to `main` publishes a GitHub release** (`.github/workflows/release.yml`):

1. `verify` runs the same checks as CI — nothing is tagged or published from a broken commit;
2. the patch version is bumped and committed (`chore(release): vX.Y.Z [skip ci]`) and tagged;
3. a changelog is generated from the commits since the previous tag, grouped by Conventional Commit
   type (`scripts/changelog.py`);
4. the release is published with **`pebble-ntn.apk`**, **`pebble-ntn.pbw`** and `SHA256SUMS.txt`.

Use **Run workflow** on the release workflow to choose a `minor` or `major` bump instead of `patch`.

> The APK is **debug-signed** until the release keystore secrets (`UPLOAD_KEYSTORE_BASE64`,
> `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are configured. It installs fine; it is not a
> Play-grade artifact, and the release notes say so. The signed **AAB** for Play is a separate,
> manually triggered workflow (`play-release.yml`).

## 8. CI

GitHub Actions build and gate PRs: `android.yml` (unit tests, lint, APK), `watchapp.yml` (.pbw),
`rules.yml` (schema/regression), `release.yml` (signed AAB, tag-triggered), `codeql.yml`.
