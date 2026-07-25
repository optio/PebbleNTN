package com.pebblentn.app.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import com.pebblentn.app.notification.NavigationNotificationListenerService

/**
 * Re-establishes the system binding to our [NavigationNotificationListenerService] (REQ-ANDROID-012).
 *
 * Android occasionally leaves a NotificationListenerService nominally connected but stops delivering
 * `onNotificationPosted`, so navigation that is running on the phone never reaches the watch. The
 * user's usual remedy is to force-stop and relaunch the app, which makes the platform tear down and
 * recreate the listener binding. This does the same thing on demand, without the jarring full restart.
 */
fun interface NotificationListenerRefresher {
    fun refresh()
}

/**
 * Real implementation. Two mechanisms, weakest-to-strongest, so a merely-idle binding and a truly
 * wedged one are both covered:
 *
 * 1. `requestRebind` (API 24+) — the platform's supported "please reconnect" call.
 * 2. Toggling the listener component off then on with `DONT_KILL_APP` — the programmatic equivalent
 *    of the force-stop the user would otherwise do, which forces a fresh bind even when the binding
 *    is wedged. The notification-access grant lives in a separate secure setting keyed by component
 *    name, so it survives the toggle; `DONT_KILL_APP` keeps our process and the visible UI alive.
 *
 * On success the platform calls `onListenerConnected`, which re-delivers the currently posted
 * notifications, so an in-progress navigation is picked up again.
 */
class SystemNotificationListenerRefresher(context: Context) : NotificationListenerRefresher {

    private val appContext = context.applicationContext

    override fun refresh() {
        val component = ComponentName(appContext, NavigationNotificationListenerService::class.java)

        val pm = appContext.packageManager
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        NotificationListenerService.requestRebind(component)
    }
}
