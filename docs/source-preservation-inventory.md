# VLink — Original Source Preservation Audit

Audit date: 2026-11-13
Repository: `alory11-bot/VLink-Consent-Manager`
Audit branch: `recovery/preserve-original-source`
Baseline: `backup/before-source-preservation-2026-11-13`
Original source artifact: Library file `Pasted text(10).txt` (42,433 bytes; 1,031 lines; file id `file_00000000d1f081f49ab7b4df7ba53f56`; library file id `libfile_4650c6e62214819194953418bd246201`).

## Scope and method

The original artifact is a PowerShell generator. Its `W '<path>' @'` declarations were enumerated across all 1,031 lines. It declares **31 output files**. Those paths were compared against the recursive Git tree of the backup branch. The source artifact remains in the user's Library; it has not been rewritten.

This document is an inventory, not a claim that the original implementation has been restored. No original implementation file has yet been copied into the application repository.

## File-by-file inventory

| # | Original generated path | Backup branch status | Notes |
|---:|---|---|---|
| 1 | `README.md` | Replaced at same path | Original build, relay and pairing workflow differs from current README. |
| 2 | `settings.gradle.kts` | Replaced at same path | Original project name is VLink and includes :app. |
| 3 | `build.gradle.kts` | Replaced at same path | Original AGP 8.7.0 / Kotlin 2.0.21. |
| 4 | `gradle.properties` | Replaced at same path | Original JVM and AndroidX settings differ. |
| 5 | `gradle/wrapper/gradle-wrapper.properties` | Missing | Wrapper distribution configured for Gradle 8.9. |
| 6 | `app/proguard-rules.pro` | Missing | Keep rules for Paho MQTT and NanoHTTPD. |
| 7 | `app/build.gradle.kts` | Replaced at same path | Original namespace `ai.vlink`, minSdk 29, dependencies include CameraX, location, NanoHTTPD, ZXing, ML Kit, MQTT, etc. |
| 8 | `app/src/main/AndroidManifest.xml` | Replaced at same path | Original declares extensive permissions, components and receivers; see sensitive inventory below. |
| 9 | `app/src/main/res/values/strings.xml` | Missing | App string resources. |
| 10 | `app/src/main/res/values/themes.xml` | Missing | Original dark Material theme. |
| 11 | `app/src/main/res/values/colors.xml` | Missing | Original background color. |
| 12 | `app/src/main/res/xml/net_sec.xml` | Missing | Permits cleartext traffic globally. |
| 13 | `app/src/main/res/xml/accessibility.xml` | Missing | Accessibility event and window-content configuration; sensitive. |
| 14 | `app/src/main/res/xml/admin.xml` | Missing | Device-admin policies include wipe-data; sensitive. |
| 15 | `app/src/main/res/drawable/ic_fg.xml` | Missing | Launcher foreground vector. |
| 16 | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Missing | Adaptive launcher icon. |
| 17 | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Missing | Round adaptive launcher icon. |
| 18 | `app/src/main/java/ai/vlink/App.kt` | Missing | Application bootstrap, local DB, PSK initialization and osmdroid setup. |
| 19 | `app/src/main/java/ai/vlink/core/Const.kt` | Missing | Port 8765, NSD service type, public MQTT broker and notification channels. |
| 20 | `app/src/main/java/ai/vlink/core/Config.kt` | Missing | DataStore settings: role, PSK, device ID, token, relay and MQTT topic. |
| 21 | `app/src/main/java/ai/vlink/core/Notif.kt` | Missing | Notifications include SMS, call, location, media and keylog event labels. |
| 22 | `app/src/main/java/ai/vlink/net/Crypto.kt` | Missing | Shared-key cryptographic transport helper. |
| 23 | `app/src/main/java/ai/vlink/data/LocalDb.kt` | Missing | Device/event database and location history query. |
| 24 | `app/src/main/java/ai/vlink/net/Pairing.kt` | Missing | PairPayload JSON and QR generation/decoding. |
| 25 | `app/src/main/java/ai/vlink/net/EventSink.kt` | Missing | Event ingestion, notification and uploaded-media persistence. |
| 26 | `app/src/main/java/ai/vlink/net/MasterServer.kt` | Missing | NanoHTTPD endpoints: /pair, /event, /media, /cmd; command queue. |
| 27 | `app/src/main/java/ai/vlink/net/Discovery.kt` | Missing | LAN NSD advertise/browse and host resolution. |
| 28 | `app/src/main/java/ai/vlink/net/Transport.kt` | Missing | LAN/relay event and media upload, command polling and pairing. |
| 29 | `app/src/main/java/ai/vlink/net/Outbox.kt` | Missing | Encrypted local outbox and retry loop; media deletion after upload. |
| 30 | `app/src/main/java/ai/vlink/net/RelayClient.kt` | Missing | WebSocket relay client, master role and blob forwarding. |
| 31 | `app/src/main/java/ai/vlink/net/MqttLink.kt` | Missing | MQTT client, topic subscription and blob publishing. |

**Path-level result:** 6 of 31 generated paths exist at the same path in the backup branch, but all 6 have different/replacement content; 25 generated paths are absent at their original paths. The current project has differently named consent/security components, but those are not byte-for-byte or feature-equivalent preservation of the original files.

## Components referenced by the original manifest but not generated by this script

The manifest references these application classes, but no corresponding `W` output declarations were found in the 1,031-line generator:

- `.svc.HubService`
- `.svc.LinkService`
- `.svc.CaptureService`
- `.svc.LocationService`
- `.svc.NotifListener`
- `.svc.KeylogService`
- `.svc.CallScreen`
- `.svc.SmsReceiver`
- `.svc.BootReceiver`
- `.svc.AlarmReceiver`
- `.svc.AdminReceiver`
- `.ui.RoleActivity`, `.ui.MasterActivity`, `.ui.ClientActivity`, `.ui.ScanActivity`

These are **referenced but not implemented in the supplied generator artifact**. Their absence must not be mistaken for an audit omission.

## Sensitive / legally restricted / abuse-prone elements found

The following were explicitly recorded rather than silently discarded:

- Accessibility service configured for text-change/focus events and window-content retrieval; notification says “کیلاگ”. This is a keylogging/privacy-capture risk.
- Camera and microphone foreground-service declarations and CameraX dependencies; hidden capture would be unacceptable.
- Fine/coarse/background location permissions and location service declaration; continuous or undisclosed tracking is a privacy risk.
- Notification-listener service; could expose private notifications.
- SMS receive/read permissions and SMS receiver; could collect private messages.
- Call-log/phone-state/answer-call permissions and call-screening service; could collect or manipulate call data.
- Device-admin receiver includes `wipe-data`.
- Boot receiver could enable persistence after reboot.
- `/cmd` endpoint and remote command queue require strict command allowlisting and authorization; arbitrary command execution is not acceptable.
- `/media` upload endpoint and generic media pipeline can exfiltrate user data absent explicit, scoped consent.
- Cleartext HTTP is enabled in network security config and used by LAN transport; authentication alone does not replace transport confidentiality/integrity.
- MQTT uses a public broker endpoint; topic secrecy and broker trust are not adequate peer authentication.
- Pairing payload includes a shared PSK; lifecycle, rotation, secure delivery and peer binding require review.

These items are preserved in the audit record for completeness. This repository branch will not activate covert keylogging, hidden camera/microphone capture, covert SMS/notification/call harvesting, stealth location tracking, remote wipe, or arbitrary remote command execution. Any legitimate feature redesign must be user-initiated, visibly active, narrowly scoped, permission-based, revocable, auditable and tested with explicit consent.

## Important limitations

1. The original 42,433-byte generator remains available in the user's Library under the identifiers above; this audit does not delete or alter it.
2. This repository does not yet contain a byte-for-byte source archive. The generator itself can create operational Android components, so it is intentionally not copied into the buildable application tree.
3. No APK build, runtime test, or CI result is claimed by this inventory commit.
4. Before any restoration, each source file must be reviewed against the original artifact and classified as safe to preserve, requiring redesign, or not suitable for activation. Any code changes must remain on this recovery branch; do not overwrite `main`.

## Audit conclusion

The earlier simplified app was not a faithful continuation of the supplied VLink source. The 31 generated paths and the additional manifest-only component references are now explicitly enumerated. **No claim of full source restoration is made.** The original Library artifact remains the authoritative untouched source reference.


## Original source archive (updated 2026-11-13)

The complete 1,031-line generator text has now also been committed as a non-build-input text archive at:

- `docs/archive/Pasted-text-10.original-source.txt`
- [Open archived original source](https://github.com/alory11-bot/VLink-Consent-Manager/blob/recovery/preserve-original-source/docs/archive/Pasted-text-10.original-source.txt)

The archive is kept as `.txt`, is not included by Gradle, and does not activate or compile its generated behaviors. The Files reader normalized line endings during retrieval; the archive has 1,031 lines and the same readable source content, but is not represented as a byte-for-byte forensic copy. The untouched Library artifact remains the authoritative original.

A separate static review is available at `docs/original-source-security-review.md`, covering protocol, permissions, transport, data storage, remote commands, release readiness and the unimplemented manifest references.


## Safe source components restored to original project paths

The following non-surveillance resources have now been restored on this recovery branch:

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/drawable/ic_fg.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/proguard-rules.pro`

The original `net_sec.xml` path was restored with a **hardened** policy (`cleartextTrafficPermitted=false`), not the original permissive setting. The original permissive source remains visible in the archived generator. No unsafe permissions or services were added to the active manifest. The wrapper JAR is still absent, so the wrapper is not yet independently runnable.
