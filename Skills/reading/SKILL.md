---
name: reading
description: >-
  Use for reading-game rules, sessions, checkpoints, reader drafts, reading UI,
  history review, offline narration, or reading-specific settings. Excludes
  catalogue generation, pack import, and persistence implementation.
---

# Reading runtime

Route by behavior; do not load every reference.

- For scoring, session flow, drafts, timers, UI, review, speech, or reading
  settings, open [sessions and UI](references/sessions-and-ui.md).
  Start at `${SRC}/ReadingModel.kt` (`ReadingEngine`) and
  `${SRC}/ReadingStore.kt` (`ReadingViewModel` only); for presentation,
  start at `${SRC}/ReadingScreen.kt`, `${SRC}/ReadingReview.kt`, or
  `${SRC}/OfflineReadingSpeaker.kt`.
- When selecting verification, open [tests](references/tests.md). Choose the
  smallest behavior-specific methods/classes, not the whole reading suite.
  Use [shared validation](../repo-router/references/validation.md) only for
  execution syntax and environment requirements.

Source notation is `${SRC}/File.kt` plus a symbol. If root variables are not
established, consult [common conventions](../repo-router/references/common.md);
`${ROOT}` is the absolute checkout root, never an assumed working directory.

Preserve attempt IDs, assigned content revisions, first-try attribution, and
the single `ReadingViewModel` write actor. Checkpoints do **not** award reward
points. Keep saved reviews read-only and failed edits retryable.

Escalate to [reading-data](../reading-data/SKILL.md) only when content contracts,
allocation, storage, imports, or generated data change; it owns `ReadingStorage`
and legacy `ReadingStore` in the shared file. Use [rewards](../rewards/SKILL.md)
for session receipts, claims, and durable deletion reconciliation, and
[app-shell](../app-shell/SKILL.md) for shared navigation/settings structure. Pure reading
presentation changes need neither database nor generator references.
