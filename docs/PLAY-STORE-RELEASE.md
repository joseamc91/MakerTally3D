# Google Play release preparation

Repository/app version: 1.0.0, versionCode 5. This document records the audited contract; it is not a statement that a release has been published.

## Identity

- App: MakerTally 3D.
- Application ID and namespace: com.joseamc91.makertally.
- Public developer: joseamc91.
- Public developer email: joseamc91.dev@gmail.com.
- Privacy/support: https://github.com/joseamc91/MakerTally3D/issues.
- Provisional public privacy URL: https://github.com/joseamc91/MakerTally3D/blob/main/PRIVACY.md.
- Canonical policy: [PRIVACY.md](../PRIVACY.md); Spanish translation: [PRIVACY.es.md](../PRIVACY.es.md). Both full texts are bundled for offline access.

## Data Safety contract

- Data collected off-device by the app: none.
- Data shared with third parties by the app: none.
- Accounts: none; no account-deletion service is required.
- Ads, analytics, telemetry, tracking and remote crash reporting: none.
- INTERNET permission: none; online privacy opens an external browser with ACTION_VIEW.
- Own backend/cloud/synchronization and Google Play Services: none.
- Filament profiles, settings and calculation inputs are stored or processed locally. No app-provided encryption claim.
- Android backup remains disabled; OEM/system transfer behavior is not guaranteed. Do not describe this as an app cloud-transfer feature.
- Voluntary GitHub/email contact is handled by those external services; no local app data is attached automatically.

Complete Play Console declarations using the shipped app's actual behavior. Revisit this contract if future dependencies or features change it.

## Distribution status and remaining steps

The repository declares stable product version 1.0.0. Google Play release is in preparation: no v1.0.0 tag, GitHub Release, production key, Play App Signing setup or signed production AAB is created by this preparation phase.

Prepare upload-key custody, production signing, final signed-build smoke test, Play Console declarations and store materials in a separate task. The approved 512 icon already exists; feature graphic and store screenshots are not prepared here. A real 16 KB runtime check remains pending.

1.0 currency is exclusively EUR; default filament prices and printer/electricity values are editable examples/estimates, not live rates.

The CI validates the wrapper, 71 JVM tests and assembleDebug. Seven real-AtomicFile instrumented tests are run locally on MakerTally_API36; CI does not execute them. Artifacts derive their version from app/build.gradle.kts. Release APK/AAB build locally but remain unsigned until signing is prepared.

Include [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) appropriately with the eventual distribution. No new legal screen is required by this document.
