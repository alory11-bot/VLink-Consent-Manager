# Slave Background Lifecycle and One-Time Setup

## Product behavior

The Slave is configured once. After setup is complete, the app UI closes and the user is not expected to reopen it for ordinary operation. A user-visible Android foreground service owns the ongoing connection lifecycle and receives authenticated control messages from the paired Master.

Closing the Activity must not stop the service. The service must not be hidden: Android's persistent foreground-service notification identifies VLink as active and provides a Stop action. The user can also stop it from Android system settings and revoke Android permissions at any time.

## One-time setup

1. The user explicitly chooses the Slave role.
2. The user configures the signaling endpoint and pairs with the intended Master.
3. The user reviews and enables only the capabilities that VLink actually supports. Each Android runtime permission is requested through the Android permission flow; one generic approval must never be treated as granting unrelated OS permissions.
4. VLink persists the trusted peer identity, the capability allowlist, and the user's setup choice using Android Keystore-backed storage.
5. VLink explains the foreground notification and ongoing background behavior before the user starts the service.
6. The app starts the foreground service. Once Android confirms service startup, the setup Activity finishes.

## Runtime behavior

- The foreground service, not an Activity or Compose effect, owns the WebRTC transport, signaling connection, reconnect policy, and trusted-peer state.
- The service accepts messages only from a previously paired and currently trusted Master identity.
- Requests outside the locally approved capability allowlist are rejected by default.
- The service writes an append-only local audit event for pairing, connection, accepted/rejected operations, permission changes, revocation, and stop.
- The service exposes an explicit stop action through its persistent notification.
- Loss of a required Android permission, trusted-peer revocation, expired credentials, or failed authentication closes the affected capability/session safely.
- The Activity is optional for status/settings and is not required for normal service operation.

## Android platform constraints

- Use a declared Android foreground-service type and its matching permission only when the implemented feature genuinely requires that type. Do not add broad permissions speculatively.
- Follow the target SDK's foreground-service start restrictions, runtime permission requirements, notification permission behavior, and background execution limits.
- Android privacy indicators and OS permission controls remain authoritative. VLink must not attempt to hide, suppress, or bypass them.
- Camera and microphone use must remain transparent to the device owner and comply with Android background-use restrictions. A generic first-run consent is not a blanket authorization for invisible or indefinite capture.
- Location and other sensitive capabilities must be individually scoped, revocable, and used only while their Android permission and platform conditions are satisfied.
- Force-stop, reboot, battery optimization, OEM process management, and network changes can interrupt service operation. Reconnection is best-effort and must never be represented as guaranteed.

## Security model

- Pairing creates a durable trust relationship bound to cryptographic device identity; a short-lived invitation is only for initial pairing and must not be reused as a permanent credential.
- Reconnect uses fresh session nonces and authenticated key material. It must not silently trust a peer merely because it knows a room ID or old invitation.
- Every incoming operation is checked against peer identity, role, protocol version, replay/revision state, and the local capability allowlist.
- Revocation removes the trusted relationship and invalidates session credentials.
- Secrets, tokens, SDP/ICE material, and sensitive payloads must not be written to logs.

## Implementation status

This document records the confirmed product requirement and the target architecture. It is not evidence that a foreground service, durable trust store, automatic reconnect, or real-device background operation has been implemented or tested.

The current implementation is Activity-owned: `DirectConnectionCard` constructs `WebRtcDirectTransport` inside the Activity composition and stops it from `DisposableEffect`. The current five-minute invitation is not a durable reconnect credential. These are blockers that must be addressed before claiming Slave background operation is complete.

## Acceptance criteria

- Closing the Slave Activity leaves the foreground service and authenticated transport running.
- A persistent notification is visible and its Stop action terminates the service and transport.
- Reboot/force-stop behavior is documented and tested; no guarantee is made where Android does not permit automatic restart.
- Only the paired Master can authenticate; a new or revoked peer is rejected.
- Requests not in the locally approved allowlist are rejected without opening a Slave request UI.
- Android permission revocation and service stop immediately prevent further use of the affected capability.
- Unit tests cover trust persistence, allowlist enforcement, replay protection, service lifecycle transitions, and stop/revoke behavior.
- Instrumented tests and two-device tests verify background survival under supported Android conditions.
- Sensitive camera/microphone/location behavior is not considered complete without a separate, transparent, OS-compliant design and validation.
