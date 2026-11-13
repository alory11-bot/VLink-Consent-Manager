package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class PeerInviteAuthenticatorTest {
    private val masterId = "11111111-1111-4111-8111-111111111111"
    private val slaveId = "22222222-2222-4222-8222-222222222222"

    @Test fun bothRolesDeriveSameProofFromCanonicalNonceAndIdentityOrder() {
        val invite = InternetInvite.create()
        val proof = PeerInviteAuthenticator.createProof(invite.token, invite.room, "master-nonce", "slave-nonce", masterId, slaveId)
        assertTrue(PeerInviteAuthenticator.verify(invite.token, invite.room, "master-nonce", "slave-nonce", masterId, slaveId, proof))
    }

    @Test fun rejectsWrongSecretRoomNonceOrIdentity() {
        val a = InternetInvite.create()
        val b = InternetInvite.create()
        val proof = PeerInviteAuthenticator.createProof(a.token, a.room, "m", "s", masterId, slaveId)
        assertFalse(PeerInviteAuthenticator.verify(b.token, a.room, "m", "s", masterId, slaveId, proof))
        assertFalse(PeerInviteAuthenticator.verify(a.token, b.room, "m", "s", masterId, slaveId, proof))
        assertFalse(PeerInviteAuthenticator.verify(a.token, a.room, "m", "changed", masterId, slaveId, proof))
        assertFalse(PeerInviteAuthenticator.verify(a.token, a.room, "m", "s", slaveId, masterId, proof))
    }
}
