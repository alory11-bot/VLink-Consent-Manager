package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class PeerInviteAuthenticatorTest {
    @Test fun bothRolesDeriveSameProofFromCanonicalNonceOrder() {
        val invite = InternetInvite.create()
        val masterNonce = "master-fresh-nonce"
        val slaveNonce = "slave-fresh-nonce"
        val masterProof = PeerInviteAuthenticator.createProof(invite.token, invite.room, masterNonce, slaveNonce)
        val slaveProof = PeerInviteAuthenticator.createProof(invite.token, invite.room, masterNonce, slaveNonce)
        assertEquals(masterProof, slaveProof)
        assertTrue(PeerInviteAuthenticator.verify(invite.token, invite.room, masterNonce, slaveNonce, masterProof))
    }

    @Test fun rejectsWrongSecretRoomOrProof() {
        val a = InternetInvite.create()
        val b = InternetInvite.create()
        val proof = PeerInviteAuthenticator.createProof(a.token, a.room, "m", "s")
        assertFalse(PeerInviteAuthenticator.verify(b.token, a.room, "m", "s", proof))
        assertFalse(PeerInviteAuthenticator.verify(a.token, b.room, "m", "s", proof))
        assertFalse(PeerInviteAuthenticator.verify(a.token, a.room, "m", "changed", proof))
    }
}
