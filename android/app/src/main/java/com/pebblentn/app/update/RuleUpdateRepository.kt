package com.pebblentn.app.update

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.core.Hashing
import com.pebblentn.app.data.db.OfficialRulesetDao
import com.pebblentn.app.data.db.OfficialRulesetEntity
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.serialization.json.Json
import java.security.PublicKey

/** Outcome of attempting to activate a downloaded ruleset. */
sealed interface ActivationResult {
    /** The feature is disabled (default). */
    data object Disabled : ActivationResult
    data class MalformedEnvelope(val message: String?) : ActivationResult
    data object SignatureInvalid : ActivationResult
    data class SchemaInvalid(val message: String?) : ActivationResult
    data class SelfTestFailed(val failedVector: String) : ActivationResult
    data class Activated(val version: String) : ActivationResult
}

/**
 * Manages downloaded official rulesets (spec/600-security-release remote rules; REQ-SEC-005/006).
 *
 * The feature is **off by default** ([enabled] = false): no automatic checks run in the initial
 * public version. When enabled, an update is only activated after ALL gates pass, in order:
 * signature (Ed25519) → schema/semantic validation → embedded self-tests. The previous active
 * ruleset is retained as last-known-good and can be [rollback]ed. No executable code is ever
 * accepted — rulesets are declarative data.
 *
 * The active downloaded rules are exposed as the engine's *downloaded* layer via [downloadedLayer],
 * read from a `@Volatile` cache (like the other repositories).
 */
class RuleUpdateRepository(
    private val dao: OfficialRulesetDao,
    private val publicKeys: List<PublicKey>,
    private val engine: RuleEngine = RuleEngine(),
    private val clock: EpochClock = EpochClock.SYSTEM,
    private val enabled: Boolean = false,
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    @Volatile
    private var activeRules: List<Rule> = emptyList()

    fun downloadedLayer(): List<Rule> = activeRules

    suspend fun refreshCache() {
        activeRules = dao.getByStatus(OfficialRulesetEntity.STATUS_ACTIVE)
            ?.let { runCatching { RulesetCodec.parse(it.canonicalJson).rules }.getOrDefault(emptyList()) }
            ?: emptyList()
    }

    /** Verify, validate, self-test and (if all pass) atomically activate a downloaded update. */
    suspend fun tryActivate(envelopeJson: String): ActivationResult {
        if (!enabled) return ActivationResult.Disabled

        val envelope = runCatching { json.decodeFromString(SignedRuleset.serializer(), envelopeJson) }
            .getOrElse { return ActivationResult.MalformedEnvelope(it.message) }

        val payload = envelope.rulesetCanonical.toByteArray(Charsets.UTF_8)
        if (!RulesetSignatureVerifier.verify(payload, envelope.signature.value, publicKeys)) {
            return ActivationResult.SignatureInvalid // last-known-good retained
        }

        val ruleset = runCatching { RulesetCodec.parse(envelope.rulesetCanonical) }
            .getOrElse { return ActivationResult.SchemaInvalid(it.message) }

        for (vector in envelope.testVectors) {
            if (!selfTestPasses(ruleset.rules, vector)) {
                return ActivationResult.SelfTestFailed(vector.name)
            }
        }

        val now = clock.nowMillis()
        dao.activate(
            OfficialRulesetEntity(
                version = ruleset.rulesetVersion,
                source = OfficialRulesetEntity.SOURCE_DOWNLOADED,
                schemaVersion = ruleset.schemaVersion,
                signatureStatus = OfficialRulesetEntity.SIGNATURE_VERIFIED,
                activationStatus = OfficialRulesetEntity.STATUS_ACTIVE,
                installedTimestamp = now,
                payloadHash = Hashing.sha256Hex(envelope.rulesetCanonical),
                canonicalJson = envelope.rulesetCanonical,
            ),
        )
        refreshCache()
        return ActivationResult.Activated(ruleset.rulesetVersion)
    }

    /** Roll back to the last-known-good ruleset, if any. */
    suspend fun rollback(): Boolean {
        val rolled = dao.rollback()
        if (rolled) refreshCache()
        return rolled
    }

    private fun selfTestPasses(rules: List<Rule>, vector: RuleTestVector): Boolean {
        val snapshot = NotificationSnapshot(
            packageName = vector.packageName,
            notificationId = 0,
            title = vector.snapshot.title,
            text = vector.snapshot.text,
            subText = vector.snapshot.subText,
            bigText = vector.snapshot.bigText,
        )
        val result = engine.evaluate(snapshot, LayeredRules(downloaded = rules), vector.locale, nowEpochSeconds = 0)
        val instruction = result.instruction ?: return false
        if (instruction.maneuver.name != vector.expected.maneuver) return false
        val expectedDistance = vector.expected.distanceMeters
        return expectedDistance == null || instruction.distanceMeters == expectedDistance
    }
}
