package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Field resolution must map Unicode space separators to an ASCII space before any condition or
 * extractor sees them. Navigation apps format the distance with a non-breaking space between number
 * and unit (`"92 m"`, `"350 m"` — real Google Maps / CoMaps / Organic Maps captures). Java's `\s`
 * on the JVM does not match U+00A0, so without this the distance parser and the title regexes fail
 * on the true input. See [SnapshotFields.normalizeSpaces].
 */
class SnapshotFieldsTest {

    private val nbsp = '\u00A0'
    private val narrowNbsp = '\u202F'
    private val thinSpace = '\u2009'

    @Test
    fun normalizeSpacesMapsUnicodeSeparatorsToAscii() {
        assertEquals("92 m", SnapshotFields.normalizeSpaces("92${nbsp}m"))
        assertEquals("1.2 km", SnapshotFields.normalizeSpaces("1.2${narrowNbsp}km"))
        assertEquals("0 yd", SnapshotFields.normalizeSpaces("0${thinSpace}yd"))
    }

    @Test
    fun normalizeSpacesLeavesAsciiUnchanged() {
        val plain = "350 m · Turn left onto Elm Street"
        assertEquals(plain, SnapshotFields.normalizeSpaces(plain))
    }

    @Test
    fun resolveNormalizesEveryTextField() {
        val snapshot = NotificationSnapshot(
            packageName = "app.comaps.google",
            notificationId = 1,
            title = "92${nbsp}m",
            text = "Kroonstraat",
        )
        assertEquals("92 m", SnapshotFields.resolve(snapshot, "title"))
        // combinedText joins the fields; the non-breaking space inside it is normalized too.
        assertEquals("92 m Kroonstraat", SnapshotFields.resolve(snapshot, "combinedText"))
    }

    @Test
    fun distanceParsesFromRealNonBreakingSpaceViaResolvedField() {
        val snapshot = NotificationSnapshot(
            packageName = "app.comaps.google",
            notificationId = 1,
            title = "92${nbsp}m",
        )
        val resolved = SnapshotFields.resolve(snapshot, "title")!!
        assertEquals(92, DistanceParser.parseMeters(resolved))
    }
}
