# VLink Consent Manager

VLink is a consent-first Android application foundation for transparently pairing a controller and a companion device.

## Implemented

- Kotlin + Jetpack Compose Android application (min SDK 26, target SDK 35, JDK 17).
- Persian RTL interface and explicit role selection.
- Cryptographically random, 8-character local invitation code with a five-minute validation window.
- A separate 256-bit QR invitation envelope primitive with a five-minute expiry, strict field parsing, duplicate-field rejection, defensive secret copying, and constant-time secret comparison.
- JVM unit tests covering invitation generation, QR envelope round-trip, expiry boundaries, malformed payloads, and secret comparison.
- Explicit local consent confirmation and a visible revoke action.
- Encrypted local preferences using AES-GCM with an Android Keystore-protected key.
- Encrypted local audit history, capped at the latest 30 events.
- GitHub Actions workflow configured to run unit tests and assemble a debug APK.

## Important implementation boundary

This is still a **local UI and security-foundation prototype**, not a working cross-device product. The current UI continues to generate and validate its legacy invitation code against local storage on the same installation. The new QR envelope is a tested protocol primitive only: the app does not yet render or scan QR codes, transmit invitations, authenticate a remote peer, establish TLS, enforce server-side replay prevention, or create a cross-device session. No APK or successful CI run has been verified yet.

The app intentionally excludes covert monitoring, keylogging, notification/SMS/call-log harvesting, hidden camera or microphone capture, stealth location tracking, remote wipe, and arbitrary remote command execution.

## Build

1. Open the repository root in Android Studio.
2. Select JDK 17 for Gradle.
3. Use Gradle 8.9 and Android Gradle Plugin 8.7.3.
4. Install Android SDK Platform 35 and accept SDK licenses.
5. Sync, then run the `app` configuration on an Android 8.0+ device or emulator.

The repository does not currently include the binary Gradle Wrapper JAR. GitHub Actions installs Gradle 8.9 directly and runs `gradle testDebugUnitTest` and `gradle assembleDebug`.

## Remaining production milestones

1. Integrate QR rendering and in-app scanning, with clear device identity and approval on both devices.
2. Exchange a short-lived challenge over an authenticated channel and enforce one-time consumption/replay prevention.
3. Establish mutually authenticated TLS and verify peer identity before any data exchange.
4. Implement bilateral session revocation and prove invalidation with integration tests.
5. Add Android instrumentation tests for Keystore persistence, consent lifecycle, and revocation.
6. Add dependency scanning, a signed release pipeline, and an independent security review.

Do not deploy this prototype as a production security product until these milestones and independent testing have passed. See [SECURITY.md](SECURITY.md) for the threat boundary and required controls.
