# fixture-publisher

Debug-only Android app that posts controlled, synthetic navigation-like notifications so the
PebbleNTN listener, allowlist and (later) rule engine can be exercised without a real navigation
app (spec/500-testing/Testing.md → "Synthetic notification publisher").

- Package: `com.pebblentn.fixturepublisher` (registered in `rules/catalog/navigation-apps.json` as a
  capture-only entry, so PebbleNTN captures it **only when this app is actually installed** — the
  entry is inert for normal users).
- UI: one button per fixture (see `NavigationFixtures`) posting an ongoing, navigation-category
  notification, plus Clear.
- Build/install: `./gradlew :fixture-publisher:installDebug` (needs a device/emulator), then enable
  "PebbleNTN Fixtures" in the PebbleNTN app and grant notification access.

Introduced in **M3 — Debug capture**.
