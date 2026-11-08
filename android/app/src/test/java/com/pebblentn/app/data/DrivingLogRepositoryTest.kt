package com.pebblentn.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The driving log keeps the last entries in order and holds no text (#132 FR14.1, REQ-DEBUG-016). */
class DrivingLogRepositoryTest {

    private fun entry(time: Long, decision: DrivingDecision = DrivingDecision.SAMPLE) =
        DrivingLogEntry(timeMillis = time, measuredKmh = 48.0, plannedKmh = 52.5, thresholdKmh = 25.0, decision = decision)

    @Test
    fun entriesAreKeptInOrder() {
        val log = DrivingLogRepository()
        log.add(entry(1))
        log.add(entry(2, DrivingDecision.HIDE))
        log.add(entry(3, DrivingDecision.SHOW))
        assertEquals(listOf(1L, 2L, 3L), log.snapshot().map { it.timeMillis })
        assertEquals(DrivingDecision.HIDE, log.snapshot()[1].decision)
    }

    @Test
    fun theOldestDropAfter500() {
        val log = DrivingLogRepository()
        for (time in 1L..501L) log.add(entry(time))
        val kept = log.snapshot()
        assertEquals(500, kept.size)
        assertEquals("the first one dropped", 2L, kept.first().timeMillis)
        assertEquals(501L, kept.last().timeMillis)
    }

    @Test
    fun anEmptyLogHasNoEntries() {
        assertTrue(DrivingLogRepository().snapshot().isEmpty())
    }

    @Test
    fun anEntryHasNoFieldThatCouldHoldText() {
        val fieldTypes = DrivingLogEntry::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.type }
        assertEquals(5, fieldTypes.size)
        assertTrue(
            "only numbers and the decision enum: $fieldTypes",
            fieldTypes.none { it == String::class.java || CharSequence::class.java.isAssignableFrom(it) },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun aZeroCapacityIsRejected() {
        DrivingLogRepository(capacity = 0)
    }
}
