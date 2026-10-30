# Build Instructions — install/update source (#119) and a second MicroG Maps package (#113)

```bash
source ~/.local/pebblentn-env.sh
android/gradlew -p android :app:assembleDebug
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.update.*' --tests 'com.pebblentn.app.export.*' --tests 'com.pebblentn.app.ui.dashboard.*' --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.rules.*'
scripts/validate-rules.sh
scripts/validate-spec-assets.sh
scripts/test-all.sh
```

Merging to `main` releases v0.0.80 (versionCode 80) through `release.yml`. The Play bundle then comes from the `play-release` workflow for `v0.0.80`.
