---
name: repo-router
description: Route ambiguous, cross-domain, or unfamiliar ssAcademy engineering requests to the smallest relevant domain Skill. Use when ownership is unclear; clearly scoped requests should activate their domain Skill directly.
---

# Route, then stop exploring

Choose one primary Skill from the requested behavior, not merely the filename:

| Request | Skill |
| --- | --- |
| Home/subject navigation, shared layout, appearance, settings shell, Admin PIN | [app-shell](../app-shell/SKILL.md) |
| Arithmetic, logic puzzles, matrix games, math setup/play/results/timers | [math](../math/SKILL.md) |
| Reading interaction, scoring, sessions, review, offline narration | [reading](../reading/SKILL.md) |
| Reading SQLite, migrations, catalogue, imports, content generation | [reading-data](../reading-data/SKILL.md) |
| Prizes, inventory/history accounting, collections, celebration artwork | [rewards](../rewards/SKILL.md) |
| APK updates, lifecycle services, diagnostics, Gradle, Android configuration, releases | [android-platform](../android-platform/SKILL.md) |

1. Load the selected Skill and only its task-relevant reference.
2. Find the entry symbol and its directly affected callers/tests.
3. Consult [architecture](references/architecture.md) only for shared-file ownership or cross-domain contracts.
4. Add another Skill only for a changed contract, affected consumer, or shared invariant. Never recursively preload dependencies.
5. Use the selected domain's test map; [validation](references/validation.md) supplies commands when needed.

[Common](references/common.md) defines portable absolute paths and discovery requirements. Do not load the full map, README, datasets, or artwork just to route. If a request assumes a backend/account service, clarify: none exists here. Before finishing, check that touched symbols and selected tests match the routed scope.
