# Security Notes

## Security boundary

VLink currently provides a local consent UI, encrypted local preferences, a bounded local audit history, and a tested QR invitation envelope primitive. It is not yet a secure remote-device pairing product.

The QR envelope contains a random 256-bit bearer secret and a five-minute validity window. Anyone who obtains an unexpired QR payload can copy that invitation. Do not post QR images, payload strings, or screenshots publicly. The current app does not yet exchange the invitation with a peer, keep a server-side one-time nonce ledger, authenticate a remote device, or establish a TLS channel.

## Explicit non-goals

The project must not implement covert surveillance or hidden collection. In particular, do not add keylogging, hidden camera/microphone capture, stealth location tracking, silent SMS/call/notification harvesting, remote wipe, or arbitrary remote command execution.

## Required controls before production

- Render and scan QR invitations inside the app, with clear peer identity and confirmation on both devices.
- Bind invitation consumption to an authenticated server-side or peer-side one-time challenge; reject replay.
- Establish mutually authenticated TLS and verify the peer identity/fingerprint before exchanging any data.
- Make consent scopes explicit, minimal, visible, time-bounded, and revocable from either device.
- Revoke sessions on both endpoints and prove invalidation with integration tests.
- Keep secrets out of logs, crash reports, analytics, clipboard history where controllable, and audit event details.
- Add instrumentation tests, dependency scanning, a signed release process, and an independent security review.

## Current verification limitation

A successful source commit or a green JVM test task is not proof of end-to-end security. Do not describe this prototype as production-ready until CI, device-level tests, transport tests, and an independent threat-model review have passed.
