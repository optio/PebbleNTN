package com.pebblentn.fixturepublisher

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Debug-only publisher. A simple programmatic UI with one button per fixture; tapping a button
 * posts a navigation-like notification, and "Clear" cancels the ongoing one. Used to exercise the
 * PebbleNTN listener/allowlist end-to-end on a device without a real navigation app.
 */
class FixturePublisherActivity : Activity() {

    private val notificationId = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureChannel()
        requestPostNotificationsIfNeeded()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        for (fixture in NavigationFixtures.ALL) {
            layout.addView(button(fixture.label) { post(fixture) })
        }
        layout.addView(button("Clear") { cancel() })

        setContentView(
            ScrollView(this).apply {
                addView(
                    layout,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun post(fixture: NavigationFixture) {
        if (!hasPostPermission()) {
            requestPostNotificationsIfNeeded()
            return
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(fixture.title)
            .setContentText(fixture.text)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        fixture.subText?.let { builder.setSubText(it) }
        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }

    private fun cancel() {
        NotificationManagerCompat.from(this).cancel(notificationId)
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Navigation fixtures", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun hasPostPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private companion object {
        const val CHANNEL_ID = "navigation-fixtures"
    }
}
