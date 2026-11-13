package com.vlink.consent

import java.net.URI
import java.security.SecureRandom
import java.util.Base64

/** A five-minute bearer invite used only to authorize entry to a signaling room. */
data class InternetInvite(
    val room: String,
    val token: String,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long
) {
    fun isValidAt(nowMillis: Long): Boolean =
        ROOM_PATTERN.matches(room) &&
            TOKEN_PATTERN.matches(token) &&
            issuedAtMillis > 0L &&
            expiresAtMillis - issuedAtMillis == LIFETIME_MS &&
            nowMillis >= issuedAtMillis && nowMillis < expiresAtMillis

    fun toShareText(): String =
        "vlink://internet?room=$room&token=$token&iat=$issuedAtMillis&exp=$expiresAtMillis"

    companion object {
        private const val LIFETIME_MS = 5 * 60 * 1000L
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val ROOM_PATTERN = Regex("^[A-Z2-9]{8}$")
        private val TOKEN_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
        private val random = SecureRandom()

        fun create(nowMillis: Long = System.currentTimeMillis()): InternetInvite {
            require(nowMillis > 0L && nowMillis <= Long.MAX_VALUE - LIFETIME_MS)
            val room = buildString(8) { repeat(8) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
            val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
            return InternetInvite(room, token, nowMillis, nowMillis + LIFETIME_MS)
        }

        fun parse(value: String): InternetInvite? = runCatching {
            val uri = URI(value.trim())
            require(uri.scheme == "vlink" && uri.host == "internet" && uri.path.isNullOrEmpty())
            val parts = uri.rawQuery.orEmpty().split('&')
            val fields = parts.associate { item ->
                val pair = item.split('=', limit = 2)
                require(pair.size == 2)
                pair[0] to pair[1]
            }
            require(parts.size == fields.size)
            require(fields.keys == setOf("room", "token", "iat", "exp"))
            InternetInvite(
                room = fields.getValue("room"),
                token = fields.getValue("token"),
                issuedAtMillis = fields.getValue("iat").toLong(),
                expiresAtMillis = fields.getValue("exp").toLong()
            ).also { require(it.isValidAt(System.currentTimeMillis())) }
        }.getOrNull()
    }
}
