package com.pebblentn.app.export

import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.Rule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The three export modes (spec/300-data/ExportFormat.md, REQ-DEBUG-005). */
enum class ExportMode {
    /** Selected user rules only; no notification content. Default. */
    RULES_ONLY,

    /** Rules + structurally redacted notification fields + match metadata. */
    PRIVACY_SAFE,

    /** Rules + raw selected notification fields. Requires explicit opt-in + warning + preview. */
    FULL,
}

@Serializable
data class ExportMetadata(
    val schemaVersion: Int = 1,
    val mode: String,
    val exportedAt: String,
    val appVersion: String,
    val androidRelease: String,
)

/**
 * What the watch was shown for an event: the normalized instruction the matched rule produced.
 * Absent when nothing matched — then nothing was sent to the watch.
 */
@Serializable
data class ExportedWatchOutput(
    val maneuver: String,
    val distanceMeters: Int? = null,
    val primaryText: String? = null,
    val secondaryText: String? = null,
)

@Serializable
data class ExportedEvent(
    val packageName: String,
    val eventType: String,
    val disposition: String,
    val matchedRuleId: String? = null,
    val watchOutput: ExportedWatchOutput? = null,
    val snapshot: NotificationSnapshot? = null,
)

@Serializable
data class ExportBundle(
    val metadata: ExportMetadata,
    val userRules: List<Rule> = emptyList(),
    val events: List<ExportedEvent> = emptyList(),
)

/**
 * Builds the JSON payload for a diagnostic/rule export. Pure and deterministic; the caller handles
 * user consent, preview and sharing (nothing is transmitted here). Privacy-safe redacts notification
 * text; full includes raw selected fields; rules-only includes no notification content at all.
 */
class ExportBuilder(
    private val redactor: Redactor = Redactor(),
    private val json: Json = Json { prettyPrint = true; prettyPrintIndent = "  "; encodeDefaults = true },
) {
    fun build(
        mode: ExportMode,
        userRules: List<Rule>,
        events: List<DebugEvent>,
        appVersion: String,
        androidRelease: String,
        exportedAt: String,
    ): String {
        val metadata = ExportMetadata(mode = mode.name, exportedAt = exportedAt, appVersion = appVersion, androidRelease = androidRelease)
        val bundle = when (mode) {
            ExportMode.RULES_ONLY -> ExportBundle(metadata, userRules = userRules)
            ExportMode.PRIVACY_SAFE -> ExportBundle(metadata, userRules, events.map { toExported(it, redact = true) })
            ExportMode.FULL -> ExportBundle(metadata, userRules, events.map { toExported(it, redact = false) })
        }
        return json.encodeToString(ExportBundle.serializer(), bundle)
    }

    private fun toExported(event: DebugEvent, redact: Boolean): ExportedEvent {
        val snapshot = event.snapshot?.let { if (redact) redactor.redact(it) else it }
        // The maneuver and distance are already normalized (no free text), so they survive
        // redaction; the road text came from the notification and must not.
        val watchOutput = event.instruction?.let {
            ExportedWatchOutput(
                maneuver = it.maneuver.name,
                distanceMeters = it.distanceMeters,
                primaryText = it.primaryText?.let { text -> if (redact) redactor.redactText(text) else text },
                secondaryText = it.secondaryText?.let { text -> if (redact) redactor.redactText(text) else text },
            )
        }
        return ExportedEvent(
            packageName = event.packageName,
            eventType = event.eventType.name,
            disposition = event.disposition,
            matchedRuleId = event.matchedRuleId,
            watchOutput = watchOutput,
            snapshot = snapshot,
        )
    }
}
