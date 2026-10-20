# Shell test selection

Tests live under `${TEST}` as defined in [common](../../repo-router/references/common.md). Select a row; do not run/load every class in this table by default. Commands and harness conventions are in [shared validation](../../repo-router/references/validation.md).

| Changed behavior | Initial tests | Expand when |
| --- | --- | --- |
| Subject/home navigation | `SubjectNavigationTest` and matching `MathAppTest` methods | Return-to-Reading changes: `ReadingEntryTest`; gameplay Back changes: relevant `GameModesTest` |
| Collection shortcut text/layout | `MathAppTest.homeCollectionButtonsShowImagesBeforeLabelsAtEveryTextSize`, `MathAppTest.homeButtonsAndSetupRemainUsableWithLargeTextOnSmallScreens` | Collection behavior changes: rewards test map |
| Shared dialog/insets | `SettingsDialogTest` | Add affected dialog consumer methods, including keyboard/large-text cases |
| Theme, skin, text scaling | `SkinsTest`, matching `MathAppTest` appearance methods | Check changed primitive consumers in reading/reward UI, not their whole engines |
| Settings organization/expansion | Relevant `RewardsSettingsTest` or `PokemonSettingsTest` methods | Gate behavior changes: `AdminMenuTest` |
| PIN storage | `AdminPinStoreTest` | Store/UI handoff changes: `AdminMenuTest`, `ChangeAdminPinDialogTest` |
| PIN dialogs/gate | `AdminMenuTest`, `ChangeAdminPinDialogTest` affected methods | Credential persistence changes: `AdminPinStoreTest` |
| Time preference migration/options | Time-limit methods in `RewardsModelTest`, `RewardsSettingsTest` | Runtime math timer: math tests; reading launch/reload: reading tests |

Shared helpers: `${TEST}/SettingsTestNavigation.kt` defines `openAdminSettings`, `openGameSettings`, and `openDebugSettings`. Read the helper instead of reproducing its click sequence.

UI semantics/test tags usually identify tests faster than filename prefixes. Use NATIVE graphics for text overflow/pixel checks, following existing tests; retain small-screen and enlarged-text coverage. For cross-domain shared primitives, add only the impacted consumers' regression cases before broader Android validation.
