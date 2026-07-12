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
