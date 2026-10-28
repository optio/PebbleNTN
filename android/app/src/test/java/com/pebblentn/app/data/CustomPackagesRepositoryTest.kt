package com.pebblentn.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** FR5: user-added package names are kept per app and survive a restart. */
@RunWith(RobolectricTestRunner::class)
class CustomPackagesRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val prefs = context.getSharedPreferences("pebblentn_custom_packages", Context.MODE_PRIVATE)

    @Test
    fun emptyByDefault() {
        assertTrue(CustomPackagesRepository(context).packages.value.isEmpty())
    }

    @Test
    fun addKeepsTheOrderPerAppAndIgnoresARepeat() {
        val repo = CustomPackagesRepository(context)
        repo.add("microg-maps", "app.revanced.android.apps.maps")
        repo.add("microg-maps", "app.other.maps")
        repo.add("microg-maps", "app.revanced.android.apps.maps")
        repo.add("osmand", "net.osmand.patched")

        assertEquals(
            mapOf(
                "microg-maps" to listOf("app.revanced.android.apps.maps", "app.other.maps"),
                "osmand" to listOf("net.osmand.patched"),
            ),
            repo.packages.value,
        )
    }

    @Test
    fun removeDropsThePackageAndAnAppLeftWithout() {
        val repo = CustomPackagesRepository(context)
        repo.add("microg-maps", "app.a.maps")
        repo.add("microg-maps", "app.b.maps")
        repo.add("osmand", "net.osmand.patched")

        repo.remove("microg-maps", "app.a.maps")
        repo.remove("osmand", "net.osmand.patched")
        repo.remove("osmand", "never.added")
        repo.remove("comaps", "never.added")

        assertEquals(mapOf("microg-maps" to listOf("app.b.maps")), repo.packages.value)
    }

    @Test
    fun packagesSurviveARestart() {
        CustomPackagesRepository(context).apply {
            add("google-maps", "com.google.android.apps.maps.patched")
            add("comaps", "app.comaps.beta")
            remove("comaps", "app.comaps.beta")
        }

        // A fresh instance reads the persisted value, as after a process restart or an app update.
        assertEquals(
            mapOf("google-maps" to listOf("com.google.android.apps.maps.patched")),
            CustomPackagesRepository(context).packages.value,
        )
    }

    @Test
    fun anUnreadableStoredValueMeansNoPackagesInsteadOfACrash() {
        prefs.edit().putString("packages_by_app", "{not json").commit()
        assertTrue(CustomPackagesRepository(context).packages.value.isEmpty())

        prefs.edit().putString("packages_by_app", """{"osmand": []}""").commit()
        assertTrue(CustomPackagesRepository(context).packages.value.isEmpty())
    }
}
