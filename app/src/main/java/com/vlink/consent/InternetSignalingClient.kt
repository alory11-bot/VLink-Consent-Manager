package com.vlink.consent

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Exchanges SDP and ICE metadata only. Never send application messages or files here.
 * The endpoint must be wss:// in production; ws:// is permitted only for local development.
 */
class InternetSignalingClient(
    private val listener: Listener,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
) {
    interface Listener {
        fun onConnected(peerId: String, role: String)
        fun onPeerJoined(peerId: String, role: String)
        fun onPeerLeft(peerId: String)
        fun onSignal(type: String, from: String, payload: JSONObject)
        fun onError(code: String, detail: String)
        fun onClosed()
    }

    private var socket: WebSocket? = null
    @Volatile private var localPeerId: String? = null

    @Synchronized
    fun connect(endpoint: String, invite: InternetInvite, isMaster: Boolean) {
        check(socket == null) { "Signaling session already started" }
        require(endpoint.startsWith("wss://")) { "Production signaling must use secure wss://" }
        require(invite.isValidAt(System.currentTimeMillis())) { "Invite expired or invalid" }
        val request = Request.Builder().url(endpoint).build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val type = if (isMaster) "create" else "join"
                val accepted = webSocket.send(JSONObject()
                    .put("type", type)
                    .put("room", invite.room)
                    .put("token", invite.signalingCredential)
                    .toString())
                if (!accepted) listener.onError("SEND_FAILED", "Could not send room authentication")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { JSONObject(text) }.getOrNull()
                if (msg == null) {
                    listener.onError("INVALID_MESSAGE", "Invalid signaling response")
                    return
                }
                when (msg.optString("type")) {
                    "joined" -> {
                        localPeerId = msg.optString("peerId")
                        listener.onConnected(localPeerId.orEmpty(), msg.optString("role"))
                    }
                    "peer-joined" -> listener.onPeerJoined(msg.optString("peerId"), msg.optString("role"))
                    "peer-left" -> listener.onPeerLeft(msg.optString("peerId"))
                    "offer", "answer", "candidate" ->
                        listener.onSignal(msg.optString("type"), msg.optString("from"), msg.optJSONObject("payload") ?: JSONObject())
                    "error" -> listener.onError(msg.optString("code", "SIGNALING_ERROR"), "Signaling server rejected the request")
                    else -> listener.onError("UNKNOWN_MESSAGE", "Unsupported signaling message")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                listener.onError("NETWORK_FAILURE", t.message ?: "Signaling connection failed")
                listener.onClosed()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                listener.onClosed()
            }
        })
    }

    fun sendSignal(type: String, targetPeerId: String, payload: JSONObject): Boolean {
        require(type in setOf("offer", "answer", "candidate"))
        require(targetPeerId.isNotBlank())
        require(payload.length() > 0)
        return socket?.send(JSONObject()
            .put("type", type)
            .put("to", targetPeerId)
            .put("payload", payload)
            .toString()) == true
    }

    fun disconnect() {
        socket?.send(JSONObject().put("type", "bye").toString())
        socket?.close(1000, "user disconnected")
        socket = null
        localPeerId = null
    }
}
