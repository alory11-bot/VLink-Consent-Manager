package com.vlink.consent

import org.json.JSONObject
import java.util.UUID

enum class CapabilityMessageType { REQUEST, DECISION, REVOKE }

/**
 * Versioned, peer-authenticated capability-control envelope.
 *
 * This message transports consent decisions; it does not itself activate Android
 * permissions or sensitive device APIs.
 */
data class CapabilityWireMessage(
    val messageId: String,
    val requestId: String,
    val sessionId: String,
    val senderDeviceId: String,
    val senderRole: DeviceRole,
    val capabilityKey: String,
    val type: CapabilityMessageType,
    val status: CapabilityStatus,
    val revision: Long,
    val createdAtEpochMs: Long,
    val version: Int = CURRENT_VERSION
) {
    fun validate(
        expectedSessionId: String,
        expectedSenderDeviceId: String,
        expectedSenderRole: DeviceRole,
        nowEpochMs: Long = System.currentTimeMillis()
    ): String? {
        if (version != CURRENT_VERSION) return "unsupported message version"
        if (!isUuid(messageId) || !isUuid(requestId) || !isUuid(senderDeviceId)) return "invalid message identity"
        if (sessionId != expectedSessionId || !sessionId.matches(Regex("^[A-Z2-9]{8}$"))) return "wrong session"
        if (senderDeviceId != expectedSenderDeviceId) return "sender identity mismatch"
        if (senderRole != expectedSenderRole) return "sender role mismatch"
        if (Capability.entries.none { it.key == capabilityKey }) return "unknown capability"
        if (revision < 1L) return "invalid revision"
        if (createdAtEpochMs <= 0L || createdAtEpochMs > nowEpochMs + MAX_CLOCK_SKEW_MS ||
            createdAtEpochMs < nowEpochMs - MAX_MESSAGE_AGE_MS) return "message timestamp outside accepted window"
        val validTransition = when (type) {
            CapabilityMessageType.REQUEST -> senderRole == DeviceRole.MASTER && status == CapabilityStatus.REQUESTED && revision == 1L
            CapabilityMessageType.DECISION -> senderRole == DeviceRole.SLAVE &&
                (status == CapabilityStatus.GRANTED || status == CapabilityStatus.REJECTED) && revision == 2L
            CapabilityMessageType.REVOKE -> senderRole == DeviceRole.SLAVE && status == CapabilityStatus.REJECTED && revision == 3L
        }
        if (!validTransition) return "invalid role/status transition"
        return null
    }

    fun toJson(): JSONObject = JSONObject()
        .put("kind", "capability")
        .put("version", version)
        .put("messageId", messageId)
        .put("requestId", requestId)
        .put("sessionId", sessionId)
        .put("senderDeviceId", senderDeviceId)
        .put("senderRole", senderRole.name)
        .put("capabilityKey", capabilityKey)
        .put("type", type.name)
        .put("status", status.name)
        .put("revision", revision)
        .put("createdAtEpochMs", createdAtEpochMs)

    companion object {
        const val CURRENT_VERSION = 1
        private const val MAX_CLOCK_SKEW_MS = 2 * 60 * 1000L
        private const val MAX_MESSAGE_AGE_MS = 24 * 60 * 60 * 1000L

        fun create(
            requestId: String,
            sessionId: String,
            senderDeviceId: String,
            senderRole: DeviceRole,
            capability: Capability,
            type: CapabilityMessageType,
            status: CapabilityStatus,
            revision: Long
        ): CapabilityWireMessage = CapabilityWireMessage(
            messageId = UUID.randomUUID().toString(),
            requestId = requestId,
            sessionId = sessionId,
            senderDeviceId = senderDeviceId,
            senderRole = senderRole,
            capabilityKey = capability.key,
            type = type,
            status = status,
            revision = revision,
            createdAtEpochMs = System.currentTimeMillis()
        )

        fun fromJson(json: JSONObject): CapabilityWireMessage {
            require(json.optString("kind") == "capability") { "not a capability message" }
            return CapabilityWireMessage(
                messageId = json.getString("messageId"),
                requestId = json.getString("requestId"),
                sessionId = json.getString("sessionId"),
                senderDeviceId = json.getString("senderDeviceId"),
                senderRole = DeviceRole.valueOf(json.getString("senderRole")),
                capabilityKey = json.getString("capabilityKey"),
                type = CapabilityMessageType.valueOf(json.getString("type")),
                status = CapabilityStatus.valueOf(json.getString("status")),
                revision = json.getLong("revision"),
                createdAtEpochMs = json.getLong("createdAtEpochMs"),
                version = json.getInt("version")
            )
        }

        private fun isUuid(value: String): Boolean =
            runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
    }
}
