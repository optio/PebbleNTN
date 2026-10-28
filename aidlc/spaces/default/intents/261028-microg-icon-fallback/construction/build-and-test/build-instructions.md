# Build Instructions — MicroG Maps turn arrows (#113 follow-up)

```bash
source ~/.local/pebblentn-env.sh
android/gradlew -p android :app:assembleDebug
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.notification.*'
scripts/test-all.sh
```

Merging to `main` releases v0.0.79 (versionCode 79) through `release.yml`. The Play bundle then comes from the `play-release` workflow for `v0.0.79`, once the new upload key is valid (2026-10-30 00:14 UTC).
