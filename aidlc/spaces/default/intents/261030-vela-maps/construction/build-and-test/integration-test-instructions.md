# Integration Test Instructions — Vela Maps support (#120)

## Automated

- **`VelaMapsRulesRegressionTest` and the workbench regression:** every fixture in `rules/fixtures/vela-maps.json`, synthetic and captured.
- **Play view:** `NavigationAppCatalogTest`, `VelaMapsEnablementTest`, `VelaMapsNavigationAppsTest`, `OfficialRuleGroupsTest`. On Play, Vela is hidden but matched and allowlisted; outside Play, it's listed with its website.
- **Route-capture harness tests:** the driver, the release asset pattern, the US route and Vela's plan.

## Route Capture (done in this stage)

```bash
cd tools/route-capture
./routecap.py install vela-maps
./routecap.py run scenarios/vela-maps-m1.json            # 8 scenarios, about 45 min; add --publish to file an issue for misses
```

The Austin en-US route (`routes/austin-downtown.json`) is available to the other apps' plans for a later imperial run.

## On a Phone (after release)

With Vela Maps installed from its GitHub releases (not Play): Navigation apps lists Vela Maps. A drive shows turn arrows, distances and the ETA on the watch.
