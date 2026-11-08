package com.pebblentn.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the watchapp hides and comes back, with virtual time (#132 FR6–FR12, NFR2, NFR3). */
class DrivingPolicyTest {

    private val hide25 = DrivingConfig(hideEnabled = true, thresholdKmh = 25.0, marginKmh = 5.0, autoLaunch = true)

    /** Runs the policy over a timeline of (seconds, measured km/h, planned km/h), one call per second. */
    private class Run(var state: DrivingState = DrivingState()) {
        val actions = mutableListOf<Pair<Long, DrivingAction>>()

        fun at(seconds: Long, measured: Double?, planned: Double? = null, config: DrivingConfig): DrivingAction {
            val millis = seconds * 1000
            val result = DrivingPolicy.evaluate(state, millis, SpeedEstimate(millis, measured, planned), config)
            state = result.state
            if (result.action != DrivingAction.NONE) actions += seconds to result.action
            return result.action
        }

        fun steady(from: Long, to: Long, measured: Double?, planned: Double? = null, config: DrivingConfig) {
            for (s in from..to) at(s, measured, planned, config)
        }
    }

    @Test
    fun showDirectionsStaysVisibleAtAnySpeed() {
        val run = Run()
        run.steady(0, 600, measured = 120.0, planned = 120.0, config = hide25.copy(hideEnabled = false))
        assertTrue(run.actions.isEmpty())
        assertFalse(run.state.hidden)
    }

    @Test
    fun hidesAfter30SecondsAbove25Kmh() {
        val run = Run()
        run.steady(0, 29, measured = 30.0, config = hide25)
        assertTrue("not yet", run.actions.isEmpty())
        assertEquals(DrivingAction.HIDE, run.at(30, 30.0, config = hide25))
        assertTrue(run.state.hidden)
    }

    @Test
    fun aShortBurstDoesNotHide() {
        val run = Run()
        run.steady(0, 19, measured = 40.0, config = hide25)
        run.steady(20, 300, measured = 5.0, config = hide25)
        assertTrue(run.actions.isEmpty())
    }

    @Test
    fun aPlannedSpeedAbove40KmhHidesEarly() {
        val run = Run()
        run.steady(0, 9, measured = 30.0, planned = 60.0, config = hide25)
        assertEquals(DrivingAction.HIDE, run.at(10, 30.0, planned = 60.0, config = hide25))
    }

    @Test
    fun showsAgainAfter2MinutesBelow20Kmh() {
        val run = Run()
        run.steady(0, 30, measured = 50.0, config = hide25)
        assertTrue(run.state.hidden)
        run.steady(31, 150, measured = 22.0, config = hide25) // above 25 − 5: stays hidden
        run.steady(151, 270, measured = 10.0, config = hide25)
        assertEquals(DrivingAction.SHOW, run.at(271, 10.0, config = hide25))
        assertFalse(run.state.hidden)
        assertEquals(listOf(30L to DrivingAction.HIDE, 271L to DrivingAction.SHOW), run.actions)
    }

    @Test
    fun trafficLightsHideAtMostOnceAndNeverShowWithinTwoMinutes() {
        val run = Run()
        // Crossing the threshold every 20 s: 40 km/h, then standing, with a planned 45 km/h.
        for (cycle in 0 until 6) {
            val base = cycle * 40L
            run.steady(base, base + 19, measured = 40.0, planned = 45.0, config = hide25)
            run.steady(base + 20, base + 39, measured = 0.0, planned = 45.0, config = hide25)
        }
        assertTrue("at most one hide: ${run.actions}", run.actions.count { it.second == DrivingAction.HIDE } <= 1)
        assertTrue("no show: ${run.actions}", run.actions.none { it.second == DrivingAction.SHOW })
    }

    @Test
    fun noMoreThanOneHideAndOneShowIn2Minutes() {
        val run = Run()
        run.steady(0, 30, measured = 50.0, config = hide25)
        // Mode switched to Show and back: the forced show is allowed, a second hide waits.
        run.at(31, 50.0, config = hide25.copy(hideEnabled = false))
        run.steady(32, 140, measured = 50.0, config = hide25)
        assertEquals(listOf(30L to DrivingAction.HIDE, 31L to DrivingAction.SHOW), run.actions)
        run.steady(141, 160, measured = 50.0, config = hide25)
        assertEquals("the next hide waits for 2 minutes after the first", 150L to DrivingAction.HIDE, run.actions.last())
    }

    @Test
    fun autoLaunchOffLeavesTheHiddenStateWithoutShowing() {
        val off = hide25.copy(autoLaunch = false)
        val run = Run()
        run.steady(0, 30, measured = 50.0, config = off)
        run.steady(31, 151, measured = 3.0, config = off)
        assertEquals(listOf(30L to DrivingAction.HIDE, 151L to DrivingAction.LEAVE_HIDDEN), run.actions)
        assertFalse(run.state.hidden)
    }

    @Test
    fun keptOpenBlocksHidingUntilTheSessionStops() {
        val run = Run()
        run.steady(0, 30, measured = 50.0, config = hide25)
        run.state = DrivingPolicy.onUserOpened(run.state)
        assertFalse(run.state.hidden)
        run.steady(31, 400, measured = 80.0, planned = 80.0, config = hide25)
        assertEquals(listOf(30L to DrivingAction.HIDE), run.actions)

        run.state = DrivingPolicy.onSessionStopped()
        run.steady(401, 431, measured = 80.0, config = hide25)
        assertEquals("a new session hides again", DrivingAction.HIDE, run.actions.last().second)
    }

    @Test
    fun aSessionStopResetsEverything() {
        val run = Run()
        run.steady(0, 30, measured = 50.0, config = hide25)
        assertTrue(run.state.hidden)
        assertEquals(DrivingState(), DrivingPolicy.onSessionStopped())
    }

    @Test
    fun switchingToShowDirectionsWhileHiddenShowsAtOnce() {
        val run = Run()
        run.steady(0, 30, measured = 50.0, config = hide25)
        assertEquals(DrivingAction.SHOW, run.at(31, 50.0, config = hide25.copy(hideEnabled = false)))
        assertFalse(run.state.hidden)
    }

    @Test
    fun theImperialThresholdOf15MphIsUsed() {
        val imperial = WatchSettingsModel.defaults()
            .with(WatchSetting.UNITS, DrivingSpeeds.UNITS_IMPERIAL)
            .with(WatchSetting.DRIVING_MODE, DrivingSpeeds.MODE_HIDE)
        val config = DrivingConfig.from(imperial, hasDrivingSettings = true, autoLaunch = true)
        assertEquals(24.14, config.thresholdKmh, 0.01)
        assertEquals("3 mph", 4.83, config.marginKmh, 0.01)
        val run = Run()
        run.steady(0, 40, measured = 24.0, config = config) // 14.9 mph: below 15 mph
        assertTrue(run.actions.isEmpty())
        run.steady(41, 71, measured = 26.0, config = config)
        assertEquals(DrivingAction.HIDE, run.actions.single().second)
    }

    @Test
    fun aWatchappWithoutTheSettingNeverHides() {
        val hide = WatchSettingsModel.defaults().with(WatchSetting.DRIVING_MODE, DrivingSpeeds.MODE_HIDE)
        assertFalse(DrivingConfig.from(hide, hasDrivingSettings = false, autoLaunch = true).hideEnabled)
        assertFalse(DrivingConfig.from(null, hasDrivingSettings = true, autoLaunch = true).hideEnabled)
        assertTrue(DrivingConfig.from(hide, hasDrivingSettings = true, autoLaunch = true).hideEnabled)
    }

    @Test
    fun anUnknownSpeedNeitherHidesNorShows() {
        val run = Run()
        run.steady(0, 300, measured = null, config = hide25)
        assertTrue(run.actions.isEmpty())
        run.steady(301, 331, measured = 50.0, config = hide25)
        run.steady(332, 600, measured = null, config = hide25)
        assertEquals(listOf(331L to DrivingAction.HIDE), run.actions)
    }
}
