# VLink Consent Manager

VLink is a consent-first Android application foundation for transparently pairing a controller and a companion device.

## Implemented in this initial milestone

- Kotlin + Jetpack Compose Android application (min SDK 26, target SDK 35, JDK 17).
- Persian RTL interface and explicit role selection.
- Cryptographically random, 8-character local invitation code with a five-minute validation window.\n- A separate 256-bit, five-minute QR invitation envelope with strict parsing, expiry validation, and constant-time secret comparison; covered by JVM unit tests.
- Explicit local consent confirmation and a visible revoke action.
- Encrypted local preference values using AES-GCM with an Android Keystore-protected key.
- Encrypted local audit history, capped at the latest 30 events.
- GitHub Actions workflow to run unit tests and assemble a debug APK.

## Important implementation boundary

This milestone is a **local UI and security-foundation prototype**, not a working cross-device product. The current UI still generates and validates its legacy invitation code against local storage on the same installation. The new QR envelope is a tested protocol primitive only: the app does not yet render or scan QR codes, transmit invitations, authenticate a remote peer, establish TLS, prevent server-side replay, or create a cross-device session. There is no relay, remote control, or background data collection.

The app intentionally excludes covert monitoring, keylogging, notification/SMS/call-log harvesting, hidden camera or microphone capture, stealth location tracking, remote wipe, and arbitrary remote command execution.

## Build in Android Studio

1. Open the repository root in Android Studio.
2. Select JDK 17 for Gradle.
3. Use Gradle 8.9 and Android Gradle Plugin 8.7.3.
4. Install Android SDK Platform 35 and accept SDK licenses.
5. Sync, then run the `app` configuration on an Android 8.0+ device or emulator.

This repository currently does not include the binary Gradle Wrapper JAR. The GitHub Actions workflow installs Gradle 8.9 directly and runs `gradle testDebugUnitTest` and `gradle assembleDebug`.

## Next engineering milestones

1. Add QR-based pairing with a short-lived challenge and one-time server-side nonce.
2. Add mutually authenticated TLS transport and explicit device approval on both ends.
3. Add a revocation protocol and server-side session invalidation.
4. Add Android instrumentation tests for Keystore persistence, consent lifecycle, and revocation.
5. Add a signed release pipeline and threat-model review.

Do not deploy this prototype as a production security product until these milestones and independent testing are complete.
