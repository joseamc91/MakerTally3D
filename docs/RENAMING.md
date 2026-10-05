# MakerTally 3D — rename and Windows CI preparation

Version remains **0.1.0-alpha.4 / v0.1-alpha4**. This change introduces branding, the new Windows data directory, a small copy migration and repository/CI infrastructure. It does not change the alpha4 UI, pricing rules or themes/languages behavior.

## Source and preservation

The source was the active alpha4 tree of the previous PrintCost workspace, identified by its solution/projects, version, vertical MAUI layouts, 98 expected tests and alpha4 validation audit. A clean copy of 109 files was verified using SHA-256. Generated directories, builds, test results, logs and archives were excluded. The original workspace remains untouched. The new working root is `D:\MakerTally 3D`.

All local source manifests, scripts, absolute original paths and build/test logs are in ignored `.work/`. They are not repository content. Historical documentation remains useful evidence, but user-specific paths and data fingerprints are removed from this copied history.

## Identity

| Item | Current identity |
| --- | --- |
| Visible product | MakerTally 3D |
| Short brand | MakerTally |
| Description | 3D Printing Cost Calculator |
| Solution | MakerTally.sln |
| Projects | MakerTally.Core / MakerTally.App / MakerTally.Tests |
| Active namespace root | MakerTally |
| App assembly / executable | MakerTally.dll / MakerTally.exe |
| Android ApplicationId | com.joseamc91.makertally |
| Windows package identity | com.joseamc91.makertally |
| Windows publisher metadata | CN=MakerTally / MakerTally |
| Windows manifest assembly identity | MakerTally.App.WinUI.app |
| Package version | 0.1.0.4 |
| Technical version | 0.1.0-alpha.4 |
| Visible version | v0.1-alpha4 |

The personal reverse-DNS-style ID uses the intended account convention, not a claimed company domain. There is no published old package or Store app. The current Windows distribution is unpackaged; publisher metadata does not constitute a signature/certificate.

Active code paths, resource assembly names, XAML/code-behind and project references are consistent. The branded calculation service/file is renamed to MakerTallyCalculationService; its mathematical method is unchanged.

## Deliberate old-brand references

1. `DataDirectoryMigration.LegacyDirectoryName = "PrintCost"`: necessary to discover old Windows data.
2. Migration explanations in current README and this report.
3. `docs/history/`: archived records of the old product and versions.
4. `legacy/`: original WPF source/reference tests/documents, explicitly frozen and excluded from the active solution and CI. Old namespaces/paths there describe the archived source, not a runnable MakerTally project. No claim is made that this WPF snapshot compiles against the current Core.

There are no accidental old-brand tokens in active namespaces, assembly/project/resource names, ApplicationId, visible AppName resources or package metadata. The existing provisional icon is intentionally unchanged because graphic/UI redesign is outside scope.

## Windows migration

The active location is `%LocalAppData%\MakerTally`, never the source directory. Before Windows loads settings or restores its window, a lazy directory resolver calls the Core copy utility once. It considers settings.json, filaments.json and window.json in that order.

Missing destinations are copied as raw bytes using a unique temporary file and promoted without replacement. Existing new files win, including a concurrently created destination. Missing legacy files are skipped, absent source produces no output, unrelated files are excluded, and repeating the migration is idempotent. Nothing is moved/deleted in the old directory.

An IO failure propagates before defaults can obscure a missing legacy file; old files remain available and only complete copies are published. There is no schema conversion, arithmetic change or automatic reseeding. Subsequent JSON compatibility/recovery is unchanged. Android keeps its private directory and does not attempt cross-package sandbox migration.

Nine new cases test the migration with temporary synthetic data. All 98 existing cases remain, with namespace/class identity updates and the storage-path test's expected brand adjusted. The mathematics and numerical expectations are not altered. Total expected: 107.

## Local validation

Windows restore/build Release x64 completed with 0 errors and 0 warnings. The local publish produces the complete folder with MakerTally.exe. SDK selected: 10.0.401, using the unchanged global.json policy (10.0.100 / latestFeature / no prereleases); MAUI package version remains 10.0.20.

The tests compiled. One test execution attempt was blocked before discovery with FileLoadException `0x800711C7` on MakerTally.Tests.dll. Attempts stopped. No security settings, trust certificates, exceptions or bypasses were used. Runtime migration and alpha4 visual validation remain pending; they are not inferred from build success.

No Android build, emulator, AVD, download or installation was performed. App still declares both Windows and Android by default. The WindowsOnly build property selects Windows only in App without changing Core's net10.0 target, avoiding Android workload restoration in CI.

## Repository and next step

Git is prepared locally on `main` with a clean initial commit after privacy and content review. Repository-local author identity uses the intended public handle and GitHub noreply email; global Git configuration is not modified. No LocalAppData files, generated outputs, local absolute user paths or credentials are tracked. Defaults and test fixtures remain intentional source examples.

GitHub CLI is installed and authenticated as `joseamc91`. After explicit authorization, private [joseamc91/MakerTally3D](https://github.com/joseamc91/MakerTally3D) was created and configured as origin. Main is the branch used for Windows CI. No Release or tag is created.

The reviewed workflow uses windows-latest, official actions pinned to commit SHAs, a read-only token, global.json, only maui-windows, mandatory non-empty/all-passing tests, build/publish, a full ZIP with SHA-256 and artifact upload. Triggered by manual dispatch, push main and pull_request main. Test results upload even on failure. Static YAML and embedded PowerShell validation are separate from actual execution; real results and logs are available in [Windows CI](https://github.com/joseamc91/MakerTally3D/actions/workflows/windows-ci.yml).

Pushing main triggers Windows CI; its result must be inspected before declaring tests or publication successful. It will name its build `MakerTally3D-v0.1-alpha4-win-x64-ci-<run_number>`. The user will download/run the artifact manually. No release or alpha5 is started.
