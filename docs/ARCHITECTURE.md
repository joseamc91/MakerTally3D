# Architecture

[Español](ARCHITECTURE.es.md) · **English**

## Native Android, local only

MakerTally uses Kotlin, Jetpack Compose and Material 3. There is one Android app module and no backend, account service, network permission, analytics or Google Play Services. Application ID and namespace are `com.joseamc91.makertally`.

| Layer | Responsibility |
| --- | --- |
| `domain/` | Validated decimal models, exact calculation engine, ratios, formatting and input/editor rules. |
| `data/` | JSON serialization, Preferences DataStore, AtomicFile access, recovery and first-run state. |
| `ui/` | Compose screens, adaptive shell and `MakerTallyViewModel` state/actions. |
| `MainActivity` | Compose host, edge-to-edge and per-app locale integration. |

## State and persistence

`MakerTallyViewModel` exposes `UiState` through StateFlow. Compose observes lifecycle-aware state and sends explicit actions back to the model. SavedStateHandle retains calculator/navigation state through normal Activity recreation; it is not a print-history database.

Preferences DataStore contains `settings_json` and a separate `onboarding_json` record. Settings hold language, theme, electricity/taxes, warm-up, machine rate, multiplier and sort mode. Onboarding stores raw text, step, completion, installation origin, a stable Generic ID and the introductory-notice acknowledgement. Raw text intentionally retains incomplete decimal input during setup.

Filaments are stored as decimal-string JSON in private `files/filaments.json`. Android AtomicFile handles both writes and recovering reads. Missing data is distinguished from IO failures; damaged content is preserved locally as `.invalid` before recovery. Runtime library fallback is empty, rather than reintroducing old development profiles.

## First run and 1.0 migration

1. An existing onboarding record controls resume or normal startup.
2. Without it, physical previous settings storage, an existing settings payload or a recoverable filament file counts as an existing installation. An empty library counts too; values and brands are never used as a heuristic.
3. Existing valid settings/library content is retained and setup is marked complete silently. No Generic profile is added.
4. Genuine absence starts Welcome, using the supported system language and System theme. No personal settings payload or old seven-profile seed is written.

Only the final setup action commits the chosen settings and completion marker and creates the single Generic example if no library already exists. The library write is atomic; settings/completion share a DataStore edit. A stable ID and preservation of existing content make an interrupted completion retry safe. The notice persists until acknowledged, and deletion never reseeds Generic.

`AppSettings` decoding defaults remain the legacy compatibility contract. Explicit neutral first-run values are separate. See [the detailed first-run contract](FIRST-RUN-1.1.md).

## UI and design principles

Portrait keeps its top bar and bottom navigation. Landscape reuses screen logic inside a rail-based shell; system navigation insets guide the rail side when available. Calculator, filament grid, editor and Settings use available width rather than physical DPI. Narrow widths and larger accessibility text can use a single column and scroll.

The editor pairs Material/Active and Brand/Variant in normal portrait widths; numeric fields stay full width. Wide landscape retains its two-column composition. Help surfaces, navigation, contrast and touch targets follow the existing Material theme. Insets/IME handling stays in the UI layer.

The product remains a cost calculator: no inventory, cloud, slicer import or order workflow. Exact monetary rules live outside Compose; see [calculations](CALCULATIONS.md). [Privacy](../PRIVACY.md) documents local data handling.
