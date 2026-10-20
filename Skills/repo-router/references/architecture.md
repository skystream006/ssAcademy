# Architecture and ownership

Path variables use [common notation](common.md). Read this map only for unfamiliar or cross-domain work.

## Execution and storage

```text
MathApplication -> UiActivityGate + DebugLog
MainActivity -> MathApp / MathAppContent
  -> MathViewModel -> MathGame / LogicMath
                  -> HistoryStore -> JSON history + shared inventory/collections
  -> ReadingScreen -> ReadingViewModel (in ReadingStore)
                   -> ReadingEngine (in ReadingModel)
                   -> ReadingStorage -> ReadingSqliteStore -> ReadingDatabase
                   -> MathViewModel reading reward receipts -> HistoryStore
  -> UpdateNotification / UpdateSettings -> AppUpdater -> GitHub release -> APK

Python generation -> manifest + immutable JSONL
                  -> ReadingPackImporter -> SQLite catalogue/content revisions
```

- `${SRC}/MainActivity.kt` installs Compose content; `${SRC}/MathApplication.kt` initializes lifecycle tracking and opt-in diagnostics.
- `${SRC}/MathUi.kt` composes the app and explicitly routes screen/overlay state. There is no separate navigation graph.
- `${SRC}/MathViewModel.kt` coordinates math, overlays, preferences, and shared rewards. `${SRC}/ReadingStore.kt` also contains `ReadingViewModel`; do not mistake the whole file for storage.
- Math history and both subjects' inventory/receipts live in `practice_history.json` in private app storage. Reading uses `reading.db`, migrating legacy `reading_private.json`. Settings and PIN state use SharedPreferences.
- Reading reserves activities and stores progress transactionally; reward receipt reconciliation bridges SQLite and the separate shared JSON store, not one cross-store transaction.
- Imports and APK updates contact GitHub. Narration uses installed offline Android TTS. No service layer, uploads, remote users, or application job queue exists.

## Domain directory map

All filenames in the Kotlin column expand under `${SRC}`; read only the indicated subset.

| Owner | Kotlin entry files/families | Other owned paths | Test map |
| --- | --- | --- | --- |
| [app-shell](../../app-shell/SKILL.md) | `MainActivity`, `Theme`, `Skins`, `SettingsDialog`, `AdminPinStore`, `AdminPinUi`, `TimeLimitSettings`; shared UI/navigation symbols below | `${RES}/drawable/skin_*.xml`, `${RES}/drawable/subject_*.xml`, `${RES}/values/styles.xml` | [shell tests](../../app-shell/references/tests.md) |
| [math](../../math/SKILL.md) | `MathGame`, `LogicMath`, `ProgressIcons`; math symbols below | Math-specific settings and result presentation | [math tests](../../math/references/tests.md) |
| [reading](../../reading/SKILL.md) | `ReadingModel`, `ReadingScreen`, `ReadingReview`, `OfflineReadingSpeaker`; `ReadingViewModel` | `${RES}/drawable/reading_*.xml`; reading settings controls | [reading tests](../../reading/references/tests.md) |
| [reading-data](../../reading-data/SKILL.md) | `ReadingContent`, `ReadingDatabase`, `ReadingSqliteStore`, `ReadingMigration`, `ReadingPack`, `ReadingPackSettings`, `ReadingDatabaseBrowser`; storage symbols below | `${DATA}` generators, manifests, immutable packs, archive, Python tests | [data tests](../../reading-data/references/tests.md) |
| [rewards](../../rewards/SKILL.md) | `Rewards`, `RewardSettings`, `HistoryStore`, `RewardUi`, `RewardDrawings`, `Celebration*`, `PokemonDrawings`, `Kanto*`, `Mega*` | `${RES}/drawable/ic_gift_box*.xml`, `${RES}/drawable/ic_pokeball.xml` | [reward tests](../../rewards/references/tests.md) |
| [android-platform](../../android-platform/SKILL.md) | `MathApplication`, `UiActivityGate`, `AppUpdater`, `UpdateRelease`, `UpdateUi`, `DebugLog`, `DebugLogUi` | `${ROOT}/app/src/main/AndroidManifest.xml`, `${RES}/xml`, launcher resources, Gradle/wrapper files, `${ROOT}/.github/workflows` | [platform tests](../../android-platform/references/tests.md) |

## Shared-file symbol ownership

| Source | Primary owners and entry symbols |
| --- | --- |
| `${SRC}/MathViewModel.kt` | **shell:** `Screen`/`Overlay`, `openSettings`, `openRewards`, `openPokemons`, `closeOverlay`, appearance setters. **math:** `ready`, `start`, `submitAnswer`, `finish`, `updateTimer`, setup/defaults and math setters. **rewards:** `prepareReadingReward`, claim/collect methods, `persistResult`, `applySnapshot`, history/inventory deletion. Subject-specific preferences belong to their feature. |
| `${SRC}/MathUi.kt` | **shell:** `MathApp`, `MathAppContent`, `HomeScreen`, `SubjectGrid`, `SubjectButton`, `CollectionButtons`, `SettingsButton`. **math:** setup/ready/playing screens, `Equation`, `LogicPuzzleEquation`, `ResultsContent`, matrix/progress display. **rewards:** `RewardsQuestionCountChoices`, `RewardsCriteria`. |
| `${SRC}/SettingsUi.kt` | **shell:** `SettingsScreen`, `ScreenScaffold`, `CollapsibleSettingsSection`, Admin gate, appearance. **math:** `LogicMathSettingsFields` and math controls in `GameSettings`. **reading:** name/level/availability controls. **rewards:** `RewardMinimumSettings`, `AddRemoveRewardsDialog`, `RewardsScreen`, `PokemonsScreen`, reward controls. **platform:** diagnostics controls inside `DebugDialog`. `HistoryScreen` presentation consults math for equations and rewards for deletion/claims. |
| `${SRC}/ReadingStore.kt` | **reading-data:** `ReadingStorage`, legacy `ReadingStore`. **reading:** `ReadingViewModel`, its command actor, timer, and import orchestration; importer behavior remains reading-data. |

Do not load a neighbor's complete Skill because its code shares a file. Search for the affected symbol and inspect its local callers first. For mixed orchestration such as `finish`, math owns session completion while rewards owns the invoked award contract.

## Conditional dependencies

- **shell -> feature:** navigation/settings changes alter the feature's session behavior.
- **math -> rewards:** completion, serialized result fields, prizes, or collection eligibility changes.
- **reading <-> reading-data:** content schema, scorer interpretation, attempt persistence, allocation, or assigned revisions change.
- **reading -> rewards:** finished-session receipts, claims, or durable history-deletion reconciliation changes.
- **reading-data <-> rewards:** retiring games or changing enum identifiers used in shared reward receipts.
- **platform <-> consumers:** lifecycle, permissions, diagnostics, build, or installed-identity contracts change.

Shared contracts include question-count constants in `${SRC}/MathGame.kt` used by reading, thresholds in `${SRC}/Rewards.kt`, preference helpers in `${SRC}/RewardSettings.kt` and `${SRC}/TimeLimitSettings.kt`, and shared Compose primitives. Owning a helper does not transfer ownership of all its consumers.

## Cross-domain workflow

1. Choose one lead Skill; list changed contracts and their owners.
2. Read the relevant contract references, not every dependent implementation.
3. Agree on compatibility/defaults before editing persisted shapes.
4. Change the owning layer, then only affected consumers.
5. Run each changed contract's focused tests plus the integration tests at its handoffs; use [validation](validation.md) for broader checks.

Example: new reading game -> reading-data schema/compatible manifest -> reading engine/session/UI -> rewards receipt/eligibility support -> feature settings -> targeted compatibility/integration tests. Preserve each owner's invariants; no generic database-first rule for UI-only work.

## Routing examples

| Request | Start | Expand only if | First tests |
| --- | --- | --- | --- |
| Fix Home labels clipping at 200% text | app-shell | Shared feature layout is affected | Matching `MathAppTest` layout methods |
| Validate new Logic Math constraints | math | Shared dialog primitive or award rules change | `LogicMathTest`, relevant `GameModesTest`/settings methods |
| Refresh Summit scramble sentences while preserving drafts | reading-data | Accepted-order scoring changes: reading | Relevant Python class, reproducibility, pack/revision regressions |
| Add a reading game with availability and prizes | reading | Schema/pack: reading-data; receipts: rewards | Engine/UI tests plus pack/storage/reward handoffs |
| Retry interrupted APK downloads safely | android-platform | Shared settings UI changes: app-shell | `AppUpdaterTest`, affected UI/lifecycle verification |

## Default exclusions

Do not load `${ROOT}/.git`, IDE/cache/build outputs, APKs, reports, local machine configuration, keystore contents, or private signing material. Whole JSONL packs and `${DATA}/archive.zip` are data-owned test inputs, not AI context. Inspect only targeted records when needed.

Exclude unrelated drawing families, the complete README, `${ROOT}/.mega-reference-review`, and `${ROOT}/file_00000000f08c81f6bc46d423d33641f0.png` unless a visual-reference task specifically requires them. Do not create Skills for absent backend/authentication/job services or placeholder subjects.
