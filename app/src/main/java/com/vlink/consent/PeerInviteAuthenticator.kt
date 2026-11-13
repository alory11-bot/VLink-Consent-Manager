package com.vlink.consent

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Mutual possession proof for the invite secret; safe to use over the encrypted DataChannel. */
object PeerInviteAuthenticator {
    fun createProof(secret: String, room: String, masterNonce: String, slaveNonce: String): String {
        require(room.matches(Regex("^[A-Z2-9]{8}$")))
        val key = Base64.getUrlDecoder().decode(secret)
        require(key.size == 32)
        require(masterNonce.isNotBlank() && slaveNonce.isNotBlank() && masterNonce != slaveNonce)
        val message = "VLINK-WEBRTC-V1|$room|$masterNonce|$slaveNonce"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    fun verify(secret: String, room: String, masterNonce: String, slaveNonce: String, candidate: String): Boolean =
        runCatching {
            val expected = createProof(secret, room, masterNonce, slaveNonce)
            MessageDigest.isEqual(expected.toByteArray(Charsets.US_ASCII), candidate.toByteArray(Charsets.US_ASCII))
        }.getOrDefault(false)
}
