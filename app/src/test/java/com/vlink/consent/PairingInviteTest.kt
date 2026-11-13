package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class PairingInviteTest {
    @Test fun createsHighEntropyShortLivedInvite() {
        val invite = PairingInvite.create(1_000_000L)
        assertEquals(43, invite.toQrUri().substringAfter("secret=").substringBefore("&").length)
        assertTrue(invite.isValidAt(1_000_000L))
        assertTrue(invite.isValidAt(1_299_999L))
        assertFalse(invite.isValidAt(1_300_000L))
    }

    @Test fun qrEnvelopeRoundTripsWithoutLosingSecret() {
        val invite = PairingInvite.create(2_000_000L)
        val parsed = PairingInvite.parseQrUri(invite.toQrUri())
        assertNotNull(parsed)
        assertEquals(invite.toQrUri(), parsed!!.toQrUri())
        assertTrue(parsed.isValidAt(2_000_001L))
    }

    @Test fun rejectsTamperedMalformedDuplicateAndWrongSchemePayloads() {
        val invite = PairingInvite.create(3_000_000L)
        assertNull(PairingInvite.parseQrUri(invite.toQrUri().replace("vlink://", "http://")))
        assertNull(PairingInvite.parseQrUri(invite.toQrUri().replace("v=1", "v=2")))
        assertNull(PairingInvite.parseQrUri(invite.toQrUri() + "&v=1"))
        assertNull(PairingInvite.parseQrUri("vlink://pair?v=1&sid=x&secret=abc&iat=1&exp=2"))
    }

    @Test fun secretComparisonRejectsDifferentValues() {
        val invite = PairingInvite.create(4_000_000L)
        val parsed = PairingInvite.parseQrUri(invite.toQrUri())!!
        assertTrue(invite.hasSameSecretAs(parsed))
        assertFalse(invite.matches(ByteArray(32)))
    }
}
