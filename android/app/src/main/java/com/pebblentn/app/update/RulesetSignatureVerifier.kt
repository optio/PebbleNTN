package com.pebblentn.app.update

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Verifies the Ed25519 signature of a downloaded ruleset against embedded public keys
 * (REQ-SEC-005). Verification failure means the update is rejected and the last-known-good ruleset
 * is retained (REQ-SEC-006).
 *
 * Note: Ed25519 via `java.security` is available on the host JVM (used by tests) and on Android 13+
 * (API 33). On API 31–32 a bundled Ed25519 provider is required before enabling remote updates; the
 * feature is off by default until then.
 */
object RulesetSignatureVerifier {

    private const val ALGORITHM = "Ed25519"

    /** True if [signatureBase64] is a valid Ed25519 signature of [payload] by one of [publicKeys]. */
    fun verify(payload: ByteArray, signatureBase64: String, publicKeys: List<PublicKey>): Boolean {
        val signatureBytes = runCatching { Base64.getDecoder().decode(signatureBase64) }.getOrNull() ?: return false
        return publicKeys.any { key -> verifyOne(payload, signatureBytes, key) }
    }

    private fun verifyOne(payload: ByteArray, signature: ByteArray, key: PublicKey): Boolean =
        runCatching {
            Signature.getInstance(ALGORITHM).apply {
                initVerify(key)
                update(payload)
            }.verify(signature)
        }.getOrDefault(false)

    /** Parse a Base64, X.509-encoded Ed25519 public key. */
    fun publicKeyFromBase64(base64: String): PublicKey {
        val der = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance(ALGORITHM).generatePublic(X509EncodedKeySpec(der))
    }
}
