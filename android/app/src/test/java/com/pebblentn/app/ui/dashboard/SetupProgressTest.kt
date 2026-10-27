package com.pebblentn.app.ui.dashboard

import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.catalog.supportedNotInstalled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #28: the setup checklist and the "also supported" list. */
class SetupProgressTest {

    @Test
    fun completeOnlyWhenEveryStepIsDone() {
        assertTrue(SetupProgress(accessGranted = true, watchappInstalled = true, navigationSeen = true).complete)
        assertFalse(SetupProgress(accessGranted = true, watchappInstalled = false, navigationSeen = true).complete)
        assertFalse(SetupProgress(accessGranted = true, watchappInstalled = true, navigationSeen = false).complete)
    }

    @Test
    fun notInstalledListsDirectionAppsFirstAndNeverTheFixtureApp() {
        val catalog = NavigationAppCatalog.parse(
            javaClass.getResourceAsStream("/catalog/navigation-apps.json")!!.bufferedReader().use { it.readText() },
        )
        val list = supportedNotInstalled(catalog, installedAppIds = setOf("google-maps"))
        assertFalse("installed apps are not repeated", list.any { it.appId == "google-maps" })
        assertFalse("our own test app is never offered", list.any { it.appId == "pebblentn-fixtures" })
        val withDirections = list.takeWhile { it.hasOfficialRules }.map { it.appId }
        // MicroG Maps uses the Google Maps rules (#113), so it has directions too.
        assertEquals(listOf("comaps", "microg-maps", "organic-maps", "osmand"), withDirections)
        assertTrue("then capture-only apps", list.drop(withDirections.size).none { it.hasOfficialRules })
        // A Google Play install never offers MicroG Maps (#113).
        val play = supportedNotInstalled(catalog.forGooglePlay(), installedAppIds = setOf("google-maps"))
        assertEquals(listOf("comaps", "organic-maps", "osmand"), play.takeWhile { it.hasOfficialRules }.map { it.appId })
    }

    @Test
    fun anUninstalledAppIsListedInItsAlphabeticalPlace() {
        val catalog = NavigationAppCatalog.parse(
            javaClass.getResourceAsStream("/catalog/navigation-apps.json")!!.bufferedReader().use { it.readText() },
        )
        // #88: Amap was installed before; it is listed like any app that isn't installed.
        val captureOnly = supportedNotInstalled(catalog, installedAppIds = setOf("google-maps")).filterNot { it.hasOfficialRules }
        assertTrue(captureOnly.any { it.appId == "amap" })
        assertEquals(captureOnly.sortedBy { it.displayName.lowercase() }, captureOnly)
    }
}
