# Reading sessions and UI

Use for runtime behavior, not catalogue construction or database mechanics.
Source notation: `${SRC}/File.kt` followed by symbols; see
[common conventions](../../repo-router/references/common.md) only if root
variables have not been established.

## First sources

| Change | Start here |
| --- | --- |
| Scoring, drafts, session lifecycle | `${SRC}/ReadingModel.kt`: `ReadingEngine`, `ReadingAttempt`, `ReadingSession`, `ReadingData` |
| Serialized interaction, retries, timers | `${SRC}/ReadingStore.kt`: `ReadingViewModel`, its `Command` actor, `persist`, `submit`, `updateSessionClock` |
| Activity/menu/progress/history UI | `${SRC}/ReadingScreen.kt`: `ReadingModule`, `ReadingActivityScreen`, `ReadingHistoryContent` |
| Saved answers | `${SRC}/ReadingReview.kt`: `ReadingReviewDialog`, `ReadingAttemptReview` |
| Offline voice lifecycle | `${SRC}/OfflineReadingSpeaker.kt`: `offlineReadingVoice`, `OfflineReadingSpeaker`, `rememberOfflineReadingSpeaker` |

Reading also owns `${ROOT}/app/src/main/res/drawable/reading_*.xml`.
`ReadingGame.backgroundImage` in `${SRC}/ReadingScreen.kt` selects artwork;
Grammar currently uses the shared subject image rather than a reading-specific file.

## Engine contracts

- Keep attempt/session/profile IDs stable across resume, edits, and review.
  `ReadingActivityRef.revision` identifies assigned content, not the latest pack.
- `ReadingEngine.requiredKeys` defines game completion: answer for Missing/Grammar,
  order for Scramble, order then answer for Sequence, answer then evidence for
  Detective, and four ordered writing checkpoints for Studio.
- A session counts an activity once, not once per checkpoint.
  Scramble's optional bonus is outside required session scoring.
- A valid wrong required answer ends that session activity; retries belong to
  legacy standalone attempts. More than 10% wrong ends the session immediately
  and archives unplayed assignments. Empty/invalid submissions are not answers.
- Preserve `firstCorrect`, `firstSupported`, `tries`, and attribution to `readerId`.
  Later success must not turn a failed first try into independent accuracy.
- Checkpoints, milestones, and saved stories award **no reward points**.
  `rewardGranted` remains a deprecated compatibility field; do not revive it.
  Session prizes are separate and require [rewards](../../rewards/SKILL.md)
  when changing receipts, eligibility, presentation, or claims.
- Scramble tile IDs are indices, even for identical labels. `-1` denotes empty
  spaces; trailing missing entries are empty too. Moves cannot duplicate tiles.
  Accepted orders compare rendered words, allowing interchangeable identical words.
- Completed checkpoints freeze their answers/orders/writing. Studio saves one
  story per attempt, not a new story on every resumed finish.

## One actor and durable state

`rememberReadingViewModel` is Activity-scoped. `ReadingViewModel` owns the command
channel; do not add UI-side stores or competing writers. Commands capture
profile/active-attempt identity so stale callbacks cannot edit another draft.
Accepted edits survive a preceding write failure; retry the pending snapshot
rather than recomputing a checkpoint, rerolling a prize, or reserving extra content.

`ReadingViewModel.submit` saves submission, finish, and next-question advancement
together. Engine-level `finish` alone does not advance; preserve this distinction.
Start-time reading rewards/timer choices are captured for that session; finish
checks live reward preferences and saves the threshold snapshot with the result.
The reading time limit is total session time, not math's question-scaled timer.
Only visible, resumed activity time counts. Routine clock saves must not disable
answer controls; timeout and failed persistence still block unsafe interaction.

Deleting any session member removes the entire session, its attempts/stories,
and affected active pointers. Persist `ReadingData.deletedSessionIds` receipts.
Shared rewards cleanup can fail independently and must reconcile after restart;
never discard receipts simply because the reading UI has removed the result.

## UI, settings, and speech

- Keep `reading-*` test tags, descriptive semantics, reachable scrolling, large
  touch targets, text scaling, and both tap/long-press-drag Scramble controls.
  Draft text buffers must survive rapid typing while canonical saves are pending.
- `ReadingReviewDialog` loads through `model.review`, in saved session order,
  with cancellation/retry handling. It must not resume, score, allocate, or save.
  Legacy history stores the latest answer, not a reconstruction of every retry.
- Instruction audio does not mark an attempt supported. Successful passage
  narration does; failed speech does not. Current activity UI exposes instructions,
  while saved stories can still be read aloud; retain legacy narration history.
- Select installed offline English voices only; prefer US English deterministically.
  Stop speech on navigation, lifecycle pause/stop, and disposal. Missing TTS cannot
  prevent play or trigger a network-voice fallback.
- Reading-specific controls live in `${SRC}/SettingsUi.kt`: `ChangeNameDialog`,
  `ReadingLevelDialog`, `ActiveReadingGamesDialog`, and `GameSettings`' reading
  subsection. Trace `readReadingTimeLimitSeconds` in `${SRC}/TimeLimitSettings.kt`
  or `readReadingRewardsEnabled` in `${SRC}/RewardSettings.kt` only as needed.
  Do not rewrite shared settings navigation to change a reading control.

## Narrow handoffs and verification

Use [test selection](tests.md) for the affected behavior.
Consult [reading-data](../../reading-data/SKILL.md) only for schema, revision
resolution, allocations, or persistence changes; its storage and generation
references are separate. Import controls/database browsing are data-owned.
For shared overlays/home/back routing, use [app-shell](../../app-shell/SKILL.md).
For session reward receipts or deletion reconciliation, use
[rewards](../../rewards/SKILL.md), not checkpoint-based accounting.
