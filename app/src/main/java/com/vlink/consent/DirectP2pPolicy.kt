package com.vlink.consent

/** Explicit topology policy for direct radio-to-radio sessions; no relay fallback exists. */
object DirectP2pPolicy {
    enum class Action { ADVERTISE, DISCOVER }
    const val SERVICE_ID = "com.vlink.consent.direct.v1"
    const val RELAY_FALLBACK_ENABLED = false
    fun actionFor(role: DeviceRole): Action =
        if (role == DeviceRole.MASTER) Action.ADVERTISE else Action.DISCOVER
    fun peerLimit(multiPoint: Boolean): Int = if (multiPoint) Int.MAX_VALUE else 1
}
