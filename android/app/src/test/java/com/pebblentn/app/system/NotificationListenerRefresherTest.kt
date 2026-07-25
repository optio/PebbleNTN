package com.pebblentn.app.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.notification.NavigationNotificationListenerService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationListenerRefresherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val component = ComponentName(context, NavigationNotificationListenerService::class.java)

    /**
     * The refresh must leave the listener component ENABLED — the toggle is only a means to force a
     * rebind, so ending disabled would strand the listener and be worse than doing nothing.
     */
    @Test
    fun refreshLeavesListenerComponentEnabled() {
        SystemNotificationListenerRefresher(context).refresh()

        val state = context.packageManager.getComponentEnabledSetting(component)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state)
    }

    /** Even if a previous run left the component disabled, refresh must recover it to enabled. */
    @Test
    fun refreshRecoversADisabledComponent() {
        context.packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        SystemNotificationListenerRefresher(context).refresh()

        val state = context.packageManager.getComponentEnabledSetting(component)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state)
    }
}
