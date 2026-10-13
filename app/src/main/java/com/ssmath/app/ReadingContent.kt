package com.ssmath.app

import kotlinx.serialization.Serializable

@Serializable
enum class ReadingGame(val title: String, val guidance: String) {
    MISSING("Missing Word Mission", "Read the sentence. Choose the word that fits."),
    SCRAMBLE("Sentence Scramble", "Tap tiles to build a sentence. Tap a placed tile to remove it."),
    SEQUENCE("Story Sequencer", "Put the illustrated events in order, then answer a question."),
    DETECTIVE("Reading Detective", "Answer the question, then tap the exact sentence that proves it."),
    CAPTION("Caption Quest", "Look at the picture. Match it to a caption."),
    CHAPTER("Choose Next Chapter", "Choose what happens next. Read your chapter and answer its question."),
    STUDIO("Story Studio", "Create a character, a setting, a problem, and an ending. Save each part."),
    PARTNERS("Story Builder Partners", "Two readers take turns writing. Arrange and reread your shared story.")
}

@Serializable
enum class ReadingLevel(val label: String) {
    SPROUT("Sprout · pictures and short sentences"),
    TRAIL("Trail · longer sentences and context clues")
}

data class ReadingPicture(val symbol: String, val description: String)
data class ReadingQuestion(val prompt: String, val choices: List<String>, val answer: Int)
data class ReadingBranch(val label: String, val text: String, val question: ReadingQuestion)
data class ReadingActivity(
    val id: String,
    val game: ReadingGame,
    val level: ReadingLevel,
    val title: String,
    val picture: ReadingPicture,
    val passage: List<String> = emptyList(),
    val question: ReadingQuestion? = null,
    val tiles: List<String> = emptyList(),
    val validOrders: List<List<Int>> = emptyList(),
    val eventPictures: List<ReadingPicture> = emptyList(),
    val evidence: Int = 0,
    val branches: List<ReadingBranch> = emptyList(),
    val writingChoices: List<List<String>> = emptyList(),
    val hint: String
)

/** Original, bundled stories: no downloads, random answer generation, or external images. */
object ReadingContent {
    private fun q(prompt: String, answer: String, vararg others: String): ReadingQuestion {
        val position = "$prompt|$answer".hashCode().ushr(1) % (others.size + 1)
        val choices = others.toMutableList().apply { add(position, answer) }
        return ReadingQuestion(prompt, choices, position)
    }

    private fun p(symbol: String, description: String) = ReadingPicture(symbol, description)

    val activities: List<ReadingActivity> = buildList {
        fun missing(level: ReadingLevel, id: String, title: String, picture: ReadingPicture,
                    sentence: String, answer: String, hint: String, vararg others: String) {
            add(ReadingActivity(id, ReadingGame.MISSING, level, title, picture,
                passage = listOf(sentence), question = q("Which word belongs in the gap?", answer, *others),
                hint = hint))
        }
        missing(ReadingLevel.SPROUT, "missing-cup", "A drink for Pip", p("🥤", "A cup, a beginning-reader word clue"),
            "Pip sips from a ___.", "cup", "Think about what can hold a drink.", "hat", "log")
        missing(ReadingLevel.SPROUT, "missing-sun", "Morning light", p("☀️", "The sun, a beginning-reader word clue"),
            "The ___ is up.", "sun", "Look for the thing that lights the sky.", "rug", "pan")
        missing(ReadingLevel.SPROUT, "missing-nest", "A soft home", p("🪺", "A nest, a beginning-reader word clue"),
            "The bird rests in its ___.", "nest", "A bird builds this home.", "sock", "bus")
        missing(ReadingLevel.TRAIL, "missing-shelter", "Rain at the park", p("🌳", "A leafy park"),
            "Rain began to fall, so the friends took ___ beneath a roof.", "shelter",
            "The roof keeps the friends dry. Which word describes protection?", "flight", "turns", "notes")
        missing(ReadingLevel.TRAIL, "missing-fragile", "The clay bowl", p("🎨", "An art table"),
            "The unfired clay bowl could break easily. It was ___.", "fragile",
            "Read the sentence before the gap: it explains the missing word.", "noisy", "distant", "hungry")
        missing(ReadingLevel.TRAIL, "missing-generous", "Enough to share", p("🧺", "A picnic basket"),
            "Mira gave half her picnic to a friend who had none. That was ___.", "generous",
            "Think about giving something to help another person.", "invisible", "ordinary", "careless")

        fun scramble(level: ReadingLevel, id: String, title: String, picture: ReadingPicture,
                     tiles: List<String>, orders: List<List<Int>>, question: ReadingQuestion, hint: String) {
            add(ReadingActivity(id, ReadingGame.SCRAMBLE, level, title, picture,
                tiles = tiles, validOrders = orders, question = question, hint = hint))
        }
        scramble(ReadingLevel.SPROUT, "scramble-cats", "Quiet paws", p("🐈", "A cat resting nearby"),
            listOf("softly", "Cats", "walk"), listOf(listOf(1, 2, 0), listOf(1, 0, 2)),
            q("Bonus: Who walks?", "Cats", "Fish", "Trees"), "Start with Cats. The word softly can go before or after walk.")
        scramble(ReadingLevel.SPROUT, "scramble-birds", "Wing beats", p("🐦", "A small bird"),
            listOf("fly", "Birds", "high"), listOf(listOf(1, 0, 2)),
            q("Bonus: What do the birds do?", "fly", "dig", "knit"), "Who is the sentence about? Start with Birds.")
        scramble(ReadingLevel.SPROUT, "scramble-boats", "On the pond", p("⛵", "A little sailboat"),
            listOf("float", "Boats", "gently"), listOf(listOf(1, 0, 2), listOf(1, 2, 0)),
            q("Bonus: How do the boats float?", "gently", "angrily", "uphill"), "Start with Boats. Gently describes the floating.")
        scramble(ReadingLevel.TRAIL, "scramble-lantern", "A warm glow", p("🏮", "A paper lantern"),
            listOf("glows", "the lantern", "At dusk,", "softly"),
            listOf(listOf(2, 1, 0, 3), listOf(2, 1, 3, 0)),
            q("Bonus: When does the lantern glow?", "At dusk", "At noon", "At breakfast"),
            "Begin with the time phrase. Softly can sit on either side of glows.")
        scramble(ReadingLevel.TRAIL, "scramble-otter", "River crossing", p("🦦", "An otter beside water"),
            listOf("the otter", "swam", "After lunch,", "quietly"),
            listOf(listOf(2, 0, 1, 3), listOf(2, 0, 3, 1)),
            q("Bonus: What happened before the swim?", "Lunch", "Bedtime", "A snowstorm"),
            "After lunch, comes first. Keep the otter together as the subject.")
        scramble(ReadingLevel.TRAIL, "scramble-seed", "A patient gardener", p("🪴", "A pot on a windowsill"),
            listOf("the seed", "Finally,", "sprouted"),
            listOf(listOf(1, 0, 2)),
            q("Bonus: Which word suggests waiting?", "Finally", "seed", "sprouted"),
            "The comma marks the introductory word.")

        fun sequence(level: ReadingLevel, id: String, title: String, events: List<String>,
                     pictures: List<ReadingPicture>, question: ReadingQuestion, hint: String) {
            // The catalog stores tiles out of story order, so arranging is a real task.
            add(ReadingActivity(id, ReadingGame.SEQUENCE, level, title, p("📖", "An open storybook"),
                tiles = listOf(events[2], events[0], events[1]), validOrders = listOf(listOf(1, 2, 0)),
                eventPictures = listOf(pictures[2], pictures[0], pictures[1]), question = question, hint = hint))
        }
        sequence(ReadingLevel.SPROUT, "sequence-seed", "Jo's seed",
            listOf("Jo puts a seed in soil.", "Jo gives it water.", "A green shoot grows."),
            listOf(p("🌰", "A seed and soil"), p("💧", "Water drops"), p("🌱", "A green shoot")),
            q("What helped the seed grow?", "Water", "A shoe", "A bell"), "Plant the seed before watering it. Growth comes last.")
        sequence(ReadingLevel.SPROUT, "sequence-toast", "Toast for Ted",
            listOf("Ted gets some bread.", "Ted toasts the bread.", "Ted eats the toast."),
            listOf(p("🍞", "Bread"), p("♨️", "Warm steam"), p("🍽️", "A plate")),
            q("What does Ted eat?", "Toast", "Soup", "Grapes"), "Find the bread first. Eating is the last event.")
        sequence(ReadingLevel.SPROUT, "sequence-kite", "A windy day",
            listOf("May gets her kite.", "May runs into the wind.", "The kite rises high."),
            listOf(p("🪁", "A kite"), p("👟", "Running shoes"), p("☁️", "Clouds in the sky")),
            q("What makes the kite rise?", "Wind", "A lamp", "Sand"), "May needs the kite before she runs.")
        sequence(ReadingLevel.TRAIL, "sequence-bridge", "The twig bridge",
            listOf("A stream blocked the ants' path, so Noor gathered twigs.",
                "She laid the twigs across the narrow stream.", "The ants crossed safely on the new bridge."),
            listOf(p("🪵", "Twigs by a stream"), p("🌉", "A small bridge"), p("🐜", "Ants on a path")),
            q("Why did Noor gather twigs?", "To help ants cross", "To feed birds", "To measure clouds"),
            "Look for the problem first, then the building, then the result.")
        sequence(ReadingLevel.TRAIL, "sequence-map", "Finding the bench",
            listOf("Eli unfolded a map to find the hidden bench.", "He followed its dotted path past the pond.",
                "At the path's end, he sat on the bench to rest."),
            listOf(p("🗺️", "A folded map"), p("👣", "Footprints"), p("🌳", "A resting place in a park")),
            q("What guided Eli?", "A map", "A drum", "The moon"),
            "He must read the map before following the path.")
        sequence(ReadingLevel.TRAIL, "sequence-mural", "The wall of waves",
            listOf("The class sketched sea shapes on paper.", "They copied the plan onto a wall with blue paint.",
                "When the paint dried, families came to admire the mural."),
            listOf(p("✏️", "Drawing materials"), p("🖌️", "A paintbrush"), p("🖼️", "A finished artwork")),
            q("Why did the class sketch first?", "To plan the mural", "To dry the paint", "To invite fish"),
            "A plan comes before painting. Visitors arrive after drying.")

        fun detective(level: ReadingLevel, id: String, title: String, passage: List<String>,
                      question: ReadingQuestion, evidence: Int, picture: ReadingPicture, hint: String) {
            add(ReadingActivity(id, ReadingGame.DETECTIVE, level, title, picture, passage = passage,
                question = question, evidence = evidence, hint = hint))
        }
        detective(ReadingLevel.SPROUT, "detective-bag", "Ready to go",
            listOf("Ari has a red bag.", "Ari puts a book in the bag.", "Ari waves to Dad."),
            q("What is in the bag?", "A book", "A drum", "A pear"), 1, p("🎒", "A school bag"),
            "Find the sentence about what Ari puts inside.")
        detective(ReadingLevel.SPROUT, "detective-pet", "A nap for Dot",
            listOf("Dot is a small cat.", "Dot plays with a ball.", "Dot naps on a mat."),
            q("Where does Dot nap?", "On a mat", "In a box", "On a bus"), 2, p("🐈", "A small cat"),
            "Look for the word naps and read to the end of that sentence.")
        detective(ReadingLevel.SPROUT, "detective-gift", "A little gift",
            listOf("Ben folds some paper.", "He makes a boat.", "He gives the boat to Nan."),
            q("Who gets the boat?", "Nan", "Ben's dog", "A bird"), 2, p("📄", "Folded paper"),
            "The sentence with gives tells you who receives the gift.")
        detective(ReadingLevel.TRAIL, "detective-puddle", "The changed route",
            listOf("Rae usually takes the short path to the library.",
                "Today, a deep puddle covered that path.", "She chose the longer, dry path instead."),
            q("Why did Rae avoid the short path?", "It was flooded", "She forgot her book", "It was too sunny"),
            1, p("🏛️", "A library building"),
            "Find the sentence that describes the obstacle, not just Rae's choice.")
        detective(ReadingLevel.TRAIL, "detective-bell", "A quiet workshop",
            listOf("Omar fixed bicycles in a busy workshop.",
                "He hung a bell above the door so he could hear visitors arrive.",
                "His tools stayed neatly on the wall."),
            q("Why did Omar hang a bell?", "To notice visitors", "To hold his tools", "To make bicycles faster"),
            1, p("🔧", "Workshop tools"), "The word so introduces the reason.")
        detective(ReadingLevel.TRAIL, "detective-shadow", "The cool corner",
            listOf("The sun warmed every stone in the courtyard.",
                "Under the broad tree, the ground stayed cool.", "Lina moved her reading rug there."),
            q("Why did Lina choose the tree?", "Its shade was cool", "It had a door", "It was noisy"),
            1, p("📚", "A stack of books"), "Which sentence explains how the ground felt under the tree?")

        fun caption(level: ReadingLevel, id: String, title: String, picture: ReadingPicture,
                    answer: String, hint: String, vararg others: String) {
            add(ReadingActivity(id, ReadingGame.CAPTION, level, title, picture,
                question = q("Which caption matches the picture?", answer, *others), hint = hint))
        }
        caption(ReadingLevel.SPROUT, "caption-cat", "Picture postcard", p("🐈 📦", "A cat beside a box"),
            "A cat is by a box.", "Match both things in the picture.", "A dog is by a box.", "A cat is by a cup.")
        caption(ReadingLevel.SPROUT, "caption-boat", "At the lake", p("⛵ 🌊", "A sailboat on water"),
            "A boat is on the water.", "Look at where the boat is.", "A boat is on a road.", "A bus is on the water.")
        caption(ReadingLevel.SPROUT, "caption-bird", "Garden visitor", p("🐦 🌳", "A bird beside a tree"),
            "A bird is by a tree.", "Find the animal and its place.", "A fish is by a tree.", "A bird is by a bed.")
        caption(ReadingLevel.TRAIL, "caption-weather", "Before the picnic", p("☁️ 🌧️ 🧺", "Clouds and falling rain over a basket"),
            "Rain clouds threaten the picnic.", "Use the weather clues to infer what may happen.",
            "Bright sunshine warms the picnic.", "Snow covers a mountain.", "The basket floats on a lake.")
        caption(ReadingLevel.TRAIL, "caption-growth", "Slow change", p("🌰 → 🌱 → 🪴", "A seed, an arrow, a shoot, an arrow, a leafy plant"),
            "A seed gradually becomes a plant.", "Read the pictures from left to right.",
            "A plant shrinks into a seed.", "A leaf turns into a stone.", "Three birds build a nest.")
        caption(ReadingLevel.TRAIL, "caption-team", "Working together", p("🧑 🪵 🧑 🌉", "Two people with timber beside a bridge"),
            "The team uses timber to build a crossing.", "Think about the material and what the people are making.",
            "One person bakes bread alone.", "The team plants flowers in a boat.", "Two people paint the moon.")

        fun chapter(level: ReadingLevel, id: String, title: String, intro: String, a: ReadingBranch,
                    b: ReadingBranch, picture: ReadingPicture) {
            add(ReadingActivity(id, ReadingGame.CHAPTER, level, title, picture,
                passage = listOf(intro), branches = listOf(a, b),
                hint = "Read the chapter you chose again. Your answer must come from this branch."))
        }
        chapter(ReadingLevel.SPROUT, "chapter-path", "Two little paths", "Kit sees two paths. Where will Kit go?",
            ReadingBranch("Go to the pond", "Kit goes to the pond. A frog hops onto a rock.",
                q("What does Kit see?", "A frog", "A fox", "A hen")),
            ReadingBranch("Go to the hill", "Kit goes up the hill. A fox rests in the sun.",
                q("What does Kit see?", "A fox", "A frog", "A hen")), p("🛤️", "A fork in a path"))
        chapter(ReadingLevel.SPROUT, "chapter-box", "The craft box", "Uma opens a craft box. What will she make?",
            ReadingBranch("Make a crown", "Uma cuts a crown. She puts a star on it.",
                q("What is on the crown?", "A star", "A leaf", "A shell")),
            ReadingBranch("Make a boat", "Uma folds a boat. She puts a leaf in it.",
                q("What is in the boat?", "A leaf", "A star", "A shell")), p("✂️", "Craft materials"))
        chapter(ReadingLevel.SPROUT, "chapter-visit", "A friendly visit", "Mo has time to visit a friend. Who will Mo visit?",
            ReadingBranch("Visit Sal", "Sal has a pup. Mo and Sal roll a ball to the pup.",
                q("What do the friends roll?", "A ball", "A hoop", "A cart")),
            ReadingBranch("Visit Ren", "Ren has a cart. Mo and Ren pull it to the gate.",
                q("What do the friends pull?", "A cart", "A ball", "A kite")), p("🏡", "A friendly neighborhood"))
        chapter(ReadingLevel.TRAIL, "chapter-signal", "The island message",
            "A paper message washes ashore: meet at sunset. Nia can follow either clue.",
            ReadingBranch("Follow the shell trail", "The shells lead Nia to a sheltered cove. Friends have brought blankets because the evening breeze is chilly.",
                q("Why did the friends bring blankets?", "To keep warm", "To catch fish", "To mark the trail")),
            ReadingBranch("Follow the ribbon trail", "Ribbons lead Nia to a hilltop. Friends have brought binoculars to watch migrating birds before dark.",
                q("Why did the friends bring binoculars?", "To see distant birds", "To keep warm", "To tie ribbons")), p("🏝️", "An island shore"))
        chapter(ReadingLevel.TRAIL, "chapter-garden", "The empty garden",
            "A community garden has an unused corner. Jules offers to help choose its future.",
            ReadingBranch("Plant night flowers", "Jules plants pale flowers that open at dusk. Soon moths visit them to sip nectar.",
                q("Why do moths visit at dusk?", "The flowers open then", "The soil freezes then", "The gates close then")),
            ReadingBranch("Build a shallow pool", "Jules makes a shallow pool with rough stones at its edge. Bees land on the stones to drink without falling in.",
                q("Why are stones placed at the edge?", "To give bees a safe landing", "To hide the water", "To stop flowers growing")), p("🌿", "A garden corner"))
        chapter(ReadingLevel.TRAIL, "chapter-music", "The street concert",
            "A sudden breeze scatters the musicians' pages. Sol can help in two different ways.",
            ReadingBranch("Gather the pages", "Sol gathers the pages and clips them in order. The musicians can read the whole tune again.",
                q("How do the clips help?", "They keep pages together", "They make the tune louder", "They call the audience")),
            ReadingBranch("Find a sheltered spot", "Sol leads the players behind a broad wall. Out of the wind, their pages stay still.",
                q("Why do the pages stay still?", "The wall blocks the wind", "The notes are heavy", "The audience is silent")), p("🎵", "Musical notes"))

        val sproutSets = listOf(
            listOf(listOf("A kind fox", "A tiny robot", "A brave duck"), listOf("a leafy park", "a cozy boat", "a sunny hill"),
                listOf("lost a hat", "found a locked box", "needed a friend"),
                listOf("asked a friend for help", "followed a trail home", "shared a happy picnic")),
            listOf(listOf("A curious cat", "A gentle giant", "A little mouse"), listOf("a quiet pond", "a windy farm", "a bright kitchen"),
                listOf("could not reach a shelf", "heard a strange tap", "lost the way"),
                listOf("made a clever plan", "found a helpful neighbor", "built a small ladder")),
            listOf(listOf("An eager otter", "A shy dragon", "A cheerful snail"), listOf("a sandy beach", "a tall tower", "a secret garden"),
                listOf("missed the bus", "needed a warm home", "spilled the seeds"),
                listOf("worked together with friends", "made a new home", "grew a colorful garden"))
        )
        val trailSets = listOf(
            listOf(listOf("A patient inventor", "A reluctant explorer", "An observant gardener"),
                listOf("an island with a silent lighthouse", "a village above the clouds", "a greenhouse in winter"),
                listOf("discovered a message nobody could read", "noticed the bridge was unsafe", "had only one seed left"),
                listOf("solved the puzzle by listening carefully", "combined everyone's ideas", "learned that small steps matter")),
            listOf(listOf("An apprentice mapmaker", "A thoughtful musician", "A determined baker"),
                listOf("a town of winding tunnels", "a theater beside the river", "a mountain shelter"),
                listOf("found that the plan was incomplete", "had to welcome an unexpected visitor", "needed to repair a mistake"),
                listOf("invented a surprising solution", "asked for advice and tried again", "turned the mistake into something useful")),
            listOf(listOf("A careful naturalist", "A resourceful traveler", "A young astronomer"),
                listOf("a forest after a storm", "a floating library", "an observatory on a hill"),
                listOf("could not explain a strange pattern", "had to protect something fragile", "needed to unite two teams"),
                listOf("tested an idea with friends", "discovered a clue in an ordinary place", "chose kindness over a prize"))
        )
        ReadingLevel.entries.forEach { level ->
            val sets = if (level == ReadingLevel.SPROUT) sproutSets else trailSets
            sets.forEachIndexed { index, choices ->
                listOf(ReadingGame.STUDIO, ReadingGame.PARTNERS).forEach { game ->
                    add(ReadingActivity("${game.name.lowercase()}-${level.name.lowercase()}-$index",
                        game, level, listOf("A surprising day", "An unexpected helper", "A new beginning")[index],
                        p(listOf("🦊", "🏰", "🌈")[index], "A story-making illustration"),
                        writingChoices = choices, hint = if (level == ReadingLevel.SPROUT)
                            "Say your idea aloud, then pick a tile or type it. Every story needs an ending."
                        else "Connect the ending to the problem. What changes because of your character's choices?"))
                }
            }
        }
    }

    fun forGame(game: ReadingGame, level: ReadingLevel) = activities.filter { it.game == game && it.level == level }
    fun find(id: String): ReadingActivity = activities.first { it.id == id }
}
