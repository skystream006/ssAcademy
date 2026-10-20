---
name: app-shell
description: Handle ssAcademy home and subject navigation, shared Compose layouts/dialogs, themes, skins, text scaling, settings organization, and local Admin PIN creation, unlock, and change flows. Use for shared shell changes, not feature-specific gameplay rules.
---

# Shared app shell

Resolve `ROOT` to the absolute checkout root; `SRC=${ROOT}/app/src/main/java/com/ssmath/app`.

Start at the affected `MathAppContent`, `SettingsScreen`, `SettingsDialog`, `MathTheme`, or Admin PIN symbol under `${SRC}`. Do not read all of `MathUi.kt`, `MathViewModel.kt`, or `SettingsUi.kt` just because they mix domains.

Load only the relevant guide:
- [UI and navigation](references/ui-and-navigation.md): entry points, shared layout, overlays, appearance, preference helpers.
- [Admin PIN](references/admin-pin.md): local credential storage, dialogs, lockout, failure handling.

Preserve navigation return destinations, paused game state, accessible semantics/test tags, large-text layouts, and transient Admin unlock state. Feature-specific settings belong to their feature even when rendered in `SettingsUi.kt`.

Consult [math](../math/SKILL.md) or [reading](../reading/SKILL.md) only when session behavior changes, [rewards](../rewards/SKILL.md) for collection behavior, or [android-platform](../android-platform/SKILL.md) for lifecycle/manifest contracts.

Normally ignore datasets, SQLite internals, drawing families, APK networking, and workflows. Use [targeted tests](references/tests.md) to select the affected classes/methods and [shared validation](../repo-router/references/validation.md) for commands. For ownership ambiguity only, consult the router's [shared-symbol map](../repo-router/references/architecture.md).
