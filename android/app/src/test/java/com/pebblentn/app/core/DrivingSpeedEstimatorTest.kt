package com.pebblentn.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Speed from Google Maps' remaining distance and arrival time (#132 FR5, NFR3). Synthetic data only. */
class DrivingSpeedEstimatorTest {

    private val zone: ZoneId = ZoneOffset.UTC

    /** 2026-11-08 11:50:00 UTC: an arrival at 12:12 is 22 minutes ahead. */
    private val start: Long = LocalDateTime.of(2026, 11, 8, 11, 50).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun estimator() = DrivingSpeedEstimator(zone = { zone })

    private fun sample(
        atMillis: Long,
        remainingMeters: Int,
        subText: String? = "Arrive 12:12",
        packageName: String = DrivingSpeedEstimator.GOOGLE_MAPS_PACKAGE,
        hasInstruction: Boolean = true,
        routeMeters: Int = 20_000,
    ) = SpeedSample(
        postTimeMillis = atMillis,
        progress = routeMeters - remainingMeters,
        progressMax = routeMeters,
        subText = subText,
        packageName = packageName,
        hasInstruction = hasInstruction,
    )

    /** Feed a sample every [stepSeconds] for [seconds] at [metresPerSecond]; the last estimate. */
    private fun drive(
        estimator: DrivingSpeedEstimator,
        metresPerSecond: Double,
        seconds: Int,
        stepSeconds: Int = 5,
        from: Long = start,
        startRemaining: Int = 15_000,
        packageName: String = DrivingSpeedEstimator.GOOGLE_MAPS_PACKAGE,
    ): SpeedEstimate? {
        var last: SpeedEstimate? = null
        for (t in 0..seconds step stepSeconds) {
            val now = from + t * 1000L
            last = estimator.add(sample(now, (startRemaining - metresPerSecond * t).toInt(), packageName = packageName), now)
        }
        return last
    }

    @Test
    fun walkingSamplesGiveAbout5Kmh() {
        val estimate = drive(estimator(), metresPerSecond = 1.4, seconds = 60)
        assertEquals(5.0, estimate!!.measuredKmh!!, 0.5)
    }

    @Test
    fun drivingSamplesGiveAbout50Kmh() {
        val estimate = drive(estimator(), metresPerSecond = 13.9, seconds = 60)
        assertEquals(50.0, estimate!!.measuredKmh!!, 1.0)
    }

    @Test
    fun noMeasuredSpeedBefore30Seconds() {
        val estimate = drive(estimator(), metresPerSecond = 13.9, seconds = 25)
        assertNull("too little history", estimate!!.measuredKmh)
    }

    @Test
    fun plannedSpeedFromA24HourArrivalClock() {
        // 15 km in 22 minutes: about 40.9 km/h.
        val estimate = estimator().add(sample(start, 15_000, subText = "Arrive 12:12"), start)
        assertEquals(15.0 / (22.0 / 60.0), estimate!!.plannedKmh!!, 0.1)
    }

    @Test
    fun plannedSpeedFromA12HourArrivalClock() {
        val estimate = estimator().add(sample(start, 15_000, subText = "Arrive 12:12 PM"), start)
        assertEquals(15.0 / (22.0 / 60.0), estimate!!.plannedKmh!!, 0.1)
        val evening = DrivingSpeedEstimator.arrivalMillis("Arrive 8:14 PM", start, zone)!!
        assertEquals(LocalDateTime.of(2026, 11, 8, 20, 14).toInstant(ZoneOffset.UTC).toEpochMilli(), evening)
    }

    @Test
    fun anArrivalClockBeforeNowIsTomorrow() {
        val arrival = DrivingSpeedEstimator.arrivalMillis("Arrive 00:10", start, zone)!!
        assertEquals(LocalDateTime.of(2026, 11, 9, 0, 10).toInstant(ZoneOffset.UTC).toEpochMilli(), arrival)
        assertNull("no clock in a rerouting line", DrivingSpeedEstimator.arrivalMillis("Arrive ", start, zone))
        assertNull(DrivingSpeedEstimator.arrivalMillis("Arrive 25:10", start, zone))
    }

    @Test
    fun reroutingCardsAreIgnored() {
        val estimator = estimator()
        assertNull(estimator.add(sample(start, 15_000, subText = "Arrive ", hasInstruction = false), start))
        assertNull("no point was taken", estimator.estimate(start + 40_000).measuredKmh)
    }

    @Test
    fun aNotificationWithoutProgressIsIgnored() {
        val noProgress = SpeedSample(start, null, null, "Arrive 12:12", DrivingSpeedEstimator.GOOGLE_MAPS_PACKAGE, true)
        assertNull(estimator().add(noProgress, start))
        val badProgress = noProgress.copy(progress = 30, progressMax = 20)
        assertNull(estimator().add(badProgress, start))
    }

    @Test
    fun aDistanceThatDoesNotCountDownIsNotASample() {
        val estimator = estimator()
        estimator.add(sample(start, 10_000), start)
        // The same distance posted again 20 s and 30 s later (an ETA refresh): no new point.
        estimator.add(sample(start + 20_000, 10_000), start + 20_000)
        val estimate = estimator.add(sample(start + 30_000, 10_000), start + 30_000)!!
        assertEquals("standing still", 0.0, estimate.measuredKmh!!, 1e-9)
    }

    @Test
    fun aLongerRouteRestartsTheWindow() {
        val estimator = estimator()
        drive(estimator, metresPerSecond = 13.9, seconds = 60)
        // Rerouted onto a longer route: the old points no longer compare.
        val now = start + 65_000
        val estimate = estimator.add(sample(now, 18_000), now)!!
        assertNull(estimate.measuredKmh)
    }

    @Test
    fun aReplanOntoAShorterRouteIsNotTravel() {
        // As Google Maps did at the start of the #132 emulator bike run: the route length changes
        // (4457 m → 3011 m) and the remaining distance drops by 1.4 km in a second. That is a new
        // route, not 188 km/h.
        val estimator = estimator()
        estimator.add(sample(start, 4_440, routeMeters = 4_457), start)
        estimator.add(sample(start + 1_200, 3_011, routeMeters = 3_011), start + 1_200)
        var last: SpeedEstimate? = null
        for (t in 5..40 step 5) {
            val now = start + 1_200 + t * 1000L
            last = estimator.add(sample(now, 3_011 - 5 * t, routeMeters = 3_011), now) // 18 km/h
        }
        assertEquals("only the new route counts", 18.0, last!!.measuredKmh!!, 1.0)
    }

    @Test
    fun noNewSamplesLowerTheSpeed() {
        val estimator = estimator()
        drive(estimator, metresPerSecond = 13.9, seconds = 60)
        // Stopped in traffic: Google Maps posts nothing new for two minutes.
        val later = estimator.estimate(start + 180_000)
        assertTrue("well below walking pace: ${later.measuredKmh}", later.measuredKmh!! < 5.0)
    }

    @Test
    fun anotherAppGivesNoEstimate() {
        val estimator = estimator()
        assertNull(drive(estimator, metresPerSecond = 13.9, seconds = 60, packageName = "net.osmand"))
        assertNull(estimator.estimate(start + 60_000).measuredKmh)
    }

    @Test
    fun microGMapsUsesTheGoogleMapsRules() {
        val estimate = drive(estimator(), metresPerSecond = 13.9, seconds = 60, packageName = "app.morphe.android.apps.maps")
        assertNotNull(estimate!!.measuredKmh)
    }

    @Test
    fun resetForgetsTheSession() {
        val estimator = estimator()
        drive(estimator, metresPerSecond = 13.9, seconds = 60)
        estimator.reset()
        val estimate = estimator.estimate(start + 61_000)
        assertTrue(estimate.isEmpty)
    }
}
