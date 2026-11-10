# Performance Test Instructions: #134

There is no load or service performance target. The two measurable targets are:

- **NFR3, rule cost:** each OsmAnd notification must match in under 5 ms on the JVM. Check: the `everyFixtureExtractsExpectedOutput` case time in `android/app/build/test-results/testDebugUnitTest/TEST-com.pebblentn.app.rules.OsmandRulesRegressionTest.xml`, divided by the number of OsmAnd fixtures (243). The validator's pattern limits are checked by `bundledRulesetIsValid` in the same class.
- **aplite heap (REQ-WATCH-025):** the free heap at load, printed by `scripts/build-watchapp.sh`, must be at least 3,282 B (no more than 200 B below 3,482 B).
