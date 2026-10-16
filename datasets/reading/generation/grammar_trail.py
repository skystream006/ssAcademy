"""Curated extra practice for the fifty TRAIL Grammar Quest concepts."""

from .grammar_games import LESSONS, Lesson


# Each action has checked inflections and a complement that works in all the
# tense and auxiliary-verb exercises using this table.
_ACTIONS = (
    ("paint", "paints", "painted", "painting", "a mural", "the bell rang"),
    ("fold", "folds", "folded", "folding", "a paper crane", "the lights went out"),
    ("carry", "carries", "carried", "carrying", "a basket of apples", "the gate opened"),
    ("write", "writes", "wrote", "writing", "a postcard", "the dog barked"),
    ("draw", "draws", "drew", "drawing", "a map of the garden", "the rain began"),
    ("wash", "washes", "washed", "washing", "the muddy boots", "the tap broke"),
    ("plant", "plants", "planted", "planting", "a row of beans", "the sun came out"),
    ("read", "reads", "read", "reading", "a story about whales", "the bus arrived"),
    ("weave", "weaves", "wove", "weaving", "a willow basket", "the doorbell rang"),
    ("mend", "mends", "mended", "mending", "a torn sleeve", "the thread snapped"),
    ("bake", "bakes", "baked", "baking", "a loaf of bread", "the timer beeped"),
    ("clean", "cleans", "cleaned", "cleaning", "the aquarium", "the phone rang"),
    ("pack", "packs", "packed", "packing", "a picnic basket", "the taxi arrived"),
    ("peel", "peels", "peeled", "peeling", "the potatoes", "the pan boiled over"),
    ("sew", "sews", "sewed", "sewing", "a cloth bag", "the needle broke"),
    ("build", "builds", "built", "building", "a shelter for hedgehogs", "the wind rose"),
    ("make", "makes", "made", "making", "a kite", "the glue ran out"),
    ("sweep", "sweeps", "swept", "sweeping", "the kitchen floor", "the cat came in"),
    ("sort", "sorts", "sorted", "sorting", "the recycling", "the truck stopped"),
)

_OWNERSHIP = (
    ("I knitted the scarf.", "me", "scarf", "my", "mine", "I", "me"),
    ("You painted the sign.", "you", "sign", "your", "yours", "you", "yourself"),
    ("He bought the compass.", "him", "compass", "his", "his", "he", "him"),
    ("She made the necklace.", "her", "necklace", "her", "hers", "she", "herself"),
    ("We built the birdhouse.", "us", "birdhouse", "our", "ours", "we", "us"),
    ("They grew the pumpkin.", "them", "pumpkin", "their", "theirs", "they", "them"),
    ("I saved up for the camera.", "me", "camera", "my", "mine", "I", "me"),
    ("You decorated the lantern.", "you", "lantern", "your", "yours", "you", "yourself"),
    ("He carved the spoon.", "him", "spoon", "his", "his", "he", "him"),
    ("She wove the basket.", "her", "basket", "her", "hers", "she", "herself"),
    ("We repaired the canoe.", "us", "canoe", "our", "ours", "we", "us"),
    ("They designed the flag.", "them", "flag", "their", "theirs", "they", "them"),
    ("I wrote the poem.", "me", "poem", "my", "mine", "I", "me"),
    ("You sewed the costume.", "you", "costume", "your", "yours", "you", "yourself"),
    ("He assembled the telescope.", "him", "telescope", "his", "his", "he", "him"),
    ("She drew the portrait.", "her", "portrait", "her", "hers", "she", "herself"),
    ("We planted the orchard.", "us", "orchard", "our", "ours", "we", "us"),
    ("They restored the wagon.", "them", "wagon", "their", "theirs", "they", "them"),
    ("I baked the pie.", "me", "pie", "my", "mine", "I", "me"),
)

_TITLES = {
    "noun-common-versus-proper": "Common names and special names",
    "noun-abstract": "Naming a quality or feeling",
    "noun-collective": "A name for the whole group",
    "plural-consonant-y": "Change y for a plural",
    "plural-leaf": "Plurals that change f to ves",
    "plural-unchanged": "The same spelling for several",
    "possessive-singular": "Show one owner's possession",
    "possessive-plural": "Show several owners' possessions",
    "present-continuous": "An action happening now",
    "past-continuous": "An action that was in progress",
    "past-final-e": "Add d to a verb ending in e",
    "past-double-consonant": "Double a consonant before ed",
    "past-consonant-y": "Change y in the past tense",
    "tense-consistency": "Keep the actions in the past",
    "negative-did-base": "Use a plain verb after did not",
    "question-did": "Ask about a past action",
    "agreement-has": "Choose have or has",
    "agreement-does": "A present question about one subject",
    "modal-ability": "Express an ability",
    "modal-base-form": "A plain verb after should",
    "linking-verb-adjective": "Describe after a linking verb",
    "comparative-y": "Comparing with an ier ending",
    "superlative-est": "Pick one from the whole group",
    "comparative-more": "Compare using more",
    "comparative-irregular": "Comparisons that change spelling",
    "adjective-versus-adverb": "Describe the person or the action",
    "adverb-well": "Good or well for an action?",
    "adverb-comparison": "Compare how actions happen",
    "adverb-sequence": "Put the actions in order",
    "adverb-degree": "An adverb describing an adjective",
    "pronoun-compound-subject": "Two people doing the action",
    "pronoun-compound-object": "Two people receiving the action",
    "possessive-before-noun": "An ownership word before a noun",
    "possessive-stands-alone": "An ownership pronoun on its own",
    "pronoun-reflexive": "A pronoun that points back",
    "pronoun-demonstrative": "Point out near or distant things",
    "pronoun-reference": "Find what the pronoun replaces",
    "preposition-into": "Movement to the inside",
    "preposition-across": "Movement from side to side",
    "preposition-duration": "A preposition for how long",
    "preposition-start-time": "A preposition for the starting point",
    "prepositional-phrase": "Find a whole phrase about place",
    "conjunction-before": "Join actions in time order",
    "conjunction-until": "An action and its endpoint",
    "conjunction-condition": "Join a permission to its condition",
    "conjunction-result": "Join a cause to its result",
    "conjunction-concession": "Something happens despite a difficulty",
    "compound-subject-agreement": "Join two subjects with and",
    "contraction-is-not": "Expand isn't into two words",
    "article-vowel-sound": "Choose an article by sound",
}

_HINTS = {
    "agreement-has": "A singular noun subject uses the same present form as he, she, or it.",
    "agreement-does": "Match the present helping verb to a single person, animal, or thing.",
    "linking-verb-adjective": "A linking verb connects its subject to an adjective describing that subject.",
    "pronoun-compound-subject": "Leave out the other person and the joining word; choose a subject pronoun for the speaker.",
    "pronoun-compound-object": "Leave out the other person and the joining word; choose an object pronoun for the speaker.",
    "pronoun-reference": "Look back for the earlier noun naming the thing used in the next sentence.",
}


def _examples(concept):
    """Yield passage, question, answer, distractors, and a checked explanation."""
    if concept == "noun-common-versus-proper":
        rows = (
            ("On Tuesday, the train reaches Oxford.", "train", "Tuesday", "Oxford", "reaches"),
            ("In April, our family explores Rome.", "family", "April", "Rome", "explores"),
            ("On Friday, the ferry leaves Dover.", "ferry", "Friday", "Dover", "leaves"),
            ("During June, the choir performs in York.", "choir", "June", "York", "performs"),
            ("In August, the museum welcomes visitors from Leeds.", "museum", "August", "Leeds", "welcomes"),
            ("On Sunday, the team travels to Bristol.", "team", "Sunday", "Bristol", "travels"),
            ("During May, the gardener works in Bath.", "gardener", "May", "Bath", "works"),
            ("In March, the river floods near Perth.", "river", "March", "Perth", "floods"),
            ("On Thursday, the orchestra rehearses in Vienna.", "orchestra", "Thursday", "Vienna", "rehearses"),
            ("During July, the ship sails to Lisbon.", "ship", "July", "Lisbon", "sails"),
            ("On Saturday, our class sketches buildings in Madrid.", "buildings", "Saturday", "Madrid", "sketches"),
            ("In October, the artist exhibits paintings in Oslo.", "artist", "October", "Oslo", "exhibits"),
            ("On Wednesday, the pilot lands in Dublin.", "pilot", "Wednesday", "Dublin", "lands"),
            ("During December, the market opens in Prague.", "market", "December", "Prague", "opens"),
            ("In February, the dancer teaches in Berlin.", "dancer", "February", "Berlin", "teaches"),
            ("On Monday, the coach drives to Cardiff.", "coach", "Monday", "Cardiff", "drives"),
            ("During September, the farmer harvests grapes near Naples.", "farmer", "September", "Naples", "harvests"),
            ("In January, the photographer works in Helsinki.", "photographer", "January", "Helsinki", "works"),
            ("During November, the school exchanges letters with Tokyo.", "school", "November", "Tokyo", "exchanges"),
        )
        for passage, noun, time, place, verb in rows:
            yield (
                passage, "Which choice is a common noun rather than a proper name or a verb?",
                noun, (time, place, verb),
                f"'{noun}' names a general kind of person, place, group, or thing. "
                f"'{time}' and '{place}' are proper names; '{verb}' is a verb.",
            )
    elif concept == "noun-abstract":
        rows = (
            ("Her bravery helped the sailor rescue a kitten.", "bravery", "sailor", "kitten", "rescue"),
            ("His courage led the climber past the rocks.", "courage", "climber", "rocks", "led"),
            ("Her honesty helped the shopkeeper find the missing coin.", "honesty", "shopkeeper", "coin", "find"),
            ("His patience helped the puppy learn to fetch a stick.", "patience", "puppy", "stick", "fetch"),
            ("Her wisdom guided the travelers through the forest.", "wisdom", "travelers", "forest", "guided"),
            ("The child felt joy when the kite flew.", "joy", "child", "kite", "flew"),
            ("The boy felt sadness beside the broken toy.", "sadness", "boy", "toy", "felt"),
            ("The carpenter felt pride in the finished chair.", "pride", "carpenter", "chair", "felt"),
            ("The gardener had hope that the seed would sprout.", "hope", "gardener", "seed", "sprout"),
            ("Her curiosity led the explorer into the cave.", "curiosity", "explorer", "cave", "led"),
            ("His fairness helped the children share the marbles.", "fairness", "children", "marbles", "share"),
            ("The bird gained freedom when the cage opened.", "freedom", "bird", "cage", "opened"),
            ("Their friendship began when the neighbors built a fence together.", "friendship", "neighbors", "fence", "built"),
            ("Her generosity supplied the family with blankets.", "generosity", "family", "blankets", "supplied"),
            ("The dog's loyalty kept it beside the farmer.", "loyalty", "dog", "farmer", "kept"),
            ("His creativity turned the cartons into puppets.", "creativity", "cartons", "puppets", "turned"),
            ("Her confidence helped the rider guide the pony.", "confidence", "rider", "pony", "guide"),
            ("His calmness helped the passengers leave the bus.", "calmness", "passengers", "bus", "leave"),
            ("Her determination helped the cyclist climb the hill.", "determination", "cyclist", "hill", "climb"),
        )
        for passage, noun, concrete_a, concrete_b, verb in rows:
            yield (
                passage, "Which noun names a feeling, idea, or quality rather than a physical thing?",
                noun, (concrete_a, concrete_b, verb),
                f"'{noun}' is an abstract noun: it names a feeling, idea, or quality. "
                f"'{concrete_a}' and '{concrete_b}' name things that can be seen or touched.",
            )
    elif concept == "noun-collective":
        rows = (
            ("team", "players", "practices", "daily"),
            ("herd", "cattle", "grazes", "peacefully"),
            ("swarm", "bees", "buzzes", "loudly"),
            ("pack", "wolves", "travels", "together"),
            ("school", "fish", "swims", "swiftly"),
            ("colony", "ants", "works", "busily"),
            ("litter", "puppies", "sleeps", "soundly"),
            ("bunch", "grapes", "hangs", "loosely"),
            ("bouquet", "flowers", "stands", "upright"),
            ("fleet", "ships", "sails", "steadily"),
            ("stack", "plates", "wobbles", "slightly"),
            ("crowd", "spectators", "cheers", "enthusiastically"),
            ("audience", "listeners", "waits", "quietly"),
            ("choir", "singers", "rehearses", "regularly"),
            ("band", "musicians", "performs", "tonight"),
            ("troupe", "dancers", "bows", "gracefully"),
            ("brood", "chicks", "huddles", "closely"),
            ("cluster", "stars", "shines", "brightly"),
            ("pile", "pebbles", "slides", "downward"),
        )
        for group, members, verb, adverb in rows:
            article = "An" if group == "audience" else "A"
            yield (
                f"{article} {group} of {members} {verb} {adverb}.",
                "Which noun names the whole group?", group, (members, verb, adverb),
                f"'{group}' is the collective noun. It names the {members} together as one group, "
                f"while '{members}' names the members of that group.",
            )
    elif concept == "plural-consonant-y":
        rows = (
            ("city", "cities", "The travelers visited several ___ along the coast."),
            ("puppy", "puppies", "The dog led her ___ into the garden."),
            ("baby", "babies", "The nurse weighed the ___ at the clinic."),
            ("berry", "berries", "We picked ripe ___ for the pie."),
            ("cherry", "cherries", "The grocer filled a basket with ___."),
            ("penny", "pennies", "The child counted the ___ in the jar."),
            ("lady", "ladies", "The ___ waved from the balcony."),
            ("party", "parties", "The hall hosts birthday ___ on weekends."),
            ("country", "countries", "The map shows several island ___."),
            ("family", "families", "Many ___ joined the school picnic."),
            ("library", "libraries", "The council opened new ___ for readers."),
            ("butterfly", "butterflies", "Bright ___ fluttered above the flowers."),
            ("daisy", "daisies", "We planted white ___ beside the path."),
            ("pony", "ponies", "The riders brushed the ___ after their lesson."),
            ("lorry", "lorries", "Several ___ delivered timber to the yard."),
            ("hobby", "hobbies", "Gardening and weaving are my favorite ___."),
            ("dictionary", "dictionaries", "The students used ___ to check the spellings."),
            ("factory", "factories", "The old ___ now make bicycles."),
            ("trophy", "trophies", "The winning teams lifted their ___."),
        )
        for singular, plural, passage in rows:
            yield (
                passage, f"Choose the correctly spelled plural of '{singular}'.",
                plural, (singular + "s", singular + "es", singular),
                f"'{singular}' ends in a consonant followed by y. Change y to i and add -es: "
                f"'{plural}' names more than one.",
            )
    elif concept == "plural-leaf":
        rows = (
            ("leaf", "leaves", "The gardener raked fallen ___ off the lawn."),
            ("loaf", "loaves", "The baker took three ___ out of the oven."),
            ("wolf", "wolves", "The ranger watched the ___ moving through the snow."),
            ("calf", "calves", "The farmer led two ___ into the barn."),
            ("shelf", "shelves", "The carpenter fitted new ___ above the desk."),
            ("half", "halves", "We cut each sandwich into two equal ___."),
            ("elf", "elves", "In the story, the ___ stitched shoes during the night."),
            ("thief", "thieves", "The guards caught both ___ beside the stolen cart."),
            ("sheaf", "sheaves", "The workers tied the harvested wheat into ___."),
            ("leaf", "leaves", "The caterpillars chewed holes in several ___."),
            ("loaf", "loaves", "We sliced the fresh ___ for everyone's lunch."),
            ("wolf", "wolves", "At dusk, the ___ began to howl from the ridge."),
            ("calf", "calves", "The newborn ___ drank milk beside their mothers."),
            ("shelf", "shelves", "The librarian arranged books on the empty ___."),
            ("half", "halves", "The two ___ of the broken shell fitted together."),
            ("elf", "elves", "The illustrator drew woodland ___ carrying tiny lanterns."),
            ("thief", "thieves", "In the play, two ___ return the missing jewels."),
            ("sheaf", "sheaves", "The horse pulled a wagon piled with wheat ___."),
            ("leaf", "leaves", "We pressed colorful ___ between the pages of a book."),
        )
        for singular, plural, passage in rows:
            yield (
                passage, f"Choose the standard plural of '{singular}' for more than one.",
                plural, (singular + "s", singular + "es", singular),
                f"The standard plural of '{singular}' is '{plural}'. "
                "This noun changes its final f to v before adding -es.",
            )
    elif concept == "plural-unchanged":
        rows = (
            ("sheep", "The shepherd guided twelve ___ through the gate."),
            ("deer", "Three ___ stepped out from behind the trees."),
            ("moose", "The ranger counted two ___ beside the lake."),
            ("bison", "A herd of twenty ___ crossed the plain."),
            ("salmon", "Several ___ leaped up the waterfall."),
            ("trout", "The clear pool held five ___."),
            ("aircraft", "Two ___ landed on the runway before noon."),
            ("spacecraft", "The museum displayed models of three ___."),
            ("sheep", "The farmer sheared the wool from eight ___."),
            ("deer", "The gardener spotted four ___ eating young shoots."),
            ("moose", "The photographer watched both ___ wade through the marsh."),
            ("bison", "Several ___ sheltered behind the rocky ridge."),
            ("salmon", "The cook wrapped two ___ in foil."),
            ("trout", "We released all six ___ back into the stream."),
            ("aircraft", "The mechanic checked the wings of both ___."),
            ("spacecraft", "The engineers tracked two ___ orbiting the planet."),
            ("sheep", "The dog brought the scattered ___ back to their flock."),
            ("deer", "Fresh snow revealed the tracks of several ___."),
            ("moose", "Two young ___ followed their mother through the reeds."),
        )
        for noun, passage in rows:
            # Avoid regular plural variants that some dictionaries accept
            # for these animal names and compound craft nouns.
            distractors = (
                (noun + "es", noun + "'s", noun + "s'")
                if noun in {"bison", "salmon", "trout", "aircraft", "spacecraft"}
                else (noun + "s", noun + "es", noun + "'s")
            )
            yield (
                passage, f"Which spelling is the usual plural of '{noun}'?",
                noun, distractors,
                f"The usual plural '{noun}' has the same spelling as the singular. "
                "The sentence refers to several, but neither an ending nor an apostrophe is needed.",
            )
    elif concept == "possessive-singular":
        rows = (
            ("dog", "lead"), ("painter", "brush"), ("teacher", "desk"),
            ("farmer", "tractor"), ("bird", "nest"), ("chef", "apron"),
            ("cyclist", "helmet"), ("baby", "blanket"), ("sailor", "compass"),
            ("rabbit", "hutch"), ("actor", "costume"), ("gardener", "spade"),
            ("horse", "saddle"), ("doctor", "bag"), ("drummer", "stool"),
            ("pilot", "map"), ("fox", "den"), ("carpenter", "saw"), ("student", "notebook"),
        )
        for owner, item in rows:
            plural = {"baby": "babies", "fox": "foxes"}.get(owner, owner + "s")
            correct = f"the {owner}'s {item}"
            yield (
                f"The {item} belongs to one {owner}.",
                "Which phrase uses an apostrophe to show that there is exactly one owner?",
                correct, (f"the {plural} {item}", f"the {plural}' {item}", f"the {owner} {item}"),
                f"'{correct}' adds apostrophe-s to the singular owner '{owner}'. "
                f"Putting an apostrophe after '{plural}' would show several owners instead.",
            )
    elif concept == "possessive-plural":
        rows = (
            ("dog", "beds"), ("painter", "brushes"), ("teacher", "desks"),
            ("farmer", "tractors"), ("bird", "nests"), ("chef", "aprons"),
            ("cyclist", "helmets"), ("sailor", "compasses"), ("rabbit", "hutches"),
            ("actor", "costumes"), ("gardener", "spades"), ("horse", "saddles"),
            ("doctor", "bags"), ("drummer", "stools"), ("pilot", "maps"),
            ("carpenter", "saws"), ("student", "notebooks"), ("dancer", "shoes"),
            ("visitor", "tickets"),
        )
        for owner, items in rows:
            plural = owner + "s"
            correct = f"the {plural}' {items}"
            yield (
                f"The {items} belong to several {plural}.",
                "Which phrase uses an apostrophe to show that there is more than one owner?",
                correct, (f"the {owner}'s {items}", f"the {plural} {items}", f"the {owner} {items}"),
                f"'{plural}' already ends in the plural s. Add an apostrophe after that s: "
                f"'{correct}'. Apostrophe-s on '{owner}' would mean only one owner.",
            )
    elif concept == "present-continuous":
        for base, third, past, ing, complement, event in _ACTIONS:
            yield (
                f"Right now, they ___ {complement}.",
                "Choose the present continuous phrase that agrees with 'they'.",
                f"are {ing}", (f"is {ing}", f"are {base}", f"are {third}"),
                f"'They are {ing} {complement}' uses are with the -ing form '{ing}'. "
                "This shows an action happening now and agrees with the plural subject.",
            )
    elif concept == "past-continuous":
        for base, third, past, ing, complement, event in _ACTIONS:
            yield (
                f"Yesterday, I ___ {complement} when {event}.",
                "Which phrase correctly shows my action was in progress in the past?",
                f"was {ing}", (f"were {ing}", f"was {base}", f"am {ing}"),
                f"'I was {ing} {complement}' shows the action was underway when {event}. "
                "Use was, not were, with I in this statement about an actual past event.",
            )
    elif concept == "past-final-e":
        rows = (
            ("bake", "a tray of rolls"), ("smile", "at the puppet show"),
            ("wave", "to the departing ship"), ("close", "the greenhouse door"),
            ("dance", "around the maypole"), ("hope", "for clear skies"),
            ("live", "in a cottage beside the river"), ("move", "the sofa upstairs"),
            ("save", "the last slice for our friend"), ("share", "our lunch with the hikers"),
            ("care", "for an injured bird"), ("race", "along the beach"),
            ("skate", "around the frozen pond"), ("phone", "the museum for directions"),
            ("invite", "our neighbors to the concert"), ("arrive", "before the gates opened"),
            ("taste", "the soup before serving it"), ("use", "a ruler to measure the card"),
            ("glue", "the broken handle back onto the cup"),
        )
        for base, complement in rows:
            past = base + "d"
            yield (
                f"Yesterday, we ___ {complement}.",
                f"Choose the correctly spelled simple past form of '{base}'.",
                past, (base + "ed", base, base + "s"),
                f"'{base}' already ends in e, so add only -d to make '{past}'. "
                f"'Yesterday' places the action '{past} {complement}' in the past.",
            )
    elif concept == "past-double-consonant":
        rows = (
            ("stop", "beside the pedestrian crossing"),
            ("hop", "over the puddles on one foot"),
            ("clap", "at the end of the performance"),
            ("plan", "a route through the mountains"),
            ("grin", "when the magician found our card"),
            ("hug", "our cousins at the station"),
            ("rub", "the wet dog with a towel"),
            ("drop", "pebbles into the stream"),
            ("shop", "for fresh vegetables at the market"),
            ("skip", "along the path with a rope"),
            ("slip", "on the wet wooden steps"),
            ("trap", "air inside a sealed bag"),
            ("wrap", "the parcel in brown paper"),
            ("nod", "to show that we understood"),
            ("pat", "the pony gently on its neck"),
            ("tap", "the rhythm on our knees"),
            ("rip", "the old paper into strips"),
            ("mop", "the spilled juice from the floor"),
            ("drag", "the heavy sled across the snow"),
        )
        for base, complement in rows:
            past = base + base[-1] + "ed"
            yield (
                f"Yesterday, we ___ {complement}.",
                f"Choose the correctly spelled simple past form of '{base}'.",
                past, (base + "ed", base, base + "s"),
                f"'{base}' ends with a short vowel followed by one consonant. "
                f"Double the final {base[-1]} before adding -ed to form '{past}'.",
            )
    elif concept == "past-consonant-y":
        rows = (
            ("cry", "when we heard the sad ending"),
            ("try", "to solve the wooden puzzle"),
            ("carry", "the picnic basket up the hill"),
            ("hurry", "to catch the last ferry"),
            ("worry", "about the lost kitten"),
            ("copy", "the pattern onto tracing paper"),
            ("dry", "the dishes with a clean towel"),
            ("study", "the map before our walk"),
            ("tidy", "the art room after painting"),
            ("reply", "to the invitation"),
            ("spy", "a deer behind the hedge"),
            ("fry", "the onions for the soup"),
            ("apply", "a thin coat of paint to the bench"),
            ("supply", "fresh water to the thirsty hikers"),
            ("rely", "on the compass in the fog"),
            ("bury", "a time capsule under the oak"),
            ("marry", "in a small ceremony by the lake"),
            ("empty", "the rain barrel into buckets"),
            ("deny", "that we had moved the goalposts"),
        )
        for base, complement in rows:
            past = base[:-1] + "ied"
            yield (
                f"Yesterday, we ___ {complement}.",
                f"Choose the correctly spelled simple past form of '{base}'.",
                past, (base + "ed", base, base[:-1] + "ies"),
                f"'{base}' ends in a consonant plus y. Change that y to i before adding -ed: "
                f"'{past}' tells about the completed past action.",
            )
    elif concept == "tense-consistency":
        rows = (
            ("washed the carrots", "chop", "chopped", "chopping", "them for soup"),
            ("opened the parcel", "unwrap", "unwrapped", "unwrapping", "the gift"),
            ("mixed the batter", "pour", "poured", "pouring", "it into a tin"),
            ("measured the wood", "mark", "marked", "marking", "the cutting line"),
            ("picked the apples", "carry", "carried", "carrying", "them home"),
            ("cleaned the jars", "fill", "filled", "filling", "them with jam"),
            ("folded the blankets", "stack", "stacked", "stacking", "them on a shelf"),
            ("drew the outline", "paint", "painted", "painting", "the background"),
            ("brushed the pony", "saddle", "saddled", "saddling", "it for the ride"),
            ("pumped up the tires", "check", "checked", "checking", "the bicycle brakes"),
            ("collected the leaves", "press", "pressed", "pressing", "them in a book"),
            ("tuned the guitars", "play", "played", "playing", "a folk song"),
            ("dug a hole", "plant", "planted", "planting", "an apple tree"),
            ("finished the model", "display", "displayed", "displaying", "it by the window"),
            ("packed the sandwiches", "close", "closed", "closing", "the picnic basket"),
            ("read the instructions", "assemble", "assembled", "assembling", "the kite"),
            ("found the missing glove", "return", "returned", "returning", "it to its owner"),
            ("fed the hens", "collect", "collected", "collecting", "the eggs"),
            ("swept the path", "clear", "cleared", "clearing", "the fallen branches"),
        )
        for first, base, past, ing, second in rows:
            yield (
                f"Yesterday, we {first} and ___ {second}.",
                "Which verb keeps both completed actions in the simple past?",
                past, (base, ing, f"will {base}"),
                f"Both actions happened yesterday: we {first} and {past} {second}. "
                f"'{past}' keeps the second action in the same past tense as the first.",
            )
    elif concept == "negative-did-base":
        for base, third, past, ing, complement, event in _ACTIONS:
            # Read has identical base and past spellings, so its finite
            # distractor must be a past phrase rather than a duplicate answer.
            past_choice = f"had {past}" if past == base else past
            yield (
                f"Yesterday, I did not ___ {complement}.",
                "Which main-verb form correctly follows 'did not'?",
                base, (past_choice, third, ing),
                f"The correct phrase is 'did not {base} {complement}'. "
                f"'Did' already marks the past, so the main verb stays in its base form, '{base}'.",
            )
    elif concept == "question-did":
        for base, third, past, ing, complement, event in _ACTIONS:
            yield (
                f"___ you {base} {complement} yesterday?",
                "Which helping verb correctly asks about this completed past action?",
                "Did", ("Do", "Does", "Will"),
                f"'Did you {base} {complement} yesterday?' asks about the past. "
                f"The helping verb did carries the tense, so '{base}' stays in its base form.",
            )
    elif concept == "agreement-has":
        rows = (
            ("The elephant", "a long trunk"), ("The bicycle", "two sturdy wheels"),
            ("The cottage", "a thatched roof"), ("The peacock", "a colorful tail"),
            ("The violin", "four strings"), ("The octopus", "eight arms"),
            ("The coat", "deep pockets"), ("The rose", "sharp thorns"),
            ("The castle", "a drawbridge"), ("The sailboat", "a tall mast"),
            ("The turtle", "a hard shell"), ("The library", "a quiet reading room"),
            ("The cactus", "thick stems"), ("The kangaroo", "a pouch"),
            ("The backpack", "a broken zip"), ("The clock", "a round face"),
            ("The chef", "a new recipe"), ("The drummer", "a pair of sticks"),
            ("The fox", "a bushy tail"),
        )
        for subject, possession in rows:
            yield (
                f"{subject} ___ {possession}.",
                "Choose the present form of 'have' that agrees with this singular subject.",
                "has", ("have", "having", "were"),
                f"'{subject}' is singular, like he, she, or it. "
                f"The present-tense sentence is '{subject} has {possession}', using has rather than have.",
            )
    elif concept == "agreement-does":
        rows = (
            ("this lantern", "need a new battery"),
            ("the baker", "make sourdough bread"),
            ("that bridge", "cross the river"),
            ("this button", "open the door"),
            ("the parrot", "copy your words"),
            ("the shop", "sell drawing paper"),
            ("this seed", "need sunlight"),
            ("the train", "stop at the coast"),
            ("that path", "lead to the waterfall"),
            ("the kitten", "like its new bed"),
            ("this machine", "wash the dishes"),
            ("the gardener", "grow strawberries"),
            ("that sign", "point toward the harbor"),
            ("the helmet", "fit your head"),
            ("this recipe", "use any eggs"),
            ("the telescope", "show the moon clearly"),
            ("the ferry", "carry bicycles"),
            ("that tree", "lose its leaves in winter"),
            ("the museum", "open on Sundays"),
        )
        for subject, predicate in rows:
            yield (
                f"___ {subject} {predicate}?",
                "Choose the present helping verb that agrees with the singular subject.",
                "Does", ("Do", "Doing", "Done"),
                f"'{subject}' names one person, animal, or thing, so the question starts with does: "
                f"'Does {subject} {predicate}?' The main verb stays in its base form.",
            )
    elif concept == "modal-ability":
        for base, third, past, ing, complement, event in _ACTIONS:
            yield (
                f"I know how to {base} {complement}. I ___ {base} {complement}.",
                "Which helping word expresses ability rather than advice, duty, or possibility?",
                "can", ("should", "must", "might"),
                f"'I can {base} {complement}' says I have that ability. "
                "Should gives advice, must expresses duty, and might expresses possibility.",
            )
    elif concept == "modal-base-form":
        for base, third, past, ing, complement, event in _ACTIONS:
            past_choice = f"to {base}" if past == base else past
            yield (
                f"We should ___ {complement} before the visitors arrive.",
                "Which verb form correctly follows the helping word 'should'?",
                base, (third, past_choice, ing),
                f"'Should {base}' uses the base form of the main verb. "
                f"In 'We should {base} {complement}', should is not followed by an -s or -ing form.",
            )
    elif concept == "linking-verb-adjective":
        rows = (
            ("The bread", "smells", "fresh", "freshly", "freshness", "freshen"),
            ("The melon", "tastes", "sweet", "sweetly", "sweetness", "sweeten"),
            ("The fleece", "feels", "soft", "softly", "softness", "soften"),
            ("The polished stone", "feels", "smooth", "smoothly", "smoothness", "smooths"),
            ("The bark", "feels", "rough", "roughly", "roughness", "roughen"),
            ("The new lamp", "looks", "bright", "brightly", "brightness", "brighten"),
            ("The empty playground", "seems", "quiet", "quietly", "quietness", "quiets"),
            ("The water in the glass", "looks", "clear", "clearly", "clarity", "clears"),
            ("The woolen shawl", "feels", "warm", "warmly", "warmth", "warms"),
            ("The shaded room", "feels", "cool", "coolly", "coolness", "cools"),
            ("The music", "sounds", "pleasant", "pleasantly", "pleasantness", "pleases"),
            ("Her voice", "sounds", "gentle", "gently", "gentleness", "gentlemen"),
            ("The new rope", "seems", "strong", "strongly", "strength", "strengthens"),
            ("The young tree", "looks", "healthy", "healthily", "health", "heals"),
            ("The birthday child", "looks", "happy", "happily", "happiness", "gladdens"),
            ("The greeting", "sounds", "cheerful", "cheerfully", "cheerfulness", "cheers"),
            ("The sunset", "looks", "beautiful", "beautifully", "beauty", "beautifies"),
            ("The stew", "tastes", "delicious", "deliciously", "deliciousness", "delights"),
            ("The sleeping village", "seems", "peaceful", "peacefully", "peace", "pacifies"),
        )
        for subject, linking, adjective, adverb, noun, other in rows:
            yield (
                f"{subject} {linking} ___.",
                f"Which adjective describes the subject after the linking verb '{linking}'?",
                adjective, (adverb, noun, other),
                f"'{adjective}' describes {subject.lower()}. "
                f"Here '{linking}' links the subject to that quality; '{adverb}' is an adverb, not the needed adjective.",
            )
    elif concept == "comparative-y":
        rows = (
            ("easy", "easier", "The new maze is ___ than the old maze."),
            ("happy", "happier", "The rescued puppy seems ___ than it did before."),
            ("busy", "busier", "The harbor is ___ than the quiet fishing village."),
            ("heavy", "heavier", "The full basket is ___ than the empty basket."),
            ("noisy", "noisier", "The playground is ___ than the reading room."),
            ("tidy", "tidier", "The desk looks ___ than it did before we cleaned it."),
            ("funny", "funnier", "The second joke was ___ than the first joke."),
            ("messy", "messier", "The art table is ___ than the writing desk."),
            ("hungry", "hungrier", "After the long walk, I felt ___ than before."),
            ("healthy", "healthier", "The watered plant looks ___ than the dry plant."),
            ("early", "earlier", "The morning train is ___ than the bus."),
            ("pretty", "prettier", "I think the painted vase is ___ than the plain one."),
            ("dirty", "dirtier", "My hiking boots are ___ than my indoor shoes."),
            ("cloudy", "cloudier", "The sky is ___ than it was at sunrise."),
            ("sunny", "sunnier", "The south-facing room is ___ than the cellar."),
            ("windy", "windier", "The hilltop is ___ than the sheltered valley."),
            ("tasty", "tastier", "The ripe peach is ___ than the unripe peach."),
            ("lucky", "luckier", "The winning player felt ___ than the other players."),
            ("chilly", "chillier", "The cave is ___ than the meadow outside."),
        )
        for adjective, comparative, passage in rows:
            yield (
                passage, f"Choose the correctly spelled comparative of '{adjective}'.",
                comparative, (adjective, adjective[:-1] + "iest", adjective + "er"),
                f"To compare using '{adjective}', change its final y to i and add -er: '{comparative}'. "
                "The word than introduces the other side of the comparison.",
            )
    elif concept == "superlative-est":
        rows = (
            ("tall", "taller", "tallest", "sunflowers"),
            ("small", "smaller", "smallest", "pebbles"),
            ("bright", "brighter", "brightest", "lanterns"),
            ("dark", "darker", "darkest", "caves"),
            ("fast", "faster", "fastest", "runners"),
            ("slow", "slower", "slowest", "snails"),
            ("long", "longer", "longest", "ribbons"),
            ("short", "shorter", "shortest", "pencils"),
            ("warm", "warmer", "warmest", "coats"),
            ("cold", "colder", "coldest", "rooms"),
            ("deep", "deeper", "deepest", "pools"),
            ("high", "higher", "highest", "peaks"),
            ("low", "lower", "lowest", "shelves"),
            ("old", "older", "oldest", "trees"),
            ("young", "younger", "youngest", "puppies"),
            ("strong", "stronger", "strongest", "ropes"),
            ("soft", "softer", "softest", "blankets"),
            ("thick", "thicker", "thickest", "books"),
            ("thin", "thinner", "thinnest", "slices"),
        )
        for adjective, comparative, superlative, group in rows:
            yield (
                f"Of all the {group}, this one is the ___.",
                f"Choose the superlative of '{adjective}' to single out one from the whole group.",
                superlative, (adjective, comparative, f"more {adjective}"),
                f"'{superlative}' is the superlative of '{adjective}'. "
                f"It singles out one of all the {group}, rather than making the comparison '{comparative}'.",
            )
    elif concept == "comparative-more":
        rows = (
            ("comfortable", "This padded seat is ___ than the wooden bench."),
            ("colorful", "The festival banner is ___ than the gray curtain."),
            ("interesting", "I found the nature trail ___ than the empty car park."),
            ("expensive", "The silver bracelet is ___ than the cloth wristband."),
            ("difficult", "The steep climb is ___ than the gentle walk."),
            ("beautiful", "I think the restored garden is ___ than it was before."),
            ("dangerous", "The broken ladder is ___ than the sturdy steps."),
            ("important", "Keeping everyone safe is ___ than finishing first."),
            ("exciting", "The final race was ___ than the practice run."),
            ("careful", "The glass blower is ___ than the clumsy apprentice."),
            ("useful", "A working torch is ___ than an empty matchbox in the dark."),
            ("peaceful", "The quiet garden is ___ than the busy station."),
            ("delicious", "I think the homemade soup is ___ than the canned soup."),
            ("popular", "The adventure story is ___ than the old instruction book."),
            ("reliable", "The repaired engine is ___ than the faulty engine."),
            ("powerful", "The tugboat is ___ than the small rowing boat."),
            ("creative", "The new design is ___ than a copy of the old design."),
            ("successful", "The second seed planting was ___ than the first attempt."),
            ("generous", "Sharing the whole basket is ___ than offering a single berry."),
        )
        for adjective, passage in rows:
            yield (
                passage, f"Choose the usual comparative form of '{adjective}'.",
                f"more {adjective}", (f"most {adjective}", adjective, adjective + "er"),
                f"Use 'more {adjective}' for this comparison with than. "
                f"'{adjective}' normally takes more instead of an -er ending; most marks a superlative.",
            )
    elif concept == "comparative-irregular":
        rows = (
            ("good", "The repaired radio has ___ sound than it had before."),
            ("bad", "The torn sail is in ___ condition than the patched sail."),
            ("good", "After practice, my handwriting is ___ than it was last week."),
            ("bad", "The flooded road is in ___ condition than the dry path."),
            ("good", "The new plan is ___ than the one that failed."),
            ("bad", "The spoiled apple tastes ___ than the fresh one."),
            ("good", "The sheltered spot is ___ for our picnic than the windy hill."),
            ("bad", "The broken chair is a ___ choice than the sturdy stool."),
            ("good", "The clear recording is ___ than the crackling recording."),
            ("bad", "The leaky roof is in ___ shape than the roof we repaired."),
            ("good", "The waterproof boots are ___ for rain than the cloth slippers."),
            ("bad", "The thick fog makes visibility ___ than it was at dawn."),
            ("good", "The careful drawing is ___ than my first rushed sketch."),
            ("bad", "The empty battery gives a ___ result than the charged one."),
            ("good", "The revised ending is ___ than the confusing first ending."),
            ("bad", "The traffic jam is ___ than yesterday's short delay."),
            ("good", "The polished lens gives a ___ view than the scratched lens."),
            ("good", "The improved recipe makes ___ bread than the old recipe."),
            ("bad", "The burnt toast smells ___ than the lightly toasted slice."),
        )
        for adjective, passage in rows:
            comparative, superlative = ("better", "best") if adjective == "good" else ("worse", "worst")
            yield (
                passage, f"Choose the irregular comparative of '{adjective}'.",
                comparative, (adjective, superlative, adjective + "er"),
                f"The comparative of '{adjective}' is '{comparative}', not '{adjective}er'. "
                f"Use '{comparative}' with than; '{superlative}' is the superlative form.",
            )
    elif concept == "adjective-versus-adverb":
        rows = (
            ("careful", "potter", "shapes", "the clay", "carefully"),
            ("quiet", "librarian", "speaks", "to the readers", "quietly"),
            ("patient", "nurse", "waits", "beside the bed", "patiently"),
            ("brave", "firefighter", "acts", "during the rescue", "bravely"),
            ("gentle", "rider", "guides", "the pony", "gently"),
            ("cheerful", "shopkeeper", "greets", "the customers", "cheerfully"),
            ("eager", "student", "listens", "to the storyteller", "eagerly"),
            ("polite", "visitor", "asks", "for directions", "politely"),
            ("skillful", "carpenter", "carves", "the wooden bowl", "skillfully"),
            ("graceful", "dancer", "moves", "across the stage", "gracefully"),
            ("thoughtful", "helper", "arranges", "the chairs", "thoughtfully"),
            ("joyful", "child", "sings", "in the garden", "joyfully"),
            ("playful", "puppy", "bounces", "around the ball", "playfully"),
            ("cautious", "climber", "steps", "onto the ledge", "cautiously"),
            ("neat", "pupil", "writes", "in the notebook", "neatly"),
            ("calm", "captain", "speaks", "to the crew", "calmly"),
            ("proud", "winner", "smiles", "at the crowd", "proudly"),
            ("curious", "explorer", "looks", "inside the cave", "curiously"),
            ("helpful", "guide", "points", "toward the exit", "helpfully"),
        )
        for adjective, noun, verb, complement, adverb in rows:
            yield (
                f"The {adjective} {noun} {verb} {complement} {adverb}.",
                f"Which word describes the {noun} rather than how the action happens?",
                adjective, (adverb, verb, noun),
                f"'{adjective}' is an adjective describing the noun '{noun}'. "
                f"'{adverb}' is an adverb describing how the {noun} {verb} {complement}.",
            )
    elif concept == "adverb-well":
        rows = (
            "The goalkeeper played ___ and saved every shot.",
            "The pupil read ___ and pronounced each word clearly.",
            "The musician performed ___ and kept a steady rhythm.",
            "The carpenter worked ___ and made a sturdy bench.",
            "The guide explained the route ___ so nobody got lost.",
            "The gardener cared for the seedlings ___, and they all grew.",
            "The swimmer raced ___ and reached the finish smoothly.",
            "The actor spoke ___ so everyone could hear each line.",
            "The rider handled the pony ___ through the narrow gate.",
            "The children cooperated ___ and completed the puzzle together.",
            "The artist drew the bird ___, including every feather.",
            "The baker kneaded the dough ___ before leaving it to rise.",
            "The mechanic repaired the engine ___, and it ran smoothly.",
            "The student listened ___ and remembered all the instructions.",
            "The team defended ___ and kept the other side from scoring.",
            "The chef cooked the vegetables ___ without burning them.",
            "The dancer balanced ___ on the narrow platform.",
            "The librarian organized the books ___ so readers could find them.",
            "The apprentice measured the board ___ before cutting it.",
        )
        for passage in rows:
            completed = passage.replace("___", "well")
            yield (
                passage, "Choose the adverb related to 'good' that describes how the action was done.",
                "well", ("good", "goodness", "wellness"),
                f"'{completed}' uses well to describe the action. "
                "Well is the adverb related to good; goodness and wellness are nouns.",
            )
    elif concept == "adverb-comparison":
        rows = (
            ("fast", "faster", "fastest", "The ferry travels fast, but the speedboat travels even ___ than the ferry."),
            ("hard", "harder", "hardest", "I worked hard yesterday, but I worked even ___ today than yesterday."),
            ("early", "earlier", "earliest", "The first robin sang early, but the blackbird sang even ___ than the robin."),
            ("late", "later", "latest", "The bus arrived late, but the train arrived even ___ than the bus."),
            ("high", "higher", "highest", "The sparrow flew high, but the eagle flew even ___ than the sparrow."),
            ("low", "lower", "lowest", "The first kite flew low, but the second kite flew even ___ than the first."),
            ("long", "longer", "longest", "We waited a long time, but the other group waited even ___ than we did."),
            ("soon", "sooner", "soonest", "The peas sprouted soon, but the beans sprouted even ___ than the peas."),
            ("fast", "faster", "fastest", "The stream flows fast, but the river flows even ___ than the stream."),
            ("hard", "harder", "hardest", "The drummer struck hard, but then struck even ___ than before."),
            ("early", "earlier", "earliest", "We left early on Monday, but on Tuesday we left even ___ than on Monday."),
            ("late", "later", "latest", "The owl hunted late, but the fox hunted even ___ than the owl."),
            ("high", "higher", "highest", "The tennis ball bounced high, but the rubber ball bounced even ___ than it."),
            ("low", "lower", "lowest", "The first swallow swooped low, but the next one swooped even ___ than the first."),
            ("long", "longer", "longest", "The first team practiced a long time, but the second practiced even ___ than the first."),
            ("soon", "sooner", "soonest", "The small parcel arrived soon, but the letter arrived even ___ than the parcel."),
            ("fast", "faster", "fastest", "The pony trotted fast, but the horse trotted even ___ than the pony."),
            ("hard", "harder", "hardest", "The wind blew hard at dawn, but blew even ___ at noon than at dawn."),
            ("early", "earlier", "earliest", "The roses bloomed early, but the crocuses bloomed even ___ than the roses."),
        )
        for adverb, comparative, superlative, passage in rows:
            yield (
                passage, f"Which comparative adverb of '{adverb}' describes the second action?",
                comparative, (superlative, adverb, f"most {adverb}"),
                f"'{comparative}' compares how or when the two actions happen. "
                f"It modifies the action as an adverb; '{superlative}' would be a superlative, not the comparison with than.",
            )
    elif concept == "adverb-sequence":
        rows = (
            ("We peeled the banana.", "Then", "we sliced it into a bowl.", "peeled", "banana", "sliced"),
            ("We measured the fabric.", "Next", "we cut it along the line.", "measured", "fabric", "cut"),
            ("We kneaded the dough.", "Afterward", "we left it to rise.", "kneaded", "dough", "left"),
            ("We dug a hole.", "Then", "we planted the sapling.", "dug", "hole", "planted"),
            ("We put on our boots.", "Next", "we walked to the pond.", "put", "boots", "walked"),
            ("We finished the sketch.", "Afterward", "we colored the background.", "finished", "sketch", "colored"),
            ("We washed the jars.", "Then", "we filled them with jam.", "washed", "jars", "filled"),
            ("We tuned the violin.", "Next", "we played the melody.", "tuned", "violin", "played"),
            ("We picked the flowers.", "Afterward", "we arranged them in a vase.", "picked", "flowers", "arranged"),
            ("We packed the tent.", "Then", "we loaded it into the car.", "packed", "tent", "loaded"),
            ("We checked the weather.", "Next", "we chose a sheltered route.", "checked", "weather", "chose"),
            ("We fed the hens.", "Afterward", "we collected their eggs.", "fed", "hens", "collected"),
            ("We opened the puzzle box.", "Then", "we sorted the pieces.", "opened", "box", "sorted"),
            ("We mixed the paint.", "Next", "we brushed it onto the fence.", "mixed", "paint", "brushed"),
            ("We cleaned the cage.", "Afterward", "we added fresh bedding.", "cleaned", "cage", "added"),
            ("We wrote the address.", "Then", "we posted the letter.", "wrote", "address", "posted"),
            ("We boiled the water.", "Next", "we poured it into the teapot.", "boiled", "water", "poured"),
            ("We repaired the puncture.", "Afterward", "we pumped up the tire.", "repaired", "puncture", "pumped"),
            ("We folded the paper.", "Then", "we creased each corner.", "folded", "paper", "creased"),
        )
        for first, adverb, second, verb_a, noun, verb_b in rows:
            yield (
                f"{first} {adverb} {second}",
                "Which word signals that the second action happens after the first?",
                adverb, (verb_a, noun, verb_b),
                f"'{adverb}' is a time adverb showing the order of events. "
                f"The action '{verb_b}' follows '{verb_a}', rather than happening before it.",
            )
    elif concept == "adverb-degree":
        rows = (
            ("soup", "very", "hot"), ("tower", "extremely", "tall"),
            ("puppy", "really", "sleepy"), ("path", "quite", "narrow"),
            ("parcel", "too", "heavy"), ("cave", "rather", "dark"),
            ("blanket", "very", "soft"), ("puzzle", "extremely", "difficult"),
            ("melon", "really", "sweet"), ("pond", "quite", "deep"),
            ("rope", "too", "short"), ("attic", "rather", "dusty"),
            ("kite", "very", "colorful"), ("drum", "extremely", "loud"),
            ("garden", "really", "peaceful"), ("slope", "quite", "steep"),
            ("coat", "too", "large"), ("weather", "rather", "chilly"),
            ("glass", "very", "fragile"),
        )
        for noun, adverb, adjective in rows:
            yield (
                f"The {noun} is {adverb} {adjective}.",
                f"Which adverb changes the degree of the adjective '{adjective}'?",
                adverb, (noun, "is", adjective),
                f"'{adverb}' modifies the adjective '{adjective}', telling to what degree the {noun} is {adjective}. "
                "It is an adverb of degree, not the noun or the linking verb.",
            )
    elif concept == "pronoun-compound-subject":
        rows = (
            ("The gardener", "planted a cherry tree"),
            ("My brother", "repaired the bicycle"),
            ("The librarian", "sorted the donated books"),
            ("My cousin", "baked a tray of buns"),
            ("The guide", "marked the trail"),
            ("My sister", "decorated the lanterns"),
            ("The coach", "carried the goalposts"),
            ("My uncle", "mended the garden gate"),
            ("The artist", "painted a mural"),
            ("My friend", "assembled a model boat"),
            ("The keeper", "fed the penguins"),
            ("My aunt", "sewed a patchwork quilt"),
            ("The captain", "checked the ropes"),
            ("My neighbor", "cleared the icy steps"),
            ("The teacher", "prepared a science display"),
            ("My grandfather", "picked ripe plums"),
            ("The drummer", "practiced the rhythm"),
            ("My grandmother", "made fresh lemonade"),
            ("The baker", "packed the loaves"),
        )
        for partner, action in rows:
            yield (
                f"{partner} and ___ {action}.",
                "Which subject pronoun refers to the speaker as one of the people doing the action?",
                "I", ("me", "my", "mine"),
                f"Leave out '{partner} and': 'I {action}' is correct. "
                f"Use I, the speaker's subject pronoun, in '{partner} and I {action}' too.",
            )
    elif concept == "pronoun-compound-object":
        rows = (
            ("The coach thanked my brother and ___ for carrying the cones.", "thanked", "my brother"),
            ("The guide led my cousin and ___ through the cave.", "led", "my cousin"),
            ("The baker invited my sister and ___ into the kitchen.", "invited", "my sister"),
            ("The teacher chose my friend and ___ for the demonstration.", "chose", "my friend"),
            ("The nurse helped my father and ___ with the heavy chair.", "helped", "my father"),
            ("The librarian welcomed my mother and ___ to the book club.", "welcomed", "my mother"),
            ("The captain called my uncle and ___ onto the deck.", "called", "my uncle"),
            ("The artist sketched my aunt and ___ beside the fountain.", "sketched", "my aunt"),
            ("The driver saw my neighbor and ___ at the bus stop.", "saw", "my neighbor"),
            ("The ranger warned my grandfather and ___ about the slippery path.", "warned", "my grandfather"),
            ("The gardener taught my grandmother and ___ to prune roses.", "taught", "my grandmother"),
            ("The audience applauded the singer and ___ after the duet.", "applauded", "the singer"),
            ("The photographer filmed the dancer and ___ during rehearsal.", "filmed", "the dancer"),
            ("The mayor congratulated the winner and ___ on our teamwork.", "congratulated", "the winner"),
            ("The keeper showed the visitor and ___ the new enclosure.", "showed", "the visitor"),
            ("The musician asked the pupil and ___ to keep the beat.", "asked", "the pupil"),
            ("The host greeted the traveler and ___ at the door.", "greeted", "the traveler"),
            ("The shopkeeper served the customer and ___ before closing.", "served", "the customer"),
            ("The leader reminded the helper and ___ to wear gloves.", "reminded", "the helper"),
        )
        for passage, verb, partner in rows:
            yield (
                passage, "Which object pronoun refers to the speaker as one of the people receiving the action?",
                "me", ("I", "my", "mine"),
                f"After '{verb}', use me, the speaker's object pronoun. Removing '{partner} and' leaves "
                f"'{verb} me', so '{verb} {partner} and me' uses the same object form.",
            )
    elif concept == "possessive-before-noun":
        for scene, owner, item, before, alone, subject, object_form in _OWNERSHIP:
            other = "himself" if before == alone else alone
            yield (
                f"{scene} The {item} belongs to {owner}. This is ___ {item}.",
                f"Which ownership word correctly goes immediately before '{item}'?",
                before, (other, subject, object_form),
                f"'{before}' goes before the noun in '{before} {item}' to show that it belongs to {owner}. "
                "The gap needs the ownership form used with a following noun.",
            )
    elif concept == "possessive-stands-alone":
        for scene, owner, item, before, alone, subject, object_form in _OWNERSHIP:
            other = "himself" if before == alone else before
            yield (
                f"{scene} The {item} belongs to {owner}. It is ___.",
                "Which possessive pronoun shows that ownership without a noun after it?",
                alone, (other, subject, object_form),
                f"'{alone}' means 'belonging to {owner}' and stands alone in 'It is {alone}'. "
                f"It replaces the ownership phrase '{before} {item}' without repeating '{item}'.",
            )
    elif concept == "pronoun-reflexive":
        rows = (
            ("I", "built the model", "myself", ("yourself", "herself", "themselves")),
            ("You", "packed the suitcase", "yourself", ("myself", "herself", "ourselves")),
            ("He", "repaired the loose handle", "himself", ("herself", "myself", "themselves")),
            ("She", "painted the scenery", "herself", ("himself", "yourself", "ourselves")),
            ("We", "organized the picnic", "ourselves", ("myself", "themselves", "himself")),
            ("They", "cleared the woodland path", "themselves", ("ourselves", "herself", "itself")),
            ("I", "learned the new tune", "myself", ("himself", "itself", "yourselves")),
            ("You", "designed the poster", "yourself", ("myself", "himself", "themselves")),
            ("He", "cooked the vegetable stew", "himself", ("herself", "ourselves", "yourself")),
            ("She", "assembled the bookshelf", "herself", ("himself", "myself", "themselves")),
            ("We", "made the bird feeders", "ourselves", ("themselves", "herself", "itself")),
            ("They", "planted the herb garden", "themselves", ("ourselves", "myself", "yourself")),
            ("I", "mended the kite", "myself", ("herself", "ourselves", "itself")),
            ("You", "sorted the recycling", "yourself", ("myself", "himself", "ourselves")),
            ("He", "carved the wooden spoon", "himself", ("herself", "yourself", "themselves")),
            ("She", "wrote the whole play", "herself", ("himself", "ourselves", "myself")),
            ("We", "sewed the costumes", "ourselves", ("themselves", "itself", "herself")),
            ("They", "built the stone wall", "themselves", ("ourselves", "myself", "himself")),
            ("It", "opened the gate", "itself", ("myself", "ourselves", "yourself")),
        )
        for subject, action, reflexive, distractors in rows:
            introduction = "The clever pony needed no help. " if subject == "It" else ""
            you_clue = " Here, 'you' means one person." if subject == "You" else ""
            yield (
                f"{introduction}{subject} {action} by ___.{you_clue}",
                f"Which reflexive pronoun points back to '{subject}'?",
                reflexive, distractors,
                f"'{reflexive}' matches the subject '{subject}'. "
                f"'{subject} {action} by {reflexive}' means the task was done without help from others.",
            )
    elif concept == "pronoun-demonstrative":
        rows = (
            ("The kites are far away above the hill. ___ are soaring in the wind.", "Those", "several distant kites"),
            ("The ticket is in my hand. ___ is the ticket I will use.", "This", "one nearby ticket"),
            ("The lighthouse is far away on the headland. ___ is where the beam comes from.", "That", "one distant lighthouse"),
            ("The seedlings are right beside me. ___ are ready to plant.", "These", "several nearby seedlings"),
            ("The boats are far away beyond the pier. ___ are returning from the race.", "Those", "several distant boats"),
            ("The mitten is here in my palm. ___ is the mitten I found.", "This", "one nearby mitten"),
            ("The waterfall is far away across the valley. ___ is where we will hike.", "That", "one distant waterfall"),
            ("The shells are here at my feet. ___ are smooth from the waves.", "These", "several nearby shells"),
            ("The deer are far away at the edge of the forest. ___ are grazing.", "Those", "several distant deer"),
            ("The compass is here in my hand. ___ is pointing north.", "This", "one nearby compass"),
            ("The castle is far away on the ridge. ___ is the oldest building in the region.", "That", "one distant castle"),
            ("The berries are here in my bowl. ___ are ready to eat.", "These", "several nearby berries"),
            ("The balloons are far away above the town. ___ are drifting east.", "Those", "several distant balloons"),
            ("The postcard is here between my fingers. ___ is from my cousin.", "This", "one nearby postcard"),
            ("The windmill is far away beyond the fields. ___ is still turning.", "That", "one distant windmill"),
            ("The gloves are here on my lap. ___ are the gloves I knitted.", "These", "several nearby gloves"),
            ("The mountains are far away on the horizon. ___ are covered in snow.", "Those", "several distant mountains"),
            ("The key is here in my palm. ___ is for the garden gate.", "This", "one nearby key"),
            ("The stepping stones are right beside me. ___ are dry enough to stand on.", "These", "several nearby stepping stones"),
        )
        choices = ("This", "That", "These", "Those")
        for passage, correct, reference in rows:
            yield (
                passage, "Which demonstrative pronoun matches both the number and the stated distance?",
                correct, tuple(choice for choice in choices if choice != correct),
                f"'{correct}' points to {reference}. This and that refer to one; these and those refer to several. "
                "This and these are near; that and those are farther away.",
            )
    elif concept == "pronoun-reference":
        rows = (
            ("Maya found a compass. She used it beside the map.", "a compass", "Maya", "the map", "found"),
            ("Noah picked a pear. He carried it in a basket.", "a pear", "Noah", "a basket", "picked"),
            ("Asha bought a lantern. She hung it beside the tent.", "a lantern", "Asha", "the tent", "bought"),
            ("Ben made a kite. He flew it above the field.", "a kite", "Ben", "the field", "made"),
            ("Lina borrowed a ruler. She laid it on the desk.", "a ruler", "Lina", "the desk", "borrowed"),
            ("Omar caught a beetle. He released it under a bush.", "a beetle", "Omar", "a bush", "caught"),
            ("Ella baked a loaf. She placed it on a rack.", "a loaf", "Ella", "a rack", "baked"),
            ("Sam carved a spoon. He polished it beside the window.", "a spoon", "Sam", "the window", "carved"),
            ("Nina drew a portrait. She displayed it in the hallway.", "a portrait", "Nina", "the hallway", "drew"),
            ("Leo planted a seed. He watered it with a jug.", "a seed", "Leo", "a jug", "planted"),
            ("Zara sewed a cushion. She put it on the sofa.", "a cushion", "Zara", "the sofa", "sewed"),
            ("Finn repaired a wheel. He attached it to the cart.", "a wheel", "Finn", "the cart", "repaired"),
            ("Tara folded a napkin. She set it beside the plate.", "a napkin", "Tara", "the plate", "folded"),
            ("Ravi found a feather. He tucked it inside a notebook.", "a feather", "Ravi", "a notebook", "found"),
            ("Esme painted a sign. She fastened it to the gate.", "a sign", "Esme", "the gate", "painted"),
            ("Hugo washed a cup. He dried it with a towel.", "a cup", "Hugo", "a towel", "washed"),
            ("Ivy collected a pebble. She dropped it into a jar.", "a pebble", "Ivy", "a jar", "collected"),
            ("Arun wrote a poem. He read it beside the fireplace.", "a poem", "Arun", "the fireplace", "wrote"),
            ("Rose knitted a scarf. She wrapped it around the snowman.", "a scarf", "Rose", "the snowman", "knitted"),
        )
        for passage, antecedent, person, later_noun, verb in rows:
            yield (
                passage, "What does 'it' replace in the second sentence?",
                antecedent, (person, later_noun, verb),
                f"'It' refers back to '{antecedent}', the thing introduced in the first sentence. "
                f"'{person}' is the person acting, and '{later_noun}' is introduced later, not replaced by it.",
            )
    elif concept == "preposition-into":
        rows = (
            ("The rabbit hopped ___ the burrow, moving from the field to its inside.", "the burrow"),
            ("The marble rolled ___ the tube, entering its hollow center.", "the tube"),
            ("The diver lowered herself ___ the pool, moving from the deck to the water.", "the pool"),
            ("The driver steered the car ___ the garage, bringing it fully inside.", "the garage"),
            ("The bee flew ___ the hive through its entrance.", "the hive"),
            ("The child crawled ___ the tent, going from the grass to the inside.", "the tent"),
            ("The cook poured the milk ___ the jug, filling it halfway.", "the jug"),
            ("The kitten squeezed ___ the basket until its whole body was inside.", "the basket"),
            ("The explorer climbed ___ the cave through the narrow entrance.", "the cave"),
            ("The worker slid the letter ___ the envelope, enclosing it completely.", "the envelope"),
            ("The hamster scurried ___ its cage through the open door.", "its cage"),
            ("The sailor stepped ___ the cabin, leaving the deck for the sheltered interior.", "the cabin"),
            ("The train passed ___ the tunnel through its entrance.", "the tunnel"),
            ("The child dropped a coin ___ the jar, where it landed on the other coins.", "the jar"),
            ("The swimmer dived ___ the sea, leaving the boat and entering the water.", "the sea"),
            ("The shopper placed the apples ___ the bag so the bag held them.", "the bag"),
            ("The puppy ran ___ the kennel, entering through the little doorway.", "the kennel"),
            ("The librarian slipped the card ___ the pocket inside the book.", "the pocket"),
            ("The baker tipped the flour ___ the bowl, adding it to the mixture inside.", "the bowl"),
        )
        for passage, destination in rows:
            yield (
                passage, "Which preposition shows movement from outside something to its inside?",
                "into", ("above", "beside", "behind"),
                f"'Into {destination}' describes movement to the inside of that place or container. "
                "The sentence describes entering, not remaining above, beside, or behind it.",
            )
    elif concept == "preposition-across":
        rows = (
            ("To reach the opposite pavement, we walked ___ the road at the crossing.", "the road"),
            ("The swimmer went from one bank to the other by swimming ___ the river.", "the river"),
            ("The players ran ___ the field from one sideline to the opposite sideline.", "the field"),
            ("The child rolled the ball ___ the room from one wall to the other.", "the room"),
            ("The dancers moved ___ the stage from the left wing to the right wing.", "the stage"),
            ("The ferry sailed ___ the lake to the village on the opposite shore.", "the lake"),
            ("The groundskeeper pulled a rope ___ the courtyard from one edge to the other.", "the courtyard"),
            ("The beetle crawled ___ the leaf from one side to the opposite side.", "the leaf"),
            ("We dragged the cloth ___ the table, wiping from one edge to the opposite edge.", "the table"),
            ("The hikers trekked ___ the valley to the opposite hillside.", "the valley"),
            ("The sled slid ___ the frozen pond from one bank to the other.", "the frozen pond"),
            ("The child drew a line ___ the page from its left edge to its right edge.", "the page"),
            ("The helper carried the tray ___ the hall to the opposite wall.", "the hall"),
            ("The farmer led the horses ___ the meadow to the gate on the far side.", "the meadow"),
            ("The canoe traveled ___ the channel to the opposite shore.", "the channel"),
            ("The workers pulled the cable ___ the stream from one bank to the other.", "the stream"),
            ("The snail crept ___ the path until it reached the grass on the other side.", "the path"),
            ("The cleaner pushed the mop ___ the kitchen from one wall to the other.", "the kitchen"),
            ("The painter brushed a stripe ___ the door from its left edge to its right edge.", "the door"),
        )
        for passage, crossed in rows:
            yield (
                passage, "Which preposition shows a complete crossing from one side to the other?",
                "across", ("under", "beside", "toward"),
                f"'Across {crossed}' takes the movement or line from one side to the other. "
                "Toward only gives a direction; it does not say that the whole crossing happens.",
            )
    elif concept == "preposition-duration":
        rows = (
            ("The soup simmered", "half an hour"),
            ("The choir rehearsed", "two hours"),
            ("The puppy slept", "most of the afternoon"),
            ("We watched the eclipse", "several minutes"),
            ("The shop closed", "a week"),
            ("The divers stayed underwater", "twenty minutes"),
            ("The family rented the cottage", "a fortnight"),
            ("The seed rested in water", "one night"),
            ("The team practiced", "an hour"),
            ("The snow covered the path", "three days"),
            ("The artist worked on the mural", "a month"),
            ("The cyclist rested", "a few moments"),
            ("The play lasted", "ninety minutes"),
            ("The gardener watered the seedlings", "a short time"),
            ("The exhibition ran", "six weeks"),
            ("The candle burned", "the whole evening"),
            ("The boat drifted", "a quarter of an hour"),
            ("The neighbors looked after the cat", "ten days"),
            ("The old tree stood", "many years"),
        )
        for action, duration in rows:
            yield (
                f"{action} ___ {duration}.",
                "Which preposition introduces a duration, telling how long?",
                "for", ("since", "at", "into"),
                f"'For {duration}' gives the length of time involved in '{action.lower()}'. "
                "Use for with a duration; since would introduce a starting point instead.",
            )
    elif concept == "preposition-start-time":
        rows = (
            ("The lights have been off", "sunrise"),
            ("The baker has been awake", "four o'clock"),
            ("We have known our neighbors", "last summer"),
            ("The museum has been closed", "Tuesday"),
            ("The puppy has been asleep", "noon"),
            ("The gardener has worked here", "March"),
            ("The team has trained indoors", "the start of winter"),
            ("The path has been muddy", "yesterday"),
            ("The clock has been silent", "midnight"),
            ("The ferry has been at the dock", "this morning"),
            ("The artist has used this studio", "January"),
            ("The school has owned the field", "2015"),
            ("The seedlings have been in the greenhouse", "Friday"),
            ("The family has lived in the cottage", "last spring"),
            ("The market has been open", "dawn"),
            ("The hikers have been on the trail", "breakfast"),
            ("The cat has been missing", "Monday"),
            ("The choir has rehearsed here", "September"),
            ("The old mill has stood empty", "the end of autumn"),
        )
        for action, starting_point in rows:
            yield (
                f"{action} ___ {starting_point}.",
                "Which preposition marks a starting point continuing up to now?",
                "since", ("for", "during", "into"),
                f"'Since {starting_point}' marks when the situation began and connects it to now. "
                f"'{starting_point}' is a starting point here, not a duration introduced by for.",
            )
    elif concept == "prepositional-phrase":
        rows = (
            ("lantern", "beside", "tent", "glows", "brightly"),
            ("kitten", "under", "bench", "sleeps", "peacefully"),
            ("kite", "above", "roof", "flutters", "wildly"),
            ("basket", "behind", "door", "holds", "our gloves"),
            ("nest", "in", "oak", "shelters", "three chicks"),
            ("book", "on", "desk", "belongs", "to the teacher"),
            ("bridge", "over", "stream", "sways", "slightly"),
            ("statue", "near", "fountain", "shines", "in the sunlight"),
            ("cottage", "beyond", "orchard", "needs", "a new roof"),
            ("path", "through", "woods", "leads", "to the lake"),
            ("flowers", "along", "fence", "attract", "butterflies"),
            ("cup", "inside", "cupboard", "has", "a blue handle"),
            ("boots", "outside", "porch", "need", "a good scrub"),
            ("picture", "against", "wall", "shows", "a snowy mountain"),
            ("pond", "below", "hill", "freezes", "in winter"),
            ("rope", "around", "parcel", "keeps", "it secure"),
            ("chair", "beneath", "window", "faces", "the garden"),
            ("gate", "between", "houses", "opens", "onto an alley"),
            ("shell", "among", "pebbles", "catches", "the light"),
        )
        for item, preposition, place, verb, complement in rows:
            phrase = f"{preposition} the {place}"
            yield (
                f"The {item} {phrase} {verb} {complement}.",
                f"Which complete prepositional phrase gives the location of the {item}?",
                phrase, (f"The {item}", f"{verb} {complement}", f"the {place} {verb}"),
                f"'{phrase}' begins with the preposition '{preposition}' and includes its object, '{place}'. "
                f"The whole phrase locates the {item}; the other choices do not include that whole phrase.",
            )
    elif concept == "conjunction-before":
        rows = (
            ("measure the board", "cut it", ("measure", "board", "cut")),
            ("tie our laces", "start the race", ("tie", "laces", "start")),
            ("wash the apples", "slice them", ("wash", "apples", "slice")),
            ("read the recipe", "mix the batter", ("read", "recipe", "mix")),
            ("check the brakes", "ride the bicycle", ("check", "brakes", "ride")),
            ("dig a hole", "plant the tree", ("dig", "hole", "plant")),
            ("tune the violin", "play the melody", ("tune", "violin", "play")),
            ("put on gloves", "prune the roses", ("put", "gloves", "prune")),
            ("buy the tickets", "board the ferry", ("buy", "tickets", "board")),
            ("pack a picnic", "leave for the beach", ("pack", "picnic", "leave")),
            ("clear the desk", "spread out the map", ("clear", "desk", "spread")),
            ("brush the pony", "fit the saddle", ("brush", "pony", "fit")),
            ("water the seeds", "cover the tray", ("water", "seeds", "cover")),
            ("write the address", "post the card", ("write", "address", "post")),
            ("test the torch", "enter the cave", ("test", "torch", "enter")),
            ("fold the blankets", "put them away", ("fold", "blankets", "put")),
            ("listen to the instructions", "begin the experiment", ("listen", "instructions", "begin")),
            ("clean the brush", "change paint colors", ("clean", "brush", "change")),
            ("warm up our muscles", "practice the dance", ("warm", "muscles", "practice")),
        )
        for first, second, distractors in rows:
            yield (
                f"We {first} before we {second}.",
                "Which conjunction shows that the first action happens earlier than the second?",
                "before", distractors,
                f"'Before' joins the two clauses and puts 'we {first}' earlier than 'we {second}'. "
                "It shows the time relationship between the actions.",
            )
    elif concept == "conjunction-until":
        rows = (
            ("Wait at the crossing", "the signal turns green", "The green signal ends the wait."),
            ("Keep stirring", "the sugar dissolves", "Stop stirring only when no sugar crystals remain."),
            ("Stay under the shelter", "the rain stops", "Leave the shelter only when the rain has ended."),
            ("Hold the ladder", "the painter climbs down", "Let go only when the painter is safely on the ground."),
            ("Leave the bread in the oven", "the crust turns golden", "Take it out only when the crust is golden."),
            ("Keep the gate closed", "the sheep reach the field", "Open it only when the sheep arrive."),
            ("Wait on the platform", "the train arrives", "The train's arrival ends the wait."),
            ("Continue watering", "the soil feels damp", "Stop only when the soil is damp."),
            ("Keep the puppy indoors", "the vet gives permission", "Let it out only after the vet approves."),
            ("Rest your ankle", "the swelling goes down", "Resume walking only after the swelling has eased."),
            ("Leave the paint to dry", "the surface is no longer sticky", "Touch it only when it is dry."),
            ("Keep practicing the tune", "you can play it smoothly", "Smooth playing marks the end of this practice."),
            ("Stay in line", "your turn comes", "Leave the line only when it is your turn."),
            ("Keep the soup warm", "the guests arrive", "Their arrival marks the end of the waiting period."),
            ("Watch the balloon", "it disappears behind the clouds", "Stop watching only when it is out of sight."),
            ("Pull gently on the rope", "the bucket reaches the top", "Stop pulling only when the bucket is at the top."),
            ("Leave the seedlings inside", "the frost has passed", "Move them out only after the frost is over."),
            ("Stay seated", "the bus stops", "Stand only once the bus is no longer moving."),
            ("Keep the ice cream in the freezer", "it is time for dessert", "Take it out only at dessert time."),
        )
        for action, endpoint, clarification in rows:
            yield (
                f"{action} ___ {endpoint}. {clarification}",
                "Which conjunction marks the endpoint up to which the action continues?",
                "until", ("because", "although", "or"),
                f"'Until {endpoint}' sets the endpoint for '{action.lower()}'. "
                "Until means up to the time when, rather than giving a reason or a contrast.",
            )
    elif concept == "conjunction-condition":
        rows = (
            ("You may use the paints", "you cover the table first"),
            ("You may take the canoe", "you wear a life jacket"),
            ("You may borrow the telescope", "you carry it carefully"),
            ("You may join the game", "you follow the rules"),
            ("You may feed the pony", "you ask the keeper first"),
            ("You may pick the berries", "you check that they are ripe"),
            ("You may use the workshop", "you put on safety glasses"),
            ("You may keep the library book longer", "you renew it before Friday"),
            ("You may camp in the meadow", "you leave no litter"),
            ("You may take the dog for a walk", "you keep it on its lead"),
            ("You may help bake the bread", "you wash your hands first"),
            ("You may play the drum", "you wait until the baby is awake"),
            ("You may ride the bicycle", "you fasten your helmet"),
            ("You may display the artwork", "you label it with the artist's permission"),
            ("You may cross the stream", "the guide says it is safe"),
            ("You may plant the seedlings outside", "the weather stays warm"),
            ("You may use my compass", "you return it after the hike"),
            ("You may climb the tower", "an adult comes with you"),
            ("You may hold the chick", "you handle it gently"),
        )
        for permission, condition in rows:
            yield (
                f"{permission} ___ {condition}.",
                "Which conjunction presents the second clause as a condition for permission?",
                "if", ("but", "or", "and"),
                f"'If {condition}' states the condition attached to '{permission.lower()}'. "
                "If makes the permission depend on the condition, rather than merely adding another fact.",
            )
    elif concept == "conjunction-result":
        rows = (
            ("The train was delayed", "we arrived after lunch"),
            ("The bag had a hole", "the marbles fell out"),
            ("The soil was dry", "the gardener watered it"),
            ("The bridge was closed", "we took another route"),
            ("The room was dark", "I switched on the lamp"),
            ("The rope was too short", "we fetched a longer one"),
            ("The puppy was thirsty", "we filled its water bowl"),
            ("The wind dropped", "the kite fell to the ground"),
            ("The jar lid was loose", "the juice spilled"),
            ("The map was torn", "we borrowed another map"),
            ("The pond froze", "the ducks moved to the river"),
            ("The music was too loud", "the host turned it down"),
            ("The basket was full", "we brought a second basket"),
            ("The soup was hot", "I waited for it to cool"),
            ("The bike tire was flat", "we stopped to repair it"),
            ("The shelf broke", "the books landed on the floor"),
            ("The sign was hidden by leaves", "the visitors missed the turning"),
            ("The flour had run out", "the baker ordered another sack"),
            ("The clouds cleared", "we could see the stars"),
        )
        for cause, result in rows:
            yield (
                f"{cause}, ___ {result}.",
                "Which conjunction introduces the second event as a result of the first?",
                "so", ("or", "although", "because"),
                f"'{cause}' gives the cause, and '{result}' gives its result. "
                "So connects the cause to that result; because would introduce a reason instead.",
            )
    elif concept == "conjunction-concession":
        rows = (
            ("the path was steep", "the hikers reached the top"),
            ("the puzzle was difficult", "we solved it"),
            ("the puppy was tiny", "it carried the large stick"),
            ("the room was noisy", "the child kept reading"),
            ("the night was cold", "the campers stayed warm"),
            ("the bike was old", "it still worked well"),
            ("the bag was heavy", "the porter carried it easily"),
            ("the river was wide", "the swimmer reached the far bank"),
            ("the instructions were long", "the pupil read every word"),
            ("the climb was tiring", "the group remained cheerful"),
            ("the wind was strong", "the tent stayed upright"),
            ("the paint had faded", "the sign was still readable"),
            ("the garden was small", "it produced many vegetables"),
            ("the water was chilly", "the swimmers completed their race"),
            ("the singer felt nervous", "she performed confidently"),
            ("the trail was muddy", "our boots kept our feet dry"),
            ("the kitten had been frightened", "it approached the friendly visitor"),
            ("the task took patience", "the apprentice finished it carefully"),
            ("the sky was cloudy", "the telescope showed a few bright stars"),
        )
        for difficulty, outcome in rows:
            yield (
                f"___ {difficulty}, {outcome}.",
                "Which conjunction means that the second event happened despite the first fact?",
                "Although", ("Because", "Until", "Or"),
                f"'Although {difficulty}' introduces a difficulty or contrast. "
                f"The main clause, '{outcome}', says what happened despite it, not because of it.",
            )
    elif concept == "compound-subject-agreement":
        rows = (
            ("baker", "chef", "measure", "measures", "measuring", "the flour"),
            ("teacher", "pupil", "read", "reads", "reading", "the notice"),
            ("dog", "puppy", "chase", "chases", "chasing", "the ball"),
            ("robin", "blackbird", "sing", "sings", "singing", "at dawn"),
            ("gardener", "helper", "water", "waters", "watering", "the seedlings"),
            ("sailor", "captain", "check", "checks", "checking", "the ropes"),
            ("drummer", "pianist", "practice", "practices", "practicing", "the tune"),
            ("rabbit", "hare", "nibble", "nibbles", "nibbling", "the grass"),
            ("artist", "illustrator", "sketch", "sketches", "sketching", "the harbor"),
            ("guide", "visitor", "follow", "follows", "following", "the path"),
            ("carpenter", "apprentice", "sand", "sands", "sanding", "the wood"),
            ("horse", "pony", "jump", "jumps", "jumping", "over the rail"),
            ("librarian", "volunteer", "sort", "sorts", "sorting", "the books"),
            ("duck", "goose", "paddle", "paddles", "paddling", "across the pond"),
            ("tailor", "designer", "choose", "chooses", "choosing", "the fabric"),
            ("ranger", "farmer", "repair", "repairs", "repairing", "the fence"),
            ("singer", "actor", "rehearse", "rehearses", "rehearsing", "on the stage"),
            ("cyclist", "runner", "stretch", "stretches", "stretching", "before the race"),
            ("mother", "father", "pack", "packs", "packing", "the picnic basket"),
        )
        for first, second, base, third, ing, complement in rows:
            subject = f"The {first} and the {second}"
            yield (
                f"The {first} {third} {complement}. The {second} {third} {complement} too.",
                "Which sentence joins both subjects with 'and' and keeps the same present-tense meaning?",
                f"{subject} {base} {complement}.",
                (f"{subject} {third} {complement}.",
                 f"{subject} {ing} {complement}.",
                 f"{subject} is {ing} {complement}."),
                f"'{subject}' names two separate subjects joined by and, so use the plural verb '{base}'. "
                f"'{third}' and is agree with only one; '{ing}' alone lacks a helping verb.",
            )
    elif concept == "contraction-is-not":
        rows = (
            "The soup isn't boiling yet.",
            "The museum isn't open on Tuesdays.",
            "The kite isn't flying above the trees.",
            "The jar isn't full of jam.",
            "The pony isn't wearing its saddle.",
            "The paint isn't dry enough to touch.",
            "The lantern isn't bright enough for the cave.",
            "The puppy isn't sleeping in its basket.",
            "The bread isn't ready to slice.",
            "The gate isn't wide enough for the cart.",
            "The singer isn't standing on the stage.",
            "The recipe isn't written in this notebook.",
            "The train isn't stopping at our station.",
            "The compass isn't pointing toward the path.",
            "The blanket isn't made of wool.",
            "The swimmer isn't racing today.",
            "The parcel isn't addressed to us.",
            "The bridge isn't safe to cross.",
            "The gardener isn't planting flowers this morning.",
        )
        for passage in rows:
            expanded = passage.replace("isn't", "is not")
            yield (
                passage, "What two words does 'isn't' represent in this sentence?",
                "is not", ("was not", "will not", "has not"),
                f"The expanded sentence is '{expanded}' "
                "Isn't joins is and not; its apostrophe replaces the missing o in not.",
            )
    elif concept == "article-vowel-sound":
        rows = (
            ("The timer measures ___ hour.", "hour", "an", "The h is silent, so hour begins with a vowel sound."),
            ("The child gave ___ honest answer.", "honest", "an", "The h is silent, so honest begins with a vowel sound."),
            ("Being chosen as captain is ___ honor.", "honor", "an", "The h is silent, so honor begins with a vowel sound."),
            ("The story describes ___ heir to the throne.", "heir", "an", "The h is silent, so heir begins with a vowel sound."),
            ("We used ___ hourglass to time the game.", "hourglass", "an", "The h is silent, so hourglass begins with a vowel sound."),
            ("The driver turned onto ___ one-way street.", "one-way", "a", "One-way begins with a w sound, which is a consonant sound."),
            ("The illustrator drew ___ unicorn.", "unicorn", "a", "Unicorn begins with a y sound, as in you, not a vowel sound."),
            ("The player wore ___ uniform.", "uniform", "a", "Uniform begins with a y sound, as in you, not a vowel sound."),
            ("My cousin studies at ___ university.", "university", "a", "University begins with a y sound, as in you, not a vowel sound."),
            ("A compass is ___ useful tool.", "useful", "a", "Useful begins with a y sound, as in you, not a vowel sound."),
            ("The family visited ___ European city.", "European", "a", "European begins with a y sound, which is a consonant sound."),
            ("The bus followed ___ usual route.", "usual", "a", "Usual begins with a y sound, as in you, not a vowel sound."),
            ("The shop advertised ___ one-time offer.", "one-time", "a", "One-time begins with a w sound, which is a consonant sound."),
            ("The farmer cared for ___ ewe and her lamb.", "ewe", "a", "Ewe sounds like you, so its first sound is the consonant y."),
            ("The child traced ___ eight-sided shape.", "eight-sided", "an", "Eight-sided begins with the vowel sound at the start of eight."),
            ("The doctor examined ___ X-ray.", "X-ray", "an", "X-ray begins with the spoken letter name ex, which starts with a vowel sound."),
            ("We opened ___ umbrella in the rain.", "umbrella", "an", "Umbrella begins with a vowel sound rather than the y sound in uniform."),
            ("The grocer peeled ___ orange.", "orange", "an", "Orange begins with a vowel sound, so it needs an rather than a."),
            ("We spotted ___ owl in the barn.", "owl", "an", "Owl begins with a vowel sound, so it needs an rather than a."),
        )
        for passage, next_word, article, sound in rows:
            other_article = "a" if article == "an" else "an"
            yield (
                passage, f"Which article fits the first spoken sound of '{next_word}'?",
                article, (other_article, "these", "many"),
                f"{sound} Use '{article} {next_word}' because the first spoken sound, "
                "not just the first written letter, determines the article.",
            )
    else:
        raise ValueError(f"Unknown TRAIL concept: {concept}")


def expanded_lessons():
    """Yield nineteen new examples per original concept, in curriculum order."""
    for original in LESSONS["TRAIL"]:
        for suffix, (passage, prompt, correct, distractors, explanation) in enumerate(
            _examples(original.slug), start=2
        ):
            yield Lesson(
                slug=f"{original.slug}-{suffix:02d}",
                focus=original.focus,
                title=_TITLES[original.slug],
                passage=passage,
                prompt=prompt,
                correct=correct,
                distractors=distractors,
                hint=_HINTS.get(original.slug, original.hint),
                explanation=explanation,
            )
