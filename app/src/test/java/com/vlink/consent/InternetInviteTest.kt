package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class InternetInviteTest {
    @Test fun inviteRoundTripsAndExpires() {
        val now = 1_800_000_000_000L
        val invite = InternetInvite.create(now)
        assertTrue(invite.isValidAt(now))
        val parsed = InternetInvite.parse(invite.toShareText())
        assertNotNull(parsed)
        assertEquals(invite.room, parsed!!.room)
        assertEquals(invite.token, parsed.token)
        assertFalse(invite.isValidAt(now + 300_000L))
    }

    @Test fun rejectsMalformedAndExpiredInvites() {
        assertNull(InternetInvite.parse("vlink://internet?room=bad&token=x&iat=1&exp=2"))
        val expired = InternetInvite.create(1_700_000_000_000L)
        assertNull(InternetInvite.parse(expired.toShareText()))
    }
}
