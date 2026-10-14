#!/usr/bin/env python3
"""Reproduce the v3 reading catalogue from original, difficulty-specific templates."""

import argparse
from collections import Counter
import hashlib
import io
import json
from pathlib import Path

from generation.creative_games import partners, studio
from generation.reasoning_games import detective, sequence
from generation.scene_games import caption, chapter
from generation.word_games import missing, scramble


ROOT = Path(__file__).resolve().parent
LEVELS = ("SPROUT", "TRAIL", "SUMMIT")
GENERATORS = {
    "MISSING": missing, "SCRAMBLE": scramble, "SEQUENCE": sequence, "DETECTIVE": detective,
    "CAPTION": caption, "CHAPTER": chapter, "STUDIO": studio, "PARTNERS": partners,
}
PER_POOL = 1_000
MAX_PACK_BYTES = 64 * 1024 * 1024
MAX_LINE_BYTES = 64 * 1024


def encoded(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")


def task_identity(activity):
    """Ignore cosmetic changes and distractor permutations when detecting repeats."""
    def answer(q):
        return [q["prompt"], q["choices"][q["answer"]]]

    game = activity["game"]
    if game in ("MISSING", "DETECTIVE"):
        task = [activity["passage"], answer(activity["question"])]
    elif game in ("SCRAMBLE", "SEQUENCE"):
        task = [sorted(" ".join(activity["tiles"][i] for i in order)
                       for order in activity["validOrders"]), answer(activity["question"])]
    elif game == "CAPTION":
        task = [activity["picture"]["symbol"], answer(activity["question"])]
    elif game == "CHAPTER":
        task = [activity["passage"], sorted(
            (branch["text"], answer(branch["question"])) for branch in activity["branches"])]
    else:
        task = [sorted(choices) for choices in activity["writingChoices"]]
    return encoded([game, activity["level"], task])


def build_pack():
    previous = json.loads((ROOT / "manifest-v2.json").read_text(encoding="utf-8"))
    prefix = (ROOT / previous["file"]).read_bytes()
    if (len(prefix) != previous["bytes"] or hashlib.sha256(prefix).hexdigest() != previous["sha256"]
            or not prefix.endswith(b"\n")):
        raise ValueError("The immutable v2 pack does not match its manifest")
    original = [json.loads(line) for line in prefix.splitlines()]
    counts = Counter(f'{a["game"]}:{a["level"]}' for a in original)
    expected_keys = {f"{game}:{level}" for game in GENERATORS for level in LEVELS}
    if len(original) != 72 or set(counts) != expected_keys or set(counts.values()) != {3}:
        raise ValueError("Expected the original 72-activity v2 catalogue")
    identities = {task_identity(a) for a in original}
    ids = {a["id"] for a in original}
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
                activity = {"game": game, "level": level, **content}
                identity = task_identity(activity)
                if identity in identities:
                    raise ValueError(f"{key}: duplicate learning task: {activity['title']}")
                identities.add(identity)
                activity["id"] = f"bulk-{game.lower()}-{level.lower()}-{hashlib.sha256(identity).hexdigest()[:20]}"
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
    data = output.getvalue()
    checksum = hashlib.sha256(data).hexdigest()
    manifest = {
        "schemaVersion": 1, "version": "v3", "file": f"activities-{checksum}.jsonl",
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
    # Keep the original manifest URL working for released apps.
    for manifest_name in ("reading-activities-import.json", "manifest.json"):
        if args.check:
            if json.loads((ROOT / manifest_name).read_text(encoding="utf-8")) != manifest:
                raise SystemExit(f"{manifest_name} differs from the generated catalogue")
        else:
            (ROOT / manifest_name).write_text(
                json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f'{sum(manifest["counts"].values()):,} activities; {manifest["bytes"]:,} bytes; {manifest["file"]}')


if __name__ == "__main__":
    main()
