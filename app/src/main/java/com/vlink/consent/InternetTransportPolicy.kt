package com.vlink.consent

/**
 * Internet transport policy for VLink.
 *
 * Direct WebRTC connectivity is always attempted first. TURN is a later,
 * explicit fallback and is not silently enabled by this policy.
 */
object InternetTransportPolicy {
    enum class Route {
        DIRECT_P2P,
        TURN_RELAY
    }

    enum class ConnectionState {
        IDLE,
        SIGNALING,
        GATHERING_ICE,
        CONNECTING_DIRECT,
        CONNECTED_DIRECT,
        DIRECT_FAILED,
        CONNECTING_TURN,
        CONNECTED_TURN,
        FAILED,
        CLOSED
    }

    enum class NextAction {
        START_SIGNALING,
        GATHER_ICE,
        TRY_DIRECT_P2P,
        REPORT_DIRECT_CONNECTED,
        OFFER_TURN_FALLBACK,
        START_TURN,
        REPORT_TURN_CONNECTED,
        REPORT_FAILURE,
        NONE
    }

    /** TURN is intentionally disabled until the direct path is implemented and verified. */
    const val TURN_FALLBACK_ENABLED = false

    fun nextAction(state: ConnectionState): NextAction = when (state) {
        ConnectionState.IDLE -> NextAction.START_SIGNALING
        ConnectionState.SIGNALING -> NextAction.GATHER_ICE
        ConnectionState.GATHERING_ICE -> NextAction.TRY_DIRECT_P2P
        ConnectionState.CONNECTING_DIRECT -> NextAction.NONE
        ConnectionState.CONNECTED_DIRECT -> NextAction.REPORT_DIRECT_CONNECTED
        ConnectionState.DIRECT_FAILED -> if (TURN_FALLBACK_ENABLED) {
            NextAction.OFFER_TURN_FALLBACK
        } else {
            NextAction.REPORT_FAILURE
        }
        ConnectionState.CONNECTING_TURN -> NextAction.NONE
        ConnectionState.CONNECTED_TURN -> NextAction.REPORT_TURN_CONNECTED
        ConnectionState.FAILED -> NextAction.REPORT_FAILURE
        ConnectionState.CLOSED -> NextAction.NONE
    }

    fun isApplicationDataRouteAllowed(route: Route, state: ConnectionState): Boolean =
        when (route) {
            Route.DIRECT_P2P -> state == ConnectionState.CONNECTED_DIRECT
            Route.TURN_RELAY -> TURN_FALLBACK_ENABLED && state == ConnectionState.CONNECTED_TURN
        }
}
