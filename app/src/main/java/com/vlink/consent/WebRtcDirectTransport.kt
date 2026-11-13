package com.vlink.consent

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** WebRTC direct data transport. Signaling is restricted to SDP/ICE metadata. */
class WebRtcDirectTransport(context: Context, private val listener: Listener) {
    interface Listener {
        fun onStatus(message: String)
        fun onPeerConnected(peerId: String)
        fun onPeerDisconnected(peerId: String)
        fun onBytesReceived(peerId: String, bytes: ByteArray)
        fun onFailure(operation: String, detail: String)
    }

    private data class Peer(
        val id: String, val pc: PeerConnection,
        var dc: DataChannel? = null, var localNonce: String? = null,
        var remoteNonce: String? = null, var remoteDeviceId: String? = null, var remoteProof: String? = null,
        var proofSent: Boolean = false, var authenticated: Boolean = false, var remoteDescriptionSet: Boolean = false
    )

    private val app = context.applicationContext
    private val localDeviceId = LocalDeviceIdentityStore(app).getOrCreate().id
    private val peers = ConcurrentHashMap<String, Peer>()
    private val queuedIce = ConcurrentHashMap<String, MutableList<IceCandidate>>()
    private var factory: PeerConnectionFactory? = null
    private var signaling: InternetSignalingClient? = null
    private var invite: InternetInvite? = null
    private var master = false
    private var running = false

    fun start(url: String, invite: InternetInvite, isMaster: Boolean) {
        check(!running)
        require(invite.isValidAt(System.currentTimeMillis()))
        this.invite = invite
        master = isMaster
        if (factory == null) {
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(app).createInitializationOptions())
            factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        }
        running = true
        signaling = InternetSignalingClient(object : InternetSignalingClient.Listener {
            override fun onConnected(peerId: String, role: String) {
                listener.onStatus("کانال مذاکره اینترنتی برقرار شد.")
            }
            override fun onPeerJoined(peerId: String, role: String) {
                if (master && role == "slave") {
                    if (peers.size >= 8) return listener.onFailure("peer_limit", "حداکثر ۸ اسلیو مجاز است.")
                    runCatching { createPeer(peerId, true); createOffer(peerId) }
                        .onFailure { listener.onFailure("offer", it.message ?: "offer failed") }
                }
            }
            override fun onPeerLeft(peerId: String) = closePeer(peerId)
            override fun onSignal(type: String, from: String, payload: JSONObject) {
                when (type) {
                    "offer" -> acceptOffer(from, payload.optString("sdp"))
                    "answer" -> acceptAnswer(from, payload.optString("sdp"))
                    "candidate" -> receiveCandidate(from, payload)
                }
            }
            override fun onError(code: String, detail: String) = listener.onFailure("signaling_$code", detail)
            override fun onClosed() { if (running) listener.onFailure("signaling_closed", "کانال مذاکره قطع شد.") }
        })
        signaling!!.connect(url, invite, isMaster)
    }

    private fun createPeer(id: String, createChannel: Boolean): Peer {
        peers[id]?.let { return it }
        val ice = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val pc = requireNotNull(factory).createPeerConnection(PeerConnection.RTCConfiguration(ice),
            object : PeerConnection.Observer {
                override fun onSignalingChange(s: PeerConnection.SignalingState) = Unit
                override fun onIceConnectionChange(s: PeerConnection.IceConnectionState) {
                    if (s == PeerConnection.IceConnectionState.FAILED || s == PeerConnection.IceConnectionState.CLOSED) closePeer(id)
                }
                override fun onIceConnectionReceivingChange(v: Boolean) = Unit
                override fun onIceGatheringChange(s: PeerConnection.IceGatheringState) = Unit
                override fun onIceCandidate(c: IceCandidate) {
                    signaling?.sendSignal("candidate", id, JSONObject().put("sdpMid", c.sdpMid).put("sdpMLineIndex", c.sdpMLineIndex).put("sdp", c.sdp))
                }
                override fun onIceCandidatesRemoved(c: Array<out IceCandidate>) = Unit
                override fun onAddStream(s: MediaStream) = Unit
                override fun onRemoveStream(s: MediaStream) = Unit
                override fun onDataChannel(c: DataChannel) = attachChannel(id, c)
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(r: RtpReceiver, s: Array<out MediaStream>) = Unit
            }) ?: error("PeerConnection creation failed")
        val peer = Peer(id, pc)
        peers[id] = peer
        if (createChannel) attachChannel(id, pc.createDataChannel("vlink-data", DataChannel.Init()) ?: error("DataChannel creation failed"))
        return peer
    }

    private fun attachChannel(id: String, channel: DataChannel) {
        val p = peers[id] ?: return
        p.dc = channel
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(v: Long) = Unit
            override fun onStateChange() { if (channel.state() == DataChannel.State.OPEN) sendHello(p) }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining()); buffer.data.get(bytes)
                if (buffer.binary) {
                    if (p.authenticated) listener.onBytesReceived(id, bytes)
                } else handleControl(p, bytes.toString(Charsets.UTF_8))
            }
        })
        if (channel.state() == DataChannel.State.OPEN) sendHello(p)
    }

    private fun sendHello(p: Peer) {
        if (p.localNonce != null) return
        p.localNonce = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        control(p, JSONObject().put("kind", "hello").put("nonce", p.localNonce).put("deviceId", localDeviceId))
    }

    private fun handleControl(p: Peer, raw: String) {
        val msg = runCatching { JSONObject(raw) }.getOrNull() ?: return failPeer(p.id, "invalid control")
        when (msg.optString("kind")) {
            "hello" -> {
                val n = msg.optString("nonce")
                val decoded = runCatching { Base64.decode(n, Base64.URL_SAFE or Base64.NO_WRAP) }.getOrNull()
                if (decoded == null || decoded.size != 32) return failPeer(p.id, "invalid nonce")
                val deviceId = msg.optString("deviceId")
                val validDeviceId = runCatching { UUID.fromString(deviceId).toString() == deviceId }.getOrDefault(false)
                if (!validDeviceId) return failPeer(p.id, "invalid device identity")
                p.remoteNonce = n
                p.remoteDeviceId = deviceId
                sendProof(p)
            }
            "proof" -> { p.remoteProof = msg.optString("value"); verifyProof(p) }
            else -> if (!p.authenticated) failPeer(p.id, "unexpected control")
        }
    }

    private fun proof(p: Peer): String {
        val local = requireNotNull(p.localNonce)
        val remote = requireNotNull(p.remoteNonce)
        val masterNonce = if (master) local else remote
        val slaveNonce = if (master) remote else local
        return PeerInviteAuthenticator.createProof(
            requireNotNull(invite).token,
            requireNotNull(invite).room,
            masterNonce,
            slaveNonce,
            if (master) localDeviceId else requireNotNull(p.remoteDeviceId),
            if (master) requireNotNull(p.remoteDeviceId) else localDeviceId
        )
    }

    private fun sendProof(p: Peer) {
        if (p.proofSent || p.localNonce == null || p.remoteNonce == null) return
        p.proofSent = true
        control(p, JSONObject().put("kind", "proof").put("value", proof(p)))
        verifyProof(p)
    }

    private fun verifyProof(p: Peer) {
        val received = p.remoteProof ?: return
        if (p.localNonce == null || p.remoteNonce == null || p.remoteDeviceId == null) return
        val local = requireNotNull(p.localNonce)
        val remote = requireNotNull(p.remoteNonce)
        val masterNonce = if (master) local else remote
        val slaveNonce = if (master) remote else local
        val masterDevice = if (master) localDeviceId else requireNotNull(p.remoteDeviceId)
        val slaveDevice = if (master) requireNotNull(p.remoteDeviceId) else localDeviceId
        val currentInvite = requireNotNull(invite)
        if (!PeerInviteAuthenticator.verify(currentInvite.token, currentInvite.room, masterNonce, slaveNonce, masterDevice, slaveDevice, received)) return failPeer(p.id, "peer authentication failed")
        if (!p.authenticated) { p.authenticated = true; listener.onPeerConnected(p.id) }
    }

    private fun control(p: Peer, msg: JSONObject) {
        val dc = p.dc ?: return
        if (dc.state() == DataChannel.State.OPEN) dc.send(DataChannel.Buffer(ByteBuffer.wrap(msg.toString().toByteArray()), false))
    }

    private fun createOffer(id: String) {
        val p = peers[id] ?: return
        p.pc.createOffer(observer({ desc ->
            p.pc.setLocalDescription(observer({
                signaling?.sendSignal("offer", id, JSONObject().put("sdp", desc.description))
            }, { failPeer(id, it) }), desc)
        }, { failPeer(id, it) }), MediaConstraints())
    }

    private fun acceptOffer(id: String, sdp: String) {
        val p = runCatching { createPeer(id, false) }.getOrElse { listener.onFailure("peer", it.message ?: "peer creation failed"); return }
        p.pc.setRemoteDescription(observer({
            p.remoteDescriptionSet = true
            flushIce(id)
            p.pc.createAnswer(observer({ answer ->
                p.pc.setLocalDescription(observer({
                    signaling?.sendSignal("answer", id, JSONObject().put("sdp", answer.description))
                }, { failPeer(id, it) }), answer)
            }, { failPeer(id, it) }), MediaConstraints())
        }, { failPeer(id, it) }), SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    private fun acceptAnswer(id: String, sdp: String) {
        val p = peers[id] ?: return
        p.pc.setRemoteDescription(observer({ p.remoteDescriptionSet = true; flushIce(id) }, { failPeer(id, it) }), SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    private fun receiveCandidate(id: String, payload: JSONObject) {
        val mid = payload.optString("sdpMid").takeIf { it.isNotBlank() } ?: return
        val c = IceCandidate(mid, payload.optInt("sdpMLineIndex"), payload.optString("sdp"))
        val p = peers[id]
        if (p == null || !p.remoteDescriptionSet) queuedIce.getOrPut(id) { mutableListOf() }.add(c) else p.pc.addIceCandidate(c)
    }

    private fun flushIce(id: String) {
        val p = peers[id] ?: return
        if (!p.remoteDescriptionSet) return
        queuedIce.remove(id)?.forEach(p.pc::addIceCandidate)
    }

    private fun observer(created: (SessionDescription) -> Unit, failed: (String) -> Unit) = object : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription) = created(d)
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(e: String) = failed(e)
        override fun onSetFailure(e: String) = failed(e)
    }

    fun sendBytes(id: String, bytes: ByteArray) {
        require(bytes.isNotEmpty() && bytes.size <= 1024 * 1024)
        val p = peers[id] ?: error("Unknown peer")
        check(p.authenticated) { "Peer authentication incomplete" }
        val dc = p.dc ?: error("DataChannel unavailable")
        check(dc.state() == DataChannel.State.OPEN && dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), true)))
    }

    fun connectedPeers(): Set<String> = peers.values.filter { it.authenticated }.map { it.id }.toSet()
    private fun failPeer(id: String, reason: String) { listener.onFailure("peer", reason); closePeer(id) }
    private fun closePeer(id: String) {
        val p = peers.remove(id) ?: return
        p.dc?.unregisterObserver(); p.dc?.close(); p.pc.close(); p.pc.dispose()
        listener.onPeerDisconnected(id)
    }

    fun stop() {
        running = false
        signaling?.disconnect(); signaling = null
        peers.keys.toList().forEach(::closePeer)
        queuedIce.clear()
        factory?.dispose(); factory = null
        invite = null
    }
}
