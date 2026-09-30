package com.pebblentn.app.ui.format

import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.R
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.data.DebugDisposition
import com.pebblentn.app.data.DebugEventType
import com.pebblentn.app.rules.RuleLayer
import com.pebblentn.app.rules.RuleOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #28: users read labels, never enum names or package names. */
@RunWith(RobolectricTestRunner::class)
class DisplayLabelsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val catalog: NavigationAppCatalog = NavigationAppCatalog.parse(
        javaClass.getResourceAsStream("/catalog/navigation-apps.json")!!.bufferedReader().use { it.readText() },
    )

    @Test
    fun everyManeuverHasItsOwnReadableLabel() {
        val labels = Maneuver.entries.map { context.getString(DisplayLabels.maneuver(it)) }
        assertEquals("labels are distinct", labels.size, labels.toSet().size)
        for ((maneuver, label) in Maneuver.entries.zip(labels)) {
            assertFalse("${maneuver.name} is not shown raw", label.contains('_') || label == maneuver.name)
        }
        assertEquals("Slight left", context.getString(DisplayLabels.maneuver(Maneuver.SLIGHT_LEFT)))
    }

    @Test
    fun dispositionsAreReadableAndUnknownOnesFallBack() {
        assertEquals("Recognised", context.getString(DisplayLabels.disposition(DebugDisposition.MATCHED)))
        assertEquals("Not recognised", context.getString(DisplayLabels.disposition(DebugDisposition.CAPTURED_UNMATCHED)))
        assertEquals("Not a direction", context.getString(DisplayLabels.disposition(DebugDisposition.CAPTURED_NON_MANEUVER)))
        assertEquals(R.string.disposition_other, DisplayLabels.disposition("SOMETHING_NEW"))
    }

    @Test
    fun eventTypesAreReadable() {
        val labels = DebugEventType.entries.map { context.getString(DisplayLabels.eventType(it)) }
        assertEquals(listOf("Notification", "Navigation ended"), labels)
    }

    @Test
    fun packageNamesBecomeAppNamesWithAFallback() {
        assertEquals("Google Maps", DisplayLabels.appName(catalog, "com.google.android.apps.maps"))
        assertNotEquals("net.osmand.plus", DisplayLabels.appName(catalog, "net.osmand.plus"))
        assertEquals("unknown packages stay visible", "com.example.other", DisplayLabels.appName(catalog, "com.example.other"))
    }

    @Test
    fun ruleOutcomesAndLayersAreReadable() {
        val outcomes = RuleOutcome.entries.map { context.getString(DisplayLabels.ruleOutcome(it)) }
        assertEquals("outcome labels are distinct", outcomes.size, outcomes.toSet().size)
        assertEquals("Didn't match", context.getString(DisplayLabels.ruleOutcome(RuleOutcome.CONDITIONS_FAILED)))
        val layers = RuleLayer.entries.map { context.getString(DisplayLabels.ruleLayer(it)) }
        assertEquals(listOf("Your rule", "Downloaded rule", "Official rule"), layers)
    }
}
