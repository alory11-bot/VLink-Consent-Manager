package com.vlink.consent

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

/** Direct radio-to-radio transport. No app server or relay fallback is used. */
class NearbyP2pTransport(context: Context, private val listener: Listener) {
    interface Listener {
        fun onStatus(message: String)
        fun onPeerFound(endpointId: String, endpointName: String)
        fun onPeerLost(endpointId: String)
        /** Show the digits locally; call decide(true) only after explicit user approval. */
        fun onVerificationRequired(endpointId: String, endpointName: String, authenticationDigits: String, decide: (Boolean) -> Unit)
        fun onPeerConnected(endpointId: String, endpointName: String)
        fun onPeerDisconnected(endpointId: String)
        fun onBytesReceived(endpointId: String, bytes: ByteArray)
        fun onFailure(operation: String, statusCode: Int)
    }

    private val client: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val connected = linkedSetOf<String>()
    private val pending = linkedSetOf<String>()
    private val requested = linkedSetOf<String>()
    private var maxPeers = 1
    private val endpointNames = mutableMapOf<String, String>()
    private var localName = "VLink"
    private var localRole: DeviceRole? = null
    private var running = false

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { listener.onBytesReceived(endpointId, it) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (localRole == DeviceRole.MASTER && connected.size + pending.size >= maxPeers) {
                client.rejectConnection(endpointId)
                listener.onStatus("ظرفیت اتصال دستگاه‌های Slave تکمیل است.")
                return
            }
            pending.add(endpointId)
            endpointNames[endpointId] = info.endpointName
            listener.onVerificationRequired(endpointId, info.endpointName, info.authenticationDigits) { approved ->
                if (approved) {
                    client.acceptConnection(endpointId, payloadCallback)
                        .addOnFailureListener { listener.onStatus("پذیرش اتصال ناموفق بود: ${it.localizedMessage}") }
                } else {
                    client.rejectConnection(endpointId)
                    listener.onStatus("درخواست اتصال رد شد.")
                }
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pending.remove(endpointId)
            requested.remove(endpointId)
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connected.add(endpointId)
                listener.onPeerConnected(endpointId, endpointNames[endpointId] ?: endpointId)
            } else {
                connected.remove(endpointId)
                listener.onFailure("connection", result.status.statusCode)
            }
        }

        override fun onDisconnected(endpointId: String) {
            pending.remove(endpointId)
            requested.remove(endpointId)
            connected.remove(endpointId)
            endpointNames.remove(endpointId)
            listener.onPeerDisconnected(endpointId)
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            listener.onPeerFound(endpointId, info.endpointName)
            if (localRole == DeviceRole.SLAVE && connected.isEmpty() && pending.isEmpty() && requested.isEmpty()) {
                requested.add(endpointId)
                client.requestConnection(localName, endpointId, lifecycleCallback)
                    .addOnFailureListener {
                        requested.remove(endpointId)
                        listener.onStatus("ارسال درخواست اتصال ناموفق بود: ${it.localizedMessage}")
                    }
            }
        }
        override fun onEndpointLost(endpointId: String) = listener.onPeerLost(endpointId)
    }

    /** Master advertises; Slave discovers. P2P_STAR supports one or many Slaves. */
    fun start(role: DeviceRole, deviceName: String, maxPeers: Int = 1) {
        require(deviceName.isNotBlank()) { "Device name must not be blank" }
        stop()
        localRole = role
        this.maxPeers = maxPeers.coerceAtLeast(1)
        localName = deviceName.take(32)
        running = true
        if (role == DeviceRole.MASTER) {
            val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
            client.startAdvertising(localName, SERVICE_ID, lifecycleCallback, options)
                .addOnSuccessListener { listener.onStatus("مستر آماده اتصال مستقیم است.") }
                .addOnFailureListener { running = false; listener.onFailure("advertising", (it as? ApiException)?.statusCode ?: -1) }
        } else {
            val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
            client.startDiscovery(SERVICE_ID, discoveryCallback, options)
                .addOnSuccessListener { listener.onStatus("اسلیو در حال جست‌وجوی مسترهای نزدیک است.") }
                .addOnFailureListener { running = false; listener.onFailure("discovery", (it as? ApiException)?.statusCode ?: -1) }
        }
    }

    fun sendBytes(endpointId: String, bytes: ByteArray) {
        require(endpointId in connected) { "Endpoint is not connected" }
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Payload must be between 1 byte and 1 MiB" }
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { listener.onStatus("ارسال مستقیم ناموفق بود: ${it.localizedMessage}") }
    }

    fun sendText(endpointId: String, message: String) {
        require(message.isNotBlank()) { "Message must not be blank" }
        sendBytes(endpointId, message.toByteArray(Charsets.UTF_8))
    }

    fun connectedPeers(): Set<String> = connected.toSet()
    fun disconnect(endpointId: String) { client.disconnectFromEndpoint(endpointId); connected.remove(endpointId) }

    fun stop() {
        if (running) {
            client.stopAdvertising()
            client.stopDiscovery()
            client.stopAllEndpoints()
        }
        running = false
        connected.clear()
        pending.clear()
        requested.clear()
        endpointNames.clear()
        localRole = null
    }

    companion object {
        const val SERVICE_ID = "com.vlink.consent.direct.v1"
        const val MAX_BYTES = 1024 * 1024
    }
}
