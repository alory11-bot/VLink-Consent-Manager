package com.vlink.consent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InternetTransportPolicyTest {
    @Test
    fun idleStartsSignalingBeforeIceGathering() {
        assertEquals(
            InternetTransportPolicy.NextAction.START_SIGNALING,
            InternetTransportPolicy.nextAction(InternetTransportPolicy.ConnectionState.IDLE)
        )
        assertEquals(
            InternetTransportPolicy.NextAction.GATHER_ICE,
            InternetTransportPolicy.nextAction(InternetTransportPolicy.ConnectionState.SIGNALING)
        )
    }

    @Test
    fun directPathIsAttemptedBeforeAnyFallback() {
        assertEquals(
            InternetTransportPolicy.NextAction.TRY_DIRECT_P2P,
            InternetTransportPolicy.nextAction(InternetTransportPolicy.ConnectionState.GATHERING_ICE)
        )
        assertEquals(
            InternetTransportPolicy.NextAction.REPORT_FAILURE,
            InternetTransportPolicy.nextAction(InternetTransportPolicy.ConnectionState.DIRECT_FAILED)
        )
        assertFalse(InternetTransportPolicy.TURN_FALLBACK_ENABLED)
    }

    @Test
    fun applicationDataIsAllowedOnlyOnAnEstablishedRoute() {
        assertTrue(
            InternetTransportPolicy.isApplicationDataRouteAllowed(
                InternetTransportPolicy.Route.DIRECT_P2P,
                InternetTransportPolicy.ConnectionState.CONNECTED_DIRECT
            )
        )
        assertFalse(
            InternetTransportPolicy.isApplicationDataRouteAllowed(
                InternetTransportPolicy.Route.DIRECT_P2P,
                InternetTransportPolicy.ConnectionState.CONNECTING_DIRECT
            )
        )
        assertFalse(
            InternetTransportPolicy.isApplicationDataRouteAllowed(
                InternetTransportPolicy.Route.TURN_RELAY,
                InternetTransportPolicy.ConnectionState.CONNECTED_TURN
            )
        )
    }

    @Test
    fun closedSessionDoesNotStartMoreWork() {
        assertEquals(
            InternetTransportPolicy.NextAction.NONE,
            InternetTransportPolicy.nextAction(InternetTransportPolicy.ConnectionState.CLOSED)
        )
    }
}
