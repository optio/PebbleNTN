package com.pebblentn.app.ui.debug

import com.pebblentn.app.data.DebugDisposition
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugEventType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** #28: the debug history filters by status and app, and groups by local day. */
class DebugHistoryFilterTest {

    private val zone = ZoneId.of("Europe/Brussels")

    private fun at(day: Int, hour: Int) = ZonedDateTime.of(2026, 9, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun event(id: Long, received: Long, disposition: String, pkg: String = "maps") = DebugEvent(
        id = id,
        packageName = pkg,
        eventType = DebugEventType.POSTED,
        eventTimestampMillis = received,
        receivedTimestampMillis = received,
        snapshot = null,
        matchedRuleId = null,
        disposition = disposition,
    )

    // Newest first, as the repository returns them.
    private val events = listOf(
        event(5, at(30, 18), DebugDisposition.MATCHED),
        event(4, at(30, 9), DebugDisposition.CAPTURED_UNMATCHED, pkg = "osmand"),
        event(3, at(30, 0), DebugDisposition.CAPTURED_NON_MANEUVER),
        event(2, at(29, 23), DebugDisposition.MATCHED),
        event(1, at(28, 12), DebugDisposition.CAPTURED_UNMATCHED),
    )

    @Test
    fun groupsByLocalDayNewestFirstKeepingOrder() {
        val days = DebugHistoryFilter().apply(events, zone)
        assertEquals(listOf(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 28)), days.map { it.date })
        assertEquals("midnight belongs to the new day", listOf(5L, 4L, 3L), days[0].events.map { it.id })
        assertEquals("23:00 stays on the 29th", listOf(2L), days[1].events.map { it.id })
    }

    @Test
    fun statusFilterKeepsOneKindOfEvent() {
        fun ids(status: StatusFilter) = DebugHistoryFilter(status).apply(events, zone).flatMap { d -> d.events.map { it.id } }
        assertEquals(listOf(5L, 2L), ids(StatusFilter.RECOGNISED))
        assertEquals(listOf(4L, 1L), ids(StatusFilter.NOT_RECOGNISED))
        assertEquals(listOf(3L), ids(StatusFilter.NOT_A_DIRECTION))
        assertEquals(5, ids(StatusFilter.ALL).size)
    }

    @Test
    fun appFilterCombinesWithStatusAndDropsEmptyDays() {
        val days = DebugHistoryFilter(StatusFilter.NOT_RECOGNISED, packageName = "osmand").apply(events, zone)
        assertEquals(listOf(LocalDate.of(2026, 9, 30)), days.map { it.date })
        assertEquals(listOf(4L), days.single().events.map { it.id })
    }

    @Test
    fun packagesAreListedInOrderOfAppearance() {
        assertEquals(listOf("maps", "osmand"), DebugHistoryFilter.packagesIn(events))
    }
}
