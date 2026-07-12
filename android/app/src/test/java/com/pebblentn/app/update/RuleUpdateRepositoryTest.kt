package com.pebblentn.app.update

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.DistanceExtractor
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import com.pebblentn.app.rules.Ruleset
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class RuleUpdateRepositoryTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var keyPair: KeyPair

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    }

    @After
    fun tearDown() = db.close()

    private fun repo(enabled: Boolean) = RuleUpdateRepository(
        dao = db.officialRulesetDao(),
        publicKeys = listOf(keyPair.public),
        clock = EpochClock { 1000 },
        enabled = enabled,
    )

    private fun turnRightRuleset(version: String) = Ruleset(
        schemaVersion = 1,
        rulesetVersion = version,
        minimumAppVersionCode = 1,
        createdAt = "2026-07-12T00:00:00Z",
        publisher = "PebbleNTN maintainers",
        rules = listOf(
            Rule(
                id = "turn-right",
                enabled = true,
                priority = 100,
                packageNames = listOf("com.google.android.apps.maps"),
                conditions = listOf(Condition("combinedText", ConditionOperator.REGEX, "(?i)turn right")),
                output = RuleOutput(maneuver = LiteralExtractor("RIGHT"), distanceMeters = DistanceExtractor("combinedText")),
            ),
        ),
    )

    private fun sign(canonical: String): String {
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(keyPair.private)
        sig.update(canonical.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    private fun envelope(
        ruleset: Ruleset,
        vectors: List<RuleTestVector>,
        signatureOverride: String? = null,
    ): String {
        val canonical = RulesetCodec.canonicalize(ruleset)
        val signed = SignedRuleset(
            rulesetCanonical = canonical,
            testVectors = vectors,
            signature = RulesetSignature(RulesetSignature.ED25519, value = signatureOverride ?: sign(canonical)),
        )
        return Json.encodeToString(SignedRuleset.serializer(), signed)
    }

    private val passingVector = RuleTestVector(
        name = "turn-right",
        packageName = "com.google.android.apps.maps",
        snapshot = TestSnapshot(title = "Turn right", text = "500 m"),
        expected = ExpectedOutput(maneuver = "RIGHT", distanceMeters = 500),
    )

    @Test
    fun disabledByDefaultDoesNotActivate() = runTest {
        val result = repo(enabled = false).tryActivate(envelope(turnRightRuleset("v1"), listOf(passingVector)))
        assertEquals(ActivationResult.Disabled, result)
    }

    @Test
    fun validSignedRulesetWithPassingSelfTestsActivates() = runTest {
        val repo = repo(enabled = true)
        val result = repo.tryActivate(envelope(turnRightRuleset("v1"), listOf(passingVector)))
        assertTrue(result is ActivationResult.Activated)
        assertEquals("v1", (result as ActivationResult.Activated).version)
        assertEquals(listOf("turn-right"), repo.downloadedLayer().map { it.id })
    }

    @Test
    fun tamperedSignatureIsRejected() = runTest {
        val repo = repo(enabled = true)
        val bad = Base64.getEncoder().encodeToString(ByteArray(64))
        val result = repo.tryActivate(envelope(turnRightRuleset("v1"), listOf(passingVector), signatureOverride = bad))
        assertEquals(ActivationResult.SignatureInvalid, result)
        assertTrue(repo.downloadedLayer().isEmpty())
    }

    @Test
    fun failingSelfTestKeepsPreviousActive() = runTest {
        val repo = repo(enabled = true)
        // First, activate a good v1.
        assertTrue(repo.tryActivate(envelope(turnRightRuleset("v1"), listOf(passingVector))) is ActivationResult.Activated)

        // v2 signed correctly but with a self-test that the ruleset cannot satisfy.
        val badVector = passingVector.copy(name = "bad", expected = ExpectedOutput(maneuver = "LEFT"))
        val result = repo.tryActivate(envelope(turnRightRuleset("v2"), listOf(badVector)))
        assertEquals(ActivationResult.SelfTestFailed("bad"), result)
        // v1 remains active.
        assertEquals(listOf("turn-right"), repo.downloadedLayer().map { it.id })
        assertEquals("v1", db.officialRulesetDao().getByStatus("ACTIVE")!!.version)
    }

    @Test
    fun rollbackRestoresLastKnownGood() = runTest {
        val repo = repo(enabled = true)
        repo.tryActivate(envelope(turnRightRuleset("v1"), listOf(passingVector)))
        repo.tryActivate(envelope(turnRightRuleset("v2"), listOf(passingVector)))
        assertEquals("v2", db.officialRulesetDao().getByStatus("ACTIVE")!!.version)

        assertTrue(repo.rollback())
        assertEquals("v1", db.officialRulesetDao().getByStatus("ACTIVE")!!.version)
    }

    @Test
    fun signedNonRulesetIsSchemaInvalid() = runTest {
        val repo = repo(enabled = true)
        val canonical = "{\"not\":\"a ruleset\"}"
        val signed = SignedRuleset(canonical, emptyList(), RulesetSignature(RulesetSignature.ED25519, value = sign(canonical)))
        val result = repo.tryActivate(Json.encodeToString(SignedRuleset.serializer(), signed))
        assertTrue(result is ActivationResult.SchemaInvalid)
        assertFalse(repo.downloadedLayer().isNotEmpty())
    }
}
