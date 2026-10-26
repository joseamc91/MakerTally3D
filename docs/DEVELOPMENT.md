# Development and tests

[Español](DEVELOPMENT.es.md) · **English**

## Toolchain

Use a compatible stable Android Studio and **JDK 21 for Gradle**, matching CI; its bundled JBR is suitable when compatible. Java source/target compatibility is 17. Use the committed Gradle Wrapper, not an independently installed Gradle.

| Component | Repository configuration |
| --- | --- |
| AGP / Gradle | 9.4.0 / 9.6.0 |
| Kotlin / Compose compiler plugin | 2.4.20 |
| Compose BOM / Material 3 | 2026.06.01 / 1.4.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| SDK components | Platform 36, Build Tools 36.0.0, Platform Tools |

Set `ANDROID_HOME` to your SDK or create your own ignored `local.properties`. Keep JDK/SDK paths, AAPT2 overrides and signing configuration local. No particular personal directory is required. Do not weaken security policies to build; use supported local tooling. CI must remain portable.

## Commands

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug

# With a dedicated, booted emulator/test device:
./gradlew connectedDebugAndroidTest

adb devices
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.joseamc91.makertally/.MainActivity
```

Use `gradlew.bat` in PowerShell/Windows. Replace the example serial with your actual emulator serial. Test against an isolated emulator, not personal app data: instrumented UI tests temporarily exercise setup/preferences. A locally signed debug build cannot update a differently signed production installation.

## Tests and CI

The current suite is 90 JVM + 9 instrumented tests. JVM coverage includes arithmetic/taxes, input, JSON/DataStore, migration, onboarding, CRUD/sort, ViewModel state and localized resources. Instrumentation covers real AtomicFile recovery/IO and Compose accessibility, validation, rotation, inline help and editor draft retention.

Android CI runs wrapper validation, JVM tests, lint and `assembleDebug` on Linux. It uploads only JUnit XML reports with a 14-day retention. It does not run an emulator, sign production, generate a publication AAB, upload installable binaries or publish to Play. Local debug APKs are for development, not an official download channel.

## Structure

`app/src/main/java/.../{domain,data,ui}` contains the app. JVM tests are in `app/src/test`, instrumentation in `app/src/androidTest`, ES/EN resources in `app/src/main/res`, workflows in `.github`, official branding in `branding/`, and public presentation assets in `docs/assets`. The `store/` directory retains 1.0 listing materials; historical reports are labelled by their original milestone.

See [architecture](ARCHITECTURE.md), [calculations](CALCULATIONS.md), [contributing](../CONTRIBUTING.md) and [release process](RELEASE_PROCESS.md). `LICENSE` and third-party notices govern redistribution; official installable binaries are distributed only through Google Play.
