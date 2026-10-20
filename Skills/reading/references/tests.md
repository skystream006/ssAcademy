# Reading runtime test selection

All Kotlin classes below are in `com.ssmath.app`; their source files are
`${TEST}/<Class>.kt`. Method names are exact existing selectors.
Use [shared validation](../../repo-router/references/validation.md) for runner
syntax/tool requirements, not a duplicated command here. Batch related selectors
in one invocation. Start with the behavior below, then widen only if necessary.

## Engine changes

- `ReadingModelTest`: standalone/legacy checkpoints, first-try accuracy,
  profiles, writing, stable tile identity, and completed-checkpoint immutability.
  - `identicalSentenceWordsAreInterchangeableWithoutAcceptingIncorrectOrIncompleteOrders`
  - `sentenceTilesCanFillAnySpaceAndMoveWithoutShiftingOtherWords`
  - `hintsAndSuccessfullyQueuedNarrationPreventIndependentCreditButFailedSpeechDoesNot`
  - `legacyCheckpointsAccuracyMilestonesAndStoriesNeverAwardRewards`
- `ReadingSessionTest`: allocated session membership, question counts, scoring,
  ending, and resume. Do not substitute standalone checkpoint tests.
  - `sessionsAllocateExactlyTheSelectedCountAndOnlyNextAdvances`
  - `firstValidWrongAnswerFinishesActivityAndCannotBeRetriedOrSubmittedTwice`
  - `moreThanTenPercentWrongEndsSessionImmediatelyAndAbandonsUnplayedAssignments`
  - `multiCheckpointActivitiesCountAsOneAndFailedEvidenceDoesNotCountEarlierAnswer`
  - `resumingAnyAssignedMemberReturnsTheCurrentMemberWithoutAllocatingAgain`
- For deletion, begin with
  `ReadingSessionTest.deletingAnySessionMemberRemovesTheWholeGameAndKeepsADurableReceipt`.
  Add the UI/actor selectors below only when those layers change.

## Actor, timers, and reading settings

`${TEST}/ReadingStoreTest.kt` covers both the legacy store and the runtime actor.
For `ReadingViewModel`, select these behaviors rather than treating the file as
exclusively storage tests:

- Serialization of edits: `actorSerializesRapidEditsAndCreditOffTheMainThread`,
  `staleQueuedCallbacksCannotChangeAnotherReaderOrNewAttempt`.
- Retry/advance: `failedSaveBlocksEditsAndRetryCommitsTheSamePendingCheckpointExactlyOnce`,
  `failedSubmissionRetriesTheAnswerAndAdvanceTogetherWithoutChangingTheNextQuestion`.
- Clock behavior: `sessionTimerCountsVisibleTimeOnlyAndResumesPersistedElapsedBeforeTimeout`,
  `failedClockSavePreservesQueuedSubmissionForRetry`,
  `timeoutSaveStillBlocksInteractionAndRejectsQueuedSubmission`.
- Settings snapshots: `eachReadingGameSnapshotsItsOwnRewardsAndTimerWithoutDisablingPlay`,
  `failedSessionAllocationRetriesItsOriginalRewardAndTimerSnapshot`,
  `readingReadsNewThresholdsAtFinishAndReloadsTheRuleSnapshot`.
- Deletion: `failedHistoryDeletionKeepsTheGameUntilRetryAndRejectsStaleEdits`.
- Disabled games: `disabledGamesCannotBeStartedSubmittedResumedOrAdvancedThroughTheModel`.

## Presentation and speech

Start with the relevant methods in `ReadingScreenTest`:

| Behavior | Existing method(s) |
| --- | --- |
| Scramble drag/drop and sparse restoration | `sentenceTilesDragIntoAnyEmptySpaceBetweenSpacesAndBackOut`, `sparseSentenceDraftRestoresAndTapControlsFillItsFirstEmptySpace` |
| Large text/repeated words | `fourteenIndividualWordsRemainPlayableAtLargeTextWithRepeatedWords`, `twoHundredPercentTextHasReachableControlsAndLargeTouchTargets` |
| Draft editing | `typedStoryDraftKeepsRapidCharactersAndRestoresAfterLeavingActivity`, `studioLockRestoresCanonicalTextAfterEditsQueuedBehindASlowCheckpointSave` |
| Immediate advancement | `submitAdvancesImmediatelyAndKeepsThePreviousCorrectionBelowTheNewProblem` |
| Nonblocking clock saves | `submitStaysEnabledDuringUntimedSessionClockSaves`, `submitStaysEnabledDuringTimedSessionClockSaves` |
| History deletion | `historyDeletionRequiresConfirmationAndRemovesWholeSessionsIncludingDrafts` |
| Speech/support distinction | `instructionsDoNotAffectIndependentCreditAndUnavailableAudioCanBeRetried`, `studioActivityHasOnlyInstructionsButSavedStoryCanStillBeReadAloud` |

For saved-answer UI, use `ReadingReviewTest`, starting with
`everyGameShowsSavedQuestionsAndResponsesAfterSerialization`,
`loadingFailureCanBeRetriedWithoutSavingOrShowingAnotherQuestion`, or
`navigatingDuringLoadNeverShowsAStaleResponseAndBackDismissesReview`.
For platform voice selection/lifecycle, use `OfflineReadingSpeakerTest`,
especially `onlyInstalledOfflineEnglishVoicesAreEligible` and
`missingSpeechServiceAndLateInitializationCannotBreakReading`.

## Cross-boundary escalation

- Changed assignment/revision/storage contract: select from
  [reading-data tests](../../reading-data/references/tests.md), not the full data suite.
- Shared reward receipts/deletion: use `ReadingRewardsTest`, notably
  `durableDeletionReceiptsFinishRewardCleanupAfterFailureAndRestart`, with
  [rewards guidance](../../rewards/SKILL.md).
- Home/settings integration: `ReadingEntryTest` covers import gating, navigation,
  and restart reconciliation; `homeFinishesPersistedReadingRewardDeletionAfterRestart`
  is useful for deletion recovery. Consult [app-shell](../../app-shell/SKILL.md).
- `ReadingBulkPackTest` is a broader published-catalogue/upgrade escalation, not
  the default for UI or scoring edits. `ReadingSummitTest` is for real Summit
  pack integration, not a replacement for a focused engine regression.
