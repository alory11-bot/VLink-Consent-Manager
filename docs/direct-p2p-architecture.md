# Direct P2P transport (V1)

## Architecture decision
- Relay policy: **never**. No VLink app server, cloud relay, TURN relay, or third-party device forwards application payloads.
- Topologies: point-to-point (one peer) and point-to-multipoint (one Master with multiple Slaves).
- Radio transport: Google Nearby Connections using the P2P_STAR strategy. It negotiates device-to-device radio links (Bluetooth/BLE/Wi-Fi); application payloads travel between endpoints.
- Master advertises; Slave discovers and requests a connection. Both endpoints must approve the displayed authentication digits before a connection is accepted.
- If direct connectivity cannot be established (range, radio, permissions, Play services availability, OEM restrictions), VLink reports failure. It must not silently fall back to a relay.

## Current implementation boundary
This change adds the transport adapter, topology policy, dependency, and manifest declarations. It does not yet wire the adapter into the Compose screens, request runtime permissions, persist trusted peer identities, or deliver a production background service. Message payloads are raw bounded bytes (1 MiB maximum) and must only be sent after the application-level capability consent gate is integrated. Do not describe this commit as a completed remote-control product.

## Permissions
Nearby Connections requires Bluetooth and nearby-device/Wi-Fi permissions that vary by Android version. Manifest declarations are not runtime grants. The app must explain the purpose and request the applicable permissions at first use. Users can revoke them in Android Settings.

## Security requirements for the next phase
1. Persist a per-install identity and authenticated peer public keys.
2. Bind the verified Nearby authentication token to the pairing transcript; never trust endpoint name as identity.
3. Require local Slave approval for each capability scope; camera/microphone/location remain runtime and foreground-visible.
4. Add message envelope versioning, replay protection, request IDs, expiry, and strict payload limits.
5. Add lifecycle handling, reconnect policy, rate limits, and multi-device authorization tests.
