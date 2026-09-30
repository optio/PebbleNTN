package com.pebblentn.app.system

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** REQ-ANDROID-004: a catalog app installed while PebbleNTN runs triggers discovery. */
@RunWith(RobolectricTestRunner::class)
class NavigationAppInstallReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private var discoveries = 0
    private val receiver = NavigationAppInstallReceiver(setOf("net.osmand.plus")) { discoveries++ }

    private fun packageIntent(action: String, pkg: String) =
        Intent(action, Uri.fromParts("package", pkg, null))

    @Test
    fun installingACatalogAppTriggersDiscovery() {
        receiver.onReceive(context, packageIntent(Intent.ACTION_PACKAGE_ADDED, "net.osmand.plus"))
        assertEquals(1, discoveries)
    }

    @Test
    fun updatingACatalogAppTriggersDiscovery() {
        receiver.onReceive(context, packageIntent(Intent.ACTION_PACKAGE_REPLACED, "net.osmand.plus"))
        assertEquals(1, discoveries)
    }

    @Test
    fun otherAppsAndOtherActionsAreIgnored() {
        receiver.onReceive(context, packageIntent(Intent.ACTION_PACKAGE_ADDED, "com.example.game"))
        receiver.onReceive(context, packageIntent(Intent.ACTION_PACKAGE_REMOVED, "net.osmand.plus"))
        assertEquals(0, discoveries)
    }

    @Test
    fun filterListensForInstallsAndUpdatesOfPackages() {
        val filter = NavigationAppInstallReceiver.intentFilter()
        assertTrue(filter.hasAction(Intent.ACTION_PACKAGE_ADDED))
        assertTrue(filter.hasAction(Intent.ACTION_PACKAGE_REPLACED))
        assertTrue(filter.hasDataScheme("package"))
    }

    @Test
    fun aRegisteredReceiverGetsTheSystemBroadcast() {
        receiver.register(context)
        context.sendBroadcast(packageIntent(Intent.ACTION_PACKAGE_ADDED, "net.osmand.plus"))
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(1, discoveries)
    }
}
