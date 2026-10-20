# APK updates and lifecycle services

Paths use [common notation](../../repo-router/references/common.md).

## First files and symbols

| Concern | Entry |
| --- | --- |
| Startup and settings presentation | `${SRC}/UpdateUi.kt`: `UpdateNotification`, `UpdateSettings`; call sites in `${SRC}/MathUi.kt` and `${SRC}/SettingsUi.kt` |
| State/check/download/install | `${SRC}/AppUpdater.kt`: `UpdateState`, `check`, `download`, permission/installer methods |
| Metadata and trust policy | `${SRC}/UpdateRelease.kt`: `UPDATE_ENDPOINT`, `parseUpdateRelease`, `trustedUpdateUrl`, `UpdateVersion` |
| Bounded stream/cancellation | `${SRC}/UpdateRelease.kt`: `awaitResponse`, `saveUpdateApk`; `${SRC}/AppUpdater.kt`: request/download helpers |
| Package/version/signer checks | `${SRC}/AppUpdater.kt`: `validateUpdateIdentity`, `currentSigners`, `packageVersionCode` |
| Foreground work | `${SRC}/UiActivityGate.kt`: `onceWhileResumed`, `readWhileResumed`; `${SRC}/MathApplication.kt`: lifecycle callbacks |
| Installation plumbing | `${SRC}/AppUpdater.kt`: `updateInstallIntent`, `updatePermissionIntent`; `${ROOT}/app/src/main/AndroidManifest.xml`; `${RES}/xml/update_paths.xml` |

## Trust and behavior invariants

- The updater uses this repository's stable latest GitHub release, not Actions artifacts. Metadata must identify exactly one correctly named universal APK.
- Compare versions numerically, including zero-padded components. Check downloaded package identity, matching newer version, and signing certificate before installation.
- Preserve HTTPS host/path restrictions and the distinction between initial release URLs and allowed asset redirects. Do not replace the explicit redirect checks with unrestricted automatic redirects.
- The APK client uses system TLS roots, excluding user-installed roots. Reading-pack transport has a different owner/policy; do not consolidate them casually.
- Metadata and APK streams have size caps and exact expected-byte checks. Preserve cancellation, partial-file cleanup, progress, and validation before promoting a temporary APK.
- Startup checks are automatic while resumed; downloads still require user confirmation. Do not describe all updater network traffic as manually initiated.
- `UiActivityGate` requires at least one resumed Activity, not just visibility. Only restart operations with no externally visible partial side effects through `readWhileResumed`.
- The application registers callbacks in `MathApplication`; production lifecycle behavior may differ from tests configured with plain `Application`.
- Recreating the updater discards cached trust/download state. Do not trust an old cache file merely because it exists.
- Installation uses a narrow FileProvider cache path and a read grant. Android permission and final installer approval remain explicit.
- Preserve in-place upgrade compatibility; never suggest uninstalling to bypass signature validation.

## Scope and verification

For release artifact naming/signing changes, consult [build-release](build-release.md); for events, load [diagnostics](diagnostics.md) only if their contract changes. Shared settings layout belongs to app-shell.

Use [platform tests](tests.md). `AppUpdaterTest` covers pure metadata/stream helpers; do not claim it proves device installer, certificate APIs, or lifecycle integration. Add appropriate focused coverage and verify those paths on a device/emulator when changing them. Do not perform a live release/install as part of unrelated documentation work.
