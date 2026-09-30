package com.pebblentn.app.ui.debug

import com.pebblentn.app.data.DebugDisposition
import com.pebblentn.app.data.DebugEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Which events the debug history shows by status (#28); [dispositions] null means all. */
enum class StatusFilter(val dispositions: Set<String>?) {
    ALL(null),
    RECOGNISED(setOf(DebugDisposition.MATCHED)),
    NOT_RECOGNISED(setOf(DebugDisposition.CAPTURED_UNMATCHED)),
    NOT_A_DIRECTION(setOf(DebugDisposition.CAPTURED_NON_MANEUVER)),
}

/** One day of history, newest events first. */
data class DayGroup(val date: LocalDate, val events: List<DebugEvent>)

/**
 * The debug history as the screen shows it (#28): filtered by status and app, then grouped by the
 * local day each notification was received. Input order (newest first) is kept inside each day.
 */
data class DebugHistoryFilter(
    val status: StatusFilter = StatusFilter.ALL,
    /** Package name, or null for every app. */
    val packageName: String? = null,
) {
    fun apply(events: List<DebugEvent>, zone: ZoneId = ZoneId.systemDefault()): List<DayGroup> = events
        .filter { status.dispositions == null || it.disposition in status.dispositions }
        .filter { packageName == null || it.packageName == packageName }
        .groupBy { Instant.ofEpochMilli(it.receivedTimestampMillis).atZone(zone).toLocalDate() }
        .map { (date, dayEvents) -> DayGroup(date, dayEvents) }
        .sortedByDescending { it.date }

    companion object {
        /** The apps that appear in [events], for the app filter chips, in order of first appearance. */
        fun packagesIn(events: List<DebugEvent>): List<String> = events.map { it.packageName }.distinct()
    }
}
