# Security Test Instructions — Vela Maps support (#120)

- One new `<queries>` package (`app.vela`); still no `QUERY_ALL_PACKAGES`. No new permission and no network use in the app.
- On Google Play installs nothing about Vela is listed or linked (no outside download link): covered by `VelaMapsNavigationAppsTest` and the Play-view catalog tests.
- The route-capture harness only downloads Vela's public GitHub release APK into the emulator; it never touches a phone.
