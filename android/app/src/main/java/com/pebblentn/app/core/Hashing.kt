package com.pebblentn.app.core

import java.security.MessageDigest

/** Small hashing helper. Used to store notification keys/tags as hashes, never in the clear. */
object Hashing {
    /** Lowercase hex SHA-256 of [value]. */
    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (b in digest) {
                append(HEX[(b.toInt() shr 4) and 0xF])
                append(HEX[b.toInt() and 0xF])
            }
        }
    }

    private const val HEX = "0123456789abcdef"
}
