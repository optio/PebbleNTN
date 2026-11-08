package com.pebblentn.app.ui.dashboard

import com.pebblentn.app.core.DrivingSpeeds
import com.pebblentn.app.pebble.DrivingStatus
import kotlin.math.roundToInt

/** The test tag of the Status section's When driving line (#132 FR13). */
const val DRIVING_STATUS_TAG = "dashboard-driving-status"

/**
 * The Status section's When driving line (#132 FR13), or null while the watchapp is not hidden:
 * "Watch: hidden while driving (about 48 km/h)" through [withSpeed], the speed rounded in the unit
 * in use ([imperial]: mph), or [noSpeed] before any speed was measured. Pure, so the text is
 * unit-tested apart from the Compose row.
 */
fun drivingStatusText(
    status: DrivingStatus,
    imperial: Boolean,
    withSpeed: (speed: Int, imperial: Boolean) -> String,
    noSpeed: String,
): String? {
    if (!status.hidden) return null
    val kmh = status.measuredKmh ?: return noSpeed
    val speed = if (imperial) kmh / DrivingSpeeds.KMH_PER_MPH else kmh
    return withSpeed(speed.roundToInt(), imperial)
}
