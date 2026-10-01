package com.pebblentn.app.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** REQ-RULE-017 (#26): the street line never starts with the distance the watch already shows. */
class StripLeadingDistanceTest {

    @Test
    fun dropsADistanceFollowedByASeparator() {
        assertEquals("Slight left onto N155", DistanceParser.stripLeadingDistance("70 m · Slight left onto N155"))
        assertEquals("Keep right", DistanceParser.stripLeadingDistance("1.2 km · Keep right"))
        assertEquals("Turn right and go", DistanceParser.stripLeadingDistance("200 m • Turn right and go"))
        assertEquals("Turn left", DistanceParser.stripLeadingDistance("500 ft | Turn left"))
    }

    @Test
    fun keepsEverythingElse() {
        assertEquals("a bare distance", "50 m", DistanceParser.stripLeadingDistance("50 m"))
        assertEquals("Turn right onto 5 m Street", DistanceParser.stripLeadingDistance("Turn right onto 5 m Street"))
        assertEquals("no unit", "12 · Example", DistanceParser.stripLeadingDistance("12 · Example"))
    }

    @Test
    fun anEmptyResultFallsThroughToTheNextField() {
        val snapshot = com.pebblentn.app.notification.NotificationSnapshot(
            packageName = "p",
            notificationId = 1,
            title = "0 m • ",
            text = "Arrive at your destination",
        )
        val result = ExtractorRunner().run(FirstNonEmptyExtractor(listOf("title", "text"), stripLeadingDistance = true), snapshot)
        assertEquals(ExtractionResult.Text("Arrive at your destination"), result)
    }
}
