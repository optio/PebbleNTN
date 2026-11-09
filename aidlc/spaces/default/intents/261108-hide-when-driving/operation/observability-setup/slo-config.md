# SLO Configuration: "When driving" (#132)

There is no service to measure continuously, so these are release-quality objectives checked per release and per report:

| Objective | Target | Measured by |
|---|---|---|
| No false hide on foot or bike | 0 hides in the walking and cycling replay fixtures; 0 confirmed user reports per release | `DrivingEmulatorReplayTest` (every build), GitHub issues |
| Hides when driving | hide within 90 s of driving above the threshold in the car fixture | `DrivingEmulatorReplayTest` |
| No flapping | at most 1 close and 1 reopen per 2-minute window | `DrivingPolicyTest` (NFR2) |
| Unchanged default | with default settings, no close ever and the same sends as before | `NavigationControllerTest` (NFR6) |
| Stability | crash-free sessions ≥ 99.5 % over a 28-day window in Play Android vitals | Play Console (once enough Play installs exist) |
