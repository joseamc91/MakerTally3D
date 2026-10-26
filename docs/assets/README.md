# Repository presentation assets

These assets document MakerTally 3D 1.1.0 (versionCode 6). They are not installable downloads or Google Play submission materials.

| Asset | Size | Purpose |
| --- | --- | --- |
| `Banner_README.png` / `Banner_README.en.png` | 1280 × 360 | Spanish / English README headers |
| `Screenshot_Overview.png` | 1600 × 1000 | Calculator, filament profiles and compact editor |
| `SocialPreview.png` | 1280 × 640 | GitHub social preview, under 1 MB |
| `screenshots/calculator-es.png` | 1080 × 2160 | Light calculator with example inputs |
| `screenshots/filaments-es.png` | 1080 × 2160 | Generic example profiles, active/inactive groups |
| `screenshots/editor-es.png` | 1080 × 2160 | Dark compact editor |
| `screenshots/onboarding-es.png` | 1080 × 2160 | Guided first-run Welcome |
| `screenshots/landscape-es.png` | 2160 × 1080 | Dark adaptive calculator |

Screenshots were captured from the actual Android app on the AOSP API 36 emulator. The temporary 18:9 display setting lets complete portrait screens fit naturally; no screenshot was stretched or cropped. Standard SystemUI demo mode only cleans system clock/notifications. MakerTally UI pixels were not retouched. Example profiles/prices are demonstration data, not additional first-run defaults or pricing recommendations. Emulator private files were restored after capture.

The overview uniformly scales whole screenshots; standalone PNGs preserve the captured pixels with lossless compression. Navy `#01245F`, green `#10F283` and white `#FDFCFD` follow the existing branding. The approved [store icon](../../branding/icon/store/makertally_playstore_512.png) is reused without redesign. Editable compositions live in `source/`; SVG text uses system Segoe UI/Arial with no font files bundled.

The Spanish and English README share actual Spanish UI screenshots; the app also supports English. `store/` retains separate 1.0 listing materials. To apply Social Preview, upload the PNG in the repository's Settings → General → Social preview.

Static technology/license badges in `badges/` are local SVG text assets. Only the live Android CI badge is fetched from GitHub; the README does not depend on an external badge-generation service.

`badges/google-play-es.png` and `badges/google-play-en.png` are the official Spanish and English Google Play download badges (478 × 142), downloaded from [Google's Partner Marketing Hub](https://partnermarketinghub.withgoogle.com/brands/google-play/google-play/lockups-icons-badges/?folder=86642). The original PNG files are preserved without recoloring, cropping or distortion and displayed proportionally at half size. Google Play and the Google Play logo are trademarks of Google LLC.
