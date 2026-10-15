"""Standard-library regression checks for the published reading catalogue."""

from collections import Counter, defaultdict
import hashlib
import json
import unittest

import generate
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
        for generator in (missing, scramble):
            with self.assertRaisesRegex(ValueError, "Unknown reading level"):
                list(generator("UNKNOWN"))


class PublishedPackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data, cls.manifest = generate.build_pack()
        cls.activities = [json.loads(line) for line in cls.data.splitlines()]
        baseline = json.loads((generate.ROOT / "manifest-v4.json").read_text(encoding="utf-8"))
        cls.previous_data = (generate.ROOT / baseline["file"]).read_bytes()
        cls.previous = [json.loads(line) for line in cls.previous_data.splitlines()]

    def test_published_files_reproduce_exactly(self):
        self.assertEqual("v5", self.manifest["version"])
        self.assertEqual(self.data, (generate.ROOT / self.manifest["file"]).read_bytes())
        self.assertEqual(len(self.data), self.manifest["bytes"])
        self.assertEqual(hashlib.sha256(self.data).hexdigest(), self.manifest["sha256"])
        for name in ("manifest.json", "reading-activities-import.json"):
            self.assertEqual(
                self.manifest, json.loads((generate.ROOT / name).read_text(encoding="utf-8")),
            )

    def test_all_catalogues_have_only_supported_games_and_schema_fields(self):
        games = {"MISSING", "SCRAMBLE", "SEQUENCE", "DETECTIVE", "CAPTION", "STUDIO"}
        fields = {
            "id", "game", "level", "title", "hint", "passage", "question", "tiles",
            "validOrders", "picture", "eventPictures", "evidence", "writingChoices",
        }
        self.assertEqual(games, set(generate.GENERATORS))
        referenced = set()
        for path in sorted(generate.ROOT.glob("manifest*.json")):
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
                self.assertEqual(games, {a["game"] for a in activities})
                self.assertTrue(all(set(a) <= fields for a in activities))
                self.assertEqual(len(activities), len({a["id"] for a in activities}))
                levels = generate.LEVELS[:2] if manifest["version"] == "v1" else generate.LEVELS
                per_pool = 3 if manifest["version"] in ("v1", "v2") else 1000
                expected = {f"{game}:{level}": per_pool for game in games for level in levels}
                counts = Counter(f'{a["game"]}:{a["level"]}' for a in activities)
                self.assertEqual(expected, counts)
                self.assertEqual(expected, manifest["counts"])
        self.assertEqual(
            referenced, {path.name for path in generate.ROOT.glob("activities-*.jsonl")},
        )

    def test_only_generated_scrambles_change_and_keep_their_ids(self):
        self.assertEqual(18_000, len(self.activities))
        self.assertEqual(len(self.previous), len(self.activities))
        self.assertEqual(
            b"".join(self.previous_data.splitlines(keepends=True)[:54]),
            b"".join(self.data.splitlines(keepends=True)[:54]),
        )
        revised = Counter()
        for index, (old, new) in enumerate(zip(self.previous, self.activities)):
            self.assertEqual(
                (old["id"], old["game"], old["level"]), (new["id"], new["game"], new["level"]),
            )
            if index >= 54 and new["game"] == "SCRAMBLE":
                self.assertNotEqual(old, new)
                revised[new["level"]] += 1
            else:
                self.assertEqual(old, new)
        self.assertEqual(dict.fromkeys(generate.LEVELS, 997), dict(revised))

    def test_published_scrambles_use_the_varied_generator(self):
        for level in generate.LEVELS:
            published = [
                a for a in self.activities[54:] if a["game"] == "SCRAMBLE" and a["level"] == level
            ]
            expected = list(scramble(level))
            self.assertEqual(len(expected), len(published))
            for content, activity in zip(expected, published):
                self.assertEqual(content, {k: v for k, v in activity.items() if k not in ("id", "game", "level")})

    def test_pool_counts_answers_and_import_limits(self):
        counts = Counter(f'{a["game"]}:{a["level"]}' for a in self.activities)
        self.assertEqual(18, len(counts))
        self.assertEqual({1000}, set(counts.values()))
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
