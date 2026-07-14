# Privacy Review (M9)

This review records the privacy boundaries PebbleNTN commits to and how each is enforced and
verified in code. It complements the normative requirements in `requirements/REQ-SECURITY.md` and
`spec/600-security-release/SecurityPrivacy.md`.

## Boundaries and enforcement

| Boundary | Requirement | Enforcement | Verified by |
|---|---|---|---|
| Master switch **before** reading content | REQ-ANDROID-011 | `NotificationDispatcher` checks the user's global toggle before the allowlist and before the snapshot builder; disabling also ends navigation on the watch. Default on; persisted in `AppEnabledRepository` (`@Volatile` cache, no I/O on the callback thread) | `NotificationDispatcherTest.masterSwitchOffProcessesNothingAndNeverBuildsContent`, `AppEnabledRepositoryTest` |
| Package allowlist **before** reading content | REQ-ANDROID-003 | `NotificationDispatcher` consults `AppAllowlist.isEnabled` before the snapshot builder is ever invoked; the listener passes only the package name + post time to the dispatcher | `NotificationDispatcherTest.disabledPackageIsNeverProcessedAndContentNeverBuilt` (content builder never runs for a disabled package) |
| Synchronous, non-blocking allowlist on the callback thread | REQ-ANDROID-003/006 | `EnabledAppRepository` keeps a `@Volatile` cache; `isEnabled` reads only the cache, never the DB | `EnabledAppRepositoryTest` |
| No debug record for disabled packages | REQ-DEBUG-002 | Only eligible (allowlisted) posts reach `DebugCaptureProcessor` | dispatcher + processor tests |
| Minimal snapshot (no PendingIntents/actions/RemoteViews/icons/bundles) | REQ-SEC-003 | `NotificationSnapshotFactory` reads only the documented extras keys; `NotificationSnapshot` has no field for anything else | `NotificationSnapshotFactoryTest.onlyDocumentedExtrasAreCaptured` |
| Notification keys/tags stored hashed, never raw | Database.md privacy | `DebugHistoryRepository` hashes key/tag with SHA-256 | `DebugHistoryRepositoryTest.keyAndTagAreStoredHashedNotRaw` |
| No automatic upload of content/diagnostics | REQ-SEC-002, REQ-DEBUG-006 | There is no network code in the app; exports are user-initiated only via the Sharesheet | code audit (no HTTP client present); `DiagnosticShareManager` uses `ACTION_SEND` |
| Prominent disclosure before granting access | REQ-SEC-004 | Onboarding shows the exact disclosure string before opening listener settings | `OnboardingScreen` (string `onboarding_disclosure`) |
| Privacy warning before full diagnostic export | REQ-DEBUG-007 | Full export requires acknowledging the exact warning dialog | `DebugHistoryScreen.ExportDialog` (string `export_privacy_warning`) |
| Privacy-safe export redaction | ExportFormat.md | `Redactor` keeps structural tokens/units/maneuver keywords, replaces road/destination words | `RedactorTest`, `ExportBuilderTest` |
| Temporary export URIs with cleanup | REQ-SEC-008 | Files under `cache/exports` via a `FileProvider` cache-path; pruned on each export | `DiagnosticShareManager`; `res/xml/file_paths.xml` |
| Last-known-state, never a replayed queue | REQ-ANDROID-010 | `NavigationSessionReducer` holds one current state; `NavigationStateRepository` persists/restores only that state (readiness reset) | reducer tests, `NavigationStateRepositoryTest` |
| No secrets in repo/rules/fixtures | REQ-SEC-007 | No keys/tokens committed; signing via CI environment secrets only | code/rule audit |

## Performance / resource bounds (hardening)

- **Event-driven, no polling** (REQ-ANDROID-007): the app reacts to `NotificationListenerService`
  callbacks and Pebble messages; there is no periodic poll of notifications, connection or GPS.
  Staleness is computed at send time, not by a timer.
- **Serialized processing off the callback thread** (REQ-ANDROID-006): `SerialProcessingQueue`.
- **Regex safety** (RuleEngine): `SafeRegex` bounds pattern length (1024), tested input (4096) and
  applies a per-evaluation time budget that aborts catastrophic backtracking; a timed-out rule is
  disabled for that run and traced (`ConditionEvaluatorTest`, `RuleEngineTest`).
- **Bounded rules per package**: `RuleEngine.MAX_RULES_PER_PACKAGE = 500`
  (`RuleEngineTest.evaluationIsBoundedByMaxRulesPerPackage`).
- **Bounded debug retention** (REQ-ANDROID-008): default 500, transactional trim.

## Residual items (tracked)

- On-device verification of the phone↔watch flow and the instrumented "disabled package never
  read" spy (REQ-ANDROID-003 acceptance) run in CI / on hardware — see `docs/IMPLEMENTATION_STATUS.md`.
- Remote official-rule authenticity (REQ-SEC-005/006) is implemented in M11 and remains disabled by
  default until separately approved.
