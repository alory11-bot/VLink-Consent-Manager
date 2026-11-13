package com.vlink.consent

import java.security.SecureRandom

data class AuditEvent(val at: Long, val action: String, val detail: String)
object PairingCode {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()
    fun create(length: Int = 8): String {
        require(length in 6..12)
        return buildString(length) { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
    }
    fun isValid(value: String): Boolean =
        value.length in 6..12 && value.all { it in ALPHABET }
}
