# Android alpha4 validation

Historical report from the initial native Android migration, before Android became canonical on `main`. Branch locations, test counts and publication status below describe that earlier stage; see the current README and UX polish report for the current state.

## Preserved source and scope

Source of truth: MAUI commit `f64b45d0656b88ce1f97894d90323916f4638737`, protected by annotated tag `maui-alpha4-final`. `main` remains at that commit. Only `android-native` contains the native rewrite. No release, merge, license, public visibility change or new business feature.

All 107 original cases are individually audited: 52 PORTADO, 27 REESCRITO (79 useful functional cases), 21 WINDOWS-SPECIFIC, 6 obsolete legacy compatibility cases and 1 MAUI notification-infrastructure case. See `MAUI-TEST-AUDIT.csv` for equivalent test methods. Optional null variants become empty native strings; hidden old Windows Name/Notes fields are intentionally omitted from the fresh Android model.

## Local tests and build

48 JVM test methods pass: 22 domain, 8 input/presentation state, 9 persistence, 7 ViewModel, 2 resources/manifest. Boundary fixtures are grouped inside relevant methods rather than inflating counts. Comparisons use exact numeric BigDecimal equality (compareTo), without floating tolerances.

`testDebugUnitTest` and `assembleDebug` succeed. Application ID `com.joseamc91.makertally`; compile/target 36, minimum 24; versionName `0.1-alpha4`, versionCode `4`. Debug APK is approximately 12.66 MiB. Local SDK/JBR/AAPT2 configuration is ignored and never included in source or CI.

Local warnings: the signed SDK AAPT2 override is marked experimental by AGP; JBR 25 emits a DataStore/protobuf Unsafe deprecation warning. A clean APK build may retain debug symbols for AndroidX graphics/DataStore native libraries when no matching strip tool is configured. These are tooling/dependency notices, not compilation failures. No security policy was changed. Standard CI does not use the local AAPT2 override.

## Emulator checks

AVD `MakerTally_API36`: API 36 AOSP/default x86_64, Pixel 7, 1080×2400 portrait, 2560 MiB RAM. WHPX reports installed and usable after the reboot. The emulator is detected as a ready device and `sys.boot_completed` returns `1`. Devices are selected dynamically from adb, not assumed by fixed port.

- Calculator: manually entered 141 g, 6 h, 0 min, ASA eSun and reference settings. PVP **9.35**, piece **2.61589**, material **2.4675**, electricity **0.14839**, machine **1.50**, gross margin **5.24** (localized EUR on screen). Details initially closed; gross margin is shown only after expansion, without ROUNDUP internals.
- Invalid temporary minute `.` displays discrete validation and no result; correcting to zero restores the exact price without a crash.
- Filaments: created `PLA Matte · EnvCheck` at 12.34; edited price to 13.25 with the same ID; deactivated the profile; deletion required confirmation. Test profile was removed afterward, preserving the original seven profiles.
- Settings: language switched between Spanish and English without reinstalling; labels/navigation/currency changed correctly. Machine step moved 0.25→0.30→0.25. Other exact step rules are verified by JVM tests.
- Persistence: after force-stop/relaunch and `adb install -r`, the edited profile (same ID/price), English, dark theme and machine rate 0.30 remained. Library JSON before/after reinstall was byte-identical. Settings use private Preferences DataStore and the library uses private AtomicFile JSON. Verified actual sandbox location with `run-as`; no Windows storage policy.
- Themes: explicit Light and Dark verified; System responds to emulator `cmd uimode night yes/no`. Native bottom navigation, inputs, details, cards and FAB remain readable.
- Rotation: portrait→landscape→portrait preserves inputs, price, navigation and state, without a crash. Content scrolls vertically; no horizontal scrolling. No special landscape UI was introduced.
- FAB remains fixed above bottom navigation, independent of card scrolling; list bottom padding allows the final card to scroll clear. Menus have a 48 dp target, despite a small visible glyph.
- Runtime crash buffer is empty. Activity exit records show intentional package replacements and force-stop, not crashes or ANR. Logcat contains emulator graphics, ashmem deprecation and AndroidX accessibility hidden-API diagnostics during UIAutomator inspection; no fatal exception or serialization failure was observed.
- Final package manifest has no INTERNET permission or Play Services. The AndroidX-generated signature permission for non-exported dynamic receivers is the only requested permission.

Screenshots and raw log/UI/persistence evidence are kept outside versioned source. Portrait captures cover all three pages, English, explicit dark/light and System dark/light; landscape is also captured. Only this technical report is committed.

## Remaining scope

Physical hardware, other API levels and broad accessibility/device coverage remain unvalidated. APK is debug-signed, not a store release. Landscape uses the same vertical composition. There is no migration from Windows LocalAppData or unpublished Android beta data, as requested.

## Quick Boot and first standard CI

After the initial post-reboot snapshot failed, a cold boot recovered the AVD and saved a new normal snapshot. A subsequent normal shutdown/restart was verified again at the end: new emulator/QEMU processes started, but Android guest uptime resumed from 2755 seconds to 2840 seconds, and the app was already the foreground Activity. This demonstrates snapshot restoration rather than a new cold boot. WHPX remained usable.

The first workflow attempt failed before compilation because `sdkmanager` was not on the runner PATH. The workflow now resolves it through the runner-provided `ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager`; no fixed PC path or custom SDK is used. Run #2 (`37309637878`, commit `52f5cb7`) passed on Linux with standard AGP tools: all 48 JVM tests, debug assembly and APK artifact `MakerTally3D-v0.1-alpha4-android-debug-ci-2`. JUnit reports were inspected independently. No CI APK was installed over the local app, avoiding a change of debug signing key/private data.

JVM tests also pass without Android stub return-defaults mode; accidental calls to unsupported framework code are not silently accepted by the test configuration. A final documentation/test-configuration commit is validated by a subsequent workflow run.
