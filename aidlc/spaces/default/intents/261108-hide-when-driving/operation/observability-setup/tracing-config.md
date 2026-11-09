# Tracing Configuration: "When driving" (#132)

Not applicable: there is no distributed system to trace. The path of one decision is fully visible on the device: notification → `DebugCaptureProcessor` → `NavigationController.onSpeedSample` → `DrivingSpeedEstimator` → `DrivingPolicy` → `WatchTransport.closeApp` / `launchApp`, with each step logged (see `log-queries.md`) and each estimate and decision recorded in the driving log.

For a reproducible trace of a report, capture the speed fields on the emulator with route-capture and add them as a fixture to `DrivingEmulatorReplayTest` (`android/app/src/test/resources/driving/`), as done for the car, bike and foot runs.
