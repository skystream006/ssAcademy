---
name: rewards
description: >-
  Use for prize eligibility, reward thresholds and settings, fragment inventory,
  claims, reward-linked history deletion, reading reward receipts, gift dialogs,
  celebration selection or collection, Pokémon galleries, and Kanto/Mega artwork.
---

# Rewards

Own shared reward accounting, persistence, presentation, and collections.
Start with the reported behavior and its exact symbol, not whole shared files.
Reward-related portions of `MathViewModel.kt`, `MathUi.kt`, and `SettingsUi.kt`
belong here; unrelated gameplay and shell behavior do not.

`ROOT` is the absolute checkout root, `SRC=${ROOT}/app/src/main/java/com/ssmath/app`, and `TEST=${ROOT}/app/src/test/java/com/ssmath/app`; optional setup is in [common conventions](../repo-router/references/common.md).

## Load only what the task needs

- [Inventory](references/inventory.md): eligibility, settings, balances, claims,
  durable snapshots, deletion, and reading receipt orchestration.
- [Celebrations](references/celebrations.md): selection, live collection,
  replay, matrix-only Mega queues, gallery behavior, and exact drawing families.
- [Tests](references/tests.md): choose an existing focused class or method after
  identifying the changed contract; shared commands live in
  [validation](../repo-router/references/validation.md).

For accounting-only work, skip drawing implementations. For artwork, follow the
dispatcher into only the affected family. Consult neighboring owners only when
changing completion, reading-data migration, or shell navigation contracts.

Preserve persisted enum names, legacy `PracticeResult` defaults, snapshot fields,
anti-reclaim IDs, deletion accounting, and caller-mutex-protected atomic saves.
Completed Math and Reading session prizes share inventory; checkpoints award no
points. Collect gameplay celebrations only when presented live, never on replay.

Documentation-only edits need link, path, and symbol checks, not app tests.
