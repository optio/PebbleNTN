# Security and Privacy Test Instructions: "When driving" (#132)

The change adds no network, location or account features. The checks are about privacy (NFR1, FR14.1):

- **No new permission:** `android/app/src/main/AndroidManifest.xml` is unchanged by this branch (`git diff main -- android/app/src/main/AndroidManifest.xml` is empty).
- **No location or network use:** the speed is derived from the navigation notification's own progress and arrival-time fields; no location API or network call is added (review of `DrivingSpeedEstimator.kt`, `DrivingPolicy.kt`, `NavigationController.kt`).
- **Driving log holds no text:** `DrivingLogEntry` has only numbers, times and a decision enum; `DrivingLogRepositoryTest` asserts there is no string field, and the export tests check the section in both export modes.
- **Secrets:** the pre-commit hook's secret and local-path scan runs on commit.
- **Play data safety:** the answers in `docs/PLAY_DATA_SAFETY.md` stay valid (data processed on device only; the export is user-initiated).
