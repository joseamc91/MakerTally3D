<p align="center"><img src="docs/assets/Banner_README.en.png" alt="MakerTally 3D — Estimate your 3D printing costs" width="100%"></p>
<p align="center"><a href="README.md">Español</a> · <strong>English</strong></p>
<p align="center">
  <a href="https://github.com/joseamc91/MakerTally3D/actions/workflows/android-ci.yml"><img src="https://github.com/joseamc91/MakerTally3D/actions/workflows/android-ci.yml/badge.svg" alt="Android CI"></a>
  <img src="docs/assets/badges/android.svg" alt="Android 7.0 or later">
  <img src="docs/assets/badges/kotlin.svg" alt="Kotlin">
  <img src="docs/assets/badges/compose.svg" alt="Jetpack Compose and Material 3">
  <a href="LICENSE"><img src="docs/assets/badges/license.svg" alt="Apache License 2.0"></a>
</p>
<p align="center">Estimate 3D printing costs and a suggested selling price using your own material, electricity and machine costs.</p>
<p align="center"><strong>Android · Offline · No accounts · No ads · EUR (€)</strong></p>
<p align="center">
  <a href="docs/assets/Screenshot_Overview.png"><img src="docs/assets/Screenshot_Overview.png" alt="Actual app screens: calculator, filament profiles and compact editor" width="100%"></a>
  <br><sub>MakerTally 3D 1.1.0 with example data. Screens shown in Spanish; English is also available.</sub>
</p>

## What is MakerTally 3D?

A lightweight Android calculator for understanding the cost of a print and estimating a selling price. It combines filament weight and purchase price, print duration, electricity consumption, warm-up, effective taxes, machine cost and a configurable multiplier.

Results depend on the values you enter. The app works in **EUR (€)** and uses exact decimal calculations. It is not a slicer, an ERP, or an order or inventory manager: filament profiles **do not track remaining stock**.

## What you can do

| Area | Features |
| --- | --- |
| **Calculator** | Choose a filament, enter weight and duration, and review material, electricity, piece and machine costs alongside the suggested price. Expand calculation details and an estimated gross margin. |
| **Filaments** | Add, edit and delete profiles with material, brand, variant, spool weight, purchase price and approximate printing power. Activate/deactivate and sort by name or €/kg; active profiles always come first. |
| **Settings** | Configure electricity, effective taxes, warm-up, machine cost and sale multiplier. Choose Spanish/English and System/Light/Dark; read Help and Privacy offline. |
| **Adaptive screens** | Use portrait and landscape layouts, with a navigation rail and two-column compositions when space permits. |

## First launch in 1.1

Guided setup lets you enter your actual costs before calculating. You can change these values later in Settings.

Completing setup adds a single example profile, **PLA · Generic**: a **1000 g** spool costing **€14.99**, with **120 W** printing power. Edit it, delete it or add your own profiles. Consumption references are illustrative and never fill fields automatically.

The calculator starts with blank Weight and Hours, Minutes at 0 and no initial result. Existing 1.0 installations are recognized automatically: settings, profiles and preferences are retained, including an empty library, without repeating setup or adding Generic.

<p align="center"><img src="docs/assets/screenshots/onboarding-es.png" alt="Welcome to guided setup, shown in Spanish" width="260"></p>

## Privacy

MakerTally works **entirely offline**. It requires no account and has no backend, developer cloud, advertising, analytics, tracking, telemetry or remote crash reporting. It does not use Google Play Services or request the **INTERNET** permission.

Profiles, settings, preferences and setup progress stay in private local app storage. Calculations run on the device. The full privacy policy is available in the app without a network connection.

Read the [canonical English policy](PRIVACY.md), its [Spanish translation](PRIVACY.es.md), and the [third-party notices](THIRD_PARTY_NOTICES.md).

## Distribution

**Google Play is MakerTally 3D's only official binary distribution channel.** GitHub provides source code and documentation, with no APK, AAB or installers for manual download. There is no download button while the public listing is not available to everyone.

## Compatibility

- Android 7.0 or later (API 24+); compile/target SDK 36.
- Spanish and English; System, Light and Dark themes.
- Portrait and adaptive landscape, with scrolling for small screens and larger accessibility text.
- EUR only; no currency conversion or selector.

Development validation uses an AOSP Android 16 / API 36 emulator. This is not a claim of exhaustive device coverage; every production publication also requires a real-device smoke test.

<details>
<summary>See the dark landscape layout</summary>
<p><img src="docs/assets/screenshots/landscape-es.png" alt="Landscape calculator with side navigation and two-column results, shown in Spanish" width="100%"></p>
</details>

## Source and documentation

Built with **Kotlin, Jetpack Compose and Material 3**. The current suite has **90 JVM tests and 9 instrumented tests**. Android CI validates the wrapper, JVM tests, lint and Debug; it retains only test reports, never installable packages. Instrumented tests run locally.

- [Changelog](CHANGELOG.en.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Calculations and precision](docs/CALCULATIONS.md)
- [Development and testing](docs/DEVELOPMENT.md)
- [Release process](docs/RELEASE_PROCESS.md)
- [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md)

The earlier .NET MAUI implementation is preserved at the historical tag `maui-alpha4-final`.

## Author and support

A project by **joseamc91**. Report bugs and discuss proposals in [GitHub Issues](https://github.com/joseamc91/MakerTally3D/issues). Public contact: **joseamc91.dev@gmail.com**. Report sensitive vulnerabilities privately as described in [SECURITY.md](SECURITY.md).

## License

MakerTally 3D is licensed under the [Apache License 2.0](LICENSE). See the [third-party component notices](THIRD_PARTY_NOTICES.md).
