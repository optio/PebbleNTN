package com.pebblentn.app.notification

import android.app.Notification
import android.os.Bundle
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationSnapshotFactoryTest {

    /**
     * Build a Notification with explicit extras (Robolectric's Notification.Builder does not
     * populate extras). This exercises exactly what the factory reads.
     */
    private fun notification(
        category: String? = null,
        whenMillis: Long = 0L,
        extras: Bundle.() -> Unit = {},
    ): Notification = Notification().apply {
        this.category = category
        this.`when` = whenMillis
        this.extras = Bundle().apply(extras)
    }

    @Test
    fun extractsSelectedTextFields() {
        val n = notification(category = Notification.CATEGORY_NAVIGATION, whenMillis = 999L) {
            putCharSequence(Notification.EXTRA_TITLE, "Turn right")
            putCharSequence(Notification.EXTRA_TEXT, "500 m")
            putCharSequence(Notification.EXTRA_SUB_TEXT, "Main St")
        }
        val snap = NotificationSnapshotFactory.create("com.google.android.apps.maps", 7, 123L, n)

        assertEquals("com.google.android.apps.maps", snap.packageName)
        assertEquals(7, snap.notificationId)
        assertEquals(123L, snap.postTimeMillis)
        assertEquals(999L, snap.whenTimeMillis)
        assertEquals("Turn right", snap.title)
        assertEquals("500 m", snap.text)
        assertEquals("Main St", snap.subText)
        assertEquals(Notification.CATEGORY_NAVIGATION, snap.category)
    }

    @Test
    fun combinedTextJoinsTextFieldsForRuleMatching() {
        val n = notification {
            putCharSequence(Notification.EXTRA_TITLE, "Turn right")
            putCharSequence(Notification.EXTRA_TEXT, "500 m")
        }
        val snap = NotificationSnapshotFactory.create("com.google.android.apps.maps", 1, 0L, n)
        assertTrue(snap.combinedText.contains("Turn right"))
        assertTrue(snap.combinedText.contains("500 m"))
    }

    @Test
    fun onlyDocumentedExtrasAreCaptured() {
        // Extras carry many keys in practice (people, template data, custom app extras). The factory
        // must read only the documented ones and ignore the rest.
        val n = notification {
            putCharSequence(Notification.EXTRA_TITLE, "Navigate")
            putString("com.evil.SECRET_EXTRA", "SECRET_VALUE")
            putCharSequence("android.people.list", "contact@example.com")
        }
        val snap = NotificationSnapshotFactory.create("com.google.android.apps.maps", 1, 0L, n)
        val json = Json.encodeToString(NotificationSnapshot.serializer(), snap)

        assertEquals("Navigate", snap.title)
        assertFalse("undocumented extras must not be captured", json.contains("SECRET_VALUE"))
        assertFalse("people/contact extras must not be captured", json.contains("contact@example.com"))
    }

    @Test
    fun missingFieldsAreNull() {
        val n = notification { putCharSequence(Notification.EXTRA_TITLE, "Only title") }
        val snap = NotificationSnapshotFactory.create("com.waze", 1, 0L, n)
        assertEquals("Only title", snap.title)
        assertNull(snap.subText)
        assertNull(snap.bigText)
        assertNull(snap.whenTimeMillis)
    }
}
