# VLink Original Source — Static Security Review

Review date: 2026-11-13
Reviewed artifact: `Pasted text(10).txt`, 1,031 lines, 31 generated-file declarations.
Review branch: `recovery/preserve-original-source`

## Preservation status

The original generator text is preserved in this branch as a non-build-input text archive:

- `docs/archive/Pasted-text-10.original-source.txt`
- The user's Library copy remains the authoritative original.
- The archive is not referenced by Gradle, Android Studio, or the app build.
- This is source preservation, not restoration or approval of the generated app.

The archive was reconstructed from the Files text reader. Its line content was preserved, but the reader normalizes line endings; therefore this repository copy should be treated as a readable text archival copy, not a byte-identical forensic image. Keep the Library original unchanged.

## Architecture and feature inventory found in the original source

| Area | Original implementation | Preservation / redesign assessment |
|---|---|---|
| Android shell | Compose, Material3, application bootstrap, activities | Preserve after ordinary build and lifecycle review. |
| Pairing | QR payload carrying PSK, relay and topic; QR encode/decode | Redesign pairing with short-lived one-time challenge, peer identity binding, explicit confirmation and key rotation. |
| LAN discovery | Android NSD advertise/browse | Preserve as user-initiated local discovery with explicit device selection. |
| Local storage | SQLite devices/events, media path, location history | Preserve data model concept; add encryption at rest, retention limits, export/delete and per-event consent. |
| Cryptography | AES-GCM helper using shared 32-byte key | Keep only after protocol redesign, unique per-peer keys, authenticated context, versioning and key lifecycle. |
| Event transport | HTTP LAN / relay upload, polling | Redesign to TLS-only authenticated transport, strict schema, replay defense, bounded payloads and opt-in event scopes. |
| Media | Generic upload and persistent media folder | Do not implement ambient or hidden capture. Only explicit user-selected media sharing, with preview and recipient confirmation. |
| Remote command queue | `/cmd` endpoint and arbitrary JSON queue | Do not enable arbitrary remote commands. Replace only with typed, allowlisted, user-approved actions. |
| WebSocket relay | Master role, token in URL, blob forwarding | Redesign authentication and authorization; avoid credentials in URLs; add peer binding, expiry and revocation. |
| MQTT | Public HiveMQ broker, predictable topic structure | Do not use public broker defaults. If retained, use a controlled broker, per-device credentials, ACLs and TLS validation. |
| Background work | Outbox retry loop, boot receiver/service references | Preserve only visible, user-enabled sync; provide pause/stop controls, status and data-retention controls. |
| Notifications | Event notifications for SMS/call/location/media/keylog | Preserve notifications only for user-authorized app events; do not harvest other apps' private notification content. |
| Device administration | Admin receiver and wipe-data policy | Exclude remote wipe. Use Android's standard uninstall / user-controlled reset only. |
| Accessibility | Text-change/focus events and window-content retrieval | Exclude keylogging and screen-content harvesting. Accessibility must not be repurposed for surveillance. |

## Static security findings

Severity is a design assessment from source inspection, not a claim that a live exploit was demonstrated.

### Critical / must not ship as written

1. **Broad surveillance permission surface.** The manifest requests SMS, call-log, phone-state, camera, microphone, background location, contacts, media, notification-listener, accessibility, overlay, exact-alarm and package-enumeration capabilities. There is no visible granular consent ledger or per-scope activation UI in the generator.
2. **Keylogging-capable accessibility configuration.** `accessibility.xml` subscribes to text-changed and focus events and permits window-content retrieval. `Notif.kt` includes a `keylog` event label. This is not an acceptable general-purpose monitoring feature.
3. **Device wipe policy.** `admin.xml` includes `wipe-data`, in addition to password/reset/force-lock policies. Remote or coerced wipe is excluded from the redesigned product.
4. **Unrestricted command abstraction.** `MasterServer.push(deviceId, cmd)` queues arbitrary JSON and `/cmd` returns it to a paired client. The supplied source does not show a typed command allowlist, risk classification, user confirmation, or an execution policy.
5. **Sensitive data collection and exfiltration pathways.** Generic event, media and notification pipelines accept categories including SMS, call, location, photo, video, audio and keylog. The manifest references services for those data sources, although their class implementations are not present in the supplied generator.

### High

6. **Cleartext HTTP enabled.** `net_sec.xml` sets `cleartextTrafficPermitted=true`; LAN transport uses `http://` for event, media, command polling and pairing. AES-GCM wrapping does not secure HTTP metadata, endpoint integrity, availability, or all request semantics.
7. **Shared global PSK.** The same key is initialized in the application and used by pairing/server/transport primitives. There is no demonstrated per-peer key derivation, rotation, secure enrollment ceremony, or revocation protocol.
8. **Pairing lacks replay and lifecycle controls.** The QR payload contains version, PSK, relay and topic only. No expiry, one-time nonce, device identity binding, audience, signature or replay cache is visible.
9. **Credential in WebSocket URL.** Relay master token is concatenated into `/hub?role=master&token=...`, where URL logging and intermediary diagnostics can expose it.
10. **Public MQTT namespace.** The hard-coded broker is `ssl://broker.hivemq.com:8883`; topic segments depend on a topic value and no broker username/password, ACL, client certificate, or ownership proof is configured.
11. **Unbounded upload / request handling.** The server reads a content-length into a byte array without a maximum; media upload copies the input stream to cache without an explicit byte cap, timeout policy, MIME allowlist or quota.
12. **Media metadata is trusted.** File extension is derived from the client-supplied MIME string. Upload handling does not demonstrate content sniffing, safe filename generation policy, malware scanning or retention limits.
13. **Outbox path and retention risks.** Outbox stores absolute media paths and later deletes the referenced file after upload. No explicit allowlisted app-private path validation, maximum queue size, expiry, or user review is shown.
14. **Weak peer authorization model.** In-memory token-to-device mapping is not persistent and no token expiry, rotation, revocation endpoint, rate limit or failed-attempt throttling is shown.
15. **Sensitive local event database.** Event bodies, coordinates and media paths are stored in SQLite without database encryption or retention policy in the supplied code.
16. **Exception detail exposure.** `MasterServer` returns `e.message` in a 500 response, potentially disclosing internal details.

### Medium / maintainability and release readiness

17. `Crypto.open` catches all exceptions and returns null, which makes authentication/decryption failures hard to distinguish from malformed data in audit telemetry.
18. AES-GCM uses no associated authenticated data (AAD), so protocol context, sender, recipient and message type are not bound to the ciphertext.
19. `MqttLink.send` reports success after calling publish without waiting for a delivery token; delivery is not established by that boolean.
20. MQTT subscribes in both `connectComplete` and after `connect().waitForCompletion`, which can create redundant subscription operations.
21. The original release build uses debug signing, disables minification and does not define a production signing/release pipeline.
22. The generator references multiple activities/services/receivers that it does not generate. These unresolved classes prevent treating the artifact as a complete compilable app.
23. `LocalDb.onUpgrade` is empty; schema evolution and migration safety are absent.
24. No unit, instrumentation, protocol, authorization, abuse-case or data-retention tests are generated by the script.
25. No dependency lock, SBOM, vulnerability scan or dependency update policy is included.

## Manifest-only references: implementation not supplied

The generator declares these classes in the manifest but contains no matching file-generation blocks for them: `HubService`, `LinkService`, `CaptureService`, `LocationService`, `NotifListener`, `KeylogService`, `CallScreen`, `SmsReceiver`, `BootReceiver`, `AlarmReceiver`, `AdminReceiver`, `RoleActivity`, `MasterActivity`, `ClientActivity`, `ScanActivity`.

These names are retained in the inventory. Their implementations cannot be audited because they were not part of the supplied 42,433-byte generator text. Do not infer that those classes were safely implemented elsewhere.

## Safe continuation plan

1. Preserve the original text and all 31 generated path declarations as audit evidence.
2. Recover non-sensitive product behavior first: app shell, explicit pairing, local discovery, device list, user-selected file sharing, consent ledger, revocation, audit history and secure sync.
3. Implement per-scope consent and visible foreground state before any data access.
4. Remove all cleartext network paths; require TLS and authenticated peer identity.
5. Replace shared global PSK with one-time enrollment and unique per-peer keys.
6. Replace generic command JSON with a fixed, typed, allowlisted protocol; no arbitrary shell, script, intent or command execution.
7. Add payload limits, quotas, retention/expiry, secure deletion, schema validation, rate limiting and replay protection.
8. Add tests for consent denial, revocation, expired invitations, replay, unauthorized peer, oversized uploads, invalid media, queue overflow and data deletion.
9. Keep this work on `recovery/preserve-original-source`. Do not merge into `main` until static review, Android build, tests and independent CI all pass.

## Completion statement

This review accounts for the original generated files and the manifest-only class references. It identifies the risky functions rather than pretending they were not part of the user's source. The original generator is archived as text in the recovery branch; none of its surveillance-oriented or arbitrary remote-control behavior has been activated in the current app.


## Recovery implementation update

Static, non-surveillance Android resources and build metadata were restored on the recovery branch. The original permissive cleartext policy was deliberately hardened to `cleartextTrafficPermitted=false`; the archived generator retains the original source text for audit. The active manifest was not expanded with the original surveillance permissions or service declarations. The restored wrapper properties do not constitute a complete Gradle Wrapper because the wrapper JAR is not present.
