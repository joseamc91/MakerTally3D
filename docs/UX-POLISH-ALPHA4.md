# Alpha4 native Android UX polish

Baseline: clean `android-native`, commit `68e6e74778a35ab60768650c899849cfb9ada1ec`; 48 passing JVM tests. Version remains `0.1-alpha4` / code 4. No changes to calculation formulas, exact decimal/fraction arithmetic, ROUNDUP, defaults, filament fields, file storage or the CI workflow. The only new stored setting is an optional filament sort preference; older settings decode to name order without resetting any other value.

## Changes

- Filaments: active profiles first, then inactive profiles. Each group uses the selected name A–Z, ascending €/kg or descending €/kg order. Locale-aware name comparison and name/ID tie-breakers make ordering deterministic. Exact price/weight comparison uses BigDecimal cross multiplication, with no division, rounding or Double. Sorting creates a presentation list; stored profile order and free-form variants are untouched.
- A 48 dp Material sort IconButton opens a DropdownMenu with a check beside the selected criterion. Selecting another criterion returns to the beginning of the new order. The criterion is saved through the existing Preferences DataStore settings payload.
- Inactive cards use themed `surfaceContainer` / `onSurfaceVariant`, retaining readable prices and the accessible 48 dp actions menu. `INACTIVE` / `INACTIVO` replaces the weight/power line; no fourth line or error styling is added. Editing or toggling activation immediately regroups the list after a successful save.
- Language uses the same stable Material 3 `SingleChoiceSegmentedButtonRow` as theme. The Application heading is removed; the calculation section becomes Calculator settings / Ajustes de calculadora. Theme behavior and existing steppers are unchanged.
- The outlined filament selector keeps its existing whole-surface click/ripple and minimum 48 dp target; a 24 dp vector chevron replaces the tiny text glyph. No extra caption or row is added.
- Opening calculation details waits for the expanded layout, then uses `ScrollState.animateScrollTo` to bring the heading and details into view when needed. The target is bounded by the actual scroll range, so it does not add empty space beneath the card. When the details already fit, it does not scroll. Closing does not initiate scrolling. Accessibility exposes expanded/collapsed state. No instrumentation framework was introduced for this small interaction.
- Hours/minutes use `KeyboardType.Number`; piece/spool weight, electricity, purchase price and estimated power use `KeyboardType.Decimal`, preserving existing fractional-value support and comma/dot parsing. Material, brand and optional variant keep standard text keyboards.
- Explicit FocusRequesters implement weight → hours → minutes Done, and material → brand → variant → spool weight → price → power Done. Electricity also uses Done. KeyboardActions dismiss the keyboard and clear focus on Done. Numeric fields select their existing content once on gaining focus, without preventing later cursor positioning. Selection-only changes do not trigger calculation input changes or writes.
- Sort, filament actions and steppers have localized accessibility descriptions. Bottom navigation and selection controls retain native Material semantics. Cancel, the editor's separate €/kg row, PLA Spool and the PVP card remain unchanged. No search, swipe, new dependency or business feature was added.

## Tests

`test` and `assembleDebug` pass locally: **57 tests, zero failures/errors/skips**. All 48 existing tests remain unchanged. New tests: six ordering cases (three sort modes with active grouping, activation moves, unit-price comparison including repeating ratios, preserved variants/deterministic ties); two preferences cases (old settings compatibility and closing/reopening real DataStore instances); one ViewModel case (saved sort, language/theme preservation, unchanged calculation, restart and failed-write behavior).

The existing ASA reference still produces material **2.4675**, electricity **0.14839**, piece **2.61589**, machine **1.50**, PVP **9.35**, gross margin **5.24**. No mathematical tests were edited.

## Emulator validation

Device: existing API 36 AOSP x86_64 Pixel 7 AVD `MakerTally_API36`, 1080×2400, density 420; boot completed and adb reports `device`. Updated debug APK installed with `adb install -r` without clearing app data.

Manual validation covers the selector, live inputs and temporary invalid decimal states, opening/closing details with smooth automatic reveal, all three sorting criteria and their checked indicators, active/inactive presentation, deactivate/reactivate through both the actions menu and editor, segmented locale/theme controls, unchanged steppers, and force-stop/relaunch persistence of sort/language/theme. Actual touchscreen keyboard Next buttons traverse every editor field; Done dismisses the keyboard (`mInputShown=false`). Numeric values can be replaced directly because the existing content is selected on focus. A temporary taller viewport also confirmed that expanding details does not move the page when all the content already fits; the device resolution was restored afterward.

Font scales 1.0, 1.25 and 1.5 were inspected. At 1.25, controls and navigation remain legible and usable. At 1.5, Settings uses normal vertical scrolling; no overlapping text, inaccessible controls or horizontal scrolling was observed. Calculator PVP/inputs, filament cards, menu, navigation, language/theme segments, steppers and the add-filament editor were inspected. English and Spanish work; Light, Dark and System following emulator night mode work. No global font reduction or alternative layout was needed.

Screenshots and validation logs are kept outside the repository. This remains an emulator/manual accessibility check, not a full TalkBack or physical-device certification.

No MakerTally crash, ANR, DataStore/JSON exception or focus/IME exception was observed; the final crash buffer is empty. Native diagnostics include ashmem deprecation and denied AndroidX accessibility selection-method lookups on this API 36 image, without a crash or observed interaction failure. One overlapping UIAutomator inspection failed because its accessibility service was already registered; its stack trace belongs to the shell inspection tool, not the app. The affected inspection was repeated sequentially and the original diagnostic log was retained separately. Final application validation uses sequential inspections.

## Tooling and CI

The ignored, machine-local build helper uses Android Studio JBR and the SDK's signed AAPT2 because of Windows Application Control. AGP reports the existing local AAPT2 override warning; JBR reports an existing DataStore/protobuf Unsafe deprecation warning. Neither is suppressed or committed. No security policies, global JAVA_HOME or production dependencies changed. CI still uses standard Gradle tooling, validates the wrapper, runs JVM tests before assembling, and uploads the debug APK.
