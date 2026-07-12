package com.pebblentn.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class RulesetSignatureVerifierTest {

    private fun keyPair() = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    private fun sign(payload: ByteArray, privateKey: java.security.PrivateKey): String {
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(privateKey)
        sig.update(payload)
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    private val payload = "canonical ruleset payload".toByteArray()

    @Test
    fun validSignatureVerifies() {
        val kp = keyPair()
        val signature = sign(payload, kp.private)
        assertTrue(RulesetSignatureVerifier.verify(payload, signature, listOf(kp.public)))
    }

    @Test
    fun tamperedPayloadFails() {
        val kp = keyPair()
        val signature = sign(payload, kp.private)
        assertFalse(RulesetSignatureVerifier.verify("tampered".toByteArray(), signature, listOf(kp.public)))
    }

    @Test
    fun wrongKeyFails() {
        val signer = keyPair()
        val other = keyPair()
        val signature = sign(payload, signer.private)
        assertFalse(RulesetSignatureVerifier.verify(payload, signature, listOf(other.public)))
    }

    @Test
    fun garbageSignatureFails() {
        val kp = keyPair()
        assertFalse(RulesetSignatureVerifier.verify(payload, "not-base64!!", listOf(kp.public)))
        assertFalse(RulesetSignatureVerifier.verify(payload, "AAAA", listOf(kp.public)))
    }

    @Test
    fun anyOfMultipleKeysVerifies() {
        val signer = keyPair()
        val other = keyPair()
        val signature = sign(payload, signer.private)
        // Key rotation: the signer is one of several trusted keys.
        assertTrue(RulesetSignatureVerifier.verify(payload, signature, listOf(other.public, signer.public)))
    }

    @Test
    fun publicKeyRoundTripsThroughBase64() {
        val kp = keyPair()
        val b64 = Base64.getEncoder().encodeToString(kp.public.encoded)
        val restored = RulesetSignatureVerifier.publicKeyFromBase64(b64)
        val signature = sign(payload, kp.private)
        assertTrue(RulesetSignatureVerifier.verify(payload, signature, listOf(restored)))
    }
}
