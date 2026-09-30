package com.pebblentn.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Re-runs navigation-app discovery when a catalog app is installed or updated while PebbleNTN is
 * running (REQ-ANDROID-004). Discovery otherwise only runs when the notification listener connects,
 * at process start, so a navigation app installed later stayed unknown, and its notifications were
 * dropped by the allowlist, until PebbleNTN was restarted.
 *
 * Registered at runtime for the lifetime of the process: package broadcasts cannot be received by a
 * manifest receiver since Android 8. Package visibility (`<queries>` in the manifest) already limits
 * delivery to catalog apps; [catalogPackages] filters again so nothing else triggers discovery.
 */
class NavigationAppInstallReceiver(
    private val catalogPackages: Set<String>,
    private val onCatalogAppInstalled: () -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName in catalogPackages) onCatalogAppInstalled()
    }

    /** Start listening; package broadcasts come from the system, so the receiver stays unexported. */
    fun register(context: Context) {
        ContextCompat.registerReceiver(context, this, intentFilter(), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    companion object {
        private val ACTIONS = setOf(Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED)

        fun intentFilter(): IntentFilter = IntentFilter().apply {
            ACTIONS.forEach(::addAction)
            addDataScheme("package")
        }
    }
}
