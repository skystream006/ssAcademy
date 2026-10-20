# Local diagnostics

Paths use [common notation](../../repo-router/references/common.md). Load this independently of build/signing and updater implementation.

## Entry points

- `${SRC}/DebugLog.kt`: `DebugEvent`, `DebugLogMode`, `DebugLog`, `DebugLogStore`.
- `${SRC}/MathApplication.kt`: initialization and application-start event.
- `${SRC}/DebugLogUi.kt`: logging settings, reading/clearing/sharing.
- `${SRC}/SettingsUi.kt`: `DebugDialog` and its entry; inspect only the relevant diagnostic controls.
- Event call sites in `${SRC}/MathViewModel.kt` and `${SRC}/AppUpdater.kt` only when altering event emission.

## Invariants

- Logging is off by default. Settings use a separate `debug_logging` preference store and files live in no-backup storage.
- Accepted payloads are enum labels, numeric status, and sanitized exception class names. Do not log arbitrary throwable strings, names, answers, stories, PINs, URLs with credentials, or other private content.
- The bounded single-thread executor discards old queued work rather than blocking UI. File rotation and export also have explicit limits.
- FULL mode retains bounded current/previous files; REACTIVE mode retains the latest bounded event list. Keep current constants authoritative rather than duplicating limits in callers.
- A generation guard prevents queued writes after disabling/clearing/reconfiguring logging. Preserve synchronization and stale-work rejection.
- Crash logging freezes after the crash event and delegates to the previous uncaught exception handler. Do not swallow the crash or replace normal Android handling.
- Share uses an explicit text intent with a bounded export, not a remote telemetry upload. Clearing logs must not silently change opt-in preferences.

## Tests and adjacent domains

Start with `${TEST}/DebugLogTest.kt` for opt-in persistence, privacy, rotation, reactive bounds, share metadata, and clearing. Select affected UI methods in `${TEST}/MathAppTest.kt` when controls change; existing navigation helpers are in `${TEST}/SettingsTestNavigation.kt`.

See [validation](../../repo-router/references/validation.md) for commands. Consult the event-producing feature only if its lifecycle changes; use app-shell for shared dialog/setting layout. A diagnostics change should not load signing workflows, datasets, or reward drawing code.
