package com.pebblentn.app.export

import android.os.Build
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.UserRuleRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

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
}
