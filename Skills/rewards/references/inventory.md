# Inventory and reward receipts

Use for accounting, eligibility, settings, claims, or reward-linked deletion.
Paths use `SRC`/`TEST` from the Skill; [common conventions](../../repo-router/references/common.md)
are optional setup, not a prerequisite to every lookup. Skip all drawing files.

## Direct discovery

Search the named symbols in these exact files before widening scope:

| File under `${SRC}` | Entry symbols / responsibility |
| --- | --- |
| `Rewards.kt` | `RewardTier`, `RewardThresholds`, `RewardType`, `RewardBalance`, `PrizeAward`, `qualifiesForReward`, `completionSlotReward`, `SlotMachineRewardPool`. |
| `RewardSettings.kt` | `MathRewardGame`, `readReadingRewardsEnabled`, `readRewardThresholds`, `readMatrixRewardTier`. |
| `HistoryStore.kt` | `PracticeResult`, `ReadingRewardResult`, `PracticeSnapshot`, `HistoryStore`; selection receipts and inventory persistence. |
| `MathViewModel.kt` | `finish`, `persistResult`, `claimReward`, `useReward`, `adjustReward`, `applySnapshot`, `storeLock`; inspect only these portions. |
| `MathViewModel.kt` | `prepareReadingReward`, `persistReadingResult`, `claimReadingReward`, `deleteReadingResults`; Reading receipt bridge. |
| `MathViewModel.kt` | `deleteResult`, `clearHistory`, `removeDeletedResultReward`, `chooseRewardThresholds`, `chooseMatrixRewardTier`, reward-enabled setters. |
| `MathViewModel.kt` / `SlotMachine.kt` | `spinSlotMachine`, `retrySlotMachinePrize`, `SlotMachineDialog`, `SlotMachineAvailableRewards`; activity and Admin slots. |
| `RewardUi.kt` | Both `RewardGiftDialog` overloads, `RewardInventory`, `UseRewardsDialog`, `RewardImage`, `rewardImageDescription`. |
| `MathUi.kt` | `RewardsQuestionCountChoices`, `RewardsCriteria`; shared question choices and eligibility display. |
| `SettingsUi.kt` | `SubjectRewardsSetting`, `GameRewardsSetting`, `RewardMinimumSettings`, `AddRemoveRewardsDialog`, `RewardsScreen`. |

## Eligibility and balance invariants

- Qualify only after all questions are answered, without timeout, with at least
  90% correct; compare exact integer ratios rather than rounded display percentages.
- Check earning at both start and finish, including subject and per-game switches.
  Disabling earning does not erase inventory or block an already-earned claim/use.
- Ordinary tiers use `RewardThresholds`; finish-time rules are stored with results.
  Thresholds are ordered values in `1..1000`, defaulting to 25 and 50.
- Matrix qualification uses its stored selected tier and exact matrix question
  count, not ordinary minima; other completion/accuracy/availability guards remain.
- New completions persist `slotReward` with the eligible tier's enabled positive
  reel weights, not a randomly selected `prizeType`. An empty tier earns no slot.
  Later settings changes must not rewrite an earned pool or fall back to another tier.
  Persist the pool's earned tier independently of current reward categories.
- Activity slots allow free pulls until one three-reel match adds one fragment,
  atomically setting `prizeType`, `prize`, and claim metadata. Do not allow tier/mode
  changes or another win. Failed winning saves retry the same roll. Unclaimed slots
  resume from history; legacy gifts keep their original direct fragment claim path.
- Wheel, Claw, and Plinko rounds continue until one reward has three completed
  outcomes, not a fixed number of turns or a majority vote. Freeze the pool for the
  round and each turn's selected reward, aim, and trajectory, including motion retries.
  Plinko accepts overlapping drops and aiming until the first three-ball bin wins.
  Count landings in time order; later in-flight balls finish without changing the winner.
  Save one prize only after animations finish, and retry that same roll after save errors.
  Claw ball IDs cycle through the frozen pool's enabled prizes; rendering and pickup
  awards share `SlotMachineRewardPool.clawBallReward`. Each prize has a stable color
  and a visible color key, not a hidden weighted draw. Wheel and Plinko retain weighted draws.
  Claw descent and lift keep the player's horizontal aim; only an in-reach ball can
  be grabbed. Misses retain a turn/path with a null draw and never increment a tally.
- Display effective per-reel, per-pull (`p³`), and share-of-wins (`p³ / sum(p³)`)
  chances from the same pool used to draw reels, on both slot and Admin screens.
  The separate PIN-protected Admin slot retains whole/fragment mode and repeat wins.
  Admin matches atomically add a standard animation, plus a Mega for Tier 3.
  Store their independent ownership in `slotMachinePokemons`; do not attach fake game receipts.
- A claim adds one fragment; three fragments carry into one whole reward.
  `RewardBalance` rejects invalid/overflowing balances and retains 0–2 fragments.
- Using inventory consumes one whole. Admin removal consumes only the requested
  available unit; it does not break a whole to supply a missing loose fragment.
- Gift/slot win presentation waits for durable `PrizeAward`, not optimistic animation state.
  Preserve busy gates, request versions, explicit dismissal, and retry behavior.
  Opening a Reading reward defers the Math dialog without removing its earned history slot.

## Persisted compatibility and atomicity

- Preserve serialized `RewardType`, `RewardTier`, `Celebration`, and game names;
  labels are not replacement storage identifiers.
- `PracticeResult.gameMode` defaults to legacy `REWARDS`, not `PRACTICE`.
  Retain `prizeType`, `prize`, `rewardThresholds`, `matrixRewardTier`,
  `pokemonReward`, `megaPokemonReward`, and gameplay identity/answer metadata.
  `slotReward` defaults to null for legacy Math results, Reading sessions, and receipts.
- Keep all `PracticeSnapshot` fields: `history`, `rewards`, `claimedResultIds`,
  `lastResultId`, `pokemons`, `readingResults`, `recordedReadingSessionIds`,
  `importedPokemons`, and `slotMachinePokemons`.
- `ReadingRewardResult` is a session receipt, never a Math history result.
  Both subjects claim into the same `rewards` map and collection.
- `HistoryStore` is not thread-safe. Serialize loads, ID allocation, claims,
  collections, adjustments, and deletions under `MathViewModel.storeLock`.
- `addNewResult` allocates monotonic IDs despite clock rollback; retain allocation
  and claim metadata through clear/reload. Never recycle anti-reclaim IDs.
- `save` atomically replaces the destination using an adjacent temporary file.
  Apply UI snapshots only after success; failed writes remain retryable.
- Legacy history arrays remain readable. A malformed inventory object must fail,
  not silently become an empty replacement snapshot.

## Server sync

Collection sync exchanges only `CollectionBackup` (balances and collected animations).
Use `MathViewModel.applySnapshot` after a durable save to signal collection changes.
Network I/O runs outside `storeLock`; downloads recheck the local snapshot under that
lock before calling `HistoryStore.importCollections`. Store the account's upload receipt ID
and comparison digest separately from practice history. Read owned imports by ID, not by
listing; a duplicate receipt may return admin-edited values. Never automatically merge balances.

## Deletion is accounting, not retention

- Explicit deletion/clear subtracts one fragment per claimed removed result,
  unpacking whole rewards as necessary and clamping spent balances at zero.
  Never restore an old `PrizeAward.balance` or deduct unclaimed prizes.
- Remove a linked celebration only when no remaining Math or Reading receipt
  owns it; keep unrelated/unlinked entries. Automatic Math retention does not
  revoke inventory. Reading receipts are not subject to Math's retention cap.
- Preserve `claimedResultIds` and `recordedReadingSessionIds` after deletion.
  Durable Reading `deletedSessionIds` drive cleanup retry after failure/restart.

## Consult another owner only when the contract crosses it

- Math completion/scoring/timing: consult [math](../../math/SKILL.md) before changing `finish`
  inputs or `${SRC}/MathGame.kt`; keep practice/challenge/matrix identity intact.
- Reading completion: consult [reading](../../reading/SKILL.md) for `${SRC}/ReadingModel.kt`
  `ReadingEngine.endSession` and `${SRC}/ReadingScreen.kt` completion callbacks.
  Copy finished-session rules; never award checkpoint points or reinterpret legacy flags.
- Reading enum retirement: consult [reading-data](../../reading-data/SKILL.md) for `${SRC}/ReadingMigration.kt`;
  `HistoryStore.loadSnapshot` must retire receipts before enum decoding, subtract
  linked awards once, preserve anti-reclaim IDs, and still reject unknown corruption.
- Shell navigation: consult [app-shell](../../app-shell/SKILL.md) before changing overlay return paths,
  foreground gates, or `${SRC}/MathUi.kt` deletion-cleanup wiring.

Choose focused coverage from [tests](tests.md); consult [celebrations](celebrations.md)
only if selection, live collection, or shared collection ownership changes.
