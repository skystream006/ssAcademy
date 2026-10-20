# Math test selection

Tests are `${TEST}/<Class>.kt`; see [path notation](../../repo-router/references/common.md) and [validation commands](../../repo-router/references/validation.md). Select the affected row, then expand only for changed contracts.

| Behavior | First tests | Integration expansion |
| --- | --- | --- |
| Bounds, parsing, arithmetic generation, scores | `MathGameTest` | Relevant setup/answer methods in `GameModesTest` and `MathAppTest` |
| Logic settings/evaluator/generator | `LogicMathTest` | `GameModesTest.logicSettingsValidatePersistAndAreSnapshottedForEveryQuestion`; applicable `RewardsSettingsTest` controls |
| Logic rendering/history | Relevant `MathAppTest` logic methods | `LogicMathTest` serialization; `GameModesTest.logicMathAwardsAndPersistsOriginalPictureEquationsWithoutChangingBasicMath`; relevant `HistoryStoreTest` |
| Matrix row order/randomization | Matrix methods in `MathGameTest` | Matrix methods in `GameModesTest`; result rendering methods in `MathAppTest` |
| Challenge defaults/ready/count snapshots | Matching `GameModesTest` methods | Relevant `MathAppTest` and `RewardsSettingsTest` controls |
| Math timers/background/early finish | Relevant `GameModesTest` and `RewardsModelTest` methods | `RewardsSettingsTest` when controls/preferences change |
| Numeric equation presentation | `EquationTest` | Only affected math result/play layout methods in `MathAppTest` |
| Progress icons/animation | `ProgressIconsTest` | `GameModesTest.bothModesKeepTheSameIconForEveryJumpAndSelectOnlyWhenStartingANewGame`; progress methods in `MathAppTest` |
| Completion changes affecting awards | Above session tests | [Rewards test map](../../rewards/references/tests.md), especially affected `HistoryStoreTest`, `RewardsModelTest`, or `MegaRewardsTest` |

`LogicMathTest` includes an independent evaluator/solver, seeded cases, and all valid setting combinations; preserve those checks rather than replacing them with assertions against the production implementation itself.

Use existing injected clocks/random/dispatchers for model tests. The shared `MathAppTest` contains unrelated shell/platform cases: filter by behavior, method, or test tag rather than reading the entire class.

For UI changes, retain accessibility and enlarged-text cases. Changes only to a math engine should not load/run reading pack tests unless shared constants or contracts are altered.
