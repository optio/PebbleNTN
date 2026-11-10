package com.pebblentn.app.core

import com.pebblentn.app.rules.ArrivalClock
import java.time.Instant
import java.time.ZoneId

/**
 * The speed fields of one eligible navigation notification (#132 FR5): raw values, before the
 * reducer quantizes anything. Nothing here is notification text except [subText], which is read
 * only for its arrival clock and never kept.
 *
 * @property postTimeMillis when the navigation app posted or updated the notification.
 * @property progress / [progressMax] the notification's documented progress values; for Google
 *   Maps `progressMax − progress` is the remaining route distance in metres (an assumption that
 *   matched every emulator run and the #132 reporter's diagnostics).
 * @property subText the line with the arrival time ("Arrive 12:12 PM", "Arrive 20:25").
 * @property packageName the posting app; only Google Maps and MicroG Maps give samples (FR5.4).
 * @property hasInstruction whether the rules found a navigation instruction in it. Status cards
 *   such as "Rerouting…" have none, and their distance is not used (FR5.3).
 */
data class SpeedSample(
    val postTimeMillis: Long,
    val progress: Int?,
    val progressMax: Int?,
    val subText: String?,
    val packageName: String,
    val hasInstruction: Boolean,
)

/**
 * A speed estimate at [atMillis]: the measured speed over the last 30–60 s and the speed the
 * arrival time implies, each null when it cannot be told.
 */
data class SpeedEstimate(val atMillis: Long, val measuredKmh: Double?, val plannedKmh: Double?) {
    val isEmpty: Boolean get() = measuredKmh == null && plannedKmh == null
}

/**
 * Estimates the travel speed from Google Maps' navigation notifications (#132 FR5, NFR1, NFR3).
 * Navigation apps do not say whether the user drives, walks or cycles, so the speed is the only
 * clue. Pure logic on samples and a clock, no Android; not thread-safe (the controller serializes).
 *
 * **Measured speed (FR5.1):** the remaining route distance only counts down, so the estimator keeps
 * the samples where it did and divides the distance covered by the time passed up to *now*. Time
 * without a new sample therefore lowers the speed: Google Maps updates its card every few seconds
 * while the distance changes, so a card that stops changing means the user is (nearly) standing still.
 * The window holds the last [WINDOW_MILLIS] plus the newest older sample as its anchor, and an
 * estimate needs at least [MIN_SPAN_MILLIS] of history.
 *
 * **Planned speed (FR5.2):** remaining distance ÷ time until the arrival clock in [SpeedSample.subText].
 *
 * **Ignored (FR5.3):** samples without both progress values, cards without an instruction
 * ("Rerouting…"), and samples whose distance did not count down. A new route length (`progressMax`)
 * or a distance that grows means a new route: the window restarts there. Apps [isSpeedPackage] rejects never produce samples (FR5.4).
 *
 * @param isSpeedPackage whether a package's notifications carry the remaining distance in their
 *   progress values: Google Maps and the packages that use its rules (MicroG Maps).
 * @param zone the time zone the arrival clock is in (the phone's own).
 */
class DrivingSpeedEstimator(
    private val isSpeedPackage: (String) -> Boolean = { it in GOOGLE_MAPS_PACKAGES },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private data class Point(val timeMillis: Long, val remainingMeters: Int, val routeMeters: Int)

    private val points = ArrayDeque<Point>()
    private var packageName: String? = null
    private var lastPlannedKmh: Double? = null

    /** Whether [packageName] is one this estimator reads (Google Maps, MicroG Maps). */
    fun supports(packageName: String): Boolean = isSpeedPackage(packageName)

    /**
     * Take [sample] into account and return the estimate at [nowMillis], or null when the sample is
     * not a speed sample at all (another app, no progress, a status card).
     */
    fun add(sample: SpeedSample, nowMillis: Long): SpeedEstimate? {
        if (!supports(sample.packageName) || !sample.hasInstruction) return null
        val remaining = remainingMeters(sample) ?: return null
        if (sample.packageName != packageName) reset()
        packageName = sample.packageName
        val route = sample.progressMax!! // remainingMeters checked it
        val point = Point(sample.postTimeMillis, remaining, route)
        val last = points.lastOrNull()
        when {
            last == null -> points.addLast(point)
            // A new route: a different length (Google Maps re-plans a few times right after the
            // start, often onto a shorter route, and on every reroute) or a longer remaining
            // distance. Comparing distances across routes would read a re-plan as travel (a false
            // 188 km/h on a bike in the #132 emulator replay), so start over from here.
            route != last.routeMeters || remaining > last.remainingMeters -> {
                points.clear()
                points.addLast(point)
            }
            remaining < last.remainingMeters && sample.postTimeMillis > last.timeMillis ->
                points.addLast(point)
            else -> Unit // did not count down: not a sample (FR5.3)
        }
        lastPlannedKmh = plannedKmh(remaining, sample.subText, sample.postTimeMillis)
        return estimate(nowMillis)
    }

    /** The estimate at [nowMillis] from the samples so far (also used by the timer while hidden). */
    fun estimate(nowMillis: Long): SpeedEstimate {
        prune(nowMillis)
        return SpeedEstimate(nowMillis, measuredKmh(nowMillis), lastPlannedKmh)
    }

    /** Forget everything: a navigation session ended (FR11). */
    fun reset() {
        points.clear()
        packageName = null
        lastPlannedKmh = null
    }

    private fun remainingMeters(sample: SpeedSample): Int? {
        val progress = sample.progress ?: return null
        val max = sample.progressMax ?: return null
        if (max <= 0 || progress < 0 || progress > max) return null
        return max - progress
    }

    /** Drop the points older than the window, keeping the newest of them as the window's anchor. */
    private fun prune(nowMillis: Long) {
        val windowStart = nowMillis - WINDOW_MILLIS
        while (points.size >= 2 && points[1].timeMillis <= windowStart) points.removeFirst()
    }

    private fun measuredKmh(nowMillis: Long): Double? {
        val first = points.firstOrNull() ?: return null
        val last = points.last()
        val spanMillis = nowMillis - first.timeMillis
        if (spanMillis < MIN_SPAN_MILLIS) return null
        val kmh = (first.remainingMeters - last.remainingMeters) / (spanMillis / 1000.0) * MS_TO_KMH
        return kmh.takeIf { it <= MAX_PLAUSIBLE_KMH }
    }

    private fun plannedKmh(remainingMeters: Int, subText: String?, postTimeMillis: Long): Double? {
        val arrival = arrivalMillis(subText, postTimeMillis, zone()) ?: return null
        val seconds = (arrival - postTimeMillis) / 1000.0
        if (seconds < MIN_PLANNED_SECONDS) return null
        return (remainingMeters / seconds * MS_TO_KMH).takeIf { it <= MAX_PLAUSIBLE_KMH }
    }

    companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

        /** Google Maps and the MicroG Maps builds, which follow the Google Maps rules (catalog). */
        val GOOGLE_MAPS_PACKAGES: Set<String> = setOf(
            GOOGLE_MAPS_PACKAGE,
            "app.morphe.android.apps.maps",
            "org.ungoogled.android.apps.maps.microg",
        )

        /** The measured speed is smoothed over this much history (plus the anchor before it). */
        const val WINDOW_MILLIS = 60_000L

        /** No measured speed before this much history: a few seconds say too little. */
        const val MIN_SPAN_MILLIS = 30_000L

        /** A planned speed needs the arrival at least this far ahead (the clock has minute steps). */
        const val MIN_PLANNED_SECONDS = 60.0

        /** Faster than this is a jump in the progress values, not a speed. */
        const val MAX_PLAUSIBLE_KMH = 200.0

        private const val MS_TO_KMH = 3.6

        /**
         * The epoch time of the arrival clock in [subText] ("Arrive 12:12 PM", "Arrive 20:25"): the
         * last h:mm in it, with AM/PM when present, on the day of [postTimeMillis] in [zone], or the
         * next day when that clock time has already passed. Null when there is no valid clock.
         * The clock is read by [ArrivalClock], the parser the rule engine uses (#134).
         */
        fun arrivalMillis(subText: String?, postTimeMillis: Long, zone: ZoneId): Long? {
            val time = ArrivalClock.parseLast(subText) ?: return null
            return ArrivalClock.next(time, Instant.ofEpochMilli(postTimeMillis), zone).toInstant().toEpochMilli()
        }
    }
}
