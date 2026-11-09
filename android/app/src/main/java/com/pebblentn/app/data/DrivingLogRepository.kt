package com.pebblentn.app.data

/**
 * What one driving log entry records (#132 FR14): a speed estimate, or a decision of the hide/show
 * logic ([com.pebblentn.app.core.DrivingPolicy]).
 */
enum class DrivingDecision {
    /** A speed estimate from a navigation notification; nothing changed. */
    SAMPLE,

    /** The watchapp was closed for driving. */
    HIDE,

    /** The hidden state ended: the watchapp was reopened, or (auto-launch off) only the state ended. */
    SHOW,

    /** The user opened the watchapp while it was hidden: it stays open for this session (FR10). */
    KEPT_OPEN,

    /** Navigation stopped: the hidden and kept-open states ended (FR11). */
    RESET,
}

/**
 * One driving log entry. Numbers, a time and a decision name only, so the log can go into the
 * privacy-safe export as it is (FR14.1, FR14.2): there is deliberately no text field that could
 * hold notification text, a street or a place.
 *
 * @property timeMillis when it happened (epoch milliseconds).
 * @property measuredKmh the smoothed measured speed, or null when none was known.
 * @property plannedKmh the speed the arrival time implies, or null when none was known.
 * @property thresholdKmh the driving speed in use, in km/h (an imperial one converted).
 */
data class DrivingLogEntry(
    val timeMillis: Long,
    val measuredKmh: Double?,
    val plannedKmh: Double?,
    val thresholdKmh: Double,
    val decision: DrivingDecision,
)

/**
 * The driving log (#132 FR14, REQ-DEBUG-016): the last [capacity] entries, in memory only. It is
 * diagnostics for tuning the hide/show timing, so it does not need to survive a restart, and keeping
 * it out of storage keeps it out of backups.
 */
class DrivingLogRepository(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    private val entries = ArrayDeque<DrivingLogEntry>()

    /** Add [entry]; the oldest entry drops once the log holds [capacity]. */
    @Synchronized
    fun add(entry: DrivingLogEntry) {
        if (entries.size == capacity) entries.removeFirst()
        entries.addLast(entry)
    }

    /** Every entry, oldest first. */
    @Synchronized
    fun snapshot(): List<DrivingLogEntry> = entries.toList()

    companion object {
        const val DEFAULT_CAPACITY = 500
    }
}
