# First-run setup — development contract for 1.1.0

Implementation lives on develop/v1.1.0. Build metadata deliberately remains 1.0.0 / code 5 until versioning is requested separately. Existing store materials and historical reports describe 1.0 and are not regenerated here.

## Storage and migration

The Preferences DataStore contains a separate onboarding_json record. It stores the current step, raw field text (including incomplete decimals), completion, installation origin, stable Generic ID and the first-filament notice acknowledgement. It does not alter the settings_json schema or the filament JSON format.

Loading precedence:

1. An existing onboarding record controls resume/normal startup.
2. Without that record, an existing settings payload, physical legacy settings file (detected before DataStore opens/repairs it), or recoverable filament library counts as an existing installation. An empty library is existing data too.
3. Such installations are marked configured without comparing any prices/brands, rewriting valid settings JSON, changing profiles or adding Generic. Stored locale/theme/sort and active states remain intact. Unreadable storage reports an error and is never classified as a clean install.
4. Only genuine absence starts Welcome. The system's Spanish/English language is used (English fallback for other languages) with System theme. No old settings payload or seven-profile seed is written. AppSettings decoding defaults remain unchanged for compatibility.

The runtime library store uses an empty fallback, so missing/corrupt data never reintroduces the developer's old profile seed. The old Defaults fixture remains available to mathematical/legacy tests.

## Setup and completion

Welcome → Electricity → Printer → Costs and pricing → completion screen. There is no Skip or Settings reset entry. The common reassurance lives outside scrolling content when height permits; with the IME or a very short viewport it remains reachable in the scroll area, while navigation remains accessible.

Electricity tax help is always inline (no modal), using the same soft help surfaces as heating and pricing references. Welcome and completion group their existing copy into balanced surfaces with the approved brand mark on Welcome.

Electricity price/taxes, machine rate and multiplier are required. Zero is accepted except for multiplier (minimum 1). Heating values are optional: blank commits zero, invalid nonempty input blocks progression. Examples are explanatory text only, with no shortcut/autofill and no recommended sale multiplier.

Each text/step change is saved to DataStore. Progression waits for pending saves. Finish is the only action that creates the example and commits settings/completion. It writes the Generic library atomically first, then settings plus the completion marker in one DataStore update. A stable ID and preservation of any existing library make a crash/retry safe without duplicate profiles or destructive overwrite.

The example is PLA · Generic, no variant, 1000 g, EUR14.99, 120 W, active. Completion navigates to Filaments. The introductory notice returns after reopening until acknowledged; acknowledgement survives restart. Deleting the example never seeds it again.

New-origin calculators start with blank weight/hours, minutes 0, the active Generic ID if still available, and no result. Legacy initial calculator behavior and reference tests are preserved; this is not a data-values heuristic for migration.

## Filament editor

Uses available Compose width, not physical DPI. Normal portrait widths pair Material/Active and Brand/Variant; Weight, Price and Power remain full width. The upper pairs require at least 360 dp of content width (scaled with the accessibility font size); narrower layouts put Active first and use one column. Landscape at 600 dp or more also pairs Weight/Price and Power/introductory text. Power is the last editable value and starts blank for a new profile; existing powers are retained. Compact permanent guidance explains temperature-dependent consumption and the 120/140/180 W reference values, with natural text wrapping and no autofill or quick buttons. Price per kg remains a card/domain value and is absent from the editor.

The existing editor scroll/IME behavior, touch targets, CRUD, sorting, validation and decimal arithmetic remain. No navigation/dependency/permission/service was added.

## Validation

JVM coverage includes fresh detection, legacy byte-preservation (including empty libraries), system-language choice, draft process reopen, steps/validation/zero/decimal inputs, completion/retry, neutral calculator state, intro acknowledgement/deletion, CRUD and power/price-per-kg regression. Existing instrumented AtomicFile tests remain; an added platform-accessibility UI test covers ES/EN inputs, validation and Activity rotation/recreation without additional test dependencies. Runtime screenshots and simulated upgrade/clean-install evidence stay outside the repository.
