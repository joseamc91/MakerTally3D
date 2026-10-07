# MakerTally 3D

**v1.0.0 · Android native · Stable**

MakerTally 3D is a lightweight Android calculator for estimating the cost and suggested selling price of 3D prints. Built with Kotlin, Jetpack Compose and Material 3, it works offline with local persistence and exact decimal calculations.

This is the stable product version in the repository. **Google Play release is in preparation**; it is not yet available on Google Play. Production signing and final distribution are separate from this repository preparation.

## Features

- **Calculator:** choose an active filament, enter piece weight, whole hours and minutes (0–59), and see the suggested selling price, piece cost, material, electricity and additional machine cost. Expand calculation details for energy, rates, multiplier and gross margin.
- **Filament profiles:** add, edit, delete with confirmation, activate or deactivate; display purchase price and calculated €/kg. Sort by name or ascending/descending €/kg. Active profiles always precede inactive profiles, and the selected sort order is remembered. These are profiles, not stock inventory.
- **Settings:** Spanish/English; System/Light/Dark themes; manual electricity price and effective electricity tax percentage; heating power, heating time, machine €/h and sale multiplier. Calculator Help explains the parameters. The full privacy policy is available offline, with an optional external link.
- **Layouts:** portrait uses a top app bar and bottom navigation. Adaptive landscape uses a compact NavigationRail beside the system navigation area, recovering vertical space; Calculator, the filament grid, Add/Edit and Settings use two-column compositions where appropriate. Help and Privacy use compact internal headers.
- **Input:** comma and dot decimal separators; validation of incomplete/invalid inputs before calculation or saving.

## Currency and defaults

MakerTally 3D 1.0 works exclusively in **EUR (€)**. Language changes formatting, not the currency. There is no currency selector or exchange-rate conversion.

Seven editable filament profiles and initial electricity/printer values are supplied so the calculator works on first launch. Their brands, prices and power values are examples/estimates inherited from the reference data, not live prices or universal printer settings. Adjust them to your purchases, printer and electricity bill. `Spool` is an intentional free-text variant.

Steppers change heating power by 100 W, heating time by 1 minute, machine rate by €0.05/hour and sale multiplier by 0.5. The multiplier has a UI minimum of 1; other steppers stop at zero. Electricity and tax percentage remain manually editable. If electricity price already includes taxes, leave Taxes at 0% to avoid counting them twice.

## Privacy

No account, backend, app-provided cloud synchronization, ads, analytics, telemetry, tracking, remote crash reporting or Google Play Services. The app does not request INTERNET permission. Filament profiles and settings stay in private local app storage; calculations run on the device.

Read the canonical [Privacy Policy](PRIVACY.md), its [Spanish translation](PRIVACY.es.md) and [third-party notices](THIRD_PARTY_NOTICES.md). Opening the optional online policy delegates to an external browser; it does not give MakerTally network permission.

## Storage

Settings use AndroidX Preferences DataStore (`files/datastore/settings.preferences_pb`). Filaments use `files/filaments.json` through Android AtomicFile for both writes and recovering reads. Decimal strings retain exact stored values. A missing library creates defaults; an intentionally empty library stays empty. Damaged data is preserved locally with `.invalid` before safe recovery, and storage problems produce localized notices.

Updating with a compatible signing certificate normally retains app data. Clearing storage or uninstalling normally removes private data through Android. `allowBackup=false` remains set, but system/OEM backup and device-transfer behavior can vary; there is no app-provided cloud backup. No Windows LocalAppData import is performed.

## Calculations

Monetary calculations use `BigDecimal`, without Float/Double conversion. Non-terminating divisions remain exact fractions until final rounding; DECIMAL128 is used only for presenting repeating technical values. Intermediate costs are not rounded.

```text
TimeHours = Hours + Minutes / 60
PricePerGram = PurchasePrice / SpoolWeightGrams
MaterialCost = PieceWeightGrams × PricePerGram
HeatingEnergy = HeatingPower × HeatingMinutes / 60000
PrintingEnergy = PrintPower × (Hours × 60 + Minutes) / 60000
TotalEnergy = HeatingEnergy + PrintingEnergy
EffectiveElectricityPrice = ElectricityPrice × (1 + ElectricityTaxPercent / 100)
ElectricityCost = TotalEnergy × EffectiveElectricityPrice
PieceCost = MaterialCost + ElectricityCost
MachineCost = MachineRate × TimeHours
RawSalePrice = PieceCost × SaleMultiplier + MachineCost
SuggestedSalePrice = Excel ROUNDUP(RawSalePrice, 2)
RawGrossMargin = SuggestedSalePrice − PieceCost − MachineCost
GrossMargin = Excel ROUNDUP(RawGrossMargin, 2)
```

Piece cost excludes machine cost. Machine cost is added after applying the sale multiplier. Excel ROUNDUP uses `RoundingMode.UP` (away from zero), including negative values; display formatting does not change calculation precision.

ASA eSun reference, 141 g / 6 h / 0 min, default electricity price and Taxes 0%: material **2.4675**, electricity **0.14839**, piece **2.61589**, machine **1.50**, raw sale **9.34767**, suggested sale **9.35**, raw gross **5.23411**, gross margin **5.24**.

## Build and test

Application ID and namespace: `com.joseamc91.makertally`. Version: `1.0.0`, versionCode `5`. Compile/target SDK **36**, minimum SDK **24**, Java source/target **17**.

Toolchain: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.4.20, Compose BOM 2026.06.01 (UI/Foundation 1.11.4, Material 3 1.4.0). Use a compatible Android Studio/JDK; CI uses Temurin 21. Install SDK Platform 36 and Build Tools 36.0.0. Set your own ignored `local.properties` or `ANDROID_HOME`; machine-specific SDK/JDK paths and AAPT2 overrides are not committed.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.joseamc91.makertally/.MainActivity

# With a booted Android emulator/device:
./gradlew connectedDebugAndroidTest

# Production variants build, but remain unsigned until signing is configured:
./gradlew assembleRelease bundleRelease
./gradlew lintDebug
```

On Windows use `gradlew.bat`. Existing local Windows Application Control restrictions are handled only through supported local tooling, without weakening security policies. CI uses standard tools without local overrides.

## Validation and CI

**71 JVM tests + 7 Android instrumented tests = 78 total local tests.** Coverage includes exact calculations and taxes, numeric input, persistence/recovery, preferences, ordering, ViewModel state and localized resources. Instrumented tests exercise real Android AtomicFile, including backup recovery and IO errors.

Validation uses the **MakerTally_API36 AOSP Pixel 7 emulator**, including portrait/landscape, ES/EN and Light/Dark/System behavior. Broader physical-device/API coverage and an actual 16 KB runtime check remain to be completed; they are not presented as already tested.

Android CI validates the Gradle Wrapper, runs the **71 JVM tests** and `assembleDebug`, then uploads test reports and a debug APK. It **does not run instrumented tests** or create a signed release, AAB, GitHub Release or Play publication. The APK artifact is named `MakerTally3D-v<versionName>-android-debug-ci-<run>` using the version from `app/build.gradle.kts`.

## Scope and publication

MakerTally is a lightweight calculator: no stock management, slicer/3MF import, accounts or cloud. EUR is the 1.0 currency. These are scope decisions, not bugs.

See [Google Play release preparation](docs/PLAY-STORE-RELEASE.md) for the audited privacy/declaration contract and remaining signing/store tasks. No final `v1.0.0` tag or production release is created by this preparation.

Historical reports in `docs/` describe their original alpha milestones and keep their original test counts. The previous .NET MAUI implementation is preserved at tag `maui-alpha4-final`.

## Developer and support

Public developer: **joseamc91**. Privacy and support: [GitHub Issues](https://github.com/joseamc91/MakerTally3D/issues). Public email: **joseamc91.dev@gmail.com**.

## License

Licensed under the [Apache License 2.0](LICENSE). See [third-party notices](THIRD_PARTY_NOTICES.md) for the included Protobuf notice.
