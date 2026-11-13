package com.vlink.consent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleCapabilityPolicyTest {
    @Test fun onlyMasterCanRequest() {
        assertTrue(RoleCapabilityPolicy.canRequest(DeviceRole.MASTER, CapabilityStatus.NOT_REQUESTED))
        assertFalse(RoleCapabilityPolicy.canRequest(DeviceRole.SLAVE, CapabilityStatus.NOT_REQUESTED))
    }

    @Test fun onlySlaveCanDecidePendingRequest() {
        assertTrue(RoleCapabilityPolicy.canDecide(DeviceRole.SLAVE, CapabilityStatus.REQUESTED))
        assertFalse(RoleCapabilityPolicy.canDecide(DeviceRole.MASTER, CapabilityStatus.REQUESTED))
        assertEquals(CapabilityStatus.GRANTED,
            RoleCapabilityPolicy.nextStatus(DeviceRole.SLAVE, CapabilityStatus.REQUESTED, true))
        assertEquals(CapabilityStatus.REJECTED,
            RoleCapabilityPolicy.nextStatus(DeviceRole.SLAVE, CapabilityStatus.REQUESTED, false))
    }

    @Test fun grantedCapabilityCannotBeRequestedAgainUntilRevoked() {
        assertFalse(RoleCapabilityPolicy.canRequest(DeviceRole.MASTER, CapabilityStatus.GRANTED))
    }

    @Test(expected = IllegalArgumentException::class)
    fun masterCannotGrant() {
        RoleCapabilityPolicy.nextStatus(DeviceRole.MASTER, CapabilityStatus.REQUESTED, true)
    }

    @Test fun slaveCanRevokeGrantedCapability() {
        assertEquals(CapabilityStatus.REJECTED,
            RoleCapabilityPolicy.revoke(DeviceRole.SLAVE, CapabilityStatus.GRANTED))
    }
}
