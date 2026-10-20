# Inventory and reward receipts

Use for accounting, eligibility, settings, claims, or reward-linked deletion.
Paths use `SRC`/`TEST` from the Skill; [common conventions](../../repo-router/references/common.md)
are optional setup, not a prerequisite to every lookup. Skip all drawing files.

## Direct discovery

Search the named symbols in these exact files before widening scope:

| File under `${SRC}` | Entry symbols / responsibility |
| --- | --- |
| `Rewards.kt` | `RewardTier`, `RewardThresholds`, `RewardType`, `RewardBalance`, `PrizeAward`, `qualifiesForReward`, `selectPrize`. |
| `RewardSettings.kt` | `MathRewardGame`, `readReadingRewardsEnabled`, `readRewardThresholds`, `readMatrixRewardTier`. |
| `HistoryStore.kt` | `PracticeResult`, `ReadingRewardResult`, `PracticeSnapshot`, `HistoryStore`; selection receipts and inventory persistence. |
| `MathViewModel.kt` | `finish`, `persistResult`, `claimReward`, `useReward`, `adjustReward`, `applySnapshot`, `storeLock`; inspect only these portions. |
| `MathViewModel.kt` | `prepareReadingReward`, `persistReadingResult`, `claimReadingReward`, `deleteReadingResults`; Reading receipt bridge. |
| `MathViewModel.kt` | `deleteResult`, `clearHistory`, `removeDeletedResultReward`, `chooseRewardThresholds`, `chooseMatrixRewardTier`, reward-enabled setters. |
| `RewardUi.kt` | Both `RewardGiftDialog` overloads, `RewardInventory`, `UseRewardsDialog`, `RewardImage`, `rewardImageDescription`. |
| `MathUi.kt` | `RewardsQuestionCountChoices`, `RewardsCriteria`; shared question choices and eligibility display. |
| `SettingsUi.kt` | `SubjectRewardsSetting`, `GameRewardsSetting`, `RewardMinimumSettings`, `AddRemoveRewardsDialog`, `RewardsScreen`. |

## Eligibility and balance invariants

- Qualify only after all questions are answered, without timeout, with strictly
  greater than 90% correct; do not round displayed percentages into eligibility.
- Check earning at both start and finish, including subject and per-game switches.
  Disabling earning does not erase inventory or block an already-earned claim/use.
- Ordinary tiers use `RewardThresholds`; finish-time rules are stored with results.
  Thresholds are ordered values in `1..1000`, defaulting to 25 and 50.
- Matrix qualification uses its stored selected tier and exact matrix question
  count, not ordinary minima; other completion/accuracy/availability guards remain.
- Select only enabled reward types in the eligible tier. An empty tier grants
  nothing; never fall back to another tier or reroll a saved prize on retry.
- A claim adds one fragment; three fragments carry into one whole reward.
  `RewardBalance` rejects invalid/overflowing balances and retains 0–2 fragments.
- Using inventory consumes one whole. Admin removal consumes only the requested
  available unit; it does not break a whole to supply a missing loose fragment.
- Gift opening waits for durable `PrizeAward`, not optimistic animation state.
  Preserve busy gates, request versions, explicit dismissal, and retry behavior.

## Persisted compatibility and atomicity

- Preserve serialized `RewardType`, `RewardTier`, `Celebration`, and game names;
  labels are not replacement storage identifiers.
- `PracticeResult.gameMode` defaults to legacy `REWARDS`, not `PRACTICE`.
  Retain `prizeType`, `prize`, `rewardThresholds`, `matrixRewardTier`,
  `pokemonReward`, `megaPokemonReward`, and gameplay identity/answer metadata.
- Keep all `PracticeSnapshot` fields: `history`, `rewards`, `claimedResultIds`,
  `lastResultId`, `pokemons`, `readingResults`, `recordedReadingSessionIds`.
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
