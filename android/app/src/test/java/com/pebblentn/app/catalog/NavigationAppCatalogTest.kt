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
        assertTrue("Waze ships without official rules initially", waze!!.captureOnly)
    }

    @Test
    fun commonNavigationAppsAreDetectedAndCaptureOnly() {
        val catalog = NavigationAppCatalog.parse(bundledCatalogJson())
        // Package name -> expected catalog appId. Locks the identifiers so a typo or an accidental
        // removal fails loudly. All are capture-only until official parsing rules are authored.
        val expected = mapOf(
            "com.waze" to "waze",
            "net.osmand" to "osmand",
            "net.osmand.plus" to "osmand",
            "app.organicmaps" to "organic-maps",
            "app.comaps.google" to "comaps",
            "app.comaps.fdroid" to "comaps",
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
            assertTrue("$appId should be capture-only until rules exist", entry.captureOnly)
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
}
