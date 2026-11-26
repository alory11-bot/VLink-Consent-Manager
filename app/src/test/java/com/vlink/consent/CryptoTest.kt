package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class CryptoTest {
    @Test fun roundTripRestoresPlaintext() {
        val k = Crypto.newKey()
        val msg = "سلام VLink".toByteArray()
        assertArrayEquals(msg, Crypto.open(k, Crypto.seal(k, msg)))
    }

    @Test fun differentKeysDoNotOpen() {
        val blob = Crypto.seal(Crypto.newKey(), "x".toByteArray())
        assertNull(Crypto.open(Crypto.newKey(), blob))
    }

    @Test fun tamperedByteFails() {
        val k = Crypto.newKey()
        val blob = Crypto.seal(k, "payload".toByteArray())
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0x01).toByte()
        assertNull(Crypto.open(k, blob))
    }

    @Test fun noncesDiffer() {
        val k = Crypto.newKey()
        assertFalse(Crypto.seal(k, "same".toByteArray()).contentEquals(Crypto.seal(k, "same".toByteArray())))
    }

    @Test fun base64RoundTrip() {
        val raw = Crypto.newKey()
        assertArrayEquals(raw, Crypto.unb64(Crypto.b64(raw)))
    }
}
