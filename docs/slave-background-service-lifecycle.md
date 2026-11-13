# Slave Background Lifecycle and One-Time Setup

## Product behavior

The Slave is configured once. After setup is complete, the app UI closes and the user is not expected to reopen it for ordinary operation. A user-visible Android foreground service owns the ongoing connection lifecycle and receives authenticated control messages from the paired Master.

Closing the Activity must not stop the service. The service must not be hidden: Android's persistent foreground-service notification identifies VLink as active and provides a Stop action. The user can also stop it from Android system settings and revoke Android permissions at any time.

## Product scope: implement capabilities, ship them disabled

VLink should retain an extensible architecture for the full planned capability set. A capability being implemented or present in the protocol does not mean it is enabled on a device.

- Keep capability modules, protocol types, policy checks, tests, and UI/configuration hooks available for future activation.
- Default every capability to `DISABLED` on every fresh installation and after trust reset.
- Separate implementation availability, device permission, owner configuration, trusted-peer authorization, and runtime-active state. None of these states implies another.
- A capability may run only when all required gates are true: implementation available, owner explicitly enabled it, Android permission is currently granted where applicable, peer is trusted, session is authenticated, and the operation is within policy.
- If any gate is absent, deny by default and record a safe audit event.
- The Master cannot turn on a disabled Slave capability remotely. Remote requests can never mutate the local allowlist or Android permission state.
- Provide independent switches for each capability; never bundle camera, microphone, location, files, or other sensitive capabilities under one blanket switch.
- Preserve modules in a dormant state rather than deleting or silently omitting them. Do not request unused Android permissions at install time merely to reserve future functionality.

### Capability inventory

The capability registry should be extensible and versioned. The initial planned inventory includes:

| Capability | Fresh-install default | Activation gate |
|---|---|---|
| Camera | DISABLED | Explicit owner enablement, Android camera permission, foreground/platform eligibility, visible OS privacy indicator |
| Microphone | DISABLED | Explicit owner enablement, Android microphone permission, foreground/platform eligibility, visible OS privacy indicator |
| Location | DISABLED | Explicit owner enablement, appropriate location permission and Android background-location rules if applicable |
| File access / transfer | DISABLED | Explicit owner enablement, scoped storage or Android document picker access, per-operation policy |
| Notifications | DISABLED | Explicit owner configuration and Android notification permission where required |
| Device status / diagnostics | DISABLED | Explicit owner enablement and a defined minimal data scope |
| Remote control / input actions | DISABLED | Explicit owner enablement, supported Android APIs and system-mediated authorization; no accessibility/security bypass |
| Screen capture / sharing | DISABLED | Explicit owner action through Android MediaProjection consent flow for each required session; no silent capture |
| Clipboard | DISABLED | Explicit owner enablement and Android-version-specific restrictions |
| Other future capabilities | DISABLED | Must declare a dedicated permission, policy, audit, and revocation contract before implementation |

The inventory is a product architecture target, not a statement that every module is already implemented. Add capabilities through a registry entry and dedicated policy/tests; do not create a generic unrestricted command executor.

## One-time setup

1. The user explicitly chooses the Slave role.
2. The user configures the signaling endpoint and pairs with the intended Master.
3. The user reviews the capability inventory. All capabilities start disabled; the user may enable only the ones they want and that Android currently permits. Each Android runtime permission is requested through the Android permission flow; one generic approval must never be treated as granting unrelated OS permissions.
4. VLink persists the trusted peer identity, the per-capability owner allowlist, and the user's setup choice using Android Keystore-backed storage.
5. VLink explains the foreground notification and ongoing background behavior before the user starts the service.
6. The app starts the foreground service. Once Android confirms service startup, the setup Activity finishes.

## Runtime behavior

- The foreground service, not an Activity or Compose effect, owns the WebRTC transport, signaling connection, reconnect policy, and trusted-peer state.
- The service accepts messages only from a previously paired and currently trusted Master identity.
- Requests outside the locally approved capability allowlist are rejected by default.
- A remote request is not an authorization grant. It is evaluated against the local policy and current Android permission state.
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

This document records the confirmed product requirement and the target architecture. It is not evidence that a foreground service, durable trust store, automatic reconnect, capability modules, or real-device background operation have been implemented or tested.

The current implementation is Activity-owned: `DirectConnectionCard` constructs `WebRtcDirectTransport` inside the Activity composition and stops it from `DisposableEffect`. The current five-minute invitation is not a durable reconnect credential. These are blockers that must be addressed before claiming Slave background operation is complete.

## Acceptance criteria

- Closing the Slave Activity leaves the foreground service and authenticated transport running.
- A persistent notification is visible and its Stop action terminates the service and transport.
- Reboot/force-stop behavior is documented and tested; no guarantee is made where Android does not permit automatic restart.
- Only the paired Master can authenticate; a new or revoked peer is rejected.
- All capabilities default to DISABLED on fresh install.
- A remote Master cannot enable a locally disabled capability or grant Android permissions.
- Requests not in the locally approved allowlist are rejected without opening a Slave request UI.
- Android permission revocation and service stop immediately prevent further use of the affected capability.
- Unit tests cover trust persistence, allowlist enforcement, default-disabled state, replay protection, service lifecycle transitions, and stop/revoke behavior.
- Instrumented tests and two-device tests verify background survival under supported Android conditions.
- Sensitive camera/microphone/location/screen-capture behavior is not considered complete without a separate, transparent, OS-compliant design and validation.
