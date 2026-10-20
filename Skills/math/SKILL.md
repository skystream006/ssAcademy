---
name: math
description: Handle arithmetic practice, Basic and Logic Math challenges, multiplication matrices, number validation, question generation, math timers and scoring, and math setup/play/results UI. Use for math gameplay changes rather than shared prize accounting.
---

# Math gameplay

Resolve `ROOT` to the absolute checkout root; `SRC=${ROOT}/app/src/main/java/com/ssmath/app`.

Start with `${SRC}/MathGame.kt` or `${SRC}/LogicMath.kt` for rules; for presentation, start at the affected symbol in `${SRC}/MathUi.kt`. Inspect only related setup/start/answer/timer/finish methods in `${SRC}/MathViewModel.kt`, and math-specific controls in `${SRC}/SettingsUi.kt`.

Read [gameplay](references/gameplay.md) only for the affected subsystem's entry points and invariants. Keep rules in the engine, preserve independent division bounds, active-game snapshots, and serialized puzzle/result meaning.

Consult [rewards](../rewards/SKILL.md) if completion contracts, result persistence, eligibility, or celebrations change. Use [app-shell](../app-shell/SKILL.md) only for shared layout/navigation. If changing question-count constants used by reading, consult [reading](../reading/SKILL.md) for that consumer.

Normally ignore reading implementations/data, PIN internals, unrelated artwork, updater code, and workflows. Do not load whole shared files to find one function.

Use [targeted tests](references/tests.md): engine first, then affected orchestration, UI, or serialization cases. [Shared validation](../repo-router/references/validation.md) supplies commands and broader Android checks.
