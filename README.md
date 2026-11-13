# VLink Consent Manager

VLink is a Persian RTL Android prototype for consent-first device connections over the public Internet.

## Current Internet transport milestone

- Internet-only transport: Bluetooth, Nearby Connections, and nearby-device permissions are removed from the active Android app.
- WebRTC PeerConnection and DataChannel are used for direct P2P application traffic.
- STUN/ICE attempts direct connectivity. TURN is deliberately not enabled yet.
- A five-minute Master invite carries a random 256-bit secret. The original secret is not sent to the signaling service; only a SHA-256-derived room credential is sent.
- The WebSocket signaling service exchanges SDP/ICE metadata only. It does not accept or relay application payloads.
- Both peers exchange fresh nonces and persistent per-install device IDs, then prove possession of the invite secret using HMAC-SHA256 over the DataChannel before application bytes are accepted.
- The Android UI lets the Master create/copy an invite and the Slave paste it, using a configured secure WSS signaling URL.
- The signaling service is in signaling-server/, includes a Dockerfile, and has automated Node.js tests.

## Important limitations

This is an **in-progress engineering prototype**, not a production-ready or fully verified cross-device product.

- A public TLS/WSS signaling endpoint must be deployed and configured in both phones. The repository contains the service source and container definition; it has not been deployed to a public host.
- TURN is not active. Some NAT/firewall combinations, especially symmetric NAT and carrier-grade NAT, may prevent direct P2P from connecting.
- No real two-physical-device test across separate Internet connections has been completed.
- The transport supports one Master with up to eight Slave PeerConnections, but per-peer consent state and a complete capability message protocol are not yet wired to the UI.
- A versioned capability-control message envelope and authenticated DataChannel send/receive hooks are implemented, including sender-role, device identity, session, timestamp, and revision validation. The per-peer UI/state reducer and end-to-end consent synchronization are still incomplete. Capability cards still represent local consent state only. Camera, microphone, location, notifications, and file transfer are not activated.
- Persistent device IDs are exchanged and bound to the invite HMAC proof; long-term trusted-peer key storage, revocation, and silent reconnect are not implemented.
- No foreground/background service or guaranteed background connectivity is implemented.

## Signaling service

See signaling-server/README.md.

For production, deploy the Node service behind a TLS-terminating reverse proxy and configure the app with the resulting WSS endpoint. The service keeps rooms in memory for up to five minutes, permits one Master and up to eight Slaves, and forwards only offer, answer, ICE candidate, and leave messages. It is a signaling component, not a TURN relay.

## Android build

- Minimum Android: API 26 (Android 8.0)
- Compile/target SDK: 35
- JDK: 17
- WebRTC Android SDK: pinned dependency in app/build.gradle.kts

The GitHub Actions workflow runs Android unit tests, builds the debug APK, packages the Android Studio project, and separately runs signaling-service tests.

## Build from terminal

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is written to app/build/outputs/apk/debug/app-debug.apk.

## Security boundary

- Production signaling must use WSS; cleartext traffic is disabled in the Android manifest.
- Treat the invite string as a bearer secret and share it only with the intended Slave.
- Never log invite strings, credentials, SDP, ICE candidates, or application payloads.
- The signaling service is not trusted to authenticate a peer; the DataChannel HMAC proof is the peer possession check.
- Transport authentication does not grant any device capability. Each sensitive capability requires a separate, explicit consent workflow.
- Do not deploy as a production security product until CI passes on the final commit, a public WSS endpoint is configured, two-device/network testing passes, capability consent is synchronized, and an independent security review is complete.

See docs/internet-p2p-architecture.md for the detailed route design and remaining milestones.
