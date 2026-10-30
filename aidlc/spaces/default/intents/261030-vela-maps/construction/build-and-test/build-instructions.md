# Build Instructions — Vela Maps support (#120)

```bash
source ~/.local/pebblentn-env.sh
android/gradlew -p android :app:assembleDebug
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.rules.*' --tests 'com.pebblentn.app.ui.*' --tests 'com.pebblentn.app.data.*'
scripts/validate-rules.sh
scripts/validate-spec-assets.sh
scripts/test-all.sh            # includes the route-capture harness tests
```

Route capture (Windows emulator driven from WSL, never inside WSL):

```bash
cd tools/route-capture
./routecap.py install vela-maps
./routecap.py run scenarios/vela-maps-m1.json --only 'london|austin'
./routecap.py run scenarios/vela-maps-m1.json --publish   # full plan, about 1 h
```

Merging to `main` releases v0.0.81 (versionCode 81) through `release.yml`; the Play bundle follows from `play-release` for `v0.0.81`.
