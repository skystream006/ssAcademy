# Focused rewards test map

All classes below are in `${TEST}/<Class>.kt`, package `com.ssmath.app`.
Use [shared validation](../../repo-router/references/validation.md) for setup and
test-command syntax; do not duplicate that recipe here.

Start with one closest class or one `Class.method` selector, not this entire map.
Expand only when a changed contract crosses layers; batch those selectors in one
runner invocation. Read the target test before changing expectations.
For documentation-only changes, verify links/paths/symbols; do not run app tests.

## Eligibility and arithmetic

| Contract | Existing focused selector |
| --- | --- |
| Tier boundaries | `RewardsTest.configurableThresholdsValidateAndMapEveryBoundary` |
| Strict accuracy | `RewardsTest.accuracyMustBeStrictlyOverNinetyPercentWithoutRounding` |
| Matrix tier exception | `RewardsTest.selectedMatrixTierIgnoresMinimaButNeverQualificationOrAvailability` |
| Empty enabled tier | `RewardsTest.disablingAnEntireTierGivesNoPrizeAndNeverFallsBackToAnotherTier` |
| Fragment carry | `RewardsTest.everyThirdFragmentBecomesAWholeRewardWithZeroRemainder` |
| Balance overflow | `RewardsTest.additionLimitsAllowSafeCarriesAndRejectOverflowWithoutChangingBalances` |

## Persistence, deletion, and shared ownership

| Contract | Existing focused selector |
| --- | --- |
| Legacy mode compatibility | `HistoryStoreTest.legacyResultsDefaultToRewardsAndKeepPrizeClaimsAndCollections` |
| Stored rule snapshots | `HistoryStoreTest.customMathAndReadingRuleSnapshotsRoundTripAndClaimOnlyOnce` |
| Atomic idempotent claims | `HistoryStoreTest.claimsAreAtomicIdempotentAndConvertEveryThreeFragments` |
| Cross-subject carry | `HistoryStoreTest.readingAndMathClaimsShareFragmentCarryWithoutDuplicateAwardsAfterRestart` |
| Explicit deletion vs retention | `HistoryStoreTest.deletionRemovesRewardsButRetentionTrimmingPreservesThemWithoutDuplicatingClaims` |
| Whole-to-fragment deletion accounting | `HistoryStoreTest.deletingAClaimedResultRemovesOnlyItsFragmentIncludingFromWholeRewards` |
| Shared celebration ownership | `HistoryStoreTest.readingDeletionRemovesOnlyLinkedRewardsAndKeepsMathOtherReadersAndSharedCelebrations` |
| Stale saves / reclaims | `HistoryStoreTest.staleResultSaveCannotOverwriteAnAlreadyClaimedPrize` |
| Corrupt inventory protection | `HistoryStoreTest.malformedSnapshotsCannotSilentlyEraseRewards` |
| Enum retirement before decoding | `HistoryStoreTest.retiredReadingReceiptsMigrateBeforeEnumDecodingAndPreserveUnrelatedInventory` |
| Migration write failure | `HistoryStoreTest.failedRetiredRewardMigrationKeepsTheOriginalFileAndRetriesWithoutDoubleSubtraction` |
| Admin unit isolation | `HistoryStoreTest.adminRemovalNeverBreaksWholeRewardsOrConsumesTheWrongUnit` |

## Model orchestration and reading receipts

| Contract | Existing focused selector |
| --- | --- |
| Start/finish per-game gates | `RewardsModelTest.eachMathGameRequiresItsOwnRewardsAtStartAndFinishAndKeepsTimerSnapshot` |
| Persisted Math rules | `RewardsModelTest.ordinaryRewardsUseFinishThresholdsAndKeepTheirRulesThroughReloadAndClaim` |
| Claim/delete serialization | `RewardsModelTest.deletionAndClearSerializeWithPendingClaimsInEitherOrder` |
| Navigation-safe asynchronous work | `RewardsModelTest.lateSavesAndClaimsDoNotReopenResultsAfterNavigation` |
| Retry without optimistic balance | `RewardsModelTest.claimFailureDoesNotUpdateDisplayedOrPersistedBalances` |
| Finished Reading rule ownership | `ReadingRewardsTest.readingReceiptsUseFinishedSessionThresholdsNotCurrentMathSettings` |
| Live Reading collection before gift | `ReadingRewardsTest.completionCelebratesBeforeClaimAndCollectsOnlyTheActuallyPresentedScene` |
| No replay/reclaim on restart | `ReadingRewardsTest.reopeningAndRestartingCannotReplayOrDuplicateButHistoryCanClaimAnEarnedGift` |
| Durable Reading deletion cleanup | `ReadingRewardsTest.durableDeletionReceiptsFinishRewardCleanupAfterFailureAndRestart` |
| Shared Math/Reading/admin mutex | `ReadingRewardsTest.readingClaimsSerializeWithMathClaimsAndAdminInventoryChanges` |

## Settings, gifts, collections, and artwork

| Contract | Existing focused selector |
| --- | --- |
| Per-game settings persistence | `RewardsSettingsTest.everyAdminSectionAndSubjectStartsCollapsedAndMathChoicesPersist` |
| Independent matrix tier UI | `RewardsSettingsTest.matrixRewardTierIsSelectableAtLargeTextSizeAndPersistsIndependently` |
| Gift rapid taps/accessibility | `RewardUiTest.giftIsAnAccessibleButtonAndRapidTapsOnlyOpenOnce` |
| Durable claim reveal | `RewardUiTest.successfulPersistenceRevealsTheFragmentAndUpdatedCarryBalancePermanently` |
| Saved celebration names | `PokemonCollectionTest.originalPokemonsKeepExactLabelsAndAllStoredNamesAreStable` |
| Live collection with earning disabled | `PokemonCollectionTest.eachPresentedCelebrationIsCollectedOnceEvenWithRewardsDisabled` |
| Replay cannot award | `PokemonSettingsTest.collectedImageReplaysAndClosesWithoutChangingPracticeOrInventory` |
| Catalog replay lifecycle | `CelebrationUiTest.everyCelebrationCanReplayWithItsLabelAndCloseOrFinish` |
| Matrix's independent two-scene queue | `MegaRewardsTest.matrixAlwaysSelectsBothPoolsIndependentlyAndDoesNotRequirePrizes` |
| Unpresented queue disposal | `MegaRewardsTest.leavingOrDeletingResultsDropsTheQueuedCelebrationWithoutCollectingIt` |
| Matrix identity, not count alone | `MegaRewardsTest.ordinaryMultiplicationWith121QuestionsCannotEarnMega` |
| Reading cannot unlock Mega | `ReadingRewardsTest.readingWith121QuestionsCannotUnlockMegaEvenAfterFinishingTheStandardCollection` |
| Kanto metadata | `KantoCatalogTest.everyRequestedPokemonHasItsExactLabelCategoryAndNdex` |
| Mega serialized names | `MegaCatalogTest.allEnumNamesRoundTripWithoutCollidingWithStandardPokemon` |
| Whole/fragment art | `RewardDrawingsTest.allRewardsRenderDistinctWholeAndPartialArtwork` |
| Kanto/additional Pokémon art | `CelebrationDrawingsTest.everyAddedPokemonHasDistinctVisibleArtworkWithoutTheStage` |
| Mega frames | `MegaDrawingTest.everySceneAnimatesOpaqueDeterministicFramesThroughoutPlayback` |

## Expand only across a changed contract

- Completion/scoring changes: add the relevant Math gameplay test or
  `ReadingSessionTest.prizesUseWholeSessionAccuracyAndStartAndFinishRewardPreferences`.
- Checkpoint/legacy flags: consult Reading ownership and
  `ReadingModelTest.legacyCheckpointsAccuracyMilestonesAndStoriesNeverAwardRewards`.
- Reading enum retirement: pair receipt tests with reading-data's applicable
  `ReadingStoreTest` / `ReadingSqliteStoreTest` migration method, not both full suites.
- Gallery return paths: consult shell ownership and
  `SubjectNavigationTest.readingCollectionsReturnToReadingAndSettingsControlsPreserveDrafts`.
- Accounting-only edits do not need drawing tests. Rendering-only edits need the
  affected family's tests, plus catalog/persistence coverage only if IDs change.
