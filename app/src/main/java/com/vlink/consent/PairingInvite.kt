package com.vlink.consent

import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Short-lived, one-time invitation envelope intended for QR transport.
 * The 256-bit secret is a bearer credential: never log it or send it to analytics.
 * This value object does not establish a network connection or authenticate a peer.
 */
class PairingInvite(
    val sessionId: String,
    secret: ByteArray,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long
) {
    private val secretBytes = secret.copyOf()

    fun isValidAt(nowMillis: Long): Boolean =
        sessionId.isNotBlank() &&
            secretBytes.size == SECRET_BYTES &&
            issuedAtMillis > 0L &&
            expiresAtMillis > issuedAtMillis &&
            expiresAtMillis - issuedAtMillis == LIFETIME_MILLIS &&
            nowMillis >= issuedAtMillis &&
            nowMillis < expiresAtMillis

    fun matches(candidate: ByteArray): Boolean =
        candidate.size == secretBytes.size && MessageDigest.isEqual(secretBytes, candidate)

    fun hasSameSecretAs(other: PairingInvite): Boolean =
        MessageDigest.isEqual(secretBytes, other.secretBytes)

    /** URI suitable for encoding as a QR code. Treat it as a secret. */
    fun toQrUri(): String {
        require(secretBytes.size == SECRET_BYTES)
        return "vlink://pair?v=1&sid=$sessionId&secret=${Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes)}&iat=$issuedAtMillis&exp=$expiresAtMillis"
    }

    companion object {
        private const val SECRET_BYTES = 32
        private const val LIFETIME_MILLIS = 300_000L
        private val random = SecureRandom()

        fun create(nowMillis: Long): PairingInvite {
            require(nowMillis > 0L && nowMillis <= Long.MAX_VALUE - LIFETIME_MILLIS)
            val secret = ByteArray(SECRET_BYTES).also(random::nextBytes)
            return PairingInvite(
                sessionId = UUID.randomUUID().toString(),
                secret = secret,
                issuedAtMillis = nowMillis,
                expiresAtMillis = nowMillis + LIFETIME_MILLIS
            )
        }

        fun parseQrUri(value: String): PairingInvite? = runCatching {
            val uri = URI(value)
            require(uri.scheme == "vlink" && uri.host == "pair" && uri.path.isNullOrEmpty())
            val parts = uri.rawQuery.orEmpty().split('&')
            val fields = parts.associate { part ->
                val pair = part.split('=', limit = 2)
                require(pair.size == 2)
                pair[0] to pair[1]
            }
            require(parts.size == fields.size)
            require(fields.keys == setOf("v", "sid", "secret", "iat", "exp"))
            require(fields["v"] == "1")
            val sessionId = fields.getValue("sid")
            require(UUID.fromString(sessionId).toString() == sessionId)
            val secret = Base64.getUrlDecoder().decode(fields.getValue("secret"))
            require(secret.size == SECRET_BYTES)
            val issuedAt = fields.getValue("iat").toLong()
            val expiresAt = fields.getValue("exp").toLong()
            require(issuedAt > 0L && issuedAt <= Long.MAX_VALUE - LIFETIME_MILLIS)
            require(expiresAt - issuedAt == LIFETIME_MILLIS)
            PairingInvite(sessionId, secret, issuedAt, expiresAt)
        }.getOrNull()
    }
}
