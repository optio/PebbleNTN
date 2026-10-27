# Health Check Report — v0.0.78

## Status: Healthy

- **Release:** the release workflow succeeded and published v0.0.78 at 2026-10-27T21:58:23Z, with the target-SDK gate passing.
- **Files:** the APK and PBW match their checksums.
- **Play bundle:** the `play-release` bundle for v0.0.78 is ready to upload to Play's internal testing track.
- **Server side:** there isn't one.

## Follow-ups for the Maintainer

- **Pebble store:** upload the v0.0.78 PBW, because the watchapp changed in #114.
- **Google Play:**
  - download `release-aab` from https://github.com/optio/PebbleNTN/actions/runs/40591210375;
  - upload it to the internal testing track;
  - at that first upload, enrol App Signing with the existing release key (PEPK), using `~/.pebblentn-play-upload/upload_certificate.pem` as the upload certificate.
- **Your phone:** uninstall the debug test build, then install v0.0.78.
- **#113:** follow up with the reporter (MicroG Maps directions, and turn arrows on icon-only cards).
