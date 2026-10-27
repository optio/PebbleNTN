# Performance Test Instructions — MicroG Maps as a Google Maps variant (#113)

There are no load or latency targets. The rule family is a map built once from the catalog, and each rule's package check stays a set lookup, so matching costs the same as before. The APK grows only by the catalog entry and a few strings.
