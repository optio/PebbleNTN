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
        assertEquals(listOf("comaps", "organic-maps", "osmand"), withDirections)
        assertTrue("then capture-only apps", list.drop(withDirections.size).none { it.hasOfficialRules })
    }
}
