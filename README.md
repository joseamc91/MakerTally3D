# MakerTally 3D

**v0.1-alpha4 · Android native · Alpha**

A small, offline 3D printing cost and pricing calculator, rebuilt in Kotlin, Jetpack Compose and Material 3. No account, backend, tracking, ads, cloud, Google Play Services or Internet permission.

This private migration branch is `android-native`. The last .NET MAUI Windows/Android implementation remains on `main` and at annotated tag `maui-alpha4-final` (commit `f64b45d0656b88ce1f97894d90323916f4638737`). It was the functional specification, not a historical Android beta. No license or public release has been chosen. Planned distribution: F-Droid / Google Play; neither is available yet.

## Functionality

- Calculator: filament, piece weight, whole hours and minutes (0–59), live suggested price, piece/material/electricity/additional-machine costs and collapsed technical details.
- Filaments: seven original profiles; add/edit/activate/deactivate/delete with confirmation, exact spool pricing and calculated €/kg. Active profiles always precede inactive profiles; name or ascending/descending €/kg order is remembered. No stock tracking.
- Settings: Spanish/English per-app locale; System/Light/Dark themes; manual electricity price; heating power ±100 W, heating time ±1 min, machine rate ±€0.05/hour, multiplier ±0.5 (minimum 1). Other steppers stop at zero.
- Dot/comma numeric input; invalid temporary input never crashes or produces a misleading calculation.

## Build and test

Toolchain: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.4.20, Compose BOM 2026.06.01 (UI/Foundation 1.11.4, Material 3 1.4.0). Android compile/target SDK 36, minimum SDK 24. Application ID `com.joseamc91.makertally`; versionName `0.1-alpha4`, versionCode `4`.

Use Android Studio stable or a compatible JDK (CI uses Temurin 21), SDK Platform 36 and Build Tools 36.0.0. Configure your own ignored `local.properties` or `ANDROID_HOME`.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.joseamc91.makertally/.MainActivity
```

On Windows use `gradlew.bat`. The repository does not contain machine-specific JDK/SDK paths or AAPT2 overrides. Local Windows Application Control restrictions must be addressed only with supported, locally configured tools; security policies are not modified. CI uses normal AGP AAPT2 and requires no local workaround.

## Structure

`app/src/main/java/com/joseamc91/makertally/` contains the pure `domain` calculator/models, small `data` repository and `ui` ViewModel/Compose screens. One Activity; no DI framework, SQL or extra modules. `app/src/test` contains JVM tests. Native string resources are in `values` (English) and `values-es` (Spanish).

## Storage

Settings are stored with AndroidX Preferences DataStore in the app-private sandbox (`files/datastore/settings.preferences_pb`). The validated settings payload and filament JSON use decimal strings so their exact values survive serialization. Filaments are stored as `files/filaments.json` using Android AtomicFile. Missing data creates only the missing defaults; an intentionally empty library stays empty. Invalid data is preserved with `.invalid` before safe recovery; read/save problems are localized and displayed. No Windows LocalAppData or old beta import is performed. Uninstalling the app clears its private data; `adb install -r` preserves it. Android backup is disabled.

## Calculations

All monetary values use `BigDecimal`; no Float/Double monetary conversion. Finite operations remain exact. Non-terminating divisions are kept as exact fractions until final rounding; DECIMAL128 is used only to project repeating technical result values for presentation.

```text
TimeHours = Hours + Minutes / 60
PricePerGram = PurchasePrice / SpoolWeightGrams
MaterialCost = PieceWeightGrams × PricePerGram
HeatingEnergy = HeatingPower × HeatingMinutes / 60000
PrintingEnergy = PrintPower × (Hours × 60 + Minutes) / 60000
ElectricityCost = (HeatingEnergy + PrintingEnergy) × ElectricityPrice
PieceCost = MaterialCost + ElectricityCost
MachineCost = MachineRate × TimeHours
RawSalePrice = PieceCost × SaleMultiplier + MachineCost
SuggestedSalePrice = Excel ROUNDUP(RawSalePrice, 2)
RawGrossMargin = SuggestedSalePrice − PieceCost − MachineCost
GrossMargin = Excel ROUNDUP(RawGrossMargin, 2)
```

ROUNDUP is away from zero (`RoundingMode.UP`), including negative values. Intermediate costs are not rounded. Display formatting alone rounds technical/cost values.

ASA eSun reference (141 g, 6 h, original defaults): material 2.4675, electricity 0.14839, piece 2.61589, machine 1.50, raw sale 9.34767, **sale 9.35**, raw gross 5.23411, **gross margin 5.24**.

## Validation and CI

The original 107 MAUI cases are individually classified in [the audit](docs/MAUI-TEST-AUDIT.csv), with behavior recorded in [the migration contract](docs/ANDROID-MIGRATION-CONTRACT.md). The initial 48 JVM tests remain intact; nine UX ordering/preferences regression tests bring the total to 57. Emulator results and known tooling warnings are recorded in [the migration validation report](docs/ANDROID-VALIDATION.md) and [the alpha4 UX polish report](docs/UX-POLISH-ALPHA4.md). Windows monitor/geometry/migration and MAUI notification infrastructure are intentionally absent.

Android CI runs wrapper validation, JVM tests and `assembleDebug` on Linux, then uploads the APK as `MakerTally3D-v0.1-alpha4-android-debug-ci-<run>`. No emulator, Windows build, signing key, store publishing or Release is involved.

Alpha limitations: validated on the API 36 AOSP Pixel 7 emulator; older devices and physical hardware still need broader testing. Landscape preserves state but has no special layout. APKs are debug-signed. No stock, slicer import, customers, taxes or new business features were added.

