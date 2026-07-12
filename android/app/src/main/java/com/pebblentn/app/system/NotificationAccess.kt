package com.pebblentn.app.system

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Reports whether the user has granted notification-listener access to this app. */
fun interface NotificationAccess {
    fun isGranted(): Boolean
}

/** Real check against the system's set of enabled notification listeners. */
class SystemNotificationAccess(private val context: Context) : NotificationAccess {
    override fun isGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
