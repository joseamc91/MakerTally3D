# Changelog

[Español](CHANGELOG.md) · **English**

## Unreleased

No additional changes recorded.

## 1.1.0 — 2026-10-26

### First-run experience

- Guided setup for electricity and effective taxes, optional warm-up, machine cost and sale multiplier.
- Setup step and entered values are saved so configuration resumes after closing the app.
- A PLA · Generic example profile is created only at completion, with an initial explanation until acknowledged.
- Neutral calculator state: blank Weight and Hours, Minutes 0, no initial result or premature errors.

### Interface and clarity

- Inline tax help and warm-up references grouped in soft information surfaces.
- Compact power explanation with PLA 120 W, PETG 140 W and ASA/ABS 180 W references, without autofill.
- Compact editor: Material/Active and Brand/Variant share rows when space permits; Weight, Price and Power stay full width in portrait.
- Better-balanced Welcome and Setup complete screens, with refinements for portrait, landscape and larger accessibility text.

### Data preservation and tests

- Automatic 1.0 migration without changing settings, profiles, active/inactive states or preferences.
- Empty libraries are preserved; Generic is not added to existing installations and deleted profiles are not reseeded.
- First-run, resume, migration and UI regression coverage: 90 JVM tests and 9 instrumented tests.
- Bilingual documentation and CI lint; GitHub retains source and test reports without distributing APK/AAB files.

## 1.0.0 — 2026-10-07

- First stable native Android version, built with Kotlin, Jetpack Compose and Material 3.
- Material, electricity, effective taxes, machine cost, suggested selling price and margin, with exact decimals and explicit rounding.
- Editable filament profiles, activation/deactivation and sorting.
- Electricity, warm-up, machine and multiplier settings; offline Help and Privacy.
- Spanish/English, System/Light/Dark, portrait and adaptive landscape.
- Private local storage with no accounts, network access, ads or tracking; initial Google Play preparation.

MakerTally began as a cross-platform prototype and was rebuilt as a native Android app before 1.0. Alpha and MAUI milestones are historical, not additional public releases.
