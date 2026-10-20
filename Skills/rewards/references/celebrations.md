# Celebrations, collections, and artwork

Use for selection, live presentation, replay, gallery behavior, or drawings.
For balance-only work, use [inventory](inventory.md) and leave drawings unopened.
Paths use the Skill's `SRC`; [common conventions](../../repo-router/references/common.md)
provide optional path setup.

## Direct discovery

| File under `${SRC}` | Start at these symbols |
| --- | --- |
| `CelebrationUi.kt` | `CelebrationCategory`, `Celebration`, `Celebration.select`, `isEligibleFor`, `collectionLabel`. |
| `CelebrationUi.kt` | `CelebrationDialog`, `CelebrationScene`, `drawCelebrationArtwork`, `CELEBRATION_DURATION_MS`. |
| `MathViewModel.kt` | `finish`, `queuedCelebrations`, `dismissCelebration`, `collectPresentedCelebration`, `collectCelebrations`. |
| `MathViewModel.kt` | `persistReadingResult`, `collectPresentedReadingCelebration`, `dismissReadingCelebration`, `collectAllCelebrations`. |
| `HistoryStore.kt` | `collectPokemon` overloads, `collectPokemons`, `collectReadingCelebration`, result collection slots and deletion helpers. |
| `SettingsUi.kt` | `PokemonsScreen`: category counts, collection labels, previews, and replay. |
| `MathUi.kt` | `MainContent` and `MathAppContent`: live `onPresented` callbacks, queue progression, gift ordering. |

Do not load `Kanto*` or `Mega*` implementations merely because a task mentions
collection eligibility. Selection and persistence entry points above come first.

## Selection and live-collection invariants

- Preserve persisted `Celebration` names, category identity, Ndex/suffix metadata,
  and collection-label formatting; existing unusual identifiers are intentional
  compatibility surface, not cleanup opportunities.
- Practice selects only `OTHER`; standard Pokémon require rewards mode and at
  least 15 questions. Celebration eligibility is not fragment eligibility.
- Completed, non-timeout sessions may celebrate even when earning is disabled,
  no prize type is enabled, or accuracy does not qualify for a fragment.
- Prefer uncollected eligible entries. Repeats become available only when the
  entire selected pool is collected, not merely its currently eligible subset.
- Mega selection requires actual Multiplication Matrix identity and its exact
  121-question count in rewards mode. Ordinary multiplication with 121 questions
  and Reading sessions cannot unlock Mega forms.
- Matrix completion selects Mega and standard pools independently, queuing Mega
  first, then a standard celebration, before the gift. Preserve both result slots.
- Leaving or deleting results drops unpresented queued scenes without collecting
  them. Selection alone is not collection.
- `CelebrationDialog` invokes `onPresented` while lifecycle state is `RESUMED`;
  collect only the matching live scene through the guarded model callbacks.
  Keep collection idempotent and saved together with its owning result/receipt.
- Gallery replay supplies no collection callback. Preview, replay, closing, and
  history reopening must not award fragments, collection entries, or new receipts.
- Reading selects only on a first durable session receipt; restarting/reopening
  must not replay the automatic celebration. History can still claim its gift.
- Explicit admin `collectAllCelebrations` is separate from gameplay eligibility:
  it may include Mega without fabricating game history or inventory awards.
- Preserve shared-owner deletion accounting and mutex/atomic-save guarantees
  described in [inventory](inventory.md).

## Drawings: dispatch to one family

Start with `CelebrationUi.kt:drawCelebrationArtwork` for celebration routing.
Read only the affected family and its directly used helpers:

| Family | Exact entry file and drawing symbols |
| --- | --- |
| Whole/fragment rewards and gift | `RewardDrawings.kt`: `drawRewardArtwork`, `drawRewardGift`; callers are in `RewardUi.kt`. |
| Dolphins, whales, anchovies, party | `CelebrationUi.kt`: `drawOcean`, `drawParty`. |
| Candy and original five Pokémon | `CelebrationDrawings.kt`: `drawCandyShower`, `drawPikachuCelebration`, `drawSquirtleCelebration`, `drawBulbasaurCelebration`, `drawCharmanderCelebration`, `drawJigglypuffCelebration`. |
| Later non-Kanto scenes | `PokemonDrawings.kt`: `drawPalafinCelebration`, `drawFinizenCelebration`, `drawWailmerCelebration`, `drawWailordCelebration`, `drawBouffalantCelebration`, `drawVeluzaCelebration`, `drawMantykeCelebration`, `drawMantineCelebration`. |
| Additional Kanto scenes | `KantoCelebrationDrawings.kt`: `drawKantoCelebration`, `drawKantoPokemon`; follow the actual dispatcher, not just species numbering. |
| Kanto body branches | `KantoEarlyDrawings.kt:drawKantoEarly` (Ndex 2–50), `KantoMiddleDrawings.kt:drawKantoMiddle` (51–100), `KantoLateDrawings.kt:drawKantoLate` (101–151). |
| Kanto environment/motion | `KantoSceneProfiles.kt:kantoSceneProfile`, `KantoHabitats.kt:drawKantoHabitat`, `KantoSceneMotion.kt:kantoScenePose` and `drawKantoEffect`; open only the affected helper. |
| Mega metadata | `MegaCatalog.kt`: `MegaPokemonProfile`, `megaPokemonProfile` for types, habitat, motion, and image references. |
| Mega scenes and dispatch | `MegaCelebrationDrawings.kt`: `drawMegaCelebration`, `drawMegaPokemon`, `megaScenePose`, `drawMegaHabitat`. |
| Mega body branches | Follow the enum branch into `MegaEarlyDrawings.kt:drawMegaEarly`, `MegaMiddleDrawings.kt:drawMegaMiddle`, or `MegaLateDrawings.kt:drawMegaLate`; do not load all three. |

Keep artwork visible at playback boundaries, in previews, and in wide/tall bounds.
Preserve distinct forms, deterministic frames, body motion, and accessible
descriptions/test tags. Do not replace account correctness tests with pixel tests.

## Consultation triggers

- Consult [math](../../math/SKILL.md) when completion, timeout, or matrix identity changes.
- Consult [reading](../../reading/SKILL.md) when completion, receipt creation, or live callback
  timing changes; checkpoints still award no points.
- Consult [reading-data](../../reading-data/SKILL.md) when retiring serialized games affects collection owners.
- Consult [app-shell](../../app-shell/SKILL.md) for gallery return paths, overlays, background behavior,
  or navigation invalidating an in-flight presentation.

Use [tests](tests.md) to select the closest existing catalog, lifecycle, persistence,
or rendering test; command setup belongs to [validation](../../repo-router/references/validation.md).
