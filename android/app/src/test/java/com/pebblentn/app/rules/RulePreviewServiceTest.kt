package com.pebblentn.app.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulePreviewServiceTest {

    private val service = RulePreviewService()

    private fun snapshot(text: String) =
        NotificationSnapshot(packageName = "com.google.android.apps.maps", notificationId = 1, text = text)

    private val turnRightJson = """
        {"id":"turn-right","enabled":true,"priority":100,"packageNames":["com.google.android.apps.maps"],
         "conditions":[{"field":"combinedText","operator":"regex","value":"(?i)turn right"}],
         "output":{"maneuver":{"type":"literal","value":"RIGHT"},"distanceMeters":{"type":"distance","field":"combinedText"}}}
    """.trimIndent()

    @Test
    fun previewCandidateValidMatch() {
        val result = service.previewCandidate(snapshot("In 500 m, turn right"), turnRightJson)
        assertTrue(result is PreviewResult.Evaluated)
        val eval = (result as PreviewResult.Evaluated).evaluation
        assertEquals(Maneuver.RIGHT, eval.instruction!!.maneuver)
        assertEquals(500, eval.instruction.distanceMeters)
    }

    @Test
    fun previewCandidateValidNoMatchShowsTrace() {
        val result = service.previewCandidate(snapshot("turn left"), turnRightJson)
        val eval = (result as PreviewResult.Evaluated).evaluation
        assertFalse(eval.matched)
        assertEquals(RuleOutcome.CONDITIONS_FAILED, eval.trace.single().outcome)
    }

    @Test
    fun previewCandidateInvalidJson() {
        val result = service.previewCandidate(snapshot("turn right"), "{ not valid")
        assertTrue(result is PreviewResult.InvalidRule)
        assertTrue((result as PreviewResult.InvalidRule).errors.isNotEmpty())
    }

    @Test
    fun previewWithRulesUsesLayeredRules() {
        val rule = RulesetCodec.parseRule(turnRightJson)
        val eval = service.previewWithRules(snapshot("turn right now"), LayeredRules(bundled = listOf(rule)))
        assertTrue(eval.matched)
        assertEquals(RuleLayer.BUNDLED, eval.matchedLayer)
    }
}
