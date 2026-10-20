# Math gameplay and presentation

Paths use [common notation](../../repo-router/references/common.md).

## Entry-point map

| Change | First symbols |
| --- | --- |
| Numeric validation/defaults | `${SRC}/MathGame.kt`: `parseMaximum`, `parseQuestionCount`, `parseAnswer`, `GameParameters`, `RewardsGameDefaults` |
| Arithmetic generation/scoring | `${SRC}/MathGame.kt`: `Operation`, `ProblemGenerator`, `Problem`, `Attempt`, `GameState` |
| Matrix ordering | `${SRC}/MathGame.kt`: `MultiplicationMatrix`, `GameState.startMultiplicationMatrix` |
| Picture puzzle rules | `${SRC}/LogicMath.kt`: `LogicMathSettings`, `LogicExpression.evaluate`, `LogicPuzzleGenerator.next` |
| Setup/ready snapshots | `${SRC}/MathViewModel.kt`: `ready`, `submitSetup`, `selectRewardsGame`, `openRewardsSetup`, `saveRewardsSetup` |
| Live session/timing | `${SRC}/MathViewModel.kt`: `start`, `submitAnswer`, `finish`, `checkTimeLimit`, `updateTimer`, `setForeground` |
| Math UI | `${SRC}/MathUi.kt`: `SetupForm`, `ReadyDialog`, `PlayingScreen`, `Equation`, `LogicPuzzleEquation`, `ResultsContent`, `MultiplicationMatrixAnswers` |
| Progress animation | `${SRC}/ProgressIcons.kt`; `${SRC}/MathUi.kt`: `GameProgress` |
| Admin math controls | `${SRC}/SettingsUi.kt`: `LogicMathSettingsFields`, math portion of `GameSettings`; corresponding ViewModel setters |

## Gameplay invariants

- Addition, subtraction, and multiplication use inclusive number bounds. Subtraction stays nonnegative; division uses independently bounded dividend/divisor and exact whole-number answers.
- `RewardsGameDefaults` stores only per-operation number limits. Challenge question counts are chosen at launch, separately from Practice choices.
- `GameParameters` validates the question count too. Do not accidentally reject valid division bounds because the second maximum exceeds the first.
- Matrix identity is explicit, not inferred from multiplication or count. It covers each pair in the 11-by-11 matrix once; randomization fixes an order at game start.
- Correct and wrong answers both advance progress. The wrong-answer threshold is strictly more than 10% of the selected total; `GameState.maxWrongAnswers` handles integer boundaries.
- Logic clues determine a unique assignment; different pictures may share equal values. Keep operation precedence, exact division, bounds/overflow checks, reproducible seeded generation, and final-answer range.
- Persist original picture equations and submitted answers, not regenerated puzzles or only computed totals. `${SRC}/HistoryStore.kt` owns the saved result envelope.
- Logic rows stay horizontal even when numeric equations use the vertical preference.

## Session and settings boundaries

- Ready state snapshots operation/number/count parameters. Active logic settings, matrix question order, and time limit must not change when editing settings during a game.
- Rewards-default editing is a Settings overlay: save/cancel returns to Settings without replacing the underlying game or unpausing its timer.
- Math time limits apply only to eligible Rewards-mode launches with timer/reward settings enabled, scaling seconds by question count / 5. Reading timing is independent.
- All overlays pause math timing; background time follows the math pause preference. Clock/deadline handling must reject late answers and avoid duplicate finish/save.
- Prize selection and celebration collection are different contracts. Practice must not receive prizes or Pokémon categories; rewards owns the eligibility details and persistence safeguards.
- A game's progress icon remains stable for all its answers; a new game selects a new icon.

## Adjacent domains

Read [rewards](../../rewards/SKILL.md) only when changing the completion/award/store boundary. Do not edit serialized `PracticeResult` fields without its compatibility tests.

Use [app-shell](../../app-shell/SKILL.md) for shared navigation/dialog/theme changes, not ordinary math-screen rendering. Reading imports the shared question-count bounds; changes to those bounds require checking its engine/store validation as an affected consumer.

Choose tests from [the math map](tests.md). Ignore drawing families, reading packs, APK updates, and release workflows unless a direct dependency emerges.
