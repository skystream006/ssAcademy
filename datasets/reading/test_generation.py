"""Standard-library regression checks for the published reading catalogue."""

from collections import Counter, defaultdict
from copy import deepcopy
import hashlib
import json
import unittest

import generate
from generation.grammar_games import LESSONS, grammar, grammar_lessons
from generation.scramble_structures import STRUCTURES
from generation.word_games import (
    _SELECTION_GROUPS,
    _SELECTIONS,
    _situations,
    missing,
    scramble,
)


def scenes(level):
    for family, item, original in _situations(level):
        group, role = _SELECTION_GROUPS[family.title]
        frame_index = getattr(family, level.lower()).index(original)
        scene = _SELECTIONS[group][frame_index]
        _, reason = scene.variants[generate.LEVELS.index(level)]
        yield family.title, item, role, scene.description.format(item=item), reason


def sentence(activity):
    return " ".join(activity["tiles"][i] for i in activity["validOrders"][0])


class SentenceStructureTest(unittest.TestCase):
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
        for generator in (missing, scramble, grammar):
            with self.assertRaisesRegex(ValueError, "Unknown reading level"):
                list(generator("UNKNOWN"))


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

    def test_contextual_answer_identity_is_only_for_grammar(self):
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
        for game in ("MISSING", "SCRAMBLE", "SEQUENCE", "DETECTIVE"):
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
        baseline = json.loads((generate.ROOT / "manifest-v4.json").read_text(encoding="utf-8"))
        cls.previous_data = (generate.ROOT / baseline["file"]).read_bytes()
        cls.previous = [json.loads(line) for line in cls.previous_data.splitlines()]
        cls.legacy_manifest = json.loads((generate.ROOT / "manifest.json").read_text(encoding="utf-8"))
        cls.legacy_data = (generate.ROOT / cls.legacy_manifest["file"]).read_bytes()

    def test_published_files_reproduce_exactly(self):
        self.assertEqual("v9", self.manifest["version"])
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
        self.assertEqual(self.legacy_data, b"".join(self.data.splitlines(keepends=True)[:15_000]))
        self.assertEqual(
            [activity for section in (slice(0, 50), slice(50, None))
             for level in generate.LEVELS for activity in list(grammar(level))[section]],
            self.activities[15_000:],
        )

    def test_all_v8_questions_and_ids_remain_byte_identical(self):
        archived = json.loads((generate.ROOT / "manifest-v8.json").read_text(encoding="utf-8"))
        self.assertEqual("v8", archived["version"])
        self.assertEqual("e69b6c094a096ca840a2a06e4bbecaceee21a2832503f186f26e205c0e5a657d",
                         archived["sha256"])
        original_data = (generate.ROOT / archived["file"]).read_bytes()
        self.assertEqual(15_150, len(original_data.splitlines()))
        self.assertEqual(original_data, b"".join(self.data.splitlines(keepends=True)[:15_150]))

    def test_all_catalogues_have_only_supported_games_and_schema_fields(self):
        legacy_games = {"MISSING", "SCRAMBLE", "SEQUENCE", "DETECTIVE", "STUDIO"}
        fields = {
            "id", "game", "level", "title", "hint", "passage", "question", "tiles",
            "validOrders", "picture", "eventPictures", "evidence", "writingChoices", "explanation",
        }
        self.assertEqual(legacy_games, set(generate.GENERATORS))
        referenced = set()
        manifests = set(generate.ROOT.glob("manifest*.json")) | set(
            generate.ROOT.glob("reading-activities*-import.json")
        )
        self.assertIn(generate.ROOT / generate.V8_ENTRY_POINT, manifests)
        for path in sorted(manifests):
            with self.subTest(manifest=path.name):
                manifest = json.loads(path.read_text(encoding="utf-8"))
                referenced.add(manifest["file"])
                data = (generate.ROOT / manifest["file"]).read_bytes()
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
                games = legacy_games | ({"GRAMMAR"} if manifest["version"] in ("v8", "v9") else set())
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
                if manifest["version"] in ("v8", "v9"):
                    grammar_count = 50 if manifest["version"] == "v8" else 1000
                    expected.update({f"GRAMMAR:{level}": grammar_count for level in levels})
                counts = Counter(f'{a["game"]}:{a["level"]}' for a in activities)
                self.assertEqual(expected, counts)
                self.assertEqual(expected, manifest["counts"])
        self.assertEqual(
            referenced, {path.name for path in generate.ROOT.glob("activities-*.jsonl")},
        )

    def test_only_generated_scrambles_change_and_keep_their_ids(self):
        self.assertEqual(18_000, len(self.activities))
        self.assertEqual(len(self.previous), len(self.activities[:15_000]))
        old_lines = self.previous_data.splitlines(keepends=True)
        new_lines = self.data.splitlines(keepends=True)
        self.assertEqual(
            b"".join(old_lines[:45]),
            b"".join(new_lines[:45]),
        )
        revised = Counter()
        for index, (old, new) in enumerate(zip(self.previous, self.activities)):
            self.assertEqual(
                (old["id"], old["game"], old["level"]), (new["id"], new["game"], new["level"]),
            )
            if index >= 45 and new["game"] == "SCRAMBLE":
                self.assertNotEqual(old, new)
                revised[new["level"]] += 1
            else:
                self.assertEqual(old, new)
                self.assertEqual(old_lines[index], new_lines[index])
        self.assertEqual(dict.fromkeys(generate.LEVELS, 997), dict(revised))

    def test_legacy_catalogues_remain_exact_prefixes(self):
        for version, count in (("v1", 30), ("v2", 45)):
            with self.subTest(version=version):
                manifest = json.loads(
                    (generate.ROOT / f"manifest-{version}.json").read_text(encoding="utf-8"),
                )
                data = (generate.ROOT / manifest["file"]).read_bytes()
                self.assertEqual(count, len(data.splitlines()))
                self.assertEqual(data, b"".join(self.data.splitlines(keepends=True)[:count]))

    def test_published_scrambles_use_the_varied_generator(self):
        for level in generate.LEVELS:
            published = [
                a for a in self.activities[45:] if a["game"] == "SCRAMBLE" and a["level"] == level
            ]
            expected = list(scramble(level))
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
