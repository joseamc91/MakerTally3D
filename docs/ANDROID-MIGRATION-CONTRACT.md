# Android migration contract — MakerTally 3D alpha4

Source of truth: MAUI commit `f64b45d0656b88ce1f97894d90323916f4638737`, preserved by annotated tag `maui-alpha4-final`. This audit reads Core, presentation, XAML, localization dictionaries, platform adapters, storage and every test method/theory case. No historical Android beta defines behavior. Main remains the MAUI baseline; native work is on `android-native`.

## A. Port to Android

- Exact business formulas and Excel ROUNDUP at two decimals (away from zero, including negatives). Gross margin is a monetary amount, not a percentage. It uses the already-rounded sale price. No intermediate cent rounding.
- Calculator starts with ASA eSun when active, 141 g, 6 h, 0 min; active filaments only. Zero costs/time remain valid; configured warmup still applies at zero print time. Invalid temporary input hides results, retaining last valid values internally. Empty active library yields an actionable translated notice.
- Weight uses decimal separators dot/comma; no exponent/grouping. Hours/minutes are non-negative whole values; minutes 0–59. Negative costs/rates rejected. Preserve useful validation and overflow guards rather than Kotlin silently accepting unbounded values that would fail in alpha4.
- Vertical hierarchy: PVP only in accent card → New print inputs → piece cost (material + electricity), material, electricity and additional machine cost → discreet entire-row details toggle, closed by default. Details contain price/g, heating/printing/total kWh, precise electricity, precise piece cost, machine/hour, multiplier, monetary gross margin. No ROUNDUP internals in UI.
- Filaments: one-column compact cards; name = trimmed material + optional variant + ` · ` + trimmed brand; weight/power, paid price, price/kg. Add/edit/delete with confirmation, activate/deactivate. IDs stable on edit. Calculator refreshes selection on library changes; inactive profiles remain stored. Save failure must not commit visible library changes.
- Editor: required material/brand, optional variant, initial spool 1000 g, power 120 W, active true, purchase price initially blank/required (zero allowed). Weight > 0; price/power >= 0; price/kg computed. Notes are not shown in alpha4, only retained for legacy Windows data; a new Android model has no redundant legacy Name/Notes fields.
- Settings: language es-ES by default, theme System; electricity 0.1349, heating power 1200 W, heating time 1 min, machine/hour 0.25, multiplier 3. Electricity manual; steps +/−100 W, 1 min, 0.05 EUR/hour, multiplier 0.5; zero minima except deliberate multiplier edits clamp at 1. Existing fractional values do not snap to a step grid. Invalid electricity text does not replace valid persisted settings. Language/theme can save independently of invalid numeric text.
- Exact initial filaments (each active, 1000 g): PLA BambuLab Spool (variant Spool), 13.20/120 W; PLA BambuLab 11.69/120; PETG BambuLab 11.69/140; PLA Jayo 10.90/120; ASA eSun 17.50/180; PLA 850 Sakata (variant 850), 16.00/120; PETG Elegoo 15.00/140. IDs are generated, not hardcoded.
- Version remains v0.1-alpha4; Android versionName 0.1-alpha4 / code 4. Offline, no backend/accounts/tracking/INTERNET permission/Google Play Services.

## B. Adapt to Android

- One Activity, Compose Material 3, bottom NavigationBar fixed above system navigation; scroll only content; touch areas >=48dp; filaments FAB with reserved list bottom space; editor uses native Material controls and scrolling.
- C# decimal → BigDecimal. Finite decimal operations stay exact. Non-terminating division (e.g. 1/60) is kept as an exact fraction inside the calculation until final ROUNDUP, so an approximate repeating decimal cannot falsely add a cent. BigDecimal projections for technical results use DECIMAL128 only when the decimal expansion is infinite; no intermediate economic rounding. Exact reference fixtures remain identical. C# 28-digit range guards are retained for user inputs and result overflow behavior.
- JSON settings → AndroidX Preferences DataStore. Filaments → Kotlin serialization JSON in private files, atomic writes, validation, corruption backup/default recovery and translated error notices. No SQL. New native sandbox starts from defaults; empty saved lists stay empty. IDs, inactive state, variant and exact decimals round-trip.
- MAUI translation bindings → Android resources (English base + Spanish), AndroidX per-app locales (including API 24–32), runtime changes. EUR formatted explicitly even for en-US, no Double conversion. Valid inputs reformat culturally; incomplete inputs survive language/rotation changes.
- Theme bindings → Compose color schemes with System/Light/Dark. No proprietary theme framework. ViewModel/state + SavedStateHandle preserve transient input, selected tab/filament and details across recreation; persistence remains only settings/library.
- Timers/MVVM commands/dialog interfaces → simple ViewModel, coroutine IO, native dialogs and state. Inventory publishes only after save succeeds; rapid settings writes are serialized; IO failures remain visible.
- Native build uses the validated stable toolchain: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.4.20, Compose BOM 2026.06.01 / UI 1.11.4 / Material 3 1.4.0, compile/target 36, min 24. Local signed-AAPT2/JBR settings are ignored/local only, never checked into Git. Standard CI uses normal AGP tools.

## C. Remove Windows-specific behavior

Window width/height/X/Y persistence, monitor working areas, WinUI/titlebar/App SDK/unpackaged single-file distribution, Windows executable/identities and LocalAppData migration from PrintCost. Android neither reads Windows files nor imports an unpublished beta sandbox. `window.json` and Windows-only tests are not part of native storage.

## D. MAUI infrastructure without necessary equivalent

.sln/.csproj, XAML dictionaries/extensions, ObservableObject notifications, INotifyDataErrorInfo, binding/indexer refresh tests, adaptive grids/rail remnants, MAUI DI bootstrap/handlers, desktop dialog hosts, WPF legacy, .NET SDK/workloads, Windows CI and package metadata. Useful behavior is rewritten idiomatically, not mechanically translated.

## Formula acceptance fixture

ASA eSun, 141 g, 6 h, defaults: PricePerGram 0.0175; Material 2.4675; heating 0.020; printing 1.080; total 1.100; electricity 0.14839; PieceCost 2.61589; MachineCost 1.50; RawSalePrice 9.34767; SuggestedSalePrice 9.35; RawGrossMargin 5.23411; GrossMargin 5.24. Assertions compare BigDecimal numerically without floating tolerances. ROUNDUP cases: 9.34000→9.34, 9.34001/9.34101/9.34767/9.34999→9.35, 9.35000→9.35; 5.23000→5.23, 5.23001/5.23411→5.24; negative counterparts and zero.

## All 107 MAUI test cases

`MAUI-TEST-AUDIT.csv` enumerates each original case, including every InlineData row, its classification and native equivalent/group. Multiple source cases may be expressed in one Kotlin test loop; the Kotlin test count is not artificially forced to 107.

- Windows-specific: 21 cases (11 window geometry/persistence, 9 LocalAppData migration, 1 Windows path policy).
- No longer applicable: 6 cases about legacy Windows filament-name/variant inference and old Windows JSON compatibility. Current native JSON/default/variant/activation tests replace useful ongoing behavior; historical inference itself is intentionally absent.
- MAUI infrastructure: 1 indexer notification case. Runtime Android locale switching is covered separately.
- Remaining 79 behavioral cases: ported or rewritten with native equivalents. See the per-case table and subsequent native test/validation report for actual results.

The initial contract commit preserves MAUI files. Native code may replace them on the migration branch only after this audit. The annotated tag and untouched main preserve the original implementation, tests, assets and history.
