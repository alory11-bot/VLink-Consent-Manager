package com.vlink.consent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectP2pPolicyTest {
    @Test fun masterAdvertisesAndSlaveDiscovers() {
        assertEquals(DirectP2pPolicy.Action.ADVERTISE, DirectP2pPolicy.actionFor(DeviceRole.MASTER))
        assertEquals(DirectP2pPolicy.Action.DISCOVER, DirectP2pPolicy.actionFor(DeviceRole.SLAVE))
    }
    @Test fun supportsSinglePeerAndMultipointLimits() {
        assertEquals(1, DirectP2pPolicy.peerLimit(false))
        assertEquals(Int.MAX_VALUE, DirectP2pPolicy.peerLimit(true))
        assertTrue(!DirectP2pPolicy.RELAY_FALLBACK_ENABLED)
    }
}
