package com.pebblentn.app.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.notification.NavigationNotificationListenerService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

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
        shadowOf(Looper.getMainLooper()).runToEndOfTasks()

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
        shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        val state = context.packageManager.getComponentEnabledSetting(component)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state)
    }

    /**
     * Regression for the on-device finding (Finding 2, MR !44999): an immediate disable-then-enable
     * is coalesced by the platform into a no-op, so the two calls must be separated in time. This
     * pins that the component is genuinely left DISABLED in the gap, proving the re-enable is a
     * distinct, later event rather than issued back-to-back with the disable.
     */
    @Test
    fun refreshDisablesBeforeTheDelayedReenable() {
        SystemNotificationListenerRefresher(context).refresh()

        val stateBeforeDelay = context.packageManager.getComponentEnabledSetting(component)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, stateBeforeDelay)

        shadowOf(Looper.getMainLooper()).runToEndOfTasks()
        val stateAfterDelay = context.packageManager.getComponentEnabledSetting(component)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, stateAfterDelay)
    }
}
