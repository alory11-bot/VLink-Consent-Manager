# VLink Signaling Service

This service performs **signaling only** for VLink WebRTC sessions. It forwards offer, answer, and ICE-candidate metadata between authenticated room participants. It does not accept, store, or relay application DataChannel payloads.

## Run locally

```sh
npm install
npm test
npm start
```

The service listens on `0.0.0.0:8080` by default. `GET /health` returns a minimal health response. WebSocket clients connect to the same origin (the server accepts the WebSocket upgrade at the root path).

## Deploy

Build the included Dockerfile and deploy behind a TLS-terminating reverse proxy. The Android client requires a public `wss://` URL for production. Do not expose plain `ws://` over the public Internet.

## Protocol

- Master sends `{type:"create", room, token}`.
- Slave sends `{type:"join", room, token}`.
- `token` is a SHA-256-derived one-way credential; the original 256-bit invitation secret is never sent to this service.
- The server stores only a hash of that credential in memory.
- Rooms expire after five minutes, allow one master and up to eight slaves, and are deleted when empty.
- Only `offer`, `answer`, `candidate`, and `bye` messages are accepted after joining.
- Message size is capped at 16 KiB and connection message rate is limited.

## Security and operational requirements

- Terminate TLS and use WSS in production.
- Use network-level rate limits and monitoring at the reverse proxy.
- The service is intentionally stateless across restarts; active sessions are dropped on restart.
- No TURN service is included or enabled in this milestone.
- WebRTC DataChannel traffic is not routed through this process.
- The app performs a separate HMAC-SHA256 proof over the DataChannel using the invitation secret, so possession of the room credential alone is insufficient to authenticate a peer.
- Do not log WebSocket message bodies, invite credentials, SDP, or ICE candidate payloads.
