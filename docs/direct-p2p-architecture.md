# Direct P2P transport (V1)

## Architecture decision
- Relay policy: **never**. No VLink app server, cloud relay, TURN relay, or third-party device forwards application payloads.
- Topologies: point-to-point (one peer) and point-to-multipoint (one Master with multiple Slaves).
- Radio transport: Google Nearby Connections using the P2P_STAR strategy. It negotiates device-to-device radio links (Bluetooth/BLE/Wi-Fi); application payloads travel between endpoints.
- Master advertises; Slave discovers and requests a connection. Both endpoints must approve the displayed authentication digits before a connection is accepted.
- If direct connectivity cannot be established (range, radio, permissions, Play services availability, OEM restrictions), VLink reports failure. It must not silently fall back to a relay.

## Current implementation boundary
This change adds the transport adapter, topology policy, dependency, and manifest declarations. It does not yet wire the adapter into the Compose screens, request runtime permissions, persist trusted peer identities, or deliver a production background service. Message payloads are raw bounded bytes (32 KiB maximum) and must only be sent after the application-level capability consent gate is integrated. Do not describe this commit as a completed remote-control product.

## Persistent local identity (V1 foundation)

- Each installation now creates a random UUID once and stores it through `SecureStore` (AES-GCM with an Android Keystore-backed key).
- The connection screen displays a short form of that local identifier for diagnostics and future pairing workflows.
- The identifier is local metadata only. It is not a secret, public key, peer credential, or proof that a remote device is trusted.
- Reinstalling the app or clearing its app data may create a new installation identity.
- This phase deliberately does not auto-approve peers, persist trusted peers, or reconnect silently. Nearby authentication digits must still be compared and approved on each connection.

### Next security milestone

1. Generate a per-install asymmetric key pair in Android Keystore.
2. Exchange public-key fingerprints only after the user approves the Nearby authentication digits.
3. Persist a peer trust record only after explicit confirmation on both devices.
4. Bind subsequent sessions to a challenge-response proof and a versioned pairing transcript.
5. Keep reconnect disabled until replay protection, key rotation/revocation, and negative-path tests are complete.

## Permissions
Nearby Connections requires Bluetooth and nearby-device/Wi-Fi permissions that vary by Android version. Manifest declarations are not runtime grants. The app must explain the purpose and request the applicable permissions at first use. Users can revoke them in Android Settings.

## Security requirements for the next phase
1. Persist a per-install identity and authenticated peer public keys.
2. Bind the verified Nearby authentication token to the pairing transcript; never trust endpoint name as identity.
3. Require local Slave approval for each capability scope; camera/microphone/location remain runtime and foreground-visible.
4. Add message envelope versioning, replay protection, request IDs, expiry, and strict payload limits.
5. Add lifecycle handling, reconnect policy, rate limits, and multi-device authorization tests.

## Manual two-device smoke test
1. Install the same VLink build on both Android devices; Google Play services must be available.
2. Keep both devices nearby, with Bluetooth and Wi-Fi enabled. Keep VLink open in the foreground.
3. Select Master on one phone and Slave on the other.
4. On Master, choose one-to-one or one-to-many and start advertising. On Slave, start discovery.
5. When the connection prompt appears on both phones, compare the displayed authentication digits out-of-band and approve only if they match.
6. Confirm that each side displays the connected peer. Stop the session from the UI to disconnect.

This transport is for nearby radio-range devices (actual range depends on hardware and environment). It is not a global-Internet P2P tunnel: it does not connect distant devices across unrelated networks. Closing the activity stops discovery/advertising in this preview. Reconnection without re-verification and persistent trusted-peer identities are not implemented yet.
