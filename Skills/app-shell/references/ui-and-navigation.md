# Shared UI and navigation

Paths use [common notation](../../repo-router/references/common.md). Read the relevant row, then its source symbol and immediate callers.

## Entry points

| Concern | Start here |
| --- | --- |
| Activity/content setup | `${SRC}/MainActivity.kt`: `onCreate`; `${SRC}/MathUi.kt`: `MathApp`, `MathAppContent` |
| Home/subjects/collection shortcuts | `${SRC}/MathUi.kt`: `HomeScreen`, `SubjectGrid`, `SubjectButton`, `CollectionButtons` |
| Screen and Back routing | `${SRC}/MathViewModel.kt`: `Screen`, `SetupDialog`, `Overlay`, `openSettings`, `openRewards`, `openPokemons`, `closeOverlay`; `${SRC}/MathUi.kt`: `MainContent` |
| Settings organization | `${SRC}/SettingsUi.kt`: `SettingsScreen`, `ScreenScaffold`, `CollapsibleSettingsSection`, `AdminDialog` |
| Full-screen dialogs and insets | `${SRC}/SettingsDialog.kt`: `SettingsDialog` |
| Themes and text scale | `${SRC}/Theme.kt`: `MathTheme`, `isDarkTheme`, text-size constants; appearance setters in `${SRC}/MathViewModel.kt` |
| Offline skins and subject images | `${SRC}/Skins.kt`; `${RES}/drawable/skin_*.xml`, `${RES}/drawable/subject_*.xml`; skin controls in `${SRC}/SettingsUi.kt` |
| Shared time preference normalization/migration | `${SRC}/TimeLimitSettings.kt`: `normalizeTimeLimitSeconds`, `readTimeLimitSeconds`, `readReadingTimeLimitSeconds` |

## Invariants

- Navigation is explicit state, not a navigation framework. Closing a nested history detail differs from closing its overlay. Preserve existing Back/cancel behavior and setup choices.
- Collection navigation records the actual originating overlay, including Reading; opening one collection from another must not overwrite that return destination. Settings also has its own return overlay.
- Overlays preserve the underlying math game and pause its timer. Math owns whether background time counts. Reading owns its visibility/lifecycle timer behavior.
- `MathAppContent` connects persisted reading deletion receipts to shared reward cleanup. Moving composition or navigation must not accidentally remove that effect.
- Use `SettingsDialog` for existing settings/setup patterns: fixed heading/actions, scrollable body, safe drawing and IME padding. Do not replace it with a clipped fixed-height dialog.
- `MathTheme` scales typography with the app text-size preference; retain device font scaling too. Test narrow screens, orientation, and enlarged text when relevant.
- `CollectionButtons` measures the wider label plus image/spacing/padding for a shared minimum width. Keep label intrinsic width so weighted FlowRow children do not clip text.
- Retain content descriptions, roles, selected/state semantics, stable test tags, and usable touch targets. Decorative images should not duplicate spoken labels.
- Persist appearance choices through the existing ViewModel setters and SharedPreferences keys. Do not reset them when entering another subject.
- Time preferences normalize seconds and fall back from legacy minutes. Reading snapshots a former shared preference once into its independent setting. Runtime scaling belongs to the math/reading owners, not this helper.
- Science and Advanced currently show placeholder feedback; do not treat them as implemented engines.

## Escalation and validation

Changing only a shared layout does not require reading every feature. If a changed primitive has other consumers, inspect those call sites and their focused UI tests.

Use [shell tests](tests.md). Consult [math](../../math/SKILL.md) for `MainContent` gameplay/ready state, [reading](../../reading/SKILL.md) for reader/session controls, [rewards](../../rewards/SKILL.md) for gift/collection behavior, and [platform](../../android-platform/SKILL.md) for updater/diagnostic implementations. Local Admin protection is covered separately in [admin-pin](admin-pin.md).
