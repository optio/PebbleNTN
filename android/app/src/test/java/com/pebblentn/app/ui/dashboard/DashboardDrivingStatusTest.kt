package com.pebblentn.app.ui.dashboard

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.pebble.DrivingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The dashboard's "hidden while driving" line (#132 FR13), as text and in Compose. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DashboardDrivingStatusTest {

    val compose = createComposeRule()

    /** See RuleReviewScreenTest: register the host activity so release unit tests run too. */
    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                shadowOf(context.packageManager)
                    .addActivityIfNotPresent(ComponentName(context, ComponentActivity::class.java))
            }
        })
        .around(compose)

    private fun text(status: DrivingStatus, imperial: Boolean) = drivingStatusText(
        status = status,
        imperial = imperial,
        withSpeed = { speed, mph -> "about $speed ${if (mph) "mph" else "km/h"}" },
        noSpeed = "no speed",
    )

    @Test
    fun noLineWhileTheWatchappIsShown() {
        assertNull(text(DrivingStatus(hidden = false, measuredKmh = 48.0), imperial = false))
    }

    @Test
    fun theSpeedIsRoundedInTheUnitInUse() {
        assertEquals("about 48 km/h", text(DrivingStatus(hidden = true, measuredKmh = 47.6), imperial = false))
        assertEquals("about 30 mph", text(DrivingStatus(hidden = true, measuredKmh = 48.0), imperial = true))
        assertEquals("no speed", text(DrivingStatus(hidden = true, measuredKmh = null), imperial = false))
    }

    private fun showDashboard(status: DrivingStatus, imperial: Boolean) {
        compose.setContent {
            DashboardScreen(accessGranted = true, lastEligibleAtMillis = 1L, drivingStatus = status, imperialUnits = imperial)
        }
    }

    @Test
    fun theDashboardSaysTheWatchIsHiddenWithTheSpeedInKmh() {
        showDashboard(DrivingStatus(hidden = true, measuredKmh = 48.2), imperial = false)
        compose.onNodeWithTag(DRIVING_STATUS_TAG).assertTextEquals("Watch: hidden while driving (about 48 km/h)")
    }

    @Test
    fun theDashboardSaysTheWatchIsHiddenWithTheSpeedInMph() {
        showDashboard(DrivingStatus(hidden = true, measuredKmh = 48.2), imperial = true)
        compose.onNodeWithTag(DRIVING_STATUS_TAG).assertTextEquals("Watch: hidden while driving (about 30 mph)")
    }

    @Test
    fun theDashboardShowsNoLineWhileTheWatchappIsShown() {
        showDashboard(DrivingStatus(hidden = false, measuredKmh = 48.2), imperial = false)
        compose.onNodeWithTag(DRIVING_STATUS_TAG).assertDoesNotExist()
    }
}
