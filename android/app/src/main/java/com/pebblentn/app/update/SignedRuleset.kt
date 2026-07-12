package com.pebblentn.app.update

import kotlinx.serialization.Serializable

/**
 * A downloadable official-rules update envelope (spec/300-data/RulesJSON.md "signature metadata";
 * spec/600-security-release remote rules). Declarative data only — no executable content.
 *
 * The signature is computed over [rulesetCanonical] exactly (the canonical ruleset JSON is what is
 * signed). [testVectors] are self-tests the ruleset must pass before it may be activated.
 */
@Serializable
data class SignedRuleset(
    val rulesetCanonical: String,
    val testVectors: List<RuleTestVector> = emptyList(),
    val signature: RulesetSignature,
)

@Serializable
data class RulesetSignature(
    val algorithm: String,
    val keyId: String? = null,
    /** Base64 (standard) of the raw signature bytes. */
    val value: String,
) {
    companion object {
        const val ED25519 = "ed25519"
    }
}

/** A self-test vector: a synthetic snapshot and the normalized output the ruleset must produce. */
@Serializable
data class RuleTestVector(
    val name: String,
    val packageName: String,
    val locale: String? = null,
    val snapshot: TestSnapshot,
    val expected: ExpectedOutput,
)

@Serializable
data class TestSnapshot(
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val bigText: String? = null,
)

@Serializable
data class ExpectedOutput(
    val maneuver: String,
    val distanceMeters: Int? = null,
)
