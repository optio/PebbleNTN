# Performance Test Instructions: "When driving" (#132)

The requirements set no latency or throughput targets, so no load or benchmark tests apply (Minimal strategy).

The one resource target is the watch memory budget (NFR4, REQ-WATCH-025): `scripts/build-watchapp.sh` prints the aplite free heap at load. Measured at this change: 3,482 B free (before: 3,615 B), within the budget with about 60 B to spare. Re-check it whenever watch menu code changes.

On the phone, the speed estimator keeps at most 60 s of samples and the driving log at most 500 entries, so memory use is bounded by design (unit-tested in `DrivingSpeedEstimatorTest` and `DrivingLogRepositoryTest`).
