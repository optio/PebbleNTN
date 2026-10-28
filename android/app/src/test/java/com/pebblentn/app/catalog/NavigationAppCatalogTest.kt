package com.pebblentn.app.catalog

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationAppCatalogTest {

    private fun bundledCatalogJson(): String =
        javaClass.getResourceAsStream("/catalog/navigation-apps.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("bundled catalog resource not found on test classpath")

    @Test
    fun bundledCatalogParsesAndIsValid() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        assertEquals(NavigationAppCatalog.SUPPORTED_SCHEMA_VERSION, catalog.schemaVersion)
        assertTrue("catalog should not be empty", catalog.apps.isNotEmpty())
    }

    @Test
    fun googleMapsHasOfficialRulesAndOthersAreCaptureOnly() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        val maps = catalog.entryForPackage("com.google.android.apps.maps")
        assertNotNull(maps)
        assertTrue(maps!!.hasOfficialRules)
        assertFalse(maps.captureOnly)

        val waze = catalog.entryForPackage("com.waze")
        assertNotNull(waze)
        assertTrue("Waze has no official rules yet", waze!!.captureOnly)
    }

    @Test
    fun commonNavigationAppsAreDetectedAndCaptureOnly() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        // Package name -> expected catalog appId. Locks the identifiers so a typo or an accidental
        // removal fails loudly. Whether each is capture-only is checked against the bundled rules in
        // captureOnlyMatchesTheBundledRules.
        val expected = mapOf(
            "app.morphe.android.apps.maps" to "microg-maps", // Google Maps patched for microG (#113)
            "com.waze" to "waze",
            "net.osmand" to "osmand",
            "net.osmand.plus" to "osmand",
            "net.osmand.dev" to "osmand", // OsmAnd Nightly (#54)
            "net.osmand.huawei" to "osmand",
            "app.organicmaps" to "organic-maps",
            "app.organicmaps.web" to "organic-maps", // GitHub release build (#54)
            "app.comaps.google" to "comaps",
            "app.comaps.fdroid" to "comaps",
            "app.comaps" to "comaps", // Codeberg/GitHub release and IzzyOnDroid build (#54)
            "app.comaps.huawei" to "comaps",
            "com.mapswithme.maps.pro" to "maps-me",
            "cz.seznam.mapy" to "mapy-com",
            "com.autonavi.minimap" to "amap",
            "com.here.app.maps" to "here-wego",
            "com.sygic.aura" to "sygic",
            "ru.yandex.yandexnavi" to "yandex-navigator",
            "ru.yandex.yandexmaps" to "yandex-maps",
            "com.huawei.maps.app" to "petal-maps",
            "com.generalmagic.magicearth" to "magic-earth",
            "com.tomtom.gplay.navapp" to "tomtom-go",
            "com.tomtom.speedcams.android.map" to "tomtom-amigo",
            "ru.dublgis.dgismobile" to "two-gis",
            "com.baidu.BaiduMap" to "baidu-maps",
        )
        for ((pkg, appId) in expected) {
            val entry = catalog.entryForPackage(pkg)
            assertNotNull("expected catalog entry for $pkg", entry)
            assertEquals("wrong app for $pkg", appId, entry!!.appId)
        }
    }

    /**
     * `hasOfficialRules` drives the "Capture only" label in the app. It drifted once (OsmAnd, Organic
     * Maps and CoMaps had rules but were still marked capture-only), so it is derived-checked here.
     */
    @Test
    fun captureOnlyMatchesTheBundledRules() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        val bundledDir = java.io.File(javaClass.getResource("/rules/bundled")!!.toURI())
        val packagesWithRules = bundledDir.walk().filter { it.isFile && it.extension == "json" }
            .flatMap { com.pebblentn.app.rules.RulesetCodec.parse(it.readText()).rules.asSequence() }
            .flatMap { it.packageNames.asSequence() }
            .toSet()
        val families = catalog.ruleFamilies()
        for (app in catalog.apps) {
            // An app using another's rules (#113) has rules exactly when its family has.
            val hasRules = app.packageNames.any { pkg -> families.familyOf(pkg).any { it in packagesWithRules } }
            assertEquals("${app.appId}: hasOfficialRules must match the bundled rules", hasRules, app.hasOfficialRules)
        }
    }

    /**
     * Each bundled ruleset lives in `rules/bundled/<appId>/`, and every rule in it must list exactly
     * that app's own catalog packages. Alternative builds (GitHub, IzzyOnDroid, nightly, AppGallery)
     * use other package names; one added to the catalog but not to a ruleset was detected yet never
     * matched (#54). This fails as soon as one place is forgotten. An app that uses another app's
     * rules (`rulesFrom`, #113) gets them through its rule family, so it has no folder and no rule
     * lists its packages.
     */
    @Test
    fun everyBundledRuleCoversExactlyItsRootAppsOwnPackages() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        val bundledDir = java.io.File(javaClass.getResource("/rules/bundled")!!.toURI())
        val appDirs = bundledDir.listFiles()!!.filter { it.isDirectory }
        assertTrue(appDirs.isNotEmpty())
        for (variant in catalog.apps.filter { it.rulesFrom != null }) {
            assertFalse("${variant.appId} uses ${variant.rulesFrom}'s rules: no folder of its own", appDirs.any { it.name == variant.appId })
        }
        for (dir in appDirs) {
            val app = catalog.apps.firstOrNull { it.appId == dir.name }
            assertNotNull("rules/bundled/${dir.name} has no catalog entry with that appId", app)
            for (file in dir.listFiles()!!.filter { it.extension == "json" }) {
                for (rule in com.pebblentn.app.rules.RulesetCodec.parse(file.readText()).rules) {
                    assertEquals(
                        "${dir.name}/${file.name} ${rule.id}: packageNames must equal the catalog's",
                        app!!.packageNames.toSet(),
                        rule.packageNames.toSet(),
                    )
                }
            }
        }
    }

    @Test
    fun installedCatalogAppsDefaultToEnabled() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        assertTrue(catalog.apps.all { it.defaultEnabled })
    }

    @Test
    fun entryForUnknownPackageIsNull() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        assertNull(catalog.entryForPackage("com.example.unknown"))
    }

    @Test
    fun duplicatePackageAcrossAppsIsRejected() {
        val json = """
            {"schemaVersion":1,"apps":[
              {"appId":"a","displayName":"A","packageNames":["p.kg"],"hasOfficialRules":false,"captureAvailable":true,"defaultEnabled":true},
              {"appId":"b","displayName":"B","packageNames":["p.kg"],"hasOfficialRules":false,"captureAvailable":true,"defaultEnabled":true}
            ]}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
    }

    @Test
    fun unknownFieldIsRejected() {
        val json = """
            {"schemaVersion":1,"apps":[
              {"appId":"a","displayName":"A","packageNames":["p.kg"],"hasOfficialRules":false,"captureAvailable":true,"defaultEnabled":true,"surprise":1}
            ]}
        """.trimIndent()
        assertThrows(SerializationException::class.java) { NavigationAppCatalog.parse(json) }
    }

    @Test
    fun unsupportedSchemaVersionIsRejected() {
        val json = """{"schemaVersion":2,"apps":[]}"""
        assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
    }

    // --- Rule families and MicroG Maps (#113) ---------------------------------------------------

    private fun catalogJson(vararg apps: String) = """{"schemaVersion":1,"apps":[${apps.joinToString(",")}]}"""

    private fun app(id: String, pkg: String, rules: Boolean = true, extra: String = "") =
        """{"appId":"$id","displayName":"$id","packageNames":["$pkg"],"hasOfficialRules":$rules,"captureAvailable":true,"defaultEnabled":true$extra}"""

    @Test
    fun microgMapsUsesTheGoogleMapsRulesWithItsWebsiteAndIsHiddenOnPlay() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        val microg = catalog.entryForPackage("app.morphe.android.apps.maps")!!
        assertEquals("microg-maps", microg.appId)
        assertEquals("MicroG Maps", microg.displayName)
        assertEquals("google-maps", microg.rulesFrom)
        assertEquals("google-maps", microg.rulesAppId)
        assertEquals("https://github.com/fangkampanat/gmaps-patches", microg.website)
        assertFalse(microg.playListed)
        assertTrue(microg.defaultEnabled)
        assertFalse(microg.captureOnly)
        // Google Maps itself is unchanged: its own rules, Play link, listed everywhere.
        val maps = catalog.entryForPackage("com.google.android.apps.maps")!!
        assertEquals(listOf("com.google.android.apps.maps"), maps.packageNames)
        assertNull(maps.rulesFrom)
        assertNull(maps.website)
        assertTrue(maps.playListed)
        assertTrue("only MicroG Maps has a website", catalog.apps.filter { it.website != null }.map { it.appId } == listOf("microg-maps"))
    }

    @Test
    fun rulesFromAnUnknownAppIsRejected() {
        val json = catalogJson(app("a", "p.a", extra = ""","rulesFrom":"nowhere""""))
        val error = assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
        assertTrue(error.message!!.contains("unknown app nowhere"))
    }

    @Test
    fun rulesFromItselfIsRejected() {
        val json = catalogJson(app("a", "p.a", extra = ""","rulesFrom":"a""""))
        val error = assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
        assertTrue(error.message!!.contains("names itself"))
    }

    @Test
    fun aRulesFromChainIsRejected() {
        val json = catalogJson(
            app("root", "p.root"),
            app("middle", "p.middle", extra = ""","rulesFrom":"root""""),
            app("leaf", "p.leaf", extra = ""","rulesFrom":"middle""""),
        )
        val error = assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
        assertTrue(error.message!!.contains("leaf"))
    }

    @Test
    fun rulesFromAnAppWithoutRulesIsRejected() {
        val json = catalogJson(app("root", "p.root", rules = false), app("v", "p.v", extra = ""","rulesFrom":"root""""))
        val error = assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
        assertTrue(error.message!!.contains("no rules of its own"))
    }

    @Test
    fun aWebsiteThatIsNotHttpsIsRejected() {
        val json = catalogJson(app("a", "p.a", extra = ""","website":"http://example.org""""))
        assertThrows(IllegalArgumentException::class.java) { NavigationAppCatalog.parse(json) }
    }

    @Test
    fun theOptionalFieldsDefaultToAnOrdinaryApp() {
        val entry = NavigationAppCatalog.parse(catalogJson(app("a", "p.a"))).apps.single()
        assertNull(entry.rulesFrom)
        assertNull(entry.website)
        assertTrue(entry.playListed)
        assertEquals("a", entry.rulesAppId)
    }

    @Test
    fun ruleFamiliesJoinGoogleMapsAndMicrogMapsAndLeaveOthersAlone() {
        val families = NavigationAppCatalog.parse(bundledCatalogJson()).ruleFamilies()
        val maps = setOf("com.google.android.apps.maps", "app.morphe.android.apps.maps")
        assertEquals(maps, families.familyOf("com.google.android.apps.maps"))
        assertEquals(maps, families.familyOf("app.morphe.android.apps.maps"))
        assertEquals(setOf("app.comaps.google", "app.comaps.fdroid", "app.comaps", "app.comaps.huawei"), families.familyOf("app.comaps"))
        assertEquals(setOf("com.waze"), families.familyOf("com.waze"))
        assertEquals(setOf("com.example.unknown"), families.familyOf("com.example.unknown"))
    }

    @Test
    fun aRuleAppliesToEveryPackageOfItsFamilyOnly() {
        val families = NavigationAppCatalog.parse(bundledCatalogJson()).ruleFamilies()
        val googleRule = listOf("com.google.android.apps.maps")
        val microgRule = listOf("app.morphe.android.apps.maps")
        assertTrue(families.applies("app.morphe.android.apps.maps", googleRule))
        assertTrue(families.applies("com.google.android.apps.maps", microgRule))
        assertTrue(families.applies("com.google.android.apps.maps", googleRule))
        assertFalse(families.applies("com.waze", googleRule))
        assertFalse(families.applies("com.example.unknown", microgRule))
        assertTrue("an unknown package still matches a rule naming it", families.applies("com.example.unknown", listOf("com.example.unknown")))
        assertFalse(RuleFamilies.NONE.applies("app.morphe.android.apps.maps", googleRule))
    }

    @Test
    fun onGooglePlayMicrogMapsIsNoEntryAndItsPackageJoinsGoogleMaps() {
        val play = NavigationAppCatalog.parse(bundledCatalogJson()).forGooglePlay()
        assertTrue(play.apps.none { it.appId == "microg-maps" })
        assertTrue("no website is linked on Play", play.apps.all { it.website == null })
        val maps = play.entryForPackage("app.morphe.android.apps.maps")!!
        assertEquals("google-maps", maps.appId)
        assertEquals("Google Maps", maps.displayName)
        assertEquals(listOf("com.google.android.apps.maps", "app.morphe.android.apps.maps"), maps.packageNames)
        assertEquals(26, play.allPackageNames.size - 1) // the fixture publisher aside, nothing is lost
        assertEquals(
            NavigationAppCatalog.parse(bundledCatalogJson()).ruleFamilies().familyOf("com.google.android.apps.maps"),
            play.ruleFamilies().familyOf("com.google.android.apps.maps"),
        )
    }

    @Test
    fun anUnlistedAppWithoutRulesFromIsLeftOutOnPlay() {
        val catalog = NavigationAppCatalog.parse(catalogJson(app("a", "p.a"), app("b", "p.b", extra = ""","playListed":false""")))
        assertEquals(listOf("a"), catalog.forGooglePlay().apps.map { it.appId })
        assertEquals(listOf("a", "b"), catalog.apps.map { it.appId })
    }

    @Test
    fun microgMapsFallsBackToTheGoogleMapsPackageOutsideAndOnPlay() {
        val bundled = NavigationAppCatalog.parse(bundledCatalogJson())
        for (families in listOf(bundled.ruleFamilies(), bundled.forGooglePlay().ruleFamilies())) {
            assertEquals("com.google.android.apps.maps", families.resourceFallbackFor("app.morphe.android.apps.maps"))
        }
    }

    @Test
    fun onlyAPackageUsingAnotherAppsRulesHasAFallback() {
        val bundled = NavigationAppCatalog.parse(bundledCatalogJson())
        for (families in listOf(bundled.ruleFamilies(), bundled.forGooglePlay().ruleFamilies())) {
            assertNull("the root itself", families.resourceFallbackFor("com.google.android.apps.maps"))
            assertNull("a CoMaps variant", families.resourceFallbackFor("app.comaps.fdroid"))
            assertNull("an Organic Maps variant", families.resourceFallbackFor("app.organicmaps.web"))
            assertNull("an OsmAnd variant", families.resourceFallbackFor("net.osmand.plus"))
            assertNull("an unknown package", families.resourceFallbackFor("com.example.unknown"))
        }
        assertNull(RuleFamilies.NONE.resourceFallbackFor("app.morphe.android.apps.maps"))
    }

    @Test
    fun theFallbackIsTheRootsFirstPackageWhateverTheCatalogOrder() {
        val json = """{"schemaVersion":1,"apps":[
            {"appId":"m","displayName":"m","packageNames":["p.m"],"hasOfficialRules":true,"captureAvailable":true,"defaultEnabled":true,"rulesFrom":"g","playListed":false},
            {"appId":"g","displayName":"g","packageNames":["p.g1","p.g2"],"hasOfficialRules":true,"captureAvailable":true,"defaultEnabled":true}
        ]}"""
        val catalog = NavigationAppCatalog.parse(json)
        assertEquals("p.g1", catalog.ruleFamilies().resourceFallbackFor("p.m"))
        assertEquals(listOf("p.g1", "p.g2", "p.m"), catalog.forGooglePlay().apps.single().packageNames)
        assertEquals("p.g1", catalog.forGooglePlay().ruleFamilies().resourceFallbackFor("p.m"))
        assertNull("the root's other package does not use another app's rules", catalog.ruleFamilies().resourceFallbackFor("p.g2"))
    }

    @Test
    fun aRootOutsideThePackagesFamilyIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RuleFamilies.of(listOf(setOf("p.a", "p.b"), setOf("p.c")), roots = mapOf("p.a" to "p.c"))
        }
    }
}
