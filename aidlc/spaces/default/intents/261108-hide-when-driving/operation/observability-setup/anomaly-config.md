# Anomaly Configuration: "When driving" (#132)

Built-in anomaly handling on the device (no remote detection):

- **Implausible speeds:** measured or planned speeds above 200 km/h are discarded as progress jumps (`DrivingSpeedEstimator.MAX_PLAUSIBLE_KMH`).
- **Route re-plans and reroutes:** a new route length or a growing remaining distance restarts the speed window; "Rerouting…" cards are ignored (found and fixed through the emulator replay, Loop-back 1).
- **Too little history:** no measured speed before 30 s of samples.
- **Flapping:** at most one hide and one show per 2 minutes.

Patterns to look for in reported `drivingLog`s: SAMPLE speeds that jump between consecutive entries (a new kind of progress jump), HIDE shortly after a session start on a walk (a planned speed that is too high), or no SHOW after a long stop (Google Maps not posting while standing still).
