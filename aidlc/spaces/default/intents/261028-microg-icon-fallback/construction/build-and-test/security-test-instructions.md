# Security Test Instructions — MicroG Maps turn arrows (#113 follow-up)

- The retry opens no other app's context: only the posting app's own resources are read.
- The fallback package always comes from the catalog. `RuleFamilies.of` rejects a root outside the package's own family (tested).
- No new permissions or network use.
