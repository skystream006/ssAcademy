"""Curated, compositional sources for Caption Quest.

The picture banks constrain combinations to a real setting. Caption answers describe
only the displayed symbols and their layout; the accessibility description is not an
extra source of evidence.
"""

from itertools import combinations

from .common import picture, question


_LEVELS = ("SPROUT", "TRAIL", "SUMMIT")
_COUNT = 997


def _check_level(level):
    if level not in _LEVELS:
        raise ValueError(f"Unknown reading level: {level!r}")


def _article(noun):
    return f"{'an' if noun[0] in 'aeiou' else 'a'} {noun}"


_IRREGULAR_PLURALS = {
    "cherry": "cherries",
    "strawberry": "strawberries",
    "tomato": "tomatoes",
    "potato": "potatoes",
    "loaf": "loaves",
    "leaf": "leaves",
    "wolf": "wolves",
    "fox": "foxes",
    "butterfly": "butterflies",
    "fish": "fish",
    "jellyfish": "jellyfish",
    "deer": "deer",
    "sheep": "sheep",
    "mouse": "mice",
    "bus": "buses",
    "peach": "peaches",
    "dress": "dresses",
    "box": "boxes",
}


def _plural(noun):
    for connector in (" of ", " with "):
        if connector in noun:
            head, tail = noun.split(connector, 1)
            return _plural(head) + connector + tail
    if noun == "fork and knife":
        return "forks and knives"
    if noun == "nut and bolt":
        return "nuts and bolts"
    if " " in noun:
        head, tail = noun.rsplit(" ", 1)
        return head + " " + _plural(tail)
    if noun in _IRREGULAR_PLURALS:
        return _IRREGULAR_PLURALS[noun]
    if noun.endswith(("s", "x", "ch", "sh")):
        return noun + "es"
    if noun.endswith("y") and noun[-2] not in "aeiou":
        return noun[:-1] + "ies"
    return noun + "s"


def _items(source):
    return tuple(tuple(item.split(":", 1)) for item in source.split("|"))


# Eight compatible, visually distinct objects per setting. Nouns describe the
# actual emoji, not an unpictured owner, use, colour, feeling, or action.
_PICTURE_BANKS = (
    ("Fruit stall", _items("🍎:apple|🍐:pear|🍌:banana|🍊:orange|🍑:peach|🍍:pineapple|🍓:strawberry|🥝:kiwi")),
    ("Vegetable stall", _items("🥕:carrot|🥔:potato|🍅:tomato|🌽:corn cob|🥦:broccoli head|🍆:eggplant|🧅:onion|🥒:cucumber")),
    ("Bakery counter", _items("🍞:loaf|🥐:croissant|🥯:bagel|🥖:baguette|🥨:pretzel|🧁:cupcake|🍪:cookie|🍩:doughnut")),
    ("Breakfast table", _items("🥚:egg|🥞:pancake stack|🧇:waffle|🥐:croissant|🍌:banana|🥣:bowl|🥛:glass of milk|🫖:teapot")),
    ("Picnic things", _items("🧺:basket|🥪:sandwich|🍎:apple|🧃:juice box|🍪:cookie|🥕:carrot|🍐:pear|🥯:bagel")),
    ("Sports cupboard", _items("⚽:soccer ball|🏀:basketball|🏈:football|⚾:baseball|🎾:tennis ball|🏓:table tennis paddle|🏸:badminton racket|🥏:flying disc")),
    ("Music room", _items("🎹:keyboard|🎸:guitar|🎻:violin|🎺:trumpet|🥁:drum|🪕:banjo|🎷:saxophone|🪗:accordion")),
    ("Art materials", _items("🎨:paint palette|🖌️:paintbrush|🖍️:crayon|✏️:pencil|✂️:pair of scissors|🧵:spool of thread|🧶:ball of yarn|📏:ruler")),
    ("School supplies", _items("📚:stack of books|📓:notebook|✏️:pencil|📏:ruler|📎:paper clip|🎒:schoolbag|🖍️:crayon|✂️:pair of scissors")),
    ("Winter clothes", _items("🧥:coat|🧣:scarf|🧤:pair of gloves|🧦:pair of socks|🥾:boot|👖:pair of trousers|👕:shirt|🎩:hat")),
    ("Beach bag", _items("🪣:bucket|🩴:sandal|👒:sun hat|🕶️:pair of sunglasses|🧴:lotion bottle|🐚:shell|🥽:pair of goggles|🩱:swimsuit")),
    ("Woodland pictures", _items("🌳:tree|🍄:mushroom|🐿️:squirrel|🦊:fox|🦉:owl|🦌:deer|🍁:maple leaf|🦔:hedgehog")),
    ("Farm pictures", _items("🐄:cow|🐑:sheep|🐐:goat|🐖:pig|🐎:horse|🐓:rooster|🦆:duck|🚜:tractor")),
    ("Sea life cards", _items("🐟:fish|🐬:dolphin|🐙:octopus|🦀:crab|🦞:lobster|🦐:shrimp|🦭:seal|🪼:jellyfish")),
    ("Garden pictures", _items("🌷:tulip|🌻:sunflower|🌹:rose|🌼:daisy|🦋:butterfly|🐝:bee|🐞:ladybird|🐌:snail")),
    ("Pet picture cards", _items("🐈:cat|🐕:dog|🐇:rabbit|🐹:hamster|🐢:turtle|🐠:tropical fish|🦜:parrot|🐁:mouse")),
    ("Transport cards", _items("🚗:car|🚌:bus|🚲:bicycle|🚂:train|🚒:fire engine|🚑:ambulance|🚕:taxi|🛵:scooter")),
    ("Party table", _items("🎂:birthday cake|🎁:gift|🎈:balloon|🧁:cupcake|🍪:cookie|🌹:rose|🧃:juice box|🍫:chocolate bar")),
    ("Clothes shop", _items("👕:shirt|👗:dress|👖:pair of trousers|🧥:coat|🧦:pair of socks|👒:sun hat|👟:shoe|👜:handbag")),
    ("Camping equipment", _items("⛺:tent|🎒:backpack|🔦:torch|🧭:compass|🥾:boot|🪣:bucket|🧢:cap|🗺️:map")),
    ("Weather chart", _items("☀️:sun|🌧️:rain cloud|❄️:snowflake|🌈:rainbow|🌪️:tornado|🌙:moon|⭐:star|🌡️:thermometer")),
    ("Kitchen shelf", _items("🥣:bowl|🍽️:plate with cutlery|🥄:spoon|🧊:ice cube|🫖:teapot|🍳:frying pan|🧂:salt shaker|☕:cup")),
    ("Tool display", _items("🔨:hammer|🪚:saw|🔧:spanner|🪛:screwdriver|🔩:nut and bolt|🧰:toolbox|📏:ruler|🧲:magnet")),
    ("Park map symbols", _items("🌳:tree|🌷:tulip|⛲:fountain|🛝:slide|🪁:kite|🚲:bicycle|🦆:duck|⚽:soccer ball")),
)


def _panel(items):
    # Keep a small panel together where the available display width allows it.
    return "\u00a0".join(symbol for symbol, _ in items)


def _list_names(items):
    words = [_article(noun) for _, noun in items]
    if len(words) == 2:
        return " and ".join(words)
    return ", ".join(words[:-1]) + ", and " + words[-1]


def _caption_entry(level, title, chosen, spare, variant):
    a, b, c = chosen
    an, bn, cn = (item[1] for item in chosen)
    if level == "SPROUT":
        symbol = _panel(chosen)
        correct = f"You can see {_list_names(chosen)}."
        wrong = [
            f"You can see {_list_names((spare, b, c))}.",
            f"You can see {_list_names((a, spare, c))}.",
        ]
        description = _list_names(chosen).capitalize() + "."
        prompt = "Which caption names what you see in the picture?"
        hint = "Match each thing you see, not just the first one."
    elif level == "TRAIL":
        if variant % 3 == 0:
            left, right = (a, b), (a, b, c)
            correct = (
                f"Both pictures show {_article(an)} and {_article(bn)}, "
                f"but only the second also shows {_article(cn)}."
            )
            wrong = [
                f"Both pictures show {_article(an)} and {_article(cn)}, but only the second shows {_article(bn)}.",
                f"The first picture shows {_article(cn)}, but it is missing from the second.",
            ]
        elif variant % 3 == 1:
            left, right = (a, b), (a, c)
            correct = (
                f"The {an} appears in both pictures, while the {bn} "
                f"in the first gives way to {_article(cn)} in the second."
            )
            wrong = [
                f"The {bn} appears in both pictures, while the {an} gives way to {_article(cn)}.",
                f"The {an} appears in both pictures, while the {cn} gives way to {_article(bn)}.",
            ]
        else:
            left, right = (b, a, c), (a, b, c)
            correct = (
                f"The {an} is second in picture 1 and first in picture 2, "
                f"while the {bn} moves from first to second and the {cn} stays third."
            )
            wrong = [
                f"The {an} is first in picture 1 and second in picture 2, while the {bn} moves from second to first and the {cn} stays third.",
                f"The {bn} stays first, while the {an} and the {cn} exchange second and third places.",
            ]
        symbol = f"①\u00a0{_panel(left)}\n②\u00a0{_panel(right)}"
        description = f"In reading order, picture 1: {_list_names(left)}. Picture 2: {_list_names(right)}."
        prompt = "Which caption describes both pictures in order?"
        hint = "Compare the two numbered pictures. Check what stays and what changes."
    else:
        if variant % 3 == 0:
            left, right = (a, a, b), (a, c, c)
            correct = (
                f"Although each picture contains three symbols, the second has one {an} "
                f"instead of two, no {bn}, and two {_plural(cn)}."
            )
            wrong = [
                f"Although each picture contains three symbols, the second has two {_plural(an)}, no {bn}, and one {cn}.",
                f"The number of symbols falls from three to two because the {bn} disappears and nothing takes its place.",
                f"The number of symbols stays at three, and the {bn} is the only thing that appears in both pictures.",
            ]
        elif variant % 3 == 1:
            left, right = (a, b, c), (c, b, a)
            correct = (
                f"Although the {bn} stays second, the {an} and the {cn} exchange "
                "first and third places; no symbol is added or removed."
            )
            wrong = [
                f"Although the {an} stays second, the {bn} and the {cn} exchange first and third places.",
                f"The {bn} moves to first place while the other two symbols keep their places.",
                f"The {an} disappears, so the second picture contains fewer symbols than the first.",
            ]
        else:
            left, right = (a, b, b), (a, b, c)
            correct = (
                f"Because one of the two {_plural(bn)} gives way to {_article(cn)} "
                f"while the {an} stays first, "
                f"the second picture has more kinds of symbols but the same total of three."
            )
            wrong = [
                f"Because {_article(cn)} is added while the {an} and both {_plural(bn)} remain, the total rises from three to four.",
                f"Although the total stays at three, the second picture has fewer kinds because the {an} disappears.",
                f"The two {_plural(bn)} both remain, so only the {an}'s place in the reading order changes.",
            ]
        symbol = f"①\u00a0{_panel(left)}\n②\u00a0{_panel(right)}"
        description = f"In reading order, picture 1: {_list_names(left)}. Picture 2: {_list_names(right)}."
        prompt = "Which caption accounts for the change and what remains the same?"
        hint = "Check the identities, reading order, and total. Every part of the caption must fit."
    return {
        "title": title,
        "picture": picture(symbol, description),
        "question": question(prompt, correct, wrong),
        "hint": hint,
    }


def caption(level: str):
    """Yield 997 reproducible, visually answerable caption activities."""
    _check_level(level)
    choices = tuple(combinations(range(8), 3))
    seen = set()
    count = 0
    # Interleave settings rather than exhausting one setting before the next.
    for variant, indices in enumerate(choices):
        for title, bank in _PICTURE_BANKS:
            chosen = tuple(bank[index] for index in indices)
            spare = next(item for index, item in enumerate(bank) if index not in indices)
            entry = _caption_entry(level, title, chosen, spare, variant)
            # Shared objects in related settings must not produce duplicate tasks.
            identity = entry["picture"]["symbol"]
            if identity in seen:
                continue
            seen.add(identity)
            yield entry
            count += 1
            if count == _COUNT:
                return
    raise ValueError(f"Caption source has only {count} distinct {level} scenes")
