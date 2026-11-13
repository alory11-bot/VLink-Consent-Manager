package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CapabilityWireMessageTest {
    private val masterId = "11111111-1111-4111-8111-111111111111"
    private val slaveId = "22222222-2222-4222-8222-222222222222"
    private val room = "ABCDEFGH"
    private val now = 1_800_000_000_000L

    private fun message(
        role: DeviceRole = DeviceRole.MASTER,
        type: CapabilityMessageType = CapabilityMessageType.REQUEST,
        status: CapabilityStatus = CapabilityStatus.REQUESTED,
        revision: Long = 1L
    ) = CapabilityWireMessage(
        messageId = UUID.randomUUID().toString(),
        requestId = UUID.randomUUID().toString(),
        sessionId = room,
        senderDeviceId = if (role == DeviceRole.MASTER) masterId else slaveId,
        senderRole = role,
        capabilityKey = Capability.MESSAGES.key,
        type = type,
        status = status,
        revision = revision,
        createdAtEpochMs = now
    )

    @Test fun masterRequestIsAcceptedOnlyForTheExpectedPeerAndSession() {
        val request = message()
        assertNull(request.validate(room, masterId, DeviceRole.MASTER, now))
        assertEquals("sender identity mismatch", request.validate(room, slaveId, DeviceRole.MASTER, now))
        assertEquals("wrong session", request.validate("ZXCVBNM2", masterId, DeviceRole.MASTER, now))
        assertEquals("sender role mismatch", request.validate(room, masterId, DeviceRole.SLAVE, now))
    }

    @Test fun onlySlaveCanSendGrantOrRejectDecisions() {
        val grant = message(DeviceRole.SLAVE, CapabilityMessageType.DECISION, CapabilityStatus.GRANTED, 2)
        val reject = message(DeviceRole.SLAVE, CapabilityMessageType.DECISION, CapabilityStatus.REJECTED, 2)
        assertNull(grant.validate(room, slaveId, DeviceRole.SLAVE, now))
        assertNull(reject.validate(room, slaveId, DeviceRole.SLAVE, now))
        assertNotNull(message(DeviceRole.MASTER, CapabilityMessageType.DECISION, CapabilityStatus.GRANTED, 2)
            .validate(room, masterId, DeviceRole.MASTER, now))
    }

    @Test fun revokeMustComeFromSlaveAndUseRejectedState() {
        val revoke = message(DeviceRole.SLAVE, CapabilityMessageType.REVOKE, CapabilityStatus.REJECTED, 3)
        assertNull(revoke.validate(room, slaveId, DeviceRole.SLAVE, now))
        assertNotNull(message(DeviceRole.SLAVE, CapabilityMessageType.REVOKE, CapabilityStatus.GRANTED, 3)
            .validate(room, slaveId, DeviceRole.SLAVE, now))
        assertNotNull(message(DeviceRole.MASTER, CapabilityMessageType.REVOKE, CapabilityStatus.REJECTED, 3)
            .validate(room, masterId, DeviceRole.MASTER, now))
    }

    @Test fun rejectsUnknownCapabilitiesInvalidIdsVersionsAndStaleMessages() {
        assertNotNull(message().copy(capabilityKey = "root_shell").validate(room, masterId, DeviceRole.MASTER, now))
        assertNotNull(message().copy(messageId = "not-a-uuid").validate(room, masterId, DeviceRole.MASTER, now))
        assertNotNull(message().copy(version = 99).validate(room, masterId, DeviceRole.MASTER, now))
        assertNotNull(message().copy(createdAtEpochMs = now - 25 * 60 * 60 * 1000L)
            .validate(room, masterId, DeviceRole.MASTER, now))
    }

    @Test fun rejectsInvalidStatusRevisionAndFutureTimestamp() {
        assertNotNull(message(status = CapabilityStatus.GRANTED).validate(room, masterId, DeviceRole.MASTER, now))
        assertNotNull(message(revision = 0).validate(room, masterId, DeviceRole.MASTER, now))
        assertNotNull(message().copy(createdAtEpochMs = now + 3 * 60 * 1000L)
            .validate(room, masterId, DeviceRole.MASTER, now))
    }
}
