"""Curated, compositional sources for Caption Quest and Choose Next Chapter.

The picture banks constrain combinations to a real setting. Caption answers describe
only the displayed symbols and their layout; the accessibility description is not an
extra source of evidence. Chapter jobs carry their own action, observation, and
question, so a pair is a choice between two real activities, not renamed characters.
"""

from dataclasses import dataclass
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
                f"while the {cn} stays third."
            )
            wrong = [
                f"The {an} is first in picture 1 and second in picture 2, while the {cn} stays third.",
                f"The {cn} is second in picture 1 and first in picture 2, while the {an} stays third.",
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
                f"Because one of the two {_plural(bn)} gives way to {_article(cn)}, "
                f"the second picture has more kinds of symbols but the same total of three."
            )
            wrong = [
                f"Because {_article(cn)} is added without anything being removed, the total rises from three to four.",
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


@dataclass(frozen=True)
class _Job:
    label: str
    symbol: str
    scene: str
    prompt: str
    answer: str
    purpose: str
    observation: str


@dataclass(frozen=True)
class _World:
    title: str
    setting: str
    jobs: tuple


# Each job has a concrete fact for Sprout, a causal purpose for Trail, and an
# observation from which Summit readers can infer that purpose. Observations
# are evidence, not claims that one small trial proves something always works.
_WORLDS = (
    _World("Library helpers", "at the library", (
        _Job("mark a page", "🔖", "A blue bookmark goes between the pages.", "What marks the page?", "A blue bookmark", "find the same page again", "The book is shut, but the blue end still sticks out at the saved page."),
        _Job("mend a cover", "📕", "A strip of tape joins the torn cover.", "What joins the torn cover?", "A strip of tape", "keep the cover attached", "Before the repair the cover hangs loose; afterward it stays on when the book opens."),
        _Job("sort the returns", "📚", "Animal books go in a box with a paw sign.", "Which sign is on the box?", "A paw", "group books about the same topic", "Books about cats and wolves are together; books about boats are in another box."),
        _Job("make a book stand", "📖", "A small stand holds an open book.", "What holds the book open?", "A small stand", "read without holding the book", "Both hands can rest on the desk, yet the page still faces the reader."),
        _Job("pack a loan", "👜", "A book goes in a cloth bag.", "What holds the book?", "A cloth bag", "carry the book without dropping it", "The bag's handles fit over a wrist, leaving a hand free to open the door."),
        _Job("make a quiet sign", "🤫", "A picture of a sleeping owl goes on the door.", "Which animal is on the sign?", "An owl", "remind visitors to use quiet voices", "Visitors see the sign, lower their voices, and pass readers without interrupting."),
        _Job("label a shelf", "🏷️", "A large number goes on the end of a shelf.", "What goes on the shelf end?", "A large number", "match a shelf to its number on a list", "A reader follows number six on a list straight to the shelf marked six."),
    )),
    _World("Garden jobs", "in the garden", (
        _Job("mark the seeds", "🏷️", "A pea picture goes on the seed pot.", "What picture is on the pot?", "A pea", "identify the seeds before they sprout", "The soil hides the seeds, but the picture still shows which pot holds peas."),
        _Job("water a pot", "💧", "Water drips slowly from a can onto dry soil.", "What drips onto the soil?", "Water", "wet the soil without washing seeds away", "The soil darkens, and the small seeds stay where they were planted."),
        _Job("support a stem", "🌱", "A loose tie holds a tall stem beside a stick.", "What stands beside the stem?", "A stick", "help a tall plant stay upright", "The bent stem stands straighter after the tie is added; the tie does not squeeze it."),
        _Job("cover bare soil", "🍂", "Dry leaves cover the soil around a plant.", "What covers the soil?", "Dry leaves", "slow the loss of water from soil", "A day later the covered soil is still damp, while the bare patch is dry."),
        _Job("collect fallen leaves", "🧺", "Fallen leaves go into a wide basket.", "Where do the leaves go?", "A wide basket", "move many leaves in one trip", "Instead of carrying one handful at a time, the helper lifts a whole basket."),
        _Job("protect the path", "🪨", "Flat stones mark a path beside the shoots.", "What marks the path?", "Flat stones", "keep feet away from young plants", "Visitors step on the stones, and none of their steps crosses the seed row."),
        _Job("make a bee stop", "🐝", "Pebbles rise above water in a shallow dish.", "What rises above the water?", "Pebbles", "give bees a place to land beside water", "A bee stands on a dry pebble and reaches the water without falling in."),
    )),
    _World("Kitchen helpers", "in the kitchen with an adult", (
        _Job("drain the peas", "🥣", "Peas sit in a sieve above a bowl.", "Where do the peas sit?", "A sieve", "separate peas from their washing water", "Water passes through the small holes, while the peas remain above them."),
        _Job("measure the flour", "🥄", "A spoon fills a cup to a marked line.", "What fills the cup?", "A spoon", "use the amount asked for in a recipe", "The recipe's line and the flour's level match before the flour goes into the bowl."),
        _Job("save the soup", "🫙", "Cool soup goes into a tub with a lid.", "What closes the tub?", "A lid", "stop the soup spilling during a short move", "When the tub is gently carried to the fridge, the closed top keeps the soup inside."),
        _Job("dry the plate", "🍽️", "A clean cloth wipes drops from a plate.", "What wipes the plate?", "A clean cloth", "remove water before putting the plate away", "The plate has no drops left, while the cloth has become damp."),
        _Job("sort the spoons", "🥄", "Small spoons go in a narrow tray.", "Where do the small spoons go?", "A narrow tray", "make the small spoons easy to find", "When a small spoon is needed, nobody has to search through the forks."),
        _Job("mash a banana", "🍌", "A fork presses a soft banana in a bowl.", "What presses the banana?", "A fork", "make a soft mixture without large lumps", "The big pieces become small enough to stir evenly into the mixture."),
        _Job("set a place", "🍴", "A fork rests beside a plate.", "What rests beside the plate?", "A fork", "put an eating tool within reach", "The diner can pick up a piece of food without getting up to find cutlery."),
    )),
    _World("Art room choices", "in the art room", (
        _Job("mix green paint", "🎨", "Blue paint and yellow paint meet on a palette.", "Which colour joins blue?", "Yellow", "make a new colour from two paints", "The stirred patch is green, unlike either unmixed patch beside it."),
        _Job("print a leaf", "🍃", "A painted leaf presses onto white paper.", "What presses onto the paper?", "A painted leaf", "copy the shape and lines of a leaf", "After the leaf is lifted, its branching lines remain in paint."),
        _Job("join a collage", "🖼️", "Small paper shapes stick to a card with glue.", "What holds the shapes?", "Glue", "keep separate shapes on one picture", "The card is lifted, and the shapes stay in their chosen places."),
        _Job("draw a border", "📏", "A ruler guides a pencil along the card's edge.", "What guides the pencil?", "A ruler", "make a straight edge around a design", "The new line runs straight rather than wobbling like the earlier freehand line."),
        _Job("shape a clay bowl", "🥣", "Fingers press a hollow into soft clay.", "What is pressed into the clay?", "A hollow", "make space for a bowl to hold things", "A bead rolls out of a flat clay pad but rests inside the hollow."),
        _Job("thread a necklace", "🧵", "Big beads slide onto a thick cord.", "What holds the beads?", "A thick cord", "keep beads together in a wearable line", "Once the ends are tied, the beads can be lifted as one loop."),
        _Job("wash a brush", "🖌️", "A brush swirls in a jar of clean water.", "Where does the brush swirl?", "A jar of water", "remove the old paint before using a new colour", "The next yellow stroke stays yellow instead of picking up the brush's old blue paint."),
    )),
    _World("Music room choices", "in the music room", (
        _Job("keep a drum beat", "🥁", "A drum sounds once for each slow step.", "What makes the beat?", "A drum", "help walkers keep the same steady pace", "The walkers' feet begin landing together rather than at scattered times."),
        _Job("soften a loud sound", "🎺", "A cloth pad goes under a small practice drum.", "What goes under the drum?", "A cloth pad", "reduce sound passing into the table", "The beat can still be heard nearby, but the table rattles much less."),
        _Job("mark a rest", "🎵", "A red card goes up during a silent beat.", "What colour is the card?", "Red", "show when players should pause", "Players stop together when the card rises, then start again when it drops."),
        _Job("sort the bells", "🔔", "The lowest bell goes first in a row.", "Which bell goes first?", "The lowest bell", "arrange sounds from low to high", "Each bell farther along the row makes a higher note than the one before."),
        _Job("hold the song pages", "📎", "A clip holds two song pages on a stand.", "What holds the pages?", "A clip", "keep music pages from slipping off the stand", "A breeze lifts a loose page nearby, but the clipped pages remain in place."),
        _Job("share a shaker", "🪇", "A timer rings when it is time to pass the shaker.", "What signals a new turn?", "A timer", "give each player the same length of turn", "Each child plays until the same timer rings, even if another child asks early."),
        _Job("remember a tune", "🎙️", "A recorder saves the tune after the players agree.", "What saves the tune?", "A recorder", "listen back to a tune after playing it", "Later, the same tune can be heard even though no one is playing."),
    )),
    _World("Playground helpers", "at the playground", (
        _Job("mark the start", "🚩", "A flag stands at the start of a running lane.", "What marks the start?", "A flag", "show all runners the same starting place", "Each runner lines up by the flag instead of choosing a different spot."),
        _Job("set up a toss", "🪣", "A beanbag lands in a wide bucket.", "Where does the beanbag land?", "A wide bucket", "give a toss game a clear target", "A bag inside the bucket counts as a hit; a bag outside does not."),
        _Job("make a hop trail", "🟦", "Chalk squares make a short trail on the ground.", "What shapes make the trail?", "Squares", "show where to land during a hopping game", "The children hop into the marked spaces without needing someone to point each time."),
        _Job("collect loose balls", "⚽", "Loose balls go into a net bag.", "Where do the balls go?", "A net bag", "keep unused balls out of the walking route", "Once the balls are in the bag, none rolls across the path."),
        _Job("count game turns", "🪙", "Each player puts one counter in a cup after a turn.", "What goes in the cup?", "A counter", "keep a visible record of completed turns", "The counters can be counted even after everyone forgets the order of play."),
        _Job("clear a puddle route", "💧", "Cones mark a dry path around a puddle.", "What marks the dry path?", "Cones", "guide walkers around slippery water", "People follow the cones and reach the gate without stepping into the puddle."),
        _Job("offer a rest spot", "🪑", "A chair waits in the shade beside the games.", "Where is the chair?", "In the shade", "provide a cooler place to pause", "A tired player sits out of the direct sun and returns after resting."),
    )),
    _World("Pond observations", "beside the pond with a guide", (
        _Job("watch the ducks", "🦆", "A child watches ducks through binoculars.", "What helps the child watch?", "Binoculars", "see birds without moving close to them", "The birds look larger through the lenses but keep swimming without being approached."),
        _Job("sketch a frog", "🐸", "A frog's long back legs appear in a sketch.", "Which legs are in the sketch?", "The back legs", "record a feature used for jumping", "The frog pushes off with those folded legs and lands far from its first spot."),
        _Job("compare the ripples", "💧", "A drawing shows rings spreading from a falling drop.", "What shapes spread out?", "Rings", "record how a small splash spreads across water", "The rings become wider as they travel away from where the drop landed."),
        _Job("spot a dragonfly", "🪰", "A dragonfly rests on a reed while a child counts its wings.", "Where does the dragonfly rest?", "A reed", "count wings while the insect is still", "During flight the wings blur; on the reed their separate outlines can be seen."),
        _Job("read the pond sign", "🪧", "A sign shows a duck and a crossed-out loaf.", "What food is crossed out?", "A loaf", "warn visitors not to give bread to ducks", "A visitor puts away bread after looking at the sign and asks the guide about pond care."),
        _Job("find a snail trail", "🐌", "A snail leaves a shiny line on a wet stone.", "What does the snail leave?", "A shiny line", "trace the route taken by a small animal", "The line leads from the edge of the stone to the snail's present position."),
        _Job("note the water level", "📏", "A fixed post has a blue mark at the water's edge.", "What colour is the mark?", "Blue", "compare the water level on a later visit", "The post stays fixed; after rain, the water is above the earlier mark."),
    )),
    _World("Woodland walk", "on a woodland walk with an adult", (
        _Job("make a bark rubbing", "🌳", "Paper lies on bark while a crayon rubs across it.", "What rubs across the paper?", "A crayon", "record bark texture without removing bark", "The raised ridges leave dark lines on the paper while the tree stays untouched."),
        _Job("compare fallen leaves", "🍂", "Two fallen leaves lie beside a ruler.", "What lies beside the leaves?", "A ruler", "compare leaf lengths using the same scale", "One leaf reaches six marks and the other reaches nine when both start at zero."),
        _Job("watch a squirrel", "🐿️", "A squirrel hides a nut under leaves.", "What does the squirrel hide?", "A nut", "record where an animal stores food", "The observer notes the spot rather than moving the leaves or taking the hidden nut."),
        _Job("listen for a bird", "🐦", "The group stops talking beside a tree.", "What does the group stop doing?", "Talking", "hear a quiet call that voices could hide", "Once the voices stop, a faint repeated whistle becomes easy to hear."),
        _Job("follow the trail marks", "➡️", "Yellow arrows point along the marked path.", "What colour are the arrows?", "Yellow", "stay on the route chosen by the guide", "At each fork the arrow points to the same marked trail shown on the guide's map."),
        _Job("look at a log", "🪵", "A magnifier reveals tiny holes in a fallen log.", "What reveals the tiny holes?", "A magnifier", "see small details without breaking the log apart", "The holes look larger through the lens, and the log is left where it was."),
        _Job("record a footprint", "🐾", "A sketch shows a footprint with three toes.", "How many toes are drawn?", "Three", "save a trace that rain might wash away", "Rain later smooths the mud, but the drawing still shows the toe pattern."),
    )),
    _World("Beach discoveries", "on the beach with an adult", (
        _Job("sort empty shells", "🐚", "Only empty shells go into groups by shape.", "How are the empty shells grouped?", "By shape", "compare shell forms without disturbing animals", "Every shell is checked first, and any shell with an animal is left in place."),
        _Job("test a sand wall", "🏖️", "A damp-sand wall stands firm. A dry-sand wall falls.", "Which sand makes the firmer wall?", "Damp sand", "compare how moisture changes loose sand", "The damp wall keeps its shape longer when both walls are gently tapped."),
        _Job("mark a safe spot", "🚩", "A bright flag marks the family's meeting place.", "What marks the meeting place?", "A bright flag", "recognize the agreed place among similar shelters", "Several shelters look alike, but only the family's spot has that flag."),
        _Job("draw a tide line", "🌊", "A drawing records seaweed along the high-water edge.", "What lies along the edge?", "Seaweed", "notice how far water reached earlier", "The sea is now farther away, yet the strand of seaweed remains high on the sand."),
        _Job("watch a crab", "🦀", "A crab moves sideways into a gap between rocks.", "How does the crab move?", "Sideways", "record an animal's movement without handling it", "The observer follows it with their eyes and leaves its shelter untouched."),
        _Job("collect safe litter", "🧤", "An adult uses a grabber to lift a plastic wrapper.", "What lifts the wrapper?", "A grabber", "remove litter without touching it directly", "The wrapper reaches the waste bag while the adult's hands remain away from it."),
        _Job("shade a snack", "☂️", "A snack box rests beneath a beach umbrella.", "What shades the snack box?", "An umbrella", "keep the snack out of direct sunlight", "The box stays in the umbrella's shadow while nearby sand is brightly lit."),
    )),
    _World("Pet care choices", "at the pet care club with an adult", (
        _Job("refill a water bowl", "🥣", "Fresh water goes into the dog's clean bowl.", "What goes into the bowl?", "Fresh water", "replace water that has become dirty", "The old water had bits of food in it; the washed bowl now holds clear water."),
        _Job("brush a dog", "🐕", "A soft brush lifts loose fur from the dog's coat.", "What lifts loose fur?", "A soft brush", "remove loose hair without pulling hard", "The loose hair collects on the brush while the dog remains relaxed."),
        _Job("make a hiding place", "📦", "A box with an open doorway sits in the cat's quiet corner.", "What has an open doorway?", "A box", "offer a retreat the cat can enter and leave", "The cat walks in during a noisy moment, then comes out when the room is quiet."),
        _Job("check a name tag", "🏷️", "An adult reads the dog's name on its collar tag.", "Where is the dog's name?", "On its collar tag", "identify the dog without guessing from appearance", "Two dogs look alike, but their tags show different names."),
        _Job("measure pet food", "🥄", "An adult-approved scoop marks the pet's food portion.", "What marks the portion?", "A scoop", "serve the amount specified by the carer", "The filled scoop matches the carer's guide; a random handful would not."),
        _Job("clean a toy", "🧸", "A washable toy is rinsed and left on a rack to dry.", "Where does the toy dry?", "On a rack", "return a toy only after it is clean and dry", "The toy is no longer muddy, and the rack lets water drip away before play."),
        _Job("observe a fish", "🐠", "A child sits still beside the tank and makes a sketch.", "What does the child make?", "A sketch", "watch a fish without tapping on its tank", "The fish keeps swimming normally while the child draws from a quiet seat."),
    )),
    _World("Farm visit helpers", "on a farm visit with the farmer", (
        _Job("gather eggs", "🥚", "The farmer sets eggs in a tray with separate hollows.", "Where do the eggs sit?", "In separate hollows", "keep eggs from knocking together", "Each egg stays in its own hollow when the tray is carried slowly."),
        _Job("fill a hay rack", "🌾", "Dry hay goes into the rack above the muddy floor.", "Where does the hay go?", "Into a rack", "keep feed away from mud underfoot", "The hay in the rack stays dry while bits dropped onto the floor become muddy."),
        _Job("close a small gate", "🚪", "The farmer slides a latch across the gate.", "What holds the gate closed?", "A latch", "keep the gate shut after people pass through", "A gentle push no longer opens the gate once the latch is across."),
        _Job("sort the apples", "🍎", "Bruised apples go in a separate box for cooking.", "Which apples go in a separate box?", "Bruised apples", "separate fruit for different uses", "The unbruised apples go to the fresh-fruit stall; the sound parts of bruised ones will be cooked."),
        _Job("label a wool sample", "🐑", "A card with a sheep picture stays beside a wool sample.", "Which animal is on the card?", "A sheep", "show where a material came from", "Visitors can match the soft sample to the animal even when the sheep are outside."),
        _Job("wash muddy boots", "🥾", "A stiff brush scrubs mud from the boot soles.", "What scrubs the soles?", "A stiff brush", "avoid carrying farm mud into the clean room", "After the scrub, the boots no longer leave muddy prints on the entrance mat."),
        _Job("record a delivery", "📝", "The farmer ticks a list as each sack arrives.", "What gets a tick?", "A list", "check delivered goods against what was ordered", "Four sacks are ticked, leaving one unticked line for the sack that has not arrived."),
    )),
    _World("Market stall helpers", "at the market stall", (
        _Job("show a price", "🏷️", "A large price card stands beside the pears.", "What stands beside the pears?", "A price card", "let shoppers check the cost before choosing", "A shopper reads the card and counts coins without needing to ask the seller."),
        _Job("weigh some apples", "⚖️", "Apples sit on a scale while the pointer settles.", "What holds the apples?", "A scale", "measure the fruit rather than guess its weight", "Two equally full-looking bags move the pointer to different marks."),
        _Job("pack soft fruit", "🍑", "Soft peaches sit in a shallow tray, not under heavy potatoes.", "Where do the peaches sit?", "A shallow tray", "protect soft fruit from heavy loads", "The peaches keep their shape because no heavy bag presses down on them."),
        _Job("count the change", "🪙", "Coins are placed in a row and counted twice.", "How are the coins arranged?", "In a row", "check the amount returned to a shopper", "The second count matches the receipt instead of relying on a quick handful."),
        _Job("keep a queue", "👣", "Footprint marks show where shoppers can stand.", "What shows where to stand?", "Footprint marks", "leave room for shoppers waiting their turns", "Each person stands on a mark, and the narrow doorway stays clear."),
        _Job("cover the bread", "🍞", "A clear lid rests over the bread tray.", "What covers the bread tray?", "A clear lid", "shield food while letting shoppers see it", "The loaves remain visible, but a falling leaf lands on the lid rather than on the bread."),
        _Job("offer a sturdy bag", "🛍️", "A strong bag has two wide handles.", "How many handles does the bag have?", "Two", "carry several purchases together", "A shopper lifts the packed bag instead of balancing loose items in both arms."),
    )),
    _World("Museum makers", "at the museum activity table", (
        _Job("label a fossil model", "🦴", "A card names the animal beside a fossil model.", "What names the animal?", "A card", "connect a displayed object with its identity", "Without touching the model, a visitor reads which animal its shape represents."),
        _Job("make a touch sample", "🖐️", "A replica sits on a mat marked Touch here.", "Where does the replica sit?", "On a mat", "offer hands-on learning without handling the original", "Visitors feel the copy while the old original remains behind glass."),
        _Job("light a display", "🔦", "An adjustable lamp points at a carved pattern.", "What points at the pattern?", "A lamp", "make shallow carved details easier to see", "When the light comes from the side, small ridges cast shadows that were hard to see before."),
        _Job("draw a timeline", "📏", "Dated cards sit in order along a long strip.", "What is written on the cards?", "Dates", "compare which events happened earlier or later", "A card from a smaller year is placed before one from a larger year."),
        _Job("offer a written guide", "📝", "Large printed words describe the sound exhibit.", "What describes the exhibit?", "Printed words", "provide information without requiring sound", "A visitor who does not play the recording still completes the matching task."),
        _Job("mark a route", "➡️", "Blue arrows lead from the door to the exhibit.", "What colour are the arrows?", "Blue", "guide visitors to a display through several rooms", "A visitor follows each arrow and reaches the exhibit without taking a wrong turn."),
        _Job("count visitor votes", "🗳️", "Each visitor drops one token into a topic jar.", "What goes into a topic jar?", "A token", "record visitors' choices for a future display", "The jars hold different totals even after the visitors have left."),
    )),
    _World("Camp preparations", "at camp with the leaders", (
        _Job("test a torch", "🔦", "A torch shines onto a card inside the dim tent.", "What lights the card?", "A torch", "check a light before it is needed after dark", "The writing becomes readable, showing that this torch works now."),
        _Job("store dry socks", "🧦", "Socks go into a sealed bag inside a backpack.", "What goes into the sealed bag?", "Socks", "keep spare clothing separate from damp gear", "A wet towel touches the outside of the bag, but the socks inside stay dry."),
        _Job("mark a tent rope", "🚩", "Bright ribbons hang on the tent ropes.", "What hangs on the ropes?", "Bright ribbons", "make low ropes easier to notice", "People spot the moving ribbons before reaching the thin ropes beneath them."),
        _Job("share a camp map", "🗺️", "A map with a star for camp is pinned to a board.", "What marks camp on the map?", "A star", "show the agreed meeting place on a shared plan", "Every group points to the same star when the leader asks where they will meet."),
        _Job("sort cooking gear", "🥣", "Clean cups sit in one crate and used cups in another.", "Where do clean cups sit?", "In their own crate", "avoid mixing washed equipment with used equipment", "No one has to guess whether a cup needs washing before using it."),
        _Job("make a weather note", "☁️", "A weather card is clipped beside the day's plan.", "What sits beside the plan?", "A weather card", "consider current conditions before an outing", "The leader sees the rain forecast and chooses the sheltered walk."),
        _Job("secure loose paper", "🪨", "A smooth stone holds the camp list on the table.", "What holds the camp list?", "A smooth stone", "stop a light sheet blowing away", "A breeze lifts the paper's corners, but the part under the stone stays down."),
    )),
    _World("Classroom helpers", "in the classroom", (
        _Job("sort the pencils", "✏️", "Sharp pencils go into a cup with a green dot.", "What colour is the dot?", "Green", "find a pencil ready for writing", "A child takes one from that cup and starts writing without sharpening it."),
        _Job("save a drawing", "📁", "A drawing slides into a named folder.", "Where does the drawing go?", "A named folder", "keep one pupil's work together", "Later, the drawing is found with the same pupil's earlier work rather than in a mixed pile."),
        _Job("write the date", "📅", "The day's date goes at the top of a journal page.", "Where does the date go?", "At the top", "tell when a recorded event happened", "Two similar entries can be put in order using their different dates."),
        _Job("show a turn", "🪙", "A wooden token passes to the next speaker.", "What passes to the next speaker?", "A wooden token", "make speaking turns easy to follow", "Only the child holding the token speaks, so voices no longer overlap."),
        _Job("measure a desk", "📏", "A strip is lined up with one end of the desk.", "Where does the strip start?", "At one end", "measure a length from a clear starting point", "Starting at the desk's end includes the whole length instead of missing a gap."),
        _Job("make a word card", "📝", "The word leaf sits below a picture of a leaf.", "Which word is on the card?", "Leaf", "connect written words with their meanings", "A reader points from the letters to the matching picture instead of to a flower."),
        _Job("return the blocks", "🧱", "Blocks go in a tub marked with a block picture.", "What goes in the marked tub?", "Blocks", "match shared equipment with its storage place", "The floor is clear, and the next group finds the blocks in the pictured tub."),
    )),
    _World("Station helpers", "at a model railway station", (
        _Job("read a timetable", "🕒", "A card shows the next model train at ten.", "When is the next train?", "At ten", "use a schedule to plan when to wait", "The visitor waits at the platform before the clock reaches the printed time."),
        _Job("match a platform", "🔢", "A ticket shows platform three.", "Which platform is on the ticket?", "Three", "match a ticket's instruction to a numbered place", "The visitor chooses the sign marked three rather than the nearest platform."),
        _Job("label a lost bag", "🎒", "A found toy bag gets a tag with the finding place.", "What is written on the tag?", "The finding place", "keep a useful clue for returning a lost item", "Someone asking about a bag from that exact bench can compare it with the tag."),
        _Job("make a route card", "🗺️", "A red line links the model town to the model port.", "What colour links the stops?", "Red", "show which stops belong to the same route", "A passenger traces the line through both places without following the blue branch."),
        _Job("test a ramp", "♿", "A small cart rolls up a smooth ramp.", "What does the cart roll up?", "A ramp", "provide a route without a step", "The cart reaches the platform on its wheels rather than needing to be lifted."),
        _Job("announce a change", "📢", "A new stop card replaces the old one on the board.", "What replaces the old stop card?", "A new stop card", "make a changed destination visible", "Visitors reading the board choose the updated stop rather than the cancelled one."),
        _Job("keep an entrance clear", "🚪", "Waiting chairs move away from the entrance.", "What moves away from the entrance?", "Chairs", "leave enough room for people to pass", "A visitor with a large bag now walks through without squeezing between seats."),
    )),
    _World("Theatre preparations", "at the school theatre", (
        _Job("make a moon prop", "🌙", "A silver circle hangs above the stage.", "What shape hangs above the stage?", "A circle", "suggest a nighttime sky in a scene", "The audience sees the silver shape above a dark backdrop and recognizes the moon."),
        _Job("mark a stage spot", "❌", "A tape cross marks where the speaker stands.", "What marks the speaker's place?", "A tape cross", "help an actor find the same position each time", "At each rehearsal the actor returns to the cross without asking where to stand."),
        _Job("sort the costumes", "👕", "Each costume hangs beside a character picture.", "What is beside each costume?", "A character picture", "match clothing with the role that uses it", "Actors find their clothes by their character pictures rather than by guessing sizes."),
        _Job("signal a scene change", "🔔", "A small bell rings after the final line.", "What rings after the line?", "A small bell", "give the backstage team a clear cue", "The helpers move the scenery at the bell instead of while the actor is still speaking."),
        _Job("quiet a prop box", "📦", "A soft towel lines the box of small props.", "What lines the prop box?", "A soft towel", "reduce noise when props are put down", "A wooden prop makes a softer sound on the towel than on the bare box bottom."),
        _Job("print a programme", "📝", "The cast names appear on a folded sheet.", "Whose names are printed?", "The cast's names", "tell the audience who plays each role", "A visitor matches a character on stage with the name listed beside that character."),
        _Job("check the curtain", "🎭", "An adult checks that no chair blocks the curtain.", "What might block the curtain?", "A chair", "let the curtain move without catching", "Once the chair is moved away, the curtain closes smoothly all the way across."),
    )),
    _World("Workshop choices", "at the supervised making table", (
        _Job("smooth a wooden edge", "🪵", "Fine sandpaper rubs a rough wooden edge.", "What rubs the edge?", "Fine sandpaper", "remove roughness from a surface", "The checked edge no longer catches the cloth that snagged there before."),
        _Job("measure a strip", "📏", "A pencil mark sits at the ruler's ten mark.", "Which mark is used?", "Ten", "prepare a piece with a specified length", "The planned piece reaches the required mark rather than being guessed by eye."),
        _Job("hold two pieces", "🗜️", "An adult fits a clamp while the glue dries.", "What holds the pieces?", "A clamp", "keep glued parts from moving before they set", "The parts stay aligned even when the helper takes their hands away."),
        _Job("sort the fasteners", "🔩", "Nuts sit in a small dish beside the bolts.", "Where do the nuts sit?", "A small dish", "keep small parts from rolling off the table", "A loose nut rolls away on the tabletop, but those in the dish stay together."),
        _Job("test a wheel", "🛞", "A toy wheel turns on its axle after a small gap is added.", "What does the wheel turn on?", "An axle", "leave room for a moving part to turn freely", "The tightly squeezed wheel stuck; with a small gap it rotates without rubbing."),
        _Job("mark matching parts", "🔵", "Matching pieces both get a blue dot.", "What mark is on matching pieces?", "A blue dot", "identify parts that belong together", "The matching dots still work after all the pieces are mixed on the table."),
        _Job("store a finished model", "📦", "A small model sits in a padded box.", "What kind of box holds the model?", "A padded box", "protect a fragile object during a move", "The padding stops the model sliding into the hard sides when the box is carried."),
    )),
    _World("Weather station", "at the class weather station", (
        _Job("measure rainfall", "🌧️", "Rain collects in a straight-sided marked cup.", "What collects the rain?", "A marked cup", "compare how much rain falls on different days", "The water reaches different marks after two storms, even though the same cup is used."),
        _Job("watch wind direction", "🚩", "A ribbon streams away from a fixed pole.", "What streams from the pole?", "A ribbon", "observe which way the wind is blowing", "When the wind changes, the ribbon points a different way while the pole stays still."),
        _Job("record temperature", "🌡️", "A thermometer reading goes onto a chart.", "Where is the reading recorded?", "A chart", "compare temperatures at recorded times", "The morning and afternoon numbers remain available after the thermometer changes again."),
        _Job("draw cloud cover", "☁️", "A sky circle is shaded to show the cloudy part.", "What part is shaded?", "The cloudy part", "record how much of the sky is covered", "A mostly shaded circle and a nearly clear circle show different amounts, not different cloud names."),
        _Job("compare shadows", "☀️", "Chalk marks the end of a fixed pole's shadow.", "What marks the shadow's end?", "Chalk", "track changes in a shadow over time", "At noon the shadow ends before the earlier mark, although the pole has not moved."),
        _Job("read a forecast", "📝", "A rain symbol appears beside tomorrow's date.", "Which symbol is beside tomorrow?", "A rain symbol", "plan for weather that may happen later", "The class packs raincoats for tomorrow rather than deciding that rain is falling now."),
        _Job("check a sheltered spot", "🏠", "Two paper flags hang inside and outside a porch.", "Where is the sheltered flag?", "Inside the porch", "compare wind exposure in two nearby places", "The outer flag flaps while the inner flag barely moves during the same gust."),
    )),
    _World("Sports club helpers", "at the sports club", (
        _Job("pump a ball", "🏀", "An adult adds air to a soft ball.", "What is added to the ball?", "Air", "restore a ball's useful shape and bounce", "Before the check it barely bounces; afterward it rebounds without sagging."),
        _Job("space the cones", "🔺", "Cones stand an equal number of steps apart.", "How are the cones spaced?", "Equally", "make repeated practice gaps consistent", "A player meets the same gap each time rather than one tiny gap and one wide gap."),
        _Job("keep the score", "📝", "A tally mark is added after each goal.", "What is added after a goal?", "A tally mark", "keep a record of points as play continues", "The final total can be counted from the marks without remembering every goal."),
        _Job("time a run", "⏱️", "A stopwatch starts and stops at the marked lines.", "What measures the run?", "A stopwatch", "measure elapsed time between two events", "The displayed seconds cover only the run, not the wait before the start."),
        _Job("share the bibs", "👕", "One team wears green bibs and the other wears blue.", "What does one team wear?", "Green bibs", "distinguish teams during a moving game", "Players recognize teammates by the bib colour even when everyone is running."),
        _Job("set up a water break", "💧", "Named water bottles wait on a rest table.", "Where do the bottles wait?", "On a rest table", "make each player's own drink easy to find", "At the break, players read their names instead of taking an unmarked bottle."),
        _Job("check the playing space", "⚽", "An adult removes a loose bag from the court.", "What is removed from the court?", "A loose bag", "clear an obstacle before players begin", "The place where a runner's foot might catch is now empty."),
    )),
    _World("Picnic preparations", "at the picnic table", (
        _Job("hold the tablecloth", "🪨", "Clips hold the cloth to the table edge.", "What holds the cloth?", "Clips", "keep a light cloth from lifting in the breeze", "The loose edge flaps, but each clipped part stays against the table."),
        _Job("separate the snacks", "🥕", "Carrot sticks go in one tub and crackers in another.", "What goes in its own tub?", "Carrot sticks", "keep dry crackers away from damp vegetables", "The crackers remain crisp instead of resting against the moist carrot sticks."),
        _Job("label the cups", "🥤", "A different picture goes on each person's cup.", "What makes each cup different?", "A picture", "help people recognize their own drink", "Two identical cups can still be told apart by the sun and leaf marks."),
        _Job("keep fruit cool", "🧊", "An ice pack rests beside the fruit tub in a cooler.", "What rests beside the fruit tub?", "An ice pack", "slow warming while food waits to be served", "The fruit stays cooler than a similar tub left outside the cooler."),
        _Job("divide a sandwich", "🥪", "An adult cuts a sandwich into two equal pieces.", "How many pieces are made?", "Two equal pieces", "share one portion evenly between two people", "Neither person receives a visibly larger part when the halves are placed together."),
        _Job("set out a waste bag", "🛍️", "A waste bag hangs beside the table.", "Where does the waste bag hang?", "Beside the table", "provide a place for wrappers after eating", "The wrappers collect in the bag rather than blowing across the grass."),
        _Job("choose a shaded seat", "🌳", "The group lays a mat under a leafy tree.", "What shades the mat?", "A leafy tree", "rest out of direct sun", "The mat lies inside the tree's shadow while the open lawn remains bright."),
    )),
    _World("Reuse centre", "at the supervised reuse centre", (
        _Job("sort clean paper", "📄", "Clean dry paper goes in a tray marked with a sheet.", "What goes in the marked tray?", "Clean dry paper", "collect suitable paper for another use", "The unmarked backs can be drawn on; wet or dirty sheets are kept out."),
        _Job("flatten a box", "📦", "An adult opens a box's flaps and folds it flat.", "What is folded flat?", "A box", "reduce the space used by an empty container", "Several flattened boxes fit where one open box used to stand."),
        _Job("save a jar", "🫙", "A clean empty jar holds spare buttons.", "What does the jar hold?", "Spare buttons", "reuse a container to organize small objects", "The buttons stop scattering, and no new storage pot is needed."),
        _Job("repair a bag", "👜", "An adult sews a loose handle back onto a cloth bag.", "What is sewn back on?", "A loose handle", "make a damaged item usable again", "The repaired bag can carry the test books without the handle coming away."),
        _Job("sort fabric pieces", "🧵", "Large fabric scraps go in a basket for patchwork.", "Where do the fabric scraps go?", "A basket", "save leftover material for a later project", "The next patchwork square is cut from a saved piece instead of new cloth."),
        _Job("read a sorting sign", "♻️", "A local sign shows which clean cartons this centre accepts.", "What shows accepted cartons?", "A local sign", "follow this centre's rules rather than guess", "A carton not shown on the sign is set aside for a helper to check."),
        _Job("exchange a puzzle", "🧩", "A complete puzzle gets a checked label before the swap.", "What gets a checked label?", "A complete puzzle", "pass on an item with all its needed parts", "The next player can finish the picture because the pieces were counted first."),
    )),
    _World("Bakery helpers", "at the bakery with an adult", (
        _Job("shape bread rolls", "🥖", "Equal pieces of dough form a row of small rolls.", "What forms the small rolls?", "Dough", "make portions that are similar in size", "The rolls begin with matching portions instead of one large piece and several tiny ones."),
        _Job("mark a recipe", "📝", "A tick goes beside each ingredient after it is added.", "What marks an added ingredient?", "A tick", "avoid adding the same ingredient twice", "When interrupted, the baker checks the ticks rather than guessing what is already in the bowl."),
        _Job("cool the bread", "🍞", "An adult puts baked bread on a wire rack.", "Where does the bread cool?", "A wire rack", "allow air to reach beneath a warm loaf", "The open spaces leave the underside exposed instead of trapping it against a solid tray."),
        _Job("stamp a biscuit", "🍪", "A star cutter presses through biscuit dough.", "What shape does the cutter make?", "A star", "make repeatable shapes from rolled dough", "Each cut biscuit has the same outline as the cutter."),
        _Job("label an order", "🏷️", "A customer's order number goes on a closed box.", "What goes on the closed box?", "An order number", "match a packed order with the right request", "The worker compares the number on the receipt with the number on the box."),
        _Job("brush a pastry", "🖌️", "An adult brushes a thin glaze over a pastry.", "What covers the pastry?", "A thin glaze", "spread a topping evenly over the surface", "The brush leaves a light coat across the top instead of one thick puddle."),
        _Job("cover resting dough", "🥣", "A clean cover rests over a bowl of dough.", "What covers the bowl?", "A clean cover", "keep the resting dough's surface from drying", "The covered surface stays soft while a small uncovered test piece forms a dry skin."),
    )),
    _World("Community centre", "at the community centre", (
        _Job("welcome a visitor", "🪧", "A sign with a waving hand stands at the help desk.", "What picture is on the sign?", "A waving hand", "show where a newcomer can ask for help", "A visitor looks at the sign and walks to the desk instead of trying every room."),
        _Job("make a large-print list", "📝", "The day's activities appear in large dark letters.", "What kind of letters are used?", "Large dark letters", "make information easier to read from farther away", "A visitor reads the list without moving as close as they did to the small print."),
        _Job("arrange chairs", "🪑", "A wide gap stays between the rows of chairs.", "What stays between the rows?", "A wide gap", "leave a usable route through a seated group", "A visitor with a walking frame passes along the gap without moving chairs."),
        _Job("prepare a quiet corner", "📚", "Books and cushions wait away from the loud games.", "What soft things are in the corner?", "Cushions", "offer a calmer activity alongside lively ones", "A visitor leaves the noisy game and keeps taking part by reading in the quieter space."),
        _Job("share an announcement", "📢", "The spoken message is also written on a board.", "Where is the message written?", "On a board", "reach people who miss the spoken message", "A visitor arriving after the announcement still reads the new meeting time."),
        _Job("collect suggestions", "📮", "Blank cards sit beside a closed suggestion box.", "What sits beside the box?", "Blank cards", "let visitors contribute ideas without speaking to a crowd", "A quiet visitor writes an idea and drops it in while the meeting continues."),
        _Job("lend a game", "🎲", "The game's name and return day go on a loan card.", "What day is written on the card?", "The return day", "keep track of when a borrowed item is due back", "Both the helper and the borrower check the same written day instead of remembering different dates."),
    )),
)


def _alternatives(world, job, field):
    correct = getattr(job, field)
    result = []
    for other in world.jobs:
        value = getattr(other, field)
        if value != correct and value not in result:
            result.append(value)
        if len(result) == 2:
            return result
    raise ValueError(f"Not enough distinct {field} choices in {world.title}")


def _job_text(job, level):
    if level == "SPROUT":
        return job.scene
    if level == "TRAIL":
        return f"{job.scene} This helps to {job.purpose}. {job.observation}"
    return f"{job.scene} {job.observation}"


def _branch(world, own, other, level, shared):
    text = f"You choose to {own.label}. {_job_text(own, level)}"
    if shared:
        text += f" Your friend chooses to {other.label}. {_job_text(other, level)}"
        if level == "SPROUT":
            prompt = f"For your job: {own.prompt[0].lower() + own.prompt[1:]}"
            answer = own.answer
            distractors = _alternatives(world, own, "answer")
        else:
            prompt = (
                "Which explanation matches your job and your friend's job?"
                if level == "TRAIL" else
                "Which conclusion connects each job with its own evidence?"
            )
            answer = f"Your job helps to {own.purpose}; your friend's helps to {other.purpose}."
            distractors = [
                f"Your job helps to {other.purpose}; your friend's helps to {own.purpose}.",
                f"Both jobs mainly help to {own.purpose}.",
                f"Both jobs mainly help to {other.purpose}.",
            ]
    else:
        text += f" The job to {other.label} will wait until later."
        if level == "SPROUT":
            prompt, answer = own.prompt, own.answer
            distractors = _alternatives(world, own, "answer")
        else:
            prompt = (
                f"Why is it useful to {own.label}?"
                if level == "TRAIL" else
                f"What purpose of choosing to {own.label} is best supported by the result?"
            )
            answer = f"To {own.purpose}."
            distractors = [
                f"To {purpose}." for purpose in _alternatives(world, own, "purpose")
            ]
    return {
        "label": own.label[0].upper() + own.label[1:],
        "text": text,
        "question": question(prompt, answer, distractors),
    }


def _chapter_entry(world, first, second, level, shared):
    if shared:
        opening = (
            f"You and a friend are {world.setting}. Two jobs need doing: "
            f"{first.label} and {second.label}. Choose your job. Your friend will do the other."
        )
    else:
        opening = (
            f"You are {world.setting}. There is time for one job now: "
            f"{first.label} or {second.label}. You can do the other job later. Which will you choose?"
        )
    return {
        "title": world.title,
        "picture": picture(
            f"{first.symbol} {second.symbol}",
            f"Two job clues: {first.label}; {second.label}.",
        ),
        "passage": [opening],
        "branches": [
            _branch(world, first, second, level, shared),
            _branch(world, second, first, level, shared),
        ],
        "hint": (
            "Read only the chapter you chose. Look for the detail the question asks about."
            if level == "SPROUT" else
            "Keep each job with its own action and result. Use your chosen chapter, not the other branch."
        ),
    }


def chapter(level: str):
    """Yield 997 two-branch activities from 24 settings and 168 authored jobs.

    A choice of one job postpones the other; sharing assigns the two distinct
    jobs to different readers. Both are substantive changes to the story, not
    alternate titles or shuffled answer choices. All settings occur throughout
    the output, including the final partial round.
    """
    _check_level(level)
    count = 0
    for first_index, second_index in combinations(range(7), 2):
        for shared in (False, True):
            for world in _WORLDS:
                yield _chapter_entry(
                    world, world.jobs[first_index], world.jobs[second_index], level, shared
                )
                count += 1
                if count == _COUNT:
                    return
    raise ValueError(f"Chapter source has only {count} {level} activities")
