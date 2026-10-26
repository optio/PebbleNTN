# Health Check Report — #114 merge (no release)

## Status: Healthy

- **`main`:** has the Play preparation, and nothing was released.
- **Next release:** a normal push to `main` (without `[skip ci]`) releases these changes as the next version, with targetSdk 36. The release workflow now runs the target-SDK gate first.

## Follow-ups for the Maintainer (see `docs/PLAY_CONSOLE.md`)

- **Before the first `play-release` run:** create the Play upload key and add the four `PLAY_UPLOAD_*` secrets to the GitHub `release` environment.
- **At the first upload:** enrol Play App Signing with the existing release key (PEPK).
- **In Play Console:** fill in the forms (app access, content rating, target audience, Data safety, privacy policy URL).
- **Before production:** run the closed test with 12 testers for 14 days, and complete developer verification for `com.pebblentn.app` and the release key.
- **After the first internal-testing install:** check the "Update from Google Play" card.
- **Your phone:** it has the debug test build. Uninstall it before installing the next release.
