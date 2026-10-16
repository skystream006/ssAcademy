"""Curated practice examples for the fifty original Sprout grammar concepts."""

from .grammar_games import LESSONS, Lesson


def _noun_person():
    scenes = (
        ("farmer", "grows", "carrots"),
        ("nurse", "checks", "a bandage"),
        ("teacher", "reads", "a story"),
        ("driver", "parks", "a bus"),
        ("artist", "paints", "a rainbow"),
        ("singer", "hums", "a tune"),
        ("gardener", "waters", "the roses"),
        ("dentist", "cleans", "teeth"),
        ("carpenter", "builds", "a shelf"),
        ("dancer", "twirls", "across the stage"),
        ("pilot", "flies", "a plane"),
        ("librarian", "sorts", "the books"),
        ("cook", "stirs", "the soup"),
        ("plumber", "fixes", "a tap"),
        ("coach", "teaches", "a new game"),
        ("tailor", "sews", "a coat"),
        ("sailor", "steers", "a boat"),
        ("firefighter", "climbs", "a ladder"),
        ("photographer", "takes", "a picture"),
    )
    for person, action, detail in scenes:
        yield (
            f"The {person} {action} {detail}.",
            "Which word names a person?",
            person,
            ("The", action),
            f"Find the naming word for someone who {action} {detail}.",
            f"'{person.capitalize()}' names a person. '{action.capitalize()}' tells "
            "what that person does, so it is an action word instead.",
        )


def _noun_proper_place():
    scenes = (
        ("We saw a tall tower in London.", "London", "saw", "tower"),
        ("My aunt grows flowers in Delhi.", "Delhi", "grows", "flowers"),
        ("Our train arrived in Rome.", "Rome", "train", "arrived"),
        ("We crossed a busy bridge in Tokyo.", "Tokyo", "crossed", "bridge"),
        ("My cousin goes to school in Cairo.", "Cairo", "cousin", "school"),
        ("We watched a puppet show in Prague.", "Prague", "watched", "show"),
        ("Our family ate noodles in Hanoi.", "Hanoi", "family", "noodles"),
        ("We heard street music in Dublin.", "Dublin", "heard", "music"),
        ("My uncle works at a zoo in Sydney.", "Sydney", "works", "zoo"),
        ("We rode a tram in Lisbon.", "Lisbon", "rode", "tram"),
        ("Our friends visited a museum in Lima.", "Lima", "friends", "museum"),
        ("We took a boat ride in Venice.", "Venice", "boat", "ride"),
        ("My sister learned to skate in Oslo.", "Oslo", "sister", "skate"),
        ("We bought warm bread in Madrid.", "Madrid", "bought", "bread"),
        ("Our class sent a letter to a school in Accra.", "Accra", "class", "letter"),
        ("We explored a garden in Seoul.", "Seoul", "explored", "garden"),
        ("My grandparents keep bees near Nairobi.", "Nairobi", "keep", "bees"),
        ("We flew a kite in Athens.", "Athens", "flew", "kite"),
        ("Our team played a match in Boston.", "Boston", "team", "match"),
    )
    for sentence, city, wrong_one, wrong_two in scenes:
        yield (
            sentence,
            "Which word is the special name of a city?",
            city,
            (wrong_one, wrong_two),
            f"Look beyond the everyday word '{wrong_two}' for a particular place's name.",
            f"'{city}' is a proper noun because it names one particular city. "
            f"'{wrong_one}' and '{wrong_two}' are not city names here.",
        )


def _noun_singular():
    scenes = (
        ("The shelf holds one vase and several bowls.", "vase", "bowls", "holds"),
        ("A plate carries one peach and some grapes.", "peach", "grapes", "carries"),
        ("The gardener plants one rose beside three tulips.", "rose", "tulips", "plants"),
        ("A child packs one sock and two shirts.", "sock", "shirts", "packs"),
        ("The pond has one frog and many ducks.", "frog", "ducks", "has"),
        ("A tray holds one muffin and four cookies.", "muffin", "cookies", "holds"),
        ("The shop sells one drum and several flutes.", "drum", "flutes", "sells"),
        ("A box contains one shell and many stones.", "shell", "stones", "contains"),
        ("The farmer keeps one goat and five hens.", "goat", "hens", "keeps"),
        ("A drawer stores one spoon and six forks.", "spoon", "forks", "stores"),
        ("The painter uses one brush and many colors.", "brush", "colors", "uses"),
        ("A child draws one star above some clouds.", "star", "clouds", "draws"),
        ("The puppy finds one bone beside two leaves.", "bone", "leaves", "finds"),
        ("A bag holds one lemon and three apples.", "lemon", "apples", "holds"),
        ("The baby drops one block beside several rings.", "block", "rings", "drops"),
        ("A rider leads one pony past the cows.", "pony", "cows", "leads"),
        ("The cook cuts one carrot and several beans.", "carrot", "beans", "cuts"),
        ("A girl wears one ribbon above her braids.", "ribbon", "braids", "wears"),
        ("The cart carries one pumpkin and many potatoes.", "pumpkin", "potatoes", "carries"),
    )
    for sentence, singular, plural, verb in scenes:
        yield (
            sentence,
            "Which choice is a noun naming just one thing or animal?",
            singular,
            (plural, verb),
            f"Compare the amounts: '{plural}' names a group, not a single item.",
            f"'{singular.capitalize()}' is singular: the sentence says there is one. "
            f"'{plural.capitalize()}' is plural, and '{verb}' is a verb.",
        )


def _noun_subject():
    scenes = (
        ("rabbit", "nibbles", "a", "carrot"),
        ("chef", "mixes", "the", "batter"),
        ("bird", "builds", "a", "nest"),
        ("baby", "shakes", "a", "rattle"),
        ("horse", "pulls", "a", "cart"),
        ("girl", "catches", "a", "ball"),
        ("boy", "opens", "the", "gate"),
        ("squirrel", "buries", "a", "nut"),
        ("cat", "chases", "a", "feather"),
        ("painter", "cleans", "the", "brush"),
        ("monkey", "peels", "a", "banana"),
        ("farmer", "drives", "the", "tractor"),
        ("pupil", "rings", "the", "bell"),
        ("bear", "catches", "a", "fish"),
        ("beaver", "cuts", "a", "branch"),
        ("clown", "juggles", "a", "scarf"),
        ("goat", "eats", "a", "leaf"),
        ("child", "flies", "a", "kite"),
        ("frog", "catches", "a", "fly"),
    )
    for subject, verb, article, obj in scenes:
        yield (
            f"The {subject} {verb} {article} {obj}.",
            f"Which noun tells who {verb} {article} {obj}?",
            subject,
            (obj, verb),
            f"Ask who does the action, rather than what happens to the {obj}.",
            f"'{subject.capitalize()}' is the subject noun: the {subject} does "
            f"the action '{verb}'. The {obj} receives that action.",
        )


def _noun_plural_s():
    scenes = (
        ("book", "books", "reading", "The reader borrows two ___ from the library."),
        ("car", "cars", "driving", "Three ___ stop at the crossing."),
        ("apple", "apples", "picking", "The farmer picks four ___ from a tree."),
        ("pencil", "pencils", "drawing", "We sharpen our ___ before drawing."),
        ("chair", "chairs", "sitting", "The helpers arrange six ___ in a circle."),
        ("drum", "drums", "drumming", "The band plays three ___ at the parade."),
        ("sock", "socks", "wearing", "Both ___ are drying by the heater."),
        ("kite", "kites", "flying", "Several ___ fly above the beach."),
        ("bell", "bells", "ringing", "Two ___ ring at the school gate."),
        ("spoon", "spoons", "stirring", "The cook washes five ___ after lunch."),
        ("seed", "seeds", "planting", "We sow many ___ in the garden."),
        ("tent", "tents", "camping", "The campers put up three ___ beside the lake."),
        ("star", "stars", "shining", "The child draws four ___ around the moon."),
        ("coin", "coins", "counting", "I drop two ___ into my money box."),
        ("shell", "shells", "collecting", "We collect several ___ along the shore."),
        ("lamp", "lamps", "glowing", "Two ___ light the reading corner."),
        ("ribbon", "ribbons", "tying", "The dancer ties two ___ around the gift."),
        ("balloon", "balloons", "floating", "Five ___ float above the party table."),
        ("crayon", "crayons", "coloring", "We share a pack of twelve ___."),
    )
    for noun, plural, action, sentence in scenes:
        yield (
            sentence,
            f"Choose the word for more than one {noun}.",
            plural,
            (noun, action),
            f"Think about the usual plural ending for a naming word like '{noun}'.",
            f"'{plural.capitalize()}' is the plural of '{noun}'. Add -s because "
            f"the sentence is about more than one {noun}.",
        )


def _noun_plural_es():
    scenes = (
        ("fox", "foxes", "The ranger sees two ___ near their den."),
        ("bus", "buses", "Three ___ wait outside the school."),
        ("dish", "dishes", "We dry the clean ___ after dinner."),
        ("brush", "brushes", "The artist rinses her two ___."),
        ("bench", "benches", "Several ___ face the pond."),
        ("lunch", "lunches", "The cook packs four ___ for the trip."),
        ("branch", "branches", "Birds rest on the tree's ___."),
        ("peach", "peaches", "The basket holds six ripe ___."),
        ("beach", "beaches", "The island has three sandy ___."),
        ("sandwich", "sandwiches", "We make two ___ for our picnic."),
        ("watch", "watches", "The clock shop repairs broken ___."),
        ("match", "matches", "Our team wins both football ___."),
        ("coach", "coaches", "Two ___ help the children learn to swim."),
        ("glass", "glasses", "The waiter fills three ___ with juice."),
        ("dress", "dresses", "The shop displays several colorful ___."),
        ("class", "classes", "Two ___ share the playground."),
        ("wish", "wishes", "The fairy grants three ___."),
        ("patch", "patches", "Dad sews two ___ onto the old quilt."),
        ("torch", "torches", "The campers switch on their ___ at dusk."),
    )
    for singular, plural, sentence in scenes:
        yield (
            sentence,
            f"Choose the correctly spelled plural of '{singular}'.",
            plural,
            (singular, singular + "s"),
            f"Say '{singular}' aloud and think about the extra syllable its plural needs.",
            f"The plural of '{singular}' is '{plural}'. This noun adds -es, "
            "not just -s, to name more than one.",
        )


def _noun_irregular_plural():
    scenes = (
        ("mouse", "mice", "Two ___ nibble grain in the barn.", "mouses", "mouse"),
        ("goose", "geese", "A flock of ___ swims across the lake.", "gooses", "goose"),
        ("tooth", "teeth", "I brush all my ___ before bed.", "tooths", "tooth"),
        ("foot", "feet", "The baby splashes both ___ in the water.", "foots", "foot"),
        ("man", "men", "Three ___ lift the heavy table.", "mans", "man"),
        ("woman", "women", "Two ___ sing a duet on stage.", "womans", "woman"),
        ("person", "people", "Many ___ wait for the train.", "peoples", "person"),
        ("ox", "oxen", "The farmer's two ___ pull a wagon.", "oxs", "ox"),
        ("sheep", "sheep", "Five ___ graze beside the fence.", "sheeps", "sheepes"),
        ("deer", "deer", "Three ___ walk quietly through the woods.", "deers", "deeres"),
        ("moose", "moose", "Two ___ drink from the forest lake.", "mooses", "meese"),
        ("calf", "calves", "The farm's four ___ rest beside their mothers.", "calfs", "calf"),
        ("leaf", "leaves", "Orange ___ cover the autumn path.", "leafs", "leaf"),
        ("wolf", "wolves", "A pack of ___ howls at night.", "wolfs", "wolf"),
        ("knife", "knives", "The cook puts two butter ___ on the tray.", "knifes", "knife"),
        ("loaf", "loaves", "The baker sells three ___ of bread.", "loafs", "loaf"),
        ("shelf", "shelves", "Books fill both ___ above the desk.", "shelfs", "shelf"),
        ("half", "halves", "I cut the orange into two equal ___.", "halfs", "half"),
        ("elf", "elves", "Three tiny ___ help the shoemaker in the story.", "elfs", "elf"),
    )
    for singular, plural, sentence, wrong_one, wrong_two in scenes:
        if singular == plural:
            rule = "Its singular and plural spellings are the same."
        elif singular.endswith(("f", "fe")):
            rule = "Its ending changes to -ves for the plural."
        else:
            rule = "Its plural changes in a special way instead of simply adding -s."
        yield (
            sentence,
            f"Which word is the plural of '{singular}' in this sentence?",
            plural,
            (wrong_one, wrong_two),
            f"Picture the group described here: {sentence} Remember that some plurals are unusual.",
            f"The plural of '{singular}' is '{plural}'. {rule}",
        )


def _proper_name_capital():
    scenes = (
        ("Lena", "The new pupil introduces herself as ___.", "the new pupil"),
        ("Omar", "Our team's goalkeeper is called ___.", "the goalkeeper"),
        ("Priya", "The name on my pen pal's letter is ___.", "the pen pal"),
        ("Felix", "The boy feeding the fish is named ___.", "the fish feeder"),
        ("Amina", "The girl playing the flute is ___.", "the flute player"),
        ("Hugo", "My cousin who builds model boats is ___.", "the boat builder"),
        ("Sofia", "The winner of the drawing contest is ___.", "the contest winner"),
        ("Arun", "Our guide at the garden is called ___.", "the garden guide"),
        ("Nora", "The child carrying the library books is ___.", "the book carrier"),
        ("Diego", "The dancer bows and says his name is ___.", "the dancer"),
        ("Mei", "The girl sharing her crayons is named ___.", "the crayon sharer"),
        ("Idris", "The boy holding the kite string is ___.", "the kite flyer"),
        ("Tessa", "The singer's name on the poster is ___.", "the singer"),
        ("Evan", "The child planting sunflower seeds is ___.", "the seed planter"),
        ("Ruby", "My friend who cares for a rabbit is ___.", "the rabbit's owner"),
        ("Luis", "The boy helping to set the table is ___.", "the table-setting helper"),
        ("Zara", "The girl reading the poem is named ___.", "the poem reader"),
        ("Noah", "The child showing us a shell collection is ___.", "the shell collector"),
        ("Anya", "Our new chess partner is called ___.", "the chess partner"),
    )
    for name, sentence, person in scenes:
        lower = name.lower()
        misplaced = lower[0] + lower[1].upper() + lower[2:]
        yield (
            sentence,
            f"Which spelling gives {name}'s name its usual capital letter?",
            name,
            (lower, misplaced),
            f"The blank names {person}; recall the usual rule for capitalizing people's names.",
            f"'{name}' is the correct spelling of this name. A person's name "
            "starts with a capital letter, not a capital in the middle.",
        )


def _verb_action():
    scenes = (
        ("dolphin", "leaps", "above the waves"),
        ("bee", "buzzes", "near a flower"),
        ("toddler", "crawls", "across the rug"),
        ("duck", "paddles", "through the pond"),
        ("rabbit", "hops", "over a stone"),
        ("snake", "slithers", "through the grass"),
        ("spider", "spins", "a web"),
        ("puppy", "digs", "in the sand"),
        ("penguin", "waddles", "over the ice"),
        ("robin", "pecks", "at a seed"),
        ("dancer", "spins", "on one foot"),
        ("runner", "sprints", "toward the finish"),
        ("child", "claps", "after the song"),
        ("donkey", "brays", "in the field"),
        ("frog", "croaks", "beside the pond"),
        ("owl", "hoots", "from a branch"),
        ("baby", "yawns", "before a nap"),
        ("horse", "trots", "along the path"),
        ("squirrel", "climbs", "a tall tree"),
    )
    for subject, action, detail in scenes:
        yield (
            f"The {subject} {action} {detail}.",
            f"Which word tells what the {subject} does?",
            action,
            ("The", subject),
            f"Imagine the {subject} moving or making a sound; find its action word.",
            f"'{action.capitalize()}' is the verb. It tells what the {subject} "
            f"does {detail}, rather than naming the {subject}.",
        )


def _verb_being():
    scenes = (
        ("The blanket is warm.", "is", "blanket", "warm"),
        ("The grapes are sweet.", "are", "grapes", "sweet"),
        ("The sky was cloudy.", "was", "sky", "cloudy"),
        ("The puppies were sleepy.", "were", "puppies", "sleepy"),
        ("The pebble is smooth.", "is", "pebble", "smooth"),
        ("The boots are muddy.", "are", "boots", "muddy"),
        ("The lantern was bright.", "was", "lantern", "bright"),
        ("The children were cheerful.", "were", "children", "cheerful"),
        ("The feather is light.", "is", "feather", "light"),
        ("The towels are damp.", "are", "towels", "damp"),
        ("The hallway was quiet.", "was", "hallway", "quiet"),
        ("The strawberries were ripe.", "were", "strawberries", "ripe"),
        ("The bowl is empty.", "is", "bowl", "empty"),
        ("The kittens are playful.", "are", "kittens", "playful"),
        ("The puzzle was tricky.", "was", "puzzle", "tricky"),
        ("The sandwiches were fresh.", "were", "sandwiches", "fresh"),
        ("The ribbon is purple.", "is", "ribbon", "purple"),
        ("The windows are clean.", "are", "windows", "clean"),
        ("The path was narrow.", "was", "path", "narrow"),
    )
    for sentence, verb, noun, description in scenes:
        yield (
            sentence,
            f"Which word links '{noun}' to its description, '{description}'?",
            verb,
            (noun, description),
            f"Find the short linking verb, not the word that describes the {noun}.",
            f"'{verb.capitalize()}' is a being verb here. It links '{noun}' "
            f"to the adjective '{description}' rather than showing an action.",
        )


_PRESENT_ACTIONS = (
    ("rabbit", "rabbits", "hop", "hops", "hopping", "over the low wall"),
    ("bee", "bees", "buzz", "buzzes", "buzzing", "around the roses"),
    ("child", "children", "read", "reads", "reading", "before bed"),
    ("duck", "ducks", "swim", "swims", "swimming", "across the pond"),
    ("pony", "ponies", "pull", "pulls", "pulling", "the little cart"),
    ("pupil", "pupils", "write", "writes", "writing", "in a notebook"),
    ("kitten", "kittens", "chase", "chases", "chasing", "a rolling ball"),
    ("farmer", "farmers", "grow", "grows", "growing", "beans each summer"),
    ("dancer", "dancers", "stretch", "stretches", "stretching", "before a show"),
    ("baby", "babies", "sleep", "sleeps", "sleeping", "after lunch"),
    ("artist", "artists", "paint", "paints", "painting", "pictures of birds"),
    ("goat", "goats", "chew", "chews", "chewing", "the fresh grass"),
    ("teacher", "teachers", "help", "helps", "helping", "new pupils"),
    ("squirrel", "squirrels", "hide", "hides", "hiding", "nuts in the ground"),
    ("dog", "dogs", "fetch", "fetches", "fetching", "sticks in the park"),
    ("cook", "cooks", "wash", "washes", "washing", "vegetables for soup"),
    ("driver", "drivers", "stop", "stops", "stopping", "at the red light"),
    ("robin", "robins", "search", "searches", "searching", "for worms"),
    ("gardener", "gardeners", "water", "waters", "watering", "the young trees"),
)


def _present_singular_agreement():
    for noun, _, base, singular, ongoing, detail in _PRESENT_ACTIONS:
        yield (
            f"As a regular habit, the {noun} ___ {detail}.",
            f"Which present-tense verb fits one {noun}?",
            singular,
            (base, ongoing),
            f"Check the number of doers: this sentence names just one {noun}.",
            f"Use '{singular}' with the singular subject 'the {noun}'. "
            f"'The {noun} {singular} {detail}' describes a regular action.",
        )


def _present_plural_agreement():
    for _, nouns, base, singular, ongoing, detail in _PRESENT_ACTIONS:
        yield (
            f"As a regular habit, the {nouns} ___ {detail}.",
            f"Which present-tense verb fits several {nouns}?",
            base,
            (singular, ongoing),
            f"The subject '{nouns}' names several doers; match the verb to that group.",
            f"Use '{base}' with the plural subject 'the {nouns}'. "
            f"'The {nouns} {base} {detail}' describes their regular action.",
        )


def _past_regular():
    scenes = (
        ("walk", "walked", "walking", "along the beach"),
        ("paint", "painted", "painting", "a picture of a ship"),
        ("plant", "planted", "planting", "a row of beans"),
        ("wash", "washed", "washing", "my muddy boots"),
        ("help", "helped", "helping", "my neighbor carry a bag"),
        ("clap", "clapped", "clapping", "at the end of the play"),
        ("bake", "baked", "baking", "a tray of buns"),
        ("smile", "smiled", "smiling", "at the little baby"),
        ("wave", "waved", "waving", "to the train driver"),
        ("dance", "danced", "dancing", "to the drum beat"),
        ("carry", "carried", "carrying", "a picnic basket"),
        ("tidy", "tidied", "tidying", "the art corner"),
        ("play", "played", "playing", "a game of chess"),
        ("open", "opened", "opening", "a letter from my cousin"),
        ("close", "closed", "closing", "the garden gate"),
        ("kick", "kicked", "kicking", "the ball toward the goal"),
        ("pick", "picked", "picking", "a ripe pear"),
        ("look", "looked", "looking", "through the telescope"),
        ("visit", "visited", "visiting", "the city aquarium"),
    )
    for base, past, ongoing, detail in scenes:
        yield (
            f"Yesterday, I ___ {detail}.",
            "Which verb shows that the action already happened?",
            past,
            (base, ongoing),
            f"The action involving '{detail}' happened before today, not right now.",
            f"'{past.capitalize()}' is the regular past tense of '{base}'. "
            f"'Yesterday' tells us that the action '{base}' has already happened.",
        )


def _past_irregular():
    scenes = (
        ("eat", "ate", "eating", "a bowl of porridge"),
        ("drink", "drank", "drinking", "some cool water"),
        ("see", "saw", "seeing", "a rainbow above the hills"),
        ("run", "ran", "running", "around the playground"),
        ("swim", "swam", "swimming", "across the small pool"),
        ("sing", "sang", "singing", "a song about the moon"),
        ("take", "took", "taking", "a photo of the ducks"),
        ("give", "gave", "giving", "my sister a paper flower"),
        ("make", "made", "making", "a crown from leaves"),
        ("find", "found", "finding", "a smooth stone"),
        ("ride", "rode", "riding", "a pony along the trail"),
        ("write", "wrote", "writing", "a note to my teacher"),
        ("draw", "drew", "drawing", "a map of the park"),
        ("bring", "brought", "bringing", "a snack for the walk"),
        ("buy", "bought", "buying", "a loaf at the bakery"),
        ("catch", "caught", "catching", "the ball with both hands"),
        ("build", "built", "building", "a tower of blocks"),
        ("sleep", "slept", "sleeping", "in a tent beside the lake"),
        ("wear", "wore", "wearing", "my striped scarf"),
    )
    for base, past, ongoing, detail in scenes:
        yield (
            f"Yesterday, I ___ {detail}.",
            f"Choose the past form of '{base}'.",
            past,
            (base, ongoing),
            f"Recall the special past form of '{base}'; this action is already over.",
            f"'{past.capitalize()}' is the irregular past tense of '{base}'. "
            f"It fits 'Yesterday, I {past} {detail}' without adding -ed.",
        )


def _future_will():
    scenes = (
        ("plant", "planted", "planting", "a sunflower"),
        ("read", "have read", "reading", "a new story"),
        ("make", "made", "making", "a paper boat"),
        ("feed", "fed", "feeding", "the rabbits"),
        ("visit", "visited", "visiting", "the science museum"),
        ("bake", "baked", "baking", "an apple pie"),
        ("draw", "drew", "drawing", "a picture of my home"),
        ("clean", "cleaned", "cleaning", "my bicycle"),
        ("pack", "packed", "packing", "a bag for the picnic"),
        ("fly", "flew", "flying", "my new kite"),
        ("play", "played", "playing", "a tune on the piano"),
        ("build", "built", "building", "a model bridge"),
        ("send", "sent", "sending", "a postcard to my uncle"),
        ("learn", "learned", "learning", "a new dance"),
        ("pick", "picked", "picking", "berries at the farm"),
        ("wash", "washed", "washing", "the muddy football"),
        ("wear", "wore", "wearing", "my warm gloves"),
        ("help", "helped", "helping", "with the class garden"),
        ("watch", "watched", "watching", "a puppet show"),
    )
    for base, past, ongoing, detail in scenes:
        yield (
            f"Tomorrow, I ___ {detail}.",
            "Which choice clearly tells about a future plan?",
            f"will {base}",
            (past, ongoing),
            f"The plan to {base} {detail} is for a day that has not arrived yet.",
            f"'Will {base}' describes a future action. Tomorrow is after today, "
            f"so 'I will {base} {detail}' tells about the plan.",
        )


def _being_am():
    descriptions = (
        "excited about our trip to the zoo",
        "proud of my paper kite",
        "careful with the glass bowl",
        "hungry after the long walk",
        "curious about the tiny seeds",
        "busy building a block castle",
        "grateful for your help",
        "tired after football practice",
        "glad to see my cousin",
        "safe inside the warm house",
        "quiet while the baby sleeps",
        "eager to learn this song",
        "sorry about the spilled juice",
        "taller than the little fence",
        "first in the lunch line",
        "responsible for watering our plant",
        "near the front of the bus",
        "dressed for the rainy weather",
        "able to tie my own shoes",
    )
    for description in descriptions:
        yield (
            f"Right now, I ___ {description}.",
            "Which present being word fits the speaker, I?",
            "am",
            ("is", "are"),
            f"Match the being verb to the speaker describing being {description}.",
            f"'I am {description}' uses the present being verb that agrees "
            "with I. We do not use 'I is' or 'I are'.",
        )


def _being_are():
    descriptions = (
        "welcome to join our game",
        "good at finding hidden shapes",
        "ready for your swimming lesson",
        "helpful when we tidy the room",
        "next in the line for lunch",
        "gentle with the new kitten",
        "brave about trying new things",
        "early for the puppet show",
        "very close to the finish line",
        "safe behind the garden fence",
        "careful when crossing the road",
        "tall enough to reach the shelf",
        "a patient partner for this puzzle",
        "responsible for carrying the map",
        "quick at counting the blocks",
        "quiet during the story",
        "free to choose a library book",
        "cheerful even on rainy days",
        "thoughtful about sharing the paints",
    )
    for description in descriptions:
        yield (
            f"Right now, you ___ {description}.",
            "Which present being word agrees with you?",
            "are",
            ("is", "am"),
            f"This description, '{description}', addresses someone as you.",
            f"'You are {description}' uses the being verb that agrees with you. "
            "That agreement stays the same for one listener or several.",
        )


def _being_past():
    scenes = (
        ("the garden", "wet after the rain"),
        ("the kitten", "curled up in its basket"),
        ("the classroom", "full of paper stars"),
        ("the picnic", "beside the river"),
        ("the puppy", "muddy after its walk"),
        ("the soup", "too hot to eat"),
        ("the playground", "quiet during lunch"),
        ("the cake", "ready before the party"),
        ("the sky", "pink at sunset"),
        ("the boat", "tied to the dock"),
        ("the baby", "asleep in the pram"),
        ("the path", "covered with leaves"),
        ("the gate", "open for the visitors"),
        ("the puzzle", "easy for my brother"),
        ("the basket", "heavy with apples"),
        ("the bus", "late because of the snow"),
        ("the blanket", "soft and warm"),
        ("the pond", "still in the morning"),
        ("the story", "funny enough to make us laugh"),
    )
    for subject, description in scenes:
        yield (
            f"Yesterday, {subject} ___ {description}.",
            "Choose the past being word for this singular subject.",
            "was",
            ("is", "were"),
            f"Think about the time word and the single subject '{subject}'.",
            f"'Yesterday, {subject} was {description}' uses 'was' for one "
            "subject in the past. 'Is' refers to the present instead.",
        )


def _adjective_color():
    scenes = (
        ("blue", "bucket", "leaks"),
        ("green", "tractor", "moves"),
        ("yellow", "lemon", "rolls"),
        ("purple", "ribbon", "flutters"),
        ("orange", "pumpkin", "shines"),
        ("pink", "balloon", "floats"),
        ("brown", "rabbit", "hops"),
        ("white", "swan", "swims"),
        ("black", "kitten", "purrs"),
        ("gray", "cloud", "drifts"),
        ("red", "apple", "falls"),
        ("silver", "bell", "rings"),
        ("gold", "star", "sparkles"),
        ("blue", "sailboat", "rocks"),
        ("green", "lizard", "climbs"),
        ("yellow", "duckling", "paddles"),
        ("purple", "flower", "blooms"),
        ("pink", "shell", "glitters"),
        ("brown", "pony", "trots"),
    )
    for color, noun, verb in scenes:
        yield (
            f"The {color} {noun} {verb}.",
            f"Which word describes the {noun}'s color?",
            color,
            (noun, verb),
            f"Look for a word about the {noun}'s appearance, not its action.",
            f"'{color.capitalize()}' is an adjective describing the {noun}'s "
            f"color. '{noun.capitalize()}' names it, and '{verb}' tells its action.",
        )


def _adjective_before_noun():
    phrases = (
        ("smooth", "stone"),
        ("round", "button"),
        ("tall", "tower"),
        ("long", "ribbon"),
        ("wide", "river"),
        ("soft", "pillow"),
        ("tiny", "seed"),
        ("bright", "lamp"),
        ("heavy", "basket"),
        ("warm", "coat"),
        ("clean", "plate"),
        ("ripe", "peach"),
        ("thick", "book"),
        ("green", "leaf"),
        ("quiet", "room"),
        ("strong", "rope"),
        ("square", "tile"),
        ("wooden", "spoon"),
        ("bumpy", "road"),
    )
    for adjective, noun in phrases:
        correct = f"a {adjective} {noun}"
        yield (
            f"Describe a {noun} that is {adjective}.",
            "Which group correctly puts the describing word just before the noun?",
            correct,
            (f"a {noun} {adjective}", f"{adjective} a {noun}"),
            f"Keep the naming word '{noun}' after its description, with the article first.",
            f"In '{correct}', the adjective '{adjective}' comes immediately "
            f"before the noun '{noun}'. The article 'a' begins the group.",
        )


def _adjective_feeling():
    scenes = (
        ("sad", "sadly", "sadness", "The child feels ___ when a favorite toy breaks."),
        ("proud", "proudly", "pride", "The girl feels ___ after learning to ride her bike."),
        ("calm", "calmly", "calmness", "The boy feels ___ while listening to soft music."),
        ("excited", "excitedly", "excitement", "The pupil feels ___ before the trip to the zoo."),
        ("nervous", "nervously", "nervousness", "The singer feels ___ before her first show."),
        ("brave", "bravely", "bravery", "The child feels ___ after trying the tall slide."),
        ("cheerful", "cheerfully", "cheerfulness", "The gardener feels ___ on the sunny morning."),
        ("hopeful", "hopefully", "hope", "The player feels ___ about the next match."),
        ("thankful", "thankfully", "thankfulness", "The hiker feels ___ for a friend's help."),
        ("worried", "worriedly", "worry", "The boy feels ___ when his kitten goes missing."),
        ("curious", "curiously", "curiosity", "The girl feels ___ about the new science kit."),
        ("peaceful", "peacefully", "peace", "The camper feels ___ beside the quiet lake."),
        ("angry", "angrily", "anger", "The child feels ___ when someone ruins her drawing."),
        ("joyful", "joyfully", "joy", "The grandmother feels ___ when her family visits."),
        ("eager", "eagerly", "eagerness", "The pupil feels ___ to begin the art project."),
        ("shy", "shyly", "shyness", "The new child feels ___ around unfamiliar classmates."),
        ("content", "contentedly", "contentment", "The baby feels ___ after a warm meal."),
        ("confident", "confidently", "confidence", "The swimmer feels ___ after lots of practice."),
        ("grateful", "gratefully", "gratitude", "The girl feels ___ for the handmade gift."),
    )
    for adjective, adverb, noun, sentence in scenes:
        yield (
            sentence,
            "Which adjective describes the person's feeling?",
            adjective,
            (adverb, noun),
            f"Choose a description of the person, not the feeling's name '{noun}'.",
            f"'{adjective.capitalize()}' describes how the person feels. "
            f"'{adverb.capitalize()}' is an adverb, while '{noun}' names the feeling.",
        )


def _adjective_after_being():
    scenes = (
        ("cracker", "crunchy"),
        ("rope", "strong"),
        ("puddle", "deep"),
        ("path", "slippery"),
        ("lemon", "sour"),
        ("snow", "cold"),
        ("suitcase", "heavy"),
        ("bubble", "round"),
        ("room", "quiet"),
        ("biscuit", "sweet"),
        ("hill", "steep"),
        ("window", "clean"),
        ("blanket", "thick"),
        ("pebble", "smooth"),
        ("road", "wide"),
        ("tunnel", "dark"),
        ("carrot", "crisp"),
        ("basket", "empty"),
        ("feather", "light"),
    )
    for noun, adjective in scenes:
        yield (
            f"The {noun} is {adjective}.",
            f"Which word describes the {noun}?",
            adjective,
            (noun, "is"),
            f"Find what the sentence tells you about the {noun}, rather than its name.",
            f"'{adjective.capitalize()}' is an adjective describing the {noun}. "
            "An adjective can follow the being verb 'is' instead of coming before a noun.",
        )


def _adjective_comparative():
    scenes = (
        ("tall", "taller", "tallest", "tree"),
        ("short", "shorter", "shortest", "pencil"),
        ("small", "smaller", "smallest", "shell"),
        ("big", "bigger", "biggest", "pumpkin"),
        ("wide", "wider", "widest", "bridge"),
        ("narrow", "narrower", "narrowest", "path"),
        ("deep", "deeper", "deepest", "pond"),
        ("high", "higher", "highest", "shelf"),
        ("low", "lower", "lowest", "branch"),
        ("fast", "faster", "fastest", "train"),
        ("slow", "slower", "slowest", "snail"),
        ("warm", "warmer", "warmest", "scarf"),
        ("cool", "cooler", "coolest", "room"),
        ("light", "lighter", "lightest", "bag"),
        ("heavy", "heavier", "heaviest", "rock"),
        ("soft", "softer", "softest", "pillow"),
        ("hard", "harder", "hardest", "biscuit"),
        ("bright", "brighter", "brightest", "lamp"),
        ("dark", "darker", "darkest", "cave"),
    )
    for adjective, comparative, superlative, noun in scenes:
        yield (
            f"This {noun} is {adjective}. That {noun} is even ___ than this one.",
            f"Which word compares these two examples of a {noun}?",
            comparative,
            (adjective, superlative),
            f"There are two examples of a {noun}; choose a form for comparing the pair.",
            f"'{comparative.capitalize()}' compares the two examples of a {noun}. "
            f"'{superlative.capitalize()}' would pick the most {adjective} one in a group.",
        )


def _adverb_how():
    scenes = (
        ("The girl", "whispers", "quietly", "girl"),
        ("The drummer", "plays", "loudly", "drummer"),
        ("The dancer", "steps", "lightly", "dancer"),
        ("The baby", "sleeps", "peacefully", "baby"),
        ("The puppy", "bounces", "happily", "puppy"),
        ("The runner", "moves", "quickly", "runner"),
        ("The boy", "answers", "politely", "boy"),
        ("The artist", "paints", "carefully", "artist"),
        ("The child", "waits", "patiently", "child"),
        ("The lion", "roars", "fiercely", "lion"),
        ("The singer", "bows", "gracefully", "singer"),
        ("The swimmer", "kicks", "strongly", "swimmer"),
        ("The mouse", "creeps", "silently", "mouse"),
        ("The kitten", "yawns", "sleepily", "kitten"),
        ("The gardener", "works", "cheerfully", "gardener"),
        ("The pony", "trots", "steadily", "pony"),
        ("The sailor", "pulls", "firmly", "sailor"),
        ("The robot", "walks", "stiffly", "robot"),
        ("The duckling", "waddles", "clumsily", "duckling"),
    )
    for subject, verb, adverb, noun in scenes:
        yield (
            f"{subject} {verb} {adverb}.",
            f"Which word tells how {subject.lower()} {verb}?",
            adverb,
            (noun, verb),
            f"Ask about the manner of the action '{verb}', not who performs it.",
            f"'{adverb.capitalize()}' is an adverb describing how {subject.lower()} "
            f"{verb}. '{verb.capitalize()}' names the action itself.",
        )


def _adverb_when():
    scenes = (
        ("We will visit tomorrow.", "tomorrow", "We", "visit", "the visit"),
        ("I painted yesterday.", "yesterday", "I", "painted", "the painting"),
        ("The concert begins tonight.", "tonight", "concert", "begins", "the concert"),
        ("We can eat now.", "now", "We", "eat", "the meal"),
        ("The guests will arrive later.", "later", "guests", "arrive", "the guests' arrival"),
        ("The flowers opened today.", "today", "flowers", "opened", "the flowers opening"),
        ("Our bus arrived early.", "early", "bus", "arrived", "the bus's arrival"),
        ("The rain will stop soon.", "soon", "rain", "stop", "the rain stopping"),
        ("We played outside recently.", "recently", "We", "played", "the outdoor play"),
        ("I have seen this movie before.", "before", "I", "movie", "seeing the movie"),
        ("We will wash the cups afterward.", "afterward", "wash", "cups", "washing the cups"),
        ("The cat crept out overnight.", "overnight", "cat", "crept", "the cat's outing"),
        ("Our new game starts immediately.", "immediately", "game", "starts", "the game's start"),
        ("The spring buds will appear shortly.", "shortly", "buds", "appear", "the buds appearing"),
        ("The turtle finally reached the pond.", "finally", "turtle", "reached", "the turtle reaching the pond"),
        ("The picnic ended late.", "late", "picnic", "ended", "the picnic ending"),
        ("Dad has already packed the tent.", "already", "packed", "tent", "packing the tent"),
        ("We made the posters earlier.", "earlier", "made", "posters", "making the posters"),
        ("We built the shelter first.", "first", "built", "shelter", "building the shelter"),
    )
    for sentence, adverb, wrong_one, wrong_two, event in scenes:
        yield (
            sentence,
            f"Which word tells when {event} happens or happened?",
            adverb,
            (wrong_one, wrong_two),
            f"Look for a time clue about {event}, rather than a person or action.",
            f"'{adverb.capitalize()}' is an adverb giving information about "
            f"the time of {event}. It answers a question about when.",
        )


def _adverb_where():
    scenes = (
        ("The children went upstairs.", "upstairs", "children", "went", "the children went"),
        ("The puppy waited downstairs.", "downstairs", "puppy", "waited", "the puppy waited"),
        ("The kittens stayed inside.", "inside", "kittens", "stayed", "the kittens stayed"),
        ("The family ate outdoors.", "outdoors", "family", "ate", "the family ate"),
        ("The choir practiced indoors.", "indoors", "choir", "practiced", "the choir practiced"),
        ("A squirrel hid nearby.", "nearby", "squirrel", "hid", "the squirrel hid"),
        ("Please put the basket here.", "here", "put", "basket", "the basket should go"),
        ("We found the lost mitten there.", "there", "found", "mitten", "we found the mitten"),
        ("The balloons drifted away.", "away", "balloons", "drifted", "the balloons drifted"),
        ("The children hurried home.", "home", "children", "hurried", "the children hurried"),
        ("The hikers rested below.", "below", "hikers", "rested", "the hikers rested"),
        ("The stars sparkled above.", "above", "stars", "sparkled", "the stars sparkled"),
        ("The rabbit hopped outside.", "outside", "rabbit", "hopped", "the rabbit hopped"),
        ("The climber looked upward.", "upward", "climber", "looked", "the climber looked"),
        ("The leaf floated downward.", "downward", "leaf", "floated", "the leaf floated"),
        ("The runner stepped forward.", "forward", "runner", "stepped", "the runner stepped"),
        ("The crab scuttled sideways.", "sideways", "crab", "scuttled", "the crab scuttled"),
        ("The hikers walked north.", "north", "hikers", "walked", "the hikers walked"),
        ("The ducks swam ashore.", "ashore", "ducks", "swam", "the ducks swam"),
    )
    for sentence, adverb, wrong_one, wrong_two, action in scenes:
        yield (
            sentence,
            f"Which word tells where {action}?",
            adverb,
            (wrong_one, wrong_two),
            f"Find a place or direction clue for the action: {action}.",
            f"'{adverb.capitalize()}' is an adverb of place or direction here. "
            f"It tells where {action}, rather than naming the action.",
        )


def _adverb_frequency():
    scenes = (
        ("I often sketch animals.", "often", "sketch", "animals", "many times"),
        ("We sometimes bake muffins.", "sometimes", "bake", "muffins", "on some occasions"),
        ("She never drops litter.", "never", "drops", "litter", "not at any time"),
        ("He usually wears boots.", "usually", "wears", "boots", "on most occasions"),
        ("They always wash their hands.", "always", "wash", "hands", "every time"),
        ("I rarely lose my pencil.", "rarely", "lose", "pencil", "not often"),
        ("We water the plants daily.", "daily", "water", "plants", "every day"),
        ("The team meets weekly.", "weekly", "team", "meets", "every week"),
        ("Our class visits the library monthly.", "monthly", "visits", "library", "every month"),
        ("The family celebrates this holiday yearly.", "yearly", "family", "celebrates", "every year"),
        ("I often count the ducks.", "often", "count", "ducks", "many times"),
        ("We sometimes share a picnic.", "sometimes", "share", "picnic", "on some occasions"),
        ("She never forgets her lunch.", "never", "forgets", "lunch", "not at any time"),
        ("He usually reads comics.", "usually", "reads", "comics", "on most occasions"),
        ("They always return borrowed toys.", "always", "return", "toys", "every time"),
        ("I rarely hear thunder.", "rarely", "hear", "thunder", "not often"),
        ("We practice our song daily.", "daily", "practice", "song", "every day"),
        ("The club gathers weekly.", "weekly", "club", "gathers", "every week"),
        ("The gardener occasionally trims the hedge.", "occasionally", "trims", "hedge", "from time to time"),
    )
    for sentence, adverb, wrong_one, wrong_two, meaning in scenes:
        yield (
            sentence,
            "Which word tells how often the action happens?",
            adverb,
            (wrong_one, wrong_two),
            f"Look for a clue to repetition in this scene about {wrong_two}.",
            f"'{adverb.capitalize()}' means '{meaning}'. In '{sentence}' "
            "it tells how often the action happens, rather than naming the action.",
        )


def _adverb_ly_form():
    scenes = (
        ("quiet", "quietly", "quietness", "The mouse creeps ___ past the sleeping cat."),
        ("loud", "loudly", "loudness", "The crowd cheers ___ at the finish line."),
        ("soft", "softly", "softness", "The rain falls ___ on the leaves."),
        ("slow", "slowly", "slowness", "The turtle walks ___ across the sand."),
        ("quick", "quickly", "quickness", "The rabbit hops ___ toward its burrow."),
        ("careful", "carefully", "carefulness", "The child carries the full cup ___."),
        ("cheerful", "cheerfully", "cheerfulness", "The gardener waves ___ to a neighbor."),
        ("patient", "patiently", "patience", "The puppy waits ___ for its meal."),
        ("polite", "politely", "politeness", "The boy asks ___ for a turn."),
        ("calm", "calmly", "calmness", "The teacher speaks ___ to the worried child."),
        ("brave", "bravely", "bravery", "The girl tries the climbing wall ___."),
        ("proud", "proudly", "pride", "The artist displays the new painting ___."),
        ("neat", "neatly", "neatness", "The pupil writes ___ along the line."),
        ("safe", "safely", "safety", "The cyclist rides ___ along the path."),
        ("gentle", "gently", "gentleness", "The child strokes the kitten ___."),
        ("happy", "happily", "happiness", "The baby laughs ___ at the puppet."),
        ("sleepy", "sleepily", "sleepiness", "The kitten blinks ___ after its nap."),
        ("angry", "angrily", "anger", "The story's giant shouts ___ at the thief."),
        ("easy", "easily", "ease", "The strong pony pulls the little cart ___."),
    )
    for adjective, adverb, noun, sentence in scenes:
        yield (
            sentence,
            "Choose the -ly adverb that tells how the action happens.",
            adverb,
            (adjective, noun),
            f"The gap describes an action, so consider an adverb related to '{adjective}'.",
            f"'{adverb.capitalize()}' is the adverb related to '{adjective}'. "
            "It describes how the action happens, not what the person or thing is like.",
        )


def _pronoun_subject_person():
    scenes = (
        ("The girl", "is holding a map", "She", "Her", "Hers"),
        ("The man", "is painting the fence", "He", "Him", "His"),
        ("The woman", "is playing the violin", "She", "Her", "Hers"),
        ("My brother", "is feeding the rabbits", "He", "Him", "His"),
        ("My sister", "is sewing a button", "She", "Her", "Hers"),
        ("My father", "is repairing the bicycle", "He", "Him", "His"),
        ("My mother", "is mixing the batter", "She", "Her", "Hers"),
        ("My uncle", "is planting a tree", "He", "Him", "His"),
        ("My aunt", "is taking a photo", "She", "Her", "Hers"),
        ("The boy", "is tying his laces", "He", "Him", "His"),
        ("My grandmother", "is flying a kite", "She", "Her", "Hers"),
        ("My grandfather", "is knitting a scarf", "He", "Him", "His"),
        ("The queen", "is reading a letter", "She", "Her", "Hers"),
        ("The king", "is greeting the visitors", "He", "Him", "His"),
        ("The girl's mother", "is driving the bus", "She", "Her", "Hers"),
        ("The boy's father", "is cooking the soup", "He", "Him", "His"),
        ("The farmer's daughter", "is leading the pony", "She", "Her", "Hers"),
        ("The baker's son", "is drawing a dragon", "He", "Him", "His"),
        ("The little girl", "is watering the herbs", "She", "Her", "Hers"),
    )
    for person, action, pronoun, object_form, possessive in scenes:
        yield (
            f"{person} {action}. ___ {action}.",
            f"Which pronoun replaces '{person.lower()}' as the subject of the second sentence?",
            pronoun,
            (object_form, possessive),
            f"Choose a subject pronoun for the person who {action}.",
            f"'{pronoun}' is a subject pronoun replacing '{person.lower()}'. "
            f"'{pronoun} {action}' keeps that person as the doer of the action.",
        )


def _pronoun_object_person():
    scenes = (
        ("The coach helped ___ tie my laces.", "helped", "tie my laces"),
        ("My aunt taught ___ a new song.", "taught", "learn a new song"),
        ("The nurse gave ___ a bandage for my knee.", "gave", "receive a bandage"),
        ("Dad showed ___ how to fold my kite.", "showed", "learn to fold my kite"),
        ("My friend invited ___ to her birthday party.", "invited", "attend a birthday party"),
        ("The guide led ___ back to my group.", "led", "return to my group"),
        ("The driver let ___ carry my bag onto the bus.", "let", "carry my bag"),
        ("Grandpa took ___ to choose my new boots.", "took", "choose my new boots"),
        ("The teacher asked ___ to read my story.", "asked", "read my story"),
        ("Mum reminded ___ to pack my lunch.", "reminded", "pack my lunch"),
        ("The baker handed ___ the bun I had chosen.", "handed", "receive my bun"),
        ("My sister thanked ___ for helping with her puzzle.", "thanked", "be thanked for helping"),
        ("The painter showed ___ where to hang my picture.", "showed", "hang my picture"),
        ("My brother found ___ when I hid behind the tree.", "found", "be found while hiding"),
        ("The captain chose ___ to carry my team's flag.", "chose", "carry my team's flag"),
        ("The librarian helped ___ find my favorite book.", "helped", "find my favorite book"),
        ("The dentist told ___ to open my mouth.", "told", "open my mouth"),
        ("The gardener let ___ plant my sunflower.", "let", "plant my sunflower"),
        ("My cousin met ___ when I arrived at the gate.", "met", "be met at the gate"),
    )
    for sentence, verb, event in scenes:
        yield (
            sentence,
            "The speaker receives the action. Which pronoun fills the gap?",
            "me",
            ("I", "my"),
            f"The speaker will {event}; choose the form used after the action word.",
            f"'{verb.capitalize()} me' identifies the speaker as the receiver "
            f"of the action in this scene about '{event}'. 'I' is a subject pronoun instead.",
        )


def _pronoun_thing():
    scenes = (
        ("clock", "has two hands", "tells us the time"),
        ("umbrella", "opens wide", "keeps the rain off"),
        ("boat", "has a sail", "moves across the lake"),
        ("lamp", "stands by the bed", "lights the page"),
        ("ball", "is made of rubber", "bounces on the path"),
        ("key", "has a round top", "opens the shed"),
        ("spoon", "is made of wood", "stirs the soup"),
        ("kite", "has a long tail", "flies above the field"),
        ("puzzle", "has many pieces", "shows a picture of a castle"),
        ("bell", "hangs by the door", "rings when we pull the string"),
        ("scarf", "is made of wool", "keeps my neck warm"),
        ("bucket", "has a strong handle", "holds the wet sand"),
        ("leaf", "has turned yellow", "falls from the tree"),
        ("train", "has a red engine", "stops at the station"),
        ("shell", "has a curved edge", "feels smooth"),
        ("pencil", "has a sharp point", "makes a thin line"),
        ("bridge", "crosses the stream", "leads to the farm"),
        ("bottle", "has a tight lid", "keeps the water inside"),
        ("drum", "is round", "makes a deep sound"),
    )
    for noun, first_action, second_action in scenes:
        yield (
            f"The {noun} {first_action}. ___ {second_action}.",
            f"Which pronoun replaces the single {noun}?",
            "It",
            ("They", "We"),
            f"The second sentence still describes one object, the {noun}.",
            f"'It' replaces the singular noun '{noun}'. "
            f"'It {second_action}' describes that same object without repeating its name.",
        )


def _pronoun_plural():
    scenes = (
        ("rabbits", "nibble grass", "twitch their noses"),
        ("bees", "visit flowers", "carry pollen"),
        ("puppies", "chase a ball", "wag their tails"),
        ("owls", "sit on the branch", "hoot at dusk"),
        ("horses", "trot around the field", "shake their manes"),
        ("frogs", "rest by the pond", "leap into the water"),
        ("ants", "march along the path", "carry tiny crumbs"),
        ("butterflies", "land on the bush", "spread their wings"),
        ("penguins", "stand on the ice", "dive for fish"),
        ("lambs", "follow the sheep", "skip across the grass"),
        ("dolphins", "swim beside the boat", "leap above the waves"),
        ("squirrels", "climb the oak", "collect acorns"),
        ("chickens", "scratch the soil", "peck at seeds"),
        ("turtles", "rest on the log", "slide into the pond"),
        ("ponies", "stand at the gate", "wait for their food"),
        ("goats", "walk up the hill", "eat the leaves"),
        ("geese", "cross the lawn", "honk loudly"),
        ("seals", "lie on the rocks", "flap their flippers"),
        ("snails", "crawl across the leaf", "leave shiny trails"),
    )
    for animals, first_action, second_action in scenes:
        yield (
            f"The {animals} {first_action}. ___ {second_action}.",
            f"Which pronoun replaces all the {animals}?",
            "They",
            ("It", "He"),
            f"Keep the whole group of {animals} as the subject, not just one animal.",
            f"'They' replaces the plural noun '{animals}'. "
            f"'They {second_action}' keeps all those animals as the subject.",
        )


def _pronoun_includes_speaker():
    scenes = (
        ("My sister and I", "bake bread", "mix the dough"),
        ("My brother and I", "build a den", "gather branches"),
        ("My cousin and I", "go fishing", "wait by the river"),
        ("My friend and I", "paint a mural", "share the brushes"),
        ("Dad and I", "plant beans", "dig small holes"),
        ("Mum and I", "make soup", "chop the vegetables"),
        ("Grandpa and I", "watch birds", "use binoculars"),
        ("Grandma and I", "sew a quilt", "choose colorful cloth"),
        ("My aunt and I", "fly kites", "run across the field"),
        ("My uncle and I", "fix a shelf", "measure the wood"),
        ("My teammates and I", "practice football", "take turns in goal"),
        ("My classmates and I", "visit a museum", "look at old toys"),
        ("My neighbor and I", "pick apples", "fill a basket"),
        ("The other pupils and I", "clean the playground", "collect fallen leaves"),
        ("My parents and I", "camp by a lake", "put up a tent"),
        ("My sisters and I", "make puppets", "cut out felt shapes"),
        ("My friends and I", "play a board game", "roll the dice"),
        ("My teacher and I", "read a poem", "find rhyming words"),
        ("My brothers and I", "wash the car", "rinse the wheels"),
    )
    for group, first_action, second_action in scenes:
        yield (
            f"{group} {first_action}. ___ {second_action}.",
            "Which subject pronoun includes the speaker and the other people?",
            "We",
            ("Us", "Our"),
            f"The people who {first_action} include the person telling this story.",
            f"'We' means the speaker together with the other people. "
            f"'We {second_action}' uses a subject pronoun for that whole group.",
        )


def _pronoun_ownership():
    scenes = (
        ("The red bicycle", "bicycle"),
        ("The striped scarf", "scarf"),
        ("The paper crown", "crown"),
        ("The wooden flute", "flute"),
        ("The purple kite", "kite"),
        ("The painted flowerpot", "flowerpot"),
        ("The spotted umbrella", "umbrella"),
        ("The toy tractor", "tractor"),
        ("The yellow raincoat", "raincoat"),
        ("The picture of a dragon", "picture"),
        ("The box of crayons", "box"),
        ("The green lunchbox", "lunchbox"),
        ("The story about a whale", "story"),
        ("The model rocket", "rocket"),
        ("The silver bell", "bell"),
        ("The blue notebook", "notebook"),
        ("The knitted hat", "hat"),
        ("The smooth pebble", "pebble"),
        ("The little drum", "drum"),
    )
    for item, noun in scenes:
        yield (
            f"{item} belongs to me. That {noun} is ___.",
            f"Which pronoun shows that the speaker owns the {noun}?",
            "mine",
            ("me", "I"),
            f"The gap must show ownership of the {noun} without adding another noun.",
            f"'Mine' means 'belonging to me'. It stands alone after 'is' "
            f"to show that the speaker owns the {noun}.",
        )


def _preposition_in():
    scenes = (
        ("The fish swims ___ the tank, surrounded by its glass walls.", "fish", "tank"),
        ("The cookies are ___ the tin, with the lid closed above them.", "cookies", "tin"),
        ("The pencils are ___ the pencil case, zipped safely away.", "pencils", "pencil case"),
        ("The coins rattle ___ the money box, which is closed.", "coins", "money box"),
        ("The milk is ___ the jug, ready to pour from it.", "milk", "jug"),
        ("The eggs rest ___ the nest, surrounded by its twig sides.", "eggs", "nest"),
        ("The socks are ___ the drawer, hidden when we shut it.", "socks", "drawer"),
        ("The lunch is ___ the bag, below the closed zip.", "lunch", "bag"),
        ("The tea is ___ the cup, ready to drink from it.", "tea", "cup"),
        ("The toys are ___ the chest, beneath its closed lid.", "toys", "chest"),
        ("The soup is ___ the pot, ready to ladle out.", "soup", "pot"),
        ("The water is ___ the bottle, sealed by its cap.", "water", "bottle"),
        ("The seeds are ___ the packet, which has not been opened.", "seeds", "packet"),
        ("The note is ___ the envelope, sealed away.", "note", "envelope"),
        ("The bread is ___ the oven, behind its closed door.", "bread", "oven"),
        ("The hamster sleeps ___ the cage, behind its closed door.", "hamster", "cage"),
        ("The grapes sit ___ the bowl, surrounded by its sides.", "grapes", "bowl"),
        ("The children are ___ the tent, behind the closed flap.", "children", "tent"),
        ("The key is ___ the pocket, hidden beneath the cloth.", "key", "pocket"),
    )
    for sentence, item, container in scenes:
        yield (
            sentence,
            f"Which preposition places the {item} within the {container}?",
            "in",
            ("beside", "above"),
            f"Think about the {container}'s enclosed space rather than its surroundings.",
            f"'In' places the {item} within the {container}. "
            f"'Beside' or 'above' would put the {item} outside that space.",
        )


def _preposition_under():
    scenes = (
        ("The slippers are ___ the bed, below the mattress.", "slippers", "bed"),
        ("The puppy crawls ___ the table, beneath its top.", "puppy", "table"),
        ("The frog hides ___ the leaf, with the leaf covering it.", "frog", "leaf"),
        ("The toy car rolls ___ the sofa, below its seat.", "toy car", "sofa"),
        ("The stream flows ___ the bridge, below its walkway.", "stream", "bridge"),
        ("The chick shelters ___ the hen's wing, which covers it.", "chick", "hen's wing"),
        ("The ball rests ___ the chair, below the seat.", "ball", "chair"),
        ("The worm lies ___ the stone, covered by the stone.", "worm", "stone"),
        ("The child's head is ___ the umbrella, beneath its fabric.", "child's head", "umbrella"),
        ("The treasure box is ___ the floorboards, hidden beneath them.", "treasure box", "floorboards"),
        ("The mouse squeezes ___ the gate through a gap at the bottom.", "mouse", "gate"),
        ("The kitten sleeps ___ the blanket, with the blanket over it.", "kitten", "blanket"),
        ("The roots spread ___ the soil, below its surface.", "roots", "soil"),
        ("The shoes are ___ the lowest shelf, on the floor beneath it.", "shoes", "lowest shelf"),
        ("The drawing lies ___ the book, covered by its back cover.", "drawing", "book"),
        ("The snowdrops grow ___ the tree's branches, which spread above them.", "snowdrops", "tree's branches"),
        ("The duck swims ___ the low branch, with the branch above its head.", "duck", "low branch"),
        ("The button fell ___ the cupboard, into the gap below its base.", "button", "cupboard"),
        ("The small boat passes ___ the arch, beneath its curved top.", "small boat", "arch"),
    )
    for sentence, item, cover in scenes:
        yield (
            sentence,
            f"Which preposition puts the {item} below the {cover}?",
            "under",
            ("on", "over"),
            f"Picture the height of the {item} compared with the {cover}.",
            f"'Under' puts the {item} below the {cover}. "
            f"The other choices would put the {item} on the surface or higher up instead.",
        )


def _preposition_between():
    scenes = (
        ("The swing has a tree on each side. The swing is ___ the trees.", "swing", "trees"),
        ("A book sits with a bookend on each side. It is ___ the bookends.", "book", "bookends"),
        ("Two chairs stand on opposite sides of a lamp. The lamp is ___ the chairs.", "lamp", "chairs"),
        ("A path runs with a field on each side. It runs ___ the fields.", "path", "fields"),
        ("Two pillows have a teddy in the gap separating them. The teddy is ___ the pillows.", "teddy", "pillows"),
        ("A shop stands with a house on either side. The shop is ___ the houses.", "shop", "houses"),
        ("Two flowerpots stand on opposite sides of a watering can. The can is ___ the pots.", "watering can", "pots"),
        ("A child stands with a parent on each side. The child is ___ the parents.", "child", "parents"),
        ("A cheese slice has a slice of bread above it and one below. The cheese is ___ the bread slices.", "cheese", "bread slices"),
        ("A nest rests in the gap separating two branches. The nest is ___ the branches.", "nest", "branches"),
        ("Two socks lie on opposite sides of a shoe. The shoe is ___ the socks.", "shoe", "socks"),
        ("A pebble lies in the gap separating two shells. The pebble is ___ the shells.", "pebble", "shells"),
        ("A flag hangs with a window on each side. It hangs ___ the windows.", "flag", "windows"),
        ("Two books lie with a ruler in the space separating them. The ruler is ___ the books.", "ruler", "books"),
        ("A goat stands with a pony on each side. The goat is ___ the ponies.", "goat", "ponies"),
        ("A stream flows with a hill on each side. It flows ___ the hills.", "stream", "hills"),
        ("Two drums stand on opposite sides of a cymbal. The cymbal is ___ the drums.", "cymbal", "drums"),
        ("A toy train sits with a block at each end. The train is ___ the blocks.", "toy train", "blocks"),
        ("A blue bead has a red bead on each side. The blue bead is ___ the red beads.", "blue bead", "red beads"),
    )
    for sentence, item, neighbors in scenes:
        yield (
            sentence,
            f"Which word shows the {item}'s position relative to the two {neighbors}?",
            "between",
            ("above", "inside"),
            f"Picture the two {neighbors} with a space separating them.",
            f"'Between' places the {item} in the space separating the two "
            f"{neighbors}, rather than above or inside them.",
        )


def _preposition_on():
    scenes = (
        ("The cushion rests ___ the seat's top surface.", "cushion", "seat"),
        ("The snail crawls ___ the top surface of the leaf.", "snail", "leaf"),
        ("The toast lies ___ the plate's upper surface.", "toast", "plate"),
        ("The snow settles ___ the roof's outer top surface.", "snow", "roof"),
        ("The cup stands ___ the tray's flat top.", "cup", "tray"),
        ("The kitten rests ___ the rug, touching its upper surface.", "kitten", "rug"),
        ("The pencil lies ___ the notebook's top cover.", "pencil", "notebook"),
        ("The robin perches ___ the branch, its feet touching the top.", "robin", "branch"),
        ("The duck floats ___ the pond's surface, rather than below it.", "duck", "pond"),
        ("The cake sits ___ the stand's flat top.", "cake", "stand"),
        ("The books rest ___ the shelf's upper surface.", "books", "shelf"),
        ("The butterfly lands ___ the flower, its feet touching the petals.", "butterfly", "flower"),
        ("The pebble lies ___ the path's surface.", "pebble", "path"),
        ("The clock stands ___ the mantel's top surface.", "clock", "mantel"),
        ("The soap rests ___ the edge of the sink, touching its top.", "soap", "edge of the sink"),
        ("The frog sits ___ the lily pad's upper surface.", "frog", "lily pad"),
        ("The loaf rests ___ the cutting board's top.", "loaf", "cutting board"),
        ("The mittens lie ___ the bench's seat, touching its top.", "mittens", "bench"),
        ("The toy robot stands ___ the box's closed lid.", "toy robot", "box"),
    )
    for sentence, item, support in scenes:
        yield (
            sentence,
            f"Which preposition places the {item} against the upper surface?",
            "on",
            ("under", "inside"),
            f"Think about the surface of the {support} supporting the {item}.",
            f"'On' shows contact with the upper surface of the {support}. "
            f"This places the {item} at the surface, not below or enclosed inside it.",
        )


def _preposition_direction():
    scenes = (
        ("We are heading toward the school. We walk ___ the school.", "school", "walk"),
        ("The bus heads toward the station. It travels ___ the station.", "station", "travels"),
        ("The pony heads toward the stable. It trots ___ the stable.", "stable", "trots"),
        ("We set out toward the beach. We cycle ___ the beach.", "beach", "cycle"),
        ("The train heads toward the city. It travels ___ the city.", "city", "travels"),
        ("The bird returns toward its nest. It flies ___ the nest.", "nest", "flies"),
        ("The hikers head toward the campsite. They walk ___ the campsite.", "campsite", "walk"),
        ("The child heads toward the slide. She runs ___ the slide.", "slide", "runs"),
        ("The duck heads toward the island. It swims ___ the island.", "island", "swims"),
        ("The puppy goes toward its bowl. It trots ___ the bowl.", "bowl", "trots"),
        ("We head toward the bakery. We stroll ___ the bakery.", "bakery", "stroll"),
        ("The children head toward the playground. They skip ___ the playground.", "playground", "skip"),
        ("The car heads toward the airport. It drives ___ the airport.", "airport", "drives"),
        ("The boat moves toward the dock. It sails ___ the dock.", "dock", "sails"),
        ("The rabbit heads toward its burrow. It hops ___ the burrow.", "burrow", "hops"),
        ("We head toward the museum. We travel ___ the museum.", "museum", "travel"),
        ("The runner heads toward the finish line. She sprints ___ the finish line.", "finish line", "sprints"),
        ("The caterpillar heads toward a leaf. It crawls ___ the leaf.", "leaf", "crawls"),
        ("The skater heads toward the rink's exit. He glides ___ the exit.", "exit", "glides"),
    )
    for sentence, destination, verb in scenes:
        yield (
            sentence,
            f"Which preposition makes the {destination} the destination?",
            "to",
            ("from", "inside"),
            f"The movement '{verb}' has a goal: reaching the {destination}.",
            f"'To' points toward the destination, the {destination}. "
            "'From' would describe leaving that place, not approaching it.",
        )


def _preposition_time():
    scenes = (
        ("Our school bell rings ___ nine o'clock.", "nine o'clock", "school bell"),
        ("The train leaves ___ half past ten.", "half past ten", "train's departure"),
        ("The puppet show begins ___ two o'clock.", "two o'clock", "puppet show"),
        ("We eat breakfast ___ seven o'clock.", "seven o'clock", "breakfast"),
        ("Football practice starts ___ four o'clock.", "four o'clock", "football practice"),
        ("The bus arrives ___ quarter past eight.", "quarter past eight", "bus's arrival"),
        ("The ducks wake ___ dawn.", "dawn", "ducks waking"),
        ("The street lamps light up ___ dusk.", "dusk", "street lamps lighting up"),
        ("The moon was high ___ midnight.", "midnight", "moon's position"),
        ("We open the gifts ___ lunchtime.", "lunchtime", "opening the gifts"),
        ("I read my story ___ bedtime.", "bedtime", "reading the story"),
        ("The class stretches ___ midday.", "midday", "class stretching"),
        ("The bakery opens ___ six o'clock.", "six o'clock", "bakery opening"),
        ("The choir meets ___ quarter to five.", "quarter to five", "choir meeting"),
        ("The cake will be ready ___ half past three.", "half past three", "cake being ready"),
        ("The boat returns ___ sunset.", "sunset", "boat's return"),
        ("The rooster calls ___ sunrise.", "sunrise", "rooster's call"),
        ("We share fruit ___ snack time.", "snack time", "sharing fruit"),
        ("The museum tour starts ___ eleven o'clock.", "eleven o'clock", "museum tour"),
    )
    for sentence, time, event in scenes:
        yield (
            sentence,
            f"Which preposition fits before '{time}'?",
            "at",
            ("on", "into"),
            f"Treat '{time}' as a point in the day for the {event}.",
            f"We say 'at {time}' to name when the {event} happens. "
            "'At' introduces a point in time rather than a whole date here.",
        )


def _conjunction_add():
    scenes = (
        ("I put both paints ___ brushes in the art box.", "paints", "brushes"),
        ("We invited both our cousins ___ our neighbors.", "our cousins", "our neighbors"),
        ("The gardener planted both beans ___ peas.", "beans", "peas"),
        ("The band has both drums ___ flutes.", "drums", "flutes"),
        ("I washed both my face ___ my hands.", "my face", "my hands"),
        ("We saw both seals ___ penguins at the aquarium.", "seals", "penguins"),
        ("The farmer keeps both goats ___ sheep.", "goats", "sheep"),
        ("I borrowed both a poem book ___ a comic.", "a poem book", "a comic"),
        ("We need both flour ___ milk for the pancakes.", "flour", "milk"),
        ("The picnic basket holds both apples ___ grapes.", "apples", "grapes"),
        ("My picture shows both mountains ___ rivers.", "mountains", "rivers"),
        ("The dancer wears both a hat ___ a cape.", "a hat", "a cape"),
        ("We brought both towels ___ swimsuits to the pool.", "towels", "swimsuits"),
        ("The baby can both clap ___ wave.", "clap", "wave"),
        ("Our class will both sing ___ dance at the show.", "sing", "dance"),
        ("I cleaned both the table ___ the chairs.", "the table", "the chairs"),
        ("The puppy likes both running ___ fetching.", "running", "fetching"),
        ("We found both shells ___ pebbles on the shore.", "shells", "pebbles"),
        ("The soup contains both carrots ___ potatoes.", "carrots", "potatoes"),
    )
    for sentence, first, second in scenes:
        yield (
            sentence,
            "Which joining word includes both things or actions?",
            "and",
            ("but", "because"),
            f"The word 'both' tells you to include '{first}' along with the other item.",
            f"'And' joins '{first}' with '{second}'. "
            "Both are included; the sentence is not giving a contrast or a reason.",
        )


def _conjunction_choice():
    scenes = (
        ("Choose one snack: a banana ___ a peach.", "a banana", "a peach"),
        ("Pick one game: chess ___ cards.", "chess", "cards"),
        ("Choose one pet to draw: a rabbit ___ a hamster.", "a rabbit", "a hamster"),
        ("Pick just one activity: swimming ___ cycling.", "swimming", "cycling"),
        ("Choose one story: the dragon tale ___ the pirate tale.", "the dragon tale", "the pirate tale"),
        ("Wear one hat: the cap ___ the sunhat.", "the cap", "the sunhat"),
        ("Take just one tool: the spade ___ the rake.", "the spade", "the rake"),
        ("Select one color: green ___ purple.", "green", "purple"),
        ("Choose one instrument: a drum ___ a flute.", "a drum", "a flute"),
        ("Pick one sandwich filling: cheese ___ egg.", "cheese", "egg"),
        ("Choose one seat: the chair ___ the stool.", "the chair", "the stool"),
        ("Take just one toy: the truck ___ the doll.", "the truck", "the doll"),
        ("Choose one pudding: yogurt ___ jelly.", "yogurt", "jelly"),
        ("Pick one ride: the train ___ the carousel.", "the train", "the carousel"),
        ("Choose one flower to plant: a rose ___ a tulip.", "a rose", "a tulip"),
        ("Select one shape: a circle ___ a square.", "a circle", "a square"),
        ("Take one route: the bridge ___ the tunnel.", "the bridge", "the tunnel"),
        ("Pick one place to visit: the zoo ___ the farm.", "the zoo", "the farm"),
        ("Choose one job: watering ___ weeding.", "watering", "weeding"),
    )
    for sentence, first, second in scenes:
        yield (
            sentence,
            "Which joining word offers the two alternatives?",
            "or",
            ("and", "because"),
            f"Only one option is wanted: '{first}' is one possibility, not an extra item.",
            f"'Or' offers a choice between '{first}' and '{second}'. "
            "The instruction asks for only one, rather than both.",
        )


def _conjunction_contrast():
    scenes = (
        ("The bag looks small", "it holds many books", "size", "capacity"),
        ("The day is sunny", "the air is cold", "sunshine", "cold air"),
        ("The puzzle looks easy", "it takes a long time to solve", "easy appearance", "difficulty"),
        ("The puppy is tiny", "its bark is loud", "small size", "a loud bark"),
        ("The coat is old", "it still keeps me warm", "age", "usefulness"),
        ("The road is long", "the walk feels short", "distance", "how the walk feels"),
        ("The melon is heavy", "the child carries it easily", "weight", "easy carrying"),
        ("The soup smells delicious", "it tastes too salty", "a good smell", "a salty taste"),
        ("The runner feels tired", "she finishes the race", "tiredness", "finishing"),
        ("The room is noisy", "the baby stays asleep", "noise", "sleeping"),
        ("The bird has bright feathers", "its song is soft", "bright appearance", "a soft sound"),
        ("The pool looks warm", "the water feels chilly", "appearance", "temperature"),
        ("The box is plain", "the gift inside is beautiful", "a plain box", "a beautiful gift"),
        ("The hill is steep", "the pony climbs it steadily", "a difficult hill", "steady climbing"),
        ("The child is shy", "she sings on the stage", "shyness", "performing"),
        ("The morning began with rain", "the afternoon is dry", "rain", "dry weather"),
        ("The lemon smells sweet", "its juice tastes sour", "a sweet smell", "a sour taste"),
        ("The snail moves slowly", "it reaches the leaf", "slow movement", "reaching the goal"),
        ("The mittens are thin", "they feel surprisingly warm", "thin cloth", "warmth"),
    )
    for first, second, first_idea, second_idea in scenes:
        yield (
            f"{first}, ___ {second}.",
            "Which joining word best shows a contrast between the ideas?",
            "but",
            ("or", "because"),
            f"Notice the difference between {first_idea} and {second_idea}.",
            f"'But' joins contrasting ideas: {first.lower()}, yet {second}. "
            "The second idea is different from what the first may lead us to expect.",
        )


def _conjunction_reason():
    scenes = (
        ("We put on coats", "the wind was cold"),
        ("I drank some water", "I felt thirsty"),
        ("The child wore boots", "the path was muddy"),
        ("We turned on the lamp", "the room was dark"),
        ("The baby slept", "she was tired"),
        ("We watered the flowers", "the soil was dry"),
        ("The dog wagged its tail", "it was pleased to see us"),
        ("I sharpened my pencil", "its point was blunt"),
        ("We used a map", "we did not know the way"),
        ("The girl held her hat", "the wind might blow it away"),
        ("We washed our hands", "they were dirty"),
        ("The boy smiled", "his friend had arrived"),
        ("We packed a picnic", "we planned to eat at the park"),
        ("I opened the window", "the kitchen was too warm"),
        ("The kitten hid", "the vacuum cleaner was noisy"),
        ("We hurried home", "a storm was coming"),
        ("The gardener tied up the plant", "its stem needed support"),
        ("I borrowed a book", "I wanted a story to read"),
        ("We spoke quietly", "the baby was asleep"),
    )
    for action, reason in scenes:
        yield (
            f"{action} ___ {reason}.",
            "Which joining word introduces the reason for the action?",
            "because",
            ("but", "or"),
            f"Ask why this happened: {action}. The next part gives the cause.",
            f"'Because' gives the reason: {reason}. "
            f"That explains why this happened: {action.lower()}.",
        )


def _conjunction_identify():
    scenes = (
        ("We hop and skip.", "and", "hop", "skip"),
        ("The puppy runs and jumps.", "and", "runs", "jumps"),
        ("I draw and paint.", "and", "draw", "paint"),
        ("Choose whether to sit or stand.", "or", "sit", "stand"),
        ("We clap and cheer.", "and", "clap", "cheer"),
        ("The baby smiles and laughs.", "and", "smiles", "laughs"),
        ("You can walk or cycle.", "or", "walk", "cycle"),
        ("The duck dives and paddles.", "and", "dives", "paddles"),
        ("We plant and water.", "and", "plant", "water"),
        ("I fold and glue.", "and", "fold", "glue"),
        ("You may whisper or speak.", "or", "whisper", "speak"),
        ("The kitten stretches and yawns.", "and", "stretches", "yawns"),
        ("We measure and cut.", "and", "measure", "cut"),
        ("You may read or write.", "or", "read", "write"),
        ("The cook peels and chops.", "and", "peels", "chops"),
        ("We rinse and dry.", "and", "rinse", "dry"),
        ("You can push or pull.", "or", "push", "pull"),
        ("The bird flaps and glides.", "and", "flaps", "glides"),
        ("We listen and learn.", "and", "listen", "learn"),
    )
    for sentence, conjunction, first, second in scenes:
        yield (
            sentence,
            f"Which word joins the action words '{first}' and '{second}'?",
            conjunction,
            (first, second),
            f"Find the connector, not the actions themselves: '{first}', '{second}'.",
            f"'{conjunction.capitalize()}' is the conjunction. It links the verbs "
            f"'{first}' and '{second}' instead of naming an action of its own.",
        )


def _sentence_capital():
    scenes = (
        ("a frog jumping", "A frog jumps."),
        ("the rain stopping", "The rain stops."),
        ("children sharing", "We share our crayons."),
        ("your uncle baking", "My uncle bakes bread."),
        ("a pony eating", "Our pony eats grass."),
        ("two ducks swimming", "Two ducks swim."),
        ("a bell ringing", "That bell rings."),
        ("flowers opening", "These flowers open."),
        ("an owl calling", "An owl calls."),
        ("someone carrying a map", "You carry the map."),
        ("a girl waving", "She waves to us."),
        ("a boy skipping", "He skips along."),
        ("some children singing", "They sing together."),
        ("a leaf falling", "One leaf falls."),
        ("some rabbits hopping", "Some rabbits hop."),
        ("a plant needing water", "Every plant needs water."),
        ("children choosing books", "Each child chooses a book."),
        ("your boots drying", "Both boots are dry."),
        ("a rainbow fading", "This rainbow fades."),
    )
    for scene, sentence in scenes:
        lowercase = sentence[0].lower() + sentence[1:]
        # The wrong interior capital stays inside the first word or next noun.
        index = next(i for i in range(1, len(lowercase)) if lowercase[i].islower())
        misplaced = lowercase[:index] + lowercase[index].upper() + lowercase[index + 1:]
        yield (
            f"Write a telling sentence about {scene}.",
            "Which sentence uses a capital letter in the correct place?",
            sentence,
            (lowercase, misplaced),
            f"Check the beginning of the sentence about {scene}, not a later letter.",
            f"'{sentence}' starts with a capital {sentence[0]}. "
            "A sentence begins with a capital letter, while ordinary later letters stay lowercase.",
        )


def _statement_period():
    statements = (
        "The rabbit eats a carrot",
        "Our bus stops by the gate",
        "I can tie my shoes",
        "The moon shines above the trees",
        "We planted beans in the garden",
        "My sister plays the flute",
        "The kitten sleeps in a basket",
        "A rainbow crosses the sky",
        "The soup smells good",
        "Two ducks float on the pond",
        "I keep shells in a box",
        "The train arrives at noon",
        "My boots are muddy",
        "We share our art supplies",
        "The cake has lemon icing",
        "A butterfly lands on the leaf",
        "The library opens after breakfast",
        "Our team practices on Fridays",
        "My uncle repairs bicycles",
    )
    for statement in statements:
        yield (
            statement + "___",
            "Which mark finishes this ordinary telling sentence?",
            ".",
            ("?", ","),
            f"Decide whether this is asking or telling: '{statement}'.",
            f"'{statement}.' tells a complete fact rather than asking a question. "
            "A period ends an ordinary statement like this one.",
        )


def _question_mark():
    questions = (
        "Who is knocking at the door",
        "What is inside the basket",
        "When does the bus arrive",
        "Why is the puppy barking",
        "How does a kite fly",
        "Can you pass the crayons",
        "Did you feed the fish",
        "Will the seeds grow soon",
        "Are the apples ripe",
        "Is the library open",
        "Do you like this song",
        "Where did the rabbit hide",
        "Which path leads to the pond",
        "How many shells did you find",
        "May I borrow your ruler",
        "Who painted this picture",
        "What makes the bell ring",
        "Have you seen my scarf",
        "Would you like some soup",
    )
    for question in questions:
        yield (
            question + "___",
            "Which mark finishes this direct asking sentence?",
            "?",
            (".", ","),
            f"Someone could reply to '{question}'; think about the sentence's purpose.",
            f"'{question}?' asks for an answer. A question mark, rather than "
            "a period or comma, ends this direct question.",
        )


def _complete_sentence():
    scenes = (
        ("The frog", "leaps", "Beside the pond"),
        ("A kitten", "purrs", "On the cushion"),
        ("The leaves", "fall", "From the tree"),
        ("Our baby", "smiles", "In the pram"),
        ("The chef", "cooks", "In the kitchen"),
        ("A robin", "sings", "On the fence"),
        ("The children", "dance", "Across the stage"),
        ("My sister", "reads", "By the window"),
        ("The horse", "gallops", "Through the field"),
        ("The puppy", "barks", "Behind the gate"),
        ("A bee", "buzzes", "Near the flower"),
        ("The boat", "rocks", "Beside the dock"),
        ("The sun", "shines", "Above the hills"),
        ("Our teacher", "waves", "At the doorway"),
        ("The turtle", "crawls", "Along the sand"),
        ("A squirrel", "climbs", "Up the trunk"),
        ("The bell", "rings", "In the tower"),
        ("My cousin", "skates", "Around the rink"),
        ("The lambs", "skip", "Across the grass"),
    )
    for subject, verb, place in scenes:
        sentence = f"{subject} {verb}."
        yield (
            f"Think about {subject.lower()} {place.lower()}.",
            "Which choice is a whole sentence with a subject and an action?",
            sentence,
            (subject + ".", place + "."),
            f"A place such as '{place.lower()}' alone does not say who does what.",
            f"'{sentence}' is complete: '{subject}' names the subject, "
            f"and '{verb}' tells the action. The other choices are fragments.",
        )


def _article_a_an():
    scenes = (
        ("an", "orange", "I peeled ___ orange for my snack.", "vowel"),
        ("a", "banana", "The monkey carried ___ banana.", "consonant"),
        ("an", "egg", "We found ___ egg in the nest.", "vowel"),
        ("a", "kite", "My sister flew ___ kite at the park.", "consonant"),
        ("an", "umbrella", "I opened ___ umbrella in the rain.", "vowel"),
        ("a", "rabbit", "We watched ___ rabbit hop away.", "consonant"),
        ("an", "octopus", "The picture shows ___ octopus with eight arms.", "vowel"),
        ("a", "turtle", "A child spotted ___ turtle on the log.", "consonant"),
        ("an", "elephant", "We saw ___ elephant splash water.", "vowel"),
        ("a", "flower", "I pressed ___ flower inside my book.", "consonant"),
        ("an", "insect", "The gardener found ___ insect on the leaf.", "vowel"),
        ("a", "sandwich", "Dad packed ___ sandwich for lunch.", "consonant"),
        ("an", "apron", "The cook tied ___ apron around her waist.", "vowel"),
        ("a", "drum", "The musician tapped ___ drum.", "consonant"),
        ("an", "owl", "We heard ___ owl in the woods.", "vowel"),
        ("a", "pencil", "The pupil sharpened ___ pencil.", "consonant"),
        ("an", "envelope", "I sealed the note inside ___ envelope.", "vowel"),
        ("a", "wagon", "The farmer pulled ___ wagon.", "consonant"),
        ("an", "acorn", "A squirrel buried ___ acorn.", "vowel"),
    )
    for article, noun, sentence, sound in scenes:
        other_article = "a" if article == "an" else "an"
        yield (
            sentence,
            f"Which word fits before the single noun '{noun}'?",
            article,
            (other_article, "many"),
            f"Say '{noun}' aloud and listen to its very first sound.",
            f"Use '{article}' before '{noun}' because it begins with a "
            f"{sound} sound. '{article.capitalize()} {noun}' names one thing or animal.",
        )


_CONCEPTS = {
    "noun-person": ("Naming a person", _noun_person),
    "noun-proper-place": ("A city's special name", _noun_proper_place),
    "noun-singular": ("A noun for just one", _noun_singular),
    "noun-subject": ("Who does the action?", _noun_subject),
    "noun-plural-s": ("A regular plural", _noun_plural_s),
    "noun-plural-es": ("A plural with an extra syllable", _noun_plural_es),
    "noun-irregular-plural": ("A special plural form", _noun_irregular_plural),
    "proper-name-capital": ("Capitals in people's names", _proper_name_capital),
    "verb-action": ("Find the action", _verb_action),
    "verb-being": ("Find the linking word", _verb_being),
    "present-singular-agreement": ("A verb for one doer", _present_singular_agreement),
    "present-plural-agreement": ("A verb for several doers", _present_plural_agreement),
    "past-regular": ("A regular action in the past", _past_regular),
    "past-irregular": ("An unusual past-tense form", _past_irregular),
    "future-will": ("A plan for tomorrow", _future_will),
    "being-am": ("The speaker's being word", _being_am),
    "being-are": ("A being word for the listener", _being_are),
    "being-past": ("A state in the past", _being_past),
    "adjective-color": ("Describe a color", _adjective_color),
    "adjective-before-noun": ("Order a description", _adjective_before_noun),
    "adjective-feeling": ("Describe a feeling", _adjective_feeling),
    "adjective-after-being": ("Describe after a being word", _adjective_after_being),
    "adjective-comparative": ("Compare a pair", _adjective_comparative),
    "adverb-how": ("How does it happen?", _adverb_how),
    "adverb-when": ("When does it happen?", _adverb_when),
    "adverb-where": ("Where does it happen?", _adverb_where),
    "adverb-frequency": ("How often does it happen?", _adverb_frequency),
    "adverb-ly-form": ("Build an action's description", _adverb_ly_form),
    "pronoun-subject-person": ("A person as the subject", _pronoun_subject_person),
    "pronoun-object-person": ("The speaker receives an action", _pronoun_object_person),
    "pronoun-thing": ("A pronoun for one object", _pronoun_thing),
    "pronoun-plural": ("A pronoun for an animal group", _pronoun_plural),
    "pronoun-includes-speaker": ("A group including the speaker", _pronoun_includes_speaker),
    "pronoun-ownership": ("Show who owns an object", _pronoun_ownership),
    "preposition-in": ("An enclosed space", _preposition_in),
    "preposition-under": ("A lower position", _preposition_under),
    "preposition-between": ("Neighbors on both sides", _preposition_between),
    "preposition-on": ("Touching a top surface", _preposition_on),
    "preposition-direction": ("Heading toward a destination", _preposition_direction),
    "preposition-time": ("A point in the day", _preposition_time),
    "conjunction-add": ("Include both things", _conjunction_add),
    "conjunction-choice": ("Offer alternatives", _conjunction_choice),
    "conjunction-contrast": ("Join contrasting ideas", _conjunction_contrast),
    "conjunction-reason": ("Explain the reason", _conjunction_reason),
    "conjunction-identify": ("Find the joining word", _conjunction_identify),
    "sentence-capital": ("Start a sentence", _sentence_capital),
    "statement-period": ("Finish a telling sentence", _statement_period),
    "question-mark": ("Finish an asking sentence", _question_mark),
    "complete-sentence": ("A whole thought", _complete_sentence),
    "article-a-an": ("An article for one noun", _article_a_an),
}


def expanded_lessons():
    """Yield nineteen new examples per original concept, in stable suffix order."""
    for original in LESSONS["SPROUT"]:
        title, examples = _CONCEPTS[original.slug]
        for number, example in enumerate(examples(), start=2):
            yield Lesson(
                f"{original.slug}-{number:02d}",
                original.focus,
                title,
                *example,
            )
