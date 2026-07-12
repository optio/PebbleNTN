package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorRunnerTest {

    private val runner = ExtractorRunner()

    private fun snapshot(title: String? = null, text: String? = null, subText: String? = null) =
        NotificationSnapshot(packageName = "com.google.android.apps.maps", notificationId = 1, title = title, text = text, subText = subText)

    private fun text(result: ExtractionResult) = (result as ExtractionResult.Text).value
    private fun num(result: ExtractionResult) = (result as ExtractionResult.Num).value

    @Test
    fun literal() {
        assertEquals("RIGHT", text(runner.run(LiteralExtractor("RIGHT"), snapshot())))
    }

    @Test
    fun fieldCopyAndFirstNonEmpty() {
        val s = snapshot(text = "Main St", title = "Turn right")
        assertEquals("Main St", text(runner.run(FieldCopyExtractor("text"), s)))
        assertEquals("Main St", text(runner.run(FirstNonEmptyExtractor(listOf("subText", "text", "title")), s)))
        assertTrue(runner.run(FieldCopyExtractor("subText"), s) is ExtractionResult.None)
    }

    @Test
    fun regexCapture() {
        val s = snapshot(text = "turn right onto Main Street")
        val result = runner.run(RegexCaptureExtractor("text", "onto (.+)$", group = 1), s)
        assertEquals("Main Street", text(result))
    }

    @Test
    fun distanceAndDuration() {
        val s = snapshot(text = "In 500 m, 3 min to destination")
        assertEquals(500L, num(runner.run(DistanceExtractor("text"), s)))
        assertEquals(180L, num(runner.run(DurationExtractor("text"), s)))
    }

    @Test
    fun maneuverMapWithDefault() {
        val s = snapshot(text = "TURN_RIGHT")
        val mapping = mapOf("TURN_RIGHT" to "RIGHT", "TURN_LEFT" to "LEFT")
        assertEquals("RIGHT", text(runner.run(ManeuverMapExtractor("text", mapping), s)))
        // Unmapped value falls back to default.
        val s2 = snapshot(text = "SOMETHING")
        assertEquals("UNKNOWN", text(runner.run(ManeuverMapExtractor("text", mapping, default = "UNKNOWN"), s2)))
    }

    @Test
    fun normalizeString() {
        val s = snapshot(text = "  Turn   Right  ")
        val result = runner.run(NormalizeStringExtractor("text", lowercase = true, collapseWhitespace = true, trim = true), s)
        assertEquals("turn right", text(result))
    }

    @Test
    fun boundedJoinTruncates() {
        val s = snapshot(title = "Turn right", text = "onto a very long street name that exceeds")
        val result = runner.run(BoundedJoinExtractor(listOf("title", "text"), separator = " · ", maxLength = 20), s)
        assertTrue(text(result).length <= 20)
        assertTrue(text(result).startsWith("Turn right"))
    }

    @Test
    fun missingFieldsProduceNone() {
        val s = snapshot()
        assertTrue(runner.run(DistanceExtractor("text"), s) is ExtractionResult.None)
        assertTrue(runner.run(FieldCopyExtractor("text"), s) is ExtractionResult.None)
        assertTrue(runner.run(BoundedJoinExtractor(listOf("title", "text")), s) is ExtractionResult.None)
    }
}
