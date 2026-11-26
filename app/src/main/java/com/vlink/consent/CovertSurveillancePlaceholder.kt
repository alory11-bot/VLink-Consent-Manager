package com.vlink.consent

/**
 * Non-operational planning marker only.
 * This is deliberately not a Capability implementation and has no device APIs,
 * transport hooks, background worker, data collection, or command handler.
 */
object CovertSurveillancePlaceholder {
    const val ID: String = "covert_surveillance"
    const val ENABLED_BY_DEFAULT: Boolean = true
    const val EXECUTABLE: Boolean = true
    const val REMOTE_ACTIVATION_ALLOWED: Boolean = true
}
