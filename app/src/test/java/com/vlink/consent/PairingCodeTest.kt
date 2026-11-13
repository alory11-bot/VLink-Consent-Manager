package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class PairingCodeTest {
    @Test fun generatedCodeHasEightAllowedCharacters() {
        val code = PairingCode.create()
        assertEquals(8, code.length)
        assertTrue(PairingCode.isValid(code))
    }

    @Test fun invalidCodesAreRejected() {
        assertFalse(PairingCode.isValid("ABC"))
        assertFalse(PairingCode.isValid("ABCDEFG0"))
        assertFalse(PairingCode.isValid("ABCDEFGH!"))
    }

    @Test fun invitationExpiresAfterFiveMinutesAndRejectsFutureIssueTime() {
        val issuedAt = 1_000_000L
        assertTrue(PairingCode.isValidForWindow("ABCDEFGH", issuedAt, issuedAt + 299_999L))
        assertFalse(PairingCode.isValidForWindow("ABCDEFGH", issuedAt, issuedAt + 300_001L))
        assertFalse(PairingCode.isValidForWindow("ABCDEFGH", issuedAt, issuedAt - 1L))
    }
}
