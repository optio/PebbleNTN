package com.pebblentn.app.export

import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.Rule
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
    /**
     * 2 adds [appVersionCode] and the per-event versions (#84); 3 the grouped occurrences (#86); 4
     * [userAddedPackages] and the per-event [ExportedEvent.customPackage].
     */
    val schemaVersion: Int = SCHEMA_VERSION,
    val mode: String,
    val exportedAt: String,
    /** PebbleNTN's version at export time; each event also carries the version that recorded it. */
    val appVersion: String,
    val appVersionCode: Long? = null,
    val androidRelease: String,
    /**
     * The device UI language tag (e.g. "it-IT"). Google Maps localizes its notifications, so this
     * tells a maintainer which language's rules a shared log can improve (spec/300-data/ExportFormat).
     */
    val locale: String = "und",
    /**
     * The package names the user added, per app id, at export time (FR7), so maintainers can consider
     * making them built in. Package names are not personal data; present in every mode.
     */
    val userAddedPackages: Map<String, List<String>> = emptyMap(),
) {
    companion object {
        const val SCHEMA_VERSION = 4
    }
}

/**
 * The versions an event was recorded with (#84): PebbleNTN, the navigation app (its notification
 * texts, layouts and drawables change between releases) and the bundled rulesets that hold rules for
 * it. Version strings are not personal data, so they are kept in privacy-safe exports. Null on events
 * recorded before the app stored them.
 */
@Serializable
data class ExportedVersions(
    val appVersion: String? = null,
    val appVersionCode: Long? = null,
    val sourceAppVersion: String? = null,
    val sourceAppVersionCode: Long? = null,
    val rulesetVersions: List<String> = emptyList(),
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
    val stopsRemaining: Int? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExportedEvent(
    /** The package that posted the notification, the one used to detect the app. */
    val packageName: String,
    /**
     * True when [packageName] is one the user added (FR7), as of the export. Omitted for a built-in
     * package, so those events read exactly as before.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val customPackage: Boolean = false,
    val eventType: String,
    val disposition: String,
    val matchedRuleId: String? = null,
    val versions: ExportedVersions = ExportedVersions(),
    /**
     * Consecutive notifications of the same card, grouped into this event (#86): how many, when the
     * first and the latest were posted, and the latest one ([snapshot] is the first). [lastSnapshot]
     * is null when it occurred once.
     */
    val occurrences: Int = 1,
    val firstSeenMillis: Long? = null,
    val lastSeenMillis: Long? = null,
    val watchOutput: ExportedWatchOutput? = null,
    val snapshot: NotificationSnapshot? = null,
    val lastSnapshot: NotificationSnapshot? = null,
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
        locale: String = "und",
        appVersionCode: Long? = null,
        /** The user-added package names per app id (FR7). */
        userAddedPackages: Map<String, List<String>> = emptyMap(),
    ): String {
        val metadata = ExportMetadata(
            mode = mode.name,
            exportedAt = exportedAt,
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            androidRelease = androidRelease,
            locale = locale,
            userAddedPackages = userAddedPackages,
        )
        val custom = userAddedPackages.values.flatten().toSet()
        val bundle = when (mode) {
            ExportMode.RULES_ONLY -> ExportBundle(metadata, userRules = userRules)
            ExportMode.PRIVACY_SAFE -> ExportBundle(metadata, userRules, events.map { toExported(it, redact = true, custom) })
            ExportMode.FULL -> ExportBundle(metadata, userRules, events.map { toExported(it, redact = false, custom) })
        }
        return json.encodeToString(ExportBundle.serializer(), bundle)
    }

    private fun toExported(event: DebugEvent, redact: Boolean, customPackages: Set<String>): ExportedEvent {
        val snapshot = event.snapshot?.let { if (redact) redactor.redact(it) else it }
        val lastSnapshot = event.lastSnapshot?.let { if (redact) redactor.redact(it) else it }
        // The maneuver and distance are already normalized (no free text), so they survive
        // redaction; the road text came from the notification and must not.
        val watchOutput = event.instruction?.let {
            ExportedWatchOutput(
                maneuver = it.maneuver.name,
                distanceMeters = it.distanceMeters,
                primaryText = it.primaryText?.let { text -> if (redact) redactor.redactText(text) else text },
                secondaryText = it.secondaryText?.let { text -> if (redact) redactor.redactText(text) else text },
                stopsRemaining = it.stopsRemaining,
            )
        }
        return ExportedEvent(
            packageName = event.packageName,
            customPackage = event.packageName in customPackages,
            eventType = event.eventType.name,
            disposition = event.disposition,
            matchedRuleId = event.matchedRuleId,
            versions = event.versions.let {
                ExportedVersions(
                    appVersion = it.appVersionName,
                    appVersionCode = it.appVersionCode,
                    sourceAppVersion = it.sourceAppVersionName,
                    sourceAppVersionCode = it.sourceAppVersionCode,
                    rulesetVersions = it.rulesetVersions?.split(',')?.filter(String::isNotBlank).orEmpty(),
                )
            },
            occurrences = event.occurrences,
            firstSeenMillis = event.eventTimestampMillis,
            lastSeenMillis = event.lastEventTimestampMillis ?: event.eventTimestampMillis,
            watchOutput = watchOutput,
            snapshot = snapshot,
            lastSnapshot = lastSnapshot,
        )
    }
}
