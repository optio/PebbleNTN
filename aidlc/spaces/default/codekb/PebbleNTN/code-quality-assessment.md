# Code Quality Assessment

## Test Coverage

| Area | Tests | Runner | In CI |
|---|---|---|---|
| Android app | 66 JVM/Robolectric files (reducer, codec, controller with `FakeWatchTransport`, rules regressions per app, repositories, Room migrations, view models, Compose rule review) | `./gradlew test` | yes (android.yml, release.yml) |
| Android instrumentation | 1 (`ManualOpenWithAutoLaunchOffTest`) | `connectedDebugAndroidTest` | no |
| Watchapp pure units | 6 host C tests (about_text, early_warning, eta_text, glance_text, panel_layout, stops_text) | scripts/test-watchapp-unit.sh | **no** - only scripts/test-all.sh |
| Watchapp UI | emulator acceptance per REQ (manual / capture_screenshots.py) | Pebble emulator | no |
| Rules | schema validation + fixture regression | validate-rules.sh, workbench.py regression | yes (rules.yml, release.yml) |
| Route capture harness | Python unittest | test-all.sh | no |

No coverage tooling (no JaCoCo/Kover, no gcov). `PebbleWatchTransport` is documented as not unit-testable (bound companion app) and verified on device.

## Linting and Formatting

- Android Lint runs in CI and pre-commit; no `lint.xml` baseline. Kotlin official style flag only; no ktlint/detekt/.editorconfig.
- C: `-Wall -Wextra -Werror` for host tests only; no clang-format or static analysis for the ARM build.
- CodeQL (java-kotlin) on the Android build.

## CI/CD

- **android.yml**: protocol `--check`, test, lint, assembleDebug (paths `android/**`).
- **watchapp.yml**: protocol `--check`, `pebble build` (paths `watchapp/**`); SDK installed as `latest`.
- **rules.yml**: spec integrity, schema validation, regression.
- **release.yml**: on push to `main`: verify (spec, protocol, rules, version sync, Android test + lint), bump version, changelog, signed APK, PBW, checksums, tag, GitHub Release.
- **play-release.yml**: signed AAB.
- **.githooks/pre-commit**: secrets, spec integrity, protocol, version sync, rules, syntax, Android test + lint when android/ changed.

## Documentation Quality

Good: numbered specs, requirement IDs and issue numbers in code comments, AGENTS.md contract, READMEs per area, docs/IMPLEMENTATION_STATUS.md. Spec and requirement files are SHA-256 protected by MANIFEST.sha256.json; changing one requires updating the manifest in the same commit.

## Quick Ratings

| Criterion | Rating | Evidence |
|---|---|---|
| Naming clarity | good | intent-revealing names on both sides |
| Function size | fair | `panel_update_proc`, `strip_update_proc`, `render_navigation` are long |
| Error handling | good | transport failures caught and logged; send retry with backoff; watch NULL-checks bitmaps and tuples |
| Test presence | good (phone) / fair (watch) | watch tests cover pure units only |
| Duplication | fair | three near-identical switch blocks in `maneuver_resource_id` |
| Dead code | good | 1 TODO in the repo (play-release.yml) |

## Technical Debt

| # | Item | Location | Impact |
|---|---|---|---|
| TD1 | Watch host unit tests not run in CI or pre-commit | .github/workflows/watchapp.yml, .githooks/pre-commit | regressions in pure C units can merge |
| TD2 | God file | watchapp/src/c/main.c (1078 lines), settings_window.c (741) | harder review and change isolation |
| TD3 | Contradictory protocol docs: README names examples/ as source of truth and says "Placeholder in M0"; JSON `_comment` cites missing `scripts/check-protocol.py` | protocol/README.md, protocol/protocol-definition.json | misleads contributors |
| TD4 | `rules/test-cases/` documented but absent | rules/README.md | stale docs |
| TD5 | Pebble.md UI section predates the current panel/strip layout | spec/200-architecture/Pebble.md vs spec/400-ui/WatchUI.md | spec drift |
| TD6 | Committed binaries: `scripts/__pycache__/changelog.cpython-312.pyc` (ignored by `.gitignore` but tracked), `tools/route-capture/device/routecap-locale.dex` | scripts/, tools/route-capture/device/ | repo hygiene |
| TD7 | Release target-SDK check failure swallowed with `|| echo "TODO(M10) ..."` | .github/workflows/play-release.yml:38 | silent policy bypass |
| TD8 | Pebble SDK installed as `latest` (unpinned) | watchapp.yml, release.yml | non-reproducible watch builds |
| TD9 | Fixed `s_details[80]` About buffer truncates silently | watchapp/src/c/about_window.c | adding lines can cut text |

## Intent Impact (#103)

Constraints for adding the companion version to About (watch-only; protocol detail in [api-documentation.md](api-documentation.md)):
- Amend REQ-WATCH-020 in requirements/REQ-WATCH.md and update MANIFEST.sha256.json in the same commit.
- Persist the version under a new key (16 is the next free key in theme.c) so "last connected" survives relaunch; handle "never seen" (older phone, compatibility-error path).
- Keep the details text within the 80-byte buffer (TD9) or enlarge it; re-check round-display layout on chalk per REQ-WATCH-020 acceptance.
- Extend watchapp/tests/test_about_text.c; it only runs through scripts/test-all.sh (TD1).
- Do not hand-edit generated protocol files (AGENTS.md rule 5).
