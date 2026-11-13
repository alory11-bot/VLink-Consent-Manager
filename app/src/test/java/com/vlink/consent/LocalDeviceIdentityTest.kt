package com.vlink.consent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class LocalDeviceIdentityTest {
    @Test fun identityRoundTripsWithoutChangingItsIdOrCreationTime() {
        val original = LocalDeviceIdentity(UUID.randomUUID().toString(), 1_800_000_000_000L)
        val decoded = LocalDeviceIdentity.decode(LocalDeviceIdentity.encode(original))
        assertNotNull(decoded)
        assertEquals(original, decoded)
    }

    @Test fun shortIdIsStableAndReadable() {
        val identity = LocalDeviceIdentity("12345678-1234-4234-8234-123456789abc", 1L)
        assertEquals("12345678", identity.shortId)
    }

    @Test fun rejectsUnsupportedVersionMalformedUuidAndInvalidTimestamp() {
        assertNull(LocalDeviceIdentity.decode("2|12345678-1234-4234-8234-123456789abc|1"))
        assertNull(LocalDeviceIdentity.decode("1|not-a-uuid|1"))
        assertNull(LocalDeviceIdentity.decode("1|12345678-1234-4234-8234-123456789abc|0"))
    }
}
