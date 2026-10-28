package com.pebblentn.app.notification.icon

import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.catalog.RuleFamilies
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which resource package names a drawable is looked up under, in order (#113 follow-up). */
class IconResourcePackagesTest {

    private val bundled = NavigationAppCatalog.parse(
        javaClass.getResourceAsStream("/catalog/navigation-apps.json")!!.bufferedReader().use { it.readText() },
    )

    private val views = listOf(bundled.ruleFamilies(), bundled.forGooglePlay().ruleFamilies())

    @Test
    fun microgMapsTriesItsOwnPackageThenGoogleMaps() {
        for (families in views) {
            assertEquals(
                listOf("app.morphe.android.apps.maps", "com.google.android.apps.maps"),
                resourcePackagesToTry("app.morphe.android.apps.maps", families),
            )
        }
    }

    @Test
    fun googleMapsTriesOnlyItself() {
        for (families in views) {
            assertEquals(listOf("com.google.android.apps.maps"), resourcePackagesToTry("com.google.android.apps.maps", families))
        }
    }

    @Test
    fun aCoMapsVariantTriesOnlyItself() {
        for (families in views) {
            assertEquals(listOf("app.comaps.fdroid"), resourcePackagesToTry("app.comaps.fdroid", families))
        }
    }

    @Test
    fun anUnknownPackageTriesOnlyItself() {
        for (families in views + RuleFamilies.NONE) {
            assertEquals(listOf("com.example.unknown"), resourcePackagesToTry("com.example.unknown", families))
        }
    }

    @Test
    fun withoutFamiliesMicrogMapsTriesOnlyItself() {
        assertEquals(listOf("app.morphe.android.apps.maps"), resourcePackagesToTry("app.morphe.android.apps.maps", RuleFamilies.NONE))
    }
}
