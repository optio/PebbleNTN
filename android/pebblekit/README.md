# Vendored PebbleKit Android 2

This module is a **vendored copy of the source** of Rebble's PebbleKit Android 2
(`io.rebble.pebblekit2`), the library the app uses to talk to the Pebble/Rebble
companion app on the phone.

- **Upstream:** https://github.com/pebble-dev/PebbleKitAndroid2
- **Version:** 1.2.0 (git tag `1.2.0`, commit `55b7a6826e47e3d80ac2e7eb388b76a202df798f`)
- **License:** Apache-2.0 (see `LICENSE` in this folder)

## Why it's vendored

Upstream publishes only to **JitPack**, which serves non-reproducible prebuilt
artifacts. F-Droid requires the whole app to build from source with no such
dependency, so the source is copied here instead of pulled from JitPack.

## What was copied

Upstream splits the code into four small Android library modules — `common-api`,
`common`, `client-api`, `client`. Their `src/main` Kotlin, the AIDL, and the
`client` module's `AndroidManifest.xml` (`<queries>` for the send-to-watch intent)
are flattened into this one module verbatim. No source was modified; only the
Gradle build files were replaced with `build.gradle.kts` here so the code compiles
under this project's own toolchain. The unused upstream modules (`client-ui`,
`client-java`, `server*`, `sample`) are not included.

## Updating

To move to a newer PebbleKit release: check out the new tag of the upstream repo,
re-copy the `src/main` trees of those four modules over `src/main` here, update the
version/commit above, and confirm `dependencies` still matches upstream's (currently:
kotlinx-coroutines, androidx datastore-preferences, androidx core-ktx, androidx
annotation, co.touchlab kermit).
