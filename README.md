# HWX Gateway — Android source handoff

This is an Android Kotlin + Jetpack Compose project with a backend source snapshot. The existing UI theme and screen structure are retained.

## Important status

This is **not yet a finished or verified production app**. It is a source project requiring Firebase configuration, Android build validation, backend contract verification, and physical-device testing. Do not use it for real customer messaging until those checks are complete.

## Demo mode

The local demo-session shortcut is disabled. A local preference can no longer count as an authenticated session, and the Welcome screen's demo action routes to real sign-in instead. Demo repository fixtures remain in source for test/reference purposes but are unreachable through the disabled demo-mode flag.

## Android setup

1. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio.
2. Install the Android SDK required by the project and use JDK 17.
3. `app/google-services.json` is now populated with the Firebase Android client configuration supplied for project `hwxgetway` and package `com.hwxgetway`. Restrict the Google API key in Google Cloud Console to the Android app (package name and signing certificate SHA-1) and only the APIs required by the project.
4. Enable Firebase Email/Password authentication. Google sign-in UI is not wired to Credential Manager yet.
5. Sync Gradle and resolve any dependency/build errors in Android Studio. The wrapper JAR and launcher scripts are not included in this export; use Android Studio's Gradle wrapper generation or a compatible local Gradle installation to generate them.
6. Test on a physical phone. SIM availability and SMS behavior depend on Android version, OEM and carrier. Grant SMS permissions only after explaining the feature and obtaining user consent.

## Backend endpoints

- REST base: `https://hwxgetaway.onrender.com/`
- Health: `https://hwxgetaway.onrender.com/health`
- Device WebSocket: `wss://hwxgetaway.onrender.com/ws/device`
- Backend source snapshot: `backend_extracted/Hwxgetaway--main/`

The deployed backend must have Firebase Admin credentials configured server-side. Never put a Firebase service-account private key in the Android app or source repository.

## Changes in this pass

- Disabled demo-mode authentication and removed the fake demo account entry point.
- A local demo preference can no longer make the app appear logged in.
- Retained the existing UI and backend snapshot.

## Still required before production

- Replace placeholder Firebase client config.
- Complete Google sign-in if required.
- Validate every Android API request against the currently deployed backend's actual routes and JSON schemas.
- Complete permission UX and background execution behavior.
- Verify SIM-to-Bot mapping and explicit SIM authorization on supported dual-SIM devices.
- Run Android build, unit/instrumentation tests, and end-to-end SMS tests with a consenting test recipient.
- Add production monitoring, rate limits, opt-in/consent controls, and applicable India DLT/carrier compliance for commercial messaging.

Only send messages to people who have agreed to receive them. Do not use this app for unsolicited bulk messaging.


## Trusted multi-device enrollment

See [DEVICE_ENROLLMENT_WORKFLOW.md](DEVICE_ENROLLMENT_WORKFLOW.md) for the new trusted-device approval flow, API endpoints, and deployment/test requirements.
