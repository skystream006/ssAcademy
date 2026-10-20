---
name: reading-data
description: >-
  Use for reading content schemas, SQLite storage, migrations, activity
  allocation, pack imports, import settings, database browsing, or generated
  reading datasets and manifests. Excludes reading engine/session rules and
  play/review UI.
---

# Reading data

Choose one entry reference; do not preload storage and generation together.

- For ledgers, migration, rotation/allocation, assigned revisions, or database
  browsing, open [storage](references/storage.md). Start at
  `${SRC}/ReadingSqliteStore.kt` and `${SRC}/ReadingDatabase.kt`; inspect
  `ReadingStorage` and legacy `ReadingStore` in `${SRC}/ReadingStore.kt` only
  for their contracts.
- For schemas, downloads/import controls, generation, or published manifests,
  open [packs](references/packs.md). Start at `${SRC}/ReadingContent.kt`
  (`ReadingActivity`), `${SRC}/ReadingPack.kt`, or `${DATA}/generate.py`,
  according to the change.
- For verification, choose [targeted tests](references/tests.md), then consult
  [shared validation](../repo-router/references/validation.md) only for execution
  syntax, versions, and Python standard-library commands.

Source notation is `${SRC}/File.kt` plus symbols, or `${DATA}/file`.
Use [common conventions](../repo-router/references/common.md) if absolute checkout
root variables are not already established.

Preserve attempt IDs, assigned content revisions, transactional reservations,
non-destructive upgrades, durable deletion receipts, and immutable published
compatibility packs. Generate current content from its sources; do not patch
checksum-named payloads manually.

[Reading](../reading/SKILL.md) owns `ReadingEngine`, `ReadingViewModel` in the
shared store file, session rules, and play/review UI. Consult it only when those
contracts change. Use [rewards](../rewards/SKILL.md) for shared session receipt
reconciliation and [app-shell](../app-shell/SKILL.md) for shared navigation, not data-only
implementation. Checkpoints are not reward points.
