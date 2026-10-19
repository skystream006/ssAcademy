"""Short, whole-word Sentence Scramble content for v10.

Sixty curated scenes have a different sentence frame at each reading level.
Vocabulary is restricted to compatible concrete nouns or names, rather than
combining arbitrary subjects and actions. Interleaving the scenes gives every
pool 1,000 distinct sentences and 16–17 examples of every frame.
Selected movable adverbs have additional accepted orders using exactly the same
punctuation-attached tiles. Boundary hints make the intended endpoints explicit.

The legacy phrase generator remains in word_games.py solely to reproduce v7/v9.
Bonus answers are unique within their sentence context, not across a whole pool:
ordinary comprehension answers need not become awkward identifying phrases.
"""

from collections import Counter, defaultdict
from dataclasses import dataclass

from .common import picture, question, scramble_tiles


LEVELS = ("SPROUT", "TRAIL", "SUMMIT")
WORD_BOUNDS = {"SPROUT": (4, 7), "TRAIL": (6, 10), "SUMMIT": (8, 14)}
VOCABULARY = {
    "animals": (
        "fox", "rabbit", "badger", "otter", "hedgehog", "squirrel", "deer", "mouse",
        "beaver", "raccoon", "mole", "hare", "ferret", "chipmunk", "weasel", "skunk",
        "porcupine", "hamster", "vole", "muskrat",
    ),
    "animal_groups": (
        "foxes", "rabbits", "badgers", "otters", "hedgehogs", "squirrels", "deer", "mice",
        "beavers", "raccoons", "moles", "hares", "ferrets", "chipmunks", "weasels", "skunks",
        "porcupines", "hamsters", "voles", "muskrats",
    ),
    "birds": (
        "robin", "sparrow", "blackbird", "thrush", "finch", "wren", "dove", "pigeon",
        "magpie", "crow", "jackdaw", "rook", "swallow", "swift", "starling", "warbler",
        "bunting", "lark", "chaffinch", "goldfinch",
    ),
    "names": (
        "Ava", "Ben", "Cora", "Dev", "Ella", "Finn", "Grace", "Hugo", "Isla", "Jay",
        "Kai", "Leah", "Milo", "Nia", "Omar", "Priya", "Rosa", "Sam", "Theo", "Zoe",
    ),
    "objects": (
        "pebble", "marble", "button", "shell", "ribbon", "pencil", "crayon", "key",
        "coin", "feather", "leaf", "stone", "twig", "spool", "sock", "block", "bead",
        "bell", "brush", "stamp",
    ),
    "fruit": (
        "apple", "pear", "peach", "plum", "mango", "melon", "lemon", "orange", "banana",
        "cherry", "fig", "lime", "nectarine", "apricot", "papaya", "pineapple",
        "grapefruit", "tangerine", "avocado", "coconut",
    ),
    "toys": (
        "kite", "ball", "doll", "drum", "truck", "robot", "puppet", "boat", "puzzle",
        "plane", "whistle", "wagon", "rocket", "marble", "windmill", "teddy", "spinner",
        "top", "scooter", "train",
    ),
    "supplies": (
        "pencils", "crayons", "brushes", "paints", "ribbons", "buttons", "beads", "shells",
        "stickers", "stamps", "cards", "papers", "markers", "notebooks", "rulers",
        "erasers", "folders", "scissors", "blocks", "strings",
    ),
    "roles": (
        "baker", "farmer", "teacher", "painter", "driver", "potter", "tailor", "singer",
        "dancer", "gardener", "doctor", "dentist", "nurse", "pilot", "sailor", "carpenter",
        "coach", "artist", "librarian", "mechanic",
    ),
    "flowers": (
        "daisy", "rose", "tulip", "lily", "poppy", "violet", "pansy", "orchid", "iris",
        "dahlia", "peony", "aster", "bluebell", "buttercup", "primrose", "sunflower",
        "marigold", "petunia", "geranium", "carnation",
    ),
}


@dataclass(frozen=True)
class Scene:
    title: str
    vocabulary: str
    sentences: tuple[str, str, str]
    prompt: str
    correct: str
    distractors: tuple[str, ...]
    hint: str
    alternatives: tuple[tuple[str, ...], tuple[str, ...], tuple[str, ...]]


def _scene(title, vocabulary, sprout, trail, summit, prompt, correct, distractors, hint,
           *, alternatives=None):
    return Scene(title, vocabulary, (sprout, trail, summit), prompt, correct,
                 tuple(distractors.split("|")), hint,
                 tuple((alternatives or {}).get(level, ()) for level in LEVELS))


SCENES = (
    _scene(
        "A shady rest", "animals",
        "The {item} sleeps under the tree.",
        "Under the tree, the tired {item} sleeps peacefully.",
        "While the sun shines overhead, the tired {item} sleeps under the tree.",
        "Where does the {item} sleep?", "under the tree",
        "inside a box|on a roof|beside a bed",
        "Keep the sleeping animal and its shady resting place together.",
    ),
    _scene(
        "A small discovery", "objects",
        "We found a {item}.",
        "Yesterday, we found a {item} beside the path.",
        "We found a {item} beside the path while walking home from school.",
        "Which action does the sentence describe?", "found",
        "lost|broke|sold",
        "Find who made the discovery, then tell what was found.",
    ),
    _scene(
        "A big jump", "names",
        "{item} can jump very high.",
        "{item} can jump very high over the soft mat.",
        "{item} can jump very high after weeks of careful practice with the team.",
        "What can {item} do?", "jump very high",
        "swim very fast|sing very softly|sleep very late",
        "Keep 'can' beside the action that the person is able to do.",
    ),
    _scene(
        "A polite request", "fruit",
        "Please pass me the {item}.",
        "Please pass me the {item} before we start cooking.",
        "Before we start cooking, please pass me the {item} from that basket.",
        "Which action does the speaker request?", "pass",
        "hide|drop|draw",
        "Find the polite request and the fruit to pass.",
    ),
    _scene(
        "Whose toy?", "toys",
        "Is this your {item}?",
        "Is this your {item} beside the classroom door?",
        "Is this your {item}, or should we ask someone else in the class?",
        "Which word asks about ownership?", "your",
        "this|is|the",
        "Make a question that asks whether the toy belongs to someone.",
    ),
    _scene(
        "Beside us", "supplies",
        "There are {item} beside us.",
        "There are some {item} beside us on the table.",
        "There are {item} beside us, so we can begin our craft project.",
        "Where are the {item}?", "beside us",
        "behind a wall|under the floor|outside the gate",
        "Use 'there are' to introduce the things beside the speakers.",
    ),
    _scene(
        "The blue bag", "roles",
        "My {item} has a blue bag.",
        "My {item} has a blue bag with strong handles.",
        "My {item} has a blue bag that holds everything needed for the day.",
        "What color is the bag?", "blue",
        "red|green|yellow",
        "Place the color just before the thing it describes.",
    ),
    _scene(
        "A bright bloom", "flowers",
        "How bright the {item} looks!",
        "How bright the {item} looks in the morning sunshine!",
        "How bright the {item} looks now that the morning sun has come out!",
        "How does the {item} look?", "bright",
        "dull|wilted|dusty",
        "Begin an excited observation with 'How' and end with the exclamation mark.",
    ),
    _scene(
        "Building a nest", "birds",
        "That {item} is building a nest.",
        "That {item} is building a nest near our window.",
        "That {item} is building a nest where its chicks will be sheltered.",
        "What is the {item} building?", "a nest",
        "a bridge|a tower|a fence",
        "Keep 'is building' together, then name what the bird is making.",
    ),
    _scene(
        "Favorite slices", "fruit",
        "I like {item} slices best.",
        "Of all these snacks, I like {item} slices best.",
        "Although there are many snacks on the plate, I like {item} slices best.",
        "Which snack does the speaker like best?", "{item} slices",
        "cheese cubes|carrot sticks|plain crackers",
        "Find the favorite snack and the word that shows it is the favorite.",
    ),
    _scene(
        "Across the grass", "animal_groups",
        "Two {item} race across the grass.",
        "Two playful {item} race across the grass toward shelter.",
        "As clouds gather overhead, two {item} race across the grass toward shelter.",
        "How many {item} are racing?", "two",
        "three|four|five",
        "Put the number before the animals and keep their route together.",
    ),
    _scene(
        "A pretty flower", "flowers",
        "The {item} looks pretty today.",
        "The {item} looks pretty after last night's gentle rain.",
        "The {item} looks pretty because last night's rain has washed away the dust.",
        "How does the flower look?", "pretty",
        "broken|muddy|dry",
        "Tell which flower is being described before saying how it looks.",
    ),
    _scene(
        "A funny story", "roles",
        "Our {item} made us laugh.",
        "Our {item} made us laugh with a funny story.",
        "Our {item} made us laugh by telling a story with a surprising ending.",
        "What did the {item} make us do?", "laugh",
        "cry|sleep|run",
        "Keep 'made us laugh' together to show the effect on the listeners.",
    ),
    _scene(
        "A helping hand", "names",
        "Can {item} help us now?",
        "Can {item} help us carry these books now?",
        "Can {item} help us now that the books are ready to be moved?",
        "What are the speakers asking {item} to do?", "help",
        "hide|leave|sleep",
        "Begin the request with 'Can', followed by the person's name.",
    ),
    _scene(
        "Take care", "objects",
        "Do not drop the {item}.",
        "Do not drop the {item} while crossing the bridge.",
        "Do not drop the {item}, or it may fall through the wooden bridge.",
        "What are you asked not to do?", "drop",
        "carry|hold|keep",
        "Keep 'do not' beside the action that should be avoided.",
    ),
    _scene(
        "My toy", "toys",
        "This {item} belongs to me.",
        "This {item} belongs to me, not to my brother.",
        "This {item} belongs to me, although my brother sometimes borrows it after school.",
        "Which word identifies the owner?", "me",
        "you|us|them",
        "Name the toy, then use 'belongs to' to explain ownership.",
        alternatives={"SUMMIT": (
            "This {item} belongs to me, although my brother borrows it sometimes after school.",
            "This {item} belongs to me, although sometimes my brother borrows it after school.",
        )},
    ),
    _scene(
        "An animal mystery", "animals",
        "Where did the {item} go?",
        "Where did the {item} go when the gate opened?",
        "Where did the {item} go after the keeper opened the garden gate?",
        "Which word asks about a place?", "where",
        "when|why|who",
        "Use the place question word first, then 'did' before the animal.",
    ),
    _scene(
        "Someone is coming", "roles",
        "Here comes the {item}!",
        "Here comes the {item}, carrying a basket of apples!",
        "Here comes the {item}, whose basket is filled with apples from the orchard!",
        "Which word shows that someone is approaching?", "comes",
        "leaves|hides|sleeps",
        "Start with 'Here comes' to announce someone's arrival.",
    ),
    _scene(
        "Getting fruit ready", "fruit",
        "We washed and dried the {item}.",
        "We washed and dried the {item} before preparing lunch.",
        "Before preparing lunch, we washed and dried the {item} with a clean cloth.",
        "What did we do after washing the fruit?", "dried",
        "painted|buried|dropped",
        "Keep the two actions in order: washing comes before drying.",
    ),
    _scene(
        "A warm welcome", "names",
        "She gave {item} a warm hug.",
        "She gave {item} a warm hug at the station.",
        "When the train arrived, she gave {item} a warm hug at the station.",
        "What did she give {item}?", "a warm hug",
        "a cold drink|a new hat|a small box",
        "Place the person receiving the hug between 'gave' and 'a warm hug'.",
    ),
    _scene(
        "Off the shelf", "objects",
        "The {item} fell off the shelf.",
        "The {item} fell off the shelf during our game.",
        "The {item} fell off the shelf when our ball bumped into the bookcase.",
        "Where did the {item} fall from?", "the shelf",
        "the roof|the tree|the bridge",
        "Keep 'fell off' beside the place the object fell from.",
    ),
    _scene(
        "Drawing together", "birds",
        "Let's draw a {item} together.",
        "Let's draw a {item} together after we finish reading.",
        "Let's draw a {item} together, using the picture in our book as a guide.",
        "What does the speaker suggest doing together?", "draw",
        "dance|cook|swim",
        "Begin the suggestion with 'Let's', followed by the shared activity.",
    ),
    _scene(
        "Missing a friend", "names",
        "I wish I could see {item}.",
        "I wish I could see {item} during the holidays.",
        "I wish I could see {item}, who moved to another town last year.",
        "Which word shows that the speaker wants something?", "wish",
        "know|forget|count",
        "Both 'I' tiles are needed: one before 'wish' and one before 'could'.",
    ),
    _scene(
        "A lovely bloom", "flowers",
        "What a lovely {item}!",
        "What a lovely {item} you have grown this year!",
        "What a lovely {item} you have grown by caring for it so patiently!",
        "Which word describes the flower?", "lovely",
        "heavy|noisy|square",
        "Begin the exclamation with 'What a', then describe the flower.",
    ),
    _scene(
        "Looking for supplies", "supplies",
        "Are the {item} in here?",
        "Are the {item} in here or in the cupboard?",
        "Are the {item} in here, or did someone move them into the cupboard?",
        "Which phrase asks about the speaker's current location?", "in here",
        "over there|far away|back outside",
        "Make a question about where the supplies might be.",
    ),
    _scene(
        "Feeling proud", "toys",
        "He feels proud of his {item}.",
        "He feels proud of his {item} after repairing it.",
        "He feels proud of his {item} because he repaired it without any help.",
        "How does he feel about his {item}?", "proud",
        "worried|angry|bored",
        "Put the feeling after 'feels' and keep 'proud of' together.",
    ),
    _scene(
        "Feeding time", "animals",
        "Who will feed the {item}?",
        "Who will feed the {item} while the keeper rests?",
        "Who will feed the {item} tomorrow if the keeper is away from work?",
        "What job needs to be done?", "feed",
        "paint|chase|hide",
        "Ask which person will do the feeding by starting with 'Who'.",
    ),
    _scene(
        "Ready to help", "roles",
        "Our {item} is ready to help.",
        "Our {item} is ready to help with the fair.",
        "Our {item} is ready to help, even though the fair starts very early.",
        "What is the {item} ready to do?", "help",
        "leave|argue|sleep",
        "Keep 'ready to help' together after naming the person.",
    ),
    _scene(
        "Sharing supplies", "supplies",
        "We must share the {item}.",
        "We must share the {item} so everyone can join.",
        "We must share the {item} fairly so that everyone can join the activity.",
        "What must we do with the {item}?", "share",
        "hide|waste|sell",
        "Use 'must' before the action that lets everyone take part.",
        alternatives={"SUMMIT": (
            "We must fairly share the {item} so that everyone can join the activity.",
        )},
    ),
    _scene(
        "We remembered", "names",
        "Neither {item} nor I forgot.",
        "Neither {item} nor I forgot to bring our lunch.",
        "Neither {item} nor I forgot our lunch, although we left home in a hurry.",
        "Which word pairs with 'neither'?", "nor",
        "and|but|so",
        "Use 'neither' for the first person and 'nor' for the second.",
    ),
    _scene(
        "Two friendly waves", "names",
        "Both {item} and I waved.",
        "Both {item} and I waved as the bus passed.",
        "Both {item} and I waved until the bus had disappeared around the corner.",
        "What did both people do?", "waved",
        "shouted|ran|slept",
        "Link the two people with 'both' and 'and' before their shared action.",
    ),
    _scene(
        "A nearby sound", "birds",
        "The {item} was heard nearby.",
        "The {item} was heard nearby but could not be seen.",
        "Although nobody could see it, the {item} was heard nearby among the trees.",
        "Which word tells how the bird was noticed?", "heard",
        "seen|touched|tasted",
        "Keep 'was heard' together to tell how people noticed the bird.",
    ),
    _scene(
        "Put it back", "objects",
        "Put the {item} back, please.",
        "Put the {item} back, please, before you leave today.",
        "Before you leave today, put the {item} back, please, where you found it.",
        "Which word makes the request polite?", "please",
        "put|back|the",
        "Keep 'back' with the action of returning the object.",
    ),
    _scene(
        "A drawing preference", "flowers",
        "I'd rather draw the {item}.",
        "I'd rather draw the {item} than pick it today.",
        "I'd rather draw the {item} than pick it, so others can enjoy it too.",
        "What would the speaker rather do?", "draw",
        "pick|cut|hide",
        "Keep 'I'd rather' together to introduce the preferred activity.",
    ),
    _scene(
        "Joining the team", "names",
        "Will {item} join our team?",
        "Will {item} join our team for the next match?",
        "Will {item} join our team if we promise to practice together every week?",
        "What is {item} invited to join?", "our team",
        "our choir|our trip|our picnic",
        "Ask about the future by putting 'Will' before the person's name.",
    ),
    _scene(
        "Rolling fruit", "fruit",
        "The {item} rolled past my feet.",
        "The {item} rolled past my feet and stopped nearby.",
        "The {item} rolled past my feet before coming to rest beside the cupboard.",
        "What did the {item} roll past?", "my feet",
        "my hands|my head|my elbows",
        "Put the moving fruit before 'rolled' and its route after the action.",
    ),
    _scene(
        "A bigger box", "supplies",
        "These {item} need a bigger box.",
        "These {item} need a bigger box with a lid.",
        "These {item} need a bigger box because the old one is already full.",
        "What do these {item} need?", "a bigger box",
        "a smaller bag|a warmer coat|a taller chair",
        "Keep 'a bigger box' together as the thing that is needed.",
        alternatives={"SUMMIT": (
            "These {item} need a bigger box because the old one already is full.",
        )},
    ),
    _scene(
        "Watching a bird", "birds",
        "I watched the {item} fly.",
        "I watched the {item} fly over the quiet pond.",
        "I watched the {item} fly over the pond until it vanished behind the trees.",
        "What did the speaker watch the bird do?", "fly",
        "swim|dig|sleep",
        "Place the bird between 'watched' and the action the speaker observed.",
    ),
    _scene(
        "Everyone sheltered", "animals",
        "No {item} was left outside.",
        "No {item} was left outside when the storm began.",
        "No {item} was left outside because the keepers had checked every enclosure.",
        "Which word tells us that none remained outside?", "no",
        "some|many|several",
        "Begin with the word that means not even one animal was left.",
    ),
    _scene(
        "A missing toy", "toys",
        "Someone has taken my {item}!",
        "Someone has taken my {item} without asking me first!",
        "Someone has taken my {item}, but perhaps they only wanted to borrow it!",
        "Which word shows that the person's identity is unknown?", "someone",
        "everyone|nobody|myself",
        "Keep 'has taken' together after the unknown person.",
        alternatives={"SUMMIT": (
            "Someone has taken my {item}, but perhaps they wanted only to borrow it!",
            "Someone has taken my {item}, but they perhaps only wanted to borrow it!",
            "Someone has taken my {item}, but they perhaps wanted only to borrow it!",
        )},
    ),
    _scene(
        "Good news", "names",
        "Tell {item} the good news.",
        "Tell {item} the good news when we arrive home.",
        "As soon as we arrive home, tell {item} the good news about our trip.",
        "What kind of news should be shared?", "good",
        "sad|worrying|false",
        "Put the listener's name after 'Tell', then say what to share.",
    ),
    _scene(
        "Comparing flowers", "flowers",
        "Your {item} is taller than mine.",
        "Your {item} is taller than mine despite its smaller pot.",
        "Your {item} is taller than mine, although we planted them on the same day.",
        "Which word compares the flowers' heights?", "taller",
        "brighter|softer|wider",
        "Use 'taller than' to compare one flower with the other.",
    ),
    _scene(
        "Counting animals", "animal_groups",
        "I counted three {item}.",
        "I counted three {item} near the edge of the field.",
        "I counted three {item} before they disappeared into the long grass beyond the field.",
        "How many {item} did the speaker count?", "three",
        "two|four|six",
        "Place the number just before the animals being counted.",
    ),
    _scene(
        "Asking to borrow", "toys",
        "May I borrow your {item}?",
        "May I borrow your {item} until the game ends?",
        "May I borrow your {item} if I promise to return it before lunch?",
        "Which action is the speaker asking permission for?", "borrow",
        "sell|break|hide",
        "Begin the polite permission question with 'May I'.",
    ),
    _scene(
        "A friendly smile", "roles",
        "The {item} smiled at us.",
        "The {item} smiled at us across the busy room.",
        "The {item} smiled at us as we entered the room carrying our project.",
        "What did the {item} do?", "smiled",
        "frowned|yawned|shouted",
        "Name the person, then the friendly action directed at the speakers.",
    ),
    _scene(
        "Look behind it", "objects",
        "Look behind the {item}, please.",
        "Look behind the {item}, please, for the missing bead.",
        "If you cannot find the missing bead, look behind the {item}, please.",
        "Where does the speaker ask someone to look?", "behind",
        "above|inside|beneath",
        "Keep the position word beside the object that marks where to look.",
    ),
    _scene(
        "Hearing the result", "names",
        "We heard that {item} won.",
        "We heard that {item} won the school drawing contest.",
        "We heard that {item} won the drawing contest after practicing for several weeks.",
        "What result did the speakers hear?", "won",
        "lost|left|stopped",
        "Use 'that' to connect what the speakers heard to the result.",
    ),
    _scene(
        "Choosing fruit", "fruit",
        "Which {item} should we choose?",
        "Which {item} should we choose for our fruit salad?",
        "Which {item} should we choose if we want to make a fresh fruit salad?",
        "Which word asks the reader to make a choice?", "which",
        "where|when|who",
        "Begin with 'Which', then name the fruit being considered.",
    ),
    _scene(
        "Holding carefully", "objects",
        "I am holding the {item}.",
        "I am holding the {item} carefully in both hands.",
        "I am holding the {item} carefully so that everyone can see its tiny details.",
        "What is the speaker doing?", "holding",
        "throwing|dropping|kicking",
        "Use 'am' with 'holding' to show an action happening now.",
        alternatives={
            "TRAIL": (
                "I am carefully holding the {item} in both hands.",
            ),
            "SUMMIT": (
                "I am carefully holding the {item} so that everyone can see its tiny details.",
            ),
        },
    ),
    _scene(
        "Remembering thanks", "names",
        "{item} always remembers to say thanks.",
        "{item} always remembers to say thanks after receiving help.",
        "{item} always remembers to say thanks whenever someone takes the time to help.",
        "What does {item} remember to say?", "thanks",
        "goodbye|sorry|hello",
        "Put 'always' before 'remembers' and finish the thought about being grateful.",
    ),
    _scene(
        "Water for a flower", "flowers",
        "The {item} needs more water.",
        "The {item} needs more water during this dry weather.",
        "Unless it rains soon, the {item} needs more water to keep its roots healthy.",
        "What does the {item} need more of?", "water",
        "paint|sand|paper",
        "Keep 'more water' together as the thing the flower needs.",
    ),
    _scene(
        "A hidden animal", "animals",
        "Nobody could find the {item}.",
        "Nobody could find the {item} among the thick bushes.",
        "Nobody could find the {item} until a rustling sound revealed its hiding place.",
        "Which word means that not one person could find it?", "nobody",
        "everybody|somebody|anybody",
        "Put 'Nobody' before 'could find' to show that the search was unsuccessful.",
    ),
    _scene(
        "Back again", "birds",
        "Once again, the {item} returned.",
        "Once again, the {item} returned to our garden fence.",
        "Once again, the {item} returned to the garden where it had nested last spring.",
        "Which word tells us that the bird came back?", "returned",
        "departed|vanished|stayed",
        "Keep 'Once again,' together at the start of this repeated event.",
    ),
    _scene(
        "Thanks for helping", "supplies",
        "Thanks for carrying the {item}.",
        "Thanks for carrying the {item} into our new classroom.",
        "Thanks for carrying the {item} upstairs while I brought in the heavier boxes.",
        "Which helpful action is the speaker thanking someone for?", "carrying",
        "hiding|losing|breaking",
        "Use 'Thanks for' before the helpful action ending in '-ing'.",
    ),
    _scene(
        "A quiet game", "names",
        "Without {item}, the game felt quiet.",
        "Without {item}, the game felt quiet and rather different.",
        "Without {item}, the game felt quiet because nobody else cheered quite so loudly.",
        "How did the game feel?", "quiet",
        "noisy|crowded|busy",
        "Begin with 'Without' to name the absent person, then describe the game.",
        alternatives={"TRAIL": (
            "Without {item}, the game felt rather quiet and different.",
        )},
    ),
    _scene(
        "Leave it there", "toys",
        "Leave the {item} where it is.",
        "Leave the {item} where it is until we return.",
        "Leave the {item} where it is so the younger children can find it easily.",
        "What should be done with the {item}?", "leave",
        "move|sell|hide",
        "Keep 'where it is' together to show the toy should stay in place.",
    ),
    _scene(
        "The object we found", "objects",
        "This is the {item} we found.",
        "This is the {item} we found beside the stream.",
        "This is the {item} we found while exploring the path beside the stream.",
        "Which action tells how the speakers got the {item}?", "found",
        "bought|borrowed|made",
        "Use 'we found' after the object to explain which one it is.",
    ),
    _scene(
        "Why hide?", "animals",
        "Why is the {item} hiding?",
        "Why is the {item} hiding behind that fallen log?",
        "Why is the {item} hiding behind the log instead of exploring its surroundings?",
        "Which word asks for a reason?", "why",
        "where|when|who",
        "Start with the reason question word and keep 'is' before the animal.",
    ),
    _scene(
        "A favorite drawing", "birds",
        "My favorite drawing shows a {item}.",
        "My favorite drawing shows a {item} spreading its wings.",
        "My favorite drawing shows a {item} whose wings stretch across the whole page.",
        "What shows the bird?", "my favorite drawing",
        "a classroom poster|a television program|a library photograph",
        "Keep 'My favorite drawing' together as the subject of the sentence.",
    ),
    _scene(
        "Reading aloud", "names",
        "After lunch, {item} read aloud.",
        "After lunch, {item} read aloud from a new book.",
        "After lunch, {item} read aloud while the younger children listened to the story.",
        "When did {item} read aloud?", "after lunch",
        "before breakfast|during dinner|at midnight",
        "Keep the time phrase with its comma at the beginning.",
    ),
)


def _word_orders(ordered, alternatives):
    """Keep each occurrence indexed, including in explicitly curated alternatives."""
    variants = [ordered, *alternatives]
    if any(Counter(words) != Counter(ordered) for words in alternatives):
        raise ValueError("Alternative orders must use the same punctuation-attached word tiles")
    tiles, _ = scramble_tiles(ordered)
    for _ in range(len(tiles)):
        if tiles not in variants:
            break
        tiles = tiles[1:] + tiles[:1]
    else:
        raise ValueError("No unsolved starting order is available")
    orders = []
    for words in variants:
        positions = defaultdict(list)
        for index, tile in enumerate(tiles):
            positions[tile].append(index)
        order = [positions[word].pop(0) for word in words]
        if order not in orders:
            orders.append(order)
    return tiles, orders


def word_scramble(level):
    """Yield 1,000 activities, including replacements for all three original IDs."""
    if level not in LEVELS:
        raise ValueError(f"Unknown reading level: {level}")
    level_index = LEVELS.index(level)
    low, high = WORD_BOUNDS[level]
    seen = set()
    for index in range(1_000):
        variant, scene_index = divmod(index, len(SCENES))
        scene = SCENES[scene_index]
        vocabulary = VOCABULARY[scene.vocabulary]
        item = vocabulary[(variant + scene_index * 7) % len(vocabulary)]
        text = scene.sentences[level_index].format(item=item)
        ordered = text.split()
        correct = scene.correct.format(item=item)
        if (not low <= len(ordered) <= high or text in seen
                or correct.casefold() not in text.casefold()):
            raise ValueError(f"Invalid or repeated {level} sentence: {text}")
        seen.add(text)
        alternatives = [
            text.format(item=item).split() for text in scene.alternatives[level_index]
        ]
        tiles, orders = _word_orders(ordered, alternatives)
        yield {
            "title": f"{scene.title}: {item}",
            "picture": picture("📖", "An open book for word-by-word reading practice."),
            "tiles": tiles,
            "validOrders": orders,
            "question": question(
                f"Bonus: {scene.prompt.format(item=item)}", correct,
                scene.distractors[:2 if level == "SPROUT" else 3],
            ),
            "hint": (
                f"Start with '{ordered[0]}'. End with '{ordered[-1]}'. "
                f"{scene.hint} Keep punctuation on its word."
            ),
        }
