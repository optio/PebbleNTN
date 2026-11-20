# Performance Test Instructions: Watch settings help text (#139)

## Scope

There is no runtime performance requirement. The change adds static text to two Compose screens and nothing on the notification-to-watch path. The only measurable size target is NFR4: the release APK grows by less than 50 KB.

## APK size check (NFR4)

```bash
# On the branch
android/gradlew -p android :app:assembleRelease
stat -c %s android/app/build/outputs/apk/release/app-release-unsigned.apk

# Baseline: the same build of main in a separate worktree
git worktree add aidlc/spaces/default/intents/261120-watch-settings-help/.aidlc-engine/main-size main
aidlc/spaces/default/intents/261120-watch-settings-help/.aidlc-engine/main-size/android/gradlew -p aidlc/spaces/default/intents/261120-watch-settings-help/.aidlc-engine/main-size/android :app:assembleRelease
stat -c %s aidlc/spaces/default/intents/261120-watch-settings-help/.aidlc-engine/main-size/android/app/build/outputs/apk/release/app-release-unsigned.apk
git worktree remove aidlc/spaces/default/intents/261120-watch-settings-help/.aidlc-engine/main-size
```

Pass when the branch APK minus the main APK is less than 51,200 bytes. Both are unsigned, minified and resource-shrunk, so the signature makes no difference to the comparison.

## Not applicable

- Load, soak, spike and latency tests: there is no service and no hot path.
- Rendering performance: there are about 30 extra short text lines per screen, which is negligible for Compose. The rows grow in height but are not re-laid out differently.
