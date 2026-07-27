package com.pebblentn.app.export

import android.os.Build
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.UserRuleRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * A size-capped export: the JSON, how many events it actually carries, and how many existed. Used to
 * tell the user when older events were trimmed to fit the email attachment budget.
 */
data class CappedExport(
    val json: String,
    val includedEvents: Int,
    val totalEvents: Int,
) {
    val truncated: Boolean get() = includedEvents < totalEvents
    val sizeBytes: Int get() = json.toByteArray(Charsets.UTF_8).size
}

/**
 * Gathers the data for a diagnostic/rule export and produces the JSON payload. Pure of any sharing
 * concerns — it only builds the string; the caller obtains consent and shares (REQ-DEBUG-006, no
 * automatic transmission).
 */
class DiagnosticExporter(
    private val debugHistory: DebugHistoryRepository,
    private val userRules: UserRuleRepository,
    private val appVersion: String,
    private val builder: ExportBuilder = ExportBuilder(),
    private val androidRelease: String = Build.VERSION.RELEASE ?: "unknown",
    private val now: () -> String = { Instant.now().toString() },
) {
    suspend fun build(mode: ExportMode): String {
        val rules = userRules.observeUserRules().first().mapNotNull { it.rule }
        val events = if (mode == ExportMode.RULES_ONLY) emptyList() else debugHistory.observeRecent().first()
        return builder.build(
            mode = mode,
            userRules = rules,
            events = events,
            appVersion = appVersion,
            androidRelease = androidRelease,
            exportedAt = now(),
        )
    }

    /**
     * Build an export capped to [maxBytes], keeping only the NEWEST events that fit (events arrive
     * newest-first). Email providers reject oversized attachments, so the share-to-help flow trims
     * the oldest events rather than fail. Returns the payload plus how many events survived the cap.
     */
    suspend fun buildCapped(mode: ExportMode, maxBytes: Int): CappedExport {
        val rules = userRules.observeUserRules().first().mapNotNull { it.rule }
        val allEvents = if (mode == ExportMode.RULES_ONLY) {
            emptyList()
        } else {
            debugHistory.observeRecent(DebugHistoryRepository.DEFAULT_RETENTION).first()
        }
        val exportedAt = now()

        fun render(events: List<DebugEvent>): String = builder.build(
            mode = mode,
            userRules = rules,
            events = events,
            appVersion = appVersion,
            androidRelease = androidRelease,
            exportedAt = exportedAt,
        )

        fun fits(json: String): Boolean = json.toByteArray(Charsets.UTF_8).size <= maxBytes

        val full = render(allEvents)
        if (allEvents.isEmpty() || fits(full)) {
            return CappedExport(full, allEvents.size, allEvents.size)
        }
        // Largest prefix of newest events whose rendered JSON still fits. Binary search on the count
        // so we render O(log n) times rather than once per dropped event.
        var lo = 0
        var hi = allEvents.size
        var bestJson = render(emptyList())
        var bestCount = 0
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val candidate = render(allEvents.take(mid))
            if (fits(candidate)) {
                bestJson = candidate
                bestCount = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return CappedExport(bestJson, bestCount, allEvents.size)
    }

    companion object {
        /** Attachment budget for the email share flow: keep only the newest 10 MB of logs. */
        const val EMAIL_MAX_BYTES: Int = 10 * 1024 * 1024
    }
}
