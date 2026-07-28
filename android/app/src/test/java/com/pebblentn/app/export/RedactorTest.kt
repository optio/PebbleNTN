package com.pebblentn.app.export

import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactorTest {

    private val redactor = Redactor(placeholder = "X")

    @Test
    fun keepsManeuverKeywordsUnitsAndDigits() {
        assertEquals("Turn right 500 m", redactor.redactText("Turn right 500 m"))
    }

    @Test
    fun replacesRoadAndDestinationWords() {
        val redacted = redactor.redactText("turn right onto Elm Street")!!
        assertTrue(redacted.startsWith("turn right onto"))
        assertFalse(redacted.contains("Elm"))
        assertFalse(redacted.contains("Street"))
        assertTrue(redacted.contains("X X")) // Elm Street -> X X
    }

    @Test
    fun keepsNonEnglishManeuverWordsButRedactsRoadNames() {
        // Italian: the maneuver ("Svolta ... destra") must survive so the log can drive it.json,
        // while the road name ("Via Roma") is still redacted.
        val it = redactor.redactText("Svolta a destra su Via Roma")!!
        assertTrue(it.contains("Svolta"))
        assertTrue(it.contains("destra"))
        assertFalse(it.contains("Roma"))

        // German: maneuver kept, destination redacted.
        val de = redactor.redactText("Rechts abbiegen auf Hauptstraße")!!
        assertTrue(de.contains("Rechts"))
        assertTrue(de.contains("abbiegen"))
        assertFalse(de.contains("Hauptstraße"))

        // Dutch roundabout maneuver kept; the exit-number digit survives too.
        val nl = redactor.redactText("Neem op de rotonde de 2e afslag")!!
        assertTrue(nl.contains("rotonde"))
        assertTrue(nl.contains("afslag"))
        assertTrue(nl.contains("2"))
    }

    @Test
    fun preservesPunctuationAndNumbers() {
        assertEquals("In 1.2 km, X", redactor.redactText("In 1.2 km, Anytown"))
    }

    @Test
    fun nullStaysNull() {
        assertNull(redactor.redactText(null))
    }

    @Test
    fun redactsAllTextFieldsOfSnapshot() {
        val snap = NotificationSnapshot(
            packageName = "com.google.android.apps.maps",
            notificationId = 1,
            title = "Turn right",
            subText = "Elm Street",
        )
        val out = redactor.redact(snap)
        assertEquals("Turn right", out.title)
        assertFalse(out.subText!!.contains("Elm"))
    }
}
