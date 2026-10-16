#!/usr/bin/env python3
"""Reproduce v9: preserve v8 and expand Grammar Quest to 1,000 questions per level."""

import argparse
from collections import Counter, defaultdict
import hashlib
import io
import json
from pathlib import Path
import unicodedata
from zipfile import ZipFile

from generation.creative_games import studio
from generation.grammar_games import grammar
from generation.reasoning_games import detective, sequence
from generation.word_games import missing, scramble


ROOT = Path(__file__).resolve().parent
ARCHIVE = ROOT / "archive.zip"
LEVELS = ("SPROUT", "TRAIL", "SUMMIT")
GENERATORS = {
    "MISSING": missing, "SCRAMBLE": scramble, "SEQUENCE": sequence, "DETECTIVE": detective,
    "STUDIO": studio,
}
PER_POOL = 1_000
GRAMMAR_PER_LEVEL = 1_000
V8_ENTRY_POINT = "reading-activities-v8-import.json"
MAX_PACK_BYTES = 64 * 1024 * 1024
MAX_LINE_BYTES = 64 * 1024


def encoded(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")


def archived_file(name):
    with ZipFile(ARCHIVE) as archive:
        return archive.read(name)


def task_identity(activity):
    """Ignore cosmetic changes and distractor permutations when detecting repeats."""
    def answer(q):
        return [q["prompt"], q["choices"][q["answer"]]]

    game = activity["game"]
    if game in ("MISSING", "DETECTIVE", "GRAMMAR"):
        task = [activity["passage"], answer(activity["question"])]
    elif game in ("SCRAMBLE", "SEQUENCE"):
        task = [sorted(" ".join(activity["tiles"][i] for i in order)
                       for order in activity["validOrders"]), answer(activity["question"])]
    else:
        task = [sorted(choices) for choices in activity["writingChoices"]]
    return encoded([game, activity["level"], task])


def answer_keys(activity):
    """Scope answers to a pool; grammar word labels also need their sentence context."""
    questions = ([activity["question"]] if activity.get("question") else [])
    keys = []
    for q in questions:
        key = (
            activity["game"], activity["level"],
            " ".join(unicodedata.normalize("NFKC", q["choices"][q["answer"]]).lower().split()),
        )
        if activity["game"] == "GRAMMAR":
            key += (encoded([activity["passage"], q["prompt"]]),)
        keys.append(key)
    return keys


def build_pack():
    previous = json.loads(archived_file("manifest-v4.json"))
    published = archived_file(previous["file"])
    if (len(published) != previous["bytes"]
            or hashlib.sha256(published).hexdigest() != previous["sha256"]
            or not published.endswith(b"\n")):
        raise ValueError("The immutable v4 pack does not match its manifest")
    previous_activities = [json.loads(line) for line in published.splitlines()]
    previous_pools = defaultdict(list)
    for activity in previous_activities:
        previous_pools[f'{activity["game"]}:{activity["level"]}'].append(activity)
    expected_keys = {f"{game}:{level}" for game in GENERATORS for level in LEVELS}
    if (set(previous_pools) != expected_keys
            or any(len(pool) != PER_POOL for pool in previous_pools.values())
            or len({a["id"] for a in previous_activities}) != len(previous_activities)):
        raise ValueError("Expected the 15,000-activity v4 catalogue")
    prefix = b"".join(published.splitlines(keepends=True)[:45])
    original = [json.loads(line) for line in prefix.splitlines()]
    counts = Counter(f'{a["game"]}:{a["level"]}' for a in original)
    if len(original) != 45 or set(counts) != expected_keys or set(counts.values()) != {3}:
        raise ValueError("Expected the 45-activity v2 catalogue")
    identities = {task_identity(a) for a in original}
    ids = {a["id"] for a in original}
    used_answers = set()
    for activity in original:
        keys = answer_keys(activity)
        if len(keys) != len(set(keys)) or used_answers.intersection(keys):
            raise ValueError(f"Duplicate legacy answer: {activity['id']}")
        used_answers.update(keys)
    output = io.BytesIO()
    output.write(prefix)
    for game, generate in GENERATORS.items():
        for level in LEVELS:
            key = f"{game}:{level}"
            for content in generate(level):
                if counts[key] >= PER_POOL:
                    raise ValueError(f"{key}: the source produces too many activities")
                if {"id", "game", "level"} & content.keys():
                    raise ValueError("Content templates must not assign catalogue identity")
                activity = previous_pools[key][counts[key]]
                if game == "SCRAMBLE":
                    # Imports are additive: revise the existing ID rather than leave
                    # its repetitive predecessor installed beside a new activity.
                    activity = {"id": activity["id"], "game": game, "level": level, **content}
                answers = answer_keys(activity)
                if len(answers) != len(set(answers)) or used_answers.intersection(answers):
                    raise ValueError(f"{key}: duplicate answer: {activity['id']}")
                used_answers.update(answers)
                identity = task_identity(activity)
                if identity in identities:
                    raise ValueError(f"{key}: duplicate learning task: {activity['title']}")
                identities.add(identity)
                if activity["id"] in ids:
                    raise ValueError("Activity ID collision")
                ids.add(activity["id"])
                line = encoded(activity) + b"\n"
                if len(line) > MAX_LINE_BYTES or output.tell() + len(line) > MAX_PACK_BYTES:
                    raise ValueError("Generated content exceeds the Android importer limits")
                output.write(line)
                counts[key] += 1
            if counts[key] != PER_POOL:
                raise ValueError(f"{key}: expected {PER_POOL} activities, got {counts[key]}")
    # Released apps cannot decode GRAMMAR. Keep both old entry points and their
    # immutable v7 file unchanged, and require newer packs to have that exact byte prefix.
    legacy_manifest_bytes = (ROOT / "manifest.json").read_bytes()
    legacy_manifest = json.loads(legacy_manifest_bytes)
    legacy_data = (ROOT / legacy_manifest["file"]).read_bytes()
    if (legacy_manifest["version"] != "v7"
            or legacy_manifest["counts"] != dict(counts)
            or legacy_manifest["bytes"] != len(legacy_data)
            or legacy_manifest["sha256"] != hashlib.sha256(legacy_data).hexdigest()
            or legacy_data != output.getvalue()
            or (ROOT / "reading-activities-import.json").read_bytes() != legacy_manifest_bytes):
        raise ValueError("The v7 catalogue and its compatibility entry points must remain unchanged")
    grammar_pools = {level: list(grammar(level)) for level in LEVELS}
    # Keep the original 150 grammar records in their published order before adding examples.
    for section in (slice(0, 50), slice(50, None)):
        for level, pool in grammar_pools.items():
            key = f"GRAMMAR:{level}"
            for activity in pool[section]:
                if activity["id"] in ids:
                    raise ValueError("Activity ID collision")
                identity = task_identity(activity)
                answers = answer_keys(activity)
                if identity in identities or used_answers.intersection(answers):
                    raise ValueError(f"{key}: duplicate grammar learning task")
                ids.add(activity["id"])
                identities.add(identity)
                used_answers.update(answers)
                line = encoded(activity) + b"\n"
                if len(line) > MAX_LINE_BYTES or output.tell() + len(line) > MAX_PACK_BYTES:
                    raise ValueError("Generated content exceeds the Android importer limits")
                output.write(line)
                counts[key] += 1
    for level in LEVELS:
        key = f"GRAMMAR:{level}"
        if counts[key] != GRAMMAR_PER_LEVEL:
            raise ValueError(f"{key}: expected {GRAMMAR_PER_LEVEL} activities, got {counts[key]}")
    data = output.getvalue()
    original_manifest = json.loads(archived_file("manifest-v8.json"))
    original_data = archived_file(original_manifest["file"])
    if (original_manifest["version"] != "v8"
            or original_manifest["bytes"] != len(original_data)
            or original_manifest["sha256"] != hashlib.sha256(original_data).hexdigest()
            or not data.startswith(original_data)):
        raise ValueError("The original v8 catalogue must remain an unchanged prefix")
    checksum = hashlib.sha256(data).hexdigest()
    manifest = {
        "schemaVersion": 1, "version": "v9", "file": f"activities-{checksum}.jsonl",
        "sha256": checksum, "bytes": len(data), "counts": dict(counts),
    }
    return data, manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check reproducibility without writing files")
    args = parser.parse_args()
    data, manifest = build_pack()
    destination = ROOT / manifest["file"]
    if args.check:
        if not destination.exists() or destination.read_bytes() != data:
            raise SystemExit("The published catalogue differs from its content source")
    else:
        if destination.exists() and destination.read_bytes() != data:
            raise SystemExit("Refusing to modify an existing checksum-named pack")
        if not destination.exists():
            destination.write_bytes(data)
    if args.check:
        if json.loads((ROOT / V8_ENTRY_POINT).read_text(encoding="utf-8")) != manifest:
            raise SystemExit(f"{V8_ENTRY_POINT} differs from the generated catalogue")
    else:
        (ROOT / V8_ENTRY_POINT).write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f'{sum(manifest["counts"].values()):,} activities; {manifest["bytes"]:,} bytes; {manifest["file"]}')


if __name__ == "__main__":
    main()
