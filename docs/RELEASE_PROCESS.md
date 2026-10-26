# Release process

[Español](RELEASE_PROCESS.es.md) · **English**

## Source and binary distribution

`main` contains the current source. **Google Play is the only official channel for installable MakerTally Android binaries.** GitHub does not host APK, AAB or Android installers in Releases, attachments or Actions artifacts. Source builds remain possible under Apache-2.0.

Future GitHub Releases, if useful, may contain notes and documentation tied to tags, but no installable Android binaries. Do not create empty releases for presentation. Test reports are permitted CI artifacts; wrapper validation, JVM tests, lint and Debug assembly remain mandatory. Instrumented tests run locally before publication.

The 1.0 line is being used in Google Play closed testing. This document is a recurring procedure, not a claim that any particular Play Console step is pending or has been completed by a repository change. Do not change existing Play tracks as part of documentation maintenance.

## Before a production publication

1. Review versionName and increment versionCode beyond all prior uploads. Current source: 1.1.0 / code 6; a source version change does not upload anything.
2. Update both changelogs and public documentation when behavior changes.
3. Run all JVM tests, lint, Debug assembly and instrumented tests on a dedicated emulator/test device. Preserve evidence and review known warnings.
4. Perform a **real-device smoke test** of clean setup, upgrade/data preservation, calculation, editor, language/theme and both orientations. Review compatibility and any native/16 KB requirements for the actual bundle.
5. Generate the final signed AAB using the existing secure production process, outside public CI. Check package, version, signing identity and Play App Signing/upload-key configuration. Keep keys, credentials and private configuration completely outside Git. Never log them.
6. Validate the signed build, store copy, screenshots, icon/feature graphic, privacy URL and third-party notices. The existing 1.0 store materials are not an automatic 1.1 submission.
7. Review Play Console testing/account requirements, declarations, listing, regions and the intended track. Submit only with explicit authorization for that publication.
8. Create a tag only after the intended final source/build has been approved. This source-repository preparation does not create a tag or GitHub Release.

## Privacy / Data Safety contract

- App/developer: MakerTally 3D / joseamc91; package `com.joseamc91.makertally`.
- Public contact: joseamc91.dev@gmail.com; support/privacy: https://github.com/joseamc91/MakerTally3D/issues.
- Canonical public policy: https://github.com/joseamc91/MakerTally3D/blob/main/PRIVACY.md; [Spanish](../PRIVACY.es.md). Both full texts are bundled offline.
- Data collected off-device by the app: none. Data shared by the app: none.
- Accounts, ads, tracking, analytics, telemetry and remote crash reporting: none.
- INTERNET permission, own backend/cloud/synchronization and Google Play Services: none.
- Profiles, settings, setup progress/values and acknowledgement flags are local. No app-provided encryption claim. Android backup is disabled, while OEM/system transfer behavior is not guaranteed.
- Opening a browser or voluntarily contacting GitHub/email involves those external services; MakerTally does not attach local app data.

Use the shipped app's actual behavior when completing Play declarations. Reassess this contract if dependencies, permissions or data flows change. Complete Ads, App access, Content rating and Target audience declarations as applicable; do not infer them from a README alone.

## Public listing and assets

When the listing is publicly accessible to normal users, add the official **Google Play badge** to both READMEs, pointing to `https://play.google.com/store/apps/details?id=com.joseamc91.makertally`. That must be the only official download button. Do not add it during closed testing.

`store/` preserves the 1.0 listing materials; `docs/assets` contains repository presentation assets. Neither directory is an installable package channel. For the GitHub social preview, upload `docs/assets/SocialPreview.png` through Settings → General → Social preview; no unsupported upload API is used.
