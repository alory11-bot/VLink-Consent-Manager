package com.vlink.consent

import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Short-lived, one-time invitation envelope intended for QR transport.
 * The secret is a 256-bit bearer value: never log it or place it in analytics.
 * This model does not itself establish a network connection or authenticate a peer.
 */
data class PairingInvite(
    val sessionId: String,
    val secret: ByteArray,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long
) {
    fun isValidAt(nowMillis: Long): Boolean =
        sessionId.isNotBlank() &&
            secret.size == SECRET_BYTES &&
            issuedAtMillis > 0L &&
            expiresAtMillis > issuedAtMillis &&
            expiresAtMillis - issuedAtMillis == LIFETIME_MILLIS &&
            nowMillis >= issuedAtMillis &&
            nowMillis < expiresAtMillis

    fun matches(candidate: ByteArray): Boolean =
        candidate.size == secret.size && MessageDigest.isEqual(secret, candidate)

    /** URI suitable for encoding as a QR code. Treat it as a secret. */
    fun toQrUri(): String {
        require(secret.size == SECRET_BYTES)
        return "vlink://pair?v=1&sid=$sessionId&secret=${Base64.getUrlEncoder().withoutPadding().encodeToString(secret)}&iat=$issuedAtMillis&exp=$expiresAtMillis"
    }

    companion object {
        private const val SECRET_BYTES = 32
        private const val LIFETIME_MILLIS = 300_000L
        private val random = SecureRandom()

        fun create(nowMillis: Long): PairingInvite {
            require(nowMillis > 0L)
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
            val fields = uri.rawQuery.orEmpty().split('&').associate { part ->
                val pair = part.split('=', limit = 2)
                require(pair.size == 2)
                pair[0] to pair[1]
            }
            require(fields.keys == setOf("v", "sid", "secret", "iat", "exp"))
            require(fields["v"] == "1")
            val secret = Base64.getUrlDecoder().decode(fields.getValue("secret"))
            val invite = PairingInvite(
                sessionId = fields.getValue("sid"),
                secret = secret,
                issuedAtMillis = fields.getValue("iat").toLong(),
                expiresAtMillis = fields.getValue("exp").toLong()
            )
            require(invite.sessionId.matches(Regex("[0-9a-fA-F-]{36}")))
            require(secret.size == SECRET_BYTES)
            require(invite.expiresAtMillis - invite.issuedAtMillis == LIFETIME_MILLIS)
            invite
        }.getOrNull()
    }
}
