# Security Test Instructions: Watch settings help text (#139)

## Scope

The only security or privacy requirement is NFR5: no change to what the app reads, stores or sends. The change adds UI text, specs and images, and no permission, network call, storage or input handling.

## Checks

```bash
# No manifest, permission, dependency or network change
git diff main --stat -- android/app/src/main/AndroidManifest.xml android/gradle/libs.versions.toml android/app/build.gradle.kts
# No new network or storage API use in the changed Kotlin
git diff main -- 'android/app/src/main/java/**/*.kt' | grep -E '^\+' | grep -E 'HttpURLConnection|OkHttp|URL\(|DataStore|Room|SharedPreferences|Intent\.ACTION_SEND' || echo "none"
# No personal data in new strings or images
git diff main -- android/app/src/main/res/values/strings.xml | grep -E '^\+' | grep -iE '@|https?://' || echo "none"
```

Pass when the first command prints nothing and the other two print "none". Lint (`gradlew lint`, part of `scripts/test-all.sh`) covers Android security lint rules.

## Not applicable

SAST beyond lint, DAST, auth and injection testing: there is no new input surface, endpoint or credential.
