---
name: android-platform
description: Handle Android runtime lifecycle services, APK update/download/install behavior, local diagnostics, manifest permissions and launcher identity, Gradle dependencies and SDK configuration, signing compatibility, CI, and release workflows.
---

# Android platform and delivery

Resolve `ROOT` to the absolute checkout root; `SRC=${ROOT}/app/src/main/java/com/ssmath/app`.

For runtime work, start at `${SRC}/AppUpdater.kt`, `${SRC}/UpdateRelease.kt`, `${SRC}/DebugLog.kt`, or `${SRC}/UiActivityGate.kt`. For build work, start at `${ROOT}/app/build.gradle.kts` and only the affected workflow under `${ROOT}/.github/workflows`.

Load one guide initially:
- [Updates](references/updates.md): trust checks, lifecycle, download/install, update UI.
- [Diagnostics](references/diagnostics.md): logging, initialization, bounded storage/export.
- [Build/release](references/build-release.md): module/dependencies, manifest, SDK, signing, workflows.

Preserve installed package/signing identity, download verification, cancellation, and private bounded diagnostics. Do not load signing material or put credentials in context.

Consult [app-shell](../app-shell/SKILL.md) only for shared settings/layout integration; consult feature owners when a lifecycle or platform contract affects them. Reading-pack networking belongs to [reading-data](../reading-data/SKILL.md), not the APK updater.

Normally ignore gameplay engines, content packs/generators, reward drawings, and unrelated workflows. Use [targeted tests](references/tests.md) and [shared validation](../repo-router/references/validation.md) to choose runtime tests or debug/release checks. Do not publish releases as a side effect of validating code.
