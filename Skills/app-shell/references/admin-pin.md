# Local Admin PIN

Paths use [common notation](../../repo-router/references/common.md). This is device-local protection for settings, not server authentication or reader accounts.

## Start narrow

- `${SRC}/AdminPinStore.kt`: `create`, `verify`, `change`, `checkPin`, `derive`.
- `${SRC}/AdminPinUi.kt`: `AdminPinDialog`, `ChangeAdminPinDialog`.
- `${SRC}/SettingsUi.kt`: `SettingsScreen` Admin gate and `AdminDialog` change-PIN entry.
- `${SRC}/SettingsDialog.kt` only if shared layout/inset behavior is implicated.

## Storage and failure boundaries

- PINs are 4–8 ASCII digits; retain leading zeros.
- Only the random salt and PBKDF2-HMAC-SHA256 hash are stored in the existing settings preferences. Do not store, log, export, or put raw PINs into saved UI state.
- `hasPin` treats either credential field as configured. Corrupt/partial credentials must fail closed, not silently allow replacement setup.
- Verification and change share failed-attempt state and persisted lockout. Five failures currently block access for 30 seconds.
- The store serializes operations with its shared lock and uses checked preference commits. Do not report success before persistence succeeds.
- `change` verifies the current PIN, rotates salt/hash, and restores prior credential/attempt state if a commit fails, including in-memory preferences. Preserve this failure behavior.
- UI derivation/storage work runs off the main thread. Preserve cancellation and prevent stale completion callbacks from unlocking a dismissed dialog.

## UI boundary

- `SettingsScreen` keeps Admin visibility/unlock in `remember`, not persistent unlock state. Leaving Settings, closing Admin, or recreating the screen requires another unlock.
- Entry fields are masked; dialogs use `SecureFlagPolicy.SecureOn`. Preserve focus, error semantics, busy-state protection, and clearing of temporary PIN values.
- A successful PIN change closes/relocks Admin. Cancelling must not alter credentials.
- Do not gate ordinary history/collection access behind Admin just because administrative controls share the same settings file.

## Verification

Use `AdminPinStoreTest` for validation, corruption, lockout, commit failure, and credential rotation; `AdminMenuTest` for the gate/recreation; `ChangeAdminPinDialogTest` for focus, dismissal, async failures, and relocking. The [test map](tests.md) links the shared harness.

Consult the affected feature only when changing what Admin controls, rather than its authentication mechanics. Keep dataset, reward artwork, and updater code out of PIN-only investigations.
