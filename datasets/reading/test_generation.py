"""Standard-library regression checks for the published reading catalogue."""

from collections import Counter, defaultdict
from copy import deepcopy
import hashlib
from itertools import product
import json
import unittest
from zipfile import ZipFile

import generate
from generation.common import scramble_tiles
from generation.grammar_games import LESSONS, grammar, grammar_lessons
from generation.scramble_structures import STRUCTURES
from generation.word_games import (
    _SELECTION_GROUPS,
    _SELECTIONS,
    _situations,
    missing,
    scramble,
)
from generation.word_scramble import SCENES, VOCABULARY, WORD_BOUNDS, _word_orders, word_scramble


def scenes(level):
    for family, item, original in _situations(level):
        group, role = _SELECTION_GROUPS[family.title]
        frame_index = getattr(family, level.lower()).index(original)
        scene = _SELECTIONS[group][frame_index]
        _, reason = scene.variants[generate.LEVELS.index(level)]
        yield family.title, item, role, scene.description.format(item=item), reason


def sentence(activity):
    return " ".join(activity["tiles"][i] for i in activity["validOrders"][0])


class LegacySentenceStructureTest(unittest.TestCase):
    def test_each_level_has_at_least_fifty_distinct_frames(self):
        self.assertEqual(set(generate.LEVELS), set(STRUCTURES))
        for level, frames in STRUCTURES.items():
            with self.subTest(level=level):
                # Ignore phrase boundaries, capitalization and vocabulary slots.
                normalized = {" ".join(frame.replace("|", " ").lower().split()) for frame in frames}
                self.assertGreaterEqual(len(normalized), 50)
                self.assertEqual(len(frames), len(normalized))
                for frame in frames:
                    self.assertEqual(1, frame.count("{answer}"))
                    self.assertEqual(1, frame.count("{reason}"))
                    self.assertTrue(frame.endswith("."))
                    bounds = {"SPROUT": (3, 5), "TRAIL": (4, 6), "SUMMIT": (5, 7)}
                    low, high = bounds[level]
                    self.assertTrue(low <= len(frame.split("|")) <= high, frame)

    def test_emitted_sentences_cover_frames_evenly_and_keep_evidence(self):
        for level in generate.LEVELS:
            activities = list(scramble(level))
            self.assertEqual(997, len(activities))
            self.assertEqual(activities, list(scramble(level)))
            frames = Counter()
            families = defaultdict(set)
            answers = set()
            previous_frame = None
            expected_frames = {frame.replace("|", " ").lower() for frame in STRUCTURES[level]}
            for activity, (family, item, role, description, reason) in zip(activities, scenes(level)):
                with self.subTest(level=level, title=activity["title"]):
                    tiles = activity["tiles"]
                    self.assertEqual(len(tiles), len(set(tiles)))
                    self.assertEqual(1, len(activity["validOrders"]))
                    order = activity["validOrders"][0]
                    self.assertEqual(list(range(len(tiles))), sorted(order))
                    self.assertNotEqual(list(range(len(tiles))), order)
                    self.assertTrue(all(tile.strip() == tile and "  " not in tile for tile in tiles))
                    text = sentence(activity)
                    self.assertTrue(text[0].isupper())
                    self.assertTrue(text.endswith("."))
                    self.assertNotRegex(text, r"[{}|]")
                    self.assertIn(description.lower(), text.lower())
                    self.assertIn(reason.lower(), text.lower())
                    frame = (text.lower().replace(description.lower(), "{answer}")
                             .replace(reason.lower(), "{reason}").replace(role, "{role}"))
                    self.assertIn(frame, expected_frames)
                    self.assertNotEqual(previous_frame, frame)
                    previous_frame = frame
                    frames[frame] += 1
                    families[family].add(frame)
                    q = activity["question"]
                    self.assertEqual(3 if level == "SPROUT" else 4, len(q["choices"]))
                    self.assertEqual(len(q["choices"]), len(set(q["choices"])))
                    answer = q["choices"][q["answer"]]
                    self.assertNotIn(answer.lower(), text.lower())
                    self.assertNotIn(answer, answers)
                    answers.add(answer)
                    self.assertIn(item, q["prompt"])
            self.assertEqual(expected_frames, set(frames))
            self.assertEqual({19, 20}, set(frames.values()))
            self.assertEqual(20, len(families))
            self.assertTrue(all(len(used) >= 49 for used in families.values()))

    def test_missing_word_keeps_its_original_sentence_patterns(self):
        for level in generate.LEVELS:
            activities = list(missing(level))
            self.assertEqual(997, len(activities))
            for activity, (_, item, role, _, reason) in zip(activities, scenes(level)):
                if level == "SPROUT":
                    expected = f"I choose ___ because {reason}."
                elif level == "TRAIL":
                    expected = f"Because {reason}, the {role} chooses ___."
                else:
                    expected = (
                        f"Although another {item} is available, "
                        f"the {role} selects ___ because {reason}."
                    )
                self.assertEqual([expected], activity["passage"])

    def test_unknown_levels_fail_explicitly(self):
        for generator in (missing, scramble, grammar, word_scramble):
            with self.assertRaisesRegex(ValueError, "Unknown reading level"):
                list(generator("UNKNOWN"))


class WordSentenceStructureTest(unittest.TestCase):
    def test_sixty_distinct_frames_and_a_thousand_distinct_sentences_per_level(self):
        self.assertEqual(60, len(SCENES))
        self.assertEqual(60, len({scene.title for scene in SCENES}))
        self.assertGreaterEqual(len(VOCABULARY), 10)
        for vocabulary in VOCABULARY.values():
            self.assertEqual(20, len(set(vocabulary)))
            self.assertTrue(all(len(word.split()) == 1 for word in vocabulary))
        for level_index, level in enumerate(generate.LEVELS):
            activities = list(word_scramble(level))
            self.assertEqual(1000, len(activities))
            frames = {scene.sentences[level_index].casefold() for scene in SCENES}
            self.assertEqual(60, len(frames))
            self.assertEqual(1000, len({sentence(a).casefold() for a in activities}))
            counts = Counter()
            for index, activity in enumerate(activities):
                variant, scene_index = divmod(index, len(SCENES))
                scene = SCENES[scene_index]
                words = VOCABULARY[scene.vocabulary]
                item = words[(variant + scene_index * 7) % len(words)]
                frame = scene.sentences[level_index]
                with self.subTest(level=level, title=activity["title"]):
                    self.assertEqual(1, frame.count("{item}"))
                    self.assertEqual(frame.format(item=item), sentence(activity))
                    self.assertEqual(f"{scene.title}: {item}", activity["title"])
                counts[frame] += 1
            self.assertEqual({16, 17}, set(counts.values()))
            self.assertEqual(60, len(counts))

    def test_every_tile_is_one_word_with_punctuation_and_a_distinct_position(self):
        for level in generate.LEVELS:
            low, high = WORD_BOUNDS[level]
            repeated_labels = 0
            for activity in word_scramble(level):
                with self.subTest(level=level, title=activity["title"]):
                    tiles = activity["tiles"]
                    self.assertTrue(low <= len(tiles) <= high)
                    self.assertTrue(activity["validOrders"])
                    for order in activity["validOrders"]:
                        self.assertEqual(list(range(len(tiles))), sorted(order))
                        self.assertNotEqual(list(range(len(tiles))), order)
                        self.assertNotEqual(tiles, [tiles[i] for i in order])
                    for tile in tiles:
                        self.assertRegex(tile, r"^[A-Za-z]+(?:'[A-Za-z]+)?[,!.?]?$")
                    text = sentence(activity)
                    self.assertTrue(text[0].isupper())
                    self.assertIn(text[-1], ".!?")
                    self.assertNotRegex(text, r"[{}|]")
                    repeated_labels += len(set(tiles)) < len(tiles)
            self.assertGreater(repeated_labels, 0)

    def test_bonuses_and_hints_match_each_sentence(self):
        for level in generate.LEVELS:
            for index, activity in enumerate(word_scramble(level)):
                scene = SCENES[index % len(SCENES)]
                text = sentence(activity)
                q = activity["question"]
                with self.subTest(level=level, title=activity["title"]):
                    self.assertEqual(3 if level == "SPROUT" else 4, len(q["choices"]))
                    self.assertEqual(len(q["choices"]), len(set(q["choices"])))
                    self.assertIn(q["choices"][q["answer"]].casefold(), text.casefold())
                    self.assertIn(scene.hint, activity["hint"])
                    self.assertTrue(activity["hint"].startswith(f"Start with '{text.split()[0]}'."))
                    self.assertIn(f"End with '{text.split()[-1]}'.", activity["hint"])
                    self.assertNotRegex(q["prompt"], r"[{}|]")
                    self.assertNotRegex(activity["hint"], r"[{}|]")
                    self.assertEqual(
                        {"symbol": "📖", "description": "An open book for word-by-word reading practice."},
                        activity["picture"],
                    )

    def test_curated_adverb_alternatives_preserve_words_punctuation_and_bonus_evidence(self):
        counts = Counter()
        for level_index, level in enumerate(generate.LEVELS):
            for index, activity in enumerate(word_scramble(level)):
                scene = SCENES[index % len(SCENES)]
                vocabulary = VOCABULARY[scene.vocabulary]
                item = vocabulary[(index // len(SCENES) + index % len(SCENES) * 7) % len(vocabulary)]
                expected = [
                    scene.sentences[level_index].format(item=item),
                    *(text.format(item=item) for text in scene.alternatives[level_index]),
                ]
                rendered = [
                    " ".join(activity["tiles"][i] for i in order)
                    for order in activity["validOrders"]
                ]
                with self.subTest(level=level, title=scene.title):
                    self.assertEqual(expected, rendered)
                    self.assertEqual(len(rendered), len(set(rendered)))
                    for text in rendered:
                        self.assertEqual(Counter(expected[0].split()), Counter(text.split()))
                        self.assertIn(scene.correct.format(item=item).casefold(), text.casefold())
                        self.assertEqual(expected[0].split()[0], text.split()[0])
                        self.assertEqual(expected[0].split()[-1], text.split()[-1])
                counts[level] += len(rendered) > 1
        self.assertGreater(counts["TRAIL"], 0)
        self.assertGreater(counts["SUMMIT"], counts["TRAIL"])

    def test_no_accepted_alternative_is_used_as_the_starting_order(self):
        ordered = ["I", "am", "holding", "the", "shell", "carefully", "in", "both", "hands."]
        initial, _ = scramble_tiles(ordered)
        tiles, orders = _word_orders(ordered, [initial])
        self.assertNotIn(tiles, [ordered, initial])
        self.assertEqual(
            [ordered, initial],
            [[tiles[i] for i in order] for order in orders],
        )
        with self.assertRaisesRegex(ValueError, "same punctuation-attached word tiles"):
            _word_orders(["The", "fox", "sleeps", "peacefully."],
                         [["The", "fox", "peacefully", "sleeps."]])

    def test_reproducible_generation_returns_independent_objects(self):
        for level in generate.LEVELS:
            first = list(word_scramble(level))
            second = list(word_scramble(level))
            self.assertEqual(first, second)
            first[0]["tiles"].reverse()
            first[0]["validOrders"][0].reverse()
            first[0]["question"]["choices"].reverse()
            self.assertEqual(second, list(word_scramble(level)))

    def test_repeated_word_occurrences_never_start_in_a_solved_rendered_order(self):
        for size in range(2, 8):
            for labels in product(("the", "cat"), repeat=size):
                if len(set(labels)) == 1:
                    continue
                with self.subTest(labels=labels):
                    tiles, orders = scramble_tiles(labels)
                    self.assertEqual(Counter(labels), Counter(tiles))
                    self.assertNotEqual(list(labels), tiles)
                    self.assertEqual(list(range(size)), sorted(orders[0]))
                    self.assertEqual(list(labels), [tiles[i] for i in orders[0]])
                    self.assertEqual((tiles, orders), scramble_tiles(labels))
        for labels in ([], ["cat"], ["the", "the"]):
            with self.assertRaisesRegex(ValueError, "at least two different labels"):
                scramble_tiles(labels)

    def test_scramble_answers_are_scoped_to_sentence_not_cosmetics_or_tile_indexes(self):
        contents = list(word_scramble("SPROUT"))
        original = {"game": "SCRAMBLE", "level": "SPROUT", **contents[0]}
        contextual = {"game": "SCRAMBLE", "level": "SPROUT", **contents[len(SCENES)]}
        self.assertEqual(set(original["question"]["choices"]), set(contextual["question"]["choices"]))
        self.assertNotEqual(generate.answer_keys(original), generate.answer_keys(contextual))
        self.assertEqual(
            generate.answer_keys(original, contextual_scramble=False),
            generate.answer_keys(contextual, contextual_scramble=False),
        )
        cosmetic = deepcopy(original)
        cosmetic.update(id="new-id", title="New title", hint="A cosmetic hint.")
        cosmetic["tiles"] = list(reversed(original["tiles"]))
        cosmetic["validOrders"] = [
            [len(original["tiles"]) - 1 - index for index in order]
            for order in original["validOrders"]
        ]
        q = cosmetic["question"]
        correct = q["choices"].pop(q["answer"])
        q["choices"].append(f"  {correct.upper()}  ")
        q["answer"] = len(q["choices"]) - 1
        self.assertEqual(generate.answer_keys(original), generate.answer_keys(cosmetic))
        self.assertEqual(4, len(generate.answer_keys(original)[0]))


class GrammarLessonTest(unittest.TestCase):
    def test_fifty_distinct_curated_objectives_per_level_cover_all_word_classes(self):
        self.assertEqual(set(generate.LEVELS), set(LESSONS))
        expected_focuses = {
            "noun", "verb", "adjective", "adverb", "pronoun", "preposition", "conjunction", "sentence",
        }
        tasks = set()
        for level, lessons in LESSONS.items():
            with self.subTest(level=level):
                self.assertEqual(50, len(lessons))
                self.assertEqual(50, len({lesson.slug for lesson in lessons}))
                self.assertEqual(50, len({lesson.title for lesson in lessons}))
                self.assertEqual(expected_focuses, {lesson.focus for lesson in lessons})
                self.assertTrue(all(count >= 3 for count in Counter(
                    lesson.focus for lesson in lessons
                ).values()))
                for lesson in lessons:
                    self.assertRegex(lesson.slug, r"^[a-z]+(?:-[a-z]+)*$")
                    task = (lesson.passage, lesson.prompt)
                    self.assertNotIn(task, tasks)
                    tasks.add(task)
        self.assertEqual(150, len(tasks))

    def test_schema_feedback_neutral_pictures_and_balanced_choices(self):
        fields = {"id", "game", "level", "title", "hint", "passage", "question", "picture", "explanation"}
        ids = set()
        for level in generate.LEVELS:
            activities = list(grammar(level))
            choice_count = 3 if level == "SPROUT" else 4
            self.assertEqual(1000, len(activities))
            expected_positions = {0: 334, 1: 333, 2: 333} if choice_count == 3 else dict.fromkeys(range(4), 250)
            self.assertEqual(expected_positions, Counter(a["question"]["answer"] for a in activities))
            for activity, lesson in zip(activities, grammar_lessons(level)):
                with self.subTest(activity=activity["id"]):
                    self.assertEqual(fields, set(activity))
                    self.assertEqual(f"grammar-{level.lower()}-{lesson.slug}", activity["id"])
                    self.assertNotIn(activity["id"], ids)
                    ids.add(activity["id"])
                    self.assertEqual("GRAMMAR", activity["game"])
                    self.assertEqual(level, activity["level"])
                    self.assertEqual([lesson.passage], activity["passage"])
                    self.assertEqual(lesson.title, activity["title"])
                    q = activity["question"]
                    self.assertEqual({"prompt", "choices", "answer"}, set(q))
                    self.assertEqual(lesson.prompt, q["prompt"])
                    self.assertEqual(choice_count, len(q["choices"]))
                    self.assertEqual(choice_count, len({c.strip() for c in q["choices"]}))
                    concept = lesson.slug.rsplit("-", 1)[0] if lesson.slug[-2:].isdigit() else lesson.slug
                    if concept not in {"proper-name-capital", "sentence-capital"}:
                        self.assertEqual(choice_count, len({c.casefold() for c in q["choices"]}))
                    self.assertIs(type(q["answer"]), int)
                    self.assertTrue(0 <= q["answer"] < choice_count)
                    self.assertEqual(lesson.correct, q["choices"][q["answer"]])
                    self.assertEqual(set((lesson.correct, *lesson.distractors)), set(q["choices"]))
                    self.assertEqual(lesson.hint, activity["hint"])
                    self.assertEqual(lesson.explanation, activity["explanation"])
                    self.assertNotEqual(activity["hint"], activity["explanation"])
                    self.assertGreater(len(activity["hint"]), 25)
                    self.assertGreaterEqual(len(activity["explanation"]), 40)
                    for text in (activity["title"], activity["hint"], activity["explanation"],
                                 *activity["passage"], q["prompt"], *q["choices"]):
                        self.assertIsInstance(text, str)
                        self.assertEqual(text, text.strip())
                        self.assertTrue(text)
                        self.assertNotRegex(text, r"[{}|]|\\u[0-9a-fA-F]{4}")
                    self.assertEqual(
                        {"symbol": "📖", "description": "An open book for reading practice."},
                        activity["picture"],
                    )
        self.assertEqual(3000, len(ids))

    def test_each_concept_has_twenty_distinct_contexts_and_stable_variant_ids(self):
        for level in generate.LEVELS:
            lessons = grammar_lessons(level)
            self.assertEqual(LESSONS[level], lessons[:50])
            by_slug = {lesson.slug: lesson for lesson in lessons}
            self.assertEqual(1000, len(by_slug))
            tasks = [(lesson.passage.casefold(), lesson.prompt.casefold()) for lesson in lessons]
            self.assertEqual(len(tasks), len(set(tasks)))
            for original in LESSONS[level]:
                variants = [original] + [
                    by_slug[f"{original.slug}-{index:02d}"] for index in range(2, 21)
                ]
                with self.subTest(level=level, concept=original.slug):
                    self.assertEqual({original.focus}, {lesson.focus for lesson in variants})
                    self.assertEqual(20, len({lesson.passage.casefold() for lesson in variants}))
                    self.assertTrue(all(lesson.hint != lesson.explanation for lesson in variants))

    def test_progression_and_representative_teaching_answers(self):
        expected = {
            "SPROUT": {
                "noun-person": "baker", "noun-plural-es": "boxes", "noun-irregular-plural": "children",
                "present-singular-agreement": "sings", "present-plural-agreement": "sing",
                "past-irregular": "went", "adjective-comparative": "longer", "adverb-when": "soon",
                "pronoun-object-person": "me", "preposition-between": "between",
                "conjunction-reason": "because", "article-a-an": "an",
            },
            "TRAIL": {
                "noun-abstract": "kindness", "noun-collective": "flock", "plural-consonant-y": "stories",
                "possessive-singular": "the cat's bowl", "possessive-plural": "the players' lockers",
                "past-continuous": "was eating", "negative-did-base": "lose", "past-final-e": "baked",
                "past-double-consonant": "stopped", "past-consonant-y": "cried",
                "adverb-well": "well", "pronoun-compound-subject": "I", "pronoun-compound-object": "me",
                "preposition-duration": "for", "preposition-start-time": "since",
                "conjunction-concession": "Although", "article-vowel-sound": "an",
            },
            "SUMMIT": {
                "noun-gerund-subject": "Swimming", "compound-noun-plural": "passers-by",
                "irregular-plural-possessive": "the children's toys", "mass-noun-less": "less",
                "count-noun-fewer": "fewer", "agreement-intervening-phrase": "sits",
                "agreement-each": "has", "present-perfect": "have painted", "past-perfect": "had left",
                "future-perfect": "will have finished", "present-perfect-continuous": "have been reading",
                "passive-agent": "the gardener", "conditional-imagined": "would fly",
                "relative-pronoun-possessive": "whose", "relative-pronoun-object": "whom",
                "preposition-versus-conjunction": "the first sentence",
                "preposition-versus-adverb": "the second sentence", "conjunction-unless": "if it does not rain",
                "parallel-list": "reading, drawing, and singing",
            },
        }
        for level, answers in expected.items():
            by_slug = {lesson.slug: lesson for lesson in LESSONS[level]}
            for slug, correct in answers.items():
                with self.subTest(level=level, concept=slug):
                    self.assertEqual(correct, by_slug[slug].correct)
        word_counts = [
            sum(len(lesson.passage.split()) + len(lesson.prompt.split()) for lesson in LESSONS[level])
            for level in generate.LEVELS
        ]
        self.assertLess(word_counts[0], word_counts[1])
        self.assertLess(word_counts[1], word_counts[2])
        sprout_concepts = {lesson.slug for lesson in LESSONS["SPROUT"]}
        self.assertFalse(sprout_concepts.intersection({
            "past-perfect", "future-perfect", "relative-pronoun-object", "modifier-attachment",
        }))
        self.assertIn("tense-consistency", {lesson.slug for lesson in LESSONS["TRAIL"]})
        self.assertIn("modifier-attachment", {lesson.slug for lesson in LESSONS["SUMMIT"]})

    def test_prompts_distinguish_otherwise_possible_wordings(self):
        sprout = {lesson.slug: lesson for lesson in LESSONS["SPROUT"]}
        summit = {lesson.slug: lesson for lesson in LESSONS["SUMMIT"]}
        self.assertIn("correctly ordered", sprout["adjective-before-noun"].prompt)
        self.assertIn("immediately before", sprout["adjective-before-noun"].prompt)
        self.assertIn("future-perfect phrase", summit["future-perfect"].prompt)
        self.assertIn("'We' is the subject doing", summit["relative-pronoun-object"].hint)

    def test_repeatable_generation_returns_independent_objects(self):
        for level in generate.LEVELS:
            first = list(grammar(level))
            second = list(grammar(level))
            self.assertEqual(first, second)
            first[0]["question"]["choices"].reverse()
            first[0]["passage"].append("A changed draft.")
            self.assertEqual(second, list(grammar(level)))

    def test_grammar_context_does_not_change_other_non_scramble_answer_keys(self):
        original = next(grammar("SPROUT"))
        cosmetic = deepcopy(original)
        cosmetic.update(id="different-id", title="Different title", explanation="Different feedback.")
        correct = cosmetic["question"]["choices"].pop(cosmetic["question"]["answer"])
        cosmetic["question"]["choices"].append(correct)
        cosmetic["question"]["answer"] = len(cosmetic["question"]["choices"]) - 1
        self.assertEqual(generate.task_identity(original), generate.task_identity(cosmetic))
        self.assertEqual(generate.answer_keys(original), generate.answer_keys(cosmetic))
        contextual = deepcopy(original)
        contextual["passage"] = ["We thanked the baker."]
        self.assertNotEqual(generate.task_identity(original), generate.task_identity(contextual))
        self.assertNotEqual(generate.answer_keys(original), generate.answer_keys(contextual))
        for game in ("MISSING", "SEQUENCE", "DETECTIVE"):
            original["game"] = contextual["game"] = game
            self.assertEqual(generate.answer_keys(original), generate.answer_keys(contextual))
            self.assertEqual(3, len(generate.answer_keys(original)[0]))
            normalized = deepcopy(original)
            q = normalized["question"]
            q["choices"][q["answer"]] = f'  {q["choices"][q["answer"]].upper()}  '
            self.assertEqual(generate.answer_keys(original), generate.answer_keys(normalized))


class PublishedPackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data, cls.manifest = generate.build_pack()
        cls.activities = [json.loads(line) for line in cls.data.splitlines()]
        cls.previous_data = (generate.ROOT / generate.V9_FILE).read_bytes()
        cls.previous = [json.loads(line) for line in cls.previous_data.splitlines()]
        cls.legacy_manifest = json.loads((generate.ROOT / "manifest.json").read_text(encoding="utf-8"))
        cls.legacy_data = (generate.ROOT / cls.legacy_manifest["file"]).read_bytes()

    def test_published_files_reproduce_exactly(self):
        self.assertEqual("v10", self.manifest["version"])
        self.assertEqual(self.data, (generate.ROOT / self.manifest["file"]).read_bytes())
        self.assertEqual(len(self.data), self.manifest["bytes"])
        self.assertEqual(hashlib.sha256(self.data).hexdigest(), self.manifest["sha256"])
        self.assertEqual(
            self.manifest, json.loads((generate.ROOT / generate.V8_ENTRY_POINT).read_text(encoding="utf-8")),
        )

    def test_repeat_build_is_byte_identical(self):
        self.assertEqual((self.data, self.manifest), generate.build_pack())

    def test_v7_compatibility_entry_points_and_immutable_pack_are_preserved(self):
        checksum = "55c35e032403a5167cb3ca8cc69d17268b001e1f9e807ce858ebcf66d597c2de"
        self.assertEqual("v7", self.legacy_manifest["version"])
        self.assertEqual(f"activities-{checksum}.jsonl", self.legacy_manifest["file"])
        self.assertEqual(checksum, hashlib.sha256(self.legacy_data).hexdigest())
        self.assertEqual(19_228_398, len(self.legacy_data))
        self.assertEqual(
            (generate.ROOT / "manifest.json").read_bytes(),
            (generate.ROOT / "reading-activities-import.json").read_bytes(),
        )
        self.assertEqual(15_000, len(self.legacy_data.splitlines()))
        self.assertEqual(self.legacy_data, b"".join(self.previous_data.splitlines(keepends=True)[:15_000]))
        self.assertEqual(
            [activity for section in (slice(0, 50), slice(50, None))
             for level in generate.LEVELS for activity in list(grammar(level))[section]],
            self.activities[15_000:],
        )

    def test_v9_reproduces_exactly_and_preserves_the_v8_prefix(self):
        reproduced, manifest = generate.build_v9_pack()
        self.assertEqual("v9", manifest["version"])
        self.assertEqual(generate.V9_SHA256, manifest["sha256"])
        self.assertEqual(21_153_420, manifest["bytes"])
        self.assertEqual(self.previous_data, reproduced)
        archived = json.loads(generate.archived_file("manifest-v8.json"))
        self.assertEqual("v8", archived["version"])
        self.assertEqual("e69b6c094a096ca840a2a06e4bbecaceee21a2832503f186f26e205c0e5a657d",
                         archived["sha256"])
        original_data = generate.archived_file(archived["file"])
        self.assertEqual(15_150, len(original_data.splitlines()))
        self.assertEqual(original_data, b"".join(self.previous_data.splitlines(keepends=True)[:15_150]))

    def test_all_catalogues_have_only_supported_games_and_schema_fields(self):
        legacy_games = {"MISSING", "SCRAMBLE", "SEQUENCE", "DETECTIVE", "STUDIO"}
        fields = {
            "id", "game", "level", "title", "hint", "passage", "question", "tiles",
            "validOrders", "picture", "eventPictures", "evidence", "writingChoices", "explanation",
        }
        self.assertEqual(legacy_games, set(generate.GENERATORS))
        with ZipFile(generate.ARCHIVE) as archive:
            archived_names = archive.namelist()
        self.assertEqual(len(archived_names), len(set(archived_names)))
        archived_names = set(archived_names)
        archived_manifests = {name for name in archived_names if name.endswith(".json")}
        self.assertEqual(
            {f"manifest-v{version}.json" for version in (1, 2, 3, 4, 8)}, archived_manifests,
        )
        live_manifests = {path.name for path in generate.ROOT.glob("*.json")}
        self.assertEqual(
            {"manifest.json", "reading-activities-import.json", generate.V8_ENTRY_POINT}, live_manifests,
        )
        referenced = {False: set(), True: set()}
        for name in sorted(live_manifests | archived_manifests):
            archived = name in archived_manifests
            read = generate.archived_file if archived else lambda name: (generate.ROOT / name).read_bytes()
            with self.subTest(manifest=name):
                manifest = json.loads(read(name))
                referenced[archived].add(manifest["file"])
                data = read(manifest["file"])
                self.assertEqual(1, manifest["schemaVersion"])
                self.assertEqual(len(data), manifest["bytes"])
                self.assertEqual(hashlib.sha256(data).hexdigest(), manifest["sha256"])
                self.assertEqual(f'activities-{manifest["sha256"]}.jsonl', manifest["file"])
                self.assertTrue(data.endswith(b"\n"))
                self.assertLessEqual(len(data), generate.MAX_PACK_BYTES)
                self.assertTrue(all(
                    len(line) + 1 <= generate.MAX_LINE_BYTES for line in data.splitlines()
                ))
                activities = [json.loads(line) for line in data.splitlines()]
                games = legacy_games | ({"GRAMMAR"} if manifest["version"] in ("v8", "v9", "v10") else set())
                self.assertEqual(games, {a["game"] for a in activities})
                self.assertTrue(all(set(a) <= fields for a in activities))
                for activity in activities:
                    if activity["game"] == "GRAMMAR":
                        self.assertIsInstance(activity["explanation"], str)
                        self.assertTrue(activity["explanation"].strip())
                    else:
                        self.assertNotIn("explanation", activity)
                self.assertEqual(len(activities), len({a["id"] for a in activities}))
                levels = generate.LEVELS[:2] if manifest["version"] == "v1" else generate.LEVELS
                per_pool = 3 if manifest["version"] in ("v1", "v2") else 1000
                expected = {f"{game}:{level}": per_pool for game in legacy_games for level in levels}
                if manifest["version"] in ("v8", "v9", "v10"):
                    grammar_count = 50 if manifest["version"] == "v8" else 1000
                    expected.update({f"GRAMMAR:{level}": grammar_count for level in levels})
                counts = Counter(f'{a["game"]}:{a["level"]}' for a in activities)
                self.assertEqual(expected, counts)
                self.assertEqual(expected, manifest["counts"])
        self.assertEqual(
            referenced[False] | {generate.V9_FILE},
            {path.name for path in generate.ROOT.glob("activities-*.jsonl")},
        )
        self.assertEqual(2, len(referenced[False]))
        self.assertEqual(archived_names, archived_manifests | referenced[True])
        self.assertFalse(archived_names.intersection(path.name for path in generate.ROOT.iterdir()))

    def test_all_scrambles_change_with_stable_ids_and_every_other_v9_line_is_unchanged(self):
        self.assertEqual(18_000, len(self.activities))
        self.assertEqual(len(self.previous), len(self.activities))
        old_lines = self.previous_data.splitlines(keepends=True)
        new_lines = self.data.splitlines(keepends=True)
        revised = Counter()
        revised_originals = Counter()
        for index, (old, new) in enumerate(zip(self.previous, self.activities)):
            self.assertEqual(
                (old["id"], old["game"], old["level"]), (new["id"], new["game"], new["level"]),
            )
            if new["game"] == "SCRAMBLE":
                self.assertNotEqual(old, new)
                revised[new["level"]] += 1
                if index < 45:
                    revised_originals[new["level"]] += 1
            else:
                self.assertEqual(old, new)
                self.assertEqual(old_lines[index], new_lines[index])
        self.assertEqual(dict.fromkeys(generate.LEVELS, 1000), dict(revised))
        self.assertEqual(dict.fromkeys(generate.LEVELS, 3), dict(revised_originals))

    def test_legacy_catalogues_remain_exact_prefixes_of_v9(self):
        for version, count in (("v1", 30), ("v2", 45)):
            with self.subTest(version=version):
                manifest = json.loads(generate.archived_file(f"manifest-{version}.json"))
                data = generate.archived_file(manifest["file"])
                self.assertEqual(count, len(data.splitlines()))
                self.assertEqual(data, b"".join(self.previous_data.splitlines(keepends=True)[:count]))

    def test_published_scrambles_use_the_varied_generator(self):
        for level in generate.LEVELS:
            published = [
                a for a in self.activities if a["game"] == "SCRAMBLE" and a["level"] == level
            ]
            expected = list(word_scramble(level))
            self.assertEqual(len(expected), len(published))
            for content, activity in zip(expected, published):
                self.assertEqual(content, {k: v for k, v in activity.items() if k not in ("id", "game", "level")})

    def test_pool_counts_answers_and_import_limits(self):
        counts = Counter(f'{a["game"]}:{a["level"]}' for a in self.activities)
        expected = {f"{game}:{level}": 1000 for game in generate.GENERATORS for level in generate.LEVELS}
        expected.update({f"GRAMMAR:{level}": 1000 for level in generate.LEVELS})
        self.assertEqual(18, len(counts))
        self.assertEqual(expected, counts)
        self.assertEqual(dict(counts), self.manifest["counts"])
        self.assertEqual(18_000, len({a["id"] for a in self.activities}))
        self.assertLessEqual(len(self.data), generate.MAX_PACK_BYTES)
        self.assertTrue(all(len(line) + 1 <= generate.MAX_LINE_BYTES for line in self.data.splitlines()))
        answers = [key for a in self.activities for key in generate.answer_keys(a)]
        self.assertEqual(len(answers), len(set(answers)))
        identities = [generate.task_identity(a) for a in self.activities]
        self.assertEqual(len(identities), len(set(identities)))


if __name__ == "__main__":
    unittest.main()
