# VLink Internet P2P Architecture

## Product decision

VLink connects devices over the public Internet. Bluetooth and nearby-device discovery are not part of the intended connection path. Wi-Fi may be used only as an Internet access network; mobile data is equally supported.

## Route policy

1. Establish a signaling session to exchange authenticated session metadata and WebRTC offer/answer/ICE candidates.
2. Use ICE with STUN to discover viable network paths.
3. Attempt a direct WebRTC PeerConnection and DataChannel between the two devices.
4. Do not send application payloads through the signaling service.
5. If direct connectivity cannot be established, report that the direct attempt failed. TURN is a separately approved later milestone; it is not enabled in this phase.
6. When TURN is eventually added, it must be an explicit fallback route and the UI must disclose that payloads are traversing a relay.

A signaling service is still required for automatic Internet connection setup unless a separate out-of-band signaling mechanism (for example, manual QR/SDP exchange) is deliberately implemented. Signaling is not a media/data relay: it exchanges connection negotiation messages only.

## Security boundaries

- WebRTC DTLS-SRTP / DTLS protects the transport, but it does not by itself establish that the remote peer is the intended VLink account/device.
- Bind the WebRTC session to VLink's authenticated device identity and a short-lived, single-use pairing challenge.
- Require explicit approval on both endpoints before accepting a peer session.
- Authenticate signaling requests; expire pairing sessions; rate-limit code attempts; reject replayed or stale offers/candidates.
- Do not treat a device display name, room ID, or signaling token as peer identity.
- Keep capability consent independent from transport connectivity. A connected peer receives no camera, microphone, location, notification, or file access until the relevant capability request is explicitly approved by the Slave.
- Do not implement silent reconnect or capability persistence until the authenticated peer-trust design is complete.

## Topology

- One-to-one: one Master and one Slave.
- One-to-many: one Master may establish separate PeerConnections/DataChannels to multiple Slaves.
- Each Slave may have only one active Master session at a time unless a future product decision changes this policy.

## TURN policy

TURN is deliberately disabled in this milestone. Direct P2P is attempted first. If it fails, VLink reports failure rather than silently relaying traffic. A later TURN milestone must include visible route disclosure, secure TURN credentials with short TTL, quota/abuse controls, and tests proving that the direct route is preferred.

## Android implementation notes

- WebRTC Android SDK: `io.github.webrtc-sdk:android`, pinned to a fixed version (no dynamic versions).
- Required network permission: `android.permission.INTERNET`.
- No Bluetooth, location, or nearby-device permission is required by the Internet transport.
- The previous Nearby transport is legacy code until the UI and session lifecycle have been migrated and verified; it must not be presented as the active Internet transport.
- Do not remove the old dependency or permissions until all references are migrated and CI confirms the app builds.

## Milestones

### I1 — Foundation
- [x] Record Internet-first architecture and security boundaries.
- [x] Add an explicit route state policy with unit tests.
- [ ] Add and resolve the pinned WebRTC Android SDK dependency in CI.
- [ ] Add authenticated signaling protocol and backend contract.
- [ ] Add WebRTC PeerConnection/DataChannel adapter.

### I2 — App integration
- [ ] Replace Nearby discovery/advertising UI with Internet pairing flow.
- [ ] Add short-lived pairing invite and mutual peer verification.
- [ ] Bind the session to persistent device identity and capability-consent gates.
- [ ] Remove Nearby SDK and radio permissions after all code references are gone.

### I3 — Verification
- [ ] Unit tests for SDP/ICE state, timeouts, replay, duplicate messages, and disconnect.
- [ ] CI debug APK and Android Studio ZIP.
- [ ] Two-device tests across separate Internet connections (mobile data vs Wi-Fi), including symmetric NAT where available.
- [ ] Confirm that no application payload is sent over signaling.
- [ ] Verify one-to-one and one-to-many sessions.

### I4 — Optional TURN fallback
- [ ] Only after direct P2P is implemented and measured.
- [ ] Visible user disclosure and explicit route status.
- [ ] Short-lived credentials, quotas, abuse protection, and operational monitoring.
