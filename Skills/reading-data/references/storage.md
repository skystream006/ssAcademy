# Reading storage, allocation, and database browsing

Use this reference without loading generation details unless the content/import
contract changes. Source notation is `${SRC}/File.kt` plus symbols;
[common conventions](../../repo-router/references/common.md) defines root variables.

## First sources and ownership

| Change | First source |
| --- | --- |
| Storage contract / JSON compatibility | `${SRC}/ReadingStore.kt`: `ReadingStorage`, legacy `ReadingStore` only |
| SQLite progress and allocations | `${SRC}/ReadingSqliteStore.kt`: `load`, `save`, `startSession`, `prepare`, `review` |
| Schema and catalogue queries | `${SRC}/ReadingDatabase.kt`: `ReadingDatabase`, `ReadingCatalog` |
| Retired serialized content | `${SRC}/ReadingMigration.kt`: `ReadingMigration.migrate` |
| Content shape / fallback | `${SRC}/ReadingContent.kt`: `ReadingActivity`, `ReadingContent` |
| Installed catalogue inspection | `${SRC}/ReadingDatabaseBrowser.kt`: `ReadingDatabaseReader`, `ReadingDatabaseBrowser` |

`ReadingViewModel` shares `${SRC}/ReadingStore.kt` but belongs to
[reading](../../reading/SKILL.md). It is the single Activity-scoped write actor;
storage work must not add a competing writer. The domain models/engine in
`${SRC}/ReadingModel.kt` are contracts to consult, not data-owned implementations.

## Identity and transaction invariants

- `reading_content` stores immutable assignments by `(id, revision)`;
  `reading_current` selects the current installed revision for new allocations.
  A content ID cannot move between game/level pools, including archived IDs.
- Preserve `ReadingAttempt.id`, `contentId`, and `ReadingActivityRef.revision`.
  Review/resume resolves the assigned revision, never `catalog.latest`.
  Canonical serialized content determines revisions; import formatting alone
  must not create a new one.
- `ReadingSqliteStore.startSession` allocates the full batch, records presentation,
  validates the ledger, and writes state in one transaction. A failed save must
  roll back attempts, reservations, and cycle changes together.
- Reject stale allocation inputs and stale `save` snapshots; keep `expectedState`
  checks. Require a successful load before saving/starting.
- Rotation is per reader/game/level. Exhaust unseen activities before starting
  another cycle; avoid the prior boundary item where alternatives exist.
  Resume does not allocate again. Imports/deletions must not reset rotation.
- Each allocation has a new attempt ID even when a one-item pool repeats content.
  Do not conflate content identity with a student's attempt or session identity.

## Bounded state and read-only review

`reading_state` contains the header/session state; `reading_attempts` and
`reading_stories` hold ordered, content-free history rows. Preserve row positions.
`writeRows` bounds each encoded history record at 256 KiB. `readState` can recover
oversized version-one ledgers in chunks before normalizing them.
`prepare` hydrates only the selected active attempt and clears other transient
`loadedContent` payloads. `review` resolves a requested attempt without changing
active pointers, reservations, scores, or persistent state.

The database browser reads only installed current rows, not `ReadingContent`
fallback. `ReadingCatalog.browse` uses stable ID order, bounded pages, and optional
game/level filters. The UI requests 40-row pages and carries the selected revision
into detail. `ReadingDatabaseReader` owns/closes each request's connection on IO;
never close the live game store's database or mutate progress while browsing.

## Compatibility and failure handling

- Fresh installs have no selectable library until a complete import succeeds.
  `ReadingContent` retains originals to resolve pre-database saves; migrating
  legacy assignments into `reading_content` does not install selectable content.
- Legacy `ReadingStore` uses `AtomicFile`, validates before writes, recovers the
  committed backup, and refuses to silently replace malformed/unsupported data.
  `ReadingSqliteStore.load` imports legacy `reading_private.json` when needed;
  a broken legacy file must not produce a replacement empty ledger.
- Database schema is currently version 4. `onUpgrade` explicitly handles prior
  versions 1–3; unsupported upgrades require a real non-destructive migration,
  not dropping/recreating the database.
- `ReadingMigration.migrate` removes retired CHAPTER/PARTNERS/CAPTION games and
  their obsolete fields before enum decoding. Preserve surviving readers,
  history, stories, drafts, revisions, and cycles; do not turn unrelated
  corruption into silent data loss. Existing upgrades remove only retired data
  and retain revision IDs when removing the obsolete content `branches` field.
- Deleting a session member deletes that whole session. Persist
  `ReadingData.deletedSessionIds` in JSON/SQLite; migration also records removed
  sessions. These durable receipts let shared rewards cleanup retry after
  failure/restart. Do not clear them, resurrect IDs, or substitute checkpoint
  reward accounting. Coordinate receipt changes with
  [rewards](../../rewards/SKILL.md) and engine changes with [reading](../../reading/SKILL.md).

## Verification and narrow handoffs

Start with [storage test selectors](tests.md), especially rollback, reopen,
revision preservation, and deletion recovery for the changed path.
Use [packs](packs.md) only for import staging, schema validation, generated
content, or published compatibility changes. Import settings belong there.
Use [app-shell](../../app-shell/SKILL.md) only when changing how Debug Menu opens the
browser or other shared navigation, not for a catalogue query change.
