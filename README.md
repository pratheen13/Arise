# ARISE

Native Android fitness RPG inspired by Solo Leveling. Current version: 4.3. Package ID: com.arise.myapp1.

## Android app

Kotlin and Jetpack Compose UI, six rank appearances, daily exercise quests, illustrated completion dialogs, local XP, and Health Connect steps/distance imports. Samsung Health-only mode filters by the Samsung Health source; it is not guaranteed to be watch-only. When recorded distance is unavailable, the app labels its 0.70 m-per-step estimate. Repetitions are self-reported.

Requires Android 9+ and an available Health Connect provider. On supported newer phones Health Connect is built in. Samsung/watch activity must first be shared by the companion app. Imports happen on return and while the app is open, not continuously in the background.

## Build

Install JDK 17 and Android SDK platform 36. Open this folder in Android Studio, which can create local.properties for your SDK path.

Run `./gradlew assembleDebug testDebugUnitTest` (Windows: `gradlew.bat assembleDebug testDebugUnitTest`). The debug APK is in app/build/outputs/apk/debug/.

Debug builds use your local Android debug signing key. They cannot update the previously delivered APK unless built with its original private signing key. Preserve the original signing key privately; it is deliberately excluded from this repository.

For a signed release, set ARISE_KEYSTORE, ARISE_STORE_PASSWORD, ARISE_KEY_ALIAS, and ARISE_KEY_PASSWORD in your environment, then run `./gradlew assembleRelease`. Without those variables a release is unsigned. Never commit signing material.

## Contents

- app/src/main: active Kotlin app and original artwork assets.
- app/src/test: progression and distance tests.
- docs: version notes and data-source limitations.



## Validation and limitations

The latest local build and 13 unit tests passed. The Kotlin app installed in an emulator, but complete visual testing was interrupted by an emulator System UI freeze. Phone/watch synchronization and the reported count difference still require real-device verification. This is a personal test app, not a production-certified tracker.

Health data remains on device. No account or upload service is implemented. Clearing app storage deletes local progress.

Artwork was AI-generated from user-provided visual references. ARISE is an unofficial fan project; no affiliation with Solo Leveling or its rights holders is claimed. No third-party artwork rights or redistribution license is granted by this repository.

