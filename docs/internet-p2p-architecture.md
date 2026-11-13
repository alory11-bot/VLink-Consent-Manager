# VLink Internet P2P Architecture

## Product decision

VLink connects devices over the public Internet. Bluetooth and nearby-device discovery are removed from the active Android app. Wi-Fi may be used only as Internet access; mobile data is equally supported.

## Current implementation

- [x] Internet permission only; radio/location/nearby permissions removed.
- [x] Nearby SDK and transport source removed from the active app.
- [x] Short-lived, five-minute Internet invite envelope (256-bit secret).
- [x] Signaling credential is SHA-256-derived; original invite secret is not sent to the signaling service.
- [x] Ephemeral WebSocket signaling service with one master and up to eight slaves.
- [x] Server forwards SDP/ICE metadata only; no application DataChannel payload is accepted.
- [x] Android WebRTC PeerConnection and DataChannel adapter.
- [x] STUN ICE server configured for direct connectivity attempts.
- [x] DataChannel HMAC-SHA256 challenge-response bound to room ID and both fresh nonces.
- [x] UI for signaling URL, Master invite creation/copy, and Slave invite entry.
- [x] TURN is disabled; direct failure is reported rather than silently relayed.
- [ ] Android CI for the latest full implementation must pass.
- [ ] Public WSS endpoint deployment and TLS verification.
- [ ] Two physical Android devices on separate Internet connections.
- [ ] Symmetric NAT / carrier-grade NAT testing.
- [x] Versioned capability request/decision/revoke envelope with session, peer identity, role, timestamp, and revision validation.\n- [x] Transport send/receive hooks restricted to authenticated WebRTC DataChannels.\n- [x] Duplicate/stale revision suppression per peer and request ID.\n- [ ] Connect protocol events to per-peer consent UI/state persistence and end-to-end device tests.
- [ ] Trusted-peer key lifecycle, revocation, and reconnect policy.
- [ ] Optional TURN fallback after direct P2P has been measured.

## Route design

1. Master creates a five-minute invite. The share string contains an eight-character room ID and a random 256-bit secret.
2. Master and Slave connect to the configured WSS signaling endpoint. The app sends only a SHA-256-derived room credential to the server.
3. Signaling exchanges WebRTC offer/answer and ICE candidates. The server holds room metadata in memory for at most five minutes.
4. WebRTC uses STUN to attempt a direct PeerConnection and DataChannel.
5. Both endpoints prove possession of the original invite secret over the encrypted DataChannel using HMAC-SHA256 over the room ID and both fresh nonces.
6. Application payloads are accepted only after the peer proof succeeds.
7. If direct connectivity fails, the app reports failure. TURN is not enabled in this milestone.

## Signaling service

The Node.js service is in `signaling-server/`. It accepts only create/join, offer, answer, candidate, and bye messages. It limits messages to 16 KiB, limits message rate, expires rooms after five minutes, and does not persist rooms. Deploy behind a TLS reverse proxy and use the resulting public `wss://` endpoint in the Android UI.

The service is not a TURN relay. It does not receive DataChannel application payloads. It is still a trusted availability and signaling component; the DataChannel HMAC proof prevents the service from authenticating as a peer without the original invite secret.

## Security boundaries

- The invite is a bearer secret. Share it only with the intended Slave and treat it like a password.
- The server sees only a one-way credential, not the original secret.
- WebRTC DTLS protects the DataChannel; the HMAC exchange binds the session to the shared invite secret.
- A successful transport handshake does not grant camera, microphone, location, notifications, or file access.
- A versioned capability-control envelope is carried only after the authenticated DataChannel handshake. The UI/state reducer must still apply owner-only decisions and persist them per peer.\n- Capability consent remains a separate gate; transport authentication alone never grants a capability.
- Do not log invite strings, room credentials, SDP, ICE candidates, or application payloads.
- Production must use WSS. Cleartext WebSocket is blocked by the Android manifest.

## Multipoint

The signaling service supports one Master and up to eight Slaves. The Android Master creates one PeerConnection per Slave. Each Slave joins only one invitation room. Product-level peer selection and independent per-peer capability consent still need completion.

## TURN fallback

TURN is deliberately disabled. When the direct route fails, the current app reports a failure. A later TURN milestone must include visible relay disclosure, short-lived credentials, quota/abuse controls, monitoring, and tests proving direct P2P is preferred.
