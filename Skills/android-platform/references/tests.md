# Platform test selection

Tests live in `${TEST}`; see [path notation](../../repo-router/references/common.md). Use [shared validation](../../repo-router/references/validation.md) for commands, prerequisites, and broader checks.

| Change | Initial coverage | Additional validation |
| --- | --- | --- |
| Versions/release JSON/URL allowlisting | `AppUpdaterTest` matching methods | Check release workflow naming if the contract changes |
| APK stream limits/cancellation/partial cleanup | `AppUpdaterTest.streamedDownloadReportsProgressAndRetainsOnlyCompleteBytes`, `truncatedOrOversizedDownloadDeletesPartialFile`, `cancellingStreamClosesInputAndDeletesPartialFile` | Actual network/lifecycle state changes need focused new regression coverage |
| Update settings layout | `MathAppTest.appUpdatesAppearFirstWithTheCheckButtonBesideTheDetails`, `appUpdateDetailsAndCheckButtonFitWithLargeTextOnNarrowScreens` | App-shell tests only if a shared primitive changes |
| Package display/identity | `MathAppTest.appLabelAndSettingsShowSsAcademyWithoutChangingInstalledIdentity` | Build/manifest verification and in-place install checks as appropriate |
| Logging storage/privacy | `DebugLogTest` | Affected `MathAppTest` logging/settings methods when UI changes |
| Lifecycle gate/installer/permission/signer behavior | Identify/add focused regression coverage for the changed helper or state transition | Device/emulator checks for Android-specific behavior not covered by existing pure helper tests |
| Gradle/plugin/dependency/SDK configuration | Full debug unit tests, lint, assemble | Release tasks for release/signing configuration; no publication during validation |
| Workflow-only changes | Review event, permissions, checkout, command, artifact, and secret boundaries | Run affected existing build commands when feasible; inspect the resulting CI run rather than claiming unexecuted workflow success |

`AppUpdaterTest` is a pure helper test class, not an end-to-end installer suite. No separate `UiActivityGateTest` is currently present; do not claim coverage that does not exist. Existing Robolectric tests commonly use plain `Application`, so they do not automatically exercise `MathApplication` lifecycle callbacks.

Do not make tests depend on live GitHub or real credentials. Keep failure/cancellation cases, narrow provider paths, privacy assertions, and signature/identity checks intact.
