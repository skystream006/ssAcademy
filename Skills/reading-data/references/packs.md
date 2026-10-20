# Reading schemas, imports, and generated packs

Choose the relevant section below; storage internals are in [storage](storage.md)
only when assignments or persistence change. Source notation uses `${SRC}` and
`${DATA}` from [common conventions](../../repo-router/references/common.md).

## Schema and importer entry points

- `${SRC}/ReadingContent.kt`: `ReadingGame`, `ReadingLevel`, `ReadingActivity`,
  `ReadingQuestion`, `ReadingPicture`; `ReadingContent` is the legacy fallback,
  not the source for new installed libraries.
- `${SRC}/ReadingPack.kt`: `ReadingPackManifest`, `ReadingPackValidator`,
  `requireReadingPackUrl`, `HttpsReadingPackTransport`, `ReadingPackImporter`.
- `${SRC}/ReadingPackSettings.kt`: `ReadingPackSettings`,
  `ReadingImportRequiredDialog`. Keep version/count/progress reporting and
  retry/import gating separate from the read-only Debug Menu database browser.
  Import requires connectivity; installed activities work offline.

The Kotlin validator is the runtime contract, not just the generator's checks.
It validates IDs/text bounds, game-specific fields, distinct answer choices and
answer indices, full tile permutations, evidence indices, and writing choices.
Grammar needs an explanation; Sequence needs event pictures and an unsolved
initial ordering. Unknown JSON fields/games are rejected, not silently accepted.
Schema additions also require a deliberate compatibility/engine review.

## Preserve import boundaries

- `READING_PACK_MANIFEST_URL` currently targets the v10 entry point. URL checks
  allow only exact, canonical, credential-free HTTPS URLs under the configured
  repository's raw GitHub path on port 443. No query/fragment, alternate host,
  branch, repository, path traversal, or encoded filename variation.
- Filename allowlists include the four published manifest entry points and
  `activities-<64 lowercase hex>.jsonl` only. Transport disables redirects,
  cookies, authentication, and automatic connection retries; preserve this.
- Manifest schema is 1. Validate version syntax, game/level counts, nonzero
  bounded bytes/counts, and a filename matching its SHA-256 before data download.
- Limits in `ReadingPackImporter`: manifest 16 KiB, record 64 KiB, pack 64 MiB,
  at most 100,000 activities. Strict UTF-8, blank-line rejection, byte count,
  checksum, duplicate IDs, and exact per-pool counts all matter.
- Download into app cache, check cancellation throughout, and delete the
  downloaded staging file on success/failure. Do not replace this with an
  unbounded in-memory payload or expose a partially validated catalogue.
- `install` validates into on-disk `reading_import_stage` inside one transaction.
  Only then update current rows and pack metadata; cancellation/failure rolls
  back staged changes, preserving the previous pack and student progress.
- Imports are additive: retain previous revisions and IDs not present in a new
  pack. Revisions hash canonical Kotlin serialization, not raw line formatting.
  An ID cannot change game/level. Updating content must not reinterpret already
  assigned questions or reset presentation cycles.

## Generation: start with source, not payload

Start at `${DATA}/generate.py`: `build_pack`, `build_v9_pack`, `task_identity`,
`answer_keys`, and `main`. Its internal Python `ROOT` is the dataset directory;
the shared shell `${ROOT}` remains the checkout root, and `${DATA}` names this
directory unambiguously.

Current v10 has 18,000 activities: 1,000 for each of six games at three levels.
It replaces all 3,000 Scramble activities with word-level tiles using the same
IDs, preserving every other v9 line. Each word occurrence keeps its own tile
index; identical labels can be interchangeable in scoring. If this contract
changes, involve [reading](../../reading/SKILL.md), not just a payload edit.

| Source | Purpose |
| --- | --- |
| `${DATA}/generation/word_scramble.py` | Current v10 scenes, vocabulary, word bounds, accepted word orders |
| `${DATA}/generation/word_games.py`, `${DATA}/generation/scramble_structures.py` | Legacy missing-word/phrase-Scramble generation; preserve published compatibility |
| `${DATA}/generation/grammar_games.py`, `${DATA}/generation/grammar_sprout.py`, `${DATA}/generation/grammar_trail.py`, `${DATA}/generation/grammar_summit.py` | Curated grammar objectives/examples |
| `${DATA}/generation/reasoning_games.py`, `${DATA}/generation/creative_games.py`, `${DATA}/generation/common.py` | Sequence/Detective/Studio sources and shared generation helpers |

Keep deterministic encoding/order, IDs, task uniqueness, contextual answer
checks, and importer size limits. Cosmetic rewording/distractor permutations
do not make duplicate learning tasks distinct. Scramble answer identity is
sentence-contextual; grammar answer identity includes passage and prompt.

## Published compatibility is immutable

- `${DATA}/manifest.json` and `${DATA}/reading-activities-import.json` stay
  byte-identical v7 aliases, with their existing checksum-named v7 payload.
- `${DATA}/reading-activities-v8-import.json` deliberately points to **v9**,
  not v8 or v10. Preserve that manifest and its checksum-named payload exactly:
  older clients use index-only Scramble scoring and cannot safely adopt v10.
- `${DATA}/reading-activities-v10-import.json` is the current generated entry.
  Generation creates the checksum-named payload and updates this entry; it
  refuses to modify an existing checksum-named file with different bytes.
- `${DATA}/archive.zip` holds historical manifests/payloads used by compatibility
  checks and Android upgrade fixtures. Do not rewrite those baselines to make
  changed tests pass or replace them with current data.

For a content change, edit the appropriate generator, use the existing generation
workflow, and inspect both manifest and payload changes. Never hand-edit generated
JSONL or repoint old aliases. New runtime entry points also need allowlist review.
The non-writing reproducibility mode is `--check`; Python standard-library
commands and supported tool versions live in
[shared validation](../../repo-router/references/validation.md).
Select [focused generator/import tests](tests.md) before broader pack integration;
do not run generation or app tests merely for documentation edits.
