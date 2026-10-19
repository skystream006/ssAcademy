"""Shared schema constructors; no network, randomness, or third-party packages."""

import hashlib


def picture(symbol, description):
    return {"symbol": symbol, "description": description}


def question(prompt, correct, distractors):
    choices = [correct, *distractors]
    if not 2 <= len(choices) <= 4 or len(set(choices)) != len(choices):
        raise ValueError(f"Invalid choices for {prompt!r}")
    position = int.from_bytes(hashlib.sha256((prompt + "\0" + correct).encode("utf-8")).digest()[:4], "big")
    position %= len(choices)
    choices.insert(position, choices.pop(0))
    return {"prompt": prompt, "choices": choices, "answer": position}


def scramble_tiles(ordered_tiles):
    if len(ordered_tiles) < 2 or len(set(ordered_tiles)) < 2:
        raise ValueError("Tiles must contain at least two different labels")
    indexes = sorted(range(len(ordered_tiles)), key=lambda i: hashlib.sha256(
        (str(i) + "\0" + ordered_tiles[i]).encode("utf-8")).digest())
    # Repeated words still have separate indexes. Moving only identical labels
    # is not a visible scramble, even when the index permutation is nonidentity.
    if [ordered_tiles[i] for i in indexes] == list(ordered_tiles):
        indexes = indexes[1:] + indexes[:1]
    return [ordered_tiles[i] for i in indexes], [[indexes.index(i) for i in range(len(indexes))]]
